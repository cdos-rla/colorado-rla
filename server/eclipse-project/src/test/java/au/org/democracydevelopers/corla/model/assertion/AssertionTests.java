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

import static java.lang.Math.ceil;
import static java.lang.Math.log;
import static java.lang.Math.max;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

import au.org.democracydevelopers.corla.util.TestClassWithDatabase;
import au.org.democracydevelopers.corla.util.testUtils;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import us.freeandfair.corla.model.CVRContestInfo;
import us.freeandfair.corla.model.CVRContestInfo.ConsensusValue;
import us.freeandfair.corla.model.CastVoteRecord;
import us.freeandfair.corla.model.CastVoteRecord.RecordType;

/**
 * This class does not contain any tests, but provides utilities for use by the
 * NEB/NEN Assertion test classes. Although much of the assertion-related functionality is
 * present in the abstract Assertion class, we have chosen to test this functionality
 * twice -- once in the context where the assertion is an NEB and then in the context
 * where the assertion is an NEN. Although, at present, it is the same code that is
 * being tested in both cases, it is possible that future development may alter the
 * assertion subclasses in potentially unforeseen ways. This extra level of testing will
 * reduce the likelihood of future programming errors if this eventuates.
 */
public class AssertionTests extends TestClassWithDatabase {

  /**
   * Establish a mocked CVRContestInfo for use in testing Assertion scoring.
   */
  @Mock
  protected CVRContestInfo cvrInfo;

  /**
   * Mocked CVRContestInfo representing the vote "A", "B", "C", "D".
   */
  @Mock
  protected CVRContestInfo ABCD;

  /**
   * Mocked CVRContestInfo representing the vote "B", "A", "C", "D".
   */
  @Mock
  protected CVRContestInfo BACD;

  /**
   * Mocked CVRContestInfo representing the vote "D", "A", "B", "C".
   */
  @Mock
  protected CVRContestInfo DABC;

  /**
   * Mocked CVRContestInfo representing the vote "B", "A".
   */
  @Mock
  protected CVRContestInfo BA;

  /**
   * Mocked CVRContestInfo representing a blank vote.
   */
  @Mock
  protected CVRContestInfo blank;

  /**
   * Mocked CVRContestInfo representing the vote "A".
   */
  @Mock
  protected CVRContestInfo A;

  /**
   * Mocked CVRContestInfo representing the vote "B".
   */
  @Mock
  protected CVRContestInfo B;

  /**
   * Mocked CastVoteRecord to represent a CVR.
   */
  @Mock
  protected CastVoteRecord cvr;

  /**
   * Mocked CastVoteRecord to represent an audited CVR.
   */
  @Mock
  protected CastVoteRecord auditedCvr;

  /**
   * Constant representing a test contest name.
   */
  public static final String TC = "Test Contest";

  /**
   * A 3% risk limit, as a BigDecimal.
   */
  public static final BigDecimal riskLimit3 = BigDecimal.valueOf(0.03);

  /**
   * A 5% risk limit, as a BigDecimal.
   */
  public static final BigDecimal riskLimit5 = BigDecimal.valueOf(0.05);

  /**
   * A small raw margin of 10 votes.
   */
  public static final Integer smallRawMargin = 10;

  /**
   * A small diluted margin of 0.001.
   */
  public static final BigDecimal smallMargin = BigDecimal.valueOf(0.001);

  /**
   * A high assertion difficulty value of 100.
   */
  public static final BigDecimal highDifficulty = BigDecimal.valueOf(100);

  /**
   * A large raw margin of 10000 votes.
   */
  public static final Integer largeRawMargin = 10000;

  /**
   * A large diluted margin of 0.55.
   */
  public static final BigDecimal largeMargin = BigDecimal.valueOf(0.55);

  /**
   * A small assertion difficulty of 1.
   */
  public static final BigDecimal smallDifficulty = BigDecimal.valueOf(1);

  /**
   * List of candidates called "W", "L", and "O".
   */
  public static final List<String> wlo = List.of("W", "L", "O");

  /**
   * Start the test container and establish persistence properties before the first test.
   */
  @BeforeClass
  public void beforeAllThisClass() {
    runSQLSetupScript("SQL/co-counties.sql");
    runSQLSetupScript("SQL/simple-assertions.sql");
  }

