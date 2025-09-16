-- Simple Assertions for testing Retrieval/Deletion
INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (1,1,0,'IRV','One NEB Assertion Contest',1,5,1);
INSERT INTO generate_assertions_summary (contest_name, error, message, version, warning, winner) VALUES ('One NEB Assertion Contest', '','',0,'', 'Alice');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'One NEB Assertion Contest', 3.125, 0.32, 'Bob', 320, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'Alice');

INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (2,2,0,'IRV','One NEN Assertion Contest',2,4,1);
INSERT INTO generate_assertions_summary (contest_name, error, message, version, warning, winner) VALUES ('One NEN Assertion Contest', '','',0,'', 'Alice');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'One NEN Assertion Contest', 8.33, 0.12, 'Charlie', 240, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'Alice');
INSERT INTO assertion_assumed_continuing values (2, 'Alice');
INSERT INTO assertion_assumed_continuing values (2, 'Charlie');
INSERT INTO assertion_assumed_continuing values (2, 'Diego');
INSERT INTO assertion_assumed_continuing values (2, 'Bob');

INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (3,3,0,'IRV','One NEN NEB Assertion Contest',3,4,1);
INSERT INTO generate_assertions_summary (contest_name, error, message, version, warning, winner) VALUES ('One NEN NEB Assertion Contest', '','',0,'', 'Amanda');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'One NEN NEB Assertion Contest', 10, 0.1, 'Liesl', 112, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'Amanda');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'One NEN NEB Assertion Contest', 2, 0.5, 'Wendell', 560, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'Amanda');
INSERT INTO assertion_assumed_continuing values (4, 'Liesl');
INSERT INTO assertion_assumed_continuing values (4, 'Wendell');
INSERT INTO assertion_assumed_continuing values (4, 'Amanda');


INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (2,4,0,'IRV','Multi-County Contest 1',4,4,1);
INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (3,5,0,'IRV','Multi-County Contest 1',5,4,1);
INSERT INTO generate_assertions_summary (contest_name, error, message, version, warning, winner) VALUES ('Multi-County Contest 1', '','',0,'', 'Charlie C. Chaplin');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Multi-County Contest 1', 100, 0.01, 'Alice P. Mangrove', 310, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'Charlie C. Chaplin');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Multi-County Contest 1', 14.3, 0.07, 'Al (Bob) Jones', 2170, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'Alice P. Mangrove');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Multi-County Contest 1', 1000, 0.001, 'West W. Westerson', 31, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'Alice P. Mangrove');
INSERT INTO assertion_assumed_continuing values (7, 'West W. Westerson');
INSERT INTO assertion_assumed_continuing values (7, 'Alice P. Mangrove');

INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,6,0,'IRV','Test Estimation NEB Only',6,4,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Test Estimation NEB Only', 100, 0.01, 'B', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Test Estimation NEB Only', 159, 0.0159, 'C', 159, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'B');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Test Estimation NEB Only', 50, 0.12345, 'G', 1235, 1, 0, 0, 0, 0, 0, 0, 1, 0, 'F');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (10, -2, 1);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Test Estimation NEB Only', 17, 0.33247528, 'I', 3325, 1, 0, 1, 0, 0, 0, 1, 0, 0, 'H');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (11, 1, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (11, 2, 2);

INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,7,0,'IRV','Test Estimation NEN Only',7,4,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Test Estimation NEN Only', 100, 0.01, 'B', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion_assumed_continuing values (12, 'A');
INSERT INTO assertion_assumed_continuing values (12, 'B');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Test Estimation NEN Only', 159, 0.0159, 'C', 159, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'B');
INSERT INTO assertion_assumed_continuing values (13, 'B');
INSERT INTO assertion_assumed_continuing values (13, 'C');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Test Estimation NEN Only', 50, 0.12345, 'G', 1235, 1, 0, 0, 0, 0, 0, 0, 1, 0, 'F');
INSERT INTO assertion_assumed_continuing values (14, 'F');
INSERT INTO assertion_assumed_continuing values (14, 'G');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (14, -2, 1);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Test Estimation NEN Only', 17, 0.33247528, 'I', 3325, 1, 0, 1, 0, 0, 0, 1, 0, 0, 'H');
INSERT INTO assertion_assumed_continuing values (15, 'H');
INSERT INTO assertion_assumed_continuing values (15, 'I');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (15, 1, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (15, 2, 2);

INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,8,0,'IRV','Test Estimation Mixed Assertions',8,4,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Test Estimation Mixed Assertions', 100, 0.01, 'B', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Test Estimation Mixed Assertions', 159, 0.0159, 'C', 159, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'B');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Test Estimation Mixed Assertions', 50, 0.12345, 'G', 1235, 1, 0, 0, 0, 0, 0, 0, 1, 0, 'F');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (18, -2, 1);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Test Estimation Mixed Assertions', 17, 0.33247528, 'I', 3325, 1, 0, 1, 0, 0, 0, 1, 0, 0, 'H');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (19, 1, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (19, 2, 2);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Test Estimation Mixed Assertions', 100, 0.01, 'B', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion_assumed_continuing values (20, 'A');
INSERT INTO assertion_assumed_continuing values (20, 'B');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Test Estimation Mixed Assertions', 159, 0.0159, 'C', 159, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'B');
INSERT INTO assertion_assumed_continuing values (21, 'B');
INSERT INTO assertion_assumed_continuing values (21, 'C');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Test Estimation Mixed Assertions', 50, 0.12345, 'G', 1235, 1, 0, 0, 0, 0, 0, 0, 1, 0, 'F');
INSERT INTO assertion_assumed_continuing values (22, 'F');
INSERT INTO assertion_assumed_continuing values (22, 'G');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (22, -2, 1);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Test Estimation Mixed Assertions', 17, 0.33247528, 'I', 3325, 1, 0, 1, 0, 0, 0, 1, 0, 0, 'H');
INSERT INTO assertion_assumed_continuing values (23, 'H');
INSERT INTO assertion_assumed_continuing values (23, 'I');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (23, 1, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (23, 2, 2);


INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,9,0,'IRV','Mixed Contest',9,4,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Mixed Contest', 100, 0.01, 'B', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Mixed Contest', 100, 0.01, 'C', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion_assumed_continuing values (25, 'A');
INSERT INTO assertion_assumed_continuing values (25, 'B');
INSERT INTO assertion_assumed_continuing values (25, 'C');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Mixed Contest', 159, 0.0159, 'C', 159, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'B');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Mixed Contest', 159, 0.0159, 'F', 159, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion_assumed_continuing values (27, 'A');
INSERT INTO assertion_assumed_continuing values (27, 'F');
INSERT INTO assertion_assumed_continuing values (27, 'B');
INSERT INTO assertion_assumed_continuing values (27, 'C');
INSERT INTO assertion_assumed_continuing values (27, 'D');

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Mixed Contest', 50, 0.12345, 'G', 1235, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'F');


INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,10,0,'IRV','Mixed Contest 2',10,4,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Mixed Contest 2', 100, 0.05, 'B', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Mixed Contest 2', 100, 0.1, 'C', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion_assumed_continuing values (30, 'A');
INSERT INTO assertion_assumed_continuing values (30, 'B');
INSERT INTO assertion_assumed_continuing values (30, 'C');

INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,11,0,'IRV','Simple Contest 3',11,4,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Simple Contest 3', 100, 0.02, 'B', 100, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'A');


INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,12,0,'IRV','Simple Contest 4',12,4,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Simple Contest 4', 200, 0.07, 'B', 200, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'F');
INSERT INTO assertion_assumed_continuing values (32, 'A');
INSERT INTO assertion_assumed_continuing values (32, 'B');
INSERT INTO assertion_assumed_continuing values (32, 'F');

INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,13,0,'IRV','Mixed Assertions With Discrepancies',13,4,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Mixed Assertions With Discrepancies', 159, 0.0159, 'C', 159, 1, 0, 0, 0, 0, 0, 0, 2, 0, 'B');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (33, -2, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (33, -2, 2);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Mixed Assertions With Discrepancies', 50, 0.12345, 'G', 1235, 1, 0, 0, 2, 0, 1, 0, 0, 0, 'F');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (34, 0, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (34, -1, 2);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (34, -1, 3);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEB', 'Mixed Assertions With Discrepancies', 17, 0.33247528, 'I', 3325, 1, 0, 0, 0, 0, 0, 2, 0, 0, 'H');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (35, 2, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (35, 2, 2);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Mixed Assertions With Discrepancies', 100, 0.01, 'B', 100, 1, 0, 2, 0, 0, 0, 0, 0, 0, 'A');
INSERT INTO assertion_assumed_continuing values (36, 'A');
INSERT INTO assertion_assumed_continuing values (36, 'B');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (36, 1, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (36, 1, 2);

INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner) values ('NEN', 'Mixed Assertions With Discrepancies', 50, 0.12345, 'G', 1235, 1, 0, 0, 1, 0, 2, 0, 0, 0, 'F');
INSERT INTO assertion_assumed_continuing values (37, 'F');
INSERT INTO assertion_assumed_continuing values (37, 'G');
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (37, -1, 1);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (37, 0, 2);
INSERT INTO assertion_discrepancies (id, discrepancy, cvr_id) values (37, 0, 3);

INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed) VALUES (4,14,0,'IRV','No Assertions',14,4,1);
