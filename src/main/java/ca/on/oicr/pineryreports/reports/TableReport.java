package ca.on.oicr.pineryreports.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Sets;
import com.itextpdf.io.font.FontConstants;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.property.TextAlignment;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.Main;
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.data.ReportFormat;
import ca.on.oicr.pineryreports.reports.pdf.AddHeaderFooterEvent;
import ca.on.oicr.pineryreports.util.GeneralUtils;

/**
 * A type of report that has one table containing all of the data
 */
public abstract class TableReport implements Report {

  private static final String EMPTY = "";
  private static final String COMMA = ",";
  private static final String NEWLINE = "\n";
  private static final String QUOTE = "\"";
  private static final Map<String, String> optionsUsed = new TreeMap<>();
  
  public TableReport() {
    
  }

  @Override
  public Collection<ReportFormat> getValidFormats() {
    return Sets.newHashSet(ReportFormat.CSV, ReportFormat.PDF);
  }

  @Override
  public ReportFormat getDefaultFormat() {
    return ReportFormat.CSV;
  }

  @Override
  public final void generate(PineryClient pinery, ReportFormat format, File outFile) throws HttpResponseException, IOException {
    collectData(pinery);
    
    switch (format) {
    case CSV:
      generateCsv(outFile);
      break;
    case PDF:
      generatePdf(outFile);
      break;
    default:
      // should be unreachable
      throw new IllegalArgumentException("Invalid report format: " + format);
    }
  }
  
  private void generateCsv(File outFile) throws IOException {
    try (FileWriter writer = new FileWriter(outFile)) {
      String[] headings = getColumnHeadings(getColumns());
      writer.write(toCsvLine(headings));
      writer.write(NEWLINE);
      
      for (int i = 0, count = getRowCount(); i < count; i++) {
        String[] row = getRow(i);
        writer.write(toCsvLine(row));
        writer.write(NEWLINE);
      }
    }
  }
  