  @BeforeClass
  public void initMocks() {
    MockitoAnnotations.openMocks(this);

    when(ABCD.choices()).thenReturn(List.of("A", "B", "C", "D"));
    when(BACD.choices()).thenReturn(List.of("B", "A", "C", "D"));
    when(DABC.choices()).thenReturn(List.of("D", "A", "B", "C"));
    when(BA.choices()).thenReturn(List.of("B", "A"));
    when(blank.choices()).thenReturn(List.of());
    when(A.choices()).thenReturn(List.of("A"));
    when(B.choices()).thenReturn(List.of("B"));

    when(cvr.id()).thenReturn(1L);
  }

  /**
   * Reset the CVR and audited CVR mock objects with the given parameters.
   * @param cvrInfo CVRContestInfo for the CVR.
   * @param acvrInfo CVRContestInfo for the audited ballot.
   * @param cvrRecType Record type for the CVR.
   * @param acvrConsensus Consensys value for the audited ballot.
   * @param acvrRecType Record type for the audited ballot.
   * @param contestName Name of the contest whose votes we are encoding.
   */
  protected void resetMocks(final CVRContestInfo cvrInfo, CVRContestInfo acvrInfo,
      final RecordType cvrRecType, final ConsensusValue acvrConsensus, final RecordType acvrRecType,
      final String contestName){
    when(cvr.contestInfoForContestResult(contestName)).thenReturn(Optional.of(cvrInfo));
    when(auditedCvr.contestInfoForContestResult(contestName)).thenReturn(Optional.of(acvrInfo));

    when(acvrInfo.consensus()).thenReturn(acvrConsensus);
    when(cvr.recordType()).thenReturn(cvrRecType);
    when(auditedCvr.recordType()).thenReturn(acvrRecType);
  }

  /**
   * A parameter set consisting of two audited ballot record types (AUDITOR_ENTERED and REAUDITED).
   * @return An array of parameter arrays.
   */
  @DataProvider(name = "AuditedRecordTypes")
  public static Object[][] auditedRecordTypes(){
    return new Object[][]{
        {RecordType.AUDITOR_ENTERED},
        {RecordType.REAUDITED}
    };
  }

  /**
   * A parameter set consisting of a series of varying current audited sample counts.
   */
  @DataProvider(name = "AuditSampleNumbers")
  public static Object[][] auditSampleNumbers(){
    return new Object[][]{
        {0}, {1}, {2}, {10}, {50}, {100}, {500}, {1500}, {5000}
    };
  }

  /**
   * A parameter set consisting of all valid discrepancy types.
   */
  @DataProvider(name = "DiscrepancyTypes")
  public static Object[][] discrepancyTypes(){
    return new Object[][]{{-2}, {-1}, {0}, {1}, {2}};
  }

