#!/usr/bin/env bash
# Compile and run the no-device context-worker stack regression.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE="$SCRIPT_DIR/test_krthread_worker_stack.cpp"
INCLUDE_DIR="$SCRIPT_DIR/../../main/cpp"
KRTHREAD_SOURCE="$INCLUDE_DIR/libohos_render/foundation/thread/KRThread.cpp"
OUTPUT_DIR="$SCRIPT_DIR/build"
OUTPUT="$OUTPUT_DIR/test_krthread_worker_stack"
CXX="${CXX:-clang++}"

if ! grep -q 'm_workerThread = KRSizedThread' "$KRTHREAD_SOURCE"; then
    echo "FAIL: KRThread does not construct its context worker with KRSizedThread"
    exit 1
fi
if grep -Eq 'm_workerThread = std::thread|pthread_(create|join)|pthread_attr_setstacksize' "$KRTHREAD_SOURCE"; then
    echo "FAIL: KRThread bypasses the sized-thread wrapper"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"
"$CXX" -std=c++17 -O0 -g -Wall -Wextra -Werror -pthread \
    -I "$INCLUDE_DIR" \
    "$SOURCE" -o "$OUTPUT"

# Keep the host default below the explicit worker stack so the regression is
# visible even on development machines whose normal pthread default is large.
ulimit -s 256
"$OUTPUT"
