/**
 * LazyList prefetch E2E cases 9.3–9.12 (Android).
 *
 * Kuikly Compose testTag is not exposed as content-desc when debugUIInspector is on,
 * so this scenario uses coordinate taps + logcat metrics.
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
import type {
  ElementRect,
  UiTreeNode,
} from "../../.claude/skills/kuikly-mobile-test/src/mobile-driver.js"
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

/**
 * 默认坐标按 emulator (1080x2400, Pixel-style) 校准。
 * 真机厂商 ROM 状态栏 / 行高差异较大，需要用 env 覆盖：
 *   LAZY_PREFETCH_ANDROID_TAP_PROFILE=vivo
 * 或单个坐标覆盖：
 *   LAZY_PREFETCH_TAP_PREFETCH_TOGGLE_Y=350
 */
const TAP_PROFILES: Record<string, Record<string, { x: number; y: number }>> = {
  default: {
    prefetchToggle: { x: 540, y: 321 },
    scenarioModifierOptIn: { x: 540, y: 482 },
    scenarioGlobalOnly: { x: 540, y: 567 },
    scenarioModifierOverrideOff: { x: 540, y: 652 },
    scenarioCacheWindow: { x: 540, y: 737 },
    heavyItemsToggle: { x: 540, y: 822 },
    resetCounts: { x: 540, y: 907 },
    clearMetricsOnly: { x: 540, y: 992 },
  },
  // 实测自 vivo PD2141 (V2141A, 1080x2400, 480dpi)
  vivo: {
    prefetchToggle: { x: 540, y: 350 },
    scenarioModifierOptIn: { x: 540, y: 520 },
    scenarioGlobalOnly: { x: 540, y: 615 },
    scenarioModifierOverrideOff: { x: 540, y: 710 },
    scenarioCacheWindow: { x: 540, y: 803 },
    heavyItemsToggle: { x: 540, y: 895 },
    resetCounts: { x: 540, y: 985 },
    clearMetricsOnly: { x: 540, y: 1075 },
  },
}
const TAP_PROFILE = process.env.LAZY_PREFETCH_ANDROID_TAP_PROFILE ?? "default"
const TAP = TAP_PROFILES[TAP_PROFILE] ?? TAP_PROFILES.default!

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
  const tree = await driver.getViewTree()
  const node = findNodeByTestTag(tree.tree, "prefetch_toggle")
  const text = (node?.text ?? "").trim()
  // demo 实际显示 "Prefetch: ON" / "Prefetch: OFF"，OFF 包含 "ON" 子串，必须先判 OFF。
  if (/\bOFF\b/.test(text)) return false
  if (/\bON\b/.test(text)) return true
  // 兜底：accessibility 树未拿到 text 时按 OFF 处理（保守，调用方会重新点击）
  return false
}

