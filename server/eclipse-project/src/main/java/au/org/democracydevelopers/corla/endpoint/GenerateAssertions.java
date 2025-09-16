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

package au.org.democracydevelopers.corla.endpoint;

import au.org.democracydevelopers.corla.communication.requestToRaire.GenerateAssertionsRequest;
import au.org.democracydevelopers.corla.communication.responseFromRaire.GenerateAssertionsResponse;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import spark.Request;
import spark.Response;
import us.freeandfair.corla.Main;
import us.freeandfair.corla.asm.ASMUtilities;
import us.freeandfair.corla.asm.DoSDashboardASM;
import us.freeandfair.corla.model.Choice;
import us.freeandfair.corla.model.ContestResult;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.ComparisonAuditQueries;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

import static us.freeandfair.corla.asm.ASMState.DoSDashboardState.PARTIAL_AUDIT_INFO_SET;

/**
 * The Generate Assertions endpoint. Takes optional query parameters specifying a contest and time
 * limit.
 * If no parameters are specified, it generates assertions for all IRV contests with a default time
 * limit. If the database contains no IRV contests, it does nothing and returns an empty list.
 * If an IRV contest is specified, it generates assertions only for that contest. If a plurality contest
 * is specified, it throws an error.
 * If a time limit is specified, it uses that instead of the default.
 * Returns a list of all responses, which are the contest name together with the winner (if one was
 * returned) or an error (if there was one).
 * For example, hitting /generate-assertions?contestName=Boulder Mayoral,timeLimitSeconds=5
 * will, if successful, produce a singleton list with a GenerateAssertionsResponseWithErrors
 * containing "Boulder Mayoral" and the winner.
 * Hitting /generate-assertions with no parameters will produce a list of
 * GenerateAssertionsResponseWithErrors, one for each IRV contest, each containing a nonempty winner
 * or a nonempty error.
 */
public class GenerateAssertions extends AbstractAllIrvEndpoint {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(GenerateAssertions.class);

  /**
   * RAIRE service generate assertions endpoint.
   */
  protected static final String RAIRE_ENDPOINT = "/raire/generate-assertions";

  /**
   * Query specifier for time limit.
   */
  private static final String TIME_LIMIT = "timeLimitSeconds";

  /**
   * Query specifier for contest name.
   */
  public static final String CONTEST_NAME = "contestName";

  /**
   * Default time limit for assertion generation.
   */
  private static final String DEFAULT_TIME_LIMIT = "5";

  /**
   * Default winner to be used in the case where winner is unknown.
   */
  public static final String UNKNOWN_WINNER = "Unknown";

