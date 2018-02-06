package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ReceiptMissingReport extends TableReport {

  private static class ReportObject {
    
    private final SampleDto tissue;
    private final SampleDto slide;
    
    public ReportObject(SampleDto tissue, SampleDto slide) {
      this.tissue = tissue;
      this.slide = slide;
    }
    
    public SampleDto getTissue() {
      return tissue;
    }
    
    public SampleDto getSlide() {
      return slide;
    }
    
  }
  
  public static final String REPORT_NAME = "receipt-missing";
  
  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(Arrays.asList(
      new ColumnDefinition("Tissue ID"),
      new ColumnDefinition("Tissue Alias"),
      new ColumnDefinition("Slide ID"),
      new ColumnDefinition("Slide Alias")
  ));
  
  private static final Option OPT_PROJECT = CommonOptions.project(true);
  
  private String project;
  
  private List<ReportObject> rows;
  
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
    this.project = cmd.getOptionValue(OPT_PROJECT.getLongOpt());
  }

  @Override
  public String getTitle() {
    return project + " Samples Missing Received Date";
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> samples = pinery.getSample().allFiltered(new SamplesFilter().withProjects(Lists.newArrayList(project)));
    Map<String, SampleDto> allSamplesById = mapSamplesById(samples);
    
    Map<SampleDto, List<SampleDto>> slidesByTissue = samples.stream()
        .filter(sam -> "Slide".equals(sam.getSampleType()))
        .collect(Collectors.groupingBy(slide -> getParent(slide, "Tissue", allSamplesById)));
    samples.stream()
        .filter(bySampleCategory("Tissue"))
        .forEach(tissue -> {
          if (!slidesByTissue.containsKey(tissue)) {
            slidesByTissue.put(tissue, Lists.newArrayList());
          }
        });
    
    rows = new ArrayList<>();
    slidesByTissue.forEach((tissue, slides) -> {
      if (getAttribute("Receive Date", tissue) == null) {
        if (slides.isEmpty()) {
          rows.add(new ReportObject(tissue, null));
        } else {
          slides.forEach(slide -> {
            if (getAttribute("Receive Date", slide) == null) {
              rows.add(new ReportObject(tissue, slide));
            }
          });
        }
      }
    });
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return rows.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    String[] row = new String[COLUMNS.size()];
    
    ReportObject data = rows.get(rowNum);
    
    int i = -1;
    row[++i] = data.getTissue().getId();
    row[++i] = data.getTissue().getName();
    row[++i] = data.getSlide() == null ? "-" : data.getSlide().getId();
    row[++i] = data.getSlide() == null ? "-" : data.getSlide().getName();
    
    return row;
  }

}
