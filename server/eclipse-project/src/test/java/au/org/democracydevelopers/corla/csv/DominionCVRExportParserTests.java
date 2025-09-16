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

package au.org.democracydevelopers.corla.csv;

import au.org.democracydevelopers.corla.model.ContestType;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;
import au.org.democracydevelopers.corla.util.testUtils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import us.freeandfair.corla.csv.DominionCVRExportParser;
import us.freeandfair.corla.model.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import org.testng.annotations.*;
import us.freeandfair.corla.query.ContestQueries;
import us.freeandfair.corla.query.ExportQueries;

import javax.transaction.Transactional;

import static au.org.democracydevelopers.corla.util.testUtils.*;
import static org.testng.Assert.*;

import static us.freeandfair.corla.query.CastVoteRecordQueries.getMatching;
import static us.freeandfair.corla.query.ContestQueries.forCounties;
import static us.freeandfair.corla.query.CountyQueries.fromString;

/**
 * Test a variety of IRV-related parsing issues for CVRs, including:
 * - some basic small test cases that can be verified by eye,
 * - a constructed test with invalid IRV ballots to check they are accepted and properly
 *   interpreted.
 * - a real example from Boulder '23, with a mix of IRV and plurality contests
 * - some examples with invalid headers, to ensure they are rejected (though most of these are
 *   tested in IRVHeadersParserTests.java and are not repeated here),
 *   plus a bad plurality "Vote For= " with a non-integer.
 *   This example includes a test that IRV ballot interpretation report contains the right valid
 *   interpretation, for a small selection of known invalid votes. (This really belongs logically
 *   in RankedBallotInterpretationReportTests, but is included here so the Boulder data only has to
 *   be loaded once.)
 * - an examples to test the broader class of Write In strings.
 * - some basic small test cases with STV votes, to ensure that STV contests are properly dropped.
 */
public class DominionCVRExportParserTests extends TestClassWithDatabase {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(DominionCVRExportParserTests.class);

  /**
   * Error message to match.
   */
  private static final String badNumsRegexp = "Unexpected or uninterpretable numbers in header:.*";

  /**
   * Blank properties for submitting to the DominionCVRExportParser instance.
   */
  private static final Properties blank = new Properties();

  /**
   * Some expected votes.
   */
  private static final List<String> ABC = List.of("Alice","Bob","Chuan");
  private static final List<String> ACB = List.of("Alice","Chuan","Bob");
  private static final List<String> BAC = List.of("Bob","Alice","Chuan");
  private static final List<String> CAB = List.of("Chuan","Alice","Bob");

  /**
   * The 10 expected votes in the TinyIRV files.
   * There are 10 votes:
   * 3 Alice, Bob, Chuan
   * 3 Alice, Chuan, Bob
   * 1 Bob, Alice, Chuan
   * 3 Chuan, Alice, Bob
   */
  private static final List<List<String>> expectedChoices = List.of(
      ABC,ABC,ABC,
      ACB,ACB,ACB,
      BAC,
      CAB, CAB, CAB);

  @BeforeClass
  public void beforeAllThisClass() {
    runSQLSetupScript("SQL/co-counties.sql");
  }

  /**
   * Simple test of successful parsing of a tiny IRV test example, ThreeCandidatesTenVotes.csv.
   * Tests that all the metadata and all the votes are correct.
   * @throws IOException never.
   */
  @Test
  @Transactional
  public void parseThreeCandidatesTenVotesSucceeds() throws IOException {
    testUtils.log(LOGGER, "parseThreeCandidatesTenVotesSucceeds");
    final Path path = Paths.get(TINY_CSV_PATH + "ThreeCandidatesTenVotes.csv");
    final Reader reader = Files.newBufferedReader(path);

    final DominionCVRExportParser parser = new DominionCVRExportParser(reader,
        fromString("Saguache"), blank, true);
    assertTrue(parser.parse().success);

    // There should be one contest, the one we just read in.
    final List<Contest> contests = forCounties(Set.of(fromString("Saguache")));
    assertEquals(1, contests.size());
    final Contest contest = contests.get(0);

    // Check basic data
    assertEquals(contest.name(), "TinyExample1");
    assertEquals(contest.description(), ContestType.IRV.toString());
    assertEquals(contest.choices().stream().map(Choice::name).collect(Collectors.toList()), ABC);

    // Votes allowed should be 3 (because there are 3 ranks), whereas winners=1 always for IRV.
    assertEquals(contest.votesAllowed().intValue(), 3);
    assertEquals(contest.winnersAllowed().intValue(), 1);

    // Check that the 10 expected votes are there.
    final List<CVRContestInfo> cvrs = getMatching(fromString("Saguache").id(),
        CastVoteRecord.RecordType.UPLOADED).map(cvr -> cvr.contestInfoForContest(contest)).toList();
    assertEquals(10, cvrs.size());
    for(int i=0 ; i < expectedChoices.size() ; i++) {
      assertEquals(cvrs.get(i).choices(), expectedChoices.get(i));
    }
  }

