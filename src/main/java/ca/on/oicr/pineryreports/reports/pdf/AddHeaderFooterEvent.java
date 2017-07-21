package ca.on.oicr.pineryreports.reports.pdf;

import java.io.IOException;

import com.itextpdf.io.font.FontConstants;
import com.itextpdf.kernel.events.Event;
import com.itextpdf.kernel.events.IEventHandler;
import com.itextpdf.kernel.events.PdfDocumentEvent;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.property.Property;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.VerticalAlignment;

public class AddHeaderFooterEvent implements IEventHandler {
  
  private static final float FONT_SIZE = 8f;
  private static final float MARGIN = 40f; // Assuming LETTER PageSize with default margin
  
  private final PdfFont font;
  private final String title;
  
  public AddHeaderFooterEvent(String title) throws IOException {
    this.title = title;
    font = PdfFontFactory.createFont(FontConstants.TIMES_ROMAN);
  }

  @Override
  public void handleEvent(Event event) {
    PdfDocumentEvent docEvent = (PdfDocumentEvent) event;
    PdfDocument pdfDoc = docEvent.getDocument();
    PdfPage page = docEvent.getPage();
    int pageNumber = pdfDoc.getPageNumber(page);
    Rectangle pageSize = page.getPageSize();
    
    PdfCanvas pdfCanvas = null;
    try {
      pdfCanvas = new PdfCanvas(page.newContentStreamBefore(), page.getResources(), pdfDoc);
      
      try (Canvas canvas = new Canvas(pdfCanvas, pdfDoc, page.getPageSize())) {
        canvas.setProperty(Property.FONT, font);
        canvas.setProperty(Property.FONT_SIZE, FONT_SIZE);
        float marginCenter = MARGIN/2;
        
        // Add title to header of all but the first page
        if (pageNumber > 1) {
          Paragraph headerParagraph = new Paragraph(title);
          canvas.showTextAligned(headerParagraph, marginCenter, pageSize.getTop() - marginCenter, TextAlignment.LEFT, VerticalAlignment.MIDDLE);
        }
        
        // Add page numbers to footer
        Paragraph footerParagraph = new Paragraph(Integer.toString(pageNumber));
        canvas.showTextAligned(footerParagraph, pageSize.getWidth() - marginCenter, marginCenter, TextAlignment.RIGHT, VerticalAlignment.MIDDLE);
      }
    } finally {
      if (pdfCanvas != null) {
        pdfCanvas.release();
      }
    }
  }

}
