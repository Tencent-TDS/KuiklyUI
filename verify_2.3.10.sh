#!/bin/bash
# Kotlin 2.3.10 验证脚本（外网 KuiklyUI）
#
# 说明：本分支默认 Kotlin 版本已升级为 2.3.10，Gradle wrapper 固定 8.9、AGP 8.6.0，
#      因此不再需要 -c settings.2.3.10.*.gradle.kts、FileReplacer 替换或环境变量注入，
#      直接用默认 settings.gradle.kts 编译即可。需要 JDK 17（Gradle 8.9 要求）。
#
# 用法：
#   ./verify_2.3.10.sh framework  只验证框架模块（编译，不含 App）
#   ./verify_2.3.10.sh app        验证 App 产物（Android APK + iOS framework），默认

set -o pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR" || exit 1

MODE="${1:-app}"

if [ "$MODE" = "framework" ]; then
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
  TASKS=":androidApp:assembleDebug :demo:linkPodDebugFrameworkIosArm64"
fi

echo "=== Kotlin 2.3.10 验证开始（默认 settings.gradle.kts）==="
echo "    Kotlin: 2.3.10   AGP: 8.6.0   Gradle: 8.9"
echo ""

./gradlew $TASKS
STATUS=$?

if [ $STATUS -eq 0 ]; then
  echo ""
  echo "=== 验证通过 ==="
else
  echo ""
  echo "=== 验证失败（exit=$STATUS）==="
fi
exit $STATUS
