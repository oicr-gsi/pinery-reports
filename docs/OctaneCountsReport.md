# OCTANE Counts Report

Originally written for OCT: https://jira.oicr.on.ca/browse/GLT-2070

Lists several case, sample, and inventory counts for OCTANE. Likely to be adapted for other projects in the future.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| after | yes | count new cases and samples created after (and including) this date (yyyy-MM-dd) | --after=2017-06-01 |
| before | yes | count new cases and samples created before (and not including) this date (yyyy-MM-dd) | --before=2017-07-01 |
| users | no | filter certain counts by creator ID | --users=25,84,35 |
| sitePrefix | no | filter where sample name begins with sitePrefix | --sitePrefix=OCT_01 |

Note: Inventory counts are for the point in time when the report is generated, and are not affected by the above options.

## Generate

Example: Generate in csv format

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r octane -f csv -o report.csv --after=2017-10-01 --before=2017-11-01 --users=35,45,27,40,83,1 --sitePrefix=OCT_01
```

## Example

| (no headings) |   |
|---------------|---|
|CASE NUMBERS (2017-10-01 - 2017-11-01)||
|Tumor Tissue Rec'd|43|
|Tumor Tissue Extracted|18|
|Tumor Tissue DNA Transferred|7|
|Tumor Tissue RNA Transferred|10|
|Buffy Coat Rec'd|100|
|Buffy Coat Extracted|10|
|Buffy Coat Transferred|10|
|Plasma Rec'd|71|
|ctDNA Plasma Rec'd|71|
|||
|SAMPLE NUMBERS (2017-10-01 - 2017-11-01)||
|Tumor Tissue Rec'd|1001|
|Tumor Tissue Extracted|20|
|Tumor Tissue DNA Transferred|40|
|Tumor Tissue RNA Transferred|25|
|Buffy Coat Rec'd|451|
|Buffy Coat Extracted|10|
|Buffy Coat Transferred|10|
|Plasma Rec'd|530|
|ctDNA Plasma Rec'd|480|
|||
|INVENTORY NUMBERS (2017-11-07 16:28)||
|Tumor Tissue Unstained Slides|5901|
|Tumor Tissue Unstained Slides (Cases)|399|
|Tumor Tissue H&E Slides|520|
|Tumor Tissue H&E Slides (Cases)|480|
|Buffy Coat|2235|
|Buffy Coat (Cases)|610|
|Plasma|3052|
|Plasma (Cases)|610|
|ctDNA Plasma|2775|
|ctDNA Plasma (Cases)|610|
|Extracted Buffy Coat|97|
|Extracted Buffy Coat (Cases)|95|
|Tumor Tissue DNA|43|
|Tumor Tissue DNA (Cases)|43|
|Tumor Tissue DNA|43|
|Tumor Tissue DNA (Cases)|43|
|||
|CASES WITH UNSTAINED SLIDES BY TISSUE TYPE||
|M|159|
|P|236|
|T|12|
|||
|CASES WITH UNSTAINED SLIDES BY TISSUE ORIGIN||
|Ab|82|
|Ag|1|
|Ap|38|
|Ax|32|
|Ba|7|
|Bd|1|
|Bl|4|
|Bn|11|

* these are made-up numbers and not actual data
