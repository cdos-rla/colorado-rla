
package us.freeandfair.corla.query;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import java.util.*;
import java.util.stream.Stream;

import au.org.democracydevelopers.corla.model.ContestType;
import au.org.democracydevelopers.corla.model.GenerateAssertionsSummary;
import au.org.democracydevelopers.corla.model.IRVComparisonAudit;
import au.org.democracydevelopers.corla.query.GenerateAssertionsSummaryQueries;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import org.postgresql.copy.CopyManager;
import org.postgresql.copy.CopyOut;
import org.postgresql.core.BaseConnection;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import java.sql.Connection;
import java.sql.SQLException;

import org.hibernate.query.Query;

import us.freeandfair.corla.model.ComparisonAudit;
import us.freeandfair.corla.model.Contest;
import us.freeandfair.corla.model.ContestResult;
import us.freeandfair.corla.persistence.Persistence;

/** export queries **/
public class ExportQueries {

  /**
   * Class-wide logger
   */
  public static final Logger LOGGER = LogManager.getLogger(ExportQueries.class);

  /** to use the hibernate jdbc connection **/
  public static class CSVWork implements Work {

    /** pg query string **/
    private final String query;

    /** where to send the csv data **/
    private final OutputStream os;

    /** instantiation **/
    public CSVWork(final String query, final OutputStream os) {

      this.query = query;
      this.os = os;
    }

    /** do the work **/
    @SuppressWarnings("PMD.PreserveStackTrace")
    public void execute(final Connection conn) throws java.sql.SQLException {
      try {
        final CopyManager cm = new CopyManager(conn.unwrap(BaseConnection.class));
        final String q = String.format("COPY (%s) TO STDOUT WITH CSV HEADER", this.query);
        // cm.copyOut(q, this.os);
        custCopyOut(q, this.os, cm);
      } catch (java.io.IOException e) {
        throw new java.sql.SQLException(e.getMessage());
      }
    }
  }

  /** no instantiation **/
  private ExportQueries() {
  };

