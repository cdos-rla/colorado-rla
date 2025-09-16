package au.org.democracydevelopers.corla.query;

import au.org.democracydevelopers.corla.communication.responseFromRaire.RaireServiceErrors;
import au.org.democracydevelopers.corla.model.GenerateAssertionsSummary;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;
import au.org.democracydevelopers.corla.util.testUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;

import java.util.Optional;

import static au.org.democracydevelopers.corla.communication.responseFromRaire.RaireServiceErrors.RaireErrorCodes.TIED_WINNERS;
import static au.org.democracydevelopers.corla.endpoint.GenerateAssertions.UNKNOWN_WINNER;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

/**
 * Basic tests for proper functioning of GenerateAssertionsSummaryQueries.
 * Test data is loaded from simple-assertions and (most usefully) summaries-generation-errors.sql.
 * This tests successful retrieval of:
 * - a summary of successful assertion generation,
 * - the winner from a summary of successful assertion generation,
 * - a warning (TIME_OUT_TRIMMING_ASSERTIONS) from a summary of successful assertion generation,
 * - the correct errors from a collection of summaries of failed assertion generation.
 */
public class GenerateAssertionsSummaryQueriesTests extends TestClassWithDatabase {

  private static final Logger LOGGER = LogManager.getLogger(GenerateAssertionsSummaryQueriesTests.class);

  /**
   * Start the test container and establish persistence properties before the first test.
   */
  @BeforeClass
  public void beforeAllThisClass() {
    runSQLSetupScript("SQL/co-counties.sql");
    runSQLSetupScript("SQL/simple-assertions.sql");
    runSQLSetupScript("SQL/summaries-generation-errors.sql");
  }

  /**
   * Correct retrieval of summary of successful assertion generation.
   */
  @Test
  public void retrieveSuccessSummary() {
    testUtils.log(LOGGER, "retrieveSuccessSummary");

    Optional<GenerateAssertionsSummary> summary = GenerateAssertionsSummaryQueries.matching("One NEB Assertion Contest");

    assertTrue(summary.isPresent());
    assertEquals(summary.get().getWinner(), "Alice");
    assertEquals(summary.get().getError(), "");
    assertEquals(summary.get().getWarning(), "");
    assertEquals(summary.get().getMessage(), "");
  }

  /**
   * Correct retrieval of winner from successful assertion generation.
   */
  @Test
  public void retrieveSuccessSummaryGetWinner() {
    testUtils.log(LOGGER, "retrieveSuccessSummaryGetWinner");

    final String winner = GenerateAssertionsSummaryQueries.matchingWinner("One NEB Assertion Contest");
    assertEquals(winner, "Alice");
  }

  /**
   * Correct retrieval of winner from successful assertion generation with a warning.
   */
  @Test
  public void retrieveSuccessSummaryWithWarning() {
    testUtils.log(LOGGER, "retrieveSuccessSummaryWithWarning");

    final Optional<GenerateAssertionsSummary> summary
        = GenerateAssertionsSummaryQueries.matching("Timeout trimming assertions Contest");

    assertTrue(summary.isPresent());
    assertEquals(summary.get().getWinner(), "Bob");
    assertEquals(summary.get().getError(), "");
    assertEquals(summary.get().getWarning(),
        RaireServiceErrors.RaireErrorCodes.TIMEOUT_TRIMMING_ASSERTIONS.toString());
    assertEquals(summary.get().getMessage(), "");
  }

  /**
   * Correct retrieval of summary of failed assertion generation, in this case a Tied winners error.
   * Note that the raw result has a blank winner.
   */
  @Test
  public void retrieveFailureSummary() {
    testUtils.log(LOGGER, "retrieveFailureSummary");

    final Optional<GenerateAssertionsSummary> summary
        = GenerateAssertionsSummaryQueries.matching("Tied winners Contest");

    assertTrue(summary.isPresent());
    assertEquals(summary.get().getWinner(), "");
    assertEquals(summary.get().getError(), TIED_WINNERS.toString());
    assertEquals(summary.get().getWarning(), "");
    assertEquals(summary.get().getMessage(), "Tied winners: Alice, Bob");
  }

  /**
   * Direct retrieval of UNKNOWN_WINNER from failed assertion generation.
   * The matchingWinner function inserts UNKNOWN_WINNER when the winner is blank.
   */
  @Test
  public void retrieveFailureSummaryGetUnknownWinner() {
    testUtils.log(LOGGER, "retrieveFailureSummaryGetUnknownWinner");

    final String winner = GenerateAssertionsSummaryQueries.matchingWinner("Tied winners Contest");
    assertEquals(winner, UNKNOWN_WINNER);
  }

  /**
   * Try to retrieve a summary that isn't there.
   */
  @Test
  public void nonExistentSummaryIsEmpty() {
    testUtils.log(LOGGER, "nonExistentSummaryIsEmpty");

    final Optional<GenerateAssertionsSummary> summary
        = GenerateAssertionsSummaryQueries.matching("No Such Contest");

    assertTrue(summary.isEmpty());
  }

  /**
   * The winner of a summary that isn't there is UNKNOWN_WINNER.
   */
  @Test
  public void nonExistentSummaryIsUnknownWinner() {
    testUtils.log(LOGGER, "nonExistentSummaryIsUnknownWinner");

    final String winner = GenerateAssertionsSummaryQueries.matchingWinner("No Such Contest");

    assertEquals(winner, UNKNOWN_WINNER);
  }
}
