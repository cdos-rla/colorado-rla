INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEN', 35, 'Uralla Mayoral', 1.00, 1.5727953410981697, 0.635810632107908, 0, 'LEDGER Natasha', 2404, 0, 0, 0, 0, 0, 0, 0, 'BELL Robert');
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEN', 36, 'Uralla Mayoral', 1.00, 1.6297413793103448, 0.6135942872256017, 0, 'STRUTT Isabel', 2320, 0, 0, 0, 0, 0, 0, 0, 'BELL Robert');
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEN', 37, 'Uralla Mayoral', 1.00, 1.6034775233248515, 0.6236445384818831, 0, 'LEDGER Natasha', 2358, 0, 0, 0, 0, 0, 0, 0, 'BELL Robert');

INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (35, 'BELL Robert');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (35, 'LEDGER Natasha');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (36, 'BELL Robert');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (36, 'STRUTT Isabel');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (37, 'BELL Robert');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (37, 'LEDGER Natasha');
INSERT INTO public.assertion_assumed_continuing (id, assumed_continuing) VALUES (37, 'STRUTT Isabel');

INSERT INTO public.generate_assertions_summary (id, contest_name, error, message, version, warning, winner) VALUES (2, 'Uralla Mayoral', '', '', 0, '', 'BELL Robert');
