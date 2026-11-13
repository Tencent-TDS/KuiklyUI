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

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "sh path: $SCRIPT_DIR"
echo "project's root path: $PROJECT_ROOT"

cd "$PROJECT_ROOT" || { echo "Can't cd project's root path: $PROJECT_ROOT"; exit 1; }

java -version

CONFIG_FILE="publish/compatible/2.4.21_ohos.yaml"

# 兼容性替换
java publish/FileReplacer.java replace "$CONFIG_FILE"

MODULE=${1:-all}
PUBLISH_TASK=${2:-publishToMavenLocal}
GRADLE_RUN_STATUS=0

if [ "$MODULE" = "all" ]; then
  echo "编译所有模块 core-annotations、core-ksp、core、core-render-android、compose"
  echo "发布方式: $PUBLISH_TASK"
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.4.21-KBA-001" ./gradlew -c settings.2.4.21.ohos.gradle.kts :core-annotations:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.4.21-KBA-001" ./gradlew -c settings.2.4.21.ohos.gradle.kts :core:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.4.21-KBA-001" ./gradlew -c settings.2.4.21.ohos.gradle.kts :core-ksp:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.4.21-KBA-001" ./gradlew -c settings.2.4.21.ohos.gradle.kts :core-render-android:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.4.21-KBA-001" ./gradlew -c settings.2.4.21.ohos.gradle.kts :compose:$PUBLISH_TASK --stacktrace

else
  echo "编译模块: $MODULE"
  echo "发布方式: $PUBLISH_TASK"
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.4.21-KBA-001" ./gradlew -c settings.2.4.21.ohos.gradle.kts :$MODULE:$PUBLISH_TASK --stacktrace
  GRADLE_RUN_STATUS=$?
fi

# 兼容性还原
java publish/FileReplacer.java restore "$CONFIG_FILE"

if [ $GRADLE_RUN_STATUS -eq 0 ]; then
  exit 0
else
  exit 1
fi
