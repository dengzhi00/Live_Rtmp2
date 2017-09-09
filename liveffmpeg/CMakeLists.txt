# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

#set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -fPIC -fpermissive -std=c++11 -D__STDC_CONSTANT_MACROS -DFIXED_POINT -DUSE_KISS_FFT -DEXPORT="" -UHAVE_CONFIG_H ")
#set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -O2 -DSYS=posix -DNO_CRYPTO")

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

set(main_path ${CMAKE_SOURCE_DIR}/src/main/cpp)
file(GLOB cpp_path ${main_path}/*.cpp)

find_library(log-lib log )


#
add_library(avcodec SHARED IMPORTED)
set_target_properties(avcodec PROPERTIES IMPORTED_LOCATION ../../../../libs/armeabi-v7a/libavcodec-56.so)

add_library(avdevice SHARED IMPORTED)
set_target_properties(avdevice PROPERTIES IMPORTED_LOCATION ../../../../libs/armeabi-v7a/libavdevice-56.so)

#
add_library(avfilter SHARED IMPORTED)
set_target_properties(avfilter PROPERTIES IMPORTED_LOCATION ../../../../libs/armeabi-v7a/libavfilter-5.so)

#
add_library(avformat SHARED IMPORTED)
set_target_properties(avformat PROPERTIES IMPORTED_LOCATION ../../../../libs/armeabi-v7a/libavformat-56.so)

#
add_library(avutil SHARED IMPORTED)
set_target_properties(avutil PROPERTIES IMPORTED_LOCATION ../../../../libs/armeabi-v7a/libavutil-54.so)

#
add_library(postproc SHARED IMPORTED)
set_target_properties(postproc PROPERTIES IMPORTED_LOCATION ../../../../libs/armeabi-v7a/libpostproc-53.so)

#
add_library(swresample SHARED IMPORTED)
set_target_properties(swresample PROPERTIES IMPORTED_LOCATION ../../../../libs/armeabi-v7a/libswresample-1.so)

#
add_library(swscale SHARED IMPORTED)
set_target_properties(swscale PROPERTIES IMPORTED_LOCATION ../../../../libs/armeabi-v7a/libswscale-3.so)

add_library(native-lib SHARED ${cpp_path} )
add_library(ffmpeg-lib STATIC src/main/cpp/native-lib.cpp src/main/cpp/Stream.cpp)
target_link_libraries(ffmpeg-lib)
target_link_libraries(native-lib avcodec avdevice avfilter avformat avutil postproc swresample swscale ffmpeg-lib ${log-lib} )

include_directories(${main_path}/include ${main_path})

