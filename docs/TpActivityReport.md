# TP Activity Report

Originally written for TP: https://jira.oicr.on.ca/browse/GR-1479

This report lists the work completed for each project by a group of users. Quantification and
Comments columns are included to be filled in manually later.

## Options

| Option      | Required | Description                                                            | Example             |
| ----------- | -------- | ---------------------------------------------------------------------- | ------------------- |
| after       | yes      | count work completed after (and including) this date (yyyy-MM-dd)      | --after=2017-06-01  |
| before      | yes      | count work completed before (and not including) this date (yyyy-MM-dd) | --before=2017-07-01 |
| users       | yes      | filter certain counts by creator ID                                    | --users=25,84,35    |
| db-host     | yes      | MISO database host                                                     | localhost:3306      |
| db-name     | yes      | MISO database name                                                     | lims                |
| db-user     | yes      | MISO database username                                                 | oicrlims            |
| db-password | yes      | MISO database user password                                            | secret              |

## Generate

Example: Generate in csv format

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r slide -f csv -o report.csv --project=OCT
```

## Example

| Project Code | Quantification | Samples Received | Samples Accessioned | Samples Extracted | Samples Aliquoted | Samples Transferred | Samples Distributed | Comments |
| ------------ | -------------- | ---------------- | ------------------- | ----------------- | ----------------- | ------------------- | ------------------- | -------- |
| PROJ1        |                | 12               | 8                   | 4                 | 4                 | 4                   | 10                  |          |
