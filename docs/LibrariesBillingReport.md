# Billing Libraries Report

Originally written to provide a count of the kits used for library creation within the past month, broken down by project. This is intended to be sent with the Monthly Libraries Report.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| after | yes | count new libraries created after (and including) this date (yyyy-MM-dd) | --after=2018-01-01 |
| before | yes | count new libraries created before (and not including) this date (yyyy-MM-dd) | --before=2018-02-01 |

## Generate

Example: Generate in CSV format

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r  libraries-billing -f csv -o report.csv --after=2018-01-01 --before=2018-02-01  
```

## Example
| Study Title | Library Kit | Count (2018-01-01 - 2018-02-01) |
|-------------|-------------|---------------------------------|
|SCRM|10X Single Cell RNA|8|
|PCSI|KAPA Hyper Prep|28|
|PCSI|TruSeq RNA Access|8|
|DKT1|Agilent SureSelect XT HS|8|
|DKT1|KAPA Hyper Prep and SureSelect XT|4|
