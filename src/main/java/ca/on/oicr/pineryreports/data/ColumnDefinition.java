package ca.on.oicr.pineryreports.data;

import com.itextpdf.layout.property.TextAlignment;

public class ColumnDefinition {
  
  private final String heading;
  private final float relativeWidth;
  private final TextAlignment alignment;

  /**
   * Creates a new column Definition with the specified attributes
   * 
   * @param heading column heading text
   * @param width a width suggestion - this width is not guaranteed to be exact as the final column width can be affected by other 
   * things, but if the column is coming out too narrow, this should be increased
   * @param alignment
   */
  public ColumnDefinition(String heading, float width, TextAlignment alignment) {
    this.heading = heading;
    this.relativeWidth = width;
    this.alignment = alignment;
  }
  
  /**
   * Creates a new ColumnDefinition with the specified heading and alignment, and automatic width 
   * 
   * @param heading column heading text
   * @param alignment horizontal text alignment
   */
  public ColumnDefinition(String heading, TextAlignment alignment) {
    this(heading, 1f, alignment);
  }
  
  /**
   * Creates a new ColumnDefinition with the specified heading, automatic width, and left alignment
   * 
   * @param heading column heading text
   */
  public ColumnDefinition(String heading) {
    this(heading, 1f, TextAlignment.LEFT);
  }

  public String getHeading() {
    return heading;
  }

  public float getRelativeWidth() {
    return relativeWidth;
  }

  public TextAlignment getAlignment() {
    return alignment;
  }

}
