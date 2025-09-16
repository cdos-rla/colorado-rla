package us.freeandfair.corla.model;

import java.io.IOException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.Period;
import java.util.Map;
import java.util.OptionalInt;
import java.util.TreeMap;
import java.util.Set;
import java.util.HashSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.testng.annotations.*;
import static org.testng.Assert.*;
import static us.freeandfair.corla.util.EqualsHashcodeHelper.nullableHashCode;

import us.freeandfair.corla.csv.ContestNameParser;
import us.freeandfair.corla.json.VersionExclusionStrategy;
import au.org.democracydevelopers.corla.util.TestClassWithDatabase;

public class AuditInfoTest extends TestClassWithDatabase {
  public Gson gson;

  @BeforeClass
  public void AuditInfoTest() {
    gson = new GsonBuilder().setPrettyPrinting().create();
  }

  @Test
  public void testCanonicalContests() {
    Map<String, Set<String>> contestMap = new TreeMap<String, Set<String>>();

    Set<String> contests = new HashSet<String>();
    contests.add("Hooligan Race");
    contests.add("Pole Paddle Pedal");
    contestMap.put("Chaffee", contests);

    AuditInfo subject =
      new AuditInfo("election type", Instant.now(), Instant.now(),
                    "12345678901234567890", new BigDecimal(0.05),
                    contestMap);

    assertEquals(subject.canonicalContests(), contestMap);
  }

  public RequestJSON contentsToUploadFile(final String json) {
    JsonParser parser = new JsonParser();
    JsonObject object = parser.parse(json).getAsJsonObject();
    RequestJSON f = gson.fromJson(object, RequestJSON.class);
    return f;
  }

  public String contestsFromJSON(final String json) {
    JsonParser parser;
    JsonObject object;

    try {
      parser = new JsonParser();
      object = parser.parse(json).getAsJsonObject();
      return object
        .getAsJsonArray("upload_file")
        .get(0)
        .getAsJsonObject()
        .get("contents")
        .getAsString();
    } catch (final Exception e) {
      System.out.println("JSON parse failure: " + e.getMessage());
      return "shark";
    }
  }

  @Test
  // @SuppressWarnings("PMD.DoNotUseThreads")
  // @SuppressFBWarnings(value = {"URF_UNREAD_FIELD"},
  //                   justification = "JSON blobs are big")
  public void testParsing() {
    final String json = "{\"election_date\":\"2018-07-31T06:00:00.000Z\",\"election_type\":\"coordinated\",\"public_meeting_date\":\"2018-08-07T06:00:00.000Z\",\"risk_limit\":0.05,\"upload_file\":[{\"preview\":\"blob:http://localhost:3000/54a4f865-9d2a-b442-881a-8630050977dc\",\"contents\":'\"CountyName\",\"ContestName\"\n\"Boulder\",\"Kombucha - DEM\"\n\"Boulder\",\"Kale - DEM\"\n\"Denver\",\"IPA - DEM\"\n\"Denver\",\"Porter - REP\"\n\"Chaffee\",\"Hooligan Race - DEM\"\n\"Chaffee\",\"Pole Pedal Paddle - DEM\"\n\"Chaffee\",\"Gunbarrel Challenge - REP\"\n'}]}";

    String contests = contestsFromJSON(json);

    assertEquals(contests,
                 "\"CountyName\",\"ContestName\"\n" +
                 "\"Boulder\",\"Kombucha - DEM\"\n" +
                 "\"Boulder\",\"Kale - DEM\"\n" +
                 "\"Denver\",\"IPA - DEM\"\n" +
                 "\"Denver\",\"Porter - REP\"\n" +
                 "\"Chaffee\",\"Hooligan Race - DEM\"\n" +
                 "\"Chaffee\",\"Pole Pedal Paddle - DEM\"\n" +
                 "\"Chaffee\",\"Gunbarrel Challenge - REP\"\n");

    try {
      ContestNameParser p = new ContestNameParser(contests);
      boolean successfulP = p.parse();
      assertEquals(p.contestCount(), OptionalInt.of(7));
    } catch (IOException ioe) { fail("Edge case", ioe); }
  }

  /**
   * A helper for writing nice JSON
   */
  public static class RequestJSON {
    private String contents = "";
    public String contents() { return contents; }
    public RequestJSON(String contents) { this.contents = contents; }
  }

  @Test
  public void testEmptyAuditInfo() {
    // Test an empty AuditInfo object
    AuditInfo ai = new AuditInfo();
    assertNull(ai.electionDate());
    assertNull(ai.electionType());
    assertNull(ai.seed());
    assertNull(ai.publicMeetingDate());
    assertNull(ai.riskLimit());

    Map<String, Set<String>> map = new TreeMap<>();
    assertEquals(ai.canonicalContests(), map);
    assertEquals(ai.getCanonicalChoices(), map);
    assertEquals(ai.hashCode(), nullableHashCode(null));

    assert(ai.equals(ai));
    assertFalse(ai.equals(""));

    assertEquals(ai.toString(),"AuditInfo[canonicalContests=" + map +
            ", electionType=null" +
            ", electionDate=null" +
            ", publicMeetingDate=null" +
            ", seed=null" +
            ", riskLimitnull");

    assertNull(ai.capitalizedElectionType());
  }

