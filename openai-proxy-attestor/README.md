# openai-proxy-attestor

`openai-proxy-attestor` is now a standalone subproject inside this repository. It contains:

- the original `pado-attestation-server` Java/JNI service
- the original `pado-attestation-callback` C++ callback sources under `callback/`

## Build locally

Use JDK 21 and set `JAVA_HOME` first.

```shell
cd openai-proxy-attestor
./gradlew clean installDist
bash ./libs/compile.sh "$(pwd)/libs/include"
```

## Build Docker image

```shell
cd openai-proxy-attestor
docker build -t openai-proxy-attestor:latest .
```

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
