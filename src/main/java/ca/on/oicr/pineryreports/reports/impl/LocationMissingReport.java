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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class LocationMissingReport extends TableReport {

  public static final String REPORT_NAME = "location-missing";
  public static final String CATEGORY = REPORT_CATEGORY_QC;
  private List<SampleDto> locationMissing = new ArrayList<>();

  private static final Option OPT_AFTER = CommonOptions.after(false);
  private static final Option OPT_BEFORE = CommonOptions.before(false);
  private static final Option OPT_PROJECT = CommonOptions.project(false);
  private static final Option OPT_USER_IDS = CommonOptions.users(false);

  private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";

  private static final List<ColumnDefinition> COLUMNS =
      Collections.unmodifiableList(
          Arrays.asList(
              new ColumnDefinition("Sample ID"),
              new ColumnDefinition("Sample Alias"),
              new ColumnDefinition("Sample Barcode")));

  private String start;
  private String end;
  private String project;
  private final List<Integer> userIds = new ArrayList<>();

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_AFTER, OPT_BEFORE, OPT_PROJECT, OPT_USER_IDS);
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

    if (cmd.hasOption(OPT_PROJECT.getLongOpt())) {
      this.project = cmd.getOptionValue(OPT_PROJECT.getLongOpt());
    }

    if (cmd.hasOption(OPT_USER_IDS.getLongOpt())) {
      String[] users = cmd.getOptionValue(OPT_USER_IDS.getLongOpt()).split(",");
      for (String user : users) {

        if (user != null && !"".equals(user)) {
          userIds.add(Integer.valueOf(user));
        }
      }
    }
    recordOptionsUsed(cmd);
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public String getTitle() {
    return "Samples with no location for "
        + (project == null ? "all projects" : project)
        + " Report";
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> samples;
    if (project == null) {
      samples = pinery.getSample().all();
    } else {
      samples =
          pinery
              .getSample()
              .allFiltered(new SamplesFilter().withProjects(Lists.newArrayList(project)));
    }
    Map<String, SampleDto> allSamples = mapSamplesById(samples);

    locationMissing =
        filter(
            samples,
            Arrays.asList(
                byNullLocation(),
                byCreator(userIds),
                byReceivedBetween(start, end),
                byNotIdentity(),
                byReceivedOrChildOfReceived(allSamples),
                byNonZeroVolume()));
    locationMissing.sort(
        new Comparator<SampleDto>() {
          @Override
          public int compare(SampleDto s1, SampleDto s2) {
            int compare = s1.getName().toUpperCase().compareTo(s2.getName().toUpperCase());
            if (compare == 0) {
              return s1.getId().compareTo(s2.getId());
            } else {
              return compare;
            }
          }
        });
  }

  private static Predicate<SampleDto> byNullLocation() {
    return dto -> dto.getStorageLocation() == null;
  }

  private static Predicate<SampleDto> byNotIdentity() {
    return bySampleCategory(SAMPLE_CATEGORY_IDENTITY).negate();
  }

  private static Predicate<SampleDto> byReceivedOrChildOfReceived(
      Map<String, SampleDto> allSamples) {
    return dto -> {
      String receiveDate = getAttribute(ATTR_RECEIVE_DATE, dto);
      if (receiveDate != null) {
        return true;
      } else {
        return getUpstreamAttribute(ATTR_RECEIVE_DATE, dto, allSamples) != null;
      }
    };
  }

  private static Predicate<SampleDto> byNonZeroVolume() {
    return dto -> dto.getVolume() == null || Float.compare(dto.getVolume(), 0f) > 0;
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return locationMissing.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    SampleDto sam = locationMissing.get(rowNum);
    String[] row = new String[COLUMNS.size()];

    int i = -1;
    // Sample ID
    row[++i] = sam.getId();
    // Sample Alias
    row[++i] = sam.getName();
    // Sample Barcode
    row[++i] = sam.getTubeBarcode() == null ? "" : sam.getTubeBarcode();

    return row;
  }
}
