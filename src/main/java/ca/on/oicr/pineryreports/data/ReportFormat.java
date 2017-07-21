package ca.on.oicr.pineryreports.data;

public enum ReportFormat {
  CSV(".csv"), PDF(".pdf");
  
  private static final String COMMA = ",";
  
  private final String extension;
  
  private ReportFormat(String extension) {
    this.extension = extension;
  }
  
  public static String getListString() {
    StringBuilder sb = new StringBuilder();
    for (ReportFormat format : ReportFormat.values()) {
      sb.append(format.toString().toLowerCase()).append(COMMA);
    }
    return sb.toString();
  }
  
  public static ReportFormat get(String format) {
    return ReportFormat.valueOf(format.trim().toUpperCase());
  }
  
  public String getExtension() {
    return extension;
  }
  
}
