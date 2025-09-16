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

package au.org.democracydevelopers.corla.communication.responseFromRaire;


/**
 * The response when a ContestRequest is sent to raire's generate-assertions endpoint.
 * This class is identical to the record of the same name in raire-service. Used for
 * deserialization.
 * All four states of the two booleans are possible - for example, generation may succeed, but
 * receive a TIME_OUT_TRIMMING_ASSERTIONS warning, in which case retry will be true.
 */
public final class GenerateAssertionsResponse {
  public String contestName;
  public boolean succeeded;
  public boolean retry;


/**
 * All args constructor.
 * @param contestName The name of the contest.
 * @param succeeded   Whether assertion generation succeeded.
 * @param retry       Whether it is worth retrying assertion generation.
 */
  public GenerateAssertionsResponse(String contestName, boolean succeeded, boolean retry) {
    this.contestName = contestName;
    this.succeeded = succeeded;
    this.retry = retry;
  }
}