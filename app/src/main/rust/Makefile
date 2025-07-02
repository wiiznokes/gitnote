DEBUG ?= 1


ifeq ($(DEBUG),0)
	ARGS := --release
endif


ifeq ($(DEBUG),1)
	TARGET_PREFIX := debug
else
	TARGET_PREFIX := release
endif


MIN_SDK := 21


ifeq ($(OS),Windows_NT)
	export OPENSSL_NO_VENDOR_PRIV ?= 1
	export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER := x86_64-linux-android${MIN_SDK}-clang.cmd
	export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER := aarch64-linux-android${MIN_SDK}-clang.cmd
else
	export PATH := ${HOME}/Android/Sdk/ndk/27.2.12479018/toolchains/llvm/prebuilt/linux-x86_64/bin:$(PATH)
	export OPENSSL_NO_VENDOR_PRIV ?= 0
	export CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER := x86_64-linux-android${MIN_SDK}-clang
	export CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER := aarch64-linux-android${MIN_SDK}-clang
endif



MAKEFILE_DIR := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))

ifeq ($(OPENSSL_NO_VENDOR_PRIV),1)
	export OPENSSL_NO_VENDOR := 1
	export OPENSSL_STATIC := 1
endif




build:
ifeq ($(OPENSSL_NO_VENDOR_PRIV),1)
	OPENSSL_DIR=$(abspath $(MAKEFILE_DIR)/openssl-prebuild/x86_64/install) cargo build ${ARGS} --target=x86_64-linux-android
	OPENSSL_DIR=$(abspath $(MAKEFILE_DIR)/openssl-prebuild/aarch64/install) cargo build ${ARGS} --target=aarch64-linux-android
else
	cargo build ${ARGS} --target=x86_64-linux-android
	cargo build ${ARGS} --target=aarch64-linux-android
endif
	make cp_to_jni

unzip:
	unzip openssl-prebuild/x86_64/install.zip
	unzip openssl-prebuild/aarch64/install.zip

cp_to_jni:
	mkdir -p ../jniLibs/x86_64
	cp target/x86_64-linux-android/${TARGET_PREFIX}/libgit_wrapper.so ../jniLibs/x86_64/
	mkdir -p ../jniLibs/arm64-v8a
	cp target/aarch64-linux-android/${TARGET_PREFIX}/libgit_wrapper.so ../jniLibs/arm64-v8a/


clean:
	cargo clean