package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.GeneralUtils;
import ca.on.oicr.pineryreports.util.SampleUtils;
import ca.on.oicr.ws.dto.SampleDto;

public class PreciseCaseList extends TableReport {

  private static class Record {

    private SampleDto identity;
    private boolean screeningBlood;
    private boolean screeningUrine;
    private boolean screeningPellet;
    private boolean sixMonthBlood;
    private boolean oneYearBlood;
    private boolean oneYearUrine;
    private boolean oneYearPellet;
    private boolean eighteenMonthBlood;
    private boolean twoYearBlood;
    private boolean twoYearUrine;
    private boolean twoYearPellet;
    private boolean fourYearBlood;
    private boolean fiveYearBlood;
    private boolean sixYearBlood;
    private boolean bxTissue;
    private boolean rpTissue;

    public Record(SampleDto identity) {
      this.identity = identity;
    }

    public SampleDto getIdentity() {
      return identity;
    }

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

    public boolean hasFourYearBlood() {
      return fourYearBlood;
    }

    public void setFourYearBlood(boolean fourYearBlood) {
      this.fourYearBlood = fourYearBlood;
    }

    public boolean hasFiveYearBlood() {
      return fiveYearBlood;
    }

    public void setFiveYearBlood(boolean fiveYearBlood) {
      this.fiveYearBlood = fiveYearBlood;
    }

    public boolean hasSixYearBlood() {
      return sixYearBlood;
    }

    public void setSixYearBlood(boolean sixYearBlood) {
      this.sixYearBlood = sixYearBlood;
    }

    public boolean hasBxTissue() {
      return bxTissue;
    }

    public void setBxTissue(boolean bxTissue) {
      this.bxTissue = bxTissue;
    }

    public boolean hasRpTissue() {
      return rpTissue;
    }

    public void setRpTissue(boolean rpTissue) {
      this.rpTissue = rpTissue;
    }

  }

  private static final String TISSUE_ORIGIN_BLOOD = "Ly";
  private static final String TISSUE_ORIGIN_URINE = "Us";
  private static final String TISSUE_ORIGIN_PELLET = "Up";
  private static final String TISSUE_ORIGIN_PROSTATE = "Pr";

  private static enum Check {
    SCREENING_BLOOD(1, TISSUE_ORIGIN_BLOOD, Record::setScreeningBlood),
    SCREENING_URINE(1, TISSUE_ORIGIN_URINE, Record::setScreeningUrine),
    SCREENING_PELLET(1, TISSUE_ORIGIN_PELLET, Record::setScreeningPellet),
    SIX_MONTH_BLOOD(2, TISSUE_ORIGIN_BLOOD, Record::setSixMonthBlood),
    ONE_YEAR_BLOOD(3, TISSUE_ORIGIN_BLOOD, Record::setOneYearBlood),
    ONE_YEAR_URINE(3, TISSUE_ORIGIN_URINE, Record::setOneYearUrine),
    ONE_YEAR_PELLET(3, TISSUE_ORIGIN_PELLET, Record::setOneYearPellet),
    EIGHTEEN_MONTH_BLOOD(4, TISSUE_ORIGIN_BLOOD, Record::setEighteenMonthBlood),
    TWO_YEAR_BLOOD(5, TISSUE_ORIGIN_BLOOD, Record::setTwoYearBlood),
    TWO_YEAR_URINE(5, TISSUE_ORIGIN_URINE, Record::setTwoYearUrine),
    TWO_YEAR_PELLET(5, TISSUE_ORIGIN_PELLET, Record::setTwoYearPellet),
    FOUR_YEAR_BLOOD(10, TISSUE_ORIGIN_BLOOD, Record::setFourYearBlood),
    FIVE_YEAR_BLOOD(11, TISSUE_ORIGIN_BLOOD, Record::setFiveYearBlood),
    SIX_YEAR_BLOOD(12, TISSUE_ORIGIN_BLOOD, Record::setSixYearBlood),
    BX_TISSUE(8, TISSUE_ORIGIN_PROSTATE, Record::setBxTissue),
    RP_TISSUE(9, TISSUE_ORIGIN_PROSTATE, Record::setRpTissue);

    private final int timepoint;
    private final String tissueOrigin;
    private BiConsumer<Record, Boolean> setter;

