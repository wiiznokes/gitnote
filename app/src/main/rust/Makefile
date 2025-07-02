
export PATH := ${HOME}/Android/Sdk/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin:$(PATH)


export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER := x86_64-linux-android21-clang
export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER := aarch64-linux-android21-clang



export OPENSSL_NO_VENDOR_PRIV ?= 1

ARCH ?= x86_64

MAKEFILE_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

ifeq ($(OPENSSL_NO_VENDOR_PRIV),1)
	export OPENSSL_NO_VENDOR := 1
	export OPENSSL_STATIC := 1
    export OPENSSL_DIR := $(abspath $(MAKEFILE_DIR)/openssl-prebuild/${ARCH}/install)
endif




# rustup target add x86_64-linux-android aarch64-linux-android


build:
	cargo build --target=${ARCH}-linux-android

build-release:
	cargo build --release --target=${ARCH}-linux-android

clean:
	cargo clean