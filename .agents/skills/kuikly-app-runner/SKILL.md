---
name: kuikly-app-runner
description: "Build, run, and capture logs from KuiklyUI demo apps on iOS simulator, Android emulator, or HarmonyOS device. Use when asked to compile iosApp/androidApp/ohosApp, launch on simulator/emulator, capture Kotlin/Native println logs, or debug runtime behavior on any platform. Also use when encountering build failures related to CocoaPods, Gradle KMP, xcodebuild, or DevEco Studio in the KuiklyUI project."
user-invocable: true
---

# KuiklyUI App Runner

## Platform Selection

Read **only** the needed platform reference:

| Platform  | Reference               | Trigger keywords                                    |
| --------- | ----------------------- | --------------------------------------------------- |
| iOS       | `references/ios.md`     | iosApp, simulator, device, iPhone, Xcode, CocoaPods |
| Android   | `references/android.md` | androidApp, emulator, adb, assembleDebug            |
| HarmonyOS | `references/ohos.md`    | ohosApp, DevEco, libshared.so, hvigor, hdc          |

When a step fails, read the platform's troubleshooting file (e.g., `references/ios-troubleshooting.md`).

## Log Directory

All build and runtime logs are stored under **`./logs/`** (relative to the **KuiklyUI project root**, NOT the AI workspace). This prevents log file conflicts between parallel tasks in different worktrees.

**Before each full build, clean old logs and recreate the directory**:
```bash
rm -rf logs && mkdir -p logs
```

## Build Command Rules

1. **Always redirect output to a log file** — never pipe through `tail`/`grep`/`head`. If the command times out, all output is lost otherwise.
2. **Use descriptive log file names** with `kuikly_` prefix, all under `./logs/`:

| Command                | Log file                              |
| ---------------------- | ------------------------------------- |
| generateDummyFramework | `./logs/kuikly_gradle_dummy.log`      |
| pod install            | `./logs/kuikly_pod_install.log`       |
| xcodebuild (sim)       | `./logs/kuikly_xcodebuild_sim.log`    |
| xcodebuild (device)    | `./logs/kuikly_xcodebuild_device.log` |
| assembleDebug          | `./logs/kuikly_gradle_android.log`    |
| hvigorw                | `./logs/kuikly_hvigor_build.log`      |
| console capture        | `./logs/kuikly_console.log`           |
| logcat capture         | `./logs/kuikly_android.log`           |
| hilog capture          | `./logs/kuikly_ohos.log`              |

3. **Use `bash ./gradlew`** instead of `./gradlew` — `env sh` can hang in worktrees.
4. **Never run multiple instances** of the same build tool concurrently.
5. **After build, use Read/Grep tools** to check log files — not bash grep.

## Fast Error Lookup

Kotlin/Native 编译错误前缀是 `e:`，路径格式 `file:///path/File.kt:行:列`。

```bash
grep "^e:.*file:" ./logs/kuikly_xcodebuild_*.log
```

示例：`e: file:///.../File.kt:10:20 Expected...`

## Console Log

启动后告诉用户：
> 进入 "**pageName**" 页面 
→ **操作步骤** → 复现问题。
日志：`./logs/kuikly_console.log`，过滤：`grep "YOUR_TAG"`，完成后告诉我分析日志。

## Cross-Platform Quick Facts

| Fact            | iOS                                                                                                 | Android                           | HarmonyOS                    |
| --------------- | --------------------------------------------------------------------------------------------------- | --------------------------------- | ---------------------------- |
| Bundle ID       | `com.tencent.kuiklycore.demo.<SUFFIX>` where `<SUFFIX>` = run `whoami` to get current username; user may override | `com.tencent.kuikly.android.demo` | `com.tencent.kuiklyohosdemo` |
| println capture | `--console-pty` (sim) / `--console` (device)                                                        | `adb logcat -d -s "System.out"`   | `hilog -P <PID> -x`          |
| Unlock needed?  | Launch only                                                                                         | No                                | Install                      |

### Debug tag convention

```kotlin
println("[MY_TAG] value=$value")
```
