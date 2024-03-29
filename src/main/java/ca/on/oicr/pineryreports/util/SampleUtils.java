package ca.on.oicr.pineryreports.util;

import ca.on.oicr.ws.dto.AttributeDto;
import ca.on.oicr.ws.dto.SampleDto;
import ca.on.oicr.ws.dto.SampleReferenceDto;
import com.google.common.collect.Lists;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SampleUtils {

  private SampleUtils() {
    throw new IllegalStateException("Util class not intended for instantiation");
  }

  public static final String ATTR_CATEGORY = "Sample Category";
  public static final String ATTR_CUSTODY = "Custody";
  public static final String ATTR_EXTERNAL_NAME = "External Name";
  public static final String ATTR_TISSUE_TYPE = "Tissue Type";
  public static final String ATTR_TISSUE_ORIGIN = "Tissue Origin";
  public static final String ATTR_INSTITUTE = "Institute";
  public static final String ATTR_TUBE_ID = "Tube ID";
  public static final String ATTR_SEX = "Sex";
  public static final String ATTR_SLIDES = "Slides";
  public static final String ATTR_INITIAL_SLIDES = "Initial Slides";
  public static final String ATTR_CONSUMED = "Slides Consumed";
  public static final String ATTR_STAIN = "Stain";
  public static final String ATTR_RECEIVE_DATE = "Receive Date";
  public static final String ATTR_CREATION_DATE = "In-lab Creation Date";
  public static final String ATTR_SOURCE_TEMPLATE_TYPE = "Source Template Type";
  public static final String ATTR_GROUP_ID = "Group ID";
  public static final String ATTR_SUBPROJECT = "Sub-project";
  public static final String ATTR_SYNTHETIC = "Synthetic";
  public static final String ATTR_DISTRIBUTED = "Distributed";
  public static final String ATTR_DISTRIBUTION_DATE = "Distribution Date";
  public static final String ATTR_INITIAL_VOLUME = "Initial Volume";
  public static final String ATTR_LATEST_TRANSFER_REQUEST = "Latest Transfer Request";

  public static final String SAMPLE_CLASS_SLIDE = "Slide";
  public static final String SAMPLE_CLASS_WHOLE_RNA = "whole RNA";

  public static final String SAMPLE_CATEGORY_IDENTITY = "Identity";
  public static final String SAMPLE_CATEGORY_TISSUE = "Tissue";
  public static final String SAMPLE_CATEGORY_TISSUE_PROCESSING = "Tissue Processing";
  public static final String SAMPLE_CATEGORY_STOCK = "Stock";
  public static final String SAMPLE_CATEGORY_ALIQUOT = "Aliquot";
  public static final String LIBRARY = "Library";

  public static final String DNA = "DNA";
  public static final String RNA = "RNA";
  public static final String STAIN_HE = "Hematoxylin+Eosin";
  // All DNA library design codes
  public static final String LIBRARY_DESIGN_WG = "WG";
  public static final String LIBRARY_DESIGN_EX = "EX";
  public static final String LIBRARY_DESIGN_TS = "TS";
  // Lesser-used DNA library design codes
  public static final String LIBRARY_DESIGN_CH = "CH";
  public static final String LIBRARY_DESIGN_AS = "AS";
  public static final String LIBRARY_DESIGN_BS = "BS";
  public static final String LIBRARY_DESIGN_SC = "SC";
  public static final String LIBRARY_DESIGN_CT = "CT";
  public static final String LIBRARY_DESIGN_CM = "CM";
  // RNA Library design codes
  public static final String LIBRARY_DESIGN_MR = "MR";
  public static final String LIBRARY_DESIGN_SM = "SM";
  public static final String LIBRARY_DESIGN_WT = "WT";
  public static final String LIBRARY_DESIGN_TR = "TR";
  public static final List<String> RNA_LIBRARY_DESIGN_CODES =
      Collections.unmodifiableList(
          Arrays.asList(
              LIBRARY_DESIGN_MR, LIBRARY_DESIGN_SM, LIBRARY_DESIGN_WT, LIBRARY_DESIGN_TR));
  // Unknown library design code
  public static final String LIBRARY_DESIGN_NN = "NN";

  public static final String NEXTSEQ = "NextSeq 550";
  public static final String NOVASEQ = "Illumina NovaSeq 6000";

  public static Map<String, SampleDto> mapSamplesById(Collection<SampleDto> samples) {
    return samples.stream().collect(Collectors.toMap(SampleDto::getId, dto -> dto));
  }

  public static List<SampleDto> filter(
      Collection<SampleDto> samples, Predicate<SampleDto> predicate) {
    return samples.stream().filter(predicate).collect(Collectors.toList());
  }

  public static List<SampleDto> filter(
      Collection<SampleDto> samples, Collection<Predicate<SampleDto>> predicates) {
    if (predicates == null || predicates.isEmpty()) {
      return Lists.newArrayList(samples);
    } else {
      Iterator<Predicate<SampleDto>> iterator = predicates.iterator();
      Predicate<SampleDto> combined = iterator.next();
      while (iterator.hasNext()) {
        combined = combined.and(iterator.next());
      }
      return filter(samples, combined);
    }
  }

  public static Predicate<SampleDto> byProject(String projectName) {
    Set<String> projectNames = new HashSet<>(Arrays.asList(projectName));
    return byProjects(projectNames);
  }

  public static Predicate<SampleDto> byProjects(Set<String> projectNames) {
    return dto -> projectNames.contains(dto.getProjectName());
  }

  public static Predicate<SampleDto> bySampleCategory(String sampleCategory) {
    return dto -> sampleCategory.equals(getAttribute(ATTR_CATEGORY, dto));
  }

  public static Predicate<SampleDto> byTissueOriginAndType(
      String origin, String type, Map<String, SampleDto> potentialParents) {
    return byHierarchyAttribute(ATTR_TISSUE_ORIGIN, origin, potentialParents)
        .and(byHierarchyAttribute(ATTR_TISSUE_TYPE, type, potentialParents));
  }

  public static Predicate<SampleDto> byHierarchyAttribute(
      String attribute, String value, Map<String, SampleDto> potentialParents) {
    return dto ->
        value.equals(getAttribute(attribute, dto))
            || value.equals(getUpstreamAttribute(attribute, dto, potentialParents));
  }

  public static Predicate<SampleDto> byEmpty(boolean empty) {
    return dto -> "EMPTY".equals(dto.getStorageLocation()) == empty;
  }

  public static Predicate<SampleDto> byCreatedBetween(String start, String end) {
    return dto -> isCreatedBetween(dto, start, end);
  }

  public static boolean isCreatedBetween(SampleDto dto, String start, String end) {
    return (start == null || dto.getCreatedDate().compareTo(start) > 0)
        && (end == null || dto.getCreatedDate().compareTo(end) < 0);
  }

  public static Predicate<SampleDto> byCreator(List<Integer> userIds) {
    if (userIds.isEmpty()) return dto -> true;
    return dto -> userIds.contains(dto.getCreatedById());
  }

  public static Predicate<SampleDto> byReceivedBetween(String start, String end) {
    return dto -> {
      String received = getAttribute(ATTR_RECEIVE_DATE, dto);
      return received != null
          && (start == null || received.compareTo(start) > 0)
          && (end == null || received.compareTo(end) < 0);
    };
  }

  public static Predicate<SampleDto> byReceived() {
    return dto -> getAttribute(ATTR_RECEIVE_DATE, dto) != null;
  }

  public static Predicate<SampleDto> byPropagated() {
    return dto -> getAttribute(ATTR_RECEIVE_DATE, dto) == null;
  }

  public static Predicate<SampleDto> byDistributed() {
    return byDistributedBetween(null, null);
  }

  public static Predicate<SampleDto> byDistributedBetween(String startDate, String endDate) {
    return dto -> {
      String distributionDate = getAttribute(ATTR_DISTRIBUTION_DATE, dto);
      return distributionDate != null
          && (startDate == null || distributionDate.compareTo(startDate) >= 0)
          && (endDate == null || distributionDate.compareTo(endDate) < 0);
    };
  }

  public static Predicate<SampleDto> byDnaLibrary() {
    return byRnaLibrary().negate();
  }

  public static Predicate<SampleDto> byRnaLibrary() {
    return dto -> {
      String designCode = getAttribute(ATTR_SOURCE_TEMPLATE_TYPE, dto);
      if (designCode == null)
        throw new IllegalArgumentException(
            "Library is missing a library design code; is " + dto.getId() + " really a library?");
      return RNA_LIBRARY_DESIGN_CODES.contains(designCode);
    };
  }

  public static boolean isNonIlluminaLibrary(SampleDto dilution) {
    if (dilution.getSampleType() == null)
      throw new IllegalArgumentException("Dilution " + dilution.getName() + " has no sample_type");
    return !dilution.getSampleType().contains("Illumina"); // note the negation here
  }

  public static Predicate<SampleDto> byNonIlluminaLibrary() {
    return SampleUtils::isNonIlluminaLibrary;
  }

  public static final Predicate<SampleDto> withSlidesRemaining =
      slide -> {
        Integer slides = getIntAttribute(ATTR_SLIDES, slide);
        if (slides == null) {
          throw new IllegalArgumentException("Sample does not seem to be a slide");
        }
        return slides > 0;
      };

  public static List<SampleDto> filterNonEmpty(Collection<SampleDto> samples) {
    return samples.stream()
        .filter(sample -> !"EMPTY".equals(sample.getStorageLocation()))
        .collect(Collectors.toList());
  }

  /**
   * Return attribute value that exists for the given attribute name on the given sample.
   *
   * @param sample Sample to look for attribute
   * @param attributeName Name of the attribute we are retrieving values for
   * @return Attribute the attribute value, or null if the attribute is not found
   */
  public static String getAttribute(String attributeName, SampleDto sample) {
    if (sample.getAttributes() != null) {
      for (AttributeDto attributeDto : sample.getAttributes()) {
        if (attributeDto.getName().equalsIgnoreCase(attributeName)) {
          return attributeDto.getValue();
        }
      }
    }
    return null;
  }

  /**
   * Return attribute value that exists for the given attribute name on the given sample.
   *
   * @param sample Sample to look for attribute
   * @param attributeName Name of the attribute we are retrieving values for
   * @return Attribute the attribute value, or null if the attribute is not found
   */
  public static Integer getIntAttribute(String attributeName, SampleDto sample) {
    return getIntAttribute(attributeName, sample, null);
  }

  /**
   * Return attribute value that exists for the given attribute name on the given sample.
   *
   * @param sample Sample to look for attribute
   * @param attributeName Name of the attribute we are retrieving values for
   * @param defaultValue value to return if the attribute is not found
   * @return Attribute the attribute value, or defaultValue if the attribute is not found
   */
  public static Integer getIntAttribute(
      String attributeName, SampleDto sample, Integer defaultValue) {
    String attr = getAttribute(attributeName, sample);
    return attr == null ? defaultValue : Integer.valueOf(attr);
  }

  public static Float getFloatAttribute(String attributeName, SampleDto sample) {
    return getFloatAttribute(attributeName, sample, null);
  }

  public static Float getFloatAttribute(
      String attributeName, SampleDto sample, Float defaultValue) {
    String attr = getAttribute(attributeName, sample);
    return attr == null ? defaultValue : Float.valueOf(attr);
  }

  /**
   * Get an attribute from higher up in the hierarchy. Will never return an attribute found directly
   * on sample
   *
   * @param attributeName
   * @param sample find the attribute in this sample's hierarchy
   * @param allSamples complete set of potential ancestors to this sample, mapped by ID
   * @return the attribute value from the nearest ancestor which has the attribute, or null if not
   *     found
   */
  public static String getUpstreamAttribute(
      String attributeName, SampleDto sample, Map<String, SampleDto> allSamples) {
    for (SampleDto parent = getParent(sample, allSamples);
        parent != null;
        parent = getParent(parent, allSamples)) {
      String value = getAttribute(attributeName, parent);
      if (value != null) return value;
    }
    return null;
  }

  public static String getUpstreamField(
      Function<SampleDto, String> getField, SampleDto sample, Map<String, SampleDto> allSamples) {
    for (SampleDto parent = getParent(sample, allSamples);
        parent != null;
        parent = getParent(parent, allSamples)) {
      String value = getField.apply(parent);
      if (value != null) return value;
    }
    return null;
  }

  /**
   * Get an attribute from lower in the hierarchy. Will never return an attribute found directly on
   * sample.
   *
   * @param attributeName
   * @param sample find the attribute in this sample's children
   * @param possibleChildren set of potential children to this sample
   * @return the attribute value set from the children which have the attribute, or empty set if not
   *     found
   */
  public static Set<String> getChildAttributes(
      String attributeName, SampleDto sample, List<SampleDto> possibleChildren) {
    Set<String> foundAttributes = new HashSet<>();
    for (SampleDto child : possibleChildren) {
      if (getParentId(child) == null || !getParentId(child).equals(sample.getId())) continue;
      String childAttribute = getAttribute(attributeName, child);
      if (childAttribute != null) {
        foundAttributes.add(childAttribute);
      }
    }
    return foundAttributes;
  }

  /**
   * Get the direct parent of sample
   *
   * @param sample sample to find the parent of
   * @param potentialParents complete set of potential ancestors to this sample, mapped by ID
   * @return the sample's parent, or null if it has no parent
   */
  public static SampleDto getParent(SampleDto sample, Map<String, SampleDto> potentialParents) {
    String parentId = getParentId(sample);
    if (parentId == null) {
      return null;
    }
    SampleDto parent = potentialParents.get(parentId);
    if (parent == null) {
      throw new IllegalStateException(
          "Parent sample " + parentId + " not found. Possibly in a different project");
    }
    return parent;
  }

  /**
   * Get an ancestor of the sample
   *
   * @param sample sample to find the parent of
   * @param sampleCategory category of parent to find
   * @param potentialParents complete set of potential ancestors to this sample, mapped by ID
   * @return the closest ancestor of sample of the provided sample category
   */
  public static SampleDto getParent(
      SampleDto sample, String sampleCategory, Map<String, SampleDto> potentialParents) {
    if (sample == null) {
      throw new IllegalArgumentException("Sample cannot be null");
    }
    for (SampleDto current = sample;
        current != null;
        current = getParent(current, potentialParents)) {
      if (sampleCategory.equals(getAttribute(ATTR_CATEGORY, current))) {
        return current;
      }
    }
    throw new IllegalStateException(
        "Parent "
            + sampleCategory
            + " of sample "
            + sample.getId()
            + " not found. Possibly in a different project");
  }

  /**
   * Get an ancestor of the sample if it exists
   *
   * @param sample sample to find the parent of
   * @param sampleClass class of parent to find
   * @param potentialParents complete set of potential ancestors to this sample, mapped by ID
   * @return the closest ancestor of sample of the provided sample class, or null if one is not
   *     found
   */
  public static SampleDto getOptionalParent(
      SampleDto sample, String sampleClass, Map<String, SampleDto> potentialParents) {
    for (SampleDto current = sample;
        current != null;
        current = getParent(current, potentialParents)) {
      if (sampleClass.equals(current.getSampleType())) {
        return current;
      }
    }
    return null;
  }

  private static String getParentId(SampleDto sample) {
    Set<SampleReferenceDto> parents = sample.getParents();
    if (parents == null) {
      return null;
    }
    switch (parents.size()) {
      case 0:
        return null;
      case 1:
        return parents.iterator().next().getId();
      default:
        throw new IllegalStateException("Sample " + sample.getId() + " has more than one parent");
    }
  }

  public static List<SampleDto> getDescendants(SampleDto sample, Map<String, SampleDto> samplesById) {
    List<SampleDto> descendants = new ArrayList<>();
    getDescendants(sample, samplesById, descendants);
    return descendants;
  }

  private static void getDescendants(SampleDto sample, Map<String, SampleDto> samplesById, List<SampleDto> descendants) {
    if (sample.getChildren() != null) {
      for (SampleReferenceDto child : sample.getChildren()) {
        SampleDto descendant = samplesById.get(child.getId());
        descendants.add(descendant);
        getDescendants(descendant, samplesById, descendants);
      }
    }
  }

  /**
   * Round to a specified number of decimal places
   *
   * @param number number to round
   * @param decimalPlaces number of decimal places to round to
   * @return the number as a String to ensure no further mutation occurs. The number will always
   *     have the specified number of decimal places, even when unneccessary (e.g. "1.20")
   */
  public static String round(double number, int decimalPlaces) {
    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(decimalPlaces);
    df.setMinimumFractionDigits(decimalPlaces);
    df.setGroupingUsed(false);
    return df.format(number);
  }

  /**
   * Determine if a library or dilution is an RNA library/dilution, based on whether the parent
   * sample is RNA or is cDNA (derived from RNA)
   *
   * @param library library or dilution to test for RNA-ness
   * @param potentialParents potential parents of the library
   * @return a boolean indicating whether the parent aliquot is RNA or cDNA
   */
  public static boolean isRnaLibrary(SampleDto library, Map<String, SampleDto> potentialParents) {
    if (library == null) {
      throw new IllegalArgumentException("Library cannot be null");
    }
    if ("Unknown".equals(library.getSampleType())) {
      // probably a PacBio library; assume DNA until we're told otherwise
      return false;
    }
    if (!library.getSampleType().contains(LIBRARY)) {
      throw new IllegalArgumentException(
          "Provided sample " + library.getName() + " is not a library");
    }
    for (SampleDto current = library;
        current != null;
        current = getParent(current, potentialParents)) {
      if (current.getSampleType().contains(LIBRARY) && current.getSampleType().contains("Seq")) {
        continue;
      }
      String libraryDesignCode = getAttribute(ATTR_SOURCE_TEMPLATE_TYPE, current);
      return libraryDesignCode != null && RNA_LIBRARY_DESIGN_CODES.contains(libraryDesignCode);
    }
    return false;
  }

  /**
   * Determine if a library was created using a 10X kit (assumes all 10X kits have "10X" in the kit
   * name)
   *
   * @param library an actual library (will return false for all dilutions)
   * @return a boolean indicating if the library was created using a 10X kit
   */
  public static boolean is10XLibrary(SampleDto library, Map<String, SampleDto> potentialParents) {
    if (library == null) {
      throw new IllegalArgumentException("Library cannot be null");
    }
    if ("Unknown".equals(library.getSampleType())) {
      // probably a PacBio library, so not 10X
      return false;
    }
    if (library.getSampleType().contains(" Seq")) {
      // is a dilution; get parent library
      for (SampleDto current = library;
          current != null;
          current = getParent(current, potentialParents)) {
        if (current.getSampleType().contains(LIBRARY)) {
          library = current; // reassign so we check the prep kit of the actual library
          break;
        }
      }
    }
    if (library.getPreparationKit() == null || library.getPreparationKit().getName() == null)
      return false; // old libraries
    return library.getPreparationKit().getName().contains("10X");
  }

  public static Integer getTimesReceived(String sampleName) {
    Matcher m =
        Pattern.compile(NAME_SEGMENT_IDENTITY + "_[A-zn][a-z]_[A-Zn]_[0-9n]{1,2}_(\\d+)-\\d+.*$")
            .matcher(sampleName);
    if (!m.matches() || m.group(1) == null)
      throw new IllegalArgumentException("Sample with alias " + sampleName + " is malformed.");
    return Integer.valueOf(m.group(1));
  }

  public static Integer getTubeNumber(String sampleName) {
    Matcher m =
        Pattern.compile(NAME_SEGMENT_IDENTITY + "_[A-zn][a-z]_[A-Zn]_[0-9n]{1,2}_\\d+-(\\d+).*$")
            .matcher(sampleName);
    if (!m.matches() || m.group(1) == null)
      throw new IllegalArgumentException("Sample with alias " + sampleName + " is malformed.");
    return Integer.valueOf(m.group(1));
  }

  private static final String NAME_SEGMENT_IDENTITY = "^[A-Z0-9]{3,5}_\\d+";
}
