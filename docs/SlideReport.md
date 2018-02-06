# Slide Report

Originally written for OCT: https://jira.oicr.on.ca/browse/GLT-2070

This report lists all of the Slide samples for a project and was created to help detect input errors.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| project | YES | LIMS project shortname | --project=OCT |

## Generate

Example: Generate in csv format for OCT project

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r slide -f csv -o report.csv --project=OCT
```

## Example

| Slide ID | Alias | Stain | Slides | Discards |
|----------|-------|-------|--------|----------|
| SAM123 | OCT_0001_Br_P_nn_1-1_SL01 | unstained | 15 | 0 |
| SAM124 | OCT_0001_Br_P_nn_1-1_SL02 | Hematoxylin+Eosin | 1 | 0 |
