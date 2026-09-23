# Bug 跟进：DeclarativeBaseView.toImage(DATA_URI) 反复调用内存持续升高（鸿蒙）

> 来源：OnCall 初步 Bug 报告 `.idea/outputs/bug_toimage_memory_leak.md`
> 平台：鸿蒙（OHOS），DSL 无关（toImage 属 DeclarativeBaseView 公共 API）
> 跟进方式：静态根因分析（Linux 环境无法实跑鸿蒙；按任务要求新建分支并提交，交接 Mac 环境工程师实机验证）

---

## REPRO

- **现象**：鸿蒙端反复调用 `DeclarativeBaseView.toImage(ImageType.DATA_URI)`（如每秒一次），进程 native 内存随调用次数单调上升且不回落。
- **入口链**：`core/.../DeclarativeBaseView.kt:301 toImage()` → `renderView?.callMethod("toImage", params, ...)` → native `KRSnapshotManager::TakeSnapshot` → `toImageCb` 回调。
- **Demo 来源**：未提供，复现依赖鸿蒙设备运行时，当前环境无法编译鸿蒙 App（需 Mac + `./2.0_ohos_demo_build.sh`）。
- **结论**：本环境无法实跑复现，根因以仓内源码静态分析 + SDK 头文件语义核对为准。

---

## DIAGNOSE

### 主根因（已静态确认 —— 高置信）

文件：`core-render-ohos/src/main/cpp/libohos_render/manager/KRSnapshotManager.cpp`

`TakeSnapshot` 的 `toImageCb` 回调（行 247–274）：

1. 行 248–256：对 `dataUri` / `cacheKey` 两条分支，都先取 `snapshotData.drawableDescriptor`，调用
   `OH_ArkUI_GetDrawableDescriptorFromNapiValue(env, drawableDescriptor, &drawableDescriptorPtr)`（行 256），
   将 ArkTS `PixelMapDrawableDescriptor` 映射为 native `ArkUI_DrawableDescriptor*`。
   - **所有权语义（已核对 SDK 头文件）**：`native_node_napi.h:107` 该 API "maps it to an ArkUI_DrawableDescriptor
     object on the native side"，对应释放函数是 `drawable_descriptor.h:154 OH_ArkUI_DrawableDescriptor_Dispose`
     （"Destroys the pointer to the drawableDescriptor"）。即**拿到的是 native 侧新对象，所有权转移给调用方，必须 Dispose**。
   - 仓库内有且仅有的释放点：`ReleaseDrawableItem`（行 34–53）对 `item->drawableDescriptor` 调用
     `OH_ArkUI_DrawableDescriptor_Dispose`（行 37）—— 专门用于 `cacheKey` 缓存项。这从侧面印证该指针必须手动释放。
2. 行 261–263：`dataUri` 分支把 `drawableDescriptorPtr` 传入 `ProcessSnapshotResultWithDataType(...)`，
   但该函数（行 110–139）**只用 `pixelMap`**，从不使用也不释放 `drawableDescriptorPtr`。
3. **结论**：每次 `dataUri` 调用泄漏一个 native `ArkUI_DrawableDescriptor`，反复调用 → 内存持续升高，与 issue 现象完全吻合。

> 注：`cacheKey` 分支会把 `drawableDescriptorPtr` 存入 `drawableDescriptorCache_`（行 177 `CacheSnapshot`），
> 由 `ReleaseDrawableItem` 在覆盖/析构时释放，所以 cacheKey 路径**不泄漏 descriptor**（但存在 follow-up 的缓存淘汰问题，见下）。
> 因此 leak 是 `dataUri` 分支特有的。

### 次要问题（同一函数，确认 + 修正）

1. **【报告称：PixelMap wrapper 泄漏，但 API 名存疑】**
   报告建议 `OH_PixelMap_Release(nativePixelMap)`。经核对整个 SDK sysroot（headers + libs），
   **`OH_PixelMap_Release` 不存在**。仓库内也无任何 `NativePixelMap*` 的 Release 调用。
   `NativePixelMap*`（来自 `OH_PixelMap_InitNativePixelMap`，`image_pixel_map_mdk.h:294`）的释放函数在此 SDK 版本中
   未发现明确的公开 `Release` API（仅有 `OH_PixelmapNative_Release` 用于另一套 `OH_PixelmapNative*` 类型）。
   → **不臆造 API**。本修复**不补 PixelMap release**，仅作为待核实项交接工程师在真机/文档确认。
2. **【已确认】行 114 后未校验 `nativePixelMap`**：创建失败返回 nullptr 时，行 116
   `OH_PixelMap_GetImageInfo(nativePixelMap, &info)` 将解引用空指针 → 崩溃风险。建议 return 前 null 早返回。
3. **【已确认】行 121–122 `malloc(size)` 未判空/未校验 size**：`size = width*height*4`，超大图或异常时
   可能 malloc 失败（返回 nullptr，第 127 行 PackToData 解引用空指针）或 size 异常导致越界。建议加判空与 size 上限。