  /**
   * Second simple test of successful parsing of a tiny IRV test example, GuideToRAIREExample3.csv.
   * Test that all the metadata are correct, then spot-check some of the votes.
   * @throws IOException never.
   */
  @Test
  @Transactional
  public void parseGuideToRaireExample3() throws IOException {
    testUtils.log(LOGGER, "parseGuideToRaireExample3");
    final Path path = Paths.get(TINY_CSV_PATH + "GuideToRAIREExample3.csv");
    final Reader reader = Files.newBufferedReader(path);

    final DominionCVRExportParser parser = new DominionCVRExportParser(reader,
        fromString("Montezuma"), blank, true);
    assertTrue(parser.parse().success);

    // There should be one contest, the one we just read in.
    final List<Contest> contests = forCounties(Set.of(fromString("Montezuma")));
    assertEquals(1, contests.size());
    final Contest contest = contests.get(0);

    // Check basic data
    assertEquals(contest.name(), "Example3");
    assertEquals(contest.description(), ContestType.IRV.toString());
    assertEquals(contest.choices().stream().map(Choice::name).collect(Collectors.toList()),
        List.of("A", "B", "C", "D"));

    // Votes allowed should be 3 (because there are 3 ranks), whereas winners=1 always for IRV.
    assertEquals(contest.votesAllowed().intValue(), 4);
    assertEquals(contest.winnersAllowed().intValue(), 1);

    // There are 225 votes. Spot-check some of them.
    final List<CVRContestInfo> cvrs = getMatching(fromString("Montezuma").id(),
        CastVoteRecord.RecordType.UPLOADED).map(cvr -> cvr.contestInfoForContest(contest)).toList();
    assertEquals(225, cvrs.size());
    assertEquals(List.of("C", "D"), cvrs.get(0).choices());
    assertEquals(List.of("C", "D"), cvrs.get(44).choices());
    assertEquals(List.of("C", "B", "D"), cvrs.get(45).choices());
    assertEquals(List.of("C", "B", "D"), cvrs.get(84).choices());
    assertEquals(List.of("A", "B", "C", "D"), cvrs.get(85).choices());
    assertEquals(List.of("A", "B", "C", "D"), cvrs.get(184).choices());
    assertEquals(List.of("B", "D", "C"), cvrs.get(185).choices());
    assertEquals(List.of("B", "D", "C"), cvrs.get(185).choices());
    assertEquals(List.of("B", "D", "C"), cvrs.get(224).choices());
  }

  /**
   * Test of successful parsing of a file with valid headers but invalid IRV votes - it includes
   * duplicate candidates and skipped or repeated ranks.
   * This checks for correct metadata and correct valid interpretation of all votes.
   * @throws IOException if there are file I/O issues.
   */
  @Test
  @Transactional
  public void parseThreeCandidatesTenInvalidVotesSucceeds() throws IOException {
    testUtils.log(LOGGER, "parseThreeCandidatesTenInvalidVotesSucceeds");
    final Path path = Paths.get(TINY_CSV_PATH + "ThreeCandidatesTenInvalidVotes.csv");
    final Reader reader = Files.newBufferedReader(path);

    final DominionCVRExportParser parser = new DominionCVRExportParser(reader,
        fromString("Gilpin"), blank, true);
    assertTrue(parser.parse().success);

    // There should be one contest, the one we just read in.
    final List<Contest> contests = forCounties(Set.of(fromString("Gilpin")));
    assertEquals(1, contests.size());
    final Contest contest = contests.get(0);

    // Check basic data
    assertEquals(contest.name(), "TinyInvalidExample1");
    assertEquals(contest.description(), ContestType.IRV.toString());
    assertEquals(contest.choices().stream().map(Choice::name).collect(Collectors.toList()),
        List.of("Alice", "Bob", "Chuan"));

    // Votes allowed should be 3 (because there are 3 ranks), whereas winners=1 always for IRV.
    assertEquals(contest.votesAllowed().intValue(), 3);
    assertEquals(contest.winnersAllowed().intValue(), 1);

    // There are 10 votes, with respective valid interpretations as below:
    final List<CVRContestInfo> cvrs = getMatching(fromString("Gilpin").id(),
        CastVoteRecord.RecordType.UPLOADED).map(cvr -> cvr.contestInfoForContest(contest)).toList();
    assertEquals(10, cvrs.size());

    // Raw: "Alice(1),Alice(2),Bob(2),Chuan(3)
    assertEquals(List.of("Alice", "Bob", "Chuan"), cvrs.get(0).choices());
    // Raw: "Alice(1),Bob(2),Alice(3),Chuan(3)
    assertEquals(List.of("Alice", "Bob", "Chuan"), cvrs.get(1).choices());
    // Raw: "Alice(1),Alice(2),Bob(2),Alice(3),Chuan(3)
    assertEquals(List.of("Alice", "Bob", "Chuan"), cvrs.get(2).choices());
    // Raw: "Alice(1),Alice(2),Bob(2),Bob(3),Chuan(3)
    assertEquals(List.of("Alice", "Bob", "Chuan"), cvrs.get(3).choices());
    // Raw: "Alice(1),Bob(2),Chuan(2),Bob(3)
    assertEquals(List.of("Alice"), cvrs.get(4).choices());
    // Raw:  "Alice(1),Alice(2),Bob(2),Chuan(2),Bob(3)
    assertEquals(List.of("Alice"), cvrs.get(5).choices());
    // Raw:  "Alice(1),Chuan(3)"
    assertEquals(List.of("Alice"), cvrs.get(6).choices());
    // Raw:  "Alice(1),Alice(2), Bob(3)"
    assertEquals(List.of("Alice"), cvrs.get(7).choices());
    // Raw: "Alice(1),Chuan(1),Alice(2),Bob(3)
    assertTrue(cvrs.get(8).choices().isEmpty());
    // Raw: "Bob(1),Chuan(1),Alice(2),Bob(3)
    assertTrue(cvrs.get(9).choices().isEmpty());
  }

