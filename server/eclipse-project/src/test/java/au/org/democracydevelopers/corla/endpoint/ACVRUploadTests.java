package au.org.democracydevelopers.corla.endpoint;

import au.org.democracydevelopers.corla.model.vote.IRVBallotInterpretation;
import au.org.democracydevelopers.corla.util.*;
import org.mockito.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import spark.HaltException;
import us.freeandfair.corla.Main;
import us.freeandfair.corla.controller.ComparisonAuditController;
import us.freeandfair.corla.endpoint.ACVRUpload;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.CastVoteRecordQueries;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import static au.org.democracydevelopers.corla.util.testUtils.tinyIRV;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

import spark.Request;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalLong;

import static us.freeandfair.corla.endpoint.Endpoint.AuthorizationType.COUNTY;
import static us.freeandfair.corla.model.CastVoteRecord.RecordType.AUDITOR_ENTERED;
import static us.freeandfair.corla.model.CastVoteRecord.RecordType.REAUDITED;

/**
 * Test upload of IRV audit cvrs. Includes tests of both valid and invalid IRV CVRs, and tests that
 * the interpreted ballots are properly stored in the database.
 * This makes use of TestClassWithAuth to mock authentication as Adams County.
 * See <a href="https://github.com/orgs/DemocracyDevelopers/projects/1/views/1?pane=issue&itemId=72434202">...</a>
 */
public class ACVRUploadTests extends TestClassWithAuth {

  private final static Logger LOGGER = LogManager.getLogger(ACVRUploadTests.class);

  /**
   * An Audit CVR Upload endpoint to test.
   */
  private final ACVRUpload uploadEndpoint = new ACVRUpload();

  /**
   * The name of the county we will pretend to be logged in as administrator for.
   */
  private static final String countyName = "Adams";

  /**
   * The ID (according to co_counties.sql) of the county we will pretend to be logged in as
   * administrator for.
   */
  private static final long countyID = 1L;


  /**
   * Flag for checking whether the audit round has already been started.
   */
  private static boolean auditRoundAlreadyStarted = false;

