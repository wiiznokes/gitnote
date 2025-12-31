# Markdown Header / Frontmatter Format

GitNote supports YAML frontmatter in Markdown files to store metadata. The frontmatter is enclosed between `---` markers at the beginning of the file.

## Supported Fields

- `title`: The title of the note (optional, can be inferred from filename).
- `updated`: Last modification timestamp in ISO format (e.g., `2025-12-31 15:51:02Z`).
- `created`: Creation timestamp in ISO format.
- `completed?`: Completion status, either `yes` or `no` (optional).
- `author`: Author name (optional).
- `tags`: List of tags (optional, one per line).

## Examples

### Basic Note with Completion
```
---
title: Buy Groceries
updated: 2025-12-31 15:51:02Z
created: 2025-12-30 10:00:00Z
completed?: no
---

- Milk
- Bread
- Eggs
```

### Note with Tags and Author
```
---
title: Project Meeting Notes
updated: 2025-12-31 14:30:00Z
created: 2025-12-31 09:00:00Z
author: John Doe
completed?: yes
tags:
  - work
  - meeting
  - project-alpha
---

Discussed the new features...
```

### Minimal Frontmatter
```
---
title: Quick Note
updated: 2025-12-31 12:00:00Z
created: 2025-12-31 12:00:00Z
---

Just a simple note without completion or tags.
```

## Notes
- Fields are case-sensitive.
- The `completed?` field is used by the app to display a checkbox in the UI.
- Timestamps should be in the format `yyyy-MM-dd HH:mm:ssZ`.
- Extra spaces around `?` and `:` are allowed (e.g., `completed ? : yes`).
- If no frontmatter is present, the app treats the note as incomplete.

## UI Rendering

### View Mode
- Frontmatter is automatically hidden when viewing Markdown notes in read-only mode.
- Only the content after the frontmatter is rendered as Markdown.
- This provides a clean reading experience without metadata clutter.

### Edit Mode
- Full content including frontmatter is shown when editing notes.
- Users can modify frontmatter fields directly in the text editor.