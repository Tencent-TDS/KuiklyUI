#!/usr/bin/env bash
#
# 发布器（MentionPublisherDemo）Android happy-path 端到端回归脚本
# 目标：保证 @人 高亮 / 候选 / 两段式删除 功能在后续迭代中不被改坏。
#
# 执行引擎：Appium + HTTP Server（普通 node 进程拉起，非 Skill / 非 AI）。
#   启动引擎： node <engine_dir>/src/server.ts   （默认监听 http://localhost:7900）
#   启动 Appium： appium --port 4723
# 前置：Android 真机/模拟器已连接；Appium server 已起；demo 已安装并开启 debugUIInspector。
#
# 真机注意事项（vivo / 部分 ROM）：
#   - Appium 不会自动建立 uiautomator2 的 8200 反向隧道，需手动 `adb reverse tcp:8200 tcp:8200`，
#     否则 view-tree / tap / input 等走设备端 server 的命令会 20s 超时。见 main() 内处理。
#   - 引擎 /input 内部是 elementClear + sendKeys（会清空字段），而本场景需要“追加第二个 @人”，
#     因此 input_text 改用 `adb shell input text`（在已聚焦的 EditText 上追加，不清空）。
#
# 退出码：全部用例 PASS 返回 0；任一 FAIL 返回 1（供 CI 判定红绿）。
#
set -uo pipefail

# ===================== 可配置项 =====================
ENGINE_PORT="${ENGINE_PORT:-7900}"
ENGINE_URL="http://localhost:${ENGINE_PORT}"
PLATFORM="${PLATFORM:-android}"
APP_PACKAGE="${APP_PACKAGE:-com.tencent.kuikly.android.demo}"
APP_ACTIVITY="${APP_ACTIVITY:-com.tencent.kuikly.android.demo.KuiklyRenderActivity}"
PAGE_NAME="${PAGE_NAME:-MentionPublisherDemo}"
ADB="${ADB:-adb}"
WAIT_DEFAULT_MS="${WAIT_DEFAULT_MS:-3000}"
# 单次引擎请求的 curl 超时（秒）：uia2 冷启动/挂死时快速失败，避免脚本无限阻塞。
ENGINE_TIMEOUT_S="${ENGINE_TIMEOUT_S:-30}"
# 建会话专用超时（秒）：CI 冷环境首次 /start-session 需Appium 装/起 uiautomator2 server
# （引擎 uiautomator2ServerLaunchTimeout=60s），必须 > 60s，否则 curl 30s 先掐断 → 空响应误判失败。
SESSION_TIMEOUT_S="${SESSION_TIMEOUT_S:-120}"
# 设备 serial：未指定时取 adb 第一台已连接设备（真机/模拟器）
DEVICE_SERIAL="${DEVICE_SERIAL:-}"
if [ -z "$DEVICE_SERIAL" ]; then
  DEVICE_SERIAL="$("$ADB" devices 2>/dev/null | awk 'NF && $2=="device"{print $1; exit}')"
fi
[ -z "$DEVICE_SERIAL" ] && { echo "未检测到 Android 设备，请设置 DEVICE_SERIAL 或连接设备"; exit 1; }

