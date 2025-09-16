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
 * A suite of tests to verify the functionality of NEBAssertion objects. This includes:
 * -- Testing of optimistic sample size computation.
 * -- Testing of estimated sample size computation.
 * -- Recording of a pre-computed discrepancy.
 * -- Removal of a pre-recorded discrepancy.
 * -- Scoring of NEB assertions.
 * -- Computation of discrepancies for NEB assertions.
 * -- The logic involved in the re-auditing of ballots.
 * -- Risk measurement (we use Equation 9 in Stark's Super Simple Simultaneous Single-Ballot
 *    Risk Limiting Audits to compute the expected risk values).
 * Refer to the Guide to RAIRE for details on how NEB assertions are scored, and how
 * discrepancies are computed (Part 2, Appendix A).
 */
public class NEBAssertionTests extends AssertionTests {

  private static final Logger LOGGER = LogManager.getLogger(NEBAssertionTests.class);

  /**
   * Test NEB assertion: Alice NEB Chuan
   */
  private final Assertion aliceNEBChaun = createNEBAssertion("Alice", "Chuan",
      TC, 50, 0.1, 8, Map.of(),
      0, 0, 0, 0, 0);

  /**
   * This suite of tests verifies the optimistic sample size computation for NEB assertions.
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
  public void testNEBOptimistic(final BigDecimal riskLimit, final Integer rawMargin,
      final BigDecimal dilutedMargin, final BigDecimal difficulty, final Map<Long,Integer> cvrDiscrepancies,
      final Integer oneVoteOver, final Integer oneVoteUnder, final Integer twoVoteOver,
      final Integer twoVoteUnder, final Integer other){

    log(LOGGER, String.format("testNEBOptimistic[%f;%f;%d;%d:%d;%d;%d]", riskLimit, dilutedMargin,
        oneVoteOver, oneVoteUnder, twoVoteOver, twoVoteUnder, other));

    Assertion a = createNEBAssertion("W", "L", TC, rawMargin, dilutedMargin.doubleValue(),
        difficulty.doubleValue(), cvrDiscrepancies, oneVoteOver, oneVoteUnder, twoVoteOver,
        twoVoteUnder, other);

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
  public void testNEBEstimatedVaryingSamples(final Integer auditedSamples, final BigDecimal riskLimit,
      final Integer rawMargin, final BigDecimal dilutedMargin, final BigDecimal difficulty,
      final Map<Long,Integer> cvrDiscrepancies, final Integer oneVoteOver, final Integer oneVoteUnder,
      final Integer twoVoteOver, final Integer twoVoteUnder, final Integer other)
  {
    log(LOGGER, String.format("testNEBEstimatedVaryingSamples[%d;%f;%f;%d;%d:%d;%d;%d]",
        auditedSamples, riskLimit, dilutedMargin, oneVoteOver, oneVoteUnder, twoVoteOver,
        twoVoteUnder, other));

    Assertion a = createNEBAssertion("W", "L", TC, rawMargin, dilutedMargin.doubleValue(),
        difficulty.doubleValue(), cvrDiscrepancies, oneVoteOver, oneVoteUnder, twoVoteOver,
        twoVoteUnder, other);

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
   * Test riskMeasurement() for an NEB assertion with no discrepancies.
   */
  @Test
  public void testNEBRiskMeasurementNoDiscrepancies(){
    log(LOGGER, "testNEBRiskMeasurementNoDiscrepancies");

    Assertion a = createNEBAssertion("W", "L", TC,  80,
        0.2, 8,  Map.of(), 0, 0,
        0, 0, 0);

    checkRiskMeasurement(a, Map.of(5, 0.603, 10, 0.364, 50, 0.006, 100, 0.0));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) in the context where no discrepancy has
   * been computed for the given CVR-ACVR pair, and the assertion has no prior recorded
   * discrepancies. It should not change the assertion's discrepancy counts. What
   * recordDiscrepancy() does is look for the CVRAuditInfo's ID in its cvrDiscrepancy map. If it is
   * there, the value matching the ID key is retrieved and the associated discrepancy type
   * incremented. If it is not there, then the method will return 'false'.
   */
  @Test
  public void testNEBRecordNoMatch1(){
    log(LOGGER, "testNEBRecordNoMatch1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    assertFalse(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of());
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) in the context where no discrepancy has
   * been computed for the given CVR-ACVR pair, and the assertion has some discrepancies recorded
   * already. The method should return false.
   */
  @Test
  public void testNEBRecordNoMatch2(){
    log(LOGGER, "testNEBRecordNoMatch2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 50, 0.1,
        8, Map.of(2L, -1, 3L, 1), 1, 1,
        0, 0, 0);

    assertFalse(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 1, 0, 0, 0,
        Map.of(2L, -1, 3L, 1));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a one vote overstatement. Also check risk
   * measurement given one 1 vote overstatement and varying sample counts.
   */
  @Test
  public void testNEBRecordOneVoteOverstatement1(){
    log(LOGGER, "testNEBRecordOneVoteOverstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 123, 0.31,
        8, Map.of(1L, 1), 0, 0, 0,
        0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 0, 0, Map.of(1L, 1));

    checkRiskMeasurement(a, Map.of(5, 0.859, 10, 0.383, 20, 0.076, 50, 0.001));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a one vote understatement. Also check risk
   * measurement given one 1 vote understatement and varying sample counts.
   */
  @Test
  public void testNEBRecordOneVoteUnderstatement1(){
    log(LOGGER, "testNEBRecordOneVoteUnderstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC,10, 0.02,
        8, Map.of(1L, -1), 0, 0, 0,
        0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 1, 0, 0, 0, Map.of(1L, -1));

    checkRiskMeasurement(a, Map.of(5, 0.643, 10, 0.613, 20, 0.556, 50,
        0.416, 200, 0.098));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote overstatement.  Also check risk
   * measurement given one 2 vote overstatement and varying sample counts.
   */
  @Test
  public void testNEBRecordTwoVoteOverstatement1(){
    log(LOGGER, "testNEBRecordTwoVoteOverstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 81, 0.29,
        8, Map.of(1L, 2), 0, 0, 0,
        0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 1, 0, 0, Map.of(1L, 2));

    checkRiskMeasurement(a, Map.of(5, 1.0, 20, 1.0, 50, 0.014));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote understatement. Also check risk
   * measurement given one 2 vote understatement and varying sample counts.
   */
  @Test
  public void testNEBRecordTwoVoteUnderstatement1(){
    log(LOGGER, "testNEBRecordTwoVoteUnderstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC,5, 0.001,
        8, Map.of(1L, -2), 0, 0, 0,
        0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 1, 0, Map.of(1L, -2));

    checkRiskMeasurement(a, Map.of(5, 0.508, 20, 0.505, 50, 0.497, 500, 0.401));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for an "other" discrepancy. Also check risk
   * measurement given one "other" discrepancy and varying sample counts.
   */
  @Test
  public void testNEBRecordOther1(){
    log(LOGGER, "testNEBRecordOther1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 55, 0.12,
        8, Map.of(1L, 0), 0, 0, 0,
        0, 0);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 1, Map.of(1L, 0));

    checkRiskMeasurement(a, Map.of(5, 0.743, 10, 0.552, 20, 0.304, 50, 0.051));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a one vote overstatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement
   * yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRecordOneVoteOverstatement2(){
    log(LOGGER, "testNEBRecordOneVoteOverstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC, 50, 0.1,
        8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 1),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 2, 0, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 1));

    checkRiskMeasurement(a, Map.of(5, 1.0, 20, 0.706, 50, 0.161, 80, 0.037));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a one vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement
   * yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRecordOneVoteUnderstatement2(){
    log(LOGGER, "testNEBRecordOneVoteUnderstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC, 63, 0.34,
        8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -1),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 1, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -1));

    checkRiskMeasurement(a, Map.of(5, 0.271, 10, 0.111, 20, 0.019));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote overstatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement
   * yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRecordTwoVoteOverstatement2(){
    log(LOGGER, "testNEBRecordTwoVoteOverstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC,25, 0.08,
        8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 2),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 1, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 2));

    checkRiskMeasurement(a, Map.of(5, 1.0, 50, 1.0, 100, 0.516, 200, 0.01));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement
   * yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRecordTwoVoteUnderstatement2(){
    log(LOGGER, "testNEBRecordTwoVoteUnderstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC, 75, 0.25,
        8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -2),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 2, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -2));

    checkRiskMeasurement(a, Map.of(5, 0.264, 10, 0.139, 20, 0.039));
  }

  /**
   * Test Assertion::recordDiscrepancy(CVRAuditInfo) for a two vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement
   * yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRecordOther2(){
    log(LOGGER, "testNEBRecordOther2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC, 77, 0.19,
        8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 0),
        1, 0, 0, 1, 1);

    assertTrue(a.recordDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 1, 2,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 0));

    checkRiskMeasurement(a, Map.of(5, 0.608, 10, 0.377, 20, 0.144, 50, 0.008));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) in the context where no discrepancy has
   * been recorded for the given CVR-ACVR pair, and the assertion has no prior recorded
   * discrepancies. It should not change the assertion's discrepancy counts. What
   * removeDiscrepancy() does is look for the CVRAuditInfo's ID in its cvrDiscrepancy map. If it is
   * there, the value matching the ID key is retrieved, the associated discrepancy type
   * decremented, and the ID removed from the map. If it is not there, then the discrepancy counts
   * and the map are not changed, and the method returns false. Also check that risk measurement
   * yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveNoMatch1(){
    log(LOGGER, "testNEBRemoveNoMatch1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 51, 0.11,
        8, Map.of(), 0, 0, 0, 0, 0);

    assertFalse(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of());

    checkRiskMeasurement(a, Map.of(5, 0.762, 10, 0.581, 20, 0.337, 50, 0.066));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) in the context where no discrepancy has
   * been computed for the given CVR-ACVR pair, and the assertion has some discrepancies recorded
   * already. It should not change the assertion's discrepancy counts. Also check that risk measurement
   * yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveNoMatch2(){
    log(LOGGER, "testNEBRemoveNoMatch2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 53, 0.12,
        8, Map.of(2L, -1, 3L, 1), 1, 1,
        0, 0, 0);

    assertFalse(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 1, 0, 0, 0,
        Map.of(2L, -1, 3L, 1));

    checkRiskMeasurement(a, Map.of(5, 0.967, 10, 0.718, 20, 0.396, 50, 0.066));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a one vote overstatement.  Also check that
   * risk measurement yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveOneVoteOverstatement1(){
    log(LOGGER, "testNEBRemoveOneVoteOverstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 60, 0.23,
        8, Map.of(1L, 1), 1, 0, 0,
        0, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L,1));

    checkRiskMeasurement(a, Map.of(5, 0.556, 10, 0.309, 20, 0.096, 50, 0.003));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a one vote understatement. Also check that
   * risk measurement yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveOneVoteUnderstatement1(){
    log(LOGGER, "testNEBRemoveOneVoteUnderstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC,41, 0.09,
        8, Map.of(1L, -1), 0, 1, 0,
        0, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L,-1));

    checkRiskMeasurement(a, Map.of(5, 0.801, 10, 0.642, 20, 0.413, 50, 0.109));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote overstatement. Also check that
   * risk measurement yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveTwoVoteOverstatement1(){
    log(LOGGER, "testNEBRemoveTwoVoteOverstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC,35, 0.06,
        8, Map.of(1L, 2), 0, 0, 1,
        0, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L,2));

    checkRiskMeasurement(a, Map.of(5, 0.864, 10, 0.746, 20, 0.557, 50, 0.231));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote understatement. Also check that
   * risk measurement yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveTwoVoteUnderstatement1(){
    log(LOGGER, "testNEBRemoveTwoVoteUnderstatement1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 50, 0.1,
        8, Map.of(1L, -2), 0, 0, 0,
        1, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L,-2));

    checkRiskMeasurement(a, Map.of(5, 0.781, 10, 0.611, 20, 0.373, 50, 0.085));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for an "other" discrepancy. Also check that
   * risk measurement yields the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveOther1(){
    log(LOGGER, "testNEBRemoveOther1");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a = createNEBAssertion("W", "L", TC, 200, 0.5,
        8, Map.of(1L, 0), 0, 0, 0,
        0, 1);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 0, 0, 0, 0, 0, Map.of(1L,0));

    checkRiskMeasurement(a, Map.of(5, 0.253, 10, 0.064, 20, 0.004));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a one vote overstatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement yields
   * the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveOneVoteOverstatement2(){
    log(LOGGER, "testNEBRemoveOneVoteOverstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC,50, 0.1,
        8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 1),
        2, 0, 0, 1, 1);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 1));

    checkRiskMeasurement(a, Map.of(5, 0.768, 10, 0.6, 50, 0.083));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a one vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement yields
   * the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveOneVoteUnderstatement2(){
    log(LOGGER, "testNEBRemoveOneVoteUnderstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC, 64, 0.39,
        8, Map.of(1L, 0, 2L, 1, 3L, -1, 4L, -1),
        1, 2, 0, 0, 1);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 1, 0, 0, 1,
        Map.of(1L, 0, 2L, 1, 3L, -1, 4L, -1));

    checkRiskMeasurement(a, Map.of(5, 0.46, 10, 0.163, 50, 0.0));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote overstatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement yields
   * the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveTwoVoteOverstatement2(){
    log(LOGGER, "testNEBRemoveTwoVoteOverstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC, 37, 0.06,
        8, Map.of(1L, 2, 2L, 1, 3L, -2, 4L, 2),
        1, 0, 2, 1, 0);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 1, 1, 0,
        Map.of(1L, 2, 2L, 1, 3L, -2, 4L, 2));

    checkRiskMeasurement(a, Map.of(5, 1.0, 100, 1.0, 150, 0.323, 200, 0.075));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement yields
   * the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveTwoVoteUnderstatement2(){
    log(LOGGER, "testNEBRemoveTwoVoteUnderstatement2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC,71, 0.28,
        8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -2),
        1, 0, 0, 2, 1);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, -2));

    checkRiskMeasurement(a, Map.of(5, 0.476, 10, 0.231, 20, 0.054));
  }

  /**
   * Test Assertion::removeDiscrepancy(CVRAuditInfo) for a two vote understatement, in the context
   * where the assertion has already recorded discrepancies. Also check that risk measurement yields
   * the correct values for varying sample sizes.
   */
  @Test
  public void testNEBRemoveOther2(){
    log(LOGGER, "testNEBRemoveOther2");
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(4L);

    Assertion a = createNEBAssertion("W", "L", TC, 63, 0.21,
        8, Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 0),
        1, 0, 0, 1, 2);

    assertTrue(a.removeDiscrepancy(info));
    checkCountsDiscrepancyMap(a, 1, 0, 0, 1, 1,
        Map.of(1L, 0, 2L, 1, 3L, -2, 4L, 0));

    checkRiskMeasurement(a, Map.of(5, 0.577, 10, 0.338, 20, 0.117, 50, 0.005));
  }

