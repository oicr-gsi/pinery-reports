package ca.on.oicr.pineryreports.reports.impl;

import static ca.on.oicr.pineryreports.util.SampleUtils.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import com.google.common.collect.Sets;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pinery.client.SampleClient.SamplesFilter;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.reports.TableReport;
import ca.on.oicr.pineryreports.util.CommonOptions;
import ca.on.oicr.ws.dto.SampleDto;

public class LibrariesBillingReport extends TableReport {

	private static class ReportObject {
		private final String project;
		private final String kit;
		private Integer libsCount = 0;
		
		public ReportObject(String project, String kit) {
		  this.project = project;
		  this.kit = kit;
		  this.libsCount = 1;
		}
		
		public String getProject() {
		  return project;
		}
		
		public String getKit() {
		  return kit;
		}
		
		public Integer getLibrariesCount() {
		  return libsCount;
		}
		
		public void addOneLibrary() {
		  libsCount += 1;
		}
		
		private static final Comparator<ReportObject> reportObjectComparator = new Comparator<ReportObject>() {
      @Override
      public int compare(ReportObject o1, ReportObject o2) {
        if (o1.getProject().equals(o2.getProject())) {
          return o2.getKit().compareTo(o2.getKit());
        } else {
          // sort on this primarily
          return o1.getProject().compareTo(o2.getProject());
        }
      }
    };
	}
	
	public static final String REPORT_NAME = "libraries-billing";
	private static final Option OPT_AFTER = CommonOptions.after(false);
    private static final Option OPT_BEFORE = CommonOptions.before(false);
	
	private static final String DATE_REGEX = "\\d{4}-\\d{2}-\\d{2}";
  private String start;
  private String end;

	@Override
	public String getReportName() {
		return REPORT_NAME;
	}

	@Override
	public Collection<Option> getOptions() {
	  return Sets.newHashSet(OPT_AFTER, OPT_BEFORE);
	}

	@Override
	public void processOptions(CommandLine cmd) throws ParseException {
	  if (cmd.hasOption(OPT_AFTER.getLongOpt())) {
      String after = cmd.getOptionValue(OPT_AFTER.getLongOpt());
      if (!after.matches(DATE_REGEX)) {
        throw new ParseException("After date must be in format yyyy-mm-dd");
      }
      this.start = after;
    }
    
    if (cmd.hasOption(OPT_BEFORE.getLongOpt())) {
      String before = cmd.getOptionValue(OPT_BEFORE.getLongOpt());
      if (!before.matches(DATE_REGEX)) {
        throw new ParseException("Before date must be in format yyyy-mm-dd");
      }
      this.end = before;
    }
	}

	@Override
	public String getTitle() {
		return "Billing Librarires "
				+ (start == null ? "Any Time" : start)
				+ " - "
				+ (end == null ? "Now" : end);
	}
  
  //{ shortName: { kitName: [libs] } }
	private List<ReportObject> reportData = new ArrayList<ReportObject>();
  
	@Override
	protected void collectData(PineryClient pinery) throws HttpResponseException {
	  // filter by libraries
	  List<String> libraryTypes = Arrays.asList("Illumina PE Library", "Illumina SE Library");
		List<SampleDto> allLibraries = pinery.getSample().allFiltered(new SamplesFilter().withTypes(libraryTypes));
		
		// filter down to libraries within the date range
		List<SampleDto> newLibraries = allLibraries.stream().filter(byCreatedBetween(start, end)).collect(Collectors.toList());
				
		for (SampleDto lib : newLibraries) {
		  String kitName = lib.getPreparationKit().getName();
		  String project = lib.getProjectName();
		  
 		  ReportObject matchingKitAndProject = reportData.stream()
 		      .filter(row -> row.getKit().equals(kitName) && row.getProject().equals(project))
 		      .findAny().orElse(null);
		  if (matchingKitAndProject == null) {
		    // new project & kit combo
		    matchingKitAndProject = new ReportObject(project, kitName);
		    reportData.add(matchingKitAndProject);
		  } else {
		    // existing project & kit combo
		    matchingKitAndProject.addOneLibrary();
		  }
		}
		
		reportData.sort(ReportObject.reportObjectComparator);
	}

	@Override
	protected List<ColumnDefinition> getColumns() {
	  return Arrays.asList(
      new ColumnDefinition("Study Title"),
      new ColumnDefinition("Library Kit"),
      new ColumnDefinition(String.format("Count (%s - %s)", start, end))
    );
	}

	@Override
	protected int getRowCount() {
		return reportData.size();
	}

	@Override
	protected String[] getRow(int rowNum) {
		ReportObject obj = reportData.get(rowNum);
		String[] row = new String[getColumns().size()];
		
		int i = -1;
		// Project
		row[++i] = obj.getProject();
		// Kit
		row[++i] = obj.getKit();
		// Count of libraries using this kit in this project
		row[++i] = obj.getLibrariesCount().toString();
		
		return row;
	}
	
}
