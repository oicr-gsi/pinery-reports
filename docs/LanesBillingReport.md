# Billing Libraries Report

Originally written to provide a count of the projects run on each lane within the past month.
This report provides four sections:
* Completed runs: 
    * Number (and type) of lanes run per project & sequencer model
    * Detailed report of each project, sequencer model, run, lane, and type of lane (DNA, RNA, or MIXED)
* Failed runs:
    * Number (and type) of lanes run per project & sequencer model
    * Detailed report of each project, sequencer model, run, lane, and type of lane (DNA, RNA, or MIXED)

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| after | yes | count new libraries created after (and including) this date (yyyy-MM-dd) | --after=2018-01-01 |
| before | yes | count new libraries created before (and not including) this date (yyyy-MM-dd) | --before=2018-02-01 |

## Generate

Example: Generate in CSV format

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r  lanes-billing -f csv -o report.csv --after=2018-01-01 --before=2018-02-01  
```

## Example
| Project | Instrument Model | DNA | RNA | MIXED ||||||
|---------|------------------|-----|-----|-------|-|-|-|-|-|
|PCSI|Illumina HiSeq 2500|3|0|0||||||
|PCSI|Illumina MiSeq|0|1|0||||||
|SCRM|Illumina HiSeq 2500|1|2|1||||||
|||||||||||
| Project | Instrument Name | Instrument Model | Run Status | Run End Date | Run Name | Lane | DNA | RNA | MIXED |
|PCSI|D00343|Illumina HiSeq 2500|Completed|2017-07-03|Run1|3|1|0|0|
|SCRM|D00303|Illumina HiSeq 2500|Completed|2017-07-03|Run1|4|1|0|0|
|||||||||||
| Project | Instrument Model | DNA | RNA | MIXED ||||||
|---------|------------------|-----|-----|-------||||||
|PCSI|Illumina MiSeq|1|0|0||||||
|||||||||||
| Project | Instrument Name | Instrument Model | Run Status | Run End Date | Run Name | Lane | DNA | RNA | MIXED |
|PCSI|D00343|Illumina MiSeq|Failed|2017-07-22|Run2|1|1|0|0|
||||||||||
