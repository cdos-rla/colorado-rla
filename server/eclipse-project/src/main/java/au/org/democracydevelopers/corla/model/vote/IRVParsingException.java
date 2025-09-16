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

package au.org.democracydevelopers.corla.model.vote;

/**
 * This exception communicates that there has been a failure to parse a CVR choice as a valid IRV
 * preference. For example, "Alice(2)" should parse correctly (it's a second preference for Alice),
 * but names with no preferences, negative preferences, unmatched parentheses and other syntactic
 * errors will throw this exception when IRVPreference tries to parse them.
 */
public class IRVParsingException extends Exception {
  // Parameterless Constructor
  public IRVParsingException() {}

  // Constructor that accepts a message
  public IRVParsingException(String s) {
    super(s);
  }
}
