/**
 * LazyList prefetch E2E cases 9.3–9.12 (Android).
 *
 * 交互走 harness：testTag tap + scrollWithin(lazy_list)；断言读 logcat。
 * 见 .claude/skills/kuikly-mobile-test/SKILL.md（勿默认 adb 坐标绕过）。
 *
 * Run（从 checkout 根目录，产物写入 logs/）:
 *   cd .claude/skills/kuikly-mobile-test
 *   KUIKLY_REPO_ROOT=/path/to/checkout ANDROID_UDID=emulator-5554 npm run lazy-prefetch:e2e
 *   LAZY_PREFETCH_ONLY=9.11 npm run lazy-prefetch:e2e   # 仅跑指定用例（自动拉依赖）
 */

import { execSync, spawnSync } from "node:child_process"
import { appendFile, mkdir, writeFile } from "node:fs/promises"
import { AppiumMobileDriver } from "../../.claude/skills/kuikly-mobile-test/src/appium-mobile-driver.js"
import { formatSessionLogLine } from "../../.claude/skills/kuikly-mobile-test/src/evidence.js"
import type { UiTreeNode } from "../../.claude/skills/kuikly-mobile-test/src/mobile-driver.js"
import { LOGS_DIR, REPORTS_DIR } from "../../.claude/skills/kuikly-mobile-test/src/paths.js"
import {
  countComposedAheadEvents,
  EvidenceError,
  formatMetricsEvidence,
  parsePrefetchDemoLines,
  readLastPrefetchMetrics,
  readSettledHeadGap,
  readSettledIndexGap,
  summarizePrefetchTrace,
} from "./lazy-prefetch-metrics.js"
import {
  buildReport,
  POST_SCROLL_SETTLE_MS,
  runLazyPrefetchCases,
  type LazyPrefetchDeps,
  type ReverseScrollResult,
  type ScrollMetricsResult,
  type TestResult,
  type ViewTreeVisibleItems,
} from "./lazy-prefetch-run-cases.js"
import {
  cleanupLazyPrefetchLogs,
  createLazyPrefetchRunMeta,
  formatLazyPrefetchRunDialogueSummary,
  formatScopeLabel,
  parseOnlyCases,
  resolveCasesToRun,
} from "./lazy-prefetch-run-utils.js"

const APPIUM_URL = process.env.APPIUM_URL ?? "http://127.0.0.1:4723"
const APP_PACKAGE = process.env.ANDROID_APP_PACKAGE ?? "com.tencent.kuikly.android.demo"
const APP_ACTIVITY =
  process.env.ANDROID_APP_ACTIVITY ?? "com.tencent.kuikly.android.demo.KuiklyRenderActivity"
const DEVICE_UDID = process.env.ANDROID_UDID ?? "emulator-5554"
const PAGE_NAME = "LazyListPrefetchDemo"

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms))

function adb(args: string) {
  execSync(`adb -s ${DEVICE_UDID} ${args}`, { stdio: "pipe" })
}

function clearLogcat() {
  adb("logcat -c")
}

function readPrefetchLogcat() {
  const result = spawnSync(
    "adb",
    ["-s", DEVICE_UDID, "logcat", "-d", "-s", "System.out"],
    { encoding: "utf8" },
  )
  const lines = (result.stdout ?? "")
    .split("\n")
    .filter((l) => l.includes("LazyListPrefetchDemo"))
  return parsePrefetchDemoLines(lines)
}

function readPrefetchTraceLogcat(): string[] {
  const result = spawnSync(
    "adb",
    ["-s", DEVICE_UDID, "logcat", "-d", "-s", "System.out"],
    { encoding: "utf8" },
  )
  return (result.stdout ?? "")
    .split("\n")
    .filter((l) => l.includes("LazyListPrefetchTrace"))
}

/** 在 UiTreeNode 树里找第一个 testTag === target 的节点（深度优先）。 */
function findNodeByTestTag(root: UiTreeNode, target: string): UiTreeNode | null {
  if (root.testTag === target) return root
  for (const child of root.children) {
    const hit = findNodeByTestTag(child, target)
    if (hit) return hit
  }
  return null
}

/** 收集所有 type=="DivView" 的后代（保留嵌套关系，仅在节点匹配时收集）。 */
function collectDivDescendants(node: UiTreeNode, out: UiTreeNode[] = []): UiTreeNode[] {
  for (const child of node.children) {
    if (child.type === "DivView" || child.rawType === "DivView") out.push(child)
    collectDivDescendants(child, out)
  }
  return out
}

