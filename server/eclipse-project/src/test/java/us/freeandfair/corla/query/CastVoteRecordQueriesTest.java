package us.freeandfair.corla.query;

import java.lang.reflect.Method;
import java.util.*;

import org.antlr.v4.runtime.misc.Array2DHashSet;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;
import org.testng.annotations.*;

import static org.testng.Assert.*;

import java.time.Instant;
import java.util.Optional;
import java.util.function.*;
import java.util.stream.*;

import us.freeandfair.corla.asm.PersistentASMState;
import us.freeandfair.corla.model.*;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.Setup;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

@Test(groups = {"integration"})
public class CastVoteRecordQueriesTest extends TestClassWithDatabase {

  public List<CVRContestInfo> noisyContestSetup(){
    return noisyContestSetup(1L);
  }

  public List<CVRContestInfo> noisyContestSetup(Long countyId){
    County c = new County("test" + countyId.toString(), countyId);

    List<Choice> choices = new ArrayList();
    Choice choice = new Choice("why?",
                               "",
                               false,
                               false);
    choices.add(choice);
    Contest co = new Contest("test",
                             c,
                             "",
                             choices,
                             1,
                             1,
                             1);
    co.setID(1L);

    List<String> votes = new ArrayList();
    votes.add("why?");

    CVRContestInfo ci = new CVRContestInfo(co, null,null, votes);
    List<CVRContestInfo> contest_info = new ArrayList();
    contest_info.add(ci);

    Persistence.save(c);
    Persistence.save(co);
    Persistence.flush();

    return contest_info;
  }

  public CastVoteRecord noisyCVRSetup() {
    return noisyCVRSetup(1);
  }

  public CastVoteRecord noisyCVRSetup(final Integer position) {
    return noisyCVRSetup(position, noisyContestSetup(Long.valueOf(position)));
  }

  public CastVoteRecord noisyCVRSetup(final Integer position, final List<CVRContestInfo> contest_info) {
    CastVoteRecord cvr = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
                                            null,
                                            1L,
                                            position,
                                            1,
                                            1,
                                            "1",
                                            1,
                                            "1",
                                            "a",
                                            contest_info);
    Persistence.save(cvr);

