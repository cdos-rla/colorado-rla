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

import au.org.democracydevelopers.corla.model.GenerateAssertionsSummary;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import us.freeandfair.corla.persistence.Persistence;

import javax.persistence.TypedQuery;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;

import static au.org.democracydevelopers.corla.endpoint.GenerateAssertions.UNKNOWN_WINNER;

/**
 * Database queries relating to the retrieval of Assertion Generation Summaries from the
 * database. It contains a method that executes a query to retrieve all GenerateAssertionsSummaries
 * belonging to a specific contest, identified by name.
 * Also includes a shortcut function to get the winner, which inserts UNKNOWN_WINNER if either the
 * record is absent, or the winner is blank.
 * (e.g. giving feedback about assertion generation) they can be safely deleted.
 */
public class GenerateAssertionsSummaryQueries {

  /**
   * Class-wide logger.
   */
  public static final Logger LOGGER = LogManager.getLogger(GenerateAssertionsSummaryQueries.class);

  /**
   * Retrieve the winner of an IRV contest matching the given contestName, or UNKNOWN_WINNER if
   * there is no record, or no winner in the record.
   * @param contestName the name of the contest.
   */
  public static String matchingWinner(final String contestName) {
    final Optional<GenerateAssertionsSummary> optSummary = matching(contestName);
    if(optSummary.isPresent() && !optSummary.get().getWinner().isBlank()) {
      return optSummary.get().getWinner();
    } else {
      return UNKNOWN_WINNER;
    }
  }

  /**
   * Retrieve all summaries in the database belonging to the contest with the given name.
   * @param contestName The contest name.
   * @return the (optional) summary of assertions defined for the contest.
   * @throws RuntimeException when an unexpected error arose in assertion retrieval (not including
   * a NoResultException, which is handled by returning an empty optional item).
   */
  public static Optional<GenerateAssertionsSummary> matching(final String contestName) throws RuntimeException {
    final String prefix = "[matching]";
    try {
      LOGGER.debug(String.format("%s Select query on generate assertions summary table, retrieving " +
          "summary for contest with name %s.", prefix, contestName));

      final Session s = Persistence.currentSession();
      final TypedQuery<GenerateAssertionsSummary> q = s.createQuery("select ca from GenerateAssertionsSummary ca "
            + " where ca.contestName = :contestName", GenerateAssertionsSummary.class);
      q.setParameter("contestName", contestName);

      final List<GenerateAssertionsSummary> result = q.getResultList();
      LOGGER.debug(String.format("%s %d summary results retrieved for contest %s.", prefix,
          result.size(), contestName));
      if(result.isEmpty()) {
        // No summary was present for this contest. This is expected if GenerateAssertions has not run.
        return Optional.empty();
      } else if(result.size() == 1) {
        // Expected unique summary, after a run of GenerateAssertions.
        return Optional.of(result.get(0));
      } else {
        // Duplicate summaries - not expected, since contestName should be unique in the Generate
        // Assertions Summary table.
        throw new RuntimeException("Duplicate summaries for contest " + contestName);
      }
    }
    catch(Exception e){
      final String msg = String.format("%s An error arose when attempting to retrieve summary data" +
          "for contest %s: %s", prefix, contestName, e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }
}
