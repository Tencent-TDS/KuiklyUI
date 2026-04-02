# iOS Troubleshooting

Read this file **only** when a build/install/launch step fails. Do not load it preemptively.

## Quick Lookup Table

| Problem | Solution |
|---------|----------|
| `Encoding::CompatibilityError` on pod install | `export LANG=en_US.UTF-8 && export LC_ALL=en_US.UTF-8` |
| `shared.framework not found` on pod install | `bash ./gradlew :demo:generateDummyFramework` |
| `./gradlew` hangs with 0 output | Use `bash ./gradlew`. See "Gradle Hangs" below. |
| `pod install` hangs, Ctrl+C doesn't work | fcntl deadlock. See "Pod Install Hangs" below. |
| Multiple `pod install` processes running | `pkill -9 -f "pod install"`. Concurrent pods deadlock. |
| Build output lost on timeout | Always redirect: `> ./logs/kuikly_*.log 2>&1`. Never pipe through `tail`/`grep`. |
| `FBSOpenApplicationServiceErrorDomain, code=4` on launch | Bundle ID mismatch. Run `plutil -extract CFBundleIdentifier raw .../iosApp.app/Info.plist` to get actual ID. |
| `Signing for "iosApp" requires a development team` | Pass `DEVELOPMENT_TEAM=<TEAM_ID>`. See "Signing" below. |
| `No Account for Team "<TEAM_ID>"` | Team ID from keychain but not logged into Xcode Accounts. Use `defaults read com.apple.dt.Xcode IDEProvisioningTeams` to find Xcode-registered teams. See "Signing" below. |
| `The app identifier cannot be registered to your development team` | Override: `PRODUCT_BUNDLE_IDENTIFIER=com.tencent.kuiklycore.demo.<suffix>` |
| `MismatchedApplicationIdentifierEntitlement` on install | Team ID mismatch. Use same Team ID or uninstall app first. See "Signing" below. |
| `invalid code signature ... profile has not been explicitly trusted` | Device Settings -> General -> VPN & Device Management -> Trust certificate. |
| Wrong DerivedData picked up | `ls -td ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphoneos/iosApp.app \| head -1` |
| `Unresolved reference` in Kotlin | Check missing imports in Kotlin source. |
| `log stream` misses Kotlin logs | Use `--console-pty` (Kotlin println goes to stdout, not os_log). |
| Incremental build still slow | K/N recompilation triggered by changes in `compose/` or `core/`. Swift/ObjC-only changes are fast. |
| Kotlin code changed but app unchanged | Framework cache. See "Stale Kotlin Framework" below. |

## Gradle Hangs (0 output)

`gradlew` uses `#!/usr/bin/env sh`. In worktree/non-interactive environments, `env sh` can hang silently — process exists but produces zero output and never spawns a Java child process.

**Diagnosis**:
```bash
# If log file is 0 bytes after 30s, check for Java child process:
ps aux | grep "[G]radleDaemon"
# No Java process = script is stuck
```

**Fix**: Always use `bash ./gradlew` instead of `./gradlew`.

## Pod Install Hangs — fcntl Deadlock (macOS 26+)

If `pod install` hangs (0% CPU, no output, Ctrl+C doesn't work), it's stuck on a kernel-level `fcntl` file lock.

**Causes**:
- Previous `pod install` was force-killed, leaving kernel file locks unreleased
- Multiple `pod install` processes ran concurrently on the same directory

**Diagnosis**:
```bash
sample <POD_PID> 1 -file ./logs/pod_sample.txt
grep "fcntl" ./logs/pod_sample.txt
```
If call stack shows `dyld4::Loader::mapSegments` -> `fcntl`, it's the deadlock.

**Fix**:
1. `kill -9 <POD_PID>`
2. If new `pod install` still hangs, **restart terminal** or **reboot Mac**
3. After restart, `pod install` works immediately

**Prevention**: Never run concurrent `pod install`. Always kill first:
```bash
pkill -9 -f "pod install" 2>/dev/null; sleep 2
```

## Real Device Signing

The project's Debug config has `DEVELOPMENT_TEAM = "${TEAM_ID}"` (placeholder) and `PRODUCT_BUNDLE_IDENTIFIER = com.tencent.kuiklycore.demo.luoyibu` (can't register to personal teams). **Override on command line** using current username as suffix (run `whoami` to get it):

