#!/usr/bin/env bash
#
# 发布器 E2E 一键本地启动器（非 AI 驱动）
# 职责：拉起 Appium + E2E 引擎(env-as-code) → 可选装包 → 跑回归脚本 → 收尾。
# 不依赖任何 AI 助手 Skill，引擎来自仓库内置 demo/e2e-engine/。
#
# 用法：
#   bash demo/run_publisher_local.sh              # 起服务 + 跑测试（假设 demo 已装好）
#   bash demo/run_publisher_local.sh --install    # 额外先 ./gradlew :androidApp:installDebug
#   DEVICE_SERIAL=15490798770018J bash demo/run_publisher_local.sh   # 指定设备
#
set -u

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

# ---- 让 appium / npm / tsx / adb 可用（无需用户预先配 PATH）----
MANAGED_NODE_BIN="/Users/zhaozining/.workbuddy/binaries/node/versions/22.22.2/bin"
export PATH="$MANAGED_NODE_BIN:$PATH"
if [ -d "/Users/zhaozining/Library/Android/sdk/platform-tools" ]; then
  export PATH="/Users/zhaozining/Library/Android/sdk/platform-tools:$PATH"
fi

APP_PORT=4723
ENGINE_PORT="${MOBILE_TEST_PORT:-7900}"
ENGINE_DIR="$REPO_ROOT/demo/e2e-engine"
TEST_SCRIPT="$REPO_ROOT/demo/mention_publisher_e2e_test.sh"

INSTALL=0
for a in "$@"; do
  [ "$a" = "--install" ] && INSTALL=1
done

APPIUM_PID=""
ENGINE_PID=""

cleanup() {
  [ -n "$ENGINE_PID" ] && kill "$ENGINE_PID" 2>/dev/null
  [ -n "$APPIUM_PID" ] && kill "$APPIUM_PID" 2>/dev/null
  wait 2>/dev/null
}
trap cleanup EXIT

echo "==> [1/4] 启动 Appium (port $APP_PORT)"
appium --port "$APP_PORT" >/tmp/appium_publisher.log 2>&1 &
APPIUM_PID=$!

echo "==> [2/4] 启动 E2E 引擎 (port $ENGINE_PORT)  [demo/e2e-engine]"
( cd "$ENGINE_DIR" && npm run server >/tmp/e2e_engine_publisher.log 2>&1 ) &
ENGINE_PID=$!

# 等两个服务就绪
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
if [ "$appium_ready" -ne 1 ]; then
  echo "!! Appium 未就绪，详见 /tmp/appium_publisher.log"; exit 1
fi
if [ "$engine_ready" -ne 1 ]; then
  echo "!! E2E 引擎未就绪，详见 /tmp/e2e_engine_publisher.log"; exit 1
fi

# 可选：装包
if [ "$INSTALL" -eq 1 ]; then
  echo "==> [3/4] 安装 demo APK (./gradlew :androidApp:installDebug)"
  ./gradlew :androidApp:installDebug || { echo "!! 装包失败"; exit 1; }
fi

# 跑测试
RUN_STEP=$((INSTALL == 1 ? 4 : 3))
echo "==> [$RUN_STEP/4] 运行回归: $TEST_SCRIPT"
if [ -n "${DEVICE_SERIAL:-}" ]; then
  echo "    设备: $DEVICE_SERIAL"
fi
bash "$TEST_SCRIPT"
RC=$?

if [ "$RC" -eq 0 ]; then
  echo "==> 结果: 全部通过 (exit 0)"
else
  echo "==> 结果: 存在失败 (exit $RC)"
fi
exit "$RC"
