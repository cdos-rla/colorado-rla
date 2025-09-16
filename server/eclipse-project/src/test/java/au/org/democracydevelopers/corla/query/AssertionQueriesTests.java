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

package au.org.democracydevelopers.corla.query;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import au.org.democracydevelopers.corla.model.assertion.Assertion;
import au.org.democracydevelopers.corla.model.assertion.NEBAssertion;
import au.org.democracydevelopers.corla.model.assertion.NENAssertion;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;
import au.org.democracydevelopers.corla.util.testUtils;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This test class tests the functionality in the DemocracyDevelopers version of AssertionQueries.
 * At the time of writing these tests, this class contains one method for collecting all assertions
 * that belong to a specific contest, identified by contest name.
 */
public class AssertionQueriesTests extends TestClassWithDatabase {

  private static final Logger LOGGER = LogManager.getLogger(AssertionQueriesTests.class);

  /**
   * Start the test container and establish persistence properties before the first test.
   */
  @BeforeClass
  public void beforeAllThisClass() {
    runSQLSetupScript("SQL/co-counties.sql");
    runSQLSetupScript("SQL/simple-assertions.sql");
  }

  /**
   * Check that an empty list is returned for a non-existent contest.
   */
  @Test
  public void testNonExistentContest(){
    testUtils.log(LOGGER, "testNonExistentContest");
    final List<Assertion> assertions = AssertionQueries.matching("Contest Number 9");

    assertTrue(assertions.isEmpty());
  }

  /**
   * Check that an empty list is returned for a existent contest with no assertions.
   */
  @Test
  public void testExistentContestNoAssertions(){
    testUtils.log(LOGGER, "testExistentContestNoAssertions");
    final List<Assertion> assertions = AssertionQueries.matching("No Assertions");

    assertTrue(assertions.isEmpty());
  }

