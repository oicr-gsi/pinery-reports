package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.REPORT_CATEGORY_INVENTORY;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.RunDto;
import ca.on.oicr.ws.dto.RunDtoPosition;
import ca.on.oicr.ws.dto.RunDtoSample;
import ca.on.oicr.ws.dto.SampleDto;
import ca.on.oicr.ws.dto.SampleReferenceDto;
import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.itextpdf.layout.property.TextAlignment;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

/** GECCO Report https://jira.oicr.on.ca/browse/GC-875 */
public class GeccoReport extends TableReport {

  public static final String REPORT_NAME = "gecco";
  public static final String CATEGORY = REPORT_CATEGORY_INVENTORY;

  private final List<ColumnDefinition> columns =
      Collections.unmodifiableList(
          Arrays.asList(
              new ColumnDefinition("Oicr Name", 150f, TextAlignment.LEFT),
              new ColumnDefinition("Person ID"),
              new ColumnDefinition("Sample ID"),
              new ColumnDefinition("Gender"),
              new ColumnDefinition("Tissue of Origin", 50f, TextAlignment.LEFT),
              new ColumnDefinition("Tissue Type"),
              new ColumnDefinition("Libraries Created"),
              new ColumnDefinition("Times Sequenced"),
              new ColumnDefinition("Runs")));

  private Map<String, SampleDto> allSamplesById;
  private List<SampleDto> geccoTissues;
  private Map<String, Set<RunDto>> sequencerRunMap;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Collections.emptySet();
  }

  @Override
  public String getCategory() {
    return CATEGORY;
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    // No options (could be expanded to report for any project, etc)
  }

  @Override
  public String getTitle() {
    return "GECCO Report";
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> allSamples = pinery.getSample().all();
    allSamplesById = mapSamplesById(allSamples);
    List<RunDto> sequencerRuns = pinery.getSequencerRun().all();
    sequencerRunMap = sequencerRunHash(sequencerRuns);
    geccoTissues =
        filter(filter(allSamples, byProject("GECCO")), bySampleCategory(SAMPLE_CATEGORY_TISSUE));
    geccoTissues.sort((dto1, dto2) -> dto1.getName().compareTo(dto2.getName()));
  }

  /**
   * Map IDs of samples that have been run to the set of sequencer runs that they were included in
   *
   * @param runs all runs
   * @return the map
   */
  private static Map<String, Set<RunDto>> sequencerRunHash(List<RunDto> runs) {
    Map<String, Set<RunDto>> result = Maps.newHashMap();
    for (RunDto run : runs) {
      for (String sampleId : getSampleIdsInRun(run)) {
        if (result.containsKey(sampleId)) {
          result.get(sampleId).add(run);
        } else {
          Set<RunDto> runDtos = Sets.newHashSet();
          runDtos.add(run);
          result.put(sampleId, runDtos);
        }
      }
    }
    return result;
  }

  private static Set<String> getSampleIdsInRun(RunDto run) {
    Set<String> ids = Sets.newHashSet();
    if (run.getPositions() == null) {
      return ids;
    }
    for (RunDtoPosition position : run.getPositions()) {
      addSamplesFromPosition(position, ids);
    }
    return ids;
  }

  private static void addSamplesFromPosition(RunDtoPosition position, Set<String> collection) {
    if (position == null || position.getSamples() == null) {
      return;
    }
    for (RunDtoSample sample : position.getSamples()) {
      collection.add(sample.getId());
    }
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return columns;
  }

  @Override
  protected int getRowCount() {
    return geccoTissues.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    SampleDto tissue = geccoTissues.get(rowNum);
    String[] row = new String[columns.size()];

    row[0] = tissue.getName();
    row[1] = getUpstreamAttribute(ATTR_EXTERNAL_NAME, tissue, allSamplesById);
    row[2] = getAttribute(ATTR_TUBE_ID, tissue);
    row[3] = getUpstreamAttribute(ATTR_SEX, tissue, allSamplesById);
    row[4] = getAttribute(ATTR_TISSUE_ORIGIN, tissue);
    row[5] = getAttribute(ATTR_TISSUE_TYPE, tissue);
    Set<String> libSeqIds = getLibSeqIds(allSamplesById, tissue);
    row[6] = Integer.toString(libSeqIds.size());
    Set<String> runs = getRuns(sequencerRunMap, libSeqIds);
    row[7] = Integer.toString(runs.size());
    row[8] = Joiner.on("; ").skipNulls().join(runs);

    return row;
  }

  /**
   * Return set of run names associated with the given set of library seq ids.
   *
   * @param sequencerRunMap Hash of sample id to Run, used to lookup runs.
   * @param libSeqIds Library seq ids.
   * @return A set of run names
   */
  private static Set<String> getRuns(
      Map<String, Set<RunDto>> sequencerRunMap, Set<String> libSeqIds) {
    Set<String> result = Sets.newHashSet();
    for (String libSeqId : libSeqIds) {
      if (sequencerRunMap.get(libSeqId) != null) {
        for (RunDto runDto : sequencerRunMap.get(libSeqId)) {
          result.add(runDto.getName());
        }
      }
    }
    return result;
  }

  /**
   * Return the list of Library Seq sample ids that exist below the given sample.
   *
   * @param sampleMap Hash of sample id to Sample, used to navigate sample hierarchy.
   * @param sample Starting sample
   * @return List of Library Seq ids.
   */
  private static Set<String> getLibSeqIds(Map<String, SampleDto> sampleMap, SampleDto sample) {
    Set<String> result = Sets.newHashSet();
    return getLibSeqIds0(sampleMap, sample, result);
  }

  private static Set<String> getLibSeqIds0(
      Map<String, SampleDto> sampleMap, SampleDto sample, Set<String> result) {
    if (sample.getSampleType().contains("Library Seq")) {
      result.add(sample.getId());
    }
    if (sample.getChildren() != null) {
      for (SampleReferenceDto childRef : sample.getChildren()) {
        SampleDto child = sampleMap.get(childRef.getId());
        getLibSeqIds0(sampleMap, child, result);
      }
    }
    return result;
  }
}
