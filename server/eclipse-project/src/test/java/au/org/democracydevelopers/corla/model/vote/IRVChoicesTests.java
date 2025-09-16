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
import org.apache.commons.collections.CollectionUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import java.util.List;

import org.testng.annotations.*;
import static org.testng.Assert.*;

/**
 * Tests for proper interpretation of IRV choices, including proper interpretation of invalid
 * choices involving repeated candidate names and repeated or skipped preferences.
 * The implementation follows the rules specified in Colorado's
 * <A HREF="https://www.sos.state.co.us/pubs/rule_making/CurrentRules/8CCR1505-1/Rule26.pdf">
 *   Election Rules [8 CCR 1505-1] Rule 26</A>
 * Examples are taken directly from the Guide, with example numbers (if given) from there.
 */
public class IRVChoicesTests {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(IRVChoicesTests.class);

  /**
   * Testing getValidIntent against Example 1 in the Guide. This removes duplicates before
   * overvotes.
   * The vote has a repeated first preference and should hence have empty valid interpretation.
   */
  @Test
  public void Example1OvervotesNoValidRankings() throws IRVParsingException {
    testUtils.log(LOGGER, "Example1OvervotesNoValidRankings");

    IRVChoices b = new IRVChoices("Candidate A(1),Candidate B(1),Candidate C(1),Candidate C(2),Candidate B(3)");
    assertEquals(b.getValidIntentAsOrderedList().size(), 0);
  }

  /**
   * Testing getValidIntent against Example 1 in the Guide. This removes duplicates before
   * overvotes. Second preference is duplicated, so only the first is valid.
   * @throws IRVParsingException never
   */
  @Test
  public void Example2OvervoteWithValidRankings() throws IRVParsingException {
    testUtils.log(LOGGER,"Example2OvervoteWithValidRankings");

    IRVChoices b = new IRVChoices("Candidate B(1),Candidate A(2),Candidate C(2),Candidate C(3)");
    CollectionUtils.isEqualCollection(List.of("Candidate B"), b.getValidIntentAsOrderedList());
  }

  /**
   * Example 1 of skipped rankings, from the Guide. Preference 2 is skipped, so everything
   * afterwards is ignored.
   * @throws IRVParsingException never
   */
  @Test
    public void Example1SkippedRankings() throws IRVParsingException {
    testUtils.log(LOGGER,"Example1SkippedRankings");

    IRVChoices b = new IRVChoices("Candidate A(1),Candidate B(3)");
    CollectionUtils.isEqualCollection(List.of("Candidate A"), b.getValidIntentAsOrderedList());
  }

  /**
   * Example 1 of duplicates, i.e. duplicate mentions of a given candidate, from the Guide.
   * Candidate A has duplicate mentions, so only the first counts. This causes preference 2 to
   * be skipped, so everything later is ignored.
   */
  @Test
  public void Example1DuplicateRankings() throws IRVParsingException {
    testUtils.log(LOGGER,"Example1DuplicateRankings");

    IRVChoices b = new IRVChoices("Candidate A(1),Candidate A(2),Candidate B(3)");
    CollectionUtils.isEqualCollection(List.of("Candidate A"), b.getValidIntentAsOrderedList());
  }

  /**
   * Example 1 of duplicates-then-overvotes, from the Guide.
   * Candidate C is duplicated, but after the duplicate mention of C is removed, rank 2 is
   * still overvoted, so only the first preference counts.
   * @throws IRVParsingException never
   */
  @Test
  public void Example1DuplicatesAndOvervotes() throws IRVParsingException {
    testUtils.log(LOGGER,"Example1DuplicatesAndOvervotes");

    IRVChoices b = new IRVChoices("Candidate B(1),Candidate A(2),Candidate C(2),Candidate C(3)");
    CollectionUtils.isEqualCollection(List.of("Candidate B"), b.getValidIntentAsOrderedList());
  }

  /**
   * Example 2 of duplicates-then-overvotes, from the Guide.
   * The preference for Candidate B(2) is removed (it duplicates B(1)), hence leaving no overvote
   * for preference 2.
   * @throws IRVParsingException never
   */
  @Test
  public void Example2DuplicatesAndOvervotes() throws IRVParsingException {
    testUtils.log(LOGGER,"Example2DuplicatesAndOvervotes");

    IRVChoices b = new IRVChoices("Candidate B(1),Candidate A(2),Candidate B(2),Candidate C(3)");
    CollectionUtils.isEqualCollection(List.of("Candidate B", "Candidate A", "Candidate C"), b.getValidIntentAsOrderedList());
  }

  /**
   * A blank vote is valid.
   */
  @Test
  public void blankVoteIsValid() throws IRVParsingException {

    IRVChoices b = new IRVChoices("");
    assertTrue(b.isValid());
    assertEquals(b.getValidIntentAsOrderedList().size(), 0);
  }

