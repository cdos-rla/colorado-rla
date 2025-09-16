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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import us.freeandfair.corla.persistence.Persistence;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

import static au.org.democracydevelopers.corla.endpoint.AbstractAllIrvEndpoint.RAIRE_URL;
import static au.org.democracydevelopers.corla.util.testUtils.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.testng.Assert.assertEquals;

/**
 * Test that the assertion generation request is accepted and blocked in the right
 * circumstances. Assertion generation should succeed during the initial state and the PARTIAL_AUDIT_INFO_SET
 * state, and otherwise fail. This test doesn't exhaustively test _all_ possible states, but it tests
 * that the transition from allowed to blocked is made correctly.
 */
public class GenerateAssertionsValidStateTest extends Workflow {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(GenerateAssertionsValidStateTest.class);

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
  public void initMocksAndMain() {

    Persistence.beginTransaction();
    runSQLSetupScript("SQL/co-counties.sql");

    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8888;

    // Set up the mock raire server.
    wireMockRaireServer = initWireMockRaireServer(config);
    String baseUrl = wireMockRaireServer.baseUrl();

    // Set the above-initialized URL for the RAIRE_URL property in Main.
    // This config is used in runMainAndInitializeDBIfNeeded.
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
   * Simple test that the assertion generation request is made to raire when in
   * ASM initial state and ASM PARTIAL_AUDIT_INFO_SET states, and not in later states.
   * This does not exhaustively test all later states, just the two after the allowed states.
   */
  @Test
  @Transactional
  void assertionGenerationBlockedWhenInWrongASMState() {
    testUtils.log(LOGGER, "assertionGenerationBlockedWhenInWrongASMState");

    SessionFilter session = doLogin("stateadmin1");

    // First test: check that the GenerateAssertions endpoint works when in the initial state
    // (which is set up initially in the database).

    // There should be no error when the ASM is in the initial state.
    Response response = generateAssertionsCorla(session, Optional.empty(), Optional.empty());
    int status = response.getStatusCode();
    assertEquals(status, HttpStatus.SC_OK);

    // Now set all the audit info - transition to PARTIAL_AUDIT_INFO_SET
    updateAuditInfo("src/test/resources/CSVs/AdamsAndAlamosa/adams-and-alamosa-canonical-list.csv",
        BigDecimal.valueOf(0.03));

    // There should be still be no error when the ASM is in the PARTIAL_AUDIT_INFO_SET state.
    response = generateAssertionsCorla(session, Optional.empty(), Optional.empty());
    status = response.getStatusCode();
    assertEquals(status, HttpStatus.SC_OK);

    // Now transition to COMPLETE_AUDIT_INFO_SET and other, subsequent, states, in which
    // assertion generation is expected to throw an error. Check that it does.
    setSeed("123412341234123412341234");

    // There should now be an error.
    response = generateAssertionsCorla(session, Optional.empty(), Optional.empty());
    status = response.getStatusCode();
    assertEquals(status, HttpStatus.SC_FORBIDDEN);

    // Start the audit round, try again; should be another error.
    response = generateAssertionsCorla(session, Optional.empty(), Optional.empty());
    status = response.getStatusCode();
    assertEquals(status, HttpStatus.SC_FORBIDDEN);

  }
}
