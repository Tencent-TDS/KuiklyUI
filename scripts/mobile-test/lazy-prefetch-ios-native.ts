/**
 * LazyList prefetch iOS — same cross-platform cases 9.3–9.11 as Android.
 *
 * Log: simctl launch --console-pty (K/N println) + Demo UI metrics.
 *
 * Run:
 *   cd .claude/skills/kuikly-mobile-test
 *   KUIKLY_REPO_ROOT=/path/to/checkout \
 *   IOS_UDID=<simulator-udid> \
 *   IOS_BUNDLE_ID=com.tencent.kuiklycore.demo.luoyibu \
 *   npm run lazy-prefetch:ios-native
 *   LAZY_PREFETCH_ONLY=9.11 npm run lazy-prefetch:ios-native   # 仅跑指定用例
 */

import { appendFile, mkdir, writeFile } from "node:fs/promises"
import { AppiumMobileDriver } from "../../.claude/skills/kuikly-mobile-test/src/appium-mobile-driver.js"
import { formatSessionLogLine } from "../../.claude/skills/kuikly-mobile-test/src/evidence.js"
import { LOGS_DIR, REPORTS_DIR } from "../../.claude/skills/kuikly-mobile-test/src/paths.js"
import { IosConsoleLog } from "./lazy-prefetch-ios-log.js"
import {
  countComposedAheadEvents,
  formatMetricsEvidence,
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
  type UiPrefetchSpotMetrics,
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
const SIM_UDID = process.env.IOS_UDID ?? "C3D05CEF-97DA-4A7F-8E3E-2280BAEA6DD8"
const BUNDLE_ID = process.env.IOS_BUNDLE_ID ?? "com.tencent.kuiklycore.demo.luoyibu"
const PLATFORM_VERSION = process.env.IOS_PLATFORM_VERSION ?? "18.6"
const DEVICE_NAME = process.env.IOS_DEVICE_NAME ?? "iPhone 16"
const PAGE_NAME = "LazyListPrefetchDemo"

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms))

interface UiMetrics {
  indexLead: number
  headGap: number
  composedCount: number
  placedCount: number
  prefetchOn: boolean
  prefetchTargetIndex: number
  prefetchTargetEnterCount: number
  prefetchTargetSource: string
  prefetchTargetPlaced: number
  prefetchTargetPipeline: number
  prefetchPipelineReentryCount: number
  compositionReentryTotal: number
  maxComposedIndex: number
  maxPlacedIndex: number
}

function parseUiMetrics(snapshot: Awaited<ReturnType<AppiumMobileDriver["getSnapshot"]>>): UiMetrics {
  const texts = snapshot.elements.map((e) => e.text ?? "").filter(Boolean)
  const pick = (prefix: string) => {
    const line = texts.find((t) => t.startsWith(prefix))
    if (!line) return 0
    return Number.parseInt(line.slice(prefix.length), 10)
  }
  const pickText = (prefix: string, fallback = "none") => {
    const line = texts.find((t) => t.startsWith(prefix))
    if (!line) return fallback
    return line.slice(prefix.length)
  }
  const pickOptional = (prefix: string, fallback: number) => {
    const line = texts.find((t) => t.startsWith(prefix))
    if (!line) return fallback
    return Number.parseInt(line.slice(prefix.length), 10)
  }
  const prefetchLine = texts.find((t) => t.startsWith("Prefetch:"))
  return {
    indexLead: pick("prefetch_index_lead="),
    headGap: pick("prefetch_head_gap="),
    composedCount: pick("composed_count="),
    placedCount: pick("placed_count="),
    prefetchOn: prefetchLine?.includes("ON") ?? false,
    prefetchTargetIndex: pickOptional("prefetch_target_index=", -1),
    prefetchTargetEnterCount: pick("prefetch_target_enter_count="),
    prefetchTargetSource: pickText("prefetch_target_source="),
    prefetchTargetPlaced: pick("prefetch_target_placed="),
    prefetchTargetPipeline: pick("prefetch_target_pipeline="),
    prefetchPipelineReentryCount: pick("prefetch_pipeline_reentry_count="),
    compositionReentryTotal: pick("composition_reentry_total="),
    maxComposedIndex: pick("max_composed_index="),
    maxPlacedIndex: pick("max_placed_index="),
  }
}

function formatUiEvidence(label: string, m: UiMetrics): string {
  return [
    `${label} (Demo UI):`,
    `  prefetch_index_lead=${m.indexLead}`,
    `  prefetch_head_gap=${m.headGap}`,
    `  composed_count=${m.composedCount}`,
    `  placed_count=${m.placedCount}`,
    `  prefetch_switch=${m.prefetchOn ? "ON" : "OFF"}`,
  ].join("\n")
}

class SessionLogWriter {
  constructor(private readonly path: string) {}

  async write(message: string): Promise<void> {
    await appendFile(this.path, `${formatSessionLogLine(message)}\n`)
  }
}

