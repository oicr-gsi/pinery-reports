# Billing Libraries Report

Originally written to provide a count of the kits used for library creation within the past month, broken down by project. This report provides two sections:
* Count of library kits per project
* Detailed report on each library made

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| after | yes | count new libraries created after (and including) this date (yyyy-MM-dd) | --after=2018-01-01 |
| before | yes | count new libraries created before (and not including) this date (yyyy-MM-dd) | --before=2018-02-01 |

## Generate

Example: Generate in CSV format

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r  libraries-billing -f csv -o report.csv --after=2018-01-01 --before=2018-02-01  
```

## Example
| Study Title | Library Kit | Count (2018-01-01 - 2018-02-01) |||
|-------------|-------------|---------------------------------|-|-|
|SCRM|10X Single Cell RNA|8|||
|PCSI|KAPA Hyper Prep|28|||
|PCSI|TruSeq RNA Access|8|||
|TGL12|Agilent SureSelect XT HS|8|||
|TGL13|KAPA Hyper Prep and SureSelect XT|4|||
||||||
| Project | Creation Date | Library | Kit | Seq. Strategy|
|SCRM|2018-01-03|SCRM_0810_Bn_T_PE_456_MR|TruSeq RNA Access|MR|
|SCRM|2018-01-03|SCRM_0811_Bn_T_PE_466_MR|TruSeq RNA Access|MR|
|TGL12|2018-01-05|TGL12_0038_Ov_P_PE_314_EX|Agilent SureSelect Human All Exon V5 + UTRs|EX|
|TGL13|2018-01-09|TGL13_0005_Ln_P_PE_311_WT|KAPA Hyper Prep and SureSelect XT|WT|
