
package us.freeandfair.corla.controller;

import java.io.OutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import us.freeandfair.corla.report.WorkbookWriter;
import us.freeandfair.corla.report.ReportRows;
import us.freeandfair.corla.report.StateReport;
import us.freeandfair.corla.model.DoSDashboard;
import us.freeandfair.corla.persistence.Persistence;
import us.freeandfair.corla.query.ExportQueries;

import static au.org.democracydevelopers.corla.endpoint.GetAssertions.CSV_SUFFIX;
import static au.org.democracydevelopers.corla.endpoint.GetAssertions.JSON_SUFFIX;
import static au.org.democracydevelopers.corla.endpoint.GetAssertions.getAssertions;

/**
 * Find the data for a report and format it to be rendered into a presentation
 * format elsewhere
 **/
public final class AuditReport {

  /**
   * Class-wide logger
   */
  public static final Logger LOGGER = LogManager.getLogger(AuditReport.class);
  private static Pattern p = Pattern.compile(".* (\\w+) County$");

  /** no instantiation **/
  private AuditReport() {
  }

  /**
   * Generate a report file and return the bytes activity: a log of acvr
   * submissions for a particular Contest (includes all participating counties)
   * activity-all: same as above for all targeted contests results: the acvr
   * submissions for each random number that was generated (to audit this
   * program's calculations) results-all: same as above for all targeted
   * contests
   *
   * Here are the specific differences: - the Activity report is sorted by
   * timestamp, the Audit Report by random number sequence - the Activity report
   * shows previous revisions, the Audit Report does not - the Result Report
   * shows the random number that was generated for the CVR (and the position),
   * the Activity Report does not - the Result Report shows
   * duplicates(multiplicity), the Activity Report does not
   *
   * contestName is optional if reportType is *-all
   **/
  public static byte[] generate(final String contentType, final String reportType,
                                final String contestName)
      throws IOException {
    // xlsx
    final WorkbookWriter writer = new WorkbookWriter();
    List<List<String>> rows;
    DoSDashboard dosdb;
    dosdb = Persistence.getByID(DoSDashboard.ID, DoSDashboard.class);
    Map<String, String> contestMap = createUniqueSheetNames(dosdb.targetedContestNames());
 
    switch (reportType) {
      case "activity":
        rows = ReportRows.getContestActivity(contestName);
        writer.addSheet(contestMap.get(contestName), rows);
        break;
      case "activity-all":
        writer.addSheet("Contests to Sheets", getMappingSheet(contestMap));
        for (final Entry<String, String> entry : contestMap.entrySet()) {
          writer.addSheet(entry.getValue(), ReportRows.getContestActivity(entry.getKey()));
        }
        break;
      case "results":
        rows = ReportRows.getResultsReport(contestName);
        writer.addSheet(contestMap.get(contestName), rows);
        break;
      case "results-all":
        writer.addSheet("Contests to Sheets", getMappingSheet(contestMap));
        writer.addSheet("Summary", ReportRows.genSumResultsReport());
        for (final Entry<String, String> entry : contestMap.entrySet()) {
          writer.addSheet(entry.getValue(), ReportRows.getResultsReport(entry.getKey()));
        }
        break;
      default:
        LOGGER.error("invalid reportType: " + reportType);
        break;

    }

    return writer.write();
  }
  
  public static List<List<String>> getMappingSheet(Map<String,String> contestMap) {
    final List<List<String>> rows = new ArrayList<>();
    rows.add(Arrays.asList("Contest Name","Sheet Name"));
    for (Entry<String, String> pair : contestMap.entrySet()) {
      rows.add(Arrays.asList(pair.getKey(), pair.getValue()));
    }
    return rows ;

  }
  
  public static String getFirst31Characters(String inName) {
    inName = inName.replace("the","");
    inName = inName.replace("of","");
    inName = inName.replace("and","");
    inName = inName.replace("to","");
    inName = inName.replace("Judicial District","");
    inName = inName.replace("Congressional District","");
    inName = inName.replace("Congress-District","");
    inName = inName.replace("  "," ");
    inName = inName.replace("  "," ");
    String first31 = inName.length() > 31 ? inName.substring(0, 30) : inName ;
    String newUniqueName = getUniqueNameUsingCounty(inName, first31);
    return newUniqueName.replaceAll("\\s+$", "");
  }
  
