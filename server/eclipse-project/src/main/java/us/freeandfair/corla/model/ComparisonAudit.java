/*
 * Free & Fair Colorado RLA System
 *
 * @title ColoradoRLA
 * @created Aug 19, 2017
 * @copyright 2017 Colorado Department of State
 * @license SPDX-License-Identifier: AGPL-3.0-or-later
 * @creator Daniel M. Zimmerman <dmz@freeandfair.us>
 * @description A system to assist in conducting statewide risk-limiting audits.
 */

package us.freeandfair.corla.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Cacheable;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import us.freeandfair.corla.math.Audit;
import us.freeandfair.corla.model.CVRContestInfo.ConsensusValue;
import us.freeandfair.corla.model.CastVoteRecord.RecordType;
import us.freeandfair.corla.persistence.PersistentEntity;
import us.freeandfair.corla.persistence.LongListConverter;

/**
 * A class representing the state of a single audited contest for
 * across multiple counties
 *
 */
@Entity
@Cacheable(true)
@Table(name = "comparison_audit")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "audit_type")
@DiscriminatorValue("PLURALITY")
@SuppressWarnings({"PMD.ImmutableField", "PMD.ExcessiveClassLength",
    "PMD.CyclomaticComplexity", "PMD.GodClass", "PMD.ModifiedCyclomaticComplexity",
    "PMD.StdCyclomaticComplexity", "PMD.TooManyFields", "PMD.TooManyMethods",
    "PMD.ExcessiveImports"})
public class ComparisonAudit implements PersistentEntity {

  /**
   * Class-wide logger
   */
  public static final Logger LOGGER =
    LogManager.getLogger(ComparisonAudit.class);

  /**
   * The database stored precision for decimal types.
   */
  public static final int PRECISION = 10;

  /**
   * The database stored scale for decimal types.
   */
  public static final int SCALE = 8;

  /**
   * The ID number.
   */
  @Id
  @Column(updatable = false, nullable = false)
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long my_id;

  /**
   * The version (for optimistic locking).
   */
  @Version
  private Long my_version;

  /**
   * The contest result for this audit state.
   */
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn
  private ContestResult my_contest_result;

  /**
   * The reason for this audit.
   */
  @Column(updatable = false, nullable = false)
  @Enumerated(EnumType.STRING)
  private AuditReason my_audit_reason;

  /**
   * The status of this audit.
   */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AuditStatus my_audit_status = AuditStatus.NOT_STARTED;

  /**
   * The gamma.
   */
  @Column(updatable = false, nullable = false,
          precision = PRECISION, scale = SCALE)
          private BigDecimal my_gamma = Audit.GAMMA;

  /**
   * The diluted margin. [Democracy Developers: this must be accessible and modifiable by
   * child classes of ComparisonAudit, hence we have made this attribute protected.]
   */
  @Column(updatable = false, nullable = false,
          precision = PRECISION, scale = SCALE)
          protected BigDecimal diluted_margin = BigDecimal.ONE;

  /**
   * The risk limit.
   */
  @Column(updatable = false, nullable = false,
          precision = PRECISION, scale = SCALE)
          private BigDecimal my_risk_limit = BigDecimal.ONE;

  /**
   * The number of samples audited.
   */
  @Column(nullable = false)
  private Integer my_audited_sample_count = 0;

  /**
   * The number of samples to audit overall assuming no further overstatements.
   * [Democracy Developers: this must be accessible and modifiable by child classes of
   * ComparisonAudit, hence we have made this attribute protected.]
   */
  @Column(nullable = false)
  protected Integer my_optimistic_samples_to_audit = 0;

  /**
   * The expected number of samples to audit overall assuming overstatements
   * continue at the current rate.
   * [Democracy Developers: this must be accessible and modifiable by child classes of
   * ComparisonAudit, hence we have made this attribute protected.]
   */
  @Column(nullable = false)
  protected Integer my_estimated_samples_to_audit = 0;

  /**
   * The number of two-vote understatements recorded so far.
   */
  @Column(nullable = false)
  private Integer my_two_vote_under_count = 0;

  /**
   * The number of one-vote understatements recorded so far.
   */
  @Column(nullable = false)
  private Integer my_one_vote_under_count = 0;

  /**
   * The number of one-vote overstatements recorded so far.
   */
  @Column(nullable = false)
  private Integer my_one_vote_over_count = 0;

  /**
   * The number of two-vote overstatements recorded so far.
   */
  @Column(nullable = false)
  private Integer my_two_vote_over_count = 0;

  /**
   * The number of discrepancies recorded so far that are neither
   * understatements nor overstatements.
   */
  @Column(nullable = false)
  private Integer my_other_count = 0;

  /**
   * The number of disagreements.
   */
  @Column(nullable = false)
  private Integer my_disagreement_count = 0;

  /**
   * gets incremented
   * DD: Note, this attribute DOES NOT get incremented during an audit. We (DD) have
   * implemented a minimal fix by returning the total of the one and two vote
   * overstatement counts in getOverstatements() rather than returning the value of
   * this variable. This variable can be removed from ComparisonAudit, and the
   * database table, in a fuller fix.
   */
  @Column(nullable = true) // true for migration
  private BigDecimal overstatements = BigDecimal.ZERO;

