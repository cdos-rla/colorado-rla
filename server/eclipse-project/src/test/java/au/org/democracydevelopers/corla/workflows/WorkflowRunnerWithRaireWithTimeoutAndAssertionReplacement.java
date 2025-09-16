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

import io.restassured.path.json.JsonPath;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.testng.Assert.*;
import static us.freeandfair.corla.asm.ASMState.DoSDashboardState.DOS_INITIAL_STATE;

/**
 * This workflow runner is designed to run in a UAT environment in which the raire service, colorado-rla
 * server and corla database are all set up and running.
 * It matches the manual test specified in src/test/java/au/org/democracydevelopers/corla/workflows/manualWorkflowWithTimeoutAndAssertionReplacement.md:w
 * 1. You will need a copy of the colorado-rla code, which you can either clone with
 * `git clone https://github.com/DemocracyDevelopers/colorado-rla.git`
 * `git clone git@github.com:DemocracyDevelopers/colorado-rla.git`
 * or download as a zip from
 * <a href="https://github.com/DemocracyDevelopers/colorado-rla/archive/refs/heads/main.zip">...</a>
 * 2. Use src/test/resources/test.properties to specify the raire url, database login credentials and
 * url/port of the main colorado-rla server.
 * 3. Ensure that maven and java are installed.
 * 4. Ensure that the database contains correct setup data such as administrator credentials,
 * which can be loaded from colorado-rla/test/corla-test.credentials.psql, and the list of Colorado
 * counties. Note that once these are in the database, you may execute the command in Step 6
 * repeatedly without resetting the database. At the start of running a workflow (with RAIRE), the
 * database is reset.
 * 5. Ensure that the colorado-rla server and raire-service are running.
 * 6. From the eclipse-project directory, to run this test via the command line, enter
 * `mvn test -Dtest="*WorkflowRunnerWithRaireWithTimeoutAndAssertionReplacement" -DworkflowFile=""
 * You can also run the test in your IDE - instructions for IntelliJ are at
 * <a href="https://www.jetbrains.com/help/idea/work-with-tests-in-maven.html#skip_test">...</a>
 * This test is skipped when the tests are run with default parameters, i.e. during automated testing.
 */
@Test(enabled=true)
public class WorkflowRunnerWithRaireWithTimeoutAndAssertionReplacement extends WorkflowRunnerWithRaire {

  /**
   * Class-wide logger.
   */
  private static final Logger LOGGER = LogManager.getLogger(WorkflowRunnerWithRaireWithTimeoutAndAssertionReplacement.class);

