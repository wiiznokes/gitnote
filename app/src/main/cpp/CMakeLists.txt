

cmake_minimum_required(VERSION 3.22.1)
project("gitnote")


set(JNI_LIBS ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

include_directories(${CMAKE_SOURCE_DIR}/../../../../libgit2-android/include)

# Attempt to use libgit2 headers from the system
#target_include_directories(logging PRIVATE /usr/include)
#include_directories(/usr/include)


add_library(${CMAKE_PROJECT_NAME} SHARED
        logging.cpp
        merge.cpp
        remote.cpp
        CallbackHandler.cpp
        gitnote.cpp)

target_link_libraries(${CMAKE_PROJECT_NAME}
        ${JNI_LIBS}/libgit2.so
        android
        log)
