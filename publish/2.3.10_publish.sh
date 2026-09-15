#!/bin/bash
# Kotlin 2.3.10 分支发布脚本
#
# 与 2.1.21_publish.sh 的差异：
#   1) AGP 7.4.2 -> 8.6.0（Kotlin 2.3 要求 8.2.2 ~ 8.13.0）
#   2) Gradle 8.9（AGP 8.6.0 要求 8.7+；wrapper 保持 7.6.3 归默认构建，
#      由 ./k2310.sh 自带外部 Gradle 8.9）
#   3) JDK 17（Gradle 8.9 硬性要求）
#   4) KSP 2.1.21-2.0.1 -> 2.3.4（由版本映射自动带出）
#
# 实现方式：复用 ./k2310.sh 无状态入口（JDK 17 + 外部 Gradle 8.9 + 版本注入 +
#          yarn.lock 切换/还原），不修改 wrapper / gradle.properties 等共享文件。
#
# 用法：
#   ./publish/2.3.10_publish.sh              # 发布全部模块到 mavenLocal
#   ./publish/2.3.10_publish.sh core publish # 只发布 core

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "sh path: $SCRIPT_DIR"
echo "project's root path: $PROJECT_ROOT"
cd "$PROJECT_ROOT" || { echo "Can't cd project's root path: $PROJECT_ROOT"; exit 1; }

MODULE=${1:-all}
PUBLISH_TASK=${2:-publishToMavenLocal}
GRADLE_RUN_STATUS=0

run_gradle() {
  # 发布用 settings（不含 demo / androidApp 等 App 宿主），
  # 通过 K2310_SETTINGS 覆盖 k2310.sh 默认的 app settings
  K2310_SETTINGS="settings.2.3.10.gradle.kts" ./k2310.sh ":$1:$PUBLISH_TASK" --stacktrace
}

if [ "$MODULE" = "all" ]; then
  MODULES="core-annotations core core-ksp core-wx core-render-android compose core-render-web:base core-render-web:h5 core-render-web:miniapp"
  echo "编译所有模块: $MODULES"
  echo "发布方式: $PUBLISH_TASK"
  for m in $MODULES; do
    echo "---- 发布模块: $m ----"
    # 直接判断命令返回值，而非依赖 $? —— 避免后续在中间插入命令时静默覆盖退出码
    if ! run_gradle "$m"; then
      echo "发布失败: $m（任务 :$m:$PUBLISH_TASK）"
      GRADLE_RUN_STATUS=1
      break
    fi
  done
else
  echo "编译模块: $MODULE"
  echo "发布方式: $PUBLISH_TASK"
  if ! run_gradle "$MODULE"; then
    GRADLE_RUN_STATUS=1
  fi
fi

if [ $GRADLE_RUN_STATUS -eq 0 ]; then
  exit 0
else
  exit 1
fi
