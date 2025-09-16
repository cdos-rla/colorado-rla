package us.freeandfair.corla.query;

import java.io.ByteArrayOutputStream;

import java.util.*;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.*;

import static org.testng.Assert.*;

import us.freeandfair.corla.persistence.Persistence;



import org.hibernate.Session;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;


@Test(groups = {"integration"})
public class ExportQueriesTest extends TestClassWithDatabase {

  @BeforeMethod
  public static void beforeEach() {
    insertSeed();
  }

  private static void insertSeed() {
    final Session s = Persistence.currentSession();
    String query = "insert into dos_dashboard (id,seed) values (99,'1234');";
    s.createNativeQuery(query).executeUpdate();
  }

  @Test()
  public void jsonRowsTest() {
    String q = "SELECT seed FROM dos_dashboard";
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    ExportQueries.jsonOut(q, os);

    assertEquals(os.toString(), "[{\"seed\":\"1234\"}]");
  }

  @Test()
  public void csvOutTest() {
    String q = "SELECT seed FROM dos_dashboard";
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    ExportQueries.csvOut(q, os);

    assertEquals(os.toString(), "seed\n1234\n");
  }

  @Test()
  public void sqlFilesTest()
    throws java.io.IOException {
    Map<String,String> files = ExportQueries.sqlFiles();
    assertTrue(files.get("seed").contains("SELECT seed FROM dos_dashboard"));
  }
}
