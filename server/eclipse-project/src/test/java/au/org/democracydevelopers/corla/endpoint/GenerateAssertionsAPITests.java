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

import au.org.democracydevelopers.corla.communication.responseFromRaire.GenerateAssertionsResponse;
import au.org.democracydevelopers.corla.util.testUtils;
import au.org.democracydevelopers.corla.workflows.Workflow;
import com.google.gson.Gson;
import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import us.freeandfair.corla.persistence.Persistence;

import java.util.*;

import static au.org.democracydevelopers.corla.endpoint.AbstractAllIrvEndpoint.RAIRE_URL;
import static au.org.democracydevelopers.corla.util.testUtils.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.testng.Assert.*;

/**
 * Test the GetAssertions endpoint via the API.
 * This runs a series of basic tests for acceptance of valid queries and rejection of invalid ones,
 * though it does not redo those from GenerateAssertionsTests, only the ones that are relevant to
 * query processing before the call to GenerateAssertions::generateAllAssertions or
 * GenerateAssertions::generateAssertionsUpdateWinners.
 */
public class GenerateAssertionsAPITests extends Workflow {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(GenerateAssertionsAPITests.class);

  /**
   * Mock response for tinyExample1 contest, which succeeded and is not worth retrying.
   */
  private final static GenerateAssertionsResponse tinyIRVResponse
      = new GenerateAssertionsResponse(tinyIRV, true, false);

  /**
   * Raire endpoint for getting assertions.
   */
  private final String raireGenerateAssertionsEndpoint = "/raire/generate-assertions";

  /**
   * GSON for json interpretation.
   */
  private final static Gson gson = new Gson();

