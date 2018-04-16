package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Predicate;

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

    public Count(CountLabel label, long value) {
      this.key = label.getKey();
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
      return CountLabel.get(key).compareTo(CountLabel.get(o.key)); // ordered based on the CountLabel declaration order
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

  private enum CountLabel {

    P_TISSUE("P Tissue"), R_TISSUE("R Tissue"), M_TISSUE("M Tissue"), X_TISSUE("X Tissue"), L_TISSUE("L Tissue"), //
    DNA_P_STOCK("DNA P Stock"), DNA_R_STOCK("DNA R Stock"), DNA_M_STOCK("DNA M Stock"), DNA_X_STOCK("DNA X Stock"), DNA_L_STOCK("DNA L Stock"), //
    RNA_P_STOCK("RNA P Stock"), RNA_R_STOCK("RNA R Stock"), RNA_M_STOCK("RNA M Stock"), RNA_X_STOCK("RNA X Stock"), RNA_L_STOCK("RNA L Stock"), //
    DNA_P_ALIQUOT("DNA P Aliq"), DNA_R_ALIQUOT("DNA R Aliq"), DNA_M_ALIQUOT("DNA M Aliq"), DNA_X_ALIQUOT("DNA X Aliq"), DNA_L_ALIQUOT("DNA L Aliq"), //
    RNA_P_ALIQUOT("RNA P Aliq"), RNA_R_ALIQUOT("RNA R Aliq"), RNA_M_ALIQUOT("RNA M Aliq"), RNA_X_ALIQUOT("RNA X Aliq"), RNA_L_ALIQUOT("RNA L Aliq"), //

    LIB_P_WG("WG P Lib"), LIB_R_WG("WG R Lib"), LIB_X_WG("WG X Lib"), LIB_M_WG("WG M Lib"), LIB_L_WG("WG L Lib"), //
    LIB_P_EX("EX P Lib"), LIB_R_EX("EX R Lib"), LIB_X_EX("EX X Lib"), LIB_M_EX("EX M Lib"), LIB_L_EX("EX L Lib"), //
    LIB_P_TS("TS P Lib"), LIB_R_TS("TS R Lib"), LIB_X_TS("TS X Lib"), LIB_M_TS("TS M Lib"), LIB_L_TS("TS L Lib"), //
    LIB_P_DNA_10X("DNA 10X P Lib"), LIB_R_DNA_10X("DNA 10X R Lib"), LIB_X_DNA_10X("DNA 10X X Lib"), LIB_M_DNA_10X("DNA 10X M Lib"), LIB_L_DNA_10X("DNA 10X L Lib"), //
    LIB_P_DNA("AS/CH/BS P Lib"), LIB_R_DNA("AS/CH/BS R Lib"), LIB_X_DNA("AS/CH/BS X Lib"), LIB_M_DNA("AS/CH/BS M Lib"), LIB_L_DNA("AS/CH/BS L Lib"), //
    LIB_P_NN("NN P Lib"), LIB_R_NN("NN R Lib"), LIB_X_NN("NN X Lib"), LIB_M_NN("NN M Lib"), LIB_L_NN("NN L Lib"), //
    LIB_P_RNA("RNA P Lib"), LIB_R_RNA("RNA R Lib"), LIB_X_RNA("RNA X Lib"), LIB_M_RNA("RNA M Lib"), LIB_L_RNA("RNA L Lib"), //
    LIB_P_RNA_10X("RNA 10X P Lib"), LIB_R_RNA_10X("RNA 10X R Lib"), LIB_X_RNA_10X("RNA 10X X Lib"), LIB_M_RNA_10X("RNA 10X M Lib"), LIB_L_RNA_10X("RNA 10X L Lib"), //

    LIB_NON_ILL("Non-Illumina Lib"), //

    LIB_P_WG_SEQD("WG P Seqd"), LIB_R_WG_SEQD("WG R Seqd"), LIB_X_WG_SEQD("WG X Seqd"), LIB_M_WG_SEQD("WG M Seqd"), LIB_L_WG_SEQD("WG L Seqd"), //
    LIB_P_EX_SEQD("EX P Seqd"), LIB_R_EX_SEQD("EX R Seqd"), LIB_X_EX_SEQD("EX X Seqd"), LIB_M_EX_SEQD("EX M Seqd"), LIB_L_EX_SEQD("EX L Seqd"), //
    LIB_P_TS_SEQD("TS P Seqd"), LIB_R_TS_SEQD("TS R Seqd"), LIB_X_TS_SEQD("TS X Seqd"), LIB_M_TS_SEQD("TS M Seqd"), LIB_L_TS_SEQD("TS L Seqd"), //
    LIB_P_DNA_10X_SEQD("DNA 10X P Seqd"), LIB_R_DNA_10X_SEQD("DNA 10X R Seqd"), LIB_X_DNA_10X_SEQD("DNA 10X X Seqd"), LIB_M_DNA_10X_SEQD("DNA 10X M Seqd"), LIB_L_DNA_10X_SEQD("DNA 10X L Seqd"), //
    LIB_P_DNA_SEQD("AS/CH/BS P Seqd"), LIB_R_DNA_SEQD("AS/CH/BS R Seqd"), LIB_X_DNA_SEQD("AS/CH/BS X Seqd"), LIB_M_DNA_SEQD("AS/CH/BS M Seqd"), LIB_L_DNA_SEQD("AS/CH/BS L Seqd"), //
    LIB_P_NN_SEQD("NN P Seqd"), LIB_R_NN_SEQD("NN R Seqd"), LIB_X_NN_SEQD("NN X Seqd"), LIB_M_NN_SEQD("NN M Seqd"), LIB_L_NN_SEQD("NN L Seqd"), //
    LIB_P_RNA_SEQD("RNA P Seqd"), LIB_R_RNA_SEQD("RNA R Seqd"), LIB_X_RNA_SEQD("RNA X Seqd"), LIB_M_RNA_SEQD("RNA M Seqd"), LIB_L_RNA_SEQD("RNA L Seqd"), //
    LIB_P_RNA_10X_SEQD("RNA 10X P Seqd"), LIB_R_RNA_10X_SEQD("RNA 10X R Seqd"), LIB_X_RNA_10X_SEQD("RNA 10X X Seqd"), LIB_M_RNA_10X_SEQD("RNA 10X M Seqd"), LIB_L_RNA_10X_SEQD("RNA 10X L Seqd"), //
    
    LIB_NON_ILL_SEQD("Non-Illumina Seqd");

    private final String key;
    private static final Map<String, CountLabel> lookup = new HashMap<>();
    static {
      for (CountLabel s : EnumSet.allOf(CountLabel.class)) {
        lookup.put(s.getKey(), s);
      }
    }

    CountLabel(String key) {
      this.key = key;
    }

    public static CountLabel get(String key) {
      return lookup.get(key);
    }

    public String getKey() {
      return key;
    }
  }

  private static final String COUNT_CATEGORY_TISSUE = "Tissue";
  private static final String COUNT_CATEGORY_DNA_STOCK = "DNA Stock";
  private static final String COUNT_CATEGORY_RNA_STOCK = "RNA Stock";
  private static final String COUNT_CATEGORY_DNA_ALIQUOT = "DNA Aliquot";
  private static final String COUNT_CATEGORY_RNA_ALIQUOT = "RNA Aliquot";
  private static final String COUNT_CATEGORY_WG_LIB = "WG Library";
  private static final String COUNT_CATEGORY_EX_LIB = "EX Library";
  private static final String COUNT_CATEGORY_TS_LIB = "TS Library";
  private static final String COUNT_CATEGORY_DNA_10X_LIB = "DNA 10X Library;";
  private static final String COUNT_CATEGORY_DNA_LIB = "DNA Library;";
  private static final String COUNT_CATEGORY_NN_LIB = "NN Library";
  private static final String COUNT_CATEGORY_RNA_LIB = "RNA Library";
  private static final String COUNT_CATEGORY_RNA_10X_LIB = "RNA 10X Library";
  private static final String COUNT_CATEGORY_NON_ILL_LIB = "Non-Illumina Library";
  private static final String COUNT_CATEGORY_WG_SEQD = "WG Lib Seqd";
  private static final String COUNT_CATEGORY_EX_SEQD = "EX Lib Seqd";
  private static final String COUNT_CATEGORY_TS_SEQD = "TS Lib Seqd";
  private static final String COUNT_CATEGORY_DNA_10X_SEQD = "DNA 10X Lib Seqd";
  private static final String COUNT_CATEGORY_DNA_SEQD = "DNA Lib Seqd";
  private static final String COUNT_CATEGORY_NN_SEQD = "NN Lib Seqd";
  private static final String COUNT_CATEGORY_RNA_SEQD = "RNA Lib Seqd";
  private static final String COUNT_CATEGORY_RNA_10X_SEQD = "RNA 10X Lib Seqd";
  private static final String COUNT_CATEGORY_NON_ILL_SEQD = "Non-Illumina Seqd";

  Set<String> projects = Sets.newHashSet(
      "APT2", "DCRT", "LEO", "LLDM", "MDT", "MNL", "MITO", "NBR", // proposed
      "APT", "ASHPC", "ASJD", "BLS", "BFS", "BTC", "CPTP", "CYT", //
      "DCIS", "DKT1", "DXRX", "DYS", "EECS", "FNS", "GCMS", "GECCO", "JAMLR", "JCK",
      "LCLC", "OVBM", "PCSI", "PRE", "RIPSQ", "ROVC", "SCRM", "TGL", "TPS", "TURN");

  List<String> ldO = Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_BS, LIBRARY_DESIGN_AS);

  protected int columnCount = 0;

  public static final String REPORT_NAME = "projects-status";

  // private static final List<ColumnDefinition> COLUMNS = {
  // for int i =
  // }


  private List<Map.Entry<String, Map<String, List<Count>>>> countsByProjectAsList; // String project, List<Count> all counts

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

    Map<String, Map<String, List<Count>>> countsByProject = new TreeMap<>(); // String project, List<Count>
                                                                                                             // all counts

    List<SampleDto> realSamples = new ArrayList<>(); // no identities
    List<SampleDto> libraries = new ArrayList<>();
    List<SampleDto> primaryTissues;
    List<SampleDto> referenceTissues;
    List<SampleDto> metastaticTissues;
    List<SampleDto> xenoTissues;
    List<SampleDto> miscTissues;
    List<SampleDto> dnaPStock;
    List<SampleDto> dnaRStock;
    List<SampleDto> dnaXStock;
    List<SampleDto> dnaMStock;
    List<SampleDto> dnaLStock;
    List<SampleDto> rnaPStock;
    List<SampleDto> rnaRStock;
    List<SampleDto> rnaXStock;
    List<SampleDto> rnaMStock;
    List<SampleDto> rnaLStock;
    List<SampleDto> dnaPAliquots;
    List<SampleDto> dnaRAliquots;
    List<SampleDto> dnaXAliquots;
    List<SampleDto> dnaMAliquots;
    List<SampleDto> dnaLAliquots;
    List<SampleDto> rnaPAliquots;
    List<SampleDto> rnaRAliquots;
    List<SampleDto> rnaXAliquots;
    List<SampleDto> rnaMAliquots;
    List<SampleDto> rnaLAliquots;

    List<SampleDto> wgPLibraries;
    List<SampleDto> wgRLibraries;
    List<SampleDto> wgXLibraries;
    List<SampleDto> wgMLibraries;
    List<SampleDto> wgLLibraries;
    List<SampleDto> exPLibraries;
    List<SampleDto> exRLibraries;
    List<SampleDto> exXLibraries;
    List<SampleDto> exMLibraries;
    List<SampleDto> exLLibraries;
    List<SampleDto> tsPLibraries;
    List<SampleDto> tsRLibraries;
    List<SampleDto> tsXLibraries;
    List<SampleDto> tsMLibraries;
    List<SampleDto> tsLLibraries;
    List<SampleDto> dna10xPLibraries;
    List<SampleDto> dna10xRLibraries;
    List<SampleDto> dna10xXLibraries;
    List<SampleDto> dna10xMLibraries;
    List<SampleDto> dna10xLLibraries;
    List<SampleDto> dnaOtherPLibraries;
    List<SampleDto> dnaOtherRLibraries;
    List<SampleDto> dnaOtherXLibraries;
    List<SampleDto> dnaOtherMLibraries;
    List<SampleDto> dnaOtherLLibraries;
    List<SampleDto> nnPLibraries;
    List<SampleDto> nnRLibraries;
    List<SampleDto> nnXLibraries;
    List<SampleDto> nnMLibraries;
    List<SampleDto> nnLLibraries;
    List<SampleDto> rnaPLibraries;
    List<SampleDto> rnaRLibraries;
    List<SampleDto> rnaXLibraries;
    List<SampleDto> rnaMLibraries;
    List<SampleDto> rnaLLibraries;
    List<SampleDto> rna10xPLibraries;
    List<SampleDto> rna10xRLibraries;
    List<SampleDto> rna10xXLibraries;
    List<SampleDto> rna10xMLibraries;
    List<SampleDto> rna10xLLibraries;
    List<SampleDto> nonIlluminaLibraries;

    Set<SampleDto> sequencedWgPLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgRLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgXLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgMLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgLLibraries = new HashSet<>();
    Set<SampleDto> sequencedExPLibraries = new HashSet<>();
    Set<SampleDto> sequencedExRLibraries = new HashSet<>();
    Set<SampleDto> sequencedExXLibraries = new HashSet<>();
    Set<SampleDto> sequencedExMLibraries = new HashSet<>();
    Set<SampleDto> sequencedExLLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsPLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsRLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsXLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsMLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsLLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xPLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xRLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xXLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xMLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xLLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaPLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaRLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaXLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaMLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaLLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnPLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnRLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnXLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnMLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnLLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaPLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaRLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaXLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaMLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaLLibraries = new HashSet<>();
    Set<SampleDto> sequencedRna10xPLibraries = new HashSet<>();
    Set<SampleDto> sequencedRna10xRLibraries = new HashSet<>();
    Set<SampleDto> sequencedRna10xXLibraries = new HashSet<>();
    Set<SampleDto> sequencedRna10xMLibraries = new HashSet<>();
    Set<SampleDto> sequencedRna10xLLibraries = new HashSet<>();
    Set<SampleDto> sequencedNonIlluminaLibraries = new HashSet<>();

    for (SampleDto sam : allSamples) {
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
    miscTissues = filter(realSamples, Arrays.asList(byTissueLike(), byLeftover(allSamplesById)));
    dnaPStock = filter(realSamples, Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaRStock = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaXStock = filter(realSamples, Arrays.asList(byXeno(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaMStock = filter(realSamples,
        Arrays.asList(byMetastatic(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaLStock = filter(realSamples, Arrays.asList(byLeftover(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    rnaPStock = filter(realSamples, Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaRStock = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaXStock = filter(realSamples, Arrays.asList(byXeno(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaMStock = filter(realSamples,
        Arrays.asList(byMetastatic(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaLStock = filter(realSamples, Arrays.asList(byLeftover(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    dnaPAliquots = filter(realSamples,
        Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    dnaRAliquots = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    dnaXAliquots = filter(realSamples,
        Arrays.asList(byXeno(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    dnaMAliquots = filter(realSamples,
        Arrays.asList(byMetastatic(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    dnaLAliquots = filter(realSamples,
        Arrays.asList(byLeftover(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    rnaPAliquots = filter(realSamples,
        Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaRAliquots = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaXAliquots = filter(realSamples,
        Arrays.asList(byXeno(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaMAliquots = filter(realSamples,
        Arrays.asList(byMetastatic(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaLAliquots = filter(realSamples,
        Arrays.asList(byLeftover(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    
    wgPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byPrimary(allSamplesById)));
    wgRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byReference(allSamplesById)));
    wgXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byXeno(allSamplesById)));
    wgMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byMetastatic(allSamplesById)));
    wgLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byLeftover(allSamplesById)));
    exPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byPrimary(allSamplesById)));
    exRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byReference(allSamplesById)));
    exXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byXeno(allSamplesById)));
    exMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byMetastatic(allSamplesById)));
    exLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byLeftover(allSamplesById)));
    tsPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byPrimary(allSamplesById)));
    tsRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byReference(allSamplesById)));
    tsXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byXeno(allSamplesById)));
    tsMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byMetastatic(allSamplesById)));
    tsLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byLeftover(allSamplesById)));
    dna10xPLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byPrimary(allSamplesById)));
    dna10xRLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byReference(allSamplesById)));
    dna10xXLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byXeno(allSamplesById)));
    dna10xMLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byMetastatic(allSamplesById)));
    dna10xLLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byLeftover(allSamplesById)));
    dnaOtherPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byPrimary(allSamplesById)));
    dnaOtherRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byReference(allSamplesById)));
    dnaOtherXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byXeno(allSamplesById)));
    dnaOtherMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byMetastatic(allSamplesById)));
    dnaOtherLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byLeftover(allSamplesById)));
    nnPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byPrimary(allSamplesById)));
    nnRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byReference(allSamplesById)));
    nnXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byXeno(allSamplesById)));
    nnMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byMetastatic(allSamplesById)));
    nnLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byLeftover(allSamplesById)));
    rnaPLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byPrimary(allSamplesById)));
    rnaRLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byReference(allSamplesById)));
    rnaXLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byXeno(allSamplesById)));
    rnaMLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byMetastatic(allSamplesById)));
    rnaLLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byLeftover(allSamplesById)));
    rna10xPLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), byPrimary(allSamplesById)));
    rna10xRLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), byReference(allSamplesById)));
    rna10xXLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), byXeno(allSamplesById)));
    rna10xMLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), byMetastatic(allSamplesById)));
    rna10xLLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), byLeftover(allSamplesById)));
    nonIlluminaLibraries = filter(libraries, Arrays.asList(byNonIlluminaLibrary()));
    
    // track which libraries where sequenced
    for (RunDto run : allRuns) {
      // ignore Running, Unknown, Stopped runs
      if (!RUN_FAILED.equals(run.getState()) && !RUN_COMPLETED.equals(run.getState())) continue;
      if (run.getPositions() == null) continue;
      for (RunDtoPosition lane : run.getPositions()) {
        if (lane.getSamples() == null) continue;
        for (RunDtoSample sam : lane.getSamples()) {
          // want only completed libraries for now
          if (RUN_FAILED.equals(run.getState())) continue;
          SampleDto dilution = allSamplesById.get(sam.getId());
          if (isNonIlluminaLibrary(dilution)) {
            sequencedNonIlluminaLibraries.add(dilution);
            continue;
          }
          String code = getUpstreamAttribute(ATTR_SOURCE_TEMPLATE_TYPE, dilution, allSamplesById);
          if (code == null) throw new IllegalArgumentException("Dilution " + dilution.getName() + " has no library design code in hierarchy");
          if (isRnaLibrary(dilution, allSamplesById)) {
            if (is10XLibrary(dilution, allSamplesById)) {
              // RNA, 10X
              assignLibraryByTissueType(dilution, allSamplesById, sequencedRna10xPLibraries, sequencedRna10xRLibraries,
                  sequencedRna10xXLibraries, sequencedRna10xMLibraries, sequencedRna10xLLibraries);
            } else {
              // RNA, no 10X
              assignLibraryByTissueType(dilution, allSamplesById, sequencedRnaPLibraries, sequencedRnaRLibraries, sequencedRnaXLibraries,
                  sequencedRnaMLibraries, sequencedRnaLLibraries);
            }
          } else {
            if (is10XLibrary(dilution, allSamplesById)) {
              // DNA, 10X
              assignLibraryByTissueType(dilution, allSamplesById, sequencedDna10xPLibraries, sequencedDna10xRLibraries,
                  sequencedDna10xXLibraries, sequencedDna10xMLibraries, sequencedDna10xLLibraries);
            } else if (LIBRARY_DESIGN_WG.equals(code)) {
              // DNA, no 10X, WG
              assignLibraryByTissueType(dilution, allSamplesById, sequencedWgPLibraries, sequencedWgRLibraries, sequencedWgXLibraries, sequencedWgMLibraries, sequencedWgLLibraries);
            } else if (LIBRARY_DESIGN_EX.equals(code)) {
              // DNA, no 10X, EX
              assignLibraryByTissueType(dilution, allSamplesById, sequencedExPLibraries, sequencedExRLibraries, sequencedExXLibraries,
                  sequencedExMLibraries, sequencedExLLibraries);
            } else if (LIBRARY_DESIGN_TS.equals(code)) {
              // DNA, no 10X, TS
              assignLibraryByTissueType(dilution, allSamplesById, sequencedTsPLibraries, sequencedTsRLibraries, sequencedTsXLibraries,
                  sequencedTsMLibraries, sequencedTsLLibraries);
            } else if (LIBRARY_DESIGN_NN.equals(code)) {
              // DNA, no 10X, NN
              assignLibraryByTissueType(dilution, allSamplesById, sequencedNnPLibraries, sequencedNnRLibraries, sequencedNnXLibraries, sequencedNnMLibraries, sequencedNnLLibraries);
            } else if (ldO.contains(code)) {
              // DNA, no 10X, AS/CH/BS
              assignLibraryByTissueType(dilution, allSamplesById, sequencedDnaPLibraries, sequencedDnaRLibraries, sequencedDnaXLibraries,
                  sequencedDnaMLibraries, sequencedDnaLLibraries);
            } else {
              System.out.println("Unexpected library design code " + code + " was found on dilution " + dilution.getId());
            }
          }
        }
      }
    }

    for (String project : projects) {
      Count priTissue = new Count(CountLabel.P_TISSUE, filter(primaryTissues, byProject(project)).size());
      Count refTissue = new Count(CountLabel.R_TISSUE, filter(referenceTissues, byProject(project)).size());
      Count metTissue = new Count(CountLabel.M_TISSUE, filter(metastaticTissues, byProject(project)).size());
      Count xenoTissue = new Count(CountLabel.X_TISSUE, filter(xenoTissues, byProject(project)).size());
      Count otherTissue = new Count(CountLabel.L_TISSUE, filter(miscTissues, byProject(project)).size());
      Count dnaPriStock = new Count(CountLabel.DNA_P_STOCK, filter(dnaPStock, byProject(project)).size());
      Count dnaRefStock = new Count(CountLabel.DNA_R_STOCK, filter(dnaRStock, byProject(project)).size());
      Count dnaXenoStock = new Count(CountLabel.DNA_X_STOCK, filter(dnaXStock, byProject(project)).size());
      Count dnaMetsStock = new Count(CountLabel.DNA_M_STOCK, filter(dnaMStock, byProject(project)).size());
      Count dnaLeftoStock = new Count(CountLabel.DNA_L_STOCK, filter(dnaLStock, byProject(project)).size());
      Count rnaPriStock = new Count(CountLabel.RNA_P_STOCK, filter(rnaPStock, byProject(project)).size());
      Count rnaRefStock = new Count(CountLabel.RNA_R_STOCK, filter(rnaRStock, byProject(project)).size());
      Count rnaXenoStock = new Count(CountLabel.RNA_L_STOCK, filter(rnaXStock, byProject(project)).size());
      Count rnaMetsStock = new Count(CountLabel.RNA_L_STOCK, filter(rnaMStock, byProject(project)).size());
      Count rnaLeftoStock = new Count(CountLabel.RNA_L_STOCK, filter(rnaLStock, byProject(project)).size());
      Count dnaPriAliq = new Count(CountLabel.DNA_P_ALIQUOT, filter(dnaPAliquots, byProject(project)).size());
      Count dnaRefAliq = new Count(CountLabel.DNA_R_ALIQUOT, filter(dnaRAliquots, byProject(project)).size());
      Count dnaXenoAliq = new Count(CountLabel.DNA_L_ALIQUOT, filter(dnaXAliquots, byProject(project)).size());
      Count dnaMetsAliq = new Count(CountLabel.DNA_L_ALIQUOT, filter(dnaMAliquots, byProject(project)).size());
      Count dnaLeftoAliq = new Count(CountLabel.DNA_L_ALIQUOT, filter(dnaLAliquots, byProject(project)).size());
      Count rnaPriAliq = new Count(CountLabel.RNA_P_ALIQUOT, filter(rnaPAliquots, byProject(project)).size());
      Count rnaRefAliq = new Count(CountLabel.RNA_R_ALIQUOT, filter(rnaRAliquots, byProject(project)).size());
      Count rnaXenoAliq = new Count(CountLabel.RNA_L_ALIQUOT, filter(rnaXAliquots, byProject(project)).size());
      Count rnaMetsAliq = new Count(CountLabel.RNA_L_ALIQUOT, filter(rnaMAliquots, byProject(project)).size());
      Count rnaLeftoAliq = new Count(CountLabel.RNA_L_ALIQUOT, filter(rnaLAliquots, byProject(project)).size());

      Count wgPriLib = new Count(CountLabel.LIB_P_WG, filter(wgPLibraries, byProject(project)).size());
      Count wgRefLib = new Count(CountLabel.LIB_R_WG, filter(wgRLibraries, byProject(project)).size());
      Count wgXenoLib = new Count(CountLabel.LIB_X_WG, filter(wgXLibraries, byProject(project)).size());
      Count wgMetsLib = new Count(CountLabel.LIB_M_WG, filter(wgMLibraries, byProject(project)).size());
      Count wgLeftoLib = new Count(CountLabel.LIB_L_WG, filter(wgLLibraries, byProject(project)).size());
      Count exPriLib = new Count(CountLabel.LIB_P_EX, filter(exPLibraries, byProject(project)).size());
      Count exRefLib = new Count(CountLabel.LIB_R_EX, filter(exRLibraries, byProject(project)).size());
      Count exXenoLib = new Count(CountLabel.LIB_X_EX, filter(exXLibraries, byProject(project)).size());
      Count exMetsLib = new Count(CountLabel.LIB_M_EX, filter(exMLibraries, byProject(project)).size());
      Count exLeftoLib = new Count(CountLabel.LIB_L_EX, filter(exLLibraries, byProject(project)).size());
      Count tsPriLib = new Count(CountLabel.LIB_P_TS, filter(tsPLibraries, byProject(project)).size());
      Count tsRefLib = new Count(CountLabel.LIB_R_TS, filter(tsRLibraries, byProject(project)).size());
      Count tsXenoLib = new Count(CountLabel.LIB_X_TS, filter(tsXLibraries, byProject(project)).size());
      Count tsMetsLib = new Count(CountLabel.LIB_M_TS, filter(tsMLibraries, byProject(project)).size());
      Count tsLeftoLib = new Count(CountLabel.LIB_L_TS, filter(tsLLibraries, byProject(project)).size());
      Count dna10xPriLib = new Count(CountLabel.LIB_P_DNA_10X, filter(dna10xPLibraries, byProject(project)).size());
      Count dna10xRefLib = new Count(CountLabel.LIB_R_DNA_10X, filter(dna10xRLibraries, byProject(project)).size());
      Count dna10xXenoLib = new Count(CountLabel.LIB_X_DNA_10X, filter(dna10xXLibraries, byProject(project)).size());
      Count dna10xMetsLib = new Count(CountLabel.LIB_M_DNA_10X, filter(dna10xMLibraries, byProject(project)).size());
      Count dna10xLeftoLib = new Count(CountLabel.LIB_L_DNA_10X, filter(dna10xLLibraries, byProject(project)).size());
      Count dnaMiscPriLib = new Count(CountLabel.LIB_P_DNA, filter(dnaOtherPLibraries, byProject(project)).size());
      Count dnaMiscRefLib = new Count(CountLabel.LIB_R_DNA, filter(dnaOtherRLibraries, byProject(project)).size());
      Count dnaMiscXenoLib = new Count(CountLabel.LIB_X_DNA, filter(dnaOtherXLibraries, byProject(project)).size());
      Count dnaMiscMetsLib = new Count(CountLabel.LIB_M_DNA, filter(dnaOtherMLibraries, byProject(project)).size());
      Count dnaMiscLeftoLib = new Count(CountLabel.LIB_L_DNA, filter(dnaOtherLLibraries, byProject(project)).size());
      Count nnPriLib = new Count(CountLabel.LIB_P_NN, filter(nnPLibraries, byProject(project)).size());
      Count nnRefLib = new Count(CountLabel.LIB_R_NN, filter(nnRLibraries, byProject(project)).size());
      Count nnXenoLib = new Count(CountLabel.LIB_X_NN, filter(nnXLibraries, byProject(project)).size());
      Count nnMetsLib = new Count(CountLabel.LIB_M_NN, filter(nnMLibraries, byProject(project)).size());
      Count nnLeftoLib = new Count(CountLabel.LIB_L_NN, filter(nnLLibraries, byProject(project)).size());
      Count rnaPriLib = new Count(CountLabel.LIB_P_RNA, filter(rnaPLibraries, byProject(project)).size());
      Count rnaRefLib = new Count(CountLabel.LIB_R_RNA, filter(rnaRLibraries, byProject(project)).size());
      Count rnaXenoLib = new Count(CountLabel.LIB_X_RNA, filter(rnaXLibraries, byProject(project)).size());
      Count rnaMetsLib = new Count(CountLabel.LIB_M_RNA, filter(rnaMLibraries, byProject(project)).size());
      Count rnaLeftoLib = new Count(CountLabel.LIB_L_RNA, filter(rnaLLibraries, byProject(project)).size());
      Count rna10xPriLib = new Count(CountLabel.LIB_P_RNA_10X, filter(rna10xPLibraries, byProject(project)).size());
      Count rna10xRefLib = new Count(CountLabel.LIB_R_RNA_10X, filter(rna10xRLibraries, byProject(project)).size());
      Count rna10xXenoLib = new Count(CountLabel.LIB_X_RNA_10X, filter(rna10xXLibraries, byProject(project)).size());
      Count rna10xMetsLib = new Count(CountLabel.LIB_M_RNA_10X, filter(rna10xMLibraries, byProject(project)).size());
      Count rna10xLeftoLib = new Count(CountLabel.LIB_L_RNA_10X, filter(rna10xLLibraries, byProject(project)).size());
      Count nonIllLib = new Count(CountLabel.LIB_NON_ILL, filter(nonIlluminaLibraries, byProject(project)).size());

      Count wgPSeqd = new Count(CountLabel.LIB_P_WG_SEQD, filter(sequencedWgPLibraries, byProject(project)).size());
      Count wgRSeqd = new Count(CountLabel.LIB_R_WG_SEQD, filter(sequencedWgRLibraries, byProject(project)).size());
      Count wgXSeqd = new Count(CountLabel.LIB_X_WG_SEQD, filter(sequencedWgXLibraries, byProject(project)).size());
      Count wgMSeqd = new Count(CountLabel.LIB_M_WG_SEQD, filter(sequencedWgMLibraries, byProject(project)).size());
      Count wgLSeqd = new Count(CountLabel.LIB_L_WG_SEQD, filter(sequencedWgLLibraries, byProject(project)).size());
      Count exPSeqd = new Count(CountLabel.LIB_P_EX_SEQD, filter(sequencedExPLibraries, byProject(project)).size());
      Count exRSeqd = new Count(CountLabel.LIB_R_EX_SEQD, filter(sequencedExRLibraries, byProject(project)).size());
      Count exXSeqd = new Count(CountLabel.LIB_X_EX_SEQD, filter(sequencedExXLibraries, byProject(project)).size());
      Count exMSeqd = new Count(CountLabel.LIB_M_EX_SEQD, filter(sequencedExMLibraries, byProject(project)).size());
      Count exLSeqd = new Count(CountLabel.LIB_L_EX_SEQD, filter(sequencedExLLibraries, byProject(project)).size());
      Count tsPSeqd = new Count(CountLabel.LIB_P_TS_SEQD, filter(sequencedTsPLibraries, byProject(project)).size());
      Count tsRSeqd = new Count(CountLabel.LIB_R_TS_SEQD, filter(sequencedTsRLibraries, byProject(project)).size());
      Count tsXSeqd = new Count(CountLabel.LIB_X_TS_SEQD, filter(sequencedTsXLibraries, byProject(project)).size());
      Count tsMSeqd = new Count(CountLabel.LIB_M_TS_SEQD, filter(sequencedTsMLibraries, byProject(project)).size());
      Count tsLSeqd = new Count(CountLabel.LIB_L_TS_SEQD, filter(sequencedTsLLibraries, byProject(project)).size());
      Count nnPSeqd = new Count(CountLabel.LIB_P_NN_SEQD, filter(sequencedNnPLibraries, byProject(project)).size());
      Count nnRSeqd = new Count(CountLabel.LIB_R_NN_SEQD, filter(sequencedNnRLibraries, byProject(project)).size());
      Count nnXSeqd = new Count(CountLabel.LIB_X_NN_SEQD, filter(sequencedNnXLibraries, byProject(project)).size());
      Count nnMSeqd = new Count(CountLabel.LIB_M_NN_SEQD, filter(sequencedNnMLibraries, byProject(project)).size());
      Count nnLSeqd = new Count(CountLabel.LIB_L_NN_SEQD, filter(sequencedNnLLibraries, byProject(project)).size());
      Count dnaPSeqd = new Count(CountLabel.LIB_P_DNA_SEQD, filter(sequencedDnaPLibraries, byProject(project)).size());
      Count dnaRSeqd = new Count(CountLabel.LIB_R_DNA_SEQD, filter(sequencedDnaRLibraries, byProject(project)).size());
      Count dnaXSeqd = new Count(CountLabel.LIB_X_DNA_SEQD, filter(sequencedDnaXLibraries, byProject(project)).size());
      Count dnaMSeqd = new Count(CountLabel.LIB_M_DNA_SEQD, filter(sequencedDnaMLibraries, byProject(project)).size());
      Count dnaLSeqd = new Count(CountLabel.LIB_L_DNA_SEQD, filter(sequencedDnaLLibraries, byProject(project)).size());
      Count dna10xPSeqd = new Count(CountLabel.LIB_P_DNA_10X_SEQD, filter(sequencedDna10xPLibraries, byProject(project)).size());
      Count dna10xRSeqd = new Count(CountLabel.LIB_R_DNA_10X_SEQD, filter(sequencedDna10xRLibraries, byProject(project)).size());
      Count dna10xXSeqd = new Count(CountLabel.LIB_X_DNA_10X_SEQD, filter(sequencedDna10xXLibraries, byProject(project)).size());
      Count dna10xMSeqd = new Count(CountLabel.LIB_M_DNA_10X_SEQD, filter(sequencedDna10xMLibraries, byProject(project)).size());
      Count dna10xLSeqd = new Count(CountLabel.LIB_L_DNA_10X_SEQD, filter(sequencedDna10xLLibraries, byProject(project)).size());

      Count rnaPSeqd = new Count(CountLabel.LIB_P_RNA_SEQD, filter(sequencedRnaPLibraries, byProject(project)).size());
      Count rnaRSeqd = new Count(CountLabel.LIB_R_RNA_SEQD, filter(sequencedRnaRLibraries, byProject(project)).size());
      Count rnaXSeqd = new Count(CountLabel.LIB_X_RNA_SEQD, filter(sequencedRnaXLibraries, byProject(project)).size());
      Count rnaMSeqd = new Count(CountLabel.LIB_M_RNA_SEQD, filter(sequencedRnaMLibraries, byProject(project)).size());
      Count rnaLSeqd = new Count(CountLabel.LIB_L_RNA_SEQD, filter(sequencedRnaLLibraries, byProject(project)).size());
      Count rna10xPSeqd = new Count(CountLabel.LIB_P_RNA_10X_SEQD, filter(sequencedRna10xPLibraries, byProject(project)).size());
      Count rna10xRSeqd = new Count(CountLabel.LIB_R_RNA_10X_SEQD, filter(sequencedRna10xRLibraries, byProject(project)).size());
      Count rna10xXSeqd = new Count(CountLabel.LIB_X_RNA_10X_SEQD, filter(sequencedRna10xXLibraries, byProject(project)).size());
      Count rna10xMSeqd = new Count(CountLabel.LIB_M_RNA_10X_SEQD, filter(sequencedRna10xMLibraries, byProject(project)).size());
      Count rna10xLSeqd = new Count(CountLabel.LIB_L_RNA_10X_SEQD, filter(sequencedRna10xLLibraries, byProject(project)).size());
      
      Count nonIllSeqd = new Count(CountLabel.LIB_NON_ILL_SEQD, filter(sequencedNonIlluminaLibraries, byProject(project)).size());

      Map<String, List<Count>> categoryCounts = new LinkedHashMap<>();
      categoryCounts.put(COUNT_CATEGORY_TISSUE, new ArrayList<>(Arrays.asList(priTissue, refTissue, metTissue, xenoTissue, otherTissue)));
      categoryCounts.put(COUNT_CATEGORY_DNA_STOCK,
          new ArrayList<>(Arrays.asList(dnaPriStock, dnaRefStock, dnaXenoStock, dnaMetsStock, dnaLeftoStock)));
      categoryCounts.put(COUNT_CATEGORY_RNA_STOCK,
          new ArrayList<>(Arrays.asList(rnaPriStock, rnaRefStock, rnaXenoStock, rnaMetsStock, rnaLeftoStock)));
      categoryCounts.put(COUNT_CATEGORY_DNA_ALIQUOT,
          new ArrayList<>(Arrays.asList(dnaPriAliq, dnaRefAliq, dnaXenoAliq, dnaMetsAliq, dnaLeftoAliq)));
      categoryCounts.put(COUNT_CATEGORY_RNA_ALIQUOT,
          new ArrayList<>(Arrays.asList(rnaPriAliq, rnaRefAliq, rnaXenoAliq, rnaMetsAliq, rnaLeftoAliq)));
      categoryCounts.put(COUNT_CATEGORY_WG_LIB, new ArrayList<>(Arrays.asList(wgPriLib, wgRefLib, wgXenoLib, wgMetsLib, wgLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_EX_LIB, new ArrayList<>(Arrays.asList(exPriLib, exRefLib, exXenoLib, exMetsLib, exLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_TS_LIB, new ArrayList<>(Arrays.asList(tsPriLib, tsRefLib, tsXenoLib, tsMetsLib, tsLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_DNA_LIB,
          new ArrayList<>(Arrays.asList(dnaMiscPriLib, dnaMiscRefLib, dnaMiscXenoLib, dnaMiscMetsLib, dnaMiscLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_DNA_10X_LIB,
          new ArrayList<>(Arrays.asList(dna10xPriLib, dna10xRefLib, dna10xXenoLib, dna10xMetsLib, dna10xLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_NN_LIB, new ArrayList<>(Arrays.asList(nnPriLib, nnRefLib, nnXenoLib, nnMetsLib, nnLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_RNA_LIB, new ArrayList<>(Arrays.asList(rnaPriLib, rnaRefLib, rnaXenoLib, rnaMetsLib, rnaLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_RNA_10X_LIB,
          new ArrayList<>(Arrays.asList(rna10xPriLib, rna10xRefLib, rna10xXenoLib, rna10xMetsLib, rna10xLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_NON_ILL_LIB, new ArrayList<>(Arrays.asList(nonIllLib)));
      categoryCounts.put(COUNT_CATEGORY_WG_SEQD, new ArrayList<>(Arrays.asList(wgPSeqd, wgRSeqd, wgXSeqd, wgMSeqd, wgLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_EX_SEQD, new ArrayList<>(Arrays.asList(exPSeqd, exRSeqd, exXSeqd, exMSeqd, exLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_TS_SEQD, new ArrayList<>(Arrays.asList(tsPSeqd, tsRSeqd, tsXSeqd, tsMSeqd, tsLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_DNA_SEQD, new ArrayList<>(Arrays.asList(dnaPSeqd, dnaRSeqd, dnaXSeqd, dnaMSeqd, dnaLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_DNA_10X_SEQD,
          new ArrayList<>(Arrays.asList(dna10xPSeqd, dna10xRSeqd, dna10xXSeqd, dna10xMSeqd, dna10xLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_NN_SEQD, new ArrayList<>(Arrays.asList(nnPSeqd, nnRSeqd, nnXSeqd, nnMSeqd, nnLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_RNA_SEQD, new ArrayList<>(Arrays.asList(rnaPSeqd, rnaRSeqd, rnaXSeqd, rnaMSeqd, rnaLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_RNA_10X_SEQD,
          new ArrayList<>(Arrays.asList(rna10xPSeqd, rna10xRSeqd, rna10xXSeqd, rna10xMSeqd, rna10xLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_NON_ILL_SEQD, new ArrayList<>(Arrays.asList(nonIllSeqd)));
      removeEmpties(categoryCounts);
      maybeUpdateColumnCount(categoryCounts);
      countsByProject.put(project, categoryCounts);

    }

    countsByProjectAsList = listifyCountsByProject(countsByProject);
  }

  private void removeEmpties(Map<String, List<Count>> categoryCounts) {
    for (List<Count> countList : categoryCounts.values()) {
      countList.removeIf(count -> count.getValue() == 0);
    }
    categoryCounts.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
  }

  private int countColumns(Map<String, List<Count>> categoryCounts) {
    int columns = 0;
    for (Map.Entry<String, List<Count>> categoryAndCounts : categoryCounts.entrySet()) {
      columns++; // one for the blank cell that demarcates each count category
      columns += categoryAndCounts.getValue().size(); // one column per count label
    }
    return columns;
  }

  /**
   * The number of columns for this report depends on what is in the report.
   * We need to count the number of columns required for each project, and select the largest of these.
   * 
   * @param categoriesAndCounts Map of categories and counts for one project
   */
  private void maybeUpdateColumnCount(Map<String, List<Count>> categoriesAndCounts) {
    int columnsForProject = countColumns(categoriesAndCounts);
    if (columnsForProject > columnCount) columnCount = columnsForProject;
  }

  private void assignLibraryByTissueType(SampleDto dilution, Map<String, SampleDto> allSamples, Set<SampleDto> pSeqd, Set<SampleDto> rSeqd,
      Set<SampleDto> xSeqd, Set<SampleDto> mSeqd, Set<SampleDto> lSeqd) {
    String type = getUpstreamAttribute(ATTR_TISSUE_TYPE, dilution, allSamples);
    switch (type) {
    case "P":
    case "S":
    case "O":
    case "A":
      pSeqd.add(dilution);
      break;
    case "R":
      rSeqd.add(dilution);
      break;
    case "X":
      xSeqd.add(dilution);
      break;
    case "M":
      mSeqd.add(dilution);
      break;
    default:
      lSeqd.add(dilution);
      break;
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
      List<String> reference = Arrays.asList("R");
      return tissueTypeMatches(dto, reference, allSamples);
    };
  }

  private Predicate<SampleDto> byMetastatic(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> metastatic = Arrays.asList("M");
      return tissueTypeMatches(dto, metastatic, allSamples);
    };
  }

  private Predicate<SampleDto> byXeno(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> xenograft = Arrays.asList("X");
      return tissueTypeMatches(dto, xenograft, allSamples);
    };
  }

  private Predicate<SampleDto> byLeftover(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> leftovers = Arrays.asList("C", "U", "T", "E", "n", "B", "F");
      return tissueTypeMatches(dto, leftovers, allSamples);
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

  private Predicate<SampleDto> by10XLibrary(Map<String, SampleDto> allSamplesById) {
    return dto -> is10XLibrary(dto, allSamplesById);
  }

  private Predicate<SampleDto> byLibraryDesignCodes(List<String> libraryDesignCodes) {
    return dto -> libraryDesignCodes.contains(getAttribute(ATTR_SOURCE_TEMPLATE_TYPE, dto));
  }

  private boolean isNonIlluminaLibrary(SampleDto dilution) {
    if (dilution.getSampleType() == null) throw new IllegalArgumentException("Dilution " + dilution.getName() + " has no sample_type");
    return !dilution.getSampleType().contains("Illumina"); // note the negation here
  }

  private Predicate<SampleDto> byNonIlluminaLibrary() {
    return dto -> isNonIlluminaLibrary(dto);
  }

  static final List<Map.Entry<String, Map<String, List<Count>>>> listifyCountsByProject(
      Map<String, Map<String, List<Count>>> countsByProject) {
    // need to convert it to a list, because getRow() takes an index and the treemap doesn't yet have one of those
    return new ArrayList<>(countsByProject.entrySet());
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    List<ColumnDefinition> cols = new ArrayList<>();
    for (int i = 0; i < columnCount; i++) {
      cols.add(new ColumnDefinition(""));
    }
    return cols;
  }

  @Override
  protected int getRowCount() {
    return countsByProjectAsList.size() * 4 - 3;
  }

  protected int listIndex = 0;

  @Override
  protected String[] getRow(int rowNum) {
    Map.Entry<String, Map<String, List<Count>>> projectCounts = countsByProjectAsList.get(listIndex);
    if (rowNum % 4 == 0) {
      listIndex++;
      return makeBlankRow();
    } else if (rowNum % 4 == 1) {
      return makeCategoriesRow(projectCounts);
    } else if (rowNum % 4 == 2) {
      return makeCountLabelsRow(projectCounts);
    } else {
      return makeCountsRow(projectCounts);
    }
  }

  private String[] makeCategoriesRow(Map.Entry<String, Map<String, List<Count>>> projectCounts) {
    String[] row = makeBlankRow();
    int i = -1;
    row[++i] = projectCounts.getKey();
    for (Map.Entry<String, List<Count>> categoryAndCounts : projectCounts.getValue().entrySet()) {
      row[++i] = categoryAndCounts.getKey(); // category label
      i += categoryAndCounts.getValue().size(); // increment by number distinct counts in each category. This will result in one blank
                                                // column at the end of each category.
    }
    return row;
  }

  private String[] makeCountLabelsRow(Map.Entry<String, Map<String, List<Count>>> projectCounts) {
    String[] row = makeBlankRow();
    int i = 0; // first column is blank
    for (Map.Entry<String, List<Count>> categoryAndCounts : projectCounts.getValue().entrySet()) {
      for (Count count : categoryAndCounts.getValue()) {
        row[++i] = count.getKey();
      }
      i++; // blank column at end of category
    }
    return row;
  }

  private String[] makeCountsRow(Map.Entry<String, Map<String, List<Count>>> projectCounts) {
    String[] row = makeBlankRow();
    int i = 0; // first column is blank
    for (Map.Entry<String, List<Count>> categoryAndCounts : projectCounts.getValue().entrySet()) {
      for (Count count : categoryAndCounts.getValue()) {
        row[++i] = String.valueOf(count.getValue());
      }
      i++; // blank column at end of category
    }
    return row;
  }

  private String[] makeBlankRow() {
    String[] row = new String[columnCount];
    Arrays.fill(row, "");
    return row;
  }

}
