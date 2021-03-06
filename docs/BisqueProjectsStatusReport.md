# Projects Status Report (Bisque)
This reports on the total numbers of samples and libraries across projects broken down by various categories: [Tissue, Stock, Aliquot, Library, Sequenced Library] multiplied by [Primary(-like), Reference, Organoid, Xenograft, Metastases, Leftover (catch-all)]. 

| Short form | Meaning |
|------------|---------|
| Aliq       | Aliquot |
| Lib        | Library |
| Seqd       | Sequenced |
| P          | Primary ("P", "S", "A" tissue types) |
| R          | Reference ("R" tissue type) |
| O          | Organoid ("O" tissue type) |
| X          | Xenograft ("X" tissue type) |
| M          | Metastases ("M" tissue type) |
| L          | 'Leftovers' ("C", "U", "T", "E", "n", "B", "F" tissue types) |

## Options

This report may accept a comma-separated list of projects (short names) to report on. 
If no list of projects is provided, the report will query Pinery (live) for a list of active projects, and will report on these.

## Generate

CSV (default format):

```
java -jar pinery-reports-<version>-jar-with-dependencies.jar -s <pinery-url> -r projects-status -o <output-filename> --project=<list,of,projects>
```

## Example

| APT2 | DNA Aliquot |        |     | TS Library |    |     | TS Lib Seqd |  |
|------|-------------|--------|-----|------------|----|-----|-------------|--|
|      | DNA R Aliq  | DNA L Aliq |  | TS R Lib  | TS L Lib |  | TS R Seqd | TS L Seqd |
|      | 2           | 2          |  | 8         | 7        |  | 8         | 7         |