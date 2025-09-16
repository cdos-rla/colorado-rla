package au.org.democracydevelopers.corla.endpoint;

import au.org.democracydevelopers.corla.model.ContestType;
import au.org.democracydevelopers.corla.util.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import spark.HaltException;
import spark.Request;
import us.freeandfair.corla.Main;
import us.freeandfair.corla.controller.ContestCounter;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.ContestQueries;
import us.freeandfair.corla.query.CountyQueries;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.*;

import static au.org.democracydevelopers.corla.util.testUtils.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static us.freeandfair.corla.endpoint.Endpoint.AuthorizationType.STATE;

/**
 * Test sample sizes endpoint.
 * This does not test that it produces the correct _values_ - it just mocks some numbers and checks
 * that the CSV is properly printed. It also tests for basic errors, i.e. when there are no contests,
 * and when the risk limit has not been set.
 */
public class EstimateSampleSizesTests extends TestClassWithAuth {

  private final static Logger LOGGER = LogManager.getLogger(EstimateSampleSizesTests.class);

  /**
   * The estimate sample sizes endpoint.
   */
  private final EstimateSampleSizes endpoint = new EstimateSampleSizes();

  /**
   * ContestResult for mocked plurality contest.
   */
  private static ContestResult pluralityContestResult;

  /**
   * ContestResult for mocked IRV contest.
   */
  private static ContestResult irvContestResult;

  /**
   * The DoSDashboard, used to get the riskLimit()
   */
  private static DoSDashboard doSD;

  /**
   * Database init.
   */
  @BeforeClass
  public void beforeAllThisClass() {

    Persistence.beginTransaction();
    runSQLSetupScript("SQL/co-counties.sql");
    runSQLSetupScript("SQL/simple-assertions.sql");

    // Set up the audit info (this is not mocked - the DosDashboard comes from the database).
    doSD = Persistence.getByID(DoSDashboard.ID, DoSDashboard.class);

    // Set up mocked plurality contest result.
    County oneNEBAssertionCounty = CountyQueries.fromString("1");
    pluralityContestResult = new ContestResult("pluralityContest");
    Contest pluralityContest = new Contest("pluralityContest", oneNEBAssertionCounty, ContestType.PLURALITY.toString(), List.of(alice, bob, chuan), 2,1,12);
    pluralityContestResult.addContests(Set.of(pluralityContest));
    pluralityContestResult.addCounties(Set.of(oneNEBAssertionCounty));
    pluralityContestResult.setDilutedMargin(BigDecimal.valueOf(0.12));
    pluralityContestResult.setBallotCount(10000L);

    // Get the IRV Contest for one NEB AssertionCounty from the database (simple-assertions.sql).
    Optional<Contest> irvContest =  ContestQueries.forCounty(oneNEBAssertionCounty).stream().findFirst();
    if(irvContest.isEmpty()) {
      throw new RuntimeException("Database setup seems wrong");
    }

    // Set up mocked IRV contest result, based on matching contest name from database.
    irvContestResult = new ContestResult(irvContest.get().name());
    irvContestResult.addContests(Set.of(irvContest.get()));
    irvContestResult.addCounties(Set.of(oneNEBAssertionCounty));
    irvContestResult.setDilutedMargin(BigDecimal.valueOf(0.01));
    irvContestResult.setBallotCount(20000L);

  }

  /**
   * Init mocks, for authentication and risk limit.
   */
  @BeforeClass
  public void initMocks() {
    testUtils.log(LOGGER, "initMocks");

    // Mock successful auth as a state admin.
    MockitoAnnotations.openMocks(this);
    mockAuth("State test 1", 1L, STATE);
  }

  /**
   * A very basic test of proper csv output format for simple mocked data, both IRV and plurality.
   * Also tests for errors properly thrown when there are no contests, or no risk limit set.
   */
  @Test
  @Transactional
  void basicEstimatedSampleSizesPluralityAndIRV() {
    testUtils.log(LOGGER, "basicEstimatedSampleSizesPluralityAndIRV");

    // Mock the main class; mock its auth as the mocked state admin auth.
    try (MockedStatic<Main> mockedMain = Mockito.mockStatic(Main.class);
         MockedStatic<ContestCounter> mockedCounter = Mockito.mockStatic(ContestCounter.class)) {

      // Mock auth.
      mockedMain.when(Main::authentication).thenReturn(auth);

      // We seem to need a dummy request to run before.
      final Request request = new SparkRequestStub("", new HashMap<String,String>());
      endpoint.before(request, response);

      // // First test: hit the endpoint before defining the risk limit. Should throw an error.
      // Set the risk limit to null.
      doSD.updateAuditInfo(new AuditInfo(null, null, null,null, null));

      String errorBody = "";
      try {
        endpoint.endpointBody(request, response);
      } catch (HaltException e) {
        errorBody = e.body();
      }
      assertTrue(errorBody.contains("No risk limit set"));

      // // Second test: hit the endpoint when there are no contests. Should be an error.
      // Set a non-null risk limit.
      doSD.updateAuditInfo(new AuditInfo(null, null, null,null, BigDecimal.valueOf(0.04)));

      // Mock return of empty contest list.
      mockedCounter.when(() -> ContestCounter.countAllContests(false)).thenReturn(List.of());

      // Check for error response.
      errorBody = "";
      try {
        endpoint.endpointBody(request, response);
      } catch (HaltException e) {
        errorBody = e.body();
      }
      assertTrue(errorBody.contains("Could not find data"));

      // // Third test: successful retrieval of two contests.

      // Mock non-empty contest response (one plurality and one IRV contest).
      List<ContestResult> mockedContestResults = List.of(pluralityContestResult, irvContestResult);
      mockedCounter.when(() -> ContestCounter.countAllContests(false)).thenReturn(mockedContestResults);

      endpoint.endpointBody(request, response);

      // Ballots cast is zero here, though this is just an artefact of the mocks - it's fine in real runs.
      String csv = response.body();
      assertEquals(csv, String.join("\n", List.of(
          "County,Contest Name,Contest Type,Ballots Cast,Total ballots,Diluted Margin,Sample Size",
          "Adams,One NEB Assertion Contest,IRV,0,20000,0.32,21",
          "Adams,pluralityContest,PLURALITY,0,10000,0.12,56"
      )));


    }
  }
}
