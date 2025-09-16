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

import static java.util.Collections.min;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import javax.persistence.*;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import us.freeandfair.corla.math.Audit;
import us.freeandfair.corla.model.CVRAuditInfo;
import us.freeandfair.corla.model.CVRContestInfo;
import us.freeandfair.corla.model.CVRContestInfo.ConsensusValue;
import us.freeandfair.corla.model.CastVoteRecord;
import us.freeandfair.corla.model.CastVoteRecord.RecordType;
import us.freeandfair.corla.model.ComparisonAudit;
import us.freeandfair.corla.persistence.PersistentEntity;

/**
 * An assertion is a statement comparing the tallies of two candidates in varying contexts.
 * A RAIRE audit of an IRV contest involves checking a finite set of assertions. If we verify
 * that the set of assertions holds (with the desired degree of confidence) then we have ruled
 * out all possible outcomes in which the reported winner did not win. Each assertion is associated
 * with an expected number of ballots to sample when checking in an audit, and will have an
 * associated risk. Subclasses of this abstract base class are defined for the different types
 * of RAIRE assertion. Assertions are created and stored in the database by raire-service. They
 * are audited by colorado-rla. During this process, the record of discrepancies attached to
 * the assertion will be updated, as well as its estimated sample sizes and risk.
 *
 * A NOTE ON DISCREPANCY MANAGEMENT:
 * The class level comment of the IRVComparisonAudit class describes how discrepancies are
 * managed by ComparisonAudit's.
 *
 * Of particular note for assertions:
 * If a discrepancy is found with respect to an assertion, the CVR ID and discrepancy type will be
 * stored in its cvrDiscrepancy map. Each assertion has its own recordDiscrepancy() and
 * removeDiscrepancy() method. When their recordDiscrepancy() method is called, its cvrDiscrepancy
 * map will be looked up to find the right discrepancy type, and the assertion's internal discrepancy
 * totals updated. Similarly, when removeDiscrepancy() is called, the cvrDiscrepancy map will be
 * looked up to find the discrepancy type, and the assertion's discrepancy totals updated. The CVR
 * ID - discrepancy type entry in its cvrDiscrepancy() map is not removed, however, it is only
 * removed if computeDiscrepancy() is called again and either a different or no discrepancy is found.
 * Note that recordDiscrepancy() and removeDiscrepancy() is designed to be called N times for a
 * particular CVR/audited ballot pair if that ballot appears N times in the sample.
 */
@Entity
@Table(name = "assertion")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "assertion_type")
public abstract class Assertion implements PersistentEntity {

  /**
   * Number of decimal places to report Assertion risk level.
   */
  public final static int RISK_DECIMALS = 3;

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(Assertion.class);

  /**
   * Assertion ID.
   */
  @Id
  @Column(updatable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * The version (for optimistic locking).
   */
  @Version
  private Long version;

  /**
   * Name of contest for which this Assertion was generated.
   */
  @Column(name = "contest_name", updatable = false, nullable = false)
  protected String contestName;

  /**
   * Winner of the assertion (a candidate in the contest).
   */
  @Column(name = "winner", updatable = false, nullable = false)
  protected String winner;

  /**
   * Loser of the assertion (a candidate in the contest).
   */
  @Column(name = "loser", updatable = false, nullable = false)
  protected String loser;

  /**
   * Raw margin of the assertion.
   */
  @Column(name = "margin", updatable = false, nullable = false)
  protected int margin;

  /**
   * Diluted margin of the assertion. (This could be a double. For consistency with the existing
   * colorado-rla code base, and the implementation of the methods provided in Audit, we are using a
   * BigDecimal).
   */
  @Column(name = "diluted_margin", updatable = false, nullable = false,
      precision = ComparisonAudit.PRECISION, scale = ComparisonAudit.SCALE)
  protected BigDecimal dilutedMargin = BigDecimal.ZERO;

  /**
   * Assertion difficulty, as estimated by raire-java.
   */
  @Column(name = "difficulty", updatable = false, nullable = false,
      precision = ComparisonAudit.PRECISION, scale = ComparisonAudit.SCALE)
  protected double difficulty = 0;

  /**
   * List of candidates that the assertion assumes are 'continuing' in the assertion's context.
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "assertion_assumed_continuing", joinColumns = @JoinColumn(name = "id"))
  @Column(name = "assumed_continuing", updatable = false, nullable = false)
  protected List<String> assumedContinuing = new ArrayList<>();

  // The attributes above are established by raire-service when the assertion is created and
  // stored in the database. The attributes below are updatable by colorado-rla during an audit.

  /**
   * Map between CVR ID and the discrepancy calculated for it (and its A-CVR) in the context of this
   * assertion, based on the last call to computeDiscrepancy(). Calls to computeDiscrepancy() will
   * update this map. This data is stored in the table "assertion_cvr_discrepancy" which has columns
   * "id,crv_id,discrepancy", where "id" corresponds to this Assertion's ID, "cvr_id" to the ID of
   * the CVR that is involved in the discrepancy, and "discrepancy" the value of the discrepancy
   * from -2 to 2.
   */
  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "assertion_discrepancies", joinColumns = @JoinColumn(name = "id"))
  @MapKeyColumn(name = "cvr_id")
  @Column(name = "discrepancy", nullable = false)
  protected Map<Long, Integer> cvrDiscrepancy = new HashMap<>();