    private Check(int timepoint, String tissueOrigin, BiConsumer<Record, Boolean> setter) {
      this.timepoint = timepoint;
      this.tissueOrigin = tissueOrigin;
      this.setter = setter;
    }

    public void apply(Record record, Collection<SampleDto> samples) {
      setter.accept(record, samples.stream().anyMatch(this::matches));
    }

    private boolean matches(SampleDto sample) {
      if (Objects.equals(sample.getSampleType(), SAMPLE_CATEGORY_IDENTITY)) {
        return false;
      }
      String sampleTissueOrigin = getAttribute(ATTR_TISSUE_ORIGIN, sample);
      if (!Objects.equals(sampleTissueOrigin, tissueOrigin)) {
        return false;
      }
      Integer timesReceived = getTimesReceived(sample.getName());
      return Objects.equals(timesReceived, timepoint);
    }
  }

  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(
      Arrays.asList(
          new ColumnDefinition("Trial ID"),
          new ColumnDefinition("Randomization Arm"),
          new ColumnDefinition("Screening Blood"),
          new ColumnDefinition("Screening Urine"),
          new ColumnDefinition("Screening Post DRE Urine Pellet"),
          new ColumnDefinition("6 Month Follow Up Blood"),
          new ColumnDefinition("1 Year Follow Up Blood"),
          new ColumnDefinition("1 Year Follow Up Urine"),
          new ColumnDefinition("1 Year Follow Up Post DRE Urine Pellet"),
          new ColumnDefinition("18 Month Follow Up Blood"),
          new ColumnDefinition("2 Year Follow Up Blood"),
          new ColumnDefinition("2 Year Follow Up Urine"),
          new ColumnDefinition("2 Year Follow Up Post DRE Urine Pellet"),
          new ColumnDefinition("4 Year Follow Up Blood"),
          new ColumnDefinition("5 Year Follow Up Blood"),
          new ColumnDefinition("6 Year Follow Up Blood"),
          new ColumnDefinition("Bx Tissue"),
          new ColumnDefinition("Rp Tissue")));

  public static final String REPORT_NAME = "precise-list";

  private final List<Record> records = new ArrayList<>();

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
    // No options
  }

  @Override
  public String getTitle() {
    return "PRECISE Case List";
  }

  @Override
  public String getCategory() {
    return GeneralUtils.REPORT_CATEGORY_INVENTORY;
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> allSamples = pinery.getSample().all();
    List<SampleDto> preciseIdentities = allSamples.stream()
        .filter(byProject("PRE"))
        .filter(bySampleCategory(SAMPLE_CATEGORY_IDENTITY))
        .collect(Collectors.toList());
    Map<String, SampleDto> samplesById = SampleUtils.mapSamplesById(allSamples);
    for (SampleDto identity : preciseIdentities) {
      Record record = new Record(identity);
      List<SampleDto> descendants = getDescendants(identity, samplesById);
      for (Check check : Check.values()) {
        check.apply(record, descendants);
      }
      records.add(record);
    }
    records.sort(Comparator.comparing(x -> getAttribute(ATTR_EXTERNAL_NAME, x.getIdentity())));
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return records.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    Record record = records.get(rowNum);
    return new String[] {
        getAttribute(ATTR_EXTERNAL_NAME, record.getIdentity()),
        "", // Randomization Arm - to be filled in later
        booleanString(record.hasScreeningBlood()),
        booleanString(record.hasScreeningUrine()),
        booleanString(record.hasScreeningPellet()),
        booleanString(record.hasSixMonthBlood()),
        booleanString(record.hasOneYearBlood()),
        booleanString(record.hasOneYearUrine()),
        booleanString(record.hasOneYearPellet()),
        booleanString(record.hasEighteenMonthBlood()),
        booleanString(record.hasTwoYearBlood()),
        booleanString(record.hasTwoYearUrine()),
        booleanString(record.hasTwoYearPellet()),
        booleanString(record.hasFourYearBlood()),
        booleanString(record.hasFiveYearBlood()),
        booleanString(record.hasSixYearBlood()),
        booleanString(record.hasBxTissue()),
        booleanString(record.hasRpTissue())
    };
  }

  private static String booleanString(boolean value) {
    return value ? "YES" : "NO";
  }

}
