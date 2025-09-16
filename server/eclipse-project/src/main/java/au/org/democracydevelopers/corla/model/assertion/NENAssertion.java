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

import java.util.List;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import us.freeandfair.corla.model.CVRContestInfo;

/**
 * A Not Eliminated Next (NEN) assertion is defined between two candidates W and L in the context
 * where an assumed set of candidates (including W and L) are 'continuing'. What this assertion
 * says is that in the context where everyone outside the assumed 'continuing' list has been
 * eliminated, W has more votes than L. This means that in the context where only the assumed
 * continuing candidates remain, W will not be eliminated next as L will have fewer votes.
 */
@Entity
@DiscriminatorValue("NEN")
public class NENAssertion extends Assertion {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(NENAssertion.class);

  /**
   * Construct an empty NEN assertion (for persistence).
   */
  public NENAssertion(){
    super();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected int score(final CVRContestInfo info){
    final String prefix = "[score]";

    // Reduce the list of choices in 'info' to those that are assumed to be continuing.
    final List<String> choices_left = info.choices().stream().filter(c ->
        assumedContinuing.contains(c)).toList();

    int score = 0;

    // If none of the candidates relevant to the assertion are on the vote, then
    // return 0.
    if (!choices_left.isEmpty()) {
      // If our winner is the first candidate in 'choices_left' our score is 1.
      if (choices_left.get(0).equals(winner)) {
        score = 1;
      }
      // If our loser is the first candidate in 'choices_left' our score is -1.
      else if (choices_left.get(0).equals(loser)) {
        score = -1;
      }
    }
    LOGGER.debug(String.format("%s Score of %d computed for NEN Assertion ID %d, contest %s, vote %s",
        prefix, score, id(), contestName, info.choices()));
    return score;
  }

  /**
   * {@inheritDoc}
   */
  public String getDescription(){
    return String.format("%s NEN %s assuming (%s) are continuing: oneOver = %d; twoOver = %d; " +
        "oneUnder = %d, twoUnder = %d; other = %d; optimistic = %d; estimated = %d; risk %f.",
        winner, loser, assumedContinuing, oneVoteOverCount, twoVoteOverCount, oneVoteUnderCount,
        twoVoteUnderCount, otherCount, optimisticSamplesToAudit, estimatedSamplesToAudit, currentRisk);
  }

}
