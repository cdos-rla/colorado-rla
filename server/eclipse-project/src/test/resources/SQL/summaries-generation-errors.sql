-- Assertion Generation summaries for testing retrieval.
INSERT INTO county (id, name) VALUES (1001,'Test County');

-- Neither a summary nor assertions
INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed)
    VALUES (1001,1001,0,'IRV','No summary and no assertions Contest',1,5,1);

-- A success summary with no assertions
-- This is not supposed to be a reachable state. We just need to check that we fail gracefully.
INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed)
    VALUES (1001,1002,0,'IRV','Summary but no assertions Contest',1,5,1);
INSERT INTO generate_assertions_summary (contest_name, error, message, version, warning, winner)
    VALUES ('Summary but no assertions Contest', '','',0,'', 'Bob');

-- Assertions with no summary
-- This is not supposed to be a reachable state. We just need to check that we fail gracefully.
INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed)
    VALUES (1001,1003,0,'IRV','No summary but some assertions Contest',1,5,1);
INSERT INTO assertion (assertion_type, contest_name, difficulty, diluted_margin, loser, margin, current_risk, estimated_samples_to_audit, one_vote_over_count, one_vote_under_count, optimistic_samples_to_audit, other_count, two_vote_over_count, two_vote_under_count, version, winner)
    VALUES ('NEN', 'No summary but some assertions Contest', 3.01, 0.12, 'Charlie', 240, 1, 0, 0, 0, 0, 0, 0, 0, 0, 'Alice');

-- A summary of successful assertion generation, with a TIMEOUT_TRIMMING_ASSERTIONS warning.
INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed)
    VALUES (1001,1004,0,'IRV','Timeout trimming assertions Contest',1,5,1);
INSERT INTO generate_assertions_summary (contest_name, error, message, version, warning, winner)
    VALUES ('Timeout trimming assertions Contest', '','',0,'TIMEOUT_TRIMMING_ASSERTIONS','Bob');

-- A summary of unsuccessful assertion generation, with a TIED_WINNERS error.
INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed)
    VALUES (1001,1005,0,'IRV','Tied winners Contest',1,5,1);
INSERT INTO generate_assertions_summary (contest_name, error, message, version, warning, winner)
    VALUES ('Tied winners Contest', 'TIED_WINNERS','Tied winners: Alice, Bob',0,'','');

-- A summary of unsuccessful assertion generation, with a TIMEOUT_FINDING_ASSERTIONS error.
INSERT INTO contest (county_id, id, version, description, name, sequence_number, votes_allowed, winners_allowed)
    VALUES (1001,1006,0,'IRV','Time out finding Contest',1,5,1);
INSERT INTO generate_assertions_summary (contest_name, error, message, version, warning, winner)
    VALUES ('Time out finding Contest', 'TIME_OUT_FINDING_ASSERTIONS','Time out finding assertions',0,'','');