  /**
   * Other general validity tests for ballot interpretation,
   * including a blank vote.
   * Valid choices.
   * @throws IRVParsingException never
   */
  @Test
  public void testValidBallots() throws IRVParsingException {
    testUtils.log(LOGGER,"testValidBallots");

    IRVChoices b = new IRVChoices("Alice(1)");
    assertTrue(b.isValid());

    b = new IRVChoices("Alice(1),Bob(2)");
    assertTrue(b.isValid());

    b = new IRVChoices("Alice(1),Chuan(3),Bob(2)");
    assertTrue(b.isValid());

    b = new IRVChoices("Diego(4),Alice(1),Chuan(3),Bob(2)");
    assertTrue(b.isValid());
  }

  /**
   * Invalid choices. Check that they're correctly identified as not valid.
   * @throws IRVParsingException never.
   */
  @Test
  public void testInvalidBallot() throws IRVParsingException {
    testUtils.log(LOGGER,"testInvalidBallot");

    IRVChoices b = new IRVChoices("Alice(1),Alice(1)");
    assertFalse(b.isValid());

    b = new IRVChoices("Alice(1),Alice(2)");
    assertFalse(b.isValid());

    b = new IRVChoices("Alice(1),Bob(1)");
    assertFalse(b.isValid());

    b = new IRVChoices("Alice(1),Bob(3)");
    assertFalse(b.isValid());

    b = new IRVChoices("Alice(2),Bob(2)");
    assertFalse(b.isValid());

    b = new IRVChoices("Alice(2),Bob(3)");
    assertFalse(b.isValid());
    }

  /**
   * The valid interpretation of a valid vote is just the same vote.
   * @throws IRVParsingException never
   */
  @Test
  public void validVoteIsUnchanged() throws IRVParsingException {
    testUtils.log(LOGGER, "validVoteIsUnchanged");

    IRVChoices b = new IRVChoices("Alice(1),Bob(2)");
    CollectionUtils.isEqualCollection(List.of("Alice","Bob"), b.getValidIntentAsOrderedList());
  }

  /**
   * The valid interpretation of a valid vote is just the same vote.
   * @throws IRVParsingException never
   */
  @Test
  public void validThreeChoiceVoteIsUnchanged() throws IRVParsingException {
    testUtils.log(LOGGER,"validThreeChoiceVoteIsUnchanged");

    IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(3)");
    CollectionUtils.isEqualCollection(List.of("Alice","Bob","Chuan"), b.getValidIntentAsOrderedList());
  }

  /**
   * Test rule 26.7.1 - removing overvotes (repeated preferences).
   * The duplicates-before-overvotes rule means that the duplicate mention of Alice will be removed,
   * leaving "Alice(1)", which is valid.
   * @throws IRVParsingException never
   */
  @Test
  public void removeOvervotesTest1() throws IRVParsingException {
    testUtils.log(LOGGER,"removeOvervotesTest1");

    IRVChoices b = new IRVChoices("Alice(1),Alice(1)");
    CollectionUtils.isEqualCollection(List.of("Alice"), b.getValidIntentAsOrderedList());
  }

  /**
   * Test rule 26.7.1 - removing overvotes (repeated preferences).
   * Removes repeat 2nd preference.
   * @throws IRVParsingException never
   */
  @Test
  public void removeOvervotesTest2() throws IRVParsingException {
    testUtils.log(LOGGER,"removeOvervotesTest2");

    IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(2)");
    assertEquals(b.getValidIntentAsOrderedList().get(0), "Alice");
  }

  /**
   * Test rule 26.7.1 - removing overvotes (repeated preferences).
   * Removes repeat 2nd preference and the subsequent preference.
   * @throws IRVParsingException never
   */
  @Test
  public void removeOvervotesTest3() throws IRVParsingException {
    testUtils.log(LOGGER,"removeOvervotesTest3");

    IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(2),Diego(3)");
    assertEquals(b.getValidIntentAsOrderedList().get(0), "Alice");
  }

  /**
   * Test rule 26.7.1 - removing overvotes (repeated preferences).
   * Removes repeat 3rd preference.
   * @throws IRVParsingException never
   */
  @Test
  public void removeOvervotesTest4() throws IRVParsingException {
    testUtils.log(LOGGER,"removeOvervotesTest4");

    IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(3),Diego(3)");
    CollectionUtils.isEqualCollection(List.of("Alice","Bob"), b.getValidIntentAsOrderedList());
  }

  /**
   * Test rule 26.7.1 - removing overvotes (repeated preferences).
   * Removes repeated 1st preference, leaving nothing.
   * @throws IRVParsingException never
   */
  @Test
  public void removeOvervotesTest5() throws IRVParsingException {
    testUtils.log(LOGGER,"removeOvervotesTest4");

    IRVChoices b = new IRVChoices("Alice(1),Bob(1)");
    CollectionUtils.isEqualCollection(List.of(), b.getValidIntentAsOrderedList());
  }

  /**
   * Test rule 26.7.2 - removing skipped rankings.
   * Removes all but the first preference.
   * @throws IRVParsingException never
   */
  @Test
  public void removeSkipsTest1() throws IRVParsingException {
    testUtils.log(LOGGER,"removeSkipsTest1");

    IRVChoices b = new IRVChoices("Alice(1),Bob(3)");
    CollectionUtils.isEqualCollection(List.of("Alice"),b.getValidIntentAsOrderedList());
  }

