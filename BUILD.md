# Build

### Linux

It simpler to build on Linux. You will need need to install

- [Rust](https://www.rust-lang.org/tools/install)
- install the necessary targets: `rustup target add x86_64-linux-android aarch64-linux-android`
- perl
- make

Go to `app/src/main/rust`, and call `make build_install`.
Don't forget to set `NDK_PATH` to the bin directory of the current ndk used by the app.
If you want to make a release build, set `DEBUG` to 0.
All C libraries are vendored by crates (openssl, libgit2, openssh).

You can now build the project like a regular Android app.

### Windows

On Windows, it's another story. The difficulty is to build openssl, because it require perl, but we can't just install the normal perl Windows version, because it will be incompatible with the build. (the error: `This perl implementation doesn't produce Unix like paths (with forward slash directory separators).  Please use an implementation that matches your building platform.`).
I think we could make it works by using a msys2 environment, but then, there is the problem of telling gradle to use this environment, and i'm not familiar with any of theses.

The recommended way to build on Windows is to install [Rust](https://www.rust-lang.org/tools/install), and use the already compiled openssl libs.
To do that,

0. Add the ndk bin path to PATH
1. open a git bash shell
2. `cd app/src/main/rust`
3. `make unzip_openssl_prebuild`
4. `make build_install` (you can add `DEBUG=0` for a release build). This will also copy the build artifacts to the jni folders

Et voilà!

# Release Builds

To build a release APK for distribution:

## 1. Generate Signing Keys

Run the key generation script:
```bash
./generate-release-keys.sh
```

Or use the just command:
```bash
just generate-release-keys
```

This will create a `app/key.jks` keystore file and display the required environment variables.

**Note:** For PKCS12 keystores (default), the key password must be the same as the store password.

## 2. Set Environment Variables

Either run the setup script:
```bash
./setup-release-env.sh
```

Or manually source the generated environment file:
```bash
source release-keys.env
```

The `release-keys.env` file is automatically created by the key generation script and contains your generated keys.

## 3. Build Release APK

```bash
just release-build
```

Or directly:
```bash
./gradlew :app:assembleRelease
```

## 4. Install to Device

```bash
just release-install
```

This will build the release APK and install it directly to your connected device using adb. It automatically checks for connected devices and provides helpful error messages if none are found.

## Security Notes

- Keep your `key.jks` file secure and never commit it to version control
- The generated passwords are for development/testing - use stronger passwords for production
- Consider using a password manager or secure environment variables for production builds
