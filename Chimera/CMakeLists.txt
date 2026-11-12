# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds them for you.
# Gradle automatically packages shared libraries with your APK.

#set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})

# Searches for a specified prebuilt library and stores the path as a
# variable. Because CMake includes system libraries in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.
include_directories(${CMAKE_SOURCE_DIR}/src/main/cpp/include)

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )


add_library( # Sets the name of the library.
             driver

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             src/main/cpp/driver/Driver.cpp
             src/main/cpp/deinterlacer.cpp)

SET(CMAKE_CXX_FLAGS  "${CMAKE_CXX_FLAGS} -D_FORTIFY_SOURCE=0")

target_link_libraries( # Specifies the target library.
                       driver

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )

add_library( # Sets the name of the library.
        hdmiin

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/hdmiin.c )

target_link_libraries( # Specifies the target library.
        hdmiin

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib} )


if ( ${ANDROID_ABI} STREQUAL "arm64-v8a" )

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11 -fno-rtti -fno-exceptions -Wall")

add_library( # Sets the name of the library.
        nativeyuv

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/com_jht_wistron_hhsw_delaycamera_libyuv_NativeYUV.cpp )

add_library(yuv
        SHARED
        IMPORTED)

set_target_properties(yuv
        PROPERTIES IMPORTED_LOCATION
        ${CMAKE_SOURCE_DIR}/src/main/libs/${ANDROID_ABI}/libyuv.so
        )

target_link_libraries(nativeyuv
        yuv
        android
        jnigraphics
        log)
endif()