  /**
   * Initialise mocked objects prior to the first test.
   */
  @BeforeClass
  public void initMocksAndMainAndDB() {

    Persistence.beginTransaction();
    runSQLSetupScript("SQL/co-counties.sql");

    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8888;

    // Set up the mock raire server.
    wireMockRaireServer = initWireMockRaireServer(config);
    String baseUrl = wireMockRaireServer.baseUrl();

    // Set the above-initialized URL for the RAIRE_URL property in Main.
    config.setProperty(RAIRE_URL, baseUrl);
    runMain(config, "GenerateAssertionsAPITests");

    // Load some IRV contests into the database.
    runSQLSetupScript(postgres, "SQL/corla-three-candidates-ten-votes-plus-plurality.sql");

    // Mock a proper response to the IRV TinyExample1 contest.
    stubFor(post(urlEqualTo(raireGenerateAssertionsEndpoint))
        .withRequestBody(containing(tinyIRV))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody(gson.toJson(tinyIRVResponse))));
  }

  @AfterClass
  public void closeMocks() {
    wireMockRaireServer.stop();
  }

  /**
   * Successful assertion generation for an IRV contest for which we've mocked a RAIRE response.
   * This tests both the case when the tinyIRV example is specifically requested, and also the case
   * where no contest is specified - these should produce the same result because there is only that
   * one IRV contest in the database. It also tests that an explicit time limit doesn't change that
   * result.
   */
  @Test
  void assertionGenerationSucceedsOnRaireMockedIRV() {
    testUtils.log(LOGGER, "assertionGenerationSucceedsOnRaireMockedIRV");

    SessionFilter session = doLogin("stateadmin1");

    // Request with TinyIRVExample1
    Response response = generateAssertionsCorla(session, Optional.of(tinyIRV), Optional.empty());
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    GenerateAssertionsResponse[] gr1 = response.getBody().as(GenerateAssertionsResponse[].class);
    assertEquals(gr1.length, 1);
    assertTrue(gr1[0].succeeded);

    // Request without explicit contest name - should be the same because TinyIRVExample1 is the only
    // one in the database.
    response = generateAssertionsCorla(session, Optional.empty(), Optional.empty());
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    GenerateAssertionsResponse[] gr2 = response.getBody().as(GenerateAssertionsResponse[].class);
    // Should be the same result as if we'd requested the one contest.
    assertEquals(gson.toJson(gr2[0]), gson.toJson(gr1[0]));

    // Request with a time limit. Nothing changes.
    response = generateAssertionsCorla(session, Optional.empty(), Optional.of(5.0));
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    GenerateAssertionsResponse[] gr3 = response.getBody().as(GenerateAssertionsResponse[].class);
    // Should be the same result as if we'd requested the one contest.
    assertEquals(gson.toJson(gr3[0]), gson.toJson(gr1[0]));

    logout(session, "stateadmin1");
  }

  /**
   * An error response is returned for a plurality contest.
   */
  @Test
  void assertionGenerationErrorOnPluralityExample() {
    testUtils.log(LOGGER, "assertionGenerationErrorOnPluralityExample");

    SessionFilter session = doLogin("stateadmin1");

    Response response = generateAssertionsCorla(session, Optional.of("PluralityExample1"), Optional.empty());
    assertEquals(response.getStatusCode(), HttpStatus.SC_UNPROCESSABLE_ENTITY);
    String s = response.getBody().asString();
    assertTrue(StringUtils.containsIgnoreCase(s, "Non-existent or non-IRV"));

    logout(session, "stateadmin1");

  }

  /**
   * An error response is returned for a non-existent contest.
   */
  @Test
  void assertionGenerationErrorOnNonExistentExample() {
    testUtils.log(LOGGER, "assertionGenerationErrorOnNonExistentExample");

    SessionFilter session = doLogin("stateadmin1");

    Response response = generateAssertionsCorla(session, Optional.of("badContest"), Optional.empty());
    assertEquals(response.getStatusCode(), HttpStatus.SC_UNPROCESSABLE_ENTITY);
    String s = response.getBody().asString();
    assertTrue(StringUtils.containsIgnoreCase(s, "Non-existent or non-IRV"));

    logout(session, "stateadmin1");
  }

  /**
   * An error response is returned when the contestName parameter is specified but the value is blank.
   */
  @Test
  void assertionGenerationErrorOnEmptyPresentContestParam() {
    testUtils.log(LOGGER, "assertionGenerationErrorOnEmptyPresentContestParam");

    SessionFilter session = doLogin("stateadmin1");

    Response response = generateAssertionsCorla(session, Optional.of(""), Optional.empty());
    assertEquals(response.getStatusCode(), HttpStatus.SC_NOT_FOUND);
    String s = response.getBody().asString();
    assertTrue(StringUtils.containsIgnoreCase(s, "parameter validation failed"));

    logout(session, "stateadmin1");
  }

  /**
   * An error response is returned when the timeLimitSeconds parameter is negative.
   */
  @Test
  void assertionGenerationErrorOnNegativeTimeLimit() {
    testUtils.log(LOGGER, "assertionGenerationErrorOnNegativeTimeLimit");

    SessionFilter session = doLogin("stateadmin1");

    Response response = generateAssertionsCorla(session, Optional.empty(), Optional.of(-1.0));
    assertEquals(response.getStatusCode(), HttpStatus.SC_NOT_FOUND);
    String s = response.getBody().asString();
    assertTrue(StringUtils.containsIgnoreCase(s, "parameter validation failed"));

    logout(session, "stateadmin1");
  }

  /**
   * An error response is returned when the timeLimitSeconds parameter is zero.
   */
  @Test
  void assertionGenerationErrorOnZeroTimeLimit() {
    testUtils.log(LOGGER, "assertionGenerationErrorOnZeroTimeLimit");

    SessionFilter session = doLogin("stateadmin1");

    Response response = generateAssertionsCorla(session, Optional.empty(), Optional.of(0.0));
    assertEquals(response.getStatusCode(), HttpStatus.SC_NOT_FOUND);
    String s = response.getBody().asString();
    assertTrue(StringUtils.containsIgnoreCase(s, "parameter validation failed"));

    logout(session, "stateadmin1");
  }
}