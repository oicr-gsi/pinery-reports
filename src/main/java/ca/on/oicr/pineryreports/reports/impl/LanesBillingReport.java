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
import com.google.common.collect.Sets;
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

public class LanesBillingReport extends TableReport {

  private static class DetailedObject {
    private final String instrumentModel;
    private final String instrumentName;
    private final String laneContents;
    private final String laneNumber;
    private final String project;
    private final BigDecimal
        projLibsPercent; // percentage of libs in lane which belong to given project
    private final String runEndDate;
    private final String runName;
    private final String runStatus;
    private final String sequencingParameters;

    /** novaSeqLanesCount should be 0 for non-NovaSeq runs, since there are no NovaSeq lanes */
    public DetailedObject(
        RunDto run,
        String instrumentName,
        String instrumentModel,
        int novaSeqLanesCount,
        String laneNumber,
        String project,
        BigDecimal libsPercent,
        String laneContents) {
      this.runEndDate = removeTime(run.getCompletionDate());
      this.instrumentName = instrumentName;
      this.instrumentModel = instrumentModel;
      this.runName = run.getName();
      this.laneNumber = laneNumber;
      this.project = project;
      this.projLibsPercent = libsPercent;
      this.runStatus = run.getState();
      this.laneContents = laneContents;
      this.sequencingParameters = extractSequencingParameters(run, novaSeqLanesCount);
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

    public String getSequencingParameters() {
      return sequencingParameters;
    }

    private static String extractSequencingParameters(RunDto run, int novaSeqLanesCount) {
      String maybeNovaSeqFlowcell =
          (novaSeqLanesCount > 0 ? String.format("S%d ", novaSeqLanesCount) : "");
      if (run.getSequencingParameters() == null
          || run.getSequencingParameters().startsWith("Custom")) {
        // use runBasesMask
        // format: y#,I#?,y#
        // We only care about the y# pars; the I# parts are optional and indicate indices
        if (run.getRunBasesMask() == null || "".equals(run.getRunBasesMask())) return "";
        List<String> reads =
            Arrays.stream(run.getRunBasesMask().split(","))
                .filter(read -> read.startsWith("y")) // get only reads
                .map(read -> read.substring(1)) // strip the "y"
                .collect(Collectors.toList());
        if (reads.size() == 1 || reads.size() == 2 && reads.get(0).equals(reads.get(1))) {
          return String.format("%s%dx%s", maybeNovaSeqFlowcell, reads.size(), reads.get(0));
        } else if (reads.size() == 2) {
          return String.format("%s%s-%s", maybeNovaSeqFlowcell, reads.get(0), reads.get(1));
        } else {
          return "unknown";
        }
      } else {
        return run.getSequencingParameters().replace("×", "x");
      }
    }

    private static final Comparator<DetailedObject> detailedComparator =
        (DetailedObject o1, DetailedObject o2) -> {
          if (o1.getProject().equals(o2.getProject())) {
            return o2.getRunEndDate().compareTo(o2.getRunEndDate());
          } else {
            // sort on this primarily
            return o1.getProject().compareTo(o2.getProject());
          }
        };
  }

  public static final String REPORT_NAME = "lanes-billing";
  public static final String CATEGORY = REPORT_CATEGORY_COUNTS;
  private static final Option OPT_AFTER = CommonOptions.after(false);
  private static final Option OPT_BEFORE = CommonOptions.before(false);
  private static final String DNA_LANE = "DNA";
  private static final String RNA_LANE = "RNA";
  private static final String MIXED_LANE = "MIXED";

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
    recordOptionsUsed(cmd);
  }

  @Override
  public String getCategory() {
    return CATEGORY;
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
    private final List<Map.Entry<String, Map<String, BigDecimal>>> summaryAsList;

    public ReportObject(
        List<DetailedObject> detailed, Map<String, Map<String, BigDecimal>> summary) {
      detailed.sort(DetailedObject.detailedComparator);
      this.detailedData = detailed;
      this.summaryAsList = listifySummary(summary);
    }

    public List<DetailedObject> getDetailedData() {
      return detailedData;
    }

    public List<Map.Entry<String, Map<String, BigDecimal>>> getSummaryAsList() {
      return summaryAsList;
    }
  }

