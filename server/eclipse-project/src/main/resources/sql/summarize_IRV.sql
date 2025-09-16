-- Show the IRV summaries, sorted by contest name.
-- This is usually a (unique) winner and an empty error, but may also be a
-- blank winner and a non-empty error with an explanatory message.
-- This also adds a column to indicate whether the contest was targeted for audit. This requires
-- a join with the contest_to_audit table, which is slightly complicated by the fact that that table
-- stores _all of_ the contest ids for multi-county contests, so this simply selects the first audit
-- reason, in the very unlikely event that one contest is selected for both COUNTY_WIDE and
-- STATE_WIDE reasons.

SELECT gas.contest_name, COALESCE(targets.targeted_reason, 'Not targeted') as target_reason, gas.winner, gas.error, gas.message
FROM generate_assertions_summary as gas
    -- This sub-query attaches audit target reasons (aggregated), if any, to contest names, and
    -- 'Not targeted' otherwise.
    -- Inner join because we only want the ones in the generate_assertions_summary table.
    -- Select distinct because we only want one row, even for multi-county contests.
LEFT JOIN (
    SELECT DISTINCT contest.name as name, contest_to_audit.reason as targeted_reason
    FROM contest_to_audit
    -- Again an inner join, because we only want the ones in the table of targeted contests.
    -- This will give us the one row of the contest table that happens to have been identified in the
    -- contest_to_audit table
    INNER JOIN contest
    ON contest_to_audit.contest_id = contest.id
) AS targets
ON gas.contest_name = targets.name;