  /**
   * A flag that indicates whether the optimistic ballots to audit
   * estimate needs to be recalculated.
   * [Democracy Developers: this must be accessible and modifiable by child classes of
   * ComparisonAudit, hence we have made this attribute protected.]
   */
  @Column(nullable = false)
  protected Boolean my_optimistic_recalculate_needed = true;

  /**
   * A flag that indicates whether the non-optimistic ballots to
   * audit estimate needs to be recalculated.
   * [Democracy Developers: this must be accessible and modifiable by child classes of
   * ComparisonAudit, hence we have made this attribute protected.]
   */
  @Column(nullable = false)
  protected Boolean my_estimated_recalculate_needed = true;

  /**
   * The sequence of CastVoteRecord ids for this contest ordered by County id
   */
  @Column(name = "contest_cvr_ids", columnDefinition = "text")
  @Convert(converter = LongListConverter.class)
  private List<Long> contestCVRIds = new ArrayList<Long>();

  /**
   * A map from CVRAuditInfo objects to their discrepancy values for this
   * audited contest.
   */
  @ElementCollection
  @CollectionTable(name = "contest_comparison_audit_discrepancy",
                   joinColumns = @JoinColumn(name = "contest_comparison_audit_id",
                                             referencedColumnName = "my_id"))
  @MapKeyJoinColumn(name = "cvr_audit_info_id")
  @Column(name = "discrepancy")
  private Map<CVRAuditInfo, Integer> my_discrepancies = new HashMap<>();

  /**
   * A map from CVRAuditInfo objects to their discrepancy values for this
   * audited contest.
   */
  @ManyToMany
  @JoinTable(name = "contest_comparison_audit_disagreement",
             joinColumns = @JoinColumn(name = "contest_comparison_audit_id",
                                       referencedColumnName = "my_id"),
             inverseJoinColumns = @JoinColumn(name = "cvr_audit_info_id",
                                              referencedColumnName = "my_id"))
  private Set<CVRAuditInfo> my_disagreements = new HashSet<>();

  /**
   * Constructs a new, empty ComparisonAudit (solely for persistence).
   */
  public ComparisonAudit() {
    super();
  }

