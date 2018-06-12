package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.RunDtoPosition;
import ca.on.oicr.ws.dto.RunDtoSample;
import ca.on.oicr.ws.dto.SampleDto;

public class GazpachoProjectStatusReport extends TableReport {

  private static class ReportItem implements Comparable<ReportItem> {

    private final SampleDto stock;
    private final String externalName;
    private final Set<String> groupIds = new HashSet<>();
    private final Set<SampleDto> aliquots = new HashSet<>();
    private final Set<SampleDto> libraries = new HashSet<>();
    private final Set<RunDto> runs = new HashSet<>();
    private final Map<Integer, List<Integer>> lanesForRun = new HashMap<>();

    public ReportItem(SampleDto stock, String externalName) {
      this.stock = stock;
      this.externalName = externalName;
    }

    public void addAliquot(SampleDto aliquot) {
      this.aliquots.add(aliquot);
    }

    public void addLibrary(SampleDto library, String groupId) {
      this.libraries.add(library);
      if (groupId != null) groupIds.add(groupId);
    }

    public void addRunLane(RunDto run, RunDtoPosition lane) {
      this.runs.add(run);
      List<Integer> lanesForCurrentRun = lanesForRun.get(run.getId());
      if (lanesForCurrentRun == null) {
        List<Integer> lanes = new ArrayList<>();
        lanes.add(lane.getPosition());
        lanesForRun.put(run.getId(), lanes);
      } else {
        lanesForRun.get(run.getId()).add(lane.getPosition());
      }
    }

    public SampleDto getStock() {
      return stock;
    }

    public Set<SampleDto> getAliquots() {
      return aliquots;
    }

    public String getWaitingAliquots() {
      Date receivedByLab;
      try {
        receivedByLab = getDateTimeFormat().parse(stock.getCreatedDate());
      } catch (java.text.ParseException e) {
        System.out.println("Error parsing created date for stock " + stock.getId());
        return "";
      }
      if (aliquots.isEmpty() && receivedByLab.toInstant().isBefore(TWO_DAYS_AGO)) {
        return "1";
      } else {
        return "";
      }
    }

    public String getWaitingLibraries() {
      Date receivedByLab;
      SampleDto mostRecentAliquot = aliquots.stream().max(Comparator.comparing(SampleDto::getCreatedDate)).orElse(null);
      if (mostRecentAliquot == null) return "";
      try {
        receivedByLab = getDateTimeFormat().parse(mostRecentAliquot.getCreatedDate());
      } catch (java.text.ParseException e) {
        System.out.println("Error parsing created date for aliquot " + mostRecentAliquot.getId());
        return "";
      }
      if (getLibraries().isEmpty() && receivedByLab.toInstant().isBefore(TWO_DAYS_AGO)) {
        return "1";
      } else {
        return "";
      }
    }

    public String getExternalName() {
      return externalName;
    }

    public String getGroupIds() {
      String joined;
      if (libraries.isEmpty()) {
        joined = getAttribute(ATTR_GROUP_ID, stock);
      } else {
        joined = String.join(",", groupIds);
      }
      return (joined == null ? "" : joined);
    }

    public Set<SampleDto> getLibraries() {
      return libraries;
    }

    public String getWaitingSequencing() {
      if (getLibraries().isEmpty()) return "";
      Date maxLibCreated = new Date();
      try {
        maxLibCreated = getDateTimeFormat().parse(getLibraries().stream()
            .max(Comparator.comparing(SampleDto::getCreatedDate))
            .map(SampleDto::getCreatedDate)
            .orElse(new Date().toString()));
      } catch (java.text.ParseException e) {
        System.out.println("Error parsing created date for libraries for sample " + stock.getId());
      }
      if (runs.isEmpty() && maxLibCreated.toInstant().isBefore(TWO_DAYS_AGO)) {
        return Integer.toString(libraries.size());
      } else {
        return "";
      }
    }

    public String getNumLanesRunning() {
      Set<RunDto> running = runs.stream().filter(run -> "Running".equals(run.getState())).collect(Collectors.toSet());
      Integer lanesRunning = 0;
      for (Integer runId : lanesForRun.keySet()) {
        if (running.stream().map(run -> run.getId()).collect(Collectors.toSet()).contains(runId)) {
          lanesRunning += lanesForRun.get(runId).size();
        }
      }
      return lanesRunning.equals(0) ? "" : lanesRunning.toString();
    }

