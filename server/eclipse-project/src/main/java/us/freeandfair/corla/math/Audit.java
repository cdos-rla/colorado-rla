package us.freeandfair.corla.math;

import static java.math.MathContext.DECIMAL128;

// this is BigDecimal land
import java.math.BigDecimal;
import static java.math.BigDecimal.*;

import java.math.MathContext;
import java.math.RoundingMode;

import static ch.obermuhlner.math.big.BigDecimalMath.pow;
import static ch.obermuhlner.math.big.BigDecimalMath.log;

/**
 * A static class that should grow to contain audit related mathematical
 * functions that do not belong in models, controllers, or endpoint
 * bodies.
 */
public final class Audit {

  /**
   * Stark's gamma from the literature. As seen in a controller.
   */
  public static final BigDecimal GAMMA = valueOf(1.03905);

  private Audit() {
  }

  /**
   * μ = V / N
   * @param margin the smallest margin of winning, V votes.
   * @param ballotCount N, the number of ballots cast in a contest.
   * @return BigDecimal the diluted margin
   */
  public static BigDecimal dilutedMargin(final Integer margin,
                                         final Long ballotCount) {
    return dilutedMargin(valueOf(margin),
                         valueOf(ballotCount));
  }

  /**
   * μ = V / N
   * @param margin the smallest margin of winning, V votes.
   * @param ballotCount N, the number of ballots cast in a contest.
   * @return BigDecimal the diluted margin
   */
  public static BigDecimal dilutedMargin(final BigDecimal margin,
                                         final BigDecimal ballotCount) {
    if (margin == ZERO || ballotCount == ZERO) {
      return ZERO;
    } else {
      return margin.divide(ballotCount, DECIMAL128);
    }
  }

  /**
   * The "total error bound" defined in the literature.
   *
   * Usually represented as `U`, this can be found in equation (8) in Stark's
   * Super-Simple Simultaneous Single-Ballot Risk Limiting Audits paper.
   *
   * @param dilutedMargin the diluted margin of the contest
   * @param gamma the "error inflator" parameter from the literature
   *
   * @return the total error bound
   */
  public static BigDecimal totalErrorBound(final BigDecimal dilutedMargin,
                                           final BigDecimal gamma) {

    return gamma.multiply(valueOf(2), DECIMAL128)
        .divide(dilutedMargin, DECIMAL128);
  }

  /**
   * Computes the expected number of ballots to audit overall, assuming
   * zero over- and understatements.
   *
   * @param riskLimit as prescribed
   * @param dilutedMargin of the contest.
   *
   * @return the expected number of ballots remaining to audit.
   * This is the stopping sample size as defined in the literature:
   * https://www.stat.berkeley.edu/~stark/Preprints/gentle12.pdf
   */
  public static BigDecimal optimistic(final BigDecimal riskLimit,
                                      final BigDecimal dilutedMargin) {
    return optimistic(riskLimit, dilutedMargin, GAMMA,
                      0, 0, 0, 0);
  }

