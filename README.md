<p align="left">
  <img align="left" src="assets/app_icon.svg" alt="app icon" width="90px" />
  <h1 style="display: inline-block; margin-left: 12px; vertical-align: middle;">GitNote</h1>
</p>

<a href="https://f-droid.org/en/packages/io.github.wiiznokes.gitnote/"><img src="https://f-droid.org/badge/get-it-on.svg" alt="F-Droid Badge" height="50"></a>
<a href="https://apt.izzysoft.de/packages/io.github.wiiznokes.gitnote"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="IzzyOnDroid Badge" height="50"></a>

<br/>

_Supported Android versions: 11 to 15_

Android note app which integrate Git. You can use this app with other desktop editors.

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
- [x] time based sort

<p  style="text-align: center;">
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/grid.png" width="32%"  alt="grid screen"/>
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/drawer.png" width="32%"  alt="drawer screen"/> 
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/edit.png" width="32%"  alt="edit screen"/>
</p>

# Install

You can either download from the release page, or use [Obtainium](https://github.com/ImranR98/Obtainium), to get updates easily.

# Build

[See](./BUILD.md).

# Current limitation

- Android does not differentiate case for file name, so if you have a folder named `A` and another folder named `a`, `a` will not be displayed.
- Rotation change will break ssl state. Workaround: restart the app
- Conflict will make the app crash
