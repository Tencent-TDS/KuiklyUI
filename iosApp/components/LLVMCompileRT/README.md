# LLVMCompileRT

LLVM Clang Profile Runtime 静态库，仅用于 `LLVM_PGO_TYPE=LLVMPGO` 插装包。产物：

- `libclang_rt.profile_ios.a`（真机）
- `libclang_rt.profile_iossim.a`（模拟器）

**必须与 Kotlin/Native 插装所用的 LLVM 大版本一致**（本仓库 demo 对应 KN 自带的 LLVM16）。不要直接使用本机 Xcode（clang17+）自带的 `libclang_rt.profile_*.a`，否则会因 ABI / raw profile version 不匹配导致运行崩溃或 `llvm-profdata merge` 失败。

准备方式（优先顺序）：

1. **优先使用工具链自带库**：若 Kotlin/Native / 同版本 LLVM 工具链已附带 `libclang_rt.profile_ios*.a`，直接拷到本目录。
2. **否则按对应版本从源码编译**：用与插装同版本的 clang，从同版本 compiler-rt 源码编出完整 profile runtime：

```bash
bash iosApp/scripts/setup_llvm_compile_rt.sh
```

该脚本使用 KN 自带的 LLVM16 clang，从 compiler-rt 16.0.0 源码编译到本目录。

详见 [machine-outliner-pgo-guide.md](../../../docs/DevGuide/machine-outliner-pgo-guide.md)。
