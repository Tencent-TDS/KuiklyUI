# 鸿蒙 Kuikly 内存泄漏复现 SOP

面向「反复进出 Kuikly 页面 → 字符串对象泄漏」这个现象。
目标：先确认现象是否成立，再定位是谁持有。

## 1. LeakWatcher 到底怎么通知

`jsLeakWatcher` 有两条路，通知形式完全不同：

| 方式 | 开启 | 通知形式 | 谁触发 |
| --- | --- | --- | --- |
| 方式二（默认，自动监控） | `enableLeakWatcher(true, configs, cb)` | 系统内部周期性检测，泄漏数达到阈值后**自动 dump 并通过 `cb(files)` 回调**，`files[0]`=`.jsleaklist`，`files[1]`=堆快照 | 系统 |
| 方式一（定点排查） | `enable(true)` + `watch(obj,msg)` + `check()` | `check()` 返回疑似泄漏对象的 JSON 字符串，**不会自动通知**，要自己调 | 你 |

所以「有人说有泄漏 LeakWatcherManager 就会通知」只对方式二成立，而且有三个前提：

1. 是 DEBUG 包（`BuildProfile.DEBUG`，release 全部 no-op）。
2. 设备支持 `SystemCapability.HiviewDFX.HiChecker`。
3. 泄漏对象类型在监控范围内。方式二只监控组件类对象（CustomComponent / Window / NodeContainer / XComponent / Ability）。
   **普通字符串对象不在方式二的监控范围内**，这就是「字符串泄漏但看不到通知」的直接原因。

另外方式二默认是「达到阈值才回调一次」（前台 5 个 / 后台 1 个），不是每次泄漏都通知。

### 为什么以前看不到

原实现里回调只做了一件事：`WLog.i(...)` 打 hilog。没有回调订阅、没有界面反馈，
所以除了过滤日志之外没有任何可观测点。现在改成：

- `LeakWatcherManager.setLeakListener(cb)` 订阅通知，两种方式统一走 `dispatch()`。
- `LeakWatcherManager.getLastReport()` 拿到最近一次报告。
- 复现台订阅后直接在界面上显示 `通知：...`，并保留 hilog。

过滤日志：

```bash
hdc shell hilog | grep -i LeakWatcher
```

## 2. 复现步骤

进入 `pages/LeakReproPage`（已在 `main_pages.json` 注册，也在 `AppKRRouterAdapter` 里挂了 `LeakRepro` 路由）。

1. 冷启动 app，进复现台，界面会记一条基线（泄漏数 + ArkTS 堆）。
2. 点「自动往返 10 轮」（或 30 轮）。每轮：进 Kuikly 页面 → 停留 1.5s 等首帧 → 自动返回 →
   等 500ms 让 GC 调度 → 采样。
3. 看三条曲线：
   - `疑似泄漏对象`：手动模式下才有数（自动模式是 -1）。
   - `ArkTS 堆`：字符串泄漏最直接的信号，看是否单调增长。
   - `通知`：方式二真触发时这里会更新，并带出 dump 出的文件路径。

注意：ArkTS 堆短期上涨不等于泄漏，必须看「多轮后 GC 完是否回落」。趋势里每轮都带 `(+delta)`，
回落过的轮次会自己显出来。

## 3. 抓 rawheap 并分析

两种拿 rawheap 的方式：

```bash
# A. 命令行主动抓（需要 pid）
hdc shell pidof com.tencent.kuiklyohosdemo
hdc shell "hidumper --mem-jsheap <pid> -T <tid> --raw"
hdc file recv <device-path> ./rawheap/

# B. app 内抓（复现台「抓 rawheap」按钮，走 hidebug.dumpJsRawHeapData）
# 路径会打在 hilog 里：[LeakRepro] rawheap path=...
# 注意有系统配额限制，失败就是超配额，过一会再试
```

转成 heapsnapshot 再用 Chrome/DevEco 看：

```bash
rawheap_translator xxx.rawheap xxx.heapsnapshot
```

`rawheap_translator` 在
`/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/`。

用 mem-analyzer（已装在 `/Users/yukiykchen/Documents/mem-analyzer/`）：

```bash
cd /Users/yukiykchen/Documents/mem-analyzer

# 单文件分析，Top 20 引用链，自动打开报告
./mem-analyzer jsleak -n 20 -o xxx.rawheap

# 有 leaklist 时一起给，能定位到具体泄漏对象
./mem-analyzer jsleak -n 20 -l xxx.jsleaklist -o xxx.rawheap

# 最有用：拿第 1 轮的快照当基线，和第 N 轮比，直接看多出来什么
./mem-analyzer jsleak -b round01.rawheap -n 20 -o round30.rawheap
```

`-b` 的基线对比是定位「反复进出累积了什么」最快的路径：先看第 30 轮比第 1 轮多了哪些对象，
再顺着引用链找持有者。

## 4. 判断是不是真泄漏

按这个顺序排除：

1. ArkTS 堆（复现台曲线）多轮后是否回落。不回落 → 可疑。
2. rawheap 基线对比里，String 相关对象数是否随轮次增长。
3. 增长的话看持有链：通常是全局 Map/回调/监听没解绑，
   或者 KMP 侧对象被 ArkTS 侧长期持有。
