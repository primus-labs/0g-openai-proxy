# README
## How to build

This library now uses CMake `find_package(JNI REQUIRED)`, so JDK 21 works as long as `JAVA_HOME` points to a valid JDK.

```shell
cmake -S . -B build
cmake --build build
```

If you want to run the standalone test program, set:

```shell
export JAVA_CLASS_PATH=-Djava.class.path=/path/to/callback.jar
export JAVA_FULL_NAME=com/pado/PadoCallback
```

