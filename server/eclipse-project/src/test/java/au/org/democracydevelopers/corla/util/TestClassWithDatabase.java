/*
Democracy Developers IRV extensions to colorado-rla.

@copyright 2024 Colorado Department of State

These IRV extensions are designed to connect to a running instance of the raire
service (https://github.com/DemocracyDevelopers/raire-service), in order to
generate assertions that can be audited using colorado-rla.

The colorado-rla IRV extensions are free software: you can redistribute it and/or modify it under the terms
of the GNU Affero General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

The colorado-rla IRV extensions are distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with
raire-service. If not, see <https://www.gnu.org/licenses/>.
*/

package au.org.democracydevelopers.corla.util;

import java.util.List;
import java.util.Properties;
import java.util.Set;

import au.org.democracydevelopers.corla.model.ContestType;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.hibernate.Session;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.persistence.Persistence;

import static au.org.democracydevelopers.corla.endpoint.GenerateAssertions.UNKNOWN_WINNER;
import static au.org.democracydevelopers.corla.util.PropertiesLoader.loadProperties;
import static au.org.democracydevelopers.corla.util.testUtils.*;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;

/**
 * This class is designed to be extended by any test class that needs to interact with a test
 * instantiation of the colorado-rla database. It provides convenience methods for instantiating
 * a postgres container (initialised with one a given SQL script) and hibernate properties.
 * Database configuration properties are loaded from test.properties, except that the Hibernate URL
 * is overridden with the test container's URL.
 * Important gotcha: although you can extend this class and write a new @BeforeClass method, which
 * will be executed after any @BeforeClass methods in this class, you *must* give the method a new
 * name - otherwise it seems to silently not run.
 */
public abstract class TestClassWithDatabase {

  /**
   * Blank properties for submitting to the DominionCVRExportParser instance.
   */
  protected static final Properties blank = new Properties();

  /**
   * Properties derived from test.properties. Not static because some workflows alter this.
   */
  protected Properties config = loadProperties();

  /**
   * Wiremock server for mocking the raire service.
   * (Note the default of 8080 clashes with the raire-service default, so this is different.)
   */
  protected WireMockServer wireMockRaireServer;

  /**
   * The string used to identify the configured port in test.properties.
   */
  public final static String raireMockPortNumberString = "raire_mock_port";

  /**
   * Container for the mock-up database.
   */
  protected PostgreSQLContainer<?> postgres = createTestContainer(config);

  /**
   * Database session.
   */
  protected Session session;

  public final static List<Choice> boulderMayoralCandidates = List.of(
      new Choice("Aaron Brockett", "", false, false),
      new Choice("Nicole Speer", "", false, false),
      new Choice("Bob Yates", "", false, false),
      new Choice("Paul Tweedlie", "", false, false)
  );

  /**
   * Contest and ContestResult for Boulder Mayoral '23 example.
   */
  public final static Contest boulderMayoralContest = new Contest(boulderMayoral, new County("Boulder", 7L), ContestType.IRV.toString(),
      boulderMayoralCandidates, 4, 1, 0);
  public final static ContestResult boulderIRVContestResult = new ContestResult(boulderMayoral);

  /**
   * Contest and ContestResult for tiny IRV example.
   */
  public final static Contest tinyIRVExample = new Contest(tinyIRV, new County("Arapahoe", 3L), ContestType.IRV.toString(),
      tinyIRVCandidates, 3, 1, 0);
  public final static ContestResult tinyIRVContestResult = new ContestResult(tinyIRV);

  /**
   * Example contestresults to mock.
   */
  public final static List<ContestResult> mockedIRVContestResults = List.of(boulderIRVContestResult, tinyIRVContestResult);

  /**
   * Contest and ContestResult for tied example.
   */
  public final static Contest tiedIRVContest = new Contest( tiedIRV, new County("Ouray", 46L), ContestType.IRV.toString(),
      tinyIRVCandidates, 3, 1, 0);
  public final static ContestResult tiedIRVContestResult = new ContestResult(tiedIRV);

