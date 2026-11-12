#!/bin/bash
# =============================================================================
# Kuikly 多版本构建执行器
#
# 用法：
#   ./kbuild.sh <kotlin版本> <gradle任务...>
#     例：./kbuild.sh 2.3.10 :androidApp:assembleDebug
#         ./kbuild.sh 2.3.10 :demo:linkPodDebugFrameworkIosArm64
#         ./kbuild.sh 2.1.21 :core:compileDebugKotlinAndroid
#   指定 settings 变体（ohos 等特殊线）：
#     ./kbuild.sh --settings settings.2.3.10.ohos.gradle.kts 2.3.10 <任务>
#
# 机制（与历史发布脚本同构：FileReplacer + compatible/<版本>.yaml）：
#   1. java publish/FileReplacer.java replace publish/compatible/<版本>.yaml
#      —— 按该版本的 yaml 临时切换共享文件（wrapper / settings 指针 / gradle.properties）
#   2. ./gradlew <任务>（wrapper 已切到对应版本；JDK 由本脚本按版本选择）
#   3. trap EXIT：FileReplacer restore 还原全部替换（正常/错误退出均覆盖）
#
# JDK 选择：
#   - 2.3.10+（Gradle 8.9+ 要求 JDK 17）：macOS 用 java_home 定位，
#     其他平台尊重外部 JAVA_HOME
#   - 其余版本：尊重外部 JAVA_HOME（2.1.21/2.0.21 默认 11，JDK 17 亦可运行）
#
# 发布：直接透传 publish 任务（版本号用 KUIKLY_VERSION 注入，同历史发布脚本）
#   例：KUIKLY_VERSION=2.27.0-beta-SNAPSHOT ./kbuild.sh 2.3.10 :core:publishToMavenLocal
#
# 新增一个 Kotlin 版本需要：① 各模块 build.<版本>.gradle.kts（既有惯例）
#   ② publish/compatible/<版本>.yaml（默认版本可为空壳）——本脚本零改动
# =============================================================================

set -o pipefail

# --- 参数解析：可选 --settings 变体，第一个位置参数为 Kotlin 版本，其余透传给 gradlew ---
SETTINGS_OVERRIDE=""
while [ $# -gt 0 ]; do
  case "$1" in
    --settings) SETTINGS_OVERRIDE="$2"; shift 2 ;;
    --settings=*) SETTINGS_OVERRIDE="${1#--settings=}"; shift ;;
    *) break ;;
  esac
done
VERSION="${1:?用法: ./kbuild.sh [--settings <settings文件>] <kotlin版本> <gradle任务...>}"
shift
[ $# -gt 0 ] || { echo "[kbuild] ❌ 缺少 gradle 任务参数。示例: ./kbuild.sh 2.3.10 :androidApp:assembleDebug" >&2; exit 1; }

YAML="publish/compatible/${VERSION}.yaml"
[ -f "$YAML" ] || { echo "[kbuild] ❌ 找不到 $YAML（该版本无 compatible 配置）" >&2; exit 1; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

# --- 1. 共享文件运行期临时切换（trap EXIT 统一还原，保证零残留） ---
# yarn.lock（不入库的本地文件；切到目标版本锁，结束后还原默认锁）
# 注：wrapper / settings 指针 / gradle.properties 的切换由 FileReplacer 按对应 yaml 执行（见步骤 2）
LOCK_DIR="kotlin-js-store"
YARN_TARGET="$LOCK_DIR/yarn.${VERSION}.lock"
if [ -f "$YARN_TARGET" ]; then
  cp "$YARN_TARGET" "$LOCK_DIR/yarn.lock"
fi

restore_shared_files() {
  if [ -f "$LOCK_DIR/yarn.2.1.21.lock" ]; then
    cp "$LOCK_DIR/yarn.2.1.21.lock" "$LOCK_DIR/yarn.lock"
  fi
}
trap restore_shared_files EXIT

# --- 2. FileReplacer replace（按目标版本 yaml 切换 wrapper / settings 指针等） ---
java publish/FileReplacer.java replace "$YAML" || {
  echo "[kbuild] ❌ FileReplacer replace 失败（$YAML）" >&2
  exit 1
}

# --- 3. JDK 选择 ---
# 2.3.10+（Gradle 8.9+ 要求 JDK 17）：macOS 用 java_home 定位，其他平台尊重外部 JAVA_HOME；
# 其余版本尊重外部 JAVA_HOME（Gradle 7.x 同时支持 11/17）
case "$VERSION" in
  2.3.*)
    if command -v /usr/libexec/java_home >/dev/null 2>&1; then
      export JAVA_HOME=$(/usr/libexec/java_home -v 17)
    fi
    ;;
esac
export PATH="${JAVA_HOME:+$JAVA_HOME/bin:}$PATH"

# --- 4. 构建（默认 settings.gradle.kts，指针已被 yaml 切到目标版本） ---
echo "[kbuild] Kotlin $VERSION | $(java -version 2>&1 | /usr/bin/head -1)"
if [ -n "$SETTINGS_OVERRIDE" ]; then
  echo "[kbuild] settings: $SETTINGS_OVERRIDE（变体）"
  ./gradlew -c "$SETTINGS_OVERRIDE" "$@"
else
  echo "[kbuild] settings: settings.gradle.kts（默认，指针已切换）"
  ./gradlew "$@"
fi
STATUS=$?

# --- 5. 共享文件还原（无论构建成败都执行；yarn.lock 由 trap 还原） ---
java publish/FileReplacer.java restore "$YAML"

echo "[kbuild] exit=$STATUS（共享文件已还原）"
exit $STATUS
