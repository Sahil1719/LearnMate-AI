cmake_minimum_required(VERSION 3.22.1)
project("executorch_llama_jni")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Configure compiler flags for ARM64 NEON
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_compile_options(-O3 -DANDROID)
endif()

add_library(
    executorch_llama_jni
    SHARED
    native_llama_bridge.cpp
)

target_link_libraries(
    executorch_llama_jni
    log
    android
)
