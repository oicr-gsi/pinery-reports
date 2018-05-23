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

    P_TISSUE("P Tissue"), R_TISSUE("R Tissue"), M_TISSUE("M Tissue"), O_TISSUE("O Tissue"), X_TISSUE("X Tissue"), L_TISSUE("L Tissue"), //
    DNA_P_STOCK("DNA P Stock"), DNA_R_STOCK("DNA R Stock"), DNA_O_STOCK("DNA O Stock"), DNA_M_STOCK("DNA M Stock"), DNA_X_STOCK("DNA X Stock"), DNA_L_STOCK("DNA L Stock"), //
    RNA_P_STOCK("RNA P Stock"), RNA_R_STOCK("RNA R Stock"), RNA_O_STOCK("RNA O Stock"), RNA_M_STOCK("RNA M Stock"), RNA_X_STOCK("RNA X Stock"), RNA_L_STOCK("RNA L Stock"), //
    DNA_P_ALIQUOT("DNA P Aliq"), DNA_R_ALIQUOT("DNA R Aliq"), DNA_O_ALIQUOT("DNA O Aliquot"), DNA_M_ALIQUOT("DNA M Aliq"), DNA_X_ALIQUOT("DNA X Aliq"), DNA_L_ALIQUOT("DNA L Aliq"), //
    RNA_P_ALIQUOT("RNA P Aliq"), RNA_R_ALIQUOT("RNA R Aliq"), RNA_O_ALIQUOT("RNA O Aliquot"), RNA_M_ALIQUOT("RNA M Aliq"), RNA_X_ALIQUOT("RNA X Aliq"), RNA_L_ALIQUOT("RNA L Aliq"), //

    LIB_P_WG("WG P Lib"), LIB_R_WG("WG R Lib"), LIB_O_WG("WG O Lib"), LIB_X_WG("WG X Lib"), LIB_M_WG("WG M Lib"), LIB_L_WG("WG L Lib"), //
    LIB_P_EX("EX P Lib"), LIB_R_EX("EX R Lib"), LIB_O_EX("EX O Lib"), LIB_X_EX("EX X Lib"), LIB_M_EX("EX M Lib"), LIB_L_EX("EX L Lib"), //
    LIB_P_TS("TS P Lib"), LIB_R_TS("TS R Lib"), LIB_O_TS("TS O Lib"), LIB_X_TS("TS X Lib"), LIB_M_TS("TS M Lib"), LIB_L_TS("TS L Lib"), //
    LIB_P_DNA_10X("DNA 10X P Lib"), LIB_R_DNA_10X("DNA 10X R Lib"), LIB_O_DNA_10X("DNA 10X O Lib"), LIB_X_DNA_10X("DNA 10X X Lib"), LIB_M_DNA_10X("DNA 10X M Lib"), LIB_L_DNA_10X("DNA 10X L Lib"), //
    LIB_P_DNA("AS/CH/BS P Lib"), LIB_R_DNA("AS/CH/BS R Lib"), LIB_O_DNA("AS/CH/BS O Lib"), LIB_X_DNA("AS/CH/BS X Lib"), LIB_M_DNA("AS/CH/BS M Lib"), LIB_L_DNA("AS/CH/BS L Lib"), //
    LIB_P_NN("NN P Lib"), LIB_R_NN("NN R Lib"), LIB_O_NN("NN O Lib"), LIB_X_NN("NN X Lib"), LIB_M_NN("NN M Lib"), LIB_L_NN("NN L Lib"), //
    LIB_P_RNA("RNA P Lib"), LIB_R_RNA("RNA R Lib"), LIB_O_RNA("RNA O Lib"), LIB_X_RNA("RNA X Lib"), LIB_M_RNA("RNA M Lib"), LIB_L_RNA("RNA L Lib"), //
    LIB_P_RNA_10X("RNA 10X P Lib"), LIB_R_RNA_10X("RNA 10X R Lib"), LIB_O_RNA_10X("RNA 10X O Lib"), LIB_X_RNA_10X("RNA 10X X Lib"), LIB_M_RNA_10X("RNA 10X M Lib"), LIB_L_RNA_10X("RNA 10X L Lib"), //

    LIB_NON_ILL("Non-Illumina Lib"), //

    LIB_P_WG_SEQD("WG P Seqd"), LIB_R_WG_SEQD("WG R Seqd"), LIB_O_WG_SEQD("WG O Seqd"), LIB_X_WG_SEQD("WG X Seqd"), LIB_M_WG_SEQD("WG M Seqd"), LIB_L_WG_SEQD("WG L Seqd"), //
    LIB_P_EX_SEQD("EX P Seqd"), LIB_R_EX_SEQD("EX R Seqd"), LIB_O_EX_SEQD("EX O Seqd"), LIB_X_EX_SEQD("EX X Seqd"), LIB_M_EX_SEQD("EX M Seqd"), LIB_L_EX_SEQD("EX L Seqd"), //
    LIB_P_TS_SEQD("TS P Seqd"), LIB_R_TS_SEQD("TS R Seqd"), LIB_O_TS_SEQD("TS O Seqd"), LIB_X_TS_SEQD("TS X Seqd"), LIB_M_TS_SEQD("TS M Seqd"), LIB_L_TS_SEQD("TS L Seqd"), //
    LIB_P_DNA_10X_SEQD("DNA 10X P Seqd"), LIB_R_DNA_10X_SEQD("DNA 10X R Seqd"), LIB_O_DNA_10X_SEQD("DNA 10X O Seqd"), LIB_X_DNA_10X_SEQD("DNA 10X X Seqd"), LIB_M_DNA_10X_SEQD("DNA 10X M Seqd"), LIB_L_DNA_10X_SEQD("DNA 10X L Seqd"), //
    LIB_P_DNA_SEQD("AS/CH/BS P Seqd"), LIB_R_DNA_SEQD("AS/CH/BS R Seqd"), LIB_O_DNA_SEQD("AS/CH/BS/O Seqd"), LIB_X_DNA_SEQD("AS/CH/BS X Seqd"), LIB_M_DNA_SEQD("AS/CH/BS M Seqd"), LIB_L_DNA_SEQD("AS/CH/BS L Seqd"), //
    LIB_P_NN_SEQD("NN P Seqd"), LIB_R_NN_SEQD("NN R Seqd"), LIB_O_NN_SEQD("NN O Seqd"), LIB_X_NN_SEQD("NN X Seqd"), LIB_M_NN_SEQD("NN M Seqd"), LIB_L_NN_SEQD("NN L Seqd"), //
    LIB_P_RNA_SEQD("RNA P Seqd"), LIB_R_RNA_SEQD("RNA R Seqd"), LIB_O_RNA_SEQD("RNA O Seqd"), LIB_X_RNA_SEQD("RNA X Seqd"), LIB_M_RNA_SEQD("RNA M Seqd"), LIB_L_RNA_SEQD("RNA L Seqd"), //
    LIB_P_RNA_10X_SEQD("RNA 10X P Seqd"), LIB_R_RNA_10X_SEQD("RNA 10X R Seqd"), LIB_O_RNA_10X_SEQD("RNA 10X O Seqd"), LIB_X_RNA_10X_SEQD("RNA 10X X Seqd"), LIB_M_RNA_10X_SEQD("RNA 10X M Seqd"), LIB_L_RNA_10X_SEQD("RNA 10X L Seqd"), //
    
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
  private static final String COUNT_CATEGORY_DNA_10X_LIB = "DNA 10X Library";
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
    List<SampleDto> organoidTissues;
    List<SampleDto> metastaticTissues;
    List<SampleDto> xenoTissues;
    List<SampleDto> miscTissues;
    List<SampleDto> dnaPStock;
    List<SampleDto> dnaRStock;
    List<SampleDto> dnaOStock;
    List<SampleDto> dnaXStock;
    List<SampleDto> dnaMStock;
    List<SampleDto> dnaLStock;
    List<SampleDto> rnaPStock;
    List<SampleDto> rnaRStock;
    List<SampleDto> rnaOStock;
    List<SampleDto> rnaXStock;
    List<SampleDto> rnaMStock;
    List<SampleDto> rnaLStock;
    List<SampleDto> dnaPAliquots;
    List<SampleDto> dnaRAliquots;
    List<SampleDto> dnaOAliquots;
    List<SampleDto> dnaXAliquots;
    List<SampleDto> dnaMAliquots;
    List<SampleDto> dnaLAliquots;
    List<SampleDto> rnaPAliquots;
    List<SampleDto> rnaRAliquots;
    List<SampleDto> rnaOAliquots;
    List<SampleDto> rnaXAliquots;
    List<SampleDto> rnaMAliquots;
    List<SampleDto> rnaLAliquots;

    List<SampleDto> wgPLibraries;
    List<SampleDto> wgRLibraries;
    List<SampleDto> wgOLibraries;
    List<SampleDto> wgXLibraries;
    List<SampleDto> wgMLibraries;
    List<SampleDto> wgLLibraries;
    List<SampleDto> exPLibraries;
    List<SampleDto> exRLibraries;
    List<SampleDto> exOLibraries;
    List<SampleDto> exXLibraries;
    List<SampleDto> exMLibraries;
    List<SampleDto> exLLibraries;
    List<SampleDto> tsPLibraries;
    List<SampleDto> tsRLibraries;
    List<SampleDto> tsOLibraries;
    List<SampleDto> tsXLibraries;
    List<SampleDto> tsMLibraries;
    List<SampleDto> tsLLibraries;
    List<SampleDto> dna10xPLibraries;
    List<SampleDto> dna10xRLibraries;
    List<SampleDto> dna10xOLibraries;
    List<SampleDto> dna10xXLibraries;
    List<SampleDto> dna10xMLibraries;
    List<SampleDto> dna10xLLibraries;
    List<SampleDto> dnaOtherPLibraries;
    List<SampleDto> dnaOtherRLibraries;
    List<SampleDto> dnaOtherOLibraries;
    List<SampleDto> dnaOtherXLibraries;
    List<SampleDto> dnaOtherMLibraries;
    List<SampleDto> dnaOtherLLibraries;
    List<SampleDto> nnPLibraries;
    List<SampleDto> nnRLibraries;
    List<SampleDto> nnOLibraries;
    List<SampleDto> nnXLibraries;
    List<SampleDto> nnMLibraries;
    List<SampleDto> nnLLibraries;
    List<SampleDto> rnaPLibraries;
    List<SampleDto> rnaRLibraries;
    List<SampleDto> rnaOLibraries;
    List<SampleDto> rnaXLibraries;
    List<SampleDto> rnaMLibraries;
    List<SampleDto> rnaLLibraries;
    List<SampleDto> rna10xPLibraries;
    List<SampleDto> rna10xRLibraries;
    List<SampleDto> rna10xOLibraries;
    List<SampleDto> rna10xXLibraries;
    List<SampleDto> rna10xMLibraries;
    List<SampleDto> rna10xLLibraries;
    List<SampleDto> nonIlluminaLibraries;

    Set<SampleDto> sequencedWgPLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgRLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgOLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgXLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgMLibraries = new HashSet<>();
    Set<SampleDto> sequencedWgLLibraries = new HashSet<>();
    Set<SampleDto> sequencedExPLibraries = new HashSet<>();
    Set<SampleDto> sequencedExRLibraries = new HashSet<>();
    Set<SampleDto> sequencedExOLibraries = new HashSet<>();
    Set<SampleDto> sequencedExXLibraries = new HashSet<>();
    Set<SampleDto> sequencedExMLibraries = new HashSet<>();
    Set<SampleDto> sequencedExLLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsPLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsRLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsOLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsXLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsMLibraries = new HashSet<>();
    Set<SampleDto> sequencedTsLLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xPLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xRLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xOLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xXLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xMLibraries = new HashSet<>();
    Set<SampleDto> sequencedDna10xLLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaPLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaRLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaOLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaXLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaMLibraries = new HashSet<>();
    Set<SampleDto> sequencedDnaLLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnPLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnRLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnOLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnXLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnMLibraries = new HashSet<>();
    Set<SampleDto> sequencedNnLLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaPLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaRLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaOLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaXLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaMLibraries = new HashSet<>();
    Set<SampleDto> sequencedRnaLLibraries = new HashSet<>();
    Set<SampleDto> sequencedRna10xPLibraries = new HashSet<>();
    Set<SampleDto> sequencedRna10xRLibraries = new HashSet<>();
    Set<SampleDto> sequencedRna10xOLibraries = new HashSet<>();
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
    organoidTissues = filter(realSamples, Arrays.asList(byTissueLike(), byOrganoid(allSamplesById)));
    metastaticTissues = filter(realSamples, Arrays.asList(byTissueLike(), byMetastatic(allSamplesById)));
    xenoTissues = filter(realSamples, Arrays.asList(byTissueLike(), byXeno(allSamplesById)));
    miscTissues = filter(realSamples, Arrays.asList(byTissueLike(), byLeftover(allSamplesById)));
    dnaPStock = filter(realSamples, Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaRStock = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaOStock = filter(realSamples, Arrays.asList(byOrganoid(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaXStock = filter(realSamples, Arrays.asList(byXeno(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaMStock = filter(realSamples,
        Arrays.asList(byMetastatic(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    dnaLStock = filter(realSamples, Arrays.asList(byLeftover(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA)));
    rnaPStock = filter(realSamples, Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaRStock = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaOStock = filter(realSamples, Arrays.asList(byOrganoid(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaXStock = filter(realSamples, Arrays.asList(byXeno(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaMStock = filter(realSamples,
        Arrays.asList(byMetastatic(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    rnaLStock = filter(realSamples, Arrays.asList(byLeftover(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA)));
    dnaPAliquots = filter(realSamples,
        Arrays.asList(byPrimary(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    dnaRAliquots = filter(realSamples,
        Arrays.asList(byReference(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
    dnaOAliquots = filter(realSamples,
        Arrays.asList(byOrganoid(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA)));
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
    rnaOAliquots = filter(realSamples,
        Arrays.asList(byOrganoid(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaXAliquots = filter(realSamples,
        Arrays.asList(byXeno(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaMAliquots = filter(realSamples,
        Arrays.asList(byMetastatic(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    rnaLAliquots = filter(realSamples,
        Arrays.asList(byLeftover(allSamplesById), bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA)));
    
    wgPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byPrimary(allSamplesById)));
    wgRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byReference(allSamplesById)));
    wgOLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byOrganoid(allSamplesById)));
    wgXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byXeno(allSamplesById)));
    wgMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byMetastatic(allSamplesById)));
    wgLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), byLeftover(allSamplesById)));
    exPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byPrimary(allSamplesById)));
    exRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byReference(allSamplesById)));
    exOLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byOrganoid(allSamplesById)));
    exXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byXeno(allSamplesById)));
    exMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byMetastatic(allSamplesById)));
    exLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), byLeftover(allSamplesById)));
    tsPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byPrimary(allSamplesById)));
    tsRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byReference(allSamplesById)));
    tsOLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byOrganoid(allSamplesById)));
    tsXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byXeno(allSamplesById)));
    tsMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byMetastatic(allSamplesById)));
    tsLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), byLeftover(allSamplesById)));
    dna10xPLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byPrimary(allSamplesById)));
    dna10xRLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byReference(allSamplesById)));
    dna10xOLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byOrganoid(allSamplesById)));
    dna10xXLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byXeno(allSamplesById)));
    dna10xMLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byMetastatic(allSamplesById)));
    dna10xLLibraries = filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), byLeftover(allSamplesById)));
    dnaOtherPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byPrimary(allSamplesById)));
    dnaOtherRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byReference(allSamplesById)));
    dnaOtherOLibraries = filter(libraries, Arrays
        .asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)), byOrganoid(allSamplesById)));
    dnaOtherXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byXeno(allSamplesById)));
    dnaOtherMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byMetastatic(allSamplesById)));
    dnaOtherLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)),
            byLeftover(allSamplesById)));
    nnPLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byPrimary(allSamplesById)));
    nnRLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byReference(allSamplesById)));
    nnOLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byOrganoid(allSamplesById)));
    nnXLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byXeno(allSamplesById)));
    nnMLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byMetastatic(allSamplesById)));
    nnLLibraries = filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), byLeftover(allSamplesById)));
    rnaPLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byPrimary(allSamplesById)));
    rnaRLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byReference(allSamplesById)));
    rnaOLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byOrganoid(allSamplesById)));
    rnaXLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byXeno(allSamplesById)));
    rnaMLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byMetastatic(allSamplesById)));
    rnaLLibraries = filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), byLeftover(allSamplesById)));
    rna10xPLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), byPrimary(allSamplesById)));
    rna10xRLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), byReference(allSamplesById)));
    rna10xOLibraries = filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), byOrganoid(allSamplesById)));
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
                  sequencedRna10xOLibraries, sequencedRna10xXLibraries, sequencedRna10xMLibraries, sequencedRna10xLLibraries);
            } else {
              // RNA, no 10X
              assignLibraryByTissueType(dilution, allSamplesById, sequencedRnaPLibraries, sequencedRnaRLibraries, sequencedRnaOLibraries,
                  sequencedRnaXLibraries, sequencedRnaMLibraries, sequencedRnaLLibraries);
            }
          } else {
            if (is10XLibrary(dilution, allSamplesById)) {
              // DNA, 10X
              assignLibraryByTissueType(dilution, allSamplesById, sequencedDna10xPLibraries, sequencedDna10xRLibraries,
                  sequencedDna10xOLibraries, sequencedDna10xXLibraries, sequencedDna10xMLibraries, sequencedDna10xLLibraries);
            } else if (LIBRARY_DESIGN_WG.equals(code)) {
              // DNA, no 10X, WG
              assignLibraryByTissueType(dilution, allSamplesById, sequencedWgPLibraries, sequencedWgRLibraries, sequencedWgOLibraries,
                  sequencedWgXLibraries, sequencedWgMLibraries, sequencedWgLLibraries);
            } else if (LIBRARY_DESIGN_EX.equals(code)) {
              // DNA, no 10X, EX
              assignLibraryByTissueType(dilution, allSamplesById, sequencedExPLibraries, sequencedExRLibraries, sequencedExOLibraries,
                  sequencedExXLibraries, sequencedExMLibraries, sequencedExLLibraries);
            } else if (LIBRARY_DESIGN_TS.equals(code)) {
              // DNA, no 10X, TS
              assignLibraryByTissueType(dilution, allSamplesById, sequencedTsPLibraries, sequencedTsRLibraries, sequencedTsOLibraries,
                  sequencedTsXLibraries,
                  sequencedTsMLibraries, sequencedTsLLibraries);
            } else if (LIBRARY_DESIGN_NN.equals(code)) {
              // DNA, no 10X, NN
              assignLibraryByTissueType(dilution, allSamplesById, sequencedNnPLibraries, sequencedNnRLibraries, sequencedNnOLibraries,
                  sequencedNnXLibraries, sequencedNnMLibraries, sequencedNnLLibraries);
            } else if (ldO.contains(code)) {
              // DNA, no 10X, AS/CH/BS
              assignLibraryByTissueType(dilution, allSamplesById, sequencedDnaPLibraries, sequencedDnaRLibraries, sequencedDnaOLibraries,
                  sequencedDnaXLibraries,
                  sequencedDnaMLibraries, sequencedDnaLLibraries);
            } else {
              System.out.println("Unexpected library design code " + code + " was found on dilution " + dilution.getId());
            }
          }
        }
      }
    }

    for (String project : projects) {
      Count priTissue = makeCount(CountLabel.P_TISSUE, primaryTissues, project);
      Count refTissue = makeCount(CountLabel.R_TISSUE, referenceTissues, project);
      Count orgTissue = makeCount(CountLabel.O_TISSUE, organoidTissues, project);
      Count metTissue = makeCount(CountLabel.M_TISSUE, metastaticTissues, project);
      Count xenoTissue = makeCount(CountLabel.X_TISSUE, xenoTissues, project);
      Count otherTissue = makeCount(CountLabel.L_TISSUE, miscTissues, project);
      Count dnaPriStock = makeCount(CountLabel.DNA_P_STOCK, dnaPStock, project);
      Count dnaRefStock = makeCount(CountLabel.DNA_R_STOCK, dnaRStock, project);
      Count dnaOrgStock = makeCount(CountLabel.DNA_O_STOCK, dnaOStock, project);
      Count dnaXenoStock = makeCount(CountLabel.DNA_X_STOCK, dnaXStock, project);
      Count dnaMetsStock = makeCount(CountLabel.DNA_M_STOCK, dnaMStock, project);
      Count dnaLeftoStock = makeCount(CountLabel.DNA_L_STOCK, dnaLStock, project);
      Count rnaPriStock = makeCount(CountLabel.RNA_P_STOCK, rnaPStock, project);
      Count rnaRefStock = makeCount(CountLabel.RNA_R_STOCK, rnaRStock, project);
      Count rnaOrgStock = makeCount(CountLabel.RNA_O_STOCK, rnaOStock, project);
      Count rnaXenoStock = makeCount(CountLabel.RNA_L_STOCK, rnaXStock, project);
      Count rnaMetsStock = makeCount(CountLabel.RNA_L_STOCK, rnaMStock, project);
      Count rnaLeftoStock = makeCount(CountLabel.RNA_L_STOCK, rnaLStock, project);
      Count dnaPriAliq = makeCount(CountLabel.DNA_P_ALIQUOT, dnaPAliquots, project);
      Count dnaRefAliq = makeCount(CountLabel.DNA_R_ALIQUOT, dnaRAliquots, project);
      Count dnaOrgAliq = makeCount(CountLabel.DNA_O_ALIQUOT, dnaOAliquots, project);
      Count dnaXenoAliq = makeCount(CountLabel.DNA_L_ALIQUOT, dnaXAliquots, project);
      Count dnaMetsAliq = makeCount(CountLabel.DNA_L_ALIQUOT, dnaMAliquots, project);
      Count dnaLeftoAliq = makeCount(CountLabel.DNA_L_ALIQUOT, dnaLAliquots, project);
      Count rnaPriAliq = makeCount(CountLabel.RNA_P_ALIQUOT, rnaPAliquots, project);
      Count rnaRefAliq = makeCount(CountLabel.RNA_R_ALIQUOT, rnaRAliquots, project);
      Count rnaOrgAliq = makeCount(CountLabel.RNA_O_ALIQUOT, rnaOAliquots, project);
      Count rnaXenoAliq = makeCount(CountLabel.RNA_L_ALIQUOT, rnaXAliquots, project);
      Count rnaMetsAliq = makeCount(CountLabel.RNA_L_ALIQUOT, rnaMAliquots, project);
      Count rnaLeftoAliq = makeCount(CountLabel.RNA_L_ALIQUOT, rnaLAliquots, project);

      Count wgPriLib = makeCount(CountLabel.LIB_P_WG, wgPLibraries, project);
      Count wgRefLib = makeCount(CountLabel.LIB_R_WG, wgRLibraries, project);
      Count wgOrgLib = makeCount(CountLabel.LIB_O_WG, wgOLibraries, project);
      Count wgXenoLib = makeCount(CountLabel.LIB_X_WG, wgXLibraries, project);
      Count wgMetsLib = makeCount(CountLabel.LIB_M_WG, wgMLibraries, project);
      Count wgLeftoLib = makeCount(CountLabel.LIB_L_WG, wgLLibraries, project);
      Count exPriLib = makeCount(CountLabel.LIB_P_EX, exPLibraries, project);
      Count exRefLib = makeCount(CountLabel.LIB_R_EX, exRLibraries, project);
      Count exOrgLib = makeCount(CountLabel.LIB_O_EX, exOLibraries, project);
      Count exXenoLib = makeCount(CountLabel.LIB_X_EX, exXLibraries, project);
      Count exMetsLib = makeCount(CountLabel.LIB_M_EX, exMLibraries, project);
      Count exLeftoLib = makeCount(CountLabel.LIB_L_EX, exLLibraries, project);
      Count tsPriLib = makeCount(CountLabel.LIB_P_TS, tsPLibraries, project);
      Count tsRefLib = makeCount(CountLabel.LIB_R_TS, tsRLibraries, project);
      Count tsOrgLib = makeCount(CountLabel.LIB_O_TS, tsOLibraries, project);
      Count tsXenoLib = makeCount(CountLabel.LIB_X_TS, tsXLibraries, project);
      Count tsMetsLib = makeCount(CountLabel.LIB_M_TS, tsMLibraries, project);
      Count tsLeftoLib = makeCount(CountLabel.LIB_L_TS, tsLLibraries, project);
      Count dna10xPriLib = makeCount(CountLabel.LIB_P_DNA_10X, dna10xPLibraries, project);
      Count dna10xRefLib = makeCount(CountLabel.LIB_R_DNA_10X, dna10xRLibraries, project);
      Count dna10xOrgLib = makeCount(CountLabel.LIB_O_DNA_10X, dna10xOLibraries, project);
      Count dna10xXenoLib = makeCount(CountLabel.LIB_X_DNA_10X, dna10xXLibraries, project);
      Count dna10xMetsLib = makeCount(CountLabel.LIB_M_DNA_10X, dna10xMLibraries, project);
      Count dna10xLeftoLib = makeCount(CountLabel.LIB_L_DNA_10X, dna10xLLibraries, project);
      Count dnaMiscPriLib = makeCount(CountLabel.LIB_P_DNA, dnaOtherPLibraries, project);
      Count dnaMiscRefLib = makeCount(CountLabel.LIB_R_DNA, dnaOtherRLibraries, project);
      Count dnaMiscOrgLib = makeCount(CountLabel.LIB_O_DNA, dnaOtherOLibraries, project);
      Count dnaMiscXenoLib = makeCount(CountLabel.LIB_X_DNA, dnaOtherXLibraries, project);
      Count dnaMiscMetsLib = makeCount(CountLabel.LIB_M_DNA, dnaOtherMLibraries, project);
      Count dnaMiscLeftoLib = makeCount(CountLabel.LIB_L_DNA, dnaOtherLLibraries, project);
      Count nnPriLib = makeCount(CountLabel.LIB_P_NN, nnPLibraries, project);
      Count nnRefLib = makeCount(CountLabel.LIB_R_NN, nnRLibraries, project);
      Count nnOrgLib = makeCount(CountLabel.LIB_O_NN, nnOLibraries, project);
      Count nnXenoLib = makeCount(CountLabel.LIB_X_NN, nnXLibraries, project);
      Count nnMetsLib = makeCount(CountLabel.LIB_M_NN, nnMLibraries, project);
      Count nnLeftoLib = makeCount(CountLabel.LIB_L_NN, nnLLibraries, project);
      Count rnaPriLib = makeCount(CountLabel.LIB_P_RNA, rnaPLibraries, project);
      Count rnaRefLib = makeCount(CountLabel.LIB_R_RNA, rnaRLibraries, project);
      Count rnaOrgLib = makeCount(CountLabel.LIB_O_RNA, rnaOLibraries, project);
      Count rnaXenoLib = makeCount(CountLabel.LIB_X_RNA, rnaXLibraries, project);
      Count rnaMetsLib = makeCount(CountLabel.LIB_M_RNA, rnaMLibraries, project);
      Count rnaLeftoLib = makeCount(CountLabel.LIB_L_RNA, rnaLLibraries, project);
      Count rna10xPriLib = makeCount(CountLabel.LIB_P_RNA_10X, rna10xPLibraries, project);
      Count rna10xRefLib = makeCount(CountLabel.LIB_R_RNA_10X, rna10xRLibraries, project);
      Count rna10xOrgLib = makeCount(CountLabel.LIB_O_RNA_10X, rna10xOLibraries, project);
      Count rna10xXenoLib = makeCount(CountLabel.LIB_X_RNA_10X, rna10xXLibraries, project);
      Count rna10xMetsLib = makeCount(CountLabel.LIB_M_RNA_10X, rna10xMLibraries, project);
      Count rna10xLeftoLib = makeCount(CountLabel.LIB_L_RNA_10X, rna10xLLibraries, project);
      Count nonIllLib = makeCount(CountLabel.LIB_NON_ILL, nonIlluminaLibraries, project);

      Count wgPSeqd = makeCount(CountLabel.LIB_P_WG_SEQD, sequencedWgPLibraries, project);
      Count wgRSeqd = makeCount(CountLabel.LIB_R_WG_SEQD, sequencedWgRLibraries, project);
      Count wgOSeqd = makeCount(CountLabel.LIB_O_WG_SEQD, sequencedWgOLibraries, project);
      Count wgXSeqd = makeCount(CountLabel.LIB_X_WG_SEQD, sequencedWgXLibraries, project);
      Count wgMSeqd = makeCount(CountLabel.LIB_M_WG_SEQD, sequencedWgMLibraries, project);
      Count wgLSeqd = makeCount(CountLabel.LIB_L_WG_SEQD, sequencedWgLLibraries, project);
      Count exPSeqd = makeCount(CountLabel.LIB_P_EX_SEQD, sequencedExPLibraries, project);
      Count exRSeqd = makeCount(CountLabel.LIB_R_EX_SEQD, sequencedExRLibraries, project);
      Count exOSeqd = makeCount(CountLabel.LIB_O_EX_SEQD, sequencedExOLibraries, project);
      Count exXSeqd = makeCount(CountLabel.LIB_X_EX_SEQD, sequencedExXLibraries, project);
      Count exMSeqd = makeCount(CountLabel.LIB_M_EX_SEQD, sequencedExMLibraries, project);
      Count exLSeqd = makeCount(CountLabel.LIB_L_EX_SEQD, sequencedExLLibraries, project);
      Count tsPSeqd = makeCount(CountLabel.LIB_P_TS_SEQD, sequencedTsPLibraries, project);
      Count tsRSeqd = makeCount(CountLabel.LIB_R_TS_SEQD, sequencedTsRLibraries, project);
      Count tsOSeqd = makeCount(CountLabel.LIB_O_TS_SEQD, sequencedTsOLibraries, project);
      Count tsXSeqd = makeCount(CountLabel.LIB_X_TS_SEQD, sequencedTsXLibraries, project);
      Count tsMSeqd = makeCount(CountLabel.LIB_M_TS_SEQD, sequencedTsMLibraries, project);
      Count tsLSeqd = makeCount(CountLabel.LIB_L_TS_SEQD, sequencedTsLLibraries, project);
      Count nnPSeqd = makeCount(CountLabel.LIB_P_NN_SEQD, sequencedNnPLibraries, project);
      Count nnRSeqd = makeCount(CountLabel.LIB_R_NN_SEQD, sequencedNnRLibraries, project);
      Count nnOSeqd = makeCount(CountLabel.LIB_O_NN_SEQD, sequencedNnOLibraries, project);
      Count nnXSeqd = makeCount(CountLabel.LIB_X_NN_SEQD, sequencedNnXLibraries, project);
      Count nnMSeqd = makeCount(CountLabel.LIB_M_NN_SEQD, sequencedNnMLibraries, project);
      Count nnLSeqd = makeCount(CountLabel.LIB_L_NN_SEQD, sequencedNnLLibraries, project);
      Count dnaPSeqd = makeCount(CountLabel.LIB_P_DNA_SEQD, sequencedDnaPLibraries, project);
      Count dnaRSeqd = makeCount(CountLabel.LIB_R_DNA_SEQD, sequencedDnaRLibraries, project);
      Count dnaOSeqd = makeCount(CountLabel.LIB_O_DNA_SEQD, sequencedDnaOLibraries, project);
      Count dnaXSeqd = makeCount(CountLabel.LIB_X_DNA_SEQD, sequencedDnaXLibraries, project);
      Count dnaMSeqd = makeCount(CountLabel.LIB_M_DNA_SEQD, sequencedDnaMLibraries, project);
      Count dnaLSeqd = makeCount(CountLabel.LIB_L_DNA_SEQD, sequencedDnaLLibraries, project);
      Count dna10xPSeqd = makeCount(CountLabel.LIB_P_DNA_10X_SEQD, sequencedDna10xPLibraries, project);
      Count dna10xRSeqd = makeCount(CountLabel.LIB_R_DNA_10X_SEQD, sequencedDna10xRLibraries, project);
      Count dna10xOSeqd = makeCount(CountLabel.LIB_O_DNA_10X_SEQD, sequencedDna10xOLibraries, project);
      Count dna10xXSeqd = makeCount(CountLabel.LIB_X_DNA_10X_SEQD, sequencedDna10xXLibraries, project);
      Count dna10xMSeqd = makeCount(CountLabel.LIB_M_DNA_10X_SEQD, sequencedDna10xMLibraries, project);
      Count dna10xLSeqd = makeCount(CountLabel.LIB_L_DNA_10X_SEQD, sequencedDna10xLLibraries, project);

      Count rnaPSeqd = makeCount(CountLabel.LIB_P_RNA_SEQD, sequencedRnaPLibraries, project);
      Count rnaRSeqd = makeCount(CountLabel.LIB_R_RNA_SEQD, sequencedRnaRLibraries, project);
      Count rnaOSeqd = makeCount(CountLabel.LIB_O_RNA_SEQD, sequencedRnaOLibraries, project);
      Count rnaXSeqd = makeCount(CountLabel.LIB_X_RNA_SEQD, sequencedRnaXLibraries, project);
      Count rnaMSeqd = makeCount(CountLabel.LIB_M_RNA_SEQD, sequencedRnaMLibraries, project);
      Count rnaLSeqd = makeCount(CountLabel.LIB_L_RNA_SEQD, sequencedRnaLLibraries, project);
      Count rna10xPSeqd = makeCount(CountLabel.LIB_P_RNA_10X_SEQD, sequencedRna10xPLibraries, project);
      Count rna10xRSeqd = makeCount(CountLabel.LIB_R_RNA_10X_SEQD, sequencedRna10xRLibraries, project);
      Count rna10xOSeqd = makeCount(CountLabel.LIB_O_RNA_10X_SEQD, sequencedRna10xOLibraries, project);
      Count rna10xXSeqd = makeCount(CountLabel.LIB_X_RNA_10X_SEQD, sequencedRna10xXLibraries, project);
      Count rna10xMSeqd = makeCount(CountLabel.LIB_M_RNA_10X_SEQD, sequencedRna10xMLibraries, project);
      Count rna10xLSeqd = makeCount(CountLabel.LIB_L_RNA_10X_SEQD, sequencedRna10xLLibraries, project);
      
      Count nonIllSeqd = makeCount(CountLabel.LIB_NON_ILL_SEQD, sequencedNonIlluminaLibraries, project);

      Map<String, List<Count>> categoryCounts = new LinkedHashMap<>();
      categoryCounts.put(COUNT_CATEGORY_TISSUE,
          new ArrayList<>(Arrays.asList(priTissue, refTissue, orgTissue, xenoTissue, metTissue, otherTissue)));
      categoryCounts.put(COUNT_CATEGORY_DNA_STOCK,
          new ArrayList<>(Arrays.asList(dnaPriStock, dnaRefStock, dnaOrgStock, dnaXenoStock, dnaMetsStock, dnaLeftoStock)));
      categoryCounts.put(COUNT_CATEGORY_RNA_STOCK,
          new ArrayList<>(Arrays.asList(rnaPriStock, rnaRefStock, rnaOrgStock, rnaXenoStock, rnaMetsStock, rnaLeftoStock)));
      categoryCounts.put(COUNT_CATEGORY_DNA_ALIQUOT,
          new ArrayList<>(Arrays.asList(dnaPriAliq, dnaRefAliq, dnaOrgAliq, dnaXenoAliq, dnaMetsAliq, dnaLeftoAliq)));
      categoryCounts.put(COUNT_CATEGORY_RNA_ALIQUOT,
          new ArrayList<>(Arrays.asList(rnaPriAliq, rnaRefAliq, rnaOrgAliq, rnaXenoAliq, rnaMetsAliq, rnaLeftoAliq)));
      categoryCounts.put(COUNT_CATEGORY_WG_LIB,
          new ArrayList<>(Arrays.asList(wgPriLib, wgRefLib, wgOrgLib, wgXenoLib, wgMetsLib, wgLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_EX_LIB,
          new ArrayList<>(Arrays.asList(exPriLib, exRefLib, exOrgLib, exXenoLib, exMetsLib, exLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_TS_LIB,
          new ArrayList<>(Arrays.asList(tsPriLib, tsRefLib, tsOrgLib, tsXenoLib, tsMetsLib, tsLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_DNA_LIB,
          new ArrayList<>(Arrays.asList(dnaMiscPriLib, dnaMiscRefLib, dnaMiscOrgLib, dnaMiscXenoLib, dnaMiscMetsLib, dnaMiscLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_DNA_10X_LIB,
          new ArrayList<>(Arrays.asList(dna10xPriLib, dna10xRefLib, dna10xOrgLib, dna10xXenoLib, dna10xMetsLib, dna10xLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_NN_LIB,
          new ArrayList<>(Arrays.asList(nnPriLib, nnRefLib, nnOrgLib, nnXenoLib, nnMetsLib, nnLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_RNA_LIB,
          new ArrayList<>(Arrays.asList(rnaPriLib, rnaRefLib, rnaOrgLib, rnaXenoLib, rnaMetsLib, rnaLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_RNA_10X_LIB,
          new ArrayList<>(Arrays.asList(rna10xPriLib, rna10xRefLib, rna10xOrgLib, rna10xXenoLib, rna10xMetsLib, rna10xLeftoLib)));
      categoryCounts.put(COUNT_CATEGORY_NON_ILL_LIB, new ArrayList<>(Arrays.asList(nonIllLib)));
      categoryCounts.put(COUNT_CATEGORY_WG_SEQD, new ArrayList<>(Arrays.asList(wgPSeqd, wgRSeqd, wgOSeqd, wgXSeqd, wgMSeqd, wgLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_EX_SEQD, new ArrayList<>(Arrays.asList(exPSeqd, exRSeqd, exOSeqd, exXSeqd, exMSeqd, exLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_TS_SEQD, new ArrayList<>(Arrays.asList(tsPSeqd, tsRSeqd, tsOSeqd, tsXSeqd, tsMSeqd, tsLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_DNA_SEQD,
          new ArrayList<>(Arrays.asList(dnaPSeqd, dnaRSeqd, dnaOSeqd, dnaXSeqd, dnaMSeqd, dnaLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_DNA_10X_SEQD,
          new ArrayList<>(Arrays.asList(dna10xPSeqd, dna10xRSeqd, dna10xOSeqd, dna10xXSeqd, dna10xMSeqd, dna10xLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_NN_SEQD, new ArrayList<>(Arrays.asList(nnPSeqd, nnRSeqd, nnOSeqd, nnXSeqd, nnMSeqd, nnLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_RNA_SEQD,
          new ArrayList<>(Arrays.asList(rnaPSeqd, rnaRSeqd, rnaOSeqd, rnaXSeqd, rnaMSeqd, rnaLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_RNA_10X_SEQD,
          new ArrayList<>(Arrays.asList(rna10xPSeqd, rna10xRSeqd, rna10xOSeqd, rna10xXSeqd, rna10xMSeqd, rna10xLSeqd)));
      categoryCounts.put(COUNT_CATEGORY_NON_ILL_SEQD, new ArrayList<>(Arrays.asList(nonIllSeqd)));
      removeEmpties(categoryCounts);
      maybeUpdateColumnCount(categoryCounts);
      countsByProject.put(project, categoryCounts);

    }

    countsByProjectAsList = listifyCountsByProject(countsByProject);
  }

  private Count makeCount(CountLabel label, Collection<SampleDto> collection, String project) {
    return new Count(label, filter(collection, byProject(project)).size());
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
      Set<SampleDto> oSeqd, Set<SampleDto> xSeqd, Set<SampleDto> mSeqd, Set<SampleDto> lSeqd) {
    String type = getUpstreamAttribute(ATTR_TISSUE_TYPE, dilution, allSamples);
    switch (type) {
    case "P":
    case "S":
    case "A":
      pSeqd.add(dilution);
      break;
    case "R":
      rSeqd.add(dilution);
      break;
    case "O":
      oSeqd.add(dilution);
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
      List<String> primaryLike = Arrays.asList("P", "S", "A");
      return tissueTypeMatches(dto, primaryLike, allSamples);
    };
  }

  private Predicate<SampleDto> byReference(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> reference = Arrays.asList("R");
      return tissueTypeMatches(dto, reference, allSamples);
    };
  }

  private Predicate<SampleDto> byOrganoid(Map<String, SampleDto> allSamples) {
    return dto -> {
      List<String> organoid = Arrays.asList("O");
      return tissueTypeMatches(dto, organoid, allSamples);
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