  /**
   * Returns a set of varying parameters to supply when constructing assertions to test
   * sample size computations. To test sample size calculation, we consider the following dimensions:
   * -- Risk limit (3% or 5%)
   * -- Assertions with large/small diluted margins.
   * -- Assertions with varying combinations of discrepancies:
   *    -- No discrepancies;
   *    -- 1 of each type of discrepancy, rest 0.
   *    -- 2 of each type of discrepancy, rest 0.
   *    -- Combinations of discrepancies including overstatements. We could have lots of
   *       combinations here, but given that overstatements are of the highest concern, we
   *       consider 1 one vote overstatement plus 1 of each other type (alternating) and
   *       1 two vote overstatement plus 1 of each other type (alternating).
   *    -- Combinations of discrepancies including no overstatements (1 of each type).
   * @return An array of parameter arrays, each containing one combination of assertion attributes
   * that we want to test sample size computation on.
   */
  @DataProvider(name = "SampleParameters")
  public static Object[][] sampleParameters(){
    // Parameter list: risk limit, raw margin, diluted margin, difficulty, cvr discrepancy map,
    // one vote overs, one vote unders, two vote overs, two vote unders, others.
    return new Object[][]{
        // Small margin cases
        // No discrepancies
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(), 0, 0, 0, 0, 0},
        // one 1 vote understatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -1), 0, 1, 0, 0, 0},
        // one 1 vote overstatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        // one 2 vote understatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -2), 0, 0, 0, 1, 0},
        // one 2 vote overstatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        // one other discrepancy
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 0), 0, 0, 0, 0, 1},

        // two 1 vote understatements
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -1, 2L, -1), 0, 2, 0, 0, 0},
        // two 1 vote overstatements
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, 1), 2, 0, 0, 0, 0},
        // two 2 vote understatements
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -2, 2L, -2), 0, 0, 0, 2, 0},
        // two 2 vote overstatements
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2, 2L, 2), 0, 0, 2, 0, 0},
        // two other discrepancies
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 0, 2L, 0), 0, 0, 0, 0, 2},

        // one 1 vote overstatement, one 1 vote understatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, -1), 1, 1, 0, 0, 0},
        // one 1 vote overstatement, one 2 vote overstatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, 2), 1, 0, 1, 0, 0},
        // one 1 vote overstatement, one 2 vote understatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, -2), 1, 0, 0, 1, 0},
        // one 1 vote overstatement, one other discrepancy
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, 0), 1, 0, 0, 0, 1},

        // one 2 vote overstatement, one 1 vote understatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2, 2L, -1), 0, 1, 1, 0, 0},
        // one 2 vote overstatement, one 2 vote understatement
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2, 2L, -2), 0, 0, 1, 1, 0},
        // one 2 vote overstatement, one other discrepancy
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2, 2L, 0), 0, 0, 1, 0, 1},

        // one discrepancy of each type
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, -1, 3L,
            2, 4L, -2, 5L, 0), 1, 1, 1, 1, 1},

        // one discrepancy of each type (excluding overstatements)
        {riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(2L, -1, 4L, -2,
            5L, 0), 0, 1, 0, 1, 1},

        // (mostly) Large margin cases
        // No discrepancies
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(), 0, 0, 0, 0, 0},
        // one 1 vote understatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, -1), 0, 1, 0, 0, 0},
        // one 1 vote overstatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        // one 2 vote understatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, -2), 0, 0, 0, 1, 0},
        // one 2 vote overstatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        // one other discrepancy
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 0), 0, 0, 0, 0, 1},

        // two 1 vote understatements
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, -1, 2L, -1), 0, 2, 0, 0, 0},
        // two 1 vote overstatements
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, 1), 2, 0, 0, 0, 0},
        // two 2 vote understatements
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, -2, 2L, -2), 0, 0, 0, 2, 0},
        // two 2 vote overstatements
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 2, 2L, 2), 0, 0, 2, 0, 0},
        // two other discrepancies
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 0, 2L, 0), 0, 0, 0, 0, 2},

        // one 1 vote overstatement, one 1 vote understatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, -1), 1, 1, 0, 0, 0},
        // one 1 vote overstatement, one 2 vote overstatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, 2), 1, 0, 1, 0, 0},
        // one 1 vote overstatement, one 2 vote understatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, -2), 1, 0, 0, 1, 0},
        // one 1 vote overstatement, one other discrepancy
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, 0), 1, 0, 0, 0, 1},

        // one 2 vote overstatement, one 1 vote understatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 2, 2L, -1), 0, 1, 1, 0, 0},
        // one 2 vote overstatement, one 2 vote understatement
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 2, 2L, -2), 0, 0, 1, 1, 0},
        // one 2 vote overstatement, one other discrepancy
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 2, 2L, 0), 0, 0, 1, 0, 1},

        // one discrepancy of each type
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, -1, 3L,
            2, 4L, -2, 5L, 0), 1, 1, 1, 1, 1},

        // one discrepancy of each type (excluding overstatements)
        {riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(2L, -1, 4L, -2,
            5L, 0), 0, 1, 0, 1, 1},

        // 5% risk limit (selected instances from the above).
        // No discrepancies
        {riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(), 0, 0, 0, 0, 0},
        // one 1 vote understatement
        {riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -1), 0, 1, 0, 0, 0},
        // one 1 vote overstatement
        {riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        // one 1 vote overstatement
        {riskLimit5, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        // one 2 vote understatement
        {riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -2), 0, 0, 0, 1, 0},
        // one 2 vote overstatement
        {riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        // one 2 vote overstatement
        {riskLimit5, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        // one other discrepancy
        {riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 0), 0, 0, 0, 0, 1},
        // one discrepancy of each type
        {riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, -1, 3L,
            2, 4L, -2, 5L, 0), 1, 1, 1, 1, 1},
        {riskLimit5, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, -1, 3L,
            2, 4L, -2, 5L, 0), 1, 1, 1, 1, 1},
        // one discrepancy of each type (excluding overstatements)
        {riskLimit5, largeRawMargin, largeMargin, smallDifficulty, Map.of(2L, -1, 4L, -2,
            5L, 0), 0, 1, 0, 1, 1},
    };
  }

  /**
   * Returns a set of varying parameters to supply when constructing assertions to test
   * estimated sample size computations in the context when varying number of ballots have
   * been sampled thus far. We consider the same dimensions as the "SampleParameters" data set,
   * with the addition of the number of ballots audited thus far.
   *
   * @return An array of parameter arrays, each containing one combination of assertion attributes
   * that we want to test estimated sample size computation on.
   */
  @DataProvider(name = "ParametersVaryingSamples")
  public static Object[][] parametersVaryingSamples(){
    // Parameter list: audited samples thus far, risk limit, raw margin, diluted margin, difficulty,
    // cvr discrepancy map, one vote overs, one vote unders, two vote overs, two vote unders, others.
    return new Object[][]{
        // No discrepancies
        {0, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(), 0, 0, 0, 0, 0},
        {0, riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(), 0, 0, 0, 0, 0},
        {0, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(), 0, 0, 0, 0, 0},
        {0, riskLimit5, largeRawMargin, largeMargin, smallDifficulty, Map.of(), 0, 0, 0, 0, 0},

        // No discrepancies
        {1, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(), 0, 0, 0, 0, 0},
        // one 1 vote understatement
        {1, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -1), 0, 1, 0, 0, 0},
        // one 1 vote overstatement
        {1, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        {1, riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        {1, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        // one 2 vote understatement
        {1, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -2), 0, 0, 0, 1, 0},
        // one 2 vote overstatement
        {1, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        {1, riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        {1, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        // one other discrepancy
        {1, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 0), 0, 0, 0, 0, 1},

        // No discrepancies
        {101, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(), 0, 0, 0, 0, 0},
        // one 1 vote understatement
        {101, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -1), 0, 1, 0, 0, 0},
        // one 1 vote overstatement
        {101, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        {101, riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        {101, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1), 1, 0, 0, 0, 0},
        // one 2 vote understatement
        {101, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, -2), 0, 0, 0, 1, 0},
        // one 2 vote overstatement
        {101, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        {101, riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        {101, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 2), 0, 0, 1, 0, 0},
        // one other discrepancy
        {101, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 0), 0, 0, 0, 0, 1},

        // one 1 vote overstatement, one 1 vote understatement
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, -1), 1, 1, 0, 0, 0},
        // one 1 vote overstatement, one 2 vote overstatement
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, 2), 1, 0, 1, 0, 0},
        {37,riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, 2), 1, 0, 1, 0, 0},
        {37, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, 2), 1, 0, 1, 0, 0},
        // Very large sample count
        {10034, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, 2), 1, 0, 1, 0, 0},
        {10034,riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, 2), 1, 0, 1, 0, 0},
        {10034, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, 2), 1, 0, 1, 0, 0},
        // one 1 vote overstatement, one 2 vote understatement
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, -2), 1, 0, 0, 1, 0},
        // one 1 vote overstatement, one other discrepancy
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, 0), 1, 0, 0, 0, 1},

        // one 2 vote overstatement, one 1 vote understatement
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2, 2L, -1), 0, 1, 1, 0, 0},
        // one 2 vote overstatement, one 2 vote understatement
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2, 2L, -2), 0, 0, 1, 1, 0},
        // one 2 vote overstatement, one other discrepancy
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 2, 2L, 0), 0, 0, 1, 0, 1},

        // one discrepancy of each type
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, -1, 3L,
            2, 4L, -2, 5L, 0), 1, 1, 1, 1, 1},
        {37,riskLimit5, smallRawMargin, smallMargin, highDifficulty, Map.of(1L, 1, 2L, -1, 3L,
            2, 4L, -2, 5L, 0), 1, 1, 1, 1, 1},
        {37, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, -1, 3L,
            2, 4L, -2, 5L, 0), 1, 1, 1, 1, 1},
        {10061, riskLimit3, largeRawMargin, largeMargin, smallDifficulty, Map.of(1L, 1, 2L, -1, 3L,
            2, 4L, -2, 5L, 0), 1, 1, 1, 1, 1},

        // one discrepancy of each type (excluding overstatements)
        {37, riskLimit3, smallRawMargin, smallMargin, highDifficulty, Map.of(2L, -1, 4L, -2,
            5L, 0), 0, 1, 0, 1, 1},
    };
  }

  /**
   * Populate the attributes of the given assertion according to the given parameters.
   * @param a Assertion to be configured.
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
   */
  public static void populateAssertion(Assertion a, final String winner, final String loser,
      final String contestName, final List<String> continuing, final int rawMargin,
      final double dilutedMargin, final double difficulty, final Map<Long,Integer> cvrDiscrepancy,
      final int oneVoteOver, final int oneVoteUnder, final int twoVoteOver,
      final int twoVoteUnder, final int other){

    a.winner = winner;
    a.loser  = loser;
    a.contestName = contestName;
    a.assumedContinuing = continuing;
    a.margin = rawMargin;
    a.dilutedMargin = BigDecimal.valueOf(dilutedMargin);
    a.difficulty = difficulty;
    a.cvrDiscrepancy.putAll(cvrDiscrepancy);
    a.oneVoteOverCount = oneVoteOver;
    a.oneVoteUnderCount = oneVoteUnder;
    a.twoVoteOverCount = twoVoteOver;
    a.twoVoteUnderCount = twoVoteUnder;
    a.otherCount = other;
  }

  /**
   * Compute the optimistic number of ballots to sample for an assertion with the given
   * parameters, and the given risk limit. This function implements the sample size
   * estimation as described in Section 2.2.1 of the Colorado IRV RLA Implementation Plan.
   * @param riskLimit Risk limit of the audit.
   * @param dilutedMargin Diluted margin of the assertion.
   * @param o1 Number of one vote overstatements.
   * @param o2 Number of two vote overstatements.
   * @param u1 Number of one vote understatements.
   * @param u2 Number of two vote understatements.
   * @param gamma Gamma parameter.
   * @return the optimistic number of ballots to sample when auditing the assertion with
   * the given characteristics.
   */
  public static int optimistic(final BigDecimal riskLimit, final double dilutedMargin, final int o1,
      final int o2, final int u1, final int u2, final BigDecimal gamma){

    final double dgamma = gamma.doubleValue();
    final double oneOnOneGamma = 1/dgamma;
    final double oneOnTwoGamma = 1/(2*dgamma);
    final double factor = (-2 * dgamma)/dilutedMargin;

    final int totalDiscrepancies = o1 + o2 + u1 + u2;

    return max(totalDiscrepancies, (int)ceil(factor * (log(riskLimit.doubleValue()) +
        u1 * log(1 + oneOnTwoGamma) + u2 * log(1 + oneOnOneGamma) + o1 * log(1 - oneOnTwoGamma) +
        o2 * log(1 - oneOnOneGamma))));
  }

  /**
   * Compute the estimated number of ballots to sample for an assertion with the given
   * parameters, and the given risk limit, under the assumption that overstatements will
   * continue at the current rate. This function implements the sample size
   * estimation as described in Section 2.2.1 of the Colorado IRV RLA Implementation Plan.
   * @param riskLimit Risk limit of the audit.
   * @param dilutedMargin Diluted margin of the assertion.
   * @param o1 Number of one vote overstatements.
   * @param o2 Number of two vote overstatements.
   * @param u1 Number of one vote understatements.
   * @param u2 Number of two vote understatements.
   * @param gamma Gamma parameter.
   * @param auditedSamples Number of ballots audited thus far.
   * @return the estimated number of ballots to sample when auditing the assertion with
   * the given characteristics, under the assumption that overstatements will continue at
   * the current rate,
   */
  public static int estimated(final BigDecimal riskLimit, final double dilutedMargin, final int o1,
      final int o2, final int u1, final int u2, final BigDecimal gamma, final int auditedSamples){

    final int optimistic = optimistic(riskLimit, dilutedMargin, o1, o2, u1, u2, gamma);
    if(auditedSamples == 0){
      return optimistic;
    }
    return (int)ceil(optimistic * (1 + (o1+o2)/(double)auditedSamples));
  }


  /**
   * Returns true if all discrepancy counts match those given as parameters.
   * @param a Given assertion.
   * @param o1 Number of expected one vote overstatements.
   * @param o2 Number of expected two vote overstatements.
   * @param u1 Number of expected one vote understatements.
   * @param u2 Number of expected two vote understatements.
   * @param o Number of other discrepancies.
   * @return True if the assertion has the specified discrepancy counts
   */
  public static boolean countsEqual(final Assertion a, final int o1, final int o2, final int u1,
      final int u2, final int o){
    return a.otherCount == o && a.oneVoteOverCount == o1 && a.oneVoteUnderCount == u1 &&
        a.twoVoteOverCount == o2 && a.twoVoteUnderCount == u2;
  }

  /**
   * Check that the given CVR and audited CVR pair represent a discrepancy of the given type
   * for the given list of assertions. For each assertion, the discrepancy counts should equal
   * those given as parameters, and their cvrDiscrepancy map should match the given
   * parameter, after computeDiscrepancy() is called on it.
   * @param cvr CVR being audited.
   * @param auditedCvr Audited ballot matching the CVR.
   * @param assertions List of assertions to compute discrepancies for.
   * @param dType Type of discrepancy that should arise.
   * @param cvrDiscrepancies Expected cvrDiscrepancy map after discrepancy computation.
   * @param o1 Expected one vote overstatement count after discrepancy computation.
   * @param o2 Expected two vote overstatement count after discrepancy computation.
   * @param u1 Expected one vote understatement count after discrepancy computation.
   * @param u2 Expected two vote understatement count after discrepancy computation.
   * @param o Expected "other" discrepancy count after discrepancy computation.
   */
  public static void checkComputeDiscrepancy(final CastVoteRecord cvr, final CastVoteRecord auditedCvr,
      List<Assertion> assertions, final int dType, final Map<Long,Integer> cvrDiscrepancies,
      final int o1, final int o2, final int u1, final int u2, final int o)
  {
    for(Assertion a : assertions){
      OptionalInt d = a.computeDiscrepancy(cvr, auditedCvr);
      assert(d.isPresent() && d.getAsInt() == dType);
      assert(countsEqual(a, o1, o2, u1, u2, o));
      assertEquals(cvrDiscrepancies, a.cvrDiscrepancy);
    }
  }


  /**
   * Checks that the discrepancy counts in the given assertion 'a' are equal to the given
   * parameters.
   * @param a Assertion to check.
   * @param oneOver Expected number of one vote overstatements.
   * @param oneUnder Expected number of one vote understatements.
   * @param twoOver Expected number of two vote overstatements.
   * @param twoUnder Expected number of two vote understatements.
   * @param other Expected number of other discrepancies.
   * @param cvrDiscrepancy Expected internal map of CVR ID to discrepancy type.
   */
  public static void checkCountsDiscrepancyMap(final Assertion a, final int oneOver,
      final int oneUnder, final int twoOver, final int twoUnder, final int other,
      final Map<Long,Integer> cvrDiscrepancy){

    assertEquals(a.oneVoteOverCount.intValue(), oneOver);
    assertEquals(a.oneVoteUnderCount.intValue(), oneUnder);
    assertEquals(a.twoVoteOverCount.intValue(), twoOver);
    assertEquals(a.twoVoteUnderCount.intValue(), twoUnder);
    assertEquals(a.otherCount.intValue(), other);

    assertEquals(a.cvrDiscrepancy, cvrDiscrepancy);
  }

  /**
   * Checks whether an assertion's riskMeasurement() method calculates the correct risks for
   * varying sample numbers (note that discrepancies may be present and stored in the given
   * Assertion).
   * @param a Assertion whose riskMeasurement() method we are testing.
   * @param risks Map of expected sample counts to risk values.
   */
  public static void checkRiskMeasurement(Assertion a, final Map<Integer,Double> risks){
    for(Map.Entry<Integer,Double> entry : risks.entrySet()){
      final BigDecimal r = a.riskMeasurement(entry.getKey());
      assertEquals(testUtils.doubleComparator.compare(entry.getValue(), r.doubleValue()), 0);
    }
  }
}