  @Test
  public void testAuditInfoNoContests() {
    String electionType = "electionType";
    Instant electionDate = Instant.now();
    Instant publicMeetingDate = Instant.now();

    String seed = "1";
    BigDecimal riskLimit = new BigDecimal(0.05);
    AuditInfo ai = new AuditInfo(electionType, electionDate, publicMeetingDate, seed, riskLimit);

    assertEquals(ai.electionType(), electionType);
    assertEquals(ai.electionDate(), electionDate);
    assertEquals(ai.publicMeetingDate(), publicMeetingDate);
    assertEquals(ai.seed(), seed);
    assertEquals(ai.riskLimit(), riskLimit);

    Map<String, Set<String>> map = new TreeMap<>();
    assertEquals(ai.canonicalContests(), map);
    assertEquals(ai.getCanonicalChoices(), map);
    assertEquals(ai.hashCode(), nullableHashCode(seed));

    assert(ai.equals(ai));
    assertFalse(ai.equals(""));
    assertEquals(ai.capitalizedElectionType(), "Electiontype");

    AuditInfo other = new AuditInfo("a", electionDate, publicMeetingDate, seed, riskLimit);
    assertFalse(ai.equals(other));
    ai.updateFrom(other);
    assertTrue(ai.equals(other));

    assertEquals(ai.capitalizedElectionType(), "A");

  }


  @Test
  public void testUpdateFromOther() {
    String electionType = "electionType";
    Instant electionDate = Instant.now();
    Instant publicMeetingDate = Instant.now();

    String seed = "1";
    BigDecimal riskLimit = new BigDecimal(0.05);
    AuditInfo ai = new AuditInfo(electionType, electionDate, publicMeetingDate, seed, riskLimit);

    AuditInfo other = new AuditInfo("a", electionDate, publicMeetingDate, seed, riskLimit);
    assertFalse(ai.equals(other));
    ai.updateFrom(other);
    assertTrue(ai.equals(other));

    Instant otherElectionDate = electionDate.plusSeconds(1);
    other = new AuditInfo(electionType, otherElectionDate, publicMeetingDate, seed, riskLimit);
    assertFalse(ai.equals(other));
    ai.updateFrom(other);
    assertTrue(ai.equals(other));

    Instant otherPublicMeetingDate = publicMeetingDate.minusSeconds(1);
    assert(otherPublicMeetingDate != publicMeetingDate);
    other = new AuditInfo(electionType, electionDate, otherPublicMeetingDate, seed, riskLimit);
    assertFalse(ai.equals(other));

    ai.updateFrom(other);
    assertTrue(ai.equals(other));

    String otherSeed = "otherSeed";
    other = new AuditInfo(electionType, electionDate, publicMeetingDate, otherSeed, riskLimit);
    assertFalse(ai.equals(other));
    ai.updateFrom(other);
    assertTrue(ai.equals(other));

    BigDecimal otherRiskLimit = new BigDecimal(0.10);
    other = new AuditInfo(electionType, electionDate, publicMeetingDate, seed, otherRiskLimit);
    assertFalse(ai.equals(other));
    ai.updateFrom(other);
    assertTrue(ai.equals(other));

    Map<String, Set<String>> map = new TreeMap<>();
    Set<String> contest = new HashSet<>();
    contest.add("Contest 1");
    map.put("Contest", contest);
    other = new AuditInfo(electionType, electionDate, publicMeetingDate, seed, riskLimit, map);
    assertFalse(ai.equals(other));
    ai.updateFrom(other);
    assertTrue(ai.equals(other));

    AuditInfo empty = new AuditInfo();
    assertNotEquals(empty, other);
    other.updateFrom(empty);
    assertNotEquals(empty, other);

    empty.updateFrom(other);
    assertEquals(empty, other);

    other.setCanonicalChoices(map);
    ai.updateFrom(other);
    assertEquals(ai, other);
  }

  @Test
  public void testCanonicalContest() {

    String electionType = "electionType";
    Instant electionDate = Instant.now();
    Instant publicMeetingDate = Instant.now();

    String seed = "1";
    BigDecimal riskLimit = new BigDecimal(0.05);
    AuditInfo ai = new AuditInfo(electionType, electionDate, publicMeetingDate, seed, riskLimit);

    Map<String, Set<String>> map = new TreeMap<>();
    Set<String> contest = new HashSet<>();
    contest.add("Contest 1");
    map.put("Contest", contest);
    ai.setCanonicalContests(map);
    ai.setCanonicalChoices(map);

    assertEquals(ai.canonicalContests(), ai.getCanonicalChoices());
  }
}