  /**
   * {@inheritDoc}
   */
  @Override
  public EndpointType endpointType() {
    return EndpointType.GET;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String endpointName() {
    return "/generate-assertions";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String endpointBody(final Request the_request, final Response the_response) {
    final String prefix = "[endpointBody]";
    LOGGER.debug(String.format("%s %s.", prefix, "Received Generate Assertions request"));

    // Check that the request is valid; abort if not.
    if (!validateParameters(the_request)) {
      final String msg = "Blank contest name or invalid time limit in Generate Assertions request";
      LOGGER.debug(String.format("%s %s %s.", prefix, msg, the_request.body()));
      badDataContents(the_response, msg);
    }

    // Check that the state is OK for assertion generation; abort if not.
    if (!assertionGenerationAllowed(the_request)) {
      final String msg = "Assertion generation not allowed in current state.";
      LOGGER.error(String.format("%s %s.", prefix, msg));
      illegalTransition(the_response, msg);
    }

    final List<GenerateAssertionsResponse> responseData;

    final String raireUrl = Main.properties().getProperty(RAIRE_URL, "") + RAIRE_ENDPOINT;

    // If a time limit was specified, use that, otherwise use the default.
    final double timeLimitSeconds
        = Double.parseDouble(the_request.queryParamOrDefault(TIME_LIMIT, DEFAULT_TIME_LIMIT));

    // If a contest was requested in the query parameter, generate assertions only for that contest.
    // Otherwise, generate them for all IRV contests.
    final String contestName = the_request.queryParamOrDefault(CONTEST_NAME, "");

    // Get all the IRV contest results.
    final List<ContestResult> IRVContestResults = getIRVContestResults();

    // Try to do the work.
    try {
      if (contestName.isBlank()) {
        // No contest was requested - generate for all.

        responseData = generateAllAssertions(IRVContestResults, timeLimitSeconds, raireUrl);
      } else {
        // Generate for the specific contest requested.

        responseData = List.of(generateAssertionsUpdateWinners(IRVContestResults, contestName,
            timeLimitSeconds, raireUrl));
      }

      the_response.header("Content-Type", "application/json");

      okJSON(the_response, Main.GSON.toJson(responseData));

      LOGGER.debug(String.format("%s %s.", prefix, "Completed Generate Assertions request"));

    } catch (IllegalArgumentException e) {
      LOGGER.debug(String.format("%s %s.", prefix, "Bad Generate Assertions request"));
      badDataContents(the_response, e.getMessage());
    } catch (RuntimeException e) {
      LOGGER.debug(String.format("%s %s.", prefix, "Error processing Generate Assertions request"));
      serverError(the_response, e.getMessage());
    }

    // The only change is updating the winners in the IRV ContestResults.
    Persistence.flush();

    return my_endpoint_result.get();
  }

  /**
   * Do the actual work of getting the assertions.
   * - Gather all the IRVContestResults
   * - For each IRV contest, make a request to the raire-service get-assertions endpoint of the right format type
   * - Collate all the results into a list.
   *
   * @param IRVContestResults the collection of all IRV ContestResults.
   * @param timeLimitSeconds  the time limit for raire assertion generation, per contest.
   * @param raireUrl          the url where the raire-service is running.
   */
  protected List<GenerateAssertionsResponse> generateAllAssertions(final List<ContestResult> IRVContestResults,
                                                               final double timeLimitSeconds, final String raireUrl) {
    final String prefix = "[generateAllAssertions]";
    LOGGER.debug(String.format("%s %s.", prefix, "Generating assertions for all IRV contests"));

    // Iterate through all IRV Contests, sending a request to the raire-service for each one's assertions
    final List<GenerateAssertionsResponse> responseData = IRVContestResults.stream().map(
        r -> generateAssertionsUpdateWinners(IRVContestResults, r.getContestName(), timeLimitSeconds, raireUrl)
        ).toList();

    LOGGER.debug(String.format("%s %s.", prefix, "Completed assertion generation for all IRV contests"));
    return responseData;
  }

  /**
   * The main work of this endpoint - sends the appropriate request for a single contest, and
   * updates stored data with the result. There are two expected kinds of responses from raire:
   * - a success response with a winner, or
   * - an error response with a reason, e.g. TIED_WINNERS or NO_VOTES_PRESENT.
   * These are expected to happen occasionally because of the data, and are saved in the
   * GenerateAssertionsSummary table.
   * Other errors, such as a BAD_REQUEST response or a failure to parse raire's response, indicate
   * programming or configuration errors. These are logged and an exception is thrown.
   *
   * @param IRVContestResults The list of all ContestResults for IRV contests. Note that these do
   *                          not have the correct winners or losers for IRV.
   * @param contestName       The name of the contest.
   * @param timeLimitSeconds  The time limit allowed for raire to compute the assertions (not
   *                          counting time taken to retrieve vote data from the database).
   * @param raireUrl          The url of the raire service.
   * @return The GenerateAssertionsResponseWithErrors, which usually contains a
   * winner but may instead be UNKNOWN_WINNER and an error message.
   */
  protected GenerateAssertionsResponse generateAssertionsUpdateWinners(final List<ContestResult> IRVContestResults,
                   final String contestName, final double timeLimitSeconds, final String raireUrl) {
    final String prefix = "[generateAssertionsUpdateWinners]";
    LOGGER.debug(String.format("%s %s %s.", prefix, "Generating assertions for contest ", contestName));

    try {
      final ContestResult cr = IRVContestResults.stream()
          .filter(r -> r.getContestName().equalsIgnoreCase(contestName)).findAny().orElseThrow(
              () -> new NoSuchElementException("Non-existent or non-IRV contest in Generate Assertions request for contest:"));
      final List<String> candidates = cr.getContests().stream().findAny().orElseThrow(
              () -> new NoSuchElementException("No contests in Contest Result for contest:"))
          .choices().stream().map(Choice::name).toList();

      // Make the request.
      final GenerateAssertionsRequest generateAssertionsRequest = new GenerateAssertionsRequest(
          cr.getContestName(),
          cr.getBallotCount().intValue(),
          timeLimitSeconds,
          candidates
      );

      // Send it to the RAIRE service.
      // Throws URISyntaxException or MalformedURLException if the raireUrl is invalid.
      final HttpResponse<String> raireResponse = httpClient.send(HttpRequest.newBuilder()
              .uri(new URL(raireUrl).toURI())
              .header("content-type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(Main.GSON.toJson(generateAssertionsRequest)))
              .build(),
          HttpResponse.BodyHandlers.ofString()
      );
      LOGGER.debug(String.format("%s %s %s.", prefix,
          "Sent Assertion Request to Raire service for contest", contestName));

      // Interpret the response.
      final int statusCode = raireResponse.statusCode();

      if (statusCode == HttpURLConnection.HTTP_OK && raireResponse.body() != null) {

        // OK response, which may indicate either that assertion generation succeeded, or that it
        // failed and raire generated a useful error. Return raire's response.
        final GenerateAssertionsResponse responseFromRaire
            = gson.fromJson(raireResponse.body(), GenerateAssertionsResponse.class);

        LOGGER.debug(String.format("%s %s %s %s.", prefix,
            responseFromRaire.succeeded ? "Success" : "Failure",
            "response for raire assertion generation for contest", contestName));
        return responseFromRaire;

      } else {
        // Something went wrong with the connection, e.g. 404 or a Bad Request. Cannot continue.
        final String msg = "Connection failure with Raire service. Http code "
            + statusCode + ". Check the configuration of Raire service url.";
        LOGGER.error(String.format("%s %s", prefix, msg));
        throw new RuntimeException(msg);
      }
    } catch (final URISyntaxException | MalformedURLException e) {
      // The raire service url is malformed, probably a config error.
      final String msg = "Bad configuration of Raire service url: " + raireUrl + ". Check your config file.";
      LOGGER.error(String.format("%s %s %s", prefix, msg, e.getMessage()));
      throw new RuntimeException(msg);
    } catch (final NoSuchElementException e) {
      // This happens if the contest name is not in the IRVContestResults, or if the Contest Result
      // does not actually contain any contests (the latter should never happen).
      LOGGER.error(String.format("%s %s %s.", prefix, e.getMessage(), contestName));
      throw new IllegalArgumentException(e.getMessage() + " " + contestName);
    } catch (JsonSyntaxException e) {
      // This happens if the raire service returns something that isn't interpretable as json,
      // so gson throws a syntax exception when trying to parse raireResponse.
      final String msg = "Error interpreting Raire response for contest ";
      final String errorMsg = String.format("%s %s %s.%s", prefix, msg, contestName, e.getMessage()==null?"":" "+e.getMessage());
      LOGGER.error(errorMsg);
      throw new RuntimeException(errorMsg);
    } catch (final UnsupportedEncodingException e) {
      // This really shouldn't happen, but would happen if the effort to make the
      // generateAssertionsRequest as json failed.
      final String msg = "Error generating request to Raire for contest ";
      final String errorMsg = String.format("%s %s %s.%s", prefix, msg, contestName, e.getMessage()==null?"":" "+e.getMessage());
      LOGGER.error(errorMsg);
      throw new RuntimeException(errorMsg);
    } catch (final NullPointerException e) {
      // This also shouldn't happen - it would indicate an unexpected problem such as the httpClient
      // returning a null response.
      final String msg = "Error requesting or receiving assertions for contest";
      final String errorMsg = String.format("%s %s %s.%s", prefix, msg, contestName, e.getMessage()==null?"":" "+e.getMessage());
      LOGGER.error(errorMsg);
      throw new RuntimeException(errorMsg);
    } catch (final IOException e) {
      // Generic error that can be thrown by the httpClient if the connection attempt fails.
      final String msg = "I/O error during generate assertions attempt for contest";
      final String errorMsg = String.format("%s %s %s.%s", prefix, msg, contestName, e.getMessage()==null?"":" "+e.getMessage());
      LOGGER.error(errorMsg);
      throw new RuntimeException(errorMsg);
    } catch (final InterruptedException e) {
      // The http connection to RAIRE was interrupted.
      final String msg = "Connection to RAIRE interrupted for contest";
      final String errorMsg = String.format("%s %s %s.%s", prefix, msg, contestName, e.getMessage()==null?"":" "+e.getMessage());
      LOGGER.error(errorMsg);
      throw new RuntimeException(errorMsg);
    }
  }

  /**
   * Validates the parameters of a request. For this endpoint, the query parameters are optional,
   * but if the contest is present it should be non-null, and if a time limit is present it should
   * be positive.
   * @param the_request the request sent to the endpoint.
   * @return true if the request's query parameters are valid.
   */
  @Override
  protected boolean validateParameters(final Request the_request) {

    // An absent time limit is OK, but a present, negative or unparseable one is invalid.
    try {
      final String timeLimit = the_request.queryParams(TIME_LIMIT);
      if (timeLimit != null && Double.parseDouble(timeLimit) <= 0) {
        return false;
      }
    } catch (final NumberFormatException e) {
      return false;
    }

    // An absent contest name is OK, but a present null or empty one is invalid.
    final String contestName = the_request.queryParams(CONTEST_NAME);
    return contestName == null || !contestName.isEmpty();
  }

  /**
   * Assertion generation is allowed to commence if we are in the DOS_INITIAL_STATE or the
   * PARTIAL_AUDIT_INFO_SET state, otherwise it is not.
   * This function also checks that there are no ComparisonAudits in the database, though this should
   * always be true in the required states.
   * @param the_request the endpoint request.
   * @return true if we are in the right state and there are no ComparisonAudits in the database.
   */
  private boolean assertionGenerationAllowed(final Request the_request) {
    final String prefix = "[assertionGenerationAllowed]";
    final String errorMsg = "Blocked assertion generation when requested for contest";

    final DoSDashboardASM dashboardASM = ASMUtilities.asmFor(DoSDashboardASM.class, DoSDashboardASM.IDENTITY);

    // Check that we're in either the initial state or the PARTIAL_AUDIT_INFO_SET state.
    final boolean allowedState
        = (dashboardASM.isInInitialState() || dashboardASM.currentState().equals(PARTIAL_AUDIT_INFO_SET));
    if(!allowedState) {
      LOGGER.debug(String.format("%s %s %s from illegal state %s.", prefix, errorMsg,
          the_request.queryParams(CONTEST_NAME), dashboardASM.currentState()));
    }

    final boolean noComparisonAudits = ComparisonAuditQueries.count() == 0;

    // Check that there are no ComparisonAudits in the database (which should not happen given the state).
    if(!noComparisonAudits) {
      LOGGER.debug(String.format("%s %s %s %s with %d ComparisonAudits in the database.", prefix, errorMsg,
          the_request.queryParams(CONTEST_NAME), dashboardASM.currentState().toString(),
          ComparisonAuditQueries.count()));
    }

    return allowedState && noComparisonAudits;
  }
}
