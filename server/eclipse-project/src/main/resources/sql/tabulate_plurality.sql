-- Vote Counts by Contest, Choice
-- Updated to include on PLURALITY contests
-- Uses SELECT DISTINCT because a contest_result can have multiple contests (from different counties),
-- of which we only want one.
SELECT DISTINCT
  contest_result.contest_name AS contest_name,
  contest_vote_total.choice AS choice,
  contest_vote_total.vote_total AS votes
FROM
  contest_vote_total,
  contest_result,
  contests_to_contest_results,
  contest
WHERE
  contest_vote_total.result_id = contest_result.id AND
  contest_result.id = contests_to_contest_results.contest_result_id AND
  contests_to_contest_results.contest_id = contest.id AND
  contest.description = 'PLURALITY'
ORDER BY
  contest_result.contest_name ASC,
  contest_vote_total.vote_total DESC;