  /**
   * Test rule 26.7.2 - removing skipped rankings.
   * First rank skipped - remove all.
   * @throws IRVParsingException never
   */
  @Test
  public void removeSkipsTest2() throws IRVParsingException {
    testUtils.log(LOGGER,"removeSkipsTest2");

    IRVChoices b = new IRVChoices("Bob(2),Chuan(3)");
    assertEquals(b.getValidIntentAsOrderedList().size(), 0);
  }

  /**
   * Test rule 26.7.2 - removing skipped rankings.
   * Third rank skipped - remove the 4th.
   * @throws IRVParsingException never
   */
  @Test
  public void removeSkipsTest3() throws IRVParsingException {
    testUtils.log(LOGGER,"removeSkipsTest3");

    IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(4)");
    CollectionUtils.isEqualCollection(List.of("Alice","Bob"),b.getValidIntentAsOrderedList());
  }

  /**
   * Test rule 26.7.2 - removing skipped rankings.
   * First rank skipped - remove all.
   * @throws IRVParsingException never
   */
  @Test
  public void removeSkipsTest4() throws IRVParsingException {
    testUtils.log(LOGGER,"removeSkipsTest4");

    IRVChoices b = new IRVChoices("Alice(3),Bob(2),Chuan(4)");
    assertEquals(b.getValidIntentAsOrderedList().size(), 0);
  }

  /**
   * Test rule 26.7.2 - removing skipped rankings.
   * First rank is not 1 - remove all.
   * @throws IRVParsingException never
   */
  @Test
  public void removeSkipsTest5() throws IRVParsingException {
    testUtils.log(LOGGER,"removeSkipsTest4");

    IRVChoices b = new IRVChoices("Alice(3)");
    assertEquals(b.getValidIntentAsOrderedList().size(), 0);
  }

  /**
   * Combination of overvotes and skips.
   * The overvoted second rank is removed, then everything afterwards.
   * @throws IRVParsingException never
   */
  @Test
  public void overvotesAndSkipsTest1() throws IRVParsingException {
    testUtils.log(LOGGER,"overvotesAndSkipsTest1");

    IRVChoices b = new IRVChoices("Alice(1),Bob(2),Chuan(2),Diego(4)");
    CollectionUtils.isEqualCollection(List.of("Alice"), b.getValidIntentAsOrderedList());
  }

  /**
   * Apply rule 26.7.3 - removing all but the highest expressed preference for a given candidate.
   * Result is a single valid 1st preference for Alice.
   * @throws IRVParsingException never
   */
  @Test
  public void duplicateCandidatesTest1() throws IRVParsingException {
    testUtils.log(LOGGER,"duplicateCandidatesTest1");

    IRVChoices b = new IRVChoices("Alice(2),Alice(1)");
    CollectionUtils.isEqualCollection(List.of("Alice"), b.getValidIntentAsOrderedList());
  }

  /**
   * Apply rule 26.7.3 - removing all but the highest expressed preference for a given candidate.
   * The second preference for Alice is removed, leaving two valid preferences.
   * @throws IRVParsingException never
   */
  @Test
  public void duplicateCandidatesTest2() throws IRVParsingException {
    testUtils.log(LOGGER,"duplicateCandidatesTest2");

    IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(2)");
    CollectionUtils.isEqualCollection(List.of("Alice","Bob"), b.getValidIntentAsOrderedList());
  }

  /**
   * Apply rule 26.7.3 - removing all but the highest expressed preference for a given candidate.
   * The second preference for Alice and third preference for Bob are removed, leaving a vote
   * that is invalid for other reasons - the 4th preference is then dropped.
   * @throws IRVParsingException never
   */
  @Test
  public void duplicateCandidatesTest3() throws IRVParsingException {
    testUtils.log(LOGGER,"duplicateCandidatesTest3");

    IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(2),Chuan(4),Bob(3)");
    CollectionUtils.isEqualCollection(List.of("Alice","Bob"), b.getValidIntentAsOrderedList());
  }

  /**
   * Apply rule 26.7.3 - removing all but the highest expressed preference for a given candidate.
   * Only the first expressed preference for Alice is retained.
   * @throws IRVParsingException never
   */
  @Test
  public void duplicateCandidatesTest4() throws IRVParsingException {
    testUtils.log(LOGGER,"duplicateCandidatesTest4");

    IRVChoices b = new IRVChoices("Alice(1),Alice(2),Alice(4)");
    CollectionUtils.isEqualCollection(List.of("Alice"), b.getValidIntentAsOrderedList());
  }

  /**
   * Apply rule 26.7.3 - removing all but the highest expressed preference for a given candidate.
   * Alice(2) is removed, leaving a skipped 2nd rank, so then the later (3rd) rank is also dropped.
   * @throws IRVParsingException never
   */
  @Test
  public void duplicateCandidatesTest5() throws IRVParsingException {
    testUtils.log(LOGGER,"duplicateCandidatesTest5");

    IRVChoices b = new IRVChoices("Alice(1),Alice(2),Bob(3)");
    CollectionUtils.isEqualCollection(List.of("Alice"), b.getValidIntentAsOrderedList());
  }
}
