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

package au.org.democracydevelopers.corla.communication.requestToRaire;

import java.util.List;

/**
 * Abstract class that serves as a parent class for particular requests to raire. This class
 * identifies a contest by name and includes
 * - the total number of auditable ballots in the relevant auditing universe,
 * - the list of candidates by name.
 * The GetAssertionsRequest and GenerateAssertionsRequest types inherit from this class and add
 * some other fields.
 */
public abstract class ContestRequest {

  /**
   * The name of the contest
   */
  public final String contestName;

  /**
   * The total number of ballots in the universe under audit.
   * This may not be the same as the number of ballots or CVRs in the contest, if the contest
   * is available only to a subset of voters in the universe.
   */
  public final int totalAuditableBallots;

  /**
   * List of candidate names.
   */
  public final List<String> candidates;

  /**
   * All args constructor.
   * @param contestName the name of the contest
   * @param totalAuditableBallots the total number of auditable ballots.
   * @param candidates the list of candidates by name
   */
  protected ContestRequest(String contestName, int totalAuditableBallots,
                            List<String> candidates) {
    this.contestName = contestName;
    this.totalAuditableBallots = totalAuditableBallots;
    this.candidates = candidates;
  }
}
