#!/bin/bash

export JAVA_CLASS_PATH='-Djava.class.path=/home/xuda/workspace/pado-server/pado-attestation-callback/pado-attestation-callback-test/pado-attestation-callback-jar-1.0-all.jar'
export JAVA_FULL_NAME=com/pado/PadoCallback
export CALL_URL=http://18.179.8.186:8080/


cmake .
make

./pado-callback-test