  private String toCsvLine(String[] rowData) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < rowData.length; i++) {
      if (i > 0) {
        sb.append(COMMA);
      }
      sb.append(QUOTE);
      sb.append(emptyIfNull(rowData[i]));
      sb.append(QUOTE);
    }
    return sb.toString();
  }
  
  private void generatePdf(File outFile) throws IOException {
    try (PdfWriter writer = new PdfWriter(outFile);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.LETTER.rotate())) {
      IEventHandler headerFooter = new AddHeaderFooterEvent(getTitle());
      pdf.addEventHandler(PdfDocumentEvent.END_PAGE, headerFooter);
      PdfFont regularFont = PdfFontFactory.createFont(FontConstants.TIMES_ROMAN);
      PdfFont boldFont = PdfFontFactory.createFont(FontConstants.TIMES_BOLD);
      
      document.add(new Paragraph(getTitle())
          .setFont(boldFont)
          .setFontSize(16f)
          .setTextAlignment(TextAlignment.CENTER));
      
      List<ColumnDefinition> columns = getColumns();
      float[] columnSizes = getRelativeWidths(columns);
      Table table = new Table(columnSizes)
          .setWidthPercent(100f);
      String[] headings = getColumnHeadings(columns);
      writeTableRow(table, headings, boldFont, true);
      for (int i = 0, count = getRowCount(); i < count; i++) {
        writeTableRow(table, getRow(i), regularFont, false);
      }
      document.add(table);
    }
  }
  
  private void writeTableRow(Table table, String[] rowData, PdfFont font, boolean isHeader) {
    List<ColumnDefinition> columns = getColumns();
    for (int i = 0; i < rowData.length; i++) {
      String text = emptyIfNull(rowData[i]);
      Paragraph p = new Paragraph(text)
          .setFont(font);
      Cell cell = new Cell()
          .setTextAlignment(columns.get(i).getAlignment())
          .add(p);
      if (isHeader) {
        if (columns.get(i).getAlignment() == TextAlignment.RIGHT) {
          cell.setTextAlignment(TextAlignment.CENTER);
        }
        p.setFontSize(11f);
        table.addHeaderCell(cell);
      } else {
        p.setFontSize(10f);
        table.addCell(cell);
      }
    }
  }
  
  private static float[] getRelativeWidths(List<ColumnDefinition> columns) {
    float[] widths = new float[columns.size()];
    for (int i = 0; i < widths.length; i++) {
      widths[i] = columns.get(i).getRelativeWidth();
    }
    return widths;
  }
  
  protected static String[] getColumnHeadings(List<ColumnDefinition> columns) {
    String[] headings = new String[columns.size()];
    for (int i = 0; i < headings.length; i++) {
      headings[i] = columns.get(i).getHeading();
    }
    return headings;
  }
  
  private static String emptyIfNull(String string) {
    return string == null ? EMPTY : string;
  }
  
  /**
   * Collect all necessary data from Pinery. Data must be organised in a way that the row count is known, and that row data may
   * be retreived by row number after this method completes
   * 
   * @param pinery data source
   * @throws HttpResponseException
   */
  protected abstract void collectData(PineryClient pinery) throws HttpResponseException, IOException;
  
  /**
   * @return the column definitions
   */
  protected abstract List<ColumnDefinition> getColumns();
  
  /**
   * @return the total number of data rows that will be added to the table. Must not be called before {@link #collectData(PineryClient)}
   */
  protected abstract int getRowCount();
  
  /**
   * Get the data for a specific table row. Must not be called before {@link #collectData(PineryClient)}
   * 
   * @param rowNum table row number
   * @return the column data for the specified row. Must return an array of the same size and in the same order as {@link #getColumns()}
   */
  protected abstract String[] getRow(int rowNum);

  private final ObjectMapper mapper = new ObjectMapper();
  private final HttpClient guanyinClient = HttpClientBuilder.create().build();

  @Override
  public final void registerReportWithGuanyin(String guanyinUrl) throws HttpResponseException, IOException {
    if (guanyinUrl == null || guanyinUrl.length() == 0) return;
    if (getReportFromGuanyin(guanyinUrl) != null) {
      // this version of this report already exists in Guanyin, so no need to register it
      return;
    }
    ObjectNode registrationInfo = getRegistration();
    System.out.println(String.format("Registering report %s with Guanyin (%s)", getReportName(), guanyinUrl));
    sendRegistration(guanyinUrl, registrationInfo);
  }

  /**
   * Queries Guanyin to see if it has a report with matching information.
   * Guanyin returns all reports with the same name, so this will only return a report if the version also matches.
   *
   * @return whether Guanyin has a report with matching name and version
   * @throws IOException
   */
  private JsonNode getReportFromGuanyin(String guanyinUrl) throws IOException {
    HttpGet get = new HttpGet(String.format("%s/reportdb/report?name=%s", guanyinUrl, getReportName()));
    try {
      HttpResponse response = guanyinClient.execute(get);
      JsonNode body = mapper.readTree(EntityUtils.toString(response.getEntity()));
      ArrayNode contents = (ArrayNode) body;
      JsonNode target = null;
      for (JsonNode report : contents) {
        if (getVersion().equals(report.get("version").asText())) target = report;
      }
      return target;
    } catch (IOException e) {
      System.out.println(String.format("Error querying Guanyin for report %s with version %s", getReportName(), getVersion()));
      throw e;
    }
  }

  private ObjectNode getRegistration() {
    ObjectNode registration = mapper.createObjectNode();
    registration.put("category", this.getCategory());
    registration.put("name", this.getReportName());
    registration.put("version", getVersion());
    registration.set("permitted_parameters", getPermittedParams());
    return registration;
  }

  private ObjectNode getReportRecordJson(Integer reportId, File outputFile) throws IOException {
    ObjectNode record = mapper.createObjectNode();
    String rightNow = GeneralUtils.getDateTimeFormat().format(new Date());
    BasicFileAttributes attrs = Files.readAttributes(outputFile.toPath(), BasicFileAttributes.class);
    record.put("report_id", reportId);
    record.put("date_generated", attrs.creationTime().toString());
    record.put("freshest_input_date", rightNow);
    // fake the values for files_in since we're pulling from Pinery
    ArrayNode filesIn = mapper.createArrayNode();
    filesIn.add(String.format("Pinery data retrieved %s", rightNow));
    record.set("files_in", filesIn);
    record.put("report_path", outputFile.getCanonicalPath());
    record.set("notification_targets", mapper.createObjectNode());
    record.put("notification_message", "");
    ObjectNode parameters = mapper.createObjectNode();
    getOptionsUsed().entrySet().stream().forEach(e -> {
      parameters.put(e.getKey(), e.getValue());
    });
    record.set("parameters", parameters);
    return record;
  }

  /**
   * Registers this report with Guanyin
   * 
   * @throws IOException
   * @throws ClientProtocolException
   */
  private void sendRegistration(String guanyinUrl, ObjectNode registration) throws IOException {
    HttpPost post = new HttpPost(String.format("%s/reportdb/report", guanyinUrl));
    StringEntity body = new StringEntity(mapper.writeValueAsString(registration));
    post.setEntity(body);
    post.setHeader("Accept", "application/json");
    post.setHeader("Content-Type", "application/json");
    try {
      HttpResponse response = guanyinClient.execute(post);
      if (201 != response.getStatusLine().getStatusCode()) {
        throw new IllegalArgumentException(
            String.format("Error registering report with Guanyin for %s version %s: return code is %d and response body is: %s",
                getReportName(), getVersion(), response.getStatusLine().getStatusCode(), response.getEntity().toString()));
      }
    } catch (IOException e) {
      System.out.println(String.format("Error registering report %s with Guanyin", getReportName()));
      throw e;
    }
  }

  private final ObjectNode getPermittedParams() {
    ObjectNode permittedParams = mapper.createObjectNode();
    for (Option option : getOptions()) {
      ObjectNode param = mapper.createObjectNode();
      param.put("required", option.isRequired());
      param.put("type", "s"); // treat everything as strings, including dates
      permittedParams.set(option.getLongOpt(), param);
    }
    return permittedParams;
  }

  private final Package mainPackage = Main.class.getPackage();

  private final String getVersion() {
    return mainPackage.getImplementationVersion();
  }

  @Override
  public void writeGuanyinReportRecordParameters(String guanyinUrl, File outputFile) throws IOException {
    JsonNode report = getReportFromGuanyin(guanyinUrl);
    if (report == null) throw new IllegalArgumentException(String
        .format("Could not find report %s with version %s in Guanyin so could not write parameters for report record", getReportName(),
            getVersion()));
    JsonNode reportRecord = getReportRecordJson(report.get("report_id").asInt(), outputFile);
    
    String outputFileDir = outputFile.getParent();
    String outputFilePath = "";
    if (outputFileDir != null) outputFilePath += outputFileDir + "/";
    outputFilePath += FilenameUtils.removeExtension(outputFile.getName());
    outputFilePath += ".guanyin.json";
    System.out.println("Guanyin report record JSON generated: " + outputFilePath);
    ObjectWriter writer = mapper.writer();
    writer.writeValue(new File(outputFilePath), reportRecord);
  }

  @Override
  public Map<String, String> getOptionsUsed() {
    return optionsUsed;
  }

  @Override
  public void recordOptionsUsed(CommandLine cmd) {
    for (Option opt : getOptions()) {
      if (cmd.hasOption(opt.getLongOpt())) {
        optionsUsed.put(opt.getLongOpt(), cmd.getOptionValue(opt.getLongOpt()));
      }
    }
  }
}
