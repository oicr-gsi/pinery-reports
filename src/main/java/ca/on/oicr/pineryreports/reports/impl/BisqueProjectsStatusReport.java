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

  public static final String LIBRARY_P_WG = "WG P Library";
  public static final String LIBRARY_R_WG = "WG R Library";
  public static final String LIBRARY_O_WG = "WG O Library";
  public static final String LIBRARY_P_EX = "EX P Library";
  public static final String LIBRARY_R_EX = "EX R Library";
  public static final String LIBRARY_O_EX = "EX O Library";
  public static final String LIBRARY_P_TS = "TS P Library";
  public static final String LIBRARY_R_TS = "TS R Library";
  public static final String LIBRARY_O_TS = "TS O Library";
  public static final String LIBRARY_DNA_10X_P = "DNA 10X P Library";
  public static final String LIBRARY_DNA_10X_R = "DNA 10X R Library";
  public static final String LIBRARY_DNA_10X_O = "DNA 10X O Library";
  public static final String LIBRARY_P_DNA = "AS/CH/BS P Library";
  public static final String LIBRARY_R_DNA = "AS/CH/BS R Library";
  public static final String LIBRARY_O_DNA = "AS/CH/BS O Library";
  public static final String LIBRARY_NN_P = "NN P Library";
  public static final String LIBRARY_NN_O = "NN O Library";
  public static final String LIBRARY_P_RNA = "RNA P Library";
  public static final String LIBRARY_R_RNA = "RNA R Library";
  public static final String LIBRARY_O_RNA = "RNA O Library";
  public static final String LIBRARY_RNA_10X_P = "RNA 10X P Library";
  public static final String LIBRARY_RNA_10X_R = "RNA 10X R Library";
  public static final String LIBRARY_RNA_10X_O = "RNA 10X O Library";
  public static final String LIBRARY_DNA_P_SEQUENCED_COMPLETED = "DNA P Sequenced Completed";
  public static final String LIBRARY_DNA_R_SEQUENCED_COMPLETED = "DNA R Sequenced Completed";
  public static final String LIBRARY_DNA_O_SEQUENCED_COMPLETED = "DNA O Sequenced Completed";
  public static final String LIBRARY_RNA_P_SEQUENCED_COMPLETED = "RNA P Sequenced Completed";
  public static final String LIBRARY_RNA_R_SEQUENCED_COMPLETED = "RNA R Sequenced Completed";
  public static final String LIBRARY_RNA_O_SEQUENCED_COMPLETED = "RNA O Sequenced Completed";
  public static final String LIBRARY_DNA_10X_P_SEQUENCED_COMPLETED = "DNA 10X P Sequenced Completed";
  public static final String LIBRARY_DNA_10X_R_SEQUENCED_COMPLETED = "DNA 10X R Sequenced Completed";
  public static final String LIBRARY_DNA_10X_O_SEQUENCED_COMPLETED = "DNA 10X O Sequenced Completed";
  public static final String LIBRARY_RNA_10X_P_SEQUENCED_COMPLETED = "RNA 10X P Sequenced Completed";
  public static final String LIBRARY_RNA_10X_R_SEQUENCED_COMPLETED = "RNA 10X R Sequenced Completed";
  public static final String LIBRARY_RNA_10X_O_SEQUENCED_COMPLETED = "RNA 10X O Sequenced Completed";
  public static final String LIBRARY_DNA_P_SEQUENCED_REPEATED = "DNA P Sequenced Repeated";
  public static final String LIBRARY_DNA_R_SEQUENCED_REPEATED = "DNA R Sequenced Repeated";
  public static final String LIBRARY_DNA_O_SEQUENCED_REPEATED = "DNA O Sequenced Repeated";
  public static final String LIBRARY_RNA_P_SEQUENCED_REPEATED = "RNA P Sequenced Repeated";
  public static final String LIBRARY_RNA_R_SEQUENCED_REPEATED = "RNA R Sequenced Repeated";
  public static final String LIBRARY_RNA_O_SEQUENCED_REPEATED = "RNA O Sequenced Repeated";
  public static final String LIBRARY_DNA_10X_P_SEQUENCED_REPEATED = "DNA 10X P Sequenced Repeated";
  public static final String LIBRARY_DNA_10X_R_SEQUENCED_REPEATED = "DNA 10X R Sequenced Repeated";
  public static final String LIBRARY_DNA_10X_O_SEQUENCED_REPEATED = "DNA 10X O Sequenced Repeated";
  public static final String LIBRARY_RNA_10X_P_SEQUENCED_REPEATED = "RNA 10X P Sequenced Repeated";
  public static final String LIBRARY_RNA_10X_R_SEQUENCED_REPEATED = "RNA 10X R Sequenced Repeated";
  public static final String LIBRARY_RNA_10X_O_SEQUENCED_REPEATED = "RNA 10X O Sequenced Repeated";

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
      LIBRARY_P_WG,
      LIBRARY_R_WG,
      LIBRARY_O_WG,
      LIBRARY_P_EX,
      LIBRARY_R_EX,
      LIBRARY_O_EX,
      LIBRARY_P_TS,
      LIBRARY_R_TS,
      LIBRARY_O_TS,
      LIBRARY_DNA_10X_P,
      LIBRARY_DNA_10X_R,
      LIBRARY_DNA_10X_O,
      LIBRARY_P_DNA,
      LIBRARY_R_DNA,
      LIBRARY_O_DNA,
      LIBRARY_NN_P,
      LIBRARY_NN_O,
      LIBRARY_P_RNA,
      LIBRARY_R_RNA,
      LIBRARY_O_RNA,
      LIBRARY_RNA_10X_P,
      LIBRARY_RNA_10X_R,
      LIBRARY_RNA_10X_O,
      LIBRARY_DNA_P_SEQUENCED_COMPLETED,
      LIBRARY_DNA_R_SEQUENCED_COMPLETED,
      LIBRARY_DNA_O_SEQUENCED_COMPLETED,
      LIBRARY_RNA_P_SEQUENCED_COMPLETED,
      LIBRARY_RNA_R_SEQUENCED_COMPLETED,
      LIBRARY_RNA_O_SEQUENCED_COMPLETED,
      LIBRARY_DNA_10X_P_SEQUENCED_COMPLETED,
      LIBRARY_DNA_10X_R_SEQUENCED_COMPLETED,
      LIBRARY_DNA_10X_O_SEQUENCED_COMPLETED,
      LIBRARY_RNA_10X_P_SEQUENCED_COMPLETED,
      LIBRARY_RNA_10X_R_SEQUENCED_COMPLETED,
      LIBRARY_RNA_10X_O_SEQUENCED_COMPLETED,
      LIBRARY_DNA_P_SEQUENCED_REPEATED,
      LIBRARY_DNA_R_SEQUENCED_REPEATED,
      LIBRARY_DNA_O_SEQUENCED_REPEATED,
      LIBRARY_RNA_P_SEQUENCED_REPEATED,
      LIBRARY_RNA_R_SEQUENCED_REPEATED,
      LIBRARY_RNA_O_SEQUENCED_REPEATED,
      LIBRARY_DNA_10X_P_SEQUENCED_REPEATED,
      LIBRARY_DNA_10X_R_SEQUENCED_REPEATED,
      LIBRARY_DNA_10X_O_SEQUENCED_REPEATED,
      LIBRARY_RNA_10X_P_SEQUENCED_REPEATED,
      LIBRARY_RNA_10X_R_SEQUENCED_REPEATED,
      LIBRARY_RNA_10X_O_SEQUENCED_REPEATED);

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
    Set<String> projects = Sets.newHashSet("ONT", "ASJD", "EECS", "LCLC", "GCMS", "TPS", "RIPSQ", "OVBM", "APT", "DKT1", "NOVA", "BTC",
        "GLCS", "RKT1", "TURN", "BLS", "FNS", "RDM", "DXRX", "FFPER", "CPTP", "C4GD", "JCK", "MATCH", "BFS", "GECCO", "SCRM", "EAC", "DYS",
        "CYT", "ERNA", "JAMLR", "IMEC", "DCIS", "PCSI", "ASHPC");
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

    List<SampleDto> wgPLibraries;
    List<SampleDto> wgRLibraries;
    List<SampleDto> wgOLibraries;
    List<SampleDto> exPLibraries;
    List<SampleDto> exRLibraries;
    List<SampleDto> exOLibraries;
    List<SampleDto> tsPLibraries;
    List<SampleDto> tsRLibraries;
    List<SampleDto> tsOLibraries;
    List<SampleDto> dna10xPLibraries;
    List<SampleDto> dna10xRLibraries;
    List<SampleDto> dna10xOLibraries;
    List<SampleDto> dnaOtherPLibraries;
    List<SampleDto> dnaOtherRLibraries;
    List<SampleDto> dnaOtherOLibraries;
    List<SampleDto> nnPLibraries;
    List<SampleDto> nnOLibraries;
    List<SampleDto> rnaPLibraries;
    List<SampleDto> rnaRLibraries;
    List<SampleDto> rnaOLibraries;
    List<SampleDto> rna10xPLibraries;
    List<SampleDto> rna10xRLibraries;
    List<SampleDto> rna10xOLibraries;

    Set<SampleDto> sequencedCompletedDnaPLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedDnaRLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedDnaOLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRnaPLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRnaRLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRnaOLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedDna10xPLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedDna10xRLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedDna10xOLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRna10xPLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRna10xRLibraries = new HashSet<>();
    Set<SampleDto> sequencedCompletedRna10xOLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDnaPLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDnaRLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDnaOLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRnaPLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRnaRLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRnaOLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDna10xPLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDna10xRLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedDna10xOLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRna10xPLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRna10xRLibraries = new HashSet<>();
    Set<SampleDto> sequencedRepeatedRna10xOLibraries = new HashSet<>();

    for (SampleDto sam : allSamples) {
      // TODO: re-add after testing projects.add(sam.getProjectName());
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
    
    wgPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byPrimary(allSamplesById)));
    wgRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byReference(allSamplesById)));
    wgOLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byOther(allSamplesById)));
    exPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byPrimary(allSamplesById)));
    exRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byReference(allSamplesById)));
    exOLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byOther(allSamplesById)));
    tsPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byPrimary(allSamplesById)));
    tsRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byReference(allSamplesById)));
    tsOLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byOther(allSamplesById)));
    dna10xPLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(), byPrimary(allSamplesById)));
    dna10xRLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(), byReference(allSamplesById)));
    dna10xOLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(), byOther(allSamplesById)));
    dnaOtherPLibraries = filter(libraries,
        Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byPrimary(allSamplesById)));
    dnaOtherRLibraries = filter(libraries,
        Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byReference(allSamplesById)));
    dnaOtherOLibraries = filter(libraries,
        Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byOther(allSamplesById)));
    nnPLibraries = filter(libraries,
        Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byPrimary(allSamplesById)));
    nnOLibraries = filter(libraries,
        Arrays.asList(byPropagated(), byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byOther(allSamplesById)));
    rnaPLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byPrimary(allSamplesById)));
    rnaRLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byReference(allSamplesById)));
    rnaOLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byOther(allSamplesById)));
    rna10xPLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(), byPrimary(allSamplesById)));
    rna10xRLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(), byReference(allSamplesById)));
    rna10xOLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(), byOther(allSamplesById)));
    
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
          List<String> p = Arrays.asList("P");
          List<String> r = Arrays.asList("R");
          if (isRnaLibrary(dilution, allSamplesById)) {
            // want only completed libraries for now
            if (RUN_FAILED.equals(run.getState())) continue;
            if (is10XLibrary(library)) {
              // RNA, 10X
              if (tissueTypeMatches(library, p, allSamplesById)) {
                addToCompletedOrRepeated(library, sequencedRepeatedRna10xPLibraries, sequencedCompletedRna10xPLibraries);
              } else if (tissueTypeMatches(library, r, allSamplesById)) {
                addToCompletedOrRepeated(library, sequencedRepeatedRna10xRLibraries, sequencedCompletedRna10xRLibraries);
              } else {
                addToCompletedOrRepeated(library, sequencedRepeatedRna10xOLibraries, sequencedCompletedRna10xOLibraries);
              }
            } else {
              // RNA, no 10X
              if (tissueTypeMatches(library, p, allSamplesById)) {
                addToCompletedOrRepeated(library, sequencedRepeatedRnaPLibraries, sequencedCompletedRnaPLibraries);
              } else if (tissueTypeMatches(library, r, allSamplesById)) {
                addToCompletedOrRepeated(library, sequencedRepeatedRnaRLibraries, sequencedCompletedRnaRLibraries);
              } else {
                addToCompletedOrRepeated(library, sequencedRepeatedRnaOLibraries, sequencedCompletedRnaOLibraries);
              }
            }
          } else {
            // want only completed libraries for now
            if (RUN_FAILED.equals(run.getState())) continue;
            if (is10XLibrary(library)) {
              // DNA, 10X
              if (tissueTypeMatches(library, p, allSamplesById)) {
                addToCompletedOrRepeated(library, sequencedRepeatedDna10xPLibraries, sequencedCompletedDna10xPLibraries);
              } else if (tissueTypeMatches(library, r, allSamplesById)) {
                addToCompletedOrRepeated(library, sequencedRepeatedDna10xRLibraries, sequencedCompletedDna10xRLibraries);
              } else {
                addToCompletedOrRepeated(library, sequencedRepeatedDna10xOLibraries, sequencedCompletedDna10xOLibraries);
              }
            } else {
              // DNA, no 10X
              if (tissueTypeMatches(library, p, allSamplesById)) {
                addToCompletedOrRepeated(library, sequencedRepeatedDnaPLibraries, sequencedCompletedDnaPLibraries);
              } else if (tissueTypeMatches(library, r, allSamplesById)) {
                addToCompletedOrRepeated(library, sequencedRepeatedDnaRLibraries, sequencedCompletedDnaRLibraries);
              } else {
                addToCompletedOrRepeated(library, sequencedRepeatedDnaOLibraries, sequencedCompletedDnaOLibraries);
              }
            }
          }
        }
      }
    }

    for (String project : projects) {
      Count priTissue = new Count(TISSUE_PRIMARY, filter(primaryTissues, byProject(project)).size());
      Count refTissue = new Count(TISSUE_REFERENCE, filter(referenceTissues, byProject(project)).size());
      Count metTissue = new Count(TISSUE_METASTATIC, filter(metastaticTissues, byProject(project)).size());
      Count xenoTissue = new Count(TISSUE_XENOGRAFT, filter(xenoTissues, byProject(project)).size());
      Count otherTissue = new Count(TISSUE_MISC, filter(miscTissues, byProject(project)).size());
      Count dnaPriStock = new Count(DNA_P_STOCK, filter(dnaPStock, byProject(project)).size());
      Count dnaRefStock = new Count(DNA_R_STOCK, filter(dnaRStock, byProject(project)).size());
      Count dnaOtherStock = new Count(DNA_O_STOCK, filter(dnaOStock, byProject(project)).size());
      Count rnaPriStock = new Count(RNA_P_STOCK, filter(rnaPStock, byProject(project)).size());
      Count rnaRefStock = new Count(RNA_R_STOCK, filter(rnaRStock, byProject(project)).size());
      Count rnaOtherStock = new Count(RNA_O_STOCK, filter(rnaOStock, byProject(project)).size());
      Count dnaPriAliq = new Count(DNA_P_ALIQUOT, filter(dnaPAliquots, byProject(project)).size());
      Count dnaRefAliq = new Count(DNA_R_ALIQUOT, filter(dnaRAliquots, byProject(project)).size());
      Count dnaOtherAliq = new Count(DNA_O_ALIQUOT, filter(dnaOAliquots, byProject(project)).size());
      Count rnaPriAliq = new Count(RNA_P_ALIQUOT, filter(rnaPAliquots, byProject(project)).size());
      Count rnaRefAliq = new Count(RNA_R_ALIQUOT, filter(rnaRAliquots, byProject(project)).size());
      Count rnaOtherAliq = new Count(RNA_O_ALIQUOT, filter(rnaOAliquots, byProject(project)).size());

      Count wgPriLib = new Count(LIBRARY_P_WG, filter(wgPLibraries, byProject(project)).size());
      Count wgRefLib = new Count(LIBRARY_R_WG, filter(wgRLibraries, byProject(project)).size());
      Count wgOtherLib = new Count(LIBRARY_O_WG, filter(wgOLibraries, byProject(project)).size());
      Count exPriLib = new Count(LIBRARY_P_EX, filter(exPLibraries, byProject(project)).size());
      Count exRefLib = new Count(LIBRARY_R_EX, filter(exRLibraries, byProject(project)).size());
      Count exOtherLib = new Count(LIBRARY_O_EX, filter(exOLibraries, byProject(project)).size());
      Count tsPriLib = new Count(LIBRARY_P_TS, filter(tsPLibraries, byProject(project)).size());
      Count tsRefLib = new Count(LIBRARY_R_TS, filter(tsRLibraries, byProject(project)).size());
      Count tsOtherLib = new Count(LIBRARY_O_TS, filter(tsOLibraries, byProject(project)).size());
      Count dna10xPriLib = new Count(LIBRARY_DNA_10X_P, filter(dna10xPLibraries, byProject(project)).size());
      Count dna10xRefLib = new Count(LIBRARY_DNA_10X_R, filter(dna10xRLibraries, byProject(project)).size());
      Count dna10xOtherLib = new Count(LIBRARY_DNA_10X_O, filter(dna10xOLibraries, byProject(project)).size());
      Count dnaMiscPriLib = new Count(LIBRARY_P_DNA, filter(dnaOtherPLibraries, byProject(project)).size());
      Count dnaMiscRefLib = new Count(LIBRARY_R_DNA, filter(dnaOtherRLibraries, byProject(project)).size());
      Count dnaMiscOtherLib = new Count(LIBRARY_O_DNA, filter(dnaOtherOLibraries, byProject(project)).size());
      Count nnPriLib = new Count(LIBRARY_NN_P, filter(nnPLibraries, byProject(project)).size());
      Count nnOtherLib = new Count(LIBRARY_NN_O, filter(nnOLibraries, byProject(project)).size());
      Count rnaPriLib = new Count(LIBRARY_P_RNA, filter(rnaPLibraries, byProject(project)).size());
      Count rnaRefLib = new Count(LIBRARY_R_RNA, filter(rnaRLibraries, byProject(project)).size());
      Count rnaOtherLib = new Count(LIBRARY_O_RNA, filter(rnaOLibraries, byProject(project)).size());
      Count rna10xPriLib = new Count(LIBRARY_RNA_10X_P, filter(rna10xPLibraries, byProject(project)).size());
      Count rna10xRefLib = new Count(LIBRARY_RNA_10X_R, filter(rna10xRLibraries, byProject(project)).size());
      Count rna10xOtherLib = new Count(LIBRARY_RNA_10X_O, filter(rna10xOLibraries, byProject(project)).size());

      Count seqDnaPriComplete = new Count(LIBRARY_DNA_P_SEQUENCED_COMPLETED,
          filter(sequencedCompletedDnaPLibraries, byProject(project)).size());
      Count seqDnaRefComplete= new Count(LIBRARY_DNA_R_SEQUENCED_COMPLETED, filter(sequencedCompletedDnaRLibraries, byProject(project)).size());
      Count seqDnaOtherComplete = new Count(LIBRARY_DNA_O_SEQUENCED_COMPLETED,
          filter(sequencedCompletedDnaOLibraries, byProject(project)).size());
      Count seqRnaPriComplete = new Count(LIBRARY_RNA_P_SEQUENCED_COMPLETED,
          filter(sequencedCompletedRnaPLibraries, byProject(project)).size());
      Count seqRnaRefComplete = new Count(LIBRARY_RNA_R_SEQUENCED_COMPLETED, filter(sequencedCompletedRnaRLibraries, byProject(project)).size());
      Count seqRnaOtherComplete = new Count(LIBRARY_RNA_O_SEQUENCED_COMPLETED, filter(sequencedCompletedRnaOLibraries, byProject(project)).size());
      Count seqDna10xPriComplete = new Count(LIBRARY_DNA_10X_P_SEQUENCED_COMPLETED,
          filter(sequencedCompletedDna10xPLibraries, byProject(project)).size());
      Count seqDna10xRefComplete = new Count(LIBRARY_DNA_10X_R_SEQUENCED_COMPLETED,
          filter(sequencedCompletedDna10xRLibraries, byProject(project)).size());
      Count seqDna10xOtherComplete = new Count(LIBRARY_DNA_10X_O_SEQUENCED_COMPLETED,
          filter(sequencedCompletedDna10xOLibraries, byProject(project)).size());
      Count seqRna10xPriComplete = new Count(LIBRARY_RNA_10X_P_SEQUENCED_COMPLETED,
          filter(sequencedCompletedRna10xPLibraries, byProject(project)).size());
      Count seqRna10xRefComplete = new Count(LIBRARY_RNA_10X_R_SEQUENCED_COMPLETED,
          filter(sequencedCompletedRna10xRLibraries, byProject(project)).size());
      Count seqRna10xOtherComplete = new Count(LIBRARY_RNA_10X_O_SEQUENCED_COMPLETED,
          filter(sequencedCompletedRna10xOLibraries, byProject(project)).size());
      Count seqDnaPriRepeat = new Count(LIBRARY_DNA_P_SEQUENCED_REPEATED,
          filter(sequencedRepeatedDnaPLibraries, byProject(project)).size());
      Count seqDnaRefRepeat = new Count(LIBRARY_DNA_R_SEQUENCED_REPEATED,
          filter(sequencedRepeatedDnaRLibraries, byProject(project)).size());
      Count seqDnaOtherRepeat = new Count(LIBRARY_DNA_O_SEQUENCED_REPEATED,
          filter(sequencedRepeatedDnaOLibraries, byProject(project)).size());
      Count seqRnaPriRepeat = new Count(LIBRARY_RNA_P_SEQUENCED_REPEATED,
          filter(sequencedRepeatedRnaPLibraries, byProject(project)).size());
      Count seqRnaRefRepeat = new Count(LIBRARY_RNA_R_SEQUENCED_REPEATED,
          filter(sequencedRepeatedRnaRLibraries, byProject(project)).size());
      Count seqRnaOtherRepeat = new Count(LIBRARY_RNA_O_SEQUENCED_REPEATED,
          filter(sequencedRepeatedRnaOLibraries, byProject(project)).size());
      Count seqDna10xPriRepeat = new Count(LIBRARY_DNA_10X_P_SEQUENCED_REPEATED,
          filter(sequencedRepeatedDna10xPLibraries, byProject(project)).size());
      Count seqDna10xRefRepeat = new Count(LIBRARY_DNA_10X_R_SEQUENCED_REPEATED,
          filter(sequencedRepeatedDna10xRLibraries, byProject(project)).size());
      Count seqDna10xOtherRepeat = new Count(LIBRARY_DNA_10X_O_SEQUENCED_REPEATED,
          filter(sequencedRepeatedDna10xOLibraries, byProject(project)).size());
      Count seqRna10xPriRepeat = new Count(LIBRARY_RNA_10X_P_SEQUENCED_REPEATED,
          filter(sequencedRepeatedRna10xPLibraries, byProject(project)).size());
      Count seqRna10xRefRepeat = new Count(LIBRARY_RNA_10X_R_SEQUENCED_REPEATED,
          filter(sequencedRepeatedRna10xRLibraries, byProject(project)).size());
      Count seqRna10xOtherRepeat = new Count(LIBRARY_RNA_10X_O_SEQUENCED_REPEATED,
          filter(sequencedRepeatedRna10xOLibraries, byProject(project)).size());

      List<Count> countsForProj = Arrays.asList(priTissue, refTissue, metTissue, xenoTissue, otherTissue, //
          dnaPriStock, dnaRefStock, dnaOtherStock, rnaPriStock, rnaRefStock, rnaOtherStock, //
          dnaPriAliq, dnaRefAliq, dnaOtherAliq, rnaPriAliq, rnaRefAliq, rnaOtherAliq, //
          wgPriLib, wgRefLib, wgOtherLib, //
          exPriLib, exRefLib, exOtherLib, //
          tsPriLib, tsRefLib, tsOtherLib, //
          dna10xPriLib, dna10xRefLib, dna10xOtherLib, //
          dnaMiscPriLib, dnaMiscRefLib, dnaMiscOtherLib, //
          nnPriLib, nnOtherLib, //
          rnaPriLib, rnaRefLib, rnaOtherLib, //
          rna10xPriLib, rna10xRefLib, rna10xOtherLib,
          seqDnaPriComplete, seqDnaRefComplete, seqDnaOtherComplete, //
          seqRnaPriComplete, seqRnaRefComplete, seqRnaOtherComplete, //
          seqDna10xPriComplete, seqDna10xRefComplete, seqDna10xOtherComplete, //
          seqRna10xPriComplete, seqRna10xRefComplete, seqRna10xOtherComplete, //
          seqDnaPriRepeat, seqDnaRefRepeat, seqDnaOtherRepeat, //
          seqRnaPriRepeat, seqRnaRefRepeat, seqRnaOtherRepeat, //
          seqDna10xPriRepeat, seqDna10xRefRepeat, seqDna10xOtherRepeat, //
          seqRna10xPriRepeat, seqRna10xRefRepeat, seqRna10xOtherRepeat);
      countsByProject.put(project, countsForProj);
    }

    countsByProjectAsList = listifyCountsByProject(countsByProject);
  }

  private void addToCompletedOrRepeated(SampleDto dto, Set<SampleDto> repeatedList, Set<SampleDto> completedList) {
    if (completedList.contains(dto)) {
      repeatedList.add(dto);
    } else {
      completedList.add(dto);
    }
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
    if (type == null) throw new IllegalArgumentException("sample " + sample.getId() + " is missing tissue type");
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
