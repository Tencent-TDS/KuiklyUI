#!/bin/bash
#
# Tencent is pleased to support the open source community by making KuiklyUI
# available.
# Copyright (C) 2026 Tencent. All rights reserved.
# Licensed under the License of KuiklyUI;
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
# https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

REPLACEMENT_CONFIG="publish/compatible/2.4.21_ohos.yaml"

restore_build_files() {
  java publish/FileReplacer.java restore "$REPLACEMENT_CONFIG"
}
trap restore_build_files EXIT

java publish/FileReplacer.java replace "$REPLACEMENT_CONFIG"

KUIKLY_AGP_VERSION="8.6.0" \
KUIKLY_KOTLIN_VERSION="2.4.21-KBA-001" \
./gradlew \
  -c settings.2.4.21.ohos.gradle.kts \
  :demo:linkSharedDebugSharedOhosArm64 \
  --stacktrace

OHOS_RENDER_PROJECT_DIR="$SCRIPT_DIR/ohosApp"
TARGET_SO_PATH="$SCRIPT_DIR/demo/build/bin/ohosArm64/sharedDebugShared/libshared.so"
TARGET_HEADER_PATH="$SCRIPT_DIR/demo/build/bin/ohosArm64/sharedDebugShared/libshared_api.h"
OHOS_SO_PATH="$OHOS_RENDER_PROJECT_DIR/entry/libs/arm64-v8a"
OHOS_HEADER_PATH="$OHOS_RENDER_PROJECT_DIR/entry/src/main/cpp/thirdparty/biz_entry"

mkdir -p "$OHOS_SO_PATH" "$OHOS_HEADER_PATH"
cp "$TARGET_SO_PATH" "$OHOS_SO_PATH/"
cp "$TARGET_HEADER_PATH" "$OHOS_HEADER_PATH/"

echo "Kotlin 2.4.21-KBA-001 OHOS artifact copied to ohosApp."
