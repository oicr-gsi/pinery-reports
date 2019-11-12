# Stocks By Concentration Report

Originally written for OCT: https://jira.oicr.on.ca/browse/GLT-3045

This report lists all of the stock samples with a concentration of exactly 50, 100, or 150.

## Options

| Option | Required | Description | Example |
| ------ | -------- | ----------- | ------- |
| project | YES | LIMS project shortname | --project=OCT |

## Generate

Example: Generate in csv format for OCT project

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r stocks-by-concentration -f csv -o report.csv --project=OCT
```

## Example

| Sample ID | Alias | Description | Concentration | Volume | Total Yield | Date Received | Date Created | Date Modified |
| --------- | ----- | ----------- | ------------- | ------ | ----------- | ------------- | ------------ | ------------- |
| SAM123 | PROJ_0001_Ov_P_nn_1-1_R_S1 |  | 50 | 60 | 3000 | 2018-03-02 | 2018-02-07T21:05:24Z | 2018-11-01T20:00:55Z |
| SAM456 | PROJ_0001_Ly_R_nn_1-1_D_S1 |  | 100 | 25 | 2500 |  | 2017-07-25T14:43:38Z | 2019-06-25T21:12:14Z |
| SAM789 | PROJ_0001_Fa_M_nn_1-1_R_S1 |  | 150 | 57 | 8550 | 2017-12-14 | 2017-12-20T17:46:49Z | 2019-06-25T21:12:14Z |
