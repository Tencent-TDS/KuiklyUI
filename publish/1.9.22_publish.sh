SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "sh path: $SCRIPT_DIR"
echo "project's root path: $PROJECT_ROOT"

cd "$PROJECT_ROOT" || { echo "Can't cd project's root path: $PROJECT_ROOT"; exit 1; }

java -version

CONFIG_FILE="publish/compatible/1.9.22.yaml"

# 兼容性替换
java publish/FileReplacer.java replace "$CONFIG_FILE"

MODULE=${1:-all}
PUBLISH_TASK=${2:-publishToMavenLocal}
GRADLE_RUN_STATUS=0

if [ "$MODULE" = "all" ]; then
  echo "编译所有模块 core-annotations、core-ksp、core、core-render-android、compose、web:base、web:h5、web:miniapp"
  echo "发布方式: $PUBLISH_TASK"
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-annotations:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-ksp:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-render-android:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :compose:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-render-web:base:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-render-web:h5:$PUBLISH_TASK --stacktrace
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :core-render-web:miniapp:$PUBLISH_TASK --stacktrace

else
  echo "编译模块: $MODULE"
  echo "发布方式: $PUBLISH_TASK"
  KUIKLY_AGP_VERSION="7.4.2" KUIKLY_KOTLIN_VERSION="1.9.22" ./gradlew -c settings.1.9.22.gradle.kts :$MODULE:$PUBLISH_TASK --stacktrace
  GRADLE_RUN_STATUS=$?
fi

# 兼容性还原
java publish/FileReplacer.java restore "$CONFIG_FILE"

if [ $GRADLE_RUN_STATUS -eq 0 ]; then
  exit 0
else
  exit 1
fi