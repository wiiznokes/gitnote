# GitNote Build Instructions

This file documents the build commands for the GitNote Android app, which uses Kotlin, Rust (via JNI), and Gradle.

## Prerequisites

- Android SDK and NDK installed
- Java JDK (configured in `.gradle/config.properties`)
- Rust toolchain with Android targets
- `just` command runner installed

**Important**: The `just` commands automatically set `JAVA_HOME` from `.gradle/config.properties`. When running `./gradlew` directly in terminal, you must manually set `JAVA_HOME` first:
```bash
JAVA_HOME=$(grep '^java.home=' .gradle/config.properties | cut -d'=' -f2) ./gradlew <command>
```

## Build Commands

All commands are run using the `just` command runner. The justfile contains the following targets:

### Rust Library Build
```bash
just rust-build
```
- Builds the Rust native library for Android (x86_64 and aarch64 targets)
- Copies shared libraries to `app/src/main/jniLibs/`
- Uses the Makefile in `app/src/main/rust/`

### Debug Build
```bash
just build
```
- Assembles the debug APK
- Sets JAVA_HOME from `.gradle/config.properties`
- Output: `app/build/outputs/apk/debug/app-debug.apk`

### Debug Install
```bash
just install
```
- Builds and installs the debug APK to connected device/emulator
- Requires device to be connected via ADB

### Release Build
```bash
just release-build
```
- Sets up release environment (signing keys, etc.)
- Assembles the release APK
- Output: `app/build/outputs/apk/release/app-release.apk`

### Release Install
```bash
just release-install
```
- Builds and installs the release APK to connected device
- Checks for connected device before installing
- Provides manual install instructions if no device found

## Additional Utility Commands

```bash
just fix          # Run lint fixes
just fmt-just     # Format the justfile
just prettier     # Format code with Prettier
just fix-wrapper  # Update Gradle wrapper (commented out)
```

## Key Files

- `justfile`: Contains all build recipes
- `app/src/main/rust/`: Rust native code
- `app/src/main/rust/Makefile`: Rust build configuration
- `.gradle/config.properties`: Java home configuration
- `setup-release-env.sh`: Release environment setup
- `generate-release-keys.sh`: Generate signing keys

## Development Workflow

1. Make changes to Kotlin code in `app/src/main/java/`
2. Make changes to Rust code in `app/src/main/rust/src/`
3. Run `just rust-build` to compile Rust changes
4. Run `just build` or `just install` to test
5. For release: `just release-build` or `just release-install`
6. **When adding features**: Update `CHANGELOG.md` and `doc/features.md` to document the new functionality

## Troubleshooting

- Ensure Android SDK/NDK are properly configured
- Check that Rust targets are installed: `rustup target add aarch64-linux-android x86_64-linux-android`
- Verify JAVA_HOME in `.gradle/config.properties` points to correct JDK
- For device issues, ensure ADB is working and device is authorized</content>
<parameter name="filePath">/home/data/Projects/Programming/Platform/Android/gitnote/copilot-instructions.md