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
import au.org.democracydevelopers.corla.model.vote.IRVPreference;
import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import us.freeandfair.corla.model.Choice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utils for parsing IRV headers as part of DominionCVRExportParser.
 * Main methods:
 * - validateIRVPreferenceHeaders validates a whole collection as part of a CSV header, to check
 *   that they are consistent with the expected pattern.  For example,
 *   Alice(1), Bob(1), Chuan(1), Alice(2), Bob(2), Chuan(2), Alice(3), Bob(3), Chuan(3) is valid,
 *   but any rearrangement of those items, or anything that omits some rank or candidate included
 *   elsewhere, is invalid.
 * - generateAllIRVPreferences produces the expected pattern of preferences, given the list of
 *   choices and the number of ranks allowed.
 */
public class IRVHeadersParser {
  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(IRVHeadersParser.class);

  /**
   * Checks whether a CSVRecord contains a sequence of choices as expected for IRV - it should be
   * a sequence of name(rank) choices, starting with preference 1 and going up to maxRank,
   * and for each rank, it should contain all the same choices in the same order.
   * This works by stepping through the 1st-rank choices, checking that the name has not been
   * repeated, and then checking that the later-ranked versions of the same choice (located
   * (rank_being_checked-1)*numChoices further along the line) have the same name and the correct
   * rank.
   * @param theLine the CSV line (basically a list of Strings)
   * @param startIndex the first index for this contest's choices.
   * @param numChoices the number of choices (candidates).
   * @param maxRank The number of ranks allowed (which matches the number expected in the csv).
   * @throws IRVParsingException if either an individual choice can't be parsed as name(rank), or
   *                             the overall collection of choices doesn't fit the pattern above.
   */
  public static void validateIRVPreferenceHeaders(CSVRecord theLine, int startIndex, int numChoices,
                                                  int maxRank) throws IRVParsingException {
    final String prefix = "[validateIRVChoiceHeaders] ";
    final String errorMsg = "Invalid IRV choices header: ";

    final Set<String> names = new HashSet<>();

    try {
      // Iterate over the first-preference choices, expected to have distinct names.
      for (int i = startIndex; i < numChoices + startIndex; i++) {
        IRVPreference irv1 = new IRVPreference(theLine.get(i));

        // A repeated name, or a preference other than 1, is an error.
        if (!names.add(irv1.candidateName) || irv1.rank != 1) {
          throw new IRVParsingException(errorMsg+irv1);
        }
        // Iterate through all other ranks, checking that each IRVPreference has the expected choice
        // and rank.
        for (int r = 2; r <= maxRank; r++) {
          IRVPreference irv_nextRank = new IRVPreference(theLine.get(i + (r - 1) * numChoices));
          if (irv_nextRank.rank != r || !irv_nextRank.candidateName.equals(irv1.candidateName)) {
            throw new IRVParsingException(errorMsg+irv_nextRank);
          }
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      final String msg = "Insufficient choices in IRV choices header: ";
      LOGGER.error(String.format("%s %s", prefix, msg + theLine));
      throw new IRVParsingException(msg);

    } catch (IRVParsingException e) {
      LOGGER.error(String.format("%s %s", prefix, e.getMessage() + theLine));
      throw e;
    }
  }

  /**
   * This is the almost-inverse of validateIRVPreferenceHeaders. Given a list of (plain-name)
   * choices, and a number of allowed ranks, it generates the complete list of all name(rank)
   * strings, by iterating through ranks from 1 to maxRank and, within each rank, iterating through
   * all names.
   * @param choices the list of candidate names.
   * @param maxRank the maximum allowed rank. (Minimum is always 1.)
   * @return a list of IRV choices in the expected pattern, e.g.
   * Alice(1), Bob(1), Chuan(1), Alice(2), Bob(2), Chuan(2), Alice(3), Bob(3), Chuan(3) is valid,
   */
  public static List<Choice> generateAllIRVPreferences(List<Choice> choices, int maxRank) {
    List<Choice> allPrefs = new ArrayList<>();
    for(int rank = 1 ; rank <= maxRank ; rank++) {
      for (final Choice c : choices) {
        allPrefs.add(new Choice(
            c.name()+"("+rank+")",
            c.description(),
            c.qualifiedWriteIn(),
            c.fictitious()
        ));
      }
    }
    return allPrefs;
  }
}