/**
 * 走 driver.getViewTree() 找 testTag="lazy_list" 的 LazyColumn 节点，
 * 用其 bounds 当 viewport，统计 DivView 子节点中高度 ≥ 180 的可见 item 数。
 * 旧实现走 adb uiautomator dump + content-desc=ScrollerView XML 正则，testTag 路径修复后直接用 view-tree 更稳。
 */
async function readViewTreeVisibleItems(
  driver: AppiumMobileDriver,
): Promise<ViewTreeVisibleItems> {
  const tree = await driver.getViewTree()
  const lazy = findNodeByTestTag(tree.tree, "lazy_list")
  if (!lazy || !lazy.bounds) {
    return {
      indices: [],
      maxIndex: -1,
      count: 0,
      scrollerViewport: null,
      visibleItemDivCount: 0,
    }
  }
  const [lazyX, lazyY, lazyW, lazyH] = lazy.bounds
  const vpTop = lazyY
  const vpBottom = lazyY + lazyH
  const scrollerViewport: [number, number] = [vpTop, vpBottom]

  const divs = collectDivDescendants(lazy)
  const itemTops: number[] = []
  for (const div of divs) {
    if (!div.bounds) continue
    const [, y1, , h] = div.bounds
    if (h < 180) continue
    const y2 = y1 + h
    if (y2 <= vpTop || y1 >= vpBottom) continue
    itemTops.push(y1)
  }

  // 同样的 div 可能因为嵌套被多次收集；按 (y, h) 去重一下。
  const seen = new Set<number>()
  const dedup = itemTops.filter((y) => {
    if (seen.has(y)) return false
    seen.add(y)
    return true
  })

  const visibleItemDivCount = dedup.length
  void lazyX
  void lazyW
  return {
    indices: dedup.map((_, i) => i),
    maxIndex: Math.max(visibleItemDivCount - 1, -1),
    count: visibleItemDivCount,
    scrollerViewport,
    visibleItemDivCount,
  }
}

/** 读 UI 上 prefetch_toggle 节点文本，判断当前是 ON 还是 OFF。不依赖 JS 端 tracker。 */
async function readPrefetchToggleOnFromUi(driver: AppiumMobileDriver): Promise<boolean> {
  let text = ""
  try {
    text = (await driver.getSelectorLabel({ testTag: "prefetch_toggle" })).trim()
  } catch {
    // 真机读 label 失败时保守按 OFF 处理，由 ensurePrefetch* 再点一次 toggle。
    return false
  }
  // demo 实际显示 "Prefetch: ON" / "Prefetch: OFF"，OFF 包含 "ON" 子串，必须先判 OFF。
  if (/\bOFF\b/.test(text)) return false
  if (/\bON\b/.test(text)) return true
  return false
}

function readLayoutVisibleFromLog(lines: string[]): { indices: number[]; max: number } {
  for (let i = lines.length - 1; i >= 0; i--) {
    const m = lines[i].match(/layoutVisible indices=\[([^\]]*)\] max=(\d+)/)
    if (!m) continue
    const indices =
      m[1].length === 0 ? [] : m[1].split(",").map((v) => Number.parseInt(v.trim(), 10))
    return { indices, max: Number.parseInt(m[2], 10) }
  }
  return { indices: [], max: -1 }
}

function formatViewTreeEvidence(label: string, viewTree: ViewTreeVisibleItems): string {
  return [
    `${label}:`,
    `  scrollerViewport=${viewTree.scrollerViewport ? `[${viewTree.scrollerViewport.join(",")}]` : "null"}`,
    `  viewTreeVisibleItemDivCount=${viewTree.visibleItemDivCount}`,
    `  viewTreeVisibleCount=${viewTree.count}`,
  ].join("\n")
}

