#!/usr/bin/env bash
# 用 Kotlin/Native 自带的 LLVM16 clang 从 compiler-rt 16.0.0 源码编译完整的
# profile runtime（libclang_rt.profile_ios*.a）。
#
# 为什么不直接拷 Xcode 的 libclang_rt.profile_*.a：
#   Xcode(clang17) 的 profile runtime 与 KN(LLVM16) 插装生成的 __llvm_prf_* ABI
#   不一致（value-profiling 结构、raw profile version=8 vs 10），运行/合并都会出错。
#   因此这里用与插装同版本的 LLVM16 源码构建，保证 ABI 完全匹配。
#
# 说明：插装侧已用 `-mllvm -disable-vp` 关闭 value profiling，但 runtime 仍需保留
#   InstrProfilingValue.c 以解析 lprofGetVPDataReader / lprofSetupValueProfiler 等符号，
#   否则 InstrProfilingFile.c / InstrProfilingMergeFile.c 链接会缺符号。
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
DEST="$ROOT/components/LLVMCompileRT"
mkdir -p "$DEST"

# --- 1. 定位 KN LLVM16 工具链 ---
KONAN_LLVM="$(ls -d "$HOME"/.konan/dependencies/llvm-16.0.0-*-macos-*/ 2>/dev/null | sort -V | tail -1 || true)"
if [[ -z "${KONAN_LLVM}" || ! -x "${KONAN_LLVM}bin/clang" ]]; then
  echo "error: 未找到 KN LLVM16 clang（~/.konan/dependencies/llvm-16.0.0-*-macos-*）。" >&2
  echo "       请先执行一次 iOS 构建以下载 Kotlin/Native LLVM 依赖。" >&2
  exit 1
fi
CLANG="${KONAN_LLVM}bin/clang"
AR="${KONAN_LLVM}bin/llvm-ar"
RANLIB="${KONAN_LLVM}bin/llvm-ranlib"
[[ -x "$AR" ]] || AR="ar"
[[ -x "$RANLIB" ]] || RANLIB="ranlib"
echo "Using clang: $CLANG"

# --- 2. 定位/获取 compiler-rt 16.0.0 源码 ---
CRT_SRC="${COMPILER_RT_SRC:-/tmp/compiler-rt-16.0.0.src}"
if [[ ! -d "$CRT_SRC/lib/profile" ]]; then
  echo "compiler-rt 源码不存在，尝试下载 llvmorg-16.0.0 ..."
  TARBALL="/tmp/compiler-rt-16.0.0.src.tar.xz"
  URL="https://github.com/llvm/llvm-project/releases/download/llvmorg-16.0.0/compiler-rt-16.0.0.src.tar.xz"
  curl -fL "$URL" -o "$TARBALL"
  tar -C /tmp -xf "$TARBALL"
  CRT_SRC="/tmp/compiler-rt-16.0.0.src"
fi
if [[ ! -d "$CRT_SRC/lib/profile" ]]; then
  echo "error: 找不到 compiler-rt profile 源码：$CRT_SRC/lib/profile" >&2
  exit 1
fi
echo "Using compiler-rt src: $CRT_SRC"

PROF="$CRT_SRC/lib/profile"
INC="$CRT_SRC/include"

# Darwin 目标需要的 profile 源文件（排除 Linux/Fuchsia/Windows/Other 与 gcov）。
SOURCES=(
  InstrProfiling.c
  InstrProfilingBuffer.c
  InstrProfilingFile.c
  InstrProfilingInternal.c
  InstrProfilingMerge.c
  InstrProfilingMergeFile.c
  InstrProfilingNameVar.c
  InstrProfilingPlatformDarwin.c
  InstrProfilingUtil.c
  InstrProfilingValue.c
  InstrProfilingVersionVar.c
  InstrProfilingWriter.c
)

build_lib() {
  local out="$1"; shift
  local target="$1"; shift
  local sdk="$1"; shift
  local work; work="$(mktemp -d)"
  echo "==> Building $(basename "$out")  target=$target"
  for src in "${SOURCES[@]}"; do
    # 这些 feature 宏正常由 compiler-rt CMake 自动探测；手动编译需显式打开，
    # 否则 lprofGetHostName / lprofLockFd 等 Darwin 可用实现不会被编入，链接缺符号。
    "$CLANG" -c -O2 -fPIC \
      -target "$target" -isysroot "$sdk" \
      -I"$INC" -I"$PROF" \
      -DCOMPILER_RT_HAS_ATOMICS=1 \
      -DCOMPILER_RT_HAS_UNAME=1 \
      -DCOMPILER_RT_HAS_FCNTL_LCK=1 \
      "$PROF/$src" -o "$work/${src%.c}.o"
  done
  rm -f "$out"
  "$AR" cru "$out" "$work"/*.o
  "$RANLIB" "$out"
  rm -rf "$work"
}

SIM_SDK="$(xcrun --sdk iphonesimulator --show-sdk-path)"
IOS_SDK="$(xcrun --sdk iphoneos --show-sdk-path)"

# iossim 需要 arm64 + x86_64 双 slice：demo/build.gradle.kts 覆盖了 ios_simulator_arm64
# 与 ios_x64；Intel Mac 上 --simulator 会链 x86_64，单 arm64 库会报 symbol not found。
SIM_ARM64_A="$DEST/libclang_rt.profile_iossim_arm64.a"
SIM_X64_A="$DEST/libclang_rt.profile_iossim_x86_64.a"
build_lib "$SIM_ARM64_A" "arm64-apple-ios14.1-simulator" "$SIM_SDK"
build_lib "$SIM_X64_A"   "x86_64-apple-ios14.1-simulator" "$SIM_SDK"
rm -f "$DEST/libclang_rt.profile_iossim.a"
lipo -create "$SIM_ARM64_A" "$SIM_X64_A" -output "$DEST/libclang_rt.profile_iossim.a"
rm -f "$SIM_ARM64_A" "$SIM_X64_A"

build_lib "$DEST/libclang_rt.profile_ios.a" "arm64-apple-ios14.1" "$IOS_SDK"

ls -lh "$DEST"/libclang_rt.profile_*.a
echo "Done. LLVMCompileRT（LLVM16 完整 profile runtime，iossim 为 arm64+x86_64 fat）已就绪: $DEST"
