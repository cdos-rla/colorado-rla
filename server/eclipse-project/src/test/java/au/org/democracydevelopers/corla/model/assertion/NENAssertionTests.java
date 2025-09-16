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

package au.org.democracydevelopers.corla.model.assertion;

import static au.org.democracydevelopers.corla.util.testUtils.log;
import static java.lang.Math.ceil;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;
import us.freeandfair.corla.math.Audit;
import us.freeandfair.corla.model.CVRAuditInfo;
import us.freeandfair.corla.model.CVRContestInfo;
import us.freeandfair.corla.model.CVRContestInfo.ConsensusValue;
import us.freeandfair.corla.model.CastVoteRecord.RecordType;

/**
 * A suite of tests to verify the functionality of NENAssertion objects. This includes:
 * -- Testing of optimistic sample size computation.
 * -- Testing of estimated sample size computation.
 * -- Recording of a pre-computed discrepancy.
 * -- Removal of a pre-recorded discrepancy.
 * -- Scoring of NEN assertions.
 * -- Computation of discrepancies.
 * -- The logic involved in the re-auditing of ballots.
 * -- Risk measurement (we use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
 *    Risk Limiting Audits to compute the expected risk values).
 * Refer to the Guide to RAIRE for details on how NEN assertions are scored, and how
 * discrepancies are computed (Part 2, Appendix A.)
 */
public class NENAssertionTests extends AssertionTests {

  private static final Logger LOGGER = LogManager.getLogger(NENAssertionTests.class);


  /**
   * Test NEN assertion: Alice NEN Chuan assuming Alice and Chuan remain.
   */
  private final Assertion aliceNENChaun1 = createNENAssertion("Alice", "Chuan",
      "Test Contest", List.of("Alice", "Chuan"), 50, 0.1,
      8, Map.of(), 0, 0, 0, 0, 0);

  /**
   * Test NEN assertion: Alice NEN Chuan assuming Alice, Chuan, and Bob remain.
   */
  private final Assertion aliceNENChaun2 = createNENAssertion("Alice", "Chuan",
      "Test Contest", List.of("Alice", "Chuan", "Bob"), 50, 0.1,
      8, Map.of(), 0, 0, 0, 0, 0);


  /**
   * This suite of tests verifies the optimistic sample size computation for NEN assertions.
   * @param riskLimit Risk limit of the audit.
   * @param rawMargin Raw margin of the assertion.
   * @param dilutedMargin Diluted margin of the assertion.
   * @param difficulty Raire-computed difficulty of the assertion.
   * @param cvrDiscrepancies Map between CVR id and associated discrepancy for the assertion.
   * @param oneVoteOver Number of one vote overstatements related to the assertion.
   * @param oneVoteUnder Number of one vote understatements related to the assertion.
   * @param twoVoteOver Number of two vote overstatements related to the assertion.
   * @param twoVoteUnder Number of two vote understatements related to the assertion.
   * @param other Number of other discrepancies related to the assertion.
   */
  @Test(dataProvider = "SampleParameters", dataProviderClass = AssertionTests.class)
  public void testNENOptimistic(final BigDecimal riskLimit, final Integer rawMargin,
      final BigDecimal dilutedMargin, final BigDecimal difficulty, final Map<Long,Integer> cvrDiscrepancies,
      final Integer oneVoteOver, final Integer oneVoteUnder, final Integer twoVoteOver,
      final Integer twoVoteUnder, final Integer other)
  {
    log(LOGGER, String.format("testNENOptimistic[%f;%f;%d;%d:%d;%d;%d]", riskLimit,
        dilutedMargin, oneVoteOver, oneVoteUnder, twoVoteOver, twoVoteUnder, other));

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo, rawMargin,
        dilutedMargin.doubleValue(), difficulty.doubleValue(), cvrDiscrepancies, oneVoteOver,
        oneVoteUnder, twoVoteOver, twoVoteUnder, other);

    final int result = a.computeOptimisticSamplesToAudit(riskLimit);
    final int expected = AssertionTests.optimistic(riskLimit, dilutedMargin.doubleValue(),
        oneVoteOver, twoVoteOver, oneVoteUnder, twoVoteUnder, Audit.GAMMA);

