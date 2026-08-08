# GitHub Actions CI 失败复盘（发布器 E2E 模拟器冒烟，2026-08-07 ~ 08-08）

> 姊妹篇：`工蜂CI失败复盘.md`（vivo 真机线，15 commit / 13 问题）。
> 该文档为个人工作记录，不入库，存于 `~/Desktop/KuiklyUI-个人文档/`。
> 两份文档互不覆盖：工蜂那份讲真机与脚本编排；本文讲 GitHub 托管 runner 的云端环境坑。

## 精简版

- **目标**：让「发布器 @人 E2E 回归（TC1-TC10）」在 GitHub Actions `ubuntu-latest` 模拟器上无人值守全绿。
- **结果**：18 个 commit 后收敛。连续 3 轮 `PASS=10 FAIL=0 exit 0`，单轮 **4m24s ~ 10m29s**（此前 30-60min 且几乎必红）。
- **核心结论**：前 7 轮红的失败点**每轮都不一样**，看起来是 7 个独立问题，实际是**同一个总根因的 7 个影子**——
  `ubuntu-latest` 的 runner 用户**没有 `/dev/kvm` 权限**，模拟器回落到纯软件模拟（TCG），整机慢约 10 倍，
  于是每轮都在**不同位置**撞到**不同的超时**。
- **最贵的一课**：**症状每轮都变，是在提示存在未识别的共同根因**。
  连续 5 轮在不同位置撞超时还在逐个放宽超时，就是在打影子。
  正确的反射不是「这个超时该设多大」，而是「**为什么这台机器做什么都慢**」。

---

## 1. 背景

- 被测对象与工蜂线相同：`MentionPublisherDemo`（Compose @人发布器），`demo/mention_publisher_e2e_test.sh` 跑 TC1-TC10。
- 与工蜂线的关键差异：

  | | 工蜂（真机线） | GitHub（模拟器线） |
  |---|---|---|
  | 执行环境 | 本机 Mac runner + vivo V2141A | 云端 `ubuntu-latest` + AVD |
  | 设备特性 | uia2 冷启动首次查询必 hang | 冷模拟器整机慢（无硬件加速时） |
  | 可观测性 | 本机 `/tmp` 日志，AI 读不到 | `gh run view --log` 直读，AI 全可见 |
  | 额外风险层 | 无 | **action 内部命令 + runner 进程组清理（黑盒）** |

- 承接文件：`.github/workflows/e2e.yml`（workflow）、`ci/run-e2e-linux.sh`（跨平台启动器）、
  `demo/e2e-engine/src/appium-mobile-driver.ts`（Appium 桥）。

---

## 2. 失败全景（按 run 序，13 轮）

### 2.1 影子期：run #1-#7（每轮症状不同，实为同一根因）

| run | 表面症状 | 当时的修法 | 实际是什么 |
|---|---|---|---|
| #1/#2 | `adb install` ShellCommandUnresponsiveException / exit 224 | 装包与构建解耦 + 重试（`7ce586f5`） | 软件模拟下装包超时 |
| #3 | `POST /session` 超过 WebDriverIO 默认 120s | 放宽到 300s（`c49175be`） | 同上 |
| #4 | `hidden_api_policy` adb shell 20s 超时 | 加 ignoreHiddenApiPolicyError（`cd0a3e6c`） | 同上 |
| #5 | `window_animation_scale` Broken pipe，脚本没机会跑 | `disable-animations: false`（`f300c9e4`） | 同上 |
| #6 | （被 cancel，未暴露新症状） | — | — |
| #7 | uia2 instrumentation 60s 起不来 | 放宽到 180s（`8372f212`） | 同上 |

**七轮下来每次都"定位到了、也修对了症状"，但总根因始终没被识别。**

### 2.2 总根因（三轮日志铁证，run #4/#6/#7 全部命中同一段）

```
ProbeKVM: This user doesn't have permissions to use KVM (/dev/kvm).
The KVM line in /etc/group is: [kvm:x:993:]     ← 组存在，但 runner 用户不在组里
Disabling Linux hardware acceleration.
WARNING | x86_64 emulation may not work without hardware acceleration!
```