  /**
   * The estimated number of samples we expect to need to audit this assertion, assuming no further
   * overstatements arise.
   */
  @Column(name = "optimistic_samples_to_audit", nullable = false)
  protected Integer optimisticSamplesToAudit = 0;

  /**
   * The estimated number of samples we expect to need to audit this assertion, assuming
   * overstatement continue at the current rate.
   */
  @Column(name = "estimated_samples_to_audit", nullable = false)
  protected Integer estimatedSamplesToAudit = 0;

  /**
   * The number of two-vote understatements recorded against this assertion so far.
   */
  @Column(name = "two_vote_under_count", nullable = false)
  protected Integer twoVoteUnderCount = 0;

  /**
   * The number of one-vote understatements recorded against this assertion so far.
   */
  @Column(name = "one_vote_under_count", nullable = false)
  protected Integer oneVoteUnderCount = 0;

  /**
   * The number of one-vote overstatements recorded against this assertion so far.
   */
  @Column(name = "one_vote_over_count", nullable = false)
  protected Integer oneVoteOverCount = 0;

  /**
   * The number of two-vote overstatements recorded against this assertion so far.
   */
  @Column(name = "two_vote_over_count", nullable = false)
  protected Integer twoVoteOverCount = 0;

  /**
   * The number of discrepancies recorded so far, against this assertion, that are neither
   * understatements nor overstatements.
   */
  @Column(name = "other_count", nullable = false)
  protected Integer otherCount = 0;

  /**
   * Current risk of the assertion. We initialize this risk to 1, as when we have no information we
   * assume maximum risk.
   */
  @Column(name = "current_risk", nullable = false,
      precision = ComparisonAudit.PRECISION, scale = ComparisonAudit.SCALE)
  protected BigDecimal currentRisk = BigDecimal.valueOf(1);

