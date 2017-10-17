# Pinery Reports

[![Build Status](https://travis-ci.org/oicr-gsi/pinery-reports.svg)](https://travis-ci.org/oicr-gsi/pinery-reports)

A Java app for generating regular reports based on Pinery data

## Build

```
mvn clean install
```

## Generate

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r <report> -f <format> -o <filename> [report-specific-options]
```

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| -s <pinery-url> | YES | Source Pinery URL | -s http://localhost:8080/pinery-miso |
| -r <report-name> | YES | Report to generate See Reports below | -r stock |
| -f <format> | no | Output format. Can be csv or pdf. Some formats may not be available for all reports. Default varies by report. See Reports below | -f pdf |
| -o <filename> | no | Output file. Defaults to "report.csv" or "report.pdf" in current working directory | -o ~/reports/PCSI-stocks-2017-06.pdf |

## Reports

See report pages for additional options and examples:

* [GECCO Report](docs/GeccoReport.md)
* [Stock Report](docs/StockReport.md)
