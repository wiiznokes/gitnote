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
- **Final Folder Navigation**: When navigating to a folder that has no subfolders (a "leaf" folder) during normal browsing, the drawer closes to let you focus on the notes in that folder.
- **Mode Switching**: Switching between tag and folder modes clears the respective filters (tags when switching to folder mode, folder when switching to tag mode) to prevent confusion.

This behavior ensures a smooth user experience by reducing the need for manual drawer management while browsing and filtering notes. Note that when moving notes, the drawer remains open during folder navigation to allow selecting the destination folder.

### Folder Display Mode

GitNote allows users to control whether notes from subfolders are included when browsing a folder, providing flexible folder navigation options.

#### How It Works
- **Current Folder Only**: Shows only notes directly in the selected folder, excluding notes from subfolders.
- **Include Subfolders**: Shows notes from the selected folder and all its subfolders (default behavior).
- **Setting Location**: Configure this option in Settings > Appearance > Folder display mode.
- **Real-time Updates**: Changes take effect immediately when switching folders or updating the setting.

#### Usage Tips
- Use "Current folder only" for focused work in specific folders without subfolder distractions.
- Use "Include subfolders" (default) to see all related notes in a folder hierarchy.
- The setting applies to both grid and list views.
- Search functionality respects the current folder display mode setting.

#### Example Workflow
1. Navigate to a folder containing both direct notes and subfolders with notes.
2. Open Settings > Appearance and select "Folder display mode".
3. Choose "Current folder only" to see only notes directly in the current folder.
4. Switch back to "Include subfolders" to see all notes in the folder hierarchy.

#### Folder Structure Example
```
📁 Project/
├── 📄 project-overview.md
├── 📁 planning/
│   ├── 📄 timeline.md
│   └── 📄 budget.md
└── 📁 implementation/
    └── 📄 code-structure.md
```

- **Current folder only** in "Project/": Shows only `project-overview.md`
- **Include subfolders** in "Project/": Shows all 4 notes from the entire hierarchy

### User Interface
- **Grid and List Views**: Switch between grid and list layouts for notes.
- **Search and Sort**: Search notes by content, sort by date, title, etc.
- **Dark Mode**: Automatic theme switching.
- **Note Actions Menu**: Long-press notes to access options like delete, multi-select, convert between notes and tasks, and move notes.
- **Minimal Note Width**: Adjustable minimum width for notes in grid view (100dp, 150dp, 200dp, 250dp).
- **Offline Indicator**: Persistent icon showing when the app is offline and cannot sync.
- **Tag Display Mode**: Control whether tags are shown in grid view, list view, both, or neither.

## Completion Checkbox Feature

GitNote now supports marking notes as completed using a checkbox in the UI, tied to the `completed?` field in the frontmatter.

### How It Works
- **Checkbox Display**: A checkbox appears next to the note title if the note has frontmatter.
- **Toggling**: Click the checkbox to toggle between `completed?: yes` and `completed?: no`.
- **Automatic Updates**: Toggling updates the `updated` timestamp and saves the changes to the file and Git repository.
- **Visual Feedback**: Completed notes can be visually distinguished (future feature).
- **List View Icons**: In list view, tasks show a checkbox icon while notes show a document icon.

### Usage Tips
- Add frontmatter to notes to enable the checkbox.
- Use completion for tasks, reminders, or project tracking.
- The checkbox is read-only in display; editing requires toggling in the app.
- Changes are committed to Git automatically.
- In list view, the icon type (checkbox vs document) indicates whether the item is a task or note.

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

## Task Sorting in List View

Completed tasks are automatically sorted to the end of the list view to keep active tasks visible at the top.

### How It Works
- **Automatic Sorting**: In list view, completed tasks (with `completed?: yes`) appear after incomplete tasks.
- **Primary Sort**: Tasks are first sorted by completion status, then by the selected sort order (date, title, etc.).
- **Grid View**: This sorting only applies to list view; grid view maintains the standard sort order.
- **Real-time Updates**: Sorting updates immediately when task completion status changes.

### Usage Tips
- Use list view to keep your active tasks at the top of the list.
- Completed tasks remain accessible but don't clutter the active task list.
- This feature works automatically for all notes with completion status.
- Switch to grid view if you prefer a different organization.

### Example
In list view, with sort by "Most recent":
```
[ ] Active Task 1 (today)
[ ] Active Task 2 (yesterday)
[x] Completed Task 1 (last week)
[x] Completed Task 2 (last month)
```

## Floating Action Button (FAB)

The FAB provides quick access to create new notes and tasks directly.

### How It Works
- **Expandable Menu**: Long-press or tap the FAB to expand creation options.
- **Create Note**: Creates a new regular note with document icon.
- **Create Task**: Creates a new task with checkbox icon and `completed?: no` in frontmatter.
- **Search**: Quick access to the search functionality.
- **Smart Defaults**: New items inherit the current folder and use query text as title if available.

### Usage Tips
- Use the document icon to create regular notes.
- Use the checkbox icon to create tasks that need completion tracking.
- The FAB respects the current folder context for new items.
- Search option focuses the search bar for quick note finding.

### Example Workflow
1. Navigate to the desired folder.
2. Tap the FAB to expand options.
3. Select the checkbox icon to create a new task.
4. The task appears immediately with completion checkbox.

## Move Note Feature

GitNote allows moving notes between folders using an intuitive drag-and-drop style interface.

### How It Works
- **Initiate Move**: Long-press a note and select "Move note" from the menu.
- **Drawer Navigation**: The navigation drawer opens in move mode, allowing folder browsing.
- **Folder Selection**: Navigate through folders without closing the drawer.
- **Confirm Move**: Click the check icon ("Insert here") to move the note to the current folder.
- **Cancel Move**: Click the close icon to abort the move operation.
- **Automatic Close**: The drawer closes automatically after a successful move.

### Usage Tips
- Use the drawer to browse and select destination folders.
- The drawer stays open during folder navigation in move mode.
- Move operations update the note's file path and commit changes to Git.
- Cancel at any time without making changes.

### Example Workflow
1. Long-press a note in the current folder.
2. Select "Move note" from the menu.
3. Use the drawer to navigate to the target folder.
4. Click the check icon to complete the move.
5. The drawer closes, showing the note in its new location.

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

## Background Git Operations

GitNote allows users to perform git synchronization operations asynchronously to prevent UI blocking during all note operations, not just refreshes.

### How It Works
- **Setting Toggle**: Enable "Background git operations" in Settings > Git
- **Non-blocking Operations**: When enabled, git pull/push operations run asynchronously after any note modification (create, update, delete, convert)
- **Immediate Commits**: Changes are committed immediately for data integrity, then sync operations happen in the background
- **UI Responsiveness**: The app remains responsive during all operations, allowing continued note editing
- **Status Updates**: Sync state indicators still update to show current operation status
- **Default Behavior**: Disabled by default to maintain immediate sync feedback

### Usage Tips
- Enable this setting if you work alone and want uninterrupted note editing during any operation
- The setting is ideal for users who perform frequent note operations and prefer responsiveness over immediate sync feedback
- Disable it if you need to know immediately when sync operations complete or fail
- Background operations don't show error toasts to avoid interrupting your workflow
- Applies to all note operations: creating, editing, deleting, converting between tasks/notes, moving notes

### Example Workflow
1. Open Settings > Git and enable "Background git operations"
2. Edit notes, convert tasks, create new notes - all operations complete immediately
3. Check sync status indicators to monitor background operations
4. Continue working without waiting for git synchronization
5. Disable the setting if you need blocking sync behavior for collaboration workflows