# HarmonyOS: Build, Run & Log Capture

## Device Target

Always determine target first:
```bash
hdc list targets
# Emulator:    127.0.0.1:5555
# Real device: FMR0223C13000246 (serial number)
```

Use `-t <TARGET>` in all hdc commands. Examples below use `<TARGET>` as placeholder.

## Quick Reference — Full Flow

```bash
# Set PATH (required for all hvigor/hdc/ohpm commands)
export DEVECO_SDK_HOME="/Applications/DevEco-Studio.app/Contents/sdk"
export PATH="/Applications/DevEco-Studio.app/Contents/tools/ohpm/bin:/Applications/DevEco-Studio.app/Contents/tools/hvigor/bin:/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains:$PATH"
export NODE_HOME="$HOME/.nvm/versions/node/v24.12.0"

# Phase 1: Generate libshared.so (from project root)
mkdir -p ohosApp/entry/libs/arm64-v8a/ ohosApp/entry/src/main/cpp/thirdparty/biz_entry/
./2.0_ohos_demo_build.sh > ./logs/kuikly_ohos_build.log 2>&1

# Phase 2 (emulator only): Start emulator
/Applications/DevEco-Studio.app/Contents/tools/emulator/Emulator \
  -hvd Huawei_Phone \
  -path "$HOME/.Huawei/Emulator/deployed" \
  -imageRoot "$HOME/Library/Huawei/Sdk" > /tmp/ohos_emulator.log 2>&1 &
until hdc -t 127.0.0.1:5555 shell "param get bootevent.boot.completed" 2>/dev/null | grep -q "true"; do sleep 3; done

# Phase 3: Check signing, then build
grep -A3 "signingConfigs" ohosApp/build-profile.json5
cd ohosApp && ohpm install >> ../logs/kuikly_ohpm_install.log 2>&1
hvigorw assembleHap --mode module -p module=entry@default -p product=default -p buildMode=debug --no-daemon \
  > ../logs/kuikly_hvigor_build.log 2>&1

# Phase 4: Install & launch  (replace <TARGET> with 127.0.0.1:5555 or device serial)
hdc -t <TARGET> install ohosApp/entry/build/default/outputs/default/entry-default-signed.hap
hdc -t <TARGET> shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo

# Phase 5: Capture logs
PID=$(hdc -t <TARGET> shell pidof com.tencent.kuiklyohosdemo | tr -d '\r')
hdc -t <TARGET> shell "hilog -P $PID -x" > ./logs/kuikly_ohos.log 2>&1
```

## Tool Paths

OHOS CLI tools are bundled inside DevEco Studio, NOT on PATH by default.

| Tool | Full path |
|------|-----------|
| `hdc` | `/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/hdc` |
| `hvigorw` | `/Applications/DevEco-Studio.app/Contents/tools/hvigor/bin/hvigorw` |
| `ohpm` | `/Applications/DevEco-Studio.app/Contents/tools/ohpm/bin/ohpm` |
| `Emulator` | `/Applications/DevEco-Studio.app/Contents/tools/emulator/Emulator` |

## Phase 1: Pre-build (libshared.so)

```bash
cd <project_root>
mkdir -p ohosApp/entry/libs/arm64-v8a/
mkdir -p ohosApp/entry/src/main/cpp/thirdparty/biz_entry/
./2.0_ohos_demo_build.sh > ./logs/kuikly_ohos_build.log 2>&1
```

Output: `ohosApp/entry/libs/arm64-v8a/libshared.so` (~104MB)

## Phase 2: Start Emulator (skip for real device)

```bash
# List existing instances and get paths
/Applications/DevEco-Studio.app/Contents/tools/emulator/Emulator -list -details
# Shows: instancePath (~/.Huawei/Emulator/deployed/Huawei_Phone), imageRoot (~/Library/Huawei/Sdk)

# Start (note: -path is the PARENT of the instance folder)
/Applications/DevEco-Studio.app/Contents/tools/emulator/Emulator \
  -hvd Huawei_Phone \
  -path "$HOME/.Huawei/Emulator/deployed" \
  -imageRoot "$HOME/Library/Huawei/Sdk" > /tmp/ohos_emulator.log 2>&1 &

# Wait for boot
until hdc -t 127.0.0.1:5555 shell "param get bootevent.boot.completed" 2>/dev/null | grep -q "true"; do
  sleep 3; done && echo "Emulator booted"
```

Real device: just connect USB + enable developer mode. No start step needed.

