package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.DATE_REGEX;
import static ca.on.oicr.pineryreports.util.GeneralUtils.REPORT_CATEGORY_COUNTS;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class PreciseCaseReport extends TableReport {
  public static final String REPORT_NAME = "precisecase";
  public static final String CATEGORY = REPORT_CATEGORY_COUNTS;
  private String start;
  private String end;

  private static final Option OPT_AFTER = CommonOptions.after(true);
  private static final Option OPT_BEFORE = CommonOptions.before(true);

  List<List<String>> table;
  List<SampleDto> allPreciseIdentities;

  private enum TimePoint {
    // Coded by the Times Received portion of the tissue name: PRE_1111111_Xx_X_nn_*#*-1
    RANDOMIZATION("Screening/Randomization", 1),
    SIX("6 mo Follow Up", 2),
    TWELVE("1 yr Follow Up", 3), //
    EIGHTEEN("18 mo Follow Up", 4),
    TWENTY_FOUR("2 yr Follow Up", 5),
    BX("Bx Slides", 6),
    RP("RP Slides", 7);
    private final String key;
    private final int timePointCode;
    private static final Map<String, PreciseCaseReport.TimePoint> lookup = new TreeMap<>();

    static {
      for (PreciseCaseReport.TimePoint s : EnumSet.allOf(PreciseCaseReport.TimePoint.class)) {
        lookup.put(s.getKey(), s);
      }
    }

    TimePoint(String key, int timePointCode) {
      this.key = key;
      this.timePointCode = timePointCode;
    }

    public String getKey() {
      return key;
    }

    /**
     * Assumes that PRECISE samples will always have _nn_ in the name, and that Tissue Origin will
     * never be "nn". Should hopefully be a safe assumption since this is a biobanking project where
     * tracking tissue origin is critical.
     */
    public Predicate<SampleDto> predicate() {
      return sample -> {
        if (SAMPLE_CATEGORY_IDENTITY.equals(sample.getSampleType())) return false;
        return timePointCode == getTimesReceived(sample.getName());
      };
    }
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> allPreciseSamples =
        pinery
            .getSample()
            .allFiltered(new SampleClient.SamplesFilter().withProjects(Lists.newArrayList("PRE")));
    List<String> tissueOriginOrder = Lists.newArrayList("_Se_", "_Pl_", "_Ly_");
    List<String> tissueOriginOrderExtra =
        Lists.newArrayList("_Se_", "_Pl_", "_Ly_", "_Us_", "_Up_");
    allPreciseIdentities =
        allPreciseSamples
            .stream()
            .filter(s -> s.getSampleType().equals(SAMPLE_CATEGORY_IDENTITY))
            .collect(Collectors.toList());

    table = new LinkedList<>();

    // Add headers
    table.add(
        Lists.newArrayList(
            "ID",
            "Screening/Randomization Serum",
            "Screening/Randomization Plasma",
            "Screening/Randomization Buffy Coat",
            "Screening/Randomization Urine Supernatant",
            "Screening/Randomization Urine Pellet",
            "6 mo Follow Up Serum",
            "6 mo Follow Up Plasma",
            "6 mo Follow Up Buffy Coat",
            "1 yr Follow Up Serum",
            "1 yr Follow Up Plasma",
            "1 yr Follow Up Buffy Coat",
            "1 yr Follow Up Urine Supernatant",
            "1 yr Follow Up Urine Pellet",
            "18 mo Follow Up Serum",
            "18 mo Follow Up Plasma",
            "18 mo Follow Up Buffy Coat",
            "2 yr Follow Up Serum",
            "2 yr Follow Up Plasma",
            "2 yr Follow Up Buffy Coat",
            "2 yr Follow Up Urine Supernatant",
            "2 yr Follow Up Urine Pellet",
            "Bx Slides - Positive",
            "Bx Slides - Negative",
            "RP Slides"));

    // One row per identity
    for (SampleDto identity : allPreciseIdentities) {
      List<String> row = new LinkedList<>();
      row.add(identity.getName());

      // For all regular time points
      for (int i = 0; i < 5; i++) {
        TimePoint currentTimePoint = TimePoint.values()[i];
        List<SampleDto> samplesAtCurrentTime =
            allPreciseSamples
                .stream()
                .filter(
                    s ->
                        s.getName().startsWith(identity.getName())) // Get children of this Identity
                .filter(currentTimePoint.predicate()) // Get samples at this time point
                .collect(Collectors.toList());
        // on 0th, 2th, 4th iterations use all the types, otherwise just 3
        List<String> tissueOrigins;
        if (i % 2 == 0) {
          tissueOrigins = tissueOriginOrderExtra;
        } else {
          tissueOrigins = tissueOriginOrder;
        }

        // for all applicable tissue origins, get counts and add them to the row
        for (String origin : tissueOrigins) {
          row.add(
              String.valueOf(
                  samplesAtCurrentTime.stream().filter(s -> s.getName().contains(origin)).count()));
        }
      }

      // Handle irregular time points
      row.add(
          String.valueOf(
              allPreciseSamples
                  .stream()
                  .filter(s -> s.getName().startsWith(identity.getName()))
                  .filter(s -> s.getSampleType().equals(SAMPLE_CLASS_SLIDE))
                  .filter(TimePoint.BX.predicate())
                  .filter(s -> true) // TODO: How to get Group ID? Need that for POS/NEG
                  .count()));
      row.add(
          String.valueOf(
              allPreciseSamples
                  .stream()
                  .filter(s -> s.getName().startsWith(identity.getName()))
                  .filter(s -> s.getSampleType().equals(SAMPLE_CLASS_SLIDE))
                  .filter(TimePoint.BX.predicate())
                  .filter(s -> true) // TODO: How to get Group ID? Need that for POS/NEG
                  .count()));

      table.add(row);
    }
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return null;
  }

  @Override
  protected int getRowCount() {
    return 1 + allPreciseIdentities.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    return (String[]) table.get(rowNum).toArray();
  }

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_BEFORE, OPT_AFTER);
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
  public String getTitle() {
    return "PRECISE Case Report generated " + end;
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }
}
