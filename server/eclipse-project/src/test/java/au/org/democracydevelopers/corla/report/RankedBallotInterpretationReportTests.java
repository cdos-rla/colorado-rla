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

package au.org.democracydevelopers.corla.report;

import au.org.democracydevelopers.corla.util.TestClassWithDatabase;
import au.org.democracydevelopers.corla.util.testUtils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import us.freeandfair.corla.csv.DominionCVRExportParser;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.testng.annotations.*;
import us.freeandfair.corla.query.ExportQueries;

import javax.transaction.Transactional;

import static au.org.democracydevelopers.corla.util.testUtils.*;
import static org.testng.Assert.*;

import static us.freeandfair.corla.query.CountyQueries.fromString;

/**
 * Test that a correct ranked vote interpretation report is produced, for a variety of IRV-related
 * parsing examples, including:
 * ThreeCandidatesTenVotes - a constructed test file with all-valid IRV votes.
 * ThreeCandidatesTenInvalidVotes - a constructed test with invalid IRV ballots to check they are accepted and properly
 *   interpreted.
 * An equivalent test using real data from Boulder '23 is in DominionCVRExportParserTests.
 * Test csvs are a subset of those in DominionCVRExportParserTests.
 */
public class RankedBallotInterpretationReportTests extends TestClassWithDatabase {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER
      = LogManager.getLogger(RankedBallotInterpretationReportTests.class);

  @BeforeClass
  public void beforeAllThisClass() {
    runSQLSetupScript("SQL/co-counties.sql");
  }

  /**
   * Simple test of successful parsing of a tiny IRV test example, ThreeCandidatesTenVotes.csv.
   * Tests that all the metadata and all the votes are correct. There are no invalid ballots, so the
   * csv has no data.
   * @throws IOException never.
   */
  @Test
  @Transactional
  public void parseGuideToRaireExample3Succeeds() throws IOException {
    testUtils.log(LOGGER, "parseGuideToRaireExample3Succeeds");

    // Parse the file.
    final Path path = Paths.get(TINY_CSV_PATH + "GuideToRAIREExample3.csv");
    final Reader reader = Files.newBufferedReader(path);
    final DominionCVRExportParser parser = new DominionCVRExportParser(reader, fromString("Saguache"), blank, true);
    assertTrue(parser.parse().success);

    // Make the ranked_ballot_interpretation report.
    final Map<String, String> files = ExportQueries.sqlFiles();
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final String q = files.get("ranked_ballot_interpretation");
    ExportQueries.csvOut(q, os);

    // There should be no data, only headers, because all the IRV votes are valid.
    assertEquals(os.toString(), "county,contest,record_type,cvr_number,imprinted_id,raw_vote,valid_interpretation\n");
  }

  /**
   * Test of successful parsing of a file with valid headers but invalid IRV votes - it includes
   * duplicate candidates and skipped or repeated ranks.
   * This checks for correct reports of valid interpretation of all invalid votes.
   * @throws IOException if there are file I/O issues.
   */
  @Test
  @Transactional
  public void parseThreeCandidatesTenInvalidVotesSucceeds() throws IOException {
    testUtils.log(LOGGER, "parseThreeCandidatesTenInvalidVotesSucceeds");
    final Path path = Paths.get(TINY_CSV_PATH + "ThreeCandidatesTenInvalidVotes.csv");
    final Reader reader = Files.newBufferedReader(path);

    final DominionCVRExportParser parser = new DominionCVRExportParser(reader, fromString("Gilpin"), blank, true);
    assertTrue(parser.parse().success);

    // Make the ranked_ballot_interpretation report.
    final Map<String, String> files = ExportQueries.sqlFiles();
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final String q = files.get("ranked_ballot_interpretation");
    ExportQueries.csvOut(q, os);

    // There should be headers and 10 records matching the valid interpretations of all those invalid votes.
    final String cvr = os.toString();
    assertTrue(StringUtils.contains(cvr, "county,contest,record_type,cvr_number,imprinted_id,raw_vote,valid_interpretation\n"));

    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,1,1-1-1,\"\"\"Alice(1)\"\",\"\"Alice(2)\"\",\"\"Bob(2)\"\",\"\"Chuan(3)\"\"\",\"\"\"Alice\"\",\"\"Bob\"\",\"\"Chuan\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,2,1-1-2,\"\"\"Alice(1)\"\",\"\"Bob(2)\"\",\"\"Alice(3)\"\",\"\"Chuan(3)\"\"\",\"\"\"Alice\"\",\"\"Bob\"\",\"\"Chuan\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,3,1-1-3,\"\"\"Alice(1)\"\",\"\"Alice(2)\"\",\"\"Bob(2)\"\",\"\"Alice(3)\"\",\"\"Chuan(3)\"\"\",\"\"\"Alice\"\",\"\"Bob\"\",\"\"Chuan\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,4,1-1-4,\"\"\"Alice(1)\"\",\"\"Alice(2)\"\",\"\"Bob(2)\"\",\"\"Bob(3)\"\",\"\"Chuan(3)\"\"\",\"\"\"Alice\"\",\"\"Bob\"\",\"\"Chuan\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,5,1-1-5,\"\"\"Alice(1)\"\",\"\"Bob(2)\"\",\"\"Chuan(2)\"\",\"\"Bob(3)\"\"\",\"\"\"Alice\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,6,1-1-6,\"\"\"Alice(1)\"\",\"\"Alice(2)\"\",\"\"Bob(2)\"\",\"\"Chuan(2)\"\",\"\"Bob(3)\"\"\",\"\"\"Alice\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,7,1-1-7,\"\"\"Alice(1)\"\",\"\"Chuan(3)\"\"\",\"\"\"Alice\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,8,1-1-8,\"\"\"Alice(1)\"\",\"\"Alice(2)\"\",\"\"Bob(3)\"\"\",\"\"\"Alice\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,9,1-1-9,\"\"\"Alice(1)\"\",\"\"Chuan(1)\"\",\"\"Alice(2)\"\",\"\"Bob(3)\"\"\","));
    assertTrue(StringUtils.contains(cvr,
        "Gilpin,TinyInvalidExample1,UPLOADED,10,1-1-10,\"\"\"Bob(1)\"\",\"\"Chuan(1)\"\",\"\"Alice(2)\"\",\"\"Bob(3)\"\"\","));
  }
}
