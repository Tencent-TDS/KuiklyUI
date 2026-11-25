# 2.31.0 变更说明

## Part 1 总体说明

2.31.0 在鸿蒙渲染稳定性、原生端崩溃防护、Compose 状态刷新与 H5 交互四方面做了修复与增强：集中修复鸿蒙 NAPI 句柄泄漏与输入崩溃，收敛 iOS/Android 的内存与生命周期隐患，并补齐 H5 与折叠屏的交互能力。具体如下：

- **鸿蒙渲染与交互**：修复 NAPI 句柄泄漏导致的 ArkTS 视图泄漏、输入框粘贴超长文本崩溃与视图销毁全树扫描卡顿，限制字号缩放上限并优化 JSON 响应解析；新增同步事件调度，折叠态映射为 pageData.hingeStatus，并在 set property 回调暴露实例 id。
- **iOS 与 Android 崩溃防护**：iOS 用 CoreText 替换文本测量解决酷狗 ANR、主线程预热 Kotlin/Native 运行时避免 finalizer 崩溃；Android 停止回收缓存位图、修正渲染层销毁时序、保护销毁后的模糊组件、修复 pager 分页除零，并统一选区高亮绘制顺序与颜色下发。
- **Compose 状态与布局**：修复快照重订阅以刷新派生状态基线、非滚动 Lazy 精确内容尺寸的 clamp，并让 safeAreaInsets 可观察，使仅 insets 变化时布局能正确更新。
- **H5 交互与渲染**：修复变换容器内 Hover 定位、跨页面通知分发与文本选择后无关控件不可点的问题，canvas 按设备像素比渲染。

历史提交可见 [2.30.0...2.31.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.30.0...2.31.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix(android): stop recycling cached bitmaps in KRMemoryCacheModule.onDestroy · [#1830](https://github.com/Tencent-TDS/KuiklyUI/pull/1830)
- fix(android): destroy render layer after pageWillDestroy · [#1834](https://github.com/Tencent-TDS/KuiklyUI/pull/1834)
- fix(android): guard RenderScriptBlur after destroy to avoid RSInvalidStateException · [#1792](https://github.com/Tencent-TDS/KuiklyUI/pull/1792)
- fix: 修复 pager 分页除零崩溃 · [#1866](https://github.com/Tencent-TDS/KuiklyUI/pull/1866)

### iOS

- fix(ios): replace Text measure TextStorage with CoreText to resolve kugou anr issue · [#1820](https://github.com/Tencent-TDS/KuiklyUI/pull/1820)
- fix(ios): warm up Kotlin/Native runtime on main thread to avoid finalizer 0xf8 crash · [#1860](https://github.com/Tencent-TDS/KuiklyUI/pull/1860)

### 鸿蒙

- fix(ohos): skip NODE_IMAGE_SOURCE_SIZE below API 21 · [#1828](https://github.com/Tencent-TDS/KuiklyUI/pull/1828)
- fix(ohos): keep KRSetSyncMainTaskWarnTimeout exported under hidden vi… · [#1831](https://github.com/Tencent-TDS/KuiklyUI/pull/1831)
- fix(ohos): close unpaired napi handle scopes to stop ArkTS view leaks · [#1729](https://github.com/Tencent-TDS/KuiklyUI/pull/1729)
- feat(ohos): Expose instance id in set property callback · [#1839](https://github.com/Tencent-TDS/KuiklyUI/pull/1839)
- feat(ohos): sync event scheduling · [#1851](https://github.com/Tencent-TDS/KuiklyUI/pull/1851)
- perf(ohos): cut redundant copies and dedup cost in JSON response parsing · [#1853](https://github.com/Tencent-TDS/KuiklyUI/pull/1853)
- fix(ohos): clamp fontSizeScale to app max font scale · [#1854](https://github.com/Tencent-TDS/KuiklyUI/pull/1854)
- fix(ohos): 输入框粘贴超长文本崩溃（UTF-8 截断 + 解码抛异常） · [#1843](https://github.com/Tencent-TDS/KuiklyUI/pull/1843)
- fix(ohos): add HandleScope to CallArkTSMethod to stop NAPI handle leak · [#1865](https://github.com/Tencent-TDS/KuiklyUI/pull/1865)
- feat(ohos): 鸿蒙折叠态映射为 pageData.hingeStatus · [#1863](https://github.com/Tencent-TDS/KuiklyUI/pull/1863)
- fix(ohos): avoid full-tree scan on KRView destruction to prevent main-thread freez · [#1870](https://github.com/Tencent-TDS/KuiklyUI/pull/1870)

### Compose

- fix(compose): re-observe snapshot reads in KNode onChanged to refresh derived-state baseline · [#1837](https://github.com/Tencent-TDS/KuiklyUI/pull/1837)
- fix(compose): clamp non-scrollable Lazy exact content size · [#1852](https://github.com/Tencent-TDS/KuiklyUI/pull/1852)
- fix(compose): make safeAreaInsets observable so insets-only changes update layout · [#1856](https://github.com/Tencent-TDS/KuiklyUI/pull/1856)

### H5

- fix(h5): stabilize Hover positioning in transformed containers · [#1838](https://github.com/Tencent-TDS/KuiklyUI/pull/1838)
- fix(h5app-js): dispatch notifications across live Kuikly pages · [#1847](https://github.com/Tencent-TDS/KuiklyUI/pull/1847)
- fix(h5App-js): keep unrelated controls clickable after text selection · [#1849](https://github.com/Tencent-TDS/KuiklyUI/pull/1849)
- fix(h5App-js): render canvas at device pixel ratio · [#1850](https://github.com/Tencent-TDS/KuiklyUI/pull/1850)

### 多端

- fix(textSelection): 选区高亮绘制顺序与颜色下发修复 · [#1845](https://github.com/Tencent-TDS/KuiklyUI/pull/1845)

### 其他

- feat(demo): add fold adaptive settings split demo · [#1832](https://github.com/Tencent-TDS/KuiklyUI/pull/1832)
- chore(demo): add fold adaptation demos under pages/fold · [#1833](https://github.com/Tencent-TDS/KuiklyUI/pull/1833)
- docs(sidebar): fix a case mismatch and register two orphaned Compose pages · [#1827](https://github.com/Tencent-TDS/KuiklyUI/pull/1827)
- feat(web): add removeNotify doc · [#1855](https://github.com/Tencent-TDS/KuiklyUI/pull/1855)
- docs: add 2.30.0 version description to changelog · [#1861](https://github.com/Tencent-TDS/KuiklyUI/pull/1861)
- docs: clarify Input and TextArea overflow guidance · [#1871](https://github.com/Tencent-TDS/KuiklyUI/pull/1871)

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。