```bash
xcodebuild ... \
  DEVELOPMENT_TEAM=<TEAM_ID> -allowProvisioningUpdates \
  CODE_SIGN_STYLE=Automatic \
  PRODUCT_BUNDLE_IDENTIFIER=com.tencent.kuiklycore.demo.<SUFFIX>
```

If user specifies a bundle ID suffix, use it. Otherwise run `whoami` and use the output as the default suffix for both simulator and device.

### Finding the correct Team ID

**Key distinction**: keychain certificates ≠ Xcode-usable teams. `security find-identity` shows keychain certs, but `xcodebuild -allowProvisioningUpdates` requires the team to be **logged into Xcode Accounts**.

**Step 1 — Check Xcode-registered teams FIRST** (this is what actually works):
```bash
defaults read com.apple.dt.Xcode IDEProvisioningTeams 2>/dev/null
```
Use a `teamID` from this output. Free Personal Teams (`isFreeProvisioningTeam = 1`) work for device debugging.

**Step 2 — Fallback** to keychain only if Step 1 returns nothing:
```bash
security find-identity -v -p codesigning
```

### Free Personal Team

Free teams auto-create provisioning profiles for new bundle IDs via `-allowProvisioningUpdates`. Limitations: 7-day cert expiry, max 3 apps, no push notifications. Sufficient for demo/debug.

### Team ID mismatch on install

Error: `Upgrade's application-identifier entitlement string (<NEW_TEAM>...) does not match installed application's application-identifier string (<OLD_TEAM>...)`

Fix: Use same Team ID as existing install, OR uninstall app from device first.

### Trust developer certificate

First-time personal dev cert install requires manual trust:

**Settings -> General -> VPN & Device Management -> find certificate -> Trust**

## Stale Kotlin Framework

**Symptom**: Kotlin code changed, Xcode build succeeds, but app shows old behavior.

**Root cause**: `xcodebuild` uses `OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED=YES` or Gradle cache marks tasks UP-TO-DATE, so the CocoaPods framework (`demo/build/cocoapods/framework/shared.framework`) is stale.

**Fix — force relink**:
```bash
rm -rf demo/build   # delete entire build dir, not just clean
rm -rf compose/build # if compose/ also changed
bash ./gradlew :demo:linkPodDebugFrameworkIosSimulatorArm64  # ~60s full recompile
bash ./gradlew :demo:syncFramework \
  -Pkotlin.native.cocoapods.platform=iphonesimulator \
  -Pkotlin.native.cocoapods.archs=arm64 \
  -Pkotlin.native.cocoapods.configuration=Debug
```

Then **clean DerivedData** before Xcode build:
```bash
rm -rf ~/Library/Developer/Xcode/DerivedData/iosApp-*
```

**Important**: Always install from DerivedData path, not `iosApp/build/`:
```bash
xcrun simctl install <SIM_ID> \
  $(ls -td ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphonesimulator/iosApp.app | head -1)
```

## DerivedData — Finding Correct Build Output

Multiple worktrees create multiple `~/Library/Developer/Xcode/DerivedData/iosApp-*` directories.

**Do NOT** blindly glob `iosApp-*/Build/Products/...` — may match stale build from another worktree.

```bash
# Find most recent
ls -td ~/Library/Developer/Xcode/DerivedData/iosApp-*/Build/Products/Debug-iphoneos/iosApp.app | head -1

# Or extract exact path from build log
grep "Touch.*iosApp.app" ./logs/kuikly_xcodebuild_device.log | tail -1
```
