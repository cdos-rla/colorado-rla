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

package au.org.democracydevelopers.corla.communication.requestToRaire;

import au.org.democracydevelopers.corla.util.testUtils;
import com.google.gson.Gson;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertTrue;

/**
 * A very basic test class that constructs a ContestRequest and checks that it serializes correctly.
 */
public class GenerateAssertionsRequestTests {

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(GenerateAssertionsRequestTests.class);

  private static final Gson gson = new Gson();

  /**
   * Basic test for correct serialization of reasonable values.
   */
  @Test
  public void testSerialization() {
    testUtils.log(LOGGER, "testSerialization");
    GenerateAssertionsRequest request = new GenerateAssertionsRequest("Test Contest", 50000,
        5, List.of("Alice", "Bob", "Chuan", "Diego"));

    String json = gson.toJson(request);

    assertTrue(json.contains("\"contestName\":\"Test Contest\""));
    assertTrue(json.contains("\"totalAuditableBallots\":50000"));
    assertTrue(json.contains("\"timeLimitSeconds\":5"));
    assertTrue(json.contains("\"candidates\":[\"Alice\",\"Bob\",\"Chuan\",\"Diego\"]"));
  }

}