function assertViewTreeMatchesPlaced(
  viewTree: ViewTreeVisibleItems,
  metrics: ReturnType<typeof readPrefetchLogcat>,
  label: string,
  opts: { allowPrefetchNativeHeadroom?: boolean } = {},
): void {
  const layoutVisible = readLayoutVisibleFromLog(metrics.lines)
  const evidence = [
    formatViewTreeEvidence(label, viewTree),
    `layoutVisibleFromLog max=${layoutVisible.max} count=${layoutVisible.indices.length}`,
    formatMetricsEvidence("metrics", metrics),
  ].join("\n\n")

  if (viewTree.visibleItemDivCount < 1) {
    throw new EvidenceError(`${label}: viewTree has no visible list item DivView rows`, evidence)
  }
  if (layoutVisible.max >= 0 && metrics.maxPlacedIndex >= 0 && layoutVisible.max !== metrics.maxPlacedIndex) {
    throw new EvidenceError(
      `${label}: layoutVisible max (${layoutVisible.max}) != placed max (${metrics.maxPlacedIndex})`,
      evidence,
    )
  }
  if (layoutVisible.indices.length >= 1) {
    const delta = viewTree.visibleItemDivCount - layoutVisible.indices.length
    if (opts.allowPrefetchNativeHeadroom) {
      if (delta < 0 || delta > 3) {
        throw new EvidenceError(
          `${label}: viewTree div count (${viewTree.visibleItemDivCount}) vs layoutVisible (${layoutVisible.indices.length}) delta=${delta}, expected 0..3 with prefetch`,
          evidence,
        )
      }
    } else if (Math.abs(delta) > 1) {
      throw new EvidenceError(
        `${label}: viewTree div count (${viewTree.visibleItemDivCount}) != layoutVisible count (${layoutVisible.indices.length})`,
        evidence,
      )
    }
  }
}

function launchActivity() {
  adb(`shell am start -n ${APP_PACKAGE}/${APP_ACTIVITY} --es pageName ${PAGE_NAME}`)
}

/** 收起软键盘，避免路由页 EditText 聚焦时 getPageSource / input 在真机上极慢。 */
function dismissAndroidKeyboard() {
  adb("shell input keyevent 4")
}

/**
 * 轻量判断是否在 LazyListPrefetchDemo（只查 testTag 节点是否存在，不 dump 整页 UI 树）。
 * 真机上整页 getPageSource 可能卡 1～2 分钟，不能用来做进页探测。
 */
async function waitForPrefetchDemoPage(
  driver: AppiumMobileDriver,
  timeoutMs: number,
): Promise<boolean> {
  try {
    await driver.waitFor({ testTag: "prefetch_toggle" }, timeoutMs)
    return true
  } catch {
    return false
  }
}

/**
 * 通过 router 首页 UI 跳转到 LazyListPrefetchDemo。
 *
 * 历史：旧 Android e2e 假设 `am start --es pageName ...` 会被 KuiklyRenderActivity 接管，
 * 直接打开指定 page。但这是 main 上 commit 32dbac41「android ksp support entry get pageName」
 * 之后才有的能力，更老分支 / fork 都没有，会停在 router 首页。
 *
 * 兼容做法：先用 waitFor(testTag=prefetch_toggle) 探测（已在目标页则直接返回）；
 * 否则 intent 直跳，再不行走 router 输入框 + 跳转2。
 */
async function navigateToPrefetchDemo(driver: AppiumMobileDriver): Promise<void> {
  if (await waitForPrefetchDemoPage(driver, 3000)) return

  launchActivity()
  if (await waitForPrefetchDemoPage(driver, 8000)) return

  for (let attempt = 0; attempt < 2; attempt++) {
    dismissAndroidKeyboard()
    await sleep(300)
    try {
      await driver.input({ xpath: "//android.widget.EditText" }, PAGE_NAME)
      await sleep(400)
      dismissAndroidKeyboard()
      await sleep(200)
      await driver.tap({ text: "跳转2" })
      if (await waitForPrefetchDemoPage(driver, 8000)) return
    } catch {
      launchActivity()
      await sleep(2500)
      if (await waitForPrefetchDemoPage(driver, 8000)) return
    }
  }
  throw new Error(
    `navigateToPrefetchDemo: 找不到 prefetch_toggle 节点（已经尝试 intent 直跳 + UI 跳转 2 轮）；` +
      `请检查 demo 是否加载、testTag 是否生效（debugUIInspector + RootNodeOwner semantics 修复）`,
  )
}

let scrollAreaPrimed = false

function invalidateScrollAreaPrime(): void {
  scrollAreaPrimed = false
}

/** 首次 scroll 前：等 lazy_list 可 find（harness scroll 内已有每次滑后 2s 盲等）。 */
async function waitScrollAreaReady(driver: AppiumMobileDriver): Promise<void> {
  const timeoutMs = Number(process.env.ANDROID_PRE_SCROLL_WAIT_MS ?? 8000)
  if (timeoutMs <= 0) return
  await driver.waitFor({ testTag: "lazy_list" }, timeoutMs)
}