4. **【确认，独立 follow-up】`cacheKey` 缓存键为 `data:image_Md5_Pixelmap<ptr>`（行 172，指向 descriptor 地址）**，
   唯一 key 只增不删（仅在 key 重复或析构时释放），长期高频唯一 key 也会增长。需加 LRU/容量上限淘汰。改动较大，建议独立 issue。

### 根因结论（暂定）

主根因 = `dataUri` 分支取得 `ArkUI_DrawableDescriptor*` 后从未 `Dispose`，导致 native 内存逐次泄漏。
已由 SDK 头文件所有权语义 + 仓库内唯一 Dispose 点的对照确认，置信度高。需真机内存曲线验证。

---

## FIX_PLAN

### 根因摘要
`toImageCb` 对 `dataUri` 分支通过 `OH_ArkUI_GetDrawableDescriptorFromNapiValue` 取得的 native
`ArkUI_DrawableDescriptor*` 未被 `OH_ArkUI_DrawableDescriptor_Dispose` 释放，每次调用泄漏一个 native 对象。

### 修复方案（最小、安全、符合仓库既有约定）

**修改文件**：`core-render-ohos/src/main/cpp/libohos_render/manager/KRSnapshotManager.cpp`

**(a) 主修复 —— 释放 dataUri 分支的 drawableDescriptorPtr（行 261–263）**：
```cpp
if (type == "dataUri") {
    resultData = snapshotManager->ProcessSnapshotResultWithDataType(
        env, pixelMap, "", "", drawableDescriptorPtr, weak_view);
    if (drawableDescriptorPtr) {
        OH_ArkUI_DrawableDescriptor_Dispose(drawableDescriptorPtr);
        drawableDescriptorPtr = nullptr;
    }
}
```
- 放置顺序：先处理（函数内只用 pixelMap，不碰 descriptorPtr），再释放，避免 Use-After-Free。
- 与 `ReleaseDrawableItem`（行 37）的释放方式完全一致，是仓库约定做法。

**(b) 次要加固 —— `ProcessSnapshotResultWithDataType` 容错（行 114–122）**：
```cpp
NativePixelMap *nativePixelMap = OH_PixelMap_InitNativePixelMap(env, pixelMap);
if (nativePixelMap == nullptr) {
    resultData.code = -1;
    resultData.message = "failed to init native pixelmap";
    return resultData;
}
OhosPixelMapInfos info;
OH_PixelMap_GetImageInfo(nativePixelMap, &info);
...
size_t size = info.width * info.height * 4;
if (size == 0 || size > kMaxSnapshotBufferSize) {  // 建议加常量上限，如 4 * 4096 * 4096
    resultData.code = -1;
    resultData.message = "invalid snapshot size";
    return resultData;
}
uint8_t *outData = reinterpret_cast<uint8_t *>(malloc(size));
if (outData == nullptr) {
    resultData.code = -1;
    resultData.message = "failed to alloc snapshot buffer";
    return resultData;
}
```

**(c) 不在本次修复范围**：
- PixelMap wrapper 的释放（SDK 无明确 `OH_PixelMap_Release`，待核实）。
- `cacheKey` 缓存淘汰（改动大，独立 follow-up）。

### 模块边界
仅改动 `core-render-ohos`（C++），不触碰 `core/`、`compose/`，符合"core-render-* 禁止依赖 core/compose、也不被其依赖"的边界约束。

### 修改文件清单
- `core-render-ohos/src/main/cpp/libohos_render/manager/KRSnapshotManager.cpp`（唯一改动）

### 风险评估
- 低：Dispose 调用与现有 cacheKey 路径写法一致；释放发生在 ProcessSnapshot 之后，无悬垂引用。
- 待真机验证：鸿蒙内存曲线是否在反复 DATA_URI 调用后走平；并确认 (b) 加固不影响正常截图清晰度/尺寸。

---

## FIX_IMPL

> 主修复已提交（分支 `fix/ohos-toimage-datauri-native-leak`，历史 commit 见 `git log`）。
> 本环境（Linux）无法实跑鸿蒙 App，但本 Stage（Bug修复）用 OHOS NDK 对修复后源码做了 `-fsyntax-only`
> 语法校验（见 `BugFix/toimage_memory_leak_ohos_compile.md`）。
> 交接 Mac 环境工程师执行 `./2.0_ohos_demo_build.sh` 实机验证反复 DATA_URI 调用的内存曲线。

### 已应用的改动（最小、仅限 core-render-ohos）

1. **主修复（上游 Bug分析 阶段已提交）**：`toImageCb` 的 `dataUri` 分支在调用 `ProcessSnapshotResultWithDataType` 之后，
   显式 `OH_ArkUI_DrawableDescriptor_Dispose(drawableDescriptorPtr)` 并置空（源文件行 264–269）。
   与 `cacheKey` 路径 `ReleaseDrawableItem`（行 37）的释放写法一致，且释放发生在 Process 之后，无悬垂引用。
