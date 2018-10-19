package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;

public class PreciseReport extends TableReport {

  public static final String REPORT_NAME = "precise";
  private String start;
  private String end;

  private static final Option OPT_AFTER = CommonOptions.after(false);
  private static final Option OPT_BEFORE = CommonOptions.before(false);

  private enum SampleLabel {
    // Coded by the trailing digits on the identification barcode: P0001.*##*
    SERUM("Serum", 1, 5), MITOMIC("Mitomic", 6, 6), PLASMA("Plasma", 7, 13), BUFFY("Buffy Coat", 14, 16), MIR("MIR", 17, 17), //
    PRE_US("Pre-DRE Urine Supernatant", 18, 27), PRE_UP("Pre-DRE Urine Pellet", 28, 30), MDX("MDX", 31, 31), //
    POST_US("Post-DRE Urine Supernatant", 32, 41), POST_UP("Post-DRE Urine Pellet",42,44), //
    BIOPSY_SLIDES("Biopsy Slides", 100, 10000), RP_SLIDES("Radical Prostatectomy Slides", 100, 10000);
    
    private final String key;
    private final int low;
    private final int high;
    private static final Map<String, SampleLabel> lookup = new TreeMap<>();
    static {
      for (SampleLabel s : EnumSet.allOf(SampleLabel.class)) {
        lookup.put(s.getKey(), s);
      }
    }

    SampleLabel(String key, int low, int high) {
      this.key = key;
      this.low = low;
      this.high = high;
    }

    public String getKey() {
      return key;
    }

    public Predicate<SampleDto> predicate() {
      return sample -> {
        if (sample.getTubeBarcode() == null) return false;
        String barcodeString = sample.getTubeBarcode().split("\\.")[1].replaceFirst("^0", "");
        if (barcodeString == null) throw new IllegalArgumentException(String
            .format("Could not detect a number after the '.' for barcode %s on sample %s; got %s", sample.getTubeBarcode(),
                sample.getName(), sample.getTubeBarcode().split("\\.")[1]));
        boolean fullMatch = low <= Integer.parseInt(barcodeString) && high >= Integer.parseInt(barcodeString);

        // Biopsy Slides and RP Slides are a hybrid between SampleLabel and TimePoint: they have the same SampleLabel numbers and should be
        // treated as sample labels in how they're displayed in tables, but they differ by their Times Received number (which is time point
        // coding) so we need to check them differently here
        if (BIOPSY_SLIDES.getKey().equals(key)) {
          return fullMatch && sample.getName().contains("_nn_6");
        } else if (RP_SLIDES.getKey().equals(key)) {
          return fullMatch && sample.getName().contains("_nn_7");
        } else {
          return fullMatch;
        }
      };
    }
  }

  private enum Site {
    // Coded by the first four digits of the donor number: PRE_*####*111_Xx_X_nn_1-1
    SUNNYBROOK("Sunnybrook (1039)", "1039"), UHN("UHN (1042)", "1042"), LONDON("London (1023)", "1023"), //
    MONTREAL("Montreal (2024)", "2024"), VANCOUVER("Vancouver (2026)", "2026");
    private final String key;
    private final String siteId;
    private static final Map<String, Site> lookup = new TreeMap<>();
    static {
      for (Site s : EnumSet.allOf(Site.class)) {
        lookup.put(s.getKey(), s);
      }
    }

    Site(String key, String siteId) {
      this.key = key;
      this.siteId = siteId;
    }

    public String getKey() {
      return key;
    }

    public static Predicate<SampleDto> predicate(Site site) {
      return sample -> sample.getName().startsWith("PRE_" + site.siteId);
    }
  }

