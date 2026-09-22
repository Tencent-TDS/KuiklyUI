# 2.12.0 变更说明

## Part 1 总体说明

2.12.0 把 Kuikly 页面带到了 macOS，修掉了文本排版与列表渲染的一批边界问题，并让滚动与翻页的控制手段更完整，具体如下：

- **macOS 端支持**（macOS）：新增 macOS 渲染端，同一套页面可运行在 macOS 上。
- **文本排版的边界修正**（iOS、鸿蒙）：硬换行后的省略号、换行间距、中文引号字形等排版异常得到修正，长文本与混排场景的显示更符合预期。
- **滚动与翻页的可控性**（iOS、Compose、跨端）：滚动容器支持回到顶部，翻页支持弹性动画，Compose 的翻页可自定义动画曲线，弹簧动画结束后的滚动停止也更可靠。
- **列表计算与性能采集的稳定性**（Android、鸿蒙、Compose、跨端）：FPS 采集不再引发 ANR，瀑布流中绝对定位子项的宽度计算被修正，列表重复增量更新、Compose 在 Kotlin 1.9 与官方 Compose 下的兼容问题一并解决。

历史提交可见 [2.11.0...2.12.0](https://github.com/Tencent-TDS/KuiklyUI/compare/2.11.0...2.12.0)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: Android fps detector continuous collection cause anr problem · [#751](https://github.com/Tencent-TDS/KuiklyUI/pull/751)

### iOS

- feat: support scrollToTop for kuikly dsl · [#769](https://github.com/Tencent-TDS/KuiklyUI/pull/769)
- fix: Fix KRBlurView blur effect reset issue and retain cycle · [#778](https://github.com/Tencent-TDS/KuiklyUI/pull/778)
- fix: add manual ellipsis handling for iOS after hard line breaks · [#795](https://github.com/Tencent-TDS/KuiklyUI/pull/795)

### 鸿蒙

- fix(ohos): scroller bounce conflict with touch event stop propagation · [#773](https://github.com/Tencent-TDS/KuiklyUI/pull/773)
- fix(ohos): add missing ets api exports · [#774](https://github.com/Tencent-TDS/KuiklyUI/pull/774)
- Line break margin · [#804](https://github.com/Tencent-TDS/KuiklyUI/pull/804)
- fix: ohos getWindowActiveDensity compatible with api12 · [#825](https://github.com/Tencent-TDS/KuiklyUI/pull/825)
- chore: update publish sh and podspec/oh-package version placeholder · [#829](https://github.com/Tencent-TDS/KuiklyUI/pull/829)
- fix: gesture collision issue · [#831](https://github.com/Tencent-TDS/KuiklyUI/pull/831)
- chore: update ohos changelog release url · [#833](https://github.com/Tencent-TDS/KuiklyUI/pull/833)
- fix: chinese quotation mark rendering issue · [#836](https://github.com/Tencent-TDS/KuiklyUI/pull/836)

### Compose

- feat: Add custom animationSpec support for PagerState.animateScrollToPage · [#767](https://github.com/Tencent-TDS/KuiklyUI/pull/767)
- fix: android spring animation end stopScroll · [#777](https://github.com/Tencent-TDS/KuiklyUI/pull/777)
- fix: compose WeakReference Internal in kotlin1.9 error · [#780](https://github.com/Tencent-TDS/KuiklyUI/pull/780)
- feat: enableConsumeSnapshot compat official compose · [#787](https://github.com/Tencent-TDS/KuiklyUI/pull/787)

### macOS

- feat: macOS support · [#820](https://github.com/Tencent-TDS/KuiklyUI/pull/820)

### 跨端

- fix: correct absolute position item(Hover/Refresh) width in Waterfall… · [#784](https://github.com/Tencent-TDS/KuiklyUI/pull/784)
- fix: issue of duplicate incremental updates in vfor · [#810](https://github.com/Tencent-TDS/KuiklyUI/pull/810)
- docs: add text stroke docs · [#815](https://github.com/Tencent-TDS/KuiklyUI/pull/815)
- feat: pagelist scrollToPageIndex support springAnimation · [#814](https://github.com/Tencent-TDS/KuiklyUI/pull/814)

### 其他

- chore: update ohos performance api docs · [#765](https://github.com/Tencent-TDS/KuiklyUI/pull/765)
- chore: update scroller doc with scrollToTop event support · [#770](https://github.com/Tencent-TDS/KuiklyUI/pull/770)
- docs: add back-press-handler introduction · [#779](https://github.com/Tencent-TDS/KuiklyUI/pull/779)
- docs: add ohos initial size docs · [#783](https://github.com/Tencent-TDS/KuiklyUI/pull/783)
- docs: add thread verify docs · [#781](https://github.com/Tencent-TDS/KuiklyUI/pull/781)
- chore: update autoDarkEnable docs · [#788](https://github.com/Tencent-TDS/KuiklyUI/pull/788)
- docs: add web doc for jssdk、spa、image path · [#782](https://github.com/Tencent-TDS/KuiklyUI/pull/782)
- docs: update longpress callback note · [#797](https://github.com/Tencent-TDS/KuiklyUI/pull/797)
- docs: update video API adapter note · [#800](https://github.com/Tencent-TDS/KuiklyUI/pull/800)
- docs: update font use dp unit · [#796](https://github.com/Tencent-TDS/KuiklyUI/pull/796)
- docs: DevGuide add 性能优化 content and back-press-handler.md · [#807](https://github.com/Tencent-TDS/KuiklyUI/pull/807)
- docs: update animation list scroller components docs · [#813](https://github.com/Tencent-TDS/KuiklyUI/pull/813)
- docs: update zIndex docs · [#816](https://github.com/Tencent-TDS/KuiklyUI/pull/816)
- docs: add enablePinyinCallback doc and android stackSize custom doc · [#817](https://github.com/Tencent-TDS/KuiklyUI/pull/817)
- feat: modify web · [#821](https://github.com/Tencent-TDS/KuiklyUI/pull/821)
- docs: add ohos capture api docs · [#822](https://github.com/Tencent-TDS/KuiklyUI/pull/822)
- docs: update canvas api and clipPath docs · [#818](https://github.com/Tencent-TDS/KuiklyUI/pull/818)
- fix: ohos core gradle appleMain problem · [#828](https://github.com/Tencent-TDS/KuiklyUI/pull/828)
- docs: update image adapter docs · [#826](https://github.com/Tencent-TDS/KuiklyUI/pull/826)
- docs: add debug name docs · [#830](https://github.com/Tencent-TDS/KuiklyUI/pull/830)
- docs: add andorid start analysis guide · [#808](https://github.com/Tencent-TDS/KuiklyUI/pull/808)
- docs: add ohos delegate note · [#809](https://github.com/Tencent-TDS/KuiklyUI/pull/809)
- docs: add ohos c module · [#835](https://github.com/Tencent-TDS/KuiklyUI/pull/835)

> **分节说明**：本部分按改动实际落在哪些 Native 端划分。`Android` / `iOS` / `鸿蒙` / `H5` / `小程序` / `macOS` 为单端改动，即只涉及一个 Native 端；`多端` 为同时改动了多个 Native 端；`跨端` 为只改 Kotlin 上层、无 Native 改动，一次改完各端都生效；`Compose` 为 Compose 侧改动，即使会下发到 Native 三端也只在此列出，不重复计入 `多端`；`其他` 为文档、示例与发版杂项。

---

## Part 3 破坏性变更

本版无业务可感知的破坏性变更。
