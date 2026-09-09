SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "sh path: $SCRIPT_DIR"
echo "project's root path: $PROJECT_ROOT"
cd "$PROJECT_ROOT" || { echo "Can't cd project's root path: $PROJECT_ROOT"; exit 1; }

java -version

# 注：Gradle 8.9 / gradle.properties 的 AGP8 属性移除等兼容性改动
# 已随「默认版本升级到 2.3.10」直接固化进仓库文件，不再需要 FileReplacer 运行时替换。

MODULE=${1:-all}
PUBLISH_TASK=${2:-publishToMavenLocal}
GRADLE_RUN_STATUS=0

if [ "$MODULE" = "all" ]; then
  echo "编译所有模块 core-annotations、core-ksp、core、core-wx、core-render-android、compose、web:base、web:h5、web:miniapp"
  echo "发布方式: $PUBLISH_TASK"
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :core-annotations:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :core:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :core-ksp:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :core-wx:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :core-render-android:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :compose:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :core-render-web:base:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :core-render-web:h5:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :core-render-web:miniapp:$PUBLISH_TASK --stacktrace

else
  echo "编译模块: $MODULE"
  echo "发布方式: $PUBLISH_TASK"
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" ./gradlew -c settings.2.3.10.gradle.kts :$MODULE:$PUBLISH_TASK --stacktrace
  GRADLE_RUN_STATUS=$?
fi

if [ $GRADLE_RUN_STATUS -eq 0 ]; then
  exit 0
else
  exit 1
fi