/*
Copyright 2024 Democracy Developers

The Raire Service is designed to connect colorado-rla and its associated database to
the raire assertion generation engine (https://github.com/DemocracyDevelopers/raire-java).

This file is part of raire-service.

raire-service is free software: you can redistribute it and/or modify it under the terms
of the GNU Affero General Public License as published by the Free Software Foundation, either
version 3 of the License, or (at your option) any later version.

raire-service is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
See the GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License along with
raire-service. If not, see <https://www.gnu.org/licenses/>.
*/

package au.org.democracydevelopers.corla.communication.responseFromRaire;

/**
 * Errors sent back from the raire service via http headers. The fields exactly match the
 * corresponding ones in the raire service. The main item is an enum listing all possible errors,
 * allowing us to distinguish those the user can do something about from those they can't.
 */
public class RaireServiceErrors {

/**
 * The string "error_code" - used for retrieving it from json etc. Must exactly match the string of
 * the same name in raire-service.
 */
public static String ERROR_CODE_KEY = "error_code";

/**
 * Error codes describing what went wrong, returned via http headers from raire. This enum exactly
 * matches the enum of the same name in raire-service.
 */
public enum RaireErrorCodes {
   // Errors that the user can do something about.

    /**
     * Tied winners - the contest is a tie and therefore cannot be audited.
     */
    TIED_WINNERS,

    /**
     * The total number of auditable ballots for a contest is less than the number of CVRs in
     * the database that contain the contest.
     */
    INVALID_TOTAL_AUDITABLE_BALLOTS,

    /**
     * Time out checking winners - can happen if the contest is tied, or if it is complicated and
     * can't be distinguished from a tie.
     */
    TIMEOUT_CHECKING_WINNER,

    /**
     * Raire timed out trying to find assertions. It may succeed if given more time.
     */
    TIMEOUT_FINDING_ASSERTIONS,

    /**
     * Raire timed out trimming assertions. The assertions have been generated, but the audit could
     * be more efficient if the trimming step is re-run with more time allowed.
     */
    TIMEOUT_TRIMMING_ASSERTIONS,

    /**
     * RAIRE couldn't rule out some alternative winner.
     */
    COULD_NOT_RULE_OUT_ALTERNATIVE,

    /**
     * The list of candidate names in the request didn't match the database.
     */
    WRONG_CANDIDATE_NAMES,

    /**
     * The user has requested to retrieve assertions for a contest for which no assertions have
     * been generated.
     */
    NO_ASSERTIONS_PRESENT,

    /**
     * The user has requested to generate assertions for a contest for which no votes are present.
     */
    NO_VOTES_PRESENT,

    // Internal errors (that the user can do nothing about)

    /**
     * A catch-all for various kinds of errors that indicate a programming error: invalid
     * input errors such as InvalidNumberOfCandidates, InvalidTimeout, InvalidCandidateNumber -
     * these should all be caught before being sent to raire-java. Also database errors.
     * These are errors that the user can't do anything about.
     */
    INTERNAL_ERROR,
}
}
