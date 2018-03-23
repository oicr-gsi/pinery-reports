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

  public static final String TISSUE_PRIMARY = "Primary Tissue";
  public static final String TISSUE_REFERENCE = "Reference Tissue";
  public static final String TISSUE_METASTATIC = "Metastatic Tissue";
  public static final String TISSUE_XENOGRAFT = "Xenograft Tissue";
  public static final String TISSUE_MISC = "Other Tissue";
  public static final String DNA_P_STOCK = "DNA P Stock";
  public static final String DNA_R_STOCK = "DNA R Stock";
  public static final String DNA_O_STOCK = "DNA O Stock";
  public static final String RNA_P_STOCK = "RNA P Stock";
  public static final String RNA_R_STOCK = "RNA R Stock";
  public static final String RNA_O_STOCK = "RNA O Stock";
  public static final String DNA_P_ALIQUOT = "DNA P Aliquot";
  public static final String DNA_R_ALIQUOT = "DNA R Aliquot";
  public static final String DNA_O_ALIQUOT = "DNA O Aliquot";
  public static final String RNA_P_ALIQUOT = "RNA P Aliquot";
  public static final String RNA_R_ALIQUOT = "RNA R Aliquot";
  public static final String RNA_O_ALIQUOT = "RNA O Aliquot";

  public static final String LIBRARY_WG = "WG Library";
  public static final String LIBRARY_EX = "EX Library";
  public static final String LIBRARY_TS = "TS Library";
  public static final String LIBRARY_DNA_10X = "DNA 10X Library";
  public static final String LIBRARY_DNA = "AS/CH/BS Library";
  public static final String LIBRARY_NN = "NN Library";
  public static final String LIBRARY_RNA = "RNA Library";
  public static final String LIBRARY_RNA_10X = "RNA 10X Library";
  public static final String LIBRARY_DNA_SEQUENCED_COMPLETED = "DNA Sequenced Completed";
  public static final String LIBRARY_RNA_SEQUENCED_COMPLETED = "RNA Sequenced Completed";
  public static final String LIBRARY_DNA_10X_SEQUENCED_COMPLETED = "DNA 10X Sequenced Completed";
  public static final String LIBRARY_RNA_10X_SEQUENCED_COMPLETED = "RNA 10X Sequenced Completed";
  public static final String LIBRARY_DNA_SEQUENCED_FAILED = "DNA Sequenced Failed";
  public static final String LIBRARY_RNA_SEQUENCED_FAILED = "RNA Sequenced Failed";
  public static final String LIBRARY_DNA_10X_SEQUENCED_FAILED = "DNA 10X Sequenced Failed";
  public static final String LIBRARY_RNA_10X_SEQUENCED_FAILED = "RNA 10X Sequenced Failed";
  public static final String LIBRARY_DNA_SEQUENCED_REPEATED = "DNA Sequenced Repeated";
  public static final String LIBRARY_RNA_SEQUENCED_REPEATED = "RNA Sequenced Repeated";
  public static final String LIBRARY_DNA_10X_SEQUENCED_REPEATED = "DNA 10X Sequenced Repeated";
  public static final String LIBRARY_RNA_10X_SEQUENCED_REPEATED = "RNA 10X Sequenced Repeated";

  public static final List<String> cols = Arrays.asList(
      TISSUE_PRIMARY,
      TISSUE_REFERENCE,
      TISSUE_METASTATIC,
      TISSUE_XENOGRAFT,
      TISSUE_MISC,
      DNA_P_STOCK,
      DNA_R_STOCK,
      DNA_O_STOCK,
      RNA_P_STOCK,
      RNA_R_STOCK,
      RNA_O_STOCK,
      DNA_P_ALIQUOT,
      DNA_R_ALIQUOT,
      DNA_O_ALIQUOT,
      RNA_P_ALIQUOT,
      RNA_R_ALIQUOT,
      RNA_O_ALIQUOT,
      LIBRARY_WG,
      LIBRARY_EX,
      LIBRARY_TS,
      LIBRARY_DNA_10X,
      LIBRARY_DNA,
      LIBRARY_NN,
      LIBRARY_RNA,
      LIBRARY_RNA_10X,
      LIBRARY_DNA_SEQUENCED_COMPLETED,
      LIBRARY_RNA_SEQUENCED_COMPLETED,
      LIBRARY_DNA_10X_SEQUENCED_COMPLETED,
      LIBRARY_RNA_10X_SEQUENCED_COMPLETED,
      LIBRARY_DNA_SEQUENCED_FAILED,
      LIBRARY_RNA_SEQUENCED_FAILED,
      LIBRARY_DNA_10X_SEQUENCED_FAILED,
      LIBRARY_RNA_10X_SEQUENCED_FAILED,
      LIBRARY_DNA_SEQUENCED_REPEATED,
      LIBRARY_RNA_SEQUENCED_REPEATED,
      LIBRARY_DNA_10X_SEQUENCED_REPEATED,
      LIBRARY_RNA_10X_SEQUENCED_REPEATED);

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
    List<SampleDto> primaryTissues;
    List<SampleDto> referenceTissues;
    List<SampleDto> metastaticTissues;
    List<SampleDto> xenoTissues;
    List<SampleDto> miscTissues;
    List<SampleDto> dnaPStock;
    List<SampleDto> dnaRStock;
    List<SampleDto> dnaOStock;
    List<SampleDto> rnaPStock;
    List<SampleDto> rnaRStock;
    List<SampleDto> rnaOStock;
    List<SampleDto> dnaPAliquots;
    List<SampleDto> dnaRAliquots;
    List<SampleDto> dnaOAliquots;
    List<SampleDto> rnaPAliquots;
    List<SampleDto> rnaRAliquots;
    List<SampleDto> rnaOAliquots;

    List<SampleDto> wgLibraries;
    List<SampleDto> exLibraries;
    List<SampleDto> tsLibraries;
    List<SampleDto> dna10XLibraries;
    List<SampleDto> dnaOtherLibraries;
    List<SampleDto> nnLibraries;
    List<SampleDto> rnaLibraries;
    List<SampleDto> rna10XLibraries;

    Set<SampleDto> sequencedCompletedDnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedDna10XLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRna10XLibraries = new HashSet<>();
    Set<SampleDto> sequencedFailedDnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedFailedRnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedFailedDna10XLibraries = new HashSet<>();
    Set<SampleDto> sequencedFailedRna10XLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRnaLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDna10XLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRna10XLibraries = new HashSet<>();

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

    primaryTissues = filter(realSamples, Arrays.asList(byTissueLike(), byPrimary(allSamplesById)));
    referenceTissues = filter(realSamples, Arrays.asList(byTissueLike(), byReference(allSamplesById)));
    metastaticTissues = filter(realSamples, Arrays.asList(byTissueLike(), byMetastatic(allSamplesById)));
    xenoTissues = filter(realSamples, Arrays.asList(byTissueLike(), byXeno(allSamplesById)));
    miscTissues = filter(realSamples, Arrays.asList(byTissueLike(), byMisc(allSamplesById)));
    dnaPStock = filter(realSamples, Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaRStock = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaOStock = filter(realSamples, Arrays.asList(byOther(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    rnaPStock = filter(realSamples, Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaRStock = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaOStock = filter(realSamples, Arrays.asList(byOther(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    dnaPAliquots = filter(realSamples,
        Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    dnaRAliquots = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    dnaOAliquots = filter(realSamples,
        Arrays.asList(byOther(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    rnaPAliquots = filter(realSamples,
        Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaRAliquots = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaOAliquots = filter(realSamples,
        Arrays.asList(byOther(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    
    wgLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG))));
    exLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX))));
    tsLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS))));
    dna10XLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary()));
    dnaOtherLibraries = filter(libraries,
        Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS))));
    nnLibraries = filter(libraries, Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN))));
    rnaLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary()));
    rna10XLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary()));
    
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
          boolean is10X = is10XLibrary(library);
          if (isRnaLibrary(dilution, allSamplesById)) {
            if (RUN_FAILED.equals(run.getState())) {
              if (is10X) {
                sequencedFailedRna10XLibraries.add(library);
              } else {
                sequencedFailedRnaLibraries.add(library);
              }
            }
            if (sequencedCompletedRnaLibraries.contains(library)) {
              sequencedRepeatedRnaLibraries.add(library);
            } else if (sequencedCompletedRna10XLibraries.contains(library)) {
              sequencedRepeatedRna10XLibraries.add(library);
            } else {
              if (is10X) {
                sequencedCompletedRna10XLibraries.add(library);
              } else {
                sequencedCompletedRnaLibraries.add(library);
              }
            }
          } else {
            if (RUN_FAILED.equals(run.getState())) {
              if (is10X) {
                sequencedFailedDna10XLibraries.add(library);
              } else {
                sequencedFailedDnaLibraries.add(library);
              }
            }
            if (sequencedCompletedDnaLibraries.contains(library)) {
              sequencedRepeatedDnaLibraries.add(library);
            } else if (sequencedCompletedDna10XLibraries.contains(library)) {
              sequencedRepeatedDna10XLibraries.add(library);
            } else {
              if (is10X) {
                sequencedCompletedDna10XLibraries.add(library);
              } else {
                sequencedCompletedDnaLibraries.add(library);
              }
            }
          }
        }
      }
    }

    for (String project : projects) {
      Count pTissue = new Count(TISSUE_PRIMARY, filter(primaryTissues, byProject(project)).size());
      Count rTissue = new Count(TISSUE_REFERENCE, filter(referenceTissues, byProject(project)).size());
      Count mTissue = new Count(TISSUE_METASTATIC, filter(metastaticTissues, byProject(project)).size());
      Count xTissue = new Count(TISSUE_XENOGRAFT, filter(xenoTissues, byProject(project)).size());
      Count oTissue = new Count(TISSUE_MISC, filter(miscTissues, byProject(project)).size());
      Count dPStock = new Count(DNA_P_STOCK, filter(dnaPStock, byProject(project)).size());
      Count dRStock = new Count(DNA_R_STOCK, filter(dnaRStock, byProject(project)).size());
      Count dOStock = new Count(DNA_O_STOCK, filter(dnaOStock, byProject(project)).size());
      Count rPStock = new Count(RNA_P_STOCK, filter(rnaPStock, byProject(project)).size());
      Count rRStock = new Count(RNA_R_STOCK, filter(rnaRStock, byProject(project)).size());
      Count rOStock = new Count(RNA_O_STOCK, filter(rnaOStock, byProject(project)).size());
      Count dPAli = new Count(DNA_P_ALIQUOT, filter(dnaPAliquots, byProject(project)).size());
      Count dRAli = new Count(DNA_R_ALIQUOT, filter(dnaRAliquots, byProject(project)).size());
      Count dOAli = new Count(DNA_O_ALIQUOT, filter(dnaOAliquots, byProject(project)).size());
      Count rPAli = new Count(RNA_P_ALIQUOT, filter(rnaPAliquots, byProject(project)).size());
      Count rRAli = new Count(RNA_R_ALIQUOT, filter(rnaRAliquots, byProject(project)).size());
      Count rOAli = new Count(RNA_O_ALIQUOT, filter(rnaOAliquots, byProject(project)).size());

      Count wgLi = new Count(LIBRARY_WG, filter(wgLibraries, byProject(project)).size());
      Count exLi = new Count(LIBRARY_EX, filter(exLibraries, byProject(project)).size());
      Count tsLi = new Count(LIBRARY_TS, filter(tsLibraries, byProject(project)).size());
      Count d10Li = new Count(LIBRARY_DNA_10X, filter(dna10XLibraries, byProject(project)).size());
      Count dOLi = new Count(LIBRARY_DNA, filter(dnaOtherLibraries, byProject(project)).size());
      Count nnLi = new Count(LIBRARY_NN, filter(nnLibraries, byProject(project)).size());
      Count rLi = new Count(LIBRARY_RNA, filter(rnaLibraries, byProject(project)).size());
      Count r10Li = new Count(LIBRARY_RNA_10X, filter(rna10XLibraries, byProject(project)).size());

      Count seqdDLiComp = new Count(LIBRARY_DNA_SEQUENCED_COMPLETED, filter(sequencedCompletedDnaLibraries, byProject(project)).size());
      Count seqdRLiComp = new Count(LIBRARY_RNA_SEQUENCED_COMPLETED, filter(sequencedCompletedRnaLibraries, byProject(project)).size());
      Count seqdD10XComp = new Count(LIBRARY_DNA_10X_SEQUENCED_COMPLETED,
          filter(sequencedCompletedDna10XLibraries, byProject(project)).size());
      Count seqdR10XComp = new Count(LIBRARY_RNA_10X_SEQUENCED_COMPLETED,
          filter(sequencedCompletedRna10XLibraries, byProject(project)).size());
      Count seqdDLiFail = new Count(LIBRARY_DNA_SEQUENCED_FAILED, filter(sequencedFailedDnaLibraries, byProject(project)).size());
      Count seqdRLiFail = new Count(LIBRARY_RNA_SEQUENCED_FAILED, filter(sequencedFailedRnaLibraries, byProject(project)).size());
      Count seqdD10XFail = new Count(LIBRARY_DNA_10X_SEQUENCED_FAILED, filter(sequencedFailedDna10XLibraries, byProject(project)).size());
      Count seqdR10XFail = new Count(LIBRARY_RNA_10X_SEQUENCED_FAILED, filter(sequencedFailedRna10XLibraries, byProject(project)).size());
      Count seqdDLiRep = new Count(LIBRARY_DNA_SEQUENCED_REPEATED, filter(sequencedRepeatedDnaLibraries, byProject(project)).size());
      Count seqdRLiRep = new Count(LIBRARY_RNA_SEQUENCED_REPEATED, filter(sequencedRepeatedRnaLibraries, byProject(project)).size());
      Count seqdD10XRep = new Count(LIBRARY_DNA_10X_SEQUENCED_REPEATED,
          filter(sequencedRepeatedDna10XLibraries, byProject(project)).size());
      Count seqdR10XRep = new Count(LIBRARY_RNA_10X_SEQUENCED_REPEATED,
          filter(sequencedRepeatedRna10XLibraries, byProject(project)).size());

      List<Count> countsForProj = Arrays.asList(pTissue, rTissue, mTissue, xTissue, oTissue, dPStock, dRStock, dOStock, rPStock, rRStock,
          rOStock, dPAli, dRAli, dOAli, rPAli, rRAli, rOAli, wgLi, exLi, tsLi, d10Li, dOLi, nnLi, rLi, r10Li, seqdDLiComp, seqdRLiComp,
          seqdD10XComp, seqdR10XComp, seqdDLiFail, seqdRLiFail, seqdD10XFail, seqdR10XFail, seqdDLiRep, seqdRLiRep, seqdD10XRep,
          seqdR10XRep);
      countsByProject.put(project, countsForProj);
    }

    countsByProjectAsList = listifyCountsByProject(countsByProject);
  }

  private Predicate<SampleDto> byTissueLike() {
    return dto -> {
      String sampleCategory = getAttribute(ATTR_CATEGORY, dto);
      if (sampleCategory == null) throw new IllegalArgumentException("Sample is missing sample category");
      List<String> tissueLike = Arrays.asList(SAMPLE_CATEGORY_TISSUE, SAMPLE_CATEGORY_TISSUE_PROCESSING);
      return tissueLike.contains(sampleCategory);
    };
  }

  private boolean tissueTypeMatches(SampleDto sample, List<String> tissueTypes, Map<String, SampleDto> allSamples) {
    String type = getAttribute(ATTR_TISSUE_TYPE, sample);
    if (type == null) type = getUpstreamAttribute(ATTR_TISSUE_TYPE, sample, allSamples);
      if (type == null) throw new IllegalArgumentException("sample is missing tissue type");
      return tissueTypes.contains(type);
  }

  private Predicate<SampleDto> byPrimary(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> primaryLike = Arrays.asList("P", "S", "O", "A");
      return tissueTypeMatches(dto, primaryLike, allSamples);
    };
  }

  private Predicate<SampleDto> byReference(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> primaryLike = Arrays.asList("R");
      return tissueTypeMatches(dto, primaryLike, allSamples);
    };
  }

  private Predicate<SampleDto> byMetastatic(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> primaryLike = Arrays.asList("M");
      return tissueTypeMatches(dto, primaryLike, allSamples);
    };
  }

  private Predicate<SampleDto> byXeno(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> primaryLike = Arrays.asList("X");
      return tissueTypeMatches(dto, primaryLike, allSamples);
    };
  }

  private Predicate<SampleDto> byMisc(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> primaryLike = Arrays.asList("C", "U", "n", "E", "T");
      return tissueTypeMatches(dto, primaryLike, allSamples);
    };
  }

  private Predicate<SampleDto> byOther(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> primaryLike = Arrays.asList("C", "M", "U", "X", "n", "E", "T");
      return tissueTypeMatches(dto, primaryLike, allSamples);
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
        throw new IllegalArgumentException("Library is missing a library design code; is " + dto.getId() + " really a library?");
      return RNA_LIBRARY_DESIGN_CODES.contains(designCode);
    };
  }

  private Predicate<SampleDto> byDnaLibrary() {
    return byRnaLibrary().negate();
  }

  private Predicate<SampleDto> by10XLibrary() {
    return dto -> is10XLibrary(dto);
  }

  private Predicate<SampleDto> byLibraryDesignCodes(List<String> libraryDesignCodes) {
    return dto -> libraryDesignCodes.contains(getAttribute(ATTR_SOURCE_TEMPLATE_TYPE, dto));
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