  private enum TimePoint {
    // Coded by the Times Received portion of the tissue name: PRE_1111111_Xx_X_nn_*#*-1
    RANDOMIZATION("Screening/Randomization", 1), SIX("6 Month Follow Up", 2), TWELVE("12 Month Follow Up", 3), //
    EIGHTEEN("18 Month Follow Up", 4), TWENTY_FOUR("24 Month Follow Up", 5), BIOPSY("Biopsy", 6), RADICAL("Radical Prostatectomy", 7);
    private final String key;
    private final int timePointCode;
    private static final Map<String, TimePoint> lookup = new TreeMap<>();
    static {
      for (TimePoint s : EnumSet.allOf(TimePoint.class)) {
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
     * Assumes that PRECISE samples will always have _nn_ in the name, and that Tissue Origin will never be "nn".
     * Should hopefully be a safe assumption since this is a biobanking project where tracking tissue origin is critical.
     */
    public static Predicate<SampleDto> predicate(TimePoint timePoint) {
      return sample -> sample.getName().contains("_nn_" + timePoint.timePointCode);
    }
  }

  private static final String NUM_SAMPLES = "# Samples";
  private static final String NUM_CASES = "# Cases";
  private static final List<Site> SITE_ORDER = Arrays.asList(Site.SUNNYBROOK, Site.UHN, Site.LONDON, Site.MONTREAL, Site.VANCOUVER);
  private final List<SampleLabel> labelListLong = Arrays.asList(SampleLabel.SERUM, SampleLabel.MITOMIC, SampleLabel.PLASMA,
      SampleLabel.BUFFY, SampleLabel.MIR, SampleLabel.PRE_US, SampleLabel.PRE_UP, SampleLabel.MDX, SampleLabel.POST_US,
      SampleLabel.POST_UP);
  private final List<SampleLabel> labelListShort = Arrays.asList(SampleLabel.SERUM, SampleLabel.MITOMIC, SampleLabel.PLASMA,
      SampleLabel.BUFFY);
  private final List<SampleLabel> labelListSlides = Arrays.asList(SampleLabel.BIOPSY_SLIDES, SampleLabel.RP_SLIDES);
  private final List<SampleLabel> labelListInventory = Arrays.asList(SampleLabel.SERUM, SampleLabel.MITOMIC, SampleLabel.PLASMA,
      SampleLabel.BUFFY, SampleLabel.MIR, SampleLabel.PRE_US, SampleLabel.PRE_UP, SampleLabel.MDX, SampleLabel.POST_US,
      SampleLabel.POST_UP, SampleLabel.BIOPSY_SLIDES, SampleLabel.RP_SLIDES);

  private List<List<String>> toDateRandom;
  private List<List<String>> toDateSix;
  private List<List<String>> toDateTwelve;
  private List<List<String>> toDateEighteen;
  private List<List<String>> toDateTwentyFour;
  private List<List<String>> toDateSlides;
  private List<List<String>> thisMonthRandom;
  private List<List<String>> thisMonthSix;
  private List<List<String>> thisMonthTwelve;
  private List<List<String>> thisMonthEighteen;
  private List<List<String>> thisMonthTwentyFour;
  private List<List<String>> thisMonthSlides;
  private List<List<String>> inventoryAvailable;
  private List<SampleDto> barcodeAndNameCodingMismatch;
  private List<SampleDto> slidesWithoutSlideTimepoint;

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
    return "PRECISE Report generated " + end;
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> allPreciseSamples = pinery.getSample().allFiltered(new SamplesFilter().withProjects(Lists.newArrayList("PRE")));
    Map<String, SampleDto> allPreciseSamplesById = mapSamplesById(allPreciseSamples);

    Map<SampleLabel, List<SampleDto>> labelList = new TreeMap<>();
    for (SampleLabel label : SampleLabel.values()) {
      labelList.put(label, filter(allPreciseSamples, label.predicate()));
    }

    toDateRandom = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListLong, TimePoint.predicate(TimePoint.RANDOMIZATION),
        bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    toDateSix = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListShort, TimePoint.predicate(TimePoint.SIX),
        bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    toDateTwelve = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListLong, TimePoint.predicate(TimePoint.TWELVE),
        bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    toDateEighteen = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListShort, TimePoint.predicate(TimePoint.EIGHTEEN),
        bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    toDateTwentyFour = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListLong, TimePoint.predicate(TimePoint.TWENTY_FOUR),
        bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    toDateSlides = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListSlides,
        bySampleCategory(SAMPLE_CATEGORY_TISSUE_PROCESSING));

    thisMonthRandom = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListLong,
        TimePoint.predicate(TimePoint.RANDOMIZATION), byCreatedBetween(start, end), bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    thisMonthSix = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListShort,
        TimePoint.predicate(TimePoint.SIX), byCreatedBetween(start, end), bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    thisMonthTwelve = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListLong,
        TimePoint.predicate(TimePoint.TWELVE), byCreatedBetween(start, end), bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    thisMonthEighteen = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListShort,
        TimePoint.predicate(TimePoint.EIGHTEEN), byCreatedBetween(start, end), bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    thisMonthTwentyFour = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListLong,
        TimePoint.predicate(TimePoint.TWENTY_FOUR), byCreatedBetween(start, end), bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    thisMonthSlides = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListSlides, byCreatedBetween(start, end),
        bySampleCategory(SAMPLE_CATEGORY_TISSUE_PROCESSING));

    inventoryAvailable = getSingleTable(allPreciseSamples, allPreciseSamplesById, labelListInventory, byNotEmpty);

