package us.freeandfair.corla.endpoint;

import static org.testng.Assert.assertEquals;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.*;

import us.freeandfair.corla.asm.CountyDashboardASM;
import us.freeandfair.corla.model.County;
import us.freeandfair.corla.model.CountyDashboard;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.Setup;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

import java.util.Properties;

@Test(groups = {"integration"})
public class StartAuditRoundTest extends TestClassWithDatabase {

  private StartAuditRoundTest() {};

  // this test doesn't do much yet
  @Test()
  public void testReadyToStartFalse() {
    StartAuditRound sar = new StartAuditRound();
    County county = new County("c1", 1L);
    CountyDashboard cdb = new CountyDashboard(county);
    CountyDashboardASM cdbAsm = new CountyDashboardASM(cdb.id().toString());

    assertEquals(false, (boolean)sar.isReadyToStartAudit(cdb));
  }

}
