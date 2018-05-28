package ca.on.oicr.pineryreports.reports;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
import ca.on.oicr.pineryreports.data.ColumnDefinition;
import ca.on.oicr.pineryreports.data.ReportFormat;
import ca.on.oicr.pineryreports.reports.pdf.AddHeaderFooterEvent;

/**
 * A type of report that has one table containing all of the data
 */
public abstract class TableReport implements Report {

  private static final String EMPTY = "";
  private static final String COMMA = ",";
  private static final String NEWLINE = "\n";
  private static final String QUOTE = "\"";
  
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
  
  private static String[] getColumnHeadings(List<ColumnDefinition> columns) {
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

}
