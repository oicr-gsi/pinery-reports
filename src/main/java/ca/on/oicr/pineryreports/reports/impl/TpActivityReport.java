package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.DATE_REGEX;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Sets;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.pineryreports.util.GeneralUtils;
import ca.on.oicr.ws.dto.SampleDto;

public class TpActivityReport extends TableReport {

  private static class ProjectCounts {

    private String projectName;
    private int receivedCount = 0;
    private int accessionedCount = 0;
    private int extractedCount = 0;
    private int aliquotedCount = 0;
    private int transferredCount = 0;
    private int distributedCount = 0;

    public ProjectCounts(String projectName) {
      this.projectName = projectName;
    }

    public String getProjectName() {
      return projectName;
    }

    public int getReceivedCount() {
      return receivedCount;
    }

    public void incrementReceiptCount(int amount) {
      receivedCount += amount;
    }

    public int getAccessionedCount() {
      return accessionedCount;
    }

    public void incrementAccessionedCount(int amount) {
      accessionedCount += amount;
    }

    public int getExtractedCount() {
      return extractedCount;
    }

    public void incrementExtractedCount() {
      extractedCount += 1;
    }

    public int getAliquotedCount() {
      return aliquotedCount;
    }

    public void incrementAliquotedCount() {
      aliquotedCount += 1;
    }

    public int getTransferredCount() {
      return transferredCount;
    }

    public void incrementTransferredCount(int amount) {
      transferredCount += amount;
    }

    public int getDistributedCount() {
      return distributedCount;
    }

    public void incrementDistributedCount(int amount) {
      distributedCount += amount;
    }

  }

  public static final String REPORT_NAME = "tp-activity";

  private static final Option OPT_AFTER = CommonOptions.after(true);
  private static final Option OPT_BEFORE = CommonOptions.before(true);
  private static final Option OPT_USER_IDS = CommonOptions.users(true);
  private static final Option OPT_DB_HOST = Option.builder()
      .longOpt("db-host")
      .hasArg()
      .argName("host")
      .required()
      .desc("MISO database host (required)")
      .build();
  private static final Option OPT_DB_NAME = Option.builder()
      .longOpt("db-name")
      .hasArg()
      .argName("name")
      .required()
      .desc("MISO database name (required)")
      .build();
  private static final Option OPT_DB_USER = Option.builder()
      .longOpt("db-user")
      .hasArg()
      .argName("username")
      .required()
      .desc("Username for MISO database access (required)")
      .build();
  private static final Option OPT_DB_PASSWORD = Option.builder()
      .longOpt("db-password")
      .hasArg()
      .argName("password")
      .required()
      .desc("Password for MISO database access (required)")
      .build();

  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(
      Arrays.asList(
          new ColumnDefinition("Project Code"),
          new ColumnDefinition("Samples Received"),
          new ColumnDefinition("Samples Accessioned"),
          new ColumnDefinition("Samples Extracted"),
          new ColumnDefinition("Samples Aliquoted"),
          new ColumnDefinition("Samples Transferred"),
          new ColumnDefinition("Samples Distributed")));

  private String start;
  private String end;
  private List<Integer> userIds = new ArrayList<>();
  private String databaseHost;
  private String databaseName;
  private String databaseUser;
  private String databasePassword;