    assertEquals(result, expected);
    assertEquals(a.optimisticSamplesToAudit.intValue(), expected);
  }

  /**
   * Test estimated sample size computation in context where none or some ballots have been sampled,
   * with various combinations of risk limit, diluted margin, and discrepancies.
   * @param auditedSamples Number of ballots that have been sampled thus far.
   * @param riskLimit Risk limit of the audit.
   * @param rawMargin Raw margin of the assertion.
   * @param dilutedMargin Diluted margin of the assertion.
   * @param difficulty Raire-computed difficulty of the assertion.
   * @param cvrDiscrepancies Map between CVR id and associated discrepancy for the assertion.
   * @param oneVoteOver Number of one vote overstatements related to the assertion.
   * @param oneVoteUnder Number of one vote understatements related to the assertion.
   * @param twoVoteOver Number of two vote overstatements related to the assertion.
   * @param twoVoteUnder Number of two vote understatements related to the assertion.
   * @param other Number of other discrepancies related to the assertion.
   */
  @Test(dataProvider = "ParametersVaryingSamples", dataProviderClass = AssertionTests.class)
  public void testNENEstimatedVaryingSamples(final Integer auditedSamples, final BigDecimal riskLimit,
      final Integer rawMargin, final BigDecimal dilutedMargin, final BigDecimal difficulty,
      final Map<Long,Integer> cvrDiscrepancies, final Integer oneVoteOver, final Integer oneVoteUnder,
      final Integer twoVoteOver, final Integer twoVoteUnder, final Integer other)
  {
    log(LOGGER, String.format("testNENEstimatedVaryingSamples[%d;%f;%f;%d;%d:%d;%d;%d]",
        auditedSamples, riskLimit, dilutedMargin, oneVoteOver, oneVoteUnder, twoVoteOver,
        twoVoteUnder, other));

    Assertion a = createNENAssertion("W", "L", TC,  AssertionTests.wlo, rawMargin,
        dilutedMargin.doubleValue(), difficulty.doubleValue(), cvrDiscrepancies, oneVoteOver,
        oneVoteUnder, twoVoteOver, twoVoteUnder, other);

    // Note that the way optimistic/estimated sample computation is performed is that
    // there is an assumption that the optimistic calculation has occurred prior to
    // 'computeEstimatedSamplesToAudit' being called. This fits with the existing design
    // of how plurality sample size computation is done.
    a.computeOptimisticSamplesToAudit(riskLimit);
    final int result = a.computeEstimatedSamplesToAudit(auditedSamples);
    final int expected = AssertionTests.estimated(riskLimit, dilutedMargin.doubleValue(),
        oneVoteOver, twoVoteOver, oneVoteUnder, twoVoteUnder, Audit.GAMMA, auditedSamples);

    assertEquals(result, expected);
    assertEquals(a.estimatedSamplesToAudit.intValue(), expected);
  }


  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) in the context where no discrepancy has
   * been computed for the given CVR-ACVR pair, and the assertion has no prior recorded
   * discrepancies. It should not change the assertion's discrepancy counts. What
   * recordDiscrepancy() does is look for the CVRAuditInfo's ID in its cvrDiscrepancy map. If it is
   * there, the value matching the ID key is retrieved and the associated discrepancy type
   * incremented. If it is not there, then the discrepancy counts are not changed.
   */
  @Test
  public void testNENRecordNoMatch1(){
    log(LOGGER, "testNENRecordNoMatch1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo, 50,
        0.1, 8, Map.of(), 0, 0, 0,
        0, 0);

    assertFalse(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of());
  }


  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) in the context where no discrepancy has
   * been computed for the given CVR-ACVR pair, and the assertion has some discrepancies recorded
   * already. It should not change the assertion's discrepancy counts.
   */
  @Test
  public void testNENRecordNoMatch2(){
    log(LOGGER, "testNENRecordNoMatch2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo, 50,
        0.1, 8,  Map.of(2L, -1, 3L, 1), 1,
        1, 0, 0, 0);

    assertFalse(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 1, 0, 0, 0,
        Map.of(2L, -1, 3L, 1));
  }

  /**
   * Test riskMeasurement() for an NEN assertion with no discrepancies.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRiskMeasurementNoDiscrepancies(){
    log(LOGGER, "testNENRiskMeasurementNoDiscrepancies");

    Assertion a = createNENAssertion("W", "L", TC,  AssertionTests.wlo, 50,
        0.1, 8,  Map.of(), 0, 0,
        0, 0, 0);

    checkRiskMeasurement(a, Map.of(5, 0.781, 10, 0.611, 100, 0.007));
  }



  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a one vote overstatement, in the context
   * where the assertion has no recorded discrepancies. Also check risk measurement given one
   * 1 vote overstatement and varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordOneVoteOverstatement1(){
    log(LOGGER, "testNENRecordOneVoteOverstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC,  AssertionTests.wlo, 50,
        0.1, 8,  Map.of(1L, 1), 0, 0,
        0, 0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 0, 0, Map.of(1L, 1));

    checkRiskMeasurement(a, Map.of(5, 1.0, 10, 1.0, 20, 0.719, 100, 0.014));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a one vote understatement, in the context
   * where the assertion has no recorded discrepancies. Also check risk measurement in the context
   * where the assertion has one 1 vote understatement and varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordOneVoteUnderstatement1(){
    log(LOGGER, "testNENRecordOneVoteUnderstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC,  AssertionTests.wlo, 60,
        0.15, 8,  Map.of(1L, -1), 0, 0,
        0, 0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 1, 0, 0, 0, Map.of(1L, -1));

    checkRiskMeasurement(a, Map.of(5, 0.464, 10, 0.319, 50, 0.016));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote overstatement, in the context
   * where the assertion has no recorded discrepancies. Also check risk measurement in the context
   * where the assertion has one 2 vote overstatement and varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordTwoVoteOverstatement1(){
    log(LOGGER, "testNENRecordTwoVoteOverstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo, 102,
        0.22, 8,  Map.of(1L, 2), 0, 0,
        0, 0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 1, 0, 0, Map.of(1L, 2));

    checkRiskMeasurement(a, Map.of(5, 1.0, 20, 1.0, 30, 0.927, 50, 0.099));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote understatement, in the context
   * where the assertion has no recorded discrepancies. Also check risk measurement in the context
   * where the assertion has one 2 vote understatement and varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordTwoVoteUnderstatement1(){
    log(LOGGER, "testNENRecordTwoVoteUnderstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC,  AssertionTests.wlo, 80,
        0.32, 8, Map.of(1L, -2), 0, 0, 0,
        0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 1, 0, Map.of(1L, -2));

    checkRiskMeasurement(a, Map.of(5, 0.221, 10, 0.096, 20, 0.018));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote understatement, in the context
   * where the assertion has no recorded discrepancies. Also check risk measurement in the context
   * where the assertion has one other discrepancies and varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordOther1(){
    log(LOGGER, "testNENRecordOther1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC,  AssertionTests.wlo, 59,
        0.19, 8,  Map.of(1L, 0), 0, 0, 0,
        0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 1, Map.of(1L, 0));

    checkRiskMeasurement(a, Map.of(5, 0.619, 10, 0.383, 50, 0.008));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a one vote overstatement, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordOneVoteOverstatement2(){
    log(LOGGER, "testNENRecordOneVoteOverstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo, 100,
        0.14, 8,  Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 1),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 2, 0, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 1));

    checkRiskMeasurement(a, Map.of(5, 1.0, 10, 0.943, 50, 0.058, 80, 0.007));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a one vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordOneVoteUnderstatement2(){
    log(LOGGER, "testNENRecordOneVoteUnderstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC,  AssertionTests.wlo, 42,
        0.09, 8,  Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -1),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 1, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -1));

    checkRiskMeasurement(a, Map.of(5, 0.531, 10, 0.426, 50, 0.072, 80, 0.019));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote overstatement, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordTwoVoteOverstatement2(){
    log(LOGGER, "testNENRecordTwoVoteOverstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC,  AssertionTests.wlo, 88,
        0.21, 8,  Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 2),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 1, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 2));

    checkRiskMeasurement(a, Map.of(5, 1.0, 10, 1.0, 50, 0.127, 80, 0.005));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordTwoVoteUnderstatement2(){
    log(LOGGER, "testNENRecordTwoVoteUnderstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo, 50,
        0.1, 8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -2),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 2, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -2));

    checkRiskMeasurement(a, Map.of(5, 0.391, 10, 0.306, 50, 0.043, 80, 0.01));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for an "other" discrepancy, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRecordOther2(){
    log(LOGGER, "testNENRecordOther2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo, 35,
        0.05, 8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 0),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 1, 2,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 0));

    checkRiskMeasurement(a, Map.of(5, 0.870, 10, 0.770, 50, 0.291, 80, 0.140));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) in the context where no discrepancy has
   * been recorded for the given CVR-ACVR pair, and the assertion has no prior recorded
   * discrepancies. It should not change the assertion's discrepancy counts. What
   * removeDiscrepancy() does is look for the CVRAuditInfo's ID in its cvrDiscrepancy map. If it is
   * there, the value matching the ID key is retrieved, the associated discrepancy type
   * decremented, and the ID removed from the map. If it is not there, then the discrepancy counts
   * and the map are not changed.  Also test risk measurement in this context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveNoMatch1(){
    log(LOGGER, "testNENRemoveNoMatch1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        24, 0.07, 8, Map.of(), 0, 0,
        0, 0, 0);

    assertFalse(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of());

    checkRiskMeasurement(a, Map.of(5, 0.843, 10, 0.710, 50, 0.180, 100, 0.033));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) in the context where no discrepancy has
   * been computed for the given CVR-ACVR pair, and the assertion has some discrepancies recorded
   * already. It should not change the assertion's discrepancy counts. Also test risk measurement in
   * this context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveNoMatch2(){
    log(LOGGER, "testNENRemoveNoMatch2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        91, 0.412, 8, Map.of(2L, -1, 3L, 1),
        1, 1, 0, 0, 0);

    assertFalse(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 1, 0, 0, 0,
        Map.of(2L, -1, 3L, 1));

    checkRiskMeasurement(a, Map.of(5, 0.431, 10, 0.143, 20, 0.016));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a one vote overstatement. Also test risk
   * measurement in this context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveOneVoteOverstatement1(){
    log(LOGGER, "testNENRemoveOneVoteOverstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        66, 0.341, 8, Map.of(1L, 1), 1,
        0, 0, 0, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L, 1));

    checkRiskMeasurement(a, Map.of(5, 0.408, 10, 0.167, 20, 0.028));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a one vote understatement. Also test risk
   * measurement in this context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveOneVoteUnderstatement1(){
    log(LOGGER, "testNENRemoveOneVoteUnderstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        44, 0.083, 8, Map.of(1L, -1), 0,
        1, 0, 0, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L, -1));

    checkRiskMeasurement(a, Map.of(5, 0.816, 10, 0.665, 20, 0.443, 50, 0.130));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote overstatement. Also test risk
   * measurement in this context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveTwoVoteOverstatement1(){
    log(LOGGER, "testNENRemoveTwoVoteOverstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        51, 0.099, 8, Map.of(1L, 2), 0,
        0, 1, 0, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L, 2));

    checkRiskMeasurement(a, Map.of(5, 0.783, 10, 0.614, 20, 0.377, 50, 0.087));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote understatement. Also test risk
   * measurement in this context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveTwoVoteUnderstatement1(){
    log(LOGGER, "testNENRemoveTwoVoteUnderstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        37, 0.1, 8, Map.of(1L, -2), 0,
        0, 0, 1, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L, -2));

    checkRiskMeasurement(a, Map.of(5, 0.781, 10, 0.611, 20, 0.373, 50, 0.085));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for an "other" discrepancy. Also test risk
   * measurement in this context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveOther1(){
    log(LOGGER, "testNENRemoveOther1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        55, 0.131, 8, Map.of(1L, 0), 0,
        0, 0, 0, 1);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L, 0));

    checkRiskMeasurement(a, Map.of(5, 0.722, 10, 0.521, 20, 0.272, 50, 0.039));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a one vote overstatement, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveOneVoteOverstatement2(){
    log(LOGGER, "testNENRemoveOneVoteOverstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        56, 0.199, 8, Map.of(1L, 0, 2L, 1, 3L, -2,
            4L, 1), 2, 0, 0, 1, 1);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 1));

    checkRiskMeasurement(a, Map.of(5, 0.594, 10, 0.359, 20, 0.131, 50, 0.006));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a one vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveOneVoteUnderstatement2(){
    log(LOGGER, "testNENRemoveOneVoteUnderstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        50, 0.112, 8, Map.of(1L, 0, 2L, 1, 3L, -1,
            4L, -1), 1, 2, 0, 0, 1);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 1, 0, 0, 1,
        Map.of(1L, 0, 2L, 1, 3L, -1, 4L, -1));

    checkRiskMeasurement(a, Map.of(5, 0.986, 10, 0.748, 20, 0.43, 50, 0.082));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote overstatement, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveTwoVoteOverstatement2(){
    log(LOGGER, "testNENRemoveTwoVoteOverstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        25, 0.066, 8, Map.of(1L, 2, 2L, 1, 3L, -2,
            4L, 2), 1, 0, 2, 1, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 1, 1, 0,
        Map.of(1L, 2, 2L, 1, 3L, -2, 4L, 2));

    checkRiskMeasurement(a, Map.of(5, 1.0, 50, 1.0, 150, 0.206, 200, 0.041));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveTwoVoteUnderstatement2(){
    log(LOGGER, "testNENRemoveTwoVoteUnderstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        54, 0.102, 8, Map.of(1L, 0, 2L, 1, 3L, -2,
            4L, -2), 1, 0, 0, 2, 1);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -2));

    checkRiskMeasurement(a, Map.of(5, 0.764, 10, 0.594, 50, 0.079));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for an "other" discrepancy, in the context
   * where the assertion has already recorded discrepancies. Also test risk measurement in this
   * context with varying sample counts.
   * We use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
   * Risk Limiting Audits to compute the expected risk values.
   */
  @Test
  public void testNENRemoveOther2(){
    log(LOGGER, "testNENRemoveOther2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNENAssertion("W", "L", TC, AssertionTests.wlo,
        133, 0.301, 8, Map.of(1L, 0, 2L, 1, 3L, -2,
            4L, 0), 1, 0, 0, 1, 2);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 0));

    checkRiskMeasurement(a, Map.of(5, 0.449, 10, 0.205, 20, 0.043));
  }


  /**
   * Test NEN assertion scoring: zero score.
   */
  @Test
  public void testNENScoreZero1() {
    log(LOGGER, "testNENScoreZero1");
    when(cvrInfo.choices()).thenReturn(List.of("Bob", "Diego"));
    final int score = aliceNENChaun1.score(cvrInfo);
    assertEquals(0, score);
  }

  /**
   * Test NEN assertion scoring: zero score.
   */
  @Test
  public void testNENScoreZero2() {
    log(LOGGER, "testNENScoreZero2");
    when(cvrInfo.choices()).thenReturn(List.of());
    final int score = aliceNENChaun1.score(cvrInfo);
    assertEquals(0, score);
  }

  /**
   * Test NEN assertion scoring: score of zero.
   */
  @Test
  public void testNENScoreZero3() {
    log(LOGGER, "testNENScoreZero3");
    when(cvrInfo.choices()).thenReturn(List.of("Diego"));
    final int score = aliceNENChaun2.score(cvrInfo);
    assertEquals(0, score);
  }

  /**
   * Test NEN assertion scoring: score of zero.
   */
  @Test
  public void testNENScoreZero4() {
    log(LOGGER, "testNENScoreZero4");
    when(cvrInfo.choices()).thenReturn(List.of("Bob", "Alice", "Diego", "Chuan"));
    final int score = aliceNENChaun2.score(cvrInfo);
    assertEquals(0, score);
  }

  /**
   * Test NEN assertion scoring: score of one.
   */
  @Test
  public void testNENScoreOne1() {
    log(LOGGER, "testNENScoreOne1");
    when(cvrInfo.choices()).thenReturn(List.of("Alice"));
    final int score = aliceNENChaun1.score(cvrInfo);
    assertEquals(1, score);
  }

  /**
   * Test NEN assertion scoring: score of one.
   */
  @Test
  public void testNENScoreOne2() {
    log(LOGGER, "testNENScoreOne2");
    when(cvrInfo.choices()).thenReturn(List.of("Alice", "Chuan"));
    final int score = aliceNENChaun2.score(cvrInfo);
    assertEquals(1, score);
  }

  /**
   * Test NEN assertion scoring: score of one.
   */
  @Test
  public void testNENScoreOne3() {
    log(LOGGER, "testNENScoreOne3");
    when(cvrInfo.choices()).thenReturn(List.of("Alice", "Bob", "Chuan"));
    final int score = aliceNENChaun1.score(cvrInfo);
    assertEquals(1, score);
  }

  /**
   * Test NEN assertion scoring: score of one.
   */
  @Test
  public void testNENScoreOne4() {
    log(LOGGER, "testNENScoreOne4");
    when(cvrInfo.choices()).thenReturn(List.of("Bob", "Alice", "Diego", "Chuan"));
    final int score = aliceNENChaun1.score(cvrInfo);
    assertEquals(1, score);
  }

  /**
   * Test NEN assertion scoring: score of minus one.
   */
  @Test
  public void testNENScoreMinusOne1() {
    log(LOGGER, "testNENScoreMinusOne1");
    when(cvrInfo.choices()).thenReturn(List.of("Diego", "Chuan", "Bob", "Alice"));
    final int score = aliceNENChaun1.score(cvrInfo);
    assertEquals(-1, score);
  }

  /**
   * Test NEN assertion scoring: score of minus one.
   */
  @Test
  public void testNENScoreMinusOne2() {
    log(LOGGER, "testNENScoreMinusOne2");
    when(cvrInfo.choices()).thenReturn(List.of("Chuan"));
    final int score = aliceNENChaun2.score(cvrInfo);
    assertEquals(-1, score);
  }

  /**
   * Test NEN assertion scoring: score of minus one.
   */
  @Test
  public void testNENScoreMinusOne3() {
    log(LOGGER, "testNENScoreMinusOne3");
    when(cvrInfo.choices()).thenReturn(List.of("Chuan", "Alice"));
    final int score = aliceNENChaun2.score(cvrInfo);
    assertEquals(-1, score);
  }

  /**
   * Test NEN assertion scoring: score of minus one.
   */
  @Test
  public void testNENScoreMinusOne4() {
    log(LOGGER, "testNENScoreMinusOne4");
    when(cvrInfo.choices()).thenReturn(List.of("Bob", "Chuan", "Alice"));
    final int score = aliceNENChaun1.score(cvrInfo);
    assertEquals(-1, score);
  }

  /**
   * Two CastVoteRecord's with a blank vote will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNone1(final RecordType auditedType){
    testNENComputeDiscrepancyNone(blank, auditedType);
  }

  /**
   * Two CastVoteRecord's with a single vote for "A" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNone2(final RecordType auditedType){
    testNENComputeDiscrepancyNone(A, auditedType);
  }

  /**
   * Two CastVoteRecord's with a single vote for "B" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNone3(final RecordType auditedType){
    testNENComputeDiscrepancyNone(B, auditedType);
  }

  /**
   * Two CastVoteRecord's with a vote for "A", "B", "C", "D" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNone4(final RecordType auditedType){
    testNENComputeDiscrepancyNone(ABCD, auditedType);
  }

  /**
   * Two CastVoteRecord's with a vote for "B", "A", "C", "D" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNone5(final RecordType auditedType){
    testNENComputeDiscrepancyNone(BACD, auditedType);
  }

  /**
   * Check that a series of NEN assertions will recognise when there is no discrepancy
   * between a CVR and audited ballot. The given vote configuration is used as the CVRContestInfo
   * field in the CVR and audited ballot CastVoteRecords.
   * @param info A vote configuration.
   */
  public void testNENComputeDiscrepancyNone(final CVRContestInfo info, final RecordType auditedType){
    log(LOGGER, String.format("testNENComputeDiscrepancyNone[%s;%s]", info.choices(), auditedType));
    resetMocks(info, info, RecordType.UPLOADED, ConsensusValue.YES, auditedType, TC);

    // Create a series of NEN assertions and check that this cvr/audited ballot are never
    // identified as having a discrepancy.
    Assertion a1 = createNENAssertion("A", "C", TC, List.of("A", "B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    Assertion a2 = createNENAssertion("D", "C", TC, List.of("C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    Assertion a3 = createNENAssertion("E", "F", TC, List.of("E", "F"), 50,
        0.1, 8, Map.of(2L, 2), 0, 0, 1,
        0, 0);

    Assertion a4 = createNENAssertion("B", "F", TC, List.of("A", "B", "D", "F"),
        50, 0.1, 8, Map.of(2L, 1), 1,
        0, 0, 0, 0);

    OptionalInt d1 = a1.computeDiscrepancy(cvr, auditedCvr);
    OptionalInt d2 = a2.computeDiscrepancy(cvr, auditedCvr);
    OptionalInt d3 = a3.computeDiscrepancy(cvr, auditedCvr);
    OptionalInt d4 = a4.computeDiscrepancy(cvr, auditedCvr);

    // None of the above calls to computeDiscrepancy should have produced a discrepancy.
    assert(d1.isEmpty() && d2.isEmpty() && d3.isEmpty() && d4.isEmpty());

    checkCountsDiscrepancyMap(a1, 0, 0, 0, 0, 0, Map.of());
    checkCountsDiscrepancyMap(a2, 0, 0, 0, 0, 0, Map.of());
    checkCountsDiscrepancyMap(a3, 0, 0, 1, 0, 0, Map.of(2L,2));
    checkCountsDiscrepancyMap(a4, 1, 0, 0, 0, 0, Map.of(2L,1));
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D" and audited ballot with vote "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertion  A NEN C assuming "A", "B",
   * "C" are continuing candidates. (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOneOver1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOneOver1[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "C",  TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A" and audited ballot with vote "B", check that the right discrepancy
   * is computed for the assertions A NEN C assuming "A" and "C" are continuing.
   * (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOneOver2(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOneOver2[%s]", recordType));
    resetMocks(A, B, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "C", TC, List.of("A", "B"), 50,
        0.1, 8, Map.of(2L, -1, 4L, 2), 0,
        1, 1, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1,2L,
            -1, 4L, 2), 0, 1, 1, 0, 0);
  }

  /**
   * Given a CVR with vote "A" and audited ballot with a blank vote, check that the right discrepancy
   * is computed for the assertions A NEN B assuming "A", "B" and "C" are continuing.
   * (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOneOver3(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOneOver3[%s]", recordType));
    resetMocks(A, blank, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A", "B", "C"),50,
        0.1, 8, Map.of(2L, -1), 0, 1,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1, 2L, -1),
        0, 0, 1, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D" and audited ballot with vote "D", "A", "B", "C",
   * check that the right discrepancy is computed for the assertion B NEN C assuming "B", "C", and
   * "D" are continuing candidates. (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOneOver4(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOneOver4[%s]", recordType));
    resetMocks(ABCD, DABC, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("B", "C",  TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A" and audited ballot with a vote of "B", check that the right discrepancy
   * is computed for the assertions A NEN B given "A" and "B", and A NEN B given "A", "B", and "C"
   * are continuing. (In this case, a two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyTwoOver1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyTwoOver1[%s]", recordType));
    resetMocks(A, B, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "B", TC,  List.of("A", "B"), 50,
        0.1, 8, Map.of(), 0, 0, 0,
        0, 0);
    Assertion a2 = createNENAssertion("A", "B", TC,  List.of("A", "B", "C"), 50,
        0.1, 8, Map.of(), 0, 0, 0,
        0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D" and audited ballot with a vote of "D", "A", "B", "C",
   * check that the right discrepancy is computed for the assertion C NEN D assuming "C",
   * and "D" are continuing. (In this case, a two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyTwoOver2(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyTwoOver2[%s]", recordType));
    resetMocks(ABCD, DABC, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("C", "D", TC,  List.of("C", "D"),
        50, 0.1, 8, Map.of(3L, 2), 0,
        0, 1, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 2,
        Map.of(1L, 2, 3L, 2), 0, 1, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D" and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertions A NEN B assuming "A", "B", "C",
   * and "D" are continuing, and A NEN B assuming "A", and "B" are continuing.
   * (In this case, a two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyTwoOver3(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyTwoOver2[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "B", TC,  List.of("A", "B", "C", "D"),
        50, 0.1, 8, Map.of(3L, 2), 0,
        0, 1, 0, 0);
    Assertion a2 = createNENAssertion("A", "B", TC,  List.of("A", "B"),
        50, 0.1, 8, Map.of(3L, 2), 0,
        0, 1, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 2,
        Map.of(1L, 2, 3L, 2), 0, 1, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D" and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertion B NEN C, assuming "A", "B", "C"
   * and "D" are continuing. (In this case, a one vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOneUnder1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOneUnder1[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("B", "C", TC, List.of("A", "B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -1, Map.of(1L, -1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a blank vote and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertion A NEN C given that "A", and "C"
   * are continuing. (In this case, a one vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOneUnder2(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOneUnder2[%s]", recordType));
    resetMocks(blank, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "C", TC, List.of("A", "C"), 50,
        0.1, 8, Map.of(2L, 0), 0, 0,
        0, 0, 1);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -1,
        Map.of(1L, -1, 2L, 0), 0, 0, 0, 0, 1);
  }

  /**
   * Given a CVR with a vote "B" and audited ballot with a vote of "A", check that the right
   * discrepancy is computed for the assertion A NEN C assuming "A", "B", and "C" are continuing.
   * (In this case, a one vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOneUnder3(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOneUnder3[%s]", recordType));
    resetMocks(B, A, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "C", TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(2L, 1), 1,
        0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -1,
        Map.of(1L, -1, 2L, 1), 1, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a vote "B" and audited ballot with a vote of "B", "A", "C", "D", check that
   * the right discrepancy is computed for the assertion A NEN C assuming "A", and "C" are continuing.
   * (In this case, a one vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOneUnder4(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOneUnder4[%s]", recordType));
    resetMocks(B, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "C", TC, List.of("A", "C"),
        50, 0.1, 8, Map.of(2L, 1), 1,
        0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -1,
        Map.of(1L, -1, 2L, 1), 1, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a vote "B" and audited ballot with a vote of "A", check that the right
   * discrepancy is computed for the assertion A NEN B assuming "A" and "B" are continuing.
   * (In this case, a two vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyTwoUnder1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyTwoUnder1[%s]", recordType));
    resetMocks(B, A, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A", "B"), 50,
        0.1, 8, Map.of(), 0, 0, 0,
        0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -2, Map.of(1L, -2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a vote "A", "B", "C", "D" and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertions B NEN A assuming "A", "B", "C",
   * and "D" are continuing, and  B NEN A assuming "A" and "B" are continuing.
   * (In this case, a two vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyTwoUnder2(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyTwoUnder2[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("B", "A", TC, List.of("A", "B", "C", "D"),
        50, 0.1, 8, Map.of(2L, -1), 0,
        1, 0, 0, 0);
    Assertion a2 = createNENAssertion("B", "A", TC, List.of("A", "B"),
        50, 0.1, 8, Map.of(2L, -1), 0,
        1, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), -2,
        Map.of(1L, -2, 2L, -1), 0, 0, 1, 0, 0);
  }

  /**
   * Given a CVR with a vote "A", "B", "C", "D" and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertions D NEN C assuming "B", "C",
   * and "D" are continuing, and D NEN E assuming "C", "D", and "E" are continuing.
   * (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOther1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOther1[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("D", "C", TC, List.of("B", "C", "D"),
        50, 0.1,  8, Map.of(2L, -1), 0,
        1, 0, 0, 0);
    Assertion a2 = createNENAssertion("D", "E", TC, List.of("C", "D", "E"),
        50, 0.1,  8, Map.of(2L, -1), 0,
        1, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 0,
        Map.of(1L, 0, 2L, -1), 0, 0, 1, 0, 0);
  }

  /**
   * Given a CVR with a vote "A" and audited ballot with a vote of "B", check that the right
   * discrepancy is computed for the assertion C NEN D assuming "C" and "D" are continuing.
   * (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOther2(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOther2[%s]", recordType));
    resetMocks(A, B, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("C", "D", TC, List.of("C", "D"), 50,
        0.1, 8, Map.of(), 0, 0, 0,
        0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a vote "A" and audited ballot with a blank vote, check that the right
   * discrepancy is computed for the assertion C NEN D assuming "A", "B", "C", and "D" are
   * continuing. (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOther3(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOther3[%s]", recordType));
    resetMocks(A, blank, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("C", "D", TC, List.of("A", "B", "C", "D"),
        50, 0.1, 8, Map.of(2L, 0), 0,
        0, 0, 0, 1);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 0, Map.of(1L, 0, 2L, 0),
        0, 0, 0, 0, 1);
  }

  /**
   * Given a CVR with a vote "A" and audited ballot with a vote of "B", "A", "C", "D", check that
   * the right discrepancy is computed for the assertion A NEN C assuming "A", "C", and "D" are
   * continuing. (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOther4(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOther4[%s]", recordType));
    resetMocks(A, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "C", TC, List.of("A", "C", "D"), 50,
        0.1, 8, Map.of(2L, 2), 0, 0, 1,
        0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 0, Map.of(1L, 0, 2L, 2),
        0, 1, 0, 0, 0);
  }

  /**
   * Given a CVR with a blank vote and audited ballot with vote "A", and "B", check that the
   * right discrepancy is computed for the assertion C NEN D assuming "C", and "D" are
   * continuing, and C NEN D assuming "A", "B", "C", and "D" are continuing.
   * (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyOther5(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyOther5[%s]", recordType));
    resetMocks(blank, BA, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("C", "D", TC, List.of("C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("C", "D", TC, List.of("A", "B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a normal ballot with a blank vote, check that the right
   * discrepancy is computed for the assertions A NEN B given A and B are continuing, and
   * C NEN A given A, B, C, and D are continuing. (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyPhantomRecordOneOver1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyPhantomRecordOneOver1[%s]", recordType));
    resetMocks(blank, blank, RecordType.PHANTOM_RECORD, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A", "B"), 50,
        0.1, 8, Map.of(), 0, 0, 0,
        0, 0);
    Assertion a2 = createNENAssertion("C", "A", TC, List.of("A", "B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a normal ballot with vote "A", "B", "C", "D", check that the right
   * discrepancy is computed for the assertions F NEN G given "B", "D", "F", and "G" are continuing,
   * and C NEN D given "B", "C", and "D" are continuing.
   * (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyPhantomRecordOneOver2(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyPhantomRecordOneOver2[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("F", "G", TC, List.of("B", "D", "F", "G"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("C", "D", TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }


  /**
   * Given a phantom CVR, and a normal ballot with vote "A", "B", "C", "D", check that the right
   * discrepancy is computed for the assertions A NEN B given "A", "B", and "C" are continuing,
   * A NEN B given "A" and "B" are continuing, and B NEN D given "B", "C" and "D" are
   * continuing. (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyPhantomRecordOther1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyPhantomRecordOther1[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("A", "B", TC, List.of("A", "B"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("B", "D", TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a normal ballot with vote "A", "B", "C", "D", check that the right
   * discrepancy is computed for the assertions B NEN A assuming "A", "B" and "D" are continuing,
   * and D NEN C assuming only "C" and "D" are continuing. (In this case, a two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyPhantomRecordTwoOver1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyPhantomRecordTwoOver1[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNENAssertion("B", "A", TC, List.of("A", "B", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("D", "C", TC, List.of("C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a ballot with vote "A", "B", "C", "D", but no consensus, check that
   * the right discrepancy is computed for any NEN assertion. (A two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyPhantomRecordNoConsensus1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyPhantomRecordNoConsensus1[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.NO, recordType, TC);

    List<Assertion> assertions = getSetAnyNEN();
    checkComputeDiscrepancy(cvr, auditedCvr, assertions, 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a ballot with a blank vote, but no consensus, check that
   * the right discrepancy is computed for any NEN assertion. (A two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyPhantomRecordNoConsensus2(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyPhantomRecordNoConsensus2[%s]", recordType));
    resetMocks(blank, blank, RecordType.PHANTOM_RECORD, ConsensusValue.NO, recordType, TC);

    List<Assertion> assertions = getSetAnyNEN();
    checkComputeDiscrepancy(cvr, auditedCvr, assertions, 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a phantom ballot, check that the right discrepancy is computed for
   * any NEN assertion. (A two vote overstatement).
   */
  @Test
  public void testNENComputeDiscrepancyPhantomRecordPhantomBallot(){
    log(LOGGER, "testNENComputeDiscrepancyPhantomRecordPhantomBallot");
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    List<Assertion> assertions = getSetAnyNEN();
    checkComputeDiscrepancy(cvr, auditedCvr, assertions, 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a phantom ballot, check that
   * the right discrepancy is computed for assertions A NEN F given "A", "C", and "F" are
   * continuing, B NEN C given "B", "C", and "D" are continuing, and C NEN F given "C" and "F"
   * are continuing. (A two vote overstatement).
   */
  @Test
  public void testNENComputeDiscrepancyPhantomBallotNormalCVR1(){
    log(LOGGER, "testNENComputeDiscrepancyPhantomRecordNormalCVR1");
    resetMocks(ABCD, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    Assertion a1 = createNENAssertion("A", "F", TC, List.of("A", "C", "F"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("B", "C", TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("C", "F", TC, List.of("C", "F"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a phantom ballot, check that
   * the right discrepancy is computed for assertions F NEN A given "A", "B", "C", and "D" are
   * continuing, D NEN B given "B" and "D" are continuing, and D NEN C given "C", and "D" are
   * continuing. (An "other" discrepancy).
   */
  @Test
  public void testNENComputeDiscrepancyPhantomBallotNormalCVR2(){
    log(LOGGER, "testNENComputeDiscrepancyPhantomRecordNormalCVR2");
    resetMocks(ABCD, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    Assertion a1 = createNENAssertion("F", "A", TC, List.of("A", "B", "C", "D"),
        50, 0.1,8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("D", "B", TC, List.of("B", "D"),
        50, 0.1,8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("D", "C", TC, List.of("C", "D"),
        50, 0.1,8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a phantom ballot, check that
   * the right discrepancy is computed for assertions B NEN C given "A", "B", and "C" are
   * continuing, D NEN C given "B", "C" and "D" are continuing. (A one vote overstatement).
   */
  @Test
  public void testNENComputeDiscrepancyPhantomBallotNormalCVR3(){
    log(LOGGER, "testNENComputeDiscrepancyPhantomRecordNormalCVR3");
    resetMocks(ABCD, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    Assertion a1 = createNENAssertion("B", "C", TC, List.of("A", "B", "C"),
        50, 0.1,8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("D", "C", TC, List.of("B", "C", "D"),
        50, 0.1,8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a blank vote, and a phantom ballot, check that the right discrepancy is computed for
   * any NEN assertion. (A one vote overstatement).
   */
  @Test
  public void testNENComputeDiscrepancyPhantomBallotNormalCVR4(){
    log(LOGGER, "testNENComputeDiscrepancyPhantomRecordNormalCVR4");
    resetMocks(blank, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    List<Assertion> assertions = getSetAnyNEN();
    checkComputeDiscrepancy(cvr, auditedCvr, assertions, 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }


  /**
   * Given a CVR with vote "A", "B", "C", "D", and a ballot with no consensus, check that
   * the right discrepancy is computed for assertions A NEN F given that "A", "C", and "F" are
   * continuing, B NEN C given "B", and "C" are continuing, and D NEN F given "D" and "F" are
   * continuing. (A two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNoConsensusNormalCVR1(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyNoConsensusNormalCVR1[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNENAssertion("A", "F", TC, List.of("A", "C", "F"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("B", "C", TC, List.of("B", "C"),50,
        0.1,8, Map.of(), 0, 0, 0,
        0, 0);
    Assertion a3 = createNENAssertion("D", "F", TC, List.of("D", "F"),
        50, 0.1,8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", and a ballot with no consensus, check that the right discrepancy
   * is computed for assertion F NEN A given "A" and "F" are continuing. (An "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNoConsensusNormalCVR2(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyNoConsensusNormalCVR2[%s]", recordType));
    resetMocks(ABCD, A, RecordType.UPLOADED, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNENAssertion("F", "A", TC, List.of("A", "F"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a ballot with no consensus, check that
   * the right discrepancy is computed for assertions B NEN C given "A", "B" and "C" are
   * continuing, and C NEN D given "B", "C", and "D" are continuing. (A one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNoConsensusNormalCVR3(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyNoConsensusNormalCVR3[%s]", recordType));
    resetMocks(ABCD, blank, RecordType.UPLOADED, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNENAssertion("B", "C", TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("C", "D", TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a blank vote, and a ballot with no consensus, check that the right
   * discrepancy is computed for any NEN assertion. (A one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNoConsensusNormalCVR4(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyNoConsensusNormalCVR4[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.UPLOADED, ConsensusValue.NO, recordType, TC);

    List<Assertion> assertions = getSetAnyNEN();
    checkComputeDiscrepancy(cvr, auditedCvr, assertions, 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR and audited ballot where the assertion's contest is not on either, check that
   * no discrepancy results for any NEN assertion.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENNoContestOnCVRAuditedBallot(final RecordType recordType){
    log(LOGGER, String.format("testNENNoContestOnCVRAuditedBallot[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    List<Assertion> assertions = getSetAnyNEN();

    for(Assertion a : assertions){
      OptionalInt d = a.computeDiscrepancy(cvr, auditedCvr);
      assert(d.isEmpty());
      assert(countsEqual(a, 0, 0, 0, 0, 0));
      assertEquals(Map.of(), a.cvrDiscrepancy);
    }
  }

  /**
   * Given a CVR that is _not_ a phantom, but does _not_ have the assertion's contest on it, and
   * an audited ballot that _is_ a phantom, check that a discrepancy of 1 (a one vote overstatement)
   * results (for any NEN assertion).
   */
  @Test
  public void testNENComputeDiscrepancyCVRNoContestPhantomBallot(){
    log(LOGGER, "testNENComputeDiscrepancyCVRNoContestPhantomBallot");
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(RecordType.PHANTOM_BALLOT);

    List<Assertion> assertions = getSetAnyNEN();

    for(Assertion a : assertions){
      OptionalInt d = a.computeDiscrepancy(cvr, auditedCvr);
      assert(d.isPresent() && d.getAsInt() == 1);
      assert(countsEqual(a, 0, 0, 0, 0, 0));
      assertEquals(Map.of(1L, 1), a.cvrDiscrepancy);
    }
  }

  /**
   * Given a CVR that _is_ a phantom and audited ballot that _is not_ a phantom, but does not
   * contain the assertion's contest on it, check that a discrepancy of 1 (a one vote overstatement)
   * results (for any NEN assertion).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyPhantomCVRBallotNoContest(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyPhantomCVRBallotNoContest[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(blank));
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(cvr.recordType()).thenReturn(RecordType.PHANTOM_RECORD);
    when(auditedCvr.recordType()).thenReturn(recordType);

    List<Assertion> assertions = getSetAnyNEN();

    for(Assertion a : assertions){
      OptionalInt d = a.computeDiscrepancy(cvr, auditedCvr);
      assert(d.isPresent() && d.getAsInt() == 1);
      assert(countsEqual(a, 0, 0, 0, 0, 0));
      assertEquals(Map.of(1L, 1), a.cvrDiscrepancy);
    }
  }

  /**
   * Given a CVR that is _not_ a phantom, but does _not_ have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, but has no consensus, check that a discrepancy of
   * 1 (a one vote overstatement) results (for any NEN assertion).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyCVRNoContestBallotNoConsensus(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyCVRNoContestBallotNoConsensus[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(ABCD.consensus()).thenReturn(ConsensusValue.NO);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    List<Assertion> assertions = getSetAnyNEN();

    for(Assertion a : assertions){
      OptionalInt d = a.computeDiscrepancy(cvr, auditedCvr);
      assert(d.isPresent() && d.getAsInt() == 1);
      assert(countsEqual(a, 0, 0, 0, 0, 0));
      assertEquals(Map.of(1L, 1), a.cvrDiscrepancy);
    }
  }

  /**
   * Given a CVR that is _not_ a phantom, but does _not_ have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, and has consensus, check that a discrepancy equal
   * to the audited ballot's score results. For this test, the resylt is a 0 (other discrepancy)
   * for all the tested assertions.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyCVRNoContestBallotScoreOfZero(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyCVRNoContestBallotScoreOfZero[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(A.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNENAssertion("B", "C", TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("C", "D", TC, List.of("B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("D", "C", TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, but does _not_ have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, and has consensus, check that a discrepancy equal
   * to the audited ballot's score results. For this test, the resylt is a 1 (one vote overstatement)
   * for all the tested assertions. (Note, in this case, the discrepancy is equal to the negation
   * of the audited ballot score).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyCVRNoContestBallotScoreOfMinusOne(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyCVRNoContestBallotScoreOfMinusOne[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNENAssertion("B", "A", TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("C", "B", TC, List.of("B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("D", "B", TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, but does _not_ have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, and has consensus, check that a discrepancy equal
   * to the audited ballot's score results. For this test, the resylt is a -1 (one vote understatement)
   * for all the tested assertions. (Note, in this case, the discrepancy is equal to the negation
   * of the audited ballot score).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyCVRNoContestBallotScoreOfOne(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyCVRNoContestBallotScoreOfOne[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("B", "F", TC, List.of("B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("B", "D", TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), -1, Map.of(1L, -1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, and does have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, but does not have the contest on it, check that a
   * discrepancy equal to the CVR's score results. For this test, the resylt is a 0 (an other
   * discrepancy) for all the tested assertions.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNormalCVRBallotNoContestZero(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyNormalCVRBallotNoContestZero[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(A));
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNENAssertion("B", "C", TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("A", "C", TC, List.of("B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("B", "A", TC, List.of("B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, and does have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, but does not have the contest on it, check that a
   * discrepancy equal to the CVR's score results. For this test, the resylt is a 1 (a one vote
   * overstatement) for all the tested assertions.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNormalCVRBallotNoContestOne(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyNormalCVRBallotNoContestOne[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNENAssertion("A", "C", TC, List.of("A", "B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("B", "C", TC, List.of("B", "C"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("D", "A", TC, List.of("D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, and does have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, but does not have the contest on it, check that a
   * discrepancy equal to the CVR's score results. For this test, the resylt is a -1 (a one vote
   * understatement) for all the tested assertions.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNENComputeDiscrepancyNormalCVRBallotNoContestMinusOne(final RecordType recordType){
    log(LOGGER, String.format("testNENComputeDiscrepancyNormalCVRBallotNoContestMinusOne[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNENAssertion("A", "C", TC, List.of("C", "D", "F"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("B", "A", TC, List.of("A", "B"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a3 = createNENAssertion("A", "D", TC, List.of("D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), -1, Map.of(1L, -1),
        0, 0, 0, 0, 0);
  }

  /**
   * Test the re-auditing of a ballot where a prior discrepancy is recorded against the
   * associated CVR (a one vote understatement). The existing discrepancies associated with the
   * 'n' copies of the CVR in the sample are removed, and discrepancy computation repeated. In this
   * case, n = 1 and the new discrepancy is a one vote overstatement.
   */
  @Test()
  public void testNENReauditBallot1(){
    log(LOGGER, String.format("testNENReauditBallot1[%s]", RecordType.REAUDITED));
    resetMocks(A, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.REAUDITED, TC);

    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A","B"), 50,
        0.1, 8, Map.of(1L, -1), 0, 1,
        0, 0, 0);

    assertTrue(a1.removeDiscrepancy(info));

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);

    assertTrue(a1.recordDiscrepancy(info));
    assert(countsEqual(a1, 1, 0, 0, 0, 0));
  }

  /**
   * Test the re-auditing of a ballot where a prior discrepancy is recorded against the
   * associated CVR (a two vote overstatement). The existing discrepancies associated with the
   * 'n' copies of the CVR in the sample are removed, and discrepancy computation repeated. In this
   * case, n = 2 and the new discrepancy is a one vote understatement.
   */
  @Test()
  public void testNENReauditBallot2(){
    log(LOGGER, String.format("testNENReauditBallot2[%s]", RecordType.REAUDITED));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, RecordType.REAUDITED, TC);

    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a1 = createNENAssertion("B", "F", TC, List.of("A","B","F"), 50,
        0.1, 8, Map.of(1L, 2), 0, 0, 2,
        0, 0);

    assertTrue(a1.removeDiscrepancy(info));
    assertTrue(a1.removeDiscrepancy(info));

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -1, Map.of(1L, -1),
        0, 0, 0, 0, 0);

    assertTrue(a1.recordDiscrepancy(info));
    assertTrue(a1.recordDiscrepancy(info));
    assert(countsEqual(a1, 0, 0, 2, 0, 0));
  }

  /**
   * Test the re-auditing of a ballot where a prior discrepancy is recorded against the
   * associated CVR (an "other" discrepancy). The existing discrepancies associated with the
   * 'n' copies of the CVR in the sample are removed, and discrepancy computation repeated. In this
   * case, n = 5 and there is no new discrepancy.
   */
  @Test()
  public void testNENReauditBallot3(){
    log(LOGGER, String.format("testNENReauditBallot3[%s]", RecordType.REAUDITED));
    resetMocks(ABCD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, RecordType.REAUDITED, TC);

    final int N = 5;
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a1 = createNENAssertion("B", "F", TC, List.of("B","F"), 50,
        0.1, 8, Map.of(1L, 0), 0, 0, 0,
        0, N);

    for(int i = 0; i < N; ++i) {
      assertTrue(a1.removeDiscrepancy(info));
    }

    OptionalInt d1 = a1.computeDiscrepancy(cvr, auditedCvr);
    assert(d1.isEmpty());

    assertEquals(Map.of(), a1.cvrDiscrepancy);
    assert(countsEqual(a1, 0, 0, 0, 0, 0));
  }

  /**
   * Test the Assertion::computeInitialOptimisticSamplesToAudit() method for NEN assertions
   * with varying diluted margins.
   */
  @Test
  public void testNENInitialOptimisticSampleSize(){
    log(LOGGER, "testNENInitialOptimisticSampleSize");
    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A", "B"),100, 0.01,
        100, Map.of(), 0, 0, 0, 0, 0);

    assertEquals(a1.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 729);

    Assertion a2 = createNENAssertion("B", "C", TC, List.of("B", "C"),159, 0.0159,
        159, Map.of(), 0, 0, 0, 0, 0);

    assertEquals(a2.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 392);

    Assertion a3 = createNENAssertion("D", "E", TC, List.of("D", "E"),1, 0.00001,
        1000, Map.of(1L, -1, 2L, 0, 3L, 1), 1,
        1, 0, 0, 1);

    assertEquals(a3.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 728698);

    Assertion a4 = createNENAssertion("F", "G", TC, List.of("F", "G"),1235, 0.12345,
        50, Map.of(1L, -2), 0, 0, 0,
        1, 0);

    assertEquals(a4.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 51);

    Assertion a5 = createNENAssertion("G", "H", TC, List.of("G", "H"),3325, 0.33247528,
        17, Map.of(1L, 1, 2L, 2), 1, 0,
        1, 0, 0);

    assertEquals(a5.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 22);
  }

  /**
   * Test the Assertion::computeOptimisticSamplesToAudit() method for NEN assertions with varying
   * diluted margins and discrepancies.
   */
  @Test
  public void computeOptimisticSamplesToAudit(){
    log(LOGGER, "computeOptimisticSamplesToAudit");
    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A", "B"),100, 0.01,
        100, Map.of(), 0, 0, 0, 0, 0);

    assertEquals(a1.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 729);
    assertEquals(a1.optimisticSamplesToAudit.intValue(), 729);

    Assertion a2 = createNENAssertion("B", "C", TC, List.of("B", "C"),159,
        0.0159, 159, Map.of(), 1, 1, 1,
        1, 1);

    assertEquals(a2.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 767);
    assertEquals(a2.optimisticSamplesToAudit.intValue(), 767);

    Assertion a3 = createNENAssertion("D", "E", TC, List.of("D", "E"),1,
        0.00001, 1000, Map.of(1L, -1, 2L, 0, 3L, 1),
        1, 1, 0, 0, 1);

    assertEquals(a3.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 783434);
    assertEquals(a3.optimisticSamplesToAudit.intValue(), 783434);

    Assertion a4 = createNENAssertion("F", "G", TC, List.of("F", "G"),1235,
        0.12345, 50, Map.of(1L, -2), 0, 0,
        0, 1, 0);

    assertEquals(a4.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 40);
    assertEquals(a4.optimisticSamplesToAudit.intValue(), 40);

    Assertion a5 = createNENAssertion("G", "H", TC, List.of("G", "H"),3325,
        0.33247528, 17, Map.of(1L, 1, 2L, 2), 1,
        0, 1, 0, 0);

    assertEquals(a5.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 47);
    assertEquals(a5.optimisticSamplesToAudit.intValue(), 47);

    Assertion a6 = createNENAssertion("B", "C", TC, List.of("B", "C"),159,
        0.0159, 159, Map.of(1L, -1, 2L, -2, 3L, 0),
        0, 1, 0, 1, 1);

    assertEquals(a6.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 253);
    assertEquals(a6.optimisticSamplesToAudit.intValue(), 253);

    Assertion a7 = createNENAssertion("B", "C", TC, List.of("B", "C"),159,
        0.0159, 159, Map.of(1L, -1, 2L, -2), 0, 1,
        0, 1, 0);

    assertEquals(a7.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 253);
    assertEquals(a7.optimisticSamplesToAudit.intValue(), 253);
  }

  /**
   * Test the Assertion::computeEstimatedSamplesToAudit() method for NEN assertions
   * with varying diluted margins, discrepancies, and current audited sample count.
   */
  @Test(dataProvider = "AuditSampleNumbers", dataProviderClass = AssertionTests.class)
  public void computeEstimatedSamplesToAudit(final int auditedSampleCount){
    log(LOGGER, String.format("computeEstimatedSamplesToAudit[%d]",auditedSampleCount));
    Assertion a1 = createNENAssertion("A", "B", TC, List.of("A","B"),100,
        0.01, 100, Map.of(), 0, 0, 0,
        0, 0);

    a1.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3);
    assertEquals(a1.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), 729);
    assertEquals(a1.optimisticSamplesToAudit.intValue(), 729);
    assertEquals(a1.estimatedSamplesToAudit.intValue(), 729);

    Assertion a2 = createNENAssertion("B", "C", TC, List.of("B","C"),159,
        0.0159, 159, Map.of(), 1, 1, 1,
        1, 1);

    a2.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5);
    double scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (2.0/(double)auditedSampleCount);
    int sample = (int)ceil(767*scalingFactor);
    assertEquals(a2.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a2.optimisticSamplesToAudit.intValue(), 767);
    assertEquals(a2.estimatedSamplesToAudit.intValue(), sample);

    Assertion a3 = createNENAssertion("D", "E", TC, List.of("D","E"),1,
        0.00001, 1000, Map.of(1L, -1, 2L, 0, 3L, 1),
        1, 1, 0, 0, 1);

    a3.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3);
    scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (1.0/(double)auditedSampleCount);
    sample = (int)ceil(783434*scalingFactor);
    assertEquals(a3.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a3.optimisticSamplesToAudit.intValue(), 783434);
    assertEquals(a3.estimatedSamplesToAudit.intValue(), sample);

    Assertion a4 = createNENAssertion("F", "G", TC, List.of("F","G"),1235,
        0.12345, 50, Map.of(1L, -2, 2L, 1), 1,
        0, 0, 1, 0);

    a4.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5);
    scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (1.0/(double)auditedSampleCount);
    sample = (int)ceil(51*scalingFactor);
    assertEquals(a4.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a4.optimisticSamplesToAudit.intValue(), 51);
    assertEquals(a4.estimatedSamplesToAudit.intValue(), sample);

    Assertion a5 = createNENAssertion("H", "I", TC, List.of("H","I"),3325,
        0.33247528, 17, Map.of(1L, 1, 2L, 2), 1,
        0, 1, 0, 0);

    a5.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3);
    scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (2.0/(double)auditedSampleCount);
    sample = (int)ceil(47*scalingFactor);
    assertEquals(a5.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a5.optimisticSamplesToAudit.intValue(), 47);
    assertEquals(a5.estimatedSamplesToAudit.intValue(), sample);

    Assertion a6 = createNENAssertion("B", "C", TC, List.of("B","C"),159,
        0.0159, 159, Map.of(1L, -1, 2L, -2, 3L, 0),
        0, 1, 0, 1, 1);

    a6.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5);
    assertEquals(a6.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), 253);
    assertEquals(a6.optimisticSamplesToAudit.intValue(), 253);
    assertEquals(a6.estimatedSamplesToAudit.intValue(), 253);

    Assertion a7 = createNENAssertion("B", "C", TC, List.of("B","C"),159,
        0.0159, 159, Map.of(1L, -1, 2L, -2), 0,
        1, 0, 1, 0);

    a7.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5);
    assertEquals(a7.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), 253);
    assertEquals(a7.optimisticSamplesToAudit.intValue(), 253);
    assertEquals(a7.estimatedSamplesToAudit.intValue(), 253);

    Assertion a8 = createNENAssertion("B", "C", TC, List.of("B","C"),159,
        0.0159, 159, Map.of(1L, -1, 2L, -2, 3L, 1,
            4L, 1, 5L, 2, 6L, 2, 7L, 1), 3, 1,
        2, 1, 0);

    scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (5.0/(double)auditedSampleCount);
    sample = (int)ceil(1434*scalingFactor);

    a8.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3);
    assertEquals(a8.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a8.optimisticSamplesToAudit.intValue(), 1434);
    assertEquals(a8.estimatedSamplesToAudit.intValue(), sample);
  }

  /**
   * Create an NEN assertion with the given parameters.
   * @param winner Winner of the assertion.
   * @param loser Loser of the assertion.
   * @param contestName Name of the contest to which the assertion belongs.
   * @param continuing List of candidates assumed to be continuing.
   * @param rawMargin Raw margin of the assertion.
   * @param dilutedMargin Diluted margin of the assertion.
   * @param difficulty Difficulty of the assertion.
   * @param cvrDiscrepancy Map between CVR ID and discrepancy type.
   * @param oneVoteOver Number of one vote overstatements to associate with the assertion.
   * @param oneVoteUnder Number of one vote understatements to associate with the assertion.
   * @param twoVoteOver Number of two vote overstatements to associate with the assertion.
   * @param twoVoteUnder Number of two vote understatements to associate with the assertion.
   * @param other Number of other discrepancies to associate with the assertion.
   * @return an NEN assertion with the given specification.
   */
  private static Assertion createNENAssertion(final String winner, final String loser,
      final String contestName, final List<String> continuing, final int rawMargin,
      final double dilutedMargin, final double difficulty,
      final Map<Long,Integer> cvrDiscrepancy, final int oneVoteOver, final int oneVoteUnder,
      final int twoVoteOver, final int twoVoteUnder, final int other){

    Assertion a = new NENAssertion();
    AssertionTests.populateAssertion(a, winner, loser, contestName, continuing, rawMargin,
        dilutedMargin, difficulty, cvrDiscrepancy, oneVoteOver, oneVoteUnder, twoVoteOver,
        twoVoteUnder, other);

    return a;
  }


  /**
   * Return a set of NEN assertions for use when testing discrepancy computation on arbitrary
   * NENs.
   * @return A set of arbitrary NEN assertions.
   */
  private static List<Assertion> getSetAnyNEN(){
    Assertion a1 = createNENAssertion("B", "A", TC, List.of("A", "B", "C", "D"),
        50, 0.1, 8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a2 = createNENAssertion("A", "B", TC, List.of("A", "B"),50,
        0.1,8, Map.of(), 0, 0, 0,
        0, 0);
    Assertion a3 = createNENAssertion("F", "G", TC, List.of("B", "D", "F", "G"),
        50, 0.1,8, Map.of(), 0, 0,
        0, 0, 0);
    Assertion a4 = createNENAssertion("F", "G", TC, List.of("F", "G"),
        50, 0.1,8, Map.of(), 0, 0,
        0, 0, 0);
    return List.of(a1, a2, a3, a4);
  }
}