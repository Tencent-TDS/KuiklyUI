## 1. 接口扩展 — core-render-android 模块

- [x] 1.1 在 `IKuiklyRenderViewExport.kt` 中新增 `KUIKLY_RENDER_TAG_KEY` 私有常量（使用 int key `0x1e1c0001` 作为唯一标识）
- [x] 1.2 在 `IKuiklyRenderViewExport` 接口中新增 `kuiklyRenderTag: Int` 属性，默认实现通过 `view().getTag(KUIKLY_RENDER_TAG_KEY) as? Int ?: -1` 获取
- [x] 1.3 在 `IKuiklyRenderViewExport` 接口中新增 `setKuiklyRenderTag(tag: Int)` 方法，默认实现通过 `view().setTag(KUIKLY_RENDER_TAG_KEY, tag)` 设置
- [x] 1.4 运行 `./gradlew :core-render-android:compileDebugKotlin` 验证编译通过

## 2. 渲染层集成 — core-render-android 模块

- [x] 2.1 在 `KuiklyRenderLayerHandler.kt` 的 `createRenderViewHandler(tag, viewName)` 方法中，创建 `RenderViewHandler` 并调用 `putRenderViewHandler` 之前，添加 `renderViewHandler.viewExport.setKuiklyRenderTag(tag)` 调用
- [x] 2.2 同样在复用路径（`popRenderViewHandlerFromReuseQueue` 返回非空时），确认 tag 是否需要更新（复用场景 tag 不变，无需额外处理）
- [x] 2.3 运行 `./gradlew :core-render-android:compileDebugKotlin` 验证编译通过

## 3. 验证与测试 — androidApp 模块

- [ ] 3.1 在 `androidApp` demo 中创建自定义测试 View（`TestTagExportView`），实现 `IKuiklyRenderViewExport`，在 `setProp` 中打印 `kuiklyRenderTag` 确认可正确读取
- [ ] 3.2 在 Kuikly DSL 侧创建对应 View，验证 tag 值与预期一致（可通过 `KuiklyRenderView.getView(tag)` 交叉验证）
- [x] 3.3 运行 `./gradlew :androidApp:assembleDebug` 验证完整 APK 编译通过

## 4. 收尾

- [ ] 4.1 检查是否需要更新 `docs/DevGuide/expand-native-ui.md` 文档，补充 `kuiklyRenderTag` 的使用说明（可选）
- [ ] 4.2 最终编译验证：`./gradlew :core-render-android:compileDebugKotlin :androidApp:assembleDebug` 全部 BUILD SUCCESSFUL
- [ ] 4.3 提交代码：`git add . && git commit -m "feat(core-render-android): expose kuiklyRenderTag in IKuiklyRenderViewExport"`