  /**
   * Test NEB assertion scoring: zero score.
   */
  @Test
  public void testNEBScoreZero1() {
    log(LOGGER, "testNEBScoreZero1");
    when(cvrInfo.choices()).thenReturn(List.of("Bob", "Diego"));
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(0, score);
  }

  /**
   * Test NEB assertion scoring: zero score.
   */
  @Test
  public void testNEBScoreZero2() {
    log(LOGGER, "testNEBScoreZero2");
    when(cvrInfo.choices()).thenReturn(List.of());
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(0, score);
  }

  /**
   * Test NEB assertion scoring: score of zero.
   */
  @Test
  public void testNEBScoreZero3() {
    log(LOGGER, "testNEBScoreZero3");
    when(cvrInfo.choices()).thenReturn(List.of("Diego", "Alice"));
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(0, score);
  }

  /**
   * Test NEB assertion scoring: score of one.
   */
  @Test
  public void testNEBScoreOne1() {
    log(LOGGER, "testNEBScoreOne1");
    when(cvrInfo.choices()).thenReturn(List.of("Alice"));
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(1, score);
  }

  /**
   * Test NEB assertion scoring: score of one.
   */
  @Test
  public void testNEBScoreOne2() {
    log(LOGGER, "testNEBScoreOne2");
    when(cvrInfo.choices()).thenReturn(List.of("Alice", "Chuan"));
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(1, score);
  }

