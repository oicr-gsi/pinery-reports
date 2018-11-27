# Lanes Billing Report

Originally written to provide a count of the projects run by Genomics on each lane within the past month.

This report makes a few assumptions:
* It assumes that all TGL libraries run on NextSeq instruments don't need to be reported (these are run by TGL for TGL)
* It groups all TGL libraries run on HiSeq/MiSeq instruments into a "TGL" project (a catch-all project, as 
  these libraries are run by Genomics for TGL) for the summary tables, and retains the specific project names for the detailed tables
* It assumes that all lanes that a) have no project, and b) have `UHN_HiSeqs` in the run path, fall under the "UHN" project
  (a catch-all project to define lanes that Genomics runs for UHN, but has no knowledge of any sample details) 
* It assumes that all lanes for which there is no sample information (`UHN` or `NoProject` lanes) are DNA lanes

This report provides four sections:
* Completed runs: 
    * Number (and type) of lanes run per project & sequencer model & sequencing parameters/read lengths
    * Detailed report of each project, sequencer model, sequencing parameters/read lengths, run, lane, and type of lane (DNA, RNA, or MIXED)
* Failed runs:
    * Number (and type) of lanes run per project & sequencer model & sequencing parameters/read lengths
    * Detailed report of each project, sequencer model, sequencing parameters/read lengths, run, lane, and type of lane (DNA, RNA, or MIXED)

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
| Project | Instrument Model | Sequencing Parameters | DNA | RNA | MIXED ||||||
|---------|------------------|--------------|-----|-----|-------|-|-|-|-|-|
|PCSI|Illumina HiSeq 2500|151_151|3|0|0||||||
|PCSI|Illumina MiSeq|26_266|0|1|0||||||
|SCRM|Illumina HiSeq 2500|151_151|1|2|1||||||
|||||||||||
| Project | Instrument Name | Instrument Model | Sequencing Parameters | Run Status | Run End Date | Run Name | Lane | DNA | RNA | MIXED |
|PCSI|D00343|Illumina HiSeq 2500|151_151|Completed|2017-07-03|Run1|3|1|0|0|
|SCRM|D00303|Illumina HiSeq 2500|151_151|Completed|2017-07-03|Run1|4|1|0|0|
|||||||||||
| Project | Instrument Model | Sequencing Parameters | DNA | RNA | MIXED ||||||
|PCSI|Illumina MiSeq|26_266|1|0|0||||||
|||||||||||
| Project | Instrument Name | Instrument Model | Sequencing Parameters | Run Status | Run End Date | Run Name | Lane | DNA | RNA | MIXED |
|PCSI|D00343|Illumina MiSeq|26_266|Failed|2017-07-22|Run2|1|1|0|0|
||||||||||
