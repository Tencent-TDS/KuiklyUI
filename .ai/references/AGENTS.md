# References 索引

| 你遇到的情况 | 读取文档 |
|------------|---------|
| 遇到编译报错、import 找不到、`Color.parseColor` 错误、生命周期方法不存在（`onDestroyView` 等）、Module 调用失败、bridge 超时、NPE crash、内存泄漏 | [common-errors.md](common-errors.md) |
| 扩展原生能力、写 `Module`、桥接原生 View、使用 `acquireModule`/`TDF_EXPORT_METHOD`/`syncToNativeMethod` | [native-bridge.md](native-bridge.md) |
| 通信调用没有响应、callback 没有触发、异步时序问题、需要了解各平台实现模板或 BridgeManager 方法 ID | [native-bridge-internals.md](native-bridge-internals.md) |
| `LazyColumn`/`LazyGrid`/`LazyStaggeredGrid` 滚动截断、`contentSize` 计算错误、`realContentSize` 为 null、加载更多不触发、`canScrollForward` 异常、`tryExpandStartSizeNoScroll` | [lazy-scroll.md](lazy-scroll.md) |
| 嵌套滚动冲突、`nestedScroll`/`NestedScrollCoordinator`/`SELF_FIRST`/`PARENT_FIRST`/`SELF_ONLY`、scroll lock/unlock、多层嵌套、`cascadeLock`、`activeInnerScrollView`、滚动事件被吞 | [nested-scroll.md](nested-scroll.md) |
| 发布新版本 SDK、执行发布脚本、更新版本号、排查发布失败、多版本构建 | [publish.md](publish.md) |
