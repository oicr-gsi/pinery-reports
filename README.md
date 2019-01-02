# Pinery Reports

[![Build Status](https://travis-ci.org/oicr-gsi/pinery-reports.svg)](https://travis-ci.org/oicr-gsi/pinery-reports)

A Java app for generating regular reports based on [Pinery](https://github.com/oicr-gsi/pinery) data

## Build

```
git checkout develop
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

See [report pages](docs) for additional options and examples:

* [GECCO Report](docs/GeccoReport.md)
* [Stock Report](docs/StockReport.md)
* [Project Sequencing Report](docs/ProjectSequencingReport.md)
* [OCTANE Counts Report](docs/OctaneCountsReport.md)
* [Receipt Missing Report](docs/ReceiptMissingReport.md)
* [Slide Report](docs/SlideReport.md)


## Generate using Docker

Build the Docker image:
```
docker build -t pinery-reports .
```

Generating the reports can be done by running a Docker container with the report-specific parameters.
  * the container saves the report with the name you give it to its `/output` directory.
    * to save the file back to the host machine, use the `-v` option to link any host machine directory to the `/output`
	  directory in the Docker container.
    * doing so will cause the file to have the same permissions as the user who ran the Docker container
  * to use the host's network (to access an intranet-accessible Pinery, for instance), run the container with the option `--network host`.

```
docker run -it --rm \
  -e PINERY_URL=<pinery-url> \
  -e REPORT_NAME=<report-name> \
  -e FILE_NAME=<output-file-name> \
  -v <host volume to write report to>:/output \
  --network host \
  pinery-reports <any report-specific options>
```

### Example

```
docker run -it --rm \
  -e PINERY_URL=http://pinery.url.goes.here:8080 \
  -e REPORT_NAME=slide \
  -e FILE_NAME="PCSI_slide.csv" \
  -v /usr/reports:/output \
  --network host \
  pinery-reports --project=PCSI
```
