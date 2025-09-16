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

package au.org.democracydevelopers.corla.model.assertion;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import us.freeandfair.corla.model.CVRContestInfo;

/**
 * A Not Eliminated Before (NEB) assertion compares the tallies of two candidates W and L in
 * the context where W is given all votes on which they are ranked first, and L is given all
 * votes on which they appear before W (or they appear and W does not). This equates to comparing
 * the minimum possible tally for W against the maximum possible tally for L while W is still
 * continuing. If this assertion holds, it means that W can never be eliminated while L is
 * still continuing, or that W will always have more votes than L.
 */
@Entity
@DiscriminatorValue("NEB")
public class NEBAssertion extends Assertion {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(NEBAssertion.class);

  /**
   * Construct an empty NEB assertion (for persistence).
   */
  public NEBAssertion(){
    super();
  }

  /**
   * {@inheritDoc}
   */
  protected int score(final CVRContestInfo info){
    final String prefix = "[score]";

    // Get index of winner and loser in this CVR/ballot's ranking
    final int winner_index = info.choices().indexOf(winner);
    final int loser_index = info.choices().indexOf(loser);

    int score = 0;

    // If our winner is the first ranked candidate, we return a score of 1.
    if (winner_index == 0){
      score = 1;
    }
    // If our winner is not mentioned on the ballot, but the loser is, we return a score of -1.
    // We also return a score of -1 if our loser is ranked higher than our winner.
    else if(loser_index != -1 && (winner_index == -1 || loser_index < winner_index)){
        score = -1;
    }

    LOGGER.debug(String.format("%s Score of %d computed for NEB Assertion ID %d, contest %s, vote %s",
        prefix, score, id(), contestName, info.choices()));
    return score;
  }


  /**
   * {@inheritDoc}
   */
  public String getDescription(){
    return String.format("%s NEB %s: oneOver = %d; twoOver = %d; oneUnder = %d, twoUnder = %d; " +
        "other = %d; optimistic = %d; estimated = %d; risk %f.", winner, loser, oneVoteOverCount,
        twoVoteOverCount, oneVoteUnderCount, twoVoteUnderCount, otherCount, optimisticSamplesToAudit,
        estimatedSamplesToAudit, currentRisk);
  }
}