## Phase 3: Signing

### Check signing status
```bash
grep -A5 "signingConfigs" ohosApp/build-profile.json5
```
- **Empty `[]`** → not configured → setup needed
- **Has `certpath`, `storeFile`** → configured → proceed to build

### Configure signing (first time / fresh worktree)

Signing requires Huawei Developer account login — must be done via DevEco Studio GUI (one-time only).

```bash
open -a "DevEco-Studio" <project_root>/ohosApp
```

Tell user: **File → Project Structure (`⌘;`) → Signing Configs → Automatically generate signature** → 登录华为账号 → OK

After user confirms, `build-profile.json5` will contain `certpath`/`storeFile`/`keyPassword` — proceed to build.

## Phase 4: Build HAP

```bash
cd ohosApp
ohpm install >> ../logs/kuikly_ohpm_install.log 2>&1   # needed if oh_modules missing
hvigorw assembleHap --mode module -p module=entry@default -p product=default -p buildMode=debug --no-daemon \
  > ../logs/kuikly_hvigor_build.log 2>&1; echo "EXIT:$?"
```

Output: `ohosApp/entry/build/default/outputs/default/entry-default-signed.hap` (~165MB)

## Phase 5: Install & Launch

```bash
# Install (real device install takes 2-5 min — use 300s timeout)
hdc -t <TARGET> install ohosApp/entry/build/default/outputs/default/entry-default-signed.hap
# Expected: "install bundle successfully"

# Launch
hdc -t <TARGET> shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo
# Expected: "start ability successfully"

# Force stop & relaunch
hdc -t <TARGET> shell aa force-stop com.tencent.kuiklyohosdemo
hdc -t <TARGET> shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo
```

**If `install sign info inconsistent`**: uninstall old version first:
```bash
hdc -t <TARGET> shell bm uninstall -n com.tencent.kuiklyohosdemo
hdc -t <TARGET> install ohosApp/entry/build/default/outputs/default/entry-default-signed.hap
```

## Phase 6: Log Capture

Kotlin/Native `println()` routes through HiLog with tag `Konan_main`. Always filter by PID.

```bash
PID=$(hdc -t <TARGET> shell pidof com.tencent.kuiklyohosdemo | tr -d '\r')
hdc -t <TARGET> shell "hilog -P $PID -x" > ./logs/kuikly_ohos.log 2>&1
# Then use Read/Grep tools to analyze
```

| Source | HiLog tag |
|--------|-----------|
| Kotlin `println()` | `Konan_main` |
| ArkTS `console.log` | `JSAPP` |
| Native C++ | `KRRender` |
| KuiklyUI framework | `[KLog][...]` prefix |

### Screenshot
```bash
hdc -t <TARGET> shell snapshot_display -f /data/local/tmp/screen.jpeg  # must be .jpeg not .png
hdc -t <TARGET> file recv /data/local/tmp/screen.jpeg ./logs/ohos_screen.jpeg
# Use Read tool to view
```

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `hdc`/`hvigorw` not found | Set PATH (see Tool Paths) |
| `libshared.so` not found | Run `./2.0_ohos_demo_build.sh`; check target dirs exist |
| `oh_modules`/`libkuikly.so` missing | Run `ohpm install` in `ohosApp/` |
| Signing not configured | Open DevEco Studio → user does `File → Project Structure → Signing Configs → Automatically generate signature` |
| `install sign info inconsistent` | `hdc -t <TARGET> shell bm uninstall -n com.tencent.kuiklyohosdemo` then reinstall |
| `hdc install` hangs (real device) | Unlock screen; allow 2-5 min; `hdc kill -r` to reset |
| `hdc list targets` empty | Check USB/developer mode (real device) or emulator started (simulator) |
| Emulator `-path` error | Use parent dir: `~/.Huawei/Emulator/deployed` (not `.../deployed/Huawei_Phone`) |
| `snapshot_display` fails | Use `.jpeg` suffix only |
| No Kotlin logs in hilog | Filter by PID: `hilog -P $PID -x`; tag is `Konan_main` |


## Quick Reference — Full Flow

