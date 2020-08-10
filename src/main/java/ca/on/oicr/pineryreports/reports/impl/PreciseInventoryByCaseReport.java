package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.REPORT_CATEGORY_COUNTS;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.SampleUtils;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class PreciseInventoryByCaseReport extends TableReport {
  public static final String REPORT_NAME = "preciseinventorybycase";
  public static final String CATEGORY = REPORT_CATEGORY_COUNTS;

  List<List<String>> table;
  List<SampleDto> allPreciseIdentities;

  private static final List<ColumnDefinition> COLUMNS =
      Collections.unmodifiableList(
          Arrays.asList(
              new ColumnDefinition("ID"),
              new ColumnDefinition("Screening/Randomization Serum"),
              new ColumnDefinition("Screening/Randomization Plasma"),
              new ColumnDefinition("Screening/Randomization Buffy Coat"),
              new ColumnDefinition("Screening/Randomization Urine Supernatant"),
              new ColumnDefinition("Screening/Randomization Urine Pellet"),
              new ColumnDefinition("6 mo Follow Up Serum"),
              new ColumnDefinition("6 mo Follow Up Plasma"),
              new ColumnDefinition("6 mo Follow Up Buffy Coat"),
              new ColumnDefinition("1 yr Follow Up Serum"),
              new ColumnDefinition("1 yr Follow Up Plasma"),
              new ColumnDefinition("1 yr Follow Up Buffy Coat"),
              new ColumnDefinition("1 yr Follow Up Urine Supernatant"),
              new ColumnDefinition("1 yr Follow Up Urine Pellet"),
              new ColumnDefinition("18 mo Follow Up Serum"),
              new ColumnDefinition("18 mo Follow Up Plasma"),
              new ColumnDefinition("18 mo Follow Up Buffy Coat"),
              new ColumnDefinition("2 yr Follow Up Serum"),
              new ColumnDefinition("2 yr Follow Up Plasma"),
              new ColumnDefinition("2 yr Follow Up Buffy Coat"),
              new ColumnDefinition("2 yr Follow Up Urine Supernatant"),
              new ColumnDefinition("2 yr Follow Up Urine Pellet"),
              new ColumnDefinition("Bx Slides - Positive"),
              new ColumnDefinition("Bx Slides - Negative"),
              new ColumnDefinition("RP Slides")));

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
    private static final Map<String, PreciseInventoryByCaseReport.TimePoint> lookup =
        new TreeMap<>();

    static {
      for (PreciseInventoryByCaseReport.TimePoint s :
          EnumSet.allOf(PreciseInventoryByCaseReport.TimePoint.class)) {
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

    public Predicate<SampleDto> predicate() {
      return sample -> {
        if (SAMPLE_CATEGORY_IDENTITY.equals(getAttribute(ATTR_CATEGORY, sample))) return false;
        return timePointCode == getTimesReceived(sample.getName());
      };
    }
  }

  static Predicate<SampleDto> filterTubes() {
    return sample -> {
      int tube = SampleUtils.getTubeNumber(sample.getName());
      return !(tube == 6 || tube == 17 || tube == 31);
    };
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> allPreciseSamples =
        pinery
            .getSample()
            .allFiltered(new SampleClient.SamplesFilter().withProjects(Lists.newArrayList("PRE")));
    Map<String, SampleDto> allPreciseSamplesById = mapSamplesById(allPreciseSamples);
    Map<String, List<SampleDto>> allPreciseSamplesByIdentity =
        mapChildrenByIdentityId(allPreciseSamplesById);
    List<String> tissueOriginOrder = Lists.newArrayList("Se", "Pl", "Ly");
    List<String> tissueOriginOrderExtra = Lists.newArrayList("Se", "Pl", "Ly", "Us", "Up");
    allPreciseIdentities =
        allPreciseSamples
            .stream()
            .filter(s -> SAMPLE_CATEGORY_IDENTITY.equals(getAttribute(ATTR_CATEGORY, s)))
            .collect(Collectors.toList());

    table = new LinkedList<>();
    // One row per identity
    for (SampleDto identity : allPreciseIdentities) {
      List<String> row = new LinkedList<>();
      row.add(identity.getName());
      if (!allPreciseSamplesByIdentity.containsKey(identity.getId())) {
        row.addAll(Collections.nCopies(COLUMNS.size() - 1, "0")); // -1 because of ID column
      } else {
        // For all regular time points
        for (int i = 0; i < 5; i++) {
          TimePoint currentTimePoint = TimePoint.values()[i];

          List<SampleDto> samplesAtCurrentTime =
              allPreciseSamplesByIdentity
                  .get(identity.getId()) // Get children of this identity
                  .stream()
                  .filter(currentTimePoint.predicate()) // Get samples at this time point
                  .filter(filterTubes())
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
                    samplesAtCurrentTime
                        .stream()
                        .filter(s -> origin.equals(getAttribute(ATTR_TISSUE_ORIGIN, s)))
                        .count()));
          }
        }
        // Handle irregular time points
        row.add(
            String.valueOf(
                allPreciseSamplesByIdentity
                    .get(identity.getId()) // Get children of this identity
                    .stream()
                    .filter(filterTubes())
                    .filter(s -> SAMPLE_CLASS_SLIDE.equals(s.getSampleType()))
                    .filter(TimePoint.BX.predicate())
                    .filter(
                        s ->
                            "POS".equals(getAttribute(ATTR_GROUP_ID, s))
                                || "POS"
                                    .equals(
                                        getUpstreamAttribute(
                                            ATTR_GROUP_ID, s, allPreciseSamplesById))
                                || null == getAttribute(ATTR_GROUP_ID, s))
                    .count()));
        row.add(
            String.valueOf(
                allPreciseSamplesByIdentity
                    .get(identity.getId()) // Get children of this identity
                    .stream()
                    .filter(filterTubes())
                    .filter(s -> SAMPLE_CLASS_SLIDE.equals(s.getSampleType()))
                    .filter(TimePoint.BX.predicate())
                    .filter(
                        s ->
                            "NEG".equals(getAttribute(ATTR_GROUP_ID, s))
                                || "NEG"
                                    .equals(
                                        getUpstreamAttribute(
                                            ATTR_GROUP_ID, s, allPreciseSamplesById)))
                    .count()));

        row.add(
            String.valueOf(
                allPreciseSamplesByIdentity
                    .get(identity.getId())
                    .stream()
                    .filter(filterTubes())
                    .filter(s -> SAMPLE_CLASS_SLIDE.equals(s.getSampleType()))
                    .filter(TimePoint.RP.predicate())
                    .count()));
      }

      table.add(row);
    }
  }

  private Map<String, List<SampleDto>> mapChildrenByIdentityId(Map<String, SampleDto> samplesById) {
    Map<String, List<SampleDto>> childrenByIdentityId = new HashMap<>();
    for (SampleDto sample : samplesById.values()) {
      if (!SAMPLE_CATEGORY_IDENTITY.equals(getAttribute(ATTR_CATEGORY, sample))) {
        SampleDto identity = getParent(sample, SAMPLE_CATEGORY_IDENTITY, samplesById);
        if (!childrenByIdentityId.containsKey(identity.getId())) {
          childrenByIdentityId.put(identity.getId(), new ArrayList<>());
        }
        childrenByIdentityId.get(identity.getId()).add(sample);
      }
    }
    return childrenByIdentityId;
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return allPreciseIdentities.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    return table.get(rowNum).toArray(new String[COLUMNS.size()]);
  }

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet();
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    return;
  }

  @Override
  public String getTitle() {
    return "PRECISE Case Report generated " + LocalDate.now();
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }
}
