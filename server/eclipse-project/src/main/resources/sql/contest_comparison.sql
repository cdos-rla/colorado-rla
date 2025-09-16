-- For each contest, and for each cast vote record in the random sequence
-- (with duplicates) for which the Audit Board has submitted information, and which
-- covers the contest in question,
-- original cvr info, audit board interp info.
-- note that the random sequence index (includes dupes) is contest_audit_info.index
-- cvr_contest_info.index is the index of the *contest* on the ballot
-- Note that in case of an overvote, `cci_a.choices` shows all the choices the Audit Board thought the voter intended, while cci.choices will *not* show all those choices.

-- TODO: ensure that "ballot not found" (cast_vote_record.record_type = 'PHANTOM_BALLOT') is properly handled


SELECT DISTINCT
   cty.name AS county_name,
   cn.name AS contest_name,
   cvr_s.imprinted_id,
   cvr_s.ballot_type,
   SUBSTRING(cci.choices, 2, LENGTH(cci.choices) - 2) AS choice_per_voting_computer,
   SUBSTRING(cci_a.choices, 2, LENGTH(cci_a.choices) - 2) AS audit_board_selection,
   cci_a.consensus,
   LOWER(cvr_s.record_type) as record_type,
   cci_a.comment AS audit_board_comment,
   cvr_a.timestamp,
   cai.cvr_id,
   cpa.audit_reason

FROM
   cvr_audit_info AS cai
 LEFT JOIN
   cvr_contest_info AS cci
   ON cci.cvr_id = cai.cvr_id
 LEFT JOIN cast_vote_record AS cvr_s
   ON cai.cvr_id = cvr_s.id
 LEFT JOIN cvr_contest_info AS cci_a
   ON cai.acvr_id = cci_a.cvr_id
     AND cci_a.contest_id = cci.contest_id
 LEFT JOIN
   cast_vote_record AS cvr_a
   ON cai.acvr_id = cvr_a.id
 LEFT JOIN
   contest AS cn
   ON cci.contest_id = cn.id
 LEFT JOIN county AS cty
   ON cn.county_id = cty.id
 LEFT JOIN
   contest_to_audit AS cta
   ON (cci.contest_id = cta.contest_id or  cn.name = (select cn1.name from contest cn1 where cn1.id=cta.contest_id))
 LEFT JOIN
   comparison_audit AS cpa
   ON cpa.audit_reason = cta.reason and cast (cci.cvr_id as TEXT) = ANY (string_to_array(substring(cpa.contest_cvr_ids from 2 for (char_length(cpa.contest_cvr_ids)-2)), ','))

ORDER BY county_name, contest_name
;