4. native 侧另有判据，用 `kuikly-leak.sh leaks` 抓 nativeleaks（不可达内存才是真泄漏，
   native heap Rss 上涨不代表泄漏）。

## 5. 相关文件

- [LeakWatcherManager.ets](/Users/yukiykchen/pr/KuiklyUI/ohosApp/entry/src/main/ets/kuikly/LeakWatcherManager.ets)：开关、通知分发、rawheap 导出
- [LeakReproPage.ets](/Users/yukiykchen/pr/KuiklyUI/ohosApp/entry/src/main/ets/pages/LeakReproPage.ets)：复现台界面与轮次驱动
- [EntryAbilityStage.ets](/Users/yukiykchen/pr/KuiklyUI/ohosApp/entry/src/main/ets/entryability/EntryAbilityStage.ets)：MMKV 后 init

## 6. 实测结论（2026-11-03，Mate 60 Pro，pid 45170）

### 现象成立

反复进出 `router` 页面，ArkTS 堆单调增长，每轮 +85~100 KB：

| | 第 1 轮 | 第 10 轮 | 第 20 轮 | 第 30 轮 |
| --- | --- | --- | --- | --- |
| 第一批（10 轮） | 3896 | 4805 | — | — |
| 第二批（30 轮） | 8787 | 9578 | 9405 | 10279 |

第二批 30 轮总增长 1492 KB。第 14 轮出现过唯一一次回落（-1103 KB），
说明 GC 是能回收一部分的，但**回收完的基线仍在每轮 +85 KB 往上抬**。
去掉回落段看：第 15-30 轮平均 +95.1 KB/轮，无任何收敛迹象。

### 方式二确实会通知

`11:55:38` 系统回调触发，dump 出真实文件：

```
jsleakwatcher-45170-45170-1793678137651.jsleaklist   (868 B)
jsleakwatcher-45170-45170-1793678137651.rawheap      (17 MB)
```

路径在 `/data/app/el2/100/base/com.tencent.kuiklyohosdemo/files/jsleak/`。
注意 `hdc file recv` 拉不动（权限不够），要用
`mem-analyzer profiler <bundle> -c snapshot` 代拉，它会走 hidumper 拿权限。

### 增长不在字符串

堆快照（163932 节点）按类型统计：

```
native     60683 个  2961.4 KB
array      15840 个  2691.8 KB
closure    17182 个  2181.2 KB
framework  13114 个  1672.6 KB
string     26333 个   466.3 KB   <-- 只占 4%
```

字符串去重后 19122 个，**除引擎常量外无任何异常重复**。
7211 个空字符串是引擎的 string size 表（name 为空、self_size 存长度），不是业务对象。

「字符串对象泄漏」这个说法在堆数据里不成立。每轮 +85 KB 的大头在
native（60683 个，2.9 MB）和 closure（17182 个，2.2 MB）。

### 下一步该查什么

1. closure 17182 个：每轮进出都可能新建闭包，如果挂在全局监听/回调上就不会释放。
   这是 Kuikly 页面反复创建销毁场景里最常见的泄漏源。
2. native 60683 个：ArkTS 侧看不出持有者，要查 native。
   跑 `kuikly-leak.sh leaks 120` 抓 nativeleaks（不可达内存才是真泄漏判据）。
3. 跨轮次 diff 才是硬证据：需要「第 N 轮抓一次 → 跑 10 轮 → 再抓一次」的两份快照，
   目前拿到的两份只隔 6 秒，没有进出动作，diff 无意义。

## 7. 根因：native 侧 ShaderEffect 泄漏（已修）

### 证据

`mem-analyzer profiler -c nativeleaks -t 150` 抓到孤儿内存（不可达 = 真泄漏），
符号化后 Top 1 泄漏栈（75600 B / 900 次）：

```
libnative_drawing_ndk.z.so+0x4ecb0            <- 分配点
  KRGradientRichTextShadow::DidBuildTextStyle(OH_Drawing_TextStyle*, double)
    KRRichTextShadow::BuildTextTypography(double, double)
      ...
        com.tencent.kuikly.core.views.shadow.TextShadow#calculateRenderViewSize
          com.tencent.kuikly.demo.pages.base.extension.ExtTextView#measure
```

900 次分配 = 每轮进出页面排版一次就泄漏一批。Top 4 里有 2 条都落在
`KRGradientRichTextShadow::DidBuildTextStyle`，另一条在
`KRRichTextView::OnForegroundDraw`。

### 原因

`OH_Drawing_ShaderEffectCreateLinearGradient` 创建的对象，在通过
`OH_Drawing_BrushSetShaderEffect` 交给 brush 之后，调用方仍需调用
`OH_Drawing_ShaderEffectDestroy` 交出自己的那份所有权。有三处漏了：

- `KRGradientRichTextShadow::DidBuildTextStyle`（主泄漏点）
- `KRRichTextShadow::BuildTextTypography`（`colorShaderEffect` 到循环结束都没释放）
- `KRCanvasView::SetStrokeStyle` / `SetFillStyle`（pen_/brush_ 是长生命周期成员）