    barcodeAndNameCodingMismatch = filter(allPreciseSamples, byBarcodeTimesReceivedMismatch);
    slidesWithoutSlideTimepoint = filter(allPreciseSamples, byTimePointCodingMismatch);

  }

  private final Predicate<SampleDto> byBarcodeTimesReceivedMismatch = dto -> {
    // check only tissues and slides with non-null barcodes
    if ((!bySampleCategory(SAMPLE_CATEGORY_TISSUE).test(dto) && !bySampleCategory(SAMPLE_CATEGORY_TISSUE_PROCESSING).test(dto))
        || dto.getTubeBarcode() == null) {
      return false;
    }

    String barcodeString = dto.getTubeBarcode().split("\\.")[1].replaceFirst("^0", "");
    String timesReceived = "";
    if (bySampleCategory(SAMPLE_CATEGORY_TISSUE).test(dto)) {
      timesReceived = dto.getName().split("-")[dto.getName().split("-").length - 1];
    } else if (getAttribute(ATTR_SLIDES, dto) != null) {
      // quick and dirty check for Slides
      Matcher m = Pattern.compile(".*(\\d{1,5}-\\d{1,5}).*").matcher(dto.getName());
      if (!m.matches()) throw new IllegalArgumentException("Sample with alias " + dto.getName() + " is malformed.");
      timesReceived = m.group(1).split("-")[1];
    } else {
      return false;
    }
    return Integer.valueOf(barcodeString) != Integer.valueOf(timesReceived);
  };

  private final Predicate<SampleDto> byTimePointCodingMismatch = dto -> {
    // finds slides that are not coded as biopsy (6) or radical prostatectomy (7) in the sample alias
    if (getAttribute(ATTR_SLIDES, dto) == null) return false; // quick and dirty check for slides
    Matcher m = Pattern.compile(".*(\\d{1,5}-\\d{1,5}).*").matcher(dto.getName());
    if (!m.matches()) throw new IllegalArgumentException("Sample with alias " + dto.getName() + " is malformed.");
    String timePoint = m.group(1).split("-")[0];
    return Integer.valueOf(timePoint) < 6;
  };

  private final Predicate<SampleDto> byNotEmpty = sample -> {
    if (sample.getSampleType() == null)
      throw new IllegalArgumentException(String.format("Got very weird sample type for sample %s", sample.getName()));
    if (sample.getSampleType().contains("Illumina") || "Unknown".equals(sample.getSampleType())) return false;
    switch (sample.getSampleType()) {
    case SAMPLE_CLASS_SLIDE:
      // slides remaining
      Integer slides = getIntAttribute(ATTR_SLIDES, sample);
      Integer discards = getIntAttribute(ATTR_DISCARDS, sample);
      if (discards == null) {
        return slides > 0;
      }
      return slides > discards;
    default:
      return sample.getStorageLocation() != null && sample.getStorageLocation() != "" && sample.getStorageLocation() != "EMPTY";
    }
  };

  @SafeVarargs
  private final List<List<String>> getSingleTable(List<SampleDto> possibleSamples, Map<String, SampleDto> allSamplesById,
      List<SampleLabel> labelList, Predicate<SampleDto>... filters) {
    List<Predicate<SampleDto>> inputFilters = Arrays.asList(filters);
    List<SampleDto> filtered = filter(possibleSamples, inputFilters);
    if (filtered.isEmpty()) {
      // nothing to report on here, so skip it
      return Collections.emptyList();
    }

    return SITE_ORDER.stream()
        .map(site -> {
          List<Predicate<SampleDto>> allFilters = new ArrayList<>();
          allFilters.add(Site.predicate(site));
          allFilters.addAll(inputFilters);

          List<SampleDto> sams = filter(possibleSamples, allFilters);
          List<String> row = new ArrayList<>();
          row.add(site.getKey());
          row.addAll(getCounts(sams, allSamplesById, labelList));
          return row;
        })
        .collect(Collectors.toList());
  }

  private List<String> getCounts(List<SampleDto> samples, Map<String, SampleDto> samplesById, List<SampleLabel> labels) {
    return labels.stream()
        .map(label -> {
          Map<String, Long> labelled = getSampleAndCaseCounts(samples, Arrays.asList(label.predicate()), samplesById);
          return Arrays.asList(labelled.get(NUM_SAMPLES), labelled.get(NUM_CASES));
        })
        .flatMap(Collection::stream)
        .map(num -> num == null ? "0" : String.valueOf(num))
        .collect(Collectors.toList());
  }

  private Map<String, Long> getSampleAndCaseCounts(List<SampleDto> samples, Collection<Predicate<SampleDto>> predicates,
      Map<String, SampleDto> allSamples) {
    List<SampleDto> filteredSamples = filter(samples, predicates);
    List<SampleDto> cases = filteredSamples.stream()
        .map(sam -> getParent(sam, SAMPLE_CATEGORY_IDENTITY, allSamples))
        .distinct().collect(Collectors.toList());

    Map<String, Long> counts = new TreeMap<>();
    counts.put(NUM_SAMPLES, Long.valueOf(filteredSamples.size()));
    counts.put(NUM_CASES, Long.valueOf(cases.size()));
    return counts;
  }

  private List<String> labelListHeader(String header, List<SampleLabel> labelList) {
    // needs to have one column for the time point, then two columns for each sampleLabel
    // which are for the "Samples" and "Cases" columns in the {#samplesCasesHeader} method
    List<String> list = new ArrayList<>();
    list.add(header);
    for (SampleLabel label : labelList) {
      list.add(label.getKey());
      list.add("");
    }
    return list;
  }

  private List<String> sampleCasesHeader(List<SampleLabel> labelList) {
    // first column is "Site", then a pair of "Samples" + "Cases" columns for each sampleLabel in the list
    List<String> sampleCasesList = new ArrayList<>();
    sampleCasesList.add("Site");
    for (int i = 0; i < labelList.size(); i++) {
      sampleCasesList.add(NUM_SAMPLES);
      sampleCasesList.add(NUM_CASES);
    }
    return sampleCasesList;
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    String[] row = makeBlankRow();
    row[0] = String.format("Samples Received To Date (%s)", new SimpleDateFormat(DATE_FORMAT).format(new Date()));
    List<ColumnDefinition> columns = new ArrayList<>();
    for (int i = 0; i < row.length; i++) {
      columns.add(new ColumnDefinition(row[i]));
    }
    return columns;
  }

  private final int columnCount = labelListHeader("", labelListInventory).size(); // inventory is the table with the most columns

  @Override
  protected int getRowCount() {
    return 2 // headers
        + toDateRandom.size()
        + 4 // total, blank, headers
        + toDateSix.size()
        + 4 // total, blank, headers
        + toDateTwelve.size()
        + 4 // total, blank, headers
        + toDateEighteen.size()
        + 4 // total, blank, headers
        + toDateTwentyFour.size()
        + 4 // total, blank, headers
        + toDateSlides.size()
        + 5 // total, blank, this-month, headers
        + thisMonthRandom.size()
        + 4 // total, blank, headers
        + thisMonthSix.size()
        + 4 // total, blank, headers
        + thisMonthTwelve.size()
        + 4 // total, blank, headers
        + thisMonthEighteen.size()
        + 4 // total, blank, headers
        + thisMonthTwentyFour.size()
        + 4 // total, blank, headers
        + thisMonthSlides.size()
        + 5 // total, blank, inventory, headers
        + inventoryAvailable.size()
        + 4 // inventory total plus extra rows between headings (To Date, This Month, Inventory)
        + 2 // blank, header
        + barcodeAndNameCodingMismatch.size()
        + 2 // blank, header
        + slidesWithoutSlideTimepoint.size()
    ;
  }

  @Override
  protected String[] getRow(int rowNum) {
    String[] row = null;
    row = maybeInList(TimePoint.RANDOMIZATION.getKey(), labelListLong, toDateRandom, rowNum);
    if (row.length != 0) return row;
    rowNum -= (toDateRandom.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList(TimePoint.SIX.getKey(), labelListShort, toDateSix, rowNum);
    if (row.length != 0) return row;
    rowNum -= (toDateSix.size() + 4); // list + (blank, 2 headers, total). This step is here because Java passes by value, so subtracting
                                      // rowNum at the end of the method does nothing

    row = maybeInList(TimePoint.TWELVE.getKey(), labelListLong, toDateTwelve, rowNum);
    if (row.length != 0) return row;
    rowNum -= (toDateTwelve.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList(TimePoint.EIGHTEEN.getKey(), labelListShort, toDateEighteen, rowNum);
    if (row.length != 0) return row;
    rowNum -= (toDateEighteen.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList(TimePoint.TWENTY_FOUR.getKey(), labelListLong, toDateTwentyFour, rowNum);
    if (row.length != 0) return row;
    rowNum -= (toDateTwentyFour.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList("Tissue", labelListSlides, toDateSlides, rowNum);
    if (row.length != 0) return row;
    rowNum -= (toDateSlides.size() + 4); // list + (blank, 2 headers, total)

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeAlmostBlankRow(String.format("Samples Received This Month (%s - %s)", start, end));
    }
    rowNum -= 2;

    row = maybeInList(TimePoint.RANDOMIZATION.getKey(), labelListLong, thisMonthRandom, rowNum);
    if (row.length != 0) return row;
    rowNum -= (thisMonthRandom.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList(TimePoint.SIX.getKey(), labelListShort, thisMonthSix, rowNum);
    if (row.length != 0) return row;
    rowNum -= (thisMonthSix.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList(TimePoint.TWELVE.getKey(), labelListLong, thisMonthTwelve, rowNum);
    if (row.length != 0) return row;
    rowNum -= (thisMonthTwelve.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList(TimePoint.EIGHTEEN.getKey(), labelListShort, thisMonthEighteen, rowNum);
    if (row.length != 0) return row;
    rowNum -= (thisMonthEighteen.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList(TimePoint.TWENTY_FOUR.getKey(), labelListLong, thisMonthTwentyFour, rowNum);
    if (row.length != 0) return row;
    rowNum -= (thisMonthTwentyFour.size() + 4); // list + (blank, 2 headers, total)

    row = maybeInList("Tissue", labelListSlides, thisMonthSlides, rowNum);
    if (row.length != 0) return row;
    rowNum -= (thisMonthSlides.size() + 4); // list + (blank, 2 headers, total)

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeAlmostBlankRow(String.format("Total Inventory Available (%s)", new SimpleDateFormat(DATE_FORMAT).format(new Date())));
    }
    rowNum -= 2;

    row = maybeInList("Total in Inventory", labelListInventory, inventoryAvailable, rowNum);
    if (row.length != 0) return row;
    rowNum -= (inventoryAvailable.size() + 4); // list + (blank, 2 headers, total)

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      String[] blank = makeBlankRow();
      blank[0] = "Samples with mismatched alias and cryovial/slide number";
      return blank;
    }

    row = maybeSampleAlias(barcodeAndNameCodingMismatch, rowNum);
    if (row.length != 0) return row;
    rowNum -= barcodeAndNameCodingMismatch.size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      String[] blank = makeBlankRow();
      blank[0] = "Slides with non-slide timepoint in alias";
      return blank;
    }

    row = maybeSampleAlias(slidesWithoutSlideTimepoint, rowNum);
    if (row.length != 0) return row;
    rowNum -= slidesWithoutSlideTimepoint.size();

    return makeBlankRow();
  }

  private String[] maybeInList(String category, List<SampleLabel> labels, List<List<String>> list, int rowNum) {
    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return generateRow(labelListHeader(category, labels));
    } else if (rowNum == 2) {
      return generateRow(sampleCasesHeader(labels));
    }
    rowNum -= 3;
    if (rowNum < list.size()) {
      return generateRow(list.get(rowNum));
    } else if (rowNum == list.size()) {
      return generateTotalsRow(list, labels);
    }
    return new String[0];
    // do our subtraction of the list + totals outside this method, because changes here won't affect rowNum outside the method
  }

  private String[] maybeSampleAlias(List<SampleDto> samples, int rowNum) {
    if (rowNum < samples.size()) {
      SampleDto sample = samples.get(rowNum);
      String[] row = makeAlmostBlankRow(sample.getName());
      row[1] = sample.getTubeBarcode();
      return row;
    }
    return new String[0];
  }

  private String[] generateRow(List<String> received) {
    String[] row = makeBlankRow();
    for (int i = 0; i < received.size(); i++) {
      row[i] = received.get(i);
    }
    return row;
  }

  private String[] generateZerosRow(String firstColumn, List<SampleLabel> labels) {
    String[] zeros = makeAlmostBlankRow(firstColumn);
    for (int i = 1; i < zeros.length; i++) {
      zeros[i] = i <= 2 * labels.size() ? "0" : "";
    }
    return zeros;
  }

  private String[] generateTotalsRow(List<List<String>> table, List<SampleLabel> labels) {
    if (table.isEmpty()) {
      return generateZerosRow("Totals", labels);
    }
    String[] row = new String[columnCount];
    row[0] = "Totals";

    for (int i = 1; i < table.get(0).size(); i++) {
      int sum = 0;
      for (List<String> tableRow : table) {
        if (tableRow.get(i) != null) {
          sum += Integer.valueOf(tableRow.get(i));
        }
      }
      row[i] = String.valueOf(sum);
    }
    return row;
  }

  private String[] makeBlankRow() {
    String[] row = new String[columnCount];
    for (int i = 0; i < row.length; i++) {
      row[i] = "";
    }
    return row;
  }

  private String[] makeAlmostBlankRow(String firstCell) {
    String[] blank = makeBlankRow();
    blank[0] = firstCell;
    return blank;
  }
}
