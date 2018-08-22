# GECCO Donor Report
This report displays donor information for all donors in the GECCO project.

[GECCO](https://www.fredhutch.org/en/labs/phs/projects/cancer-prevention/projects/gecco.html) is an acronym for Genetics and Epidemiology of Colorectal Cancer Consortium.

## Options

This report has no additional options

## Generate

CSV:
```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r gecco-donor -o <output-filename>

## Example
| OICR Name  | MISO ID | External Name | Description | Lab | Sex    |
|------------|---------|---------------|-------------|-----|--------|
| GECCO_0001 | SAM2345 | 1029384756    |             | UHN | Male   |
| GECCO_0002 | SAM2346 | 92384759      |             | UHN | Female | 
```