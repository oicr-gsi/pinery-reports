# Empty Stock Report

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| users | yes | filter by creator ID  | -users=25,84,35 |
| after | no | include stock samples created after (and including) this date (yyyy-MM-dd) | --after=2017-06-01 |
| before | no | include stock samples created before (and not including) this date (yyyy-MM-dd) | --before=2017-07-01 |

## Generate

Example: Include all stocks

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r empty-stock -f { pdf | csv } -o <output-filename> --users=25,21 
```


Example: Include all stocks created in June 2017

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r empty-stock -f { pdf | csv } -o <output-filename> --users=25,21 --after=2017-06-01 --before=2017-07-01
```

## Example

| Sample ID | Alias | Stock Created | Stock Modified | Modified By | External Name | Conc. (ng/µl) | Initial Vol. (µl) | Current Vol. (µl) | Location | 
|---------------|------------|---------------|----------------|-------------|-----------|-------------|---------------|-------------------|----------|
| 12345 | PCSI_0001_Pa_P_nn_D_S1 | 2017-06-03    | 2017-06-13     | 25          | PCSI_0001 | 3.00 | 37.50 | 0.00              | box 1    |
| 23456 | PCSI_0002_Ly_R_nn_D_S1 | 2017-09-03    | 2017-09-03     | 84          | PCSI_0002 | 5.50 | 125.40 | 0.00              | box 1    |
