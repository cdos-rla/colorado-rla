-- List of invalid ranked (IRV) ballots, and their valid interpretation as an order list of choices.
-- correlated with cvr_id in contest_comparison export, and with audited_cvr_count.

SELECT
    cou.name AS County,
    con.name AS Contest,
    irv.record_type,
    irv.cvr_number,
    irv.imprinted_id,
    -- prettier printing by removing the enclosing [].
    SUBSTRING(irv.raw_choices, 2, LENGTH(irv.raw_choices) - 2) AS raw_vote,
    SUBSTRING(irv.interpretation, 2, LENGTH(irv.interpretation) - 2) AS valid_interpretation
FROM
   irv_ballot_interpretation AS irv
LEFT JOIN
   contest AS con
   ON irv.contest_id = con.id
LEFT JOIN
    county as cou
    ON con.county_id = cou.id;
