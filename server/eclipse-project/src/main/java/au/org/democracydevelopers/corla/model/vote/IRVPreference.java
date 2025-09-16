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

import org.apache.commons.csv.CSVRecord;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import us.freeandfair.corla.model.Choice;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A single IRV name-rank pair, part of an IRV vote (which may have several name-rank pairs).
 * - The constructor parses the name-rank pair in the format expected in CO CVRs, that is,
 *   name(rank), where the rank is a positive integer.
 * - The comparator compares them by rank, ignoring the name.
 */
public class IRVPreference implements Comparable<IRVPreference> {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(IRVPreference.class);

  public final int rank;
  public final String candidateName;

  /**
   * All args constructor
   * Constructs an IRVPreference with the given rank and candidate name.
   * @param r the selected rank (a positive integer)
   * @param name the candidate's name
   */
  public IRVPreference(int r, String name) {
    rank = r;
    candidateName = name;
  }

  /**
   * Constructor from a csv string indicating an IRV choice: candidate name with parenthesized rank.
   * Extracts the name and rank of an IRV preference, provided as a string, and constructs an
   * IRVPreference. The given string should contain a candidate name followed by a parenthesized
   * rank.
   * @param nameWithRank - a string expected to be of the form "name(rank)".
   * @throws IRVParsingException if the string cannot be parsed in the "name(digits)" pattern.
   */
  public IRVPreference(String nameWithRank) throws IRVParsingException {
    // Use strip() instead of trim() for unicode awareness.
    String trimmed = nameWithRank.strip();

    try {
      // Look for digits in parentheses at the end of the string.
      // Split the string into name ([0]) and rank ([1]). Take the first element as candidate name.
      String name = trimmed.split("\\((\\s*\\d+\\s*\\))\\s*$")[0];
      // Remove exactly the candidate-name substring, from the original trimmed string.
      String rankWithParentheses = trimmed.replace(name, "");
      // Get rid of the parentheses - just take the digits inside.
      String rankString = rankWithParentheses.trim().split("[()]")[1];

      candidateName = name.trim();
      rank = Integer.parseInt(rankString.strip());

      // If we got nonsense values, we didn't parse it properly.
      if(candidateName.isBlank() || rank <= 0) {
        throw new IRVParsingException();
      }
    } catch (NumberFormatException | IndexOutOfBoundsException | IRVParsingException e) {
      final String prefix = "[IRVChoices constructor]";
      final String errorMessage = "Could not parse candidate-preference: ";
      LOGGER.error(String.format("%s %s %s", prefix, errorMessage, nameWithRank), e);
      throw new IRVParsingException(errorMessage + nameWithRank);
    }
  }

  /**
   * Compares this, and the given IRVPreference 'preference', in terms of rank. This comparison
   * does not consider the candidate names of the two preferences, only their integer ranks.
   */
  @Override
  public int compareTo(IRVPreference preference) {
    return Integer.compare(rank, preference.rank);
  }

  /**
   * @return the IRVPreference as a human-readable string with the candidate name followed by the
   * rank in parentheses, e.g. "Diego(1)".
   */
  public String toString() {
    return candidateName+"("+rank+")";
  }
}