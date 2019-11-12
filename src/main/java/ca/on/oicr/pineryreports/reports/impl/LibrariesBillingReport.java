package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class LibrariesBillingReport extends TableReport {

  private static class SummaryObject {
    private final String project;
    private final String kit;
    private Integer libsCount = 0;

    public SummaryObject(String project, String kit) {
      this.project = project;
      this.kit = kit;
      this.libsCount = 1;
    }

    public String getProject() {
      return project;
    }

    public String getKit() {
      return kit;
    }

    public Integer getLibrariesCount() {
      return libsCount;
    }

    public void addOneLibrary() {
      libsCount += 1;
    }

    private static final Comparator<SummaryObject> summaryComparator =
        (SummaryObject o1, SummaryObject o2) -> {
          if (o1.getProject().equals(o2.getProject())) {
            return o2.getKit().compareTo(o2.getKit());
          } else {
            // sort on this primarily
            return o1.getProject().compareTo(o2.getProject());
          }
        };
  }

  private static class DetailedObject {
    private final String creationDate;
    private final String kit;
    private final String libraryAlias;
    private final String libraryDesignCode;
    private final String project;

    public DetailedObject(
        String project,
        String kit,
        String creationDate,
        String libraryAlias,
        String libraryDesignCode) {
      this.project = project;
      this.kit = kit;
      this.creationDate = removeTime(creationDate);
      this.libraryAlias = libraryAlias;
      this.libraryDesignCode = libraryDesignCode;
    }

    public String getCreationDate() {
      return creationDate;
    }

    public String getKit() {
      return kit;
    }

    public String getLibrary() {
      return libraryAlias;
    }

    public String getLibraryDesignCode() {
      return libraryDesignCode;
    }

    public String getProject() {
      return project;
    }

    private static final Comparator<DetailedObject> detailedComparator =
        (DetailedObject o1, DetailedObject o2) -> {
          if (o1.getProject().equals(o2.getProject())) {
            return o2.getCreationDate().compareTo(o2.getCreationDate());
          } else {
            // sort on this primarily
            return o1.getProject().compareTo(o2.getProject());
          }
        };
  }

  public static final String REPORT_NAME = "libraries-billing";
  public static final String CATEGORY = REPORT_CATEGORY_COUNTS;
  private static final Option OPT_AFTER = CommonOptions.after(false);
  private static final Option OPT_BEFORE = CommonOptions.before(false);

  private String start;
  private String end;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_AFTER, OPT_BEFORE);
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
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public String getTitle() {
    return "Libraries Creation (Billing) "
        + (start == null ? "Any Time" : start)
        + " - "
        + (end == null ? "Now" : end);
  }

  private static final String LIBRARY_DESIGN_CODE = "Source Template Type";
  private final List<SummaryObject> summaryData = new ArrayList<>();
  private final List<DetailedObject> detailedData = new ArrayList<>();

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    // filter by libraries
    List<String> libraryTypes = Arrays.asList("Illumina PE Library", "Illumina SE Library");
    List<SampleDto> allLibraries =
        pinery.getSample().allFiltered(new SamplesFilter().withTypes(libraryTypes));

    // filter down to libraries within the date range
    List<SampleDto> newLibraries =
        allLibraries.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());

    for (SampleDto lib : newLibraries) {
      String kitName =
          (lib.getPreparationKit() == null ? "No Kit" : lib.getPreparationKit().getName());
      String project = lib.getProjectName();

      // update summary
      SummaryObject matchingKitAndProject =
          summaryData
              .stream()
              .filter(row -> row.getKit().equals(kitName) && row.getProject().equals(project))
              .findAny()
              .orElse(null);
      if (matchingKitAndProject == null) {
        // new project & kit combo
        matchingKitAndProject = new SummaryObject(project, kitName);
        summaryData.add(matchingKitAndProject);
      } else {
        // existing project & kit combo
        matchingKitAndProject.addOneLibrary();
      }

      // add detailed row
      detailedData.add(makeDetailedRow(lib));
    }

    summaryData.sort(SummaryObject.summaryComparator);
    detailedData.sort(DetailedObject.detailedComparator);
  }

  private DetailedObject makeDetailedRow(SampleDto lib) {
    return new DetailedObject(
        lib.getProjectName(),
        (lib.getPreparationKit() == null ? "No Kit" : lib.getPreparationKit().getName()),
        lib.getCreatedDate(),
        lib.getName(),
        getAttribute(LIBRARY_DESIGN_CODE, lib));
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return Arrays.asList(
        new ColumnDefinition("Study Title"),
        new ColumnDefinition("Library Kit"),
        new ColumnDefinition(
            String.format(
                "Count (%s - %s)",
                (start == null ? "Any Time" : start), (end == null ? "Now" : end))),
        new ColumnDefinition(""),
        new ColumnDefinition(""));
  }

  private List<String> getDetailedHeadings() {
    return Arrays.asList("Project", "Creation Date", "Library", "Library Kit", "Seq. Strategy");
  }

  @Override
  protected int getRowCount() {
    return summaryData.size() + 2 + detailedData.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    if (rowNum < summaryData.size()) {
      SummaryObject obj = summaryData.get(rowNum);
      return makeSummaryRow(obj);
    }
    rowNum -= summaryData.size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow(getDetailedHeadings());
    }
    rowNum -= 2;

    return makeDetailedRow(detailedData.get(rowNum));
  }

  private String[] makeSummaryRow(SummaryObject obj) {
    String[] row = new String[getColumns().size()];

    int i = -1;
    // Project
    row[++i] = obj.getProject();
    // Kit
    row[++i] = obj.getKit();
    // Count of libraries using this kit in this project
    row[++i] = obj.getLibrariesCount().toString();

    // extra columns to account for detailed row size
    for (int j = i + 1; j < row.length; j++) {
      row[j] = "";
    }

    return row;
  }

  private String[] makeBlankRow() {
    String[] row = new String[getColumns().size()];
    for (int i = 0; i < row.length; i++) {
      row[i] = "";
    }
    return row;
  }

  private String[] makeHeadingRow(List<String> headings) {
    String[] row = new String[headings.size()];
    return headings.toArray(row);
  }

  private String[] makeDetailedRow(DetailedObject obj) {
    String[] row = new String[getColumns().size()];

    int i = -1;
    // Project
    row[++i] = obj.getProject();
    // Creation Date
    row[++i] = obj.getCreationDate();
    // Library
    row[++i] = obj.getLibrary();
    // Library Kit
    row[++i] = obj.getKit();
    // Seq. Strategy
    row[++i] = obj.getLibraryDesignCode();

    return row;
  }
}
