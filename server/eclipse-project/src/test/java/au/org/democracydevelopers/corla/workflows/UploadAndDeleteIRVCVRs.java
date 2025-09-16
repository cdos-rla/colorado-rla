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

import au.org.democracydevelopers.corla.util.testUtils;
import io.restassured.RestAssured;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import us.freeandfair.corla.persistence.Persistence;

import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * Simple workflow to test cascading deletion of IRVBallotInterpretations when a CVR upload file is
 * deleted. This is a check that this bug:
 * <a href="https://github.com/orgs/DemocracyDevelopers/projects/1?pane=issue&itemId=98663326&issue=DemocracyDevelopers%7Ccolorado-rla%7C241">...</a>
 * was correctly fixed.
 * This uploads the same file full of invalid IRV votes in two counties, then deletes each CSV,
 * checking that the right number of rows have been removed from the IRVBallotInterpretation table.
 */
@Test(enabled=true)
public class UploadAndDeleteIRVCVRs extends Workflow {

  /**
   * Path for all the data files.
   */
  private static final String dataPath = "src/test/resources/CSVs/Tiny-IRV-Examples/";

  /**
   * Class-wide logger
   */
  private static final Logger LOGGER = LogManager.getLogger(UploadAndDeleteIRVCVRs.class);

  @BeforeClass
  public void setup() {
    runMain(config, "UploadAndDeleteIRVCVRs");
    Persistence.beginTransaction();
    runSQLSetupScript(postgres, "SQL/co-admins.sql");

    RestAssured.baseURI = "http://localhost";
    RestAssured.port = 8888;
  }

  /**
   * This "test" uploads CVRs and ballot manifests.
   */
  @Test(enabled=true)
  public void runUploadAndDeleteCVRs() throws InterruptedException {
    testUtils.log(LOGGER, "runUploadAndDeleteIRVCVRs");

    // Upload CSVs with 10 invalid IRV votes, for counties 1 and 2.
    final String CVRFile = dataPath + "ThreeCandidatesTenInvalidVotes";

    for(int i = 1 ; i <= 2 ; i++) {
      uploadCounty(i, CVR_FILETYPE, CVRFile + ".csv", CVRFile + ".csv.sha256sum");
    }

    assertTrue(uploadSuccessfulWithin(5, Set.of(1,2), CVR_JSON));
    // assertFalse(uploadSuccessfulWithin(5, Set.of(1,2,3), MANIFEST_JSON));

    // Now there should be 20 interpretations of invalid IRV votes, i.e. 21 lines including the header.
    List<String> ivrBallotInterpretations = getReportAsCSV("ranked_ballot_interpretation");
    assertEquals(ivrBallotInterpretations.size(), 21);

    // County 1 deletes its csv.
    deleteFile(1, "cvr");

    // Now there should be 10 interpretations of invalid IRV votes, i.e. 11 lines including the header.
    ivrBallotInterpretations = getReportAsCSV("ranked_ballot_interpretation");
    assertEquals(ivrBallotInterpretations.size(), 11);

    // County 2 deletes its csv.
    deleteFile(2, "cvr");

    // Now there should be 0 interpretations of invalid IRV votes, i.e. only the header.
    ivrBallotInterpretations = getReportAsCSV("ranked_ballot_interpretation");
    assertEquals(ivrBallotInterpretations.size(), 1);
  }
}
