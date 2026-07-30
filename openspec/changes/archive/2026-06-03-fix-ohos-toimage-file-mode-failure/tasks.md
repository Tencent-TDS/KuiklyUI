## 1. C++ 层修复（core-render-ohos）

- [x] 1.1 修改 `KRSnapshotManager::TakeSnapshot` 回调逻辑：按 `type` 分流，file 模式不再检查 `drawableDescriptor`，改为检查 `path/pathURI` 有效性
- [x] 1.2 修复 C++ 侧 message 字段错位：失败时将 ArkTS `message` 读入 `resultData.message` 而非 `resultData.data`
- [x] 1.3 验证 C++ 日志 Tag 已统一为 `ohosToImage`（当前 C++ 文件无日志系统，无需修改）

## 2. ArkTS 层修复（core-render-ohos）

- [x] 2.1 修改 `KRSnapshotModule.ets`：在 `componentSnapshot.get` 的 `err` 分支中补充 `callback([resultParam, new ArrayBuffer(1)])`，避免回调悬挂
- [x] 2.2 验证 ArkTS 日志 Tag 已统一为 `ohosToImage`
- [x] 2.3 验证 `KRPixelMapUtil.ets` 日志 Tag 已统一为 `ohosToImage`（该文件当前无日志，无需修改）

## 3. 验证与测试

- [ ] 3.1 编译鸿蒙 Demo：`./2.0_ohos_demo_build.sh`
- [ ] 3.2 在鸿蒙设备/模拟器上运行 `SearchMerchantToImageDemoPage`
- [ ] 3.3 点击「生成图片」，确认日志链路：
  - ArkTS：`toImage file save success`
  - C++：`TakeSnapshot final result, code=0, data=file://...`
  - 业务回调：`code=0, filePath=非空`
- [ ] 3.4 确认预览图正常显示（Image 组件加载返回的文件路径）
- [ ] 3.5 验证 dataUri / cacheKey 模式行为未改变（可选回归）
