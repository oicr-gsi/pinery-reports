package ca.on.oicr.pineryreports.reports;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ReportFormat;

public interface Report {
  
  /**
   * @return report name used as command-line argument and shown in help message. Must not depend on report data
   */
  public String getReportName();
  
  /**
   * @return report-specific options, which are combined with the main options (See {@link ca.on.oicr.pineryreports.Main Main})
   */
  public Collection<Option> getOptions();
  
  /**
   * @return the file types that this report can output
   */
  public Collection<ReportFormat> getValidFormats();
  
  /**
   * @return the default file type output for this report
   */
  public ReportFormat getDefaultFormat();
  
  /**
   * Extracts all configuration from the command line. Only report-specific options should be handled here
   * 
   * @param cmd the parsed command line
   * @throws ParseException if there are any invalid arguments. The message should be user-friendly for display in a help message
   */
  public void processOptions(CommandLine cmd) throws ParseException;
  
  /**
   * Returns the title of this report. Must not be called before {@link #processOptions(CommandLine)}
   */
  public String getTitle();
  
  /**
   * Collect data from Pinery, generate the report according to the options, and write it to file. Must not be called
   * before {@link #processOptions(CommandLine)}
   * 
   * @param pinery data source
   * @param format output file type
   * @param outFile output file
   */
  public void generate(PineryClient pinery, ReportFormat format, File outFile) throws HttpResponseException, IOException;
  
}
