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

| Donor ID | Buffy Coat Aliquots Remaining | Buffy Coat Aliquots Exhausted | # Buffy Coat DNA Samples Distributed | Projects Buffy Coat DNA Distributed To | Buffy Coat DNA Remaining (ng) | Buffy Coat DNA Samples Exhausted | cfDNA Plasma Aliquots Remaining | cfDNA Plasma Aliquots Exhausted | # cfDNA Samples Distributed | Projects cfDNA Samples Distributed To | cfDNA Remaining (ng) | cfDNA Exhausted | Plasma Aliquots Remaining | Plasma Aliquots Exhausted | # Plasma DNA Samples Distributed | Projects Plasma DNA Samples Distributed To | Plasma DNA Remaining (ng) | Plasma DNA Exhausted | Tumour Tissue Remaining (# slides) | Tumour Tissue Exhausted (# slides) | # Tumour DNA Samples Distributed | Projects Tumour DNA Distributed To | Tumour DNA Remaining (ng) | Tumour DNA Exhausted | # Tumour RNA Samples Distributed | Projects Tumour RNA Distributed To | Tumour RNA Remaining (ng) | Tumour RNA Exhausted |
|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|--|
| externalname-01 | 4 | 0 | 0 |  | 0 | 0 | 4 | 0 | 0 |  | 0 | 0 | 4 | 0 | 0 |  | 0 | 0 | 0 | 0 | 0 |  | 0 | 0 | 0 |  | 0 | 0 |
| externalname-02 | 4 | 0 | 0 |  | 5331.34 | 0 | 4 | 0 | 0 |  | 0 | 0 | 4 | 0 | 0 |  | 0 | 0 | 0 | 0 | 0 |  | 0 | 0 | 0 |  | 0 | 0 |
| externalname-03 | 4 | 0 | 0 |  | 7213.44 | 0 | 4 | 0 | 0 |  | 0 | 0 | 4 | 0 | 0 |  | 0 | 0 | 0 | 0 | 0 |  | 0 | 0 | 0 |  | 0 | 0 |
| externalname-04 | 4 | 0 | 2 | Unspecified (Internal) | 0 | 1 | 4 | 0 | 0 |  | 0 | 0 | 4 | 0 | 0 |  | 0 | 0 | 14 | 0 | 2 | Unspecified (Internal) | 0 | 1 | 2 | Unspecified (Internal) | 0 | 1 |
| externalname-05 | 4 | 0 | 0 |  | 0 | 0 | 4 | 0 | 0 |  | 0 | 0 | 4 | 0 | 0 |  | 0 | 0 | 1 | 15 | 1 | Unspecified (Internal) | 0 | 1 | 1 | Unspecified (Internal) | 0 | 1 |
