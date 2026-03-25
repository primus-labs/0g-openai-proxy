cmake_minimum_required(VERSION 3.25)
project(pado-callback-test)

set(CMAKE_CXX_STANDARD 17)

link_directories(${CMAKE_CURRENT_SOURCE_DIR})

add_executable(pado-callback-test main.cpp)

target_link_libraries(pado-callback-test pado_callback_lib)
