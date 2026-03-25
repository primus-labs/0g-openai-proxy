cmake_minimum_required(VERSION 3.25)
project(pado_callback_lib)

set(CMAKE_CXX_STANDARD 17)

# Find JNI package
find_package(JNI REQUIRED)

# Include JNI headers
include_directories(${JNI_INCLUDE_DIRS})

# Create shared library
add_library(pado_callback_lib SHARED PadoCallbackLib.cpp)

set_target_properties(pado_callback_lib PROPERTIES LINKER_LANGUAGE CXX)

target_link_libraries(pado_callback_lib ${JNI_LIBRARIES})
