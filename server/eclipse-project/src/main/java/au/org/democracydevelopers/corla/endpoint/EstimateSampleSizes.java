package au.org.democracydevelopers.corla.endpoint;

import au.org.democracydevelopers.corla.model.ContestType;
import au.org.democracydevelopers.corla.model.IRVComparisonAudit;
import spark.Request;
import spark.Response;
import us.freeandfair.corla.asm.ASMEvent;
import us.freeandfair.corla.controller.ContestCounter;
import us.freeandfair.corla.endpoint.AbstractDoSDashboardEndpoint;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.CountyContestResultQueries;

import java.math.BigDecimal;
import java.math.MathContext;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.text.StringEscapeUtils.escapeCsv;
import static us.freeandfair.corla.controller.ComparisonAuditController.createAuditOfCorrectType;

/**
 * Estimate Sample sizes.
 * This uses ComparisonAudit::estimatedSampleSize() to get the estimated samples for each contest.
 * It can be used either before or during an audit because it makes transient ComparisonAudits (not
 * stored in the database).
 */
public class EstimateSampleSizes extends AbstractDoSDashboardEndpoint {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(EstimateSampleSizes.class);

  /**
   * The event to return for this endpoint.
   */
  private final ThreadLocal<ASMEvent> my_event = new ThreadLocal<ASMEvent>();

