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

import au.org.democracydevelopers.corla.model.vote.IRVParsingException;
import au.org.democracydevelopers.corla.util.testUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static au.org.democracydevelopers.corla.csv.IRVHeadersParser.validateIRVPreferenceHeaders;
import static au.org.democracydevelopers.corla.util.testUtils.BAD_CSV_PATH;
import static au.org.democracydevelopers.corla.util.testUtils.TINY_CSV_PATH;

/**
 * Tests for various kinds of invalid IRV headers, such as those that don't have a consistent
 * list of choice names for each rank, or have reordered or repeated choices.
 * Note this does _not_ test
 * - when the choice itself can't be parsed properly,
 * - when the list of ranks is larger than expected (this needs to be checked by
 *   DominionCVRExportParser).
 */
public class IRVHeadersParserTests {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(IRVHeadersParserTests.class);

  /**
   * Regexp for testing error string.
   */
  private static final String invalidIRVChoicesRegexp = "Invalid IRV choices.*";

  /**
   * Regexp for testing error string.
   */
  private static final String insufficientChoicesRegexp = "Insufficient choices.*";

  /**
   * Test valid IRV preference headers. This is a very simple set of valid headers.
   * @throws IRVParsingException never.
   */
  @Test
  public void validateValidIRVPreferenceHeaders() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "validateValidIRVPreferenceHeaders");
    Path path = Paths.get(TINY_CSV_PATH +"ThreeCandidatesTenVotes.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3,3);
  }

  /**
   * Another simple test of valid IRV preference headers.
   * @throws IRVParsingException never.
   */
  @Test
  public void validateValidIRVPreferenceHeaders2() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "validateValidIRVPreferenceHeaders2");
    Path path = Paths.get(TINY_CSV_PATH +"GuideToRAIREExample3.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 4,4);
  }

  /**
   * Another simple test of valid IRV preference headers, except the expected maxRank is too large.
   * @throws IRVParsingException always.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = insufficientChoicesRegexp)
  public void maxRankTooLarge() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "maxRankTooLarge");
    Path path = Paths.get(TINY_CSV_PATH +"ThreeCandidatesTenVotes.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3,4);
  }

  /**
   * Another simple test of valid IRV preference headers, except the expected maxRank is too small.
   * This does not throw an exception - the test for sufficiently _many_ ranks must be made before
   * calling validateIRVPreferenceHeaders().
   * @throws IRVParsingException never.
   */
  @Test
  public void maxRankTooSmallIsValid() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "maxRankTooSmallIsValid");
    Path path = Paths.get(TINY_CSV_PATH +"ThreeCandidatesTenVotes.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3,2);
  }

  /**
   * Another simple test of valid IRV preference headers, except the expected number of choices is
   * too large.
   * @throws IRVParsingException always.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = invalidIRVChoicesRegexp)
  public void numChoicesTooLarge() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "numChoicesTooLarge");
    Path path = Paths.get(TINY_CSV_PATH +"ThreeCandidatesTenVotes.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 4,3);
  }

  /**
   * Another simple test of valid IRV preference headers, except the expected number of choices is
   * too small.
   * @throws IRVParsingException always.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = invalidIRVChoicesRegexp)
  public void numChoicesTooSmall() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "numChoicesTooSmall");
    Path path = Paths.get(TINY_CSV_PATH +"ThreeCandidatesTenVotes.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 2,3);
  }

  /**
   * Invalid headers with inconsistent names.
   * @throws IRVParsingException always.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = invalidIRVChoicesRegexp)
  public void differentCandidateNamesInDifferentRanks1() throws IRVParsingException, IOException {
    testUtils.log(LOGGER,
        "differentCandidateNamesInDifferentRanks1");
    Path path = Paths.get(BAD_CSV_PATH +
        "InvalidIRVHeadersDifferentCandidateNamesInDifferentRanks1.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3,3);
  }

  /**
   * Another example of invalid headers with inconsistent names.
   * @throws IRVParsingException always.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = invalidIRVChoicesRegexp)
  public void differentCandidateNamesInDifferentRanks2() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "differentCandidateNamesInDifferentRanks2");
    Path path = Paths.get(BAD_CSV_PATH +
        "InvalidIRVHeadersDifferentCandidateNamesInDifferentRanks2.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3, 3);
  }

  /**
   * A repeated candidate name in the first rank.
   * @throws IRVParsingException never.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = invalidIRVChoicesRegexp)
  public void repeated1stRankCandidate() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "repeated1stRankCandidate");
    Path path = Paths.get(BAD_CSV_PATH + "InvalidIRVHeadersRepeated1stRankCandidate.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3, 3);
  }


  /**
   * Repeated candidate in the second rank.
   * @throws IRVParsingException always.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = invalidIRVChoicesRegexp)
  public void repeated2ndRankCandidate() throws IRVParsingException, IOException {

    testUtils.log(LOGGER,"repeated2ndRankCandidate");
    Path path = Paths.get(BAD_CSV_PATH +"InvalidIRVHeadersRepeated2ndRankCandidate.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3,3);
  }


  /**
   * Candidates are the same between ranks, but are in a shuffled order. This is invalid.
   * @throws IRVParsingException always.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = invalidIRVChoicesRegexp)
  public void shuffledChoices() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "shuffledChoices");
    Path path = Paths.get(BAD_CSV_PATH + "InvalidIRVHeadersShuffledChoices.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3, 3);
  }

  /**
   * A first-rank candidate is skipped.
   * @throws IRVParsingException always.
   */
  @Test(expectedExceptions = IRVParsingException.class,
      expectedExceptionsMessageRegExp = invalidIRVChoicesRegexp)
  public void skipped1stRankCandidate() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "skipped1stRankCandidate");
    Path path = Paths.get(BAD_CSV_PATH +"InvalidIRVHeadersSkipped1stRankCandidate.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3,3);
  }

  /**
   * Invalid IRV preference headers - skipped 3rd rank candidate.
   * @throws IRVParsingException always.
   * There's no message regexp here because, although the current implementation says "Invalid IRV
   * choices header", it would be equally valid to say "Insufficient choices"
   */
  @Test(expectedExceptions = IRVParsingException.class)
  public void skipped3rdRank() throws IRVParsingException, IOException {
    testUtils.log(LOGGER, "skipped3rdRank");
    Path path = Paths.get(BAD_CSV_PATH +"InvalidIRVHeadersSkipped3rdRankCandidate.csv");

    Reader reader = Files.newBufferedReader(path);
    CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT);
    CSVRecord theLine = parser.getRecords().get(2);
    validateIRVPreferenceHeaders(theLine, 7, 3,3);
  }
}
