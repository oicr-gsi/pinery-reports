package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.byEndedBetween;
import static ca.on.oicr.pineryreports.util.GeneralUtils.timeStringToYyyyMmDd;
import static ca.on.oicr.pineryreports.util.SampleUtils.isRnaLibrary;
import static ca.on.oicr.pineryreports.util.SampleUtils.mapSamplesById;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Sets;

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

public class LanesBillingReport extends TableReport {
  
  private static class DetailedObject {
    private final String instrumentModel;
    private final String instrumentName;
    private final String laneContents;
    private final String laneNumber;
    private final String project;
    private final BigDecimal projLibsPercent; // percentage of libs in lane which belong to given project
    private final String runEndDate;
    private final String runName;
    private final String runStatus;
    
    public DetailedObject(RunDto run, String instrumentName, String instrumentModel, String laneNumber, String project,
        BigDecimal libsPercent, String laneContents) {
      this.runEndDate = removeTime(run.getCompletionDate());
      this.instrumentName = instrumentName;
      this.instrumentModel = instrumentModel;
      this.runName = run.getName();
      this.laneNumber = laneNumber;
      this.project = project;
      this.projLibsPercent = libsPercent;
      this.runStatus = run.getState();
      this.laneContents = laneContents;
    }

    public String getInstrumentModel() {
      return instrumentModel;
    }
    
    public String getInstrumentName() {
      return instrumentName;
    }
    
    public String getLaneContents() {
      return laneContents;
    }

    public String getLaneNumber() {
      return laneNumber;
    }

    public String getProject() {
      return project;
    }
    
    public String getPrintableProjLibsPercent() {
      return projLibsPercent.toPlainString();
    }
    
    public BigDecimal getProjLibsPercent() {
      return projLibsPercent;
    }

    public String getRunEndDate() {
      return runEndDate;
    }

    public String getRunName() {
      return runName;
    }
    
    public String getRunStatus() {
      return runStatus;
    }
    
    private static final Comparator<DetailedObject> detailedComparator = (DetailedObject o1, DetailedObject o2) -> {
        if (o1.getProject().equals(o2.getProject())) {
          return o2.getRunEndDate().compareTo(o2.getRunEndDate());
        } else {
          // sort on this primarily
          return o1.getProject().compareTo(o2.getProject());
        }
    };
  }
  
  public static final String REPORT_NAME = "lanes-billing";
  private static final Option OPT_AFTER = CommonOptions.after(false);
  private static final Option OPT_BEFORE = CommonOptions.before(false);
  private static final String DNA_LANE = "DNA";
  private static final String RNA_LANE = "RNA";
  private static final String MIXED_LANE = "MIXED";
  
  private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
  private String start;
  private String end;


