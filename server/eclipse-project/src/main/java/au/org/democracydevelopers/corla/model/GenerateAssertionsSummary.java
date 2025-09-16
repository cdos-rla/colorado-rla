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

package au.org.democracydevelopers.corla.model;

import au.org.democracydevelopers.corla.query.GenerateAssertionsSummaryQueries;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.persistence.PersistentEntity;

import javax.persistence.*;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.min;


/**
 * RAIRE (raire-java) generates a set of assertions for a given IRV contest, but it also returns
 * the winner and (possibly) an informative error. These are stored in the database, which
 * colorado-rla needs to read in order to produce IRV reports. This is read-only table here, with
 * data identical to the corresponding class in raire-service.
 */
@Entity
@Table(name = "generate_assertions_summary")
public class GenerateAssertionsSummary implements PersistentEntity {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(GenerateAssertionsSummary.class);

  /**
   * ID.
   */
  @Id
  @Column(updatable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  /**
   * Version. Used for optimistic locking.
   */
  @Version
  @Column(name = "version", updatable = false, nullable = false)
  private long version;

  /**
   * Name of the contest.
   */
  @Column(name = "contest_name", unique = true, updatable = false, nullable = false)
  private String contestName;

  /**
   * Name of the winner of the contest, as determined by raire-java.
   */
  @Column(name = "winner", updatable = false, nullable = false)
  private String winner;

  /**
   * An error (matching one of the RaireServiceErrors.RaireErrorCodes), if there was one. Errors
   * mean there are no assertions (nor winner), but some warnings
   * (e.g. TIME_OUT_TRIMMING_ASSERTIONS) do have assertions and a winner, and allow the audit to
   * continue.
   */
  @Column(name = "error", updatable = false, nullable = false)
  private String error;

  /**
   * A warning, if there was one, or emptystring if none. Warnings (e.g. TIME_OUT_TRIMMING_ASSERTIONS)
   * mean that assertion generation succeeded and the audit can continue, but re-running with longer
   * time allowed might be beneficial.
   */
  @Column(name = "warning", updatable = false, nullable = false)
  private String warning;

  /**
   * The message associated with the error, for example the names of the tied winners.
   */
  @Column(name = "message", updatable = false, nullable = false)
  private String message;

  /**
   * Default no-args constructor (required for persistence).
   */
  public GenerateAssertionsSummary() {
  }

  /**
   * @return the contest name.
   */
  public String getContestName() {
    return contestName;
  }

  /**
   * @return the winner.
   */
  public String getWinner() {
    return winner;
  }

  /**
   * @return the error, or emptystring if there is none.
   */
  public String getError() {
    return error;
  }

  /**
   * @return a warning, or emptystring if there is none.
   */
  public String getWarning() {
    return warning;
  }

  /**
   * @return the message associated with the error, or emptystring if there is none.
   */
  public String getMessage() {
    return message;
  }

  @Override
  public Long id() {
    return id;
  }

  @Override
  public void setID(Long the_id) {
    id = the_id;
  }

  @Override
  public Long version() {
    return version;
  }
}