PASS=0
FAIL=0
FAILED_CASES=()
# 失败证据目录（每次运行一个时间戳子目录）；失败用例自动收 view-tree/logcat/截图。
RUN_ID="$(date +%Y%m%d-%H%M%S)"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_DIR="${LOG_DIR:-$SCRIPT_DIR/logs/$RUN_ID}"
# golden baseline 目录（先人审 MVP，调研 G4）：CAPTURE_BASELINE=1 时存成功态截图，供失败时人审比对。
BASELINE_DIR="${BASELINE_DIR:-$SCRIPT_DIR/golden-baseline}"
# 注意：macOS/BSD 的 mktemp 要求随机后缀 XXXXXX 必须位于模板末尾，
# 否则不会随机化、会直接创建字面上的 eng_resp.XXXXXX.json（导致 “File exists” 卡死）。
RESP_FILE="$(mktemp /tmp/eng_resp.XXXXXX)"
# 测试输入法：vivo 默认百度/搜狗中文拼音会把英文转写（如 @zzz → @z'z'z），
# 导致“英文无匹配”用例失真。改用 Appium 自带的 AppiumIME 原样提交文本。
ORIG_IM="$( "$ADB" -s "$DEVICE_SERIAL" shell settings get secure default_input_method 2>/dev/null | tr -d '\r' )"
TEST_IM="io.appium.settings/.AppiumIME"
restore_ime() { [ -n "${ORIG_IM:-}" ] && "$ADB" -s "$DEVICE_SERIAL" shell ime set "$ORIG_IM" >/dev/null 2>&1 || true; }
trap 'rm -f "$RESP_FILE"; restore_ime' EXIT

# ===================== 引擎请求封装 =====================
# 引擎所有定位类接口都要求 body 内带 {"selector":{...}} 包装，且 Compose 的 testTag 映射到 resource-id，
# 故统一用 {"selector":{"testTag":"xxx"}}（不要用 id，引擎对 id 选择器会抛 Unknown selector）。
engine_post() { # endpoint json-body [timeout_s] -> http_code (响应体写入 RESP_FILE)
  curl -s --max-time "${3:-$ENGINE_TIMEOUT_S}" -o "$RESP_FILE" -w "%{http_code}" -X POST "$ENGINE_URL$1" \
    -H "Content-Type: application/json" -d "$2" 2>/dev/null || echo "000"
}
engine_get() { # endpoint -> http_code
  curl -s --max-time "$ENGINE_TIMEOUT_S" -o "$RESP_FILE" -w "%{http_code}" "$ENGINE_URL$1" 2>/dev/null || echo "000"
}
resp_ok() { # http_code
  [ "$1" = "200" ] && grep -q '"ok":true' "$RESP_FILE"
}
resp_text() { # 取 view-tree 响应的 text 字段
  python3 -c "import sys,json;d=json.load(open('$RESP_FILE'));print(d.get('text',''))" 2>/dev/null
}

# ===================== 动作 / 断言原语 =====================
tap() { local c; c=$(engine_post /tap "{\"selector\":{\"testTag\":\"$1\"}}"); resp_ok "$c"; }
# 在已聚焦的输入框“追加”文本；用 adb input text 避免引擎 input() 的 elementClear 清空字段。
# 仅输入 ASCII（本场景只输 @ / @zzz，中文靠点候选插入）。
input_text() { "$ADB" -s "$DEVICE_SERIAL" shell input text "$1"; }
# 聚焦输入框（引擎 tap 外层 DivView 会聚焦内部 EditText）
focus_input() { tap mention_input; sleep 0.2; }
wait_visible() { local c; c=$(engine_post /wait-for "{\"selector\":{\"testTag\":\"$1\"},\"timeoutMs\":${2:-$WAIT_DEFAULT_MS}}"); resp_ok "$c"; }
assert_visible() { local c; c=$(engine_post /assert-visible "{\"selector\":{\"testTag\":\"$1\"}}"); resp_ok "$c"; }

# 取指定 testTag 节点的整行文本（view-tree 形如 ... testTag="xxx" text="yyy" ...）
# 用 grep -F（固定字符串），避免 testTag / 断言内容里的 [ ] ( ) 被当成正则。
node_line() {
  engine_get "/view-tree?visible=true" >/dev/null
  resp_text | grep -F "testTag=\"$1\""
}
# 取指定 testTag 节点 text="" 属性的原始值（TC9 用来解析 debug_text / debug_mentions 做自洽切片）
node_text_value() {
  engine_get "/view-tree?visible=true" >/dev/null
  resp_text | grep -F "testTag=\"$1\"" | sed 's/.*text="\([^"]*\)".*/\1/' | head -1
}
# 该 testTag 节点文本包含子串（固定字符串匹配）
assert_node_contains() {
  node_line "$1" | grep -qF "$2"
}
# 该 testTag 节点不存在（候选未弹出等）
assert_not_visible() {
  engine_get "/view-tree?visible=true" >/dev/null
  ! resp_text | grep -qF "testTag=\"$1\""
}