  /**
   * Start the postgres container with appropriate config.
   * init the contest results above (these are just generic/static test values).
   */
  @BeforeClass
  public void initDatabase() {
    initContestResults();
    postgres.start();
    // Each class that inherits from TestClassWithDatabase gets a different url for the mocked DB.
    // Everything else is the same.
    config.setProperty("hibernate.url", postgres.getJdbcUrl());
    Persistence.setProperties(config);
  }

  /**
   * Stop the postgres container.
   */
  @AfterClass
  public void afterAll() {
    postgres.stop();
  }

  /**
   * Begin a new transaction before each test method in the class is run.
   */
  @BeforeMethod
  public static void beforeTest(){
    Persistence.beginTransaction();
  }

  /**
   * Set up some example contest results for testing.
   */
  private static void initContestResults() {

    boulderIRVContestResult.setAuditReason(AuditReason.COUNTY_WIDE_CONTEST);
    boulderIRVContestResult.setBallotCount((long) bouldMayoralCount);
    boulderIRVContestResult.setWinners(Set.of("Aaron Brockett"));
    boulderIRVContestResult.addContests(Set.of(boulderMayoralContest));

    tinyIRVContestResult.setAuditReason(AuditReason.COUNTY_WIDE_CONTEST);
    tinyIRVContestResult.setBallotCount((long) tinyIRVCount);
    tinyIRVContestResult.setWinners(Set.of("Alice"));
    tinyIRVContestResult.addContests(Set.of(tinyIRVExample));

    tiedIRVContestResult.setAuditReason(AuditReason.COUNTY_WIDE_CONTEST);
    tiedIRVContestResult.setBallotCount((long) tinyIRVCount);
    tiedIRVContestResult.setWinners(Set.of(UNKNOWN_WINNER));
    tiedIRVContestResult.addContests(Set.of(tiedIRVContest));
  }

  /**
   * Rollback any changes to the (test) database after each test method is run.
   */
  @AfterMethod
  public void afterTest(){
    try {
      Persistence.rollbackTransaction();
    } catch (Exception ignored) {
    }
  }

  /**
   * Create and return a postgres test container for the purposes of testing functionality that
   * interacts with the database.
   * @return a postgres test container representing a test database.
   */
  private static PostgreSQLContainer<?> createTestContainer(Properties config) {
    return new PostgreSQLContainer<>("postgres:15-alpine")
        // None of these actually have to be the same as the real database (except its name),
        // but this makes it easy to match the setup scripts.
        .withDatabaseName("corla")
        .withUsername(config.getProperty("hibernate.user"))
        .withPassword(config.getProperty("hibernate.pass"))
        .withInitScript("SQL/corla.sql");
  }

  /**
   * Given a path to an SQL file containing an SQL script that we want to run, run the script.
   * Interacts with a test database.
   * @param initScriptPath Path to an SQL file containing the SQL script we want to run.
   */
  protected void runSQLSetupScript(String initScriptPath) {
    runSQLSetupScript(postgres, initScriptPath);
  }

  /**
   * Given a postgres container and a path to an SQL file containing an SQL script that we want to
   * run, run the script.
   * @param initScriptPath Path to an SQL file containing the SQL script we want to run.
   */
  public static void runSQLSetupScript(final PostgreSQLContainer<?> container, final String initScriptPath) {
    var containerDelegate = new JdbcDatabaseDelegate(container, "");
    ScriptUtils.runInitScript(containerDelegate, initScriptPath);
  }

  /**
   * Set up default raire server on the port defined in config.
   *
   * @param config The configuration properties.
   * @return the base url of the mocked server.
   */
  protected WireMockServer initWireMockRaireServer(Properties config) {

    final int raireMockPort = Integer.parseInt(config.getProperty(raireMockPortNumberString, ""));
    wireMockRaireServer = new WireMockServer(raireMockPort);
    wireMockRaireServer.start();

    configureFor("localhost", wireMockRaireServer.port());

    return wireMockRaireServer;
  }

}
