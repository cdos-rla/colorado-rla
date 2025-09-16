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

import static au.org.democracydevelopers.corla.util.testUtils.*;

import au.org.democracydevelopers.corla.util.TestClassWithDatabase;
import au.org.democracydevelopers.corla.util.testUtils;

import com.google.gson.Gson;
import org.apache.http.HttpStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import us.freeandfair.corla.model.*;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

/**
 * Test the GetAssertions endpoint, both CSV and JSON versions. The response is supposed to be a zip file containing
 * the assertions.
 * Includes tests that AbstractAllIrvEndpoint::getIRVContestResults returns the correct values and
 * throws the correct exceptions.
 */
public class GenerateAssertionsTests extends TestClassWithDatabase {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(GenerateAssertionsTests.class);

  /**
   * Mock response for Boulder Mayoral '23
   */
  private final static GenerateAssertionsResponse boulderResponse
      = new GenerateAssertionsResponse(boulderMayoral, true, false);

  /**
   * Mock response for tinyExample1 contest
   */
  private final static GenerateAssertionsResponse tinyIRVResponse
      = new GenerateAssertionsResponse(tinyIRV, true, false);

  /**
   * Mock response for tiedIRV contest
   */
  private final static GenerateAssertionsResponse tiedIRVResponse
      = new GenerateAssertionsResponse(tiedIRV, false, false);

  /**
   * Request for Boulder Mayoral '23
   */
  private final static GenerateAssertionsRequest boulderRequest
      = new GenerateAssertionsRequest(boulderMayoral, bouldMayoralCount, 5,
          boulderMayoralCandidates.stream().map(Choice::name).toList());

  /**
   * Request for tinyExample1 contest
   */
  private final static GenerateAssertionsRequest tinyIRVRequest
      = new GenerateAssertionsRequest(tinyIRV, tinyIRVCount, 5,
      tinyIRVCandidates.stream().map(Choice::name).toList());

  /**
   * Request for tiedIRV contest. This has the same candidates and ballot count as tinyIRV.
   */
  private final static GenerateAssertionsRequest tiedIRVRequest
      = new GenerateAssertionsRequest(tiedIRV, tinyIRVCount, 5,
      tinyIRVCandidates.stream().map(Choice::name).toList());

  /**
   * Raire endpoint for getting assertions.
   */
  private final static String raireGenerateAssertionsEndpoint = "/raire/generate-assertions";

  /**
   * Base url - this is set up to use the wiremock server, but could be set here to wherever you have the
   * raire-service running to test with that directly.
   */
  private static String baseUrl;

  /**
   * Bad url, for testing we deal appropriately with the resulting error.
   */
  private final String badEndpoint = "/badUrl";

  /**
   * An endpoint that produces nonsense responses, i.e. valid json but not a valid
   * GenerateAssertionsResponse, for testing that we deal appropriately with the resulting error.
   */
  private final String nonsenseResponseEndpoint = "/raire/nonsense-generating-url";

  /**
   * An endpoint that produces nonsense/uninterpretable responses, i.e. not valid json, for testing
   * that we deal appropriately with the resulting error.
   */
  private final String invalidResponseEndpoint = "/raire/invalid-json-generating-url";

  /**
   * GSON for json interpretation.
   */
  private final static Gson gson = new Gson();

