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

package au.org.democracydevelopers.corla.util;

import java.util.Comparator;

/**
 * A double comparison with EPS precision.
 * Returns:
 * 0 when x and y are within EPS,
 * -1 when x < y (by more than EPS),
 * +1 when x > y (by more than EPS).
 */
public class DoubleComparator implements Comparator<Double> {

  /**
   * Error allowed when comparing doubles.
   */
  public static final double EPS = 0.0000001;

  public int compare(Double x, Double y) {
    if(Math.abs(x - y) < EPS) {
      return 0;
    } else if (x < y) {
      return -1;
    }
    return 1;
  }
}