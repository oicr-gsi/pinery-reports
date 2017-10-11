# Project Sequencing Report

Lists library, stock, pool, run, and lane details for all runs involving libraries from the specified project. Failed and incomplete
runs are excluded, and the data is sorted reverse-chronologically by pool creation date.

Original request for DYS: https://jira.oicr.on.ca/browse/GLT-2068

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| project | YES | LIMS project shortname | --project=DYS |

## Generate

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r sequencing -f { pdf | csv } -o <output-filename> --project=DYS
```

## Example

| Stock | Pool | Pool Creator | Pool Created | Dilutions | Library | Run | Lane | Index |
|-------|------|--------------|--------------|-----------|---------|-----|------|-------|
| DYS_1234_Ly_R_nn_1-1_D_S1 | DYS_Pool_3 | Dys User | 2017-10-11 | 2 | DYS_1234_Ly_R_PE_123_TS | 171011_D00333_987_ABCDEFANXX | 1 | GCGAGTAA |
| DYS_2000_Ly_R_nn_1-1_D_S1 | DYS_Pool_3 | Dys User | 2017-10-11 | 2 | DYS_2000_Ly_R_PE_200_TS | 171011_D00333_987_ABCDEFANXX | 1 | AAGGTACA |
| DYS_1234_Ly_R_nn_1-1_D_S1 | DYS_Pool_3 | Dys User | 2017-10-11 | 2 | DYS_1234_Ly_R_PE_123_TS | 171011_D00333_987_ABCDEFANXX | 2 | GCGAGTAA |
| DYS_2000_Ly_R_nn_1-1_D_S1 | DYS_Pool_3 | Dys User | 2017-10-11 | 2 | DYS_2000_Ly_R_PE_200_TS | 171011_D00333_987_ABCDEFANXX | 2 | AAGGTACA |
