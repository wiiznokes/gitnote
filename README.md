<div align="center">

<h1>
<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/logo_wide_dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="assets/logo_wide_light.svg">
  <img alt="GitNote" width="50%" src="assets/logo_wide_light.svg">
</picture>
</h1>

</div>

[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/wiiznokes/gitnote.svg?logo=github&label=GitHub&cacheSeconds=3600)](https://github.com/wiiznokes/gitnote/releases/latest)
[![F-Droid](https://img.shields.io/f-droid/v/io.github.wiiznokes.gitnote?logo=f-droid&label=F-Droid&cacheSeconds=3600)](https://f-droid.org/packages/io.github.wiiznokes.gitnote)
[![IzzyOnDroid](https://img.shields.io/endpoint?url=https://apt.izzysoft.de/fdroid/api/v1/shield/io.github.wiiznokes.gitnote)](https://apt.izzysoft.de/fdroid/index/apk/io.github.wiiznokes.gitnote)

Android note app which integrate Git. You can use this app with other desktop editors.

## Why

Because all apps which integrate git on Android either separate the note title from the name of the file or use old UI/UX frameworks

# Features

- [x] create/open/clone repositories
- [x] notes search (global and from specific folder)
- [x] grid view
- [x] tree view
- [x] edit view
- [x] private repo (SSH and HTTPS)
- [x] remote sync
- [x] time based sort

<p  style="text-align: center;">
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/grid.png" width="32%"  alt="grid screen"/>
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/drawer.png" width="32%"  alt="drawer screen"/> 
  <img src="https://media.githubusercontent.com/media/wiiznokes/gitnote/master/assets/edit.png" width="32%"  alt="edit screen"/>
</p>

_Supported Android versions: 11 to 16_

_Supported Architecture: `arm64-v8a`, `x86_64`_

# Build

[See](./BUILD.md).

# Current limitation

- Only repositories with a branch "main" are supported
- Android does not differentiate case for file name, so if you have a folder named `A` and another folder named `a`, `a` will not be displayed.
- Conflict will make the app crash

## Contributing

See [this file](./CONTRIBUTING.md).
