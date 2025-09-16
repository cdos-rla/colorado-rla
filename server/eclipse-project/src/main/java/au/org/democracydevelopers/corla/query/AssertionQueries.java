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

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.hibernate.Session;
import au.org.democracydevelopers.corla.model.assertion.Assertion;
import us.freeandfair.corla.persistence.Persistence;

import javax.persistence.TypedQuery;

/**
 * This class contains database queries relating to the retrieval of Assertions from the
 * database. It contains a method that executes a query to retrieval all Assertions belonging
 * to a specific contest, identified by name.
 */
public class AssertionQueries {

  /**
   * Class-wide logger.
   */
  public static final Logger LOGGER = LogManager.getLogger(AssertionQueries.class);

  /**
   * Retrieve all assertions in the database belonging to the contest with the given name.
   *
   * @param contestName The contest name.
   * @return the list of assertions defined for the contest.
   * @throws RuntimeException when an unexpected error arose in assertion retrieval (not including
   * a NoResultException, which is handled by returning an empty list).
   */
  public static List<Assertion> matching(final String contestName) throws RuntimeException {
    final String prefix = "[matching]";
    try {
      LOGGER.debug(String.format("%s Select query on assertion table, retrieving all " +
          "assertions for contest with name %s.", prefix, contestName));

      final Session s = Persistence.currentSession();
      final TypedQuery<Assertion> q = s.createQuery("select ca from Assertion ca "
            + " where ca.contestName = :contestName", Assertion.class);
      q.setParameter("contestName", contestName);

      List<Assertion> result = q.getResultList();
      LOGGER.debug(String.format("%s %d assertions retrieved for contest %s.", prefix,
          result.size(), contestName));
      return result;

    } catch (javax.persistence.NoResultException e) {
      final String msg = String.format("%s No assertions retrieved for contest %s.", prefix,
          contestName);
      LOGGER.warn(msg);
      return new ArrayList<>();
    }
    catch(Exception e){
      final String msg = String.format("%s An error arose when attempting to retrieve assertions " +
          "for contest %s: %s", prefix, contestName, e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }
}
