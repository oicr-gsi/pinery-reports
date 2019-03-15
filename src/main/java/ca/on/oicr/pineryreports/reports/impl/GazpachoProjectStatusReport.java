package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.InstrumentDto;
import ca.on.oicr.ws.dto.InstrumentModelDto;
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.RunDtoPosition;
import ca.on.oicr.ws.dto.RunDtoSample;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class GazpachoProjectStatusReport extends TableReport {

  private static class ModifiedRunDto {
    private final String completionDate;
    private final String instrumentModel;
    private final List<Integer> lanes;
    private final Integer runId;
    private final String status;

    public ModifiedRunDto(RunDto run, String instrumentModel, List<Integer> lanes) {
      this.runId = run.getId();
      this.status = run.getState();
      this.completionDate = run.getCompletionDate();
      this.lanes = lanes;
      this.instrumentModel = instrumentModel;
    }

    public String getCompletionDate() {
      return completionDate;
    }

    public List<Integer> getLanes() {
      return lanes;
    }

    public void addLane(Integer lane) {
      lanes.add(lane);
    }

    public Integer getRunId() {
      return runId;
    }

    public String getStatus() {
      return status;
    }

    public String getInstrumentModel() {
      return instrumentModel;
    }
  }

  private static class ReportItem implements Comparable<ReportItem> {

    private final SampleDto stock;
    private final String subproject;
    private final String externalName;
    private final Set<String> groupIds = new HashSet<>();
    private final Set<SampleDto> aliquots = new HashSet<>();
    private final Set<SampleDto> libraries = new HashSet<>();
    private final Set<ModifiedRunDto> runs = new HashSet<>();

    public ReportItem(SampleDto stock, String externalName, String subproject) {
      this.stock = stock;
      this.externalName = externalName;
      this.subproject = subproject;
    }

    public void addAliquot(SampleDto aliquot) {
      this.aliquots.add(aliquot);
    }

    public void addLibrary(SampleDto library, String groupId) {
      this.libraries.add(library);
      if (groupId != null) groupIds.add(groupId);
    }

    public void addRunLane(RunDto run, RunDtoPosition lane, String instrumentModel) {
      boolean match = false;
      for (ModifiedRunDto r : runs) {
        if (r.getRunId() == run.getId()) {
          match = true;
          r.addLane(lane.getPosition());
        }
      }
      if (match) return;
      List<Integer> lanes = new ArrayList<>();
      lanes.add(lane.getPosition());
      runs.add(new ModifiedRunDto(run, instrumentModel, lanes));
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
      SampleDto mostRecentAliquot =
          aliquots.stream().max(Comparator.comparing(SampleDto::getCreatedDate)).orElse(null);
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

    public String getSubproject() {
      return (subproject == null ? "" : subproject);
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
        maxLibCreated =
            getDateTimeFormat()
                .parse(
                    getLibraries()
                        .stream()
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

    public Set<ModifiedRunDto> getRuns() {
      return runs;
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
      if (runs.isEmpty()
          || !runs.stream()
              .filter(run -> RUN_RUNNING.equals(run.getStatus()))
              .collect(Collectors.toList())
              .isEmpty()) {
        return Long.toString(daysSinceReceived);
      } else {
        ModifiedRunDto mostRecent =
            runs.stream().max(Comparator.comparing(ModifiedRunDto::getCompletionDate)).orElse(null);
        if (mostRecent == null) return Long.toString(daysSinceReceived);
        try {
          runCompleted = getDateTimeFormat().parse(mostRecent.getCompletionDate());
        } catch (java.text.ParseException e) {
          System.out.println("Error parsing completion date for run " + mostRecent.getRunId());
          return "";
        }
        long daysReceivedToCompleted =
            Duration.between(receivedByLab.toInstant(), runCompleted.toInstant()).toDays();
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
  private static final Instant SIX_MONTHS_AGO = ZonedDateTime.now().minusMonths(6).toInstant();
  private static final String ALL = "ALL";
  private static final String MISEQ = "MiSeq";
  private static final String HISEQ = "HiSeq";
  private static final String NOVASEQ = "NovaSeq";

  private String project;
  private String start;
  private String end;
  private String analyte = "ALL"; // defaults to displaying both DNA and RNA in the same report
  private final String todate = removeTime(TODAY.toString());

  private static final Option OPT_PROJECT = CommonOptions.project(true);
  private static final Option OPT_AFTER = CommonOptions.after(true);
  private static final Option OPT_BEFORE = CommonOptions.before(true);
  private static final Option OPT_ANALYTE = CommonOptions.analyte(false);
  public static final String REPORT_NAME = "project-status";
  public static final String CATEGORY = REPORT_CATEGORY_COUNTS;

  // keep track of the base stock name for all samples/libraries/runs active this month
  Map<String, ReportItem> dnaThisMonth = Maps.newTreeMap();
  Map<String, ReportItem> rnaThisMonth = Maps.newTreeMap();
  Map<String, ReportItem> allDna = Maps.newTreeMap();
  Map<String, ReportItem> allRna = Maps.newTreeMap();
  List<Map.Entry<String, ReportItem>> dnaRecentList;
  List<Map.Entry<String, ReportItem>> rnaRecentList;
  List<Map.Entry<String, ReportItem>> dnaStagnantList;
  List<Map.Entry<String, ReportItem>> rnaStagnantList;
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
  public String getCategory() {
    return CATEGORY;
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
      this.analyte = cmd.getOptionValue(OPT_ANALYTE.getLongOpt());
      if (!DNA.equals(analyte) && !RNA.equals(analyte) && !ALL.equals(analyte)) {
        throw new ParseException("Analyte must be DNA or RNA or ALL");
      }
    }

    this.project = cmd.getOptionValue(OPT_PROJECT.getLongOpt());
    recordOptionsUsed(cmd);
  }

  @Override
  public String getTitle() {
    return "Project Status Report (Gazpacho) for project "
        + project
        + (ALL.equals(analyte) ? " DNA & RNA" : String.format(" %s", analyte))
        + " for "
        + (start == null ? "Any Time" : start)
        + " - "
        + (end == null ? "Now" : end)
        + ". Generated "
        + new SimpleDateFormat(DATE_FORMAT).format(new Date());
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> allSamples = pinery.getSample().all();
    List<SampleDto> allProjectSamples =
        allSamples.stream().filter(byProject(project)).collect(Collectors.toList());
    Map<String, SampleDto> allSamplesById =
        mapSamplesById(allSamples); // separate since hierarchy may include multiple projects
    List<RunDto> allRuns = pinery.getSequencerRun().all();
    Map<Integer, InstrumentDto> instrumentsById =
        pinery
            .getInstrument()
            .all()
            .stream()
            .collect(Collectors.toMap(InstrumentDto::getId, dto -> dto));
    Map<Integer, InstrumentModelDto> instrumentModelsById =
        pinery
            .getInstrumentModel()
            .all()
            .stream()
            .collect(Collectors.toMap(InstrumentModelDto::getId, dto -> dto));
    // Filtering now helps with pulling data from a historic point in time
    allProjectSamples =
        allProjectSamples.stream().filter(byCreatedBetween(null, end)).collect(Collectors.toList());
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
        String subproject = getAttribute(ATTR_SUBPROJECT, stock);
        if (subproject == null)
          subproject = getUpstreamAttribute(ATTR_SUBPROJECT, stock, allSamplesById);
        if (hasDnaAnalyteName(stock)) {
          ReportItem stockItem = allDna.get(stock.getId());
          if (stockItem == null) {
            stockItem = new ReportItem(stock, externalName, subproject);
          }
          if (aliquot != null) stockItem.addAliquot(aliquot);
          addReportItem(stockItem, dnaThisMonth, allDna);
        } else {
          ReportItem stockItem = allRna.get(stock.getId());
          if (stockItem == null) {
            stockItem = new ReportItem(stock, externalName, subproject);
          }
          if (aliquot != null) stockItem.addAliquot(aliquot);
          addReportItem(stockItem, rnaThisMonth, allRna);
        } // else don't care
      }
    }

    // Then go through and categorize all libraries
    for (SampleDto library : allProjectSamples) {
      String type = library.getSampleType();

      if (type != null
          && type.contains(LIBRARY)
          && !type.contains(" Seq")
          && project.equals(library.getProjectName())) {
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
              match.addRunLane(
                  run,
                  lane,
                  getInstrumentModel(run.getInstrumentId(), instrumentsById, instrumentModelsById));
              classifyIfActiveThisMonth(run, stock, match, dnaThisMonth);
              allDna.put(stock.getId(), match);
            } else {
              match = allRna.get(stock.getId());
              match.addRunLane(
                  run,
                  lane,
                  getInstrumentModel(run.getInstrumentId(), instrumentsById, instrumentModelsById));
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

    dnaStagnantList = getStagnant(dnaAllList);
    rnaStagnantList = getStagnant(rnaAllList);
  }

  /**
   * Get all the items created in the past six months which don't have recent activity (within the
   * past month)
   *
   * @param allReportItems list of all report items
   * @return list of report items created in the past six months with no recent activity
   */
  private List<Entry<String, ReportItem>> getStagnant(
      List<Entry<String, ReportItem>> allReportItems) {
    return allReportItems
        .stream()
        .filter(
            entry -> {
              boolean stagnant = false;
              try {
                String lanesActive = getNumLanes(entry.getValue(), RUN_COMPLETED, null);
                if ("".equals(lanesActive))
                  lanesActive = getNumLanes(entry.getValue(), RUN_RUNNING, null);
                stagnant =
                    "".equals(lanesActive)
                        && !dnaRecentList.contains(entry)
                        && !rnaRecentList.contains(entry)
                        && getDateTimeFormat()
                            .parse(entry.getValue().getStock().getCreatedDate())
                            .toInstant()
                            .isAfter(SIX_MONTHS_AGO);
              } catch (java.text.ParseException e) {
                e.printStackTrace();
              }
              return stagnant;
            })
        .collect(Collectors.toList());
  }

  private void addReportItem(
      ReportItem item, Map<String, ReportItem> recent, Map<String, ReportItem> all) {
    Set<SampleDto> recentAliquots =
        item.getAliquots()
            .stream()
            .filter(aliq -> isCreatedBetween(aliq, start, end))
            .collect(Collectors.toSet());
    if (!recentAliquots.isEmpty() || isCreatedBetween(item.getStock(), start, end)) {
      recent.put(item.getStock().getId(), item);
    }
    all.put(item.getStock().getId(), item);
  }

  private static final ReportItem addLibToReportItem(
      SampleDto stock,
      SampleDto lib,
      Map<String, ReportItem> all,
      Map<String, SampleDto> allSamples) {
    String groupId = getAttribute(ATTR_GROUP_ID, lib);
    if (groupId == null) groupId = getUpstreamAttribute(ATTR_GROUP_ID, lib, allSamples);
    ReportItem match = all.get(stock.getId());
    if (match == null) {
      // SIGH. some test projects use aliquots from different projects
      SampleDto identity = getParent(stock, SAMPLE_CATEGORY_IDENTITY, allSamples);
      String externalName = getAttribute(ATTR_EXTERNAL_NAME, identity);
      String subproject = getAttribute(ATTR_SUBPROJECT, stock);
      if (subproject == null) subproject = getUpstreamAttribute(ATTR_SUBPROJECT, stock, allSamples);
      ReportItem deNovo = new ReportItem(stock, externalName, subproject);
      match = deNovo;
    }
    match.addLibrary(lib, groupId);
    return match;
  }

  private final void classifyIfActiveThisMonth(
      RunDto run, SampleDto stock, ReportItem match, Map<String, ReportItem> thisMonth) {
    if ((RUN_COMPLETED.equals(run.getState())
            && run.getCompletionDate().compareTo(start) > 0
            && run.getCompletionDate().compareTo(end) < 0)
        || (RUN_RUNNING.equals(run.getState())
            && (run.getStartDate().compareTo(start) > 0
                && run.getStartDate().compareTo(end) < 0))) {
      thisMonth.put(stock.getId(), match);
    }
  }

  public static String getNumLanes(ReportItem item, String runStatus, String instrumentModel) {
    // instrumentModel may be null
    Predicate<ModifiedRunDto> predicates = dto -> runStatus.equals(dto.getStatus());
    if (instrumentModel != null && !"".equals(instrumentModel)) {
      predicates = predicates.and(dto -> dto.getInstrumentModel().contains(instrumentModel));
    }
    Set<ModifiedRunDto> matching =
        item.getRuns().stream().filter(predicates).collect(Collectors.toSet());
    Integer matchingLanes = 0;
    for (ModifiedRunDto run : matching) {
      List<Integer> lanesForRun = run.getLanes();
      matchingLanes += lanesForRun.size();
    }
    return matchingLanes.equals(0) ? "" : matchingLanes.toString();
  }

  private static final List<Map.Entry<String, ReportItem>> listifyMap(Map<String, ReportItem> map) {
    // need to convert it to a list, because getRow() calls item by index
    ArrayList<Map.Entry<String, ReportItem>> listified = new ArrayList<>(map.entrySet());
    Collections.sort(
        listified,
        new Comparator<Map.Entry<String, ReportItem>>() {
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
    return dto -> start == null && end != null || dto.getCreatedDate().compareTo(end) < 0;
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    String analyteHeader = (RNA.equals(analyte) ? RNA : DNA);
    return Arrays.asList(
        new ColumnDefinition(
            String.format("%s: %s Recent (%s - %s)", project, analyteHeader, start, end)),
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
    int count = 1;
    if (ALL.equals(analyte) || DNA.equals(analyte)) {
      count += dnaRecentList.size() + 3 + dnaStagnantList.size() + 3 + dnaAllList.size();
    }
    if (ALL.equals(analyte) || RNA.equals(analyte)) {
      count += rnaRecentList.size() + 3 + rnaStagnantList.size() + 3 + rnaAllList.size();
    }
    return count;
    // the "+1" is to make the getRow() call happy -- adds a blank row at the end since
    // don't know whether the last row will be DNA or RNA
  }

  @Override
  protected String[] getRow(int rowNum) {
    if (rowNum == 0) {
      return makeHeadingRow();
    }
    rowNum -= 1;

    // RECENT
    if (ALL.equals(analyte) || DNA.equals(analyte)) {
      if (rowNum < dnaRecentList.size()) {
        return makeStatusRow(dnaRecentList.get(rowNum), end);
      }
      rowNum -= dnaRecentList.size();
    }

    if (ALL.equals(analyte)) {
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
    if (ALL.equals(analyte) || RNA.equals(analyte)) {
      if (rowNum < rnaRecentList.size()) {
        return makeStatusRow(rnaRecentList.get(rowNum), end);
      }
      rowNum -= rnaRecentList.size();
    }

    // STAGNANT
    if (ALL.equals(analyte) || DNA.equals(analyte)) {
      if (rowNum == 0) {
        return makeBlankRow();
      } else if (rowNum == 1) {
        return makeTitleRow(
            String.format("%s: %s (%s - %s)", project, "DNA Stagnant", "6 months ago", "Today"));
      } else if (rowNum == 2) {
        return makeHeadingRow();
      }
      rowNum -= 3;

      if (rowNum < dnaStagnantList.size()) {
        return makeStatusRow(dnaStagnantList.get(rowNum), todate);
      }
      rowNum -= dnaStagnantList.size();
    }

    if (ALL.equals(analyte) || RNA.equals(analyte)) {
      if (rowNum == 0) {
        return makeBlankRow();
      } else if (rowNum == 1) {
        return makeTitleRow(
            String.format("%s: %s (%s - %s)", project, "RNA Stagnant", "6 months ago", "Today"));
      } else if (rowNum == 2) {
        return makeHeadingRow();
      }
      rowNum -= 3;

      if (rowNum < rnaStagnantList.size()) {
        return makeStatusRow(rnaStagnantList.get(rowNum), todate);
      }
      rowNum -= rnaStagnantList.size();
    }

    // ALL
    if (ALL.equals(analyte) || DNA.equals(analyte)) {
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

    if (ALL.equals(analyte) || RNA.equals(analyte)) {
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
    row[++i] = info.getSubproject();
    row[++i] = info.getExternalName();
    row[++i] = info.getGroupIds();
    row[++i] = "1"; // received samples
    row[++i] = info.getWaitingAliquots(); // aliquots waiting
    row[++i] = info.getWaitingLibraries(); // libraries waiting
    row[++i] =
        info.getLibraries().isEmpty()
            ? ""
            : Integer.toString(info.getLibraries().size()); // libraries created
    row[++i] = info.getWaitingSequencing(); // sequencing waiting
    row[++i] = getNumLanes(info, RUN_RUNNING, MISEQ); // MiSeq lanes running
    row[++i] = getNumLanes(info, RUN_COMPLETED, MISEQ); // MiSeq lanes completed
    row[++i] = getNumLanes(info, RUN_RUNNING, HISEQ); // HiSeq lanes running
    row[++i] = getNumLanes(info, RUN_COMPLETED, HISEQ); // HiSeq lanes completed
    row[++i] = getNumLanes(info, RUN_RUNNING, NOVASEQ); // NovaSeq lanes running
    row[++i] = getNumLanes(info, RUN_COMPLETED, NOVASEQ); // NovaSeq lanes completed
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
      "Subproject", //
      "Ext. Name", //
      "Group IDs", //
      "Transferred to GT", //
      "In Samples", //
      "In Lib Prep", //
      "# Libs", //
      "In Seq", //
      "# MiSeq Lanes Running", //
      "# MiSeq Lanes Done", //
      "# HiSeq Lanes Running", //
      "# HiSeq Lanes Done", //
      "# NovaSeq Lanes Running", //
      "# NovaSeq Lanes Done", //
      "Days in Genomics", //
      "Analysis"
    };
  }

  private String[] makeBlankRow() {
    String[] row = new String[getColumns().size()];
    Arrays.fill(row, "");
    return row;
  }
}
