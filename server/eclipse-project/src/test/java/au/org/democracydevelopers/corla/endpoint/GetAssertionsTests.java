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

import au.org.democracydevelopers.corla.model.ContestType;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;
import au.org.democracydevelopers.corla.util.testUtils;
import org.apache.http.HttpStatus;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import us.freeandfair.corla.Main;
import us.freeandfair.corla.model.*;

import java.io.*;
import java.net.MalformedURLException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipInputStream;

import com.github.tomakehurst.wiremock.WireMockServer;

import static au.org.democracydevelopers.corla.endpoint.AbstractAllIrvEndpoint.RAIRE_URL;
import static au.org.democracydevelopers.corla.endpoint.GetAssertions.*;
import static au.org.democracydevelopers.corla.util.testUtils.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.testng.Assert.*;

/**
 * Test the GetAssertions endpoint, both CSV and JSON versions. The response is supposed to be a zip file containing
 * the assertions.
 * Includes tests that AbstractAllIrvEndpoint::getIRVContestResults returns the correct values and
 * throws the correct exceptions.
 * Includes testing of filename sanitization (from contest names).
 */
public class GetAssertionsTests extends TestClassWithDatabase {

  private static final Logger LOGGER = LogManager.getLogger(GetAssertionsTests.class);

  /**
   * Endpoint for getting assertions.
   */
  private final static String raireGetAssertionsEndpoint = "/raire/get-assertions";

  /**
   * Base url - this is set up to use the wiremock server, but could be set here to wherever you have the
   * raire-service running to test with that directly.
   */
  private static String baseUrl;

  /**
   * The Properties that will be mocked in Main, specifically for the RAIRE_URL.
   */
  private static final Properties mockProperties = new Properties();

  /**
   * The mocked contest results, which start off with just the ones from TestUtils, but to which we will add some
   * difficult contest names.
   */
  private static final List<ContestResult> mockedTrickyContestResults = new ArrayList<>(mockedIRVContestResults);

  /**
   * Database init.
   */
  @BeforeClass
  public void beforeAllThisClass() {

    // Load in the counties data, actually just for basic setup such as DoSDashboard.
    runSQLSetupScript("SQL/co-counties.sql");
  }

