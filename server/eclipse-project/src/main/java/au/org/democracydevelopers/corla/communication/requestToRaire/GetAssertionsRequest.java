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

import java.beans.ConstructorProperties;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request (expected to be json) identifying the contest for which assertions should be retrieved
 * from the database (expected to be exported as json).
 * Identical data to GetAssertionsRequest in raire-service.
 * This extends ContestRequest and uses the contest name, totalAuditable ballots and candidate
 * list from there.
 * A GetAssertionsRequest identifies a contest by name along with the candidate list
 * (which is necessary for producing the metadata for later visualization). The riskLimit states the
 * risk limit for the audit. This is not actually used in raire-service computations,
 * but will be output later with the assertion export, so that it can be used in the assertion
 * visualizer.
 */
public class GetAssertionsRequest extends ContestRequest {

  /**
   * Class-wide logger
   */
  private static final org.apache.log4j.Logger LOGGER =
      LogManager.getLogger(GetAssertionsRequest.class);

  /**
   * The risk limit for the audit, expected to be in the range [0,1]. Defaults to zero, because
   * then we know we will never mistakenly claim the risk limit has been met.
   */
  public final BigDecimal riskLimit;

  /**
   * All args constructor.
   * @param contestName           the name of the contest
   * @param totalAuditableBallots the total number of ballots in the universe.
   * @param candidates            a list of candidates by name
   * @param riskLimit             the risk limit for the audit, expected to be in the range [0,1].
   */
  @ConstructorProperties({"contestName", "totalAuditableBallots", "candidates", "winner", "riskLimit"})
  public GetAssertionsRequest(String contestName, int totalAuditableBallots, List<String> candidates,
                              BigDecimal riskLimit) {
    super(contestName, totalAuditableBallots, candidates);

    final String prefix = "[GetAssertionsRequest constructor]";
    LOGGER.debug(String.format("%s Making GetAssertionsRequest for contest %s", prefix,
        contestName));

    this.riskLimit = riskLimit;
  }
}
