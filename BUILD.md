# Build

This app uses this [fork](https://github.com/wiiznokes/libgit2-android) of the [libgit2](https://github.com/libgit2/libgit2) library.
You can compile it from source or use the binaries already in place in the jniLibs folder.

I all case, you will need to clone this [repo](https://github.com/wiiznokes/libgit2-android) to get the header.

```
git clone https://github.com/wiiznokes/libgit2-android --depth=1 --branch=patch-android app/libgit2-android
```

Make sure you clone it in the `app` folder.

You can't just `sudo apt install libgit2-dev` because `cmake` won't let you use the header in `/usr/include` for some reason.

I you want to compile libgit2, good luck, you can find info in the `build.sh` script.
