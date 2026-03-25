FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace

RUN apt-get update \
    && apt-get install -y --no-install-recommends build-essential cmake \
    && rm -rf /var/lib/apt/lists/*

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle gradle
COPY src src
COPY libs libs
COPY certs certs
COPY cipher cipher
COPY callback callback

RUN chmod +x ./gradlew \
    && cmake -S ./callback/pado-attestation-callback-lib -B ./callback/pado-attestation-callback-lib/build \
    && cmake --build ./callback/pado-attestation-callback-lib/build \
    && cp ./callback/pado-attestation-callback-lib/build/libpado_callback_lib.so ./libs/include/libpado_callback_lib.so \
    && ./gradlew --no-daemon clean installDist \
    && bash ./libs/compile.sh "$(pwd)/libs/include"

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        libxext6 \
        libxrender1 \
        libxtst6 \
        libxi6 \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /app/libs

ENV JAVA_FULL_NAME=com/pado/PadoCallback
ENV LD_LIBRARY_PATH=/app/libs:/opt/java/openjdk/lib/server
ENV SERVER_PORT=8081

COPY --from=builder /workspace/build/install/openai-proxy-attestor /app/openai-proxy-attestor
COPY --from=builder /workspace/libs/libpado_jni.so /app/libs/libpado_jni.so
COPY --from=builder /workspace/libs/include/libpado.so /app/libs/libpado.so
COPY --from=builder /workspace/libs/include/libpado_callback_lib.so /app/libs/libpado_callback_lib.so
COPY --from=builder /workspace/libs/include/libpado_sim.so /app/libs/libpado_sim.so
COPY --from=builder /workspace/certs /app/certs
COPY --from=builder /workspace/cipher /app/cipher

EXPOSE 8081

ENTRYPOINT ["/app/openai-proxy-attestor/bin/openai-proxy-attestor"]
