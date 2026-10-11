cmake_minimum_required(VERSION 3.22.1)

project(ua_cryptography_jni LANGUAGES CXX)

# The library of the pinned ua-cryptography-rust release, supplied per ABI by the supplyNativeLibraries task.
add_library(ua_cryptography SHARED IMPORTED)
set_target_properties(ua_cryptography PROPERTIES
    IMPORTED_LOCATION "${UA_CRYPTOGRAPHY_LIBRARY_DIR}/${ANDROID_ABI}/libua_cryptography.so"
    # The release library has no SONAME: linked by name, the loader looks for libua_cryptography.so, not a build path.
    IMPORTED_NO_SONAME TRUE
    INTERFACE_INCLUDE_DIRECTORIES "${CMAKE_CURRENT_SOURCE_DIR}/include")

add_library(ua_cryptography_jni SHARED
    jni_marshalling.cpp
    ua_cryptography_jni.cpp)
target_compile_features(ua_cryptography_jni PRIVATE cxx_std_17)
target_compile_options(ua_cryptography_jni PRIVATE -Wall -Wextra -Werror -fvisibility=hidden)
target_link_libraries(ua_cryptography_jni PRIVATE ua_cryptography)
# The 16 KB pages of Android 15 devices; NDK r28 does it already, this keeps an older NDK from undoing it.
target_link_options(ua_cryptography_jni PRIVATE "-Wl,-z,max-page-size=16384")
