package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.AttributeDto;
import ca.on.oicr.ws.dto.SampleDto;
import ca.on.oicr.ws.dto.UserDto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

public class OctaneCountsReport extends TableReport {

  private static class Count implements Comparable<Count> {
    private final String key;
    private final long value;

    public Count(String key, long value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public long getValue() {
      return value;
    }

    @Override
    public int compareTo(Count o) {
      int compare = key.toUpperCase().compareTo(o.key.toUpperCase());
      if (compare == 0) {
        return Long.compare(value, o.value);
      } else {
        return compare;
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((key == null) ? 0 : key.hashCode());
      result = prime * result + (int) (value ^ (value >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Count other = (Count) obj;
      if (key == null) {
        if (other.key != null) return false;
      } else if (!key.equals(other.key)) return false;
      return value == other.value;
    }
  }

  public static final String REPORT_NAME = "octane";
  public static final String CATEGORY = REPORT_CATEGORY_COUNTS;

  private static final Option OPT_AFTER = CommonOptions.after(false);
  private static final Option OPT_BEFORE = CommonOptions.before(false);
  private static final Option OPT_USER_IDS = CommonOptions.users(false);
  private static final Option OPT_SITE_PREFIX =
      Option.builder()
          .longOpt("sitePrefix")
          .hasArg()
          .argName("sitePrefix")
          .required(false)
          .desc("Optional MISO alias prefix for site to filter on")
          .build();

  private static final List<ColumnDefinition> COLUMNS =
      Collections.unmodifiableList(
          Arrays.asList(new ColumnDefinition("CASE NUMBERS"), new ColumnDefinition("")));

  private String start;
  private String end;
  private final List<Integer> userIds = new ArrayList<>();
  private String sitePrefix = null;
  private Map<Integer, UserDto> allUsersById;

  private final List<Count> caseNumbers = new ArrayList<>();
  private final List<Count> sampleNumbers = new ArrayList<>();
  private List<Count> inventoryNumbers = new ArrayList<>();
  private List<Count> tissueTypeNumbers = new ArrayList<>();
  private List<Count> tissueOriginNumbers = new ArrayList<>();

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_AFTER, OPT_BEFORE, OPT_USER_IDS, OPT_SITE_PREFIX);
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

    if (cmd.hasOption(OPT_USER_IDS.getLongOpt())) {
      String[] users = cmd.getOptionValue(OPT_USER_IDS.getLongOpt()).split(",");
      for (String user : users) {

        if (user != null && !"".equals(user)) {
          userIds.add(Integer.valueOf(user));
        }
      }
    }

    if (cmd.hasOption(OPT_SITE_PREFIX.getLongOpt())) {
      sitePrefix = cmd.getOptionValue(OPT_SITE_PREFIX.getLongOpt());
    }
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public String getTitle() {
    return "OCTANE Counts "
        + (sitePrefix == null ? "" : String.format("for site %s for ", sitePrefix))
        + (start == null ? "Any Time" : start)
        + " - "
        + (end == null ? "Now" : end)
        + ", and Inventory "
        + new SimpleDateFormat(DATE_FORMAT).format(new Date())
        + (userIds.isEmpty() ? " for all users" : " for selected users");
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<UserDto> allUsers = pinery.getUser().all();
    allUsersById = mapUsersById(allUsers);

    List<SampleDto> allOctaneSamples =
        pinery
            .getSample()
            .allFiltered(new SamplesFilter().withProjects(Lists.newArrayList("OCT", "OCTCAP")));
    Map<String, SampleDto> allOctaneSamplesById = mapSamplesById(allOctaneSamples);
    if (sitePrefix != null) {
      allOctaneSamples = filterBySitePrefix(allOctaneSamples);
    }
    if (allOctaneSamples.isEmpty()) {
      throw new IllegalStateException("Could not find any samples in Pinery");
    }

    List<SampleDto> buffyCoats = new ArrayList<>(); // Ly_R tissues
    List<SampleDto> plasma = new ArrayList<>(); // Pl_R tissues
    List<SampleDto> ctDnaPlasma = new ArrayList<>(); // Ct_T tissues
    List<SampleDto> slides = new ArrayList<>(); // Slides
    List<SampleDto> stocksFromBuffy = new ArrayList<>(); // Ly_R stocks
    List<SampleDto> stocksFromCt = new ArrayList<>(); // Ct_T stocks
    List<SampleDto> stockDnaFromSlides = new ArrayList<>(); // DNA Stocks with Slide in hierarchy
    List<SampleDto> stockRnaFromSlides = new ArrayList<>(); // RNA Stocks with Slide in hierarchy
    List<SampleDto> dnaAliquots = new ArrayList<>(); // DNA aliquots
    List<SampleDto> dnaAliquotsFromSlides =
        new ArrayList<>(); // DNA aliquots with Slide in hierarchy
    List<SampleDto> dnaAliquotsFromBuffy = new ArrayList<>(); // DNA aliquots with Ly_R tissue
    List<SampleDto> dnaAliquotsFromCt = new ArrayList<>(); // DNA aliquots with Ct_T tissue
    List<SampleDto> rnaAliquots = new ArrayList<>(); // RNA aliquots
    List<SampleDto> rnaAliquotsFromSlides =
        new ArrayList<>(); // RNA aliquots with Slide in hierarchy

    for (SampleDto sam : allOctaneSamples) {
      String category = getAttribute(ATTR_CATEGORY, sam);
      if (category == null) {
        // don't need libraries/dilutions
        continue;
      }
      if (SAMPLE_CLASS_SLIDE.equals(sam.getSampleType())) {
        addSlidesRemainingCount(sam);
        slides.add(sam);
        continue;
      }
      switch (category) {
        case SAMPLE_CATEGORY_TISSUE:
          if (tissueOriginAndTypeMatch(sam, "Ly", "R")) {
            buffyCoats.add(sam);
          } else if (tissueOriginAndTypeMatch(sam, "Pl", "R")) {
            plasma.add(sam);
          } else if (tissueOriginAndTypeMatch(sam, "Ct", "T")) {
            ctDnaPlasma.add(sam);
          }
          break;
        case SAMPLE_CATEGORY_STOCK:
          SampleDto tissue = getParent(sam, SAMPLE_CATEGORY_TISSUE, allOctaneSamplesById);
          if (tissueOriginAndTypeMatch(tissue, "Ly", "R")) {
            stocksFromBuffy.add(sam);
          } else if (tissueOriginAndTypeMatch(tissue, "Ct", "T")) {
            stocksFromCt.add(sam);
          }
          if (getOptionalParent(sam, SAMPLE_CLASS_SLIDE, allOctaneSamplesById) != null) {
            if (sam.getSampleType().contains("DNA")) {
              stockDnaFromSlides.add(sam);
            } else if (sam.getSampleType().contains("RNA")) {
              stockRnaFromSlides.add(sam);
            }
          }
          break;
        case SAMPLE_CATEGORY_ALIQUOT:
          if (sam.getSampleType().contains("DNA")) {
            if (getOptionalParent(sam, SAMPLE_CLASS_SLIDE, allOctaneSamplesById) != null) {
              dnaAliquotsFromSlides.add(sam);
            }
            if (tissueOriginAndTypeMatch(
                getParent(sam, SAMPLE_CATEGORY_TISSUE, allOctaneSamplesById), "Ly", "R")) {
              dnaAliquotsFromBuffy.add(sam);
            } else if (tissueOriginAndTypeMatch(
                getParent(sam, SAMPLE_CATEGORY_TISSUE, allOctaneSamplesById), "Ct", "T")) {
              dnaAliquotsFromCt.add(sam);
            } else {
              dnaAliquots.add(sam);
            }
          } else if (SAMPLE_CLASS_WHOLE_RNA.equals(sam.getSampleType())) {
            if (!tissueOriginAndTypeMatch(
                    getParent(sam, SAMPLE_CATEGORY_TISSUE, allOctaneSamplesById), "Ly", "R")
                && !tissueOriginAndTypeMatch(
                    getParent(sam, SAMPLE_CATEGORY_TISSUE, allOctaneSamplesById), "Ct", "T")) {
              rnaAliquots.add(sam);
            }
            if (getOptionalParent(sam, SAMPLE_CLASS_SLIDE, allOctaneSamplesById) != null) {
              rnaAliquotsFromSlides.add(sam);
            }
          }
          break;
        default:
          // Other sample categories are not applicable
          continue;
      }
    }
    calculateConsumedSlides(allOctaneSamples, allOctaneSamplesById);

    // Inventory Numbers
    inventoryNumbers = new ArrayList<>();

    List<SampleDto> unstainedInventory =
        filter(
            slides,
            Arrays.asList(byStain(null), withSlidesRemaining, byCreator(userIds), byCustodyTp()));
    inventoryNumbers.add(
        new Count("Tumor Tissue Unstained Slides", countSlidesRemaining(unstainedInventory)));
    inventoryNumbers.add(
        new Count(
            "Tumor Tissue Unstained Slides (Cases)",
            countUniqueIdentities(unstainedInventory, allOctaneSamplesById)));

    List<SampleDto> heInventory =
        filter(
            slides,
            Arrays.asList(
                byStain(STAIN_HE), withSlidesRemaining, byCreator(userIds), byCustodyTp()));
    inventoryNumbers.add(new Count("Tumor Tissue H&E Slides", countSlidesRemaining(heInventory)));
    inventoryNumbers.add(
        new Count(
            "Tumor Tissue H&E Slides (Cases)",
            countUniqueIdentities(heInventory, allOctaneSamplesById)));

    List<SampleDto> buffyInventory =
        filter(filterNonEmpty(filterByCreator(buffyCoats)), byCustodyTp());
    inventoryNumbers.add(new Count("Buffy Coat", buffyInventory.size()));
    inventoryNumbers.add(
        new Count(
            "Buffy Coat (Cases)", countUniqueIdentities(buffyInventory, allOctaneSamplesById)));

    List<SampleDto> plasmaInventory =
        filter(filterNonEmpty(filterByCreator(plasma)), byCustodyTp());
    inventoryNumbers.add(new Count("Plasma", plasmaInventory.size()));
    inventoryNumbers.add(
        new Count("Plasma (Cases)", countUniqueIdentities(plasmaInventory, allOctaneSamplesById)));

    List<SampleDto> ctInventory =
        filter(filterNonEmpty(filterByCreator(ctDnaPlasma)), byCustodyTp());
    inventoryNumbers.add(new Count("ctDNA Plasma", ctInventory.size()));
    inventoryNumbers.add(
        new Count(
            "ctDNA Plasma (Cases)", countUniqueIdentities(ctInventory, allOctaneSamplesById)));

    List<SampleDto> buffyStockInventory =
        filter(filterNonEmpty(filterByCreator(stocksFromBuffy)), byCustodyTp());
    inventoryNumbers.add(new Count("Extracted Buffy Coat", buffyStockInventory.size()));
    inventoryNumbers.add(
        new Count(
            "Extracted Buffy Coat (Cases)",
            countUniqueIdentities(buffyStockInventory, allOctaneSamplesById)));

    List<SampleDto> ctStockInventory =
        filter(filterNonEmpty(filterByCreator(stocksFromCt)), byCustodyTp());
    inventoryNumbers.add(new Count("Extracted cfDNA", ctStockInventory.size()));
    inventoryNumbers.add(
        new Count(
            "Extracted cfDNA (Cases)",
            countUniqueIdentities(ctStockInventory, allOctaneSamplesById)));

    // These next four need to be filtered to include only Transformative Pathology users
    List<SampleDto> stockDnaFromSlideInventory =
        filter(filterNonEmpty(filterByCreator(stockDnaFromSlides)), byCustodyTp());
    inventoryNumbers.add(new Count("Tumor Tissue DNA", stockDnaFromSlideInventory.size()));
    inventoryNumbers.add(
        new Count(
            "Tumor Tissue DNA (Cases)",
            countUniqueIdentities(stockDnaFromSlideInventory, allOctaneSamplesById)));

    List<SampleDto> stockRnaFromSlideInventory =
        filter(filterNonEmpty(filterByCreator(stockRnaFromSlides)), byCustodyTp());
    inventoryNumbers.add(new Count("Tumor Tissue RNA", stockRnaFromSlideInventory.size()));
    inventoryNumbers.add(
        new Count(
            "Tumor Tissue RNA (Cases)",
            countUniqueIdentities(stockRnaFromSlideInventory, allOctaneSamplesById)));

    Map<String, Set<SampleDto>> unstainedSlideCasesByTissueType = new HashMap<>();
    Map<String, Set<SampleDto>> unstainedSlideCasesByTissueOrigin = new HashMap<>();

    slides
        .stream()
        .filter(byStain(null))
        .filter(byCustodyTp())
        .forEach(
            sam -> {
              String tissueType = getUpstreamAttribute(ATTR_TISSUE_TYPE, sam, allOctaneSamplesById);
              Set<SampleDto> ttCases =
                  unstainedSlideCasesByTissueType.getOrDefault(tissueType, new HashSet<>());
              ttCases.add(sam);
              unstainedSlideCasesByTissueType.put(tissueType, ttCases);

              String tissueOrigin =
                  getUpstreamAttribute(ATTR_TISSUE_ORIGIN, sam, allOctaneSamplesById);
              Set<SampleDto> toCases =
                  unstainedSlideCasesByTissueOrigin.getOrDefault(tissueOrigin, new HashSet<>());
              toCases.add(sam);
              unstainedSlideCasesByTissueOrigin.put(tissueOrigin, toCases);
            });

    tissueTypeNumbers =
        unstainedSlideCasesByTissueType
            .entrySet()
            .stream()
            .map(entry -> new Count(entry.getKey(), entry.getValue().size()))
            .sorted()
            .collect(Collectors.toList());
    tissueOriginNumbers =
        unstainedSlideCasesByTissueOrigin
            .entrySet()
            .stream()
            .map(entry -> new Count(entry.getKey(), entry.getValue().size()))
            .sorted()
            .collect(Collectors.toList());

    // Filter down to samples within date range
    List<Predicate<SampleDto>> byDatesAndUsers =
        Arrays.asList(byCreatedBetween(start, end), byCreator(userIds));
    List<SampleDto> newBuffyCoats = filter(buffyCoats, byDatesAndUsers);
    List<SampleDto> newPlasma = filter(plasma, byDatesAndUsers);
    List<SampleDto> newCtDnaPlasma = filter(ctDnaPlasma, byDatesAndUsers);
    List<SampleDto> newSlides = filter(slides, byDatesAndUsers);
    stocksFromBuffy = filter(stocksFromBuffy, byDatesAndUsers);
    stocksFromCt = filter(stocksFromCt, byDatesAndUsers);
    stockDnaFromSlides = filter(stockDnaFromSlides, byDatesAndUsers);
    dnaAliquots = filter(dnaAliquots, byDatesAndUsers);
    dnaAliquotsFromSlides = filter(dnaAliquotsFromSlides, byDatesAndUsers);
    dnaAliquotsFromBuffy = filter(dnaAliquotsFromBuffy, byDatesAndUsers);
    dnaAliquotsFromCt = filter(dnaAliquotsFromCt, byDatesAndUsers);
    rnaAliquots = filter(rnaAliquots, byDatesAndUsers);
    rnaAliquotsFromSlides = filter(rnaAliquotsFromSlides, byDatesAndUsers);

    // Case Numbers
    caseNumbers.add(
        new Count("Tumor Tissue Rec'd", countNewCases(newSlides, slides, allOctaneSamplesById)));
    caseNumbers.add(
        new Count(
            "Tumor Tissue Extracted",
            countUniqueIdentities(stockDnaFromSlides, allOctaneSamplesById)));

    caseNumbers.add(
        new Count(
            "Tumor Tissue DNA Transferred",
            countUniqueIdentities(dnaAliquotsFromSlides, allOctaneSamplesById)));
    caseNumbers.add(
        new Count(
            "Tumor Tissue RNA Transferred",
            countUniqueIdentities(rnaAliquotsFromSlides, allOctaneSamplesById)));

    caseNumbers.add(
        new Count(
            "Buffy Coat Rec'd", countNewCases(newBuffyCoats, buffyCoats, allOctaneSamplesById)));
    caseNumbers.add(
        new Count(
            "Buffy Coat Extracted", countUniqueIdentities(stocksFromBuffy, allOctaneSamplesById)));
    caseNumbers.add(
        new Count(
            "Buffy Coat Transferred",
            countUniqueIdentities(dnaAliquotsFromBuffy, allOctaneSamplesById)));
    caseNumbers.add(
        new Count("Plasma Rec'd", countNewCases(newPlasma, plasma, allOctaneSamplesById)));
    caseNumbers.add(
        new Count(
            "cfDNA Plasma Rec'd",
            countNewCases(newCtDnaPlasma, ctDnaPlasma, allOctaneSamplesById)));
    caseNumbers.add(
        new Count("ctDNA Extracted", countUniqueIdentities(stocksFromCt, allOctaneSamplesById)));
    caseNumbers.add(
        new Count(
            "ctDNA Transferred", countUniqueIdentities(dnaAliquotsFromCt, allOctaneSamplesById)));

    // Sample numbers
    sampleNumbers.add(new Count("Tumor Tissue Rec'd", countSlidesReceived(newSlides)));
    sampleNumbers.add(new Count("Tumor Tissue Extracted", stockDnaFromSlides.size()));

    sampleNumbers.add(new Count("Tumor Tissue DNA Transferred", dnaAliquots.size()));
    sampleNumbers.add(new Count("Tumor Tissue RNA Transferred", rnaAliquots.size()));

    sampleNumbers.add(new Count("Buffy Coat Rec'd", newBuffyCoats.size()));
    sampleNumbers.add(new Count("Buffy Coat Extracted", stocksFromBuffy.size()));
    sampleNumbers.add(new Count("Buffy Coat Transferred", dnaAliquotsFromBuffy.size()));
    sampleNumbers.add(new Count("Plasma Rec'd", newPlasma.size()));
    sampleNumbers.add(new Count("cfDNA Plasma Rec'd", newCtDnaPlasma.size()));
    sampleNumbers.add(new Count("cfDNA Extracted", stocksFromCt.size()));
    sampleNumbers.add(new Count("cfDNA Transferred", dnaAliquotsFromCt.size()));
  }

  private List<SampleDto> filterByCreator(List<SampleDto> unfiltered) {
    Set<Predicate<SampleDto>> filters = Sets.newHashSet();
    if (!userIds.isEmpty()) {
      filters.add(byCreator(userIds));
    }
    return filter(unfiltered, filters);
  }

  private List<SampleDto> filterBySitePrefix(List<SampleDto> unfiltered) {
    Set<Predicate<SampleDto>> filters = Sets.newHashSet();
    filters.add(dto -> dto.getName().startsWith(sitePrefix));
    return filter(unfiltered, filters);
  }

  private void addSlidesRemainingCount(SampleDto slide) {
    Integer slides = getIntAttribute(ATTR_SLIDES, slide, 0);
    Integer discards = getIntAttribute(ATTR_DISCARDS, slide, 0);
    AttributeDto attr = new AttributeDto();
    attr.setName(ATTR_REMAINING);
    attr.setValue(Integer.toString(slides - discards));
    slide.getAttributes().add(attr);
  }

  private void calculateConsumedSlides(
      Collection<SampleDto> samples, Map<String, SampleDto> potentialParents) {
    samples
        .stream()
        .filter(sam -> getAttribute(ATTR_CONSUMED, sam) != null)
        .forEach(
            sam ->
                sam.getParents()
                    .forEach(
                        ref -> {
                          SampleDto parent = potentialParents.get(ref.getId());
                          AttributeDto remainingAttr =
                              parent
                                  .getAttributes()
                                  .stream()
                                  .filter(attr -> ATTR_REMAINING.equals(attr.getName()))
                                  .findFirst()
                                  .get();
                          Integer remaining = Integer.parseInt(remainingAttr.getValue());
                          Integer consumed = Integer.parseInt(getAttribute(ATTR_CONSUMED, sam));
                          remainingAttr.setValue(Integer.toString(remaining - consumed));
                        }));
  }

  private int countSlidesRemaining(Collection<SampleDto> samples) {
    return samples.stream().mapToInt(s -> getIntAttribute(ATTR_REMAINING, s)).sum();
  }

  private int countSlidesReceived(Collection<SampleDto> samples) {
    return samples.stream().mapToInt(s -> getIntAttribute(ATTR_SLIDES, s)).sum();
  }

  private static Predicate<SampleDto> byStain(String stain) {
    return slide ->
        stain == null
            ? getAttribute(ATTR_STAIN, slide) == null
            : stain.equals(getAttribute(ATTR_STAIN, slide));
  }

  private static Predicate<SampleDto> byCustodyTp() {
    return sample -> {
      String custody = getAttribute(ATTR_CUSTODY, sample);
      return custody == null || custody.equals("TP") || custody.equals("Unspecified (Internal)");
    };
  }

  /**
   * Find the number of unique identities amongst a set of new samples, excluding those with
   * descendants created before the report start date in another set of samples. For example, this
   * can be used to find the number of Identities for which slides were received for the first time
   * during the reporting period. To determine this, newSamples would contain all slides created
   * during the reporting period, and potentialOldSamples would contain all slides ever created
   *
   * @param newSamples filtered samples for initial Identity count
   * @param potentialOldSamples samples to exclude Identities for if the sample's creation date is
   *     before the report start date
   * @param potentialParents all samples, used for navigating the hierarchy from the other samples
   *     to their Identities
   * @return the count of unique Identities with descendants in newSamples, but no descendants
   *     created before the report start date in potentialOldSamples
   */
  private long countNewCases(
      Collection<SampleDto> newSamples,
      Collection<SampleDto> potentialOldSamples,
      Map<String, SampleDto> potentialParents) {
    if (start == null) {
      // return all the old samples, since we want all samples ever created
      return getUniqueIdentities(newSamples, potentialParents).stream().count();
    }
    return getUniqueIdentities(newSamples, potentialParents)
        .stream()
        .filter(
            identity ->
                potentialOldSamples
                    .stream()
                    .noneMatch(
                        sam ->
                            identity
                                    .getId()
                                    .equals(getParent(sam, "Identity", potentialParents).getId())
                                && sam.getCreatedDate().compareTo(start) < 0))
        .count();
  }

  private List<SampleDto> getUniqueIdentities(
      Collection<SampleDto> samples, Map<String, SampleDto> potentialParents) {
    return samples
        .stream()
        .map(sam -> getParent(sam, SAMPLE_CATEGORY_IDENTITY, potentialParents))
        .distinct()
        .collect(Collectors.toList());
  }

  private int countUniqueIdentities(
      Collection<SampleDto> samples, Map<String, SampleDto> potentialParents) {
    return getUniqueIdentities(samples, potentialParents).size();
  }

  private static boolean tissueOriginAndTypeMatch(
      SampleDto sample, String tissueOrigin, String tissueType) {
    String origin = getAttribute(ATTR_TISSUE_ORIGIN, sample);
    String type = getAttribute(ATTR_TISSUE_TYPE, sample);
    if (origin == null || type == null) {
      throw new IllegalArgumentException("sample is missing tissue origin or tissue type");
    }
    return tissueOrigin.equals(origin) && tissueType.equals(type);
  }

  private String getUserNames(List<Integer> userIds) {
    return allUsersById
        .entrySet()
        .stream()
        .filter(userById -> userIds.contains(userById.getKey()))
        .map(
            userById ->
                String.format(
                    "%s %s", userById.getValue().getFirstname(), userById.getValue().getLastname()))
        .collect(Collectors.joining(", "));
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return Arrays.asList(
        new ColumnDefinition(
            String.format(
                "CASE NUMBERS (%s - %s) %s%s",
                (start == null ? "All Time" : start),
                (end == null ? "Now" : end),
                (sitePrefix == null ? "" : String.format(" site %s for ", sitePrefix)),
                (userIds.isEmpty() ? "all users" : " users " + getUserNames(userIds)))),
        new ColumnDefinition(""));
  }

  @Override
  protected int getRowCount() {
    return caseNumbers.size()
        + 2
        + sampleNumbers.size()
        + 2
        + inventoryNumbers.size()
        + 2
        + tissueTypeNumbers.size()
        + 2
        + tissueOriginNumbers.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    if (rowNum < caseNumbers.size()) {
      return makeCountRow(caseNumbers.get(rowNum));
    }
    rowNum -= caseNumbers.size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow(
          String.format(
              "SAMPLE NUMBERS (%s - %s)",
              (start == null ? "All Time" : start), (end == null ? "Now" : end)));
    }
    rowNum -= 2;
    if (rowNum < sampleNumbers.size()) {
      return makeCountRow(sampleNumbers.get(rowNum));
    }
    rowNum -= sampleNumbers.size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      Date now = new Date();
      DateFormat df = new SimpleDateFormat(DATETIME_FORMAT);
      return makeHeadingRow("INVENTORY NUMBERS (" + df.format(now) + ")");
    }
    rowNum -= 2;
    if (rowNum < inventoryNumbers.size()) {
      return makeCountRow(inventoryNumbers.get(rowNum));
    }
    rowNum -= inventoryNumbers.size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow("CASES WITH UNSTAINED SLIDES BY TISSUE TYPE");
    }
    rowNum -= 2;
    if (rowNum < tissueTypeNumbers.size()) {
      return makeCountRow(tissueTypeNumbers.get(rowNum));
    }
    rowNum -= tissueTypeNumbers.size();

    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow("CASES WITH UNSTAINED SLIDES BY TISSUE ORIGIN");
    }
    rowNum -= 2;
    return makeCountRow(tissueOriginNumbers.get(rowNum));
  }

  private static String[] makeCountRow(Count count) {
    return new String[] {count.getKey(), Long.toString(count.getValue())};
  }

  private static String[] makeBlankRow() {
    String[] row = new String[COLUMNS.size()];
    for (int i = 0; i < row.length; i++) {
      row[i] = "";
    }
    return row;
  }

  private static String[] makeHeadingRow(String heading) {
    String[] row = makeBlankRow();
    row[0] = heading;
    return row;
  }
}