/**
 * 用 driver.scroll() 在 LazyColumn 内做滚动。起终点按 lazy_list 的 rect 上下沿计算，
 * 留 5% margin 防 release 出列被判作 cancel。`opts.fling=true` 会触发惯性 fling
 * （driver 端去掉 leading pause + clamp duration ≤200ms）。
 */
async function scrollList(
  driver: AppiumMobileDriver,
  direction: "down" | "up",
  times = 3,
  opts: { fling?: boolean; durationMs?: number; settleMs?: number } = {},
) {
  if (!scrollAreaPrimed) {
    await waitScrollAreaReady(driver)
    scrollAreaPrimed = true
  }
  await driver.scrollWithin(
    { testTag: "lazy_list" },
    {
      direction,
      times,
      fling: opts.fling ?? true,
      durationMs: opts.durationMs,
      ...(opts.settleMs !== undefined ? { settleMs: opts.settleMs } : {}),
    },
  )
}

async function tapTestTag(driver: AppiumMobileDriver, tag: string) {
  if (tag === "scenario_modifier_opt_in" || tag === "reset_counts") {
    invalidateScrollAreaPrime()
  }
  await driver.tap({ testTag: tag })
}

/**
 * 按 text/accessibilityId 点击。Compose 在 Android 上把 testTag 暴露为 contentDescription，
 * Text 节点同时携带 text。比 fixed coord 更稳，跨设备/密度不会失效。
 */
async function tapText(driver: AppiumMobileDriver, text: string) {
  try {
    await driver.tap({ text })
  } catch {
    // 某些 Compose 节点只有 contentDescription/testTag 暴露
    await driver.tap({ accessibilityId: text })
  }
  await sleep(700)
}

class SessionLogWriter {
  constructor(private readonly path: string) {}

  get filePath(): string {
    return this.path
  }

  async write(message: string): Promise<void> {
    await appendFile(this.path, `${formatSessionLogLine(message)}\n`)
  }
}

function sessionLogPath(stamp: string): string {
  return `${LOGS_DIR}/mobile_test_session_android_prefetch_${stamp}.log`
}

function createAndroidDeps(
  driver: AppiumMobileDriver,
  prefetchUiState: { value: boolean },
): LazyPrefetchDeps {
  return {
    platform: "android",

    async openDemo() {
      // 先 adb 直跳，避免 startSession 把 App 拉回路由页后再用慢路径导航
      launchActivity()
      await sleep(1500)
      await driver.startSession()
      await navigateToPrefetchDemo(driver)
      prefetchUiState.value = false
    },

    clearLog() {
      clearLogcat()
    },

    readDemoMetrics() {
      return readPrefetchLogcat()
    },

    readTraceLines() {
      return readPrefetchTraceLogcat()
    },

    async captureIdleWindow(label, opts = {}) {
      const settleMs = opts.settleMs ?? 2000
      const idleMs = opts.idleMs ?? 3500
      await sleep(settleMs)
      clearLogcat()
      await sleep(idleMs)
      const metrics = readPrefetchLogcat()
      return { metrics, evidence: formatMetricsEvidence(label, metrics) }
    },

    async readMetricsAfterScroll(label, scrollTimes): Promise<ScrollMetricsResult> {
      clearLogcat()
      await scrollList(driver, "down", scrollTimes)
      await sleep(POST_SCROLL_SETTLE_MS)
      const metrics = readPrefetchLogcat()
      const viewTree = await readViewTreeVisibleItems(driver)
      const traceSummary = summarizePrefetchTrace(readPrefetchTraceLogcat())
      return {
        metrics,
        evidence: [
          formatMetricsEvidence(label, metrics),
          formatViewTreeEvidence("viewTree visible (driver)", viewTree),
          traceSummary,
        ].join("\n\n"),
        lastLead: readLastPrefetchMetrics(metrics),
        aheadEvents: countComposedAheadEvents(metrics),
        viewTree,
        traceSummary,
      }
    },

    async readReverseScrollPhase(label, opts): Promise<ReverseScrollResult> {
      if (opts.prefetchOn) {
        await this.ensurePrefetchOn()
      } else {
        await this.ensurePrefetchOff()
      }
      await tapTestTag(driver, "clear_metrics_only")
      clearLogcat()
      if (opts.establishForwardScroll) {
        await scrollList(driver, "down", 1)
        await sleep(300)
      }
      await scrollList(driver, "up", 4)
      await sleep(POST_SCROLL_SETTLE_MS)
      const metrics = readPrefetchLogcat()
      const traceLines = readPrefetchTraceLogcat()
      return {
        metrics,
        traceLines,
        headGap: readSettledHeadGap(metrics),
        tailGap: readSettledIndexGap(metrics),
        lastLead: readLastPrefetchMetrics(metrics),
        evidence: [
          formatMetricsEvidence(label, metrics),
          summarizePrefetchTrace(traceLines),
        ].join("\n\n"),
      }
    },

    tapScenarioModifierOptIn: async () => {
      await tapTestTag(driver, "scenario_modifier_opt_in")
      prefetchUiState.value = false
    },
    tapScenarioGlobalOnly: () => tapTestTag(driver, "scenario_global_only"),
    tapScenarioModifierOverrideOff: () => tapTestTag(driver, "scenario_modifier_override_off"),
    tapScenarioCacheWindow: () => tapTestTag(driver, "scenario_cache_window"),
    tapHeavyItemsToggle: () => tapTestTag(driver, "heavy_items_toggle"),
    tapResetCounts: () => tapTestTag(driver, "reset_counts"),
    tapClearMetricsOnly: () => tapTestTag(driver, "clear_metrics_only"),

    async ensurePrefetchOff() {
      const on = await readPrefetchToggleOnFromUi(driver)
      if (on) {
        await driver.tap({ testTag: "prefetch_toggle" })
      }
      prefetchUiState.value = false
    },

    async ensurePrefetchOn() {
      if (!prefetchUiState.value) {
        await driver.tap({ testTag: "prefetch_toggle" })
        prefetchUiState.value = true
        return
      }
      const on = await readPrefetchToggleOnFromUi(driver)
      if (!on) {
        await driver.tap({ testTag: "prefetch_toggle" })
      }
      prefetchUiState.value = true
    },

    scrollList: (direction, times, opts) => scrollList(driver, direction, times, opts),
    setPrefetchUiState(value: boolean) {
      prefetchUiState.value = value
    },
    assertViewTreeMatchesPlaced,
  }
}

