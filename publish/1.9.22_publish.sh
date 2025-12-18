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

# 3.开始发布
MODULE=${1:-all}
PUBLISH_TASK=${2:-publishToMavenLocal}
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
fi

# 4.还原文件
mv gradle/wrapper/gradle-wrapper.properties.bak gradle/wrapper/gradle-wrapper.properties