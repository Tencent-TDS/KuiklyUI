#!/bin/bash
# =============================================================================
# Kotlin 2.3.10 无状态构建入口
#
# 设计原则：默认构建（./gradlew = 2.1.21）零侵入，本脚本每次调用自带完整
# 环境（JDK 17 + 外部 Gradle 8.9 + 2.3.10 settings + AGP 8.6.0），用完即走，
# 不修改、不残留任何共享文件（wrapper / gradle.properties / buildSrc 均不动）。
#
# 用法：
#   ./k2310.sh <gradle任务...>
#     例：./k2310.sh :core:compileDebugKotlinAndroid
#         ./k2310.sh :androidApp:assembleDebug
#         ./k2310.sh :demo:linkPodDebugFrameworkIosArm64
#
#   发布 settings（不含 demo/androidApp）可通过环境变量覆盖：
#     K2310_SETTINGS=settings.2.3.10.gradle.kts ./k2310.sh :core:publishToMavenLocal
#
# 版本兼容矩阵：
#   | 项       | 默认 ./gradlew (2.1.21) | 本脚本 (2.3.10)          |
#   |----------|--------------------------|--------------------------|
#   | Gradle   | wrapper 7.6.3（不动）    | 外部 gradle-8.9（独立）  |
#   | JDK      | 11                       | 17                       |
#   | AGP      | 7.4.2（buildSrc 默认）   | 8.6.0（环境变量注入）    |
#   | settings | settings.gradle.kts      | settings.2.3.10.app.*    |
#   | KSP      | 2.1.21-2.0.1             | 2.3.4                    |
# =============================================================================

set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# --- 1. JDK 17（Gradle 8.9 硬性要求；只作用于本脚本进程，不外泄） ---
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
export PATH=$JAVA_HOME/bin:$PATH
if ! java -version 2>&1 | grep -q '"17'; then
  echo "[k2310] ❌ 未找到 JDK 17（Gradle 8.9 要求）。请先安装。" >&2
  exit 1
fi

# --- 2. Gradle 8.9（外部独立发行版，绕过 wrapper——wrapper 保持 7.6.3 归默认构建使用） ---
# 优先复用 wrapper 缓存里已下载的 8.9；没有则下载到 .gradle-dist/
GRADLE_89="$(ls -d "$HOME"/.gradle/wrapper/dists/gradle-8.9-bin/*/gradle-8.9/bin/gradle 2>/dev/null | /usr/bin/head -1)"
if [ -z "$GRADLE_89" ] || [ ! -x "$GRADLE_89" ]; then
  DIST_DIR="$PWD/.gradle-dist"
  if [ ! -x "$DIST_DIR/gradle-8.9/bin/gradle" ]; then
    echo "[k2310] 首次运行：下载 Gradle 8.9 ..."
    mkdir -p "$DIST_DIR"
    curl -fSL https://services.gradle.org/distributions/gradle-8.9-bin.zip -o "$DIST_DIR/gradle-8.9.zip" || {
      echo "[k2310] ❌ Gradle 8.9 下载失败" >&2; exit 1; }
    unzip -q "$DIST_DIR/gradle-8.9.zip" -d "$DIST_DIR" && rm -f "$DIST_DIR/gradle-8.9.zip"
  fi
  GRADLE_89="$DIST_DIR/gradle-8.9/bin/gradle"
fi

# --- 3. 2.3.10 专属版本变量（Kotlin/AGP；KSP 由版本映射自动带出 2.3.4） ---
# 尊重外层已传入的值（如鸿蒙定制版 KUIKLY_KOTLIN_VERSION=2.3.10-KBAT-008），默认标准版
export KUIKLY_AGP_VERSION="${KUIKLY_AGP_VERSION:-8.6.0}"
export KUIKLY_KOTLIN_VERSION="${KUIKLY_KOTLIN_VERSION:-2.3.10}"

# --- 4. yarn.lock（不入库的本地文件；JS 构建需 2.3 版锁，结束后还原 2.1.21 版） ---
LOCK_DIR="kotlin-js-store"
if [ -f "$LOCK_DIR/yarn.2.3.10.lock" ]; then
  cp "$LOCK_DIR/yarn.2.3.10.lock" "$LOCK_DIR/yarn.lock"
fi
restore_lock() {
  if [ -f "$LOCK_DIR/yarn.2.1.21.lock" ]; then
    cp "$LOCK_DIR/yarn.2.1.21.lock" "$LOCK_DIR/yarn.lock"
  fi
}
trap restore_lock EXIT

# --- 5. 停掉 8.9 的旧 daemon（防止此前以错误环境启动的 daemon 被复用串台） ---
"$GRADLE_89" --stop > /dev/null 2>&1

# --- 6. 构建（默认用含全部模块的 app settings；发布场景用 K2310_SETTINGS 覆盖） ---
SETTINGS="${K2310_SETTINGS:-settings.2.3.10.app.gradle.kts}"
if [ ! -f "$SETTINGS" ]; then
  echo "[k2310] ❌ 找不到 $SETTINGS" >&2
  exit 1
fi

echo "[k2310] Kotlin 2.3.10 | AGP 8.6.0 | Gradle 8.9 | $(java -version 2>&1 | /usr/bin/head -1)"
echo "[k2310] settings: $SETTINGS"
echo ""

# 注意：不可用 exec —— exec 会替换本进程导致 trap EXIT 失效，yarn.lock 将无法还原
"$GRADLE_89" -c "$SETTINGS" "$@"
exit $?
