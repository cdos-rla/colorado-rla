/*
Democracy Developers IRV extensions to colorado-rla.

@copyright 2024 Colorado Department of State

These IRV extensions are designed to connect to a running instance of the raire
service (https://github.com/DemocracyDevelopers/raire-service), in order to
generate assertions that can be audited using colorado-rla.

The colorado-rla IRV extensions are free software: you can redistribute it and/or modify it under the terms
of the GNU Affero General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

The colorado-rla IRV extensions are distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with
raire-service. If not, see <https://www.gnu.org/licenses/>.
*/

package au.org.democracydevelopers.corla.model;

import static au.org.democracydevelopers.corla.util.testUtils.log;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import au.org.democracydevelopers.corla.model.assertion.Assertion;
import au.org.democracydevelopers.corla.model.assertion.AssertionTests;
import au.org.democracydevelopers.corla.model.assertion.NEBAssertion;
import au.org.democracydevelopers.corla.model.assertion.NENAssertion;
import au.org.democracydevelopers.corla.util.testUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import us.freeandfair.corla.model.AuditReason;
import us.freeandfair.corla.model.AuditStatus;
import us.freeandfair.corla.model.CVRAuditInfo;
import us.freeandfair.corla.model.CVRContestInfo;
import us.freeandfair.corla.model.CVRContestInfo.ConsensusValue;
import us.freeandfair.corla.model.CastVoteRecord.RecordType;
import us.freeandfair.corla.model.ContestResult;

import javax.transaction.Transactional;
import us.freeandfair.corla.util.Pair;

/**
 * This class contains tests for the functionality present in IRVComparisonAudit.
 */
@Transactional
public class IRVComparisonAuditTests extends AssertionTests {

  private static final Logger LOGGER = LogManager.getLogger(IRVComparisonAuditTests.class);

  /**
   * Mock of a ContestResult for the contest 'One NEB Assertion Contest'.
   */
  @Mock
  private ContestResult oneNEBContestResult;

  /**
   * Mock of a ContestResult for the contest 'One NEN Assertion Contest'.
   */
  @Mock
  private ContestResult oneNENContestResult;

  /**
   * Mock of a ContestResult for the contest 'One NEN NEB Assertion Contest'.
   */
  @Mock
  private ContestResult oneNENNEBContestResult;

  /**
   * Mock of a ContestResult for the contest 'Multi-County Contest 1'.
   */
  @Mock
  private ContestResult multiCountyContestResult;

  /**
   * Mock of a ContestResult for a contest with no assertions.
   */
  @Mock
  private ContestResult doesNotExistContestResult;

  /**
   * Mock of a ContestResult for the contest 'Test Estimation NEB Only'.
   */
  @Mock
  private ContestResult testEstimationNEBOnly;

  /**
   * Mock of a ContestResult for the contest 'Test Estimation NEN Only'.
   */
  @Mock
  private ContestResult testEstimationNENOnly;

  /**
   * Mock of a ContestResult for the contest 'Test Estimation Mixed Assertions'.
   */
  @Mock
  private ContestResult testEstimationMixedAssertions;

  /**
   * Mock of a ContestResult for the contest 'Mixed Contest'.
   */
  @Mock
  private ContestResult mixedContest;

  /**
   * Mock of a ContestResult for the contest 'Mixed Contest 2'.
   */
  @Mock
  private ContestResult mixedContest2;

  /**
   * Mock of a ContestResult for the contest 'Simple Contest 3'.
   */
  @Mock
  private ContestResult simpleContest3;

  /**
   * Mock of a ContestResult for the contest 'Simple Contest 4'.
   */
  @Mock
  private ContestResult simpleContest4;

  /**
   * Mock of a CVRAuditInfo object, matched to a CVR with id 1.
   */
  @Mock
  private CVRAuditInfo auditInfo;

  /**
   * Mock of a CVRAuditInfo object, matched to a null CVR
   */
  @Mock
  private CVRAuditInfo auditInfoNullCVR;