function createIosDeps(
  driver: AppiumMobileDriver,
  iosLog: IosConsoleLog,
  prefetchUiState: { value: boolean },
): LazyPrefetchDeps {
  async function tapText(text: string) {
    await driver.tap({ text })
    await sleep(700)
  }

  async function tapPrefetchToggle() {
    await driver.tap({
      xpath: "//XCUIElementTypeStaticText[contains(@label,'Prefetch:')]",
    })
    await sleep(700)
    prefetchUiState.value = !prefetchUiState.value
  }

  async function scrollList(
    direction: "down" | "up",
    times = 3,
    opts: { fling?: boolean; durationMs?: number } = {},
  ) {
    const rect = await driver.getElementRect({
      xpath: "(//XCUIElementTypeScrollView)[last()]",
    })
    const centerX = rect.x + rect.width / 2
    // 留 5% margin 防 release 出列。fling 模式贴上下沿（大位移 + 短 duration），
    // 普通模式收 25/75，避免 OS 把 release 出列判作 cancel。
    const fling = opts.fling === true
    const lo = fling ? 0.05 : 0.25
    const hi = fling ? 0.95 : 0.75
    const startY = direction === "down" ? rect.y + rect.height * hi : rect.y + rect.height * lo
    const endY = direction === "down" ? rect.y + rect.height * lo : rect.y + rect.height * hi
    const durationMs = opts.durationMs ?? (fling ? 150 : 450)
    const settleMs = fling ? 1200 : 700
    for (let i = 0; i < times; i++) {
      await driver.scroll({ startX: centerX, startY, endX: centerX, endY, durationMs, fling })
      await sleep(settleMs)
    }
  }

  async function readUi(): Promise<UiMetrics> {
    return parseUiMetrics(await driver.getSnapshot())
  }

  return {
    platform: "ios",

    async openDemo() {
      try {
        await driver.startSession()
      } catch {
        // Appium optional; gestures + console-pty log remain primary.
      }
      iosLog.relaunchApp(BUNDLE_ID)
      await sleep(2000)
      const snap = await driver.getSnapshot()
      if (snap.elements.some((e) => e.text === "scenario_modifier_opt_in")) {
        prefetchUiState.value = snap.elements.some(
          (e) => (e.text ?? "").startsWith("Prefetch:") && (e.text ?? "").includes("ON"),
        )
        return
      }
      await driver.input({ xpath: "//XCUIElementTypeTextField" }, PAGE_NAME)
      await sleep(400)
      await driver.tap({ text: "跳转2" })
      await sleep(4000)
      await driver.assertText("scenario_modifier_opt_in")
      prefetchUiState.value = false
    },

    clearLog() {
      iosLog.clear()
    },

    readDemoMetrics() {
      return iosLog.readDemoMetrics()
    },

    readTraceLines() {
      return iosLog.readTraceLines()
    },

    async captureIdleWindow(label, opts = {}) {
      const settleMs = opts.settleMs ?? 2000
      const idleMs = opts.idleMs ?? 3500
      await sleep(settleMs)
      iosLog.clear()
      await sleep(idleMs)
      const metrics = iosLog.readDemoMetrics()
      const ui = await readUi()
      return {
        metrics,
        evidence: [formatMetricsEvidence(label, metrics), formatUiEvidence("idle UI", ui)].join("\n\n"),
      }
    },

    async readMetricsAfterScroll(label, scrollTimes): Promise<ScrollMetricsResult> {
      iosLog.clear()
      await scrollList("down", scrollTimes)
      await sleep(POST_SCROLL_SETTLE_MS)
      const metrics = iosLog.readDemoMetrics()
      const ui = await readUi()
      const traceSummary = summarizePrefetchTrace(iosLog.readTraceLines())
      return {
        metrics,
        evidence: [
          formatMetricsEvidence(label, metrics),
          formatUiEvidence(label, ui),
          traceSummary,
        ].join("\n\n"),
        lastLead: readLastPrefetchMetrics(metrics) || ui.indexLead,
        aheadEvents: countComposedAheadEvents(metrics),
        traceSummary,
        uiTailGap: ui.indexLead,
      }
    },

    async readReverseScrollPhase(label, opts): Promise<ReverseScrollResult> {
      if (opts.prefetchOn) {
        await this.ensurePrefetchOn()
      } else {
        await this.ensurePrefetchOff()
      }
      await tapText("clear_metrics_only")
      iosLog.clear()
      if (opts.establishForwardScroll) {
        await scrollList("down", 1)
        await sleep(300)
      }
      await scrollList("up", 4)
      await sleep(POST_SCROLL_SETTLE_MS)
      const metrics = iosLog.readDemoMetrics()
      const traceLines = iosLog.readTraceLines()
      const ui = await readUi()
      const headGap = readSettledHeadGap(metrics) || ui.headGap
      return {
        metrics,
        traceLines,
        headGap,
        tailGap: readSettledIndexGap(metrics) || ui.indexLead,
        lastLead: readLastPrefetchMetrics(metrics) || ui.indexLead,
        evidence: [
          formatMetricsEvidence(label, metrics),
          formatUiEvidence(label, ui),
          summarizePrefetchTrace(traceLines),
        ].join("\n\n"),
        uiHeadGap: ui.headGap,
      }
    },

    tapScenarioModifierOptIn: () => tapText("scenario_modifier_opt_in"),
    tapScenarioGlobalOnly: () => tapText("scenario_global_only"),
    tapScenarioModifierOverrideOff: () => tapText("scenario_modifier_override_off"),
    tapScenarioCacheWindow: () => tapText("scenario_cache_window"),
    async tapHeavyItemsToggle() {
      const snap = await driver.getSnapshot()
      const label = snap.elements.find((e) => (e.text ?? "").startsWith("heavy_items:"))?.text
      await tapText(label ?? "heavy_items: OFF")
    },
    tapResetCounts: () => tapText("reset_counts"),
    tapClearMetricsOnly: () => tapText("clear_metrics_only"),

    async ensurePrefetchOn() {
      const ui = await readUi()
      if (!ui.prefetchOn) await tapPrefetchToggle()
      prefetchUiState.value = true
    },

    async ensurePrefetchOff() {
      const ui = await readUi()
      if (ui.prefetchOn) await tapPrefetchToggle()
      prefetchUiState.value = false
    },

    scrollList,
    setPrefetchUiState(value: boolean) {
      prefetchUiState.value = value
    },

    async readUiPrefetchSpotMetrics(): Promise<UiPrefetchSpotMetrics> {
      const ui = await readUi()
      return {
        maxComposedIndex: ui.maxComposedIndex,
        maxPlacedIndex: ui.maxPlacedIndex,
        prefetchTargetIndex: ui.prefetchTargetIndex,
        prefetchTargetEnterCount: ui.prefetchTargetEnterCount,
        prefetchTargetSource: ui.prefetchTargetSource,
        prefetchTargetPlaced: ui.prefetchTargetPlaced,
        prefetchTargetPipeline: ui.prefetchTargetPipeline,
        prefetchPipelineReentryCount: ui.prefetchPipelineReentryCount,
        compositionReentryTotal: ui.compositionReentryTotal,
        indexLead: ui.indexLead,
      }
    },
  }
}

