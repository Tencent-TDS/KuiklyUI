# Compose 视频横竖屏 Demo — 交接文档

> 最后更新：2026-06-25  
> 用途：新会话接续 `ComposeVideoOrientationDemo` 开发与真机验证

---

## 1. 工作环境

| 项 | 值 |
|---|---|
| **工作区完整路径** | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg` |
| **当前分支** | `cursor/f5289c9f`（基于 `main` 的 worktree，**未提交**） |
| **DSL 类型** | **Compose DSL**（`ComposeContainer` / `setContent`） |
| **页面名** | `ComposeVideoOrientationDemo` |
| **对话记录** | `/Users/qibu/.cursor/projects/Users-qibu-cursor-worktrees-KuiklyUI-ztkg/agent-transcripts/84b481de-1df7-4322-9994-407864bf9454/84b481de-1df7-4322-9994-407864bf9454.jsonl` |

---

## 2. 需求目标

实现一个 **Compose DSL** 示例页：顶部视频 + 评论列表，点击「横屏」进入全屏播放，系统负责转屏，UI 用黑色蒙层 + `movableContentOf` 移动视频，返回时过渡尽量自然。

**硬性约束（已遵守）：**

- 不用 `Dialog`，只用 `Box`
- 视频和蒙层**不旋转**（旋转交给系统）
- 点击横屏时**先 `requestLandscape()`，再显示蒙层**（与放大动画并行）
- 返回时：冻结横屏布局 → `requestPortrait()` → **100ms** 后再卸蒙层

---

## 3. 核心实现逻辑（Kotlin 共享层）

### 3.1 主文件

```
/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeVideoOrientationDemo.kt
```

> 状态：**新建，未 `git add`**

### 3.2 关键常量

```kotlin
FoldableExpandedMinWidth = 600.dp      // 折叠屏展开判断（同步，不用 LaunchedEffect）
VIDEO_EXPAND_ANIM_MS = 360             // 进入全屏视频放大动画
PORTRAIT_HOLD_MS = 100L                // 返回时蒙层冻结时长
PortraitVideoHeaderHeight = 220.dp
VIDEO_DEBUG_TAG = "BD_VideoOrientation"
```

### 3.3 普通手机流程

1. 点「横屏」→ `bridgeModule.requestLandscape()` + `showFullscreenOverlay = true`
2. `PhoneFullscreenHoldOverlay`（约 283–334 行）：
   - 外层 `fillMaxSize()` 黑蒙层跟随窗口
   - 内层按窗口尺寸布局
   - 视频 360ms 居中放大（16:9 长边撑满）
3. 点返回 → 记录 `FrozenOverlayLayout` → `shouldRequestPortrait = true` → `requestPortrait()` → 冻结 100ms → 卸蒙层

### 3.4 折叠屏流程

- `isFoldableExpanded = maxWidth >= 600.dp`（同步）
- 展开态主页面 `fillMaxSize()`，左右分栏（左视频+评论，右推荐列表）
- 全屏用 `FoldableFullscreenOverlay`（不转屏，只放大视频）

### 3.5 Bridge 调用链

```
ComposeVideoOrientationDemo
  → BridgeModule.requestLandscape() / requestPortrait()
  → callNativeMethod("requestOrientation", { orientation: "landscape"|"portrait" })
  → 各平台 KRBridgeModule 原生实现
```

**Bridge 定义：**

```
/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/base/BridgeModule.kt
```

**入口注册（ComposeAllSample 列表）：**

```
/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeAllSample.kt
```

条目：`DemoItem("视频横竖屏", "MovableContent视频横竖屏切换示例", "ComposeVideoOrientationDemo")`

---

## 4. 各平台改动与状态

### 4.1 Android — 已验证效果较好

| 文件 | 完整路径 | 改动 |
|------|----------|------|
| KRBridgeModule | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/androidApp/src/main/java/com/tencent/kuikly/android/demo/module/KRBridgeModule.kt` | 新增 `requestOrientation` → `activity.requestedOrientation` |
| activity 布局 | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/androidApp/src/main/res/layout/activity_hr.xml` | 根布局 + `hr_container` 背景改 `#000000`（原黄色调试用） |
| VideoViewAdapter | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/androidApp/src/main/java/com/tencent/kuikly/android/demo/adapter/VideoViewAdapter.kt` | 有改动（与视频播放相关） |

**真机 ID：** `10AEBJ0D55002N7`

```bash
cd /Users/qibu/.cursor/worktrees/KuiklyUI/ztkg
./gradlew :androidApp:assembleDebug
adb -s 10AEBJ0D55002N7 install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s 10AEBJ0D55002N7 shell am start -n com.tencent.kuikly.android.demo/.KuiklyRenderActivity --es pageName ComposeVideoOrientationDemo
```

### 4.2 HarmonyOS — 转屏已补实现，待真机验证

**问题根因：** 鸿蒙 `KRBridgeModule.ets` 原先**没有** `requestOrientation` 实现，Kotlin 调用无响应。

**已修复：**

| 文件 | 完整路径 | 改动 |
|------|----------|------|
| KRBridgeModule | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/ohosApp/entry/src/main/ets/kuikly/modules/KRBridgeModule.ets` | 新增 `REQUEST_ORIENTATION` + `setPreferredOrientation(LANDSCAPE/PORTRAIT)` |
| module.json5 | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/ohosApp/entry/src/main/module.json5` | `EntryAbility` 增加 `"orientation": "auto_rotation_restricted"` |
| 签名配置 | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/ohosApp/build-profile.json5` | DevEco 自动写入 `signingConfigs`（**勿提交密钥**） |
| bundleName | `/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/ohosApp/AppScope/app.json5` | `com.tencent.kuiklyohosdemo.luoyibu` |