  private List<ProjectCounts> projectCounts;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_USER_IDS, OPT_AFTER, OPT_BEFORE, OPT_DB_HOST, OPT_DB_NAME, OPT_DB_USER, OPT_DB_PASSWORD);
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    String after = cmd.getOptionValue(OPT_AFTER.getLongOpt());
    if (!after.matches(DATE_REGEX)) {
      throw new ParseException("After date must be in format yyyy-mm-dd");
    }
    this.start = after;

    String before = cmd.getOptionValue(OPT_BEFORE.getLongOpt());
    if (!before.matches(DATE_REGEX)) {
      throw new ParseException("Before date must be in format yyyy-mm-dd");
    }
    this.end = before;

    String[] users = cmd.getOptionValue(OPT_USER_IDS.getLongOpt()).split(",");
    for (String user : users) {
      if (user != null && !"".equals(user)) {
        userIds.add(Integer.valueOf(user));
      }
    }
    databaseHost = cmd.getOptionValue(OPT_DB_HOST.getLongOpt());
    databaseName = cmd.getOptionValue(OPT_DB_NAME.getLongOpt());
    databaseUser = cmd.getOptionValue(OPT_DB_USER.getLongOpt());
    databasePassword = cmd.getOptionValue(OPT_DB_PASSWORD.getLongOpt());
  }

  @Override
  public String getTitle() {
    return String.format("TP Activity Report for %s - %s", start, end);
  }

  @Override
  public String getCategory() {
    return GeneralUtils.REPORT_CATEGORY_COUNTS;
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> samples = pinery.getSample().all().stream()
        .filter(byCreator(userIds))
        .filter(sample -> !"True".equals(getAttribute(ATTR_SYNTHETIC, sample)))
        .filter(bySampleCategory(SAMPLE_CATEGORY_IDENTITY).negate())
        .collect(Collectors.toList());
    Map<String, ProjectCounts> countsByProjectName = new HashMap<>();

    samples.stream()
        .filter(byCreatedBetween(start, end))
        .forEach(sample -> {
          ProjectCounts counts = getProjectCounts(sample.getProjectName(), countsByProjectName);
          String category = getAttribute(ATTR_CATEGORY, sample);
          if (getAttribute(ATTR_RECEIVE_DATE, sample) != null) {
            if (SAMPLE_CLASS_SLIDE.equals(sample.getSampleType())) {
              counts.incrementReceiptCount(getIntAttribute(ATTR_INITIAL_SLIDES, sample, 1));
            } else {
              counts.incrementReceiptCount(1);
            }
          }
          if (getAttribute(ATTR_CREATION_DATE, sample) == null) {
            if (SAMPLE_CLASS_SLIDE.equals(sample.getSampleType())) {
              counts.incrementAccessionedCount(getIntAttribute(ATTR_INITIAL_SLIDES, sample, 1));
            } else {
              counts.incrementAccessionedCount(1);
            }
          } else if (Objects.equals(category, SAMPLE_CATEGORY_STOCK)) {
            counts.incrementExtractedCount();
          } else if (Objects.equals(category, SAMPLE_CATEGORY_ALIQUOT)) {
            counts.incrementAliquotedCount();
          }
        });

    samples.stream()
        .filter(byDistributedBetween(start, end))
        .forEach(sample -> {
          ProjectCounts counts = getProjectCounts(sample.getProjectName(), countsByProjectName);
          if (SAMPLE_CLASS_SLIDE.equals(sample.getSampleType())) {
            counts.incrementDistributedCount(getIntAttribute(ATTR_INITIAL_SLIDES, sample, 1));
          } else {
            counts.incrementDistributedCount(1);
          }
        });

    addMisoDbData(countsByProjectName);

    projectCounts = countsByProjectName.values().stream()
        .sorted(Comparator.comparing(ProjectCounts::getProjectName))
        .collect(Collectors.toList());
  }

  private void addMisoDbData(Map<String, ProjectCounts> countsByProjectName) throws IOException {
    String url = String.format("jdbc:mysql://%s/%s", databaseHost, databaseName);
    try (Connection conn = DriverManager.getConnection(url, databaseUser, databasePassword)) {
      String userIdString = userIds.stream().map(Long::toString).collect(Collectors.joining(","));
      PreparedStatement stmt = conn.prepareStatement(
          "SELECT p.code AS project, COUNT(*) AS count\n"
              + "FROM Sample s\n"
              + "JOIN Project p ON p.projectId = s.project_projectId\n"
              + "JOIN (\n"
              + "  SELECT ts.sampleId, MAX(t.transferTime)\n"
              + "  FROM Transfer_Sample ts\n"
              + "  JOIN Transfer t ON t.transferId = ts.transferId\n"
              + "  WHERE t.senderGroupId IS NOT NULL\n"
              + "  AND t.recipientGroupId IS NOT NULL\n"
              + "  AND transferTime BETWEEN '" + start + "' AND '" + end + "'\n"
              + "  GROUP BY ts.sampleId\n"
              + ") xfer ON xfer.sampleId = s.sampleId\n"
              + "WHERE s.creator IN (" + userIdString + ")\n"
              + "GROUP BY p.code;");
      try (ResultSet results = stmt.executeQuery()) {
        while (results.next()) {
          ProjectCounts counts = getProjectCounts(results.getString("project"), countsByProjectName);
          counts.incrementTransferredCount(results.getInt("count"));
        }
      }
    } catch (SQLException e) {
      throw new IOException(e);
    }
  }

  private static ProjectCounts getProjectCounts(String project, Map<String, ProjectCounts> countsByProjectName) {
    if (!countsByProjectName.containsKey(project)) {
      countsByProjectName.put(project, new ProjectCounts(project));
    }
    return countsByProjectName.get(project);
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return projectCounts.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    ProjectCounts counts = projectCounts.get(rowNum);
    String[] row = new String[COLUMNS.size()];

    row[0] = counts.getProjectName();
    row[1] = String.valueOf(counts.getReceivedCount());
    row[2] = String.valueOf(counts.getAccessionedCount());
    row[3] = String.valueOf(counts.getExtractedCount());
    row[4] = String.valueOf(counts.getAliquotedCount());
    row[5] = String.valueOf(counts.getTransferredCount());
    row[6] = String.valueOf(counts.getDistributedCount());

    return row;
  }

}
