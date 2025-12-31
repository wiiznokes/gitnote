# Changelog

All notable changes to this project will be documented in this file.
This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [26.01]

### Added

- Task Management: Support for marking notes as tasks with completion checkboxes
  - YAML frontmatter parsing for `completed?` field
  - Toggleable checkboxes in grid and list views
  - Convert notes to tasks and vice versa via long-press menu
- Frontmatter Hiding: Automatically hide YAML frontmatter in read-only view mode for clean Markdown rendering
- List View Improvements: Option to show full note titles with line wrapping instead of truncation

### Changed

- Improved note title display logic to respect "show full path" settings consistently

## [25.12]

### Changed

- new markdown rendering lib

### Added

- load keys from devices

## [25.11]

### Changed

- optimization for big repo
  - use fts in libsql
  - use a padger (only load notes visibles)

### Added

- cloning page
- pat authentification
- default path for new notes

## [25.08]

### Changed

- new rust backend for libgit2
- ssh support and strong github integration (list repos, create a new one)

## [25.07]

### Added

- markdown dedicated editor

## [25.07]

### Added

- izzy support

## [25.06]

### Added

- read only mode with markdown rendering

## [25.05]

### Added

- f-droid support

## [24.08]

- delete folder (#42)
- remove fuzzy for searching. Use substring instead

### Changed

- kotlin 2.0

### Fixed

- provider dd
