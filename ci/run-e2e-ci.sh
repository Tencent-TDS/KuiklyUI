#!/usr/bin/env bash
#
# CI 专用 E2E 启动器（适配工蜂私有 Runner 干净环境）。
# 与 demo/run_publisher_local.sh 同源，但：
#   - 不依赖交互式 PATH，先 source ci/setup-env.sh
#   - 默认 --install（CI 每次拉新代码，需重新编译安装 demo APK）
#   - DEVICE_SERIAL 默认取已连真机；CI 变量可覆盖
#
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# 1) 注入 CI 环境 PATH
source "$REPO_ROOT/ci/setup-env.sh" || exit 1

APP_PORT=4723
ENGINE_PORT="${MOBILE_TEST_PORT:-7900}"
ENGINE_DIR="$REPO_ROOT/demo/e2e-engine"
TEST_SCRIPT="$REPO_ROOT/demo/mention_publisher_e2e_test.sh"

# 2) 确认设备已连（CI 必须显式校验，避免静默空跑）
DEVICE_SERIAL="${DEVICE_SERIAL:-$(adb devices 2>/dev/null | awk 'NF && $2=="device"{print $1; exit}')}"
[ -z "$DEVICE_SERIAL" ] && { echo "!! 未检测到 Android 设备，E2E 无法运行"; exit 1; }
echo "==> 目标设备: $DEVICE_SERIAL"
export DEVICE_SERIAL

APPIUM_PID=""
ENGINE_PID=""

cleanup() {
  [ -n "$ENGINE_PID" ] && kill "$ENGINE_PID" 2>/dev/null
  [ -n "$APPIUM_PID" ] && kill "$APPIUM_PID" 2>/dev/null
  wait 2>/dev/null
}
trap cleanup EXIT

# 3) 启动 Appium
echo "==> [1/4] 启动 Appium (port $APP_PORT)"
appium --port "$APP_PORT" >/tmp/appium_publisher.log 2>&1 &
APPIUM_PID=$!

# 4) 启动 E2E 引擎
echo "==> [2/4] 启动 E2E 引擎 (port $ENGINE_PORT) [demo/e2e-engine]"
( cd "$ENGINE_DIR" && npm run server >/tmp/e2e_engine_publisher.log 2>&1 ) &
ENGINE_PID=$!

echo "==> 等待服务就绪 ..."
appium_ready=0
engine_ready=0
for i in $(seq 1 60); do
  if [ "$appium_ready" -eq 0 ] && curl -s "http://localhost:$APP_PORT/status" >/dev/null 2>&1; then
    appium_ready=1; echo "    Appium 就绪"
  fi
  if [ "$engine_ready" -eq 0 ] && curl -s "http://localhost:$ENGINE_PORT/status" >/dev/null 2>&1; then
    engine_ready=1; echo "    E2E 引擎就绪"
  fi
  [ "$appium_ready" -eq 1 ] && [ "$engine_ready" -eq 1 ] && break
  sleep 0.5
done
[ "$appium_ready" -ne 1 ] && { echo "!! Appium 未就绪，详见 /tmp/appium_publisher.log"; tail -20 /tmp/appium_publisher.log; exit 1; }
[ "$engine_ready" -ne 1 ] && { echo "!! E2E 引擎未就绪，详见 /tmp/e2e_engine_publisher.log"; tail -20 /tmp/e2e_engine_publisher.log; exit 1; }

# 5) 编译安装 demo APK
echo "==> [3/4] 安装 demo APK (./gradlew :androidApp:installDebug)"
./gradlew :androidApp:installDebug || { echo "!! 装包失败"; exit 1; }

# 6) 跑测试
echo "==> [4/4] 运行回归: $TEST_SCRIPT"
bash "$TEST_SCRIPT"
RC=$?
if [ "$RC" -eq 0 ]; then
  echo "==> 结果: 全部通过 (exit 0)"
else
  echo "==> 结果: 存在失败 (exit $RC)"
fi
exit "$RC"
