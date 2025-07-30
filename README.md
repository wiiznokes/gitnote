<div align="center">

<h1>
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo_wide_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="assets/logo_wide_light.svg">
  <img alt="GitNote" width="50%" src="assets/logo_wide_light.svg">
</picture>
</h1>

<a href="https://f-droid.org/en/packages/io.github.wiiznokes.gitnote/"><img src="https://f-droid.org/badge/get-it-on.svg" alt="F-Droid Badge" height="50"></a>
<a href="https://apt.izzysoft.de/packages/io.github.wiiznokes.gitnote"><img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="IzzyOnDroid Badge" height="50"></a>

</div>

_Supported Android versions: 11 to 16_

Android note app which integrate Git. You can use this app with other desktop editors.

## Why

Because all apps which integrate git on Android either separate the note title from the name of the file or use old UI/UX frameworks

# Features

- [x] create/open/clone repositories
- [x] fuzzy search (global and from specific folder)
- [x] grid view
- [x] tree view
- [x] edit view
- [x] private repo with SSH
- [x] remote sync
- [x] time based sort

<p  style="text-align: center;">
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/grid.png" width="32%"  alt="grid screen"/>
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/drawer.png" width="32%"  alt="drawer screen"/> 
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/edit.png" width="32%"  alt="edit screen"/>
</p>

# Build

[See](./BUILD.md).

# Current limitation

- Android does not differentiate case for file name, so if you have a folder named `A` and another folder named `a`, `a` will not be displayed.
- Conflict will make the app crash
