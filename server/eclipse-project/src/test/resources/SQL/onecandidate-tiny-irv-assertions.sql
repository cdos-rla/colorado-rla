
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEB', 3, 'Example3', 1.00, 5, 0.2, 0, 'Dinh', 45, 0, 0, 0, 0, 0, 0, 0, 'Cherry');
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEN', 4, 'Example3', 1.00, 9, 0.1111111111111111, 0, 'Aaron', 25, 0, 0, 0, 0, 0, 0, 0, 'Cherry');
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEN', 5, 'Example3', 1.00, 3.75, 0.26666666666666666, 0, 'Bernice', 60, 0, 0, 0, 0, 0, 0, 0, 'Aaron');
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEN', 6, 'Example3', 1.00, 5, 0.2, 0, 'Bernice', 45, 0, 0, 0, 0, 0, 0, 0, 'Cherry');
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEN', 7, 'Example3', 1.00, 2.25, 0.4444444444444444, 0, 'Dinh', 100, 0, 0, 0, 0, 0, 0, 0, 'Aaron');

INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (4, 'Aaron');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (4, 'Cherry');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (5, 'Aaron');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (5, 'Bernice');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (5, 'Cherry');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (6, 'Aaron');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (6, 'Bernice');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (6, 'Cherry');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (7, 'Aaron');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (7, 'Bernice');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (7, 'Cherry');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (7, 'Dinh');

INSERT INTO public.generate_assertions_summary (id, contest_name, error, message, version, warning, winner) VALUES (1, 'TinyExample1', '', '', 0, '', 'Alice');
INSERT INTO public.generate_assertions_summary (id, contest_name, error, message, version, warning, winner) VALUES (2, 'Tied_IRV', 'TIED_WINNERS', 'Tied winners: Alice, Chuan.', 0, '', '');
INSERT INTO public.generate_assertions_summary (id, contest_name, error, message, version, warning, winner) VALUES (3, 'Example3', '', '', 0, '', 'Cherry');
