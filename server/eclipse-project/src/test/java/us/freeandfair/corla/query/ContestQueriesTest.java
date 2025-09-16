package us.freeandfair.corla.query;

import net.sf.ehcache.search.aggregator.Count;
import org.hibernate.TransientObjectException;
import org.hibernate.query.Query;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testng.annotations.*;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.model.Choice;
import us.freeandfair.corla.persistence.Persistence;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.testng.Assert.*;

@Test(groups = {"integration"})
public class ContestQueriesTest extends TestClassWithDatabase {

    public County countySetup() {
        return countySetup(1L);
    }

    public County countySetup(long county_id) {
        return new County("test" + county_id, county_id);
    }

    public List<Choice> setupChoices() {
        List<Choice> choices = new ArrayList<>();
        Choice choice = new Choice("Test Choice 1", "Test choice description", false, false);
        choices.add(choice);
        choice = new Choice("Test Choice 2", "Test choice description", false, false);
        choices.add(choice);
        return choices;
    }

    @Test
    public void testForCountiesSimple() {
        County county = countySetup();
        List<Choice> choices = setupChoices();
        Contest contest = new Contest("Test Contest", county, "Test description", choices, 1, 1, 1);

        Persistence.saveOrUpdate(county);
        Persistence.saveOrUpdate(contest);

        Set<County> countySet = new HashSet<>();
        countySet.add(county);

        List<Contest> expected = new ArrayList<>();
        expected.add(contest);
        assertEquals(ContestQueries.forCounties(countySet), expected);

    }

    @Test
    public void testForCountiesMulti() {

        List<County> counties = new ArrayList<>();
        List<Contest> contests = new ArrayList<>();

        List<Choice> choices = setupChoices();
        for(int i = 0; i < 10; i++) {
           County county = countySetup(i);
           counties.add(county);
           Persistence.saveOrUpdate(county);


           Contest contest = new Contest("Test", county, "Description", choices, 1, 1, 1);
           Persistence.saveOrUpdate(contest);
           contests.add(contest);
        };

        // See what happens if we only want the first 5 counties
        Set<County> countySet = new HashSet<>(counties.subList(0, 5));

        List<Contest> expected = contests.subList(0, 5);
        assertEquals(ContestQueries.forCounties(countySet), expected);

    }

    @Test
    public void testForCountySimple() {

        County county = countySetup();
        List<Choice> choices = setupChoices();
        Contest contest = new Contest("Test Contest", county, "Test description", choices, 1, 1, 1);

        Persistence.saveOrUpdate(county);
        Persistence.saveOrUpdate(contest);

        List<Contest> expected = new ArrayList<>();
        expected.add(contest);
        assertEquals(ContestQueries.forCounty(county), expected);
    }

    @Test
    public void testForCountyMulti() {

        County county = countySetup();
        Persistence.saveOrUpdate(county);

        List<Contest> contests = new ArrayList<>();
        List<Choice> choices = setupChoices();
        for(int i = 0; i < 10; i++) {
            Contest contest = new Contest("Test" + i, county, "Description", choices, 1, 1, 1);
            Persistence.saveOrUpdate(contest);
            contests.add(contest);
        };

        Set<Contest> expected = new HashSet<>(contests);
        assertEquals(ContestQueries.forCounty(county), expected);

    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testForCountyNull() {
        // We are intentionally not persisting this county to see what happens.
        County county = countySetup();

        ContestQueries.forCounty(county);
        fail("An error should have been thrown for a non-existent county!");

    }

    @Test()
    public void testDBError() {
        // Close the database
        Persistence.commitTransaction();

        // TODO: is there a way to check that the right message is logged?
        County county = countySetup();
        assertNull(ContestQueries.forCounty(county));

        Set<County> counties = new HashSet<>();
        counties.add(county);
        assertNull(ContestQueries.forCounties(counties));

        Persistence.beginTransaction();
        ContestQueries.deleteForCounty(-1L);

    }

    @Test
    public void testDeleteForCounty() {

        County county = countySetup();
        Persistence.saveOrUpdate(county);

        Set<Contest> expected = new HashSet<>();
        assertEquals(ContestQueries.forCounty(county), expected);

        List<Contest> contests = new ArrayList<>();
        List<Choice> choices = setupChoices();
        for(int i = 0; i < 10; i++) {
            Contest contest = new Contest("Test" + i, county, "Description", choices, 1, 1, 1);
            Persistence.saveOrUpdate(contest);
            contests.add(contest);
        };

        expected = new HashSet<>(contests);
        assertEquals(ContestQueries.forCounty(county), expected);

        assertEquals(ContestQueries.deleteForCounty(county.id()), 10);

        expected = new HashSet<>();
        assertEquals(ContestQueries.forCounty(county), expected);

        // This should be a no-op as county.contests is null
        assertEquals(ContestQueries.deleteForCounty(county.id()), 0);
        assertEquals(ContestQueries.forCounty(county), expected);

        // See what happens when we pass an invalid county
        assertEquals(ContestQueries.deleteForCounty(-1L), 0);

        // Test what happens when forCounty returns null
        Query q = Persistence.currentSession().createNativeQuery("DROP TABLE contest CASCADE");
        q.executeUpdate();
        assertEquals(ContestQueries.deleteForCounty(1L), -1);
    }


}