  private static String getUniqueName(String inUniqueName) {
    char lastChar = inUniqueName.charAt(inUniqueName.length()-1) ;
    if (lastChar >= 'a' && lastChar <= 'z') {
      lastChar++;
      return inUniqueName.substring(0, inUniqueName.length()-1) + lastChar;
    }
    return inUniqueName.substring(0, inUniqueName.length()-1) + 'a';
  }
  
  private static String getUniqueNameUsingCounty(String originalName, String inUniqueName) {
    Matcher m = p.matcher(originalName) ; 
    if ( m.matches()) {
        String countyName = m.group(1);
        return inUniqueName.substring(0, inUniqueName.length()-countyName.length()-2) + " " + countyName; 
    }
    return inUniqueName ;
  }
  
  private static Map<String, String> createUniqueSheetNames(Set<String> contestNames) {
    Map<String, String> uniqueNames = new HashMap<>(contestNames.size());
    for (String contestName : contestNames) {
      String newUniqueName = getFirst31Characters(contestName);

      //TRK 279 2021 practice period, dowload audit report hangs
//      for (Map.Entry me : uniqueNames.entrySet()) {
//        LOGGER.info("Key: "+me.getKey() + " & Value: " + me.getValue());
//      }
      while (uniqueNames.containsKey(newUniqueName)) {
        newUniqueName = getUniqueName(newUniqueName);
      }
      uniqueNames.put(newUniqueName, contestName);
    }
    return
       uniqueNames.entrySet()
           .stream()
           .sorted(Map.Entry.comparingByValue())
           .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey,(oldValue, newValue) -> oldValue, LinkedHashMap::new))
           ;
  }

  /** all the reports in one "package" **/
  public static void generateZip(final OutputStream os, List<String> selectedReports) {
    final ZipOutputStream zos = new ZipOutputStream(os);

    try {
      final Map<String, String> files = ExportQueries.sqlFiles();

      for (String reportName : selectedReports) {
        for (final Map.Entry<String, String> entry : files.entrySet()) {
          String name = entry.getKey();
          if (name.equals(reportName)){
            final String filename = name + ".csv";
            final ZipEntry zipEntry = new ZipEntry(filename);
            zos.putNextEntry(zipEntry);
            ExportQueries.csvOut(entry.getValue(), zos);
            zos.closeEntry();
          }
        }

        if ("JSON".equalsIgnoreCase(reportName)) {
          for (final Map.Entry<String, String> entry : files.entrySet()) {
            final String filename = entry.getKey() + ".json";
            final ZipEntry zipEntry = new ZipEntry(filename);
            zos.putNextEntry(zipEntry);
            ExportQueries.jsonOut(entry.getValue(), zos);
            zos.closeEntry();
          }
        }

        if ("ActivityReport".equalsIgnoreCase(reportName)) {
          zos.putNextEntry(new ZipEntry("ActivityReport.xlsx"));
          zos.write(generate("xlsx", "activity-all", null));
          zos.closeEntry();
        }

        if ("ResultReport".equalsIgnoreCase(reportName)) {
          zos.putNextEntry(new ZipEntry("ResultsReport.xlsx"));
          zos.write(generate("xlsx", "results-all", null));
          zos.closeEntry();
        }

        if ("StateReport".equalsIgnoreCase(reportName)) {
          final StateReport sr = new StateReport();
          zos.putNextEntry(new ZipEntry(sr.filenameExcel()));
          zos.write(sr.generateExcel());
          zos.closeEntry();
        }

        // These can throw either IOExceptions or InterruptedExceptions, if the http call to raire
        // fails or is interrupted.
        if ("assertions_json".equalsIgnoreCase(reportName)) {
          getAssertions(zos, "assertions-json", JSON_SUFFIX);
        }

        if ("assertions_csv".equalsIgnoreCase(reportName)) {
          getAssertions(zos, "assertions-csv", CSV_SUFFIX);
        }
      }
    } catch (IOException | InterruptedException e) {
      LOGGER.error(e.getMessage());
    } finally {
      try {
        zos.close();
      } catch (IOException e) {
        LOGGER.warn(String.format("Cannot close stream: %s", e.getMessage()));
      }
    }
  }
}
