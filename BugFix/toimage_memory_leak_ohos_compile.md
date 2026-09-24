# FIX_COMPILE_VERIFY（Bug修复阶段 · 2026-09-22）

> 用 ohpm/OHOS NDK 工具链对修复后的 `KRSnapshotManager.cpp` 做语法编译校验。

## 环境

- ohpm 6.1.2 / hvigorw 6.24.2（位于 `/usr/bin/ohos_tools/command-line-tools/bin`）
- OHOS NDK：aarch64-unknown-linux-ohos clang++（llvm-15）
- sysroot：`/usr/bin/ohos_tools/command-line-tools/sdk/default/openharmony/native/sysroot`

## 校验方式

对 `core-render-ohos/src/main/cpp/libohos_render/manager/KRSnapshotManager.cpp`
做 `-fsyntax-only` 语法校验（仅验证修复代码的可编译性，不做完整 ohpm 工程链接）：

```bash
CLANG=$NDK/llvm/bin/clang++
$CLANG --target=aarch64-unknown-linux-ohos --sysroot=$SYSROOT \
       -std=c++17 -fsyntax-only \
       -I src/main/cpp -I src/main/cpp/libohos_render/api/include \
       -I $SYSROOT/usr/include -I $NDK/llvm/include/libcxx-ohos/include \
       -I $SYSROOT/usr/include/c++/v1 \
       src/main/cpp/libohos_render/manager/KRSnapshotManager.cpp
```

## 结果

- **EXIT=0，无报错** ✅
- 主修复（`OH_ArkUI_DrawableDescriptor_Dispose`）、次要加固
  （`nativePixelMap` 空指针早返回 / `size` 上限 / `malloc` 判空）均通过 SDK 头文件校验。
- `drawable_descriptor.h`、`native_node_napi.h` 等 API 签名与修复调用一致。

## 说明

- 完整 `ohpm`/hvigor 工程构建（生成 HAR + 拷贝 so 到 ohosApp）需在 DevEco
  工程上下文执行 `./2.0_ohos_demo_build.sh`，当前 Linux 沙箱仅做 native 语法校验。
- 内存曲线走平的真机验证仍按流程交接 Mac 工程师执行。

## 提交

- 主修复已提交（上游 Bug分析 阶段）：分支 `fix/ohos-toimage-datauri-native-leak`，HEAD `c4478825`
  `fix(ohos): 修复 toImage(DATA_URI) 反复调用导致的 native DrawableDescriptor 泄漏`。
- 加固 (b) 由本 Stage（Bug修复）在工作树中补全并同样通过本语法校验，**尚未单独 commit**
  （待与文档修订一并提交，或按流程在 Mac 环境完整验证后提交）。
- 注：历史记录中的 `361cd9e4` / `beac6fa5` 为早期中间 commit，已被后续提交覆盖/改写，当前分支最新为 `c4478825`。
