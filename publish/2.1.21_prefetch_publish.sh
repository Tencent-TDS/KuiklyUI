#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
CONFIG_FILE="publish/compatible/2.1.21-prefetch.yaml"
PUBLISH_TASK=${1:-publishToMavenLocal}

cd "$PROJECT_ROOT" || exit 1

restore_compatibility_files() {
  local publish_status=$?
  trap - EXIT
  if ! java publish/FileReplacer.java restore "$CONFIG_FILE"; then
    echo "Failed to restore prefetch compatibility files." >&2
    exit 1
  fi
  exit "$publish_status"
}
trap restore_compatibility_files EXIT

java publish/FileReplacer.java replace "$CONFIG_FILE" || exit 1

KUIKLY_AGP_VERSION="8.6.0" \
KUIKLY_KOTLIN_VERSION="2.1.21" \
./gradlew \
  -c settings.2.1.21.prefetch.gradle.kts \
  ":compose:$PUBLISH_TASK" \
  --stacktrace