```bash
# Phase 1: Generate libshared.so (from project root)
mkdir -p ohosApp/entry/libs/arm64-v8a/ ohosApp/entry/src/main/cpp/thirdparty/biz_entry/
./2.0_ohos_demo_build.sh > ./logs/kuikly_ohos_build.log 2>&1

# Set PATH (required for all hvigor/hdc/ohpm commands)
export DEVECO_SDK_HOME="/Applications/DevEco-Studio.app/Contents/sdk"
export PATH="/Applications/DevEco-Studio.app/Contents/tools/ohpm/bin:/Applications/DevEco-Studio.app/Contents/tools/hvigor/bin:/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains:$PATH"
export NODE_HOME="$HOME/.nvm/versions/node/v24.12.0"

# Phase 2: Start emulator (if using emulator)
/Applications/DevEco-Studio.app/Contents/tools/emulator/Emulator \
  -hvd Huawei_Phone \
  -path "$HOME/.Huawei/Emulator/deployed" \
  -imageRoot "$HOME/Library/Huawei/Sdk" > /tmp/ohos_emulator.log 2>&1 &
# Wait for boot
until hdc -t 127.0.0.1:5555 shell "param get bootevent.boot.completed" 2>/dev/null | grep -q "true"; do sleep 3; done

# Phase 3: Check/configure signing, then build
grep -A3 "signingConfigs" ohosApp/build-profile.json5  # check if configured
cd ohosApp && ohpm install >> ../logs/kuikly_ohpm_install.log 2>&1
hvigorw assembleHap --mode module -p module=entry@default -p product=default -p buildMode=debug --no-daemon \
  > ../logs/kuikly_hvigor_build.log 2>&1

# Phase 4: Install & launch
hdc -t 127.0.0.1:5555 shell bm uninstall -n com.tencent.kuiklyohosdemo  # if already installed
hdc -t 127.0.0.1:5555 install ohosApp/entry/build/default/outputs/default/entry-default-signed.hap
hdc -t 127.0.0.1:5555 shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo

# Phase 5: Capture logs
PID=$(hdc -t 127.0.0.1:5555 shell pidof com.tencent.kuiklyohosdemo | tr -d '\r')
hdc -t 127.0.0.1:5555 shell "hilog -P $PID -x" > ./logs/kuikly_ohos.log 2>&1
```

## Tool Paths

OHOS CLI tools are bundled inside DevEco Studio, NOT on PATH by default.

| Tool | Full path |
|------|-----------|
| `hdc` | `/Applications/DevEco-Studio.app/Contents/sdk/default/openharmony/toolchains/hdc` |
| `hvigorw` | `/Applications/DevEco-Studio.app/Contents/tools/hvigor/bin/hvigorw` |
| `ohpm` | `/Applications/DevEco-Studio.app/Contents/tools/ohpm/bin/ohpm` |
| `Emulator` | `/Applications/DevEco-Studio.app/Contents/tools/emulator/Emulator` |

Always set PATH at the start of any OHOS session (see Quick Reference above).

## Phase 1: Pre-build (libshared.so)

```bash
cd <project_root>
# Create target dirs first (fresh worktree may not have them)
mkdir -p ohosApp/entry/libs/arm64-v8a/
mkdir -p ohosApp/entry/src/main/cpp/thirdparty/biz_entry/
# Build
./2.0_ohos_demo_build.sh > ./logs/kuikly_ohos_build.log 2>&1
```

Output: `ohosApp/entry/libs/arm64-v8a/libshared.so` (~104MB)

## Phase 2: Emulator

### Check existing instances
```bash
/Applications/DevEco-Studio.app/Contents/tools/emulator/Emulator -list -details
```
This shows `instancePath`, `imageRoot` needed to start. Example output:
```json
{ "name": "Huawei_Phone", "instancePath": "~/.Huawei/Emulator/deployed/Huawei_Phone", "imageRoot": "~/Library/Huawei/Sdk", ... }
```

### Start emulator
```bash
/Applications/DevEco-Studio.app/Contents/tools/emulator/Emulator \
  -hvd Huawei_Phone \
  -path "$HOME/.Huawei/Emulator/deployed" \
  -imageRoot "$HOME/Library/Huawei/Sdk" > /tmp/ohos_emulator.log 2>&1 &
```
Note: `-path` is the **parent** of the instance folder (not the instance folder itself).

### Wait for boot
```bash
until hdc -t 127.0.0.1:5555 shell "param get bootevent.boot.completed" 2>/dev/null | grep -q "true"; do
  sleep 3; done
echo "Emulator booted"
```

### Verify connected
```bash
hdc list targets
# Emulator shows as: 127.0.0.1:5555
# Real device shows as: FMR0223C13000246 (serial)
```

## Phase 3: Signing

