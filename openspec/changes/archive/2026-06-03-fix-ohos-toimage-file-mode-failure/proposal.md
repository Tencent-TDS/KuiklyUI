## Why

鸿蒙（HarmonyOS）平台调用 `view.toImage(DeclarativeBaseView.ImageType.FILE)` 时，ArkTS 侧已成功截图并落盘到本地文件，但 C++ 层因 `drawableDescriptor == null` 将其误判为失败，返回 `code=-1, message=""` 给业务。该问题导致 FILE 模式截图完全不可用，阻塞了依赖截图分享的商家海报等业务场景。

## What Changes

- **修复 C++ 成功判定**：`KRSnapshotManager::TakeSnapshot` 回调中，按 `type` 分流成功判定逻辑。`file` 模式以 `path/pathURI` 有效性为依据，不再强制要求 `drawableDescriptor` 非空。
- **修复 message 字段错位**：C++ 侧读取 ArkTS 错误信息时，将 `message` 正确写入 `resultData.message`（而非 `resultData.data`），确保业务能收到可读的错误提示。
- **修复 ArkTS err 分支 callback 缺失**：`KRSnapshotModule.ets` 中 `componentSnapshot.get` 失败时补充 `callback` 调用，避免 C++ 侧永远挂起。
- **统一日志关键词**：将所有鸿蒙 `toImage` 日志 Tag 统一为 `ohosToImage`，便于按同一关键词检索整条链路。

## Capabilities

### New Capabilities

（无新增能力，本次为 bug 修复）

### Modified Capabilities

（无 spec 级需求变更，本次为实现层 bug 修复）

## Impact

- **平台**：HarmonyOS（OHOS）
- **模块**：`core-render-ohos`（C++ render 层 + ArkTS module 层）
- **API 影响**：`DeclarativeBaseView.toImage(ImageType.FILE)` 在鸿蒙端的行为从"始终返回 -1"变为"正常返回文件路径"
- **风险面**：仅影响 `core-render-ohos/src/main/cpp/libohos_render/manager/KRSnapshotManager.cpp` 和 `core-render-ohos/src/main/ets/modules/internal/KRSnapshotModule.ets`，不涉及跨平台共享模块，不影响 Android/iOS/Web/小程序。

## Non-goals

- 不修改 `core/` 或 `compose/` 等 KMP 共享模块的 API 定义
- 不改动 Android / iOS / Web / 小程序等其他平台的截图实现
- 不新增截图格式或截图参数（如 sampleSize 行为不变）
- 不修改 Demo 页面的业务逻辑（Demo 只用于验证修复）
