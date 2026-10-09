# 2.23.1 变更说明

## Part 1 总体说明

2.23.1 收敛了文本输入与选区的跨端行为，修复了动画回调与异常钩子相关的崩溃隐患，并优化了快速滑动、翻页回弹与鸿蒙桥调用的表现，页面在高频交互下更稳更顺。具体如下：

- **文本输入与选区时机**（鸿蒙、Android、iOS）：多行输入改用系统原生文本域属性，选区创建与清除的事件派发与 Android 对齐，自动聚焦也只在程序化同步文本时触发。
- **崩溃与异常处理**（鸿蒙、Android）：动画回调改用弱引用持有，避免回调执行时对象已释放导致的野指针崩溃；同时移除致命错误守护，保留 Kotlin/Native 原生的未捕获异常钩子，页面销毁后通知模块也不再重复注册监听。
- **滑动与翻页流畅度**（Compose、鸿蒙、Android）：无障碍语义更新按帧合并，快速滑动不再出现白屏；翻页阻尼回弹改用默认弹簧参数，拖拽结束已同步时也不再打断进行中的动画。
- **鸿蒙桥调用开销**（鸿蒙）：渲染桥调用的性能得到优化并补充压测页面，页面内高频通信的开销明显下降。

历史提交可见 [2.23.0...2.23.1](https://github.com/Tencent-TDS/KuiklyUI/compare/2.23.0...2.23.1)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix(android): prevent KRNotifyModule from re-registering receiver after onDestroy · [#1522](https://github.com/Tencent-TDS/KuiklyUI/pull/1522)
- fix(android): no need to stop animate when Pager dragEnd is sync · [#1527](https://github.com/Tencent-TDS/KuiklyUI/pull/1527)

### 鸿蒙

- fix(ohos): use NODE_TEXT_AREA_* attrs for TextArea and support real s… · [#1506](https://github.com/Tencent-TDS/KuiklyUI/pull/1506)
- fix(ohos): use weak_ptr in animation callbacks to prevent UAF crash · [#1509](https://github.com/Tencent-TDS/KuiklyUI/pull/1509)
- perf(ohos): optimize bridge call performance and add stress test page · [#1512](https://github.com/Tencent-TDS/KuiklyUI/pull/1512)
- fix(ohos): clear sent_start_event in HandleClearSelection · [#1515](https://github.com/Tencent-TDS/KuiklyUI/pull/1515)
- refactor(ohos): remove RunWithFatalGuard to preserve K/N unhandled-exception hook · [#1516](https://github.com/Tencent-TDS/KuiklyUI/pull/1516)
- fix(ohos): align CreateSelection event dispatch with Android semantics · [#1517](https://github.com/Tencent-TDS/KuiklyUI/pull/1517)
- fix(ohos): use default pager spring for damping one · [#1528](https://github.com/Tencent-TDS/KuiklyUI/pull/1528)

### Compose

- fix(iOS): gate TextArea auto-focus on programmatic text sync · [#1502](https://github.com/Tencent-TDS/KuiklyUI/pull/1502)
- fix(compose): coalesce semantics updates per frame to prevent fling white screen · [#1523](https://github.com/Tencent-TDS/KuiklyUI/pull/1523)
- chore(ohos): bump compose runtime deps to 1.7.3-kuikly2 · [#1525](https://github.com/Tencent-TDS/KuiklyUI/pull/1525)

### 多端

- feat(demo): Compose 视频/蒙层横竖屏 Demo · [#1518](https://github.com/Tencent-TDS/KuiklyUI/pull/1518)

---

## Part 3 破坏性变更

## 条件性行为变化

### iOS / 鸿蒙原始 touch 回调的冒泡行为

- 结果：当前节点已处理 `touchDown`、`touchMove`、`touchUp` 或 `touchCancel` 后，不再继续向父节点传播同一 action。
- 影响：父子节点同时注册原始 touch 回调，且业务依赖子节点处理后父节点再次处理的场景。
- 迁移：把一次触摸只应执行一次的逻辑放到实际交互节点，不要依赖重复冒泡。
- 说明文档：[View 触摸事件](../../API/components/view.md)
