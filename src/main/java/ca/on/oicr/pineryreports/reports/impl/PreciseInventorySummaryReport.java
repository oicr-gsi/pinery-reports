package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.REPORT_CATEGORY_INVENTORY;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class PreciseInventorySummaryReport extends TableReport {

  private static enum Timepoint {
    SCREENING(1), //
    SIX_MONTH(2), //
    ONE_YEAR(3), //
    EIGHTEEN_MONTH(4), //
    TWO_YEAR(5);

    private final int timesReceived;

    private Timepoint(int timesReceived) {
      this.timesReceived = timesReceived;
    }

    public static Timepoint get(int timesReceived) {
      return Arrays.stream(Timepoint.values())
          .filter(timepoint -> timepoint.timesReceived == timesReceived)
          .findAny()
          .orElse(null);
    }
  }

  private static enum SampleType {
    BLOOD(Arrays.asList("Pl", "Se", "Ly")), //
    URINE(Arrays.asList("Us")), //
    URINE_PELLET(Arrays.asList("Up"));

    private final List<String> tissueOrigins;

    private SampleType(List<String> originsTypes) {
      this.tissueOrigins = originsTypes;
    }

    public static SampleType get(String tissueOrigin) {
      return Arrays.stream(SampleType.values())
          .filter(type -> type.tissueOrigins.contains(tissueOrigin))
          .findAny()
          .orElse(null);
    }
  }

  private static class Case {

    private boolean screeningBlood = false;

    private boolean screeningUrine = false;

    private boolean screeningPellet = false;

    private boolean sixMonthBlood = false;

    private boolean oneYearBlood = false;

    private boolean oneYearUrine = false;

    private boolean oneYearPellet = false;

    private boolean eighteenMonthBlood = false;

    private boolean twoYearBlood = false;

    private boolean twoYearUrine = false;

    private boolean twoYearPellet = false;

    private boolean ffpeTissue = false;

    public boolean hasScreeningBlood() {
      return screeningBlood;
    }

    public void setScreeningBlood(boolean screeningBlood) {
      this.screeningBlood = screeningBlood;
    }

    public boolean hasScreeningUrine() {
      return screeningUrine;
    }

    public void setScreeningUrine(boolean screeningUrine) {
      this.screeningUrine = screeningUrine;
    }

    public boolean hasScreeningPellet() {
      return screeningPellet;
    }

    public void setScreeningPellet(boolean screeningPellet) {
      this.screeningPellet = screeningPellet;
    }

    public boolean hasSixMonthBlood() {
      return sixMonthBlood;
    }

    public void setSixMonthBlood(boolean sixMonthBlood) {
      this.sixMonthBlood = sixMonthBlood;
    }

    public boolean hasOneYearBlood() {
      return oneYearBlood;
    }

    public void setOneYearBlood(boolean oneYearBlood) {
      this.oneYearBlood = oneYearBlood;
    }

    public boolean hasOneYearUrine() {
      return oneYearUrine;
    }

    public void setOneYearUrine(boolean oneYearUrine) {
      this.oneYearUrine = oneYearUrine;
    }

    public boolean hasOneYearPellet() {
      return oneYearPellet;
    }

    public void setOneYearPellet(boolean oneYearPellet) {
      this.oneYearPellet = oneYearPellet;
    }

    public boolean hasEighteenMonthBlood() {
      return eighteenMonthBlood;
    }

    public void setEighteenMonthBlood(boolean eighteenMonthBlood) {
      this.eighteenMonthBlood = eighteenMonthBlood;
    }

    public boolean hasTwoYearBlood() {
      return twoYearBlood;
    }

    public void setTwoYearBlood(boolean twoYearBlood) {
      this.twoYearBlood = twoYearBlood;
    }

    public boolean hasTwoYearUrine() {
      return twoYearUrine;
    }

    public void setTwoYearUrine(boolean twoYearUrine) {
      this.twoYearUrine = twoYearUrine;
    }

    public boolean hasTwoYearPellet() {
      return twoYearPellet;
    }

    public void setTwoYearPellet(boolean twoYearPellet) {
      this.twoYearPellet = twoYearPellet;
    }

    public boolean hasFfpeTissue() {
      return ffpeTissue;
    }

    public void setFfpeTissue(boolean ffpeTissue) {
      this.ffpeTissue = ffpeTissue;
    }
  }

  public static final String REPORT_NAME = "precise-summary";
  public static final String REPORT_CATEGORY = REPORT_CATEGORY_INVENTORY;

  private static final List<ColumnDefinition> COLUMNS =
      Collections.unmodifiableList(
          Arrays.asList(
              new ColumnDefinition("PtID"),
              new ColumnDefinition("Screening Blood"),
              new ColumnDefinition("Screening Urine"),
              new ColumnDefinition("Screening Post DRE Urine Pellet"),
              new ColumnDefinition("6mo F/U Blood"),
              new ColumnDefinition("1yr F/U Blood"),
              new ColumnDefinition("1yr F/U Urine"),
              new ColumnDefinition("1yr F/U Post DRE Urine Pellet"),
              new ColumnDefinition("18mo F/U Blood"),
              new ColumnDefinition("2yr F/U Blood"),
              new ColumnDefinition("2yr F/U Urine"),
              new ColumnDefinition("2yr F/U Post DRE Urine Pellet"),
              new ColumnDefinition("FFPE Tissue")));

  private Map<String, Case> cases;
  private List<String> orderedKeys;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Collections.emptySet();
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    // no options to process
  }

  @Override
  public String getTitle() {
    return "PRECISE Inventory Summary";
  }

  @Override
  public String getCategory() {
    return REPORT_CATEGORY;
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> samples =
        pinery.getSample().allFiltered(new SamplesFilter().withProjects(Lists.newArrayList("PRE")));
    Map<String, SampleDto> samplesMap = mapSamplesById(samples);

    cases =
        samples.stream()
            .filter(bySampleCategory(SAMPLE_CATEGORY_IDENTITY))
            .collect(Collectors.toMap(s -> getAttribute(ATTR_EXTERNAL_NAME, s), s -> new Case()));
    orderedKeys = cases.keySet().stream().sorted().collect(Collectors.toList());

    List<SampleDto> items =
        samples.stream()
            .filter(
                bySampleCategory(SAMPLE_CATEGORY_TISSUE)
                    .or(bySampleCategory(SAMPLE_CATEGORY_TISSUE_PROCESSING)))
            .filter(byEmpty(false))
            .filter(byDistributed().negate())
            .collect(Collectors.toList());

    for (SampleDto item : items) {
      String externalName = getUpstreamAttribute(ATTR_EXTERNAL_NAME, item, samplesMap);
      Case record = cases.get(externalName);
      recordItem(record, item, samplesMap);
    }
  }

  private void recordItem(Case record, SampleDto item, Map<String, SampleDto> samplesMap) {
    int timesReceived = getTimesReceived(item.getName());
    String tissueOrigin = getAttribute(ATTR_TISSUE_ORIGIN, item);
    if (tissueOrigin == null) {
      tissueOrigin = getUpstreamAttribute(ATTR_TISSUE_ORIGIN, item, samplesMap);
    }

    Timepoint timepoint = Timepoint.get(timesReceived);
    SampleType sampleType = SampleType.get(tissueOrigin);
    if (timepoint == null) {
      String tissueType = getAttribute(ATTR_TISSUE_TYPE, item);
      if (tissueType == null) {
        tissueType = getUpstreamAttribute(ATTR_TISSUE_TYPE, item, samplesMap);
      }
      if (isFfpeTissue(timesReceived, tissueType, tissueOrigin)) {
        record.setFfpeTissue(true);
      }
    } else if (sampleType != null) {
      switch (timepoint) {
        case SCREENING:
          switch (sampleType) {
            case BLOOD:
              record.setScreeningBlood(true);
              break;
            case URINE:
              record.setScreeningUrine(true);
              break;
            case URINE_PELLET:
              record.setScreeningPellet(true);
              break;
          }
          break;
        case SIX_MONTH:
          if (sampleType == SampleType.BLOOD) {
            record.setSixMonthBlood(true);
          }
          break;
        case ONE_YEAR:
          switch (sampleType) {
            case BLOOD:
              record.setOneYearBlood(true);
              break;
            case URINE:
              record.setOneYearUrine(true);
              break;
            case URINE_PELLET:
              record.setOneYearPellet(true);
              break;
          }
          break;
        case EIGHTEEN_MONTH:
          if (sampleType == SampleType.BLOOD) {
            record.setEighteenMonthBlood(true);
          }
          break;
        case TWO_YEAR:
          switch (sampleType) {
            case BLOOD:
              record.setTwoYearBlood(true);
              break;
            case URINE:
              record.setTwoYearUrine(true);
              break;
            case URINE_PELLET:
              record.setTwoYearPellet(true);
              break;
          }
          break;
      }
    }
  }

  private boolean isFfpeTissue(int timesReceived, String tissueType, String tissueOrigin) {
    return timesReceived >= 6
        && "Pr".equals(tissueOrigin)
        && ("P".equals(tissueType) || "R".equals(tissueType));
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return cases.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    String[] row = new String[COLUMNS.size()];
    String externalName = orderedKeys.get(rowNum);
    Case record = cases.get(externalName);

    int i = -1;
    row[++i] = externalName;
    row[++i] = yesOrNo(record.hasScreeningBlood());
    row[++i] = yesOrNo(record.hasScreeningUrine());
    row[++i] = yesOrNo(record.hasScreeningPellet());
    row[++i] = yesOrNo(record.hasSixMonthBlood());
    row[++i] = yesOrNo(record.hasOneYearBlood());
    row[++i] = yesOrNo(record.hasOneYearUrine());
    row[++i] = yesOrNo(record.hasOneYearPellet());
    row[++i] = yesOrNo(record.hasEighteenMonthBlood());
    row[++i] = yesOrNo(record.hasTwoYearBlood());
    row[++i] = yesOrNo(record.hasTwoYearUrine());
    row[++i] = yesOrNo(record.hasTwoYearPellet());
    row[++i] = yesOrNo(record.hasFfpeTissue());

    return row;
  }

  private static String yesOrNo(boolean booleanValue) {
    return booleanValue ? "Yes" : "No";
  }
}