`KRParagraph.cpp` 里原本就有正确实现，但它是文件内 static 函数，其他组件用不到，
所以另外三处各自漏了。

### 修复

新增 [KRShaderEffectUtil.h](/Users/yukiykchen/pr/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/utils/KRShaderEffectUtil.h)，
把安全释放逻辑提成共享 inline 函数 `kuikly::util::KRSafeDestroyShaderEffect`：

```cpp
inline void KRSafeDestroyShaderEffect(OH_Drawing_ShaderEffect *shaderEffect) {
    if (shaderEffect == nullptr) {
        return;
    }
    if (OH_GetSdkApiVersion() >= SHADER_EFFECT_DESTROY_API_LEVEL) {  // 19
        OH_Drawing_ShaderEffectDestroy(shaderEffect);
    }
}
```

API 门槛 19 是必须的：`OH_Drawing_ShaderEffectDestroy` 在 api 13、17 的设备上
会在绘制阶段崩溃（`lib2d_graphics.z.so` 的 `ColorFilter::Serialize()`），
这个坑是 `KRParagraph.cpp` 原注释里记录的，不能去掉。
低版本上仍然是 noop + 泄漏，但比崩溃可接受。

调用点：

- [KRGradientRichTextShadow.cpp](/Users/yukiykchen/pr/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/gradient_richtext/KRGradientRichTextShadow.cpp)
- [KRRichTextShadow.cpp](/Users/yukiykchen/pr/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRRichTextShadow.cpp)
- [KRCanvasView.cpp](/Users/yukiykchen/pr/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/canvas/KRCanvasView.cpp)
- [KRParagraph.cpp](/Users/yukiykchen/pr/KuiklyUI/core-render-ohos/src/main/cpp/libohos_render/expand/components/richtext/KRParagraph.cpp)：改为复用共享 helper

### 验证状态

C++ 侧 4 个文件 ninja 编译通过。尚未在设备上复测——
需要先解决签名证书过期（`SignHap` 报错 `NotAfter: Nov 02 2026`，
叶子调试证书全部过期，要在 DevEco 里重新生成签名材料）。

复测方法：装上新包后跑 30 轮，看 ArkTS 堆谷底是否仍然逐次抬高，
以及 native heap 是否稳定。

## 8. 复测结果（14:13 新包，pid 14565）与结论修正

### 复测数据

```
基线 3836 KB → 第 30 轮 9316 KB   平均 +182.7 KB/轮（含第 25 轮 +2643 的跳变）
单轮常态增量：+85~91 KB
```

nativeleaks 泄漏栈：**10 个 → 3 个**

| 泄漏点 | 修复前 | 修复后 |
| --- | --- | --- |
| `KRGradientRichTextShadow::DidBuildTextStyle` | 75600 B / 900 次 | **消失** |
| `KRRichTextView::OnForegroundDraw` | 3360 B / 210 次 | 2464 B / 154 次 |
| `OnInitRenderView` | — | 103352 B / 442 次（占 0.49%） |

ShaderEffect 修复确实生效（主泄漏点已消失）。

### 但「+85 KB/轮」不是 ShaderEffect 造成的

上一轮我用「修复后 30 轮 6690 KB vs 修复前 10279 KB」判定修复生效，那个对比**无效**：
两个进程基线不同（3836 vs 8787），峰值不可比。

同一进程内的单轮增量才是有效判据：

```
修复前  +84~90 KB/轮
修复后  +85~91 KB/轮   ← 完全一样
```

而且量级对不上：75600 B 摊到 900 次分配，每次仅 84 字节。
900 是分配**次数**不是**字节**，我当时看到它跟轮次吻合就认定是主因，推理有误。

### 当前结论

native 侧：native heap 静止时从 54 MB 自行回落到 36.4 MB 并稳定，
**不随轮次累积**。原生泄漏（ShaderEffect 那批）已修掉，剩余量级很小。

ArkTS 堆的 +85 KB/轮：更像正常的堆增长-GC 循环。判据是**谷底**而非峰值——
这批 30 轮里无回落，是因为没跑到 GC 阈值就结束了（9316 KB < 约 10200 的触发点）。

**尚未定论。** 需要跑 100 轮或每 10 轮静置 30 秒后再采样，看谷底是否逐次抬高。

### 已知遗留泄漏（未修）

`KRRenderView::native_resources_manager_`：由 `napi_init.cpp` 的 `OnInitRenderView`
每次进入页面时 `OH_ResourceManager_InitNativeResourceManager` 新建，析构时只置空未释放。

实测仅 103 KB / 442 次（占 app native 总量 0.49%），收益很小。
**暂不释放**的原因：`KRImageLoadOption` 等处以裸指针持有该 manager，
而图片解码存在异步路径（`KRCustomEmojiPixmapCache::ImageLoadedCallback`），
若在 view 析构后仍被访问会变成悬垂指针导致随机崩溃。
要安全修复需先把持有方改成 weak/shared 所有权，属于独立改造，已在代码注释中标注。