    public String getNumLanesCompleted() {
      Set<RunDto> completed = runs.stream().filter(run -> "Completed".equals(run.getState())).collect(Collectors.toSet());
      Integer lanesCompleted = 0;
      for (Integer runId : lanesForRun.keySet()) {
        if (completed.stream().map(run -> run.getId()).collect(Collectors.toSet()).contains(runId)) {
          lanesCompleted += lanesForRun.get(runId).size();
        }
      }
      return lanesCompleted.equals(0) ? "" : lanesCompleted.toString();
    }

    public String getDaysInLab(String end) {
      Date receivedByLab;
      Date runCompleted;
      Instant reportingEnd;
      try {
        receivedByLab = getDateTimeFormat().parse(stock.getCreatedDate());
        if (end != null) {
          reportingEnd = getDateTimeFormat().parse(end).toInstant();
        } else {
          reportingEnd = TODAY;
        }
      } catch (java.text.ParseException e) {
        System.out.println("Error parsing created date for stock " + stock.getId());
        return "";
      }
      long daysSinceReceived = Duration.between(receivedByLab.toInstant(), reportingEnd).toDays();
      if (runs.isEmpty() || !runs.stream().filter(run -> "Running".equals(run.getState())).collect(Collectors.toList()).isEmpty()) {
        return Long.toString(daysSinceReceived);
      } else {
        RunDto mostRecent = runs.stream().max(Comparator.comparing(RunDto::getCompletionDate)).orElse(null);
        if (mostRecent == null) return Long.toString(daysSinceReceived);
        try {
          runCompleted = getDateTimeFormat().parse(mostRecent.getCompletionDate());
        } catch (java.text.ParseException e) {
          System.out.println("Error parsing completion date for run " + mostRecent.getId());
          return "";
        }
        long daysReceivedToCompleted = Duration.between(receivedByLab.toInstant(), runCompleted.toInstant()).toDays();
        return Long.toString(daysReceivedToCompleted);
      }
    }

    @Override
    public int compareTo(ReportItem other) {
      return this.getStock().getName().compareTo(other.getStock().getName());
    }
  }

  private static final Instant TODAY = ZonedDateTime.now().toInstant();
  private static final Instant TWO_DAYS_AGO = ZonedDateTime.now().minusDays(2).toInstant();

  private String project;
  private String start;
  private String end;
  private String analyte;
  private final String todate = removeTime(TODAY.toString());

  private static final Option OPT_PROJECT = CommonOptions.project(true);
  private static final Option OPT_AFTER = CommonOptions.after(true);
  private static final Option OPT_BEFORE = CommonOptions.before(true);
  private static final Option OPT_ANALYTE = CommonOptions.analyte(false);
  public static final String REPORT_NAME = "project-status";

