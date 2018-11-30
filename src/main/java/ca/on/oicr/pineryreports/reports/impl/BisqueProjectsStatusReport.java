package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.*;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
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
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
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
      } else if (!key.equals(other.key)) {
        return false;
      }
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
    
    LIB_NON_ILL_SEQD(COUNT_CATEGORY_NON_ILL_SEQD);

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
  private static final String COUNT_CATEGORY_WG_SEQD = "WG Lib Seqd";
  private static final String COUNT_CATEGORY_EX_SEQD = "EX Lib Seqd";
  private static final String COUNT_CATEGORY_TS_SEQD = "TS Lib Seqd";
  private static final String COUNT_CATEGORY_DNA_10X_SEQD = "DNA 10X Lib Seqd";
  private static final String COUNT_CATEGORY_DNA_SEQD = "DNA Lib Seqd";
  private static final String COUNT_CATEGORY_NN_SEQD = "NN Lib Seqd";
  private static final String COUNT_CATEGORY_RNA_SEQD = "RNA Lib Seqd";
  private static final String COUNT_CATEGORY_RNA_10X_SEQD = "RNA 10X Lib Seqd";

  private static final String COUNT_CATEGORY_NON_ILL_LIB = "Non-Illumina Library";
  private static final String COUNT_CATEGORY_NON_ILL_SEQD = "Non-Illumina Seqd";

  private static final String P = "P";
  private static final String R = "R";
  private static final String O = "O";
  private static final String X = "X";
  private static final String M = "M";
  private static final String L = "L";

  private Set<String> projects = Sets.newHashSet();

  List<String> ldO = Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_BS, LIBRARY_DESIGN_AS);

  protected int columnCount = 0;

  private static final Option OPT_PROJECT = CommonOptions.project(false);
  public static final String REPORT_NAME = "projects-status";

  // right now this needs to be the live version, as the flatfile version doesn't have the active attribute on projects
  private static final String PINERY_PROJECTS_ADDRESS = "http://seqbio-pinery-prod-www.hpc.oicr.on.ca:8080/pinery-ws-miso-live/sample/projects";

  private List<Map.Entry<String, Map<String, List<Count>>>> countsByProjectAsList; // String project, List<Count> all counts

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_PROJECT);
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    if (cmd.hasOption(OPT_PROJECT.getLongOpt())) {
      List<String> projx = Arrays.asList(cmd.getOptionValue(OPT_PROJECT.getLongOpt()).split(","));
      this.projects = projx.stream().map(String::trim).collect(Collectors.toSet());
    } else {
      try {
        this.projects = getActiveProjectsFromPinery();
      } catch (IOException e) {
        throw new IllegalArgumentException("Failed to retrieve active projects list from Pinery");
      }
    }
  }

  private Set<String> getActiveProjectsFromPinery() throws IOException {
    try {
      String response = getProjectsFromPinery();
      ObjectMapper mapper = new ObjectMapper();
      JsonNode json = mapper.readTree(response);
      ArrayNode array = (ArrayNode) json;
      // JSONArray json = new JSONArray(response);
      Set<String> activeProjects = new HashSet<>();
      for (int i = 0; i < array.size(); i++) {
        ObjectNode project = (ObjectNode) array.get(i);
        if (project.get("active").asBoolean()) {
          activeProjects.add(project.get("name").asText());
        }
      }
      return activeProjects;
    } catch (IOException e) {
      throw new IOException("Error getting Pinery projects endpoint", e);
    }
  }

  private String getProjectsFromPinery() throws IOException {
    URL pineryUrl = new URL(PINERY_PROJECTS_ADDRESS);
    HttpURLConnection connection = (HttpURLConnection) pineryUrl.openConnection();
    connection.setConnectTimeout(15000);
    connection.setReadTimeout(15000);

    int status = connection.getResponseCode();
    if (status != 200) {
      throw new IllegalStateException(String.format("Got unexpected HTTP status %d when requesting Pinery projects", status));
    }
    BufferedReader in = new BufferedReader(
        new InputStreamReader(connection.getInputStream()));
    String inputLine;
    StringBuffer response = new StringBuffer();

    while ((inputLine = in.readLine()) != null) {
      response.append(inputLine);
    }
    in.close();
    connection.disconnect();

    return response.toString();
  }

  @Override
  public String getTitle() {
    return "Projects Status Report (Bisque) generated " + new SimpleDateFormat(DATE_FORMAT).format(new Date());
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> allSamples = pinery.getSample().all();
    Map<String, SampleDto> allSamplesById = mapSamplesById(allSamples);
    List<RunDto> allRuns = pinery.getSequencerRun().all();

    Map<String, Map<String, List<Count>>> countsByProject = new TreeMap<>(); // String project, List<Count>
                                                                                                             // all counts

    List<SampleDto> realSamples = new ArrayList<>(); // no identities
    List<SampleDto> libraries = new ArrayList<>();

    List<SampleDto> nonIlluminaLibraries;

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

    Map<String, List<SampleDto>> primaries = filterAllTheThings(realSamples, libraries, allSamplesById, P);
    Map<String, List<SampleDto>> references = filterAllTheThings(realSamples, libraries, allSamplesById, R);
    Map<String, List<SampleDto>> organoids = filterAllTheThings(realSamples, libraries, allSamplesById, O);
    Map<String, List<SampleDto>> xenografts = filterAllTheThings(realSamples, libraries, allSamplesById, X);
    Map<String, List<SampleDto>> metastases = filterAllTheThings(realSamples, libraries, allSamplesById, M);
    Map<String, List<SampleDto>> leftovers = filterAllTheThings(realSamples, libraries, allSamplesById, L);

    nonIlluminaLibraries = filter(libraries, Arrays.asList(byNonIlluminaLibrary()));

    Map<String, Map<String, Set<SampleDto>>> seqdLibsByCategory = new HashMap<>();
    seqdLibsByCategory.put(COUNT_CATEGORY_WG_SEQD, new HashMap<>());
    seqdLibsByCategory.put(COUNT_CATEGORY_EX_SEQD, new HashMap<>());
    seqdLibsByCategory.put(COUNT_CATEGORY_TS_SEQD, new HashMap<>());
    seqdLibsByCategory.put(COUNT_CATEGORY_DNA_10X_SEQD, new HashMap<>());
    seqdLibsByCategory.put(COUNT_CATEGORY_DNA_SEQD, new HashMap<>());
    seqdLibsByCategory.put(COUNT_CATEGORY_NN_SEQD, new HashMap<>());
    seqdLibsByCategory.put(COUNT_CATEGORY_RNA_SEQD, new HashMap<>());
    seqdLibsByCategory.put(COUNT_CATEGORY_RNA_10X_SEQD, new HashMap<>());

    for (Map<String, Set<SampleDto>> category : seqdLibsByCategory.values()) {
      category.put(P, new HashSet<>());
      category.put(R, new HashSet<>());
      category.put(O, new HashSet<>());
      category.put(X, new HashSet<>());
      category.put(M, new HashSet<>());
      category.put(L, new HashSet<>());
    }

    seqdLibsByCategory.put(COUNT_CATEGORY_NON_ILL_SEQD, new HashMap<>());
    seqdLibsByCategory.get(COUNT_CATEGORY_NON_ILL_SEQD).put("All", new HashSet<>());

    
    // track which libraries were sequenced
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
          SampleDto library = getParent(dilution, allSamplesById);
          if (isNonIlluminaLibrary(dilution)) {
            seqdLibsByCategory.get(COUNT_CATEGORY_NON_ILL_SEQD).get("All").add(dilution);
            continue;
          }
          String code = getUpstreamAttribute(ATTR_SOURCE_TEMPLATE_TYPE, dilution, allSamplesById);
          if (code == null) throw new IllegalArgumentException("Dilution " + dilution.getName() + " has no library design code in hierarchy");
          if (isRnaLibrary(dilution, allSamplesById)) {
            if (is10XLibrary(library, allSamplesById)) {
              // RNA, 10X
              assignLibraryByTissueType(dilution, allSamplesById, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_10X_SEQD));
            } else {
              // RNA, no 10X
              assignLibraryByTissueType(dilution, allSamplesById, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_SEQD));
            }
          } else {
            if (is10XLibrary(library, allSamplesById)) {
              // DNA, 10X
              assignLibraryByTissueType(dilution, allSamplesById, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_10X_SEQD));
            } else if (LIBRARY_DESIGN_WG.equals(code)) {
              // DNA, no 10X, WG
              assignLibraryByTissueType(dilution, allSamplesById, seqdLibsByCategory.get(COUNT_CATEGORY_WG_SEQD));
            } else if (LIBRARY_DESIGN_EX.equals(code)) {
              // DNA, no 10X, EX
              assignLibraryByTissueType(dilution, allSamplesById, seqdLibsByCategory.get(COUNT_CATEGORY_EX_SEQD));
            } else if (LIBRARY_DESIGN_TS.equals(code)) {
              // DNA, no 10X, TS
              assignLibraryByTissueType(dilution, allSamplesById, seqdLibsByCategory.get(COUNT_CATEGORY_TS_SEQD));
            } else if (LIBRARY_DESIGN_NN.equals(code)) {
              // DNA, no 10X, NN
              assignLibraryByTissueType(dilution, allSamplesById, seqdLibsByCategory.get(COUNT_CATEGORY_NN_SEQD));
            } else if (ldO.contains(code)) {
              // DNA, no 10X, AS/CH/BS
              assignLibraryByTissueType(dilution, allSamplesById, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_SEQD));
            } else {
              throw new IOException("Unable to categorize dilution " + dilution.getId());
            }
          }
        }
      }
    }

    for (String project : projects) {
      Map<String, List<Count>> categoryCounts = new LinkedHashMap<>();
      categoryCounts.put(COUNT_CATEGORY_TISSUE, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.P_TISSUE, primaries.get(COUNT_CATEGORY_TISSUE), project),
          makeCount(CountLabel.R_TISSUE, references.get(COUNT_CATEGORY_TISSUE), project),
          makeCount(CountLabel.O_TISSUE, organoids.get(COUNT_CATEGORY_TISSUE), project),
          makeCount(CountLabel.X_TISSUE, xenografts.get(COUNT_CATEGORY_TISSUE), project),
          makeCount(CountLabel.M_TISSUE, metastases.get(COUNT_CATEGORY_TISSUE), project),
          makeCount(CountLabel.L_TISSUE, leftovers.get(COUNT_CATEGORY_TISSUE), project)
          )));
      categoryCounts.put(COUNT_CATEGORY_DNA_STOCK, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.DNA_P_STOCK, primaries.get(COUNT_CATEGORY_DNA_STOCK), project),
          makeCount(CountLabel.DNA_R_STOCK, references.get(COUNT_CATEGORY_DNA_STOCK), project),
          makeCount(CountLabel.DNA_O_STOCK, organoids.get(COUNT_CATEGORY_DNA_STOCK), project),
          makeCount(CountLabel.DNA_X_STOCK, xenografts.get(COUNT_CATEGORY_DNA_STOCK), project),
          makeCount(CountLabel.DNA_M_STOCK, metastases.get(COUNT_CATEGORY_DNA_STOCK), project),
          makeCount(CountLabel.DNA_L_STOCK, leftovers.get(COUNT_CATEGORY_DNA_STOCK), project)
              )));
      categoryCounts.put(COUNT_CATEGORY_RNA_STOCK, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.RNA_P_STOCK, primaries.get(COUNT_CATEGORY_RNA_STOCK), project),
          makeCount(CountLabel.RNA_R_STOCK, references.get(COUNT_CATEGORY_RNA_STOCK), project),
          makeCount(CountLabel.RNA_O_STOCK, organoids.get(COUNT_CATEGORY_RNA_STOCK), project),
          makeCount(CountLabel.RNA_X_STOCK, xenografts.get(COUNT_CATEGORY_RNA_STOCK), project),
          makeCount(CountLabel.RNA_M_STOCK, metastases.get(COUNT_CATEGORY_RNA_STOCK), project),
          makeCount(CountLabel.RNA_L_STOCK, leftovers.get(COUNT_CATEGORY_RNA_STOCK), project)
              )));
      categoryCounts.put(COUNT_CATEGORY_DNA_ALIQUOT, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.DNA_P_ALIQUOT, primaries.get(COUNT_CATEGORY_DNA_ALIQUOT), project),
          makeCount(CountLabel.DNA_R_ALIQUOT, references.get(COUNT_CATEGORY_DNA_ALIQUOT), project),
          makeCount(CountLabel.DNA_O_ALIQUOT, organoids.get(COUNT_CATEGORY_DNA_ALIQUOT), project),
          makeCount(CountLabel.DNA_X_ALIQUOT, xenografts.get(COUNT_CATEGORY_DNA_ALIQUOT), project),
          makeCount(CountLabel.DNA_M_ALIQUOT, metastases.get(COUNT_CATEGORY_DNA_ALIQUOT), project),
          makeCount(CountLabel.DNA_L_ALIQUOT, leftovers.get(COUNT_CATEGORY_DNA_ALIQUOT), project)
              )));
      categoryCounts.put(COUNT_CATEGORY_RNA_ALIQUOT, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.RNA_P_ALIQUOT, primaries.get(COUNT_CATEGORY_RNA_ALIQUOT), project),
          makeCount(CountLabel.RNA_R_ALIQUOT, references.get(COUNT_CATEGORY_RNA_ALIQUOT), project),
          makeCount(CountLabel.RNA_O_ALIQUOT, organoids.get(COUNT_CATEGORY_RNA_ALIQUOT), project),
          makeCount(CountLabel.RNA_X_ALIQUOT, xenografts.get(COUNT_CATEGORY_RNA_ALIQUOT), project),
          makeCount(CountLabel.RNA_M_ALIQUOT, metastases.get(COUNT_CATEGORY_RNA_ALIQUOT), project),
          makeCount(CountLabel.RNA_L_ALIQUOT, leftovers.get(COUNT_CATEGORY_RNA_ALIQUOT), project)
              )));
      
      categoryCounts.put(COUNT_CATEGORY_WG_LIB, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_WG, primaries.get(COUNT_CATEGORY_WG_LIB), project),
          makeCount(CountLabel.LIB_R_WG, references.get(COUNT_CATEGORY_WG_LIB), project),
          makeCount(CountLabel.LIB_O_WG, organoids.get(COUNT_CATEGORY_WG_LIB), project),
          makeCount(CountLabel.LIB_X_WG, xenografts.get(COUNT_CATEGORY_WG_LIB), project),
          makeCount(CountLabel.LIB_M_WG, metastases.get(COUNT_CATEGORY_WG_LIB), project),
          makeCount(CountLabel.LIB_L_WG, leftovers.get(COUNT_CATEGORY_WG_LIB), project)
              )));
      categoryCounts.put(COUNT_CATEGORY_EX_LIB, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_EX, primaries.get(COUNT_CATEGORY_EX_LIB), project),
          makeCount(CountLabel.LIB_R_EX, references.get(COUNT_CATEGORY_EX_LIB), project),
          makeCount(CountLabel.LIB_O_EX, organoids.get(COUNT_CATEGORY_EX_LIB), project),
          makeCount(CountLabel.LIB_X_EX, xenografts.get(COUNT_CATEGORY_EX_LIB), project),
          makeCount(CountLabel.LIB_M_EX, metastases.get(COUNT_CATEGORY_EX_LIB), project),
          makeCount(CountLabel.LIB_L_EX, leftovers.get(COUNT_CATEGORY_EX_LIB), project)
              )));
      categoryCounts.put(COUNT_CATEGORY_TS_LIB, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_TS, primaries.get(COUNT_CATEGORY_TS_LIB), project),
          makeCount(CountLabel.LIB_R_TS, references.get(COUNT_CATEGORY_TS_LIB), project),
          makeCount(CountLabel.LIB_O_TS, organoids.get(COUNT_CATEGORY_TS_LIB), project),
          makeCount(CountLabel.LIB_X_TS, xenografts.get(COUNT_CATEGORY_TS_LIB), project),
          makeCount(CountLabel.LIB_M_TS, metastases.get(COUNT_CATEGORY_TS_LIB), project),
          makeCount(CountLabel.LIB_L_TS, leftovers.get(COUNT_CATEGORY_TS_LIB), project))));
      categoryCounts.put(COUNT_CATEGORY_DNA_LIB, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_DNA, primaries.get(COUNT_CATEGORY_DNA_LIB), project),
          makeCount(CountLabel.LIB_R_DNA, references.get(COUNT_CATEGORY_DNA_LIB), project),
          makeCount(CountLabel.LIB_O_DNA, organoids.get(COUNT_CATEGORY_DNA_LIB), project),
          makeCount(CountLabel.LIB_X_DNA, xenografts.get(COUNT_CATEGORY_DNA_LIB), project),
          makeCount(CountLabel.LIB_M_DNA, metastases.get(COUNT_CATEGORY_DNA_LIB), project),
          makeCount(CountLabel.LIB_L_DNA, leftovers.get(COUNT_CATEGORY_DNA_LIB), project))));
      categoryCounts.put(COUNT_CATEGORY_DNA_10X_LIB, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_DNA_10X, primaries.get(COUNT_CATEGORY_DNA_10X_LIB), project),
          makeCount(CountLabel.LIB_R_DNA_10X, references.get(COUNT_CATEGORY_DNA_10X_LIB), project),
          makeCount(CountLabel.LIB_O_DNA_10X, organoids.get(COUNT_CATEGORY_DNA_10X_LIB), project),
          makeCount(CountLabel.LIB_X_DNA_10X, xenografts.get(COUNT_CATEGORY_DNA_10X_LIB), project),
          makeCount(CountLabel.LIB_M_DNA_10X, metastases.get(COUNT_CATEGORY_DNA_10X_LIB), project),
          makeCount(CountLabel.LIB_L_DNA_10X, leftovers.get(COUNT_CATEGORY_DNA_10X_LIB), project))));
      categoryCounts.put(COUNT_CATEGORY_NN_LIB, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_NN, primaries.get(COUNT_CATEGORY_NN_LIB), project),
          makeCount(CountLabel.LIB_R_NN, references.get(COUNT_CATEGORY_NN_LIB), project),
          makeCount(CountLabel.LIB_O_NN, organoids.get(COUNT_CATEGORY_NN_LIB), project),
          makeCount(CountLabel.LIB_X_NN, xenografts.get(COUNT_CATEGORY_NN_LIB), project),
          makeCount(CountLabel.LIB_M_NN, metastases.get(COUNT_CATEGORY_NN_LIB), project),
          makeCount(CountLabel.LIB_L_NN, leftovers.get(COUNT_CATEGORY_NN_LIB), project))));
      categoryCounts.put(COUNT_CATEGORY_RNA_LIB, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_RNA, primaries.get(COUNT_CATEGORY_RNA_LIB), project),
          makeCount(CountLabel.LIB_R_RNA, references.get(COUNT_CATEGORY_RNA_LIB), project),
          makeCount(CountLabel.LIB_O_RNA, organoids.get(COUNT_CATEGORY_RNA_LIB), project),
          makeCount(CountLabel.LIB_X_RNA, xenografts.get(COUNT_CATEGORY_RNA_LIB), project),
          makeCount(CountLabel.LIB_M_RNA, metastases.get(COUNT_CATEGORY_RNA_LIB), project),
          makeCount(CountLabel.LIB_L_RNA, leftovers.get(COUNT_CATEGORY_RNA_LIB), project))));
      categoryCounts.put(COUNT_CATEGORY_RNA_10X_LIB, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_RNA_10X, primaries.get(COUNT_CATEGORY_RNA_10X_LIB), project),
          makeCount(CountLabel.LIB_R_RNA_10X, references.get(COUNT_CATEGORY_RNA_10X_LIB), project),
          makeCount(CountLabel.LIB_O_RNA_10X, organoids.get(COUNT_CATEGORY_RNA_10X_LIB), project),
          makeCount(CountLabel.LIB_X_RNA_10X, xenografts.get(COUNT_CATEGORY_RNA_10X_LIB), project),
          makeCount(CountLabel.LIB_M_RNA_10X, metastases.get(COUNT_CATEGORY_RNA_10X_LIB), project),
          makeCount(CountLabel.LIB_L_RNA_10X, leftovers.get(COUNT_CATEGORY_RNA_10X_LIB), project))));

      categoryCounts.put(COUNT_CATEGORY_NON_ILL_LIB,
          new ArrayList<>(Arrays.asList(makeCount(CountLabel.LIB_NON_ILL, nonIlluminaLibraries, project))));

      categoryCounts.put(COUNT_CATEGORY_WG_SEQD, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_WG_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_WG_SEQD).get(P), project),
          makeCount(CountLabel.LIB_R_WG_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_WG_SEQD).get(R), project),
          makeCount(CountLabel.LIB_O_WG_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_WG_SEQD).get(O), project),
          makeCount(CountLabel.LIB_X_WG_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_WG_SEQD).get(X), project),
          makeCount(CountLabel.LIB_M_WG_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_WG_SEQD).get(M), project),
          makeCount(CountLabel.LIB_L_WG_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_WG_SEQD).get(L), project))));
      categoryCounts.put(COUNT_CATEGORY_EX_SEQD, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_EX_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_EX_SEQD).get(P), project),
          makeCount(CountLabel.LIB_R_EX_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_EX_SEQD).get(R), project),
          makeCount(CountLabel.LIB_O_EX_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_EX_SEQD).get(O), project),
          makeCount(CountLabel.LIB_X_EX_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_EX_SEQD).get(X), project),
          makeCount(CountLabel.LIB_M_EX_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_EX_SEQD).get(M), project),
          makeCount(CountLabel.LIB_L_EX_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_EX_SEQD).get(L), project))));
      categoryCounts.put(COUNT_CATEGORY_TS_SEQD, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_TS_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_TS_SEQD).get(P), project),
          makeCount(CountLabel.LIB_R_TS_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_TS_SEQD).get(R), project),
          makeCount(CountLabel.LIB_O_TS_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_TS_SEQD).get(O), project),
          makeCount(CountLabel.LIB_X_TS_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_TS_SEQD).get(X), project),
          makeCount(CountLabel.LIB_M_TS_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_TS_SEQD).get(M), project),
          makeCount(CountLabel.LIB_L_TS_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_TS_SEQD).get(L), project))));
      categoryCounts.put(COUNT_CATEGORY_DNA_SEQD, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_DNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_SEQD).get(P), project),
          makeCount(CountLabel.LIB_R_DNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_SEQD).get(R), project),
          makeCount(CountLabel.LIB_O_DNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_SEQD).get(O), project),
          makeCount(CountLabel.LIB_X_DNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_SEQD).get(X), project),
          makeCount(CountLabel.LIB_M_DNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_SEQD).get(M), project),
          makeCount(CountLabel.LIB_L_DNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_SEQD).get(L), project))));
      categoryCounts.put(COUNT_CATEGORY_DNA_10X_SEQD, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_DNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_10X_SEQD).get(P), project),
          makeCount(CountLabel.LIB_R_DNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_10X_SEQD).get(R), project),
          makeCount(CountLabel.LIB_O_DNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_10X_SEQD).get(O), project),
          makeCount(CountLabel.LIB_X_DNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_10X_SEQD).get(X), project),
          makeCount(CountLabel.LIB_M_DNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_10X_SEQD).get(M), project),
          makeCount(CountLabel.LIB_L_DNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_DNA_10X_SEQD).get(L), project))));
      categoryCounts.put(COUNT_CATEGORY_NN_SEQD, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_NN_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_NN_SEQD).get(P), project),
          makeCount(CountLabel.LIB_R_NN_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_NN_SEQD).get(R), project),
          makeCount(CountLabel.LIB_O_NN_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_NN_SEQD).get(O), project),
          makeCount(CountLabel.LIB_X_NN_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_NN_SEQD).get(X), project),
          makeCount(CountLabel.LIB_M_NN_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_NN_SEQD).get(M), project),
          makeCount(CountLabel.LIB_L_NN_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_NN_SEQD).get(L), project)
          )));
      categoryCounts.put(COUNT_CATEGORY_RNA_SEQD, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_RNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_SEQD).get(P), project),
          makeCount(CountLabel.LIB_R_RNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_SEQD).get(R), project),
          makeCount(CountLabel.LIB_O_RNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_SEQD).get(O), project),
          makeCount(CountLabel.LIB_X_RNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_SEQD).get(X), project),
          makeCount(CountLabel.LIB_M_RNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_SEQD).get(M), project),
          makeCount(CountLabel.LIB_L_RNA_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_SEQD).get(L), project))));
      categoryCounts.put(COUNT_CATEGORY_RNA_10X_SEQD, new ArrayList<>(Arrays.asList(
          makeCount(CountLabel.LIB_P_RNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_10X_SEQD).get(P), project),
          makeCount(CountLabel.LIB_R_RNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_10X_SEQD).get(R), project),
          makeCount(CountLabel.LIB_O_RNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_10X_SEQD).get(O), project),
          makeCount(CountLabel.LIB_X_RNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_10X_SEQD).get(X), project),
          makeCount(CountLabel.LIB_M_RNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_10X_SEQD).get(M), project),
          makeCount(CountLabel.LIB_L_RNA_10X_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_RNA_10X_SEQD).get(L), project))));
      categoryCounts.put(COUNT_CATEGORY_NON_ILL_SEQD,
          new ArrayList<>(Arrays
              .asList(makeCount(CountLabel.LIB_NON_ILL_SEQD, seqdLibsByCategory.get(COUNT_CATEGORY_NON_ILL_SEQD).get("All"), project))));
      removeEmpties(categoryCounts);
      maybeUpdateColumnCount(categoryCounts);
      countsByProject.put(project, categoryCounts);

    }

    countsByProjectAsList = listifyCountsByProject(countsByProject);
  }

  private Map<String, List<SampleDto>> filterAllTheThings(Collection<SampleDto> samples, Collection<SampleDto> libraries,
      Map<String, SampleDto> allSamplesById, String predicateFilter) {
    Map<String, List<SampleDto>> all = new HashMap<>();
    Predicate<SampleDto> bySpecialFilter = getSpecialFilter(predicateFilter, allSamplesById);
    all.put(COUNT_CATEGORY_TISSUE, filter(samples, Arrays.asList(byTissueLike(), bySpecialFilter)));
    all.put(COUNT_CATEGORY_DNA_STOCK,
        filter(samples, Arrays.asList(bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(DNA), bySpecialFilter)));
    all.put(COUNT_CATEGORY_RNA_STOCK,
        filter(samples, Arrays.asList(bySampleCategory(SAMPLE_CATEGORY_STOCK), byAnalyteType(RNA), bySpecialFilter)));
    all.put(COUNT_CATEGORY_DNA_ALIQUOT,
        filter(samples, Arrays.asList(bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(DNA), bySpecialFilter)));
    all.put(COUNT_CATEGORY_RNA_ALIQUOT,
        filter(samples, Arrays.asList(bySampleCategory(SAMPLE_CATEGORY_ALIQUOT), byAnalyteType(RNA), bySpecialFilter)));
    all.put(COUNT_CATEGORY_WG_LIB,
        filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_WG)), bySpecialFilter)));
    all.put(COUNT_CATEGORY_EX_LIB,
        filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_EX)), bySpecialFilter)));
    all.put(COUNT_CATEGORY_TS_LIB,
        filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_TS)), bySpecialFilter)));
    all.put(COUNT_CATEGORY_DNA_10X_LIB, filter(libraries, Arrays.asList(byDnaLibrary(), by10XLibrary(allSamplesById), bySpecialFilter)));
    all.put(COUNT_CATEGORY_DNA_LIB, filter(libraries,
        Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_CH, LIBRARY_DESIGN_AS, LIBRARY_DESIGN_BS)), bySpecialFilter)));
    all.put(COUNT_CATEGORY_NN_LIB,
        filter(libraries, Arrays.asList(byLibraryDesignCodes(Arrays.asList(LIBRARY_DESIGN_NN)), bySpecialFilter)));
    all.put(COUNT_CATEGORY_RNA_LIB, filter(libraries, Arrays.asList(byPropagated(), byRnaLibrary(), bySpecialFilter)));
    all.put(COUNT_CATEGORY_RNA_10X_LIB, filter(libraries, Arrays.asList(byRnaLibrary(), by10XLibrary(allSamplesById), bySpecialFilter)));
    return all;
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

  private void assignLibraryByTissueType(SampleDto dilution, Map<String, SampleDto> allSamples, Map<String, Set<SampleDto>> libsByProxml) {
    String type = getUpstreamAttribute(ATTR_TISSUE_TYPE, dilution, allSamples);
    switch (type) {
    case "P":
    case "S":
    case "A":
      libsByProxml.get("P").add(dilution);
      break;
    case "R":
      libsByProxml.get("R").add(dilution);
      break;
    case "O":
      libsByProxml.get("O").add(dilution);
      break;
    case "X":
      libsByProxml.get("X").add(dilution);
      break;
    case "M":
      libsByProxml.get("M").add(dilution);
      break;
    default:
      libsByProxml.get("L").add(dilution);
      break;
    }
  }

  private Predicate<SampleDto> getSpecialFilter(String predicateFilter, Map<String, SampleDto> allSamples) {
    switch (predicateFilter) {
    case "P":
    case "S":
    case "A":
      return byPrimary(allSamples);
    case "R":
      return byReference(allSamples);
    case "O":
      return byOrganoid(allSamples);
    case "X":
      return byXeno(allSamples);
    case "M":
      return byMetastatic(allSamples);
    default:
      return byLeftover(allSamples);
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

  private Predicate<SampleDto> by10XLibrary(Map<String, SampleDto> allSamplesById) {
    return dto -> is10XLibrary(dto, allSamplesById);
  }

  private Predicate<SampleDto> byLibraryDesignCodes(List<String> libraryDesignCodes) {
    return dto -> libraryDesignCodes.contains(getAttribute(ATTR_SOURCE_TEMPLATE_TYPE, dto));
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
