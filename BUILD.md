# Build

### Linux

It simpler to build on Linux. You will need need to install

- [Rust](https://www.rust-lang.org/tools/install)
- install the necessary targets: `rustup target add x86_64-linux-android aarch64-linux-android`
- perl

Then, just open Android studio and execute the task `cargoBuild`. You only need to re-execute it if you modify the rust part. All C libraries are vendored by crates (openssl, libgit2, openssh).

You can now build the project like a regular Android app.

And i think that's it (open an issue if not).

### Windows

On Windows, it's another story. The difficulty is to build openssl, because it require perl, but we can't just install the normal perl Windows version, because it will be incompatible with the build. (the error: `This perl implementation doesn't produce Unix like paths (with forward slash directory separators).  Please use an implementation that matches your building platform.`).
I think we could make it works by using a msys2 environment, but then, there is the problem of telling gradle to use this environment, and i'm not familiar with any of theses.

The recommended way to build on Windows is to install [Rust](https://www.rust-lang.org/tools/install), and use the already compiled openssl libs.
To do that,

1. open a git bash shell
2. `cd app/src/main/rust`
3. `make unzip`. This will unzip the openssl pre-build
4. `make build` (you can add `DEBUG=0` for a release build). This will also copy the build artifacts to the jni folders

Et voilà!
