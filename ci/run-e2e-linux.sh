#!/usr/bin/env bash
#
# GitHub Actions 跨平台 E2E 启动器（Linux ubuntu 模拟器 / macOS self-hosted 真机通用）
#
# 与 ci/run-e2e-ci.sh 同源，但针对 GitHub 做了以下适配：
#   1. 不写死本机路径：node 由 actions/setup-node 注入 PATH；Android SDK 用 runner 自带 $ANDROID_HOME
#   2. Appium server + uiautomator2 driver 在干净 runner 上现装（装到 RUNNER_TEMP，避免全局权限问题）
#   3. Appium / 引擎日志 tail -f 进 step stdout —— GitHub job log 可观测，AI 用 gh 直读排障
#      （绝不把 /tmp 当唯一日志落点；工蜂 7 轮根因之一就是本机 /tmp AI 读不到）
#   4. 仍用 python os.setsid 双 fork 脱离进程组（macOS 无 setsid 命令；Linux 同样适用）
#
# 复用：demo/mention_publisher_e2e_test.sh（测试逻辑）、demo/e2e-engine/（HTTP→Appium 桥）
#
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_ROOT"

APP_PORT=4723
ENGINE_PORT="${MOBILE_TEST_PORT:-7900}"
ENGINE_DIR="$REPO_ROOT/demo/e2e-engine"
TEST_SCRIPT="$REPO_ROOT/demo/mention_publisher_e2e_test.sh"

INSTALL=0
for a in "$@"; do
  [ "$a" = "--install" ] && INSTALL=1
done

# ---- 依赖 PATH ----
# Android SDK：runner 自带 $ANDROID_HOME（ubuntu 与 macOS self-hosted 均设）；补 platform-tools 到 PATH
if [ -n "${ANDROID_HOME:-}" ] && [ -d "$ANDROID_HOME/platform-tools" ]; then
  export PATH="$ANDROID_HOME/platform-tools:$PATH"
elif [ -n "${ANDROID_SDK_ROOT:-}" ] && [ -d "$ANDROID_SDK_ROOT/platform-tools" ]; then
  export PATH="$ANDROID_SDK_ROOT/platform-tools:$PATH"
fi
# node/tsx：CI 由 actions/setup-node 注入；本机兜底到 workbuddy 托管 node
if ! command -v node >/dev/null 2>&1; then
  MANAGED_NODE_BIN="$HOME/.workbuddy/binaries/node/versions/22.22.2/bin"
  [ -d "$MANAGED_NODE_BIN" ] && export PATH="$MANAGED_NODE_BIN:$PATH"
fi

# ---- 装 Appium（干净 runner 无全局 appium）----
# 装到 RUNNER_TEMP（GitHub 托管临时目录，macOS self-hosted 退化为 /tmp），hermetic、不碰全局 prefix
APPIUM_DIR="${RUNNER_TEMP:-/tmp}/gh-e2e-appium"
if [ ! -x "$APPIUM_DIR/node_modules/.bin/appium" ]; then
  echo "==> 安装 Appium 到 $APPIUM_DIR ..."
  mkdir -p "$APPIUM_DIR"
  ( cd "$APPIUM_DIR" && npm init -y >/dev/null 2>&1 && npm install appium >/tmp/appium_install.log 2>&1 ) \
    || { echo "!! Appium 安装失败"; tail -20 /tmp/appium_install.log; exit 1; }
fi
APPIUM_BIN="$APPIUM_DIR/node_modules/.bin/appium"
echo "==> appium: $APPIUM_BIN"

# Appium 2.x 需显式装 uiautomator2 driver（已装则 apium 提示已安装并退出 0）
"$APPIUM_BIN" driver install uiautomator2 || echo "!! uiautomator2 driver 安装提示（见上），继续"

# ---- 校验关键依赖 ----
missing=0
for bin in adb node npm; do
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "!! 缺少依赖: $bin"; missing=1
  else
    echo "   ✓ $bin -> $(command -v $bin)"
  fi
done
[ "$missing" -eq 1 ] && { echo "环境注入失败，退出"; exit 1; }

# ---- 设备 serial ----
# 模拟器：reactivecircus/android-emulator-runner 设 ANDROID_SERIAL；self-hosted 真机：传 DEVICE_SERIAL env
DEVICE_SERIAL="${DEVICE_SERIAL:-${ANDROID_SERIAL:-}}"
if [ -z "$DEVICE_SERIAL" ]; then
  DEVICE_SERIAL="$(adb devices 2>/dev/null | awk 'NF && $2=="device"{print $1; exit}')"
fi
[ -z "$DEVICE_SERIAL" ] && { echo "!! 未检测到 Android 设备（模拟器未起 / 真机未连）"; exit 1; }
echo "==> 目标设备: $DEVICE_SERIAL"
export DEVICE_SERIAL

