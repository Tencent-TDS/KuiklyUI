#!/usr/bin/env bash
# PGO 插装包构建：LLVM_PGO_TYPE=LLVMPGO
# 默认打模拟器 Debug；真机请传 --device。
# 通过 SWIFT_ACTIVE_COMPILATION_CONDITIONS=LLVM_PGO_INSTRUMENT 打开 App 侧落盘逻辑；
# 普通 / MachineOutliner 包不传该宏，不会挂定时器。
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
IOS_APP="$REPO_ROOT/iosApp"
MODE="simulator"
CONFIGURATION="${CONFIGURATION:-Debug}"

for arg in "$@"; do
  case "$arg" in
    --device) MODE="device" ;;
    --release) CONFIGURATION="Release" ;;
    --simulator) MODE="simulator" ;;
    *)
      echo "未知参数: $arg" >&2
      echo "用法: $0 [--simulator|--device] [--release]" >&2
      exit 1
      ;;
  esac
done

bash "$IOS_APP/scripts/setup_llvm_compile_rt.sh"

export LLVM_PGO_TYPE=LLVMPGO
export LANG=en_US.UTF-8
export LC_ALL=en_US.UTF-8

cd "$REPO_ROOT"
bash ./gradlew :demo:generateDummyFramework --console=plain

cd "$IOS_APP"
pod install

if [[ "$MODE" == "simulator" ]]; then
  # generic/platform=iOS Simulator 可能编出含 x86_64 的 universal；profile runtime 已 lipo 成 fat。
  # 仍显式指定 ARCHS=arm64，避免在 Apple Silicon 上无谓编 x86_64。
  DEST='generic/platform=iOS Simulator'
  ARCHS_OVERRIDE=(ARCHS=arm64 ONLY_ACTIVE_ARCH=NO)
else
  DEST='generic/platform=iOS'
  ARCHS_OVERRIDE=(ARCHS=arm64)
fi

echo "==> Building iosApp ($CONFIGURATION, $MODE) with LLVM_PGO_TYPE=$LLVM_PGO_TYPE"
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -destination "$DEST" \
  -configuration "$CONFIGURATION" \
  "${ARCHS_OVERRIDE[@]}" \
  SWIFT_ACTIVE_COMPILATION_CONDITIONS='$(inherited) LLVM_PGO_INSTRUMENT' \
  GCC_PREPROCESSOR_DEFINITIONS='$(inherited) LLVM_PGO_INSTRUMENT=1' \
  build

echo "BUILD SUCCEEDED (instrumented). 运行 App 执行核心业务后，从沙盒导出 .profraw，再执行 merge_profraw.sh"
