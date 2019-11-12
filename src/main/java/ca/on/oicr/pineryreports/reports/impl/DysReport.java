package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.AttributeDto;
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.RunDtoPosition;
import ca.on.oicr.ws.dto.RunDtoSample;
import ca.on.oicr.ws.dto.SampleDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class DysReport extends TableReport {

  private static class ReportObject {
    private final SampleDto dilution;
    private final RunDto run;
    private final RunDtoPosition lane;
    private final String targetedSequencing;

    public ReportObject(
        SampleDto dilution, RunDto run, RunDtoPosition lane, String targetedSequencing) {
      this.dilution = dilution;
      this.run = run;
      this.lane = lane;
      this.targetedSequencing = targetedSequencing;
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

    public String getTargetedSequencing() {
      return targetedSequencing;
    }
  }

  public static final String REPORT_NAME = "dys";
  public static final String CATEGORY = REPORT_CATEGORY_COUNTS;
  public static final String GSLE_USER = "Geospiza";

  private static final List<ColumnDefinition> COLUMNS =
      Collections.unmodifiableList(
          Arrays.asList(
              new ColumnDefinition("External.Identifier"),
              new ColumnDefinition("Stock.Alias"),
              new ColumnDefinition("Aliquot.Alias"),
              new ColumnDefinition("Library.Alias"),
              new ColumnDefinition("Run"),
              new ColumnDefinition("Barcode"),
              new ColumnDefinition("Lane"),
              new ColumnDefinition("Targeted.Sequencing"),
              new ColumnDefinition("Tissue.Material"),
              new ColumnDefinition("Group.ID"),
              new ColumnDefinition("Pool.Alias"),
              new ColumnDefinition("Num.Dilutions.In.Pool"),
              new ColumnDefinition("Pool.Date.Created"),
              new ColumnDefinition("Freezer.Box.position")));

  private static final String DYS = "DYS";
  Map<String, SampleDto> allSamplesById;
  List<ReportObject> reportData;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public Collection<Option> getOptions() {
    return Collections.emptySet();
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    // No options (could be expanded to report for any project, etc)
  }

  @Override
  public String getTitle() {
    return DYS + " Sequencing Report";
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> samples = pinery.getSample().all();
    allSamplesById = mapSamplesById(samples);

    List<ReportObject> rows = new ArrayList<>();
    List<RunDto> allRuns = pinery.getSequencerRun().all();
    // get the TargetedSequencing from the RunSampleDtos
    for (RunDto run : allRuns) {
      if ("Completed".equals(run.getState()) && run.getPositions() != null) {
        for (RunDtoPosition pos : run.getPositions()) {
          if (pos.getSamples() != null) {
            for (RunDtoSample sam : pos.getSamples()) {
              SampleDto dilution = allSamplesById.get(sam.getId());
              if (DYS.equals(dilution.getProjectName())) {
                rows.add(
                    new ReportObject(
                        dilution,
                        run,
                        pos,
                        getAttribute(sam.getAttributes(), "Targeted Resequencing")));
              }
            }
          }
        }
      }
    }
    rows.sort(byPoolDate);
    reportData = rows;
  }

  /** Sort descending by pool date */
  private final Comparator<ReportObject> byPoolDate =
      (o1, o2) -> {
        String o1Created = o1.getLane().getPoolCreated();
        String o2Created = o2.getLane().getPoolCreated();
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

  private String getAttribute(Set<AttributeDto> attributes, String target) {
    AttributeDto filtered =
        attributes.stream().filter(attr -> target.equals(attr.getName())).findAny().orElse(null);
    if (filtered == null) return "";
    return filtered.getValue();
  }

  @Override
  protected String[] getRow(int rowNum) {
    ReportObject obj = reportData.get(rowNum);
    String[] row = new String[COLUMNS.size()];
    SampleDto identity = getParent(obj.getDilution(), "Identity", allSamplesById);
    SampleDto tissue = getParent(obj.getDilution(), "Tissue", allSamplesById);
    SampleDto aliquot = getParent(obj.getDilution(), "Aliquot", allSamplesById);
    SampleDto stock = getParent(obj.getDilution(), "Stock", allSamplesById);

    String i1 = getUpstreamAttribute("Barcode", obj.getDilution(), allSamplesById);
    String i2 = getUpstreamAttribute("Barcode Two", obj.getDilution(), allSamplesById);
    String indices = makeIndicesString(i1, i2);

    int i = -1;
    // External.Identifier
    row[++i] = getAttribute(identity.getAttributes(), "External Name");
    // Stock.Alias
    row[++i] = stock.getName();
    // Aliquot.Alias
    row[++i] = aliquot.getName();
    // Library.Alias
    row[++i] = obj.getDilution().getName();
    // Run
    row[++i] = obj.getRun().getName();
    // Barcode
    row[++i] = indices;
    // Lane
    row[++i] = obj.getLane().getPosition().toString();
    // Targeted.Sequencing
    row[++i] = obj.getTargetedSequencing();
    // Tissue.Material
    row[++i] = getAttribute(tissue.getAttributes(), "Tissue Preparation");
    // Group.ID
    row[++i] = getUpstreamAttribute("Group ID", obj.getDilution(), allSamplesById);
    // Pool.Alias
    row[++i] = obj.getLane().getPoolName();
    // Num.Dilutions.In.Pool
    row[++i] = Integer.toString(obj.getLane().getSamples().size());
    // Pool.Date.Created
    row[++i] =
        obj.getLane().getPoolCreated() == null ? null : removeTime(obj.getLane().getPoolCreated());
    // Freezer.Box.position
    row[++i] = getUpstreamField(SampleDto::getStorageLocation, obj.getDilution(), allSamplesById);

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
}