async function main() {
  await mkdir(REPORTS_DIR, { recursive: true })
  await mkdir(LOGS_DIR, { recursive: true })

  const runMeta = createLazyPrefetchRunMeta()
  const onlyCases = parseOnlyCases()
  const onlySet = resolveCasesToRun(onlyCases)
  const scopeLabel = formatScopeLabel(onlySet)

  await cleanupLazyPrefetchLogs("android", LOGS_DIR)

  const sessionLog = new SessionLogWriter(sessionLogPath(runMeta.stamp))
  await writeFile(
    sessionLog.filePath,
    `${formatSessionLogLine(`SESSION START platform=android goal="LazyList Prefetch E2E (${PAGE_NAME})" scope="${scopeLabel}"`)}\n`,
  )

  const prefetchUiState = { value: false }
  const driver = new AppiumMobileDriver({
    platform: "android",
    appiumUrl: APPIUM_URL,
    appPackage: APP_PACKAGE,
    appActivity: APP_ACTIVITY,
    androidPageName: PAGE_NAME,
    deviceName: DEVICE_UDID,
    udid: DEVICE_UDID,
    lockPortrait: process.env.ANDROID_LOCK_PORTRAIT !== "0",
  })

  try {
    const deps = createAndroidDeps(driver, prefetchUiState)
    const results = await runLazyPrefetchCases(deps, sessionLog, { only: onlyCases })

    const failed = results.filter((r) => r.status === "failed").length
    const reportPath = `${REPORTS_DIR}/lazy_prefetch_e2e_${runMeta.stamp}.md`
    const report = buildReport(
      "Android",
      DEVICE_UDID,
      PAGE_NAME,
      sessionLog.filePath,
      results,
      runMeta.label,
      scopeLabel,
    )
    await writeFile(reportPath, report)
    await sessionLog.write(
      `SESSION END 结果=${failed > 0 ? "不通过" : "通过"} 报告=${reportPath}`,
    )
    console.log(
      formatLazyPrefetchRunDialogueSummary({
        platform: "Android",
        runLabel: runMeta.label,
        scopeLabel,
        results,
        reportPath,
        sessionLogPath: sessionLog.filePath,
      }),
    )
    if (failed > 0) process.exit(1)
  } finally {
    try {
      await driver.stopSession()
    } catch {}
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
