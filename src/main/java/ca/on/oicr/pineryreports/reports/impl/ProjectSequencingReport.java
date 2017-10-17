package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.RunDtoPosition;
import ca.on.oicr.ws.dto.RunDtoSample;
import ca.on.oicr.ws.dto.SampleDto;
import ca.on.oicr.ws.dto.UserDto;

import com.google.common.base.Functions;
import com.google.common.collect.Sets;

public class ProjectSequencingReport extends TableReport {
  
  private static class ReportObject {
    private final SampleDto dilution;
    private final RunDto run;
    private final RunDtoPosition lane;
    private final UserDto poolCreator;
    
    public ReportObject(SampleDto dilution, RunDto run, RunDtoPosition lane, UserDto poolCreator) {
      this.dilution = dilution;
      this.run = run;
      this.lane = lane;
      this.poolCreator = poolCreator;
    }

    public SampleDto getDilution() {
      return dilution;
    }

    public RunDto getRun() {
      return run;
    }

    public RunDtoPosition getLane() {
      return lane;
    }
    
    public UserDto getPoolCreator() {
      return poolCreator;
    }
  }
  
  public static final String REPORT_NAME = "sequencing";
  private static final String OPT_PROJECT = "project";
  
  public static final String GSLE_USER = "Geospiza";

  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(Arrays.asList(
      new ColumnDefinition("Stock"),
      new ColumnDefinition("Pool"),
      new ColumnDefinition("Pool Creator"),
      new ColumnDefinition("Pool Created"),
      new ColumnDefinition("Dilutions"),
      new ColumnDefinition("Library"),
      new ColumnDefinition("Run"),
      new ColumnDefinition("Lane"),
      new ColumnDefinition("Index")
  ));
  
  private String project;
  Map<String, SampleDto> allSamplesById;
  List<ReportObject> reportData;
  
  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    Set<Option> opts = Sets.newHashSet();
    
    opts.add(Option.builder()
        .longOpt(OPT_PROJECT)
        .hasArg()
        .argName("code")
        .required()
        .desc("Project to report on (required)")
        .build());
    
    return opts;
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    this.project = cmd.getOptionValue(OPT_PROJECT);
  }

  @Override
  public String getTitle() {
    return project + " Sequencing Report";
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> samples = pinery.getSample().all();
    allSamplesById = mapSamplesById(samples);
    Map<Integer, UserDto> allUsersById = pinery.getUser().all().stream()
        .collect(Collectors.toMap(UserDto::getId, Functions.identity()));
    
    List<ReportObject> rows = new ArrayList<>();
    List<RunDto> allRuns = pinery.getSequencerRun().all();
    for (RunDto run : allRuns) {
      if ("Completed".equals(run.getState()) && run.getPositions() != null) {
        for (RunDtoPosition pos : run.getPositions()) {
          if (pos.getSamples() != null) {
            for (RunDtoSample sam : pos.getSamples()) {
              SampleDto dilution = allSamplesById.get(sam.getId());
              if (project.equals(dilution.getProjectName())) {
                UserDto poolCreator = pos.getPoolCreatedById() == null ? null : allUsersById.get(pos.getPoolCreatedById());
                rows.add(new ReportObject(dilution, run, pos, poolCreator));
              }
            }
          }
        }
      }
    }
    rows.sort(byPoolDate);
    reportData = rows;
  }
  
  /**
   * Sort descending by pool date
   */
  private final Comparator<ReportObject> byPoolDate = (o1, o2) -> {
    String o1Created = removeTime(o1.getLane().getPoolCreated());
    String o2Created = removeTime(o2.getLane().getPoolCreated());
    if (o1Created == null) {
      if (o2Created != null) {
        return -1;
      }
    } else if (o2Created == null) {
      if (o1Created != null) {
        return 1;
      }
    } else {
      return o1Created.compareTo(o2Created) * -1;
    }
    return 0;
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
    SampleDto stock = getParent(obj.getDilution(), "Stock", allSamplesById);
    
    String i1 = getUpstreamAttribute("Barcode", obj.getDilution(), allSamplesById);
    String i2 = getUpstreamAttribute("Barcode Two", obj.getDilution(), allSamplesById);
    String indices = makeIndicesString(i1, i2);
    
    int i = -1;
    // Stock
    row[++i] = stock.getName();
    // Pool
    row[++i] = obj.getLane().getPoolName();
    // Pool Creator
    row[++i] = getUserName(obj.getPoolCreator());
    // Pool Created
    row[++i] = obj.getLane().getPoolCreated() == null ? null : removeTime(obj.getLane().getPoolCreated());
    // Dilutions
    row[++i] = Integer.toString(obj.getLane().getSamples().size());
    // Library
    row[++i] = obj.getDilution().getName();
    // Run
    row[++i] = obj.getRun().getName();
    // Lane
    row[++i] = obj.getLane().getPosition().toString();
    // Index
    row[++i] = indices;
    
    return row;
  }
  
  private static String makeIndicesString(String index1, String index2) {
    if (index1 == null) {
      return "";
    } else if (index2 == null) {
      return index1;
    } else {
      return index1 + ", " + index2;
    }
  }
  
  private String getUserName(UserDto user) {
    if (user == null) return null;
    if (GSLE_USER.equals(user.getFirstname()) && GSLE_USER.equals(user.getLastname())) return GSLE_USER;
    return user.getFirstname() + " " + user.getLastname();
  }

}
