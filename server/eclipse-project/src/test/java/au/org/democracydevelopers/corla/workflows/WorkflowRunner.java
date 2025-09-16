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

package au.org.democracydevelopers.corla.workflows;

import static org.testng.Assert.assertTrue;

import io.restassured.RestAssured;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import us.freeandfair.corla.persistence.Persistence;

import javax.transaction.Transactional;

/**
 * This workflow runner is designed to execute all JSON workflows present in a specified
 * directory (defined in the "pathToInstances" member). These JSON workflows define a complete
 * audit to undertake, along with expected results. The workflow runner will execute all stages of
 * the audit: CVR and manifest uploads; defining the audit; selecting contests to target;
 * uploading audited ballots; reauditing ballots; and executing rounds until there are no further
 * ballots to sample. The workflow ends when the audit ends. Reporting is not tested in these workflows.
 */
@Test(enabled = true)
public class WorkflowRunner extends Workflow {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(WorkflowRunner.class);

  /**
   * Directory in which instance JSON files are stored.
   */
  private static final String pathToInstances = "src/test/resources/workflows/instances";

  @BeforeClass
  public void setup() {
    runMain(config, "WorkflowRunner");
    Persistence.beginTransaction();
    runSQLSetupScript("SQL/co-admins.sql");

    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8888;
  }

  /**
   * Returns a list of parameter lists to supply to the runWorkflow test method in this class.
   * Each parameter list contains one element -- a path to the workflow JSON instance to run.
   *
   * @return A list of test parameter lists as a 2D array of objects.
   */
  @DataProvider(name = "workflow-provider")
  public Object[][] supplyWorkflowPaths() {
    final String prefix = "[supplyWorkflowPaths]";
    List<Path> pathList;

    try (Stream<Path> stream = Files.walk(Paths.get(pathToInstances))) {
      pathList = stream.map(Path::normalize)
          .filter(Files::isRegularFile)
          .filter(p -> isJSON(p.toString()))
          .toList();
    } catch (IOException e) {
      final String msg = prefix + " " + e.getMessage();
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }

    // Convert list of paths into an array of parameter arrays
    Object[][] params = new Object[pathList.size()][1];
    for (int i = 0; i < pathList.size(); ++i) {
      params[i][0] = pathList.get(i);
    }
    return params;
  }

  /**
   * Returns a singleton list containing a hardcoded .json file representing a single
   * workflow instance.
   * Useful for running a single workflow during testing of the testing.
   */
  @DataProvider(name = "single-workflow-provider")
  public Object[][] supplySingleWorkflowPath() {
    final String filename = "TinyIRV.INFINITE.json";
    Path path = Paths.get(pathToInstances, filename);
    Path normalizedPath = path.normalize();
    assertTrue(Files.isRegularFile(normalizedPath));
    assertTrue(isJSON(normalizedPath.toString()));
    Object[][] params = new Object[1][1];
    params[0][0] = normalizedPath;
    return params;
  }

  /**
   * Given a JSON file defining an audit workflow (CVRs, Manifests, which CVRs to replace with
   * phantoms, which ballots to treat as phantoms, expected diluted margins and sample sizes,
   * which ballots to simulate discrepancies for, and expected end of round states ...), run
   * the test audit and verify that the expected outcomes arise.
   *
   * @param pathToInstance Path to the JSON workflow instance defining the test.
   * @throws InterruptedException if there is a problem with the CVR and Manifest upload.
   */
  @Transactional
  @Test(dataProvider = "workflow-provider")
  // @Test(dataProvider = "single-workflow-provider")
  public void runInstance(final Path pathToInstance) throws InterruptedException {
    final String prefix = "[runInstance] " + pathToInstance;

    try {

      // Reset the database; do the workflow.
      resetDatabase("stateadmin1");
      doWorkflow(pathToInstance, Optional.of(postgres));

    } catch(IOException e){
      final String msg = prefix + " " + e.getMessage();
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }


  /**
   * Utility that returns true if the given file path represents a JSON file.
   * @param filePath Path to the file (as a string).
   * @return true if the given file is a JSON file.
   */
  private static boolean isJSON(final String filePath) {
    return filePath.toLowerCase().endsWith(".json");
  }
}
