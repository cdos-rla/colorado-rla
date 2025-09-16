--
--
-- Database state to reflect Adams County being partway through an audit of the TinyExample1 contest.
-- This assumes that corla.sql and co-counties.sql and corla-three-candidates-ten-votes-inconsistent-types.sql
-- (or equivalent) are already loaded.

--
-- Data for Name: asm_state; Type: TABLE DATA; Schema: public; Owner: corlaadmin
--

UPDATE public.county_dashboard
SET driving_contests =  '["TinyExample1"]', audited_prefix_length = 0, audited_sample_count = 0, ballots_audited = 10, ballots_in_manifest = 10
WHERE id = 1;

INSERT INTO public.cvr_audit_info (id, count_by_contest, multiplicity_by_contest, disagreement, discrepancy, version, acvr_id, cvr_id) VALUES (240509, '{}', '{}', '[]', '[]', 0, null, 240509);
INSERT INTO public.cvr_audit_info (id, count_by_contest, multiplicity_by_contest, disagreement, discrepancy, version, acvr_id, cvr_id) VALUES (240510, '{}', '{}', '[]', '[]', 0, null, 240510);
INSERT INTO public.cvr_audit_info (id, count_by_contest, multiplicity_by_contest, disagreement, discrepancy, version, acvr_id, cvr_id) VALUES (240511, '{}', '{}', '[]', '[]', 0, null, 240511);
INSERT INTO public.cvr_audit_info (id, count_by_contest, multiplicity_by_contest, disagreement, discrepancy, version, acvr_id, cvr_id) VALUES (240512, '{}', '{}', '[]', '[]', 0, null, 240512);
INSERT INTO public.cvr_audit_info (id, count_by_contest, multiplicity_by_contest, disagreement, discrepancy, version, acvr_id, cvr_id) VALUES (240513, '{}', '{}', '[]', '[]', 0, null, 240513);

INSERT INTO public.asm_state (id, asm_class, asm_identity, state_class, state_value, version) VALUES (1267523, 'us.freeandfair.corla.asm.CountyDashboardASM', '1', 'us.freeandfair.corla.asm.ASMState$CountyDashboardState', 'COUNTY_AUDIT_UNDERWAY', 4);

-- Assertions for the contest. Note that initially there are zero discrepancies of any kind.
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEB', 1, 'TinyExample1', 1.00, 1.25, 0.8, 87, 'Bob', 8, 0, 0, 50, 0, 0, 0, 77, 'Alice');
INSERT INTO public.assertion (assertion_type, id, contest_name, current_risk, difficulty, diluted_margin, estimated_samples_to_audit, loser, margin, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) VALUES ('NEB', 2, 'TinyExample1', 1.00, 1.25, 0.8, 109, 'Chuan', 8, 0, 0, 63, 0, 0, 0, 77, 'Alice');