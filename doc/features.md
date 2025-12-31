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

This feature enhances productivity by allowing quick status updates without opening the editor.