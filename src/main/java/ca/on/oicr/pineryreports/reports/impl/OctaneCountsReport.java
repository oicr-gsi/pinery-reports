package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.SampleUtils.*;

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

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.AttributeDto;
import ca.on.oicr.ws.dto.SampleDto;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class OctaneCountsReport extends TableReport {
  
  private static class Count {
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
  }
  
  public static final String REPORT_NAME = "octane";
  
  private static final String OPT_AFTER = "after";
  private static final String OPT_BEFORE = "before";
  
  private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";

  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(Arrays.asList(
      new ColumnDefinition("CASE NUMBERS"),
      new ColumnDefinition("")
  ));
  
  private String start;
  private String end;
  
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
    Set<Option> opts = Sets.newHashSet();
    
    opts.add(Option.builder()
        .longOpt(OPT_AFTER)
        .hasArg()
        .argName("date")
        .desc("Report receipt/creation after this date, inclusive (yyyy-mm-dd)")
        .build());
    opts.add(Option.builder()
        .longOpt(OPT_BEFORE)
        .hasArg()
        .argName("date")
        .desc("Report receipt/creation before this date, exclusive (yyyy-mm-dd)")
        .build());
    
    return opts;
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    if (cmd.hasOption(OPT_AFTER)) {
      String after = cmd.getOptionValue(OPT_AFTER);
      if (!after.matches(DATE_REGEX)) {
        throw new ParseException("After date must be in format yyyy-mm-dd");
      }
      this.start = after;
    }
    
    if (cmd.hasOption(OPT_BEFORE)) {
      String before = cmd.getOptionValue(OPT_BEFORE);
      if (!before.matches(DATE_REGEX)) {
        throw new ParseException("Before date must be in format yyyy-mm-dd");
      }
      this.end = before;
    }
  }

  @Override
  public String getTitle() {
    return "OCTANE Counts "
        + (start == null ? "Any Time" : start)
        + " - "
        + (end == null ? "Now" : end)
        + ", and Inventory "
        + new SimpleDateFormat("yyyy-MM-dd").format(new Date());
  }
  
  private static final String ATTR_CATEGORY = "Sample Category";
  private static final String ATTR_TISSUE_TYPE = "Tissue Type";
  private static final String ATTR_TISSUE_ORIGIN = "Tissue Origin";
  private static final String ATTR_SLIDES = "Slides";
  private static final String ATTR_DISCARDS = "Discards";
  private static final String ATTR_CONSUMED = "Slides Consumed";
  private static final String ATTR_REMAINING = "Remaining";
  private static final String ATTR_STAIN = "Stain";
  
  private static final String SAMPLE_CLASS_SLIDE = "Slide";
  private static final String SAMPLE_CLASS_WHOLE_RNA = "whole RNA";
  
  private static final String SAMPLE_CATEGORY_IDENTITY = "Identity";
  private static final String SAMPLE_CATEGORY_TISSUE = "Tissue";
  private static final String SAMPLE_CATEGORY_STOCK = "Stock";
  private static final String SAMPLE_CATEGORY_ALIQUOT = "Aliquot";

  private static final String STAIN_HE = "Hematoxylin+Eosin";
  
  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> allOctaneSamples = pinery.getSample().allFiltered(new SamplesFilter().withProjects(Lists.newArrayList("OCT")));
    Map<String, SampleDto> allOctaneSamplesById = mapSamplesById(allOctaneSamples);
    
    List<SampleDto> buffyCoats = new ArrayList<>(); // Ly_R tissues
    List<SampleDto> plasma = new ArrayList<>(); // Pl_R tissues
    List<SampleDto> ctDnaPlasma = new ArrayList<>(); // Ct_T tissues
    List<SampleDto> slides = new ArrayList<>(); // Slides
    List<SampleDto> stocksFromBuffy = new ArrayList<>(); // Ly_R stocks
    List<SampleDto> stockDnaFromSlides = new ArrayList<>(); // DNA Stocks with Slide in hierarchy
    List<SampleDto> stockRnaFromSlides = new ArrayList<>(); // RNA Stocks with Slide in hierarchy
    List<SampleDto> dnaAliquots = new ArrayList<>(); // DNA aliquots
    List<SampleDto> dnaAliquotsFromSlides = new ArrayList<>(); // DNA aliquots with Slide in hierarchy
    List<SampleDto> dnaAliquotsFromBuffy = new ArrayList<>(); // DNA aliquots with Ly_R tissue
    List<SampleDto> rnaAliquots = new ArrayList<>(); // RNA aliquots
    List<SampleDto> rnaAliquotsFromSlides = new ArrayList<>(); // RNA aliquots with Slide in hierarchy
    
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
          stocksFromBuffy.add(tissue);
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
          dnaAliquots.add(sam);
          if (getOptionalParent(sam, SAMPLE_CLASS_SLIDE, allOctaneSamplesById) != null) {
            dnaAliquotsFromSlides.add(sam);
          }
          if (tissueOriginAndTypeMatch(getParent(sam, SAMPLE_CATEGORY_TISSUE, allOctaneSamplesById), "Ly", "R")) {
            dnaAliquotsFromBuffy.add(sam);
          }
        } else if (SAMPLE_CLASS_WHOLE_RNA.equals(sam.getSampleType())) {
          rnaAliquots.add(sam);
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
    
    List<SampleDto> unstainedInventory = slides.stream()
        .filter(byStain(null))
        .filter(withSlidesRemaining)
        .collect(Collectors.toList());
    inventoryNumbers.add(new Count("Tumor Tissue Unstained Slides", countSlidesRemaining(unstainedInventory)));
    inventoryNumbers.add(new Count("Tumor Tissue Unstained Slides (Cases)",
        countUniqueIdentities(unstainedInventory, allOctaneSamplesById)));
    
    List<SampleDto> heInventory = slides.stream()
        .filter(byStain(STAIN_HE))
        .filter(withSlidesRemaining)
        .collect(Collectors.toList());
    inventoryNumbers.add(new Count("Tumor Tissue H&E Slides", countSlidesRemaining(heInventory)));
    inventoryNumbers.add(new Count("Tumor Tissue H&E Slides (Cases)", countUniqueIdentities(heInventory, allOctaneSamplesById)));
    
    List<SampleDto> buffyInventory = filterNonEmpty(buffyCoats, allOctaneSamplesById);
    inventoryNumbers.add(new Count("Buffy Coat", buffyInventory.size()));
    inventoryNumbers.add(new Count("Buffy Coat (Cases)", countUniqueIdentities(buffyInventory, allOctaneSamplesById)));
    
    List<SampleDto> plasmaInventory = filterNonEmpty(plasma, allOctaneSamplesById);
    inventoryNumbers.add(new Count("Plasma", plasmaInventory.size()));
    inventoryNumbers.add(new Count("Plasma (Cases)", countUniqueIdentities(plasmaInventory, allOctaneSamplesById)));
    
    List<SampleDto> ctInventory = filterNonEmpty(ctDnaPlasma, allOctaneSamplesById);
    inventoryNumbers.add(new Count("ctDNA Plasma", ctInventory.size()));
    inventoryNumbers.add(new Count("ctDNA Plasma (Cases)", countUniqueIdentities(ctInventory, allOctaneSamplesById)));
    
    List<SampleDto> buffyStockInventory = filterNonEmpty(stocksFromBuffy, allOctaneSamplesById);
    inventoryNumbers.add(new Count("Extracted Buffy Coat", buffyStockInventory.size()));
    inventoryNumbers.add(new Count("Extracted Buffy Coat (Cases)", countUniqueIdentities(buffyStockInventory, allOctaneSamplesById)));
    
    List<SampleDto> stockDnaFromSlideInventory = filterNonEmpty(stockDnaFromSlides, allOctaneSamplesById);
    inventoryNumbers.add(new Count("Tumor Tissue DNA", stockDnaFromSlideInventory.size()));
    inventoryNumbers.add(new Count("Tumor Tissue DNA (Cases)", countUniqueIdentities(stockDnaFromSlideInventory, allOctaneSamplesById)));
    
    List<SampleDto> stockRnaFromSlideInventory = filterNonEmpty(stockRnaFromSlides, allOctaneSamplesById);
    inventoryNumbers.add(new Count("Tumor Tissue DNA", stockRnaFromSlideInventory.size()));
    inventoryNumbers.add(new Count("Tumor Tissue DNA (Cases)", countUniqueIdentities(stockRnaFromSlideInventory, allOctaneSamplesById)));
    
    Map<String, Integer> slideCountsByTissueType = new HashMap<>();
    Map<String, Set<SampleDto>> casesByTissueOrigin = new HashMap<>();
    
    slides.stream()
        .filter(byStain(null))
        .forEach(sam -> {
          String tissueType = getUpstreamAttribute(ATTR_TISSUE_TYPE, sam, allOctaneSamplesById);
          Integer c1 = slideCountsByTissueType.getOrDefault(tissueType, 0);
          slideCountsByTissueType.put(tissueType, c1 + getIntAttribute(ATTR_SLIDES, sam));
          
          String tissueOrigin = getUpstreamAttribute(ATTR_TISSUE_ORIGIN, sam, allOctaneSamplesById);
          Set<SampleDto> cases = casesByTissueOrigin.getOrDefault(tissueOrigin, new HashSet<>());
          cases.add(sam);
          casesByTissueOrigin.put(tissueOrigin, cases);
        });
    
    tissueTypeNumbers = new ArrayList<>();
    slideCountsByTissueType.forEach((k, v) -> tissueTypeNumbers.add(new Count(k, v)));
    tissueOriginNumbers = new ArrayList<>();
    casesByTissueOrigin.forEach((k, v) -> tissueOriginNumbers.add(new Count(k, v.size())));
    
    // Filter down to samples within date range
    buffyCoats = buffyCoats.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    plasma = plasma.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    ctDnaPlasma = ctDnaPlasma.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    slides = slides.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    stocksFromBuffy = stocksFromBuffy.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    stockDnaFromSlides = stockDnaFromSlides.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    dnaAliquots = dnaAliquots.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    dnaAliquotsFromSlides = dnaAliquotsFromSlides.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    dnaAliquotsFromBuffy = dnaAliquotsFromBuffy.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    rnaAliquots = rnaAliquots.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    rnaAliquotsFromSlides = rnaAliquotsFromSlides.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
    
    // Case Numbers
    caseNumbers.add(new Count("Tumor Tissue Rec'd", countUniqueNewIdentities(slides, allOctaneSamplesById)));
    caseNumbers.add(new Count("Tumor Tissue Extracted", countUniqueIdentities(stockDnaFromSlides, allOctaneSamplesById)));
    caseNumbers.add(new Count("Tumor Tissue DNA Transferred", countUniqueIdentities(dnaAliquotsFromSlides, allOctaneSamplesById)));
    caseNumbers.add(new Count("Tumor Tissue RNA Transferred", countUniqueIdentities(rnaAliquotsFromSlides, allOctaneSamplesById)));
    caseNumbers.add(new Count("Buffy Coat Rec'd", countUniqueNewIdentities(buffyCoats, allOctaneSamplesById)));
    caseNumbers.add(new Count("Buffy Coat Extracted", getUniqueIdentities(stocksFromBuffy, allOctaneSamplesById).size()));
    caseNumbers.add(new Count("Buffy Coat Transferred", countUniqueIdentities(dnaAliquotsFromBuffy, allOctaneSamplesById)));
    caseNumbers.add(new Count("Plasma Rec'd", countUniqueNewIdentities(plasma, allOctaneSamplesById)));
    caseNumbers.add(new Count("ctDNA Plasma Rec'd", countUniqueNewIdentities(ctDnaPlasma, allOctaneSamplesById)));
    
    
    // Sample numbers
    sampleNumbers.add(new Count("Tumor Tissue Rec'd", countSlidesReceived(slides)));
    sampleNumbers.add(new Count("Tumor Tissue Extracted", stockDnaFromSlides.size()));
    sampleNumbers.add(new Count("Tumor Tissue DNA Transferred", dnaAliquots.size()));
    sampleNumbers.add(new Count("Tumor Tissue RNA Transferred", rnaAliquots.size()));
    sampleNumbers.add(new Count("Buffy Coat Rec'd", buffyCoats.size()));
    sampleNumbers.add(new Count("Buffy Coat Extracted", stocksFromBuffy.size()));
    sampleNumbers.add(new Count("Plasma Rec'd", plasma.size()));
    sampleNumbers.add(new Count("Plasma Extracted", ctDnaPlasma.size()));
  }
  
  private void addSlidesRemainingCount(SampleDto slide) {
    Integer slides = getIntAttribute(ATTR_SLIDES, slide, 0);
    Integer discards = getIntAttribute(ATTR_DISCARDS, slide, 0);
    AttributeDto attr = new AttributeDto();
    attr.setName(ATTR_REMAINING);
    attr.setValue(Integer.toString(slides - discards));
    slide.getAttributes().add(attr);
  }
  
  private void calculateConsumedSlides(Collection<SampleDto> samples, Map<String, SampleDto> potentialParents) {
    samples.stream()
        .filter(sam -> getAttribute(ATTR_CONSUMED, sam) != null).forEach(sam -> {
          sam.getParents().forEach(ref -> {
            SampleDto parent = potentialParents.get(ref.getId());
            AttributeDto remainingAttr = parent.getAttributes().stream().filter(attr -> ATTR_REMAINING.equals(attr.getName())).findFirst().get();
            Integer remaining = Integer.parseInt(remainingAttr.getValue());
            Integer consumed = Integer.parseInt(getAttribute(ATTR_CONSUMED, sam));
            remainingAttr.setValue(Integer.toString(remaining - consumed));
          });
        });
  }
  
  private int countSlidesRemaining(Collection<SampleDto> samples) {
    return samples.stream()
        .mapToInt(s -> getIntAttribute(ATTR_REMAINING, s))
        .sum();
  }
  
  private int countSlidesReceived(Collection<SampleDto> samples) {
    return samples.stream()
        .mapToInt(s -> getIntAttribute(ATTR_SLIDES, s))
        .sum();
  }
  
  private static Predicate<SampleDto> byStain(String stain) {
    return slide -> {
      return stain == null ? getAttribute(ATTR_STAIN, slide) == null : stain.equals(getAttribute(ATTR_STAIN, slide));
    };
  }
  
  private static List<SampleDto> filterNonEmpty(Collection<SampleDto> samples, Map<String, SampleDto> potentialParents) {
    return samples.stream()
        .filter(sample -> !"EMPTY".equals(sample.getStorageLocation()))
        .collect(Collectors.toList());
  }
  
  private final Predicate<SampleDto> withSlidesRemaining = slide -> {
    Integer slides = getIntAttribute(ATTR_SLIDES, slide);
    Integer discards = getIntAttribute(ATTR_DISCARDS, slide);
    if (slides == null) {
      throw new IllegalArgumentException("Sample does not seem to be a slide");
    }
    if (discards == null) {
      return slides > 0;
    }
    return slides > discards;
  };
  
  private long countUniqueNewIdentities(Collection<SampleDto> samples, Map<String, SampleDto> potentialParents) {
    return getUniqueIdentities(samples, potentialParents).stream()
        .filter(byCreatedBetween(start, end))
        .count();
  }
  
  private List<SampleDto> getUniqueIdentities(Collection<SampleDto> samples, Map<String, SampleDto> potentialParents) {
    return samples.stream()
        .map(sam -> getParent(sam, SAMPLE_CATEGORY_IDENTITY, potentialParents))
        .distinct()
        .collect(Collectors.toList());
  }
  
  private int countUniqueIdentities(Collection<SampleDto> samples, Map<String, SampleDto> potentialParents) {
    return getUniqueIdentities(samples, potentialParents).size();
  }
  
  private static boolean tissueOriginAndTypeMatch(SampleDto sample, String tissueOrigin, String tissueType) {
    String origin = getAttribute(ATTR_TISSUE_ORIGIN, sample);
    String type = getAttribute(ATTR_TISSUE_TYPE, sample);
    if (origin == null || type == null) {
      throw new IllegalArgumentException("sample is missing tissue origin or tissue type");
    }
    return tissueOrigin.equals(origin) && tissueType.equals(type);
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return caseNumbers.size()
        + 2 + sampleNumbers.size()
        + 2 + inventoryNumbers.size()
        + 2 + tissueTypeNumbers.size()
        + 2 + tissueOriginNumbers.size();
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
      return makeHeadingRow("SAMPLE NUMBERS");
    }
    rowNum -= 2;
    if (rowNum < sampleNumbers.size()) {
      return makeCountRow(sampleNumbers.get(rowNum));
    }
    rowNum -= sampleNumbers.size();
    
    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow("INVENTORY NUMBERS");
    }
    rowNum -= 2;
    if (rowNum < inventoryNumbers.size()) {
      return makeCountRow(inventoryNumbers.get(rowNum));
    }
    rowNum -= inventoryNumbers.size();
    
    if (rowNum == 0) {
      return makeBlankRow();
    } else if (rowNum == 1) {
      return makeHeadingRow("UNSTAINED SLIDES BY TISSUE TYPE");
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
    return new String[] { count.getKey(), Long.toString(count.getValue()) };
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
