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

/**
 * The possible types of contests that can be audited, including plurality and IRV.
 * Used for setting up the correct kinds of audits and doing the correct validity tests on choices.
 */
public enum ContestType {
  /**
   * Single- and multi-winner plurality elections. Voters select their favourite candidates(s), and
   * the winner is the one with the most votes.
   */
  PLURALITY,

  /**
   * Instant-runoff voting (IRV). Voters select candidates with ranks (preferences). The winner is
   * determined by a process of eliminating the candidate with the lowest tally and redistributing
   * their votes according to the next preference, until a candidate has a majority.
   */
  IRV
}
