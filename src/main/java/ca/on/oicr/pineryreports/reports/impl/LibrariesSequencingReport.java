package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.RunDtoPosition;
import ca.on.oicr.ws.dto.RunDtoSample;
import ca.on.oicr.ws.dto.SampleDto;

public class LibrariesSequencingReport extends TableReport {
  
  private static class ReportObject {
    private final String libraryName;
    private final String libraryType;
    private final String receivedDate;
    private final String identityName;
    private final String externalName;
    private final Set<RunDto> runs = new HashSet<>();
    
    public ReportObject(SampleDto library, SampleDto identity) {
      this.libraryName = library.getName();
      this.libraryType = getAttribute(ATTR_SOURCE_TEMPLATE_TYPE, library);
      this.receivedDate = getAttribute(ATTR_RECEIVE_DATE, library) == null ? getAttribute(ATTR_CREATION_DATE, library)
          : getAttribute(ATTR_RECEIVE_DATE, library);
      this.identityName = identity.getName();
      this.externalName = getAttribute(ATTR_EXTERNAL_NAME, identity);
    }

    public String getLibraryName() {
      return libraryName;
    }

    public String getLibraryType() {
      return libraryType;
    }

    public String getReceivedDate() {
      return receivedDate;
    }

    public String getIdentityName() {
      return identityName;
    }

    public String getExternalName() {
      return externalName;
    }

    public Set<String> getRunNames() {
      return runs.stream().map(run -> run.getName()).collect(Collectors.toSet());
    }
    
    public void addRun(RunDto run) {
      this.runs.add(run);
    }
  }
  
  public static final String REPORT_NAME = "libraries-sequencing";
  public static final String CATEGORY = REPORT_CATEGORY_INVENTORY;
  private static final Option OPT_PROJECT = CommonOptions.project(true);

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }
  
  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_PROJECT);
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    this.project = cmd.getOptionValue(OPT_PROJECT.getLongOpt());
    recordOptionsUsed(cmd);
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public String getTitle() {
    return project + " Libraries Sequencing Report";
  }
  
  private final Map<String, ReportObject> detailedData = new TreeMap<>();
  private List<Map.Entry<String, ReportObject>> detailedList;
  private String project;

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    Map<String, SampleDto> samplesById = mapSamplesById(pinery.getSample().all());
    Set<RunDto> newRuns = pinery.getSequencerRun().all().stream().collect(Collectors.toSet());
    Set<RunDto> failedRuns = newRuns.stream().filter(run -> RUN_FAILED.equals(run.getState())).collect(Collectors.toSet());
    newRuns.removeAll(failedRuns);
    
    getReportAndSummaryData(newRuns, samplesById);
    detailedList = listify(detailedData);
  }
  
  private void getReportAndSummaryData(Collection<RunDto> runs, Map<String, SampleDto> samplesById) {
    for (RunDto run : runs) {
      if (run.getPositions() == null) continue;
      for (RunDtoPosition lane : run.getPositions()) {
        if (lane.getSamples() == null) {
          continue;
        }
        for (RunDtoSample sam : lane.getSamples()) {
          SampleDto dilution = samplesById.get(sam.getId());
          SampleDto library = getParent(dilution, samplesById);
          if (!library.getProjectName().equals(project)) {
            continue;
          }
          SampleDto identity = getParent(library, SAMPLE_CATEGORY_IDENTITY, samplesById);
          ReportObject lib;
          if (detailedData.get(library.getName()) == null) {
            lib = new ReportObject(library, identity);
          } else {
            lib = detailedData.get(library.getName());
          }
          lib.addRun(run);
          detailedData.put(library.getName(), lib);
        }
      }
    }
  }
  
  static final List<Map.Entry<String, ReportObject>> listify(Map<String, ReportObject> detailed) {
    // need to convert it to a list, because getRow() takes an index and the treemap doesn't yet have one of those
    return new ArrayList<>(detailed.entrySet());
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return Arrays.asList(
        new ColumnDefinition("Library"),
        new ColumnDefinition("Lib. Type"),
        new ColumnDefinition("Identity"),
        new ColumnDefinition("External Identifier"),
        new ColumnDefinition("Received Date"),
        new ColumnDefinition("Runs")
    );
  }

  @Override
  protected int getRowCount() {
    return detailedList.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    String[] row = new String[getColumns().size()];
    ReportObject obj = detailedList.get(rowNum).getValue();

    int i = -1;
    row[++i] = obj.getLibraryName();
    row[++i] = obj.getLibraryType();
    row[++i] = obj.getIdentityName();
    row[++i] = obj.getExternalName();
    row[++i] = obj.getReceivedDate();
    row[++i] = String.join(", ", obj.getRunNames());
    return row;
  }


}
