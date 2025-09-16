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

package au.org.democracydevelopers.corla.util;
import au.org.democracydevelopers.corla.model.ContestType;
import org.apache.log4j.Logger;
import us.freeandfair.corla.model.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * This class contains utilities and default data for use in testing.
 */
public class testUtils {

  /**
   * First of two IRV contests for mocking the IRVContestCollector: Boulder Mayoral '22
   */
   public final static String boulderMayoral = "City of Boulder Mayoral Candidates";

  /**
   * Count of the universe size for Boulder Mayoral '23.
   */
  public final static int bouldMayoralCount = 118669;

  /**
   * Tiny constructed example.
   */
  public final static String tinyIRV = "TinyExample1";

  /**
   * Tied contest for mocking.
   */
  public final static String tiedIRV = "TiedIRV";

  /**
   * Count of the universe size for TinyExample1
   */
  public final static int tinyIRVCount = 10;

  /**
   * Non-existent contest
   */
  public final static String nonExistentContest = "Non existent contest";

  public final static Choice alice = new Choice("Alice", "", false, false);
  public final static Choice bob = new Choice("Bob", "", false, false);
  public final static Choice chuan = new Choice("Chuan", "", false, false);

  /**
   * Candidates for tinyExample1
   */
  public final static List<Choice> tinyIRVCandidates = List.of(alice, bob, chuan);



  /**
   * Comparator for doubles within a specific tolerance.
   */
  public static final DoubleComparator doubleComparator = new DoubleComparator();

  /**
   * Location of the tiny examples intended to be human-tallyable.
   */
  public static final String TINY_CSV_PATH = "src/test/resources/CSVs/Tiny-IRV-Examples/";

  /**
   * Location of the (redacted) data from Boulder '23.
   */
  public static final String BOULDER_CSV_PATH = "src/test/resources/CSVs/Boulder23/";

  /**
   * Location of the data derived from New South Wales '21 elections.
   */
  public static final String NSW_CSV_PATH = "src/test/resources/CSVs/NewSouthWales21/";

  /**
   * Location of examples that are expected to fail to parse.
   */
  public static final String BAD_CSV_PATH = "src/test/resources/CSVs/badExamples/";

  /**
   * Location of examples that test expanded definition of Write-In string.
   */
  public static final String WRITEIN_CSV_PATH = "src/test/resources/CSVs/WriteIns/";

  /**
   * Print log statement indicating that a specific test has started running.
   */
  public static void log(Logger logger, String test){
    logger.debug(String.format("RUNNING TEST: %s.",test));
  }

  /**
   * Test that two lists of strings are identical.
   */
  public static boolean equalStringLists(final List<String> s1, final List<String> s2) {
    if (s1.size() != s2.size()) {
      return false;
    }

    for(int i = 0; i < s1.size(); i++){
      if(!s1.get(i).equals(s2.get(i))){
        return false;
      }
    }
    return true;
  }
}
