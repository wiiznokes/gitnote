# GitNote WIP

*Supported Android version: 12-14*

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
- [ ] time based sort
- [ ] rename/create/delete noteFolder/directory
- [ ] available on f-droid
- [ ] easy login (without copy pasting token, like GitJournal)
- [ ] Markdown integration
- [ ] private repo with ssh
- [ ] encryption

<p  align="middle">
  <img src="assets/grid.png" width="32%" />
  <img src="assets/drawer.png" width="32%" /> 
  <img src="assets/edit.png" width="32%" />
</p>

# Install

You can either download from the release page, or use [Obtainium](https://github.com/ImranR98/Obtainium), to get updates easily.

# Build

This app uses the [libgit2](https://github.com/libgit2/libgit2) library.
You can compile it from source or use the binaries already in place in the jniLibs folder.

You will necessarily have to clone `libgit2` for the headers:

```
git submodule init
git submodule update
```
