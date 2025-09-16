-- List of contests with basic info, current status and
-- data needed for risk calculations.

SELECT 
   cr.contest_name AS contest_name,
   LOWER(ca.audit_reason) AS audit_reason,
   --LOWER(cta.audit) AS current_audit_type,
   LOWER(ca.audit_status) AS random_audit_status,
   -- cn.votes_allowed,
   cr.winners_allowed,
   cr.ballot_count AS ballot_card_count,
   agg.contest_ballot_card_count,
   -- Note these will only be correct for IRV after a run of ExportQueries::updateIRVContestResults.
   SUBSTRING(cr.winners, 2, LENGTH(cr.winners) - 2) AS winners,
   cr.min_margin,
   ca.risk_limit,
   ca.audited_sample_count,
   ca.two_vote_over_count,
   ca.one_vote_over_count,
   ca.other_count as zero_discrepancy_count,
   ca.one_vote_under_count,
   ca.two_vote_under_count,
   ca.disagreement_count,
   ca.gamma,
   ROUND(ca.one_vote_over_count+ca.two_vote_over_count, 0) AS overstatements,
   ca.optimistic_samples_to_audit,
   ca.estimated_samples_to_audit
FROM 
   comparison_audit AS ca
--LEFT JOIN
--   contest_to_audit AS cta
--   on ca.contest_id = cta.contest_id    -- now pick up via contests_to_contest_results
-- LEFT JOIN 
--   contest AS cn ON cn.id = ca.contest_id -- now pick up via contests_to_contest_results
LEFT JOIN
   contest_result AS cr
   ON ca.contest_result_id = cr.id
LEFT JOIN
  (SELECT ctcr.contest_result_id,
          SUM(contest_ballot_count) AS contest_ballot_card_count
      FROM county_contest_result AS ccr
      LEFT JOIN contests_to_contest_results AS ctcr
      ON ccr.contest_id = ctcr.contest_id
      GROUP BY ctcr.contest_result_id
   ) AS agg
  ON agg.contest_result_id = cr.id
ORDER BY contest_name
;
