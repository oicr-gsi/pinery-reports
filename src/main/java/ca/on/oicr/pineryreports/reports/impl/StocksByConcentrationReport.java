package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.REPORT_CATEGORY_QC;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class StocksByConcentrationReport extends TableReport {

  public static final String REPORT_NAME = "stocks-by-concentration";
  private static final Option OPT_PROJECT = CommonOptions.project(true);

  private static final List<ColumnDefinition> COLUMNS =
      Collections.unmodifiableList(
          Arrays.asList(
              new ColumnDefinition("Sample ID"),
              new ColumnDefinition("Alias"),
              new ColumnDefinition("Description"),
              new ColumnDefinition("Concentration"),
              new ColumnDefinition("Volume"),
              new ColumnDefinition("Total Yield"),
              new ColumnDefinition("Date Received"),
              new ColumnDefinition("Date Created"),
              new ColumnDefinition("Date Modified")));

  private String project;
  private List<SampleDto> concentration50;
  private List<SampleDto> concentration100;
  private List<SampleDto> concentration150;

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
  }

  @Override
  public String getTitle() {
    return project + " Stocks by Concentration";
  }

  @Override
  public String getCategory() {
    return REPORT_CATEGORY_QC;
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> stocks =
        pinery
            .getSample()
            .allFiltered(new SamplesFilter().withProjects(Lists.newArrayList(project)))
            .stream()
            .filter(bySampleCategory(SAMPLE_CATEGORY_STOCK))
            .filter(sample -> sample.getConcentration() != null)
            .collect(Collectors.toList());
    concentration50 =
        stocks
            .stream()
            .filter(sample -> sample.getConcentration() == 50f)
            .collect(Collectors.toList());
    concentration100 =
        stocks
            .stream()
            .filter(sample -> sample.getConcentration() == 100f)
            .collect(Collectors.toList());
    concentration150 =
        stocks
            .stream()
            .filter(sample -> sample.getConcentration() == 150f)
            .collect(Collectors.toList());
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return concentration50.size() + concentration100.size() + concentration150.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    SampleDto stock = null;
    if (rowNum < concentration50.size()) {
      stock = concentration50.get(rowNum);
    } else if (rowNum < (concentration50.size() + concentration100.size())) {
      stock = concentration100.get(rowNum - concentration50.size());
    } else {
      stock = concentration150.get(rowNum - concentration50.size() - concentration100.size());
    }

    String[] row = new String[COLUMNS.size()];
    row[0] = stock.getId();
    row[1] = stock.getName();
    row[2] = stock.getDescription();
    row[3] = round(stock.getConcentration(), 2);
    row[4] = round(stock.getVolume(), 2);
    row[5] = getYield(stock);
    row[6] = getAttribute(ATTR_RECEIVE_DATE, stock);
    row[7] = stock.getCreatedDate();
    row[8] = stock.getModifiedDate();
    return row;
  }

  private String getYield(SampleDto sample) {
    if (sample.getVolume() == null) {
      return "n/a";
    }
    return round(sample.getVolume() * sample.getConcentration(), 2);
  }
}
