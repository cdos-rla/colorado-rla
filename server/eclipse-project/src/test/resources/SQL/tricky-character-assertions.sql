INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEB', 169, 'TinyExample1', 1.00, 2, 0.5, 15, 'Bob with \backslashes and \" " quotes"', 5, 0, 0, 15, 0, 0, 0, 1, 'A£ϒç©é');
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEN', 170, 'TinyExample1', 1.00, 2.5, 0.4, 19, 'Candidate, with, commas', 4, 0, 0, 19, 0, 0, 0, 1, 'A£ϒç©é');

INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (170, 'A£ϒç©é');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (170, 'Candidate, with, commas');

INSERT INTO public.generate_assertions_summary (id, contest_name, error, message, version, warning, winner) VALUES (36, 'TinyExample1', '', '', 0, '', 'A£ϒç©é');
