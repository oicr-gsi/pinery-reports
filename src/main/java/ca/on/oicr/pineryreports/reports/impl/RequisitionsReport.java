package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.AssayDto;
import ca.on.oicr.ws.dto.RequisitionDto;

public class RequisitionsReport extends TableReport {

  public static final String REPORT_NAME = "requisitions";

  public static Option OPT_MATCH = Option.builder()
      .longOpt("match")
      .hasArg()
      .argName("text")
      .required(false)
      .desc("Comma-separated list; Only requisitions with aliases containing one of these strings will be included")
      .build();

  public static Option OPT_EXCLUDE = Option.builder()
      .longOpt("exclude")
      .hasArg()
      .argName("text")
      .required(false)
      .desc("Comma-separated list; Requisitions with aliases containing any of these strings will be excluded")
      .build();

  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(
      Arrays.asList(
          new ColumnDefinition("Requisition"),
          new ColumnDefinition("Assay"),
          new ColumnDefinition("Created"),
          new ColumnDefinition("Stopped"),
          new ColumnDefinition("Stop Reason")));

  private Set<String> matchPatterns = new HashSet<>();
  private Set<String> excludePatterns = new HashSet<>();

  private List<RequisitionDto> requisitions;
  private Map<Integer, AssayDto> assaysById;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Arrays.asList(OPT_MATCH, OPT_EXCLUDE);
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    if (cmd.hasOption(OPT_MATCH.getLongOpt())) {
      String[] matches = cmd.getOptionValue(OPT_MATCH.getLongOpt()).split(",");
      for (String match : matches) {
        matchPatterns.add(match);
      }
    }
    if (cmd.hasOption(OPT_EXCLUDE.getLongOpt())) {
      String[] excludes = cmd.getOptionValue(OPT_MATCH.getLongOpt()).split(",");
      for (String exclude : excludes) {
        excludePatterns.add(exclude);
      }
    }
  }

  @Override
  public String getTitle() {
    return "Requisitions Report " + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
  }

  @Override
  public String getCategory() {
    return REPORT_CATEGORY_INVENTORY;
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    requisitions = pinery.getRequisition().all().stream()
        .filter(requisition -> {
          if (matchPatterns.stream().noneMatch(requisition.getName()::contains)) {
            return false;
          }
          for (String text : excludePatterns) {
            if (requisition.getName().contains(text)) {
              return false;
            }
          }
          return true;
        })
        .collect(Collectors.toList());
    assaysById = pinery.getAssay().all().stream()
        .collect(Collectors.toMap(AssayDto::getId, Function.identity()));
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return requisitions.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    RequisitionDto requisition = requisitions.get(rowNum);
    String assayNames = requisition.getAssayIds() == null ? null
        : requisition.getAssayIds().stream()
            .map(id -> assaysById.get(id).getName())
            .collect(Collectors.joining("; "));

    return new String[] {
        requisition.getName(),
        assayNames,
        removeTime(requisition.getCreatedDate()),
        requisition.isStopped() ? "Yes" : "No",
        requisition.getStopReason()
    };
  }

}
