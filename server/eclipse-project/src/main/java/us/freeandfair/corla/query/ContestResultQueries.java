/** Copyright (C) 2018 the Colorado Department of State  **/

package us.freeandfair.corla.query;

import java.util.Optional;

import org.hibernate.query.Query;
import org.hibernate.Session;

import us.freeandfair.corla.model.ContestResult;
import us.freeandfair.corla.persistence.Persistence;


public final class ContestResultQueries {
  /**
   * prevent construction
   */
  private ContestResultQueries() {
  }

  /**
   * Return the ContestResult with the contestName given or create a new
   * ContestResult with the contestName.
   **/
  public static ContestResult findOrCreate(final String contestName) {
    final Session s = Persistence.currentSession();
    final Query q = s.createQuery("select cr from ContestResult cr " +
                                  "where cr.contestName = :contestName");
    q.setParameter("contestName", contestName);
    final Optional<ContestResult> contestResultMaybe = q.uniqueResultOptional();
    if (contestResultMaybe.isPresent()) {
      return contestResultMaybe.get();
    } else {
      final ContestResult cr = new ContestResult(contestName);
      Persistence.save(cr);
      return cr;
    }
  }

  /**
   * Return an Optional ContestResult, which is Present if a contest of the requested name is in
   * the database.
   * @param contestName the name of the contest.
   * @return an Optional<ContestResult>, containing the contest requested by name if present,
   * otherwise empty.
   */
  public static Optional<ContestResult> find(final String contestName) {
    final Session s = Persistence.currentSession();
    final Query<ContestResult> q = s.createQuery("select cr from ContestResult cr " +
        "where cr.contestName = :contestName", ContestResult.class);
    q.setParameter("contestName", contestName);
    return q.uniqueResultOptional();
  }

  /**
   * Return the ContestResult with the contestName given or create a new
   * ContestResult with the contestName.
   **/
  public static Integer count() {
    final Session s = Persistence.currentSession();
    final Query q = s.createQuery("select count(cr) from ContestResult cr ");
    return ((Long)q.uniqueResult()).intValue();
  }

}
