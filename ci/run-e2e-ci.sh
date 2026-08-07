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

APPIUM_PIDFILE="/tmp/appium_publisher.pid"
ENGINE_PID=""

# 递归杀进程树：先杀子进程再杀自己，避免 npm→tsx 孙进程残留（与 demo/run_publisher_local.sh 同步）
kill_tree() {
  local pid=$1 child
  for child in $(pgrep -P "$pid" 2>/dev/null); do kill_tree "$child"; done
  kill "$pid" 2>/dev/null
}

cleanup() {
  [ -n "$ENGINE_PID" ] && kill_tree "$ENGINE_PID"
  # Appium 已脱离脚本进程树（独立 session），按 pidfile 精确回收真实 PID
  if [ -f "$APPIUM_PIDFILE" ]; then
    local apid; apid="$(cat "$APPIUM_PIDFILE" 2>/dev/null)"
    [ -n "$apid" ] && kill_tree "$apid"
  fi
  rm -f "$APPIUM_PIDFILE"
  wait 2>/dev/null
}
trap cleanup EXIT

# 在独立 session 启动 Appium（macOS/Linux 通用；macOS 默认无 setsid）。
# 双 fork + os.setsid()：让 Appium 成为新会话/新进程组的 leader，且被 launchd(PID 1) 收养，
# 彻底脱离 CI agent 的进程树监管 —— agent 在装包空隙（探活后 ~20s）向脚本进程组发 SIGTERM 时，
# 不再能连带杀掉 Appium。脚本仍通过 pidfile 持有真实 PID，退出时由 trap cleanup 精确回收。
start_appium_detached() {
  local port="$1" pidfile="$2" logfile="$3"
  : > "$pidfile"
  local py
  py="$(command -v python3 || command -v python || true)"
  if [ -z "$py" ]; then
    echo "!! 未找到 python3，无法以独立 session 启动 Appium（setsid 等价物）" >&2
    return 1
  fi
  "$py" - "$port" "$pidfile" "$logfile" <<'PY' &
import os, sys, subprocess
port, pidfile, logfile = sys.argv[1], sys.argv[2], sys.argv[3]
if os.fork() > 0:
    sys.exit(0)          # 父进程退出，孙进程被 init/launchd 收养，脱离脚本进程树
os.setsid()              # 新会话 leader，脱离 agent 进程组/控制终端
if os.fork() > 0:
    sys.exit(0)          # 再 fork，确保无控制终端
with open(logfile, "w") as logf:
    p = subprocess.Popen(["appium", "--port", port], stdout=logf, stderr=logf)
    with open(pidfile, "w") as f:
        f.write(str(p.pid))
    p.wait()
PY
  return 0
}

# 3) 启动 Appium
echo "==> [1/4] 启动 Appium (port $APP_PORT) [独立 session，防 agent 误杀]"
start_appium_detached "$APP_PORT" "$APPIUM_PIDFILE" "/tmp/appium_publisher.log" || { echo "!! Appium 启动失败"; exit 1; }

# 4) 启动 E2E 引擎
echo "==> [2/4] 启动 E2E 引擎 (port $ENGINE_PORT) [demo/e2e-engine]"
# 用本地 tsx 绝对路径启动，避免 agent shell 下 npm run 未注入 node_modules/.bin 到 PATH 导致 tsx: command not found
( cd "$ENGINE_DIR" && npm install >/tmp/e2e_engine_install.log 2>&1 )
# ANDROID_UIA2_READ_TIMEOUT_MS：缩短 Appium→uia2 代理读超时（默认 20000），让 uia2 冷启动/重建后
# 假死时的 findElements 在 10s 内快速返回（而非卡满 20s），配合测试脚本多轮短轮询预热即可在 uia2
# 一就绪时命中节点、消除"误判未打开→重开"。下限 8000；仅影响代理读超时，正常 findElements（<1s）不受影响。
export ANDROID_UIA2_READ_TIMEOUT_MS="${ANDROID_UIA2_READ_TIMEOUT_MS:-10000}"
# exec 让 $! 直接捕获 tsx 进程 PID（否则只拿到 subshell PID，cleanup 杀不到真正的引擎孙进程，
# 多次运行会残留孤儿占着 7900 端口，导致 CI 偶发"引擎未就绪"）。
( cd "$ENGINE_DIR" && exec env ANDROID_UIA2_READ_TIMEOUT_MS="$ANDROID_UIA2_READ_TIMEOUT_MS" "$ENGINE_DIR/node_modules/.bin/tsx" src/server.ts >/tmp/e2e_engine_publisher.log 2>&1 ) &
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
