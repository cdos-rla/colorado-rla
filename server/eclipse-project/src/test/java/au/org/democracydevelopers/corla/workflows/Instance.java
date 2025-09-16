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

package au.org.democracydevelopers.corla.workflows;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Object for storing all the data describing a test workflow, mapping to a workflow instance
 * JSON.
 */
public class Instance {

  /**
   * Use -1 to indicate an infinite number of expected rounds.
   */
  public static final int INFINITE_ROUNDS = -1;

  /**
   * Specification of reaudit choices and consensus for a specific contest on a CVR.
   * @param choices    Choices, as a list of strings, for the relevant contest/CVR.
   * @param consensus  Whether the audit board reached a consensus on those choices ("YES","NO").
   */
  public record ReAuditDetails(
      @JsonProperty("choices") List<String> choices,
      @JsonProperty("consensus") String consensus) {}

  /**
   * Name of the workflow instance.
   */
  @JsonProperty("NAME")
  private String name;

  /**
   * Risk limit for the test audit.
   */
  @JsonProperty("RISK_LIMIT")
  private BigDecimal riskLimit;

  /**
   * Random seed for the test audit.
   */
  @JsonProperty("SEED")
  private String seed;

  /**
   * List of additional SQL files (as path strings) containing extra data with which to
   * initialise the database. County data is automatically added to the database when
   * running workflows.
   */
  @JsonProperty("SQLS")
  private List<String> sqls;

  /**
   * List of CVR csv files (path strings), one for each county.
   */
  @JsonProperty("CVRS")
  private List<String> cvrs;

  /**
   * List of ballot manifest files (path strings), one for each county.
   */
  @JsonProperty("MANIFESTS")
  private List<String> manifests;

  /**
   * Path (as a string) to the canonicalisation file.
   */
  @JsonProperty("CANONICAL_LIST")
  private String canonicalList;

  /**
   *  Map between old contest names and their new ones -- for use in canonicalisation.
   *   "CONTEST_NAME_CHANGE" : {
   *     "Regent of the University of Colorado - At Large" : "Regent of the University of Colorado"
   *   },
   */
  @JsonProperty("CONTEST_NAME_CHANGE")
  private Map<String,String> contestNameChanges;

  /**
   * Map between contest names (after canonicalisation of contest names) and old candidate names
   * with their new values -- for use in canonicalisation.
   * "CANDIDATE_NAME_CHANGE" : {
   *   "Regent of the University of Colorado" : {
   *      "Clear Winner" : "Distant Winner"
   *   }
   * },
   */
  @JsonProperty("CANDIDATE_NAME_CHANGE")
  private Map<String,Map<String,String>> candidateNameChanges;

  /**
   * Contests targeted for audit, map between contest name and audit reason.
   */
  @JsonProperty("TARGETS")
  private Map<String,String> targets;

  /**
   * Map between contest name (selected contests) and their winner(s) (as a String). Used for
   * testing reports.
   */
  @JsonProperty("WINNERS")
  private Map<String,String> winners;

  /**
   * Map between contest name (selected contests) and their raw margin. Used for testing reports.
   */
  @JsonProperty("RAW_MARGINS")
  private Map<String,Integer> rawMargins;

  /**
   * Expected diluted margins for targeted contests.
   */
  @JsonProperty("DILUTED_MARGINS")
  private Map<String,Double> dilutedMargins;

  /**
   * Expected sample sizes for targeted contests (first round).
   */
  @JsonProperty("EXPECTED_SAMPLES")
  private Map<String,Integer> expectedSamples;

  /**
   * Expected number of audited ballots for each targeted contest, after all rounds.
   */
  @JsonProperty("FINAL_EXPECTED_AUDITED_BALLOTS")
  private Map<String,Integer> finalExpectedSamples;

  /**
   * Expected final optimistic sample count for selected contests, after all rounds.
   */
  @JsonProperty("FINAL_EXPECTED_OPTIMISTIC_SAMPLES")
  private Map<String,Integer> finalExpectedOptimistic;

  /**
   * Expected final estimated sample count for selected contests, after all rounds.
   */
  @JsonProperty("FINAL_EXPECTED_ESTIMATED_SAMPLES")
  private Map<String,Integer> finalExpectedEstimated;

  /**
   * Subset of contests targeted for audit that are IRV contests.
   */
  @JsonProperty("IRV_CONTESTS")
  private List<String> irvContests;