# ===================== 用例框架 =====================
# 失败证据采集（调研 G2）：失败时收 view-tree JSON + page-source + logcat + 截图，
# 存 $LOG_DIR/<用例名>/，供排查「断了不知为啥红」。成功不采集（省时省空间）。
collect_evidence() { # case_name
  local case_name="$1" dir
  dir="$LOG_DIR/$case_name"
  mkdir -p "$dir" 2>/dev/null || return 0
  # view-tree JSON（失败时刻现场，含 testTag/text 的渲染树）
  engine_get "/view-tree?visible=true" >/dev/null 2>&1
  cp -f "$RESP_FILE" "$dir/view-tree.json" 2>/dev/null || true
  # page-source（uia2 原始 XML，比 view-tree 更全）
  engine_get "/page-source" >/dev/null 2>&1
  cp -f "$RESP_FILE" "$dir/page-source.json" 2>/dev/null || true
  # logcat（最近 300 行，定位崩溃/异常）
  "$ADB" -s "$DEVICE_SERIAL" logcat -d -v time 2>/dev/null | tail -300 > "$dir/logcat.txt" 2>/dev/null || true
  # 截图（PNG，adb exec-out screencap，不依赖引擎）
  "$ADB" -s "$DEVICE_SERIAL" exec-out screencap -p > "$dir/screenshot.png" 2>/dev/null || true
  echo "   证据已存: $dir"
}

# golden baseline（先人审 MVP，调研 G4）：CAPTURE_BASELINE=1 时 PASS 用例也存截图作 baseline，
# 一次性建立基准；平时不开。失败时 collect_evidence 的截图可与 baseline 人审比对
# （不做自动像素 diff，避免真机/模拟器渲染差异导致的 flaky）。
capture_baseline() { # case_name
  [ "${CAPTURE_BASELINE:-0}" = "1" ] || return 0
  mkdir -p "$BASELINE_DIR" 2>/dev/null || return 0
  "$ADB" -s "$DEVICE_SERIAL" exec-out screencap -p > "$BASELINE_DIR/$1.png" 2>/dev/null || true
}

run_case() { # case_name  ->  执行后续命令，记录 PASS/FAIL
  local name="$1"; shift
  echo "── TC: $name"
  if "$@"; then
    PASS=$((PASS+1)); echo "   PASS"
    capture_baseline "$name"
  else
    FAIL=$((FAIL+1)); FAILED_CASES+=("$name"); echo "   FAIL"
    collect_evidence "$name"
  fi
}

# ===================== 导航到发布器页 =====================
# 事件驱动等待：轮询 /view-tree 直到响应成功且内容（按字节长度）连续 2 次一致 = 页面已稳定。
# 预算按真实时间（date deadline）而非次数；轮询用 5s 短 --max-time——
# uia2 冷启动时单次请求慢，短超时可快速失败重试，避免吃满 ENGINE_TIMEOUT_S(30s) 拖爆总时长。
wait_quiescence() { # [timeout_s]
  local timeout_s=${1:-20}
  local deadline=$(( $(date +%s) + timeout_s ))
  local prev=-1 cur stable=0
  while [ "$(date +%s)" -lt "$deadline" ]; do
    # 轮询专用短超时 curl（不走 engine_get 的 30s --max-time）
    local code
    code=$(curl -s --max-time 5 -o "$RESP_FILE" -w "%{http_code}" "$ENGINE_URL/view-tree?visible=true" 2>/dev/null || echo "000")
    if [ "$code" = "200" ]; then
      cur=$(wc -c < "$RESP_FILE" 2>/dev/null | tr -d ' ')
      if [ "${cur:-0}" -gt 50 ] && [ "$cur" = "$prev" ]; then
        stable=$((stable+1))
        [ "$stable" -ge 2 ] && return 0
      else
        stable=0
      fi
      prev="${cur:-0}"
    fi
    sleep 0.4
  done
  return 1
}

