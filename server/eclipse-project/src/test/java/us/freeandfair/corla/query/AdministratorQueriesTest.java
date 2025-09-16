package us.freeandfair.corla.query;

import org.testng.annotations.*;
import us.freeandfair.corla.model.Administrator;
import us.freeandfair.corla.model.County;
import us.freeandfair.corla.persistence.Persistence;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

@Test
public class AdministratorQueriesTest extends TestClassWithDatabase {

    @Test
    public void testByUsernameState() {

        Administrator admin = new Administrator("testname",
                                                Administrator.AdministratorType.STATE,
                                                "fulltestname",
                                                null);

        Persistence.saveOrUpdate(admin);
        assertEquals(AdministratorQueries.byUsername("testname"), admin);
    }


    @Test
    public void testByUsernameCounty() {

        County county = new County("test", 1L);
        Persistence.saveOrUpdate(county);
        Administrator admin = new Administrator("testname",
                Administrator.AdministratorType.COUNTY,
                "fulltestname",
                county
                );

        Persistence.saveOrUpdate(admin);
        assertEquals(AdministratorQueries.byUsername("testname"), admin);
    }

    @Test
    public void testByUsernameStateAndCounty() {
        County county = new County("test", 1L);
        Persistence.saveOrUpdate(county);
        Administrator countyadmin = new Administrator("county",
                Administrator.AdministratorType.COUNTY,
                "countyfull",
                county);

        Persistence.saveOrUpdate(countyadmin);

        Administrator stateadmin = new Administrator("state",
                Administrator.AdministratorType.STATE,
                "statefull",
                null);

        Persistence.saveOrUpdate(stateadmin);

        assertEquals(AdministratorQueries.byUsername("county"),countyadmin);
        assertEquals(AdministratorQueries.byUsername("state"), stateadmin);
    }

    @Test
    public void testSameName() {

        Administrator first = new Administrator("state",
                Administrator.AdministratorType.STATE,
                "first",
                null);

        Administrator second = new Administrator("state",
                Administrator.AdministratorType.STATE,
                "second",
                null);
        Persistence.saveOrUpdate(first);
        Persistence.saveOrUpdate(second);

        assertNull(AdministratorQueries.byUsername("state"));
    }

    @Test
    public void testDBError() {
        // Close the DB
        Persistence.commitTransaction();
        assertNull(AdministratorQueries.byUsername("username"));
        Persistence.beginTransaction();
    }
}
