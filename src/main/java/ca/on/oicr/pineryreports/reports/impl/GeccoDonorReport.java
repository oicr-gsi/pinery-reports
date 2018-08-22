package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.itextpdf.layout.property.TextAlignment;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.SampleDto;

/**
 * 
 * GECCO Donor Report https://jira.oicr.on.ca/browse/GR-639
 *
 */
public class GeccoDonorReport extends TableReport {
  
  public static final String REPORT_NAME = "gecco-donor";
  
  private final List<ColumnDefinition> columns = Collections.unmodifiableList(Arrays.asList(
      new ColumnDefinition("OICR Name", 150f, TextAlignment.LEFT),
      new ColumnDefinition("MISO ID"),
      new ColumnDefinition("External Name"),
      new ColumnDefinition("Description"),
      new ColumnDefinition("Lab"),
      new ColumnDefinition("Sex")
  ));

  private List<SampleDto> geccoIdentities;
  private List<SampleDto> geccoTissues;
  
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
    // No options (could be expanded to report for any project, etc)
  }

  @Override
  public String getTitle() {
    return "GECCO Donor Report";
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> allSamples = pinery.getSample().all();
    geccoIdentities = filter(filter(allSamples, byProject("GECCO")), bySampleCategory(SAMPLE_CATEGORY_IDENTITY));
    geccoTissues = filter(filter(allSamples, byProject("GECCO")), bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    geccoIdentities.sort((dto1, dto2) -> dto1.getName().compareTo(dto2.getName()));
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return columns;
  }

  @Override
  protected int getRowCount() {
    return geccoIdentities.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    SampleDto identity = geccoIdentities.get(rowNum);
    String[] row = new String[columns.size()];
    
    row[0] = identity.getName();
    row[1] = identity.getId();
    row[2] = getAttribute(ATTR_EXTERNAL_NAME, identity);
    row[3] = identity.getDescription();
    row[4] = getChildAttrsString(ATTR_INSTITUTE, identity);
    row[5] = getAttribute(ATTR_SEX, identity);
    return row;
  }

  public String getChildAttrsString(String attr, SampleDto identity) {
    Set<String> attrs = getChildAttributes(attr, identity, geccoTissues);
    if (attrs.isEmpty()) return "";
    return String.join(",", attrs);
   }

}
