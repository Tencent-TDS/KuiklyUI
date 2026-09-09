#!/bin/bash
# Kotlin 2.3.10 分支 —— 本地一键验证脚本（外网 KuiklyUI）
#
# 用法：
#   ./verify_2.3.10.sh framework   只验证框架模块（编译，不含 App）
#   ./verify_2.3.10.sh app         验证 App 产物（Android APK + iOS framework），默认
#
# 说明：
#   1) 2.3.10 与 2.1.21 完全并行：使用独立的 build.2.3.10*.gradle.kts /
#      settings.2.3.10*.gradle.kts，默认分支仍是 2.1.21，互不影响。
#   2) Gradle 版本、gradle.properties、yarn.lock、podspec 版本由
#      publish/compatible/2.3.10.yaml 临时切换（沿用 2.1.21 以来既有模式），
#      脚本结束（无论成功或失败）自动还原，不会污染工作区。
#   3) 需要 JDK 17（AGP 8.6 要求）。

set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

CONFIG="publish/compatible/2.3.10.yaml"

restore() {
  java publish/FileReplacer.java restore "$CONFIG" > /dev/null 2>&1
  echo ""
  echo "[已还原] $CONFIG 涉及的文件（Gradle wrapper / gradle.properties / podspec / yarn.lock）"
}
trap restore EXIT

java publish/FileReplacer.java replace "$CONFIG"

export KUIKLY_AGP_VERSION=8.6.0
export KUIKLY_KOTLIN_VERSION=2.3.10

MODE="${1:-app}"

if [ "$MODE" = "framework" ]; then
  SETTINGS="settings.2.3.10.gradle.kts"
  TASKS=":core:compileDebugKotlinAndroid :core:compileKotlinJs \
:compose:compileDebugKotlinAndroid :compose:compileKotlinJs \
:core-annotations:compileDebugKotlinAndroid \
:core-ksp:compileKotlin \
:core-wx:compileDebugKotlinAndroid :core-wx:compileKotlinJs \
:core-render-android:compileDebugKotlin \
:core-render-web:base:compileKotlinJs \
:core-render-web:h5:compileKotlinJs \
:core-render-web:miniapp:compileKotlinJs"
else
  # App 侧入口：settings.2.3.10.app.gradle.kts 包含 androidApp / demo / h5App / miniApp
  SETTINGS="settings.2.3.10.app.gradle.kts"
  TASKS=":androidApp:assembleDebug :demo:linkPodDebugFrameworkIosArm64"
fi

echo "=== Kotlin 2.3.10 验证开始（$SETTINGS）==="
echo "    Kotlin: $KUIKLY_KOTLIN_VERSION   AGP: $KUIKLY_AGP_VERSION"
echo ""

./gradlew -c "$SETTINGS" $TASKS
STATUS=$?

if [ $STATUS -eq 0 ]; then
  echo ""
  echo "=== 验证通过 ==="
else
  echo ""
  echo "=== 验证失败（exit=$STATUS）==="
fi
exit $STATUS
