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

KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.4.21-KBA-001" ./gradlew -c settings.2.4.21.ohos.gradle.kts :demo:linkSharedReleaseSharedOhosArm64 --stacktrace
GRADLE_RUN_STATUS=$?

# 兼容性还原
java publish/FileReplacer.java restore "$CONFIG_FILE"

if [ $GRADLE_RUN_STATUS -ne 0 ]; then
  exit $GRADLE_RUN_STATUS
fi

# 拷贝 so
echo "Copying artifact files:"
OHOS_RENDER_PROJECT_DIR=./ohosApp

TARGET_SO_PATH=$PWD/demo/build/bin/ohosArm64/sharedReleaseShared/libshared.so
OHO_SO_PROJECT_PATH=$OHOS_RENDER_PROJECT_DIR/entry/libs/arm64-v8a
mkdir -p $OHO_SO_PROJECT_PATH
cp $TARGET_SO_PATH $OHO_SO_PROJECT_PATH
echo "libshared.so: copied from $TARGET_SO_PATH to ohos demo directory: $OHO_SO_PROJECT_PATH"

TARGET_SO_HEADER_PATH=$PWD/demo/build/bin/ohosArm64/sharedReleaseShared/libshared_api.h
OHO_SO_HEADER_PATH=$OHOS_RENDER_PROJECT_DIR/entry/src/main/cpp/thirdparty/biz_entry
mkdir -p $OHO_SO_HEADER_PATH
cp $TARGET_SO_HEADER_PATH $OHO_SO_HEADER_PATH
echo "libshared_api.h: copied from $TARGET_SO_HEADER_PATH to ohos demo directory: $OHO_SO_HEADER_PATH"
echo "Copy ops done!"
