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
  local candidates=(
    "$HOME/.konan/dependencies/llvm-16.0.0-aarch64-macos-essentials-65/bin/llvm-profdata"
    "$HOME/.konan/dependencies/llvm-19-aarch64-macos-essentials-79/bin/llvm-profdata"
  )
  local c
  for c in "${candidates[@]}"; do
    if [[ -x "$c" ]]; then
      echo "$c"
      return 0
    fi
  done
  # 回退：任意 konan llvm-profdata / xcrun
  local found
  found="$(find "$HOME/.konan/dependencies" -name llvm-profdata -type f 2>/dev/null | head -1 || true)"
  if [[ -n "$found" ]]; then
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

echo "Using: $PROFDATA_BIN"
echo "Input: ${PROFRAW_FILES[*]}"
"$PROFDATA_BIN" merge -output="$OUT_FILE" "${PROFRAW_FILES[@]}"
echo "Wrote: $OUT_FILE ($(du -h "$OUT_FILE" | awk '{print $1}'))"
