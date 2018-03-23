package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Sets;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.RunDtoPosition;
import ca.on.oicr.ws.dto.RunDtoSample;
import ca.on.oicr.ws.dto.SampleDto;

public class BisqueProjectsStatusReport extends TableReport {

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
      List<String> countKeys = cols;
      int compare = Integer.valueOf(countKeys.indexOf(key)).compareTo(countKeys.indexOf(o.key));
      return compare;
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
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Count other = (Count) obj;
      if (key == null) {
        if (other.key != null)
          return false;
      } else if (!key.equals(other.key))
        return false;
      return value == other.value;
    }
  }

  public static final String SAMPLE_TISSUE_PRIMARY_RECEIVED = "Primary Tumor Tissue Received";
  public static final String SAMPLE_TISSUE_REFERENCE_RECEIVED = "Reference Tissue Received";
  public static final String SAMPLE_TISSUE_METASTATIC_RECEIVED = "Metastatic Tumor Tissue Received";
  public static final String SAMPLE_TISSUE_MISC_RECEIVED = "Misc. Tissue Received";
  public static final String SAMPLE_TISSUE_XENOGRAFT_RECEIVED = "Xenograft Tissue Received";
  public static final String SAMPLE_TISSUE_CELLS_RECEIVED = "Cell Line Received";
  public static final String SAMPLE_DNA_RECEIVED = "DNA Analyte Received";
  public static final String SAMPLE_RNA_RECEIVED = "RNA Analyte Received";
  public static final String SAMPLE_DNA_STOCK_PREPPED = "DNA Stock Prepped";
  public static final String SAMPLE_RNA_STOCK_PREPPED = "RNA Stock Prepped";
  public static final String SAMPLE_DNA_ALIQUOT_PREPPED = "DNA Aliquot Prepped";
  public static final String SAMPLE_RNA_ALIQUOT_PREPPED = "RNA Aliquot Prepped";
  public static final String SAMPLE_STOCK_UNRECEIVED = "Stock Unreceived";
  public static final String LIBRARY_DNA_RECEIVED = "DNA Library Received";
  public static final String LIBRARY_RNA_RECEIVED = "RNA Library Received";
  public static final String LIBRARY_WG_PREPPED = "WG Library Prepped";
  public static final String LIBRARY_EX_PREPPED = "EX Library Prepped";
  public static final String LIBRARY_TS_PREPPED = "TS Library Prepped";
  public static final String LIBRARY_DNA_PREPPED = "AS/CH/BS Library Prepped";
  public static final String LIBRARY_RNA_PREPPED = "RNA Library Prepped";
  public static final String LIBRARY_NN_PREPPED = "NN Library Prepped";
  public static final String LIBRARY_DNA_SEQUENCED_COMPLETED = "DNA Sequenced Completed";
  public static final String LIBRARY_RNA_SEQUENCED_COMPLETED = "RNA Sequenced Completed";
  public static final String LIBRARY_10X_SEQUENCED_COMPLETED = "10X Sequenced Completed";
  public static final String LIBRARY_DNA_SEQUENCED_FAILED = "DNA Sequenced Failed";
  public static final String LIBRARY_RNA_SEQUENCED_FAILED = "RNA Sequenced Failed";
  public static final String LIBRARY_DNA_SEQUENCED_REPEATED = "DNA Sequenced Repeated";
  public static final String LIBRARY_RNA_SEQUENCED_REPEATED = "RNA Sequenced Repeated";

  public static final List<String> cols = Arrays.asList(
      SAMPLE_TISSUE_PRIMARY_RECEIVED,
      SAMPLE_TISSUE_REFERENCE_RECEIVED,
      SAMPLE_TISSUE_METASTATIC_RECEIVED,
      SAMPLE_TISSUE_MISC_RECEIVED,
      SAMPLE_TISSUE_XENOGRAFT_RECEIVED,
      SAMPLE_TISSUE_CELLS_RECEIVED,
      SAMPLE_DNA_RECEIVED,
      SAMPLE_RNA_RECEIVED,
      SAMPLE_DNA_STOCK_PREPPED,
      SAMPLE_RNA_STOCK_PREPPED,
      SAMPLE_DNA_ALIQUOT_PREPPED,
      SAMPLE_RNA_ALIQUOT_PREPPED,
      SAMPLE_STOCK_UNRECEIVED,
      LIBRARY_WG_PREPPED,
      LIBRARY_EX_PREPPED,
      LIBRARY_TS_PREPPED,
      LIBRARY_DNA_PREPPED,
      LIBRARY_RNA_PREPPED,
      LIBRARY_NN_PREPPED,
      LIBRARY_DNA_RECEIVED,
      LIBRARY_RNA_RECEIVED,
      LIBRARY_DNA_SEQUENCED_COMPLETED,
      LIBRARY_RNA_SEQUENCED_COMPLETED,
      LIBRARY_10X_SEQUENCED_COMPLETED,
      LIBRARY_DNA_SEQUENCED_FAILED,
      LIBRARY_RNA_SEQUENCED_FAILED,
      LIBRARY_DNA_SEQUENCED_REPEATED,
      LIBRARY_RNA_SEQUENCED_REPEATED);

  public static final String REPORT_NAME = "projects-status";

  private static final List<ColumnDefinition> COLUMNS = Stream
      .concat(
          Arrays.asList(new ColumnDefinition("Project")).stream(),
          cols.stream().map(col -> new ColumnDefinition(col)))
      .collect(Collectors.toList());


  private List<Map.Entry<String, List<Count>>> countsByProjectAsList; // String project, List<Count> all counts

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
    // Do nothing
  }

  @Override
  public String getTitle() {
    return "Projects Status Report generated " + new SimpleDateFormat(DATE_FORMAT).format(new Date());
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> allSamples = pinery.getSample().all();
    Map<String, SampleDto> allSamplesById = mapSamplesById(allSamples);
    List<RunDto> allRuns = pinery.getSequencerRun().all();

    Map<String, List<Count>> countsByProject = new TreeMap<>(); // String project, List<Count> all counts

    List<SampleDto> realSamples = new ArrayList<>(); // no identities
    List<SampleDto> libraries = new ArrayList<>();
    Set<String> projects = new HashSet<>();
    List<SampleDto> receivedPrimaryTissueSamples;
    List<SampleDto> receivedReferenceTissueSamples;
    List<SampleDto> receivedMetastaticTissueSamples;
    List<SampleDto> receivedXenoTissueSamples;
    List<SampleDto> receivedCellTissueSamples;
    List<SampleDto> receivedMiscTissueSamples;
    List<SampleDto> receivedDnaAnalyte;
    List<SampleDto> receivedRnaAnalyte;
    List<SampleDto> receivedDnaLibraries;
    List<SampleDto> receivedRnaLibraries;
    List<SampleDto> unreceivedAliquots;

    List<SampleDto> preppedDnaStockSamples;
    List<SampleDto> preppedRnaStockSamples;
    List<SampleDto> preppedDnaAliquotSamples;
    List<SampleDto> preppedRnaAliquotSamples;
    List<SampleDto> preppedWgLibraries;
    List<SampleDto> preppedExLibraries;
    List<SampleDto> preppedTsLibraries;
    List<SampleDto> preppedDnaLibraries;
    List<SampleDto> preppedRnaLibraries;
    List<SampleDto> preppedNnLibraries;

    Set<SampleDto> sequencedCompletedDnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompleted10XLibraries = new HashSet<>();
    Set<SampleDto> sequencedFailedDnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedFailedRnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRnaLibraries = new HashSet<>();

    for (SampleDto sam : allSamples) {
      projects.add(sam.getProjectName());
      String category = getAttribute(ATTR_CATEGORY, sam);
      if (category == null) {
        // don't need dilutions
        if (sam.getSampleType() != null && sam.getSampleType().contains(" Seq")) continue;
        libraries.add(sam);
        continue;
      }
      // don't care about identities, as they aren't received or prepped
      if (SAMPLE_CATEGORY_IDENTITY.equals(category)) continue;
      realSamples.add(sam);
    }

    receivedPrimaryTissueSamples = 
        filter(realSamples, Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byTissueLike(),
            byTissueTypes(Arrays.asList("P", "S", "O", "A"), allSamplesById)));
    receivedReferenceTissueSamples = filter(realSamples,
        Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byTissueLike(), byTissueTypes(Arrays.asList("R"), allSamplesById)));
    receivedMetastaticTissueSamples = filter(realSamples,
        Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byTissueLike(), byTissueTypes(Arrays.asList("M"), allSamplesById)));
    receivedXenoTissueSamples = filter(realSamples,
        Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byTissueLike(), byTissueTypes(Arrays.asList("X"), allSamplesById)));
    receivedCellTissueSamples = filter(realSamples,
        Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byTissueLike(), byTissueTypes(Arrays.asList("C"), allSamplesById)));
    receivedMiscTissueSamples = filter(realSamples,
        Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byTissueLike(),
            byTissueTypes(Arrays.asList("U", "n", "E", "T"), allSamplesById)));
    receivedDnaAnalyte = filter(realSamples,
        Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byAnalyte(), byAnalyteType(DNA)));
    receivedRnaAnalyte = filter(realSamples,
        Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byAnalyte(), byAnalyteType(RNA)));
    receivedDnaLibraries = filter(libraries, Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byDnaLibrary()));
    receivedRnaLibraries = filter(libraries, Arrays.asList(byReceivedWithUnreceivedParents(allSamplesById), byRnaLibrary()));
    unreceivedAliquots = filter(realSamples,
        Arrays.asList(bySampleCategory(SAMPLE_CATEGORY_STOCK), byUnreceivedHierarchy(allSamplesById)));

    preppedDnaStockSamples = filter(realSamples,
        Arrays.asList(byPropagated(), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    preppedRnaStockSamples = filter(realSamples,
        Arrays.asList(byPropagated(), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    preppedDnaAliquotSamples = filter(realSamples,
        Arrays.asList(byPropagated(), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    preppedRnaAliquotSamples = filter(realSamples,
        Arrays.asList(byPropagated(), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    preppedWgLibraries = filter(libraries, Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG))));
    preppedExLibraries = filter(libraries, Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX))));
    preppedTsLibraries = filter(libraries, Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS))));
    preppedDnaLibraries = filter(libraries,
        Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS))));
    preppedRnaLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary()));
    preppedNnLibraries = filter(libraries, Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN))));
    
    // track which libraries where sequenced
    for (RunDto run : allRuns) {
      // ignore Running, Unknown, Stopped runs
      if (!RUN_FAILED.equals(run.getState()) && !RUN_COMPLETED.equals(run.getState())) continue;
      if (run.getPositions() == null) continue;
      for (RunDtoPosition lane : run.getPositions()) {
        if (lane.getSamples() == null) continue;
        for (RunDtoSample sam : lane.getSamples()) {
          SampleDto dilution = allSamplesById.get(sam.getId());
          SampleDto library = getParent(dilution, allSamplesById);
          if (is10XLibrary(library)) {
            sequencedCompleted10XLibraries.add(library);
          } else if (isRnaLibrary(dilution, allSamplesById)) {
            if (RUN_FAILED.equals(run.getState())) sequencedFailedRnaLibraries.add(library);
            if (sequencedCompletedRnaLibraries.contains(library)) {
              sequencedRepeatedRnaLibraries.add(library);
            } else {
              sequencedCompletedRnaLibraries.add(library);
            }
          } else {
            if (RUN_FAILED.equals(run.getState())) sequencedFailedDnaLibraries.add(library);
            if (sequencedCompletedDnaLibraries.contains(library)) {
              sequencedRepeatedDnaLibraries.add(library);
            } else {
              sequencedCompletedDnaLibraries.add(library);
            }
          }
        }
      }
    }

    for (String project : projects) {
      Count recdP = new Count(SAMPLE_TISSUE_PRIMARY_RECEIVED, filter(receivedPrimaryTissueSamples, byProject(project)).size());
      Count recdR = new Count(SAMPLE_TISSUE_REFERENCE_RECEIVED, filter(receivedReferenceTissueSamples, byProject(project)).size());
      Count recdM = new Count(SAMPLE_TISSUE_METASTATIC_RECEIVED, filter(receivedMetastaticTissueSamples, byProject(project)).size());
      Count recdU = new Count(SAMPLE_TISSUE_MISC_RECEIVED, filter(receivedMiscTissueSamples, byProject(project)).size());
      Count recdX = new Count(SAMPLE_TISSUE_XENOGRAFT_RECEIVED, filter(receivedXenoTissueSamples, byProject(project)).size());
      Count recdC = new Count(SAMPLE_TISSUE_CELLS_RECEIVED, filter(receivedCellTissueSamples, byProject(project)).size());
      Count recdDAn = new Count(SAMPLE_DNA_RECEIVED, filter(receivedDnaAnalyte, byProject(project)).size());
      Count recdRAn = new Count(SAMPLE_RNA_RECEIVED, filter(receivedRnaAnalyte, byProject(project)).size());
      Count unrecd = new Count(SAMPLE_STOCK_UNRECEIVED, filter(unreceivedAliquots, byProject(project)).size());

      Count prepdDSt = new Count(SAMPLE_DNA_STOCK_PREPPED, filter(preppedDnaStockSamples, byProject(project)).size());
      Count prepdRSt = new Count(SAMPLE_RNA_STOCK_PREPPED, filter(preppedRnaStockSamples, byProject(project)).size());
      Count prepdDAl = new Count(SAMPLE_DNA_ALIQUOT_PREPPED, filter(preppedDnaAliquotSamples, byProject(project)).size());
      Count prepdRAl = new Count(SAMPLE_RNA_ALIQUOT_PREPPED, filter(preppedRnaAliquotSamples, byProject(project)).size());

      Count prepdWgLi = new Count(LIBRARY_WG_PREPPED, filter(preppedWgLibraries, byProject(project)).size());
      Count prepdExLi = new Count(LIBRARY_EX_PREPPED, filter(preppedExLibraries, byProject(project)).size());
      Count prepdTsLi = new Count(LIBRARY_TS_PREPPED, filter(preppedTsLibraries, byProject(project)).size());
      Count prepdDLi = new Count(LIBRARY_DNA_PREPPED, filter(preppedDnaLibraries, byProject(project)).size());
      Count prepdRLi = new Count(LIBRARY_RNA_PREPPED, filter(preppedRnaLibraries, byProject(project)).size());
      Count prepdNnLi = new Count(LIBRARY_NN_PREPPED, filter(preppedNnLibraries, byProject(project)).size());

      Count recdDLi = new Count(LIBRARY_DNA_RECEIVED, filter(receivedDnaLibraries, byProject(project)).size());
      Count recdRLi = new Count(LIBRARY_RNA_RECEIVED, filter(receivedRnaLibraries, byProject(project)).size());

      Count seqdDLiComp = new Count(LIBRARY_DNA_SEQUENCED_COMPLETED, filter(sequencedCompletedDnaLibraries, byProject(project)).size());
      Count seqdRLiComp = new Count(LIBRARY_RNA_SEQUENCED_COMPLETED, filter(sequencedCompletedRnaLibraries, byProject(project)).size());
      Count seqd10XComp = new Count(LIBRARY_10X_SEQUENCED_COMPLETED, filter(sequencedCompleted10XLibraries, byProject(project)).size());
      Count seqdDLiFail = new Count(LIBRARY_DNA_SEQUENCED_FAILED, filter(sequencedFailedDnaLibraries, byProject(project)).size());
      Count seqdRLiFail = new Count(LIBRARY_RNA_SEQUENCED_FAILED, filter(sequencedFailedRnaLibraries, byProject(project)).size());
      Count seqdDLiRep = new Count(LIBRARY_DNA_SEQUENCED_REPEATED, filter(sequencedRepeatedDnaLibraries, byProject(project)).size());
      Count seqdRLiRep = new Count(LIBRARY_RNA_SEQUENCED_REPEATED, filter(sequencedRepeatedRnaLibraries, byProject(project)).size());
      List<Count> countsForProj = Arrays.asList(recdP, recdR, recdM, recdU, recdX, recdC, recdDAn, recdRAn, prepdDSt, prepdRSt, prepdDAl,
          prepdRAl, unrecd, prepdWgLi, prepdExLi, prepdTsLi, prepdDLi, prepdRLi, prepdNnLi, recdDLi, recdRLi, seqdDLiComp, seqdRLiComp,
          seqd10XComp, seqdDLiFail, seqdRLiFail, seqdDLiRep, seqdRLiRep);
      countsByProject.put(project, countsForProj);
    }

    countsByProjectAsList = listifyCountsByProject(countsByProject);
  }

  private Predicate<SampleDto> byReceivedWithUnreceivedParents(Map<String, SampleDto> allSamples) {
    return dto -> {
      String received = getAttribute(ATTR_RECEIVE_DATE, dto);
      if (received == null) return false;
      String parentReceived = getUpstreamAttribute(ATTR_RECEIVE_DATE, dto, allSamples);
      if (parentReceived == null) return true;
      return false;
    };
  }

  private Predicate<SampleDto> byTissueLike() {
    return dto -> {
      String sampleCategory = getAttribute(ATTR_CATEGORY, dto);
      if (sampleCategory == null) throw new IllegalArgumentException("Sample is missing sample category");
      List<String> tissueLike = Arrays.asList(SAMPLE_CATEGORY_TISSUE, SAMPLE_CATEGORY_TISSUE_PROCESSING);
      return tissueLike.contains(sampleCategory);
    };
  }

  private Predicate<SampleDto> byAnalyte() {
    return dto -> {
      String sampleCategory = getAttribute(ATTR_CATEGORY, dto);
      if (sampleCategory == null) throw new IllegalArgumentException("Sample is missing sample category");
      List<String> analyte = Arrays.asList(SAMPLE_CATEGORY_STOCK, SAMPLE_CATEGORY_ALIQUOT);
      return analyte.contains(sampleCategory);
    };
  }

  private Predicate<SampleDto> byTissueTypes(List<String> tissueTypes, Map<String, SampleDto> allSamples) {
    return dto -> {
      String type = getAttribute(ATTR_TISSUE_TYPE, dto);
      if (type == null) type = getUpstreamAttribute(ATTR_TISSUE_TYPE, dto, allSamples);
      if (type == null) throw new IllegalArgumentException("sample is missing tissue type");
      return tissueTypes.contains(type);
    };
  }

  private Predicate<SampleDto> byAnalyteType(String analyteType) {
    return dto -> {
      String sampleType = dto.getSampleType();
      if (sampleType == null || analyteType == null) throw new IllegalArgumentException("sample is missing sample type or analyte type; may be a lib/dil?");
      return sampleType.contains(analyteType);
    };
  }

  private Predicate<SampleDto> byRnaLibrary() {
    return dto -> {
      String designCode = getAttribute(ATTR_SOURCE_TEMPLATE_TYPE, dto);
      if (designCode == null)
        throw new IllegalArgumentException("Library is missing a library design code; is " + dto.getName() + " really a library?");
      return RNA_LIBRARY_DESIGN_CODES.contains(designCode);
    };
  }

  private Predicate<SampleDto> byDnaLibrary() {
    return byRnaLibrary().negate();
  }

  private Predicate<SampleDto> byLibraryDesignCodes(List<String> libraryDesignCodes) {
    return dto -> libraryDesignCodes.contains(getAttribute(ATTR_SOURCE_TEMPLATE_TYPE, dto));
  }

  /**
   * Filters for samples which have no receive dates anywhere in the hierarchy (the sample itself; its parents; its children)
   * 
   * @param allSamples list of samples mapped by ID
   * @return a filter which returns true if a sample and all its children and direct ancestors have no received date.
   */
  private Predicate<SampleDto> byUnreceivedHierarchy(Map<String, SampleDto> allSamples) {
    return dto -> {
      String received = getAttribute(ATTR_RECEIVE_DATE, dto);
      if (received != null && !"".equals(received)) return false;
      received = getUpstreamAttribute(ATTR_RECEIVE_DATE, dto, allSamples);
      if (received != null && !"".equals(received)) return false;
      received = getDownstreamAttribute(ATTR_RECEIVE_DATE, dto, allSamples);
      if (received != null && !"".equals(received)) return false;
      return true;
    };
  }

  /**
   * Finds the direct children of a given sample.
   * 
   * @param sample the sample whose children should be returned
   * @param allSamples list of all samples, mapped by ID
   * @return a fully-populated list of children (if they exist; empty list otherwise)
   */
  private static List<SampleDto> getChildren(SampleDto sample, Map<String, SampleDto> allSamples) {
    if (sample.getChildren() == null) return null;
    List<String> childIds = sample.getChildren().stream().map(ref -> ref.getId()).collect(Collectors.toList());
    if (childIds == null || childIds.isEmpty()) {
      return null;
    }
    return childIds.stream().map(id -> allSamples.get(id)).collect(Collectors.toList());
  }

  /**
   * Get a downstream attribute if it is present on any of the children further down the hierarchy. Will never return an attribute present
   * on the given sample.
   * 
   * @param attributeName the attribute name to search for
   * @param sample a given sample
   * @param allSamples list of all samples mapped by ID
   * @return the attribute value if it is found; null otherwise
   */
  private static String getDownstreamAttribute(String attributeName, SampleDto sample, Map<String, SampleDto> allSamples) {
    List<SampleDto> children = getChildren(sample, allSamples);
    boolean found = false;
    if (children == null || children.isEmpty()) return null;
    for (SampleDto child : children) {
      // see if the child has the attr
      if (found) break;
      String attr = getAttribute(attributeName, child);
      if (attr != null) {
        found = true;
        return attr;
      } else {
        // if it doesn't, call this on its children
        getDownstreamAttribute(attributeName, child, allSamples);
      }
    }
    return null;
  }

  static final List<Map.Entry<String, List<Count>>> listifyCountsByProject(Map<String, List<Count>> countsByProject) {
    // need to convert it to a list, because getRow() takes an index and the treemap doesn't yet have one of those
    return new ArrayList<>(countsByProject.entrySet());
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return countsByProjectAsList.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
      Map.Entry<String, List<Count>> projectCounts = countsByProjectAsList.get(rowNum);
      return makeSummaryRow(projectCounts);
  }

  private String[] makeSummaryRow(Map.Entry<String, List<Count>> obj) {
    String[] row = new String[COLUMNS.size()];
    // Project
    row[0] = obj.getKey();
    // everything in `cols`, which is the list from which COLUMNS are generated and Count names are taken
    for (int i = 1; i <= cols.size(); i++) {
      row[i] = getCountForHeader(cols.get(i - 1), obj.getValue());
    }
    return row;
  }

  private String getCountForHeader(String header, List<Count> counts) {
    long headerCount = counts.stream()
        .filter(count -> count.getKey().equals(header))
        .findFirst().get().getValue();
    return String.valueOf(headerCount);
  }
}
