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

import io.restassured.RestAssured;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.*;
import us.freeandfair.corla.persistence.Persistence;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * This workflow runner is designed to run in a UAT environment in which the raire service, colorado-rla
 * server and corla database are all set up and running.
 * 1. You will need a copy of the colorado-rla code, which you can either clone with
 * `git clone https://github.com/DemocracyDevelopers/colorado-rla.git`
 * `git clone git@github.com:DemocracyDevelopers/colorado-rla.git`
 * or download as a zip from
 * https://github.com/DemocracyDevelopers/colorado-rla/archive/refs/heads/main.zip
 * 2. Use src/test/resources/test.properties to specify the raire url, database login credentials and
 * url/port of the main colorado-rla server.
 * 3. Ensure that maven and java are installed.
 * 4. Ensure that the database contains correct setup data such as administrator credentials,
 * which can be loaded from colorado-rla/test/corla-test.credentials.psql, and the list of Colorado
 * counties. Note that once these are in the database, you may execute the command in Step 6
 * repeatedly without resetting the database. At the start of running a workflow (with RAIRE), the
 * database is reset.
 * 5. Ensure that the colorado-rla server and raire-service are running.
 * 6. From the eclipse-project directory, to run a workflow json file via the command line, enter
 * `mvn test -Dtest="*WorkflowRunnerWithRaire" -DworkflowFile="[Path to workflow file]"`
 * For example, to run the AllPluralityTwoVoteOverstatementTwoRounds workflow, enter
 * `mvn test -Dtest="*WorkflowRunnerWithRaire" -DworkflowFile="src/test/resources/workflows/instances/AllPluralityTwoVoteOverstatementTwoRounds.json"`
 * You can also run the test in your IDE - instructions for IntelliJ are at
 * <a href="https://www.jetbrains.com/help/idea/work-with-tests-in-maven.html#skip_test">...</a>
 * This test is skipped when the tests are run with default parameters, i.e. during automated testing.
 * This deliberately does _not_ reset the database after the workflow runs. This might be helpful if
 * someone wants to examine its state via the client afterwards.
 */
@Test(enabled=true)
public class WorkflowRunnerWithRaire extends Workflow {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(WorkflowRunnerWithRaire.class);

  /**
   * Override Workflow's setup of RestAssured's baseURI and port - point them at the real
   * colorado-rla server.
   */
  @BeforeClass
  public void setup() {
    RestAssured.baseURI = config.getProperty("corla_url");
    RestAssured.port = Integer.parseInt(config.getProperty("corla_http_port"));
  }

  /**
   * Override TestClassWithDatabase setup of a postgres container - assume we've got a real corla
   * database to use.
   */
  @BeforeClass
  @Override
  public void initDatabase() {
    Persistence.setProperties(config);
    session = Persistence.openSession();
  }

  @AfterClass
  @Override
  public void afterAll() {
    session.close();
  }

  /**
   * Override TestClassWithDatabase's rollback.
   */
  @AfterMethod
  @Override
  public void afterTest() {}

  /**
   * Given a JSON file defining an audit workflow (CVRs, Manifests, which CVRs to replace with
   * phantoms, which ballots to treat as phantoms, expected diluted margins and sample sizes,
   * which ballots to simulate discrepancies for, and expected end of round states ...), run
   * the test audit and verify that the expected outcomes arise.
   * @param workflowFile Path to JSON workflow instance to execute.
   * @throws InterruptedException if something goes wrong with file I/O.
   */
  @Parameters("workflowFile")
  @Test
  public void runInstance(final String workflowFile) throws InterruptedException {
    final String prefix = "[runInstance] ";

    if(workflowFile.equalsIgnoreCase("SKIP")) {
      // Return without running the test. This means that when included in automated workflows
      // without command-line arguments, this test always passes.
      // The default argument is passed in from src/test/resources/testng.xml
      return;
    }

    if(workflowFile == null || workflowFile.isEmpty()) {
      System.out.println("Usage example, from eclipse-project directory: " +
          "mvn -Dtest=\"*WorkflowRunnerWithRaire\" " +
          "-DworkflowFile=\"src/test/resources/workflows/instances/AllPluralityTwoVoteOverstatementTwoRounds.json\" test");
      return;
    }

    try {
      final Path pathToInstance = Paths.get(workflowFile);
      LOGGER.info(String.format("%s %s %s.", prefix, "running workflow", pathToInstance));

      // Do the workflow. Reset the database first.
      resetDatabase("stateadmin1");

      doWorkflow(pathToInstance, Optional.empty());

    } catch(IOException e){
      final String msg = prefix + " " + e.getMessage() +
          ". Check colorado-rla is running, and that the address, path and filename are correct.";
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }
  }
}
