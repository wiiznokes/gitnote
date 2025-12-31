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