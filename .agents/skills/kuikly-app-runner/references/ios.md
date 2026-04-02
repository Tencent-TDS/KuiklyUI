# iOS: Build, Run & Log Capture

If a step fails, read `references/ios-troubleshooting.md` for diagnosis and fixes.

## Quick Reference — Simulator

```bash
# From project root
bash ./gradlew :demo:generateDummyFramework --console=plain > ./logs/kuikly_gradle_dummy.log 2>&1
cd iosApp
export LANG=en_US.UTF-8 && export LC_ALL=en_US.UTF-8
pod install > ./logs/kuikly_pod_install.log 2>&1
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -destination 'platform=iOS Simulator,id=<SIM_ID>' -configuration Debug build \
  > ./logs/kuikly_xcodebuild_sim.log 2>&1
xcrun simctl boot <SIM_ID>
open -a Simulator
xcrun simctl install <SIM_ID> \
  $(ls -td ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphonesimulator/iosApp.app | head -1)
xcrun simctl launch <SIM_ID> com.tencent.kuiklycore.demo.<SUFFIX>
```

## Quick Reference — Real Device

```bash
# From project root
bash ./gradlew :demo:generateDummyFramework --console=plain > ./logs/kuikly_gradle_dummy.log 2>&1
cd iosApp
export LANG=en_US.UTF-8 && export LC_ALL=en_US.UTF-8
pod install > ./logs/kuikly_pod_install.log 2>&1

# Find DEVICE_ID and TEAM_ID
xcrun xctrace list devices 2>&1 | grep "iPhone"
defaults read com.apple.dt.Xcode IDEProvisioningTeams 2>/dev/null  # Use teamID from here

xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -destination 'platform=iOS,id=<DEVICE_ID>' -configuration Debug build \
  DEVELOPMENT_TEAM=<TEAM_ID> -allowProvisioningUpdates \
  CODE_SIGN_STYLE=Automatic \
  PRODUCT_BUNDLE_IDENTIFIER=com.tencent.kuiklycore.demo.<SUFFIX> \
  > ./logs/kuikly_xcodebuild_device.log 2>&1
xcrun devicectl device install app --device <DEVICE_ID> \
  $(ls -td ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphoneos/iosApp.app | head -1)
xcrun devicectl device process launch --device <DEVICE_ID> com.tencent.kuiklycore.demo.<SUFFIX>
```

Default `<SUFFIX>`: run `whoami` to get the current macOS username and use that. If user specifies a different suffix, use that instead. Use the same suffix for both simulator and device.

## Phase 1: Pre-build

```bash
bash ./gradlew :demo:generateDummyFramework --console=plain > ./logs/kuikly_gradle_dummy.log 2>&1
```

- Always use `bash ./gradlew` (not `./gradlew`) — `env sh` can hang in worktrees.
- Verify: Read log for `BUILD SUCCESSFUL`. Typically ~6s incremental, ~30s first run.
- Output: `demo/build/cocoapods/framework/shared.framework` (empty stub).

## Phase 2: Build

### Pod Install

```bash
cd iosApp
export LANG=en_US.UTF-8 && export LC_ALL=en_US.UTF-8
pod install > ./logs/kuikly_pod_install.log 2>&1
```

- UTF-8 exports are **mandatory** (CocoaPods 1.16+ / Ruby 3.4).
- Skip `--repo-update` unless pod install fails with "Unable to find a specification".
- Verify: Read log for `Pod installation complete!`.

### Xcode Build

```bash
# Simulator
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -destination 'platform=iOS Simulator,id=<SIM_ID>' -configuration Debug build \
  > ./logs/kuikly_xcodebuild_sim.log 2>&1

# Real Device (MUST pass signing overrides)
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp \
  -destination 'platform=iOS,id=<DEVICE_ID>' -configuration Debug build \
  DEVELOPMENT_TEAM=<TEAM_ID> -allowProvisioningUpdates \
  CODE_SIGN_STYLE=Automatic \
  PRODUCT_BUNDLE_IDENTIFIER=com.tencent.kuiklycore.demo.<SUFFIX> \
  > ./logs/kuikly_xcodebuild_device.log 2>&1
```

- ~90s first build (K/N dominates), ~30s incremental.
- Verify: Grep log for `BUILD SUCCEEDED` or `error:`.
- **Always redirect to file. NEVER pipe through grep/tail.**

## Phase 3: Deploy & Launch

**Before launch, always check the actual Bundle ID from the built app** — it may have been changed by previous device builds:

```bash
APP_PATH=$(ls -td ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphonesimulator/iosApp.app | head -1)
plutil -extract CFBundleIdentifier raw "$APP_PATH/Info.plist"
```

Use the returned value as `<BUNDLE_ID>` below. Do NOT assume it matches the default.

### Simulator

```bash
xcrun simctl boot <SIM_ID>
open -a Simulator  # bring simulator window to front
xcrun simctl install <SIM_ID> "$APP_PATH"
xcrun simctl launch <SIM_ID> <BUNDLE_ID>
```

### Real Device

Device must be **unlocked** before launch.

```bash
xcrun devicectl device install app --device <DEVICE_ID> \
  $(ls -td ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphoneos/iosApp.app | head -1)
xcrun devicectl device process launch --device <DEVICE_ID> com.tencent.kuiklycore.demo.<SUFFIX>

# Terminate & relaunch
xcrun devicectl device process launch --device <DEVICE_ID> --terminate-existing com.tencent.kuiklycore.demo.<SUFFIX>
```

**Bundle ID**: Default suffix for both simulator and device is the current macOS username (run `whoami` to get it). User may specify a custom suffix.

## Phase 4: Log Capture

Kotlin/Native `println()` writes to stdout, NOT os_log. `log stream` will NOT capture it.

### Simulator
```bash
xcrun simctl terminate <SIM_ID> <BUNDLE_ID>
xcrun simctl launch --console-pty <SIM_ID> \
  <BUNDLE_ID> > ./logs/kuikly_console.log 2>&1 &
# Read: grep "YOUR_TAG" ./logs/kuikly_console.log
```

### Real Device
```bash
xcrun devicectl device process launch --device <DEVICE_ID> \
  --terminate-existing --console com.tencent.kuiklycore.demo.<SUFFIX> > ./logs/kuikly_console.log 2>&1 &
# Read: grep "YOUR_TAG" ./logs/kuikly_console.log
```

Note: Real device uses `--console` (not `--console-pty`).

## Directory Structure

```
iosApp/
├── iosApp.xcworkspace/     # <-- Open this (not .xcodeproj)
├── Podfile                 # Dependencies: SDWebImage, OpenKuiklyIOSRender, demo, WMPlayer, libpag
└── iosApp/
    ├── iOSApp.swift        # @main SwiftUI entry
    ├── ContentView.swift   # Root view -> KuiklyRenderViewPage
    └── KuiklyRenderExpand/ # Custom handlers/modules/controllers
```
