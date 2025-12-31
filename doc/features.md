# GitNote Features

GitNote is a Git-based note-taking app for Android that integrates seamlessly with Git repositories for version control and synchronization.

## Core Features

### Git Integration
- **Repository Management**: Clone, pull, and push notes to Git repositories.
- **Version Control**: Automatic commits on note changes, with conflict resolution.
- **SSH Support**: Secure authentication using SSH keys.

### Note Management
- **Markdown Support**: Full Markdown rendering with syntax highlighting.
- **Frontmatter Metadata**: YAML headers for titles, timestamps, completion status, tags, and authors.
- **File-Based Storage**: Notes are stored as Markdown files in the repository.

### Tag Filtering
GitNote supports filtering notes by tags defined in the frontmatter, allowing users to quickly find related notes.

#### How It Works
- **Tag Parsing**: Tags are extracted from the `tags:` field in YAML frontmatter.
- **Drawer Navigation**: Switch between folder and tag browsing modes in the navigation drawer.
- **Tag Selection**: Click on tags in the drawer to filter notes, or select "All notes" to show everything.
- **Real-time Filtering**: Notes are filtered instantly when a tag is selected, showing only matching notes.

#### Usage Tips
- Add tags to notes using YAML frontmatter: `tags: - tag1 - tag2`
- Use the toggle button in the drawer to switch between folder and tag modes.
- Tag filtering works in both grid and list views.
- Tags are case-sensitive and must match exactly.

#### Example Workflow
1. Add tags to a note's frontmatter:
   ```
   ---
   title: Meeting Notes
   tags:
     - meeting
     - planning
     - work
   ---
   ```
2. Open the navigation drawer and click the tag icon to switch to tag mode.
3. Click on "meeting" to show only notes tagged with "meeting".
4. Click "All notes" to return to showing all notes.

#### Auto-Closing Drawer
To provide a focused view of filtered results, the navigation drawer automatically closes in certain scenarios:

- **Tag Selection**: When you select a tag (including "All notes"), the drawer closes immediately, allowing you to view the filtered notes without obstruction.
- **Final Folder Navigation**: When navigating to a folder that has no subfolders (a "leaf" folder), the drawer closes to let you focus on the notes in that folder.
- **Mode Switching**: Switching between tag and folder modes clears the respective filters (tags when switching to folder mode, folder when switching to tag mode) to prevent confusion.

This behavior ensures a smooth user experience by reducing the need for manual drawer management while browsing and filtering notes.

### User Interface
- **Grid and List Views**: Switch between grid and list layouts for notes.
- **Search and Sort**: Search notes by content, sort by date, title, etc.
- **Dark Mode**: Automatic theme switching.
- **Note Actions Menu**: Long-press notes to access options like delete, multi-select, and convert between notes and tasks.

## Completion Checkbox Feature

GitNote now supports marking notes as completed using a checkbox in the UI, tied to the `completed?` field in the frontmatter.

### How It Works
- **Checkbox Display**: A checkbox appears next to the note title if the note has frontmatter.
- **Toggling**: Click the checkbox to toggle between `completed?: yes` and `completed?: no`.
- **Automatic Updates**: Toggling updates the `updated` timestamp and saves the changes to the file and Git repository.
- **Visual Feedback**: Completed notes can be visually distinguished (future feature).

### Usage Tips
- Add frontmatter to notes to enable the checkbox.
- Use completion for tasks, reminders, or project tracking.
- The checkbox is read-only in display; editing requires toggling in the app.
- Changes are committed to Git automatically.

### Example Workflow
1. Create a note with frontmatter:
   ```
   ---
   title: Finish Report
   completed?: no
   ---
   ```
2. Open the note in GitNote; a checkbox appears.
3. Click the checkbox to mark as complete; the file updates to `completed?: yes`.
4. Sync with Git to persist changes across devices.

## Convert to Task/Note Feature

GitNote allows quick conversion between regular notes and task-like notes via the long-press menu.

### How It Works
- **Long-Press Menu**: Access additional options by long-pressing a note.
- **Convert to Task**: Adds the `completed?: no` field to the frontmatter. If no frontmatter exists, creates one with title, updated, created, and completed fields.
- **Convert to Note**: Removes the `completed?:` field from the frontmatter, converting the task back to a regular note.
- **Automatic Updates**: Conversion updates the `updated` timestamp and saves changes to the file and Git repository.

### Usage Tips
- Use "Convert to Task" for notes that represent actionable items.
- Use "Convert to Note" to remove task tracking from a note.
- The menu shows the appropriate option based on the note's current state (task or note).
- Converted notes immediately show/hide the completion checkbox in the UI.

### Example Workflow
1. Long-press a regular note.
2. Select "Convert to Task"; frontmatter is added with `completed?: no`.
3. The note now displays a checkbox for completion tracking.
4. To revert, long-press again and select "Convert to Note"; the `completed?` field is removed.

## Frontmatter Hiding in View Mode

GitNote automatically hides YAML frontmatter when viewing Markdown notes in read-only mode to provide a clean, distraction-free reading experience.

### How It Works
- **Automatic Detection**: The app detects YAML frontmatter enclosed in `---` markers at the beginning of Markdown files.
- **Content Extraction**: In view mode, only the content after the frontmatter is rendered as Markdown.
- **Edit Mode Visibility**: Frontmatter remains visible and editable when switching to edit mode.
- **No Data Loss**: Frontmatter is preserved in the file; it's only hidden during rendering.

### Usage Tips
- Frontmatter is completely hidden in read-only view, showing only the actual note content.
- Switch to edit mode to see and modify frontmatter fields.
- This feature works automatically for all Markdown notes with valid frontmatter.
- Non-Markdown files and notes without frontmatter display normally.

### Example
A note file containing:
```
---
title: My Note
completed?: yes
---

# Main Content
This is the actual note content that users see.
```

Will display in view mode as:
> # Main Content
> This is the actual note content that users see.

## Show Full Title in List View

GitNote allows long note titles to wrap to multiple lines in list view instead of being truncated with ellipsis.

### How It Works
- **Setting Toggle**: Enable "Show full title in list view" in Settings > Appearance
- **Multi-line Display**: When enabled, note titles wrap naturally to multiple lines
- **Single-line Display**: When disabled, titles are truncated with "..." after one line
- **List View Only**: This setting only affects the list view, not the grid view

### Usage Tips
- Enable this setting if you have long note titles that are being cut off
- The setting provides better readability for notes with descriptive titles
- Disable it to keep a more compact list view with consistent row heights
- Changes take effect immediately without restarting the app

### Example
With the setting **disabled** (default):
```
My Very Long Note Title That Gets... 
```

With the setting **enabled**:
```
My Very Long Note Title That Gets
Cut Off At The End Of The Line
```