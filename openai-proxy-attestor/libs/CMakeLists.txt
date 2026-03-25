cmake_minimum_required(VERSION 3.22.1)
project(pado-attestation-server)

set(CMAKE_CXX_STANDARD 17)
find_package(JNI REQUIRED)

include_directories(${JNI_INCLUDE_DIRS} ./include)

add_library(pado_jni SHARED com_pado_jni_JNI.cpp)

target_link_libraries(
    pado_jni
    ${CMAKE_INSTALL_PREFIX}/libpado.so
    ${CMAKE_INSTALL_PREFIX}/libpado_callback_lib.so
)
