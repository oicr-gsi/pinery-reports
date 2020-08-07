package ca.on.oicr.pineryreports;

import ca.on.oicr.pinery.client.HttpResponseException;
import ca.on.oicr.pinery.client.PineryClient;
import ca.on.oicr.pineryreports.data.ReportFormat;
import ca.on.oicr.pineryreports.reports.Report;
import ca.on.oicr.pineryreports.reports.impl.*;
import java.io.File;
import java.io.IOException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger LOG = LoggerFactory.getLogger(Main.class);

  private static final String OPT_SOURCE = "source";
  private static final String OPT_GUANYIN = "guanyin";
  private static final String OPT_REPORT = "report";
  private static final String OPT_FORMAT = "format";
  private static final String OPT_OUTFILE = "outfile";

  public static void main(String[] args) {
    Options opts = getMainOptions();
    Report report = null;
    try {
      CommandLine mainCommand = getCommandLine(args, opts, false);

      String pineryOpt = mainCommand.getOptionValue(OPT_SOURCE);
      try (PineryClient pinery = new PineryClient(pineryOpt)) {
        String reportOpt = mainCommand.getOptionValue(OPT_REPORT);
        report = getReport(reportOpt);

        String formatOpt = mainCommand.getOptionValue(OPT_FORMAT);
        ReportFormat format =
            formatOpt == null ? report.getDefaultFormat() : ReportFormat.get(formatOpt);
        if (!report.getValidFormats().contains(format)) {
          throw new ParseException(String.format("Invalid format for this report: %s", formatOpt));
        }

        String outputOpt =
            mainCommand.getOptionValue(OPT_OUTFILE, OPT_REPORT + format.getExtension());
        File outFile = new File(outputOpt);
        if (outFile.exists()) {
          throw new ParseException(String.format("Output file already exists: %s", outputOpt));
        }

        String guanyinOpt = mainCommand.getOptionValue(OPT_GUANYIN);
        if (guanyinOpt != null) {
          guanyinOpt = guanyinOpt.trim().replaceAll("\\/$", ""); // remove trailing slash if present
          report.registerReportWithGuanyin(guanyinOpt);
        }

        for (Option opt : report.getOptions()) {
          opts.addOption(opt);
        }
        CommandLine reportCommand = getCommandLine(args, opts, true);
        report.processOptions(reportCommand);
        report.recordOptionsUsed(reportCommand);

        LOG.info("Options ok. Generating {}...", report.getTitle());
        report.generate(pinery, format, outFile);
        LOG.info("Report generated: {}", outFile.getName());
        if (guanyinOpt != null) report.writeGuanyinReportRecordParameters(guanyinOpt, outFile);
      }
    } catch (ParseException e) {
      LOG.error(e.getMessage());
      showHelp(report);
      System.exit(1);
    } catch (HttpResponseException e) {
      LOG.error("Error fetching data from Pinery", e);
      System.exit(2);
    } catch (IOException e) {
      LOG.error("Error writing to file", e);
      System.exit(3);
    }
  }

  private static CommandLine getCommandLine(String[] args, Options opts, boolean acceptExtras)
      throws ParseException {
    CommandLineParser parser = new DefaultParser();
    return parser.parse(opts, args, !acceptExtras);
  }

  private static Options getMainOptions() {
    Options opts = new Options();

    opts.addOption(
        Option.builder("s")
            .longOpt(OPT_SOURCE)
            .required()
            .hasArg()
            .argName("pinery-url")
            .desc("Pinery base URL")
            .build());
    opts.addOption(
        Option.builder("g")
            .longOpt(OPT_GUANYIN)
            .required(false)
            .hasArg()
            .argName("guanyin-url")
            .desc("Guanyin URL")
            .build());
    opts.addOption(
        Option.builder("r")
            .longOpt(OPT_REPORT)
            .required()
            .hasArg()
            .argName("name")
            .desc(
                "Report to generate {"
                    + "donor, "
                    + "donors-for-existing-samples, "
                    + "dys, "
                    + "gecco, "
                    + "lanes-billing, "
                    + "libraries-billing, "
                    + "libraries-sequencing, "
                    + "location-missing, "
                    + "octane, "
                    + "precise, "
                    + "precisecase, "
                    + "projects-status, "
                    + "receipt-missing, "
                    + "sequencing, "
                    + "slide, "
                    + "stock, "
                    + "stocks-by-concentration, "
                    + "tgl-libraries-run"
                    + "}")
            .build());
    opts.addOption(
        Option.builder("f")
            .longOpt(OPT_FORMAT)
            .hasArg()
            .argName("code")
            .desc("Output format. Options and default depend on report {csv, pdf}")
            .build());
    opts.addOption(
        Option.builder("o")
            .longOpt(OPT_OUTFILE)
            .hasArg()
            .argName("filename")
            .desc("Output file (Default: report.<format-code>")
            .build());

    return opts;
  }

  private static Report getReport(String reportName) throws ParseException {
    switch (reportName) {
      case StockReport.REPORT_NAME:
        return new StockReport();
      case GeccoReport.REPORT_NAME:
        return new GeccoReport();
      case DonorReport.REPORT_NAME:
        return new DonorReport();
      case ProjectSequencingReport.REPORT_NAME:
        return new ProjectSequencingReport();
      case OctaneCountsReport.REPORT_NAME:
        return new OctaneCountsReport();
      case ReceiptMissingReport.REPORT_NAME:
        return new ReceiptMissingReport();
      case SlideReport.REPORT_NAME:
        return new SlideReport();
      case LibrariesBillingReport.REPORT_NAME:
        return new LibrariesBillingReport();
      case LanesBillingReport.REPORT_NAME:
        return new LanesBillingReport();
      case TglLibrariesRunReport.REPORT_NAME:
        return new TglLibrariesRunReport();
      case DysReport.REPORT_NAME:
        return new DysReport();
      case LocationMissingReport.REPORT_NAME:
        return new LocationMissingReport();
      case BisqueProjectsStatusReport.REPORT_NAME:
        return new BisqueProjectsStatusReport();
      case GazpachoProjectStatusReport.REPORT_NAME:
        return new GazpachoProjectStatusReport();
      case PreciseReport.REPORT_NAME:
        return new PreciseReport();
      case LibrariesSequencingReport.REPORT_NAME:
        return new LibrariesSequencingReport();
      case DonorsForExistingSamplesReport.REPORT_NAME:
        return new DonorsForExistingSamplesReport();
      case StocksByConcentrationReport.REPORT_NAME:
        return new StocksByConcentrationReport();
      case PreciseCaseReport.REPORT_NAME:
        return new PreciseCaseReport();
      default:
        throw new ParseException("Invalid report requested: " + reportName);
    }
  }

  private static void showHelp(Report report) {
    HelpFormatter formatter = new HelpFormatter();
    Options mainOpts = getMainOptions();
    formatter.printHelp(
        "java -jar pinery-reports.jar -s <pinery-url> [-g <guanyin-url>] -r <report> [-f {pdf|csv}]"
            + " [-o <filename>] [report-specific options]",
        mainOpts);

    if (report != null) {
      Options reportOpts = new Options();
      for (Option opt : report.getOptions()) {
        reportOpts.addOption(opt);
      }
      LOG.info("");
      formatter.printHelp(
          String.format("Report-specific options (%s)", report.getReportName()), reportOpts);
    }
  }
}