  /**
   * {@inheritDoc}
   */
  @Override
  public EndpointType endpointType() {
    return EndpointType.GET;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String endpointName() {
    return "/estimate-sample-sizes";
  }

  /**
   * @return STATE authorization is necessary for this endpoint.
   */
  public AuthorizationType requiredAuthorization() {
    return AuthorizationType.STATE;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected ASMEvent endpointEvent() {
    return my_event.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void reset() {
    my_event.set(null);
  }

  /**
   * Headers for the CSV file. "Ballots cast" are the number that had the contest on them;
   * "total ballots" is the total universe size (which includes all the ballots in any county that
   * ran the contest).
   */
  private static final String[] HEADERS = {
      "County",
      "Contest Name",
      "Contest Type",
      "Ballots Cast",
      "Total ballots",
      "Diluted Margin",
      "Sample Size"
  };

  /**
   * {@inheritDoc}
   */
  @Override
  public String endpointBody(final Request the_request, final Response the_response) {
    final String prefix = "[endpointBody]";
    LOGGER.debug(String.format("%s %s", prefix, "Started Estimate Sample sizes."));

    try {
      final String data = estimateSampleSizes();

      if (data.isEmpty()) {
        final String msg = "Could not find data for sample size estimation.";
        LOGGER.debug(String.format("%s %s.", prefix, msg));
        dataNotFound(the_response, msg);
      } else {
        the_response.header("Content-Type", "test/csv");
        the_response.header("Content-Disposition", "attachment; filename*=UTF-8''sample_sizes.csv");

        the_response.body((String.join(",", HEADERS)) + "\n" + data);
        LOGGER.debug(String.format("%s %s.", prefix, "Completed sample size estimation"));
        okString(the_response);
      }
    } catch (IllegalStateException e) {
      // This occurs if the risk limit has not been set in the state dashboard.
      dataNotFound(the_response, e.getMessage());
    }
    return my_endpoint_result.get();
  }

  /**
   * Compute sample sizes for all contests for which CountyContestResults exist in the database.
   * This method ignores manifests, instead using the count of uploaded CSVs. This means that the
   * estimate may differ from the estimate computed by estimatedSampleSize() during the audit, if
   * the manifest has more votes than the CVR file.
   * @return A list of string arrays containing rows with the following data: county name,
   * contest name, contest type, single or multi-jurisdictional, ballots cast, diluted margin,
   * and estimated sample size.
   */
  public String estimateSampleSizes() {
    final String prefix = "[estimateSampleSizes]";
    final List<EstimateData> dataRows = new ArrayList<>();

    // For estimation of sample sizes for each audit, we need to collect the ContestResult
    // for each contest. For Plurality audits, this will involve tabulating the votes across
    // Counties for that contest. For preliminary sample size estimation, we assign
    // OPPORTUNISTIC_BENEFITS as the reason for each audit. The tabulated vote totals
    // in a ContestResult for an IRV contest will not be used. In the call to ContestCounter
    // (countAllContests), all persisted CountyContestResults will be accessed from the database,
    // grouped by contest, and accumulated into a single ContestResult.
    // Set the useManifests flag to false, to tell contest counter to use CVR count instead.
    final List<ContestResult> countedCRs = ContestCounter.countAllContests(false).stream().peek(cr ->
        cr.setAuditReason(AuditReason.OPPORTUNISTIC_BENEFITS)).toList();

    // Try to get the DoS Dashboard, which may contain the risk limit for the audit.
    final BigDecimal riskLimit = Persistence.getByID(DoSDashboard.ID, DoSDashboard.class).auditInfo().riskLimit();
    if (riskLimit == null) {
      // If the risk limit is not initialized, it is not possible to estimate sample sizes.
      final String msg = "No risk limit set";
      LOGGER.error(String.format("%s %s.", prefix, msg));
      throw new IllegalStateException(msg);
    }

    // Iterate over all the contest results, getting relevant data into an EstimateData record.
    for (final ContestResult cr : countedCRs) {

      final ComparisonAudit ca = createAuditOfCorrectType(cr, riskLimit);
      final List<String> countyNames = ca.getCounties().stream().map(County::name).toList();

      // The total number of ballots cast that have that contest on it. This is equal to the sum
      // of my_contest_count in the county contest results for the contest.
      final int contestBallots = CountyContestResultQueries.withContestName(ca.getContestName())
          .stream().mapToInt(CountyContestResult::contestBallotCount).sum();

      dataRows.add(new EstimateData(countyNames, ca, contestBallots, cr.getBallotCount()));
    }

    // Sort the data according to the name of the county, and then by contest.
    Collections.sort(dataRows);
    return String.join("\n", dataRows.stream().map(EstimateData::toString).toList());
  }

  /**
   * A record to store the data we need for each row in the sample size estimate csv. Stores relevant
   * data (in some cases after retrieving it from more complex structures) and outputs it as a CSV
   * row (toString()).
   * @param countyName       The name of the county.
   * @param contestName      The name of the contest.
   * @param contestType      The type, as a string, IRV or plurality.
   * @param contestBallots   The number of ballots that actually contained the contest.
   * @param totalBallots     The total universe of ballots for this contest's audit.
   * @param dilutedMargin    The margin divided by the total Auditable ballots.
   * @param estimatedSamples The estimated samples to audit.
   */
  public record EstimateData(String countyName, String contestName, String contestType,
                              int contestBallots, long totalBallots, BigDecimal dilutedMargin,
                              int estimatedSamples) implements Comparable<EstimateData> {

    /**
     * Construct the record by pulling relevant data out of the ComparisonAudit data structure.
     * If the county name is unique, we print it; if not, we just print "Multiple."
     *
     * @param countyNames    The list of names of the Counties in which the contest occurs.
     * @param ca             The comparisonAudit (used to extract the type, diluted margin, and estimated samples).
     * @param contestBallots The number of ballots that actually contained the contest.
     * @param totalBallots   The total universe of ballots for this contest's audit.
     */
    public EstimateData(final List<String> countyNames, final ComparisonAudit ca,
                        final int contestBallots, final long totalBallots) {
      this(countyNames.size() == 1 ? countyNames.get(0) : "Multiple",
          ca.getContestName(),
          ca instanceof IRVComparisonAudit ? ContestType.IRV.toString() : ContestType.PLURALITY.toString(),
          contestBallots,
          totalBallots,
          ca.getDilutedMargin(),
          ca.estimatedSamplesToAudit()
      );
    }


    /**
     * Print the record as a csv row.
     * Headers:
     * "County", "Contest Name", "Contest Type", "Ballots Cast", "Total ballots", "Diluted Margin", "Sample Size"
     *
     * @return all the data as a csv row.
     */
    @Override
    public String toString() {
      return String.join(",",
          List.of(
              escapeCsv(countyName),
              escapeCsv(contestName),
              contestType,
              "" + contestBallots,
              "" + totalBallots,
              // Format the diluted margin to have two significant figures.
              new DecimalFormat("0.##############").format(dilutedMargin.round(new MathContext(2))),
              // Max out the estimatedSamples at totalAuditableBallots, because it looks weird to have a sample
              // size greater than the possible universe.
              "" + Math.min(estimatedSamples, totalBallots)
          ));
    }

    /**
     * For sorting the csv rows. First compare the county names as strings; if they're equal, use the
     * contest name as a secondary sort.
     * @param  anotherRow other row to be compared with.
     * @return -1 if this value is (lexicographically by county and contest name) less than the other value,
     *         0 if their county and contest names are equal (which is unexpected)
     *         +1 if this value is greater than the other value.
     */
    @Override
    public int compareTo(final EstimateData anotherRow) {
      // If the county names are the same, sort by contest name.
      if (countyName.equals(anotherRow.countyName)) {
        return contestName.compareTo(anotherRow.contestName());
      }
      // If the county names are different, sort by county name.
      return countyName.compareTo(anotherRow.countyName);
    }
  }
}
