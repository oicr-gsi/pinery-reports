# Donors for Existing Samples (OCTANE) Report

Originally written for OCT: https://jira.oicr.on.ca/browse/GR-710

Lists names donors and samples for all samples that are still on-site (not empty, not discarded, not distributed) 

The report is separated into the following categories:

  * Tissue (Slides; no Blood)
    * P tissue type
    * M tissue type
    * n tissue type
    * 'other' tissue type
  * DNA Stock (no Blood)
    * P tissue type
    * M tissue type
    * n tissue type
    * 'other' tissue type
  * RNA Stock (no Blood)
    * P tissue type
    * M tissue type
    * n tissue type
    * 'other' tissue type
  * Blood
    * Buffy Coat tissue
    * Buffy Coat DNA stock
    * Plasma tissue
    * ctPlasma tissue
    
## Options

There are no options for this report.

## Generate

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r donors-for-existing-samples -o report.csv
```

## Example

| Identity Alias | Sample Alias |
|----------------|--------------|
|Cases with P samples||
|OCT_000001|OCT_000001_Br_P_nn_1-1_SL01|
|OCT_000011|OCT_000011_Ut_P_nn_1-1_SL01|
|OCT_000101|OCT_000101_Co_P_nn_1-1_SL01|