2. **加固（本 Stage Bug修复 实际落地）**：`ProcessSnapshotResultWithDataType`（原行 114 起）补：
   - `nativePixelMap` 空指针早返回（避免 `OH_PixelMap_GetImageInfo` 解引用空指针崩溃，见 FIX_PLAN 次要问题 2）；
   - `size` 上限 `kMaxSnapshotBufferSize = 4×4096×4096`（避免异常尺寸越界，见 FIX_PLAN 次要问题 3）；
   - `malloc(size)` 返回值判空（避免 OOM 时空指针解引用，见 FIX_PLAN 次要问题 3）。
   > 注：FIX_PLAN 已确认本加固，但上游阶段提交时工作树仅含主修复、(b) 实际未进入源码；
   > 本轮在 Bug修复阶段补全 (b) 并经 NDK 语法校验通过。

### SDK 头文件二次核对（本轮验证）
- `arkui/drawable_descriptor.h:154` `OH_ArkUI_DrawableDescriptor_Dispose` 签名 `void(...)`，主修复调用合法。
- `core-render-ohos` 内 `OH_ArkUI_GetDrawableDescriptorFromNapiValue` 仅此一处取 descriptor；`cacheKey` 分支把
  ptr 存入 `drawableDescriptorCache_` 由 `ReleaseDrawableItem` 后续释放，无需在此处释放 —— 主修复只覆盖 `dataUri` 分支，无双重释放风险。
- `Image_PixelMap` 的 `NativePixelMap*`（来自 `image_pixel_map_mdk.h:294 OH_PixelMap_InitNativePixelMap`，
  API since 10）在该 SDK 头文件中**无 `Release`/`Dispose`/`Free`**；`pixelmap_native.h` 的 `OH_PixelmapNative_Release`
  属于另一类型 `OH_PixelmapNative*`，不可混用。→ 本轮仍**不补 PixelMap release**，不臆造 API。

### 未改动（交接确认项）
- PixelMap wrapper 释放：SDK 该版本无 `OH_PixelMap_Release`，未臆造 API，待真机/文档核实。
- cacheKey 缓存淘汰：独立 follow-up。

### 验证步骤（Mac 环境）
1. `./2.0_ohos_demo_build.sh` 编译 ohosApp。
2. 在任意页面接入反复 `toImage(ImageType.DATA_URI)`（每秒一次）的定时器。
3. 观察 DevEco 内存曲线：修复后应在调用次数增长时走平（不再单调上升）。
4. 确认正常截图清晰度/尺寸不受影响（加固分支的 size 上限 4×4096×4096 对常规 View 截图足够）。


---

## DOC_REVIEW

（待 FIX_IMPL 验证后评估）

---

## FIX_RESULT（Stage_bug_fix 轮次 · 2026-09-23）

> 本 Stage 接续上游 Bug分析，沿用 `kuikly-debug` 工作流。根因（主）在上游已暂定 + 本环境静态确认（高置信）。

### 本轮实际落地内容
1. **核对现状**：拉取分支 `fix/ohos-toimage-datauri-native-leak`（HEAD `c4478825`），确认主修复（a）已在源码并落地；
   但 FIX_PLAN 确认的加固（b）实际未进入源码（仅在文档里被记为"已提交"），存在文档/代码不一致。
2. **补全加固（b）**：在 `ProcessSnapshotResultWithDataType` 增加 `nativePixelMap` 空指针早返回、
   `size` 上限 `kMaxSnapshotBufferSize`、以及 `malloc` 返回值判空，覆盖 FIX_PLAN 次要问题 2/3 的崩溃/OOM 风险。
3. **真编译验证（非仅文档声称）**：用 OHOS NDK
   `aarch64-unknown-linux-ohos clang++ --target=... -fsyntax-only`（llvm-15，sysroot 来自 command-line-tools SDK）
   对 `KRSnapshotManager.cpp` 校验，**EXIT=0 无报错**——主修复 + 加固均通过 SDK 头文件签名检查。
   完整 ohpm/hvigor HAR 构建仍须在 DevEco 执行 `./2.0_ohos_demo_build.sh`。

### 根因最终确认
- 主根因 = `dataUri` 分支取得 `ArkUI_DrawableDescriptor*` 后从未 `Dispose` → native 内存逐次泄漏。已通过 SDK 头文件
  所有权语义 + 仓库内唯一 `ReleaseDrawableItem` Dispose 点对照确认，置信度高。
- 加固（b）属同一函数内的健壮性二次问题，非 leak 主因，但属 FIX_PLAN 范围内应交付项，本轮补齐。

### 实际修改文件清单
- `core-render-ohos/src/main/cpp/libohos_render/manager/KRSnapshotManager.cpp`（主修复已由上游提交；本轮回补加固 (b)）
- `BugFix/toimage_memory_leak_ohos.md`（修正 FIX_IMPL 文档、追加本段 FIX_RESULT）

### 待交接 / 不在本 Stage 范围
- 真机内存曲线走平验证：Mac 环境 `./2.0_ohos_demo_build.sh` + 反复 DATA_URI 定时调用（上游未交付，按流程交接）。
- PixelMap wrapper 释放：该 SDK 版本无 `OH_PixelMap_Release`，不臆造 API（FIX_PLAN 次要问题 1，已澄清不补）。
- cacheKey 缓存淘汰（LRU/容量上限）：改动大，独立 follow-up。
