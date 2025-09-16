package us.freeandfair.corla.query;

import org.apache.velocity.util.ArrayListWrapper;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;
import org.testng.annotations.*;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.persistence.Persistence;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.Optional;

import static org.testng.Assert.assertEquals;

@Test
public class BallotManifestInfoQueriesTest extends TestClassWithDatabase {

    public BallotManifestInfo setupBMI() {
         return new BallotManifestInfo(1L,
                1,
                "batch1",
                64,
                "storage1",
                0L,
                64L);
    }

    @Test
    public void testGetMatching() {
        BallotManifestInfo bmi = setupBMI();
        Persistence.saveOrUpdate(bmi);

        Set<Long> counties = new HashSet<>();
        counties.add(1L);

        Set<BallotManifestInfo> expected = new HashSet<>();
        expected.add(bmi);
        assertEquals(BallotManifestInfoQueries.getMatching(counties), expected);

    }

    @Test
    public void testGetMatchingDBError() {
        Set<Long> counties = new HashSet<>();
        counties.add(1L);

        Persistence.rollbackTransaction();
        // This code should return an empty set in the event of a PersistenceException
        assertEquals(new TreeSet<>(), BallotManifestInfoQueries.getMatching(counties));
        Persistence.beginTransaction();
    }

    @Test
    public void testLocationForURI() {
        BallotManifestInfo bmi = setupBMI();
        Persistence.saveOrUpdate(bmi);

        String uri = String.format("%s:%s:%s-%s",
                "bmi",
                1L,
                1,
                "batch1");

        Set<String> uris = new HashSet();
        uris.add(uri);

        List<BallotManifestInfo> expected = new ArrayList<>();
        expected.add(bmi);
        assertEquals(BallotManifestInfoQueries.locationFor(uris), expected);

    }

    @Test
    public void testLocationForURIEmpty() {
        assertEquals(BallotManifestInfoQueries.locationFor(new HashSet<>()), new ArrayList<>());
    }

    @Test
    public void testLocationForCVR() {
        BallotManifestInfo bmi = setupBMI();
        Persistence.saveOrUpdate(bmi);

        County county = new County("Test County", 1L);
        Persistence.saveOrUpdate(county);

        List<Choice> choices = new ArrayList();
        Choice choice = new Choice("Bob Anderson", "Esquire", false, false);
        choices.add(choice);

        List<String> votes = new ArrayList<>();
        votes.add("Bob Anderson");
        votes.add("Bob Anderson");

        Contest contest = new Contest("test", county, "description", choices, 1, 1, 1);
        Persistence.saveOrUpdate(contest);
        List<CVRContestInfo> contest_info = new ArrayList<>();
        contest_info.add( new CVRContestInfo(contest, "comment", null, votes));

        CastVoteRecord cvr = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
                null,
                1L,
                1,
                1,
                1,
                "batch1",
                1,
                "1",
                "a",
                contest_info);

        Persistence.saveOrUpdate(cvr);

        Optional<BallotManifestInfo> expected = Optional.of(bmi);
        assertEquals(expected, BallotManifestInfoQueries.locationFor(cvr));

        CastVoteRecord bad = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
                null,
                1L,
                1,
                1,
                1,
                "batchthatdoesntexist",
                1,
                "1",
                "a",
                contest_info);
        Persistence.saveOrUpdate(bad);

        assertEquals(Optional.empty(), BallotManifestInfoQueries.locationFor(bad));

    }

    @Test
    public void testLocationForCVRDBError() {
        Persistence.commitTransaction();
        assertEquals(Optional.empty(), BallotManifestInfoQueries.locationFor(new CastVoteRecord()));
        Persistence.beginTransaction();
    }

    @Test
    public void testDeleteMatching() {
        BallotManifestInfo bmi = setupBMI();
        Persistence.saveOrUpdate(bmi);
        String uri = String.format("%s:%s:%s-%s",
                "bmi",
                1L,
                1,
                "batch1");

        Set<String> uris = new HashSet();
        uris.add(uri);

        List<BallotManifestInfo> expected = new ArrayList<>();
        expected.add(bmi);
        assertEquals(BallotManifestInfoQueries.locationFor(uris), expected);

        // Now delete everything for county 1L
        int num_deleted = BallotManifestInfoQueries.deleteMatching(1L);

        assertEquals(1, num_deleted);
        assertEquals(new ArrayList<>(), BallotManifestInfoQueries.locationFor(uris));

    }

    @Test
    public void testCount() {
        BallotManifestInfo bmi = setupBMI();
        Persistence.saveOrUpdate(bmi);

        assertEquals(OptionalLong.of(1L), BallotManifestInfoQueries.count());
    }

    @Test
    public void testCountDBError() {
        Persistence.commitTransaction();
        assertEquals(OptionalLong.empty(), BallotManifestInfoQueries.count());
        Persistence.beginTransaction();
    }

    @Test
    public void testHoldingSequencePosition() {
        BallotManifestInfo bmi = setupBMI();
        Persistence.saveOrUpdate(bmi);

        assertEquals(Optional.of(bmi), BallotManifestInfoQueries.holdingSequencePosition(1L, 1L));
    }

    @Test
    public void testHoldingSequencePositionDBError() {
        Persistence.commitTransaction();
        assertEquals(Optional.empty(), BallotManifestInfoQueries.holdingSequencePosition(1L, 1L));
        Persistence.beginTransaction();
    }

    @Test
    public void testMaxSequence() {
        BallotManifestInfo bmi = setupBMI();
        Persistence.saveOrUpdate(bmi);

        assertEquals(OptionalLong.of(64L), BallotManifestInfoQueries.maxSequence(1L));
    }


    @Test
    public void testMaxSequenceDBError() {
        Persistence.commitTransaction();
        assertEquals(OptionalLong.empty(), BallotManifestInfoQueries.maxSequence(1L));
        Persistence.beginTransaction();
    }

    @Test
    public void testTotalBallots() {
        BallotManifestInfo bmi = setupBMI();
        Persistence.saveOrUpdate(bmi);
        Persistence.flush();

        Set<Long> countyIDs = new HashSet<>();
        assertEquals((Long)0L,BallotManifestInfoQueries.totalBallots(countyIDs));

        countyIDs.add(1L);
        assertEquals((Long)64L,BallotManifestInfoQueries.totalBallots(countyIDs));

        // Make sure that non-existing counties don't impact others
        countyIDs.add(2L);
        assertEquals((Long)64L,BallotManifestInfoQueries.totalBallots(countyIDs));

        countyIDs.remove(1L);
        assertEquals((Long)0L,BallotManifestInfoQueries.totalBallots(countyIDs));
    }

}
