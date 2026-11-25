#!/usr/bin/env bash
# PGO + Machine Outliner 优化包构建：LLVM_PGO_TYPE=MachineOutliner
# 需要先存在 demo/llvmProfile/real_ios.profdata
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
IOS_APP="$REPO_ROOT/iosApp"
PROFDATA="$REPO_ROOT/demo/llvmProfile/real_ios.profdata"
MODE="device"
CONFIGURATION="${CONFIGURATION:-Release}"

for arg in "$@"; do
  case "$arg" in
    --simulator) MODE="simulator" ;;
    --device) MODE="device" ;;
    --debug) CONFIGURATION="Debug" ;;
    *)
      echo "未知参数: $arg" >&2
      echo "用法: $0 [--device|--simulator] [--debug]" >&2
      exit 1
      ;;
  esac
done

if [[ ! -f "$PROFDATA" ]]; then
  echo "error: 缺少 $PROFDATA" >&2
  echo "请先构建插装包、采集 .profraw，并运行 iosApp/scripts/merge_profraw.sh" >&2
  exit 1
fi

export LLVM_PGO_TYPE=MachineOutliner
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

cd "$REPO_ROOT"
bash ./gradlew :demo:generateDummyFramework --console=plain

cd "$IOS_APP"
# MachineOutliner 模式不引入 LLVMCompileRT
pod install

if [[ "$MODE" == "simulator" ]]; then
  DEST='generic/platform=iOS Simulator'
else
  DEST='generic/platform=iOS'
fi

echo "==> Building iosApp ($CONFIGURATION, $MODE) with LLVM_PGO_TYPE=$LLVM_PGO_TYPE"
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -destination "$DEST" \
  -configuration "$CONFIGURATION" \
  build

echo "BUILD SUCCEEDED (MachineOutliner)。请对比 shared.framework / App 体积并做性能回归。"