**鸿蒙转屏实现要点（`KRBridgeModule.ets`）：**

```typescript
// landscape → window.Orientation.LANDSCAPE
// portrait  → window.Orientation.PORTRAIT
context.windowStage.getMainWindowSync().setPreferredOrientation(...)
// fallback: window.getLastWindow(getContext(), ...)
```

**真机 ID：** `FMR0223C13000246`

```bash
cd /Users/qibu/.cursor/worktrees/KuiklyUI/ztkg

# 1. 编 KMP shared（demo 有改动时必须重编）
./2.0_ohos_demo_build.sh

# 2. 编 HAP
export DEVECO_SDK_HOME="/Applications/DevEco-Studio.app/Contents/sdk"
export PATH="/Applications/DevEco-Studio.app/Contents/tools/ohpm/bin:/Applications/DevEco-Studio.app/Contents/tools/hvigor/bin:/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains:$PATH"
export NODE_HOME="$HOME/.nvm/versions/node/v24.12.0"
cd ohosApp && hvigorw assembleHap --mode module -p module=entry@default -p product=default -p buildMode=debug --no-daemon

# 3. 安装启动
hdc -t FMR0223C13000246 install -r entry/build/default/outputs/default/entry-default-signed.hap
hdc -t FMR0223C13000246 shell aa force-stop com.tencent.kuiklyohosdemo.luoyibu
hdc -t FMR0223C13000246 shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo.luoyibu --ps pageName ComposeVideoOrientationDemo
```

**进 Demo 路径：** 路由页 → `ComposeAllSample` → 「视频横竖屏」  
（冷启动 `--ps pageName` 不一定能直达 Kuikly 页，`Index.ets` 用 `router.getParams()`）

**鸿蒙入口相关：**

```
/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/ohosApp/entry/src/main/ets/pages/Index.ets
/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/ohosApp/entry/src/main/ets/entryability/EntryAbility.ets
/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/ohosApp/entry/src/main/ets/kuikly/KuiklyViewDelegate.ets
/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/ohosApp/entry/src/main/ets/kuikly/adapters/AppKRVideoAdapter.ets
```

**render 层（只读参考，本次未改）：**

```
/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg/core-render-ohos/
```

### 4.3 iOS / H5

未开始。`requestOrientation` 需各平台 Bridge 分别实现。

---

## 5. 已知问题与技术结论

| 问题 | 结论 |
|------|------|
| 转屏时右侧露黄/视频跳左上角 | 根因：系统 Window 先变横屏，Compose 约束更新滞后；方案：蒙层 `fillMaxSize()` 跟随窗口 + 退出冻结布局；Activity 背景改黑 |
| 折叠屏展开布局延迟 | 部分来自 `LaunchedEffect` 异步（已改同步 `maxWidth >= 600.dp`）；若仍有延迟可能是 Pager 下发 `activityWidth` 慢 |
| 鸿蒙不转屏 | **已补** `requestOrientation`；**待用户确认**修复后真机效果 |
| 视频播放异常 | 蒙层/动画/多次 `playControl` 可能触发；Android 上已改善；鸿蒙待验 |

**调试日志：**

```bash
# Android
adb logcat | grep -iE "BD_VideoOrientation|KuiklyRender"

# 鸿蒙
hdc -t FMR0223C13000246 shell hilog | grep -iE "BD_VideoOrientation|requestOrientation|KuiklyRender"
```

---

## 6. Git 状态（交接时快照）

**分支：** `cursor/f5289c9f`，**全部未提交**

**已修改（tracked）：**

- `androidApp/src/main/java/com/tencent/kuikly/android/demo/adapter/VideoViewAdapter.kt`
- `androidApp/src/main/java/com/tencent/kuikly/android/demo/module/KRBridgeModule.kt`
- `androidApp/src/main/res/layout/activity_hr.xml`
- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/base/BridgeModule.kt`
- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeAllSample.kt`
- `ohosApp/AppScope/app.json5`
- `ohosApp/build-profile.json5`（含签名 material 路径）
- `ohosApp/entry/oh-package-lock.json5`
- `ohosApp/entry/src/main/ets/kuikly/modules/KRBridgeModule.ets`
- `ohosApp/entry/src/main/module.json5`

**未跟踪（需 `git add`）：**

- `demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/ComposeVideoOrientationDemo.kt`（主 Demo 文件，~736 行）

---

## 7. 新会话建议接续步骤

1. **确认鸿蒙转屏**：真机点「横屏」，看是否转屏 + 蒙层动画是否正常；不行则查 `hilog` 里 `requestOrientation` 报错
2. **若转屏 OK、动画有问题**：继续调 `PhoneFullscreenHoldOverlay`（`ComposeVideoOrientationDemo.kt` 约 283–334 行）
3. **若鸿蒙视频不播**：查 `AppKRVideoAdapter.ets`
4. **iOS / H5**：未开始；`requestOrientation` 需各平台 Bridge 分别实现
5. **提交前**：`git add ComposeVideoOrientationDemo.kt`；检查 `build-profile.json5` 是否应 gitignore 签名段

---

## 8. 新会话 Prompt 模板

```
工作区：/Users/qibu/.cursor/worktrees/KuiklyUI/ztkg
分支：cursor/f5289c9f（未提交）
交接文档：./devdocs/compose-video-orientation-handoff.md

继续 ComposeVideoOrientationDemo 横竖屏 Demo：
- 主文件：demo/src/commonMain/kotlin/.../ComposeVideoOrientationDemo.kt
- Android 已验；鸿蒙刚补 requestOrientation（KRBridgeModule.ets + module.json5 orientation）
- 真机：Android 10AEBJ0D55002N7，鸿蒙 FMR0223C13000246
- 请先验证鸿蒙转屏效果，再处理动画/视频播放问题
```