async function main() {
  await mkdir(REPORTS_DIR, { recursive: true })
  await mkdir(LOGS_DIR, { recursive: true })

  const runMeta = createLazyPrefetchRunMeta()
  const onlyCases = parseOnlyCases()
  const onlySet = resolveCasesToRun(onlyCases)
  const scopeLabel = formatScopeLabel(onlySet)

  await cleanupLazyPrefetchLogs("ios", LOGS_DIR)

  const sessionPath = `${LOGS_DIR}/mobile_test_session_ios_prefetch_${runMeta.stamp}.log`
  const sessionLog = new SessionLogWriter(sessionPath)
  await writeFile(
    sessionPath,
    `${formatSessionLogLine(`SESSION START platform=ios goal="LazyList Prefetch E2E (${PAGE_NAME})" scope="${scopeLabel}"`)}\n`,
  )

  const consolePath = `${LOGS_DIR}/kuikly_ios_prefetch_console.log`
  const iosLog = new IosConsoleLog(SIM_UDID, consolePath)
  iosLog.start()

  const prefetchUiState = { value: false }
  const driver = new AppiumMobileDriver({
    platform: "ios",
    appiumUrl: APPIUM_URL,
    bundleId: BUNDLE_ID,
    deviceName: DEVICE_NAME,
    udid: SIM_UDID,
    platformVersion: PLATFORM_VERSION,
  })

  let results: TestResult[] = []
  try {
    const deps = createIosDeps(driver, iosLog, prefetchUiState)
    results = await runLazyPrefetchCases(deps, sessionLog, { only: onlyCases })

    const failed = results.filter((r) => r.status === "failed").length
    const reportPath = `${REPORTS_DIR}/lazy_prefetch_ios_native_${runMeta.stamp}.md`
    const report = buildReport(
      "iOS",
      `${DEVICE_NAME} (${SIM_UDID})`,
      PAGE_NAME,
      sessionPath,
      results,
      runMeta.label,
      scopeLabel,
    )
    await writeFile(reportPath, `${report}\n\n- Console log: ${consolePath}\n`)
    await sessionLog.write(
      `SESSION END 结果=${failed > 0 ? "不通过" : "通过"} 报告=${reportPath}`,
    )
    console.log(
      formatLazyPrefetchRunDialogueSummary({
        platform: "iOS",
        runLabel: runMeta.label,
        scopeLabel,
        results,
        reportPath,
        sessionLogPath: sessionPath,
        consoleLogPath: consolePath,
      }),
    )
    if (failed > 0) process.exit(1)
  } finally {
    iosLog.stop()
    try {
      await driver.stopSession()
    } catch {}
  }
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
