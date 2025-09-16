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

import au.org.democracydevelopers.corla.query.AssertionQueries;
import com.google.inject.internal.util.ImmutableList;

import java.util.*;
import javax.persistence.*;

import us.freeandfair.corla.math.Audit;
import us.freeandfair.corla.model.*;

import java.math.BigDecimal;

import au.org.democracydevelopers.corla.model.assertion.Assertion;

import static java.util.Collections.max;

/**
 * A class representing the state of a single audited IRV contest across one or more counties.
 * When an IRVComparisonAudit is constructed, the base ComparisonAudit constructor is called.
 * This will invoke methods to compute initial sample sizes for the audit. For IRV audits,
 * however, these initial sample sizes will not be meaningful as the audit's assertions are
 * not loaded until the ComparisonAudit base constructor finished. The IRVComparisonAudit
 * constructor will load the relevant assertions from the database (populating the audit's
 * assertions list), compute the audit's diluted margin, and then re-invoke the sample size
 * computation methods. This class overrides some of the methods in the base ComparisonAudit
 * class: recalculateSamplesToAudit(); initialSamplesToAudit(); computeDiscrepancy();
 * riskMeasurement(); removeDiscrepancy(); and recordDiscrepancy().
 *
 * DISCREPANCY MANAGEMENT
 * Colorado-rla uses the following logic for computing and storing discrepancies:
 * 1. Call the ComparisonAudit's computeDiscrepancy() method given a CVR and audited ballot.
 * Return any discrepancy found as an OptionalInt.
 * 2. If there was a discrepancy, call the ComparisonAudit's recordDiscrepancy() method N times
 * where N is the number of times the ballot appears in the sample (sampling is by replacement).
 *
 * RE-AUDITING OF BALLOTS:
 * If a ballot is being re-audited, all discrepancies associated with the matching CVR are
 * removed by calling removeDiscrepancy() N times where N is the number of times the ballot
 * appears in the sample. Steps 1, and 2 if needed, above are then performed.
 *
 * For IRV audits, a CVR and audited ballot may represent a different type of discrepancy for
 * different assertions, or a discrepancy for some assertions and not for others. When this
 * class' computeDiscrepancy() method is called, a corresponding method in each of the audit's
 * assertions is called. If a discrepancy is found with respect to an assertion, the CVR ID and
 * discrepancy type will be stored in its cvrDiscrepancy map. Each assertion has its own
 * recordDiscrepancy() and removeDiscrepancy() method. When their recordDiscrepancy() method
 * is called, its cvrDiscrepancy map will be looked up to find the right discrepancy type, and the
 * assertion's internal discrepancy totals updated. Similarly, when removeDiscrepancy() is called,
 * the cvrDiscrepancy map will be looked up to find the discrepancy type, and the assertion's
 * discrepancy totals updated. The CVR ID - discrepancy type entry in its cvrDiscrepancy() map
 * is not removed, however, it is only removed if computeDiscrepancy() is called again and either
 * a different or no discrepancy is found.
 */
@Entity
@DiscriminatorValue("IRV")
public class IRVComparisonAudit extends ComparisonAudit {

  /**
   * List of assertions associated with this audit -- the set of all assertions in the database
   * table 'assertion' whose contest name matches that of this ComparisonAudit (located in the
   * base classes' contest result attribute, 'my_contest_resylt').
   */
  @ManyToMany()
  @JoinTable(name = "audit_to_assertions", joinColumns = { @JoinColumn(name = "id") })
  private List<Assertion> assertions;

  /**
   * Constructs a new, empty IRVComparisonAudit (solely for persistence).
   */
  public IRVComparisonAudit() {
      super();
  }