open_publisher() {
  # Appium 接管后 restartApp 停在 demo 首页；用 pageName 深链直达发布器页。
  "$ADB" -s "$DEVICE_SERIAL" shell am start -n "$APP_PACKAGE/$APP_ACTIVITY" --es pageName "$PAGE_NAME" >/dev/null 2>&1
  # 直接等目标节点出现：wait_quiescence 会反复请求完整 view-tree；CI 冷启动时 curl 超时虽断开，
  # 引擎侧 getPageSource 仍持续占用 uia2 队列，反而把下面的 findElements 堵住。
  # wait_visible 是单一、顺序的目标节点条件等待，不产生并发 page-source 请求。
  wait_visible mention_input 20000
}

# ===================== TC1-TC6 =====================
tc1_candidate_appears() {
  focus_input && input_text "@" && wait_visible mention_candidate_张三
}
tc2_select_insert() {
  # debug_text 源码已去掉装饰引号（Text("text = ${editorValue.text}")），
  # 不再破坏 UiAutomator2 的 XML text 提取，可正常断言原始输入文本。
  tap mention_candidate_张三 && sleep 0.3 && \
  assert_node_contains debug_mentions "(@张三,[0,3])" && \
  assert_node_contains debug_text "@张三 "
}
tc3_multi_no_drift() {
  # 关键：不要重新 focus（会移动光标），直接在 TC2 末尾光标处追加第二个 @，验证两个 @人共存不漂移。
  input_text "@" && wait_visible mention_candidate_李四 && \
  tap mention_candidate_李四 && sleep 0.3 && \
  assert_node_contains debug_mentions "(@张三,[0,3])" && \
  assert_node_contains debug_mentions "(@李四,"
}
tc4_two_stage_delete() {
  # 两段式删除（经原生 KRTextFieldView 拦截）：
  #   第 1 次 DEL 删尾部空格；第 2 次 DEL 选中光标处整段 @人（deleteState=MentionSelected）；第 3 次才真删。
  #   TC3 末尾光标在“李四”之后，故此处选中/删除的是李四。
  #   验证完整两段式：先选中、再真删（确保“没删掉”只是中间态，最终确实删得掉）。
  "$ADB" -s "$DEVICE_SERIAL" shell input keyevent 67 && sleep 0.3 && \
  "$ADB" -s "$DEVICE_SERIAL" shell input keyevent 67 && sleep 0.3 && \
  assert_node_contains debug_delete_state "MentionSelected" && \
  "$ADB" -s "$DEVICE_SERIAL" shell input keyevent 67 && sleep 0.3 && \
  assert_node_contains debug_mentions "(@张三" && \
  ! assert_node_contains debug_mentions "李四"
}
tc5_no_match_no_dropdown() {
  open_publisher && focus_input && input_text "@zzz" && sleep 0.3 && \
  assert_not_visible mention_candidate_张三
}
tc6_space_not_in_range() {
  open_publisher && focus_input && input_text "@" && wait_visible mention_candidate_张三 && \
  tap mention_candidate_张三 && sleep 0.3 && \
  assert_node_contains debug_mentions "(@张三,[0,3])" && \
  ! assert_node_contains debug_mentions "@张三 "
}
tc7_query_filters_candidate() {
  # 确定性验证“@ + 名字 过滤候选”路径（绕过输入法，直接注入正在输入 @张三 的中间态 @张）：
  #   triggerPos 命中 @，query="张"，KNOWN_MENTIONS.filter{startsWith("张")}=[张三]，
  #   故应弹出张三候选、且不弹李四（证明过滤生效，而非 TC1 那种全量列出）。
  #   用部分 query "@张" 而非完整 "@张三"：真实打字输到完整 "@张三" 会被 scanMentions 当成已完成 mention 而抑制弹窗。
  tap debug_inject_张 && sleep 0.3 && \
  wait_visible mention_candidate_张三 && \
  assert_not_visible mention_candidate_李四
}

