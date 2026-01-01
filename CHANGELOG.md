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
- Tag Display Mode: Control tag visibility in grid and list views
  - Settings option with 4 modes: None, List Only, Grid Only, Both
  - YAML frontmatter parsing for `tags:` field with list support
  - Tag chips with Material 3 styling in note cards
- Folder Display Mode: Control whether subfolder notes are included when browsing folders
  - Toggle setting: Include subfolders on/off
  - Real-time folder filtering in both grid and list views
  - Default behavior maintains existing subfolder inclusion
- Background Git Operations: Option to perform git pull/push operations in background
  - Toggle setting to avoid blocking UI during refresh operations
  - Useful when working alone and app syncs at startup
  - Maintains sync status without UI interruptions
- Tag Filtering: Browse and filter notes by tags in the navigation drawer
  - Automatic tag extraction from all notes' frontmatter
  - Tag-based navigation with drawer auto-close for focused viewing
  - Real-time filtering in both grid and list views
- Move Notes: Drag-and-drop style note relocation between folders
  - Long-press menu option to initiate move mode
  - Drawer navigation for folder selection during move operations
  - Automatic file relocation and Git commit
- Enhanced Floating Action Button: Expandable menu for quick note/task creation
  - Create regular notes or tasks with completion tracking
  - Quick search access
  - Context-aware folder inheritance for new items

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
