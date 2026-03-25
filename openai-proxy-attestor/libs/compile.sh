#!/bin/bash
#command such as:
#   ./compile.sh  /home/xuda/workspace/pado-server/pado-attestation-server/libs/include
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="${SCRIPT_DIR}/build"

echo $1
path=$1
#JAVA_HOME=/home/xuda/env/jdk1.8.0_202
if [ ! ${JAVA_HOME} ]; then
  echo 'Please set environment JAVA_HOME!'
  exit 1
fi

if [ ! -f ${JAVA_HOME}"/include/jni.h" ]; then
  echo  'jni.h is not exist , please check you JAVA_HOME'
  exit 1
fi

if [ ! ${path} ]; then
  echo "The path cannot be empty. Please add the path parameter in the command line"
  exit 1
else
  echo 'libpado.so path is '${path}
fi
cmake -S "${SCRIPT_DIR}" -B "${BUILD_DIR}" -D CMAKE_INSTALL_PREFIX="${path}" -D CMAKE_BUILD_TYPE=Debug
cmake --build "${BUILD_DIR}"

JNI_LIB="$(find "${BUILD_DIR}" -maxdepth 1 -type f \( -name 'libpado_jni.so' -o -name 'libpado_jni.dylib' \) | head -n 1)"

if [ ! "${JNI_LIB}" ]; then
  echo "libpado_jni output not found under ${BUILD_DIR}"
  exit 1
fi

cp "${JNI_LIB}" "${SCRIPT_DIR}/$(basename "${JNI_LIB}")"
echo "Copied $(basename "${JNI_LIB}") to ${SCRIPT_DIR}"