  /**
   * Run assertion generation test workflow, interacting with raire-service.
   * 1. Request assertion generation with a very short timeout; check for failure.
   * 2. Repeat the request, with a longer timeout; check for success and replacement of the summary.
   * 3. Change the CVRs, repeat assertion generation, and check that the assertions were replaced.
   * @param workflowFile Only used to check whether the default of "SKIP" is still present.
   * @throws InterruptedException if something goes wrong with file upload.
   */
  @Parameters("workflowFile")
  @Test
  @Override
  public void runInstance(final String workflowFile) throws InterruptedException {
    final String prefix = "[runInstance] ";
    LOGGER.info(String.format("%s %s.", prefix, "running WorkflowRunnerWithRaireWithTimeoutAndAssertionReplacement"));
    final String path = "src/test/resources/CSVs/NewSouthWales21/";

    if(workflowFile.equalsIgnoreCase("SKIP")) {
      // Return without running the test. This means that when included in automated workflows
      // without command-line arguments, this test always passes.
      // The default argument is passed in from src/test/resources/testng.xml
      return;
    }

    // Reset the database.
    resetDatabase("stateadmin1");

    // Upload the manifest and CSVs for Byron Mayoral, into Adams (county 1).
    uploadCounty(1, MANIFEST_FILETYPE, path + "Byron_Mayoral.manifest.csv", path + "Byron_Mayoral.manifest.csv.sha256sum");
    uploadCounty(1, CVR_FILETYPE, path + "Byron_Mayoral.csv", path + "Byron_Mayoral.csv.sha256sum");

    // Wait while CVRs and manifests are uploaded.
    assertTrue(uploadSuccessfulWithin(600, Set.of(1), CVR_JSON));
    assertTrue(uploadSuccessfulWithin(20, Set.of(1), MANIFEST_JSON));

    // Get the DoSDashboard refresh response; sanity check for initial state.
    JsonPath dashboard = getDoSDashBoardRefreshResponse();
    assertEquals(dashboard.get(ASM_STATE), DOS_INITIAL_STATE.toString());
    assertEquals(dashboard.getMap(AUDIT_INFO + "." + CANONICAL_CHOICES).toString(), "{}");
    assertNull(dashboard.get(AUDIT_INFO + "." + RISK_LIMIT_JSON));
    assertNull(dashboard.get(AUDIT_INFO + "." + SEED));

    // Provide a risk limit and (unused) canonicalization file.
    updateAuditInfo(path + "Ballina_Bellingen_Boulder2023_Test_Canonical_List.csv",
        BigDecimal.valueOf(0.03));

    // 1. Call raire to request the assertion data, with an extremely small time limit.
    makeAssertionData(Optional.empty(), List.of(), 0.00001);

    // Get the generate assertions summaries and check for timeout failure.
    dashboard = getDoSDashBoardRefreshResponse();
    assertEquals(dashboard.getList(GENERATE_ASSERTIONS_SUMMARIES).size(), 1);
    String contestName = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.contestName");
    assertTrue(StringUtils.containsIgnoreCase(contestName, "Byron"));
    String winner = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.winner");
    assertTrue(winner.isEmpty());
    String error = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.error");
    assertTrue(StringUtils.containsIgnoreCase(error, "TIMEOUT_FINDING_ASSERTIONS"));
    String message = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.message");
    assertTrue(StringUtils.containsIgnoreCase(message, "Time out finding assertions"));

    // Get the Assertions CSV and check that contains only the timeout error.
    List<String> assertionsCSV = getReportAsCSV("assertions_csv");
    assertEquals(assertionsCSV.size(), 1);
    assertTrue(StringUtils.containsIgnoreCase(assertionsCSV.get(0), "NO_ASSERTIONS_PRESENT"));

    // 2. Request assertion generation with a reasonable time limit.
    makeAssertionData(Optional.empty(), List.of(), 5);

    // Get the generate assertions summaries and check for success and the correct winner.
    dashboard = getDoSDashBoardRefreshResponse();
    assertEquals(dashboard.getList(GENERATE_ASSERTIONS_SUMMARIES).size(), 1);
    contestName = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.contestName");
    assertTrue(StringUtils.containsIgnoreCase(contestName, "Byron"));
    winner = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.winner");
    assertTrue(StringUtils.containsIgnoreCase(winner, "LYON Michael"));
    error = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.error");
    assertTrue(error.isEmpty());
    message = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.message");
    assertTrue(message.isEmpty());

    // Get the Assertions CSV and check that it contains assertions, including the name of the
    // substituted winner and not the name of the previous winner.
    assertionsCSV = getReportAsCSV("assertions_csv");
    assertTrue(assertionsCSV.size() > 1);
    assertTrue(assertionsCSV.stream().anyMatch(l -> StringUtils.containsIgnoreCase(l, "LYON Michael")));

    // 3. Substitute the CSVs with an equivalent set that has two candidates swapped.
    uploadCounty(1, CVR_FILETYPE, path + "Byron_Mayoral_Swivel_Lyon_Swapped.csv", path + "Byron_Mayoral_Swivel_Lyon_Swapped.csv.sha256sum");
    assertTrue(uploadSuccessfulWithin(600, Set.of(1), CVR_JSON));

    // Wait for 2 seconds to make sure the updated CVRs are available to the raire-service in the DB.
    // Note: if this test is failing, try increasing this number.
    Thread.sleep(2000);
    // Request assertion generation with a reasonable time limit.
    makeAssertionData(Optional.empty(), List.of(), 5);

    // Get the generate assertions summaries and check for success and the substituted winner.
    dashboard = getDoSDashBoardRefreshResponse();
    assertEquals(dashboard.getList(GENERATE_ASSERTIONS_SUMMARIES).size(), 1);
    contestName = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.contestName");
    assertTrue(StringUtils.containsIgnoreCase(contestName, "Byron"));
    winner = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.winner");
    assertTrue(StringUtils.containsIgnoreCase(winner, "SWIVEL SwappedWithLyon"));
    error = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.error");
    assertTrue(error.isEmpty());
    message = dashboard.getString(GENERATE_ASSERTIONS_SUMMARIES + "[0].summary.message");
    assertTrue(message.isEmpty());

    // Get the Assertions CSV and check that it contains assertions, including the name of the
    // substituted winner and not the name of the previous winner.
    assertionsCSV = getReportAsCSV("assertions_csv");
    assertTrue(assertionsCSV.size() > 1);
    assertTrue(assertionsCSV.stream().noneMatch(l -> StringUtils.containsIgnoreCase(l, "LYON Michael")));
    assertTrue(assertionsCSV.stream().anyMatch(l -> StringUtils.containsIgnoreCase(l, "SWIVEL SwappedWithLyon")));
  }
}