  @Override
  public String getReportName() {
    return REPORT_NAME;
  }
  
  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_AFTER, OPT_BEFORE);
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
  }

  @Override
  public String getTitle() {
    return "Lanes Completion (Billing) "
        + (start == null ? "Any Time" : start)
        + " - "
        + (end == null ? "Now" : end);
  }
  
  private static class ReportObject {
    private final List<DetailedObject> detailedData;
    private final List<Map<String, Map<String, BigDecimal>>> summaryAsList;
    
    public ReportObject(List<DetailedObject> detailed, Map<String, Map<String, BigDecimal>> summary) {
      detailed.sort(DetailedObject.detailedComparator);
      this.detailedData = detailed;
      this.summaryAsList = listifySummary(summary);
    }
    
    public List<DetailedObject> getDetailedData() {
      return detailedData;
    }
    
    public List<Map<String, Map<String, BigDecimal>>> getSummaryAsList() {
      return summaryAsList;
    }
  }
  
  private ReportObject completed;
  private ReportObject failed;

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    // samples and instruments and instrument models will come in handy
    Map<String, SampleDto> samplesById = mapSamplesById(pinery.getSample().all());
    Map<Integer, InstrumentDto> instrumentsById = pinery.getInstrument().all().stream()
        .collect(Collectors.toMap(InstrumentDto::getId, dto->dto));
    Map<Integer, InstrumentModelDto> instrumentModelsById = pinery.getInstrumentModel().all().stream()
        .collect(Collectors.toMap(InstrumentModelDto::getId, dto->dto));
    // filter runs within the date range
    Set<RunDto> newRuns = pinery.getSequencerRun().all().stream().filter(byEndedBetween(start, end)).collect(Collectors.toSet());
    Set<RunDto> failedRuns = newRuns.stream().filter(run -> "Failed".equals(run.getState())).collect(Collectors.toSet());
    newRuns.removeAll(failedRuns);
    
    completed = getReportAndSummaryData(newRuns, samplesById, instrumentsById, instrumentModelsById);
    failed = getReportAndSummaryData(failedRuns, samplesById, instrumentsById, instrumentModelsById);
  }
  
  private ReportObject getReportAndSummaryData(Collection<RunDto> runs, Map<String, SampleDto> samplesById,
      Map<Integer, InstrumentDto> instrumentsById, Map<Integer, InstrumentModelDto> instrumentModelsById) {
    List<DetailedObject> detailedData = new ArrayList<>();
    Map<String, Map<String, BigDecimal>> summaryData = new TreeMap<>();
    for (RunDto run : runs) {
      String instrumentName = getInstrumentName(run.getInstrumentId(), instrumentsById);
      String instrumentModel = getInstrumentModel(run.getInstrumentId(), instrumentsById, instrumentModelsById);
      if (run.getPositions() == null) continue;; 
      for (RunDtoPosition lane : run.getPositions()) {
        int laneNumber = lane.getPosition();
        int samplesInLane = lane.getSamples() == null ? 0 : lane.getSamples().size();
        boolean dnaInLane = false;
        boolean rnaInLane = false;
        // projectName : numLibsFromProjInLane
        Map<String, Integer> projectsInLane = new HashMap<>();
        if (lane.getSamples() == null) continue;
        for (RunDtoSample sam : lane.getSamples()) {
          SampleDto dilution = samplesById.get(sam.getId());
          if (isRnaLibrary(dilution, samplesById)) {
            rnaInLane = true;
          } else {
            dnaInLane = true;
          }
          if (projectsInLane.containsKey(dilution.getProjectName())) {
            // increment project libraries
            projectsInLane.merge(dilution.getProjectName(), 1, Integer::sum);
          } else {
            // add project with library to map
            projectsInLane.put(dilution.getProjectName(), 1);
          }
        }
        String laneContents = dnaInLane 
            ? (rnaInLane ? MIXED_LANE : DNA_LANE) 
            : RNA_LANE;
        
        for (Map.Entry<String, Integer> entry : projectsInLane.entrySet()) {
          BigDecimal projectPercent = getPercentProjectInLane(entry.getValue(), samplesInLane);
          DetailedObject newReport = new DetailedObject(run, instrumentName, instrumentModel, Integer.toString(laneNumber), entry.getKey(),
              projectPercent, laneContents);
          detailedData.add(newReport);
          addSummaryData(summaryData, newReport);
        }
      }
    }
    return new ReportObject(detailedData, summaryData);
  }
  
  private void addSummaryData(Map<String, Map<String, BigDecimal>> summary, DetailedObject newReport) {
    String summaryKey = getSummaryKey(newReport);
    if (summary.containsKey(summaryKey)) {
      // increment lane number+type for this project+instrument
      addSummaryLanes(summary.get(summaryKey), newReport.getLaneContents(), newReport.getProjLibsPercent());
    } else {
      // add new project+instrument and lane number+type
      Map<String, BigDecimal> newLanes = makeNewSummaryLanes(newReport.getLaneContents(), newReport.getProjLibsPercent());
      summary.put(summaryKey, newLanes);
    }
  }
  
  private BigDecimal getPercentProjectInLane(Integer numProjectLibraries, Integer numLaneLibraries) {
    return new BigDecimal(numProjectLibraries).divide(new BigDecimal(numLaneLibraries), 1,  RoundingMode.HALF_UP);
  }
  
  private String getInstrumentModel(Integer instrumentId, Map<Integer, InstrumentDto> instruments, Map<Integer, InstrumentModelDto> models) {
    return models.get(
        instruments.get(instrumentId).getModelId())
      .getName();
  }
  
  private String getInstrumentName(Integer instrumentId, Map<Integer, InstrumentDto> instrumentsById) {
    InstrumentDto instrument = instrumentsById.get(instrumentId);
    return instrument == null ? "Unknown" : instrument.getName();
  }
  
  private String getSummaryKey(DetailedObject row) {
    return row.getProject() + ":" + row.getInstrumentModel();
  }
  
  private Map<String, BigDecimal> makeNewSummaryLanes(String laneType, BigDecimal laneCount) {
    Map<String, BigDecimal> lanes = new HashMap<>();
    lanes.put(DNA_LANE, BigDecimal.ZERO);
    lanes.put(RNA_LANE, BigDecimal.ZERO);
    lanes.put(MIXED_LANE, BigDecimal.ZERO);
    addSummaryLanes(lanes, laneType, laneCount);
    return lanes;
  }
  
  private void addSummaryLanes(Map<String, BigDecimal> lanes, String laneType, BigDecimal laneCount) {
    lanes.merge(laneType, laneCount, BigDecimal::add);
  }
  
  static final List<Map<String, Map<String, BigDecimal>>> listifySummary(Map<String, Map<String, BigDecimal>> summary) {
    // need to convert it to a list, because getRow() takes an index and the treemap doesn't yet have one of those
    List<Map<String, Map<String, BigDecimal>>> regrettable = new ArrayList<>();
    
    for (Map.Entry<String, Map<String, BigDecimal>> entry : summary.entrySet()) {
      Map<String, Map<String, BigDecimal>> oneProjModel = new HashMap<>();
      oneProjModel.put(entry.getKey(), entry.getValue());
      regrettable.add(oneProjModel);
    }
    return regrettable;
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return Arrays.asList(
        new ColumnDefinition("Project"),
        new ColumnDefinition("Instrument Model"),
        new ColumnDefinition(DNA_LANE),
        new ColumnDefinition(RNA_LANE),
        new ColumnDefinition(MIXED_LANE),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition("")
    );
  }
  
  private List<String> getDetailedHeadings() {
    return Arrays.asList(
        "Project",
        "Instrument Name",
        "Instrument Model",
        "Run Status",
        "Run End Date",
        "Run Name",
        "Lane",
        DNA_LANE,
        RNA_LANE,
        MIXED_LANE
    );
  }

  @Override
  protected int getRowCount() {
    return completed.getDetailedData().size()
        + (completed.getSummaryAsList().isEmpty() ? 0 : 2)
        + completed.getSummaryAsList().size()
        + (failed.getDetailedData().isEmpty() ? 0 : 2)
        + failed.getDetailedData().size()
        + (failed.getSummaryAsList().isEmpty() ? 0 : 2)
        + failed.getSummaryAsList().size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    if (rowNum < completed.getSummaryAsList().size()) {
      Map<String, Map<String, BigDecimal>> completedSummary = completed.getSummaryAsList().get(rowNum);
      return makeSummaryRow(completedSummary);
    }
    rowNum -= completed.getSummaryAsList().size();
    
    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow(getDetailedHeadings());
    }
    rowNum -= 2;

    if (rowNum < completed.getDetailedData().size()) {
      return makeDetailedRow(completed.getDetailedData().get(rowNum));
    }
    rowNum -= completed.getDetailedData().size();
    
    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow(getColumns().stream().map(ColumnDefinition::getHeading).collect(Collectors.toList()));
    }
    rowNum -= 2;
    
    if (rowNum < failed.getSummaryAsList().size()) {
      Map<String, Map<String, BigDecimal>> failedSummary = failed.getSummaryAsList().get(rowNum);
      return makeSummaryRow(failedSummary);
    }
    rowNum -= failed.getSummaryAsList().size();
    
    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow(getDetailedHeadings());
    }
    rowNum -= 2;
    
    return makeDetailedRow(failed.getDetailedData().get(rowNum));
  }

  private String[] makeSummaryRow(Map<String, Map<String, BigDecimal>> obj) {
    String[] row = new String[getColumns().size()];
    int i = -1;
    String summaryKey = "";
    for (String key : obj.keySet()) {
      summaryKey = key;
    }

    String[] key = summaryKey.split(":");
    // Project
    row[++i] = key[0];
    // Instrument Model
    row[++i] = key[1];
    // DNA Lanes
    row[++i] = obj.get(summaryKey).get(DNA_LANE).toPlainString();
    // RNA Lanes
    row[++i] = obj.get(summaryKey).get(RNA_LANE).toPlainString();
    // Mixed Lanes
    row[++i] = obj.get(summaryKey).get(MIXED_LANE).toPlainString();
    
    
    for (int j = i+1; j < row.length; j++) {
      row[j] = "";
    }
    return row;
  }
  
  private String[] makeBlankRow() {
    String[] row = new String[getColumns().size()];
    for (int i = 0; i < row.length; i++) {
      row[i] = "";
    }
    return row;
  }
  
  private String[] makeHeadingRow(List<String> headings) {
    String[] row = new String[headings.size()];
    return headings.toArray(row);
  }

  private String[] makeDetailedRow(DetailedObject obj) {
    String[] row = new String[getColumns().size()];
    
    int i = -1;
    // Project
    row[++i] = obj.getProject();
    // Instrument Name
    row[++i] = obj.getInstrumentName();
    // Instrument Model
    row[++i] = obj.getInstrumentModel();
    // Run Status
    row[++i] = obj.getRunStatus();
    // Run End Date
    row[++i] = obj.getRunEndDate();
    // Run Name
    row[++i] = obj.getRunName();
    // Lane
    row[++i] = obj.getLaneNumber();
    // DNA Lanes
    row[++i] = obj.getLaneContents().equals(DNA_LANE) ? obj.getPrintableProjLibsPercent() : "";
    // RNA Lanes
    row[++i] = obj.getLaneContents().equals(RNA_LANE) ? obj.getPrintableProjLibsPercent() : "";
    // Mixed Lanes
    row[++i] = obj.getLaneContents().equals(MIXED_LANE) ? obj.getPrintableProjLibsPercent() : "";
    
    return row;
  }
}
