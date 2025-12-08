{
  description = "Dev shell for building GitNote (Android + Rust) on NixOS";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    rust-overlay.inputs.nixpkgs.follows = "nixpkgs";
    rust-overlay.url = "github:oxalica/rust-overlay";
  };

  outputs =
    {
      self,
      nixpkgs,
      rust-overlay,
    }:
    let
      system = "x86_64-linux";
      overlays = [ (import rust-overlay) ];
      pkgs = import nixpkgs {
        inherit system overlays;
        config.allowUnfree = true;
        config.android_sdk.accept_license = true;
      };

      # Match app/src/main/rust/RUST_VERSION
      rustToolchain = pkgs.rust-bin.stable."1.89.0".default.override {
        targets = [
          "aarch64-linux-android"
          "x86_64-linux-android"
        ];
        extensions = [
          "rust-src"
          "rustfmt"
          "clippy"
        ];
      };

      # Match app/build.gradle.kts + app/src/main/rust/NDK_VERSION
      android = pkgs.androidenv.composeAndroidPackages {
        platformVersions = [ "36" ];
        buildToolsVersions = [
          "35.0.0"
          "36.0.0"
        ];
        ndkVersions = [ "27.3.13750724" ];
        cmakeVersions = [ "3.22.1" ];
        includeEmulator = false;
        includeSources = false;
        includeSystemImages = false;
        includeNDK = true;
        includeCmake = true;
      };

      jdk = pkgs.jdk21;
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        packages = [
          android.androidsdk
          rustToolchain
          jdk
          pkgs.gradle
          pkgs.git
          pkgs.unzip
          pkgs.pkg-config
          pkgs.just
          pkgs.cmake
        ];

        JAVA_HOME = "${jdk}/lib/openjdk";

        shellHook = ''
          build_tools_version="36.0.0"
          cmake_version="3.22.1"
          sdk_root="${android.androidsdk}"
          for candidate in "$sdk_root" "$sdk_root/share/android-sdk" "$sdk_root/libexec/android-sdk"; do
            if [ -d "$candidate/platforms" ]; then
              sdk_root="$candidate"
              break
            fi
          done

          export ANDROID_SDK_ROOT="$sdk_root"
          export ANDROID_HOME="$sdk_root"
          export ANDROID_NDK_ROOT="$sdk_root/ndk/27.3.13750724"
          export ANDROID_NDK_HOME="$ANDROID_NDK_ROOT"
          export NDK_HOME="$ANDROID_NDK_ROOT"
          export NDK_PATH="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/bin"
          export ANDROID_SYSROOT="$ANDROID_NDK_ROOT/toolchains/llvm/prebuilt/linux-x86_64/sysroot"
          # Cross-compilers for Rust cc/cc-rs crates
          export CC_x86_64_linux_android="$NDK_PATH/x86_64-linux-android21-clang"
          export AR_x86_64_linux_android="$NDK_PATH/llvm-ar"
          export CC_aarch64_linux_android="$NDK_PATH/aarch64-linux-android21-clang"
          export AR_aarch64_linux_android="$NDK_PATH/llvm-ar"

          export PATH="$NDK_PATH:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/build-tools/$build_tools_version:$ANDROID_SDK_ROOT/cmake/$cmake_version/bin:$PATH"

          # FIXME: https://github.com/NixOS/nixpkgs/issues/402297
          # export GRADLE_OPTS="-Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_SDK_ROOT/build-tools/$build_tools_version/aapt2"
          # Workaround with `-Dorg.gradle.project.android.aapt2FromMavenOverride=$ANDROID_SDK_ROOT/build-tools/36.0.0/aapt2`

          echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"
          echo "ANDROID_NDK_ROOT=$ANDROID_NDK_ROOT"
          echo "Using Rust $(rustc --version)"
        '';
      };
    };
}