  /**
   * Construct an empty assertion (required for persistence). Note that creation and storage of
   * assertions is the responsibility of raire-service.
   */
  public Assertion() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long id() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setID(final Long theId) {
    id = theId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long version() {
    return version;
  }

  /**
   * Get the assertion's diluted margin.
   */
  public BigDecimal getDilutedMargin(){
    return dilutedMargin;
  }

  /**
   * Get the assertion's absolute margin.
   */
  public int getMargin() { return margin; }

  /**
   * Get an (unmodifiable) map of the assertion's CVR ID-discrepancy records. This is used
   * for testing purposes.
   */
  public Map<Long,Integer> getCvrDiscrepancy(){
    return Collections.unmodifiableMap(cvrDiscrepancy);
  }

  /**
   * Returns the value of the discrepancy, if any, that has been computed against the CVR with
   * the given ID, as an OptionalInt. An empty OptionalInt will be returned if no discrepancy
   * has been computed against the CVR for this assertion.
   * @param cvrID CVR ID
   * @return The value of the disrepancy, if one exists, computed against the given CVR for this
   * assertion.
   */
  public OptionalInt getDiscrepancy(long cvrID){
    if(cvrDiscrepancy.containsKey(cvrID)){
      return OptionalInt.of(cvrDiscrepancy.get(cvrID));
    }
    return OptionalInt.empty();
  }

  /**
   * Return a string representation of a subset of this assertion's data. This is used for
   * testing purposes.
   */
  public abstract String getDescription();

  /**
   * Given a number of audited samples, and the total number of overstatements experienced thus far,
   * increase the sample count by a scaling factor. The scaling factor grows as the ratio of
   * overstatements to samples increases. This method could be directly moved to
   * us.freeandfair.corla.math.Audit, and called within this class. The method is almost a direct
   * copy of a private scalingFactor() method in us.freeandfair.corla.ComparisonAudit. If this
   * scalingFactor method is moved to math.Audit, then that private method in ComparisonAudit can be
   * deleted, with calls replaced with calls to the public static method in math.Audit.
   *
   * @return the given number of audited samples increased by a scaling factor.
   */
  private static BigDecimal scalingFactor(BigDecimal auditedSamples, BigDecimal overstatements) {
    if (auditedSamples.equals(BigDecimal.ZERO)) {
      return BigDecimal.ONE;
    } else {
      return BigDecimal.ONE.add(overstatements.divide(auditedSamples, MathContext.DECIMAL128));
    }
  }

  /**
   * For the given risk limit, compute the expected (optimistic) number of samples to audit for this
   * assertion. This calculation assumes that no further overstatements will arise. This method
   * updates the Assertion::optimisticSamplesToAudit attribute with a call to Audit.optimistic()
   * and then returns the new optimisticSamplesToAudit value.
   *
   * @param riskLimit The risk limit of the audit.
   * @return The (optimistic) number of samples we expect we will need to sample to audit this
   * assertion.
   */
  public Integer computeOptimisticSamplesToAudit(BigDecimal riskLimit) {
    final String prefix = "[computeOptimisticSamplesToAudit]";

    LOGGER.debug(String.format("%s Calling Audit::optimistic() with parameters: risk limit " +
            "%f; diluted margin %f; gamma %f; two vote under count %d; one vote under count %d; " +
            "one vote over count %d; two vote over count %d.", prefix, riskLimit, dilutedMargin,
            Audit.GAMMA, twoVoteUnderCount, oneVoteUnderCount, oneVoteOverCount, twoVoteOverCount));

    // Call the colorado-rla audit math; update optimistic_samples_to_audit and return new value.
    optimisticSamplesToAudit = Audit.optimistic(riskLimit, dilutedMargin, Audit.GAMMA,
        twoVoteUnderCount, oneVoteUnderCount, oneVoteOverCount, twoVoteOverCount).intValue();

    LOGGER.debug(String.format("%s Computed optimistic samples to audit for Assertion %d" +
        " of %s ballots.", prefix, id, optimisticSamplesToAudit));

    return optimisticSamplesToAudit;
  }

  /**
   * For the given risk limit, compute the expected initial (optimistic) number of samples to audit
   * for this assertion. This calculation calls Audit::optimistic() with zero's for each discrepancy
   * count. This method does not change any of this assertion's internal sample size attributes.
   *
   * @param riskLimit The risk limit of the audit.
   * @return The initial (optimistic) number of samples we expect we will need to sample to
   * audit this assertion.
   */
  public Integer computeInitialOptimisticSamplesToAudit(BigDecimal riskLimit){
    final String prefix = "[computeInitialOptimisticSamplesToAudit]";

    LOGGER.debug(String.format("%s Calling Audit::optimistic() with parameters: risk limit " +
            "%f; diluted margin %f; gamma %f; two vote under count 0; one vote under count 0; " +
            "one vote over count 0; two vote over count 0.", prefix, riskLimit, dilutedMargin, Audit.GAMMA));

    // Call the colorado-rla audit math; update optimistic_samples_to_audit and return new value.
    final int initialOptimistic = Audit.optimistic(riskLimit, dilutedMargin, Audit.GAMMA,
        0, 0, 0, 0).intValue();

    LOGGER.debug(String.format("%s Computed initial optimistic samples to audit for Assertion %d" +
        " of %s ballots.", prefix, id, initialOptimistic));

    return initialOptimistic;
  }

  /**
   * Compute and return the estimated number of samples to audit for this assertion, given the
   * number of ballots audited thus far and number of observed overstatements. This method takes
   * the current optimistic number of ballots to sample for the assertion, and applies a scaling
   * factor to it. This scaling factor is computed based on the total ballots audited thus far,
   * and the number of observed overstatements. This method updates the assertion's internal record
   * of estimated samples to audit.
   * @param auditedSampleCount Number of ballots audited thus far.
   * @return The estimated number of ballots to audit for this assertion assuming overstatements
   * continue at the current observed rate.
   */
  public Integer computeEstimatedSamplesToAudit(int auditedSampleCount) {
    final String prefix = "[computeEstimatedSamplesToAudit]";
    final int totalOverstatements = oneVoteOverCount + twoVoteOverCount;

    if (totalOverstatements == 0) {
      estimatedSamplesToAudit = optimisticSamplesToAudit;

      LOGGER.debug(String.format("%s No overstatements thus far; estimated ballot samples (%d) " +
              "equals optimistic samples (%d). Assertion ID %d, contest %s.", prefix,
              estimatedSamplesToAudit, optimisticSamplesToAudit, id, contestName));
    }
    else {
      // Compute scaling factor (based on rate of observed overstatements to audited ballots).
      final BigDecimal scalingFac = scalingFactor(BigDecimal.valueOf(auditedSampleCount),
          BigDecimal.valueOf(totalOverstatements));

      estimatedSamplesToAudit = BigDecimal.valueOf(optimisticSamplesToAudit)
              .multiply(scalingFac).setScale(0, RoundingMode.CEILING).intValue();

      LOGGER.debug(String.format("%s %d overstatements thus far; scaling factor of %f applied to " +
              "optimistic sample count of %d; estimate sample count is %d ballots " +
              "(Assertion ID %d, contest %s).", prefix, totalOverstatements, scalingFac,
              optimisticSamplesToAudit, estimatedSamplesToAudit, id, contestName));
    }

    return estimatedSamplesToAudit;
  }

  /**
   * Computes any discrepancy that exists between the given CVR and its matching paper ballot in
   * the context of this assertion. A discrepancy has one of several values: 1 (a one vote
   * overstatement); 2 (a two vote overstatement); -1 (a one vote understatement); -2 (a two vote
   * understatement)l or 0 (an "other" discrepancy). The "other" discrepancy means that the
   * recorded vote on the CVR and audited ballot for the assertion's contest differs, but that this
   * difference has no bearing on the assertion itself.
   * This function will first test the special case where this assertion's contest is not on either
   * the CVR record or the audited ballot. In this case, there can be no discrepancy, and an empty
   * OptionalInt is returned.
   * This function will then compute a score for the CVR and a score for the audited ballot in
   * relation to this assertion. The discrepancy is equal to CVR score - audited ballot score. If
   * zero results, we check if this occurred because there was no difference in the votes
   * recorded on the CVR and audited ballot. If so, we return an empty OptionalInt indicating
   * that there is no discrepancy. Otherwise, we wrap the discrepancy in an OptionalInt and return.
   * A RuntimeException will be thrown if a null 'cvr' or 'auditedCVR' is provided.
   * @param cvr        The CVR that the machine saw.
   * @param auditedCVR The ACVR that the human audit board saw.
   * @return an optional int that is present if there is a discrepancy and absent otherwise.
   */
  public OptionalInt computeDiscrepancy(final CastVoteRecord cvr, final CastVoteRecord auditedCVR) {
    final String prefix = "[computeDiscrepancy]";

    if(cvr == null || auditedCVR == null){
      // This should never happen, and indicates an error has occurred somewhere.
      final String msg = String.format("%s A null CVR/audited ballot record has been passed to " +
          "the discrepancy computation method for Assertion ID %d, contest %s.", prefix, id, contestName);
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }

    LOGGER.debug(String.format("%s Computing discrepancy for CVR ID %d, Assertion ID %d, " +
        "contest %s.", prefix, cvr.id(), id, contestName));

    // Get CVRContestInfo matching this assertion's contest from the CVR and audited ballot.
    final Optional<CVRContestInfo> cvrInfo = cvr.contestInfoForContestResult(contestName);
    final Optional<CVRContestInfo> acvrInfo = auditedCVR.contestInfoForContestResult(contestName);

    // Special case: the assertion's contest is not on either the CVR record or Audited Ballot.
    // No discrepancy possible as there is nothing to compare.
    if(cvrInfo.isEmpty() && acvrInfo.isEmpty()){
      LOGGER.debug(String.format("%s Assertion ID %d contest %s is not on CVR ID %d or audited " +
          "ballot. No discrepancy.",prefix, id, contestName, cvr.id()));
      return OptionalInt.empty();
    }

    // Compute a score for the CVR record and audited ballot in relation to this assertion. The
    // discrepancy is equal to cvrScore - acvrScore. If the difference is 0, we check whether the
    // votes on each record are the same. If so, we return a "no discrepancy"/empty OptinalInt.
    // Otherwise, we return a discrepancy of 0.
    final int cvrScore = scoreCVR(cvr);
    final int acvrScore = scoreAuditedBallot(cvr.id(), auditedCVR);

    final int discrepancy = cvrScore - acvrScore;

    // If discrepancy == 0, we may not have a discrepancy if: both the CVR and audited ballot
    // have the assertion's contest on it; the audited ballot has consensus (YES); and the
    // recorded votes are the same.
    if(discrepancy == 0 && cvrInfo.isPresent() && acvrInfo.isPresent() &&
        acvrInfo.get().consensus() == ConsensusValue.YES &&
        cvrInfo.get().choices().equals(acvrInfo.get().choices()))
    {
      // We have no discrepancy: recorded votes are the same on both the CVR and audited ballot.
      LOGGER.debug(String.format("%s CVR ID %d, Assertion ID %d, contest %s, no discrepancy.",
          prefix, cvr.id(), id, contestName));

      if(cvrDiscrepancy.containsKey(cvr.id())){
        // We can only get here if the CVR was re-audited.
        cvrDiscrepancy.remove(cvr.id());
        LOGGER.debug(String.format("%s CVR ID %d, Assertion ID %d, contest %s, prior computed " +
            "discrepancy removed from assertion's records.", prefix, cvr.id(), id, contestName));
      }
      return OptionalInt.empty();
    }

    // We have a discrepancy.
    LOGGER.debug(String.format("%s CVR ID %d, Assertion ID %d, contest %s, discrepancy of %d.",
        prefix, cvr.id(), id, contestName, discrepancy));

    // Add the discrepancy to the discrepancy map.
    cvrDiscrepancy.put(cvr.id(), discrepancy);

    return OptionalInt.of(discrepancy);
  }

  /**
   * This function produces one of three values for the given CVR: a 0; -1; or 1. This value
   * is the CVRs score in relation to this assertion. It will look at the CVRContestInfo on the
   * CVR for the assertion's contest. If the contest is not on the CVR, a 0 will be returned. If
   * the CVR is a phantom (i.e., it is missing), the worst case score (for a CVR) of 1 will be
   * returned.  A 1 will produce the highest discrepancy value when combined with an audited ballot
   * score, as the formula used is CVRScore - AuditedBallotScore. Otherwise, the assertion's
   * scoring method will be applied to the vote recorded in the CVR's CVRContestInfo. A
   * RuntimeException will be thrown if a null 'cvr' is provided,
   * @param cvr The CVR to be scored in relation to this assertion.
   * @return a value of 0, -1, or 1 representing the CVR's score in relation to this assertion.
   */
  private int scoreCVR(final CastVoteRecord cvr) {
    final String prefix = "[scoreCVR]";

    if(cvr == null){
      // This should never happen, and indicates an error has occurred somewhere.
      final String msg = String.format("%s A null CVR record has been passed to the CVR " +
          "scoring method for Assertion ID %d, contest %s.", prefix, id, contestName);
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }

    LOGGER.debug(String.format("%s Computing score for CVR ID %d, Assertion ID %d, contest %s.",
        prefix, cvr.id(), id, contestName));

    // Just in case other parts of the code base inconsistently use different PHANTOM
    // indicators for phantom CVRS -- we want to make sure any PHANTOM identifier
    // gets the CVR treated as a phantom.
    if(cvr.recordType() == RecordType.PHANTOM_RECORD ||
      cvr.recordType() == RecordType.PHANTOM_RECORD_ACVR ||
      cvr.recordType() == RecordType.PHANTOM_BALLOT){
      // The CVR is missing entirely. Return a worst case CVR score of 1.
      LOGGER.debug(String.format("%s CVR %d is a Phantom Record, CVR score is 1 for Assertion ID %d.",
          prefix, cvr.id(), id));
      return 1;
    }

    // Get CVRContestInfo matching this assertion's contest from the CVR.
    final Optional<CVRContestInfo> cvrInfo = cvr.contestInfoForContestResult(contestName);

    if(cvrInfo.isEmpty()){
      // The assertion's contest in not on the CVR.
      LOGGER.debug(String.format("%s Contest %s not on CVR %d, CVR score is 0 for Assertion ID %d.",
          prefix, contestName, cvr.id(), id));
      return 0;
    }

    // Compute the score as per this assertion's score() function, and return the result.
    final int cvrScore = score(cvrInfo.get());
    LOGGER.debug(String.format("%s CVR ID %d, Assertion ID %d, contest %s, CVR score is %d.",
        prefix, cvr.id(), id, contestName, cvrScore));
    return cvrScore;
  }

  /**
   * This function produces one of three values for the given audited ballot: a 0; -1; or 1. This
   * value is the audited ballot's score in relation to this assertion. It will look at the
   * CVRContestInfo on the audited ballot for the assertion's contest. If the contest is not on the
   * CVR, a 0 will be returned. If the audited ballot is a phantom (i.e., it is missing), the worst
   * case score (for an audited ballot) of -1 will be returned. A -1 will produce the highest
   * discrepancy value when combined with a CVR score, as the formula used is CVRScore -
   * AuditedBallotScore. Otherwise, the assertion's scoring method will be applied to the vote recorded
   * in the audited ballot's CVRContestInfo. A RuntimeException is thrown if a null 'auditedCVR'
   * is provided.
   * @param cvrID The ID of the CVR corresponding to this audited ballot (note: colorado-rla is
   *              a little flaky in its assignment of IDs to audited CVR records, so it is safest
   *              to supply this as a parameter rather than trying to access it from the record).
   * @param auditedCVR The audited ballot to be scored in relation to this assertion.
   * @return a value of 0, -1, or 1 representing the audited ballot's score in relation to this assertion.
   */
  private int scoreAuditedBallot(long cvrID, final CastVoteRecord auditedCVR) {
    final String prefix = "[scoreAuditedBallot]";

    if(auditedCVR == null){
      // This should never happen, and indicates an error has occurred somewhere.
      final String msg = String.format("%s A null auditedCVR record has been passed to the audited " +
          "ballot scoring method for Assertion ID %d, contest %s.", prefix, id, contestName);
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }

    LOGGER.debug(String.format("%s Computing score for audited ballot for CVR ID %d, Assertion ID %d, " +
        "contest %s.", prefix, cvrID, id, contestName));

    // Just in case other parts of the code base inconsistently use different PHANTOM
    // indicators for phantom ballots -- we want to make sure any PHANTOM identifier
    // gets the ballot treated as a phantom.
    if(auditedCVR.recordType() == RecordType.PHANTOM_BALLOT ||
      auditedCVR.recordType() == RecordType.PHANTOM_RECORD_ACVR ||
      auditedCVR.recordType() == RecordType.PHANTOM_RECORD){
      // The audited ballot is missing entirely. Return a worst case audited ballot score of -1.
      LOGGER.debug(String.format("%s audited ballot for CVR ID %d is a Phantom Record, " +
          "audited ballot score is -1 for Assertion ID %d.",
          prefix, cvrID, id));
      return -1;
    }

    // Get CVRContestInfo matching this assertion's contest from the audited ballot.
    final Optional<CVRContestInfo> acvrInfo = auditedCVR.contestInfoForContestResult(contestName);

    if(acvrInfo.isEmpty()){
      // The assertion's contest in not on the audited ballot.
      LOGGER.debug(String.format("%s Contest %s not on audited ballot for CVR ID %d, audited " +
          "ballot score is 0 for Assertion ID %d.", prefix, contestName, cvrID, id));
      return 0;
    }

    if(acvrInfo.get().consensus() == ConsensusValue.NO){
      // The audited ballot has no consensus, we treat it as a Phantom Ballot.
      // Return the worst case audited ballot score of -1.
      LOGGER.debug(String.format("%s audited ballot for CVR ID %d has no consensus, " +
              "audited ballot score is -1 for Assertion ID %d.", prefix, cvrID, id));
      return -1;
    }

    // Compute the score as per this assertion's score() function, and return the result.
    final int acvrScore = score(acvrInfo.get());
    LOGGER.debug(String.format("%s CVR ID %d, Assertion ID %d, contest %s, audited ballot score is %d.",
        prefix, cvrID, id, contestName, acvrScore));
    return acvrScore;
  }

  /**
   * For a given CVRAuditInfo capturing a discrepancy between a CVR and ACVR, check if the
   * discrepancy is relevant for this assertion (if it is present in its cvrDiscrepancy map).
   * If so, increment the counters for its discrepancy type. A RuntimeException will be thrown
   * if the discrepancy type associated with this CVR-ACVR pair is not valid (i.e., not one of the
   * defined types). This method is designed to be called 'n' times for a given CVRAuditInfo if
   * the associated CVR appears 'n' times in the sample.
   * @param theRecord CVRAuditInfo representing the CVR-ACVR pair that has resulted in a discrepancy.
   * @return a boolean indicating if a discrepancy associated with the given CVRAuditInfo was
   * recorded against the totals of at least one of this audit's assertions.
   */
  public boolean recordDiscrepancy(final CVRAuditInfo theRecord) {
    final String prefix = "[recordDiscrepancy]";

    // Flag which will be set to true if we do increase the assertion's internal discrepancy
    // counts.
    boolean recorded = false;

    if(cvrDiscrepancy.containsKey(theRecord.id())){
      final int theType = cvrDiscrepancy.get(theRecord.id());
      switch (theType) {
        case -2 -> twoVoteUnderCount += 1;
        case -1 -> oneVoteUnderCount += 1;
        case 0 -> otherCount += 1;
        case 1 -> oneVoteOverCount += 1;
        case 2 -> twoVoteOverCount += 1;
        default -> {
          final String msg = String.format("%s Invalid discrepancy type %d stored in " +
              "discrepancy map for Assertion ID %d, contest %s.", prefix, theType, id, contestName);
          LOGGER.error(msg);
          throw new RuntimeException(msg);
        }
      }

      LOGGER.debug(String.format("%s Discrepancy of type %d added to Assertion ID %d,"+
          "contest %s, CVR ID %d. New totals: 1 vote understatements %d; 1 vote overstatements %d; " +
          "2 vote understatements %d; 2 vote overstatements %d; other %d.", prefix, theType, id,
          contestName, theRecord.id(), oneVoteUnderCount, oneVoteOverCount, twoVoteUnderCount,
          twoVoteOverCount, otherCount));

      recorded = true;
    }
    else{
      final String msg = String.format("%s Assertion ID %d contest %s, CVR ID %s: no record of " +
          " a pre-computed discrepancy associated with that CVR. No increase to assertion discrepancy " +
          "totals: 1 vote understatements %d; 1 vote overstatements %d; 2 vote understatements %d; " +
          "2 vote overstatements %d; other %d.", prefix, id, contestName, theRecord.id(),
          oneVoteUnderCount, oneVoteOverCount, twoVoteUnderCount, twoVoteOverCount, otherCount);
      LOGGER.debug(msg);
    }

    return recorded;
  }

  /**
   * Removes discrepancies relating to a given CVR-ACVR comparison. (This is relevant when
   * ballots are 'un-audited' to be subsequently re-audited). A RuntimeException will be thrown
   * if an invalid discrepancy type has been stored in this assertion. Note that we do not remove
   * the CVR ID from the cvrDiscrepancy map. This is because there may be multiple instances of the
   * discrepancy counted in the assertion's totals (e.g, if the CVR appears multiple times in the
   * sample to be audited). This method is designed to be called 'n' times for a given CVRAuditInfo if
   * the associated CVR appears 'n' times in the sample.
   * @param theRecord The CVRAuditInfo record that generated the discrepancy.
   * @return a boolean indicating if a discrepancy associated with the given CVRAuditInfo was
   * removed from this assertion's totals.
   */
  public boolean removeDiscrepancy(final CVRAuditInfo theRecord) {
    final String prefix = "[removeDiscrepancy]";

    // Flag which will be set to true if we do remove a discrepancy from the assertions
    // internal totals.
    boolean removed = false;

    // Check if this CVR-ACVR pair produced a discrepancy with respect to this assertion.
    // (Note the CVRAuditInfo ID is always the CVR ID).
    if(cvrDiscrepancy.containsKey(theRecord.id())){
      final int theType = cvrDiscrepancy.get(theRecord.id());
      switch (theType) {
        case -2 -> twoVoteUnderCount -= 1;
        case -1 -> oneVoteUnderCount -= 1;
        case 0 -> otherCount -= 1;
        case 1 -> oneVoteOverCount -= 1;
        case 2 -> twoVoteOverCount -= 1;
        default -> {
          final String msg = String.format("%s Invalid discrepancy type %d stored in " +
              "discrepancy map for Assertion ID %d, contest %s.", prefix, theType, id, contestName);
          LOGGER.error(msg);
          throw new RuntimeException(msg);
        }
      }

      // Note that we do not remove the CVR ID from the cvrDiscrepancy map. This is because there
      // may be multiple instances of the discrepancy counted in the assertion's totals (e.g, if
      // the CVR appears multiple times in the sample to be audited). If a ballot is reaudited,
      // and a determination made that a discrepancy does not exist, the entry in cvrDiscrepancies
      // will be updated via the computeDiscrepancy() method.

      LOGGER.debug(String.format("%s Discrepancy of type %d removed from Assertion ID %d,"+
              "contest %s, CVR ID %d. New totals: 1 vote understatements %d; 1 vote overstatements %d; " +
              "2 vote understatements %d; 2 vote overstatements %d; other %d.", prefix, theType, id,
              contestName, theRecord.id(), oneVoteUnderCount, oneVoteOverCount, twoVoteUnderCount,
              twoVoteOverCount, otherCount));
      removed = true;
    }
    else{
      final String msg = String.format("%s Assertion ID %d contest %s, CVR ID %s: no record of a " +
          "pre-computed discrepancy associated with that CVR. No change to discrepancy totals: " +
          "1 vote understatements %d; 1 vote overstatements %d; 2 vote understatements %d; 2 vote " +
          "overstatements %d; other %d.", prefix, id, contestName, theRecord.id(), oneVoteUnderCount,
          oneVoteOverCount, twoVoteUnderCount, twoVoteOverCount, otherCount);
      LOGGER.debug(msg);
    }

    if(min(List.of(oneVoteOverCount, oneVoteUnderCount, twoVoteOverCount, twoVoteUnderCount, otherCount)) < 0) {
      // This should really be an error, however there appears to be some logic in CORLA that
      // allows discrepancy counts to temporarily fall below zero in certain scenarios
      // (eg. when the same ballot appears in multiple rounds, CORLA seems to unaudit the ballot
      // a given number of times corresponding to how often the ballot has appeared in the samples
      // across all rounds, and the audit it again a certain number of times.)
      final String msg = String.format("%s Negative discrepancy counts in Assertion ID %d, " +
          "contest %s when removing discrepancy for CVR %d.", prefix, id, contestName, theRecord.id());
      LOGGER.warn(msg);
    }

    return removed;
  }

  /**
   * Compute the current level of risk achieved for this assertion given a specified
   * audited sample count and gamma value. Audit.pValueApproximation is used to compute
   * this risk using the diluted margin of the assertion, and the recorded number of
   * one/two vote understatements and one/two vote overstatements. The assertions 'currentRisk'
   * attribute is updated, and the computed risk returned.
   * @param auditedSampleCount  Number of ballots audited.
   * @return Level of risk achieved for this assertion.
   */
  public BigDecimal riskMeasurement(int auditedSampleCount){
    final String prefix = "[riskMeasurement]";
    if (auditedSampleCount > 0 && dilutedMargin.doubleValue() > 0) {
      LOGGER.debug(String.format("%s Assertion ID %d, contest %s, calling " +
          "Audit.pValueApproximation() with parameters: audited sample count %d; diluted margin " +
          "%f; gamma %f; one vote under count %d; two vote under count %d; one vote over count " +
          "%d; two vote over count %d.", prefix, id, contestName, auditedSampleCount, dilutedMargin,
          Audit.GAMMA, oneVoteUnderCount, twoVoteUnderCount, oneVoteOverCount, twoVoteOverCount));

      currentRisk = Audit.pValueApproximation(auditedSampleCount, dilutedMargin, Audit.GAMMA,
          oneVoteUnderCount, twoVoteUnderCount, oneVoteOverCount, twoVoteOverCount).setScale(
              RISK_DECIMALS, RoundingMode.HALF_UP);
    } else {
      // Full risk (100%) when nothing is known
      currentRisk = BigDecimal.ONE;
    }

    LOGGER.debug(String.format("%s Assertion ID %d, contest %s, risk %f.", prefix, id,
        contestName, currentRisk));
    return currentRisk;
  }


  /**
   * Computes the Score for the given vote in the context of this assertion. For details on how
   * votes are scored for assertions, refer to the Guide to RAIRE (Part 2, Appendix A, Table A.1.).
   *
   * @param info Contest information containing the vote to be scored.
   * @return Vote score (either -1, 0, or 1).
   */
  protected abstract int score(final CVRContestInfo info);

}