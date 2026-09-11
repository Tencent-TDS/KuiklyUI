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

run_gradle() {
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" \
    ./gradlew -c settings.2.3.10.gradle.kts ":$1:$PUBLISH_TASK" --stacktrace
}

if [ "$MODULE" = "all" ]; then
  MODULES="core-annotations core core-ksp core-wx core-render-android compose core-render-web:base core-render-web:h5 core-render-web:miniapp"
  echo "编译所有模块: $MODULES"
  echo "发布方式: $PUBLISH_TASK"
  for m in $MODULES; do
    echo "---- 发布模块: $m ----"
    run_gradle "$m"
    if [ $? -ne 0 ]; then
      echo "发布失败: $m（任务 :$m:$PUBLISH_TASK）"
      GRADLE_RUN_STATUS=1
      break
    fi
  done
else
  echo "编译模块: $MODULE"
  echo "发布方式: $PUBLISH_TASK"
  run_gradle "$MODULE"
  GRADLE_RUN_STATUS=$?
fi

if [ $GRADLE_RUN_STATUS -eq 0 ]; then
  exit 0
else
  exit 1
fi