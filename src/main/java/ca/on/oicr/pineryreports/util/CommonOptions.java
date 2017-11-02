package ca.on.oicr.pineryreports.util;

import org.apache.commons.cli.Option;

public class CommonOptions {

  public static Option project(boolean required) {
    return Option.builder()
        .longOpt("project")
        .hasArg()
        .argName("code")
        .required(required)
        .desc("Project to report on" + (required ? " (required)" : ""))
        .build();
  }
  
  public static Option after(boolean required) {
    return Option.builder().longOpt("after")
        .hasArg()
        .argName("date")
        .required(required)
        .desc("Report receipt/creation after this date, inclusive (yyyy-mm-dd)")
        .build();
  }
  
  public static Option before(boolean required) {
    return Option.builder().longOpt("before")
        .hasArg()
        .argName("date")
        .required(required)
        .desc("Report receipt/creation before this date, exclusive (yyyy-mm-dd)")
        .build();
  }
  
}