`ubuntu-latest`(24.04) 的 `/dev/kvm` udev 规则默认不给 runner 用户权限 →
`reactivecircus/android-emulator-runner` 回落纯软件模拟（TCG）→ 整机慢约 10 倍。

**修复**（`d48b14be`）：起模拟器**之前**加一步放开权限。

```yaml
- name: 开启 KVM 硬件加速
  run: |
    echo 'KERNEL=="kvm", GROUP="kvm", MODE="0666", OPTIONS+="static_node=kvm"' \
      | sudo tee /etc/udev/rules.d/99-kvm4all.rules
    sudo udevadm control --reload-rules
    sudo udevadm trigger --name-match=kvm
    ls -l /dev/kvm
```

**效果对比**（run #7 无 KVM vs run #9 有 KVM）：

| 阶段 | 无 KVM | 有 KVM |
|---|---|---|
| boot | 374s | **30s** |
| adb install | 3m26s | **<1s** |
| start-session | 3 次全失败（各 4min） | **6s，一次成功** |
| 首开 | 从未成功 | **一次成功，不走恢复分支** |

生效判据：`crw-rw-rw- 1 root kvm /dev/kvm` + `disable Linux hardware acceleration: false`。

> **副作用（正面）**：工蜂线花大力气做的恢复分支 / 健康探测，在有硬件加速后**根本不会被触发**——
> 首开零失败不是靠恢复救回来的，是根本没失败。

### 2.3 真实问题期：run #9/#10（KVM 生效后首次真正跑完 TC1-TC10）

两轮独立复现同一结果：**PASS=7 FAIL=3**（TC4 / TC7 / TC8）。这是**第一次拿到干净的业务级失败**。

**定位**：三个用例都依赖 `debug_delete_state` / `debug_inject_张`。
这两个节点在整份日志的 view-tree dump 里**一次都没出现**，而同区域前 4 个
（`debug_text` / `debug_selection` / `debug_mentions` / `debug_trigger`）都正常渲染，
最后一个 `debug_trigger` bounds=`[16,401][103,417]`。

**根因**：日志 `INFO | qemu.skin=320x640` —— **不指定 profile 时默认 AVD 是极小屏**。
发布器页外层 `Column` **不可滚动**，按 dp 排下来（top padding 32 + 标题 + 输入框 min 120dp
+ 若干 Spacer + 6 个调试 Text）超出 640px 可用高度，最后两个节点被挤出屏幕、
**不进 accessibility 树** → `assert_node_contains` / `tap` 必然失败。

**佐证非业务回归**：源码 `MentionPublisherDemo.kt:237,245` 两节点自 `dc8b4733` 起一直存在；
vivo 真机屏幕大得多，工蜂同版本 PASS=10。属**模拟器屏幕尺寸的环境差异**。

**修复**（`394d21be`）：`profile: pixel_5`（1080x2340）。

### 2.4 掩盖期：run #9/#10 的 60min timeout

测试 13:50:43 就跑完了（`PASS=7 FAIL=3`，exit 1），Appium 13:50:44 已
`HTTP server has been successfully closed`，之后**静默 50 分钟**到 job timeout 被判 `cancelled`。

**根因**：`cleanup()` 里写死 `pkill -f "tail -f ..."`，而 `c49175be` 已把启动侧改成 `tail -F`。
模式不再匹配 → 两个 tail 杀不掉 → cleanup 末尾裸 `wait` 永远等 → step 不返回。

**修复**（`f1296206`）：启动 tail 时把 `$!` 记进 `TAIL_PIDS` 数组，cleanup 直接 kill PID。

> 这个 bug 最坏的地方不是浪费 50min runner 时间，而是**把「测试失败」的 conclusion
> 掩盖成「cancelled/timeout」**，严重干扰判读。

---

## 3. 排查方法论（本线新增，与工蜂那 4 条互补）

### 3.1 症状每轮都变 → 找共同根因，别逐个放宽超时（最重要）

连续 5 轮在不同位置撞超时，还在逐个放宽超时，就是在打影子。
**先查环境能力**（硬件加速 / 内存 / 磁盘 / 权限），**再查应用层参数**。

反面教材：run #1-#7 每轮都"成功定位并修复"了当轮症状，7 轮下来一轮没绿。

