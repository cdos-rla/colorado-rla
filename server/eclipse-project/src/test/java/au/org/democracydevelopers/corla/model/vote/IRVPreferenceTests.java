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

package au.org.democracydevelopers.corla.model.vote;

import au.org.democracydevelopers.corla.util.testUtils;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.testng.annotations.*;
import static org.testng.Assert.*;

/**
 * Tests for proper parsing of IRV choices - these are supposed to be of the form name(rank).
 * Tests include both valid complex tests (e.g. candidate names with parentheses, whitespace
 * inside parentheses), and invalid tests to check an exception is thrown.
 */
public class IRVPreferenceTests {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(IRVPreferenceTests.class);


  /**
   * Regular expressions used to check we get the right error message for parse errors.
   */
  private static final String noParseRegexp = "Could not parse.*";

  /**
   * Ordinary two-digit rank.
   * @throws IRVParsingException never.
   */
  @Test
  public void parseTwoDigitPreferences() throws IRVParsingException {
    testUtils.log(LOGGER, "parseTwoDigitPreferences");
    String choice = "Alice(10)";

    IRVPreference p = new IRVPreference(choice);

    assertEquals(10, p.rank);
    assertEquals("Alice", p.candidateName);
  }

  /**
   * Whitespace and digit in the candidate name.
   * @throws IRVParsingException never.
   */
  @Test
  public void parseTwoDigitPreferences2() throws IRVParsingException {
    testUtils.log(LOGGER, "parseTwoDigitPreferences2");

    String choice = "Candidate 1(10)";

    IRVPreference p = new IRVPreference(choice);

    assertEquals(10, p.rank);
    assertEquals("Candidate 1", p.candidateName);
  }

  /**
   * Two-digit rank; whitespace after parentheses.
   * @throws IRVParsingException never.
   */
  @Test
  public void parseTwoDigitPreferences2WithSpace() throws IRVParsingException {
    testUtils.log(LOGGER, "parseTwoDigitPreferences2WithSpace");

    String choice = "Candidate 1(10) ";

    IRVPreference p = new IRVPreference(choice);

    assertEquals(10, p.rank);
    assertEquals("Candidate 1", p.candidateName);
  }

  /**
   * Two-digit rank; whitespace before and after parentheses.
   * @throws IRVParsingException never.
   */
  @Test
  public void parseTwoDigitPreferences2WithOtherSpace() throws IRVParsingException {
    testUtils.log(LOGGER, "parseTwoDigitPreferences2WithOtherSpace");

    String choice = "Candidate 1  (10) ";

    IRVPreference p = new IRVPreference(choice);

    assertEquals(10, p.rank);
    assertEquals("Candidate 1", p.candidateName);
  }

  /**
   * Candidate name contains a digit in parentheses.
   * @throws IRVParsingException never.
   */
  @Test
  public void parseCandidateNameWithParentheses() throws IRVParsingException {
    testUtils.log(LOGGER, "parseCandidateNameWithParentheses");

    String choice = "  Henry(8) (10) ";

    IRVPreference p = new IRVPreference(choice);

    assertEquals(10, p.rank);
    assertEquals("Henry(8)", p.candidateName);
  }

  /**
   * Whitespace inside and after parentheses.
   * @throws IRVParsingException never.
   */
  @Test
  public void parseWhiteSpaceInsideParentheses() throws IRVParsingException {
    testUtils.log(LOGGER, "parseWhiteSpaceInsideParentheses");

    String choice = "  Henry (  10  ) ";

    IRVPreference p = new IRVPreference(choice);

    assertEquals(10, p.rank);
    assertEquals("Henry", p.candidateName);
  }

  /**
   * Parentheses but no rank. This is an error.
   * @throws IRVParsingException and checks it is thrown.
   */
  @Test(expectedExceptions = IRVParsingException.class,
        expectedExceptionsMessageRegExp = noParseRegexp)
  public void parseChoiceWithEmptyParenthesesThrowsException() throws IRVParsingException {
    testUtils.log(LOGGER, "parseChoiceWithEmptyParenthesesThrowsException");


    String choice = "CandidateWithNoPreference()";

    new IRVPreference(choice);
  }


