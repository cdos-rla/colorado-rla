/** Copyright (C) 2018 the Colorado Department of State  **/
package us.freeandfair.corla.controller;


import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import us.freeandfair.corla.math.Audit;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.BallotManifestInfoQueries;
import us.freeandfair.corla.query.CastVoteRecordQueries;
import us.freeandfair.corla.query.ContestResultQueries;

public final class ContestCounter {
  /**
   * Class-wide logger
   */
  public static final Logger LOGGER = LogManager.getLogger(ContestCounter.class);

  /** private to prevent construction **/
  private ContestCounter() {
  }

  /**
   * Group all CountyContestResults by contest name and tally the votes
   * across all counties that have reported results.
   * If 'useManifests' is true, it calculates the total universe size from the uploaded manifests -
   * this is important for the validity of the audit step. useManifests can be false for sample-size
   * estimation, where we expect that counties may not have uploaded valid manifests - in this case,
   * universe size is calculated by counting the CVRs.
   * The actual tallying is valid only for plurality - it is not valid, and not needed, for IRV.
   * However, this function may still be useful for IRV, e.g. for gathering contests together by
   * name and calculating their universes.
   * @return List<ContestResult> A high level view of contests and their participants.
   */
  public static List<ContestResult> countAllContests(boolean useManifests) {
    return
      Persistence.getAll(CountyContestResult.class)
      .stream()
      .collect(Collectors.groupingBy(x -> x.contest().name()))
      .entrySet()
      .stream()
      .map((Entry<String, List<CountyContestResult>> countyContestResults) -> countContest(countyContestResults, useManifests))
      .collect(Collectors.toList());
  }

  /**
   * Calculates all the pairwise margins - like a cross product - using
   * the vote totals. When there are no losers, all margins are zero.
   * Not valid for IRV.
   *
   * @param winners those who won the contest
   * @param losers those who did not win the contest
   * @param voteTotals a map of choice name to number of votes received
   * @return a Set of Integers representing all margins between winners
   * and losers
   */
  public static Set<Integer> pairwiseMargins(final Set<String> winners,
                                             final Set<String> losers,
                                             final Map<String, Integer> voteTotals) {
    final Set<Integer> margins = new HashSet<>();

    if (losers.isEmpty()) {
      margins.add(0);
    } else {
      for (final String w : winners) {
        for (final String l : losers) {
          margins.add(voteTotals.get(w) - voteTotals.get(l));
        }
      }
    }

    return margins;
  }

  /**
   * Set voteTotals on CONTEST based on all counties that have that
   * Contest name in their uploaded CVRs
   * Not valid for IRV.
   * @param countyContestResults the county-by-county contest results, which are useful for plurality.
   * @param useManifests         whether to use manifests to compute the total number of ballots. This
   *                             *must* be true when counting for audits - it can be false only when
   *                             doing pre-audit sample size estimation. In this case, it computes
   *                             the total number of ballots based on the (untrusted) CVRs.
   **/
  public static ContestResult countContest(final Map.Entry<String, List<CountyContestResult>> countyContestResults,
                                                                           boolean useManifests) {
    final String contestName = countyContestResults.getKey();
    final ContestResult contestResult = ContestResultQueries.findOrCreate(contestName);

    final Map<String,Integer> voteTotals =
      accumulateVoteTotals(countyContestResults.getValue().stream()
                           .map((cr) -> cr.voteTotals())
                           .collect(Collectors.toList()));
    contestResult.setVoteTotals(voteTotals);

    int numWinners;
    final Set<Integer> winnersAllowed = countyContestResults.getValue().stream()
      .map(x -> x.winnersAllowed())
      .collect(Collectors.toSet());

    if (winnersAllowed.isEmpty()) {
      LOGGER.error(String.format("[countContest: %s doesn't have any winners allowed."
                                 + " Assuming 1 allowed! Check the CVRS!", contestName));
      numWinners = 1;
    } else {
      if (winnersAllowed.size() > 1) {
        LOGGER.error(String.format("[countContest: County results for %s contain different"
                                   + " numbers of winners allowed: %s. Check the CVRS!",
                                   contestName, winnersAllowed));
      }
      numWinners = Collections.max(winnersAllowed);
    }

    contestResult.setWinnersAllowed(numWinners);
    contestResult.setWinners(winners(voteTotals, numWinners));
    contestResult.setLosers(losers(voteTotals, contestResult.getWinners()));

    contestResult.addContests(countyContestResults.getValue().stream()
                              .map(cr -> cr.contest())
                              .collect(Collectors.toSet()));
    contestResult.addCounties(countyContestResults.getValue().stream()
                              .map(cr -> cr.county())
                              .collect(Collectors.toSet()));

    // If we are supposed to use manifests, set the ballotCount to their indicated total, otherwise
    // count the CVRs.
    final Long ballotCount = useManifests ?
      BallotManifestInfoQueries.totalBallots(contestResult.countyIDs()) : countCVRs(contestResult);
    LOGGER.debug(String.format("%s Contest %s counted %s manifests.", "[countContest]", contestName,
        useManifests ? "with" : "without"));

    final Set<Integer> margins = pairwiseMargins(contestResult.getWinners(),
                                                  contestResult.getLosers(),
                                                  voteTotals);
    final Integer minMargin = Collections.min(margins);
    final Integer maxMargin = Collections.max(margins);
    final BigDecimal dilutedMargin = Audit.dilutedMargin(minMargin, ballotCount);
    // dilutedMargin of zero is ok here, it means the contest is uncontested
    // and the contest will not be auditable, so samples should not be selected for it
    contestResult.setBallotCount(ballotCount);
    contestResult.setMinMargin(minMargin);
    contestResult.setMaxMargin(maxMargin);
    contestResult.setDilutedMargin(dilutedMargin);

    if (ballotCount == 0L) {
      final String dataSource = useManifests ? "ballot manifests" : "uploaded CVRs";
      LOGGER.error(String.format("[countContest: %s has no %s for"
                                 + " countyIDs: %s", contestName, dataSource, contestResult.countyIDs()));
    }

    return contestResult;
  }

