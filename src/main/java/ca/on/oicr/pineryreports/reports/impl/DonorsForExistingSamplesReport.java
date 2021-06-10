package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.REPORT_CATEGORY_INVENTORY;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.itextpdf.layout.property.TextAlignment;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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

/** OCTANE Report Request https://jira.oicr.on.ca/browse/GR-710 */
public class DonorsForExistingSamplesReport extends TableReport {

  public static final String REPORT_NAME = "donors-for-existing-samples";
  public static final String CATEGORY = REPORT_CATEGORY_INVENTORY;

  private static final List<ColumnDefinition> COLUMNS =
      Collections.unmodifiableList(
          Arrays.asList(
              new ColumnDefinition("Identity Alias", TextAlignment.CENTER),
              new ColumnDefinition("Sample Alias")));

  private static List<String> bloodOrigins = Arrays.asList("Ly", "Ct", "Pl");

  private final Map<String, List<SampleDto>> tissuesByType = new HashMap<>();
  private final Map<String, List<SampleDto>> dnaByType = new HashMap<>();
  private final Map<String, List<SampleDto>> rnaByType = new HashMap<>();
  private final Map<String, List<SampleDto>> bloodByType = new HashMap<>();

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet();
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    // no options
  }

  @Override
  public String getTitle() {
    return "Donors for non-empty slides/DNA/RNA";
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> allSamples =
        pinery.getSample().allFiltered(new SamplesFilter().withProjects(Lists.newArrayList("OCT")));
    Map<String, SampleDto> allSamplesById = mapSamplesById(allSamples);

    List<SampleDto> tissue = filterReportableSlides(allSamples, allSamplesById);
    List<SampleDto> dna = filterReportableAnalyte(allSamples, allSamplesById, true);
    List<SampleDto> rna = filterReportableAnalyte(allSamples, allSamplesById, false);

    tissuesByType.put("P", deduplicateIdentities(filterBySampleNameContainsSection("P", tissue)));
    tissuesByType.put("M", deduplicateIdentities(filterBySampleNameContainsSection("M", tissue)));
    tissuesByType.put("n", deduplicateIdentities(filterBySampleNameContainsSection("n", tissue)));
    tissuesByType.put(
        "other",
        deduplicateIdentities(
            tissue.stream()
                .filter(byOther())
                .sorted((s1, s2) -> s1.getName().compareTo(s2.getName()))
                .collect(Collectors.toList())));

    dnaByType.put("P", deduplicateIdentities(filterBySampleNameContainsSection("P", dna)));
    dnaByType.put("M", deduplicateIdentities(filterBySampleNameContainsSection("M", dna)));
    dnaByType.put("n", deduplicateIdentities(filterBySampleNameContainsSection("n", dna)));
    dnaByType.put(
        "other",
        deduplicateIdentities(
            dna.stream()
                .filter(byOther())
                .sorted((s1, s2) -> s1.getName().compareTo(s2.getName()))
                .collect(Collectors.toList())));

    rnaByType.put("P", deduplicateIdentities(filterBySampleNameContainsSection("P", rna)));
    rnaByType.put("M", deduplicateIdentities(filterBySampleNameContainsSection("M", rna)));
    rnaByType.put("n", deduplicateIdentities(filterBySampleNameContainsSection("n", rna)));
    rnaByType.put(
        "other",
        deduplicateIdentities(
            rna.stream()
                .filter(byOther())
                .sorted((s1, s2) -> s1.getName().compareTo(s2.getName()))
                .collect(Collectors.toList())));

    bloodByType.put("BC", deduplicateIdentities(filterReportableBloodTissue(allSamples, "Ly")));
    bloodByType.put("BC DNA", deduplicateIdentities(filterReportableBloodStock(allSamples, "Ly")));
    bloodByType.put("Plasma", deduplicateIdentities(filterReportableBloodTissue(allSamples, "Pl")));
    bloodByType.put(
        "ctPlasma", deduplicateIdentities(filterReportableBloodTissue(allSamples, "Ct")));
  }

  private List<SampleDto> filterBySampleNameContainsSection(
      String target, List<SampleDto> unfiltered) {
    return unfiltered.stream()
        .filter(bySampleNameContains(target))
        .sorted((s1, s2) -> s1.getName().compareTo(s2.getName()))
        .collect(Collectors.toList());
  }

  private Predicate<SampleDto> bySampleNameContains(String target) {
    return dto -> dto.getName().contains("_" + target + "_");
  }

  private Predicate<SampleDto> byOther() {
    return dto ->
        !dto.getName().contains("_P_")
            && !dto.getName().contains("_M_")
            && !dto.getName().contains("_n_");
  }

  private List<SampleDto> deduplicateIdentities(List<SampleDto> full) {
    List<SampleDto> withUniqueIdentities = new ArrayList<>();
    Set<String> identityAliases = new HashSet<>();
    for (SampleDto sample : full) {
      String identity = sample.getName().substring(0, 10);
      if (!identityAliases.contains(identity)) {
        identityAliases.add(identity);
        withUniqueIdentities.add(sample);
      }
    }
    return withUniqueIdentities;
  }

  private List<SampleDto> filterReportableSlides(
      List<SampleDto> unfiltered, Map<String, SampleDto> allSamplesById) {
    Set<Predicate<SampleDto>> filters = Sets.newHashSet();
    filters.add(bySlide());
    filters.add(byUnstained());
    filters.add(byNonEmpty());
    filters.add(byNotBlood(allSamplesById));
    return filter(unfiltered, filters);
  }

  private List<SampleDto> filterReportableBloodTissue(
      List<SampleDto> unfiltered, String bloodType) {
    Set<Predicate<SampleDto>> filters = Sets.newHashSet();
    filters.add(bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    filters.add(byNonEmpty());
    filters.add(bySampleNameContains(bloodType));
    return filter(unfiltered, filters);
  }

  private List<SampleDto> filterReportableBloodStock(List<SampleDto> unfiltered, String bloodType) {
    Set<Predicate<SampleDto>> filters = Sets.newHashSet();
    filters.add(bySampleNameContains("Ly"));
    filters.add(bySampleCategory(SAMPLE_CATEGORY_STOCK));
    filters.add(byDna());
    return filter(unfiltered, filters);
  }

  private List<SampleDto> filterReportableAnalyte(
      List<SampleDto> unfiltered, Map<String, SampleDto> allSamplesById, boolean dna) {
    Set<Predicate<SampleDto>> filters = Sets.newHashSet();
    filters.add(bySampleCategory(SAMPLE_CATEGORY_STOCK));
    filters.add(byNonEmpty());
    filters.add(byNotBlood(allSamplesById));
    if (dna) {
      filters.add(byDna());
    } else {
      filters.add(byRna());
    }
    return filter(unfiltered, filters);
  }

  private static Predicate<SampleDto> bySlide() {
    return dto -> {
      return dto.getSampleType() != null && SAMPLE_CLASS_SLIDE.equals(dto.getSampleType());
    };
  }

  private static Predicate<SampleDto> byNonEmpty() {
    return dto -> {
      String slides = getAttribute(ATTR_SLIDES, dto);
      if (slides != null && Integer.parseInt(slides) <= 0) {
        return false; // slides are used up
      }
      if (getAttribute(ATTR_DISTRIBUTED, dto) != null) return false; // sample has been distributed
      if (dto.getStorageLocation() == null) return true;
      return !dto.getStorageLocation().contains("EMPTY");
    };
  }

  private static Predicate<SampleDto> byUnstained() {
    return dto -> {
      return getAttribute(ATTR_STAIN, dto) == null;
    };
  }

  private static Predicate<SampleDto> byDna() {
    return dto -> {
      return dto.getSampleType() != null && dto.getSampleType().contains("DNA");
    };
  }

  private static Predicate<SampleDto> byRna() {
    return dto -> {
      return dto.getSampleType() != null && dto.getSampleType().contains("RNA");
    };
  }

  private static Predicate<SampleDto> byNotBlood(Map<String, SampleDto> allSamplesById) {
    return dto -> {
      SampleDto tissue =
          (SAMPLE_CATEGORY_TISSUE.equals(getAttribute(ATTR_CATEGORY, dto))
              ? dto
              : getParent(dto, SAMPLE_CATEGORY_TISSUE, allSamplesById));
      if (tissue == null)
        throw new IllegalArgumentException("Cannot find tissue for sample " + dto.getId());
      return !bloodOrigins.contains(getAttribute(ATTR_TISSUE_ORIGIN, tissue));
    };
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return tissuesByType.get("P").size()
        + tissuesByType.get("M").size()
        + tissuesByType.get("n").size()
        + tissuesByType.get("other").size()
        + dnaByType.get("P").size()
        + dnaByType.get("M").size()
        + dnaByType.get("n").size()
        + dnaByType.get("other").size()
        + rnaByType.get("P").size()
        + rnaByType.get("M").size()
        + rnaByType.get("n").size()
        + rnaByType.get("other").size()
        + bloodByType.get("BC").size()
        + bloodByType.get("BC DNA").size()
        + bloodByType.get("Plasma").size()
        + bloodByType.get("ctPlasma").size()
        + (4 * 4 * 2); // headings
  }

  @Override
  protected String[] getRow(int rowNum) {
    String[] row = makeRowsFor("samples", "P", tissuesByType.get("P"), rowNum);
    if (row != null) return row;
    rowNum -= (tissuesByType.get("P").size() + 2);
    row = makeRowsFor("samples", "M", tissuesByType.get("M"), rowNum);
    if (row != null) return row;
    rowNum -= (tissuesByType.get("M").size() + 2);
    row = makeRowsFor("samples", "n", tissuesByType.get("n"), rowNum);
    if (row != null) return row;
    rowNum -= (tissuesByType.get("n").size() + 2);
    row = makeRowsFor("samples", "other", tissuesByType.get("other"), rowNum);
    if (row != null) return row;
    rowNum -= (tissuesByType.get("other").size() + 2);

    row = makeRowsFor("DNA", "P", dnaByType.get("P"), rowNum);
    if (row != null) return row;
    rowNum -= (dnaByType.get("P").size() + 2);
    row = makeRowsFor("DNA", "M", dnaByType.get("M"), rowNum);
    if (row != null) return row;
    rowNum -= (dnaByType.get("M").size() + 2);
    row = makeRowsFor("DNA", "n", dnaByType.get("n"), rowNum);
    if (row != null) return row;
    rowNum -= (dnaByType.get("n").size() + 2);
    row = makeRowsFor("DNA", "other", dnaByType.get("other"), rowNum);
    if (row != null) return row;
    rowNum -= (dnaByType.get("other").size() + 2);

    row = makeRowsFor("RNA", "P", rnaByType.get("P"), rowNum);
    if (row != null) return row;
    rowNum -= (rnaByType.get("P").size() + 2);
    row = makeRowsFor("RNA", "M", rnaByType.get("M"), rowNum);
    if (row != null) return row;
    rowNum -= (rnaByType.get("M").size() + 2);
    row = makeRowsFor("RNA", "n", rnaByType.get("n"), rowNum);
    if (row != null) return row;
    rowNum -= (rnaByType.get("n").size() + 2);
    row = makeRowsFor("RNA", "other", rnaByType.get("other"), rowNum);
    if (row != null) return row;
    rowNum -= (rnaByType.get("other").size() + 2);

    row = makeRowsFor("Blood", "BC", bloodByType.get("BC"), rowNum);
    if (row != null) return row;
    rowNum -= (bloodByType.get("BC").size() + 2);
    row = makeRowsFor("Blood", "BC DNA", bloodByType.get("BC DNA"), rowNum);
    if (row != null) return row;
    rowNum -= (bloodByType.get("BC DNA").size() + 2);
    row = makeRowsFor("Blood", "Plasma", bloodByType.get("Plasma"), rowNum);
    if (row != null) return row;
    rowNum -= (bloodByType.get("Plasma").size() + 2);
    row = makeRowsFor("Blood", "ctPlasma", bloodByType.get("ctPlasma"), rowNum);
    if (row != null) return row;
    rowNum -= (bloodByType.get("ctPlasma").size() + 2);
    return new String[] {"", ""};
  }

  private static String[] makeRowsFor(
      String broadCategory, String key, List<SampleDto> samplesByType, int rowNum) {
    if (rowNum == 0) {
      return makeHeadingRow(String.format("Cases with %s %s", key, broadCategory));
    }
    rowNum -= 1;
    if (rowNum < samplesByType.size()) {
      String sampleName = samplesByType.get(rowNum).getName();
      return new String[] {sampleName.substring(0, 10), sampleName};
    }
    rowNum -= samplesByType.size();
    if (rowNum == 0) {
      return makeBlankRow();
    }
    rowNum -= 1;
    return null;
  }

  private static String[] makeHeadingRow(String heading) {
    String[] row = makeBlankRow();
    row[0] = heading;
    return row;
  }

  private static String[] makeBlankRow() {
    String[] row = new String[COLUMNS.size()];
    for (int i = 0; i < row.length; i++) {
      row[i] = "";
    }
    return row;
  }
}
