SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "sh path: $SCRIPT_DIR"
echo "project's root path: $PROJECT_ROOT"

cd "$PROJECT_ROOT" || { echo "Can't cd project's root path: $PROJECT_ROOT"; exit 1; }

# 1.记录原始url
ORIGIN_DISTRIBUTION_URL=$(grep "distributionUrl" gradle/wrapper/gradle-wrapper.properties | cut -d "=" -f 2)
echo "origin gradle url: $ORIGIN_DISTRIBUTION_URL"
# 2.切换gradle版本
NEW_DISTRIBUTION_URL="https\:\/\/services.gradle.org\/distributions\/gradle-7.5.1-bin.zip"
sed -i.bak "s/distributionUrl=.*$/distributionUrl=$NEW_DISTRIBUTION_URL/" gradle/wrapper/gradle-wrapper.properties

# 3.语法兼容修改
ios_main_dir="core/src/appleMain/kotlin/com/tencent/kuikly"

ios_platform_impl="$ios_main_dir/core/module/PlatformImp.kt"
sed -i.bak '/@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)/d' "$ios_platform_impl"

ios_exception_tracker="$ios_main_dir/core/exception/ExceptionTracker.kt"
sed -i.bak \
    -e '/@file:OptIn(kotlin\.experimental\.ExperimentalNativeApi::class)/d' \
    -e 's/import kotlin\.concurrent\.AtomicReference/import kotlin.native.concurrent.AtomicReference/g' \
    "$ios_exception_tracker"

ios_verify_util="$ios_main_dir/core/utils/VerifyUtil.ios.kt"
sed -i.bak '/@OptIn(kotlinx\.cinterop\.ExperimentalForeignApi::class)/d' "$ios_verify_util"

# 4.开始发布
MODULE=${1:-all}
PUBLISH_TASK=${2:-publishToMavenLocal}
if [ "$MODULE" = "all" ]; then
  echo "编译所有模块 core-annotations、core-ksp、core、core-render-android"
  echo "发布方式: $PUBLISH_TASK"
  KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :core:$PUBLISH_TASK --stacktrace
  KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :core-annotations:$PUBLISH_TASK --stacktrace
  KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :core-ksp:$PUBLISH_TASK --stacktrace
  KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :core-render-android:$PUBLISH_TASK --stacktrace

else
  echo "编译模块: $MODULE"
  echo "发布方式: $PUBLISH_TASK"
  KUIKLY_KOTLIN_VERSION="1.7.20" ./gradlew -c settings.1.7.20.gradle.kts :$MODULE:$PUBLISH_TASK --stacktrace
fi

# 5.还原文件
mv gradle/wrapper/gradle-wrapper.properties.bak gradle/wrapper/gradle-wrapper.properties
mv "$ios_platform_impl.bak" "$ios_platform_impl"
mv "$ios_exception_tracker.bak" "$ios_exception_tracker"
mv "$ios_verify_util.bak" "$ios_verify_util"