  /**
   * Check that the correct assertions are returned for a contest with one NEB assertion.
   */
  @Test
  public void testOneNEBContest(){
    testUtils.log(LOGGER, "testOneNEBContest");
    final List<Assertion> assertions = AssertionQueries.matching("One NEB Assertion Contest");

    assertEquals(assertions.size(), 1);

    final Assertion a = assertions.get(0);

    assertTrue(a instanceof NEBAssertion);
    assertTrue(a.getDescription().startsWith(
        "Alice NEB Bob: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));
  }

  /**
   * Check that the correct assertions are returned for a contest with one NEN assertion.
   */
  @Test
  public void testOneNENContest(){
    testUtils.log(LOGGER, "testOneNENContest");
    final List<Assertion> assertions = AssertionQueries.matching("One NEN Assertion Contest");

    assertEquals(assertions.size(), 1);

    final Assertion a = assertions.get(0);

    assertTrue(a instanceof NENAssertion);
    assertTrue(a.getDescription().startsWith(
        "Alice NEN Charlie assuming ([Alice, Charlie, Diego, Bob]) are continuing: oneOver = 0; " +
            "twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));
  }

  /**
   * Check that the correct assertions are returned for a contest with one NEN and one NEB assertion.
   */
  @Test
  public void testOneNENNEBContest(){
    testUtils.log(LOGGER, "testOneNENNEBContest");
    final List<Assertion> assertions = AssertionQueries.matching("One NEN NEB Assertion Contest");

    assertEquals(assertions.size(), 2);

    final Assertion a1 = assertions.get(0);
    assertTrue(a1 instanceof NEBAssertion);
    assertTrue(a1.getDescription().startsWith(
        "Amanda NEB Liesl: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a2 = assertions.get(1);
    assertTrue(a2 instanceof NENAssertion);
    assertTrue(a2.getDescription().startsWith(
        "Amanda NEN Wendell assuming ([Liesl, Wendell, Amanda]) are continuing: oneOver = 0; " +
            "twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));
  }

  /**
   * Check that the correct assertions are returned for a multi-county contest.
   */
  @Test
  public void testMultiCountyContest(){
    testUtils.log(LOGGER, "testMultiCountyContest");
    final List<Assertion> assertions = AssertionQueries.matching("Multi-County Contest 1");

    assertEquals(assertions.size(), 3);

    final Assertion a1 = assertions.get(0);
    assertTrue(a1 instanceof NEBAssertion);
    assertTrue(a1.getDescription().startsWith(
        "Charlie C. Chaplin NEB Alice P. Mangrove: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a2 = assertions.get(1);
    assertTrue(a2 instanceof NEBAssertion);
    assertTrue(a2.getDescription().startsWith(
        "Alice P. Mangrove NEB Al (Bob) Jones: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a3 = assertions.get(2);
    assertTrue(a3 instanceof NENAssertion);
    assertTrue(a3.getDescription().startsWith(
        "Alice P. Mangrove NEN West W. Westerson assuming ([West W. Westerson, Alice P. Mangrove]) are "
            + "continuing: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));
  }

  /**
   * Check that the correct assertions are returned for a mixed assertion contest where some of
   * the assertions have discrepancies.
   */
  @Test
  public void testMixedAssertionsWithDiscrepancies1(){
    testUtils.log(LOGGER, "testMixedAssertionsWithDiscrepancies1");
    final List<Assertion> assertions = AssertionQueries.matching("Test Estimation Mixed Assertions");

    assertEquals(assertions.size(), 8);

    final Assertion a1 = assertions.get(0);
    assertTrue(a1 instanceof NEBAssertion);
    assertTrue(a1.getDescription().startsWith(
        "A NEB B: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a2 = assertions.get(1);
    assertTrue(a2 instanceof NEBAssertion);
    assertTrue(a2.getDescription().startsWith(
        "B NEB C: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a3 = assertions.get(2);
    assertTrue(a3 instanceof NEBAssertion);
    assertTrue(a3.getDescription().startsWith(
        "F NEB G: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 1; other = 0"
    ));

    final Assertion a4 = assertions.get(3);
    assertTrue(a4 instanceof NEBAssertion);
    assertTrue(a4.getDescription().startsWith(
        "H NEB I: oneOver = 1; twoOver = 1; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a5 = assertions.get(4);
    assertTrue(a5 instanceof NENAssertion);
    assertTrue(a5.getDescription().startsWith(
        "A NEN B assuming ([A, B]) are continuing: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a6 = assertions.get(5);
    assertTrue(a6 instanceof NENAssertion);
    assertTrue(a6.getDescription().startsWith(
        "B NEN C assuming ([B, C]) are continuing: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a7 = assertions.get(6);
    assertTrue(a7 instanceof NENAssertion);
    assertTrue(a7.getDescription().startsWith(
        "F NEN G assuming ([F, G]) are continuing: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 1; other = 0"
    ));

    final Assertion a8 = assertions.get(7);
    assertTrue(a8 instanceof NENAssertion);
    assertTrue(a8.getDescription().startsWith(
        "H NEN I assuming ([H, I]) are continuing: oneOver = 1; twoOver = 1; oneUnder = 0, twoUnder = 0; other = 0"
    ));
  }

  /**
   * Check that the correct assertions are returned for a mixed assertion contest where all of
   * the assertions have discrepancies, with more than one of the same type..
   */
  @Test
  public void testMixedAssertionsWithDiscrepancies2(){
    testUtils.log(LOGGER, "testMixedAssertionsWithDiscrepancies2");
    final List<Assertion> assertions = AssertionQueries.matching("Mixed Assertions With Discrepancies");

    assertEquals(assertions.size(), 5);

    final Assertion a1 = assertions.get(0);
    assertTrue(a1 instanceof NEBAssertion);
    assertTrue(a1.getDescription().startsWith(
        "B NEB C: oneOver = 0; twoOver = 0; oneUnder = 0, twoUnder = 2; other = 0"
    ));

    final Assertion a2 = assertions.get(1);
    assertTrue(a2 instanceof NEBAssertion);
    assertTrue(a2.getDescription().startsWith(
        "F NEB G: oneOver = 0; twoOver = 0; oneUnder = 2, twoUnder = 0; other = 1"
    ));

    final Assertion a3 = assertions.get(2);
    assertTrue(a3 instanceof NEBAssertion);
    assertTrue(a3.getDescription().startsWith(
        "H NEB I: oneOver = 0; twoOver = 2; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a4 = assertions.get(3);
    assertTrue(a4 instanceof NENAssertion);
    assertTrue(a4.getDescription().startsWith(
        "A NEN B assuming ([A, B]) are continuing: oneOver = 2; twoOver = 0; oneUnder = 0, twoUnder = 0; other = 0"
    ));

    final Assertion a5 = assertions.get(4);
    assertTrue(a5 instanceof NENAssertion);
    assertTrue(a5.getDescription().startsWith(
        "F NEN G assuming ([F, G]) are continuing: oneOver = 0; twoOver = 0; oneUnder = 1, twoUnder = 0; other = 2"
    ));
  }

}