  /**
   * Number of rounds of auditing that we expect will take place. Optional (can be null).
   */
  @JsonProperty("EXPECTED_ROUNDS")
  private Integer expectedRounds;

  /**
   * Round by round results for contests whose statuses we want to test.
   */
  @JsonProperty("CONTEST_RESULTS")
  private Map<Integer,Map<String,Map<String,Integer>>> contestResults;

  /**
   * Round by round results for counties whose statuses we want to test.
   * Example:
   *   "COUNTY_RESULTS" : {
   *     "1" : {
   *       "32" : {
   *         "discrepancy_count" : {
   *           "UNAUDITED_CONTEST" : 1
   *         }
   *       }
   *     }
   *   },
   * In the above, the round number is the first key (round 1), and the county number is the key of the
   * second map (county 32 in the example).
   */
  @JsonProperty("COUNTY_RESULTS")
  private Map<Integer,Map<String,Map<String,Map<String,Integer>>>> countyResults;

  /**
   * Imprinted CVR ids that should map to phantom ballots. Organised as a map between county ID
   * (as a string), and the list of imprinted IDs for ballots in that county that we want to convert
   * to Phantoms.
   */
  @JsonProperty("PHANTOM_BALLOTS")
  private Map<String,List<String>> phantomBallots;

  /**
   * Identifying information for contests on CVRs that we want to introduce an audit board
   * disagreement. Organised as a map between county ID (as a string), and a map between
   * ballot imprinted ID and the names of the contests for which we want disagreements injected.
   * Example entry:
   *     "4" : {
   *       "1-5-9" : [
   *         "Uralla Mayoral"
   *       ]
   *     }
   */
  @JsonProperty("DISAGREEMENTS")
  private Map<String,Map<String,List<String>>> disagreements;

  /**
   * Audited ballot choices for select CVR Ids and contests. The instance JSON records only the
   * ones we want to be different to what is on the CVR. Example map element (below). We are
   * specifying discrepant choices for ballot 108-1-87 in county 7, for the contest Byron Mayoral.
   *     "7" : {
   *         "108-1-87": [
   *           {
   *             "Byron Mayoral": {
   *               "choices" : [
   *                 "LYON Michael(1)",
   *                 "DEY Duncan(2)"
   *               ],
   *               "consensus" : "YES"
   *             }
   *           }
   *         ]
   *     }
   */
  @JsonProperty("DISCREPANT_AUDITED_BALLOT_CHOICES")
  private Map<String,Map<String,Map<String,List<String>>>> actualChoices;

  /**
   * Specification of ballots to reaudit. Organised as a map between county ID,
   * and a map between CVR imprinted ID and a list of maps that define the paper
   * ballot choices to upload for selected contests (where we want them to differ
   * from what's on the CVR). These choices, and whether the audit board reached
   * a consensus on those choices, are defined in terms of a ReAuditDetails record.
   */
  @JsonProperty("REAUDITS")
  private Map<String,Map<String,List<Map<String,ReAuditDetails>>>> reaudits;

  /**
   * Names of contests present in selected counties, when verifying the reports after
   * conclusion of the audit, the contests identified for these counties will be checked
   * against what is expected. Map between county name and the list of contests for that county.
   */
  @JsonProperty("CONTESTS_BY_SELECTED_COUNTIES")
  private Map<String,List<String>> contestsByCounty;

  /**
   *  Record, for selected plurality contests, of the available choices and their tallies.
   *  Example:
   *   "PLURALITY_TABULATION" : {
   *     "Adams COUNTY COMMISSIONER DISTRICT 3" : {
   *       "Distant Winner" : 122,
   *       "Distant Loser" : 14
   *     },
   *     "Alamosa COUNTY COMMISSIONER DISTRICT 3" : {
   *       "Distant Winner" : 122,
   *       "Distant Loser" : 14
   *     },
   *     "Regent of the University of Colorado" : {
   *       "Jeff Baker" : 81,
   *       "Janet Lee Cook" : 2
   *     }
   *   },
   */
  @JsonProperty("PLURALITY_TABULATION")
  private Map<String,Map<String,Integer>> pluralityTabulation;

