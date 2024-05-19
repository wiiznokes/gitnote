# GitNote WIP

_Supported Android versions: 11 to 14_

Android note which integrate Git. You can use this app with other desktop editors.

## Why

Because all apps which integrate git on Android either separate the note title from the name of the file or use old UI/UX frameworks

# Features

- [x] create/open/clone repositories
- [x] fuzzy search (global and from specific folder)
- [x] grid view
- [x] tree view
- [x] edit view
- [x] private repo with https
- [x] remote sync
- [x] edit/rename/create/update note/file
- [x] time based sort
- [ ] rename/create/delete noteFolder/directory
- [ ] Markdown integration
- [ ] available on f-droid
- [ ] private repo with ssh
- [ ] easy login (without copy pasting token, like GitJournal (require ssh support))

<p  style="text-align: center;">
  <img src="assets/grid.png" width="32%"  alt="grid screen"/>
  <img src="assets/drawer.png" width="32%"  alt="drawer screen"/> 
  <img src="assets/edit.png" width="32%"  alt="edit screen"/>
</p>

# Install

You can either download from the release page, or use [Obtainium](https://github.com/ImranR98/Obtainium), to get updates easily.

# Build

[See](./BUILD.md).

# Current limitation

- Android does not differentiate case for file name, so if you have a folder named `A` and another folder named `a`, `a` will not be displayed.
- Rotation change will break ssl state. Workaround: restart the app
- Conflict will make the app crash