### 3.2 「节点不存在」先怀疑视口，再怀疑业务逻辑

查源码确认节点定义存在后，重点看 **bounds 与屏幕高度的关系**、容器是否可滚动。
UI E2E 必须**显式指定模拟器 profile**——默认 AVD 是 320x640，
不可滚动容器底部的节点会**静默消失**，现象是"节点不存在"，极易误判成业务 bug。

### 3.3 conclusion 会骗人

`cancelled` / `timeout` 可能掩盖着"测试其实早就跑完并失败了"。
排障先在日志里搜脚本自己的结论（`结果: PASS=`），别只看 job 状态。

### 3.4 改了 A 处，要搜 A 的对侧引用

`tail -f` → `tail -F` 漏改 `pkill` 模式即此类。
**`pkill -f "<写死的命令行>"` 是脆弱耦合**——改启动侧参数就会静默失效。
**能记 PID 就别用 pkill 匹配。**

### 3.5 capability 的归属要查 constraints，别照抄邻近写法

`378a293a` 把 `appium:ignoreHiddenApiPolicyError` 改成嵌套
`appium:settings[ignoreHiddenApiPolicyError]`，**反而让开关彻底失效**（是一次反向修复）。

源码三处互证：

1. `appium/build/lib/helpers/capability.js:110` `pullSettings()` —— `appium:settings[xxx]`
   会被从 caps 里 **delete**，转成 device settings，**session 建好后**才 `updateSettings` 下发；
2. `appium-uiautomator2-driver/build/lib/constraints.d.ts:243` —— 它是**驱动 capability**；
3. `.../uiautomator2-server/session.js:101` `performPreExecSetup()` 读
   `this.opts.ignoreHiddenApiPolicyError`，发生在**建 session 期间**，早于 settings 下发
   → 嵌套后永远 `undefined`，`!!undefined === false`，错误照抛。

**规则**：判断依据是 **`constraints.ts` 里有没有这个 key**——
有 = 驱动 cap 必须扁平；没有 = device setting 才嵌套。
`waitForSelectorTimeout` / `waitForIdleTimeout` 该嵌套，`ignoreHiddenApiPolicyError` 必须扁平。
**别照抄邻近 cap 的形状，也别信错误信息里的简写提示**（那句 "You could also set the
'appium:ignoreHiddenApiPolicyError' capability" 正是误导来源）。

### 3.6 治标修复要在根因修好后主动回收

`8372f212` 把 launch timeout 放宽到 180s 是根因未明时的兜底。KVM 修好后该阈值永不触发，
但**留着会把未来「instrumentation 真的变慢」这类退化静默吸收掉**——默认 60s 反而是有效告警线。
故 `96fde447` 移除注入、保留 env 覆盖能力。

> 区分标准：**纯阈值放宽**（无行为）根因修好后应回收；
> **有行为的自愈机制**（如 `a7670811` 的 preWarm / 重试 / 活性探测）无副作用且对其他环境有价值，保留。

---

## 4. 方案清单

### 4.1 正确方案（按价值排序）

1. **开启 runner KVM 权限**（`d48b14be`）—— 总根因，一改解决 7 轮症状。
2. **AVD 显式指定 profile**（`394d21be`）—— `pixel_5`，避免小屏挤掉节点。
3. **cleanup 记 PID 杀 tail**（`f1296206`）—— 消除 50min 空挂与 conclusion 污染。
4. **capability 回退扁平写法**（`fda84e88`）—— 纠正反向修复。
5. **artifact 上传 + CAPTURE_BASELINE**（`96cc2836`）—— 无头模拟器画面可见，
   TC1-TC10 每条留 1080x2340 截图，失败时另有 view-tree/page-source/logcat。
6. **装包与构建解耦 + 重试**（`7ce586f5`）—— 即使有 KVM 仍是好实践（避免 6min 构建陪着重试）。
7. **引擎层 start-session 自愈**（`a7670811`）—— 对真机线与无加速环境仍有价值。
8. **`tail -F` 抗文件竞态**（`c49175be`）—— 与 grandchild 创建日志文件竞态。

### 4.2 错误 / 被证伪方案

