#!/usr/bin/env bash
# 将收集到的 .profraw 合并为 demo/llvmProfile/real_ios.profdata
# 优先使用 Kotlin/Native 自带的 llvm-profdata（与 KN LLVM 版本更匹配）。
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
OUT_DIR="$REPO_ROOT/demo/llvmProfile"
OUT_FILE="$OUT_DIR/real_ios.profdata"
PROFRAW_DIR="${1:-$OUT_DIR/profraw}"

mkdir -p "$OUT_DIR" "$PROFRAW_DIR"

shopt -s nullglob
PROFRAW_FILES=("$PROFRAW_DIR"/*.profraw)
if [[ ${#PROFRAW_FILES[@]} -eq 0 ]]; then
  echo "error: 在 $PROFRAW_DIR 下未找到 .profraw 文件。" >&2
  echo "用法: $0 [profraw目录]" >&2
  exit 1
fi

find_llvm_profdata() {
  # 优先：与插装同大版本的 KN LLVM16（glob，避免写死 essentials hash 后缀）
  local llvm16
  llvm16="$(ls -d "$HOME"/.konan/dependencies/llvm-16.0.0-*-macos-*/bin/llvm-profdata 2>/dev/null | sort -V | tail -1 || true)"
  if [[ -n "$llvm16" && -x "$llvm16" ]]; then
    echo "$llvm16"
    return 0
  fi

  # 回退：任意 konan llvm-profdata / PATH / xcrun（可能与 LLVM16 不一致，调用方需 warn）
  local found
  found="$(find "$HOME/.konan/dependencies" -name llvm-profdata -type f 2>/dev/null | sort -V | tail -1 || true)"
  if [[ -n "$found" && -x "$found" ]]; then
    echo "$found"
    return 0
  fi
  if command -v llvm-profdata >/dev/null 2>&1; then
    command -v llvm-profdata
    return 0
  fi
  if xcrun --find llvm-profdata >/dev/null 2>&1; then
    xcrun --find llvm-profdata
    return 0
  fi
  return 1
}

PROFDATA_BIN="$(find_llvm_profdata)" || {
  echo "error: 找不到 llvm-profdata" >&2
  exit 1
}

case "$PROFDATA_BIN" in
  */llvm-16.0.0-*/bin/llvm-profdata) ;;
  *)
    echo "warning: 未命中 KN LLVM16 的 llvm-profdata，当前使用: $PROFDATA_BIN" >&2
    echo "         插装产物为 LLVM16 raw profile；版本不匹配可能导致 merge 失败或静默损坏。" >&2
    echo "         请先执行一次 iOS 构建以下载 ~/.konan/dependencies/llvm-16.0.0-*-macos-*。" >&2
    ;;
esac

echo "Using: $PROFDATA_BIN"
echo "Input: ${PROFRAW_FILES[*]}"
"$PROFDATA_BIN" merge -output="$OUT_FILE" "${PROFRAW_FILES[@]}"
echo "Wrote: $OUT_FILE ($(du -h "$OUT_FILE" | awk '{print $1}'))"