  /**
   * Calculate the size of the audit universe for a given contest by counting CVRs. This is the
   * total, over all counties that have any votes in the contest, of the total number of CVRs in the
   * county. Used for preliminary sample-size estimation before the audit.
   * For example, if a county had 10,000 CVRs, of which only 500 contained the contest, it would
   * contribute 10,000 to the total.
   * Note this should *not* be used during auditing, only for preliminary sample-size estimation in
   * advance of the audit. During auditing, the sample-size estimate calculation should get this
   * value from the manifests, not the CVRs.
   * @param contestResult the contestResult for this contest.
   * @return the sum, over all counties that contain the contest, of the total number of CVRs in
   *         that county. This will be 0 if either the contestResult has no counties, or the counties
   *         have uploaded no CVRs.
   */
  private static Long countCVRs(ContestResult contestResult) {
    final String prefix = "[countCVRs]";

    long total = 0L;
    for(County county : contestResult.getCounties()) {
      final OptionalLong countyCount
          = CastVoteRecordQueries.countMatching(county.id(), CastVoteRecord.RecordType.UPLOADED);
      if(countyCount.isPresent() && countyCount.getAsLong() != 0L)  {
        // Add all the ballots in this county to the total.
        total += countyCount.getAsLong();
      } else {
        // If there are no CVRs, we can still make an estimate based on the other counties' data,
        // but we need to warn that it may be inaccurate.
        LOGGER.warn(String.format("%s Found no CVRs in database for county %s. Estimate for contest "
            + "%s may be inaccurate.", prefix, county.name(), contestResult.getContestName()));
      }
    }
    return total;
  }

  /** add em up **/
  public static Map<String,Integer>
    accumulateVoteTotals(final List<Map<String,Integer>> voteTotals) {
    final Map<String,Integer> acc = new HashMap<String,Integer>();
    return voteTotals.stream().reduce(acc,
                                      (a, vt) -> addVoteTotal(a, vt));
  }

  /** add one vote total to another **/
  public static Map<String,Integer> addVoteTotal(final Map<String,Integer> acc,
                                                 final Map<String,Integer> vt) {
    // we iterate over vt because it may have a key that the accumulator has not
    // seen yet
    vt.forEach((k,v) -> acc.merge(k, v,
                                  (v1,v2) -> { return (null == v1) ? v2 : v1 + v2; }));
    return acc;
  }

  /**
   * Ranks a list of the choices in descending order by number of votes
   * received. Not relevant to IRV; not related to ranked-choice voting.
   **/
  public static List<Entry<String, Integer>> rankTotals(final Map<String,Integer> voteTotals) {
    return voteTotals.entrySet().stream()
      .sorted(Collections.reverseOrder(Entry.comparingByValue()))
      .collect(Collectors.toList());
  }

  /**
   * Find the set of winners for the ranking of voteTotals. Assumes only
   * one winner allowed. Not valid for IRV.
   *
   * @param voteTotals a map of choice name to number of votes
   */
  public static Set<String> winners(final Map<String,Integer> voteTotals) {
    return winners(voteTotals, 1);
  }

  /**
   * Find the set of winners for the ranking of voteTotals. Not valid for IRV.
   *
   * @param voteTotals a map of choice name to number of votes
   * @param winnersAllowed how many can win this contest?
   */
  public static Set<String> winners(final Map<String,Integer> voteTotals,
                                    final Integer winnersAllowed) {
    return rankTotals(voteTotals).stream()
      .limit(winnersAllowed)
      .map(Entry::getKey)
      .collect(Collectors.toSet());
  }

  /**
   * Find the set of losers given a ranking of voteTotals and some set
   * of contest winners. Not valid for IRV.
   *
   * @param voteTotals a map of choice name to number of votes
   * @param winners the choices that aren't losers
   */
  public static Set<String> losers(final Map<String,Integer> voteTotals,
                                   final Set<String> winners) {
    final Set<String> l = new HashSet<String>();
    l.addAll((Set<String>)voteTotals.keySet());
    l.removeAll(winners);
    return l;
  }
}