1. **逐个放宽超时**（run #1-#7 主线）—— 打影子，7 轮未绿。**最大教训。**
2. **`appium:settings[ignoreHiddenApiPolicyError]` 嵌套写法**（`378a293a`）—— 反向修复，让开关失效。
3. **信错误信息里的 capability 简写提示** —— 应以 `constraints.ts` 为准。
4. **`disable-animations: true`** —— action pre-script 的 `adb shell settings put` 在软件模拟下 Broken pipe，
   崩了会跳过整个 `script:`。（根因修好后其实可以恢复，但关掉无损：uia2 看 view tree 不依赖动画。）
5. **只放宽 job timeout 45→60min** —— 治标，run #9/#10 照样跑满。

---

## 5. 目前效果

| 指标 | 之前 | 现在 |
|---|---|---|
| 单轮耗时 | 30-60min（多数 timeout） | **4m24s ~ 10m29s** |
| boot | 374s | 30s |
| start-session | 3 次全败 | 6s 一次成功 |
| 首开 | 从未成功 | 一次成功，不走恢复分支 |
| TC 结果 | 从未跑完 | **PASS=10 FAIL=0，连续 3 轮** |
| 模拟器可观测性 | 无（无头且不上传） | 每 TC 一张 1080x2340 截图，artifact 保留 14 天 |

### 已知边界（诚实说明）

- **`e2e-device`（vivo 真机 job）仍是 `if: false`**，需注册 self-hosted runner + 配 `DEVICE_SERIAL` secret。
- **本文效果均基于模拟器线**；真机线的结论见工蜂那份文档。
- **KVM 修复依赖 GitHub 托管 runner 的当前镜像行为**。若未来镜像默认放开权限，
  这一步会变成 no-op（无害）；若换 runner 提供商需重新确认。
- **`profile: pixel_5` 是够用而非最优**——只要垂直空间足够即可，未做多尺寸矩阵覆盖。

---

## 6. 下一步

1. ~~清理已被 KVM 取代的治标修复~~（`96fde447` 已完成）。
2. **分支策略评估**：工蜂线与 GitHub 线是否拆分支（当前共用一个分支、靠 env 区分）。
3. **启用 `e2e-device`**：注册 self-hosted runner + 配 secret。
4. 可选：多屏幕尺寸矩阵，防止再出现"小屏挤掉节点"类问题。

---

## 7. 时间线（GitHub 线 commit 流）

| commit | 内容 | 性质 |
|---|---|---|
| `f58e6ef4` | 加 GitHub Actions E2E workflow + 跨平台启动器 | 基础 |
| `bf6f6732` | 触发覆盖 `feat/**` 分支 push | 基础 |
| `7857999d` | Gradle/Kotlin-Native 缓存 + job timeout 45→60min | 治标 |
| `7ce586f5` | 装包与构建解耦 + adb install 重试；修正 `force-avd-creation` 拼写 | 影子期 |
| `6cf36b64` | 重建后加 uia2 健康探测 + 二次 force-stop 兜底 | 影子期 |
| `c49175be` | WebDriverIO/session 超时放宽 5min；`tail -f`→`-F` | 影子期 |
| `7370138f` | 健康探测改 findElements 语义 | 影子期 |
| `cd0a3e6c` | 加 `ignoreHiddenApiPolicyError`（扁平，**本来是对的**） | 影子期 |
| `f300c9e4` | 关 `disable-animations` | 影子期 |
| `378a293a` | 改嵌套写法 —— **反向修复** | ❌ 错误 |
| `8372f212` | launch timeout 可配置，放宽 180s | 治标 |
| **`d48b14be`** | **开启 runner KVM 权限** | ✅ **总根因** |
| `fda84e88` | capability 改回扁平（回退 `378a293a`） | ✅ 纠错 |
| `a7670811` | 引擎层 start-session 自愈 | ✅ 保留 |
| `394d21be` | AVD 指定 `pixel_5` profile | ✅ 真实问题 |
| `f1296206` | cleanup 记 PID 杀 tail | ✅ 真实问题 |
| `96cc2836` | artifact 上传 + `CAPTURE_BASELINE` | ✅ 可观测性 |
| `96fde447` | 移除已被 KVM 取代的 180s 放宽 | ✅ 回收治标 |