  /**
   * Nested parentheses. This is an error.
   * @throws IRVParsingException and checks it is thrown.
   */
  @Test(expectedExceptions = IRVParsingException.class,
        expectedExceptionsMessageRegExp = noParseRegexp)
  public void parseChoiceWithNestedParenthesesThrowsException() throws IRVParsingException {
    testUtils.log(LOGGER, "parseChoiceWithNestedParenthesesThrowsException");

    String choice = "CandidateWithNestedParentheses((42))";

    new IRVPreference(choice);
  }

  /**
   * A rank of 0, which is not allowed. This is an error.
   * @throws IRVParsingException and checks that it is thrown.
   */
  @Test(expectedExceptions = IRVParsingException.class,
        expectedExceptionsMessageRegExp = noParseRegexp)
  public void parseChoiceWithZeroPreferenceThrowsException() throws IRVParsingException {
    testUtils.log(LOGGER, "parseChoiceWithZeroPreferenceThrowsException");

    String choice = "CandidateWithZeroPreference(0)";

    new IRVPreference(choice);
  }

  /**
   * A negative rank, which is not allowed. This is an error.
   * @throws IRVParsingException and checks it is thrown.
   */
  @Test(expectedExceptions = IRVParsingException.class,
        expectedExceptionsMessageRegExp = noParseRegexp)
  public void parseChoiceWithNegativePreferenceThrowsException() throws IRVParsingException {
    testUtils.log(LOGGER, "parseChoiceWithNegativePreferenceThrowsException");

    String choice = "CandidateWithZeroPreference(-10)";

    new IRVPreference(choice);
  }

  /**
   * A non-integer rank, which is not allowed. This is an error.
   * @throws IRVParsingException and checks it is thrown.
   */
  @Test(expectedExceptions = IRVParsingException.class,
        expectedExceptionsMessageRegExp = noParseRegexp)
  public void parseChoiceWithFractionalPreferenceThrowsException() throws IRVParsingException {
    testUtils.log(LOGGER, "parseChoiceWithFractionalPreferenceThrowsException");

    String choice = "CandidateWithFractionalPreference(2.5)";

    new IRVPreference(choice);
  }

  /**
   * A non-numerical rank, which is not allowed. This is an error.
   * @throws IRVParsingException and checks it is thrown.
   */
  @Test(expectedExceptions = IRVParsingException.class,
        expectedExceptionsMessageRegExp = noParseRegexp)
  public void parseChoiceWithStringPreferenceThrowsException() throws IRVParsingException {
    testUtils.log(LOGGER, "parseChoiceWithStringPreferenceThrowsException");

    String choice = "CandidateWithFractionalPreference(pref)";

    new IRVPreference(choice);
  }

  /**
   * No rank, and no parentheses, which is not allowed. This is an error.
   * @throws IRVParsingException and checks it is thrown.
   */
  @Test(expectedExceptions = IRVParsingException.class,
        expectedExceptionsMessageRegExp = noParseRegexp)
  public void parseChoiceWithNoParenthesesThrowsException() throws IRVParsingException {
    testUtils.log(LOGGER, "parseChoiceWithNoParenthesesThrowsException");

    String choice = "CandidateWithNoPreference";

    new IRVPreference(choice);
  }

  /**
   * All-whitespace candidate name. This is an error.
   * @throws IRVParsingException and checks it is thrown.
   */
  @Test(expectedExceptions = IRVParsingException.class,
        expectedExceptionsMessageRegExp = noParseRegexp)
  public void parseChoiceWithWhiteSpaceNameThrowsException() throws IRVParsingException {
    testUtils.log(LOGGER, "parseChoiceWithWhiteSpaceNameThrowsException");

    String choice = "    (23)";

    new IRVPreference(choice);
  }
}
