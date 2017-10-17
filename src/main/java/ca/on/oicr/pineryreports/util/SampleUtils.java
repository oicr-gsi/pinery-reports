package ca.on.oicr.pineryreports.util;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ca.on.oicr.ws.dto.AttributeDto;
import ca.on.oicr.ws.dto.SampleDto;
import ca.on.oicr.ws.dto.SampleReferenceDto;

import com.google.common.collect.Lists;

public class SampleUtils {

  private SampleUtils() {
    throw new IllegalStateException("Util class not intended for instantiation");
  }
  
  public static Map<String, SampleDto> mapSamplesById(Collection<SampleDto> samples) {
    return samples.stream()
    .collect(Collectors.toMap(SampleDto::getId, dto->dto));
  }
  
  public static List<SampleDto> filter(Collection<SampleDto> samples, Predicate<SampleDto> predicate) {
    return samples.stream()
        .filter(predicate)
        .collect(Collectors.toList());
  }
  
  public static List<SampleDto> filter(Collection<SampleDto> samples, Collection<Predicate<SampleDto>> predicates) {
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
    return dto -> projectName.equals(dto.getProjectName());
  }
  
  public static Predicate<SampleDto> bySampleCategory(String sampleCategory) {
    return dto -> sampleCategory.equals(getAttribute("Sample Category", dto));
  }
  
  public static Predicate<SampleDto> byCreatedBetween(String start, String end) {
    return dto -> (start == null || dto.getCreatedDate().compareTo(start) > 0)
        && (end == null || dto.getCreatedDate().compareTo(end) < 0);
  }
  
  /**
   * Return attribute value that exists for the given attribute name on the given sample.
   * 
   * @param sample
   *          Sample to look for attribute
   * @param attributeName
   *          Name of the attribute we are retrieving values for
   * @return Attribute the attribute value, or an empty String if the attribute is not found
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
   * @return Attribute the attribute value, or an empty String if the attribute is not found
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
  public static Integer getIntAttribute(String attributeName, SampleDto sample, Integer defaultValue) {
    String attr = getAttribute(attributeName, sample);
    return attr == null ? defaultValue : Integer.valueOf(attr);
  }
  
  /**
   * Get an attribute from higher up in the hierarchy. Will never return an attribute found directly on sample
   * 
   * @param attributeName
   * @param sample find the attribute in this sample's hierarchy
   * @param allSamples complete set of potential ancestors to this sample, mapped by ID
   * @return the attribute value from the nearest ancestor which has the attribute, or null if not found
   */
  public static String getUpstreamAttribute(String attributeName, SampleDto sample, Map<String, SampleDto> allSamples) {
    for (SampleDto parent = getParent(sample, allSamples); parent != null; parent = getParent(parent, allSamples)) {
      String value = getAttribute(attributeName, parent);
      if (value != null) return value;
    }
    return null;
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
      throw new IllegalStateException("Parent sample " + parentId + " not found. Possibly in a different project");
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
  public static SampleDto getParent(SampleDto sample, String sampleCategory, Map<String, SampleDto> potentialParents) {
    for (SampleDto current = sample; current != null; current = getParent(current, potentialParents)) {
      if (sampleCategory.equals(getAttribute("Sample Category", current))) {
        return current;
      }
    }
    throw new IllegalStateException("Parent " + sampleCategory + " of sample " + sample.getId()
        + " not found. Possibly in a different project");
  }
  
  /**
   * Get an ancestor of the sample if it exists
   * 
   * @param sample sample to find the parent of
   * @param sampleClass class of parent to find
   * @param potentialParents complete set of potential ancestors to this sample, mapped by ID
   * @return the closest ancestor of sample of the provided sample class, or null if one is not found
   */
  public static SampleDto getOptionalParent(SampleDto sample, String sampleClass, Map<String, SampleDto> potentialParents) {
    for (SampleDto current = sample; current != null; current = getParent(current, potentialParents)) {
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
  
  /**
   * Round to a specified number of decimal places
   * 
   * @param number number to round
   * @param decimalPlaces number of decimal places to round to
   * @return the number as a String to ensure no further mutation occurs. The number will always have the specified number of
   * decimal places, even when unneccessary (e.g. "1.20")
   */
  public static String round(double number, int decimalPlaces) {
    DecimalFormat df = new DecimalFormat();
    df.setMaximumFractionDigits(decimalPlaces);
    df.setMinimumFractionDigits(decimalPlaces);
    df.setGroupingUsed(false);
    return df.format(number);
  }
  
  /**
   * Removes the time portion of a date/time String
   * 
   * @param datetime String in format "YYYY-MM-DD..."
   * @return String in format "YYYY-MM-DD" (anything beyond this is truncated)
   */
  public static String removeTime(String datetime) {
    if (datetime == null) {
      return null;
    }
    Matcher m = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}).*").matcher(datetime);
    if (!m.matches()) {
      throw new IllegalArgumentException("Datetime string is not in expected format (YYYY-MM-DD...)");
    }
    return m.group(1);
  }

}
