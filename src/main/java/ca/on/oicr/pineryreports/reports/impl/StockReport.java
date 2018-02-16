package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.removeTime;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Sets;
import com.itextpdf.layout.property.TextAlignment;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;

/**
 * PCSI Report Request https://jira.oicr.on.ca/browse/GLT-1892
 */
public class StockReport extends TableReport {
  
  public static final String REPORT_NAME = "stock";
  
  private static final Option OPT_PROJECT = CommonOptions.project(true);
  private static final Option OPT_AFTER = CommonOptions.after(false);
  private static final Option OPT_BEFORE = CommonOptions.before(false);
  
  private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
  
  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(Arrays.asList(
      new ColumnDefinition("Stock Created", TextAlignment.CENTER),
      new ColumnDefinition("MISO Alias"),
      new ColumnDefinition("External Name"),
      new ColumnDefinition("Conc. (ng/µl)", 50f, TextAlignment.RIGHT),
      new ColumnDefinition("Vol. (µl)", 50f, TextAlignment.RIGHT),
      new ColumnDefinition("Total Yield (ng)", 70f, TextAlignment.RIGHT),
      new ColumnDefinition("Date Received", TextAlignment.CENTER),
      new ColumnDefinition("Institution")
  ));
  
  private String project;
  private String start;
  private String end;
  
  private List<SampleDto> stocks;
  private Map<String, SampleDto> allSamplesById;
  
  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_PROJECT, OPT_AFTER, OPT_BEFORE);
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    this.project = cmd.getOptionValue(OPT_PROJECT.getLongOpt());
    
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
    return project + " Stock Report, "
        + (start == null ? "Any Time" : start)
        + " - "
        + (end == null ? "Now" : end);
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> allSamples = pinery.getSample().all();
    allSamplesById = mapSamplesById(allSamples);
    stocks = filterReportableStocks(allSamples);
    stocks.sort(byReceiveDateAndName);
  }
  
  private List<SampleDto> filterReportableStocks(List<SampleDto> unfiltered) {
    Set<Predicate<SampleDto>> filters = Sets.newHashSet();
    filters.add(byProject(project));
    filters.add(bySampleCategory("Stock"));
    filters.add(byCreatedBetween(start, end));
    return filter(unfiltered, filters);
  }
  
  /**
   * Sort created date, then name
   */
  private final Comparator<SampleDto> byReceiveDateAndName = (dto1, dto2) -> {
    String dto1Created = removeTime(dto1.getCreatedDate());
    String dto2Created = removeTime(dto2.getCreatedDate());
    int byDate = 0;
    if (dto1Created == null) {
      if (dto2Created != null) {
        byDate = 1;
      }
    } else if (dto2Created == null) {
      byDate = -1;
    } else {
      byDate = dto1Created.compareTo(dto2Created);
    }
    return byDate == 0 ? dto1.getName().compareTo(dto2.getName()) : byDate;
  };

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return stocks.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    SampleDto stock = stocks.get(rowNum);
    String[] row = new String[COLUMNS.size()];
    
    row[0] = removeTime(stock.getCreatedDate());
    row[1] = stock.getName();
    row[2] = getUpstreamAttribute("External Name", stock, allSamplesById);
    Float concentration = stock.getConcentration();
    row[3] = concentration == null ? null : round(concentration, 2);
    Float volume = stock.getVolume();
    row[4] = volume == null ? null : round(volume, 2);
    row[5] = toStringOrNull(concentration == null || volume == null ? null : round(concentration * volume, 2));
    row[6] = getUpstreamAttribute("Receive Date", stock, allSamplesById);
    row[7] = getUpstreamAttribute("Institute", stock, allSamplesById);
    
    return row;
  }
  
  private static String toStringOrNull(Object obj) {
    return obj == null ? null : obj.toString();
  }

}
