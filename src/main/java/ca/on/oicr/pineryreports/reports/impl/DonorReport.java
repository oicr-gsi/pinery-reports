package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.REPORT_CATEGORY_COUNTS;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Sets;
import com.itextpdf.layout.property.TextAlignment;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/** Donor Report https://jira.oicr.on.ca/browse/GR-639 */
public class DonorReport extends TableReport {

  public static final String REPORT_NAME = "donor";
  public static final String CATEGORY = REPORT_CATEGORY_COUNTS;
  private static final Option OPT_PROJECT = CommonOptions.project(true);
  private String project;

  private final List<ColumnDefinition> columns =
      Collections.unmodifiableList(
          Arrays.asList(
              new ColumnDefinition("OICR Name", 150f, TextAlignment.LEFT),
              new ColumnDefinition("MISO ID"),
              new ColumnDefinition("External Name"),
              new ColumnDefinition("Description"),
              new ColumnDefinition("Lab"),
              new ColumnDefinition("Sex")));

  private List<SampleDto> identities;
  private List<SampleDto> tissues;

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
    this.project = cmd.getOptionValue(OPT_PROJECT.getLongOpt()).trim();
    recordOptionsUsed(cmd);
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public String getTitle() {
    return project + " Donor Report " + new SimpleDateFormat("yyyy-MM-dd").format(new Date());
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> allSamples = pinery.getSample().all();
    identities =
        filter(filter(allSamples, byProject(project)), bySampleCategory(SAMPLE_CATEGORY_IDENTITY));
    tissues =
        filter(filter(allSamples, byProject(project)), bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    identities.sort((dto1, dto2) -> dto1.getName().compareTo(dto2.getName()));
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return columns;
  }

  @Override
  protected int getRowCount() {
    return identities.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    SampleDto identity = identities.get(rowNum);
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
    Set<String> attrs = getChildAttributes(attr, identity, tissues);
    if (attrs.isEmpty()) return "";
    return String.join(", ", attrs);
  }
}
