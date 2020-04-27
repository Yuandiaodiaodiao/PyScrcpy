cmake_minimum_required(VERSION 3.10)
project(ASremote)
add_definitions(/utf-8)
set(CMAKE_CXX_STANDARD 17)


include_directories(ffmpeg/include/)
link_directories(ffmpeg/lib/)
include_directories(.)
#include_directories(/usr/local/Cellar/sdl2/2.0.8/include/)
#link_directories(/usr/local/Cellar/sdl2/2.0.8/lib/)
#
#add_executable(ASremote main.cpp SDL_Screen.cpp SDL_Screen.h SocketConnection.cpp SocketConnection.h FFmpegDecoder.cpp FFmpegDecoder.h FrameCache.cpp FrameCache.h EventCache.cpp EventCache.h EventController.cpp EventController.h)
#
#target_link_libraries(
#        ASremote
#        avcodec
#        avdevice
#        avfilter
#        avformat
#        avresample
#        avutil
#        postproc
#        swresample
#        swscale
#        SDL2
#)
add_executable(ASremote main2.cpp)
target_link_libraries(ASremote avformat avutil avcodec avdevice avfilter)
add_library(dllcore SHARED dlltest.cpp)
SET(LIBRARY_OUTPUT_PATH ${PROJECT_SOURCE_DIR}/../lib)
add_executable(memcpytest memcpytest.cpp)


add_library(server3 SHARED server.cpp)
target_link_libraries(server3 avformat avutil avcodec avdevice avfilter)
add_executable(server4 server.cpp)
target_link_libraries(server4 avformat avutil avcodec avdevice avfilter)

if (CMAKE_BUILD_TYPE MATCHES "Release")
    add_definitions(/O2)
    message("release!1!")
endif ()