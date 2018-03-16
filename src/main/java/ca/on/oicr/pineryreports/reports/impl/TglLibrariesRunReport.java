package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
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
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.RunDtoPosition;
import ca.on.oicr.ws.dto.RunDtoSample;
import ca.on.oicr.ws.dto.SampleDto;

public class TglLibrariesRunReport extends TableReport {
  
  private static class ReportObject {
    private final SampleDto library;
    private final RunDto run;
    private final String instrument;
    
    public ReportObject(SampleDto library, RunDto run, String instrument) {
      this.library = library;
      this.run = run;
      this.instrument = instrument;
    }

    public SampleDto getLibrary() {
      return library;
    }

    public RunDto getRun() {
      return run;
    }

    public String getInstrument() {
      return instrument;
    }
  }
  
  public static final String REPORT_NAME = "tgl-libraries-run";
  private static final Option OPT_AFTER = CommonOptions.after(false);
  private static final Option OPT_BEFORE = CommonOptions.before(false);
  private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
  private static final Pattern tglPattern = Pattern.compile("^(TGL\\d+)|(OCT$)");

  private String start;
  private String end;

  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(Arrays.asList(
      new ColumnDefinition("Library creation date"),
      new ColumnDefinition("Run completion date"),
      new ColumnDefinition("Instrument"),
      new ColumnDefinition("Run name"),
      new ColumnDefinition("Project"),
      new ColumnDefinition("Library"),
      new ColumnDefinition("Seq strategy")));

  Map<String, SampleDto> allSamplesById;
  List<ReportObject> reportData;

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
    return "TGL Libraries Run "
        + (start == null ? "Any Time" : start)
        + " - "
        + (end == null ? "Now" : end);
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    allSamplesById = mapSamplesById(pinery.getSample().all());
    Map<Integer, InstrumentDto> instrumentsById = pinery.getInstrument().all().stream()
        .collect(Collectors.toMap(InstrumentDto::getId, dto -> dto));
    // filter runs within the date range
    Set<RunDto> newRuns = pinery.getSequencerRun().all().stream().filter(byEndedBetween(start, end)).collect(Collectors.toSet());
    List<ReportObject> rows = new ArrayList<>();
    
    for (RunDto run : newRuns) {
      String instrumentName = getInstrumentName(run.getInstrumentId(), instrumentsById);
      if (run.getPositions() == null) continue;
      for (RunDtoPosition lane : run.getPositions()) {
        if (lane.getSamples() == null) continue;
        for (RunDtoSample sam : lane.getSamples()) {
          SampleDto library = getParent(allSamplesById.get(sam.getId()), allSamplesById);
          if (!isTglLibrary(library)) continue;
          rows.add(new ReportObject(library, run, instrumentName));
        }
      }
    }
    rows.sort(byLibraryCreationDate);
    reportData = rows;
  }

  private boolean isTglLibrary(SampleDto library) {
    return tglPattern.matcher(library.getProjectName()).matches();
  }

  /**
   * Sort descending by library creation date
   */
  private final Comparator<ReportObject> byLibraryCreationDate = (o1, o2) -> {
    String o1Created = o1.getLibrary().getCreatedDate();
    String o2Created = o2.getLibrary().getCreatedDate();
    return o1Created.compareTo(o2Created) * -1;
  };

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return reportData.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    ReportObject obj = reportData.get(rowNum);
    String[] row = new String[COLUMNS.size()];

    int i = -1;
    // Lib creation date
    row[++i] = removeTime(obj.getLibrary().getCreatedDate());
    // Run completion date
    row[++i] = removeTime(obj.getRun().getCompletionDate());
    // Instrument name
    row[++i] = obj.getInstrument();
    // Run name
    row[++i] = obj.getRun().getName();
    // Project
    row[++i] = obj.getLibrary().getProjectName();
    // Library name
    row[++i] = obj.getLibrary().getName();
    // Library Design
    row[++i] = getAttribute("Source Template Type", obj.getLibrary());

    return row;
  }

}
