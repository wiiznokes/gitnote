
export PATH := ${HOME}/Android/Sdk/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin:$(PATH)


export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER := x86_64-linux-android21-clang
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER := aarch64-linux-android21-clang

# rustup target add x86_64-linux-android aarch64-linux-android


build:
	cargo build --target=x86_64-linux-android
	cargo build --target=aarch64-linux-android

build-release:
	cargo build --release --target=x86_64-linux-android
	cargo build --release --target=aarch64-linux-android