  /**
   * Test of successful parsing of data from Boulder '23, which contains a mix of IRV and plurality
   * contests. Check all their metadata.
   * Check correct parsing of the first vote.
   * Check proper reporting of valid interpretation of some invalid IRV votes.
   */
  @Test
  @Transactional
  public void parseBoulder23Succeeds() throws IOException {
    testUtils.log(LOGGER, "parseBoulder23Succeeds");
    final Path path = Paths.get(BOULDER_CSV_PATH + "Boulder-2023-Coordinated-CVR-Redactions-removed.csv");
    final Reader reader = Files.newBufferedReader(path);

    final DominionCVRExportParser parser = new DominionCVRExportParser(reader, fromString("Boulder"),
            blank, true);
    assertTrue(parser.parse().success);

    // There should be 38 contests. Check their metadata.
    final List<Contest> contests = forCounties(Set.of(fromString("Boulder")));
    assertEquals(38, contests.size());

    final Contest boulderMayoral = contests.get(0);
    assertEquals(boulderMayoral.name(), "City of Boulder Mayoral Candidates");
    assertEquals((int) boulderMayoral.votesAllowed(), 4);
    assertEquals((int) boulderMayoral.winnersAllowed(), 1);

    final Contest boulderCouncil = contests.get(1);
    assertEquals(boulderCouncil.name(), "City of Boulder Council Candidates");
    assertEquals((int) boulderCouncil.votesAllowed(), 4);
    assertEquals((int) boulderCouncil.winnersAllowed(), 4);


    final Contest lafayetteCouncil = contests.get(2);
    assertEquals(lafayetteCouncil.name(), "City of Lafayette City Council Candidates");
    assertEquals((int) lafayetteCouncil.votesAllowed(), 4);
    assertEquals((int) lafayetteCouncil.winnersAllowed(), 4);

    final Contest longmontMayor = contests.get(3);
    assertEquals(longmontMayor.name(),"City of Longmont - Mayor");
    assertEquals((int) longmontMayor.votesAllowed(), 1);
    assertEquals((int) longmontMayor.winnersAllowed(), 1);

    final Contest longmontCouncillorAtLarge = contests.get(4);
    assertEquals(longmontCouncillorAtLarge.name(),
        "City of Longmont - City Council Member At-Large");
    assertEquals((int) longmontCouncillorAtLarge.votesAllowed(), 1);
    assertEquals((int) longmontCouncillorAtLarge.winnersAllowed(), 1);

    final Contest longmontCouncillorWard1 = contests.get(5);
    assertEquals(longmontCouncillorWard1.name(),"City of Longmont - Council Member Ward 1");
    assertEquals((int) longmontCouncillorWard1.votesAllowed(), 1);
    assertEquals((int) longmontCouncillorWard1.winnersAllowed(), 1);

    final Contest longmontCouncillorWard3 = contests.get(6);
    assertEquals(longmontCouncillorWard3.name(),"City of Longmont - Council Member Ward 3");
    assertEquals((int) longmontCouncillorWard3.votesAllowed(), 1);
    assertEquals((int) longmontCouncillorWard3.winnersAllowed(), 1);

    final Contest louisvilleMayor = contests.get(7);
    assertEquals(louisvilleMayor.name(),"City of Louisville Mayor At-Large (4 Year Term)");
    assertEquals((int) louisvilleMayor.votesAllowed(), 1);
    assertEquals((int) louisvilleMayor.winnersAllowed(), 1);

    final Contest louisvilleCouncilWard1 = contests.get(8);
    assertEquals(louisvilleCouncilWard1.name(),
        "City of Louisville City Council Ward 1 (4-year term)");
    assertEquals((int) louisvilleCouncilWard1.votesAllowed(), 1);
    assertEquals((int) louisvilleCouncilWard1.winnersAllowed(), 1);

    final Contest louisvilleCouncilWard2 = contests.get(9);
    assertEquals(louisvilleCouncilWard2.name(),
        "City of Louisville City Council Ward 2 (4-year term)");
    assertEquals((int) louisvilleCouncilWard2.votesAllowed(), 1);
    assertEquals((int) louisvilleCouncilWard2.winnersAllowed(), 1);

    final Contest louisvilleCouncilWard3 = contests.get(10);
    assertEquals( louisvilleCouncilWard3.name(),
        "City of Louisville City Council Ward 3");
    assertEquals((int) louisvilleCouncilWard3.votesAllowed(), 2);
    assertEquals((int) louisvilleCouncilWard3.winnersAllowed(), 2);

    final Contest boulderValleySchoolDirectorA = contests.get(11);
    assertEquals( boulderValleySchoolDirectorA.name(),
        "Boulder Valley School District RE-2 Director District A (4 Years)");
    assertEquals((int) boulderValleySchoolDirectorA.votesAllowed(), 1);
    assertEquals((int) boulderValleySchoolDirectorA.winnersAllowed(), 1);

    final Contest boulderValleySchoolDirectorC = contests.get(12);
    assertEquals(boulderValleySchoolDirectorC.name(),
        "Boulder Valley School District RE-2 Director District C (4 Years)");
    assertEquals((int) boulderValleySchoolDirectorC.votesAllowed(), 1);
    assertEquals((int) boulderValleySchoolDirectorC.winnersAllowed(), 1);

    final Contest boulderValleySchoolDirectorD = contests.get(13);
    assertEquals(boulderValleySchoolDirectorD.name(),
        "Boulder Valley School District RE-2 Director District D (4 Years)");
    assertEquals((int) boulderValleySchoolDirectorD.votesAllowed(), 1);
    assertEquals((int) boulderValleySchoolDirectorD.winnersAllowed(), 1);

    final Contest boulderValleySchoolDirectorG = contests.get(14);
    assertEquals(boulderValleySchoolDirectorG.name(),
        "Boulder Valley School District RE-2 Director District G (4 Years)");
    assertEquals((int) boulderValleySchoolDirectorG.votesAllowed(), 1);
    assertEquals((int) boulderValleySchoolDirectorG.winnersAllowed(), 1);

    final Contest estesParkSchoolDirectorAtLarge = contests.get(15);
    assertEquals(estesParkSchoolDirectorAtLarge.name(),
        "Estes Park School District R-3 School Board Director At Large (4 Year)");
    assertEquals((int) estesParkSchoolDirectorAtLarge.votesAllowed(), 2);
    assertEquals((int) estesParkSchoolDirectorAtLarge.winnersAllowed(), 2);

    final Contest thompsonSchoolDirectorA = contests.get(16);
    assertEquals(thompsonSchoolDirectorA.name(),
        "Thompson R2-J School District Board of Education Director District A (4 Year Term)");
    assertEquals((int) thompsonSchoolDirectorA.votesAllowed(), 1);
    assertEquals((int) thompsonSchoolDirectorA.winnersAllowed(), 1);

    final Contest thompsonSchoolDirectorC = contests.get(17);
    assertEquals(thompsonSchoolDirectorC.name(),
        "Thompson R2-J School District Board of Education Director District C (4 Year Term)");
    assertEquals((int) thompsonSchoolDirectorC.votesAllowed(), 1);
    assertEquals((int) thompsonSchoolDirectorC.winnersAllowed(), 1);

    final Contest thompsonSchoolDirectorD = contests.get(18);
    assertEquals( thompsonSchoolDirectorD.name(),
        "Thompson R2-J School District Board of Education Director District D (4 Year Term)");
    assertEquals((int) thompsonSchoolDirectorD.votesAllowed(), 1);
    assertEquals((int) thompsonSchoolDirectorD.winnersAllowed(), 1);

    final Contest thompsonSchoolDirectorG = contests.get(19);
    assertEquals(thompsonSchoolDirectorG.name(),
        "Thompson R2-J School District Board of Education Director District G (4 Year Term)");
    assertEquals((int) thompsonSchoolDirectorG.votesAllowed(), 1);
    assertEquals((int) thompsonSchoolDirectorG.winnersAllowed(), 1);

    final Contest cityOfLongmontJudge = contests.get(20);
    assertEquals(cityOfLongmontJudge.name(), "City of Longmont Municipal Court Judge - Frick");
    assertEquals((int) cityOfLongmontJudge.votesAllowed(), 1);
    assertEquals((int) cityOfLongmontJudge.winnersAllowed(), 1);

    final Contest propHH = contests.get(21);
    assertEquals(propHH.name(),"Proposition HH (Statutory)");
    assertEquals((int) propHH.votesAllowed(), 1);
    assertEquals((int) propHH.winnersAllowed(), 1);

    final Contest propII = contests.get(22);
    assertEquals(propII.name(),"Proposition II (Statutory)");
    assertEquals((int) propII.votesAllowed(), 1);
    assertEquals((int) propII.winnersAllowed(), 1);

    final Contest boulder1A = contests.get(23);
    assertEquals(boulder1A.name(),"Boulder County Ballot Issue 1A");
    assertEquals((int) boulder1A.votesAllowed(), 1);
    assertEquals((int) boulder1A.winnersAllowed(), 1);

    final Contest boulder1B = contests.get(24);
    assertEquals(boulder1B.name(),"Boulder County Ballot Issue 1B");
    assertEquals((int) boulder1B.votesAllowed(), 1);
    assertEquals((int) boulder1B.winnersAllowed(), 1);

    final Contest boulder2A = contests.get(25);
    assertEquals(boulder2A.name(), "City of Boulder Ballot Issue 2A");
    assertEquals((int) boulder2A.votesAllowed(), 1);
    assertEquals((int) boulder2A.winnersAllowed(), 1);

    final Contest boulder2B = contests.get(26);
    assertEquals(boulder2B.name(), "City of Boulder Ballot Question 2B");
    assertEquals((int) boulder2B.votesAllowed(), 1);
    assertEquals((int) boulder2B.winnersAllowed(), 1);

    final Contest boulder302 = contests.get(27);
    assertEquals(boulder302.name(), "City of Boulder Ballot Question 302");
    assertEquals((int) boulder302.votesAllowed(), 1);
    assertEquals((int) boulder302.winnersAllowed(), 1);

    final Contest erie3A = contests.get(28);
    assertEquals(erie3A.name(), "Town of Erie Ballot Question 3A");
    assertEquals((int) erie3A.votesAllowed(), 1);
    assertEquals((int) erie3A.winnersAllowed(), 1);

    final Contest erie3B = contests.get(29);
    assertEquals(erie3B.name(), "Town of Erie Ballot Question 3B");
    assertEquals((int) erie3B.votesAllowed(), 1);
    assertEquals((int) erie3B.winnersAllowed(), 1);

    final Contest longmont3C = contests.get(30);
    assertEquals(longmont3C.name(), "City of Longmont Ballot Issue 3C");
    assertEquals((int) longmont3C.votesAllowed(), 1);
    assertEquals((int) longmont3C.winnersAllowed(), 1);

    final Contest longmont3D = contests.get(31);
    assertEquals(longmont3D.name(), "City of Longmont Ballot Issue 3D");
    assertEquals((int) longmont3D.votesAllowed(), 1);
    assertEquals((int) longmont3D.winnersAllowed(), 1);

    final Contest longmont3E = contests.get(32);
    assertEquals(longmont3E.name(), "City of Longmont Ballot Issue 3E");
    assertEquals((int) longmont3E.votesAllowed(), 1);
    assertEquals((int) longmont3E.winnersAllowed(), 1);

    final Contest louisville2C = contests.get(33);
    assertEquals(louisville2C.name(), "City of Louisville Ballot Issue 2C");
    assertEquals((int) louisville2C.votesAllowed(), 1);
    assertEquals((int) louisville2C.winnersAllowed(), 1);

    final Contest superior301 = contests.get(34);
    assertEquals(superior301.name(), "Town of Superior Ballot Question 301");
    assertEquals((int) superior301.votesAllowed(), 1);
    assertEquals((int) superior301.winnersAllowed(), 1);

    final Contest superiorHomeRule = contests.get(35);
    assertEquals(superiorHomeRule.name(), "Town of Superior - Home Rule Charter Commission");
    assertEquals((int) superiorHomeRule.votesAllowed(), 9);
    assertEquals((int) superiorHomeRule.winnersAllowed(), 9);

    final Contest nederlandEcoPass6A = contests.get(36);
    assertEquals(nederlandEcoPass6A.name(),
        "Nederland Eco Pass Public Improvement District Ballot Issue 6A");
    assertEquals((int) nederlandEcoPass6A.votesAllowed(), 1);
    assertEquals((int) nederlandEcoPass6A.winnersAllowed(), 1);

    final Contest northMetroFire7A = contests.get(37);
    assertEquals(northMetroFire7A.name(), "North Metro Fire Rescue District Ballot Issue 7A");
    assertEquals((int) northMetroFire7A.votesAllowed(), 1);
    assertEquals((int) northMetroFire7A.winnersAllowed(), 1);

    // Check that the number of cvrs is correct. We have redacted CVRs, so the total is slightly
    // less than the actual official count of 119757.
    final List<CastVoteRecord> cvrs = getMatching(fromString("Boulder").id(),
        CastVoteRecord.RecordType.UPLOADED).toList();
    assertEquals(cvrs.size(), 118669);
    final CastVoteRecord cvr1 = cvrs.get(0);

    // Check that the first cvr was correctly parsed.
    // We expect the first cvr to have voted on 13 contests (all Boulder), as follows:
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulderMayoral)).choices(),
        List.of("Aaron Brockett", "Nicole Speer", "Bob Yates", "Paul Tweedlie"));
    assertNotNull(cvr1.contestInfoForContest(boulderCouncil));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulderCouncil)).choices(),
        List.of("Silas Atkins", "Ryan Schuchard", "Tara Winer", "Taishya Adams"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulderValleySchoolDirectorA)).choices(),
        List.of("Jason Unger"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulderValleySchoolDirectorC)).choices(),
        List.of("Alex Medler"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulderValleySchoolDirectorD)).choices(),
        List.of("Andrew Brandt"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulderValleySchoolDirectorG)).choices(),
        List.of("Jorge ChÃ¡vez"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(propHH)).choices(),
        List.of("Yes/For"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(propII)).choices(),
        List.of("Yes/For"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulder1A)).choices(),
        List.of("Yes/For"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulder1B)).choices(),
        List.of("Yes/For"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulder2A)).choices(),
        List.of("Yes/For"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulder2B)).choices(),
        List.of("Yes/For"));
    assertEquals(Objects.requireNonNull(cvr1.contestInfoForContest(boulder302)).choices(),
        List.of("No/Against"));

    // Make the ranked_ballot_interpretation report.
    final Map<String, String> files = ExportQueries.sqlFiles();
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    final String q = files.get("ranked_ballot_interpretation");
    ExportQueries.csvOut(q, os);

    // Test for the proper reporting of some known invalid votes.
    final String boulderMayoralName = "City of Boulder Mayoral Candidates";
    final String cvr = os.toString();
    assertTrue(StringUtils.contains(cvr,
        "county,contest,record_type,cvr_number,imprinted_id,raw_vote,valid_interpretation\n"));
    assertTrue(StringUtils.contains(cvr,
        "Boulder,"+boulderMayoralName+",UPLOADED,140,108-1-32,\"\"\"Bob Yates(1)\"\"," +
            "\"\"Bob Yates(2)\"\",\"\"Bob Yates(3)\"\",\"\"Bob Yates(4)\"\"\",\"\"\"Bob Yates\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Boulder,"+boulderMayoralName+",UPLOADED,112680,108-100-48,\"\"\"Bob Yates(1)\"\"," +
            "\"\"Nicole Speer(2)\"\",\"\"Aaron Brockett(3)\"\",\"\"Bob Yates(3)\"\"," +
            "\"\"Paul Tweedlie(4)\"\"\",\"\"\"Bob Yates\"\",\"\"Nicole Speer\"\"," +
            "\"\"Aaron Brockett\"\",\"\"Paul Tweedlie\"\""));
    assertTrue(StringUtils.contains(cvr,
        "Boulder,"+boulderMayoralName+",UPLOADED,107599,101-178-114,\"\"\"Bob Yates(1)\"\"," +
            "\"\"Paul Tweedlie(1)\"\",\"\"Aaron Brockett(2)\"\",\"\"Paul Tweedlie(2)\"\"," +
            "\"\"Paul Tweedlie(3)\"\",\"\"Paul Tweedlie(4)\"\"\","));
    assertTrue(StringUtils.contains(cvr,
        "Boulder,"+boulderMayoralName+",UPLOADED,118738,101-190-124," +
            "\"\"\"Aaron Brockett(1)\"\",\"\"Bob Yates(1)\"\"\","));
  }

  /**
   * A plurality cvr with a "Vote for=2.0". This is an error.
   * Note different behaviour from earlier parsing, which simply assumed 1.
   * @throws RuntimeException always,
   */
  @Test(expectedExceptions = RuntimeException.class,
      expectedExceptionsMessageRegExp = badNumsRegexp)
  @Transactional
  public void parseBadVoteForPluralityError() throws IOException {
    testUtils.log(LOGGER, "parseBadVoteForPluralityError");
    final Path path = Paths.get(BAD_CSV_PATH + "badVoteForPlurality.csv");
    final Reader reader = Files.newBufferedReader(path);

    final DominionCVRExportParser parser = new DominionCVRExportParser(reader, fromString("Sedgwick"), blank, true);
    assertTrue(parser.parse().success);
  }

  /**
   * Test for correct interpretation of a variety of different write-in strings, including
   * "WRITEIN", "WRITE-in" etc.
   * The regexp should match any whitespace, followed by any capitalization of "write", followed
   * by -, _, space or no space, followed by any capitalization of "in", followed by any whitespace.
   */
  @Test
  @Transactional
  public void parseWriteIns() throws IOException {
    testUtils.log(LOGGER, "parseWriteIns");
    final Path path = Paths.get(WRITEIN_CSV_PATH + "WriteIns.csv");
    final Reader reader = Files.newBufferedReader(path);

    final DominionCVRExportParser parser = new DominionCVRExportParser(reader, fromString("Las Animas"), blank, true);
    assertTrue(parser.parse().success);

    // There should be seven contests, one for each example way of writing write-in:
    // "Write-in", "Write-In", "Write in", "Write_in", "writeIn", "WRITEIN", "WRITE_IN"
    final List<Contest> contests = forCounties(Set.of(fromString("Las Animas")));
    assertEquals(7, contests.size());

    for(int i=0 ; i < contests.size() ; i++) {
      Contest contest = contests.get(i);
      List<Choice> choices = contest.choices();

      // Check basic data
      assertEquals(contest.name(), String.format("Contest %d", i+1));
      assertEquals(contest.description(), ContestType.PLURALITY.toString());

      assertEquals(choices.size(), 3);
      assertFalse(choices.get(0).qualifiedWriteIn());
      assertTrue(choices.get(1).fictitious());
      assertTrue(contest.choices().get(2).qualifiedWriteIn());
    }
  }

  /**
   * Simple test of successful parsing of a tiny IRV test example, with IRV, STV, and plurality,
   * in that order.
   * Tests that all the metadata and some of the votes are correct.
   * In particular, the STV contest should be completely dropped.
   * @throws IOException never.
   */
  @Test
  @Transactional
  public void parseThreeCandidatesTenVotesPlusSTVPlusPluralitySucceedsAndDropsSTV() throws IOException {
    testUtils.log(LOGGER, "parseThreeCandidatesTenVotesPlusSTVPlusPluralitySucceedsAndDropsSTV");
    final Path path = Paths.get(TINY_CSV_PATH + "ThreeCandidatesTenVotesPlusSTVPlusPlurality.csv");

    doIRVAndSTVAndPluralityTest(path, "Cheyenne");
  }

  /**
   * Second test for successful parsing of mixed IRV, plurality, STV contests. The STV contest is
   * at the end.
   * @throws IOException never.
   */
  @Test
  @Transactional
  public void parseThreeCandidatesTenVotesPlusPluralityPlusSTVSucceedsAndDropsSTV() throws IOException {
    testUtils.log(LOGGER, "parseThreeCandidatesTenVotesPlusPluralityPlusSTVSucceedsAndDropsSTV");
    final Path path = Paths.get(TINY_CSV_PATH + "ThreeCandidatesTenVotesPlusPluralityPlusSTV.csv");

    doIRVAndSTVAndPluralityTest(path, "Yuma");
  }

  /**
   * Third test for successful parsing of mixed IRV, plurality, STV contests. The STV contest is
   * at the beginning.
   * @throws IOException never.
   */
  @Test
  @Transactional
  public void parseSTVPlusThreeCandidatesTenVotesPlusPluralitySucceedsAndDropsSTV() throws IOException {
    testUtils.log(LOGGER, "parseSTVPlusThreeCandidatesTenVotesPlusPluralitySucceedsAndDropsSTV");
    final Path path = Paths.get(TINY_CSV_PATH + "STVPlusThreeCandidatesTenVotesPlusPlurality.csv");

    doIRVAndSTVAndPluralityTest(path, "La Plata");
  }

  /**
   * The parser throws an ArrayOutOfBounds exception when given a csv with no contests (because at
   * DominionCVRExportParser:870 it tries to get the 0-th element of an empty list, i.e. the list of column headers).
   * I'm not certain whether this is intended behaviour, but it's _different_ behaviour from what happens with an
   * all-STV contest (see below).
   * @throws IOException never.
   */
  @Test(expectedExceptions = ArrayIndexOutOfBoundsException.class)
  @Transactional
  public void noContestUploadIsAnError() throws IOException {
    testUtils.log(LOGGER, "noContestUploadIsAnError");
    final Path path = Paths.get(TINY_CSV_PATH + "NoContest.csv");
    final Reader reader = Files.newBufferedReader(path);

    County montezuma = fromString("Montezuma");
    final DominionCVRExportParser parser = new DominionCVRExportParser(reader, montezuma, blank, true);
    assertTrue(parser.parse().success);
  }

  /**
   * An all-STV CVR is OK, though it will produce a somewhat strange state in which a county has uploaded a file but has
   * no contests.
   * @throws IOException never.
   */
  @Test
  @Transactional
  public void allSTVUploadIsOK() throws IOException {
    testUtils.log(LOGGER, "allSTVUploadIsOK");
    final Path path = Paths.get(TINY_CSV_PATH + "TinySTVOnly.csv");
    final Reader reader = Files.newBufferedReader(path);

    County bent = fromString("Bent");
    final DominionCVRExportParser parser = new DominionCVRExportParser(reader, bent, blank, true);
    assertTrue(parser.parse().success);
  }

  /**
   * The actual work function for the three files with mixed IRV, STV and plurality. Although the
   * position of the STV contest varies in these three files, the other data is the same, so the
   * parsed result should be the same.
   * @param path       The path to the CSV file.
   * @param countyName The name of the county.
   * @throws IOException never.
   */
  private void doIRVAndSTVAndPluralityTest(Path path, String countyName) throws IOException {
    final Reader reader = Files.newBufferedReader(path);

    County cheyenne = fromString(countyName);
    final DominionCVRExportParser parser = new DominionCVRExportParser(reader, cheyenne, blank, true);
    assertTrue(parser.parse().success);

    // There should be three contests, an IRV one and two plurality ones, because the STV one was
    // dropped.
    final List<Contest> contests = forCounties(Set.of(fromString(countyName)));
    assertEquals(contests.size(), 3);
    final Contest IRVContest = contests.get(0);
    final Contest pluralityContest = contests.get(1);
    final Contest pluralityContest2 = contests.get(2);

    // Check basic data
    assertEquals(IRVContest.name(), "TinyExample1");
    assertEquals(IRVContest.description(), ContestType.IRV.toString());
    assertEquals(IRVContest.choices().stream().map(Choice::name).collect(Collectors.toList()), ABC);

    // Votes allowed should be 3 (because there are 3 ranks), whereas winners=1 always for IRV.
    assertEquals(IRVContest.votesAllowed().intValue(), 3);
    assertEquals(IRVContest.winnersAllowed().intValue(), 1);

    // Check that the 10 expected votes are there.
    final List<CVRContestInfo> cvrs = getMatching(fromString(countyName).id(),
        CastVoteRecord.RecordType.UPLOADED).map(cvr -> cvr.contestInfoForContest(IRVContest)).toList();
    assertEquals(10, cvrs.size());
    for(int i=0 ; i < expectedChoices.size() ; i++) {
      assertEquals(cvrs.get(i).choices(), expectedChoices.get(i));
    }

    // Check basic data for the plurality Example1 contest
    assertEquals(pluralityContest.name(), "PluralityExample1");
    assertEquals(pluralityContest.description(), ContestType.PLURALITY.toString());
    assertEquals(pluralityContest.choices().stream().map(Choice::name)
        .collect(Collectors.toList()), List.of("Diego", "Eli", "Farhad"));

    // Votes allowed should be 1. For plurality, votes allowed = winners allowed.
    assertEquals(pluralityContest.votesAllowed().intValue(), 1);
    assertEquals(pluralityContest.winnersAllowed().intValue(), 1);

    // There are 10 votes:
    // 1 Diego,
    // 3 Farhad, Farhad, Farhad,
    // 3 Diego, Diego, Diego,
    // 3 Eli, Eli, Eli
    final List<List<String>> expectedPluralityChoices = List.of(
        List.of("Diego"),
        List.of("Farhad"), List.of("Farhad"), List.of("Farhad"),
        List.of("Diego"), List.of("Diego"), List.of("Diego"),
        List.of("Eli"), List.of("Eli"), List.of("Eli"));

    final List<CVRContestInfo> pluralityCvrs = getMatching(fromString(countyName).id(),
        CastVoteRecord.RecordType.UPLOADED).map(cvr -> cvr.contestInfoForContest(pluralityContest)).toList();
    assertEquals(10, pluralityCvrs.size());
    for(int i=0 ; i < expectedPluralityChoices.size() ; i++) {
      assertEquals(cvrs.get(i).choices(), expectedChoices.get(i));
    }

    // Check basic data for the plurality Example2 contest
    assertEquals(pluralityContest2.name(), "PluralityExample2");
    assertEquals(pluralityContest2.description(), ContestType.PLURALITY.toString());
    assertEquals(pluralityContest2.choices().stream().map(Choice::name).collect(Collectors.toList()),
       List.of("Gertrude", "Ho", "Imogen"));

    // Votes allowed should be 2
    assertEquals(pluralityContest2.votesAllowed().intValue(), 2);
    assertEquals(pluralityContest2.winnersAllowed().intValue(), 2);

    // Just for good measure, check that there's no contest called "STVExample1" in the database.
    assertTrue(ContestQueries.forCounty(cheyenne).stream().noneMatch(c -> c.name().equals("STVExample1")));
  }
}
