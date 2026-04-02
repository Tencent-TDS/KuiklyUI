# Android: Build, Run & Log Capture

## Quick Reference

```bash
# 0. Prerequisites: ensure local.properties exists
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# 1. Check/start emulator
~/Library/Android/sdk/emulator/emulator -list-avds
~/Library/Android/sdk/emulator/emulator -avd <AVD_NAME> -no-snapshot-load &
sleep 15 && adb devices   # wait until "device" appears (not "offline")

# 2. Build
./gradlew :androidApp:assembleDebug 2>&1 | grep -E "BUILD|error:|FAILED"

# 3. Install & launch a specific page
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n com.tencent.kuikly.android.demo/.KuiklyRenderActivity \
  --es pageName <PAGE_NAME>

# 4. Capture logs
adb logcat -d -s "System.out" | grep "YOUR_TAG"

# 5. Screenshot
adb shell screencap -p /sdcard/screen.png && adb pull /sdcard/screen.png ./logs/screen.png
```

## Phase 0: Prerequisites

### local.properties (REQUIRED — often missing in fresh worktrees)

Gradle requires `local.properties` at the project root with the Android SDK path.
If missing, the build fails with: `SDK location not found`.

```bash
# Check if it exists
ls local.properties

# Create if missing (macOS default SDK location)
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties
```

### Verify Android SDK tools are on PATH

```bash
# adb
adb version || echo "adb not found — add to PATH or use full path"

# emulator
~/Library/Android/sdk/emulator/emulator -version 2>&1 | head -1
```

## Phase 1: Pre-build

No separate pre-build step needed. Gradle handles KMP compilation as part of the Android build.

## Phase 2: Build

```bash
cd <project_root>
./gradlew :androidApp:assembleDebug 2>&1 | grep -E "BUILD|error:|FAILED"
```

**Timing**: ~2-3 min first build, ~30-40s incremental.

**Filter output** (build is very verbose):
```bash
# Show only errors and final result
./gradlew :androidApp:assembleDebug 2>&1 | grep -E "e: file|BUILD|FAILED" | head -30

# Show full error details
./gradlew :androidApp:assembleDebug 2>&1 | grep -A5 "e: file"
```

To specify Kotlin version:
```bash
KUIKLY_KOTLIN_VERSION=2.1.21 ./gradlew :androidApp:assembleDebug
```

## Phase 3: Deploy & Launch

### Start Emulator

```bash
# List available AVDs
~/Library/Android/sdk/emulator/emulator -list-avds

# Start emulator (background, no snapshot for clean state)
~/Library/Android/sdk/emulator/emulator -avd <AVD_NAME> -no-snapshot-load &

# Wait for device to be ready (may take 15-30s)
sleep 15 && adb devices
# Expected output: "emulator-5554   device"
# If "offline" appears, wait longer and retry
```

**Known AVDs on this machine**: `Medium_Phone_API_35`, `Copy_of_Medium_Phone_API_35`, `Pixel_Tablet`

### Install & Launch

```bash
# Install (use -r to replace existing)
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk

# Find package name and main activity (if unknown)
adb shell pm list packages | grep kuikly
adb shell pm dump <PACKAGE_NAME> | grep -A2 "MAIN" | head -10

# Launch the app's main screen
adb shell am start -n com.tencent.kuikly.android.demo/.KuiklyRenderActivity

# Launch a specific Kuikly page directly (pass pageName extra)
adb shell am start -n com.tencent.kuikly.android.demo/.KuiklyRenderActivity \
  --es pageName <PAGE_NAME>
```

**Verified package info**:
- Package: `com.tencent.kuikly.android.demo`
- Main activity: `com.tencent.kuikly.android.demo/.KuiklyRenderActivity`
- APK path: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`

### Navigate to a page

The KuiklyRenderActivity accepts a `pageName` string extra that maps to `@Page("...")` annotations in Kotlin:

```bash
# Example: open the ComposeVideoDemo page
adb shell am start -n com.tencent.kuikly.android.demo/.KuiklyRenderActivity \
  --es pageName ComposeVideoDemo
```

## Phase 4: Log Capture

### Kotlin println()

On Android, Kotlin `println()` goes to Logcat with tag `System.out`:

```bash
# Dump recent logs (after app has run)
adb logcat -d -s "System.out" | grep "YOUR_TAG"

# Real-time streaming
adb logcat -s "System.out" | grep "YOUR_TAG"

# Capture to file (background), then grep
adb logcat -s "System.out" > ./logs/kuikly_android.log &
# ... operate the app ...
grep "YOUR_TAG" ./logs/kuikly_android.log

# Filter by app process
adb logcat --pid=$(adb shell pidof com.tencent.kuikly.android.demo) | grep "YOUR_TAG"
```

### KuiklyUI framework logs

```bash
adb logcat -s "KLog" "KuiklyRender" "KRPerformance"
```

### Screenshot

```bash
# Take screenshot and pull to local machine
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png ./logs/screen.png
# Then use Read tool to view the image
```

## Directory Structure

```
androidApp/
├── build.gradle.kts        # Android app module config
├── src/main/
│   ├── AndroidManifest.xml  # Package name, activities
│   ├── java/                # Host app code (KuiklyRenderActivity etc.)
│   └── res/                 # Resources
└── build/outputs/apk/
    └── debug/
        └── androidApp-debug.apk   # Output APK
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `SDK location not found` | Create `local.properties` with `sdk.dir=$HOME/Library/Android/sdk` |
| `adb: command not found` | Use full path `~/Library/Android/sdk/platform-tools/adb` or add to PATH |
| `emulator: command not found` | Use full path `~/Library/Android/sdk/emulator/emulator` |
| `adb devices` shows "offline" | Emulator still booting — wait 15-30s and retry |
| `Error type 3: Activity does not exist` | Wrong package/activity name — use `adb shell pm dump <pkg> \| grep MAIN` |
| Gradle sync failure | Check JDK 17, check `gradle.properties` |
| App crashes on launch | `adb logcat --pid=$(adb shell pidof <pkg>)` for stack trace |
| Build takes too long | Use incremental: only rebuild after code changes, not config changes |
| No logs appearing | Use `-d` flag for dump (not streaming) after app has run; check tag is `System.out` |