### Check signing status
```bash
grep -A5 "signingConfigs" ohosApp/build-profile.json5
```
- **Empty `[]`** → not configured → need signing setup (see below)
- **Has `certpath`, `storeFile`** → configured → proceed to build

### Configure signing (first time / fresh worktree)

Signing requires Huawei Developer account — cannot be done fully via CLI.

**Workflow**: Claude opens DevEco Studio, user does one-time signing setup:

```bash
open -a "DevEco-Studio" <project_root>/ohosApp
```

Tell user: **File → Project Structure (`⌘;`) → Signing Configs → Automatically generate signature** → 登录华为账号 → OK

After user confirms done, `build-profile.json5` will have `certpath`, `storeFile`, `keyPassword` etc. — proceed to build.

## Phase 4: Build HAP

```bash
cd ohosApp
# Install JS dependencies (needed if oh_modules missing)
ohpm install >> ../logs/kuikly_ohpm_install.log 2>&1
# Build
hvigorw assembleHap --mode module -p module=entry@default -p product=default -p buildMode=debug --no-daemon \
  > ../logs/kuikly_hvigor_build.log 2>&1; echo "EXIT:$?"
```

Output: `ohosApp/entry/build/default/outputs/default/entry-default-signed.hap` (~165MB)

Common error: `missing libkuikly.so` → run `ohpm install` first.

## Phase 5: Install & Launch

### Install
```bash
# Use -t to target specific device/emulator
hdc -t 127.0.0.1:5555 install ohosApp/entry/build/default/outputs/default/entry-default-signed.hap
```

**If `install sign info inconsistent`**: uninstall old version first:
```bash
hdc -t 127.0.0.1:5555 shell bm uninstall -n com.tencent.kuiklyohosdemo
hdc -t 127.0.0.1:5555 install ohosApp/entry/build/default/outputs/default/entry-default-signed.hap
```

Real device install is slow (~2-5 min for 165MB HAP) — use 300s timeout.

### Launch
```bash
hdc -t 127.0.0.1:5555 shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo
# Expected: "start ability successfully"
```

### Force stop & relaunch
```bash
hdc -t 127.0.0.1:5555 shell aa force-stop com.tencent.kuiklyohosdemo
hdc -t 127.0.0.1:5555 shell aa start -a EntryAbility -b com.tencent.kuiklyohosdemo
```

## Phase 6: Log Capture

Kotlin/Native `println()` routes through HiLog with tag `Konan_main`. Always filter by PID.

```bash
# Get PID
PID=$(hdc -t 127.0.0.1:5555 shell pidof com.tencent.kuiklyohosdemo | tr -d '\r')

# Dump to file
hdc -t 127.0.0.1:5555 shell "hilog -P $PID -x" > ./logs/kuikly_ohos.log 2>&1

# Then grep with Read/Grep tools (not bash grep)
```

| Source | HiLog tag |
|--------|-----------|
| Kotlin `println()` | `Konan_main` |
| ArkTS `console.log` | `JSAPP` |
| Native C++ | `KRRender` |
| KuiklyUI framework | `[KLog][...]` prefix |

### Screenshot
```bash
hdc -t 127.0.0.1:5555 shell snapshot_display -f /data/local/tmp/screen.jpeg
hdc -t 127.0.0.1:5555 file recv /data/local/tmp/screen.jpeg ./logs/ohos_screen.jpeg
# Use Read tool to view
```
Note: suffix must be `.jpeg` (not `.png`).

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `hdc`/`hvigorw` not found | Set PATH (see Tool Paths) |
| `libshared.so` not found | Run `./2.0_ohos_demo_build.sh`; check target dirs exist |
| `oh_modules` missing / `libkuikly.so` missing | Run `ohpm install` in `ohosApp/` |
| Signing not configured | Open DevEco Studio, user does `File → Project Structure → Signing Configs → Automatically generate signature` |
| `install sign info inconsistent` | Uninstall old version: `hdc shell bm uninstall -n com.tencent.kuiklyohosdemo` |
| `hdc install` hangs (real device) | Unlock screen; allow 2-5 min; `hdc kill -r` to reset |
| `hdc list targets` empty | Check USB/network; for emulator check it started on port 5555 |
| Emulator `-path` error | Use parent dir of instance (e.g. `~/.Huawei/Emulator/deployed`, not `.../deployed/Huawei_Phone`) |
| `snapshot_display` error | Use `.jpeg` suffix, not `.png` |
| No Kotlin logs | Filter by PID: `hilog -P $PID -x`; tag is `Konan_main` |
