#!/usr/bin/env sh
set -eu

# Build the Docker image for this service.
# Usage:
#   ./build.sh

IMAGE_NAME="primuslabs/${IMAGE_NAME:-openai-proxy}"
IMAGE_TAG="${IMAGE_TAG:-latest}"

docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" .

