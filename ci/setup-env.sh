#!/usr/bin/env bash
#
# CI 环境 PATH 注入（必须在每个 CI job 的 script 最先 source 本文件）。
# 原因：私有 Runner 的干净 login shell 不会自动加载 Android SDK / workbuddy node 的 PATH，
#       而这些是跑 E2E 的硬依赖（adb / appium / node / tsx）。
#
# 用法： source ci/setup-env.sh
#
set -u

# ---- Android SDK (adb / sdkmanager) ----
if [ -d "/Users/zhaozining/Library/Android/sdk" ]; then
  export ANDROID_HOME="/Users/zhaozining/Library/Android/sdk"
  export ANDROID_SDK_ROOT="$ANDROID_HOME"
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
fi

# ---- workbuddy 托管的 node 22.22.2 (自带 appium / npx / tsx) ----
MANAGED_NODE_BIN="/Users/zhaozining/.workbuddy/binaries/node/versions/22.22.2/bin"
if [ -d "$MANAGED_NODE_BIN" ]; then
  export PATH="$MANAGED_NODE_BIN:$PATH"
fi

# ---- 校验关键依赖 ----
missing=0
for bin in adb appium node npm; do
  if ! command -v "$bin" >/dev/null 2>&1; then
    echo "!! 缺少依赖: $bin (PATH 未正确注入？)"
    missing=1
  else
    echo "   ✓ $bin -> $(command -v $bin)"
  fi
done
[ "$missing" -eq 1 ] && { echo "环境注入失败，退出"; exit 1; }
echo "✓ CI 环境 PATH 注入完成"
