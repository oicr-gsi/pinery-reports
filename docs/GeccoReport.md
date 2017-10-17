# GECCO Report
This report displays received sample details and sequencing status for
all the samples in the GECCO project.

[GECCO](https://www.fredhutch.org/en/labs/phs/projects/cancer-prevention/projects/gecco.html) is an acronym for Genetics and Epidemiology of Colorectal
Cancer Consortium.

## Options

This report has no additional options

## Generate

CSV (default format):
```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r gecco -o <output-filename>
```

PDF:
```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r gecco -f pdf -o <output-filename>
```

## Example

| OICR Name                | Person ID    | Sample ID | Gender | Tissue of Origin | Tissue Type | Libraries Created | Times Sequenced | Runs                          |
|--------------------------|--------------|-----------|--------|------------------|-------------|-------------------|-----------------|-------------------------------|
| GECCO_2584_Bu_R_nn_1-1_D | 98278793         |           | Male   | Bu               | R           | 1                 | 1               | 170302_D00353_0187_BCAHMNANXX |
| GECCO_2585_Bu_R_nn_1-1_D | 0723648       |           | Male   | Bu               | R           | 1                 | 1               | 170302_D00353_0187_BCAHMNANXX |
| GECCO_0088_Li_P_nn_1-1_D | 7610939 | 734879  | Female | Li               | P           | 1                 | 1               | 160920_D00343_0135_AC9C22ANXX |
| GECCO_0088_Ly_R_nn_1-1_D | 9837498347 | 135565    | Female | Ly               | R           | 1                 | 1               | 161124_D00331_0212_AC9W2WANXX |