  /**
   * Record, for selected counties and plurality contests within those counties, of the available
   * choices and their tallies (within that county only).
   * Example:
   *   "PLURALITY_COUNTY_TABULATION" : {
   *     "0" : {
   *       "Adams COUNTY COMMISSIONER DISTRICT 3": {
   *         "Jeff Baker": 43,
   *         "Janet Lee Cook": 1
   *       },
   *       "Regent of the University of Colorado" : {
   *         "Distant Winner" : 122,
   *         "Distant Loser" : 14
   *       }
   *     },
   *     "1" : {
   *       "Alamosa COUNTY COMMISSIONER DISTRICT 3": {
   *         "Jeff Baker": 38,
   *         "Janet Lee Cook": 1
   *       },
   *       "Regent of the University of Colorado" : {
   *         "Distant Winner" : 122,
   *         "Distant Loser" : 14
   *       }
   *     }
   *   },
   */
  @JsonProperty("PLURALITY_COUNTY_TABULATION")
  private Map<String,Map<String,Map<String,Integer>>> pluralityCountyTabulation;
  /**
   * Constructs an empty workflow instance.
   */
  public Instance(){
    name = "";
    riskLimit = BigDecimal.ONE;
    seed = "";
    targets = new HashMap<>();
    winners = new HashMap<>();
    rawMargins = new HashMap<>();
    canonicalList = "";
    contestNameChanges = new HashMap<>();
    candidateNameChanges = new HashMap<>();
    manifests = new ArrayList<>();
    cvrs = new ArrayList<>();
    sqls = new ArrayList<>();
    dilutedMargins = new HashMap<>();
    expectedSamples = new HashMap<>();
    irvContests = new ArrayList<>();
    contestResults = new HashMap<>();
    countyResults = new HashMap<>();
    phantomBallots = new HashMap<>();
    actualChoices = new HashMap<>();
    disagreements = new HashMap<>();
    reaudits = new HashMap<>();
    expectedRounds = null;
    contestsByCounty = new HashMap<>();
    finalExpectedOptimistic = new HashMap<>();
    finalExpectedEstimated = new HashMap<>();
    pluralityTabulation = new HashMap<>();
    pluralityCountyTabulation = new HashMap<>();
  }

  /**
   * @return SQL files (as path strings) representing additional data to load
   * into the database for the given workflow instance. Returned as an
   * unmodifiable list.
   */
  public List<String> getSQLs(){
    return Collections.unmodifiableList(sqls);
  }

  /**
   * @return CVR files (as path strings) for each county, in order. Returned as an
   * unmodifiable list.
   */
  public List<String> getCVRs(){
    return Collections.unmodifiableList(cvrs);
  }

  /**
   * @return Manifest files (as path strings) for each county, in order. Returned as an
   * unmodifiable list.
   */
  public List<String> getManifests(){
    return Collections.unmodifiableList(manifests);
  }

  /**
   * @return Risk limit for this test workflow.
   */
  public BigDecimal getRiskLimit(){
    return riskLimit;
  }

  /**
   * @return Path (as string) to the canonicalisation file for this test workflow.
   */
  public String getCanonicalisationFile(){
    return canonicalList;
  }

  /**
   * @return Mapping between old contest names and the new names for those contests
   * that should be applied during canonicalisation, as an unmodifiable map.
   */
  public Map<String,String> getContestNameChanges() {
    return Collections.unmodifiableMap(contestNameChanges);
  }

  /**
   * @return Mapping between contest names, and a map of candidate name changes to be
   * applied during canonicalisation, as an unmodifiable map.
   */
  public Map<String,Map<String,String>> getCandidateNameChanges(){
    return Collections.unmodifiableMap(candidateNameChanges);
  }

  /**
   * @return Seed for the test audit (as a string).
   */
  public String getSeed() { return seed;}

  /**
   * @return Unmodifiable map of targeted contests (contest name key and value is audit reason).
   */
  public Map<String,String> getTargetedContests(){
    return Collections.unmodifiableMap(targets);
  }

  /**
   * @param contest Contest whose winner we want returned.
   * @return Winner for given contest, if we have that record, and empty optional otherwise.
   */
  public Optional<String> getWinner(final String contest){
    return winners.containsKey(contest) ?
        Optional.of(winners.get(contest).replace("\"","")) : Optional.empty();
  }

