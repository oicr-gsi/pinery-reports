package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.GeneralUtils.OPT_PROJECT;
import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.ws.dto.SampleDto;

public class SlideReport extends TableReport {
  
  public static final String REPORT_NAME = "slide";
  
  private static final List<ColumnDefinition> COLUMNS = Collections.unmodifiableList(Arrays.asList(
      new ColumnDefinition("Slide ID"),
      new ColumnDefinition("Alias"),
      new ColumnDefinition("Stain"),
      new ColumnDefinition("Slides"),
      new ColumnDefinition("Discards")
  ));

  private String project;
  
  private List<SampleDto> slides;
  
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
    return project + " Slide Report " + new SimpleDateFormat("yyyy-MM-dd").format(new Date());
  }

  @Override
  protected void collectData(PineryClient pinery) throws HttpResponseException {
    List<SampleDto> samples = pinery.getSample().allFiltered(new SamplesFilter().withProjects(Lists.newArrayList(project)));
    slides = samples.stream()
        .filter(s -> SAMPLE_CLASS_SLIDE.equals(s.getSampleType()))
        .sorted((s1, s2) -> s1.getName().compareTo(s2.getName()))
        .collect(Collectors.toList());
  }

  @Override
  protected List<ColumnDefinition> getColumns() {
    return COLUMNS;
  }

  @Override
  protected int getRowCount() {
    return slides.size();
  }

  @Override
  protected String[] getRow(int rowNum) {
    String[] row = new String[COLUMNS.size()];
    SampleDto slide = slides.get(rowNum);
    String stain = getAttribute(ATTR_STAIN, slide);
    
    int i = -1;
    row[++i] = slide.getId();
    row[++i] = slide.getName();
    row[++i] = stain == null ? "unstained" : stain;
    row[++i] = getAttribute(ATTR_SLIDES, slide);
    row[++i] = getAttribute(ATTR_DISCARDS, slide);
    
    return row;
  }

}