  /**
   * Constructs a ComparisonAudit for the given params
   *
   * @param contestResult The contest result.
   * @param riskLimit The risk limit.
   * @param dilutedMargin μ
   * @param gamma γ
   * @param auditReason The audit reason.
   */
  // FIXME estimatedSamplesToAudit / optimisticSamplesToAudit have side
  // effects, so we should call that out
  //
  // FIXME Remove the warning by not calling overridable methods :D
  @SuppressWarnings({"PMD.ConstructorCallsOverridableMethod"})
  public ComparisonAudit(final ContestResult contestResult,
                         final BigDecimal riskLimit,
                         final BigDecimal dilutedMargin,
                         final BigDecimal gamma,
                         final AuditReason auditReason) {

    super();
    my_contest_result = contestResult;
    my_risk_limit = riskLimit;
    this.diluted_margin = dilutedMargin;
    my_gamma = gamma;
    my_audit_reason = auditReason;
    // compute initial sample size
    optimisticSamplesToAudit();
    estimatedSamplesToAudit();

    if (contestResult.getDilutedMargin().equals(BigDecimal.ZERO)) {
      // the diluted margin is 0, so this contest is not auditable
      my_audit_status = AuditStatus.NOT_AUDITABLE;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long id() {
    return my_id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setID(final Long the_id) {
    my_id = the_id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long version() {
    return my_version;
  }

  /**
   * @return the counties related to this contestresult.
   */
  public Set<County> getCounties() {
    return Collections.unmodifiableSet(this.contestResult().getCounties());
  }

  /**
   * @return the contest result associated with this audit.
   */
  public ContestResult contestResult() {
    return my_contest_result;
  }

  /**
   * @return the gamma associated with this audit.
   */
  public BigDecimal getGamma() {
    return my_gamma;
  }

  /**
   * @return the risk limit associated with this audit.
   */
  public BigDecimal getRiskLimit() {
    return my_risk_limit;
  }

  /**
   * @return the diluted margin from the ContestResult.
   */
  public BigDecimal getDilutedMargin() {
    return this.diluted_margin;
  }

  public String getContestName() {
    return this.contestResult().getContestName();
  }
  
  /**
   * @return the audit reason associated with this audit.
   */
  public AuditReason auditReason() {
    return my_audit_reason;
  }

  /**
   * @return the audit status associated with this audit.
   */
  public AuditStatus auditStatus() {
    return my_audit_status;
  }

  /** set audit status **/
  public void setAuditStatus(final AuditStatus auditStatus) {
    my_audit_status = auditStatus;
  }

  /** see if the county is participating in this audit(contest) **/
  public boolean isForCounty(final Long countyId) {
    return getCounties().stream()
      .filter(c -> c.id().equals(countyId))
      .findFirst()
      .isPresent();
  }

  /**
   * Does this audit belong to only a single county?
   */
  public boolean isSingleCountyFor(final County c) {
    return getCounties().equals(Stream.of(c)
                                .collect(Collectors.toSet()));
  }

  /**
   * Updates the audit status based on the current risk limit. If the audit
   * has already been ended or the contest is not auditable, this method has
   * no effect on its status. 
   * Fix: RLA-00450
   */
  public void updateAuditStatus() {
    LOGGER.debug(String.format("[updateAuditStatus: %s for contest=%s "
        + "my_optimistic_samples_to_audit=%d my_audited_sample_count=%d my_optimistic_recalculate_needed=%s my_estimated_recalculate_needed=%s]",
        my_audit_status, contestResult().getContestName(),
        my_optimistic_samples_to_audit, my_audited_sample_count,
        my_optimistic_recalculate_needed, my_estimated_recalculate_needed));

    if (my_audit_status == AuditStatus.ENDED ||
        my_audit_status == AuditStatus.HAND_COUNT ||
        my_audit_status == AuditStatus.NOT_AUDITABLE) {
      return;
    }

    if (Boolean.TRUE.equals(my_optimistic_recalculate_needed) ||
        Boolean.TRUE.equals(my_estimated_recalculate_needed)) {
      recalculateSamplesToAudit();
    } //below calculation needs recalculate RLA-00450
        
    if (my_optimistic_samples_to_audit - my_audited_sample_count <= 0) {
      LOGGER.debug(String.format("[updateAuditStatus: RISK_LIMIT_ACHIEVED for contest=%s]",
                                 contestResult().getContestName()));
      my_audit_status = AuditStatus.RISK_LIMIT_ACHIEVED;
    } else {
      // risk limit has not been achieved
      // note that it _is_ possible to go from RISK_LIMIT_ACHIEVED to
      // IN_PROGRESS if a sample or set of samples is "unaudited"
      if (my_audit_status.equals(AuditStatus.RISK_LIMIT_ACHIEVED)) {
        LOGGER.warn("[updateAuditStatus: Moving from RISK_LIMIT_ACHIEVED -> IN_PROGRESS!]");
      }

      my_audit_status = AuditStatus.IN_PROGRESS;
    }
  }

  /**
   * Ends this audit; if the audit has already reached its risk limit,
   * or the contest is not auditable, this call has no effect on its status.
   */
  public void endAudit() {
    if (my_audit_status != AuditStatus.RISK_LIMIT_ACHIEVED &&
        my_audit_status != AuditStatus.NOT_AUDITABLE) {
      my_audit_status = AuditStatus.ENDED;
    }
  }

  /**
   * @return the initial expected number of samples to audit.
   */
  @SuppressWarnings({"checkstyle:magicnumber", "PMD.AvoidDuplicateLiterals"})
  public int initialSamplesToAudit() {
    return computeOptimisticSamplesToAudit(0, 0, 0, 0).
      setScale(0, RoundingMode.CEILING).intValue();
  }

  /**
   * @return the expected overall number of ballots to audit, assuming no
   * further overstatements occur.
   */
  public Integer optimisticSamplesToAudit() {
    if (my_optimistic_recalculate_needed) {
      recalculateSamplesToAudit();
    }
    return my_optimistic_samples_to_audit;
  }

  /** estimatedSamplesToAudit minus getAuditedSampleCount **/
  public final Integer estimatedRemaining() {
    return Math.max(0, estimatedSamplesToAudit() - getAuditedSampleCount());
  }

  /** optimisticSamplesToAudit minus getAuditedSampleCount **/
  public final Integer optimisticRemaining() {
    return Math.max(0, optimisticSamplesToAudit() - getAuditedSampleCount());
  }

  /**
   * @return the expected overall number of ballots to audit, assuming
   * overstatements continue to occur at the current rate.
   */
  public final Integer estimatedSamplesToAudit() {
    if (my_estimated_recalculate_needed) {
      LOGGER.debug("[estimatedSampleToAudit: recalculate needed]");
      recalculateSamplesToAudit();
    }
    return my_estimated_samples_to_audit;
  }

  /**
   *
   * The number of one-vote and two-vote overstatements across the set
   * of counties participating in this audit.
   *
   * TODO collect the number of 1 and 2 vote overstatements across
   * participating counties.
   *
   * DD: We replaced this method with the sum of 1 and 2 vote overstatements as
   * the 'overstatements' attribute was not being updated throughout an audit. It
   * was always zero. This was evidently suggested in a prior review of the system.
   * Note that the overstatements attribute could be removed from the class and
   * database table if desired.
   */
  public BigDecimal getOverstatements() {
    return BigDecimal.valueOf(this.my_one_vote_over_count + this.my_two_vote_over_count);
    //this.overstatements; // FIXME
  }

  /** the number of ballots audited  **/
  public Integer getAuditedSampleCount() {
    return this.my_audited_sample_count;
  }

  /**
   * Risk limit achieved according to math.Audit.
   * This has a fallback to 1.0 (max risk) when ``nothing is known''.
   **/
  public BigDecimal riskMeasurement() {
    if (my_audited_sample_count > 0
        && diluted_margin.compareTo(BigDecimal.ZERO) > 0) {
      final BigDecimal result =  Audit.pValueApproximation(my_audited_sample_count,
          diluted_margin,
          my_gamma,
          my_one_vote_under_count,
          my_two_vote_under_count,
          my_one_vote_over_count,
          my_two_vote_over_count);
      return result.setScale(3, BigDecimal.ROUND_HALF_UP);
    } else {
      // full risk (100%) when nothing is known
      return BigDecimal.ONE;
    }
  }

  /**
   * A scaling factor for the estimate, from 1 (when no samples have
   * been audited) upward.41
   The scaling factor grows as the ratio of
   * overstatements to samples increases.
   */
  private BigDecimal scalingFactor() {
    final BigDecimal auditedSamples = BigDecimal.valueOf(getAuditedSampleCount());
    if (auditedSamples.equals(BigDecimal.ZERO)) {
      return BigDecimal.ONE;
    } else {
      return BigDecimal.ONE.add(getOverstatements()
                                .divide(auditedSamples, MathContext.DECIMAL128));
    }
  }

  /**
   * Recalculates the overall numbers of ballots to audit, setting this
   * object's `my_optimistic_samples_to_audit` and
   * `my_estimates_samples_to_audit` fields.
   */
  protected void recalculateSamplesToAudit() {
    LOGGER.debug(String.format("[recalculateSamplestoAudit start contestName=%s, "
                               + "twoUnder=%d, oneUnder=%d, oneOver=%d, twoOver=%d"
                               + " optimistic=%d, estimated=%d]",
                               contestResult().getContestName(),
                               my_two_vote_under_count, my_one_vote_under_count,
                               my_one_vote_over_count, my_two_vote_over_count,
                               my_optimistic_samples_to_audit, my_estimated_samples_to_audit));

    if (my_optimistic_recalculate_needed) {
      LOGGER.debug("[recalculateSamplesToAudit: calling computeOptimisticSamplesToAudit]");
      final BigDecimal optimistic = computeOptimisticSamplesToAudit(my_two_vote_under_count,
                                                                    my_one_vote_under_count,
                                                                    my_one_vote_over_count,
                                                                    my_two_vote_over_count);
      my_optimistic_samples_to_audit = optimistic.intValue();
      my_optimistic_recalculate_needed = false;
    }

    if (my_one_vote_over_count + my_two_vote_over_count == 0) {
      LOGGER.debug("[recalculateSamplesToAudit: zero overcounts]");
      my_estimated_samples_to_audit = my_optimistic_samples_to_audit;
    } else {
      LOGGER.debug(String.format("[recalculateSamplesToAudit: non-zero overcounts, using scaling factor %s]", scalingFactor()));
      my_estimated_samples_to_audit =
        BigDecimal.valueOf(my_optimistic_samples_to_audit)
        .multiply(scalingFactor())
        .setScale(0, RoundingMode.CEILING)
        .intValue();
    }

    LOGGER.debug(String.format("[recalculateSamplestoAudit end contestName=%s, "
                               + "twoUnder=%d, oneUnder=%d, oneOver=%d, twoOver=%d"
                               + " optimistic=%d, estimated=%d]",
                               contestResult().getContestName(),
                               my_two_vote_under_count, my_one_vote_under_count,
                               my_one_vote_over_count, my_two_vote_over_count,
                               my_optimistic_samples_to_audit, my_estimated_samples_to_audit));
    my_estimated_recalculate_needed = false;
  }

  /**
   * Computes the expected number of ballots to audit overall given the
   * specified numbers of over- and understatements.
   *
   * @param the_two_under The two-vote understatements.
   * @param the_one_under The one-vote understatements.
   * @param the_one_over The one-vote overstatements.
   * @param the_two_over The two-vote overstatements.
   *
   * @return the expected number of ballots remaining to audit.
   * This is the stopping sample size as defined in the literature:
   * https://www.stat.berkeley.edu/~stark/Preprints/gentle12.pdf
   */
  private BigDecimal computeOptimisticSamplesToAudit(final int twoUnder,
                                                     final int oneUnder,
                                                     final int oneOver,
                                                     final int twoOver) {
    return Audit.optimistic(getRiskLimit(), getDilutedMargin(), getGamma(),
                            twoUnder, oneUnder, oneOver, twoOver) ;
  }

  /**
   * Signals that a sample has been audited. This ensures that estimates
   * are recalculated correctly and states are updated.
   *
   * @param count The count of samples that have been audited simultaneously
   * (for duplicates).
   */
  public void signalSampleAudited(final int count) {
    my_estimated_recalculate_needed = true;
    my_audited_sample_count = my_audited_sample_count + count;

    // this may not be needed, but I'm not sure
    if (my_audit_status == AuditStatus.RISK_LIMIT_ACHIEVED) {
      LOGGER.warn("RESETTING AuditStatus from RISK_LIMIT_ACHIEVED to IN_PROGRESS");
      my_audit_status = AuditStatus.IN_PROGRESS;
    }
  }

  /**
   * Signals that a sample has been audited, if the CVR was selected for
   * this audit and this audit is targeted (i.e., not for opportunistic
   * benefits.)
   *
   * @param count The count of samples that have been audited simultaneously
   * @param cvrID ID of the CVR being audited
   */
  public void signalSampleAudited(final int count, final Long cvrID) {
    final boolean covered = isCovering(cvrID);
    final boolean targeted = isTargeted();

    if (targeted && !covered) {
      LOGGER.debug
        (String.format("[signalSampleAudited: %s is targeted, but cvrID (%d) not selected for audit.]",
                       contestResult().getContestName(), cvrID));
    }

    if (targeted && covered) {
    LOGGER.debug
      (String.format
       ("[signalSampleAudited: targeted and covered! "
        + "contestName=%s, cvrID=%d, auditedSamples=%d, count=%d]",
        contestResult().getContestName(), cvrID, getAuditedSampleCount(), count));
      signalSampleAudited(count);
    }
  }

  /**
   * Signals that a sample has been unaudited. This ensures that estimates
   * are recalculated correctly and states are updated.
   *
   * @param the_count The count of samples that have been unaudited simultaneously
   * (for duplicates).
   */
  public void signalSampleUnaudited(final int count) {
    my_estimated_recalculate_needed = true;
    my_audited_sample_count = my_audited_sample_count - count;

    // this may not be needed, but I'm not sure
    if (my_audit_status == AuditStatus.RISK_LIMIT_ACHIEVED) {
      LOGGER.warn("RESETTING AuditStatus from RISK_LIMIT_ACHIEVED to IN_PROGRESS");
      my_audit_status = AuditStatus.IN_PROGRESS;
    }
  }


  /**
   * Signals that a sample has been unaudited, if the CVR was selected
   * for this audit.
   *
   * @param count The count of samples that have been unaudited simultaneously
   * (for duplicates).
   * @parma cvrID The ID of the CVR to unaudit
   */
  public void signalSampleUnaudited(final int count, final Long cvrID) {
    LOGGER.debug
      (String.format
       ("[signalSampleUnaudited: start "
        + "contestName=%s, cvrID=%d, auditedSamples=%d, count=%d]",
        contestResult().getContestName(), cvrID, getAuditedSampleCount(), count));

    final boolean covered = isCovering(cvrID);
    final boolean targeted = isTargeted();

    if (targeted && !covered) {
      LOGGER.debug
        (String.format("[signalSampleUnaudited: Targeted contest, but cvrID (%d) not selected.]",
                       cvrID));
    }

    if (targeted && covered) {
      LOGGER.debug(String.format("[signalSampleUnaudited: CVR ID [%d] is interesting to %s]",
                                 cvrID, contestResult().getContestName()));
      signalSampleUnaudited(count);
    }
  }


  /**
   * Records a disagreement with the specified CVRAuditInfo.
   *
   * @param the_record The CVRAuditInfo record that generated the disagreement.
   */
  public void recordDisagreement(final CVRAuditInfo the_record) {
    my_disagreements.add(the_record);
    my_disagreement_count = my_disagreement_count + 1;
  }

  /**
   * Removes a disagreement with the specified CVRAuditInfo.
   *
   * @param the_record The CVRAuditInfo record that generated the disagreement.
   */
  public void removeDisagreement(final CVRAuditInfo the_record) {
    my_disagreements.remove(the_record);
    my_disagreement_count = my_disagreement_count - 1;
  }

  /**
   * @return the disagreement count.
   */
  public int disagreementCount() {
    return my_disagreement_count;
  }

  /** was the given cvrid selected for this contest? **/
  public boolean isCovering(final Long cvrId) {
    return getContestCVRIds().contains(cvrId);
  }

  /**
   * Adds to the current collection of Contest CVR IDs
   * @param contestCVRIds a list
   */
  public void addContestCVRIds (final List<Long> contestCVRIds) {
    this.contestCVRIds.addAll(contestCVRIds);
  }

  /**
   * getter
   */
  public List<Long> getContestCVRIds() {
    return this.contestCVRIds;
  }

  /**
   * Is this audit because of a targeted contest?
   */
  public boolean isTargeted() {
    return this.contestResult().getAuditReason().isTargeted()
      && !isHandCount();
  }

  /**
   * Is an audit finished, or should we find more samples to compare?
   *
   */
  public boolean isFinished() {
    return
      this.auditStatus().equals(AuditStatus.NOT_AUDITABLE) ||
      this.auditStatus().equals(AuditStatus.RISK_LIMIT_ACHIEVED) ||
      this.auditStatus().equals(AuditStatus.HAND_COUNT) ||
      this.auditStatus().equals(AuditStatus.ENDED);
  }

  public boolean isHandCount() {
    return this.auditStatus().equals(AuditStatus.HAND_COUNT);
  }

  /** calculate the number of times the given cvrId appears in the selection
   * (across all rounds)
   **/
  public int multiplicity(final Long cvrId) {
    return Collections.frequency(getContestCVRIds(), cvrId);
  }

  /**
   * Records the specified discrepancy. If the discrepancy is for this Contest
   * but from a CVR/ballot that was not selected for this Contest (selected for
   * another Contest), it does not contribute to the counts and calculations. It
   * is still recorded, though, for informational purposes. The valid range is
   * -2 .. 2: -2 and -1 are understatements, 0 is a discrepancy that doesn't
   * affect the RLA calculations, and 1 and 2 are overstatements.
   *
   * @param the_record The CVRAuditInfo record that generated the discrepancy.
   * @param the_type The type of discrepancy to add.
   * @exception IllegalArgumentException if an invalid discrepancy type is
   * specified.
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public void recordDiscrepancy(final CVRAuditInfo the_record,
                                final int the_type) {
    // we never trigger an estimated recalculate here; it is
    // triggered by signalBallotAudited() regardless of whether there is
    // a discrepancy or not

    if (isCovering(the_record.cvr().id())) {
      switch (the_type) {
      case -2:
        my_two_vote_under_count = my_two_vote_under_count + 1;
        my_optimistic_recalculate_needed = true;
        break;

      case -1:
        my_one_vote_under_count = my_one_vote_under_count + 1;
        my_optimistic_recalculate_needed = true;
        break;

      case 0:
        my_other_count = my_other_count + 1;
        // no optimistic recalculate needed
        break;

      case 1:
        my_one_vote_over_count = my_one_vote_over_count + 1;
        my_optimistic_recalculate_needed = true;
        break;

      case 2:
        my_two_vote_over_count = my_two_vote_over_count + 1;
        my_optimistic_recalculate_needed = true;
        break;

      default:
        throw new IllegalArgumentException("invalid discrepancy type: " + the_type);
      }
    }

    LOGGER.info(String.format("[recordDiscrepancy type=%s, record=%s]",
                              the_type, the_record));
    my_discrepancies.put(the_record, the_type);
  }

  /**
   * get the discrepancy value that was recorded for this
   * ComparisonAudit(contest) on the given CVRAuditInfo(ballot). used for
   * reporting.
   **/
  public Integer getDiscrepancy(final CVRAuditInfo cai) {
    return my_discrepancies.get(cai);
  }

  /**
   * Removes the specified over/understatement (the valid range is -2 .. 2:
   * -2 and -1 are understatements, 0 is a discrepancy that doesn't affect the
   * RLA calculations, and 1 and 2 are overstatements). This is typically done
   * when a new interpretation is submitted for a ballot that had already been
   * interpreted.
   *
   * @param the_record The CVRAuditInfo record that generated the discrepancy.
   * @param the_type The type of discrepancy to remove.
   * @exception IllegalArgumentException if an invalid discrepancy type is
   * specified.
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public void removeDiscrepancy(final CVRAuditInfo the_record, final int the_type) {
    // we never trigger an estimated recalculate here; it is
    // triggered by signalBallotAudited() regardless of whether there is
    // a discrepancy or not
    switch (the_type) {
    case -2:
      my_two_vote_under_count = my_two_vote_under_count - 1;
      my_optimistic_recalculate_needed = true;
      break;

    case -1:
      my_one_vote_under_count = my_one_vote_under_count - 1;
      my_optimistic_recalculate_needed = true;
      break;

    case 0:
      my_other_count = my_other_count - 1;
      // no recalculate needed
      break;

    case 1:
      my_one_vote_over_count = my_one_vote_over_count - 1;
      my_optimistic_recalculate_needed = true;
      break;

    case 2:
      my_two_vote_over_count = my_two_vote_over_count - 1;
      my_optimistic_recalculate_needed = true;
      break;

    default:
      throw new IllegalArgumentException("invalid discrepancy type: " + the_type);
    }

    my_discrepancies.remove(the_record);
  }

  /**
   * Returns the count of the specified type of discrepancy. -2 and -1 represent
   * understatements, 0 represents a discrepancy that doesn't affect the RLA
   * calculations, and 1 and 2 represent overstatements.
   *
   * @param the_type The type of discrepancy.
   * @exception IllegalArgumentException if an invalid discrepancy type is
   * specified.
   */
  @SuppressWarnings("checkstyle:magicnumber")
  public int discrepancyCount(final int the_type) {
    final int result;

    switch (the_type) {
    case -2:
      result = my_two_vote_under_count;
      break;

    case -1:
      result = my_one_vote_under_count;
      break;

    case 0:
      result = my_other_count;
      break;

    case 1:
      result = my_one_vote_over_count;
      break;

    case 2:
      result = my_two_vote_over_count;
      break;

    default:
      throw new IllegalArgumentException("invalid discrepancy type: " + the_type);
    }

    return result;
  }

  /**
   * Computes the over/understatement represented by the CVR/ACVR pair stored in
   * the specified CVRAuditInfo. This method returns an optional int that, if
   * present, indicates a discrepancy. There are 5 possible types of
   * discrepancy: -1 and -2 indicate 1- and 2-vote understatements; 1 and 2
   * indicate 1- and 2- vote overstatements; and 0 indicates a discrepancy that
   * does not count as either an under- or overstatement for the RLA algorithm,
   * but nonetheless indicates a difference between ballot interpretations.
   *
   * @param the_info The CVRAuditInfo.
   * @return an optional int that is present if there is a discrepancy and absent
   * otherwise.
   */
  public OptionalInt computeDiscrepancy(final CVRAuditInfo the_info) {
    if (the_info.acvr() == null || the_info.cvr() == null) {
      throw new IllegalArgumentException("null CVR or ACVR in pair " + the_info);
    } else {
      return computeDiscrepancy(the_info.cvr(), the_info.acvr());
    }
  }

  /**
   * Computes the over/understatement represented by the specified CVR and ACVR.
   * This method returns an optional int that, if present, indicates a discrepancy.
   * There are 5 possible types of discrepancy: -1 and -2 indicate 1- and 2-vote
   * understatements; 1 and 2 indicate 1- and 2- vote overstatements; and 0
   * indicates a discrepancy that does not count as either an under- or
   * overstatement for the RLA algorithm, but nonetheless indicates a difference
   * between ballot interpretations.
   *
   * @param cvr The CVR that the machine saw
   * @param auditedCVR The ACVR that the human audit board saw
   * @return an optional int that is present if there is a discrepancy and absent
   * otherwise.
   */
  @SuppressWarnings("checkstyle:magicnumber")
  // FIXME Should we point to the ContestResult instead?
  public OptionalInt computeDiscrepancy(final CastVoteRecord cvr,
                                        final CastVoteRecord auditedCVR) {
    OptionalInt result = OptionalInt.empty();

    // FIXME this needs to get this stuff from the ContestResult
    // - a CastVoteRecord belongs to a county.
    // - a CVRContestInfo belongs to a Contest, which belongs to a county.
    // - should we change the CVRContestInfo to belong to a ContestResult instead?
    //
    // The CVRContestInfo has teh list of choices. we need this for
    // winners and loser of the contest......BUT the ContestResult also
    // has a set of winners and losers, which is now the MOST ACCURATE
    // version of this, since we're now out of the county context...
    final Optional<CVRContestInfo> cvr_info = cvr.contestInfoForContestResult(my_contest_result);
    final Optional<CVRContestInfo> acvr_info = auditedCVR.contestInfoForContestResult(my_contest_result);

    if (auditedCVR.recordType() == RecordType.PHANTOM_BALLOT) {
      if (cvr_info.isPresent()) {
        result = OptionalInt.of(computePhantomBallotDiscrepancy(cvr_info.get(), my_contest_result));
      } else {
        //not sure why exactly, but that is what computePhantomBallotDiscrepancy
        //returns if winner_votes is empty, which it is, in this case, if it is
        //not present
        result = OptionalInt.of(1);
      }
    } else if (cvr.recordType() == RecordType.PHANTOM_RECORD){
      // similar to the phantom ballot, we use the worst case scenario, a 2-vote
      // overstatement, except here, we don't have a CVR to check anything on.
      result = OptionalInt.of(2);
    } else if (cvr_info.isPresent() && acvr_info.isPresent()) {
      if (acvr_info.get().consensus() == ConsensusValue.NO) {
        // a lack of consensus for this contest is treated
        // identically to a phantom ballot
        result = OptionalInt.of(computePhantomBallotDiscrepancy(cvr_info.get(), my_contest_result));
      } else {
        result = computeAuditedBallotDiscrepancy(cvr_info.get(), acvr_info.get());
      }
    }

    return result;
  }

  /**
   * Computes the discrepancy between two ballots. This method returns an optional
   * int that, if present, indicates a discrepancy. There are 5 possible types of
   * discrepancy: -1 and -2 indicate 1- and 2-vote understatements; 1 and 2 indicate
   * 1- and 2- vote overstatements; and 0 indicates a discrepancy that does not
   * count as either an under- or overstatement for the RLA algorithm, but
   * nonetheless indicates a difference between ballot interpretations.
   *
   * @param the_cvr_info The CVR info.
   * @param the_acvr_info The ACVR info.
   * @return an optional int that is present if there is a discrepancy and absent
   * otherwise.
   */
  @SuppressWarnings({"PMD.ModifiedCyclomaticComplexity", "PMD.StdCyclomaticComplexity",
        "PMD.NPathComplexity", "PMD.ExcessiveMethodLength",
        "checkstyle:methodlength"})

  private OptionalInt computeAuditedBallotDiscrepancy(final CVRContestInfo the_cvr_info,
                                                      final CVRContestInfo the_acvr_info) {
    // Check for overvotes.
    //
    // Overvotes are represented, perhaps confusingly, in the CVR as "all
    // zeroes" for the given contest - it will look indistinguishable from a
    // contest in which no selections were made. We therefore have to check if
    // the number of selections the audit board found is less than or equal to
    // the allowed votes for the given contest. If it is, then the audit board
    // found a valid selection and we can proceed with the rest of the math as
    // usual. If not, then the audit board recorded an overvote which we must
    // now make match the way the CVR format records overvotes: we must record
    // *no* selections. The code below does that by excluding the selections
    // submitted by the audit board.
    //
    // If the CVR does show an overvote (no selections counted) then our
    // zero-selection ACVR will match it and we will find no discrepancies. If,
    // however, the CVR *did* show a selection but the audit board recorded an
    // overvote, then we will be able to calculate the discrepancy - the CVR
    // will have a choice (or choices) marked as selected, but the ACVR will
    // not. The converse is also true: if the CVR shows an overvote but the
    // audit board records a valid selection, we will calculate an expected
    // discrepancy.
    final Set<String> acvr_choices = new HashSet<>();
    if (the_acvr_info.choices().size() <= my_contest_result.winnersAllowed()) {
      acvr_choices.addAll(the_acvr_info.choices());
    }

    // avoid linear searches on CVR choices
    final Set<String> cvr_choices = new HashSet<>(the_cvr_info.choices());
    // if the choices in the CVR and ACVR are identical now, we can simply return the
    // fact that there's no discrepancy
    if (cvr_choices.equals(acvr_choices)) {
      return OptionalInt.empty();
    }

    // we want to get the maximum pairwise update delta, because that's the "worst"
    // change in a pairwise margin, and the discrepancy we record; we start with
    // Integer.MIN_VALUE so our maximization algorithm works. it is also the case
    // that _every_ pairwise margin must be increased for an understatement to be
    // reported

    int raw_result = Integer.MIN_VALUE;

    boolean possible_understatement = true;
    // FIXME my_contest_result is global to this object. I'd rather it
    // be an argument to this function.
    for (final String winner : my_contest_result.getWinners()) {
      final int winner_change;
      if (!cvr_choices.contains(winner) && acvr_choices.contains(winner)) {
        // this winner gained a vote
        winner_change = 1;
      } else if (cvr_choices.contains(winner) && !acvr_choices.contains(winner)) {
        // this winner lost a vote
        winner_change = -1;
      } else {
        // this winner's votes didn't change
        winner_change = 0;
      }
      if (my_contest_result.getLosers().isEmpty()) {
        // if there are no losers, we'll just negate this number - even though in
        // real life, we wouldn't be auditing the contest at all
        raw_result = Math.max(raw_result, -winner_change);
      } else {
        for (final String loser : my_contest_result.getLosers()) {
          final int loser_change;
          if (!cvr_choices.contains(loser) && acvr_choices.contains(loser)) {
            // this loser gained a vote
            loser_change = 1;
          } else if (cvr_choices.contains(loser) && !acvr_choices.contains(loser)) {
            // this loser lost a vote
            loser_change = -1;
          } else {
            // this loser's votes didn't change
            loser_change = 0;
          }
          // the discrepancy is the loser change minus the winner change (i.e., if this
          // loser lost a vote (-1) and this winner gained a vote (1), that's a 2-vote
          // understatement (-1 - 1 = -2). Overstatements are worse than understatements,
          // as far as the audit is concerned, so we keep the highest discrepancy
          final int discrepancy = loser_change - winner_change;

          // taking the max here does not cause a loss of information even if the
          // discrepancy is 0; if the discrepancy is 0 we can no longer report an
          // understatement, and we still know there was a discrepancy because we
          // didn't short circuit earlier
          raw_result = Math.max(raw_result, discrepancy);

          // if this discrepancy indicates a narrowing of, or no change in, this pairwise
          // margin, then an understatement is no longer possible because that would require
          // widening _every_ pairwise margin
          if (discrepancy >= 0) {
            possible_understatement = false;
          }
        }
      }
    }

    if (raw_result == Integer.MIN_VALUE) {
      // this should only be possible if something went horribly wrong (like the contest
      // has no winners)
      throw new IllegalStateException("unable to compute discrepancy in contest " +
                                      contestResult().getContestName());
    }

    final OptionalInt result;

    if (possible_understatement) {
      // we return the raw result unmodified
      result = OptionalInt.of(raw_result);
    } else {
      // we return the raw result with a floor of 0, because we can't report an
      // understatement
      result = OptionalInt.of(Math.max(0, raw_result));
    }

    return result;
  }

  /**
   * Computes the discrepancy between a phantom ballot and the specified
   * CVRContestInfo.
   * @return The number of discrepancies
   */
  private Integer computePhantomBallotDiscrepancy(final CVRContestInfo cvrInfo,
                                                  final ContestResult contestResult) {
    int result = 2;
    // the second predicate means "no contest winners had votes on the
    // original CVR"
    final Set<String> winner_votes = new HashSet<>(cvrInfo.choices());
    winner_votes.removeAll(contestResult.getLosers());
    if (winner_votes.isEmpty()) {
      result = 1;
    }
    return result;
  }

  /**
   * a good idea
   */
  @Override
  public String toString() {
    return  String.format("[ComparisonAudit for %s: counties=%s, auditedSampleCount=%d, overstatements=%f,"
                          + " contestResult.contestCvrIds=%s, status=%s, reason=%s]",
                          this.contestResult().getContestName(),
                          this.contestResult().getCounties(),
                          this.getAuditedSampleCount(),
                          this.getOverstatements(),
                          this.getContestCVRIds(),
                          my_audit_status,
                          this.auditReason());
  }
}
