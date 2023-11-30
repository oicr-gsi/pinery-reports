# Precise Case List

This report lists all cases in the PRECISE project and indicates which of the expected samples have
been received.


## Options

This report has no additional options.

## Generate

Example: Generate in csv format

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r precise-list -f csv -o report.csv
```

## Example

| Trial ID | Randomization Arm | Screening Blood | Screening Urine | Screening Post DRE Urine Pellet | 6 Month Follow Up Blood | 1 Year Follow Up Blood | 1 Year Follow Up Urine | 1 Year Follow Up Post DRE Urine Pellet | 18 Month Follow Up Blood | 2 Year Follow Up Blood | 2 Year Follow Up Urine | 2 Year Follow Up Post DRE Urine Pellet | 4 Year Follow Up Blood | 5 Year Follow Up Blood | 6 Year Follow Up Blood | Bx Tissue | Rp Tissue |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 123456 |  | YES | YES | YES | YES | YES | NO | NO | YES | YES | YES | YES | NO | NO | NO | NO | NO |

The Randomization Arm column is left blank and intended to be filled in separately based on a list
of cases that are either "Experimental" or "Control". If this list is added as a second worksheet in
Excel named "Sheet1", the following formula can be used to fill in the column. Modify the range in
the formula (A3:B338) if necessary to cover the entire list of Trial IDs and Randomization Arms.

```
=VLOOKUP(VALUE(REPLACE(A2, 5, 1, "")), Sheet1!A3:B338, 2, FALSE)
```
