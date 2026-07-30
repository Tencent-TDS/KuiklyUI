SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "sh path: $SCRIPT_DIR"
echo "project's root path: $PROJECT_ROOT"

cd "$PROJECT_ROOT" || { echo "Can't cd project's root path: $PROJECT_ROOT"; exit 1; }

java -version

CONFIG_FILE="publish/compatible/2.0_ohos.yaml"

# 兼容性替换
java publish/FileReplacer.java replace "$CONFIG_FILE"

MODULE=${1:-all}
PUBLISH_TASK=${2:-publishToMavenLocal}
GRADLE_RUN_STATUS=0

KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="2.0.21-KBA-010" ./gradlew -c settings.2.0.ohos.gradle.kts :demo:linkSharedReleaseSharedOhosArm64  --stacktrace

# 兼容性还原
java publish/FileReplacer.java restore "$CONFIG_FILE"

# 5.拷贝so
echo "Copying artifact files:"
OHOS_RENDER_PROJECT_DIR=./ohosApp

TARGET_SO_PATH=$PWD/demo/build/bin/ohosArm64/sharedReleaseShared/libshared.so
OHO_SO_PROJECT_PATH=$OHOS_RENDER_PROJECT_DIR/entry/libs/arm64-v8a
cp $TARGET_SO_PATH $OHO_SO_PROJECT_PATH
echo "libshared.so: copied from $TARGET_SO_PATH to ohos demo directory: $OHO_SO_PROJECT_PATH"

TARGET_SO_HEADER_PATH=$PWD/demo/build/bin/ohosArm64/sharedReleaseShared/libshared_api.h
OHO_SO_HEADER_PATH=$OHOS_RENDER_PROJECT_DIR/entry/src/main/cpp/thirdparty/biz_entry
cp $TARGET_SO_HEADER_PATH $OHO_SO_HEADER_PATH
echo "libshared_api.h: copied from $TARGET_SO_HEADER_PATH to ohos demo directory: $OHO_SO_HEADER_PATH"
echo "Copy ops done!"