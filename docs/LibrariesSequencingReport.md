# Libraries Sequencing Report

Written to give Karen a summary of all sequencing runs done on FNS libraries: https://jira.oicr.on.ca/browse/GR-681
Note that failed runs are not included.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| project | yes | project to report on | --project=FNS |

## Generate

```
java -jar target/pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r libraries-sequencing -o <output-filename> --project=FNS
```

## Example
| Library | Lib. Type | Identity | External Identifier | Received Date | Runs |
|---------|-----------|----------|---------------------|---------------|------|
| FNS_0001_Ce_C_PE_584_WG | WG | FNS_0001 | MB_3_1 | 2016-11-28 | 161208_D00343_0153_BC9VL1ANXX |
| FNS_0002_Ce_C_PE_604_WG | WG | FNS_0002 | MB_3_2 | 2016-11-28 | 161208_D00343_0153_BC9VL1ANXX |
