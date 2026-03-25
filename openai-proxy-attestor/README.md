# openai-proxy-attestor

`openai-proxy-attestor` is now a standalone subproject inside this repository. It contains:

- the original `pado-attestation-server` Java/JNI service
- the original `pado-attestation-callback` C++ callback sources under `callback/`

## Build locally

Use JDK 21 and set `JAVA_HOME` first.

This project requires a JDK 21-compatible Lombok version. The embedded Gradle build now uses Lombok `1.18.32`.

```shell
cd openai-proxy-attestor
cmake -S ./callback/pado-attestation-callback-lib -B ./callback/pado-attestation-callback-lib/build
cmake --build ./callback/pado-attestation-callback-lib/build
cp ./callback/pado-attestation-callback-lib/build/libpado_callback_lib.so ./libs/include/libpado_callback_lib.so
./gradlew clean installDist
bash ./libs/compile.sh "$(pwd)/libs/include"
```

## Build Docker image

```shell
cd openai-proxy-attestor
docker build -t openai-proxy-attestor:latest .
```

During `docker build`, the callback library is rebuilt from `callback/pado-attestation-callback-lib/` and replaces `libs/include/libpado_callback_lib.so` before JNI compilation.

## Runtime environment variables

Required:

- `SERVER_PORT`
- `LD_LIBRARY_PATH`
- `JAVA_FULL_NAME`

Callback mode:

- `CALL_URL` or `PADO_CALLBACK_URL`

Local signing mode:

- `KEYSTORE_PATH` or `keystorePath`
- `KEYSTORE_PASS` or `keystorePass`
