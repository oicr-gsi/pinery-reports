# OCTANE Items in Bank Report

Written for OCT: https://jira.oicr.on.ca/browse/GR-1226

Lists buffy coat, cfDNA, plasma, and tumour sample counts for each donor. See example table below.

## Options

| Option | Required | Description | Example |
|--------|----------|-------------|---------|
| users  | yes      | filter items by creator ID | --users=25,84,35 |

## Generate

Example: Generate in csv format

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r octane-bank -f csv -o report.csv --users=35,45,27,40,83,107,109,129
```

## Example

| Donor ID | Buffy Coat Aliquots Remaining | cfDNA Plasma Aliquots Remaining | Plasma Aliquots Remaining | Tumour Tissue Remaining (# slides) | Tumour DNA Available | Tumour RNA Available | Buffy Coat DNA Available |
|--|--|--|--|--|--|--|--|
| externalname-01 | 4 | 0 | 0 | 0 | No | No | No |
| externalname-02 | 4 | 0 | 0 | 0 | No | No | No |
| externalname-03 | 4 | 0 | 0 | 0 | No | No | No |
| externalname-04 | 4 | 0 | 0 | 0 | Yes | Yes | Yes |
| externalname-05 | 4 | 0 | 0 | 15 | Yes | Yes | No |