  /**
   * write the resulting rows from the query, as json objects, to the
   * OutputStream
   **/
  public static void customOut(final String query, final OutputStream os) {
    final Session s = Persistence.currentSession();
    final String withoutSemi = query.replace(";", "");

    final String jsonQuery =
        String.format("SELECT cast(row_to_json(r) as text)" + " FROM (%s) r", withoutSemi);
    final Query q = s.createNativeQuery(jsonQuery).setReadOnly(true).setFetchSize(1000);

    // interleave an object separator (the comma and line break) into the stream
    // of json objects to create valid json thx!
    // https://stackoverflow.com/a/25624818
    final Stream<Object[]> results =
        q.stream().flatMap(i -> Stream.of(new String[] {",\n"}, i)).skip(1); // remove
                                                                             // the
                                                                             // first
                                                                             // separator

    // write json by hand to preserve streaming writes in case of big data
    try {
      os.write("[".getBytes(StandardCharsets.UTF_8));
      results.forEach(line -> {
        try {
          // the object array is the columns, but in this case there is only
          // one, so we take it at index 0
          os.write(line[0].toString().getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
          LOGGER.error(e.getMessage());
        }
      });

      os.write("]".getBytes(StandardCharsets.UTF_8));
    } catch (java.io.IOException e) {
      // log it
      LOGGER.error(e.getMessage());
    }
  }

  /**
   * write the resulting rows from the query, as json objects, to the
   * OutputStream
   **/
  public static void jsonOut(final String query, final OutputStream os) {
    final Session s = Persistence.currentSession();

    // Make sure the contest_result table has the right info for IRV.
    updateIRVContestResults(s);

    final String withoutSemi = query.replace(";", "");
    final String jsonQuery =
        String.format("SELECT cast(row_to_json(r) as text)" + " FROM (%s) r", withoutSemi);
    final Query q = s.createNativeQuery(jsonQuery).setReadOnly(true).setFetchSize(1000);

    // interleave an object separator (the comma and line break) into the stream
    // of json objects to create valid json thx!
    // https://stackoverflow.com/a/25624818
    // call .skip() to remove the first separator
    final Stream<Object[]> results =
            q.stream().flatMap(i -> Stream.of(",\n", i)).skip(1);

    try {
      os.write("[".getBytes(StandardCharsets.UTF_8));

      for(final Object line : results.toArray()) {
        try {
          os.write(line.toString().getBytes(StandardCharsets.UTF_8));
        } catch (java.io.IOException e) {
          LOGGER.error(e.getMessage());
        }
      }

      os.write("]".getBytes(StandardCharsets.UTF_8));
    } catch (java.io.IOException e) {
      // log it
      LOGGER.error(e.getMessage());
    }
  }

  /** send query results to output stream as csv **/
  public static void csvOut(final String query, final OutputStream os) {
    final Session s = Persistence.currentSession();

    // Make sure the contest_result table has the right info for IRV.
    updateIRVContestResults(s);

    final String withoutSemi = query.replace(";", "");
    s.doWork(new CSVWork(withoutSemi, os));
  }

  /**
   * The directory listing of the sql resource directory on the classpath,
   * hopefully! I couldn't figure out how to do this from within a deployed jar,
   * so here we are
   **/
  public static List<String> getSqlFolderFiles() {
    final List<String> paths = new ArrayList<String>();
    final String folder = "sql";
    final String[] fileNames = {"batch_count_comparison.sql", "contest.sql",
        "contest_comparison.sql", "contest_selection.sql", "contests_by_county.sql",
        "tabulate_plurality.sql", "tabulate_county_plurality.sql", "upload_status.sql", "seed.sql",
        "summarize_IRV.sql", "ranked_ballot_interpretation.sql"};
    for (final String f : fileNames) {
      paths.add(String.format("%s/%s", folder, f));
    }
    return paths;
  }

  /** remove path and ext leaving the file name **/
  private static String fileName(final String path) {
    final int slash = path.lastIndexOf('/') + 1;
    final int dot = path.lastIndexOf('.');
    return path.substring(slash, dot);
  }

  /** file contents to string **/
  // I respectfully disagree
  @SuppressWarnings({"PMD.AssignmentInOperand"})
  public static String fileContents(final String path) throws java.io.IOException {

    final StringBuilder contents = new StringBuilder();
    final ClassLoader loader = Thread.currentThread().getContextClassLoader();
    try (final InputStream is = loader.getResourceAsStream(path);
        final InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
        final BufferedReader br = new BufferedReader(isr);) {
      String line;
      while ((line = br.readLine()) != null) {
        contents.append(line);
        contents.append('\n');
      }
    }
    return contents.toString();
  }

  /**
   * read files from resources/sql/ and return map with keys as file names
   * without extension and value as the file contents
   **/
  public static Map<String, String> sqlFiles() throws java.io.IOException {
    final Map files = new HashMap<String, String>();
    List<String> paths = getSqlFolderFiles();
    for (final String path : paths) {
      if (path.endsWith(".sql")) {
        files.put(fileName(path), fileContents(path));
      }
    }
    return files;
  }

  public static long custCopyOut(final String sql, OutputStream to, CopyManager cm)
      throws SQLException, IOException {
    byte[] buf;
    CopyOut cp = cm.copyOut(sql);
    try {
      while ((buf = cp.readFromCopy()) != null) {
        String s = new String(buf,Charset.forName("ASCII"));
        if (s.contains("\\\"")) {
          List<Byte> newBuf = new ArrayList<>();
          for (int i = 0; i < buf.length; i++) {
            byte b1 = buf[i];
            Character ch = (char) b1;
            if (!ch.equals('\\')) {
              newBuf.add(b1);
            }
          }
          byte[] result = new byte[newBuf.size()];
          for (int i = 0; i < newBuf.size(); i++) {
            result[i] = newBuf.get(i).byteValue();
          }
          to.write(result);
        } else {
          List<Byte> newBuf = new ArrayList<>();
          for (int i = 0; i < buf.length; i++) {
            byte b1 = buf[i];
            newBuf.add(b1);
          }
          byte[] result = new byte[newBuf.size()];
          for (int i = 0; i < newBuf.size(); i++) {
            result[i] = newBuf.get(i).byteValue();
          }
          to.write(result);
        }
      }
      return cp.getHandledRowCount();
    } catch (IOException ioEX) {
      // if not handled this way the close call will hang, at least in 8.2
      if (cp.isActive()) {
        cp.cancelCopy();
      }
      try { // read until exhausted or operation cancelled SQLException
        while ((buf = cp.readFromCopy()) != null) {
        }
      } catch (SQLException sqlEx) {
      } // typically after several kB
      throw ioEX;
    } finally { // see to it that we do not leave the connection locked
      if (cp.isActive()) {
        cp.cancelCopy();
      }
    }
  }

  /**
   * This function deals, somewhat inelegantly, with the problem that the ContestResult data structure
   * used in most queries does not have correct values for things like winners, losers, margin, and
   * diluted margin for IRV contests. This function sets them manually from the
   * GenerateAssertionsSummary table, then flushes the database so that the csv reports, which are
   * based on database queries, get the right values from the contest_result table.
   * @param s The current Hibernate session.
   */
  private static void updateIRVContestResults(final Session s) {
    final String prefix = "[updateIRVContestResults]";
    LOGGER.debug(String.format("%s %s.", prefix,
        "Updating IRV contest results from generate assertions summary"));

    final List<ComparisonAudit> comparisonAudits = ComparisonAuditQueries.sortedList();

    for(final ComparisonAudit ca : comparisonAudits) {
      if(ca instanceof IRVComparisonAudit) {
        final Set<String> choices = new HashSet<>();
        // Get the choices for the contest. These should be the same for all the contests, but
        // gather the whole set from all of them just in case.
        for(final Contest contest : ca.contestResult().getContests()) {
          if (contest.description().equals(ContestType.IRV.toString())) {
            contest.choices().stream().map(ch -> choices.add(ch.name()));
          } else {
            // We have an IRVComparisonAudit for a not-IRV contest. Definitely not supposed to happen.
            final String msg = "IRV-type Comparison Audit encountered for non-IRV contest";
            LOGGER.error(String.format("%s %s %s", prefix, msg, contest.name()));
            throw new RuntimeException(msg+" "+contest.name());
          }
        }

        final ContestResult contestResult = ca.contestResult();

        // Use the choices and the summary to update the contest result in the database.
        final Optional<GenerateAssertionsSummary> summary
            = GenerateAssertionsSummaryQueries.matching(ca.getContestName());
        if(summary.isPresent()) {
          final String winner = summary.get().getWinner();
          contestResult.setWinners(Set.of(winner));
          choices.remove(winner);
          contestResult.setLosers(choices);
          contestResult.setMinMargin(((IRVComparisonAudit) ca).getMinMargin());
          contestResult.setDilutedMargin(ca.getDilutedMargin());
        } else {
          // If no summary is present, just set the winner to be blank, the losers to be everyone,
          // and the margins to be zero.
          LOGGER.debug(String.format("%s %s %s", prefix, "Couldn't find summary for IRV contest",
              ca.getContestName()));
          contestResult.setWinners(Set.of());
          contestResult.setLosers(choices);
          contestResult.setMinMargin(0);
          contestResult.setDilutedMargin(BigDecimal.ZERO);
        }
      }
    }

    s.flush();

  }
}