# ===================== TC8-TC10（P1 边界/异常）=====================
tc8_mid_cursor_backspace_selects() {
  # 边界：光标停在 @人 中段退格，原生 KRTextFieldView 应选中整段 @人（deleteState=MentionSelected）。
  # "@张三" 共 3 字，点选后 text="@张三 " 光标在末尾位置 4；左移 3 次到位置 1（@ 与 张 之间，落在 [0,3] 内）再退格。
  open_publisher && focus_input && input_text "@" && wait_visible mention_candidate_张三 && \
  tap mention_candidate_张三 && sleep 0.3 && \
  for i in 1 2 3; do "$ADB" -s "$DEVICE_SERIAL" shell input keyevent 21; sleep 0.15; done && \
  "$ADB" -s "$DEVICE_SERIAL" shell input keyevent 67 && sleep 0.3 && \
  assert_node_contains debug_delete_state "MentionSelected"
}

tc9_interval_self_consistent() {
  # 最有价值：文本与区间自洽——对每个 mention 断言 debug_text.substring(start,end)==displayName。
  # 抓“UI 看着对、内部区间已错位”类隐蔽回归（off-by-one 等），是改坏→红实证那类 bug 的常驻防线。
  open_publisher && focus_input && input_text "@" && wait_visible mention_candidate_张三 && \
  tap mention_candidate_张三 && sleep 0.3 && \
  input_text "@" && wait_visible mention_candidate_李四 && \
  tap mention_candidate_李四 && sleep 0.3 || return 1
  local dt dm
  dt=$(node_text_value debug_text)
  dm=$(node_text_value debug_mentions)
  python3 - "$dt" "$dm" <<'PY'
import sys, re
text_raw, mentions_raw = sys.argv[1], sys.argv[2]
m = re.search(r'text\s*=\s*(.*)', text_raw, re.S)
text = m.group(1) if m else text_raw
mentions = re.findall(r'\((@[^,]+),\[(\d+),(\d+)\]\)', mentions_raw)
if not mentions:
    print("  TC9: 未解析到 mention"); sys.exit(1)
ok = True
for name, s, e in mentions:
    s, e = int(s), int(e)
    seg = text[s:e]
    match = (seg == name)
    print(f"  TC9: {name} [{s},{e}] -> substring={seg!r} {'OK' if match else 'MISMATCH'}")
    if not match: ok = False
sys.exit(0 if ok else 1)
PY
}

tc10_empty_backspace_no_crash() {
  # 异常：无 @人 时连续退格不应崩溃。断言：连退 5 次后 mention_input 仍可定位（app 存活）。
  open_publisher && focus_input || return 1
  for i in 1 2 3 4 5; do "$ADB" -s "$DEVICE_SERIAL" shell input keyevent 67; sleep 0.2; done
  wait_visible mention_input 3000
}

