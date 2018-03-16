# TGL Libraries Run Report

Originally written for TGL as a weekly report on which libraries have been run in the past week for the OCT project and projects named TGL##.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| after | no | include libraries where the run's completion date is after (and including) this date (yyyy-MM-dd) | --after=2017-06-01 |
| before | no | include libraries where the run's completion date is before (and not including) this date (yyyy-MM-dd) | --before=2017-06-08 |

## Generate

Example: Include all sequenced libraries from TGL

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r tgl-libraries-run -o <output-filename> 
```


Example: Include all sequenced libraries from TGL whose runs completed between June 1-8

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r tgl-libraries-run -o <output-filename> --after=2017-06-01 --before=2017-06-08
```

## Example

| Library creation date | Run completion date | Instrument | Run name | Project | Library | Seq strategy |
|---------------|------------|---------------|-----------|------------------|---------------|-------------|
| 2017-11-29 | 2018-01-04 | NB551051 | 180103_NB551051_0042_AH53Y5BGX5 | TGL11 | TGL11_0003_Lu_P_PE_261_WT | WT |
| 2017-11-29 | 2018-01-04 | NB551051 | 180103_NB551051_0042_AH53Y5BGX5 | TGL11 | TGL11_0001_Lu_P_PE_264_WT | WT |
