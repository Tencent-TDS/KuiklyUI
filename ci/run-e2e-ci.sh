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
# 单变量验证：CI 冷环境下首次 uia2 页面树读取是否仅超过默认 20s。
# 只影响本 CI E2E 进程；本地启动器与测试逻辑均不改。若 CI 仍失败，说明不是超时余量问题。
export ANDROID_UIA2_READ_TIMEOUT_MS="${ANDROID_UIA2_READ_TIMEOUT_MS:-60000}"
echo "==> uia2 页面树读取超时: ${ANDROID_UIA2_READ_TIMEOUT_MS}ms（CI 单变量实验）"
ENGINE_DIR="$REPO_ROOT/demo/e2e-engine"
TEST_SCRIPT="$REPO_ROOT/demo/mention_publisher_e2e_test.sh"

# 2) 确认设备已连（CI 必须显式校验，避免静默空跑）
DEVICE_SERIAL="${DEVICE_SERIAL:-$(adb devices 2>/dev/null | awk 'NF && $2=="device"{print $1; exit}')}"
[ -z "$DEVICE_SERIAL" ] && { echo "!! 未检测到 Android 设备，E2E 无法运行"; exit 1; }
echo "==> 目标设备: $DEVICE_SERIAL"
export DEVICE_SERIAL

APPIUM_PID=""
ENGINE_PID=""

# 递归杀进程树：先杀子进程再杀自己，避免 npm→tsx 孙进程残留（与 demo/run_publisher_local.sh 同步）
kill_tree() {
  local pid=$1 child
  for child in $(pgrep -P "$pid" 2>/dev/null); do kill_tree "$child"; done
  kill "$pid" 2>/dev/null
}

cleanup() {
  [ -n "$ENGINE_PID" ] && kill_tree "$ENGINE_PID"
  [ -n "$APPIUM_PID" ] && kill_tree "$APPIUM_PID"
  wait 2>/dev/null
}
trap cleanup EXIT

# 3) 启动 Appium
echo "==> [1/4] 启动 Appium (port $APP_PORT)"
appium --port "$APP_PORT" >/tmp/appium_publisher.log 2>&1 &
APPIUM_PID=$!

# 4) 启动 E2E 引擎
echo "==> [2/4] 启动 E2E 引擎 (port $ENGINE_PORT) [demo/e2e-engine]"
# 用本地 tsx 绝对路径启动，避免 agent shell 下 npm run 未注入 node_modules/.bin 到 PATH 导致 tsx: command not found
( cd "$ENGINE_DIR" && npm install >/tmp/e2e_engine_install.log 2>&1 )
( cd "$ENGINE_DIR" && "$ENGINE_DIR/node_modules/.bin/tsx" src/server.ts >/tmp/e2e_engine_publisher.log 2>&1 ) &
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
