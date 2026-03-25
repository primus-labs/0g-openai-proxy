#!/bin/bash
#command such as:
#   ./compile.sh  /home/xuda/workspace/pado-server/pado-attestation-server/libs/include
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
cmake . -D CMAKE_INSTALL_PREFIX=${path} -D CMAKE_BUILD_TYPE=Debug
make