  /**
   * Computes the expected number of ballots to audit overall given the
   * specified numbers of over- and understatements.
   *
   * @param the_two_under The two-vote understatements.
   * @param the_one_under The one-vote understatements.
   * @param the_one_over The one-vote overstatements.
   * @param the_two_over The two-vote overstatements.
   *
   * @return the expected number of ballots remaining to audit.
   * This is the stopping sample size as defined in the literature:
   * https://www.stat.berkeley.edu/~stark/Preprints/gentle12.pdf
   */
  public static BigDecimal optimistic(final BigDecimal riskLimit,
                                      final BigDecimal dilutedMargin,
                                      final BigDecimal gamma,
                                      final int twoUnder,
                                      final int oneUnder,
                                      final int oneOver,
                                      final int twoOver) {

    if (dilutedMargin.compareTo(ZERO) == 0) { //hilarious
      // nothing to do here, no samples will need to be audited because the
      // contest is uncontested
      return ZERO;
    }

    final BigDecimal result;
    final BigDecimal invgamma = ONE.divide(gamma, DECIMAL128);
    final BigDecimal twogamma = valueOf(2).multiply(gamma);
    final BigDecimal invtwogamma =
      ONE.divide(twogamma, DECIMAL128);
    final BigDecimal two_under_bd = valueOf(twoUnder);
    final BigDecimal one_under_bd = valueOf(oneUnder);
    final BigDecimal one_over_bd = valueOf(oneOver);
    final BigDecimal two_over_bd = valueOf(twoOver);

    final BigDecimal over_under_sum =
      two_under_bd.add(one_under_bd).add(one_over_bd).add(two_over_bd);
    final BigDecimal two_under =
      two_under_bd.multiply(log(ONE.add(invgamma),
                                               DECIMAL128));
    final BigDecimal one_under =
      one_under_bd.multiply(log(ONE.add(invtwogamma),
                                               DECIMAL128));
    final BigDecimal one_over =
      one_over_bd.multiply(log(ONE.subtract(invtwogamma),
                                              DECIMAL128));
    final BigDecimal two_over =
      two_over_bd.multiply(log(ONE.subtract(invgamma),
                                              DECIMAL128));
    final BigDecimal numerator =
      twogamma.negate().
      multiply(log(riskLimit, DECIMAL128).
               add(two_under.add(one_under).add(one_over).add(two_over)));
      final BigDecimal ceil =
        numerator.divide(dilutedMargin, DECIMAL128).setScale(0, RoundingMode.CEILING);
      result = ceil.max(over_under_sum);

    return result;
  }

  /**
   * Conservative approximation of the Kaplan-Markov P-value.
   *
   * The audit can stop when the P-value drops to or below the defined risk
   * limit. The output of this method will never estimate a P-value that is too
   * low, it will always be at or above the (more complicated to calculate)
   * Kaplan-Markov P-value, but usually not by much. Therefore this method is
   * safe to use as the stopping condition for the audit, even though it may be
   * possible to stop the audit "a ballot or two" earlier if calculated using
   * the Kaplan-Markov method.
   *
   * Implements equation (10) of Philip B. Stark's paper, Super-Simple
   * Simultaneous Single-Ballot Risk-Limiting Audits.
   *
   * Translated from Stark's implementation under the heading "A simple
   * approximation" at the following URL:
   *
   * https://github.com/pbstark/S157F17/blob/master/audit.ipynb
   *
   * NOTE: The ordering of the under and overstatement parameters is different
   * from its cousin method `optimistic`.
   *
   * @param auditedBallots the number of ballots audited so far
   * @param dilutedMargin the diluted margin of the contest
   * @param gamma the "error inflator" parameter from the literature
   * @param twoUnder the number of two-vote understatements
   * @param oneUnder the number of one-vote understatements
   * @param oneOver the number of one-vote overstatements
   * @param twoOver the number of two-vote overstatements
   *
   * @return approximation of the Kaplan-Markov P-value
   */
  public static BigDecimal pValueApproximation(final int auditedBallots,
                                               final BigDecimal dilutedMargin,
                                               final BigDecimal gamma,
                                               final int oneUnder,
                                               final int twoUnder,
                                               final int oneOver,
                                               final int twoOver) {
    final BigDecimal totalErrorBound = totalErrorBound(dilutedMargin, gamma);

    return ONE.min(
        pow(
            ONE.subtract(
                ONE.divide(totalErrorBound, DECIMAL128)
            ),
            auditedBallots,
            DECIMAL128
        )
        .multiply(
            pow(
                ONE.subtract(
                    ONE.divide(
                        gamma.multiply(valueOf(2), DECIMAL128),
                        DECIMAL128
                    )
                ),
                -1 * oneOver,
                DECIMAL128
            ),
            DECIMAL128
        )
        .multiply(
            pow(
                ONE.subtract(
                    ONE.divide(gamma, DECIMAL128)
                ),
                -1 * twoOver,
                DECIMAL128
            ),
            DECIMAL128
        )
        .multiply(
            pow(
                ONE.add(
                    ONE.divide(
                        gamma.multiply(valueOf(2), DECIMAL128),
                        DECIMAL128
                    )
                ),
                -1 * oneUnder,
                DECIMAL128
            ),
            DECIMAL128
        )
        .multiply(
            pow(
                ONE.add(
                    ONE.divide(gamma, DECIMAL128)
                ),
                -1 * twoUnder,
                DECIMAL128
            ),
            DECIMAL128
        )
    );
  }
}
