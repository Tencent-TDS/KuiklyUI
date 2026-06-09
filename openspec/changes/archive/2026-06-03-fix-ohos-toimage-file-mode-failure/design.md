## Context

鸿蒙平台 `toImage(FILE)` 的调用链路如下：

```
Kotlin (core)
  └── DeclarativeBaseView.toImage(ImageType.FILE)
        └── NativeBridge 调用 toImage 方法
              └── C++ KRSnapshotManager::TakeSnapshot()
                    └── CallArkTSMethod("KRSnapshotModule", "toImage")
                          └── ArkTS KRSnapshotModule.call()
                                └── componentSnapshot.get(id) 截图
                                      └── 成功 → KRPixelMapUtil.toFile() 落盘
                                            └── callback([resultParam, buf]) 回传 C++
```

**当前问题**：

1. **FILE 模式被误判为失败**：ArkTS 侧 `file` 分支不设置 `drawableDescriptor`（这是合理的，file 模式只需要路径），但 C++ 侧用 `drawableDescriptor == null` 作为统一的失败判定条件，导致 FILE 成功被误判为 `code=-1`。

2. **message 字段错位**：C++ 侧失败时把 ArkTS 的 `message` 读进了 `resultData.data`，最终回调拼装时读的是 `resultData.message`（从未赋值），导致业务收到空 message。

3. **ArkTS err 分支未 callback**：`componentSnapshot.get` 失败时直接 `return`，没有调用 `callback`，C++ 侧会永远挂起。

## Goals / Non-Goals

**Goals：**
- FILE 模式截图成功时正确返回 `code=0` 和文件路径
- 失败时返回有意义的 `message` 给业务
- `componentSnapshot.get` 失败时及时回传错误，避免回调悬挂

**Non-Goals：**
- 不新增 ArkTS 截图 API 或参数
- 不改变 dataUri / cacheKey 模式的行为
- 不修改 Kotlin 层的 `toImage` 接口定义
- 不涉及 Android / iOS / Web 等其他平台

## Decisions

### 1. C++ 侧按 `type` 分流判定（而非改 ArkTS 伪造 drawableDescriptor）

**选择**：在 `KRSnapshotManager::TakeSnapshot` 的回调 lambda 中，先判断 `type`，`file` 模式直接检查 `path/pathURI` 有效性，不再检查 `drawableDescriptor`。

**理由**：
- `file` 模式的语义就是"给我文件路径"，不需要 `PixelMapDrawableDescriptor` 这个对象
- 如果在 ArkTS 侧伪造 `drawableDescriptor`，会增加不必要的内存/NAPI 引用生命周期管理，且语义不对
- C++ 是判定逻辑的拥有方，修复判定逻辑最合理

**替代方案（已否决）**：在 ArkTS `file` 分支创建 `PixelMapDrawableDescriptor` 并回传。否决原因：增加内存开销和生命周期复杂度，且掩盖了真正的判定逻辑错误。

### 2. 同时修复 message 错位和 ArkTS err callback

**选择**：把 C++ 侧的 `resultData.data = arkTs.GetString(...)` 改为 `resultData.message = ...`，并给 ArkTS err 分支补上 `callback([resultParam, buf])`。

**理由**：
- message 错位是同一个判定分支里的连带 bug，顺手修成本极低
- err 分支缺失 callback 是独立 bug，但修复面极小（一行），且同属"错误信息回传"主题

## Risks / Trade-offs

- **[Risk] C++ `file` 分支 `path` 为空时的防御** → Mitigation: 加 `pathStr.empty()` 检查，空路径返回 `code=-1, message="snapshot path is empty"`
- **[Risk] ArkTS 侧 callback 参数格式变化** → Mitigation: 不改变 callback 参数结构，只是补充了之前遗漏的调用
- **[Risk] 日志关键词 `ohosToImage` 与已有日志系统冲突** → Mitigation: `ohosToImage` 是本次新增的专用 Tag，不会与其他系统日志冲突

## File Changes

### core-render-ohos

| 文件 | 修改内容 |
|------|----------|
| `src/main/cpp/libohos_render/manager/KRSnapshotManager.cpp` | ① 按 `type` 分流成功判定 ② 修复 message 字段读取 ③ Tag 统一为 `ohosToImage` |
| `src/main/ets/modules/internal/KRSnapshotModule.ets` | ① err 分支补 callback ② 所有日志 Tag 统一为 `ohosToImage` |
| `src/main/ets/utils/KRPixelMapUtil.ets` | 所有日志 Tag 统一为 `ohosToImage` |