  /**
   * @param contest Contest whose raw margin we want returned.
   * @return Raw margin for given contest, if we have that record, and empty optional otherwise.
   */
  public Optional<Integer> getRawMargin(final String contest){
    return rawMargins.containsKey(contest) ? Optional.of(rawMargins.get(contest)) : Optional.empty();
  }

  /**
   * @return Unmodifiable list of IRV contests among those targeted for audit.
   */
  public List<String> getIRVContests(){ return Collections.unmodifiableList(irvContests); }

  /**
   * @return Audit reason of the given targeted contest, and an empty optional if that contest is
   * not present in the instances records.
   */
  public Optional<String> getTargetedContestReason(final String contest){
    return targets.containsKey(contest) ? Optional.of(targets.get(contest)) : Optional.empty();
  }

  /**
   * @return Unmodifiable mapping between targeted contest name and diluted margin.
   */
  public Map<String,Double> getDilutedMargins(){
    return Collections.unmodifiableMap(dilutedMargins);
  }

  /**
   * @return Unmodifiable mapping between targeted contest name and expected sample size.
   */
  public Map<String,Integer> getExpectedSamples(){
    return Collections.unmodifiableMap(expectedSamples);
  }

  /**
   * @return Unmodifiable mapping between targeted contest name and expected number of audited
   * ballots after all audit rounds.
   */
  public Map<String,Integer> getExpectedAuditedBallots(){
    return Collections.unmodifiableMap(finalExpectedSamples);
  }

  /**
   * For a given contest, return expected final audited samples count (if we have a record for
   * that, and an optional empty otherwise).
   * @param contest Contest name
   * @return Optional Integer final audited sample count for given contest if we have the record.
   */
  public Optional<Integer> getExpectedAuditedBallots(final String contest){
    return finalExpectedSamples.containsKey(contest) ?
        Optional.of(finalExpectedSamples.get(contest)) : Optional.empty();
  }

  /**
   * For a given contest, return expected final optimistic samples count (if we have a record for
   * that, and an optional empty otherwise).
   * @param contest Contest name
   * @return Optional Integer final optimistic sample count for given contest if we have the record.
   */
  public Optional<Integer> getExpectedOptimisticSamples(final String contest){
    return finalExpectedOptimistic.containsKey(contest) ?
        Optional.of(finalExpectedOptimistic.get(contest)) : Optional.empty();
  }

  /**
   * For a given contest, return expected final estimated samples count (if we have a record for
   * that, and an optional empty otherwise).
   * @param contest Contest name
   * @return Optional Integer final estimated sample count for given contest if we have the record.
   */
  public Optional<Integer> getExpectedEstimatedSamples(final String contest){
    return finalExpectedEstimated.containsKey(contest) ?
        Optional.of(finalExpectedEstimated.get(contest)) : Optional.empty();
  }

  /**
   * @return The number of rounds of auditing we expect will take place, wrapped in an Optional<>
   * that is empty if nothing is specified in the JSON instance.
   */
  public Optional<Integer> getExpectedRounds(){
    return expectedRounds == null ? Optional.empty() : Optional.of(expectedRounds);
  }

  /**
   * Considers a CVR identified by imprinted ID and county ID, and determines if we should
   * treat the matching paper ballot as a phantom ballot.
   * @param id  Imprinted Id of the CVR.
   * @param countyID County id of the CVR
   * @return True if the paper ballot matching the given ID should be treated as a phantom ballot.
   */
  public boolean isPhantomBallot(final String id, final long countyID){
    final String county = String.valueOf(countyID);
    if(phantomBallots.containsKey(county)){
      return phantomBallots.get(county).contains(id);
    }
    return false;
  }

  /**
   * Considers a CVR identified by imprinted ID and county ID, and returns the list of contests
   * that we should introduce a disagreement for on the CVR (if any).
   * @param id  Imprinted Id of the CVR.
   * @param countyID County id of the CVR
   * @return List of names of contests for which we want to introduce a disagreement during auditing
   * of the given CVR.
   */
  public List<String> getDisagreements(final String id, final long countyID){
    final String county = String.valueOf(countyID);
    if(disagreements.containsKey(county)) {
      if (disagreements.get(county).containsKey(id)) {
        return Collections.unmodifiableList(disagreements.get(county).get(id));
      }
    }
    return List.of();
  }

