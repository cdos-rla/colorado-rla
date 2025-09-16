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

import au.org.democracydevelopers.corla.util.testUtils;
import au.org.democracydevelopers.corla.workflows.Workflow;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import io.restassured.filter.session.SessionFilter;
import io.restassured.response.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import us.freeandfair.corla.persistence.Persistence;

import java.util.Optional;

import static au.org.democracydevelopers.corla.endpoint.AbstractAllIrvEndpoint.RAIRE_URL;
import static au.org.democracydevelopers.corla.endpoint.GetAssertions.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.testng.Assert.*;

/**
 * Test the GetAssertions endpoint API, both CSV and JSON versions. This mostly tests that it hits
 * the right endpoint (json or csv) given various (possibly bad) query strings.
 * It does not re-test bad config or the presence of the right contests in the zip, because they are
 * tested in GetAssertionsTests.java.
 */
public class GetAssertionsAPITests extends Workflow {

  private static final Logger LOGGER = LogManager.getLogger(GetAssertionsAPITests.class);

  /**
   * Endpoint for getting assertions.
   */
  private final static String raireGetAssertionsEndpoint = "/raire/get-assertions";

  /**
   * Wiremock server for mocking the raire service.
   * (Note the default of 8080 clashes with the raire-service default, so this is different.)
   */
  WireMockServer wireMockRaireServer;

  /**
   * Base url - this is set up to use the wiremock server, but could be set here to wherever you have the
   * raire-service running to test with that directly.
   */
  private static String baseUrl;

  /**
   * Initialise mocked raire service prior to the first test.
   */
  @BeforeClass
  public void initMocksAndMain() {

    Persistence.beginTransaction();
    runSQLSetupScript("SQL/co-counties.sql");

    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8888;

    // Set up the mock raire server.
    wireMockRaireServer = initWireMockRaireServer(config);
    baseUrl = wireMockRaireServer.baseUrl();

    // Set the above-initialized URL for the RAIRE_URL property in Main; run main.
    config.setProperty(RAIRE_URL, baseUrl);
    runMain(config, "GetAssertionsAPITests");

    // Load some IRV contests into the database.
    runSQLSetupScript(postgres, "SQL/corla-three-candidates-ten-votes-plus-plurality.sql");

    // Mock a proper response for both json and csv.
    stubFor(post(urlEqualTo(raireGetAssertionsEndpoint + "-" + CSV_SUFFIX))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/octet-stream")
            .withBody("Test csv")));
    stubFor(post(urlEqualTo(raireGetAssertionsEndpoint + "-" + JSON_SUFFIX))
        .willReturn(aResponse()
            .withStatus(HttpStatus.SC_OK)
            .withHeader("Content-Type", "application/json")
            .withBody("Test json")));
  }

  @AfterClass
  public void closeMocks() {
    wireMockRaireServer.stop();
  }

  /**
   * Test that the endpoint interprets the format query parameter correctly, selecting csv if a
   * proper query string "format=csv" is present, and json otherwise.
   */
  @Test
  public void hitsRightEndpointJsonOrCSV() {
    testUtils.log(LOGGER, "hitsRightEndpointJsonOrCSV");

    SessionFilter session = doLogin("stateadmin1");

    // Hit the endpoint with no format string - defaults to JSON.
    Response response = getAssertionsCorla(session, Optional.empty());
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    // Check that we hit the right endpoint, which is mocked to return a string with the requested format.
    assertTrue(StringUtils.containsIgnoreCase(response.getBody().asString(), "json"));

    // Hit the endpoint with format specifying json - should hit json.
    response = getAssertionsCorla(session, Optional.of("?" + FORMAT_PARAM + "=JSON"));
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    // Check that we hit the right endpoint, which is mocked to return a string with the requested format.
    assertTrue(StringUtils.containsIgnoreCase(response.getBody().asString(), "json"));

    // Hit the endpoint with format specifying json, lowercase - should hit csv.
    response = getAssertionsCorla(session, Optional.of("?" + FORMAT_PARAM + "=json"));
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    // Check that we hit the right endpoint, which is mocked to return a string with the requested format.
    assertTrue(StringUtils.containsIgnoreCase(response.getBody().asString(), "json"));

    // Hit the endpoint with format specifying csv - should hit csv.
    response = getAssertionsCorla(session, Optional.of("?" + FORMAT_PARAM + "=CSV"));
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    // Check that we hit the right endpoint, which is mocked to return a string with the requested format.
    assertTrue(StringUtils.containsIgnoreCase(response.getBody().asString(), "csv"));

    // Hit the endpoint with format specifying csv, lowercase - should hit csv.
    response = getAssertionsCorla(session, Optional.of("?" + FORMAT_PARAM + "=csv"));
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    // Check that we hit the right endpoint, which is mocked to return a string with the requested format.
    assertTrue(StringUtils.containsIgnoreCase(response.getBody().asString(), "csv"));

    // Hit the endpoint with bad format string - defaults to JSON.
    response = getAssertionsCorla(session, Optional.of("?" + "Bad format string"));
    assertEquals(response.getStatusCode(), HttpStatus.SC_OK);
    // Check that we hit the right endpoint, which is mocked to return a string with the requested format.
    assertTrue(StringUtils.containsIgnoreCase(response.getBody().asString(), "json"));
  }

  /**
   * Good urls with bad endpoints.
   */
  @DataProvider(name = "SampleBadEndpoints")
  public static String[][] SampleBadEndpoints() {
    return new String[][]{
        {baseUrl + "/badUrl", CSV_SUFFIX},
        {baseUrl + "/badUrl", JSON_SUFFIX}
    };
  }

  /**
   * Bad urls.
   */
  @DataProvider(name = "SampleBadUrls")
  public static String[][] SampleBadUrls() {
    return new String[][]{
        {"completelyNotAUrl" + "/badUrl", CSV_SUFFIX},
        {"completelyNotAUrl" + "/badUrl", JSON_SUFFIX}
    };
  }
}