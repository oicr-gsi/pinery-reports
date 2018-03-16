# Location Missing Report

Originally written for OCT: https://jira.oicr.on.ca/browse/GR-543

This report lists cases where 

a) a sample has a Received Date and volume is not zero but no location is set
b) a sample is descended from a sample with a Received Date and volume is not zero, but no location is set


## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| project | YES | LIMS project shortname | --project=OCT |
| after | no | include samples which were received after (and including) this date (yyyy-MM-dd) | --after=2018-03-01 |
| before | no | include samples which were received before (and not including) thsi date (yyyy-MM-dd) | --before=2018-03-27 |
| users | no | comma-separated list of user IDs for filtering on sample creator | --users=35,45,27,40,83 |

## Generate

Example: Generate in csv format for OCT project

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r location-missing -f csv -o report.csv --project=OCT --after=2018-03-01 --before=2018-03-31 --users=35,45,27,40,83
```

## Example

| Sample ID | Sample Alias | Sample Barcode |
|-----------|--------------|----------------|
| SAM123    | OCT_0001_Ly_R_nn_1-1_SL01 | 22622 |
| SAM124    | OCT_0002_Ly_R_nn_1-1_SL02 | 13579 |
| SAM125    | OCT_0003_Ur_P_nn_1-1 | |
| SAM125    | OCT_0003_Ur_P_nn_1-1_D_S1 | |
