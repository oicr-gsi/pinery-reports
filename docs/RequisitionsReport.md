# Requisitions Report

Originally written for CHARM2PLAS: https://jira.oicr.on.ca/browse/GR-1526

This report lists all of the requisitions matching the specified criteria.

## Options

| Option  | Required | Description                                                        | Example              |
| ------- | -------- | ------------------------------------------------------------------ | -------------------- |
| match   | no       | A requisition will only be included if its name contains this text | --match "CHARM2PLAS" |
| exclude | no       | A requisition will be excluded if its name contains this text      | --exclude "Mock"     |

## Generate

Example: List requisitions with names containing "CHARM2PLAS" and excluding "Mock". Generate in csv format.

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r requisitions -f csv -o report.csv -match "CHARM2PLAS" -exclude "Mock"
```

## Example

| Requisition    | Assay            | Created    | Stopped | Stop Reason           |
| -------------- | ---------------- | ---------- | ------- | --------------------- |
| CHARM2PLAS-001 | WGTS - 80XT/30XN | 2024-01-02 | Yes     | Insufficient material |
| CHARM2PLAS-002 | CHARM - cfDNA+BC | 2024-03-04 | No      |                       |