# ===================== 主流程 =====================
main() {
  echo "== 发布器 E2E 回归（Android happy path）=="
  echo "引擎: $ENGINE_URL  页面: $PAGE_NAME  设备: $DEVICE_SERIAL"
  echo "证据目录: $LOG_DIR"

  # 真机（vivo 等）uiautomator2 server 易进入“进程在但 HTTP 无响应”的僵尸态，
  # 导致 getPageSource 等命令 20s 超时。先强制停止并清理残留反向隧道，让 /start-session 干净重拉。
  "$ADB" -s "$DEVICE_SERIAL" shell am force-stop io.appium.uiautomator2.server 2>/dev/null || true
  "$ADB" -s "$DEVICE_SERIAL" shell am force-stop io.appium.uiautomator2.server.test 2>/dev/null || true
  "$ADB" -s "$DEVICE_SERIAL" reverse --remove tcp:8200 2>/dev/null || true

  # 切到 AppiumIME（原样提交英文，避开中文拼音把 @zzz 转写成 @z'z'z），
  # 结束由顶部 EXIT trap 切回原输入法，任何退出路径都不会遗留。
  "$ADB" -s "$DEVICE_SERIAL" shell ime enable "$TEST_IM" >/dev/null 2>&1 || true
  "$ADB" -s "$DEVICE_SERIAL" shell ime set "$TEST_IM" >/dev/null 2>&1 || true

  # 建立会话（restartApp 后停在首页，再深链进发布器）。
  # Appium 刚起时 /session 偶发 "Failed to fetch"（冷启动竞争），重试 2 次吸收。
  local c sess_tries=0
  while [ "$sess_tries" -lt 3 ]; do
    c=$(engine_post /start-session "{\"platform\":\"$PLATFORM\",\"appPackage\":\"$APP_PACKAGE\",\"appActivity\":\"$APP_ACTIVITY\",\"udid\":\"$DEVICE_SERIAL\",\"deviceName\":\"$DEVICE_SERIAL\"}" "$SESSION_TIMEOUT_S")
    resp_ok "$c" && break
    sess_tries=$((sess_tries+1))
    echo "start-session 第 $sess_tries 次失败，等 2s 重试..."
    sleep 2
  done
  if ! resp_ok "$c"; then
    echo "start-session 失败: $(cat "$RESP_FILE")"; exit 1
  fi

  # 部分 ROM（vivo 等）Appium 不会自动建立 uiautomator2 的 8200 反向隧道，手动补上，
  # 否则后续 view-tree / tap / input 等命令会 20s 超时。
  "$ADB" -s "$DEVICE_SERIAL" reverse tcp:8200 tcp:8200 2>/dev/null || true

  # 打开发布器页；首次失败则彻底重建（强制重启 uia2 两组件 + 重建 Appium session）后重试。
  # 诊断输出用于分诊：8200 隧道 / uia2 进程 / 前台 activity / 原生 dump 是否含 mention_input。
  open_publisher || {
    echo "首次打开发布器页失败，诊断 + 重建 uia2/session 后重试..."
    echo "   diag: 8200=$("$ADB" -s "$DEVICE_SERIAL" reverse --list 2>&1 | tr '\n' ' ')"
    echo "   diag: uia2 进程："; "$ADB" -s "$DEVICE_SERIAL" shell ps -A 2>/dev/null | grep -E "uiautomator2" || echo "   (无 uiautomator2 进程)"
    echo "   diag: 前台 activity：" ; "$ADB" -s "$DEVICE_SERIAL" shell dumpsys activity activities 2>/dev/null | grep -E "topResumedActivity|mResumedActivity" | head -3
    # 关键分诊：人眼可见页面不等于 Android 无障碍树已经下发 testTag。
    # 保存原生 dump + 截图，并在控制台打印 resource-id 摘要，确认是「节点未下发」还是 Appium 通道问题。
    local diag_dir="$LOG_DIR/open_publisher_failure"
    mkdir -p "$diag_dir" 2>/dev/null || true
    echo "   diag: 原生 uiautomator dump："
    "$ADB" -s "$DEVICE_SERIAL" shell uiautomator dump /sdcard/_diag.xml 2>&1 | tee "$diag_dir/native-dump-command.txt"
    "$ADB" -s "$DEVICE_SERIAL" exec-out cat /sdcard/_diag.xml > "$diag_dir/native-uiautomator.xml" 2>/dev/null || true
    "$ADB" -s "$DEVICE_SERIAL" exec-out screencap -p > "$diag_dir/screenshot.png" 2>/dev/null || true
    local mention_count debug_count xml_bytes
    mention_count=$(grep -o "mention_input" "$diag_dir/native-uiautomator.xml" 2>/dev/null | wc -l | tr -d ' ')
    debug_count=$(grep -o "debug_text" "$diag_dir/native-uiautomator.xml" 2>/dev/null | wc -l | tr -d ' ')
    xml_bytes=$(wc -c < "$diag_dir/native-uiautomator.xml" 2>/dev/null | tr -d ' ')
    echo "   diag: 原生 dump bytes=${xml_bytes:-0}, mention_input=${mention_count:-0}, debug_text=${debug_count:-0}"
    echo "   diag: 原生 dump 的 resource-id 摘要（最多 20 个）："
    grep -o 'resource-id="[^"]*"' "$diag_dir/native-uiautomator.xml" 2>/dev/null | sort -u | head -20 || true
    echo "   diag: 原生证据已存: $diag_dir"
    echo "   diag: appium 日志尾部（看 uia2 server 启动/报错）："
    tail -80 /tmp/appium_publisher.log 2>/dev/null | tee "$diag_dir/appium-tail.log" || echo "   (无 /tmp/appium_publisher.log)"
    # 彻底重建：强制停 uia2 两组件 → 重建 8200 → 重建 Appium session（重新拉 uia2 + restartApp）
    echo "   [重建] force-stop uia2 组件 + 重建 8200 + 重建 session..."
    "$ADB" -s "$DEVICE_SERIAL" shell am force-stop io.appium.uiautomator2.server 2>/dev/null || true
    "$ADB" -s "$DEVICE_SERIAL" shell am force-stop io.appium.uiautomator2.server.test 2>/dev/null || true
    "$ADB" -s "$DEVICE_SERIAL" reverse tcp:8200 tcp:8200 2>/dev/null || true
    local c2 sess2_tries=0
    while [ "$sess2_tries" -lt 3 ]; do
      c2=$(engine_post /start-session "{\"platform\":\"$PLATFORM\",\"appPackage\":\"$APP_PACKAGE\",\"appActivity\":\"$APP_ACTIVITY\",\"udid\":\"$DEVICE_SERIAL\",\"deviceName\":\"$DEVICE_SERIAL\"}" "$SESSION_TIMEOUT_S")
      resp_ok "$c2" && break
      sess2_tries=$((sess2_tries+1)); echo "   重建 session 第 $sess2_tries 次失败，等 2s 重试..."; sleep 2
    done
    sleep 2
    open_publisher
  } || { echo "无法打开发布器页"; exit 1; }

  run_case "TC1 输入@候选下拉出现"        tc1_candidate_appears
  run_case "TC2 点选张三插入并高亮区间"   tc2_select_insert
  run_case "TC3 再选李四双区间不漂移"     tc3_multi_no_drift
  run_case "TC4 两段式删除选中整段"       tc4_two_stage_delete
  # 重新打开干净页面跑否定/边界用例
  run_case "TC5 无匹配候选不弹出"         tc5_no_match_no_dropdown
  run_case "TC6 @后空格不计入高亮区间"    tc6_space_not_in_range
  run_case "TC7 @+名字过滤出对应候选"     tc7_query_filters_candidate
  run_case "TC8 光标中段退格选中整段"     tc8_mid_cursor_backspace_selects
  run_case "TC9 文本与区间自洽切片"       tc9_interval_self_consistent
  run_case "TC10 空列表连续退格不崩"      tc10_empty_backspace_no_crash

  engine_post /stop-session "{}" >/dev/null

  echo "────────────────────────────"
  echo "结果: PASS=$PASS  FAIL=$FAIL"
  [ "$FAIL" -eq 0 ] && echo "✅ 全部通过" || { echo "❌ 失败用例: ${FAILED_CASES[*]}"; echo "证据见: $LOG_DIR"; }
  rm -f "$RESP_FILE"
  [ "$FAIL" -eq 0 ]
}

main "$@"
