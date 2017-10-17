# Stock Report

Originally written for PCSI: https://jira.oicr.on.ca/browse/GLT-1892

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| project | YES | LIMS project shortname | --project=PCSI |
| after | no | include stock samples created after (and including) this date (yyyy-MM-dd) | --after=2017-06-01 |
| before | no | include stock samples created before (and not including) this date (yyyy-MM-dd) | --before=2017-07-01 |

## Generate

Example: Include all stocks

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r stock -f { pdf | csv } -o <output-filename> --project=PCSI
```


Example: Include all stocks created in June 2017

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r stock -f { pdf | csv } -o <output-filename> --project=PCSI --after=2017-06-01 --before=2017-07-01
```

## Example

| External Name | MISO Alias | Conc. (ng/µl) | Vol. (µl) | Total Yield (ng) | Date Received | Institution |
|---------------|------------|---------------|-----------|------------------|---------------|-------------|
| 12345 | PCSI_0001_Pa_P_nn_D_S1 | 12.50 | 3.00 | 37.50 | 2017-06-03 | University Health Network |
| 23456 | PCSI_0002_Ly_R_nn_D_S1 | 22.80 | 5.50 | 125.40 | 2017-06-05 | University Health Network |