  /**
   * Initialise mocked objects prior to the first test. Note that the diluted margin
   * returned by ContestResult's for IRV will not have a sensible value, and it will
   * not be used for IRV computations. For testing purposes, we should set it with
   * varied values and ensure that the audit itself is constructed properly.
   */
  @BeforeClass
  public void initContestResultMocks() {
    when(oneNENContestResult.getContestName()).thenReturn("One NEN Assertion Contest");
    when(oneNENContestResult.getDilutedMargin()).thenReturn(BigDecimal.ZERO);
    when(oneNEBContestResult.getContestName()).thenReturn("One NEB Assertion Contest");
    when(oneNEBContestResult.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.10));
    when(oneNENNEBContestResult.getContestName()).thenReturn("One NEN NEB Assertion Contest");
    when(oneNENNEBContestResult.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.03));
    when(multiCountyContestResult.getContestName()).thenReturn("Multi-County Contest 1");
    when(multiCountyContestResult.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.001));
    when(doesNotExistContestResult.getContestName()).thenReturn("Does Not Exist");
    when(doesNotExistContestResult.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.98));
    when(testEstimationNEBOnly.getContestName()).thenReturn("Test Estimation NEB Only");
    when(testEstimationNEBOnly.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.01));
    when(testEstimationNENOnly.getContestName()).thenReturn("Test Estimation NEN Only");
    when(testEstimationNENOnly.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.01));
    when(testEstimationMixedAssertions.getContestName()).thenReturn("Test Estimation Mixed Assertions");
    when(testEstimationMixedAssertions.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.01));
    when(mixedContest.getContestName()).thenReturn("Mixed Contest");
    when(mixedContest.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.01));

    when(mixedContest2.getContestName()).thenReturn("Mixed Contest 2");
    when(mixedContest2.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.05));

    when(simpleContest3.getContestName()).thenReturn("Simple Contest 3");
    when(simpleContest3.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.02));

    when(simpleContest4.getContestName()).thenReturn("Simple Contest 4");
    when(simpleContest4.getDilutedMargin()).thenReturn(BigDecimal.valueOf(0.07));

    when(auditInfo.id()).thenReturn(1L);
    when(auditInfo.cvr()).thenReturn(cvr);
    when(auditInfo.acvr()).thenReturn(auditedCvr);

    when(auditInfoNullCVR.id()).thenReturn(1L);
    when(auditInfoNullCVR.cvr()).thenReturn(null);
    when(auditInfoNullCVR.acvr()).thenReturn(auditedCvr);
  }

  /**
   * Create an IRVComparisonAudit for a contest with no assertions in the database.
   */
  @Test
  public void testCreateIRVAuditNoAssertions(){
    testUtils.log(LOGGER, "testCreateIRVAuditNoAssertions");
    IRVComparisonAudit ca = new IRVComparisonAudit(doesNotExistContestResult, AssertionTests.riskLimit3,
        AuditReason.OPPORTUNISTIC_BENEFITS);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS,
        AuditStatus.NOT_AUDITABLE, 0);

    assertEquals(0, ca.optimisticSamplesToAudit().intValue());
    assertEquals(0, ca.estimatedSamplesToAudit().intValue());

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(0, assertions.size());
  }

  /**
   * Create an IRVComparisonAudit for a contest with one NEB assertion stored in the database. This
   * assertion has a diluted margin of 0.32.
   */
  @Test
  public void testCreateIRVAuditOneNEBAssertion(){
    testUtils.log(LOGGER, "testCreateIRVAuditOneNEBAssertion");
    IRVComparisonAudit ca = new IRVComparisonAudit(oneNEBContestResult, AssertionTests.riskLimit3,
        AuditReason.COUNTY_WIDE_CONTEST);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.COUNTY_WIDE_CONTEST,
        AuditStatus.NOT_STARTED, 0.32);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(1, assertions.size());

    checkNEBAssertion(assertions.get(0), "Alice", "Bob", 0, 0, 0,
        0, 0, 23, 23, 1, Map.of(), 0.32);
  }

  /**
   * Create an IRVComparisonAudit for a contest with one NEB assertion stored in the database, and
   * test that riskMeasurement() returns the maximum risk (no samples audited).
   */
  @Test
  public void testCreateIRVAuditOneNEBAssertionRiskMeasurement(){
    testUtils.log(LOGGER, "testCreateIRVAuditOneNEBAssertionRiskMeasurement");
    IRVComparisonAudit ca = new IRVComparisonAudit(oneNEBContestResult, AssertionTests.riskLimit3,
        AuditReason.COUNTY_WIDE_CONTEST);

    assertEquals(0, testUtils.doubleComparator.compare(1.0, ca.riskMeasurement().doubleValue()));
  }

  /**
   * Create an IRVComparisonAudit for a contest with one NEN assertion stored in the database. This
   * assertion has a diluted margin of 0.12.
   */
  @Test
  public void testCreateIRVAuditOneNENAssertion(){
    testUtils.log(LOGGER, "testCreateIRVAuditOneNENAssertion");
    IRVComparisonAudit ca = new IRVComparisonAudit(oneNENContestResult, AssertionTests.riskLimit3,
        AuditReason.GEOGRAPHICAL_SCOPE);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.GEOGRAPHICAL_SCOPE,
        AuditStatus.NOT_STARTED, 0.12);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(1, assertions.size());

    checkNENAssertion(assertions.get(0), "Alice", "Charlie", List.of("Alice","Charlie",
            "Diego","Bob"), 0, 0, 0, 0, 0, 61,
        61, 1, Map.of(), 0.12);
  }

  /**
   * Create an IRVComparisonAudit for a contest with one NEN assertion stored in the database, and
   * test that riskMeasurement() returns the maximum risk (no samples audited).
   */
  @Test
  public void testCreateIRVAuditOneNENAssertionRiskMeasurement(){
    testUtils.log(LOGGER, "testCreateIRVAuditOneNENAssertionRiskMeasurement");
    IRVComparisonAudit ca = new IRVComparisonAudit(oneNENContestResult, AssertionTests.riskLimit3,
        AuditReason.GEOGRAPHICAL_SCOPE);

    assertEquals(0, testUtils.doubleComparator.compare(1.0, ca.riskMeasurement().doubleValue()));
  }

  /**
   * Create an IRVComparisonAudit for a contest with one NEN and one NEB assertion stored in the
   * database. The smallest diluted margin across these assertions is 0.1.
   */
  @Test
  public void testCreateIRVAuditOneNENNEBAssertion(){
    testUtils.log(LOGGER, "testCreateIRVAuditOneNENNEBAssertion");
    IRVComparisonAudit ca = new IRVComparisonAudit(oneNENNEBContestResult, AssertionTests.riskLimit3,
        AuditReason.COUNTY_WIDE_CONTEST);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.COUNTY_WIDE_CONTEST,
        AuditStatus.NOT_STARTED, 0.1);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(2, assertions.size());

    checkNEBAssertion(assertions.get(0), "Amanda", "Liesl", 0, 0,
        0, 0, 0, 73, 73, 1, Map.of(), 0.1);

    checkNENAssertion(assertions.get(1), "Amanda", "Wendell", List.of("Liesl","Wendell",
            "Amanda"), 0, 0, 0, 0, 0, 15,
        15, 1, Map.of(), 0.5);
  }

  /**
   * Create an IRVComparisonAudit for a contest with one NEN and one NEN assertion stored in the database, and
   * test that riskMeasurement() returns the maximum risk (no samples audited).
   */
  @Test
  public void testCreateIRVAuditOneNENNEBAssertionRiskMeasurement(){
    testUtils.log(LOGGER, "testCreateIRVAuditOneNENNEBAssertionRiskMeasurement");
    IRVComparisonAudit ca = new IRVComparisonAudit(oneNENNEBContestResult, AssertionTests.riskLimit3,
        AuditReason.COUNTY_WIDE_CONTEST);

    assertEquals(0, testUtils.doubleComparator.compare(1.0, ca.riskMeasurement().doubleValue()));
  }

  /**
   * Create an IRVComparisonAudit for a multi-county contest with two NEBs and one NEN stored in the
   * database. The smallest diluted margin across these assertions is 0.1.
   */
  @Test
  public void testCreateIRVAuditMultiCountyContest(){
    testUtils.log(LOGGER, "testCreateIRVAuditMultiCountyContest");
    IRVComparisonAudit ca = new IRVComparisonAudit(multiCountyContestResult,
        AssertionTests.riskLimit3, AuditReason.CLOSE_CONTEST);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.CLOSE_CONTEST,
        AuditStatus.NOT_STARTED, 0.001);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(3, assertions.size());

    checkNEBAssertion(assertions.get(0), "Charlie C. Chaplin", "Alice P. Mangrove",
        0, 0, 0, 0, 0, 729, 729,
        1, Map.of(), 0.01);

    checkNEBAssertion(assertions.get(1), "Alice P. Mangrove", "Al (Bob) Jones",
        0, 0, 0, 0, 0, 105, 105,
        1, Map.of(), 0.07);

    checkNENAssertion(assertions.get(2), "Alice P. Mangrove", "West W. Westerson",
        List.of("West W. Westerson","Alice P. Mangrove"), 0, 0, 0,
        0, 0, 7287, 7287, 1, Map.of(), 0.001);
  }

  /**
   * Test the IRVComparisonAudit::initialSamplesToAudit() method for a fresh IRVComparisonAudit
   * containing only NEB assertions.
   */
  @Test
  public void testInitialOptimisticSampleSizeNEBOnly(){
    log(LOGGER, "testInitialOptimisticSampleSizeNEBOnly");
    IRVComparisonAudit ca = new IRVComparisonAudit(testEstimationNEBOnly,
        AssertionTests.riskLimit3, AuditReason.CLOSE_CONTEST);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.CLOSE_CONTEST,
        AuditStatus.NOT_STARTED, 0.01);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(4, assertions.size());

    checkNEBAssertion(assertions.get(0), "A", "B",
        0, 0, 0, 0, 0, 729, 729,
        1, Map.of(), 0.01);

    checkNEBAssertion(assertions.get(1), "B", "C",
        0, 0, 0, 0, 0, 459, 459,
        1, Map.of(), 0.0159);

    checkNEBAssertion(assertions.get(2), "F", "G",
        0, 0, 0, 1, 0, 48, 48,
        1, Map.of(1L, -2), 0.12345);

    checkNEBAssertion(assertions.get(3), "H", "I",
        1, 1, 0, 0, 0, 47, 47,
        1, Map.of(1L, 1, 2L, 2), 0.33247528);

    assertEquals(729, ca.initialSamplesToAudit());
  }

  /**
   * Test the IRVComparisonAudit::initialSamplesToAudit() method for a fresh IRVComparisonAudit
   * containing only NEN assertions.
   */
  @Test
  public void testInitialOptimisticSampleSizeNENOnly(){
    log(LOGGER, "testInitialOptimisticSampleSizeNENOnly");
    IRVComparisonAudit ca = new IRVComparisonAudit(testEstimationNENOnly,
        AssertionTests.riskLimit3, AuditReason.CLOSE_CONTEST);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.CLOSE_CONTEST,
        AuditStatus.NOT_STARTED, 0.01);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(4, assertions.size());

    checkNENAssertion(assertions.get(0), "A", "B", List.of("A","B"),
        0, 0, 0, 0, 0, 729, 729,
        1, Map.of(), 0.01);

    checkNENAssertion(assertions.get(1), "B", "C", List.of("B","C"),
        0, 0, 0, 0, 0, 459, 459,
        1, Map.of(), 0.0159);

    checkNENAssertion(assertions.get(2), "F", "G", List.of("F","G"),
        0, 0, 0, 1, 0, 48, 48,
        1, Map.of(1L, -2), 0.12345);

    checkNENAssertion(assertions.get(3), "H", "I", List.of("H","I"),
        1, 1, 0, 0, 0, 47, 47,
        1, Map.of(1L, 1, 2L, 2), 0.33247528);

    assertEquals(729, ca.initialSamplesToAudit());
  }

  /**
   * Test the IRVComparisonAudit::initialSamplesToAudit() method for a fresh IRVComparisonAudit
   * containing only NEN assertions.
   */
  @Test
  public void testInitialOptimisticSampleSizeMixedAssertions(){
    log(LOGGER, "testInitialOptimisticSampleSizeMixedAssertions");
    IRVComparisonAudit ca = new IRVComparisonAudit(testEstimationMixedAssertions,
        AssertionTests.riskLimit3, AuditReason.CLOSE_CONTEST);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.CLOSE_CONTEST,
        AuditStatus.NOT_STARTED, 0.01);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(8, assertions.size());

    checkNEBAssertion(assertions.get(0), "A", "B",
        0, 0, 0, 0, 0, 729, 729,
        1, Map.of(), 0.01);

    checkNEBAssertion(assertions.get(1), "B", "C",
        0, 0, 0, 0, 0, 459, 459,
        1, Map.of(), 0.0159);

    checkNEBAssertion(assertions.get(2), "F", "G",
        0, 0, 0, 1, 0, 48, 48,
        1, Map.of(1L, -2), 0.12345);

    checkNEBAssertion(assertions.get(3), "H", "I",
        1, 1, 0, 0, 0, 47, 47,
        1, Map.of(1L, 1, 2L, 2), 0.33247528);

    checkNENAssertion(assertions.get(4), "A", "B", List.of("A","B"),
        0, 0, 0, 0, 0, 729, 729,
        1, Map.of(), 0.01);

    checkNENAssertion(assertions.get(5), "B", "C", List.of("B","C"),
        0, 0, 0, 0, 0, 459, 459,
        1, Map.of(), 0.0159);

    checkNENAssertion(assertions.get(6), "F", "G", List.of("F","G"),
        0, 0, 0, 1, 0, 48, 48,
        1, Map.of(1L, -2), 0.12345);

    checkNENAssertion(assertions.get(7), "H", "I", List.of("H","I"),
        1, 1, 0, 0, 0, 47, 47,
        1, Map.of(1L, 1, 2L, 2), 0.33247528);

    assertEquals(729, ca.initialSamplesToAudit());
  }


  /**
   * Two CastVoteRecord's with a blank vote will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyNone1(final RecordType auditedType){
    testComputeDiscrepancyNone(blank, auditedType, "Mixed Contest");
  }

  /**
   * Two CastVoteRecord's with a single vote for "A" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyNone2(final RecordType auditedType){
    testComputeDiscrepancyNone(A, auditedType, "Mixed Contest");
  }

  /**
   * Two CastVoteRecord's with a single vote for "B" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyNone3(final RecordType auditedType){
    testComputeDiscrepancyNone(B, auditedType, "Mixed Contest");
  }

  /**
   * Two CastVoteRecord's with a vote for "A", "B", "C", "D" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNone4(final RecordType auditedType){
    testComputeDiscrepancyNone(ABCD, auditedType, "Mixed Contest");
  }

  /**
   * Two CastVoteRecord's with a vote for "B", "A", "C", "D" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNone5(final RecordType auditedType){
    testComputeDiscrepancyNone(BACD, auditedType, "Mixed Contest");
  }

  /**
   * If we call recordDiscrepancy() for a CVR for which no computed discrepancies exist, with any
   * discrepancy type, an exception will be thrown.
   */
  @Test(dataProvider = "DiscrepancyTypes", dataProviderClass = AssertionTests.class,
    expectedExceptions = IllegalArgumentException.class)
  public void testNENRecordNoDiscrepancy(final int theType){
    IRVComparisonAudit ca = testComputeDiscrepancyNone(BACD, RecordType.AUDITOR_ENTERED,
        "Mixed Contest");

    ca.recordDiscrepancy(auditInfo, theType);
  }

  /**
   * If we call removeDiscrepancy() with an invalid discrepancy type (3), an IllegalArgumentException
   * should be thrown.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRemoveInvalidDiscrepancy1(){
    log(LOGGER, "testRemoveInvalidDiscrepancy1");
    IRVComparisonAudit ca = new IRVComparisonAudit(testEstimationNEBOnly,
        AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS);

    ca.removeDiscrepancy(auditInfo, 3);
  }

  /**
   * If we call removeDiscrepancy() with an invalid discrepancy type (-3), an IllegalArgumentException
   * should be thrown.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRemoveInvalidDiscrepancy2(){
    log(LOGGER, "testRemoveInvalidDiscrepancy2");
    IRVComparisonAudit ca = new IRVComparisonAudit(testEstimationNEBOnly,
        AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS);

    ca.removeDiscrepancy(auditInfo, -3);
  }

  /**
   * If we call removeDiscrepancy() with a null audit info, an IllegalArgumentException will be
   * thrown.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRemoveNullAuditInfo(){
    log(LOGGER, "testRemoveNullAuditInfo");
    IRVComparisonAudit ca = new IRVComparisonAudit(testEstimationNEBOnly,
        AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS);

    ca.removeDiscrepancy(null, -1);
  }

  /**
   * If we call removeDiscrepancy() with a null cvr in the provided audit info, an
   * IllegalArgumentException will be thrown.
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRemoveNullAuditInfoCVR(){
    log(LOGGER, "testRemoveNullAuditInfoCVR");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    ca.removeDiscrepancy(auditInfoNullCVR, -1);
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "A" and audited ballot "A", "B", "C" ,"D".
   * The maximum discrepancy is 0.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyCVR_A_ACVR_ABCD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeDiscrepancyCVR_A_ACVR_ABCD[%s]", auditedType));
    resetMocks(A, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(0, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);
  }

  /**
   * Test that if we follow discrepancy computation with an invalid call to recordDiscrepancy() then
   * an IllegalArgumentException is thrown. (Invalid in the sense that the provided discrepancy type
   * is not the maximum across the audit's assertions for the given CVR).
   */
  @Test(expectedExceptions = IllegalArgumentException.class)
  public void testRecordWrongDiscrepancy(){
    log(LOGGER, "testRecordWrongDiscrepancy");
    resetMocks(A, ABCD, RecordType.UPLOADED, ConsensusValue.YES, RecordType.AUDITOR_ENTERED,
        "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(0, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.recordDiscrepancy(auditInfo,2); // Should throw an exception
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "A","B","C","D" and audited ballot
   * "B","A","C","D" and recording of the resulting maximum discrepancy of type 2. Also checks
   * risk measurement before and after the removal of the recorded discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyCVR_ABCD_ACVR_BACD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyCVR_ABCD_ACVR_BACD[%s]", auditedType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(2, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, 2);

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 0, 1, 0, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(1000, 0.214),
        Pair.make(1500, 0.019)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of 2).
    ca.removeDiscrepancy(auditInfo, 2);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 1500, should yield
    // a risk of 0.001.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.001,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "A","B","C","D" and audited ballot
   * "B","A","C","D", followed by a re-audit to find the ballot was actually "A","B","C","D". Also checks
   * risk measurement before and after the removal of the recorded discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyReauditCVR_ABCD_ACVR_BACD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyReauditCVR_ABCD_ACVR_BACD[%s]", auditedType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(2, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, 2);

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 0, 1, 0, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(1000, 0.214),
        Pair.make(1500, 0.019)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of 2).
    ca.removeDiscrepancy(auditInfo, 2);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    resetMocks(ABCD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isEmpty());

    // Check that none of ca's assertions have a discrepancy associated with the CVR with ID 1L.
    for(final Assertion a : ca.getAssertions()){
      assert(a.getDiscrepancy(1L).isEmpty());
    }

    // ca.riskMeasurement() at this point, where the audited sample count is 1500, should yield
    // a risk of 0.001.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.001,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "A","B","C","D" and audited ballot
   * "B","A","C","D" and recording of the resulting maximum discrepancy of type 2. In this test,
   * the CVR appears in the sample 3 times. Also checks risk measurement before and after the removal
   * of the recorded discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyMultiplicityCVR_ABCD_ACVR_BACD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyMultiplicityCVR_ABCD_ACVR_BACD[%s]", auditedType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(2, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    for(int i = 0; i < 3; ++i) {
      ca.recordDiscrepancy(auditInfo, 2);
    }
    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 0, 3, 0, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(1000, 1.0),
        Pair.make(2500, 0.109)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of 2).
    for(int i = 0; i < 3; ++i) {
      ca.removeDiscrepancy(auditInfo, 2);
    }
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 2500, should yield
    // a risk of 0.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.0,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Given an ordered list of sample size and expected risk value, check that the risk
   * measurement method in the given IRVComparisonAudit produces expected values.
   * @param audit IRVComparisonAudit whose risk measurement method is being tested.
   * @param risks Expected risks for varying audited sample counts (ordered by sample size).
   */
  private void riskMeasurementCheck(IRVComparisonAudit audit, final List<Pair<Integer, Double>> risks){
    int sample = 0;
    for(Pair<Integer,Double> entry : risks){
      // Increment sample count according to entry key
      audit.signalSampleAudited(entry.first()-sample);
      sample += (entry.first()-sample);
      final BigDecimal r = audit.riskMeasurement();
      Assert.assertEquals(testUtils.doubleComparator.compare(entry.second(), r.doubleValue()), 0);
    }
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "A","B","C","D" and audited ballot
   * "B","A","C","D" and recording of the resulting maximum discrepancy of type 2 in the context
   * where the base ComparisonAudit does not "cover" the CVR.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordDiscrepancyNoCover(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordDiscrepancyNoCover[%s]", auditedType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(2, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // The CVR ID (1L) is not 'covered' by the audit.
    ca.recordDiscrepancy(auditInfo, 2);

    // Discrepancy counts should not have been changed.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "B","A","C","D" and audited ballot
   * "A","B","C","D". The maximum discrepancy is 1.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyCVR_BACD_ACVR_ABCD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeDiscrepancyCVR_BACD_ACVR_ABCD[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(1, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);
  }

  /**
   * Discrepancy computation and recording for 'Mixed Contest' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D". The maximum discrepancy is 1. Also checks risk measurement
   * before and after the removal of the recorded discrepancies against the ballot/CVR pair. We
   * use Equation 9 in Stark's Super Simple Simultaneous Comparison Single-Ballot Risk Limiting
   * Audits to compute the expected risk values.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyCVR_BACD_ACVR_ABCD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyCVR_BACD_ACVR_ABCD[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(1, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, 1);

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 1, 0, 0, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 1.0),
        Pair.make(100, 0.894), Pair.make(200, 0.415)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of 1).
    ca.removeDiscrepancy(auditInfo, 1);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 200, should yield
    // a risk of 0.381.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.381,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Mixed Contest' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D", where a re-audit finds the audited ballot to actually be
   * "B","A","C","D". The maximum (original) discrepancy is 1. Also checks risk measurement
   * before and after the removal of the recorded discrepancies against the ballot/CVR pair. We
   * use Equation 9 in Stark's Super Simple Simultaneous Comparison Single-Ballot Risk Limiting
   * Audits to compute the expected risk values.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyReauditCVR_BACD_ACVR_ABCD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyReauditCVR_BACD_ACVR_ABCD[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(1, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, 1);

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 1, 0, 0, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 1.0),
        Pair.make(100, 0.894), Pair.make(200, 0.415)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of 1).
    ca.removeDiscrepancy(auditInfo, 1);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    resetMocks(BACD, BACD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isEmpty());

    // Check that none of ca's assertions have a discrepancy associated with the CVR with ID 1L.
    for(final Assertion a : ca.getAssertions()){
      assert(a.getDiscrepancy(1L).isEmpty());
    }

    // ca.riskMeasurement() at this point, where the audited sample count is 200, should yield
    // a risk of 0.381.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.381,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Mixed Contest' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D". The CVR appears in the sample five times. The maximum
   * discrepancy is 1. Also checks risk measurement before and after the removal of the recorded
   * discrepancies against the ballot/CVR pair. We use Equation 9 in Stark's Super Simple Simultaneous
   * Comparison Single-Ballot Risk Limiting Audits to compute the expected risk values.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyMultiplicityCVR_BACD_ACVR_ABCD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyMultiplicityCVR_BACD_ACVR_ABCD[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(1, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    for(int i = 0; i < 5; ++i) {
      ca.recordDiscrepancy(auditInfo, 1);
    }

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 5, 0, 0, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 1.0),
        Pair.make(500, 0.572), Pair.make(1000, 0.012)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of 1).
    for(int i = 0; i < 5; ++i) {
      ca.removeDiscrepancy(auditInfo, 1);
    }
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 1000, should yield
    // a risk of 0.008.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.008,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Mixed Contest 2' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D". The maximum discrepancy is -1. Also checks risk measurement
   * before and after the removal of the recorded discrepancies against the ballot/CVR pair. We
   * use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot Risk Limiting Audits to
   * compute the expected risk values.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyCVR_BACD_ACVR_ABCD_mixed2(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyCVR_BACD_ACVR_ABCD_mixed2[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest 2");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed2();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(-1, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, -1);

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 0, 0, 1, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.412),
        Pair.make(50, 0.151), Pair.make(100, 0.045)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of -1).
    ca.removeDiscrepancy(auditInfo, -1);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.088.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.088,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Mixed Contest 2' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D", where a re-audit finds the audited ballot to be "B","A","C","D".
   * The maximum (original) discrepancy is -1. Also checks risk measurement before and after the
   * removal of the recorded discrepancies against the ballot/CVR pair. We use Equation 9 in Stark's
   * Super Simple Simultaneous Single-Ballot Risk Limiting Audits to compute the expected risk values.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyReauditCVR_BACD_ACVR_ABCD_mixed2(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyReauditCVR_BACD_ACVR_ABCD_mixed2[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest 2");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed2();

    OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(-1, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, -1);

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 0, 0, 1, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.412),
        Pair.make(50, 0.151), Pair.make(100, 0.045)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of -1).
    ca.removeDiscrepancy(auditInfo, -1);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    resetMocks(BACD, BACD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest 2");
    d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isEmpty());

    // Check that none of ca's assertions have a discrepancy associated with the CVR with ID 1L.
    for(final Assertion a : ca.getAssertions()){
      assert(a.getDiscrepancy(1L).isEmpty());
    }

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.088.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.088,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Mixed Contest 2' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D" where the CVR appears in the sample 2 times.
   * The maximum discrepancy is -1. Also checks risk measurement before and after the removal of the
   * recorded discrepancies against the ballot/CVR pair. We use Equation 9 in Stark's Super Simple
   * Simultaneous Single-Ballot Risk Limiting Audits to compute the expected risk values.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyMultiplicityCVR_BACD_ACVR_ABCD_mixed2(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyMultiplicityCVR_BACD_ACVR_ABCD_mixed2[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest 2");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed2();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(-1, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    for(int i = 0; i < 2; ++i) {
      ca.recordDiscrepancy(auditInfo, -1);
    }
    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 0, 0, 2, 0, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.278),
        Pair.make(50, 0.077), Pair.make(100, 0.023)));

    // Note that all discrepancies associated with this CVR/ballot are removed, across all
    // assertions in the audit (i.e., not just where it represented a discrepancy of -1).
    for(int i = 0; i < 2; ++i) {
      ca.removeDiscrepancy(auditInfo, -1);
    }
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.088.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.088,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Simple Contest 3' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D". The maximum discrepancy is -2. Also checks risk measurement
   * before and after the removal of the recorded discrepancies against the ballot/CVR pair. We
   * use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot Risk Limiting Audits to
   * compute the expected risk values. Simple Contest 3 contains one NEB assertion.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyCVR_BACD_ACVR_ABCD_simple3(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordDiscrepancyCVR_BACD_ACVR_ABCD_simple3[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Simple Contest 3");
    IRVComparisonAudit ca = createIRVComparisonAuditSimple3();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(-2, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, -2);

    checkDiscrepancies(ca, 0, 0, 0, 1, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.463),
        Pair.make(50, 0.314), Pair.make(100, 0.194)));

    ca.removeDiscrepancy(auditInfo, -2);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.380.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.380,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Simple Contest 3' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D", where a re-audit finds the audited ballot to be "B","A","C","D".
   * The maximum (original) discrepancy is -2. Also checks risk measurement before and after the
   * removal of the recorded discrepancies against the ballot/CVR pair. We use Equation 9 in Stark's
   * Super Simple Simultaneous Single-Ballot Risk Limiting Audits to compute the expected risk values.
   * Simple Contest 3 contains one NEB assertion.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyReauditCVR_BACD_ACVR_ABCD_simple3(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyReauditCVR_BACD_ACVR_ABCD_simple3[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Simple Contest 3");
    IRVComparisonAudit ca = createIRVComparisonAuditSimple3();

    OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(-2, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, -2);

    checkDiscrepancies(ca, 0, 0, 0, 1, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.463),
        Pair.make(50, 0.314), Pair.make(100, 0.194)));

    ca.removeDiscrepancy(auditInfo, -2);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    resetMocks(BACD, BACD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Simple Contest 3");
    d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isEmpty());

    // Check that none of ca's assertions have a discrepancy associated with the CVR with ID 1L.
    for(final Assertion a : ca.getAssertions()){
      assert(a.getDiscrepancy(1L).isEmpty());
    }

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.380.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.380,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Simple Contest 3' with CVR "B","A","C","D" and
   * audited ballot "A","B","C","D" where the CVR appears 3 times in the sample.
   * The maximum discrepancy is -2. Also checks risk measurement before and after the removal of the
   * recorded discrepancies against the ballot/CVR pair. We use Equation 9 in Stark's Super Simple
   * Simultaneous Single-Ballot Risk Limiting Audits to compute the expected risk values. Simple
   * Contest 3 contains one NEB assertion.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyMultiplicityCVR_BACD_ACVR_ABCD_simple3(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyMultiplicityCVR_BACD_ACVR_ABCD_simple3[%s]", auditedType));
    resetMocks(BACD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Simple Contest 3");
    IRVComparisonAudit ca = createIRVComparisonAuditSimple3();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(-2, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    for(int i = 0; i < 3; ++i) {
      ca.recordDiscrepancy(auditInfo, -2);
    }

    checkDiscrepancies(ca, 0, 0, 0, 3, 0);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.120),
        Pair.make(50, 0.082), Pair.make(100, 0.050)));

    for(int i = 0; i < 3; ++i) {
      ca.removeDiscrepancy(auditInfo, -2);
    }
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.380.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.380,
        ca.riskMeasurement().doubleValue()), 0);
  }


  /**
   * Discrepancy computation and recording for 'Simple Contest 4' with blank CVR vote and
   * audited ballot "A","B","C","D". The maximum discrepancy is 0. Also checks risk measurement
   * before and after the removal of the recorded discrepancy against the ballot/CVR pair. We
   * use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot Risk Limiting Audits to
   * compute the expected risk values. Simple Contest 4 contains one NEN assertion.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyCVR_BACD_ACVR_ABCD_simple4(RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordDiscrepancyCVR_BACD_ACVR_ABCD_simple4[%s]", auditedType));
    resetMocks(blank, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Simple Contest 4");
    IRVComparisonAudit ca = createIRVComparisonAuditSimple4();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(0, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, 0);

    checkDiscrepancies(ca, 0, 0, 0, 0, 1);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.710),
        Pair.make(50, 0.180), Pair.make(100, 0.033)));

    ca.removeDiscrepancy(auditInfo, 0);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.033.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.033,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "A" and audited ballot "B". The maximum
   * discrepancy is 2.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyCVR_A_ACVR_B(final RecordType auditedType){
    log(LOGGER, String.format("testComputeDiscrepancyCVR_A_ACVR_B[%s]", auditedType));
    resetMocks(A, B, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(2, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "B" and audited ballot "A". The maximum
   * discrepancy is 1.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyCVR_B_ACVR_A(final RecordType auditedType){
    log(LOGGER, String.format("testComputeDiscrepancyCVR_B_ACVR_A[%s]", auditedType));
    resetMocks(B, A, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(1, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR blank and audited ballot "A","B","C","D".
   * The maximum discrepancy is 0.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyCVR_blank_ACVR_ABCD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeDiscrepancyCVR_blank_ACVR_ABCD[%s]", auditedType));
    resetMocks(blank, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(0, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);
  }

  /**
   * Discrepancy computation and recording for 'Mixed Contest' with CVR blank and audited ballot
   * "A","B","C","D". The maximum discrepancy is 0. Also checks risk measurement before and after
   * the removal of the recorded discrepancies against the ballot/CVR pair. We use Equation 9 in
   * Stark's Super Simple Simultaneous Single-Ballot Risk Limiting Audits to compute the expected
   * risk values.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyCVR_blank_ACVR_ABCD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyCVR_blank_ACVR_ABCD[%s]", auditedType));
    resetMocks(blank, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(0, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    ca.recordDiscrepancy(auditInfo, 0);

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 0, 0, 0, 0, 1);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.926),
        Pair.make(50, 0.681), Pair.make(100, 0.464)));

    ca.removeDiscrepancy(auditInfo, 0);
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.617.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.617,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation and recording for 'Mixed Contest' with CVR blank and audited ballot
   * "A","B","C","D" where the CVR appears four times in the sample. The maximum discrepancy is 0.
   * Also checks risk measurement before and after the removal of the recorded discrepancies against
   * the ballot/CVR pair. We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot Risk
   * Limiting Audits to compute the expected risk values.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeRecordRemoveDiscrepancyMultiplicityCVR_blank_ACVR_ABCD(final RecordType auditedType){
    log(LOGGER, String.format("testComputeRecordRemoveDiscrepancyMultiplicityCVR_blank_ACVR_ABCD[%s]", auditedType));
    resetMocks(blank, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(0, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    ca.addContestCVRIds(List.of(1L));
    for(int i = 0; i < 4; ++i) {
      ca.recordDiscrepancy(auditInfo, 0);
    }

    // Note that only the maximum discrepancy across assertions is recorded in the base level
    // classes discrepancy counts (for reporting purposes). Each assertion's discrepancies are
    // taken into account for risk measurement.
    checkDiscrepancies(ca, 0, 0, 0, 0, 4);

    riskMeasurementCheck(ca, List.of(Pair.make(10, 0.926),
        Pair.make(50, 0.681), Pair.make(100, 0.464)));

    for(int i = 0; i < 4; ++i) {
      ca.removeDiscrepancy(auditInfo, 0);
    }
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    // ca.riskMeasurement() at this point, where the audited sample count is 100, should yield
    // a risk of 0.617.
    Assert.assertEquals(testUtils.doubleComparator.compare(0.617,
        ca.riskMeasurement().doubleValue()), 0);
  }

  /**
   * Discrepancy computation for 'Mixed Contest' with CVR "A","B","C","D" and audited ballot blank.
   * The maximum discrepancy is 0.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testComputeDiscrepancyCVR_ABCD_ACVR_blank(final RecordType auditedType){
    log(LOGGER, String.format("testComputeDiscrepancyCVR_ABCD_ACVR_blank[%s]", auditedType));
    resetMocks(blank, ABCD, RecordType.UPLOADED, ConsensusValue.YES, auditedType, "Mixed Contest");
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    final OptionalInt d = ca.computeDiscrepancy(cvr, auditedCvr);
    assert(d.isPresent());
    assertEquals(0, d.getAsInt());

    // Note that computeDiscrepancy() does not update internal discrepancy counts, only
    // recordDiscrepancy() and removeDiscrepancy() do.
    checkDiscrepancies(ca, 0, 0, 0, 0, 0);
  }

  /**
   * Check that the discrepancy counts in the given IRVComparisonAudit are as specified by the
   * given parameters.
   * @param ca IRVComparisonAudit whose discrepancy counts we want to check.
   * @param o1 Number of expected one vote overstatements.
   * @param o2 Number of expected two vote overstatements.
   * @param u1 Number of expected one vote understatements.
   * @param u2 Number of expected two vote understatements.
   * @param o Number of expected "other" discrepancies.
   */
  private void checkDiscrepancies(final IRVComparisonAudit ca, final int o1, final int o2,
      final int u1, final int u2, final int o){
    assertEquals(o1, ca.discrepancyCount(1));
    assertEquals(o2, ca.discrepancyCount(2));
    assertEquals(u1, ca.discrepancyCount(-1));
    assertEquals(u2, ca.discrepancyCount(-2));
    assertEquals(o, ca.discrepancyCount(0));
  }

  /**
   * Check that an IRVComparisonAudit will recognise when there is no discrepancy between a CVR and
   * audited ballot. The given vote configuration is used as the CVRContestInfo field in the CVR and
   * audited ballot CastVoteRecords.
   * @param info A vote configuration.
   * @param contestName Name of the vote's contest.
   * @return The IRVComparisonAudit constructed during the check.
   */
  private IRVComparisonAudit testComputeDiscrepancyNone(final CVRContestInfo info,
      final RecordType auditedType, final String contestName) {
    log(LOGGER, String.format("testComputeDiscrepancyNone[%s;%s]", info.choices(), auditedType));
    resetMocks(info, info, RecordType.UPLOADED, ConsensusValue.YES, auditedType, contestName);
    IRVComparisonAudit ca = createIRVComparisonAuditMixed();

    assert(ca.computeDiscrepancy(cvr, auditedCvr).isEmpty());

    checkDiscrepancies(ca, 0, 0, 0, 0, 0);
    return ca;
  }

  /**
   * Create and return an IRVComparisonAudit for the contest 'Mixed Contest'.
   * @return IRVComparisonAudit for the contest 'Mixed Contest'.
   */
  private IRVComparisonAudit createIRVComparisonAuditMixed(){
    IRVComparisonAudit ca = new IRVComparisonAudit(mixedContest,
        AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS,
        AuditStatus.NOT_STARTED, 0.01);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(5, assertions.size());

    return ca;
  }

  /**
   * Create and return an IRVComparisonAudit for the contest 'Mixed Contest 2'.
   * @return IRVComparisonAudit for the contest 'Mixed Contest 2'.
   */
  private IRVComparisonAudit createIRVComparisonAuditMixed2(){
    IRVComparisonAudit ca = new IRVComparisonAudit(mixedContest2,
        AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS,
        AuditStatus.NOT_STARTED, 0.05);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(2, assertions.size());

    return ca;
  }

  /**
   * Create and return an IRVComparisonAudit for the contest 'Simple Contest 3'.
   * @return IRVComparisonAudit for the contest 'Simple Contest 3'.
   */
  private IRVComparisonAudit createIRVComparisonAuditSimple3(){
    IRVComparisonAudit ca = new IRVComparisonAudit(simpleContest3,
        AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit3, AuditReason.OPPORTUNISTIC_BENEFITS,
        AuditStatus.NOT_STARTED, 0.02);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(1, assertions.size());

    return ca;
  }

  /**
   * Create and return an IRVComparisonAudit for the contest 'Simple Contest 4'.
   * @return IRVComparisonAudit for the contest 'Simple Contest $'.
   */
  private IRVComparisonAudit createIRVComparisonAuditSimple4(){
    IRVComparisonAudit ca = new IRVComparisonAudit(simpleContest4,
        AssertionTests.riskLimit5, AuditReason.COUNTY_WIDE_CONTEST);

    checkIRVComparisonAudit(ca, AssertionTests.riskLimit5, AuditReason.COUNTY_WIDE_CONTEST,
        AuditStatus.NOT_STARTED, 0.07);

    final List<Assertion> assertions = ca.getAssertions();
    assertEquals(1, assertions.size());

    return ca;
  }

  /**
   * A method to check that the given IRVComparisonAudit has been initialised properly.
   * @param ca IRVComparisonAudit to check.
   * @param risk Risk limit of the audit.
   * @param reason Reason for the audit.
   * @param status Current status of the audit.
   * @param dilutedMargin Diluted margin of the contest being audited.
   */
  private void checkIRVComparisonAudit(final IRVComparisonAudit ca, final BigDecimal risk,
      final AuditReason reason, final AuditStatus status, final double dilutedMargin){

    assertEquals(status, ca.auditStatus());
    assertEquals(reason, ca.auditReason());
    assertEquals(0, ca.getAuditedSampleCount().intValue());

    assertEquals(0, ca.disagreementCount());
    assertEquals(0, ca.getOverstatements().intValue());

    checkDiscrepancies(ca, 0, 0, 0, 0, 0);

    assertEquals(0, testUtils.doubleComparator.compare(
        risk.doubleValue(), ca.getRiskLimit().doubleValue()));

    assertEquals(0, testUtils.doubleComparator.compare(
        dilutedMargin, ca.getDilutedMargin().doubleValue()));
  }

  /**
   * Check that the given NEB assertion has the given characteristics.
   * @param a Assertion to check.
   * @param winner Expected winner of the assertion.
   * @param loser Expected loser of the assertion.
   * @param oneOver Expected number of one vote overstatements.
   * @param twoOver Expected number of two vote overstatements.
   * @param oneUnder Expected number of one vote understatements.
   * @param twoUnder Expected number of two vote understatements.
   * @param other Expected number of "other" discrepancies.
   * @param optimistic Expected optimistic sample size.
   * @param estimated Expected estimated sample size.
   * @param risk Expected current risk of the assertion.
   * @param cvrDiscrepancy Expected CVR ID-discrepancy map.
   * @param dilutedMargin Expected diluted margin.
   */
  private void checkNEBAssertion(final Assertion a, final String winner, final String loser,
      final int oneOver, final int twoOver, final int oneUnder, final int twoUnder, final int other,
      final int optimistic, final int estimated, final double risk, final Map<Long,Integer> cvrDiscrepancy,
      final double dilutedMargin){

    assert(a instanceof NEBAssertion);

    final String expected = String.format("%s NEB %s: oneOver = %d; twoOver = %d; oneUnder = %d, " +
            "twoUnder = %d; other = %d; optimistic = %d; estimated = %d; risk %f.", winner, loser,
        oneOver, twoOver, oneUnder, twoUnder, other, optimistic, estimated,
        BigDecimal.valueOf(risk).setScale(Assertion.RISK_DECIMALS, RoundingMode.HALF_UP));

    assertEquals(expected, a.getDescription());
    assertEquals(cvrDiscrepancy, a.getCvrDiscrepancy());

    assertEquals(0, testUtils.doubleComparator.compare(dilutedMargin,
        a.getDilutedMargin().doubleValue()));
  }

  /**
   * Check that the given NEN assertion has the given characteristics.
   * @param a Assertion to check.
   * @param winner Expected winner of the assertion.
   * @param loser Expected loser of the assertion.
   * @param continuing Expected list of assumed continuing candidates.
   * @param oneOver Expected number of one vote overstatements.
   * @param twoOver Expected number of two vote overstatements.
   * @param oneUnder Expected number of one vote understatements.
   * @param twoUnder Expected number of two vote understatements.
   * @param other Expected number of "other" discrepancies.
   * @param optimistic Expected optimistic sample size.
   * @param estimated Expected estimated sample size.
   * @param risk Expected current risk of the assertion.
   * @param cvrDiscrepancy Expected CVR ID-discrepancy map.
   * @param dilutedMargin Expected diluted margin.
   */
  private void checkNENAssertion(final Assertion a, final String winner, final String loser,
      final List<String> continuing, final int oneOver, final int twoOver, final int oneUnder,
      final int twoUnder, final int other, final int optimistic, final int estimated, final double risk,
      final Map<Long,Integer> cvrDiscrepancy, final double dilutedMargin){

    assert(a instanceof NENAssertion);

    final String expected = String.format("%s NEN %s assuming (%s) are continuing: oneOver = %d; " +
            "twoOver = %d; oneUnder = %d, twoUnder = %d; other = %d; optimistic = %d; estimated = %d; " +
            "risk %f.", winner, loser, continuing, oneOver, twoOver, oneUnder, twoUnder, other,
            optimistic, estimated, BigDecimal.valueOf(risk).setScale(Assertion.RISK_DECIMALS,
            RoundingMode.HALF_UP));

    assertEquals(expected, a.getDescription());
    assertEquals(cvrDiscrepancy, a.getCvrDiscrepancy());

    assertEquals(0, testUtils.doubleComparator.compare(dilutedMargin,
        a.getDilutedMargin().doubleValue()));
  }
}
