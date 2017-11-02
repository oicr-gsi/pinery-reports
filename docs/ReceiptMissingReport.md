# Receipt Missing Report

Originally written for OCT: https://jira.oicr.on.ca/browse/GLT-2070

This report lists cases where either

a) a Tissue has no Received Date set and no Slide children, or
b) a Slide has no Received Date set and its parent Tissue also has no Received Date set

In cases where a Tissue has multiple Slide children, each slide will be listed separately.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| project | YES | LIMS project shortname | --project=OCT |

## Generate

Example: Generate in csv format for OCT project

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r receipt-missing -f csv -o report.csv --project=OCT
```

## Example

| Tissue ID | Tissue Alias | Slide ID | Slide Alias |
|-----------|--------------|----------|-------------|
| SAM123    | OCT_0001_Ly_R_nn_1-1 | - | - |
| SAM124    | OCT_0002_Ly_R_nn_1-1 | - | - |
| SAM125    | OCT_0003_Ur_P_nn_1-1 | SAM126 | OCT_0003_Ur_P_nn_1-1_SL01 |
| SAM125    | OCT_0003_Ur_P_nn_1-1 | SAM127 | OCT_0003_Ur_P_nn_1-1_SL02 |
