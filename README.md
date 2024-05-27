<table cellpadding="0"border="0">
  <tr>
    <td><h1>GitNote</h1></td>
    <td><img src="assets/gitnote_icon.svg" alt="app icon" width="100px" /></td>
  </tr>
</table>

_Supported Android versions: 11 to 14_

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