  /**
   * Initialise mocked objects prior to the first test.
   */
  @BeforeClass
  public void initMocks() {
    MockitoAnnotations.openMocks(this);

    // Set up the mock raire server.
    wireMockRaireServer = initWireMockRaireServer(config);
    baseUrl = wireMockRaireServer.baseUrl();

    // Mock the above-initialized URL for the RAIRE_URL property in Main.
    mockProperties.setProperty(RAIRE_URL, baseUrl);

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
   * Test data for the two valid IRV contest names.  This gives the successfully-sanitized version of
   * the contest names.
   */
  @DataProvider(name = "TwoIRVContests")
  public static String[][][] IRVContests() {
    return new String[][][]{
        {{"CityofBoulderMayoralCandidates", tinyIRV}}
    };
  }

  /**
   * Calls the getAssertions main endpoint function and checks that the right file names are present in the zip. This
   * runs two instances, one with the ordinary IRV contests ("City of Boulder Mayoral Candidates" and "TinyIRVExample1"),
   * and the other with a list of weird contest names.
   * @throws IOException or InterruptedException if something goes wrong with unzip or getting assertions.
   */
  @Test
  public void rightSanitizedFileNamesInZip() throws IOException, InterruptedException {
    testUtils.log(LOGGER, "rightSanitizedFileNamesInZip");

    // rightFileNamesInZip(new String[] {"CityofBoulderMayoralCandidates", tinyIRV}, mockedIRVContestResults);

    // Now mock some with non-word characters and check they're properly sanitized.
    // All these complicated characters are removed.
    final Contest kempseyTricky = new Contest("K&e&*mpsey-!Mayoral", new County("Alamosa", 2L),
        ContestType.IRV.toString(), boulderMayoralCandidates, 4, 1, 0);
    final ContestResult kempseyTrickyIRVContestResult = new ContestResult(kempseyTricky.name());
    kempseyTrickyIRVContestResult.setAuditReason(AuditReason.COUNTY_WIDE_CONTEST);
    kempseyTrickyIRVContestResult.setBallotCount(100000L);
    kempseyTrickyIRVContestResult.setWinners(Set.of("Bob"));
    kempseyTrickyIRVContestResult.addContests(Set.of(boulderMayoralContest));
    mockedTrickyContestResults.add(kempseyTrickyIRVContestResult);

    // Underscores are retained.
    final Contest byronTricky = new Contest(" Byron_Mayoral    ", new County("Arapahoe", 3L),
        ContestType.IRV.toString(), boulderMayoralCandidates, 4, 1, 0);
    final ContestResult byronTrickyIRVContestResult = new ContestResult(byronTricky.name());
    byronTrickyIRVContestResult.setAuditReason(AuditReason.COUNTY_WIDE_CONTEST);
    byronTrickyIRVContestResult.setBallotCount(10L);
    byronTrickyIRVContestResult.setWinners(Set.of("Alice"));
    byronTrickyIRVContestResult.addContests(Set.of(byronTricky));
    mockedTrickyContestResults.add(byronTrickyIRVContestResult);

    final String[] expectedSanitizedContestNames =  new String[] {"CityofBoulderMayoralCandidates", tinyIRV, "KempseyMayoral", "Byron_Mayoral"};

    try (MockedStatic<AbstractAllIrvEndpoint> mockIRVContestResults = Mockito.mockStatic(AbstractAllIrvEndpoint.class);
         MockedStatic<Main> mockedMain = Mockito.mockStatic(Main.class)) {

      // Mock IRV contest results
      mockIRVContestResults.when(AbstractAllIrvEndpoint::getIRVContestResults).thenReturn(mockedTrickyContestResults);

      // Mock the RAIRE_URL from main.
      mockedMain.when(Main::properties).thenReturn(mockProperties);

      for (String suffix : List.of(JSON_SUFFIX, CSV_SUFFIX)) {
        final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        final ZipOutputStream zos = new ZipOutputStream(bytesOut);
        getAssertions(zos, "", suffix);
        zos.close();

        final byte[] bytes = bytesOut.toByteArray();
        final InputStream bais = new ByteArrayInputStream(bytes);
        final ZipInputStream in = new ZipInputStream(bais);
        // Check that each entry is in the list of expectedSanitizedContestNames.
        for (int i = 0; i < expectedSanitizedContestNames.length; i++) {
          final ZipEntry entry = in.getNextEntry();
          assertNotNull(entry);
          assertTrue(Arrays.stream(expectedSanitizedContestNames)
                  .anyMatch(cn -> entry.getName().equalsIgnoreCase(cn + "_assertions." + suffix)));
        }

        // Check that there are no more entries than expectedSanitizedContestNames.
        assertNull(in.getNextEntry());
      }
    }
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
   * Checks that, when given a bad endpoint, a runtime exception is thrown, for both csv and json.
   *
   * @throws Exception always.
   */
  @Test(dataProvider = "SampleBadEndpoints", expectedExceptions = RuntimeException.class)
  public void badEndpointThrowsRuntimeException(String url, String suffix) throws Exception {
    testUtils.log(LOGGER, "badEndpointThrowsRuntimeException");
    try (MockedStatic<AbstractAllIrvEndpoint> mockIRVContestResults
             = Mockito.mockStatic(AbstractAllIrvEndpoint.class);
         MockedStatic<Main> mockedMain = Mockito.mockStatic(Main.class)) {
      mockIRVContestResults.when(AbstractAllIrvEndpoint::getIRVContestResults)
          .thenReturn(mockedIRVContestResults);

      final Properties badURLProperties = new Properties();
      badURLProperties.setProperty(RAIRE_URL, url);
      mockedMain.when(Main::properties).thenReturn(badURLProperties);

      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      ZipOutputStream zos = new ZipOutputStream(bytesOut);
      getAssertions(zos, "", suffix);
      zos.close();
    }
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

  /**
   * Checks that, when given a bad url, an appropriate url-parsing exception is thrown, for both csv and json.
   *
   * @throws Exception always.
   */
  @Test(dataProvider = "SampleBadUrls", expectedExceptions = MalformedURLException.class)
  public void badUrlThrowsUrlException(String url, String suffix) throws Exception {
    testUtils.log(LOGGER, "badUrlThrowsUrlException");
    try (MockedStatic<AbstractAllIrvEndpoint> mockIRVContestResults
             = Mockito.mockStatic(AbstractAllIrvEndpoint.class);
         MockedStatic<Main> mockedMain = Mockito.mockStatic(Main.class)) {

      mockIRVContestResults.when(AbstractAllIrvEndpoint::getIRVContestResults)
          .thenReturn(mockedIRVContestResults);

      final Properties badURLProperties = new Properties();
      badURLProperties.setProperty(RAIRE_URL, url);
      mockedMain.when(Main::properties).thenReturn(badURLProperties);

      ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
      ZipOutputStream zos = new ZipOutputStream(bytesOut);
      getAssertions(zos, "", suffix);
      zos.close();
    }
  }
}