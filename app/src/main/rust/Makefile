

export PATH := ${HOME}/Android/Sdk/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin:$(PATH)

export CC_x86_64_linux_android=x86_64-linux-android21-clang
export AR_aarch64_linux_android=llvm-ar

export CC_aarch64_linux_android=aarch64-linux-android21-clang
export AR_aarch64_linux_android=llvm-ar


# rustup target add x86_64-linux-android
build:
	cargo build --target=x86_64-linux-android