  /**
   * Constructs an IRVComparisonAudit for the contest described in the given ContestResult,
   * with the given risk limit, and audit reason. This constructor calls the base class
   * constructor, and then populates this IRV audit's list of assertions by collecting all
   * assertions from the database associated with the given contest. It then calculates and
   * sets the audit's diluted margin (stored in the base class) and calls methods for
   * establishing the optimistic and estimated sample size for the audit. This follows the
   * logic of the original ComparisonAudit class for Plurality contests. A RuntimeException is
   * thrown if an unexpected error arises when retrieving assertions from the database, or in the
   * computation of optimistic and estimated sample sizes.
   *
   * @param contestResult The contest result (identifies the contest under audit). This is updated
   *                      with the winner returned
   * @param riskLimit The risk limit.
   * @param auditReason The audit reason.
   */
  @SuppressWarnings({"PMD.ConstructorCallsOverridableMethod"})
  public IRVComparisonAudit(final ContestResult contestResult, final BigDecimal riskLimit,
                            final AuditReason auditReason)  {
    super(contestResult, riskLimit, BigDecimal.ONE, Audit.GAMMA, auditReason);

    final String prefix = "[IRVComparisonAudit all args constructor]";
    final String contestName = contestResult.getContestName();

    LOGGER.debug(String.format("%s called for contest %s risk limit %f and audit reason %s.",
        prefix, contestName, riskLimit, auditReason));

    // Populate the list of assertions belonging to this audit (retrieve all assertions from
    // the database whose contest name matches that in the given contestResult). If an
    // unexpected error arises in retrieving this data from the database, matching() will throw
    // a RunTimeException.
    assertions = AssertionQueries.matching(contestName);

    LOGGER.debug(String.format("%s retrieved %d assertions for contest %s.",
        prefix, assertions.size(), contestName));

    try {
      // If there are no assertions for this audit, then the contest is not auditable.
      if (assertions.isEmpty()) {
        // Setting the diluted margin to 0, as the contest is not auditable.
        diluted_margin = BigDecimal.ZERO;
        setAuditStatus(AuditStatus.NOT_AUDITABLE);
        LOGGER.debug(String.format("%s contest %s is not auditable, setting diluted margin to 0," +
            "and status to NOT_AUDITABLE.", prefix, contestName));
      } else {
        // Assign a diluted margin to the audit. This is equal to the smallest diluted margin of
        // any assertion in 'assertions' (ie. the hardest to audit assertion).
        diluted_margin = Collections.min(assertions.stream().map(Assertion::getDilutedMargin).toList());

        // Initialise the status of the audit.
        setAuditStatus(AuditStatus.NOT_STARTED);

        // As the super class invokes sample size estimation methods in its constructor,
        // we need to invoke them here after the audit's assertions have been populated. Otherwise,
        // they will not have been computed correctly for IRV.
        my_optimistic_recalculate_needed = true;
        optimisticSamplesToAudit();
        estimatedSamplesToAudit();

        LOGGER.debug(String.format("%s Created IRVComparisonAudit for contest %s: status = %s; " +
                "reason = %s; diluted margin = %f; assertions = %d; optimistic = %d; estimated = %d",
            prefix, contestName, auditStatus(), auditReason(), diluted_margin, assertions.size(),
            my_optimistic_samples_to_audit, my_estimated_samples_to_audit));
      }
    } catch(Exception e){
      final String msg = String.format("%s An unexpected error arose during diluted margin " +
          "and initial sample size computation when instantiating an IRVComparison audit " +
          "for contest %s: %s", prefix, contestName, e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   *  Recalculates the estimated number of ballots to audit, and updates the audit's current level
   *  of risk, setting the base class' `my_optimistic_samples_to_audit` and
   *  `my_estimated_samples_to_audit` attributes in the process. Each assertion in this audit is
   *  updated with its current risk. The method will check whether the optimistic sample size
   *  of the audit (the number of ballots we expect to need to sample assuming no further
   *  overstatements arise) needs to be recalculated. If so, each assertion's optimistic sample
   *  size will be computed, and we will take the maximum of these as the optimistic sample size
   *  for the audit as a whole. We then call each assertion's estimated sample size computation
   *  method, and take the maximum of these as the estimated sample size of the audit as a whole.
   * A RuntimeException is thrown when an unexpected error arises during sample size computation. This
   * calculation involves both newly added code and existing code within various parts of colorado-rla.
   */
  @Override
  protected void recalculateSamplesToAudit() {
    if(assertions == null){
      // We have not associated assertions with this audit yet. When an IRVComparisonAudit is
      // constructed, we call the base class constructor first. The ComparisonAudit constructor
      // calls sample size computation methods. So, this method may end up being called before
      // the IRVComparisonAudit constructor has completed its setup (retrieving assertions from
      // the database and so on). In which case, we simply ignore the call.
      return;
    }

    final String prefix = "[recalculateSamplesToAudit]";
    final String contestName = getContestName();

    try {
      if (assertions.isEmpty()) {
        // This contest is not auditable.
        // There are no assertions in this audit (Note: this is distinct from the above case where
        // we have not yet *retrieved* assertions and initialised the 'assertions' attribute).
        my_optimistic_samples_to_audit = 0;
        my_estimated_samples_to_audit = 0;
        my_optimistic_recalculate_needed = false;
        my_estimated_recalculate_needed = false;

        LOGGER.debug(String.format("%s No assertions for contest %s; setting optimistic and " +
                "estimate sample sizes to 0; and optimistic and estimated recalculate needed to false.",
            prefix, getContestName()));
      } else {
        // Sample size computation works by (1) computing the optimistic sample size if its
        // current value is out of date, and then (2) computing estimated sample size (which uses
        // the optimistic sample size as a starting point). We first check whether we need to update
        // the optimistic sample size stored in the base ComparisonAudit class.
        if (my_optimistic_recalculate_needed) {
          // We compute the optimistic sample size for each of the IRVComparisonAudit's
          // assertions and take the largest of these as the optimistic sample size of the
          // audit as a whole.
          LOGGER.debug(String.format("%s calling computeOptimisticSamplesToAudit() for each " +
              "assertion in contest %s given risk limit %f.", prefix, contestName, getRiskLimit()));
          my_optimistic_samples_to_audit = max(assertions.stream().map(a ->
              a.computeOptimisticSamplesToAudit(getRiskLimit())).toList());
          my_optimistic_recalculate_needed = false;
          LOGGER.debug(String.format("%s optimistic sample size of %d computed for contest %s.",
              prefix, my_optimistic_samples_to_audit, contestName));
        }

        // We now compute the estimated sample size for each assertion, and take the maximum as
        // the overall estimated sample sizes of the audit.
        LOGGER.debug(String.format("%s calling computeEstimatedSamplesToAudit() for each " +
                "assertion in contest %s given current audited sample count of %d.", prefix,
            contestName, getAuditedSampleCount()));
        my_estimated_samples_to_audit = max(assertions.stream().map(a ->
            a.computeEstimatedSamplesToAudit(getAuditedSampleCount())).toList());
        my_optimistic_recalculate_needed = false;

        // Tell each assertion to update its risk calculation, return maximum risk across
        // assertions. This will record an updated risk in each assertion's currentRisk attribute.
        final BigDecimal risk = max(assertions.stream().map(a ->
            a.riskMeasurement(getAuditedSampleCount())).toList());

        LOGGER.debug(String.format("%s estimated sample size of %d computed for contest %s; risk %f.",
            prefix, my_estimated_samples_to_audit, contestName, risk));
      }
    } catch(Exception e){
      // An unexpected error has arisen during sample size computation.
      final String msg = String.format("%s An unexpected error arose in sample size " +
          "computation for the IRVComparisonAudit of contest %s: %s.", prefix, contestName,
          e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * This method computes and returns the initial (optimistic) sample size for the audit. This is
   * computed by calling an initial sample size computation method in each assertion, and returning
   * the maximum of the returned values.
   * @return the initial (optimistic) sample size for the audit.
   */
  @Override
  public int initialSamplesToAudit() {
    if(assertions == null){
      // We have not associated assertions with this audit yet.
      // In this case, we simply ignore the call. Whether this is an error depends on whether
      // this method is invoked in the constructor of the ComparisonAudit class. At this point,
      // the IRVComparisonAudit's assertions list will not have been initialised.
      return 0;
    }

    final String prefix = "[initialSamplesToAudit]";
    final String contestName = getContestName();

    try {
      LOGGER.debug(String.format("%s computing the initial optimistic samples to audit for the " +
              "IRV contest %s.", prefix, contestName));

      if (assertions.isEmpty()) {
        LOGGER.debug(String.format("%s No assertions for contest %s; returning an initial sample " +
            "size of 0.", prefix, contestName));
        return 0;
      } else {
        LOGGER.debug(String.format("%s calling computeInitialOptimisticSamplesToAudit() for each " +
            "assertion in contest %s given risk limit %f.", prefix, contestName, getRiskLimit()));
        final int samples = max(assertions.stream().map(a ->
            a.computeInitialOptimisticSamplesToAudit(getRiskLimit())).toList());
        LOGGER.debug(String.format("%s initial optimistic sample size of %d computed for contest %s.",
            prefix, samples, contestName));
        return samples;
      }
    } catch(Exception e){
      // An unexpected error has arisen during sample size computation.
      final String msg = String.format("%s An unexpected error arose in sample size " +
              "computation for the IRVComparisonAudit of contest %s: %s.", prefix, contestName,
              e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * Computes the discrepancy (if any) between the specified CVR and matching audited ballot (ACVR),
   * with respect to each assertion in this audit and the audit's contest. This method returns an
   * optional int that, if present, indicates that a discrepancy exists.
   * There are 5 possible types of discrepancy: -1 indicates a one vote understatement; a -2
   * indicates a two vote understatement; a 1 indicates a one vote overstatement; a 2 a two vote
   * overstatement; and a 0 an "other" discrepancy (not an understatement or overstatement but
   * a difference in the vote recorded for this audit's contest).
   * Discrepancies are computed with respect to individual assertions. This method will consider
   * each assertion, and compute the discrepancy (if one exists) between the CVR and ACVR for that
   * assertion. If no discrepancy exists for any of the audit's assertions, then an empty
   * optional int will be returned. Otherwise, the method will return the maximum discrepancy
   * arising from this CVR-ACVR pair across all assertions.
   * This method has side effects: calling each assertion's computeDiscrepancy() method will store
   * the CVR ID and discrepancy value of the CVR-ACVR pair *if* a discrepancy was found. If the
   * recordDiscrepancy() method is called with this CVR and ACVR pair, the previously computed
   * discrepancies will be recorded in each assertion's discrepancy counts. This logic follows
   * how discrepancy computation and recording works for Plurality audits. A RuntimeException
   * will be thrown in the following circumstances: the provided CVR/ACVR is null; the
   * IRVComparisonAudit's assertions list is null (not yet set); a RuntimeException has been
   * thrown by an assertion's computeDiscrepancy() method; or an unexpected error has been caught.
   *
   * @param cvr         The CVR that the machine saw.
   * @param auditedCVR  The ACVR that the human audit board saw.
   * @return an optional int that is present if there is a discrepancy between the CVR and audited
   * ballot, for at least one assertion, and absent otherwise.
   */
  @Override
  public OptionalInt computeDiscrepancy(final CastVoteRecord cvr, final CastVoteRecord auditedCVR) {
    final String prefix = "[computeDiscrepancy]";
    final String contestName = getContestName();

    if(cvr == null || auditedCVR == null){
      // This is not valid input.
      final String msg = String.format("%s Calling computeDiscrepancy() with null CVR/ACVR.", prefix);
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }

    LOGGER.debug(String.format("%s Computing discrepancy between CVR ID %d and audited ballot, " +
        "contest %s.", prefix, cvr.id(), contestName));

    // Check that the IRVComparisonAudit's assertions have been initialised. This will throw
    // a RuntimeException if they have not, and log an appropriate error message.
    nullAssertionsCheck(prefix);

    LOGGER.debug(String.format("%s Contest %s: Calling computeDiscrepancy() for each assertion.",
        prefix, contestName));

    // Compute discrepancies with respect to each assertion for this CVR/ACVR pair. If a
    // discrepancy exists for an assertion, add its value to a list.
    try {
      List<Integer> discrepancies = new ArrayList<>();
      for (Assertion a : assertions) {
        final OptionalInt result = a.computeDiscrepancy(cvr, auditedCVR);

        if (result.isPresent()) {
          discrepancies.add(result.getAsInt());
        }
      }

      // If we've found no discrepancies across the assertion set, then none exists for this contest
      // and this CVR/ACVR pair. Otherwise, return the maximum discrepancy found across all assertions.
      if (discrepancies.isEmpty()) {
        LOGGER.debug(String.format("%s Contest %s: No discrepancies found for CVR ID %d.", prefix,
            contestName, cvr.id()));
        return OptionalInt.empty();
      } else {
        final Integer maxDiscrepancy = max(discrepancies);
        LOGGER.info(String.format("%s Contest %s: Maximum discrepancy of %d found for CVR ID %d.",
            prefix, contestName, maxDiscrepancy, cvr.id()));
        return OptionalInt.of(maxDiscrepancy);
      }

    } catch(RuntimeException e){
      final String msg = String.format("%s Contest %s, RuntimeException: %s", prefix,
          contestName, e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);

    } catch(Exception e){
      // Unexpected exception: log and throw as a RuntimeException.
      final String msg = String.format("%s Contest %s, Exception: %s", prefix, contestName,
          e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * Returns the maximum assertion risk across this audit's set of assertions. This method
   * will call each assertion's riskMeasurement() method to update their risk, and return the
   * largest of these computed risks. This method will throw a RuntimeException if called
   * and the IRVComparisonAudit's assertions list is null, or an unexpected error arises during
   * risk computation.
   * @return The largest current level of risk attached to an assertion in this audit.
   */
  @Override
  public BigDecimal riskMeasurement() {
    final String prefix = "[riskMeasurement]";
    final String contestName = getContestName();

    // Check that the IRVComparisonAudit's assertions have been initialised. This will throw
    // a RuntimeException if they have not.
    nullAssertionsCheck(prefix);

    BigDecimal risk = BigDecimal.ONE;
    if(!assertions.isEmpty()){
      LOGGER.debug(String.format("%s IRVComparisonAudit ID %d for contest %s, computing risks.",
          prefix, id(), contestName));
      try {
        risk = max(assertions.stream().map(a -> a.riskMeasurement(getAuditedSampleCount())).toList());
      }
      catch(Exception e){
        final String msg = String.format("%s IRVComparisonAudit ID %d for contest %s, error " +
            "in assertion risk measurement: %s", prefix, id(), contestName, e.getMessage());
        LOGGER.error(msg);
        throw new RuntimeException(msg);
      }
    }

    LOGGER.info(String.format("%s IRVComparisonAudit ID %d for contest %s, risk %f.", prefix,
        id(), contestName, risk));
    return risk;
  }

  /**
   * Removes the specified over/understatement or other discrepancy (the valid range is -2 to 2:
   * -2 and -1 are understatements, 0 is a discrepancy that doesn't affect the RLA calculations,
   * and 1 and 2 are overstatements) corresponding to the given audited ballot. This is typically
   * done when a new interpretation is submitted for a ballot that has already been interpreted
   * (i,e. a ballot is being re-audited). Note that a given CVR/audited ballot can be associated
   * with different types of discrepancies across the audit's set of assertions. In the overall
   * discrepancy counts in the ComparisonAudit base class, the maximum of these per-assertion
   * discrepancies will be used when deciding what type counter to increment/decrement.
   *
   * @param theRecord The CVRAuditInfo record that generated the discrepancy.
   * @param theType The type of discrepancy to remove.
   * @exception IllegalArgumentException if an invalid discrepancy type is specified, or a null
   * CVRAuditInfo record is provided.
   */
  @Override
  public void removeDiscrepancy(final CVRAuditInfo theRecord, final int theType) {
    final String contestName = getContestName();
    final String prefix = String.format("[removeDiscrepancy] IRVComparisonAudit ID %d for " +
        "contest %s:", id(), contestName);

    // Check that the IRVComparisonAudit's assertions have been initialised. This will throw
    // a RuntimeException if they have not.
    nullAssertionsCheck(prefix);

    // Check that the CVRAuditInfo and discrepancy type are valid.
    recordTypeCheck(prefix, theRecord, theType);

    try {
      LOGGER.info(String.format("%s removing discrepancy associated with CVR %d (maximum type %d).",
              prefix, theRecord.cvr().id(), theType));

      // The next check is whether the base classes isCovering(theRecord.id()) method holds.
      // In this case, we just want to call the base classes removeDiscrepancy() method so that
      // the discrepancy, which would be part of the my_discrepancies list, is removed from
      // that list. Note that in this case, the base classes overall discrepancy counts will not
      // have included the discrepancy.
      if(!isCovering(theRecord.id())){
        // Ensure that the base class' record method is called (note that the audit's discrepancy
        // counts *should not be changed*, but the discrepancy should be removed from its list of
        // discrepancies for reporting purposes).
        super.removeDiscrepancy(theRecord, theType);
      }
      else {
        // Remove the discrepancy associated with the given CVRAuditInfo (CVR/ACVR pair), if one
        // exists, in each of the audit's assertions.
        boolean removed = false;
        for (Assertion a : assertions) {
          boolean removed_a = a.removeDiscrepancy(theRecord);
          removed = removed || removed_a;
        }

        // Update the discrepancy tallies in the base ComparisonAudit class, for reporting purposes,
        // and the flag for indicating that a sample size recalculation is needed, *if* we did
        // indeed remove a discrepancy from at least one of this audit's assertions.
        if (removed) {
          super.removeDiscrepancy(theRecord, theType);
        } else {
          // There is an argument that this should actually be a case where an exception should be
          // thrown -- where the audit logic is telling the audit to remove discrepancies that have
          // not been prior computed and recorded. However, the logic of ComparisonAudit does not
          // include these defensive checks.
          LOGGER.warn(String.format("%s no discrepancies removed.", prefix));
        }
      }

      LOGGER.info(String.format("%s total number of overstatements (%d), optimistic sample " +
              "recalculate needed (%s), estimated sample recalculate needed (%s),", prefix,
          discrepancyCount(1)+discrepancyCount(2), my_optimistic_recalculate_needed,
          my_estimated_recalculate_needed));

    } catch(Exception e){
      final String msg = String.format("%s an error arose in the removal of discrepancies " +
          "associated with CVR %d (maximum type %d). %s", prefix, theRecord.cvr().id(), theType,
          e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * Checks if the given CVRAuditInfo contains a discrepancy with respect to one or more of this
   * audit's assertions. If so, it removes the discrepancy from the internal records of the
   * assertion. The CVRAuditInfo details the ID of the CVR involved in the discrepancy. The
   * given discrepancy type (an integer between -2 and 2, inclusive) represents the value of
   * the maximum discrepancy associated with the CVRAuditInfo and one of this audit's assertions.
   * The count for this discrepancy type (theType) is incremented in the base ComparisonAudit's
   * totals.
   *
   * @param theRecord The CVRAuditInfo record that generated the discrepancy.
   * @param theType The type of discrepancy to remove.
   * @exception IllegalArgumentException if an invalid discrepancy type is specified, a null
   * CVRAuditInfo record is provided, or 'theType' is not the maximum discrepancy associated with
   * the given record and this audit's assertions.
   */
  @Override
  public void recordDiscrepancy(final CVRAuditInfo theRecord, final int theType) {
    final String contestName = getContestName();
    final String prefix = String.format("[recordDiscrepancy] IRVComparisonAudit ID %d for " +
        "contest %s:", id(), contestName);

    // Check that the IRVComparisonAudit's assertions have been initialised. This will throw
    // a RuntimeException if they have not.
    nullAssertionsCheck(prefix);

    // Check that the CVRAuditInfo and discrepancy type are valid. This method will throw an
    // IllegalArgumentException if they are not.
    recordTypeCheck(prefix, theRecord, theType);

    LOGGER.info(String.format("%s recording discrepancies for CVR ID %d, max type %d.", prefix,
        theRecord.id(), theType));

    // Check that 'theType' is indeed the maximum discrepancy associated with an assertion in
    // this audit and the CVR referred to in 'theRecord'. If it is not, or if there are no
    // discrepancies associated with the record and an assertion, then an IllegalArgumentException
    // will be thrown.
    List<Integer> types = new ArrayList<>();
    for(Assertion a : assertions){
      OptionalInt d = a.getDiscrepancy(theRecord.id());
      if(d.isPresent()) {
        types.add(d.getAsInt());
      }
    }
    if(types.isEmpty() || max(types) != theType){
      final String msg = String.format("%s %d is not the maximum discrepancy type for CVR %d " +
          "across assertions.", prefix, theType, theRecord.id());
      LOGGER.error(msg);
      throw new IllegalArgumentException(msg);
    }

    // The next check is whether the base classes isCovering(theRecord.id()) method holds. If it
    // doesn't, display a warning. This valid discrepancy is not going to be counted in the
    // base class counts (as isCovering()) does not hold, yet there is a discrepancy.
    if(!isCovering(theRecord.id())){
      final String msg = String.format("%s We have computed a discrepancy for contest %s, CVR %d, " +
          "but that CVR does not cover the contest.", prefix, contestName, theRecord.id());
      LOGGER.warn(msg);

      // Ensure that the base class' record method is called (note that the audit's discrepancy
      // counts will not be changed, but the discrepancy will be stored in its list of
      // discrepancies for reporting purposes).
      super.recordDiscrepancy(theRecord, theType);
      return;
    }

    try {
      // Note if we get to this point, then discrepancies are present against the given CVR
      // in at least one assertion, with 'theType' being the maximum type of these discrepancies.

      // Iterate over the assertions for this audit, check that the CVR in the CVRAuditInfo is
      // listed in their discrepancy map, and if so, record it as a discrepancy in its internal
      // totals. Note that the CVR may represent a different type of discrepancy from assertion to
      // assertion (not necessarily of type 'theType'). The parameter 'theType' will represent the
      // maximum discrepancy associated with CVR/ACVR pair and one of this audit's assertions. Note
      // that a given CVR/ACVR pair can only be associated with a single discrepancy per assertion.
      // Before we call each assertion's recordDiscrepancy(), we check that 'theType' is actually
      // the maximum discrepancy that appears across the assertions for this CVR/ACVR.
      for (Assertion a : assertions) {
        a.recordDiscrepancy(theRecord);
      }

      // Update the discrepancy tallies in the base ComparisonAudit class, for reporting purposes,
      // and the flag for indicating that a sample size recalculation is needed.
      super.recordDiscrepancy(theRecord, theType);

      LOGGER.info(String.format("%s total number of overstatements (%d), optimistic sample " +
              "recalculate needed (%s), estimated sample recalculate needed (%s),", prefix,
          discrepancyCount(1)+discrepancyCount(2),
          my_optimistic_recalculate_needed, my_estimated_recalculate_needed));

    } catch(Exception e){
      final String msg = String.format("%s an error arose in the recording of discrepancies " +
          "associated with CVR %d (maximum type %d). %s", prefix, theRecord.cvr().id(), theType,
          e.getMessage());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * This method is used purely in testing to inspect the assertions present in the
   * IRVComparisonAudit, returned as an ImmutableList.
   * @return An ImmutableList of the assertions present in this IRVComparisonAudit.
   */
  public ImmutableList<Assertion> getAssertions(){
    return ImmutableList.<Assertion>builder().addAll(assertions).build();
  }

  /**
   * Return the overall margin, which is the minimum margin of all the assertions.
   * @return the minimum assertion margin.
   * Defaults to 0 if there are no assertions - this is an established convention for 'not auditable'.
   */
  public int getMinMargin() {
    if(assertions.isEmpty()) {
      return 0;
    }
    return Collections.min(assertions.stream().map(Assertion::getMargin).toList());
  }

  /**
   * This method checks whether this IRVComparisonAudit's list of assertions has been
   * appropriately initialised, and throws a RunTimeException if not. It takes a string identifying
   * the method making this check, to use in logging an appropriate error message.
   * @param prefix String identifying the IRVComparisonAudit method that is performing this check.
   */
  private void nullAssertionsCheck(final String prefix){
    if(assertions == null){
      final String msg = String.format("%s IRVComparisonAudit ID %d for contest %s, null " +
          "assertions list.", prefix, id(), getContestName());
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }

  /**
   * Checks that the given CVRAuditInfo and discrepancy type are valid: that the CVRAuditInfo
   * record is not null (and its CVR is not null); and that the discrepancy type falls in the
   * range -2 to 2. An IllegalArgumentException is logged and thrown otherwise.
   * @param prefix Information (for logging) detailing the method calling this check.
   * @param theRecord The CVRAuditInfo being checked.
   * @param theType The discrepancy type being checked.
   * @throws IllegalArgumentException when the given CVRAuditInfo and discrepancy types are not
   * valid.
   */
  private void recordTypeCheck(final String prefix, final CVRAuditInfo theRecord, final int theType){
    if(theRecord == null){
      // This is an error, and should not happen.
      final String msg = String.format("%s null CVRAuditInfo provided.", prefix);
      LOGGER.error(msg);
      throw new IllegalArgumentException(msg);
    }

    if(theRecord.cvr() == null){
      // This is an error, and should not happen.
      final String msg = String.format("%s null CVR in CVRAuditInfo provided.", prefix);
      LOGGER.error(msg);
      throw new IllegalArgumentException(msg);
    }

    if(theType < -2 || theType > 2){
      // This is an error, and should not happen.
      final String msg = String.format("%s invalid discrepancy type provided of %d.", prefix, theType);
      LOGGER.error(msg);
      throw new IllegalArgumentException(msg);
    }
  }

}