  /**
   * Test NEB assertion scoring: score of one.
   */
  @Test
  public void testNEBScoreOne3() {
    log(LOGGER, "testNEBScoreOne3");
    when(cvrInfo.choices()).thenReturn(List.of("Alice", "Bob", "Chuan"));
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(1, score);
  }

  /**
   * Test NEB assertion scoring: score of minus one.
   */
  @Test
  public void testNEBScoreMinusOne1() {
    log(LOGGER, "testNEBScoreMinusOne1");
    when(cvrInfo.choices()).thenReturn(List.of("Diego", "Chuan", "Bob", "Alice"));
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(-1, score);
  }

  /**
   * Test NEB assertion scoring: score of minus one.
   */
  @Test
  public void testNEBScoreMinusOne2() {
    log(LOGGER, "testNEBScoreMinusOne2");
    when(cvrInfo.choices()).thenReturn(List.of("Chuan"));
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(-1, score);
  }

  /**
   * Test NEB assertion scoring: score of minus one.
   */
  @Test
  public void testNEBScoreMinusOne3() {
    log(LOGGER, "testNEBScoreMinusOne3");
    when(cvrInfo.choices()).thenReturn(List.of("Chuan", "Alice"));
    final int score = aliceNEBChaun.score(cvrInfo);
    assertEquals(-1, score);
  }

