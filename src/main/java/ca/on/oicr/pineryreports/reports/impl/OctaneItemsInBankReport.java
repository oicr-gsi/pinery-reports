package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.DATE_FORMAT;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OctaneItemsInBankReport extends TableReport {

  public static final String REPORT_NAME = "octane-bank";

  private static final Logger LOG = LoggerFactory.getLogger(OctaneItemsInBankReport.class);
  private static final Option OPT_USER_IDS = CommonOptions.users(true);

  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(
      Arrays.asList(
          new ColumnDefinition("Donor ID"),
          new ColumnDefinition("Buffy Coat Aliquots Remaining"),
          new ColumnDefinition("cfDNA Plasma Aliquots Remaining"),
          new ColumnDefinition("Plasma Aliquots Remaining"),
          new ColumnDefinition("Tumour Tissue Remaining (# slides)"),
          new ColumnDefinition("Tumour DNA Available"),
          new ColumnDefinition("Tumour RNA Available"),
          new ColumnDefinition("Buffy Coat DNA Available")));

  private static final Predicate<SampleDto> byTransferred = dto -> {
    String custody = getAttribute(ATTR_CUSTODY, dto);
    return custody != null
        && !"TP".equals(custody)
        && !"Unspecified (Internal)".equals(custody);
  };

  private final List<Integer> userIds = new ArrayList<>();

  private Map<String, SampleDto> allSamplesById;
  private List<SampleDto> octaneSamples;

  @Override
  public String getReportName() {
    return REPORT_NAME;
  }

  @Override
  public Collection<Option> getOptions() {
    return Sets.newHashSet(OPT_USER_IDS);
  }

  @Override
  public void processOptions(CommandLine cmd) throws ParseException {
    if (cmd.hasOption(OPT_USER_IDS.getLongOpt())) {
      String[] users = cmd.getOptionValue(OPT_USER_IDS.getLongOpt()).split(",");
      for (String user : users) {
        if (user != null && !"".equals(user)) {
          userIds.add(Integer.valueOf(user));
        }
      }
    }
  }

  @Override
  public String getTitle() {
    return String.format(
        "OCTANE - Items in Bank (%s)", new SimpleDateFormat(DATE_FORMAT).format(new Date()));
  }

  @Override
  public String getCategory() {
    return "counts";
  }

  private final List<String[]> rowData = new ArrayList<>();

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException, IOException {
    List<SampleDto> allSamples = pinery.getSample().all();
    allSamplesById = mapSamplesById(allSamples);
    octaneSamples = allSamples.stream()
        .filter(
            sam -> "OCT".equals(sam.getProjectName()) || "OCTCAP".equals(sam.getProjectName()))
        .collect(Collectors.toList());

    List<SampleDto> identities = findIdentities();
    Map<String, List<SampleDto>> childrenByIdentityId = mapChildrenByIdentityId();

    for (SampleDto identity : identities) {
      List<SampleDto> children = childrenByIdentityId.get(identity.getId());
      if (children == null) {
        children = Collections.emptyList();
      }
      String[] row = new String[COLUMNS.size()];
      int col = -1;
      row[++col] = getAttribute(ATTR_EXTERNAL_NAME, identity);

      col = addDnaCount(row, col, children, "Ly", "R");
      col = addDnaCount(row, col, children, "Ct", "T");
      col = addDnaCount(row, col, children, "Pl", "R");
      col = addTumourCounts(row, col, children);
      col = addAnyRemaining(row, col, children, "Ly", "R");

      // Exclude Identities with no other data
      for (int i = 1; i < row.length; i++) {
        String value = row[i];
        if (value != null && !value.isEmpty() && !"0".equals(value) && !"0.0".equals(value)) {
          rowData.add(row);
          break;
        }
      }
    }
  }

  private List<SampleDto> findIdentities() {
    return octaneSamples.stream()
        .filter(bySampleCategory(SAMPLE_CATEGORY_IDENTITY))
        .collect(Collectors.toList());
  }

  private Map<String, List<SampleDto>> mapChildrenByIdentityId() {
    Map<String, List<SampleDto>> childrenByIdentityId = new HashMap<>();
    for (SampleDto sample : octaneSamples) {
      if (!SAMPLE_CATEGORY_IDENTITY.equals(getAttribute(ATTR_CATEGORY, sample))) {
        SampleDto identity = getParent(sample, SAMPLE_CATEGORY_IDENTITY, allSamplesById);
        if (!childrenByIdentityId.containsKey(identity.getId())) {
          childrenByIdentityId.put(identity.getId(), new ArrayList<>());
        }
        childrenByIdentityId.get(identity.getId()).add(sample);
      }
    }
    return childrenByIdentityId;
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return rowData.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    return rowData.get(rowNum);
  }

  private int addDnaCount(
      String[] row, int col, List<SampleDto> children, String tissueOrigin, String tissueType) {
    List<SampleDto> filtered = filterByTissueOriginAndType(children, tissueOrigin, tissueType);
    row[++col] = Long.toString(countTissues(filtered, false));
    return col;
  }

  private int addAnyRemaining(String[] row, int col, List<SampleDto> children, String tissueOrigin,
      String tissueType) {
    List<SampleDto> filtered = filterByTissueOriginAndType(children, tissueOrigin, tissueType);
    row[++col] = anyRemaining(filtered, DNA);
    return col;
  }

  private List<SampleDto> filterByTissueOriginAndType(List<SampleDto> children, String tissueOrigin,
      String tissueType) {
    return children.stream()
        .filter(byTissueOriginAndType(tissueOrigin, tissueType, allSamplesById))
        .collect(Collectors.toList());
  }

  private long countTissues(List<SampleDto> children, boolean empty) {
    return children.stream()
        .filter(bySampleCategory(SAMPLE_CATEGORY_TISSUE))
        .filter(byEmpty(empty))
        .filter(byCreator(userIds))
        .count();
  }

  private String anyRemaining(List<SampleDto> children, String dnaOrRna) {
    return getRemaining(children, dnaOrRna) > 0F ? "Yes" : "No";
  }

  private float getRemaining(List<SampleDto> children, String dnaOrRna) {
    return children.stream()
        .filter(bySampleCategory(SAMPLE_CATEGORY_STOCK))
        .filter(dto -> dto.getSampleType().contains(dnaOrRna))
        .filter(byTransferred.negate())
        .filter(byCreator(userIds))
        .map(
            dto -> {
              if (dto.getVolume() == null || dto.getConcentration() == null) {
                LOG.warn(
                    String.format(
                        "Stock sample %s (%s) missing volume (%f) or concentration (%f)",
                        dto.getId(), dto.getName(), dto.getVolume(), dto.getConcentration()));
                return 0F;
              } else {
                return dto.getVolume() * dto.getConcentration();
              }
            })
        .reduce((a, b) -> a + b)
        .orElse(0F);
  }

  private int addTumourCounts(String[] row, int col, List<SampleDto> children) {
    row[++col] = Integer.toString(countSlides(children, false));

    // filter to only include items descended from slides
    List<SampleDto> filtered = children.stream()
        .filter(dto -> getOptionalParent(dto, SAMPLE_CLASS_SLIDE, allSamplesById) != null)
        .collect(Collectors.toList());

    row[++col] = anyRemaining(filtered, DNA);
    row[++col] = anyRemaining(filtered, RNA);
    return col;
  }

  private int countSlides(List<SampleDto> children, boolean empty) {
    return children.stream()
        .filter(dto -> SAMPLE_CLASS_SLIDE.equals(dto.getSampleType()))
        // for exhausted (empty == true), include partially-used
        // for remaining (empty == false), only count if not "EMPTY"
        .filter(empty ? s -> true : byEmpty(false))
        .filter(byCreator(userIds))
        .mapToInt(
            dto -> empty
                ? (getIntAttribute(ATTR_INITIAL_SLIDES, dto)
                    - getIntAttribute(ATTR_SLIDES, dto))
                : getIntAttribute(ATTR_SLIDES, dto))
        .sum();
  }
}
