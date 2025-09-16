package us.freeandfair.corla.model;

import org.testng.annotations.Test;
import us.freeandfair.corla.persistence.Persistence;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

import java.time.Clock;
import java.time.Instant;

import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.*;
import static us.freeandfair.corla.util.EqualsHashcodeHelper.nullableHashCode;

@Test
public class AdministratorTest extends TestClassWithDatabase {

    @Test
    public static void testGettersAndSetters() {
        String expectedUsername = "testname";
        Administrator.AdministratorType expectedType = Administrator.AdministratorType.STATE;
        String expectedFullname = "fulltestname";

        // Make the clock stuck
        Clock testClock = Clock.fixed(Instant.now(), Clock.systemDefaultZone().getZone());
        Administrator admin = new Administrator(expectedUsername,
                expectedType,
                expectedFullname,
                null,
                testClock);


        assertNull(admin.id());
        assertNull(admin.version());
        Persistence.saveOrUpdate(admin);

        // this is a database constraint
        assertNotNull(admin.id());

        Long expectedID = 42L;
        admin.setID(expectedID);
        assertEquals(expectedID, admin.id());

        assertEquals(expectedUsername, admin.username());
        assertEquals(expectedFullname, admin.fullName());
        assertEquals(expectedType, admin.type());
        assertNull(admin.county());

        // Because we're using a stopped clock, this time will be the same for both
        Instant expectedTime = testClock.instant();
        assertNull(admin.lastLoginTime());
        admin.updateLastLoginTime();
        assertEquals(expectedTime, admin.lastLoginTime());

        assertNull(admin.lastLogoutTime());
        admin.updateLastLogoutTime();
        assertEquals(expectedTime, admin.lastLogoutTime());

        String expected_string = "Administrator [username=" + expectedUsername + ", type=" +
                expectedType+ ", full_name=" + expectedFullname+ ", county=" +
                null + ", last_login_time=" + expectedTime +
                ", last_logout_time=" + expectedTime + "]";

        assertEquals(expected_string, admin.toString());

        /**
         * Now test an admin without a clock set. This means we can't test timing-specific things,
         * but we can at least test that the code paths behave the way we expect.
         */
        admin = new Administrator(expectedUsername,
                expectedType,
                expectedFullname,
                null);

        assertNull(admin.lastLoginTime());
        assertNull(admin.lastLogoutTime());

        admin.updateLastLoginTime();
        assertNotNull(admin.lastLoginTime());
        admin.updateLastLogoutTime();
        assertNotNull(admin.lastLogoutTime());
        assert(admin.lastLoginTime() != admin.lastLogoutTime());
    }

    @Test
    public static void testEquality() {
        String firstAdminUsername = "first";
        String secondAdminUsername = "second";
        Administrator.AdministratorType expectedType = Administrator.AdministratorType.STATE;
        String expectedFullname = "fulltestname";

        Administrator firstAdmin = new Administrator(firstAdminUsername, expectedType, expectedFullname, null);
        Administrator secondAdmin = new Administrator(secondAdminUsername, expectedType, expectedFullname, null);

        assertTrue(firstAdmin.equals(firstAdmin));
        assertFalse(firstAdmin.equals(secondAdmin));

        // Now test that non-admin objects result in false
        assertFalse(firstAdmin.equals(firstAdminUsername));
    }

    @Test
    public static void testHash() {
        String expectedUsername = "testname";
        Administrator.AdministratorType expectedType = Administrator.AdministratorType.STATE;
        String expectedFullname = "fulltestname";

        Administrator admin = new Administrator(expectedUsername,
                expectedType,
                expectedFullname,
                null);

        assertEquals(admin.hashCode(), nullableHashCode(expectedUsername));

    }
}
