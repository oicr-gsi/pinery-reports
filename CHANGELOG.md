# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/)
and as of version 1.0.0, follows semantic versioning. For unreleased changes,
see [changes](changes).

--------------------------------------------------------------------------------

## [1.20.0] - 2025-10-02

### Added



## [1.19.1] - 2025-07-04

### Fixed



## [1.19.0] - 2025-06-26

### Added

* The ability to provide multiple comma separated strings for the -match and -exclude argument on the Requistions Report

## [1.18.0] - 2025-05-29

### Added

* LCM, Sectioning, and Macrodissection/Coring columns to the TP Activity Report

### Changed

* On the TP Activity Report, CHARM2 and CHARM2PLAS extraction samples with tissue origin 'Pl' will each
  be counted as 3 extractions. This is because 3 extractions are done and then elutions are pooled and
  only one sample is entered into MISO


## [1.17.0] - 2025-03-25

### Changed

* OCTANE Items in Bank Report now checks remaining volume instead of yield, and no longer requires
  concentration to be set
  
* OCTANE Items in Bank Report slide count now includes only unstained slides

### Fixed

* OCTANE Items in Bank Report should exclude donors with no remaining samples


## [1.16.0] - 2025-02-12

### Changed

* Items in Bank report updated - now only includes remaining sample counts and availability. See report
  documentation for more details


## [1.15.0] - 2024-09-25

### Added

* Assay, Stopped, and Stop Reason columns to the Requisitions Report

### Upgrade Notes

* Updated to Pinery 2.36.0


## [1.14.0] - 2024-06-26

### Added

* Requisitions Report


## [1.13.0] - 2023-12-05

### Added

* PRECISE Case List Report

### Changed

* Updated to Pinery 2.29.0
* PRECISE Report now includes Four, Five, and Six Year Follow Up timepoints


## [1.12.0] - 2023-06-15

### Added

* TP Activity Report

### Upgrade Notes

* Upgrade Pinery to 2.25.0


## [1.11.0] - 2021-06-10

### Added

* PRECISE Inventory Summary Report

### Changed

* Updated reports to account for slide changes in MISO/Pinery
  * Donors for Existing Samples
  * OCTANE Counts
  * OCTANE Items in Bank
  * PRECISE
  * Slide

### Upgrade Notes

* Project now requires Java 11 or later to build


## [1.10.0]  2021-04-08
### Added:
  * External Names in Library Billing report
### Changed:
  * updated to Pinery v1.19.0

## [1.9.0]  2020-11-26
### Changed:
  * group by subproject in billing reports

## [1.8.1]  2020-09-25
### Fixed:
  * `octane`: handle cross-project sample hierarchies
  * `octane-bank`: handle cross-project sample hierarchies

## [1.8.0]  2020-09-02
### Added:
  * Precise Case Report (`precisecase`)
  * OCTANE Items in Bank Report (`octane-bank`)

## [1.7.0]  2020-04-29
### Changed
  * `octane`: exclude transferred/distributed samples from inventory counts

## [1.6.0]  2020-01-20
### Changed
  * `stock`: Allow reporting on multiple projects
  * `stock`: Use initial volume in report instead of current volume

## [1.5.2]  2019-12-17
### Changed:
  * `octane`: Add OCTCAP project and expand cfDNA fields

## [1.5.1]  2019-12-06
### Changed:
  * updated to Pinery 2.8.0

## [1.5.0]  2019-11-12
### Added:
  * `stocks-by-concentration`

## [1.4.0]  2019-10-25
### Changed:
  * Use Pinery 2.6.0
### Fixed:
  * Bump Jackson version to fix security vulnerability

## [1.3.2]  2019-05-27
### Fixed:
  * `lanes-billing`: libraries count in NovaSeq joined lanes

## [1.3.1]  2019-04-04
### Changed:
  * `octane`: filter all samples by creator
  * `bisque`: added more library designs

## [1.3.0]  2019-03-26
### Added:
  * Code formatter
  * `donors-for-existing-samples` report
### Fixed:
  * `lanes-billing`: don't error on empty NovaSeq flowcells
  * `octane`: don't include `Ly_R` samples in Tumor Tissue DNA/RNA Transferred

## [1.2.0]  2019-01-23
### Added:
  * Enable reports to interact with Guanyin
  * Add Dockerfile to create executable jar container

## [1.1.0]  2018-12-19
### Added:
  * `libraries-sequencing` report
### Changed:
  * `lanes-billing`: report on joined NovaSeq lanes
  * `precise`: add distribution tables
  * update release procedure to deploy after PR approval
### Fixed:
  * `precise`: handle spaces in barcodes

## [1.0.0]  2018-11-30
### Added:
  * add release script
  * add changelog
### Changed:
  * updated to Pinery 2.3.0
  * `bisque`: get active projects from Pinery
  * `lanes-billing`: show flowcell type for NovaSeq runs
  * `lanes-billing`: display sequencing parameters
### Fixed:
  * `bisque`: fix categorizing libraries and dilutions
  * `precise`: report on received (instead of created) this month

# [0.0.14]  2018-10-25
### Added:
  * `precise`
  * `donor` (intended for GECCO project, but can be used for any project)
### Changed
  * `receipt-missing`: no longers on samples that have an In-lab Creation Date
### Fixed
  * `octane`: running without a start date nows donor counts for all time

## [0.0.13]  2018-08-13
### Added:
  * `gazpacho`: Subproject column now gets filled out

## [0.0.12]  2018-07-20
### Added:
  * `octane`: ability to on a per-site basis
### Changed:
  * `gazpacho`: changes to table headers and details

## [0.0.11]  2018-06-28
### Added:
  * `gazpacho`: flag for reporting on DNA only, RNA only, or both

## [0.0.10]  2018-06-28
### Added:
  * `gazpacho`

## [0.0.9]  2018-05-28
### Added:
  * `bisque`

## [0.0.8]  2018-04-30
### Changed:
  * `lanes-billing`: report UHN_HiSeqs separate from NoProject

## [0.0.7]  2018-04-02
### Fixed:
  * `lanes-billing`: fix exception

## [0.0.6]  2018-03-29
### Added:
  * `location-missing`
  * `lanes-billing`: added read lengths

## [0.0.5]  2018-03-20
### Added:
  * `dys`
### Changed:
  * `lanes-billing`: report lanes with no samples as NoProject

## [0.0.4]  2018-earlier
  * more fixes to make this release-worthy
