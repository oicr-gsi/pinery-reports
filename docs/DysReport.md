# DYS Report
This was a custom one-off DYS report for ticket GLT-2150.

## Options

This report has no additional options

## Generate

CSV (default format):
```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r dys -o <output-filename>
```

## Example

| External.Identifier | Stock.Alias               | Aliquot.Alias           | Library.Alias            | 
Run                           | Barcode | Lane | Targeted.Sequencing | Tissue.Material | Group.ID | Pool.Alias       | Num.Dilutions.In.Pool | Pool.Date.Created | Freezer.Box.position         |
|---------------------|-------------|---------------|---------------|---------------|------|---------------------|-----------------
--|----------|---------------|-----------|-------------|--------------|-------------------|
| DYS_4219            | DYS_4219_Es_P_nn_7-1_D_S1 | DYS_4219_Es_P_nn_7-1_D_1 | DYS_4219_Es_P_PE_365_TS | 	171117_D00343_0204_BCBUW0ANXX | GGACTATG | 2   | DYS_IAD78789_185    |Fresh Frozen     | 7-1      | DYS_POOL_TOPUP_1 | 
20                    | 2017-11-10        |  DYS0022, DYS_PLATE_0022, B11 |
