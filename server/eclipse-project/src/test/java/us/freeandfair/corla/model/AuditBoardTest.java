package us.freeandfair.corla.model;

import org.testng.annotations.Test;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

import java.lang.reflect.Array;
import java.time.Instant;

import java.util.ArrayList;

import static org.testng.AssertJUnit.*;
import static us.freeandfair.corla.util.EqualsHashcodeHelper.nullableHashCode;

public class AuditBoardTest extends TestClassWithDatabase {

    @Test
    public static void testGettersAndSetters () {

        AuditBoard defaultAB = new AuditBoard();
        assertEquals(new ArrayList<Elector>(), defaultAB.members());
        assertNull(defaultAB.signInTime());
        assertNull(defaultAB.signOutTime());

        ArrayList<Elector> electors = new ArrayList<>();

        electors.add(new Elector("firstname", "lastname", "party"));

        // TODO: do we want a stopped clock here too?
        Instant sign_in_time = Instant.now();

        AuditBoard ab = new AuditBoard(electors, sign_in_time);

        assertEquals(electors, ab.members());

        assertEquals(sign_in_time, ab.signInTime());

        Instant sign_out_time = Instant.now();

        ab.setSignOutTime(sign_out_time);
        assertEquals(sign_out_time, ab.signOutTime());

        String expectedToString = "AuditBoard [members=" + electors + ", sign_in_time=" +
                sign_in_time + ", sign_out_time=" + sign_out_time + "]";

        assertEquals(expectedToString, ab.toString());

        assertEquals(nullableHashCode(sign_in_time), ab.hashCode());

        assertFalse(ab.equals("Not an auditboard"));
        ArrayList<Elector> otherElectors = new ArrayList<>();
        otherElectors.add(new Elector("otherfirstname", "otherlastname", "otherparty"));

        AuditBoard otherAB = new AuditBoard(otherElectors, sign_in_time);

        assertFalse(ab.equals(otherAB));
        assertTrue(ab.equals(ab));

    }
}
