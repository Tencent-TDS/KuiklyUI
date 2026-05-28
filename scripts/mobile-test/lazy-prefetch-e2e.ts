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

function readViewTreeVisibleItems(): ViewTreeVisibleItems {
  adb("shell uiautomator dump /sdcard/kuikly_prefetch_ui.xml")
  const xml = execSync(`adb -s ${DEVICE_UDID} shell cat /sdcard/kuikly_prefetch_ui.xml`, {
    encoding: "utf8",
  })
  const scrollerMatch = xml.match(
    /content-desc="ScrollerView"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/,
  )
  const scrollerViewport: [number, number] | null = scrollerMatch
    ? [Number.parseInt(scrollerMatch[2], 10), Number.parseInt(scrollerMatch[4], 10)]
    : null

  const itemDivBounds: number[] = []
  const divPattern = /content-desc="DivView"[^>]*bounds="\[(\d+),(\d+)\]\[(\d+),(\d+)\]"/g
  let match: RegExpExecArray | null
  while ((match = divPattern.exec(xml)) !== null) {
    const y1 = Number.parseInt(match[2], 10)
    const y2 = Number.parseInt(match[4], 10)
    const height = y2 - y1
    if (height < 180) continue
    if (!scrollerViewport) continue
    const [vpTop, vpBottom] = scrollerViewport
    if (y2 <= vpTop || y1 >= vpBottom) continue
    itemDivBounds.push(y1)
  }

  const visibleItemDivCount = itemDivBounds.length
  return {
    indices: itemDivBounds.map((_, i) => i),
    maxIndex: Math.max(visibleItemDivCount - 1, -1),
    count: visibleItemDivCount,
    scrollerViewport,
    visibleItemDivCount,
  }
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

function launchPrefetchDemo() {
  adb(`shell am start -n ${APP_PACKAGE}/${APP_ACTIVITY} --es pageName ${PAGE_NAME}`)
}

async function scrollList(_driver: AppiumMobileDriver, direction: "down" | "up", times = 3) {
  const centerX = 540
  const startY = direction === "down" ? 1728 : 912
  const endY = direction === "down" ? 912 : 1728
  for (let i = 0; i < times; i++) {
    adb(`shell input swipe ${centerX} ${startY} ${centerX} ${endY} 450`)
    await sleep(700)
  }
}

/** 单次「长滑」手势：覆盖大部分屏幕高度，短 duration → 触发惯性 fling。 */
async function longSwipe(opts?: { durationMs?: number }) {
  const centerX = 540
  const startY = 2100
  const endY = 360
  const duration = opts?.durationMs ?? 180
  adb(`shell input swipe ${centerX} ${startY} ${centerX} ${endY} ${duration}`)
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
        // Appium optional; adb taps + logcat remain the primary assertion path.
      }
      launchPrefetchDemo()
      // 真机首次渲染 Compose 树需要时间，taps 太早会落到空白处。
      await sleep(2500)
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
      const viewTree = readViewTreeVisibleItems()
      const traceSummary = summarizePrefetchTrace(readPrefetchTraceLogcat())
      return {
        metrics,
        evidence: [
          formatMetricsEvidence(label, metrics),
          formatViewTreeEvidence("viewTree visible (uiautomator)", viewTree),
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
      if (prefetchUiState.value) {
        await tapCoord(driver, TAP.prefetchToggle)
        prefetchUiState.value = false
      }
    },

    async ensurePrefetchOn() {
      if (!prefetchUiState.value) {
        await tapCoord(driver, TAP.prefetchToggle)
        prefetchUiState.value = true
      }
    },

    scrollList: (direction, times) => scrollList(driver, direction, times),
    longSwipe,
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
