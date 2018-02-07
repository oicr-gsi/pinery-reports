# Libraries Creation Report

Originally written to create the monthly libraries report as a Pinery report.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| after | yes | count new libraries created after (and including) this date (yyyy-MM-dd) | --after=2018-01-01 |
| before | yes | count new libraries created before (and not including) this date (yyyy-MM-dd) | --before=2018-02-01 |

## Generate

Example: Generate in CSV format

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r  libraries-creation -f csv -o monthly-libraries-report.csv --after=2018-01-01 --before=2018-02-01  
```

## Example
| Project | Creation Date | Library | Seq_strategy | Kit |
|---------|---------------|---------|--------------|-----|
|SCRM|2018-01-03|SCRM_0810_Bn_T_PE_456_MR|MR|TruSeq RNA Access|
|SCRM|2018-01-03|SCRM_0811_Bn_T_PE_466_MR|MR|TruSeq RNA Access|
|TGL12|2018-01-05|TGL12_0038_Ov_P_PE_314_EX|EX|Agilent SureSelect Human All Exon V5 + UTRs|
|TGL13|2018-01-09|TGL13_0005_Ln_P_PE_311_WT|WT|
