# Tasks: 鸿蒙 Native Vsync 帧驱动

## 1. 探索与基线

- [x] 1.1 阅读 upstream PR #1520 全部讨论，确认本地 main 与其基线的 API 差异（`registerVsync(Long)` 时间戳 → 本地 `registerVsyncWithFrameInterval(Int)` 帧间隔）
- [x] 1.2 核实本地现状：鸿蒙走 `ComposeSceneMediator` 12ms Timer（`ComposeContainer.kt` isOhOs 分支），native 无 Vsync 模块
- [x] 1.3 摸清桥接链路：`KRRenderValue` int32 编码 → napi `Type::INT` → Kotlin `Int` 类型链；`KRRenderCallback` 内部自动切 context 线程（`KRRenderCore.cpp:345-355`）；`KRContextScheduler::ScheduleTask` 与帧任务同队列
- [x] 1.4 建设帧节拍观测设施（`FrameTickMonitor` + `renderFrame` 埋点），真机采集 12ms Timer 基线（p50=12.1~12.8ms，est_fps 78~83 封顶）

## 2. Native 模块（发动机本体）

- [x] 2.1 新增 `core-render-ohos/.../expand/modules/vsync/KRVsyncModule.h/.cpp`：`registerVsync`/`unRegisterVsync`、每帧重 arm、帧间隔时间戳差值 + [1ms,100ms] 钳制、int32 上报
- [x] 2.2 生命周期防护：`weak_ptr` + `generation` 丢弃在途回调；析构/`OnDestroy` 销毁 `OH_NativeVSync`
- [x] 2.3 背压：`tick_pending_` 原子标志 + `ScheduleTask(0)` FIFO 排队复位
- [x] 2.4 `ModulesRegisterEntry.h` 注册模块；`CMakeLists.txt` 追加编译单元

## 3. Kotlin 侧切换

- [x] 3.1 `ComposeContainer.startFrameDispatcher`：鸿蒙分支切换到 `registerVsyncWithFrameInterval` 路径（帧间隔透传给 `renderFrame`）
- [x] 3.2 watchdog 兜底：100ms 无首个 tick → `unRegisterVsync` + 退回 12ms Timer + 降级日志（旧宿主兼容）
- [x] 3.3 `ohosUseNativeVsync` 静态 A/B 开关（默认 true；false 回到 12ms Timer 旧行为）
- [x] 3.4 修复 `stopFrameDispatcher` 鸿蒙空实现：补 `unRegisterVsync` + 取消 watchdog / 兜底 Timer

## 4. 验证与收尾

- [x] 4.1 全链路编译：compose / ohos Kotlin so / hvigor hap 通过；`libkuikly.so` 确认含 `KRVsyncModule` 符号
- [x] 4.2 真机 A/B 对比（LazyColumnDemo5 / VerticalPagerDemo / 业务 Compose 页面）：p50 12.1→8.3ms，est_fps 82→120，`driverReportedInterval` n/a→8.28ms，稳态 jank 大幅下降
- [x] 4.3 验证 LTPO 变频自适应：静止/低负载时帧间隔自动跟随屏幕 16.57ms（60Hz），操作时回 8.28ms（120Hz）
- [x] 4.4 验证开关与兜底：A/B 开关切换生效；无 fallback 误触发；页面退出无 crash
- [x] 4.5 清理验证期日志设施（`FrameTickMonitor`、perfAPI 周期采样），保留 watchdog 降级日志
- [x] 4.6 确认提交清单（5 个框架文件）；performance 观测链（KRFrameData/KRPerformanceData/PerformanceModule/delegate）另行提交