  /**
   * Two CastVoteRecord's with a blank vote will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNone1(final RecordType auditedType){
    testNEBComputeDiscrepancyNone(blank, auditedType);
  }

  /**
   * Two CastVoteRecord's with a single vote for "A" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNone2(final RecordType auditedType){
    testNEBComputeDiscrepancyNone(A, auditedType);
  }

  /**
   * Two CastVoteRecord's with a single vote for "B" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNone3(final RecordType auditedType){
    testNEBComputeDiscrepancyNone(B, auditedType);
  }

  /**
   * Two CastVoteRecord's with a vote for "A", "B", "C", "D" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNone4(final RecordType auditedType){
    testNEBComputeDiscrepancyNone(ABCD, auditedType);
  }

  /**
   * Two CastVoteRecord's with a vote for "B", "A", "C", "D" will not trigger a discrepancy.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNone5(final RecordType auditedType){
    testNEBComputeDiscrepancyNone(BACD, auditedType);
  }

  /**
   * Check that a series of NEB assertions will recognise when there is no discrepancy
   * between a CVR and audited ballot. The given vote configuration is used as the CVRContestInfo
   * field in the CVR and audited ballot CastVoteRecords.
   * @param info A vote configuration.
   */
  public void testNEBComputeDiscrepancyNone(final CVRContestInfo info, final RecordType auditedType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyNone[%s;%s]", info.choices(), auditedType));
    resetMocks(info, info, RecordType.UPLOADED, ConsensusValue.YES, auditedType, TC);

    // Create a series of NEB assertions and check that this cvr/audited ballot are never
    // identified as having a discrepancy.
    Assertion a1 = createNEBAssertion("A", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    Assertion a2 = createNEBAssertion("D", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    Assertion a3 = createNEBAssertion("E", "F", TC, 50, 0.1,
        8, Map.of(2L, 2), 0, 0, 1,
        0, 0);

    Assertion a4 = createNEBAssertion("B", "F", TC, 50, 0.1,
        8, Map.of(2L, 1), 1, 0, 0,
        0, 0);

    OptionalInt d1 = a1.computeDiscrepancy(cvr, auditedCvr);
    OptionalInt d2 = a2.computeDiscrepancy(cvr, auditedCvr);
    OptionalInt d3 = a3.computeDiscrepancy(cvr, auditedCvr);
    OptionalInt d4 = a4.computeDiscrepancy(cvr, auditedCvr);

    // None of the above calls to computeDiscrepancy should have produced a discrepancy.
    assert(d1.isEmpty() && d2.isEmpty() && d3.isEmpty() && d4.isEmpty());

    assert(countsEqual(a1, 0, 0, 0, 0, 0));
    assertEquals(Map.of(), a1.cvrDiscrepancy);
    assert(countsEqual(a2, 0, 0, 0, 0, 0));
    assertEquals(Map.of(), a2.cvrDiscrepancy);
    assert(countsEqual(a3, 0, 1, 0, 0, 0));
    assertEquals(Map.of(2L, 2), a3.cvrDiscrepancy);
    assert(countsEqual(a4, 1, 0, 0, 0, 0));
    assertEquals(Map.of(2L, 1), a4.cvrDiscrepancy);
  }


  /**
   * Given a CVR with vote "A", "B", "C", "D" and audited ballot with vote "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertion  A NEB C. (In this case, a one
   * vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOneOver1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOneOver1[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A" and audited ballot with vote "B", check that the right discrepancy
   * is computed for the assertions A NEB C. (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOneOver2(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOneOver2[%s]", recordType));
    resetMocks(A, B, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "C", TC, 50, 0.1,
        8, Map.of(2L, -1, 4L, 2), 0, 1,
        1, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1,
            2L, -1, 4L, 2),0, 1, 1, 0, 0);
  }

  /**
   * Given a CVR with vote "A" and audited ballot with a blank vote, check that the right discrepancy
   * is computed for the assertions A NEB B. (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOneOver3(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOneOver3[%s]", recordType));
    resetMocks(A, blank, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(2L, -1), 0, 1, 0,
        0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1, 2L, -1),
        0, 0, 1, 0, 0);
  }

  /**
   * Given a CVR with vote "A" and audited ballot with a vote of "B", check that the right discrepancy
   * is computed for the assertions A NEB B. (In this case, a two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyTwoOver1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyTwoOver1[%s]", recordType));
    resetMocks(A, B, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D" and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertions A NEB B. (In this case, a
   * two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyTwoOver2(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyTwoOver2[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(3L, 2), 0, 0, 1,
        0, 0);
    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 2, Map.of(1L, 2, 3L, 2),
        0, 1, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D" and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertion B NEB C. (In this case, a
   * one vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOneUnder1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOneUnder1[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("B", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -1, Map.of(1L, -1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a blank vote and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertion B NEB C. (In this case, a
   * one vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOneUnder2(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOneUnder2[%s]", recordType));
    resetMocks(blank, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("B", "C", TC, 50, 0.1,
        8, Map.of(2L, 0), 0, 0, 0,
        0, 1);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -1, Map.of(1L, -1, 2L, 0),
        0, 0, 0, 0, 1);
  }

  /**
   * Given a CVR with a vote "B" and audited ballot with a vote of "A", check that the right
   * discrepancy is computed for the assertion A NEB C. (In this case, a one vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOneUnder3(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOneUnder3[%s]", recordType));
    resetMocks(B, A, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "C", TC, 50, 0.1,
        8, Map.of(2L, 1), 1, 0, 0,
        0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -1, Map.of(1L, -1, 2L, 1),
        1, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a vote "B" and audited ballot with a vote of "A", check that the right
   * discrepancy is computed for the assertion A NEB B. (In this case, a two vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyTwoUnder1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyTwoUnder1[%s]", recordType));
    resetMocks(B, A, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -2, Map.of(1L, -2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a vote "A", "B", "C", "D" and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertion B NEB A. (In this case, a two
   * vote understatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyTwoUnder2(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyTwoUnder2[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(2L, -1), 0, 1, 0,
        0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), -2, Map.of(1L, -2, 2L, -1),
        0, 0, 1, 0, 0);
  }

  /**
   * Given a CVR with a vote "A", "B", "C", "D" and audited ballot with a vote of "B", "A", "C", "D",
   * check that the right discrepancy is computed for the assertion D NEB C. (In this case, an
   * "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOther1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOther1[%s]", recordType));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("D", "C", TC, 50, 0.1,
        8, Map.of(2L, -1), 0, 1, 0,
        0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 0, Map.of(1L, 0,2L, -1),
        0, 0, 1, 0, 0);
  }

  /**
   * Given a CVR with a vote "A" and audited ballot with a vote of "B", check that the right
   * discrepancy is computed for the assertion C NEB D. (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOther2(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOther2[%s]", recordType));
    resetMocks(A, B, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("C", "D", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a vote "A" and audited ballot with a blank vote, check that the right
   * discrepancy is computed for the assertion C NEB D. (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyOther3(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyOther3[%s]", recordType));
    resetMocks(A, blank, RecordType.UPLOADED, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("C", "D", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a normal ballot with a blank vote, check that the right
   * discrepancy is computed for the assertion A NEB B. (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyPhantomRecordOneOver1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyPhantomRecordOneOver1[%s]", recordType));
    resetMocks(blank, blank, RecordType.PHANTOM_RECORD, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a normal ballot with vote "A", "B", "C", "D", check that the right
   * discrepancy is computed for the assertion F NEB G. (In this case, a one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyPhantomRecordOneOver2(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyPhantomRecordOneOver2[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a normal ballot with vote "A", "B", "C", "D", check that the right
   * discrepancy is computed for the assertion A NEB B. (In this case, an "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyPhantomRecordOther1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyPhantomRecordOther1[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a normal ballot with vote "A", "B", "C", "D", check that the right
   * discrepancy is computed for the assertion B NEB A. (In this case, a two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyPhantomRecordTwoOver1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyPhantomRecordTwoOver1[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.YES, recordType, TC);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a ballot with vote "A", "B", "C", "D", but no consensus, check that
   * the right discrepancy is computed for any NEB assertion. (A two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyPhantomRecordNoConsensus1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyPhantomRecordNoConsensus1[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a ballot with a blank vote, but no consensus, check that
   * the right discrepancy is computed for any NEB assertion. (A two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyPhantomRecordNoConsensus2(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyPhantomRecordNoConsensus2[%s]", recordType));
    resetMocks(blank, blank, RecordType.PHANTOM_RECORD, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a phantom CVR, and a phantom ballot, check that the right discrepancy is computed for
   * any NEB assertion. (A two vote overstatement).
   */
  @Test
  public void testNEBComputeDiscrepancyPhantomRecordPhantomBallot(){
    log(LOGGER, "testNEBComputeDiscrepancyPhantomRecordPhantomBallot");
    resetMocks(blank, ABCD, RecordType.PHANTOM_RECORD, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a phantom ballot, check that
   * the right discrepancy is computed for assertions A NEB F, and A NEB C. (A two vote overstatement).
   */
  @Test
  public void testNEBComputeDiscrepancyPhantomBallotNormalCVR1(){
    log(LOGGER, "testNEBComputeDiscrepancyPhantomRecordNormalCVR1");
    resetMocks(ABCD, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    Assertion a1 = createNEBAssertion("A", "F", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a phantom ballot, check that
   * the right discrepancy is computed for assertions F NEB A, and D NEB B. (An "other" discrepancy).
   */
  @Test
  public void testNEBComputeDiscrepancyPhantomBallotNormalCVR2(){
    log(LOGGER, "testNEBComputeDiscrepancyPhantomRecordNormalCVR2");
    resetMocks(ABCD, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    Assertion a1 = createNEBAssertion("F", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("D", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a phantom ballot, check that
   * the right discrepancy is computed for assertions B NEB C, and B NEB D. (A one vote overstatement).
   */
  @Test
  public void testNEBComputeDiscrepancyPhantomBallotNormalCVR3(){
    log(LOGGER, "testNEBComputeDiscrepancyPhantomRecordNormalCVR3");
    resetMocks(ABCD, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    Assertion a1 = createNEBAssertion("B", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("B", "D", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a blank vote, and a phantom ballot, check that the right discrepancy is computed for
   * any NEB assertion. (A one vote overstatement).
   */
  @Test
  public void testNEBComputeDiscrepancyPhantomBallotNormalCVR4(){
    log(LOGGER, "testNEBComputeDiscrepancyPhantomRecordNormalCVR4");
    resetMocks(blank, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.PHANTOM_BALLOT, TC);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a ballot with no consensus, check that
   * the right discrepancy is computed for assertions A NEB F, and A NEB C. (A two vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNoConsensusNormalCVR1(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyNoConsensusNormalCVR1[%s]", recordType));
    resetMocks(ABCD, ABCD, RecordType.UPLOADED, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNEBAssertion("A", "F", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 2, Map.of(1L, 2),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a ballot with no consensus, check that
   * the right discrepancy is computed for assertions F NEB A, and D NEB A. (An "other" discrepancy).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNoConsensusNormalCVR2(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyNoConsensusNormalCVR2[%s]", recordType));
    resetMocks(ABCD, ABCD, RecordType.UPLOADED, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNEBAssertion("F", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("D", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with vote "A", "B", "C", "D", and a ballot with no consensus, check that
   * the right discrepancy is computed for assertions B NEB C, and B NEB D. (A one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNoConsensusNormalCVR3(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyNoConsensusNormalCVR3[%s]", recordType));
    resetMocks(ABCD, blank, RecordType.UPLOADED, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNEBAssertion("B", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("B", "D", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR with a blank vote, and a ballot with no consensus, check that the right
   * discrepancy is computed for any NEB assertion. (A one vote overstatement).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNoConsensusNormalCVR4(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyNoConsensusNormalCVR4[%s]", recordType));
    resetMocks(blank, ABCD, RecordType.UPLOADED, ConsensusValue.NO, recordType, TC);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR and audited ballot where the assertion's contest is not on either, check that
   * no discrepancy results for any NEB assertion.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBNoContestOnCVRAuditedBallot(final RecordType recordType){
    log(LOGGER, String.format("testNEBNoContestOnCVRAuditedBallot[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(1L, 1), 1, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(1L, -1, 2L, 0, 3L, 2), 0,
        1, 1, 0, 1);

    OptionalInt d1 = a1.computeDiscrepancy(cvr, auditedCvr);
    OptionalInt d2 = a2.computeDiscrepancy(cvr, auditedCvr);
    OptionalInt d3 = a3.computeDiscrepancy(cvr, auditedCvr);

    // None of the above calls to computeDiscrepancy should have produced a discrepancy.
    assert(d1.isEmpty() && d2.isEmpty() && d3.isEmpty());

    assert(countsEqual(a1, 0, 0, 0, 0, 0));
    assertEquals(Map.of(), a1.cvrDiscrepancy);
    assert(countsEqual(a2, 1, 0, 0, 0, 0));
    assertEquals(Map.of(1L, 1), a2.cvrDiscrepancy);
    assert(countsEqual(a3, 0, 1, 1, 0, 1));
    assertEquals(Map.of(1L, -1, 2L, 0, 3L, 2), a3.cvrDiscrepancy);
  }

  /**
   * Given a CVR that is _not_ a phantom, but does _not_ have the assertion's contest on it, and
   * an audited ballot that _is_ a phantom, check that a discrepancy of 1 (a one vote overstatement)
   * results (for any NEB assertion).
   */
  @Test
  public void testNEBComputeDiscrepancyCVRNoContestPhantomBallot(){
    log(LOGGER, "testNEBComputeDiscrepancyCVRNoContestPhantomBallot");
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(RecordType.PHANTOM_BALLOT);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that _is_ a phantom and audited ballot that _is not_ a phantom, but does not
   * contain the assertion's contest on it, check that a discrepancy of 1 (a one vote overstatement)
   * results (for any NEB assertion).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyPhantomCVRBallotNoContest(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyPhantomCVRBallotNoContest[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(blank));
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(cvr.recordType()).thenReturn(RecordType.PHANTOM_RECORD);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, but does _not_ have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, but has no consensus, check that a discrepancy of
   * 1 (a one vote overstatement) results (for any NEB assertion).
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyCVRNoContestBallotNoConsensus(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyCVRNoContestBallotNoConsensus[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(ABCD.consensus()).thenReturn(ConsensusValue.NO);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a3 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2, a3), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, but does _not_ have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, and has consensus, check that a discrepancy equal
   * to the audited ballot's score results. For this test, the resylt is a 0 (other discrepancy)
   * for all the tested assertions.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyCVRNoContestBallotScoreOfZero(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyCVRNoContestBallotScoreOfZero[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(A));

    when(A.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("B", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 0, Map.of(1L, 0),
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
  public void testNEBComputeDiscrepancyCVRNoContestBallotScoreOfMinusOne(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyCVRNoContestBallotScoreOfMinusOne[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("B", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("F", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 1, Map.of(1L, 1),
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
  public void testNEBComputeDiscrepancyCVRNoContestBallotScoreOfOne(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyCVRNoContestBallotScoreOfOne[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("A", "D", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "F", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), -1, Map.of(1L, -1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, and does have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, but does not have the contest on it, check that a
   * discrepancy equal to the CVR's score results. For this test, the resylt is a 0 (an other
   * discrepancy) for all the tested assertions.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNormalCVRBallotNoContestZero(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyNormalCVRBallotNoContestZero[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(A));
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("B", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("F", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 0, Map.of(1L, 0),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, and does have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, but does not have the contest on it, check that a
   * discrepancy equal to the CVR's score results. For this test, the resylt is a 1 (a one vote
   * overstatement) for all the tested assertions.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNormalCVRBallotNoContestOne(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyNormalCVRBallotNoContestOne[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(A));
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("A", "C", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("A", "G", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), 1, Map.of(1L, 1),
        0, 0, 0, 0, 0);
  }

  /**
   * Given a CVR that is _not_ a phantom, and does have the assertion's contest on it, and
   * an audited ballot that _is not_ a phantom, but does not have the contest on it, check that a
   * discrepancy equal to the CVR's score results. For this test, the resylt is a -1 (a one vote
   * understatement) for all the tested assertions.
   */
  @Test(dataProvider = "AuditedRecordTypes", dataProviderClass = AssertionTests.class)
  public void testNEBComputeDiscrepancyNormalCVRBallotNoContestMinusOne(final RecordType recordType){
    log(LOGGER, String.format("testNEBComputeDiscrepancyNormalCVRBallotNoContestMinusOne[%s]", recordType));
    when(cvr.contestInfoForContestResult(TC)).thenReturn(Optional.of(ABCD));
    when(auditedCvr.contestInfoForContestResult(TC)).thenReturn(Optional.empty());

    when(ABCD.consensus()).thenReturn(ConsensusValue.YES);
    when(cvr.recordType()).thenReturn(RecordType.UPLOADED);
    when(auditedCvr.recordType()).thenReturn(recordType);

    Assertion a1 = createNEBAssertion("D", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);
    Assertion a2 = createNEBAssertion("F", "A", TC, 50, 0.1,
        8, Map.of(), 0, 0, 0, 0, 0);

    checkComputeDiscrepancy(cvr, auditedCvr, List.of(a1, a2), -1, Map.of(1L, -1),
        0, 0, 0, 0, 0);
  }

  /**
   * Test the re-auditing of a ballot where a prior discrepancy is recorded against the
   * associated CVR (a one vote understatement). The existing discrepancies associated with the
   * 'n' copies of the CVR in the sample are removed, and discrepancy computation repeated. In this
   * case, n = 1 and the new discrepancy is a one vote overstatement.
   */
  @Test()
  public void testNEBReauditBallot1(){
    log(LOGGER, String.format("testNEBReauditBallot1[%s]", RecordType.REAUDITED));
    resetMocks(A, blank, RecordType.UPLOADED, ConsensusValue.YES, RecordType.REAUDITED, TC);

    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a1 = createNEBAssertion("A", "B", TC, 50, 0.1,
        8, Map.of(1L, -1), 0, 1, 0,
        0, 0);

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
  public void testNEBReauditBallot2(){
    log(LOGGER, String.format("testNEBReauditBallot2[%s]", RecordType.REAUDITED));
    resetMocks(ABCD, BACD, RecordType.UPLOADED, ConsensusValue.YES, RecordType.REAUDITED, TC);

    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a1 = createNEBAssertion("B", "F", TC, 50, 0.1,
        8, Map.of(1L, 2), 0, 0, 2,
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
  public void testNEBReauditBallot3(){
    log(LOGGER, String.format("testNEBReauditBallot3[%s]", RecordType.REAUDITED));
    resetMocks(ABCD, ABCD, RecordType.UPLOADED, ConsensusValue.YES, RecordType.REAUDITED, TC);

    final int N = 5;
    CVRAuditInfo info = new CVRAuditInfo();
    info.setID(1L);

    Assertion a1 = createNEBAssertion("B", "F", TC, 50, 0.1,
        8, Map.of(1L, 0), 0, 0, 0,
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
   * Test the Assertion::computeInitialOptimisticSamplesToAudit() method for NEB assertions
   * with varying diluted margins.
   */
  @Test
  public void testNEBInitialOptimisticSampleSize(){
    Assertion a1 = createNEBAssertion("A", "B", TC, 100, 0.01,
        100, Map.of(), 0, 0, 0, 0, 0);

    assertEquals(a1.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 729);

    Assertion a2 = createNEBAssertion("B", "C", TC, 159, 0.0159,
        159, Map.of(), 0, 0, 0, 0, 0);

    assertEquals(a2.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 392);

    Assertion a3 = createNEBAssertion("D", "E", TC, 1, 0.00001,
        1000, Map.of(1L, -1, 2L, 0, 3L, 1), 1,
        1, 0, 0, 1);

    assertEquals(a3.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 728698);

    Assertion a4 = createNEBAssertion("F", "G", TC, 1235, 0.12345,
        50, Map.of(1L, -2), 0, 0, 0,
        1, 0);

    assertEquals(a4.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 51);

    Assertion a5 = createNEBAssertion("H", "I", TC, 3325, 0.33247528,
        17, Map.of(1L, 1, 2L, 2), 1, 0,
        1, 0, 0);

    assertEquals(a5.computeInitialOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 22);
  }

  /**
   * Test the Assertion::computeOptimisticSamplesToAudit() method for NEB assertions
   * with varying diluted margins and discrepancies.
   */
  @Test
  public void computeOptimisticSamplesToAudit(){
    Assertion a1 = createNEBAssertion("A", "B", TC, 100, 0.01,
        100, Map.of(), 0, 0, 0, 0, 0);

    assertEquals(a1.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 729);
    assertEquals(a1.optimisticSamplesToAudit.intValue(), 729);

    Assertion a2 = createNEBAssertion("B", "C", TC, 159, 0.0159,
        159, Map.of(), 1, 1, 1, 1, 1);

    assertEquals(a2.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 767);
    assertEquals(a2.optimisticSamplesToAudit.intValue(), 767);

    Assertion a3 = createNEBAssertion("D", "E", TC, 1, 0.00001,
        1000, Map.of(1L, -1, 2L, 0, 3L, 1), 1,
        1, 0, 0, 1);

    assertEquals(a3.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 783434);
    assertEquals(a3.optimisticSamplesToAudit.intValue(), 783434);

    Assertion a4 = createNEBAssertion("F", "G", TC, 1235, 0.12345,
        50, Map.of(1L, -2), 0, 0, 0,
        1, 0);

    assertEquals(a4.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 40);
    assertEquals(a4.optimisticSamplesToAudit.intValue(), 40);

    Assertion a5 = createNEBAssertion("H", "I", TC, 3325, 0.33247528,
        17, Map.of(1L, 1, 2L, 2), 1, 0,
        1, 0, 0);

    assertEquals(a5.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3).intValue(), 47);
    assertEquals(a5.optimisticSamplesToAudit.intValue(), 47);

    Assertion a6 = createNEBAssertion("B", "C", TC, 159, 0.0159,
        159, Map.of(1L, -1, 2L, -2, 3L, 0), 0, 1,
        0, 1, 1);

    assertEquals(a6.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 253);
    assertEquals(a6.optimisticSamplesToAudit.intValue(), 253);

    Assertion a7 = createNEBAssertion("B", "C", TC, 159, 0.0159,
        159, Map.of(1L, -1, 2L, -2), 0, 1,
        0, 1, 0);

    assertEquals(a7.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5).intValue(), 253);
    assertEquals(a7.optimisticSamplesToAudit.intValue(), 253);
  }

  /**
   * Test the Assertion::computeEstimatedSamplesToAudit() method for NEB assertions
   * with varying diluted margins, discrepancies, and current audited sample count.
   */
  @Test(dataProvider = "AuditSampleNumbers", dataProviderClass = AssertionTests.class)
  public void computeEstimatedSamplesToAudit(final int auditedSampleCount){
    Assertion a1 = createNEBAssertion("A", "B", TC, 100, 0.01,
        100, Map.of(), 0, 0, 0, 0, 0);

    a1.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3);
    assertEquals(a1.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), 729);
    assertEquals(a1.optimisticSamplesToAudit.intValue(), 729);
    assertEquals(a1.estimatedSamplesToAudit.intValue(), 729);

    Assertion a2 = createNEBAssertion("B", "C", TC, 159, 0.0159,
        159, Map.of(), 1, 1, 1, 1, 1);

    a2.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5);
    double scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (2.0/(double)auditedSampleCount);
    int sample = (int)ceil(767*scalingFactor);
    assertEquals(a2.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a2.optimisticSamplesToAudit.intValue(), 767);
    assertEquals(a2.estimatedSamplesToAudit.intValue(), sample);

    Assertion a3 = createNEBAssertion("D", "E", TC, 1, 0.00001,
        1000, Map.of(1L, -1, 2L, 0, 3L, 1), 1,
        1, 0, 0, 1);

    a3.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3);
    scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (1.0/(double)auditedSampleCount);
    sample = (int)ceil(783434*scalingFactor);
    assertEquals(a3.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a3.optimisticSamplesToAudit.intValue(), 783434);
    assertEquals(a3.estimatedSamplesToAudit.intValue(), sample);

    Assertion a4 = createNEBAssertion("F", "G", TC, 1235, 0.12345,
        50, Map.of(1L, -2, 2L, 1), 1, 0, 0,
        1, 0);

    a4.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5);
    scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (1.0/(double)auditedSampleCount);
    sample = (int)ceil(51*scalingFactor);
    assertEquals(a4.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a4.optimisticSamplesToAudit.intValue(), 51);
    assertEquals(a4.estimatedSamplesToAudit.intValue(), sample);

    Assertion a5 = createNEBAssertion("H", "I", TC, 3325, 0.33247528,
        17, Map.of(1L, 1, 2L, 2), 1, 0,
        1, 0, 0);

    a5.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3);
    scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (2.0/(double)auditedSampleCount);
    sample = (int)ceil(47*scalingFactor);
    assertEquals(a5.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a5.optimisticSamplesToAudit.intValue(), 47);
    assertEquals(a5.estimatedSamplesToAudit.intValue(), sample);

    Assertion a6 = createNEBAssertion("B", "C", TC, 159, 0.0159,
        159, Map.of(1L, -1, 2L, -2, 3L, 0), 0, 1,
        0, 1, 1);

    a6.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5);
    assertEquals(a6.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), 253);
    assertEquals(a6.optimisticSamplesToAudit.intValue(), 253);
    assertEquals(a6.estimatedSamplesToAudit.intValue(), 253);

    Assertion a7 = createNEBAssertion("B", "C", TC, 159, 0.0159,
        159, Map.of(1L, -1, 2L, -2), 0, 1,
        0, 1, 0);

    a7.computeOptimisticSamplesToAudit(AssertionTests.riskLimit5);
    assertEquals(a7.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), 253);
    assertEquals(a7.optimisticSamplesToAudit.intValue(), 253);
    assertEquals(a7.estimatedSamplesToAudit.intValue(), 253);

    Assertion a8 = createNEBAssertion("B", "C", TC, 159, 0.0159,
            159, Map.of(1L, -1, 2L, -2, 3L, 1, 4L, 1, 5L,
            2, 6L, 2, 7L, 1), 3, 1, 2,
            1, 0);

    scalingFactor = auditedSampleCount == 0 ? 1 : 1 + (5.0/(double)auditedSampleCount);
    sample = (int)ceil(1434*scalingFactor);

    a8.computeOptimisticSamplesToAudit(AssertionTests.riskLimit3);
    assertEquals(a8.computeEstimatedSamplesToAudit(auditedSampleCount).intValue(), sample);
    assertEquals(a8.optimisticSamplesToAudit.intValue(), 1434);
    assertEquals(a8.estimatedSamplesToAudit.intValue(), sample);
  }

  /**
   * Create an NEB assertion with the given parameters.
   * @param winner Winner of the assertion.
   * @param loser Loser of the assertion.
   * @param contestName Name of the contest to which the assertion belongs.
   * @param rawMargin Raw margin of the assertion.
   * @param dilutedMargin Diluted margin of the assertion.
   * @param difficulty Difficulty of the assertion.
   * @param cvrDiscrepancy Map between CVR ID and discrepancy type.
   * @param oneVoteOver Number of one vote overstatements to associate with the assertion.
   * @param oneVoteUnder Number of one vote understatements to associate with the assertion.
   * @param twoVoteOver Number of two vote overstatements to associate with the assertion.
   * @param twoVoteUnder Number of two vote understatements to associate with the assertion.
   * @param other Number of other discrepancies to associate with the assertion.
   * @return an NEB assertion with the given specification.
   */
  private static Assertion createNEBAssertion(final String winner, final String loser,
      final String contestName, final int rawMargin, final double dilutedMargin, final double difficulty,
      final Map<Long,Integer> cvrDiscrepancy, final int oneVoteOver, final int oneVoteUnder,
      final int twoVoteOver, final int twoVoteUnder, final int other){

    Assertion a = new NEBAssertion();
    AssertionTests.populateAssertion(a, winner, loser, contestName, List.of(), rawMargin,
        dilutedMargin, difficulty, cvrDiscrepancy, oneVoteOver, oneVoteUnder, twoVoteOver,
        twoVoteUnder, other);

    return a;
  }
}