  /**
   * Get the expected result of a given round (in terms of a map between contest name and a
   * map of status type and expected value for that contest). If the round cannot be found in our
   * record of results, it means that we don't want to check their value in our testing.
   * @param round     Round number
   * @return A mapping between contest name and a map of status type and expected value for that
   * contest. Returns an empty Optional if no record for that round is present in our record of results.
   */
  public Optional<Map<String,Map<String,Integer>>> getRoundContestResult(final int round){
    if(contestResults.containsKey(round)){
      return Optional.of(contestResults.get(round));
    }
    return Optional.empty();
  }

  /**
   * Get the expected result of a given round (in terms of a map between county and a
   * map of status type and expected value for that county). If the round cannot be found in our
   * record of results, it means that we don't want to check their value in our testing.
   * @param round     Round number
   * @return A mapping between county and a map of status type and expected value for that
   * county. Returns an empty Optional if no record for that round is present in our record of results.
   */
  public Optional<Map<String,Map<String,Map<String,Integer>>>> getRoundCountyResult(final int round){
    if(countyResults.containsKey(round)){
      return Optional.of(countyResults.get(round));
    }
    return Optional.empty();
  }

  /**
   * If in our workflow instance record, we want a specific choices list to be used as the
   * ACVR entry for a contest and CVR, this method returns the choices to be used. If we want
   * to simply use what is on the CVR (ie. the raw choices that were on the CVR), then this method
   * will return an empty Optional. This method is used for the first auditing of a ballot, and
   * not for reaudits.
   *
   * @param countyID    County ID for the CVR as a string.
   * @param imprintedId Imprinted ID of the CVR that we want to create a choice list for.
   * @param contest     Contest of interest on the CVR.
   * @return An Optional list of strings representing the actual choices to be used for a given
   * contest on an ACVR. If the Optional is empty, it indicates that we should use the CVR choices.
   */
  public Optional<List<String>> getActualChoices(final String countyID, final String imprintedId,
                                                 final String contest) {
    if(actualChoices.containsKey(countyID)) {
      if (actualChoices.get(countyID).containsKey(imprintedId)) {
        if (actualChoices.get(countyID).get(imprintedId).containsKey(contest)) {
          return Optional.of(
              Collections.unmodifiableList(actualChoices.get(countyID).get(imprintedId).get(contest)));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * For a given CVR, identified by county ID and imprinted Id, return (if any) the details of
   * any reaudits we want to perform on that CVR. These details will be a list of maps (string-list of
   * string) where each map details the actual choices to be entered for given contests (where we
   * want them to differ from what is on the CVR). The number of maps in this list equals the number
   * of times we want the CVR to be reaudited.
   * @param countyID     County ID for the CVR being reaudited.
   * @param imprintedId  Imprinted ID of the CVR being reaudited.
   * @return An optional list of maps, one for each reaudit of the CVR, detailing the sets of contests
   * whose choices we want to alter from the CVR, and how they should be altered.
   */
  public Optional<List<Map<String,ReAuditDetails>>> getReAudits(final String countyID, final String imprintedId){
    if(reaudits.containsKey(countyID)){
      if(reaudits.get(countyID).containsKey(imprintedId)){
        return Optional.of(reaudits.get(countyID).get(imprintedId));
      }
    }
    return Optional.empty();
  }

  /**
   * @return Map between selected county names and the full list of contests for that county.
   */
  public Map<String,List<String>> getContestsByCounty(){
    return Collections.unmodifiableMap(contestsByCounty);
  }

  /**
   * Returns a mapping between candidate name and their tally for the given contest, if we
   * have a record of that plurality contest's tallies in the instance. Otherwise, returns an
   * empty optional.
   */
  public Optional<Map<String,Integer>> getPluralityTabulation(final String contest) {
    return pluralityTabulation.containsKey(contest) ? Optional.of(pluralityTabulation.get(contest)) :
      Optional.empty();
  }

  /**
   * Returns a mapping between candidate name and vote tally for a given county-contest combination,
   * if we have a record of those tallies in the instance. Otherwise, returns an empty optional.
   */
  public Optional<Map<String,Integer>> getPluralityCountyTabulation(final String county, final String contest) {
    if(pluralityCountyTabulation.containsKey(county) &&
        pluralityCountyTabulation.get(county).containsKey(contest)){
      return Optional.of(pluralityCountyTabulation.get(county).get(contest));
    }
    return Optional.empty();
  }
}