/** 通过 testTag 拿 LazyColumn 屏幕矩形；scrollList / fling 用它算起终点（贴上下沿留 5% 边距）。 */
async function getLazyListBounds(driver: AppiumMobileDriver): Promise<ElementRect> {
  return driver.getElementRect({ testTag: "lazy_list" })
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

/**
 * 通过 router 首页 UI 跳转到 LazyListPrefetchDemo。
 *
 * 历史：旧 Android e2e 假设 `am start --es pageName ...` 会被 KuiklyRenderActivity 接管，
 * 直接打开指定 page。但这是 main 上 commit 32dbac41「android ksp support entry get pageName」
 * 之后才有的能力，更老分支 / fork 都没有，会停在 router 首页。
 *
 * 兼容做法（与 iOS 同套路）：拿到 view-tree 后判一下 prefetch_toggle 是否已经存在；不在就
 * 走 router 输入框 + 跳转2 按钮的 UI 路径。这样在 hasIntentEntry 与无之间都能跑。
 */
async function navigateToPrefetchDemo(driver: AppiumMobileDriver): Promise<void> {
  for (let attempt = 0; attempt < 3; attempt++) {
    try {
      const tree = await driver.getViewTree()
      if (findNodeByTestTag(tree.tree, "prefetch_toggle")) return
    } catch {
      // first call after launch may race with view ready；不影响后续 attempt
    }

    if (attempt === 0) {
      // 优先 intent 直跳（main 已支持），不行也无所谓，下面退回 UI 路径
      launchActivity()
      await sleep(2500)
      continue
    }

    // UI 导航：输入框 + 跳转2
    try {
      await driver.input(
        { xpath: "//android.widget.EditText" },
        PAGE_NAME,
      )
      await sleep(400)
      await driver.tap({ text: "跳转2" })
      await sleep(4000)
    } catch {
      // 极端情况下输入框拿不到，再启 activity 重试
      launchActivity()
      await sleep(2500)
    }
  }
  throw new Error(
    `navigateToPrefetchDemo: 找不到 prefetch_toggle 节点（已经尝试 intent 直跳 + UI 跳转 3 轮）；` +
      `请检查 demo 是否加载、testTag 是否生效（debugUIInspector + RootNodeOwner semantics 修复）`,
  )
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
  opts: { fling?: boolean; durationMs?: number } = {},
) {
  const rect = await getLazyListBounds(driver)
  const centerX = rect.x + rect.width / 2
  const margin = rect.height * 0.05
  const top = rect.y + margin
  const bottom = rect.y + rect.height - margin
  const startY = direction === "down" ? bottom : top
  const endY = direction === "down" ? top : bottom
  const fling = opts.fling === true
  const durationMs = opts.durationMs ?? (fling ? 150 : 450)
  const settleMs = fling ? 1200 : 700
  // area 透传给 driver：Android+fling 走 `mobile: swipeGesture` 需要真实可滚动 viewport rect，
  // 否则 driver 会按 start/end 反推一个窄包围盒，emulator 上 release 易被判作 cancel。
  for (let i = 0; i < times; i++) {
    await driver.scroll({
      startX: centerX,
      startY,
      endX: centerX,
      endY,
      durationMs,
      fling,
      area: rect,
    })
    await sleep(settleMs)
  }
}

async function tapCoord(_driver: AppiumMobileDriver, point: { x: number; y: number }) {
  adb(`shell input tap ${point.x} ${point.y}`)
  await sleep(900)
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
      try {
        await driver.startSession()
      } catch {
        // Appium 起不来时跑不了 view-tree-based 导航；走原 adb am start 兜底，
        // 后续 ensure*/scroll 也会失败，但保留诊断空间。
        launchActivity()
        await sleep(2500)
        return
      }
      // intent 直跳 + 必要时走 router UI 跳转，兜到 prefetch_toggle 出现为止
      await navigateToPrefetchDemo(driver)
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
      await tapCoord(driver, TAP.clearMetricsOnly)
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

    tapScenarioModifierOptIn: () => tapCoord(driver, TAP.scenarioModifierOptIn),
    tapScenarioGlobalOnly: () => tapCoord(driver, TAP.scenarioGlobalOnly),
    tapScenarioModifierOverrideOff: () => tapCoord(driver, TAP.scenarioModifierOverrideOff),
    tapScenarioCacheWindow: () => tapCoord(driver, TAP.scenarioCacheWindow),
    tapHeavyItemsToggle: () => tapCoord(driver, TAP.heavyItemsToggle),
    tapResetCounts: () => tapCoord(driver, TAP.resetCounts),
    tapClearMetricsOnly: () => tapCoord(driver, TAP.clearMetricsOnly),

    async ensurePrefetchOff() {
      // 读 prefetch_toggle 节点真值，避免和 JS 端 prefetchUiState tracker 漂移
      // （`scenario_modifier_opt_in` 会把全局 flag 关掉，但 modifier 路径仍要求 ON，
      // 跨用例时 tracker 容易和真实 UI 状态不同步）。
      const on = await readPrefetchToggleOnFromUi(driver)
      if (on) {
        await driver.tap({ testTag: "prefetch_toggle" })
        await sleep(700)
      }
      prefetchUiState.value = false
    },

    async ensurePrefetchOn() {
      const on = await readPrefetchToggleOnFromUi(driver)
      if (!on) {
        await driver.tap({ testTag: "prefetch_toggle" })
        await sleep(700)
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
    deviceName: DEVICE_UDID,
    udid: DEVICE_UDID,
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
