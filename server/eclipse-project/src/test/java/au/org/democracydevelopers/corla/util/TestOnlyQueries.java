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

import au.org.democracydevelopers.corla.model.vote.IRVBallotInterpretation;
import java.util.Optional;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.query.Query;
import us.freeandfair.corla.model.CastVoteRecord;
import us.freeandfair.corla.model.CastVoteRecord.RecordType;
import us.freeandfair.corla.persistence.Persistence;

import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple database queries used only for testing.
 * - Get IRVBallotInterpretations matching a given contest name and record type.
 */
public class TestOnlyQueries {

  /**
   * Class-wide logger.
   */
  public static final Logger LOGGER = LogManager.getLogger(TestOnlyQueries.class);

  /**
   * Retrieve all IRVBallotInterpretations in the database belonging to the contest with the given
   * name, of the requested record type.
   *
   * @param contestName The contest name.
   * @param imprintedId The imprinted ID.
   * @param recordType The record type (UPLOADED or AUDITOR_ENTERED or REAUDIT).
   * @return the list of matching IRVBallotInterpretations. There may be none, or several, because
   *         imprinted IDs may be repeated between counties for one contest name.
   * @throws RuntimeException when an unexpected error arose in assertion retrieval (not including
   *                          a NoResultException, which is handled by returning an empty optional item).
   */
  public static List<IRVBallotInterpretation> matching(final String contestName,
      final String imprintedId, final CastVoteRecord.RecordType recordType) throws RuntimeException {
    final String prefix = "[matching]";
    try {
      LOGGER.debug(String.format("%s Select query on IRV Ballot interpretations, retrieving " +
          "all for contest with name %s, of recordType %s.", prefix, contestName, recordType));

      final Session s = Persistence.currentSession();
      final TypedQuery<IRVBallotInterpretation> q = s.createQuery("select bi from IRVBallotInterpretation bi "
          + " where bi.contest.my_name = :contestName AND bi.imprintedID = :imprintedId " +
          "AND bi.recordType = :recordType", IRVBallotInterpretation.class);
      q.setParameter("contestName", contestName);
      q.setParameter("imprintedId", imprintedId);
      q.setParameter("recordType", recordType);

      final List<IRVBallotInterpretation> result = q.getResultList();
      LOGGER.debug(String.format("%s %d summary results retrieved for contest %s.", prefix,
          result.size(), contestName));
      if(result.isEmpty()) {
        // No summary was present for this contest. This is expected if GenerateAssertions has not run.
        return new ArrayList<>();
      } else {
        // Expected unique summary, after a run of GenerateAssertions.
        return result;
      }
    }
    catch(Exception e){
      final String msg = String.format("%s An error arose when attempting to retrieve IRV Ballot " +
          "intepretations for contest %s: %s", prefix, contestName, e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * Retrieve all IRVBallotInterpretations in the database belonging to the contest with the given
   * name, on a given CVR, of the requested record type. The only time there should be more than one
   * is if the same CVR has been reaudited multiple times, in which case there may be several with
   * recordType = "REAUDITED". Otherwise, there should be at most one.
   * @param cvrNumber   CVR number
   * @param contestID The contest ID.
   * @param imprintedId The imprinted ID.
   * @param recordType The record type (UPLOADED or AUDITOR_ENTERED or REAUDIT).
   * @return the list of matching IRVBallotInterpretations. There may be none, or several.
   * @throws RuntimeException when an unexpected error arose in assertion retrieval (not including
   *                          a NoResultException, which is handled by returning an empty optional item).
   */
  public static List<IRVBallotInterpretation> matching(final int cvrNumber,
                              final Long contestID, final String imprintedId,
                              final CastVoteRecord.RecordType recordType) throws RuntimeException {
    final String prefix = "[matching]";
    try {
      LOGGER.debug(String.format("%s Select query on IRV Ballot interpretations, retrieving " +
          "all for contest with name %s, cvr number %d, of recordType %s.", prefix, contestID,
          cvrNumber, recordType));

      final Session s = Persistence.currentSession();
      final TypedQuery<IRVBallotInterpretation> q = s.createQuery("select bi from IRVBallotInterpretation bi "
          + " where bi.cvrNumber = :cvrNumber AND bi.contest.id = :contestID AND bi.imprintedID = :imprintedId " +
          "AND bi.recordType = :recordType", IRVBallotInterpretation.class);
      q.setParameter("contestID", contestID);
      q.setParameter("cvrNumber", cvrNumber);
      q.setParameter("imprintedId", imprintedId);
      q.setParameter("recordType", recordType);

      final List<IRVBallotInterpretation> result = q.getResultList();
      LOGGER.debug(String.format("%s %d summary results retrieved for contest %s, cvr number %d.", prefix,
          result.size(), contestID, cvrNumber));
      if(result.isEmpty()) {
        // No summary was present for this contest. This is expected if GenerateAssertions has not run.
        return new ArrayList<>();
      } else {
        // Expected unique summary, after a run of GenerateAssertions.
        return result;
      }
    }
    catch(Exception e){
      final String msg = String.format("%s An error arose when attempting to retrieve IRV Ballot " +
          "intepretations for contest %s, cvr number %d: %s", prefix, contestID, cvrNumber, e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }
}