  private ReportObject completed;
  private ReportObject failed;

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    // samples and instruments and instrument models will come in handy
    Map<String, SampleDto> samplesById = mapSamplesById(pinery.getSample().all());
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
    // filter runs within the date range
    Set<RunDto> newRuns =
        pinery
            .getSequencerRun()
            .all()
            .stream()
            .filter(byEndedBetween(start, end))
            .collect(Collectors.toSet());
    Set<RunDto> failedRuns =
        newRuns
            .stream()
            .filter(run -> RUN_FAILED.equals(run.getState()))
            .collect(Collectors.toSet());
    newRuns.removeAll(failedRuns);

    completed =
        getReportAndSummaryData(newRuns, samplesById, instrumentsById, instrumentModelsById);
    failed =
        getReportAndSummaryData(failedRuns, samplesById, instrumentsById, instrumentModelsById);
  }

  private ReportObject getReportAndSummaryData(
      Collection<RunDto> runs,
      Map<String, SampleDto> samplesById,
      Map<Integer, InstrumentDto> instrumentsById,
      Map<Integer, InstrumentModelDto> instrumentModelsById) {
    List<DetailedObject> detailedData = new ArrayList<>();
    Map<String, Map<String, BigDecimal>> summaryData = new TreeMap<>();
    for (RunDto run : runs) {
      String instrumentName = getInstrumentName(run.getInstrumentId(), instrumentsById);
      String instrumentModel =
          getInstrumentModel(run.getInstrumentId(), instrumentsById, instrumentModelsById);
      if (run.getPositions() == null) continue;
      int novaSeqLanesCount = 0;
      if (NOVASEQ.equals(instrumentModel)) novaSeqLanesCount = run.getPositions().size();
      for (RunDtoPosition lane : run.getPositions()) {
        int laneNumber = lane.getPosition();
        int samplesInLane = lane.getSamples() == null ? 0 : lane.getSamples().size();
        boolean dnaInLane = false;
        boolean rnaInLane = false;
        Map<String, Integer> projectsInLane = new HashMap<>();
        if (lane.getSamples() != null) {
          // Set the samples in lane count, then continue to the `for RunSampleDto :
          // lane.getSamples()` clause.
          samplesInLane = lane.getSamples().size();
        } else if (lane.getSamples() == null && NOVASEQ.equals(instrumentModel)) {
          // Some NovaSeq lanes are joined so the pool is only added to the first lane in LIMS, but
          // is present in all the other lanes.
          // Report the same data as for the first lane.
          lane =
              run.getPositions()
                  .stream()
                  .filter(l -> l.getPosition() == 1)
                  .findFirst()
                  .orElse(null);

          if (lane == null || lane.getSamples() == null) {
            // couldn't find a lane 1, so report this as NoProject
            DetailedObject noProject =
                new DetailedObject(
                    run,
                    instrumentName,
                    instrumentModel,
                    novaSeqLanesCount,
                    Integer.toString(laneNumber),
                    "NoProject",
                    BigDecimal.ONE,
                    DNA_LANE);
            detailedData.add(noProject);
            addSummaryData(summaryData, noProject);
            // skip further processing because there are no samples in the lane
            continue;
          }
        } else if (lane.getSamples() == null && !NOVASEQ.equals(instrumentModel)) {
          // NextSeq flowcells are 4 lanes but they can't be split. We report these as single-lane
          // runs and ignore lanes 2-4.
          if (NEXTSEQ.equals(instrumentModel) && laneNumber != 1) continue;

          // Sequencing done at UHN is rsynced into a folder called "UHN_HiSeqs". These lanes are
          // "UHN" instead of "NoProject".
          if (run.getRunDirectory() != null && run.getRunDirectory().contains("UHN_HiSeqs")) {
            DetailedObject uhn =
                new DetailedObject(
                    run,
                    instrumentName,
                    instrumentModel,
                    novaSeqLanesCount,
                    Integer.toString(laneNumber),
                    "UHN",
                    BigDecimal.ONE,
                    DNA_LANE);
            detailedData.add(uhn);
            addSummaryData(summaryData, uhn);
            // skip further processing because there are no samples in the lane
            continue;
          }
          // All other empty lanes should still be reported for billing purposes (may be run as
          // Sequencing as a service).
          // They are reported as DNA by default, though Pinery has no knowledge of the actual
          // contents.
          DetailedObject noProject =
              new DetailedObject(
                  run,
                  instrumentName,
                  instrumentModel,
                  novaSeqLanesCount,
                  Integer.toString(laneNumber),
                  "NoProject",
                  BigDecimal.ONE,
                  DNA_LANE);
          detailedData.add(noProject);
          addSummaryData(summaryData, noProject);
          // skip further processing because there are no samples in the lane
          continue;
        }

        for (RunDtoSample sam : lane.getSamples()) {
          SampleDto dilution = samplesById.get(sam.getId());
          // Reject if it's a TGL dilution on a NextSeq ("TGL for TGL").
          if (NEXTSEQ.equals(instrumentModel) && dilution.getProjectName().startsWith(TGL))
            continue;

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
        String laneContents = dnaInLane ? (rnaInLane ? MIXED_LANE : DNA_LANE) : RNA_LANE;

        for (Map.Entry<String, Integer> entry : projectsInLane.entrySet()) {
          BigDecimal projectPercent = getPercentProjectInLane(entry.getValue(), samplesInLane);
          DetailedObject newReport =
              new DetailedObject(
                  run,
                  instrumentName,
                  instrumentModel,
                  novaSeqLanesCount,
                  Integer.toString(laneNumber),
                  entry.getKey(),
                  projectPercent,
                  laneContents);
          detailedData.add(newReport);
          addSummaryData(summaryData, newReport);
        }
      }
    }
    return new ReportObject(detailedData, summaryData);
  }

  private static final String TGL = "TGL";

  /**
   * Some project names need to be processed (like the TGL projects, which should all be reported as
   * one project)
   */
  private String processProjectName(String projectName) {
    if (projectName.startsWith(TGL)) return TGL;
    return projectName;
  }

  private void addSummaryData(
      Map<String, Map<String, BigDecimal>> summary, DetailedObject newReport) {
    String summaryKey = getSummaryKey(newReport);
    if (summary.containsKey(summaryKey)) {
      // increment lane number+type for this project+instrument+sequencing parameters
      addSummaryLanes(
          summary.get(summaryKey), newReport.getLaneContents(), newReport.getProjLibsPercent());
    } else {
      // add new project+instrument and lane number+type
      Map<String, BigDecimal> newLanes =
          makeNewSummaryLanes(newReport.getLaneContents(), newReport.getProjLibsPercent());
      summary.put(summaryKey, newLanes);
    }
  }

  private BigDecimal getPercentProjectInLane(
      Integer numProjectLibraries, Integer numLaneLibraries) {
    return new BigDecimal(numProjectLibraries)
        .divide(new BigDecimal(numLaneLibraries), 1, RoundingMode.HALF_UP);
  }

  private String getSummaryKey(DetailedObject row) {
    return processProjectName(row.getProject())
        + "!"
        + row.getInstrumentModel()
        + "!"
        + row.getSequencingParameters();
  }

  private Map<String, BigDecimal> makeNewSummaryLanes(String laneType, BigDecimal laneCount) {
    Map<String, BigDecimal> lanes = new HashMap<>();
    lanes.put(DNA_LANE, BigDecimal.ZERO);
    lanes.put(RNA_LANE, BigDecimal.ZERO);
    lanes.put(MIXED_LANE, BigDecimal.ZERO);
    addSummaryLanes(lanes, laneType, laneCount);
    return lanes;
  }

  private void addSummaryLanes(
      Map<String, BigDecimal> lanes, String laneType, BigDecimal laneCount) {
    lanes.merge(laneType, laneCount, BigDecimal::add);
  }

  static final List<Map.Entry<String, Map<String, BigDecimal>>> listifySummary(
      Map<String, Map<String, BigDecimal>> summary) {
    // need to convert it to a list, because getRow() takes an index and the treemap doesn't yet
    // have one of those
    return new ArrayList<>(summary.entrySet());
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return Arrays.asList(
        new ColumnDefinition("Project"),
        new ColumnDefinition("Instrument Model"),
        new ColumnDefinition("Sequencing Parameters"),
        new ColumnDefinition(DNA_LANE),
        new ColumnDefinition(RNA_LANE),
        new ColumnDefinition(MIXED_LANE),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""),
        new ColumnDefinition(""));
  }

  private List<String> getDetailedHeadings() {
    return Arrays.asList(
        "Project",
        "Instrument Name",
        "Instrument Model",
        "Sequencing Parameters",
        "Run Status",
        "Run End Date",
        "Run Name",
        "Lane",
        DNA_LANE,
        RNA_LANE,
        MIXED_LANE);
  }

  @Override
  protected int getRowCount() {
    return completed.getDetailedData().size()
        + 3 // blank, "Detailed List (Lanes Completed)", headers
        + completed.getSummaryAsList().size()
        + 3 // blank, "Summary (Lanes Failed)", headers
        + failed.getSummaryAsList().size()
        + 3 // blank, "Detailed List (Lanes Failed)", headers
        + failed.getDetailedData().size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    if (rowNum < completed.getSummaryAsList().size()) {
      Map.Entry<String, Map<String, BigDecimal>> completedSummary =
          completed.getSummaryAsList().get(rowNum);
      return makeSummaryRow(completedSummary);
    }
    rowNum -= completed.getSummaryAsList().size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow(Arrays.asList("Detailed List (Completed Lanes)"));
    }
    rowNum -= 2;

    if (rowNum == 0) {
      return makeHeadingRow(getDetailedHeadings());
    }
    rowNum -= 1;

    if (rowNum < completed.getDetailedData().size()) {
      return makeDetailedRow(completed.getDetailedData().get(rowNum));
    }
    rowNum -= completed.getDetailedData().size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow(Arrays.asList("Summary (Failed Lanes)"));
    }
    rowNum -= 2;

    if (rowNum == 0) {
      return makeHeadingRow(
          getColumns().stream().map(ColumnDefinition::getHeading).collect(Collectors.toList()));
    }
    rowNum -= 1;
    if (rowNum < failed.getSummaryAsList().size()) {
      Map.Entry<String, Map<String, BigDecimal>> failedSummary =
          failed.getSummaryAsList().get(rowNum);
      return makeSummaryRow(failedSummary);
    }
    rowNum -= failed.getSummaryAsList().size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow(Arrays.asList("Detailed List (Failed Lanes)"));
    }
    rowNum -= 2;

    if (rowNum == 0) {
      return makeHeadingRow(getDetailedHeadings());
    }
    rowNum -= 1;

    return makeDetailedRow(failed.getDetailedData().get(rowNum));
  }

  private String[] makeSummaryRow(Map.Entry<String, Map<String, BigDecimal>> obj) {
    String[] row = new String[getColumns().size()];
    int i = -1;
    String summaryKey = obj.getKey();

    String[] key = summaryKey.split("!");
    // Project
    row[++i] = key[0];
    // Instrument Model
    row[++i] = key[1];
    // Sequencing Parameters
    row[++i] = key.length > 2 ? key[2] : "";
    // DNA Lanes
    row[++i] = obj.getValue().get(DNA_LANE).toPlainString();
    // RNA Lanes
    row[++i] = obj.getValue().get(RNA_LANE).toPlainString();
    // Mixed Lanes
    row[++i] = obj.getValue().get(MIXED_LANE).toPlainString();

    for (int j = i + 1; j < row.length; j++) {
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
    // Sequencing Parameters
    row[++i] = obj.getSequencingParameters();
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
