cmake_minimum_required(VERSION 3.4.1)

set(SOURCE_FILES
    src/main/jni/delta_patch.c
    src/main/jni/bspatch.c
    src/main/jni/bzip2/blocksort.c
    src/main/jni/bzip2/bzip2.c
    src/main/jni/bzip2/bzip2recover.c
    src/main/jni/bzip2/bzlib.c
    src/main/jni/bzip2/compress.c
    src/main/jni/bzip2/crctable.c
    src/main/jni/bzip2/decompress.c
    src/main/jni/bzip2/huffman.c
    src/main/jni/bzip2/mk251.c
    src/main/jni/bzip2/randtable.c
    src/main/jni/bzip2/spewG.c
    src/main/jni/bzip2/unzcrash.c
)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             delta

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             ${SOURCE_FILES} )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because system libraries are included in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

# If use android/log, uncomment below line.
# target_link_libraries( delta ${log-lib} )
