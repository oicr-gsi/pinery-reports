# Donor Report
This report displays donor information for all donors in the given project. Note that this report can only be run over one project at a time.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| project | YES | LIMS project shortname | --project=GECCO |

## Generate

CSV:
```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r donor -o <output-filename> --project=<project>

## Example
| OICR Name  | MISO ID | External Name | Description | Lab | Sex    |
|------------|---------|---------------|-------------|-----|--------|
| GECCO_0001 | SAM2345 | 1029384756    |             | UHN | Male   |
| GECCO_0002 | SAM2346 | 92384759      |             | UHN | Female | 
```