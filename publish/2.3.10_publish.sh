#!/bin/bash
# Kotlin 2.3.10 分支发布脚本
#
# 与 2.1.21_publish.sh 的差异：
#   1) AGP 7.4.2 -> 8.6.0（Kotlin 2.3 要求 8.2.2 ~ 8.13.0）
#   2) Gradle 8.9 + JDK 17（AGP 8.6.0 要求 Gradle 8.7+，Gradle 8.9 要求 JDK 17），
#      由 publish/compatible/2.3.10.yaml 在发布期临时切换 wrapper，结束后还原
#   3) KSP 2.1.21-2.0.1 -> 2.3.4（由版本映射自动带出）
#
# 机制（与历史发布脚本同构：FileReplacer + compatible/<版本>.yaml）：
#   发布前 replace（临时切换 wrapper / settings 指针 / gradle.properties 属性），
#   发布后 restore 还原。settings.gradle.kts 全仓库唯一，无 settings.2.3.10.* 变体。
#
# 用法：
#   ./publish/2.3.10_publish.sh              # 发布全部模块到 mavenLocal
#   ./publish/2.3.10_publish.sh core publish # 只发布 core

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "sh path: $SCRIPT_DIR"
echo "project's root path: $PROJECT_ROOT"
cd "$PROJECT_ROOT" || { echo "Can't cd project's root path: $PROJECT_ROOT"; exit 1; }

# --- JDK 17（Gradle 8.9 硬性要求）---
# macOS 用 java_home 定位；Linux CI 等无该命令的环境尊重外部已设置的 JAVA_HOME
if command -v /usr/libexec/java_home >/dev/null 2>&1; then
  export JAVA_HOME=$(/usr/libexec/java_home -v 17)
fi
export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$PATH"
java -version

CONFIG_FILE="publish/compatible/2.3.10.yaml"

# --- 共享文件临时切换（发布结束统一还原） ---
# yarn.lock：JS 模块（core-render-web x3）发布需 2.3 版锁
LOCK_DIR="kotlin-js-store"
if [ -f "$LOCK_DIR/yarn.2.3.10.lock" ]; then
  cp "$LOCK_DIR/yarn.2.3.10.lock" "$LOCK_DIR/yarn.lock"
fi

# FileReplacer：临时切换 wrapper（7.6.3 -> 8.9）/ settings 指针（2.1.21 -> 2.3.10）/
# gradle.properties 属性（AGP 8 禁止），发布结束 restore 还原
java publish/FileReplacer.java replace "$CONFIG_FILE"

MODULE=${1:-all}
PUBLISH_TASK=${2:-publishToMavenLocal}
GRADLE_RUN_STATUS=0

run_gradle() {
  KUIKLY_AGP_VERSION="8.6.0" KUIKLY_KOTLIN_VERSION="2.3.10" \
    ./gradlew ":$1:$PUBLISH_TASK" --stacktrace
}

if [ "$MODULE" = "all" ]; then
  MODULES="core-annotations core core-ksp core-wx core-render-android compose core-gradle-plugin core-render-web:base core-render-web:h5 core-render-web:miniapp ui-tooling"
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

# --- 共享文件还原（无论发布成败都执行） ---
java publish/FileReplacer.java restore "$CONFIG_FILE"
if [ -f "$LOCK_DIR/yarn.2.1.21.lock" ]; then
  cp "$LOCK_DIR/yarn.2.1.21.lock" "$LOCK_DIR/yarn.lock"
fi

if [ $GRADLE_RUN_STATUS -eq 0 ]; then
  exit 0
else
  exit 1
fi
