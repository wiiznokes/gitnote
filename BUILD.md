# Build

This app uses this [fork](https://github.com/wiiznokes/libgit2-android) of the [libgit2](https://github.com/libgit2/libgit2) library.
You can compile it from source or use the binaries already in place in the jniLibs folder. Note that this binaries are stored using git-lfs. I think you just need to have this package when cloning the repo and `git` will download them automatically.

I all case, you will need to clone the [repo](https://github.com/wiiznokes/libgit2-android) to get the headers.

```
git submodule update --init
```

(Add `--recursive` if you want to build libgit2 from source).

You can't just `sudo apt install libgit2-dev` because `cmake` won't let you use the header in `/usr/include` for some reason.

I you want to compile libgit2, good luck, you can find info in the `build.sh` script.
