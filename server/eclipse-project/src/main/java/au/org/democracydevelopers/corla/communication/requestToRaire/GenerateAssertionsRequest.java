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

import org.apache.log4j.LogManager;

import java.util.List;

/**
 * Request (expected to be json) identifying a contest by name and listing other data:
 * - the total number of auditable ballots (used to calculate difficulty in raire),
 * - the time limit allowed to raire,
 * - the candidates (by name).
 * This is used directly for requesting assertion generation, and is identical to ContestRequest
 * in raire-service.
 */
public class GenerateAssertionsRequest extends ContestRequest {

  /**
   * Class-wide logger
   */
  private static final org.apache.log4j.Logger LOGGER
          = LogManager.getLogger(GenerateAssertionsRequest.class);

  /**
   * The elapsed time allowed to raire to generate the assertions, in seconds.
   */
  public final double timeLimitSeconds;

  /**
   * All args constructor
   * @param timeLimitSeconds the elapsed time allowed for RAIRE to generate assertions, in seconds.
   * @param contestName the name of the contest
   * @param totalAuditableBallots the total number of auditable ballots.
   * @param candidates the list of candidates by name
   */
  public GenerateAssertionsRequest(String contestName, int totalAuditableBallots,
                                   double timeLimitSeconds, List<String> candidates) {
    super(contestName, totalAuditableBallots, candidates);

    final String prefix = "[GenerateAssertionsRequest constructor]";
    LOGGER.debug(String.format("%s Making GenerateAssertionsRequest for contest %s", prefix,
        contestName));

    this.timeLimitSeconds = timeLimitSeconds;
  }
}