  // keep track of the base stock name for all samples/libraries/runs active this month
  Map<String, ReportItem> dnaThisMonth = Maps.newTreeMap();
  Map<String, ReportItem> rnaThisMonth = Maps.newTreeMap();
  Map<String, ReportItem> allDna = Maps.newTreeMap();
  Map<String, ReportItem> allRna = Maps.newTreeMap();
  List<Map.Entry<String, ReportItem>> dnaRecentList;
  List<Map.Entry<String, ReportItem>> rnaRecentList;
  List<Map.Entry<String, ReportItem>> dnaAllList;
  List<Map.Entry<String, ReportItem>> rnaAllList;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_PROJECT, OPT_AFTER, OPT_BEFORE, OPT_ANALYTE);
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    if (cmd.hasOption(OPT_AFTER.getLongOpt())) {
      String after = cmd.getOptionValue(OPT_AFTER.getLongOpt());
      if (!after.matches(DATE_REGEX)) {
        throw new ParseException("After date must be in format yyyy-mm-dd");
      }
      this.start = after;
    }

    if (cmd.hasOption(OPT_BEFORE.getLongOpt())) {
      String before = cmd.getOptionValue(OPT_BEFORE.getLongOpt());
      if (!before.matches(DATE_REGEX)) {
        throw new ParseException("Before date must be in format yyyy-mm-dd");
      }
      this.end = before;
    }

    if (cmd.hasOption(OPT_ANALYTE.getLongOpt())) {
      String analyte = cmd.getOptionValue(OPT_ANALYTE.getLongOpt());
      if (!DNA.equals(analyte) && !RNA.equals(analyte)) {
        throw new ParseException("Analyte must be DNA or RNA");
      }
      this.analyte = analyte;
    }

    this.project = cmd.getOptionValue(OPT_PROJECT.getLongOpt());
  }

  @Override
  public String getTitle() {
    return "Project Status Report (Gazpacho) for project " + project
        + (analyte == null ? "" : (DNA.equals(analyte) ? " DNA" : " RNA"))
        + " for "
        + (start == null ? "Any Time" : start)
        + " - "
        + (end == null ? "Now" : end)
        + ". Generated " + new SimpleDateFormat(DATE_FORMAT).format(new Date());
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> allSamples = pinery.getSample().all();
    List<SampleDto> allProjectSamples = allSamples.stream().filter(byProject(project)).collect(Collectors.toList());
    Map<String, SampleDto> allSamplesById = mapSamplesById(allSamples); // separate since hierarchy may include multiple projects
    List<RunDto> allRuns = pinery.getSequencerRun().all();
    // Filtering now helps with pulling data from a historic point in time
    allProjectSamples = allProjectSamples.stream().filter(byCreatedBetween(null, end)).collect(Collectors.toList());
    allRuns = allRuns.stream().filter(byCreatedBefore(end)).collect(Collectors.toList());

    // First, go through and categorize all stocks.
    // We do this in a separate step so that stocks can form the root of the Report Item,
    // and libraries and sequencing can be added to them.
    for (SampleDto sample : allProjectSamples) {
      if (sample.getSampleType().contains("Library")) continue;
      SampleDto aliquot = null;
      SampleDto stock;
      if (SAMPLE_CATEGORY_ALIQUOT.equals(getAttribute(ATTR_CATEGORY, sample))) {
        aliquot = sample;
        stock = getParent(aliquot, SAMPLE_CATEGORY_STOCK, allSamplesById);
      } else {
        stock = sample;
      }
      if (SAMPLE_CATEGORY_STOCK.equals(getAttribute(ATTR_CATEGORY, stock))) {
        SampleDto identity = getParent(stock, SAMPLE_CATEGORY_IDENTITY, allSamplesById);
        String externalName = getAttribute(ATTR_EXTERNAL_NAME, identity);
        if (hasDnaAnalyteName(stock)) {
          ReportItem stockItem = allDna.get(stock.getId());
          if (stockItem == null) {
            stockItem = new ReportItem(stock, externalName);
          }
          if (aliquot != null) stockItem.addAliquot(aliquot);
          addReportItem(stockItem, dnaThisMonth, allDna);
        } else {
          ReportItem stockItem = allRna.get(stock.getId());
          if (stockItem == null) {
            stockItem = new ReportItem(stock, externalName);
          }
          if (aliquot != null) stockItem.addAliquot(aliquot);
          addReportItem(stockItem, rnaThisMonth, allRna);
        } // else don't care
      }
    }



    // Then go through and categorize all libraries
    for (SampleDto library : allProjectSamples) {
      String type = library.getSampleType();

      if (type != null && type.contains(LIBRARY) && !type.contains(" Seq") && project.equals(library.getProjectName())) {
        SampleDto stock = getParent(library, SAMPLE_CATEGORY_STOCK, allSamplesById);
        ReportItem match;
        if (hasDnaAnalyteName(stock)) {

          match = addLibToReportItem(stock, library, allDna, allSamplesById);
          if (isCreatedBetween(library, start, end)) {
            dnaThisMonth.put(stock.getId(), match);
          }
          allDna.put(stock.getId(), match);
        } else {
          match = addLibToReportItem(stock, library, allRna, allSamplesById);
          if (isCreatedBetween(library, start, end)) {
            rnaThisMonth.put(stock.getId(), match);
          }
          allRna.put(stock.getId(), match);
        }
      }
    }

    // Then go through all the runs and pull out lanes that contained this project's samples
    for (RunDto run : allRuns) {
      if (!RUN_COMPLETED.equals(run.getState()) && !RUN_RUNNING.equals(run.getState())) continue;
      if (run.getPositions() == null) continue;
      for (RunDtoPosition lane : run.getPositions()) {
        if (lane.getSamples() == null) continue;
        for (RunDtoSample runLib : lane.getSamples()) {
          SampleDto lib = allSamplesById.get(runLib.getId());
          if (lib != null && project.equals(lib.getProjectName())) {
            SampleDto stock = getParent(lib, SAMPLE_CATEGORY_STOCK, allSamplesById);
            ReportItem match;
            if (hasDnaAnalyteName(stock)) {
              match = allDna.get(stock.getId());
              match.addRunLane(run, lane);
              classifyIfActiveThisMonth(run, stock, match, dnaThisMonth);
              allDna.put(stock.getId(), match);
            } else {
              match = allRna.get(stock.getId());
              match.addRunLane(run, lane);
              classifyIfActiveThisMonth(run, stock, match, rnaThisMonth);
              allRna.put(stock.getId(), match);
            }
          }
        }
      }
    }
    dnaRecentList = listifyMap(dnaThisMonth);
    rnaRecentList = listifyMap(rnaThisMonth);
    dnaAllList = listifyMap(allDna);
    rnaAllList = listifyMap(allRna);
    // reverse the "all" lists, as we want to display most recent items at the top
    Collections.reverse(dnaAllList);
    Collections.reverse(rnaAllList);
  }

  private void addReportItem(ReportItem item, Map<String, ReportItem> recent, Map<String, ReportItem> all) {
    Set<SampleDto> recentAliquots = item.getAliquots().stream()
        .filter(aliq -> isCreatedBetween(aliq, start, end))
        .collect(Collectors.toSet());
    if (!recentAliquots.isEmpty() || isCreatedBetween(item.getStock(), start, end)) {
      recent.put(item.getStock().getId(), item);
    }
    all.put(item.getStock().getId(), item);
  }

  private static final ReportItem addLibToReportItem(SampleDto stock, SampleDto lib, Map<String, ReportItem> all,
      Map<String, SampleDto> allSamples) {
    String groupId = getAttribute(ATTR_GROUP_ID, lib);
    if (groupId == null) groupId = getUpstreamAttribute(ATTR_GROUP_ID, lib, allSamples);
    ReportItem match = all.get(stock.getId());
    if (match == null) {
      // SIGH. some test projects use aliquots from different projects
      SampleDto identity = getParent(stock, SAMPLE_CATEGORY_IDENTITY, allSamples);
      String externalName = getAttribute(ATTR_EXTERNAL_NAME, identity);
      ReportItem deNovo = new ReportItem(stock, externalName);
      match = deNovo;
    }
    match.addLibrary(lib, groupId);
    return match;
  }

  private final void classifyIfActiveThisMonth(RunDto run, SampleDto stock, ReportItem match, Map<String, ReportItem> thisMonth) {
    if ((RUN_COMPLETED.equals(run.getState())
        && run.getCompletionDate().compareTo(start) > 0
        && run.getCompletionDate().compareTo(end) < 0)
        ||
        (RUN_RUNNING.equals(run.getState()) &&
            (run.getStartDate().compareTo(start) > 0
                && run.getStartDate().compareTo(end) < 0))) {
      thisMonth.put(stock.getId(), match);
    }
  }

  private static final List<Map.Entry<String, ReportItem>> listifyMap(Map<String, ReportItem> map) {
    // need to convert it to a list, because getRow() calls item by index
    ArrayList<Map.Entry<String, ReportItem>> listified = new ArrayList<>(map.entrySet());
    Collections.sort(listified, new Comparator<Map.Entry<String, ReportItem>>() {
      @Override
      public int compare(Map.Entry<String, ReportItem> s1, Map.Entry<String, ReportItem> s2) {
        return s1.getValue().getStock().getName().compareTo(s2.getValue().getStock().getName());
      }
    });
    return listified;
  }

  private static boolean hasDnaAnalyteName(SampleDto dto) {
    return (dto.getName().endsWith("_D") || dto.getName().contains("_D_"))
        && !dto.getSampleType().contains("cDNA");
  }

  private Predicate<RunDto> byCreatedBefore(String end) {
    return dto -> {
      return start == null && end != null || dto.getCreatedDate().compareTo(end) < 0;
    };
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    String analyteHeader = (analyte == null ? "DNA" : analyte);
    return Arrays.asList(
        new ColumnDefinition(String.format("%s: %s Recent (%s - %s)", project, analyteHeader, start, end)),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""));
  }

  @Override
  protected int getRowCount() {
    if (analyte == null) {
      // print DNA and RNA on same sheet
      return dnaRecentList.size()
          + 3
          + rnaRecentList.size()
          + 3
          + dnaAllList.size()
          + 3
          + rnaAllList.size()
          + 1;
    } else if (DNA.equals(analyte)) {
      // only DNA
      return dnaRecentList.size()
          + 3
          + dnaAllList.size()
          + 1;
    } else {
      // only RNA
      return rnaRecentList.size()
          + 3
          + rnaAllList.size()
          + 1;
    }
    // the "+1" is to make the getRow() call happy -- adds a blank row at the end since
    // don't know whether the last row will be DNA or RNA
  }

  @Override
  protected String[] getRow(int rowNum) {
    if (rowNum == 0) {
      return makeHeadingRow();
    }
    rowNum -= 1;

    if (analyte == null || analyte.equals(DNA)) {
      if (rowNum < dnaRecentList.size()) {
        return makeStatusRow(dnaRecentList.get(rowNum), end);
      }
      rowNum -= dnaRecentList.size();
    }

    if (analyte == null || RNA.equals(analyte)) {
      if (analyte == null) {
        // add extra headers for DNA & RNA combined report.
        // if RNA.equals(analyte) then the "RNA Recent" title and heading are already written
        if (rowNum == 0) {
          return makeBlankRow();
        } else if (rowNum == 1) {
          return makeTitleRow(String.format("%s: %s (%s - %s)", project, "RNA Recent", start, end));
        } else if (rowNum == 2) {
          return makeHeadingRow();
        }
        rowNum -= 3;
      }

      if (rowNum < rnaRecentList.size()) {
        return makeStatusRow(rnaRecentList.get(rowNum), end);
      }
      rowNum -= rnaRecentList.size();
    }

    if (analyte == null || DNA.equals(analyte)) {
      if (rowNum == 0) {
        return makeBlankRow();
      } else if (rowNum == 1) {
        return makeTitleRow(String.format("%s: %s (%s - %s)", project, "DNA", "All Time", todate));
      } else if (rowNum == 2) {
        return makeHeadingRow();
      }
      rowNum -= 3;

      if (rowNum < dnaAllList.size()) {
        return makeStatusRow(dnaAllList.get(rowNum), todate);
      }
      rowNum -= dnaAllList.size();
    }

    if (analyte == null || RNA.equals(analyte)) {
      if (rowNum == 0) {
        return makeBlankRow();
      } else if (rowNum == 1) {
        return makeTitleRow(String.format("%s: %s (%s - %s)", project, "RNA", "All Time", todate));
      } else if (rowNum == 2) {
        return makeHeadingRow();
      }
      rowNum -= 3;

      return makeStatusRow(rnaAllList.get(rowNum), todate);
    }
    return makeBlankRow(); // sigh. Here since we have to return something outside an if block
  }

  private String[] makeStatusRow(Map.Entry<String, ReportItem> sample, String end) {

    String[] row = makeBlankRow();
    ReportItem info = sample.getValue();
    int i = -1;
    row[++i] = info.getStock().getName(); // stock name
    row[++i] = info.getExternalName();
    row[++i] = info.getGroupIds();
    row[++i] = "1"; // received samples
    row[++i] = info.getWaitingAliquots(); // aliquots waiting
    row[++i] = info.getWaitingLibraries(); // libraries waiting
    row[++i] = info.getLibraries().isEmpty() ? "" : Integer.toString(info.getLibraries().size());// libraries created
    row[++i] = info.getWaitingSequencing(); // sequencing waiting
    row[++i] = info.getNumLanesRunning(); // lanes running
    row[++i] = info.getNumLanesCompleted(); // lanes completed
    row[++i] = info.getDaysInLab(end); // days in genomics
    // analysis completed is always left blank because lab fills it out
    return row;
  }

  private String[] makeTitleRow(String title) {
    String[] row = makeBlankRow();
    row[0] = title;
    return row;
  }

  private String[] makeHeadingRow() {
    return new String[] { //
        "Stock", //
        "Ext. Name", //
        "Group ID", //
        "Samples", //
        "Aliq Waiting", //
        "Libs Waiting", //
        "Libs", //
        "Seq Waiting", //
        "Seq Running", //
        "Seq Done", //
        "Days in Genomics", //
        "Analysis" };
  }

  private String[] makeBlankRow() {
    String[] row = new String[getColumns().size()];
    Arrays.fill(row, "");
    return row;
  }

}
