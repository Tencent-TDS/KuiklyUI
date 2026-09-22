#!/bin/bash
# Kotlin 2.3.10 验证
#
# 说明：本分支默认构建（./gradlew）是 Kotlin 2.1.21；本脚本通过 ./k2310.sh
#      显式启用 2.3.10（JDK 17 + 外部 Gradle 8.9 + AGP 8.6.0 注入 +
#      settings.2.3.10.app），不修改 wrapper / gradle.properties 等共享文件，
#      用完即走。
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

echo "=== Kotlin 2.3.10 验证开始（./k2310.sh → settings.2.3.10.app.gradle.kts）==="
echo "    Kotlin: 2.3.10   AGP: 8.6.0   Gradle: 8.9   JDK: 17"
echo ""

./kbuild.sh 2.3.10 $TASKS
STATUS=$?

if [ $STATUS -eq 0 ]; then
  echo ""
  echo "=== 验证通过 ==="
else
  echo ""
  echo "=== 验证失败（exit=$STATUS）==="
fi
exit $STATUS
