#!/usr/bin/env bash
# 一键编译 + 运行 ScheduleDeallocRenderValues 压测
#
# 用法:
#   ./run_stress.sh                # 默认 release 构建并运行
#   ./run_stress.sh tsan           # TSAN 构建并运行 (检查数据竞争)
#   ./run_stress.sh release 16 200000
#
# 依赖: clang++ (macOS 自带; Linux 请 apt install clang)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$SCRIPT_DIR/stress_schedule_dealloc_render_values.cpp"
OUT_DIR="$SCRIPT_DIR/build"
mkdir -p "$OUT_DIR"

MODE="${1:-release}"
shift || true

case "$MODE" in
    release)
        BIN="$OUT_DIR/stress_sdrv"
        echo ">>> [Release] 编译 $BIN"
        clang++ -std=c++17 -O2 -Wall -Wextra -pthread "$SRC" -o "$BIN"
        ;;
    tsan)
        BIN="$OUT_DIR/stress_sdrv_tsan"
        echo ">>> [TSAN] 编译 $BIN"
        clang++ -std=c++17 -O1 -g -Wall -Wextra -pthread \
            -fsanitize=thread "$SRC" -o "$BIN"
        ;;
    asan)
        BIN="$OUT_DIR/stress_sdrv_asan"
        echo ">>> [ASAN] 编译 $BIN"
        clang++ -std=c++17 -O1 -g -Wall -Wextra -pthread \
            -fsanitize=address,undefined "$SRC" -o "$BIN"
        ;;
    *)
        echo "未知模式: $MODE (release|tsan|asan)"
        exit 2
        ;;
esac

echo ">>> 运行 $BIN $*"
"$BIN" "$@"