    // Without flushing the persistence context, `deleteAll(1L)` will
    // throw an exception.
    Persistence.flush();
    return cvr;
  }

  @Test()
  public void reauditTest() {
    // let's pretend the current max revision is 2, which we'll inc, and then
    // assertEquals
    Long curMax = 2L;
    CastVoteRecord cvr = noisyCVRSetup(2);
    cvr.setRevision(curMax + 1L);
    cvr.setToReaudited();
    Long result = CastVoteRecordQueries.forceUpdate(cvr);
    assertEquals((long) 1L, (long)result);
    Long maxRev = CastVoteRecordQueries.maxRevision(cvr);
    assertEquals((long)maxRev, (long)curMax + 1L);
  }

  @Test()
  public void deleteAllTest() {
    noisyCVRSetup(1);
    // this is the method under test
    Integer result = CastVoteRecordQueries.deleteAll(1L);

    assertEquals((int) result, (int) 1,
                 "a result of 1 means one thing was deleted");
  }

  @Test()
  public void canonicalChoicesTest() {
    CastVoteRecord cvr = noisyCVRSetup(1);
    // commit the transaction to populate the DB for debugging
    // Persistence.commitTransaction();
    // Persistence.beginTransaction();
    // Note: the weird access to get the contest ID is because somehow it gets changed from 1 to 4 when run with other tests.
    // TODO: THIS SHOULD NOT HAPPEN! WHY IS IT CHANGING FROM 1 TO 4 WHEN RUN WITH OTHER TESTS?!
    int result = CastVoteRecordQueries.updateCVRContestInfos(cvr.countyID(),cvr.contestInfo().get(0).contest().id(), "why?","because.");
    assertEquals(result, 1,
                 "a result of 1 means one choice was changed");

    Persistence.currentSession().refresh(cvr, LockMode.PESSIMISTIC_WRITE);
    assertTrue(cvr.contestInfo().toString().contains("choices=[because.]"));

  }


  @Test()
  public void activityReportTest() {
    CastVoteRecord cvr = noisyCVRSetup(1);
    CastVoteRecord acvr = new CastVoteRecord(CastVoteRecord.RecordType.AUDITOR_ENTERED, Instant.now(),
                                             cvr.countyID(), cvr.cvrNumber(), null, cvr.scannerID(),
                                             cvr.batchID(), cvr.recordID(), cvr.imprintedID(),
                                             cvr.ballotType(), cvr.contestInfo());
    acvr.setComment("testing");
    acvr.setAuditBoardIndex(14);
    acvr.setCvrId(cvr.id());
    CVRAuditInfo cai = new CVRAuditInfo(cvr);
    cai.setACVR(acvr);

    Persistence.save(acvr);
    Persistence.save(cai);

    List<CastVoteRecord> acvrs = new ArrayList(){{ add(acvr); }};
    List<Long> contestCVRIds = new ArrayList(){{ add(cvr.id()); }};
    List<CastVoteRecord> result = CastVoteRecordQueries.activityReport(contestCVRIds);
    assertEquals(acvrs, result);
  }

  @Test(groups =  {"localonly"})
  public void activityReportTestBig() {
    List<CVRContestInfo> contest_info = noisyContestSetup();
    Tribute tribute;
    CastVoteRecord cvr;

    List<CastVoteRecord> expected = new ArrayList<>();

    List<CastVoteRecord> acvrs = new ArrayList();
    List<Long> contestCVRIds = new ArrayList();
    // Now we need to test chunking, so add a whole lotta tributes
    // These are not the greatest CVRs in the world, they are just a tribute
    for (int i = 0; i < 2000; i++) {
      cvr = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
              null,
              1L,
              i,
              1,
              1,
              "1",
              i,
              "1",
              "a",
              contest_info);

      Persistence.save(cvr);
      CastVoteRecord acvr = new CastVoteRecord(CastVoteRecord.RecordType.AUDITOR_ENTERED, Instant.now(),
              cvr.countyID(), cvr.cvrNumber(), null, cvr.scannerID(),
              cvr.batchID(), cvr.recordID(), cvr.imprintedID(),
              cvr.ballotType(), cvr.contestInfo());

      acvr.setComment("testing");
      acvr.setAuditBoardIndex(14);
      acvr.setCvrId(cvr.id());

      CVRAuditInfo cai = new CVRAuditInfo(cvr);
      cai.setACVR(acvr);

      Persistence.save(acvr);
      Persistence.save(cai);

      acvrs.add(acvr);
      contestCVRIds.add(cvr.id());
    }
    List<CastVoteRecord> result = CastVoteRecordQueries.activityReport(contestCVRIds);
    assertEquals(acvrs, result);
  }

  @Test
  public void testGetMatching() {
    Set<CastVoteRecord> expected = new HashSet<>();

    assertEquals(expected, CastVoteRecordQueries.getMatching(CastVoteRecord.RecordType.UPLOADED).collect(Collectors.toSet()));
    assertEquals(expected, CastVoteRecordQueries.getMatching(1L, CastVoteRecord.RecordType.UPLOADED).collect(Collectors.toSet()));

    CastVoteRecord cvr = noisyCVRSetup();
    expected.add(cvr);

    assertEquals(expected, CastVoteRecordQueries.getMatching(CastVoteRecord.RecordType.UPLOADED).collect(Collectors.toSet()));
    assertEquals(expected, CastVoteRecordQueries.getMatching(1L, CastVoteRecord.RecordType.UPLOADED).collect(Collectors.toSet()));
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testGetMatchingDBError() {
    Persistence.commitTransaction();
    CastVoteRecordQueries.getMatching(CastVoteRecord.RecordType.UPLOADED);
  }

  @Test
  public void testGetMatchingNoCVRTable (){
    // Drop the CVR table, which should cause a different error in getMatching
    Query q = Persistence.currentSession().createNativeQuery("DROP TABLE cast_vote_record CASCADE");
    q.executeUpdate();
    assertNull(CastVoteRecordQueries.getMatching(CastVoteRecord.RecordType.UPLOADED));
    assertNull(CastVoteRecordQueries.getMatching(1L, CastVoteRecord.RecordType.UPLOADED));
  }

  @Test
  public void testCountMatching() {
    assertEquals(OptionalLong.of(0), CastVoteRecordQueries.countMatching(CastVoteRecord.RecordType.UPLOADED));
    assertEquals(OptionalLong.of(0), CastVoteRecordQueries.countMatching(1L, CastVoteRecord.RecordType.UPLOADED));
    CastVoteRecord cvr = noisyCVRSetup();
    assertEquals(OptionalLong.of(1), CastVoteRecordQueries.countMatching(CastVoteRecord.RecordType.UPLOADED));
    assertEquals(OptionalLong.of(1), CastVoteRecordQueries.countMatching(1L, CastVoteRecord.RecordType.UPLOADED));
  }

  @Test(expectedExceptions = IllegalStateException.class)
  public void testCountMatchingDBError() {
    Persistence.commitTransaction();
    CastVoteRecordQueries.countMatching(CastVoteRecord.RecordType.UPLOADED);
  }

  @Test
  public void testCountMatchingNoCVRTable (){
    // Drop the CVR table, which should cause a different error in getMatching
    Query q = Persistence.currentSession().createNativeQuery("DROP TABLE cast_vote_record CASCADE");
    q.executeUpdate();
    assertEquals(OptionalLong.empty(), CastVoteRecordQueries.countMatching(CastVoteRecord.RecordType.UPLOADED));
    assertEquals(OptionalLong.empty(), CastVoteRecordQueries.countMatching(1L, CastVoteRecord.RecordType.UPLOADED));
  }

  @Test
  public void testGetSingle() {
    // We don't have any records yet
    assertNull(CastVoteRecordQueries.get(1L, CastVoteRecord.RecordType.UPLOADED, 1));

    CastVoteRecord expected = noisyCVRSetup();
    assertEquals(expected, CastVoteRecordQueries.get(1L, CastVoteRecord.RecordType.UPLOADED, 1));

    // Now we have two CVRs with the same county, recordType, and sequenceNumber
    noisyCVRSetup(2);
    assertNull(CastVoteRecordQueries.get(1L, CastVoteRecord.RecordType.UPLOADED, 1));
  }

  @Test
  public void testGetSingleDBError() {
    // Drop the CVR table, which should cause a different error in getMatching
    Query q = Persistence.currentSession().createNativeQuery("DROP TABLE cast_vote_record CASCADE");
    q.executeUpdate();
    assertNull(CastVoteRecordQueries.get(1L, CastVoteRecord.RecordType.UPLOADED, 1));

  }

  @Test
  public void testGetMulti() {
    List<Integer> sequence_numbers = new ArrayList<>();
    Map<Integer, CastVoteRecord> expected = new HashMap<>();
    // We don't have any records yet
    assertEquals(expected, CastVoteRecordQueries.get(1L, CastVoteRecord.RecordType.UPLOADED, sequence_numbers));

    List<CVRContestInfo> contest_info = noisyContestSetup();

    CastVoteRecord cvr = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
            null,
            1L,
            1,
            1,
            1,
            "1",
            1,
            "1",
            "a",
            contest_info);
    Persistence.save(cvr);
    expected.put(1, cvr);
    sequence_numbers.add(1);
    assertEquals(expected, CastVoteRecordQueries.get(1L, CastVoteRecord.RecordType.UPLOADED, sequence_numbers));

    // Now we have two CVRs with the same county, recordType, and sequenceNumber
    CastVoteRecord second = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
            null,
            1L,
            1,
            2,
            1,
            "1",
            1,
            "1",
            "a",
            contest_info);
    Persistence.saveOrUpdate(second);

    sequence_numbers.add(2);
    expected.put(2, second);
    assertEquals(expected, CastVoteRecordQueries.get(1L, CastVoteRecord.RecordType.UPLOADED, sequence_numbers));
  }

  @Test
  public void testGetMultiDBError() {
    Query q = Persistence.currentSession().createNativeQuery("DROP TABLE cast_vote_record CASCADE");
    q.executeUpdate();
    assertNull(CastVoteRecordQueries.get(1L, CastVoteRecord.RecordType.UPLOADED, new ArrayList<>()));
  }

  @Test
  public void testGetIDs() {
    List<Long> ids = new ArrayList<>();
    List<CastVoteRecord> expected = new ArrayList<>();
    // We don't have any records yet
    assertEquals(expected, CastVoteRecordQueries.get(ids));

    // Make sure that a bad lookup also returns nothing
    ids.add(4L);
    assertEquals(expected, CastVoteRecordQueries.get(ids));
    ids.clear();

    List<CVRContestInfo> contest_info = noisyContestSetup();

    CastVoteRecord cvr = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
            null,
            1L,
            1,
            1,
            1,
            "1",
            1,
            "1",
            "a",
            contest_info);
    Persistence.save(cvr);
    expected.add(cvr);
    ids.add(cvr.id());
    assertEquals(expected, CastVoteRecordQueries.get(ids));

    // Now we have two CVRs with the same county, recordType, and sequenceNumber
    CastVoteRecord second = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
            null,
            1L,
            1,
            2,
            1,
            "1",
            1,
            "1",
            "a",
            contest_info);
    Persistence.saveOrUpdate(second);

    ids.add(second.id());
    expected.add(second);
    assertEquals(expected, CastVoteRecordQueries.get(ids));

  }
  @Test
  public void testGetIDsDBError() {
    Persistence.commitTransaction();
    List<Long> list = new ArrayList<>();
    list.add(1L);
    assertEquals(new ArrayList<>(), CastVoteRecordQueries.get(list));
  }

  @Test
  public void testAtPositionTribute() {
    Tribute tribute = new Tribute();
    tribute.countyId = 1L;
    tribute.scannerId = 1;
    tribute.batchId = "1";
    tribute.ballotPosition = 1;

    CastVoteRecord expected = noisyCVRSetup();

    assertEquals(expected, CastVoteRecordQueries.atPosition(tribute));
  }


  @Test
  public void testAtPositionTributes() {
    List<CVRContestInfo> contest_info = noisyContestSetup();
    Tribute tribute = new Tribute();
    tribute.countyId = 1L;
    tribute.scannerId = 1;
    tribute.batchId = "1";
    tribute.ballotPosition = 1;

    tribute.setUri();

    List<Tribute> tributes = new ArrayList<>();

    List<CastVoteRecord> expected = new ArrayList<>();
    assertEquals(expected, CastVoteRecordQueries.atPosition(tributes));

    tributes.add(tribute);
    CastVoteRecord cvr = noisyCVRSetup(1, contest_info);
    expected.add(cvr);

    assertEquals(expected, CastVoteRecordQueries.atPosition(tributes));

    // Create a fake tribute to cause a branch at line 504 in CastVoteRecordQueries.java
    // This causes a phantom to get created
    tribute = new Tribute();
    tribute.countyId = 1L;
    tribute.scannerId = 1;
    tribute.batchId = "1";
    tribute.ballotPosition = 5;

    tribute.setUri();
    tributes.add(tribute);

    CastVoteRecord phantom = CastVoteRecordQueries.phantomRecord(tribute);
    expected.add(phantom);
    assertEquals(CastVoteRecordQueries.atPosition(tributes), expected);
  }

  @Test
  public void testPositionAtLots() {
    List<CVRContestInfo> contest_info = noisyContestSetup();
    Tribute tribute;
    CastVoteRecord cvr;

    List<Tribute> tributes = new ArrayList<>();
    List<CastVoteRecord> expected = new ArrayList<>();

    // Now we need to test chunking, so add a whole lotta tributes
    // These are not the greatest CVRs in the world, they are just a tribute
    for (int i = 0; i < 2000; i++) {
      tribute = new Tribute();
      tribute.countyId = 1L;
      tribute.scannerId = 1;
      tribute.batchId = "1";
      tribute.ballotPosition = i;
      tribute.setUri();

      tributes.add(tribute);

      cvr = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
              null,
              1L,
              i,
              1,
              1,
              "1",
              i,
              "1",
              "a",
              contest_info);
      Persistence.save(cvr);
      expected.add(cvr);
    }

    assertEquals(CastVoteRecordQueries.atPosition(tributes), expected);
  }

  @Test
  public void testAtPositionRawData() {
    CastVoteRecord expected = noisyCVRSetup();
    assertEquals(expected, CastVoteRecordQueries.atPosition(1L, 1, "1", 1));

    // Make sure it creates a phantom if I ask for an invalid record:
    CastVoteRecord actual = CastVoteRecordQueries.atPosition(2L, 3, "blah", 9);
    expected = CastVoteRecordQueries.phantomRecord(2L, 3, "blah", 9);
    assertEquals(expected, actual);

  }


  @Test
  public void testPhantomRecordTribute() {
    Tribute tribute = new Tribute();
    tribute.countyId = 1L;
    tribute.scannerId = 1;
    tribute.batchId = "1";
    tribute.ballotPosition = 5;
    tribute.setUri();

    CastVoteRecord expected = new CastVoteRecord(CastVoteRecord.RecordType.PHANTOM_RECORD,
            null,
            1L,
            0,
            0,
            1,
            "1",
            5,
            "1-1-5",
            "PHANTOM RECORD",
            null);

    CastVoteRecord phantom = CastVoteRecordQueries.phantomRecord(tribute);
    assertEquals(phantom, expected);

  }

  @Test
  public void testPhantomRecordRawData() {
    CastVoteRecord expected = new CastVoteRecord(CastVoteRecord.RecordType.PHANTOM_RECORD,
            null,
            1L,
            0,
            0,
            1,
            "1",
            5,
            "1-1-5",
            "PHANTOM RECORD",
            null);

    CastVoteRecord phantom = CastVoteRecordQueries.phantomRecord(1L, 1, "1", 5);
    assertEquals(phantom, expected);
  }

  @Test
  public void testMaxRevisionDBError() {
    CastVoteRecord cvr = noisyCVRSetup();
    Query q = Persistence.currentSession().createNativeQuery("DROP TABLE cast_vote_record CASCADE");
    q.executeUpdate();
    assertEquals((Long)0L, CastVoteRecordQueries.maxRevision(cvr));
  }

  @Test
  public void testResultsReportBig() {
    List<CVRContestInfo> contest_info = noisyContestSetup();
    Tribute tribute;
    CastVoteRecord cvr;

    List<CastVoteRecord> expected = new ArrayList<>();

    List<CastVoteRecord> acvrs = new ArrayList();
    List<Long> contestCVRIds = new ArrayList();
    // Now we need to test chunking, so add a whole lotta tributes
    // These are not the greatest CVRs in the world, they are just a tribute
    for (int i = 0; i < 2000; i++) {
      cvr = new CastVoteRecord(CastVoteRecord.RecordType.UPLOADED,
              null,
              1L,
              i,
              1,
              1,
              "1",
              i,
              "1",
              "a",
              contest_info);

      Persistence.save(cvr);
      CastVoteRecord acvr = new CastVoteRecord(CastVoteRecord.RecordType.AUDITOR_ENTERED, Instant.now(),
              cvr.countyID(), cvr.cvrNumber(), null, cvr.scannerID(),
              cvr.batchID(), cvr.recordID(), cvr.imprintedID(),
              cvr.ballotType(), cvr.contestInfo());

      acvr.setComment("testing");
      acvr.setAuditBoardIndex(14);
      acvr.setCvrId(cvr.id());

      CVRAuditInfo cai = new CVRAuditInfo(cvr);
      cai.setACVR(acvr);

      Persistence.save(acvr);
      Persistence.save(cai);

      acvrs.add(acvr);
      contestCVRIds.add(cvr.id());
    }
    List<CastVoteRecord> result = CastVoteRecordQueries.resultsReport(contestCVRIds);
    assertEquals(acvrs, result);
  }
}