APPIUM_PIDFILE="/tmp/appium_publisher_gh.pid"
ENGINE_PID=""

kill_tree() {
  local pid=$1 child
  for child in $(pgrep -P "$pid" 2>/dev/null); do kill_tree "$child"; done
  kill "$pid" 2>/dev/null
}
cleanup() {
  # 停 tail 流式日志（保 stdout 可读）
  pkill -f "tail -f /tmp/appium_publisher_gh.log" 2>/dev/null || true
  pkill -f "tail -f /tmp/e2e_engine_publisher_gh.log" 2>/dev/null || true
  [ -n "$ENGINE_PID" ] && kill_tree "$ENGINE_PID"
  if [ -f "$APPIUM_PIDFILE" ]; then
    local apid; apid="$(cat "$APPIUM_PIDFILE" 2>/dev/null)"
    [ -n "$apid" ] && kill_tree "$apid"
  fi
  rm -f "$APPIUM_PIDFILE"
  wait 2>/dev/null
}
trap cleanup EXIT

# 在独立 session 启动 Appium（跨平台：macOS 无 setsid 命令，用 python os.setsid 双 fork）
start_appium_detached() {
  local appium_bin="$1" port="$2" pidfile="$3" logfile="$4"
  : > "$pidfile"
  local py
  py="$(command -v python3 || command -v python || true)"
  if [ -z "$py" ]; then
    echo "!! 未找到 python3，无法以独立 session 启动 Appium（setsid 等价物）" >&2
    return 1
  fi
  "$py" - "$appium_bin" "$port" "$pidfile" "$logfile" <<'PY' &
import os, sys, subprocess
appium_bin, port, pidfile, logfile = sys.argv[1], sys.argv[2], sys.argv[3], sys.argv[4]
if os.fork() > 0:
    sys.exit(0)          # 父进程退出，孙进程被 init/launchd 收养，脱离脚本进程树
os.setsid()              # 新会话 leader，脱离 runner 进程组/控制终端
if os.fork() > 0:
    sys.exit(0)          # 再 fork，确保无控制终端
with open(logfile, "w") as logf:
    p = subprocess.Popen([appium_bin, "--port", port], stdout=logf, stderr=logf)
    with open(pidfile, "w") as f:
        f.write(str(p.pid))
    p.wait()
PY
  return 0
}

# 3) 启动 Appium（脱离进程组，免疫 runner 在 installDebug 间隙发 SIGTERM —— 工蜂 7 轮真因）
echo "==> [1/4] 启动 Appium (port $APP_PORT) [独立 session，防 runner SIGTERM]"
start_appium_detached "$APPIUM_BIN" "$APP_PORT" "$APPIUM_PIDFILE" "/tmp/appium_publisher_gh.log" || { echo "!! Appium 启动失败"; exit 1; }
tail -f /tmp/appium_publisher_gh.log >&2 &   # 流式日志进 step stdout（保 AI 可观测）

# 4) 装引擎依赖 + 启动 E2E 引擎
echo "==> [2/4] 启动 E2E 引擎 (port $ENGINE_PORT) [demo/e2e-engine]"
( cd "$ENGINE_DIR" && npm install >/tmp/e2e_engine_install.log 2>&1 ) \
  || { echo "!! 引擎依赖安装失败"; tail -20 /tmp/e2e_engine_install.log; exit 1; }
export ANDROID_UIA2_READ_TIMEOUT_MS="${ANDROID_UIA2_READ_TIMEOUT_MS:-10000}"
( cd "$ENGINE_DIR" && ANDROID_UIA2_READ_TIMEOUT_MS="$ANDROID_UIA2_READ_TIMEOUT_MS" "$ENGINE_DIR/node_modules/.bin/tsx" src/server.ts >/tmp/e2e_engine_publisher_gh.log 2>&1 ) &
ENGINE_PID=$!
tail -f /tmp/e2e_engine_publisher_gh.log >&2 &   # 流式日志进 step stdout

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
[ "$appium_ready" -ne 1 ] && { echo "!! Appium 未就绪，详见日志"; exit 1; }
[ "$engine_ready" -ne 1 ] && { echo "!! E2E 引擎未就绪，详见日志"; exit 1; }

# 5) 编译安装 demo APK
if [ "$INSTALL" -eq 1 ]; then
  echo "==> [3/4] 安装 demo APK (./gradlew :androidApp:installDebug)"
  ./gradlew :androidApp:installDebug || { echo "!! 装包失败"; exit 1; }
fi

# 6) 跑测试
RUN_STEP=$((INSTALL == 1 ? 4 : 3))
echo "==> [$RUN_STEP/4] 运行回归: $TEST_SCRIPT"
bash "$TEST_SCRIPT"
RC=$?
if [ "$RC" -eq 0 ]; then
  echo "==> 结果: 全部通过 (exit 0)"
else
  echo "==> 结果: 存在失败 (exit $RC)"
fi
exit "$RC"
