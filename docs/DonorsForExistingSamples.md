# Donors for Existing Samples (OCTANE) Report

Originally written for OCT: https://jira.oicr.on.ca/browse/GR-710

Lists names donors and samples for all samples that are:

  * still on-site (not empty, not discarded, not distributed) 
  * not blood of some sort (Ly, Ct, Pl).

The report is separated into the following categories:

  * Slides
    * P tissue type
    * M tissue type
    * n tissue type
    * 'other' tissue type
  * DNA
    * P tissue type
    * M tissue type
    * n tissue type
    * 'other' tissue type
  * RNA
    * P tissue type
    * M tissue type
    * n tissue type
    * 'other' tissue type
    
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