  /**
   * 1. Valid IRV vote, for CVR ID 240509; imprinted ID 1-1-1.
   * This has to be constructed as a json string, rather than by constructing and serializing the
   * CVR object(s), because the constructors reject IRV choices with explicit parentheses.
   * CVR IDs are in corla-three-candidates-ten-votes-inconsistent-types.sql.
   */
  private static final String validIRVAsJson = "{" +
      "  \"cvr_id\": 240509," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 1," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 1," +
      "    \"imprinted_id\": \"1-1-1\"," +
      "    \"uri\": \"acvr:1:1-1-1\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "          \"Bob(1)\"," +
      "          \"Chuan(2)\"" +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": false," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * 2. Invalid IRV vote, for CVR ID 240510; imprinted ID 1-1-2.
   */
  private static final String invalidIRVAsJson = "{" +
      "  \"cvr_id\": 240510," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 2," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 2," +
      "    \"imprinted_id\": \"1-1-2\"," +
      "    \"uri\": \"acvr:1:1-1-2\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "          \"Chuan(1)\"," +
      "          \"Chuan(2)\"," +
      "          \"Bob(2)\"," +
      "          \"Alice(3)\"" +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": false," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * 3. Blank IRV vote, for CVR ID 240511; imprinted ID 1-1-3.
   */
  private static final String blankIRVAsJson = "{" +
      "  \"cvr_id\": 240511," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 3," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 3," +
      "    \"imprinted_id\": \"1-1-3\"," +
      "    \"uri\": \"acvr:1:1-1-3\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": false," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * 4. IRV vote with non-parenthesized names, for CVR ID 240512; imprinted ID 1-1-4. Should cause an error.
   */
  private static final String pluralityIRVAsJson = "{" +
      "  \"cvr_id\": 240512," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 4," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 4," +
      "    \"imprinted_id\": \"1-1-4\"," +
      "    \"uri\": \"acvr:1:1-1-4\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "          \"Chuan\"," +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": false," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * 5. IRV vote with candidate names who are not on the list of valid choices, for CVR ID 240513; imprinted ID 1-1-5.
   * Should cause an error.
   */
  private static final String wrongCandidateNamesIRVAsJson = "{" +
      "  \"cvr_id\": 240513," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 5," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 5," +
      "    \"imprinted_id\": \"1-1-5\"," +
      "    \"uri\": \"acvr:1:1-1-5\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "          \"Cherry(1)\"," +
      "          \"Alicia(2)\"," +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": false," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * 6. IRV vote with IDs that do not properly correspond to what they should, for CVR ID 240514; imprinted ID 1-1-6.
   * Should cause an error.  Deliberately inconsistent between 5s and 6s.
   */
  private static final String IRVWithInconsistentIDsAsJson = "{" +
      "  \"cvr_id\": 240514," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 5," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 5," +
      "    \"imprinted_id\": \"1-1-6\"," +
      "    \"uri\": \"acvr:1:1-1-6\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "          \"Chuan(1)\"," +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": false," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * 7. IRV vote with typos that produce a json deserialization failure, for CVR ID 240515; imprinted ID 1-1-7.
   * Should cause an error. (The fail is that the choices list doesn't have the right commas.)
   */
  private static final String IRVJsonDeserializationFail = "{" +
      "  \"cvr_id\": 240515," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 7," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 7," +
      "    \"imprinted_id\": \"1-1-7\"," +
      "    \"uri\": \"acvr:1:1-1-7\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "          \"Bob(1)\"," +
      "          \"Chuan(2)\"" +
      "          \"Alice(3)\"" +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": false," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * 8 & 9. Reaudit of valid IRV vote, for CVR ID 240509; imprinted ID 1-1-1.
   */
  private static final String validIRVReauditAsJson = "{" +
      "  \"cvr_id\": 240509," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 1," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 1," +
      "    \"imprinted_id\": \"1-1-1\"," +
      "    \"uri\": \"acvr:1:1-1-1\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "          \"Alice(1)\"," +
      "          \"Chuan(2)\"" +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": true," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * 10. Attempt to reaudit a vote that has not yet been audited - #7, the IRV vote with typos.
   * Should cause an error.
   */
  private static final String IRVReauditNoPriorUpload = "{" +
      "  \"cvr_id\": 240515," +
      "  \"audit_cvr\": {" +
      "    \"record_type\": \"AUDITOR_ENTERED\"," +
      "    \"county_id\": 1," +
      "    \"cvr_number\": 7," +
      "    \"sequence_number\": 1," +
      "    \"scanner_id\": 1," +
      "    \"batch_id\": \"1\"," +
      "    \"record_id\": 7," +
      "    \"imprinted_id\": \"1-1-7\"," +
      "    \"uri\": \"acvr:1:1-1-7\"," +
      "    \"ballot_type\": \"Ballot 1 - Type 1\"," +
      "    \"contest_info\": [" +
      "      {" +
      "        \"contest\": 240503," +
      "        \"comment\": \"A comment\"," +
      "        \"consensus\": \"YES\"," +
      "        \"choices\": [" +
      "        ]" +
      "      }" +
      "    ]" +
      "  }," +
      "  \"reaudit\": true," +
      "  \"comment\": \"\"," +
      "  \"auditBoardIndex\": -1" +
      "}";

  /**
   * Database init.
   */
  @BeforeClass
  public void beforeAllThisClass() {
    runSQLSetupScript("SQL/co-counties.sql");
    runSQLSetupScript("SQL/corla-three-candidates-ten-votes-inconsistent-types.sql");
    runSQLSetupScript("SQL/adams-partway-through-audit.sql");
  }

  /**
   * Init mocks, particularly for authentication.
   */
  @BeforeClass
  public void initMocks() {
    testUtils.log(LOGGER, "initMocks");

    // Mock successful auth as a county.
    MockitoAnnotations.openMocks(this);
    mockAuth(countyName, countyID, COUNTY);
  }

  /**
   * Basic test of proper functioning for uploaded audit CVRs, including:
   * 1. a valid IRV vote,
   * 2. an invalid IRV vote, which should be stored as its valid interpretation,
   * 3. a blank IRV vote,
   * 4. a vote with non-IRV choices ("Alice" instead of "Alice(1)"),
   * 5. a vote with invalid choices (names not in the list of choices for the contest),
   * 6. a vote that doesn't properly correspond to the IDs it should have,
   * 7. an unparseable vote (typos in json data),
   * 8. a reaudit (which checks that the superseded upload is marked REAUDITED), and
   * 9. a second reaudit with the same data (which checks that the previous reaudit is also now
   *    marked REAUDITED and that there are now two revisions).
   * We check that it is accepted (or rejected if it should be) and that the right records for CVR
   * and CVRContestInfo are stored in the database.
   */
  @Test
  @Transactional
  void testACVRUploadAndStorage() {
    testUtils.log(LOGGER, "testACVRUploadAndStorage");

    // Mock the main class; mock its auth as the mocked Adams county auth.
    try (MockedStatic<Main> mockedMain = Mockito.mockStatic(Main.class)) {
      mockedMain.when(Main::authentication).thenReturn(auth);

      startTheRound();

      // We seem to need a dummy request to run before.
      final Request request = new SparkRequestStub("", new HashMap<String,String>());
      uploadEndpoint.before(request, response);

      // Before the test, there should be 10 UPLOADED and zero AUDITOR_ENTERED cvrs.
      final List<CastVoteRecord> preCvrs
          = CastVoteRecordQueries.getMatching(1L, CastVoteRecord.RecordType.UPLOADED).toList();
      final List<CastVoteRecord> preACvrs
          = CastVoteRecordQueries.getMatching(1L, AUDITOR_ENTERED).toList();
      assertEquals(preCvrs.size(), 10);
      assertEquals(preACvrs.size(), 0);

      // First test: upload the first audit CVR (1-1-1; id 240509) (votes ["Bob(1)", "Chuan(2)"]) to
      // the endpoint. Check that the right database records result, with correct interpreted vote
      // ["Bob", "Chuan"].
      testSuccessResponse(240509L, "1-1-1", validIRVAsJson, List.of("Bob", "Chuan"), 1);

      // Second test: upload an invalid IRV vote [Chuan(1), Chuan(2), Bob(2), Alice(3)]. Check that
      // it is accepted and that its valid interpretation [Chuan, Bob, Alice] is properly stored.
      testSuccessResponse(240510L, "1-1-2", invalidIRVAsJson, List.of("Chuan","Bob","Alice"), 2);
      testIRVBallotInterpretations(2, "1-1-2", List.of("Chuan(1)","Chuan(2)","Bob(2)","Alice(3)"),
          List.of("Chuan","Bob","Alice"));

      // Third test: upload a blank vote. Check the results.
      testSuccessResponse(240511L, "1-1-3", blankIRVAsJson, List.of(), 3);

      // Fourth test: upload a vote with plurality-style choices (no parenthesized ranks). This
      // should cause an error.

      //  Expected error messages for malformed upload cvrs.
      String malformedACVRMsg = "malformed audit CVR upload";

      testErrorResponseAndNoMatchingCvr(240512L, pluralityIRVAsJson, malformedACVRMsg);

      // Fifth test: upload a vote with IRV choices that are not among the valid candidates. This
      // should cause an error.
      testErrorResponseAndNoMatchingCvr(240513L, wrongCandidateNamesIRVAsJson, malformedACVRMsg);

      // Sixth test: upload a vote with IDs that do not correspond properly to the expected CVR.
      // This should cause an error.
      testErrorResponseAndNoMatchingCvr(240514L, IRVWithInconsistentIDsAsJson, malformedACVRMsg);

      // Seventh test: upload a vote that has typos preventing json deserialization. This should
      // cause an error.
      testErrorResponseAndNoMatchingCvr(240515L, IRVJsonDeserializationFail, malformedACVRMsg);

      // Eighth test: upload a reaudited ballot.
      // Check that the new data successfully replaces the prior upload.
      testSuccessResponse(240509L, "1-1-1", validIRVReauditAsJson, List.of("Alice","Chuan"), 3);
      // Test that the previous upload, with a vote for Bob and Chuan (validIRVAsJson) is now tagged
      // as REAUDITED.
      testPreviousAreReaudited(240509L, "1-1-1", List.of("Bob","Chuan"), 1, 1);

      // Ninth test: upload the same ballot being reaudited, again.
      // Check that the new data is identical (it replaces it, but it's the same).
      testSuccessResponse(240509L, "1-1-1", validIRVReauditAsJson, List.of("Alice","Chuan"), 3);
      // Test that the previous upload (from test 8), with a vote for Alice and Chuan
      // (validIRVReauditAsJson) is now tagged as REAUDITED.
      // There should be two reaudited ballots now, and the max revision should be 2.
      testPreviousAreReaudited(240509L, "1-1-1", List.of("Alice","Chuan"), 2, 2);

      // Tenth test: try to "re-"audit something that has not previously been successfully audited. Should throw an error.
      testErrorResponseAndNoMatchingCvr(240515L, IRVReauditNoPriorUpload, "CVR has not previously been audited");
    }
  }

  /**
   * Test that there is exactly one record matching the given CvrID, imprinted ID, and contest name. Check that it has
   * the expected raw choices and valid interpretation.
   * @param CvrNum              The cvr record number (same as the last number in the imprinted ID).
   * @param imprintedId         The imprinted ID (scanner-batch-record).
   * @param rawChoices          The raw choices (with parentheses, presumed invalid).
   * @param validInterpretation The valid interpretation of the raw choices.
   */
  private void testIRVBallotInterpretations(final long CvrNum, final String imprintedId,
                           final List<String> rawChoices, final List<String> validInterpretation) {
    final String CVRHeader = "CVR Number";
    final String imprintedIDHeader = "Imprinted ID";

    final List<IRVBallotInterpretation> IrvBallotInterpretations
        = TestOnlyQueries.matching(tinyIRV, imprintedId, AUDITOR_ENTERED);
    assertEquals(IrvBallotInterpretations.size(), 1);
    final String result = IrvBallotInterpretations.get(0).logMessage(CVRHeader, imprintedIDHeader);
    assertEquals(result, "County Adams, Contest TinyExample1, CVR Number " + CvrNum + ", Imprinted ID " + imprintedId
        + ", Record type " + AUDITOR_ENTERED
        + ", Choices " + String.join(",",rawChoices) + ", Interpretation ["
        + String.join(",",validInterpretation) + "]");
  }

  /**
   * Test expected consequences of successful ACVR upload.
   * @param CvrId                      The CVR ID (of the original UPLOADED CVR).
   * @param expectedImprintedId        The imprinted id, scanner-batch-record.
   * @param CvrAsJson                  The upload cvr, as a json string.
   * @param expectedInterpretedChoices The expected valid interpretation, which should be stored.
   * @param expectedACVRs              The number of audit CVRs expected in total.
   */
  private void testSuccessResponse(final long CvrId, final String expectedImprintedId, final String CvrAsJson,
                                   final List<String> expectedInterpretedChoices, final int expectedACVRs) {
    final Request request = new SparkRequestStub(CvrAsJson, new HashMap<String,String>());
    uploadEndpoint.endpointBody(request, response);

    // There should now be expectedACVRs audit cvrs.
    final List<CastVoteRecord> acvrs = CastVoteRecordQueries.getMatching(1L,
        AUDITOR_ENTERED).toList();
    assertEquals(acvrs.size(), expectedACVRs);

    // There should now be an AUDITOR_ENTERED ACVR with matching cvrId.
    final CastVoteRecord acvr = acvrs.stream().filter(a -> a.getCvrId() == CvrId).findFirst().orElseThrow();
    assertEquals(acvr.recordType(), AUDITOR_ENTERED);
    // Check that we have the right record: CvrId and Imprinted ID should match.
    assertEquals(acvr.imprintedID(), expectedImprintedId);
    assertEquals(acvr.getCvrId().intValue(), CvrId);

    // Check that it has the expected vote choices.
    assertTrue(acvr.contestInfoForContestResult(tinyIRV).isPresent());
    final List<String> choices = acvr.contestInfoForContestResult(tinyIRV).get().choices();
    assertTrue(testUtils.equalStringLists(choices, expectedInterpretedChoices));
  }

  /**
   * Check that some previously-uploaded ballot has been tagged as "REAUDITED". This happens when a new ballot with the
   * same CvrId is uploaded with "reaudit = true". The idea is that the most recent REAUDIT is always simply
   * of type AUDITOR_ENTERED, but all _previously_ submitted versions of the same ballot are tagged as REAUDITED - these
   * are then omitted from discrepancy and risk calculations, because only the most recent upload counts.
   * @param CvrId                      The CVR ID (of the original UPLOADED CVR).
   * @param expectedImprintedId        The imprinted id, scanner-batch-record.
   * @param expectedInterpretedChoices The expected valid interpretation, which should be stored.
   * @param expectedReauditedBallots   The number of expected REAUDITED ballots with this CvrId.
   * @param expectedMaxRevision        The revision of the old audit cvr (null for the first audit ballot; 1L for the
   *                                   first reaudit, then incrementing by 1 for subsequent reaudits.
   */
  private void testPreviousAreReaudited(final long CvrId, final String expectedImprintedId,
      final List<String> expectedInterpretedChoices, final long expectedReauditedBallots, final long expectedMaxRevision) {

    // Check for the expected number of Reaudited ballots of the required CvrId.
    final List<CastVoteRecord> acvrs = CastVoteRecordQueries.getMatching(1L, REAUDITED).filter(a -> a.getCvrId() == CvrId).toList();
    assertEquals(acvrs.size(), expectedReauditedBallots);

    // Check that we have the right records: Imprinted ID should match.
    assertTrue(acvrs.stream().allMatch(cvr -> cvr.imprintedID().equals(expectedImprintedId)));

    // Find the maximum revision; check that it matches the expected maximum revision (this should match the number of
    // reaudits).
    assertFalse(acvrs.isEmpty());
    final OptionalLong maxRevision = acvrs.stream().mapToLong(CastVoteRecord::getRevision).max();
    assertEquals(maxRevision, OptionalLong.of(expectedMaxRevision));

    // Check that there is one record with the expected revision and it has the expected vote choices.
    final List<CastVoteRecord> maxRevisionAcvrs
        = acvrs.stream().filter(a -> a.getRevision() == expectedMaxRevision).toList();
    assertEquals(maxRevisionAcvrs.size(), 1);
    assertTrue(maxRevisionAcvrs.get(0).contestInfoForContestResult(tinyIRV).isPresent());
    final List<String> choices = maxRevisionAcvrs.get(0).contestInfoForContestResult(tinyIRV).get().choices();
    assertTrue(testUtils.equalStringLists(choices, expectedInterpretedChoices));
  }

  /**
   * Test that submitting the given ACVR to the endpoint produces the given error, and that there are no corresponding
   * ACVRs stored in the database afterward.
   * @param CvrId         The CVR ID (of the original UPLOADED CVR).
   * @param CvrAsJson     The upload cvr, as a json string.
   * @param expectedError The expected error message.
   */
  private void testErrorResponseAndNoMatchingCvr(final long CvrId, final String CvrAsJson, final String expectedError) {
    final Request request = new SparkRequestStub(CvrAsJson, new HashMap<String,String>());
    String errorBody = "";

    try {
      uploadEndpoint.endpointBody(request, response);
    } catch (HaltException e) {
      errorBody = e.body();
    }
    assertTrue(errorBody.contains(expectedError));

    // Get all the audit CVRs.
    final List<CastVoteRecord> acvrs = CastVoteRecordQueries.getMatching(1L,
        AUDITOR_ENTERED).toList();

    // There should be no audit CVR with matching CvrId.
    assertEquals(acvrs.stream().filter(a -> a.getCvrId() == CvrId).toList().size(),0);
  }

  /**
   * Start the audit round. This keeps a flag to check whether it has already been started, so it
   * only gets started once.
   */
  private static void startTheRound() {

    if(!auditRoundAlreadyStarted) {

      // Get the dashboard and start the audit round.
      final CountyDashboard cdb = Persistence.getByID(countyID, CountyDashboard.class);
      ComparisonAuditController.startRound(cdb, null,
          List.of(240509L, 240510L, 240511L, 240512L, 240513L, 240514L),
          List.of(240509L, 240510L, 240511L, 240512L, 240513L, 240514L));
      auditRoundAlreadyStarted = true;
    }
  }
}