  /**
   * Initialise mocked objects prior to the first test.
   */
  @BeforeClass
  public void initMocks() {

    // Set up the mock raire server.
    wireMockRaireServer = initWireMockRaireServer(config);
    baseUrl = wireMockRaireServer.baseUrl();

    final String badUrl = baseUrl + badEndpoint;

    // Mock a proper response to the Boulder Mayoral '23 contest.
    stubFor(post(urlEqualTo(raireGenerateAssertionsEndpoint))
        .withRequestBody(equalToJson(gson.toJson(boulderRequest)))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(boulderResponse))));
    // Mock a proper response to the IRV TinyExample1 contest.
    stubFor(post(urlEqualTo(raireGenerateAssertionsEndpoint))
                .withRequestBody(equalToJson(gson.toJson(tinyIRVRequest)))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(tinyIRVResponse))));
    // Mock failed, don't redo response to the tiedIRV contest.
    stubFor(post(urlEqualTo(raireGenerateAssertionsEndpoint))
        .withRequestBody(equalToJson(gson.toJson(tiedIRVRequest)))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(tiedIRVResponse))));
    // Mock a 404 for badUrl.
    stubFor(post(urlEqualTo(badUrl))
        .withRequestBody(equalToJson(gson.toJson(tinyIRVRequest)))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_NOT_FOUND)));
    // Mock an OK but invalid response from the nonsense endpoint.
    // This is just a list of candidates, which should not make sense as a response.
    stubFor(post(urlEqualTo(nonsenseResponseEndpoint))
        .withRequestBody(equalToJson(gson.toJson(tinyIRVRequest)))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(tinyIRVCandidates))));
    // Mock an OK response with invalid json.
    stubFor(post(urlEqualTo(invalidResponseEndpoint))
        .withRequestBody(equalToJson(gson.toJson(tinyIRVRequest)))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody("This isn't valid json")));

  }

  @AfterClass
  public void closeMocks() {
    wireMockRaireServer.stop();
  }

  /**
   * Calls the single-contest version of the endpoint for the boulder Mayor '23 example, checks
   * that generation succeeded and does not recommend retry.
   */
  @Test
  public void rightBoulderIRVWinner() {
    testUtils.log(LOGGER, "rightBoulderIRVWinner");

    GenerateAssertions endpoint = new GenerateAssertions();
    GenerateAssertionsResponse result = endpoint.generateAssertionsUpdateWinners(
        mockedIRVContestResults, boulderRequest.contestName, boulderRequest.timeLimitSeconds,
        baseUrl + raireGenerateAssertionsEndpoint);

    assertEquals(result.contestName, boulderMayoral);
    assertTrue(result.succeeded);
    assertFalse(result.retry);
  }

  /**
   * Calls the single-contest version of the endpoint for the tied contests, checks that
   * assertion generation fails and does not recommend retry.
   */
  @Test
  public void tiedWinnersFailsNoRetry() {
  testUtils.log(LOGGER, "tiedWinnersFailsNoRetry");

  GenerateAssertions endpoint = new GenerateAssertions();
  GenerateAssertionsResponse result = endpoint.generateAssertionsUpdateWinners(
      List.of(tiedIRVContestResult), tiedIRV, tiedIRVRequest.timeLimitSeconds,
      baseUrl + raireGenerateAssertionsEndpoint);

  assertEquals(result.contestName, tiedIRV);
  assertFalse(result.succeeded);
  assertFalse(result.retry);
  }

  /**
   * Calls the generateAssertions main endpoint function for the two example contests (Boulder and TinyIRV).
   * and checks that both succeed and do not recommend retry.
   */
  @Test
  public void successAsExpected() {
    testUtils.log(LOGGER, "successAsExpected");

    GenerateAssertions endpoint = new GenerateAssertions();
    List<GenerateAssertionsResponse> results
        = endpoint.generateAllAssertions(mockedIRVContestResults, boulderRequest.timeLimitSeconds,
        baseUrl + raireGenerateAssertionsEndpoint);

    assertEquals(results.size(), 2);
    assertEquals(results.get(0).contestName, boulderMayoral);
    assertTrue(results.get(0).succeeded);
    assertFalse(results.get(0).retry);
    assertEquals(results.get(1).contestName, tinyIRV);
    assertTrue(results.get(1).succeeded);
    assertFalse(results.get(1).retry);
  }

  /**
   * A nonexistent contest causes an appropriate error message.
   * (The requested contest does not appear in the mockedIRVContestResults.)
   */
  @Test(expectedExceptions = IllegalArgumentException.class,
      expectedExceptionsMessageRegExp = ".*Non-existent or non-IRV contest.*")
    public void nonExistentContestThrowsRuntimeException() {
    testUtils.log(LOGGER, "nonExistentContestThrowsRuntimeException");

    GenerateAssertions endpoint = new GenerateAssertions();
    endpoint.generateAssertionsUpdateWinners(mockedIRVContestResults, nonExistentContest, 5,
      baseUrl + raireGenerateAssertionsEndpoint);
  }

  /**
   * When raire sends an uninterpretable response, an appropriate error message appears.
   * This tests a response that is not valid json.
   * The (?s) workaround is added because otherwise the regexp was failing to match, but tbh I'm not
   * sure why that's necessary.
   */
  @Test(expectedExceptions = RuntimeException.class,
      expectedExceptionsMessageRegExp = "(?s).*Error interpreting Raire response for contest.*")
  public void uninterpretableRaireResponseThrowsRuntimeException() {
    testUtils.log(LOGGER, "uninterpretableRaireResponseThrowsRuntimeException");

    GenerateAssertions endpoint = new GenerateAssertions();
    endpoint.generateAssertionsUpdateWinners(mockedIRVContestResults, tinyIRV, 5,
        baseUrl + invalidResponseEndpoint);
  }

  /**
   * When raire sends an unexpected response, an appropriate error message appears.
   * This tests a response that is valid json, but not the json we were expecting.
   */
  @Test(expectedExceptions = RuntimeException.class,
      expectedExceptionsMessageRegExp = "(?s).*Error interpreting Raire response for contest.*")
  public void unexpectedRaireResponseThrowsRuntimeException() {
    testUtils.log(LOGGER, "unexpectedRaireResponseThrowsRuntimeException");

    GenerateAssertions endpoint = new GenerateAssertions();
    endpoint.generateAssertionsUpdateWinners(mockedIRVContestResults, tinyIRV, 5,
        baseUrl + nonsenseResponseEndpoint);
  }

  /**
   * When given a bad endpoint, a runtime exception is thrown with an appropriate error message.
   */
  @Test(expectedExceptions = RuntimeException.class,
      expectedExceptionsMessageRegExp = ".*Connection failure.*Raire service url.*")
  public void badEndpointThrowsRuntimeException() {
    testUtils.log(LOGGER, "badEndpointThrowsRuntimeException");

    GenerateAssertions endpoint = new GenerateAssertions();
    String badUrl = baseUrl + badEndpoint;
    endpoint.generateAllAssertions(mockedIRVContestResults, 5, badUrl);
  }

  /**
   * When given a bad url, an appropriate url-parsing error appears.
   */
  @Test(expectedExceptions = RuntimeException.class,
      expectedExceptionsMessageRegExp = ".*Bad configuration of Raire service url.*")
  public void badUrlThrowsUrlException() {
    testUtils.log(LOGGER, "badUrlThrowsUrlException");

    String url = "completelyNotAUrl" + "/badUrl";

    GenerateAssertions endpoint = new GenerateAssertions();
    endpoint.generateAllAssertions(mockedIRVContestResults, 5, url);
  }

  /**
   * When given a bad url, an appropriate url-parsing error appears.
   */
  @Test(expectedExceptions = RuntimeException.class,
      expectedExceptionsMessageRegExp = ".*Bad configuration of Raire service url.*")
  public void nonExistentContestThrowsException() {
    testUtils.log(LOGGER, "badUrlThrowsUrlException");

    GenerateAssertions endpoint = new GenerateAssertions();
    String url = "completelyNotAUrl" + "/badUrl";
    endpoint.generateAllAssertions(mockedIRVContestResults, 5, url);
  }


}