/**
 * Cross-platform LazyList prefetch E2E cases 9.3–9.12.
 * Platform adapters implement log I/O and gestures.
 */

import {
  caseCriterion,
  formatCaseEvidence,
  formatCaseSummary,
} from "../../.claude/skills/kuikly-mobile-test/src/evidence.js"

interface SessionLogWriter {
  write(message: string): Promise<void>
}
import {
  classifyCompositionEvents,
  countComposedAheadEvents,
  countExecuteComposedInTrace,
  countTraceLines,
  evaluateContinuation,
  evaluatePrefetchIronEvidence,
  evaluateSchedulerBudget,
  EvidenceError,
  findReverseComposeEvidence,
  formatMetricsEvidence,
  indexGap,
  maxLeadOnComposedLines,
  MIN_CACHE_WINDOW_SETTLED_GAP,
  parseTraceCounts,
  readLastPrefetchMetrics,
  logHasPlacedIndex,
  pickPrefetchEvidenceIndex,
  readSettledHeadGap,
  readSettledIndexGap,
  summarizeIdleFrameTrace,
  summarizePrefetchTrace,
  type LogMetrics,
} from "./lazy-prefetch-metrics.js"
import {
  formatScopeLabel,
  resolveCasesToRun,
  shouldRunCase,
} from "./lazy-prefetch-run-utils.js"

export interface RunLazyPrefetchOptions {
  /** e.g. ['9.11']; null = full suite; undefined = read LAZY_PREFETCH_ONLY env */
  only?: string[] | null
}

export const POST_SCROLL_SETTLE_MS = 500
/** Fast fling-like scroll gap for 9.12 (minimal idle between gestures). */
export const FAST_SCROLL_GAP_MS = 80
/** Idle window after fast scroll so prefetch can compose on idle frames (9.12). */
export const PREFETCH_IDLE_SETTLE_MS = 1500

export interface TestResult {
  id: string
  name: string
  status: "passed" | "failed" | "skipped"
  detail?: string
  evidence?: string
  durationMs?: number
}

export interface ViewTreeVisibleItems {
  indices: number[]
  maxIndex: number
  count: number
  scrollerViewport: [number, number] | null
  visibleItemDivCount: number
}

export interface ScrollMetricsResult {
  metrics: LogMetrics
  evidence: string
  lastLead: number
  aheadEvents: number
  viewTree?: ViewTreeVisibleItems
  traceSummary: string
  /** iOS: Demo UI prefetch_index_lead at settle */
  uiTailGap?: number
  uiHeadGap?: number
  uiRecomposeCount8?: number
}

export interface ReverseScrollResult {
  metrics: LogMetrics
  traceLines: string[]
  headGap: number
  tailGap: number
  lastLead: number
  evidence: string
  uiHeadGap?: number
}

export interface UiPrefetchSpotMetrics {
  maxComposedIndex: number
  maxPlacedIndex: number
  prefetchTargetIndex: number
  prefetchTargetEnterCount: number
  prefetchTargetSource: string
  prefetchTargetPlaced: number
  prefetchTargetPipeline: number
  prefetchPipelineReentryCount: number
  compositionReentryTotal: number
  indexLead: number
}

export interface LazyPrefetchDeps {
  platform: "android" | "ios"
  openDemo(): Promise<void>
  clearLog(): void
  readDemoMetrics(): LogMetrics
  readTraceLines(): string[]
  captureIdleWindow(
    label: string,
    opts?: { settleMs?: number; idleMs?: number },
  ): Promise<{ metrics: LogMetrics; evidence: string }>
  readMetricsAfterScroll(label: string, scrollTimes: number): Promise<ScrollMetricsResult>
  readReverseScrollPhase(
    label: string,
    opts: { prefetchOn: boolean; establishForwardScroll?: boolean },
  ): Promise<ReverseScrollResult>
  tapScenarioModifierOptIn(): Promise<void>
  tapScenarioGlobalOnly(): Promise<void>
  tapScenarioModifierOverrideOff(): Promise<void>
  tapScenarioCacheWindow(): Promise<void>
  tapHeavyItemsToggle(): Promise<void>
  tapResetCounts(): Promise<void>
  tapClearMetricsOnly(): Promise<void>
  ensurePrefetchOn(): Promise<void>
  ensurePrefetchOff(): Promise<void>
  /**
   * 滚动 LazyColumn。
   * - `opts.fling=true`：触发惯性 fling（去掉 leading pause + clamp duration ≤200ms）。9.13 长滑用。
   * - `opts.durationMs`：手势持续时间。fling 默认 ~150ms，普通 ~450ms。
   *
   * 实现 (driver.scroll()) 必须以 `lazy_list` testTag 的 rect 计算起终点（贴上下沿、留 5% 边距，
   * 防 release 出列被 Android 判作 cancel），不能写死屏幕坐标。
   */
  scrollList(
    direction: "down" | "up",
    times?: number,
    opts?: { fling?: boolean; durationMs?: number },
  ): Promise<void>
  setPrefetchUiState(value: boolean): void
  readUiPrefetchSpotMetrics?(): Promise<UiPrefetchSpotMetrics | undefined>
  assertViewTreeMatchesPlaced?(
    viewTree: ViewTreeVisibleItems,
    metrics: LogMetrics,
    label: string,
    opts?: { allowPrefetchNativeHeadroom?: boolean },
  ): void
}

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms))

/** Stop-stable lead: UI indexLead (iOS) or log 最后一行 indexLead；不用 log 尾部 maxComposed−maxPlaced（停稳后易与 indexLead 不一致）。 */
function effectiveSettledIndexLead(result: ScrollMetricsResult): number {
  if (result.uiTailGap !== undefined) return result.uiTailGap
  return readLastPrefetchMetrics(result.metrics)
}

function effectiveTailGap(result: ScrollMetricsResult): number {
  return effectiveSettledIndexLead(result)
}

/** Debug-only tail gap from log; 与 indexLead 差 >1 时说明指标不可混用。 */
function logTailGap(metrics: LogMetrics): number {
  return readSettledIndexGap(metrics)
}

function assertLeadMetricsConsistent(
  label: string,
  indexLead: number,
  tailGap: number,
  hasLog: boolean,
): void {
  if (!hasLog) return
  if (Math.abs(tailGap - indexLead) > 1) {
    throw new EvidenceError(
      `${label}: log tail gap (${tailGap}) != settled indexLead (${indexLead}); 勿用 OR 混判`,
      `settledIndexLead=${indexLead}\nlogTailGap=${tailGap}`,
    )
  }
}

function effectiveHeadGap(result: ReverseScrollResult): number {
  const fromLog = result.headGap
  if (fromLog > 0 || result.metrics.lines.length > 0) return fromLog
  return result.uiHeadGap ?? 0
}

function traceHasSchedulePremeasure(traceSummary: string, traceLines: string[]): boolean {
  if (traceLines.some((l) => l.includes("schedulePremeasure"))) return true
  return (
    traceSummary.includes("schedulePremeasure=") &&
    !/schedulePremeasure=0\b/.test(traceSummary)
  )
}

async function runTest(
  id: string,
  name: string,
  results: TestResult[],
  sessionLog: SessionLogWriter,
  fn: () => Promise<string | void>,
) {
  const start = Date.now()
  try {
    const evidence = await fn()
    const durationMs = Date.now() - start
    results.push({ id, name, status: "passed", evidence: evidence ?? undefined, durationMs })
    await sessionLog.write(`${id} ${name} → 通过 (${durationMs}ms)`)
    console.log(`✅ ${id} ${name}`)
  } catch (err) {
    const detail = err instanceof Error ? err.message : String(err)
    const evidence = err instanceof EvidenceError ? err.evidence : undefined
    const durationMs = Date.now() - start
    results.push({ id, name, status: "failed", detail, evidence, durationMs })
    await sessionLog.write(`${id} ${name} → 不通过: ${detail} (${durationMs}ms)`)
    console.error(`❌ ${id} ${name}: ${detail}`)
  }
}

export async function runLazyPrefetchCases(
  deps: LazyPrefetchDeps,
  sessionLog: SessionLogWriter,
  options: RunLazyPrefetchOptions = {},
): Promise<TestResult[]> {
  const results: TestResult[] = []
  const only = resolveCasesToRun(options.only)
  const runIf = async (
    id: string,
    name: string,
    fn: () => Promise<string | void>,
  ) => {
    if (!shouldRunCase(id, only)) return
    await runTest(id, name, results, sessionLog, fn)
  }

  if (only) {
    await sessionLog.write(formatScopeLabel(only))
  }

  let baselineOff = {
    idleLastLead: 0,
    idleComposedLead: 0,
    idleIndexGap: 0,
    scrollLastLead: 0,
    scrollAheadEvents: 0,
  }
  let baselineSingleItemGap = 1

  await runIf("setup", "start session and open LazyListPrefetchDemo", async () => {
    deps.clearLog()
    await deps.openDemo()
    await sleep(5000)
  })

  await runIf("9.3", "预取关闭：空闲无领先，停稳后无领先", async () => {
    deps.clearLog()
    await deps.tapScenarioModifierOptIn()
    await deps.tapResetCounts()
    deps.setPrefetchUiState(false)
    const { metrics: idle, evidence } = await deps.captureIdleWindow(
      "idle window after reset (prefetch OFF; log cleared before idle)",
    )
    const lastLead = readLastPrefetchMetrics(idle)
    const composedLead = maxLeadOnComposedLines(idle)
    const gap = indexGap(idle)
    baselineOff = {
      idleLastLead: lastLead,
      idleComposedLead: composedLead,
      idleIndexGap: gap,
      scrollLastLead: 0,
      scrollAheadEvents: 0,
    }

    await sleep(2000)
    const offScroll = await deps.readMetricsAfterScroll(
      `scroll x4 prefetch OFF, settle ${POST_SCROLL_SETTLE_MS}ms after scroll stops`,
      4,
    )
    const offScrollGap = effectiveTailGap(offScroll)
    baselineOff.scrollLastLead = offScrollGap
    baselineOff.scrollAheadEvents = offScroll.aheadEvents

    const assertion = [
      `idle: lastIndexLead=${lastLead} (expected 0), logLines=${idle.lines.length}`,
      `scrollOFF: aheadEvents=${offScroll.aheadEvents} (transient composed lead during scroll)`,
      `scrollOFF: settledTailGap=${offScrollGap} after scroll stops`,
      `beyondBoundsItemCount=0 on demo LazyColumn`,
    ].join("\n")

    if (lastLead > 0 || composedLead > 0 || gap > 0) {
      throw new EvidenceError(
        `prefetch OFF idle should have no compose activity: last=${lastLead}`,
        `${evidence}\n\n${offScroll.evidence}\n\n${assertion}`,
      )
    }
    if (offScroll.aheadEvents < 1 && offScroll.metrics.lines.length > 0) {
      throw new EvidenceError(
        `prefetch OFF scroll produced no composed-ahead events`,
        `${evidence}\n\n${offScroll.evidence}\n\n${assertion}`,
      )
    }
    if (offScrollGap > 0) {
      throw new EvidenceError(
        `prefetch OFF should settle with settledTailGap=0 after scroll stops, got gap=${offScrollGap}`,
        `${evidence}\n\n${offScroll.evidence}\n\n${assertion}`,
      )
    }
    if (offScroll.viewTree && deps.assertViewTreeMatchesPlaced) {
      deps.assertViewTreeMatchesPlaced(offScroll.viewTree, offScroll.metrics, "9.3 scrollOFF viewTree vs placed")
    }
    return formatCaseEvidence(
      formatCaseSummary(
        "9.3",
        "预取关闭：空闲无领先，停稳后无领先",
        [
          {
            title: "测试配置",
            lines: [
              "场景：Modifier 开启预取（UI 预取开关关闭）",
              "LazyColumn beyondBoundsItemCount=0",
            ],
          },
          {
            title: "操作步骤",
            lines: [
              "1. 点击「Modifier 开启预取」场景按钮",
              "2. 点击「重置计数」，并清空日志窗口",
              "3. 静置约 2 秒，采集空闲窗口日志",
              `4. 向下滑动列表 4 次，停稳后等待 ${POST_SCROLL_SETTLE_MS}ms，再采集日志`,
            ],
          },
          {
            title: "实测结果",
            lines: [
              `空闲窗口：lastIndexLead=${lastLead}，composedLead=${composedLead}，indexGap=${gap}，log 行数=${idle.lines.length}`,
              `下滑停稳：aheadEvents=${offScroll.aheadEvents}，settledTailGap=${offScrollGap}`,
            ],
          },
        ],
        [
          caseCriterion(
            "空闲态无预取 compose 活动",
            "lastIndexLead=0，且无 compose 领先",
            `lastIndexLead=${lastLead}，composedLead=${composedLead}，indexGap=${gap}`,
            lastLead === 0 && composedLead === 0 && gap === 0,
          ),
          caseCriterion(
            "下滑过程中出现短暂 compose（滚动期正常现象）",
            "aheadEvents ≥ 1（有 log 时）",
            `aheadEvents=${offScroll.aheadEvents}`,
            offScroll.metrics.lines.length === 0 || offScroll.aheadEvents >= 1,
          ),
          caseCriterion(
            "停稳后 composed 不领先 placed",
            "settledTailGap=0",
            `settledTailGap=${offScrollGap}`,
            offScrollGap === 0,
          ),
        ],
      ),
      `${evidence}\n\n${offScroll.evidence}\n\n${assertion}`,
    )
  })

  await runIf("9.4", "Modifier 开启预取：停稳后 composed 领先 placed", async () => {
    await deps.tapScenarioModifierOptIn()
    await deps.ensurePrefetchOff()
    await deps.tapResetCounts()
    await deps.ensurePrefetchOn()
    await sleep(2000)
    const onScroll = await deps.readMetricsAfterScroll(
      `scroll x4 prefetch ON, settle ${POST_SCROLL_SETTLE_MS}ms after scroll stops`,
      4,
    )

    const onScrollGap = effectiveTailGap(onScroll)
    const traceLines = deps.readTraceLines()
    const assertion = [
      `baselineOff scroll (9.3): settledTailGap=${baselineOff.scrollLastLead}`,
      `scrollON: aheadEvents=${onScroll.aheadEvents}, settledTailGap=${onScrollGap}`,
      `prefetch trace: ${onScroll.traceSummary.replace(/\n/g, " | ")}`,
    ].join("\n")
    const evidence = `${onScroll.evidence}\n\n${assertion}`

    if (onScroll.viewTree && deps.assertViewTreeMatchesPlaced) {
      deps.assertViewTreeMatchesPlaced(onScroll.viewTree, onScroll.metrics, "9.4 scrollON viewTree vs placed", {
        allowPrefetchNativeHeadroom: true,
      })
    }
    if (traceLines.some((l) => l.includes("NoOpPrefetchScheduler"))) {
      throw new EvidenceError("prefetch executor is NoOpPrefetchScheduler — requests never run", evidence)
    }
    if (deps.platform === "android" && !traceHasSchedulePremeasure(onScroll.traceSummary, traceLines)) {
      throw new EvidenceError("prefetch ON scroll produced no schedulePremeasure trace events", evidence)
    }
    if (onScrollGap <= baselineOff.scrollLastLead) {
      throw new EvidenceError(
        `prefetch ON settledTailGap (${onScrollGap}) must exceed OFF baseline (${baselineOff.scrollLastLead})`,
        evidence,
      )
    }
    if (onScrollGap < 1) {
      throw new EvidenceError(
        `prefetch ON must leave composed ahead of placed after scroll stops, settledTailGap=${onScrollGap}`,
        evidence,
      )
    }
    baselineSingleItemGap = onScrollGap
    const traceCounts = parseTraceCounts(onScroll.traceSummary)
    const hasSchedulePremeasure = traceHasSchedulePremeasure(onScroll.traceSummary, traceLines)
    const noOpExecutor = traceLines.some((l) => l.includes("NoOpPrefetchScheduler"))
    const summary = formatCaseSummary(
      "9.4",
      "Modifier 开启预取：停稳后 composed 领先 placed",
      [
        {
          title: "测试配置",
          lines: ["场景：Modifier.enableLazyListPrefetch() 开启", "LazyColumn beyondBoundsItemCount=0"],
        },
        {
          title: "操作步骤",
          lines: [
            "1. 点击「Modifier 开启预取」场景",
            "2. 确认 UI 预取开关关闭后点击「重置计数」",
            "3. 打开 UI 预取开关（Modifier 预取生效）",
            "4. 清空日志窗口",
            `5. 向下滑动列表 4 次，停稳后等待 ${POST_SCROLL_SETTLE_MS}ms`,
          ],
        },
        {
          title: "实测结果",
          lines: [
            `停稳尾部领先 settledTailGap=${onScrollGap}`,
            `9.3 对照 baseline settledTailGap=${baselineOff.scrollLastLead}`,
          ],
        },
        {
          title: "跟踪日志",
          lines: [
            `schedulePremeasure=${traceCounts.schedulePremeasure}`,
            `resetCancel=${traceCounts.resetCancel}`,
            `onScrollSkipped=${traceCounts.onScrollSkipped}`,
          ],
        },
      ],
      [
        caseCriterion(
          "预取调度器可用（非 NoOp）",
          "日志中不出现 NoOpPrefetchScheduler",
          noOpExecutor ? "出现 NoOpPrefetchScheduler" : "未出现 NoOpPrefetchScheduler",
          !noOpExecutor,
        ),
        caseCriterion(
          "下滑触发 schedulePremeasure（Android 强校验；iOS 有 trace 时校验）",
          "trace 含 schedulePremeasure 事件",
          hasSchedulePremeasure ? "有 schedulePremeasure" : "无 schedulePremeasure",
          deps.platform === "ios" ? true : hasSchedulePremeasure,
        ),
        caseCriterion(
          "停稳领先大于预取关闭 baseline",
          `settledTailGap > ${baselineOff.scrollLastLead}`,
          `settledTailGap=${onScrollGap}`,
          onScrollGap > baselineOff.scrollLastLead,
        ),
        caseCriterion(
          "停稳后至少领先 1 项",
          "settledTailGap ≥ 1",
          `settledTailGap=${onScrollGap}`,
          onScrollGap >= 1,
        ),
      ],
    )
    return formatCaseEvidence(summary, evidence)
  })

  await runIf("9.5", "反向滑动：方向切换取消预取 + 头部领先", async () => {
    await deps.tapScenarioModifierOptIn()
    await deps.tapResetCounts()
    deps.setPrefetchUiState(false)
    await deps.scrollList("down", 5)
    await sleep(POST_SCROLL_SETTLE_MS)

    const reverseOff = await deps.readReverseScrollPhase("reverse scroll UP prefetch OFF (control)", {
      prefetchOn: false,
    })

    await deps.scrollList("down", 4)
    await sleep(POST_SCROLL_SETTLE_MS)

    const reverseOn = await deps.readReverseScrollPhase("reverse scroll UP prefetch ON (primary)", {
      prefetchOn: true,
      establishForwardScroll: true,
    })

    const reverseOffHead = effectiveHeadGap(reverseOff)
    const reverseOnHead = effectiveHeadGap(reverseOn)
    const reverseEvidence = findReverseComposeEvidence(reverseOn.metrics, 8)
    const reverseLine = reverseEvidence
      ? `composed index=${reverseEvidence.composedIndex} < minVisible=${reverseEvidence.minVisible}，headGap=${reverseEvidence.headGap}`
      : `log 中无 composed<minVisible 配对（layoutVisible 尾部 headGap=${reverseOnHead}）`
    const resetCancelOff = countTraceLines(reverseOff.traceLines, "strategy reset cancel")
    const resetCancelOn = countTraceLines(reverseOn.traceLines, "strategy reset cancel")
    const schedulePremeasureOn = countTraceLines(reverseOn.traceLines, "schedulePremeasure")
    const backwardCompose =
      reverseOnHead >= 1 ||
      reverseEvidence !== null ||
      reverseOn.traceLines.some((l) => l.includes("schedulePremeasure"))

    const summary = formatCaseSummary(
      "9.5",
      "反向滑动：方向切换取消预取 + 头部领先",
      [
        {
          title: "对照组操作（预取关闭）",
          lines: [
            "1. 点击「仅清空指标」",
            "2. 清空日志窗口",
            `3. 向上滑动 4 次，停稳等待 ${POST_SCROLL_SETTLE_MS}ms`,
          ],
        },
        {
          title: "实验组操作（预取开启）",
          lines: [
            "1. 打开 UI 预取开关",
            "2. 点击「仅清空指标」",
            "3. 清空日志窗口",
            "4. 向下滑动 1 次（建立正向滚动上下文）",
            `5. 向上滑动 4 次，停稳等待 ${POST_SCROLL_SETTLE_MS}ms`,
          ],
        },
        {
          title: "实测结果",
          lines: [
            `对照组 headGap=${reverseOffHead}，resetCancel=${resetCancelOff}`,
            `实验组 headGap=${reverseOnHead}，tailGap=${reverseOn.tailGap}（正向噪声，9.5 不计分）`,
            `向上预取证据：${reverseLine}`,
            `实验组 trace：resetCancel=${resetCancelOn}，schedulePremeasure=${schedulePremeasureOn}`,
          ],
        },
      ],
      [
        caseCriterion(
          "对照组停稳后无头部领先",
          "headGap=0",
          `headGap=${reverseOffHead}`,
          reverseOffHead === 0,
        ),
        caseCriterion(
          "对照组无方向切换取消",
          "strategy reset cancel=0",
          `resetCancel=${resetCancelOff}`,
          deps.platform === "ios" ? resetCancelOff === 0 : resetCancelOff === 0,
        ),
        caseCriterion(
          "实验组 headGap 大于对照组",
          `headGap > ${reverseOffHead} 且 ≥ 1`,
          `headGap=${reverseOnHead}`,
          reverseOnHead >= 1 && reverseOnHead > reverseOffHead,
        ),
        caseCriterion(
          "实验组方向切换触发 cancel（有 trace 时）",
          "strategy reset cancel ≥ 1",
          `resetCancel=${resetCancelOn}`,
          deps.platform === "ios" ? true : resetCancelOn >= 1,
        ),
        caseCriterion(
          "实验组出现向上预取证据",
          "composed 低于可见区 min，或 schedulePremeasure>0，或 headGap≥1",
          backwardCompose
            ? reverseEvidence
              ? `composed=${reverseEvidence.composedIndex} < minVisible=${reverseEvidence.minVisible}`
              : `headGap=${reverseOnHead} 或 schedulePremeasure=${schedulePremeasureOn}`
            : "无向上预取证据",
          backwardCompose,
        ),
      ],
    )
    const evidence = formatCaseEvidence(summary, `${reverseOff.evidence}\n\n${reverseOn.evidence}`)

    if (reverseOffHead > 0) {
      throw new EvidenceError(
        `prefetch OFF reverse scroll should settle with headGap=0, got ${reverseOffHead}`,
        evidence,
      )
    }
    if (deps.platform === "android") {
      if (reverseOff.traceLines.some((l) => l.includes("strategy reset cancel"))) {
        throw new EvidenceError(
          "prefetch OFF reverse scroll should not emit strategy reset cancel",
          evidence,
        )
      }
      if (!reverseOn.traceLines.some((l) => l.includes("strategy reset cancel"))) {
        throw new EvidenceError(
          "prefetch ON reverse scroll must cancel prior prefetch on direction change",
          evidence,
        )
      }
    }
    if (!backwardCompose) {
      throw new EvidenceError(
        "prefetch ON reverse scroll must compose below visible window or schedule prefetch",
        evidence,
      )
    }
    if (reverseOnHead < 1 || reverseOnHead <= reverseOffHead) {
      throw new EvidenceError(
        `reverse ON headGap (${reverseOnHead}) must exceed OFF baseline (${reverseOffHead})`,
        evidence,
      )
    }
    return evidence
  })

  if (!only) {
    results.push({
      id: "9.6",
      name: "RecompositionProfiler 预取耗时",
      status: "skipped",
      detail: "RecompositionProfiler demo not wired in repo",
    })
    console.log("⏭️  9.6 skipped (no profiler demo)")
    await sessionLog.write("9.6 RecompositionProfiler 预取耗时 → 跳过：仓库未接入 profiler demo")
  }

  await runIf("9.7", "仅全局开关：停稳后 composed 领先 placed", async () => {
    await deps.tapScenarioGlobalOnly()
    await sleep(1000)
    await deps.tapResetCounts()
    deps.setPrefetchUiState(false)
    await sleep(2000)
    const globalScroll = await deps.readMetricsAfterScroll(
      `global flag only scroll x4, settle ${POST_SCROLL_SETTLE_MS}ms`,
      4,
    )
    const globalGap = effectiveTailGap(globalScroll)
    const traceLines = deps.readTraceLines()
    const traceCounts = parseTraceCounts(globalScroll.traceSummary)
    const hasSchedulePremeasure = traceHasSchedulePremeasure(globalScroll.traceSummary, traceLines)
    const summary = formatCaseSummary(
      "9.7",
      "仅全局开关：停稳后 composed 领先 placed",
      [
        {
          title: "测试配置",
          lines: [
            "场景：ComposeFoundationFlags.isLazyListPrefetchEnabled=true",
            "无 Modifier.enableLazyListPrefetch()",
          ],
        },
        {
          title: "操作步骤",
          lines: [
            "1. 点击「仅全局开关」场景",
            "2. 点击「重置计数」",
            "3. 静置约 2 秒",
            "4. 清空日志窗口",
            `5. 向下滑动列表 4 次，停稳后等待 ${POST_SCROLL_SETTLE_MS}ms`,
          ],
        },
        {
          title: "实测结果",
          lines: [
            `停稳尾部领先 settledTailGap=${globalGap}`,
            `9.3 对照 baseline settledTailGap=${baselineOff.scrollLastLead}`,
          ],
        },
        {
          title: "跟踪日志",
          lines: [
            `schedulePremeasure=${traceCounts.schedulePremeasure}`,
            `resetCancel=${traceCounts.resetCancel}`,
            `onScrollSkipped=${traceCounts.onScrollSkipped}`,
          ],
        },
      ],
      [
        caseCriterion(
          "下滑触发 schedulePremeasure（Android 强校验）",
          "trace 含 schedulePremeasure 且计数 > 0",
          hasSchedulePremeasure ? "有 schedulePremeasure" : "无有效 schedulePremeasure",
          deps.platform === "ios" ? true : hasSchedulePremeasure,
        ),
        caseCriterion(
          "停稳领先大于预取关闭 baseline",
          `settledTailGap > ${baselineOff.scrollLastLead}`,
          `settledTailGap=${globalGap}`,
          globalGap > baselineOff.scrollLastLead,
        ),
        caseCriterion(
          "停稳后至少领先 1 项",
          "settledTailGap ≥ 1",
          `settledTailGap=${globalGap}`,
          globalGap >= 1,
        ),
      ],
    )
    const evidence = formatCaseEvidence(summary, globalScroll.evidence)
    if (deps.platform === "android" && !hasSchedulePremeasure) {
      throw new EvidenceError("global-only produced no schedulePremeasure trace events", evidence)
    }
    if (globalGap <= baselineOff.scrollLastLead) {
      throw new EvidenceError(
        `global-only settledTailGap (${globalGap}) must exceed OFF baseline (${baselineOff.scrollLastLead})`,
        evidence,
      )
    }
    if (globalGap < 1) {
      throw new EvidenceError(
        `global-only must leave composed ahead after scroll stops, settledTailGap=${globalGap}`,
        evidence,
      )
    }
    return evidence
  })

  await runIf("9.8", "全局开 + Modifier 关闭：Modifier 覆盖全局", async () => {
    deps.clearLog()
    await deps.tapScenarioModifierOverrideOff()
    await deps.tapResetCounts()
    await sleep(3000)
    const idle = deps.readDemoMetrics()
    const lead = readLastPrefetchMetrics(idle)
    const rawEvidence = formatMetricsEvidence("idle override-off scenario", idle)
    const summary = formatCaseSummary(
      "9.8",
      "全局开 + Modifier 关闭：Modifier 覆盖全局",
      [
        {
          title: "测试配置",
          lines: ["场景：全局预取开，Modifier.enableLazyListPrefetch(false) 强制关闭"],
        },
        {
          title: "操作步骤",
          lines: [
            "1. 清空日志窗口",
            "2. 点击「Modifier 覆盖关闭」场景",
            "3. 点击「重置计数」",
            "4. 静置约 3 秒，采集空闲日志",
          ],
        },
        {
          title: "实测结果",
          lines: [`空闲 lastIndexLead=${lead}，maxComposedIndex=${idle.maxComposedIndex}`],
        },
      ],
      [
        caseCriterion(
          "Modifier 关闭时 idle 领先应很低",
          "lastIndexLead ≤ 1",
          `lastIndexLead=${lead}`,
          lead <= 1,
        ),
      ],
    )
    const evidence = formatCaseEvidence(summary, rawEvidence)
    if (lead > 1) {
      throw new EvidenceError(
        `idle indexLead should stay low when Modifier disables prefetch: ${lead}`,
        evidence,
      )
    }
    return evidence
  })

  await runIf("9.9", "重 item + 预取开：滑动可正常 compose", async () => {
    await deps.tapScenarioModifierOptIn()
    await deps.tapResetCounts()
    await deps.tapHeavyItemsToggle()
    await deps.ensurePrefetchOn()
    await sleep(1000)
    const heavyScroll = await deps.readMetricsAfterScroll(
      `重 item + 预取开，向下滑动 3 次，停稳 ${POST_SCROLL_SETTLE_MS}ms`,
      3,
    )
    const tailGap = effectiveTailGap(heavyScroll)
    const traceLines = deps.readTraceLines()
    const traceCounts = parseTraceCounts(heavyScroll.traceSummary)
    const hasSchedulePremeasure = traceHasSchedulePremeasure(heavyScroll.traceSummary, traceLines)
    const summary = formatCaseSummary(
      "9.9",
      "重 item + 预取开：滑动可正常 compose",
      [
        {
          title: "测试配置",
          lines: ["场景：Modifier 预取开 + 重 item 开关打开（Pausable 预算场景）"],
        },
        {
          title: "操作步骤",
          lines: [
            "1. 点击「Modifier 开启预取」场景",
            "2. 点击「重置计数」",
            "3. 点击「重 item」开关",
            "4. 打开 UI 预取开关",
            "5. 静置约 1 秒",
            "6. 清空日志窗口",
            `7. 向下滑动 3 次，停稳后等待 ${POST_SCROLL_SETTLE_MS}ms`,
          ],
        },
        {
          title: "实测结果",
          lines: [
            `停稳尾部领先 settledTailGap=${tailGap}`,
            `maxComposedIndex=${heavyScroll.metrics.maxComposedIndex}（全程最大 compose index，仅 debug）`,
          ],
        },
        {
          title: "跟踪日志",
          lines: [
            `schedulePremeasure=${traceCounts.schedulePremeasure}`,
            `resetCancel=${traceCounts.resetCancel}`,
            `skipBudget=${traceCounts.skipBudget}`,
          ],
        },
      ],
      [
        caseCriterion(
          "重 item 下滑停稳后预取仍产生尾部领先",
          "settledTailGap ≥ 1",
          `settledTailGap=${tailGap}`,
          tailGap >= 1,
        ),
        caseCriterion(
          "重 item 下预取调度未被完全跳过（Android 强校验）",
          "trace 含 schedulePremeasure 且计数 > 0",
          hasSchedulePremeasure ? "有 schedulePremeasure" : "无有效 schedulePremeasure",
          deps.platform === "ios" ? true : hasSchedulePremeasure,
        ),
      ],
    )
    const evidence = formatCaseEvidence(summary, heavyScroll.evidence)
    if (tailGap < 1) {
      throw new EvidenceError(`heavy items scroll settledTailGap (${tailGap}) must be >= 1`, evidence)
    }
    if (deps.platform === "android" && !hasSchedulePremeasure) {
      throw new EvidenceError("heavy items scroll produced no schedulePremeasure trace events", evidence)
    }
    return evidence
  })

  await runIf("9.10", "CacheWindow 1000dp：停稳尾部领先 ≥ 4", async () => {
    await deps.tapScenarioCacheWindow()
    await deps.tapResetCounts()
    deps.setPrefetchUiState(false)
    await deps.ensurePrefetchOn()
    await sleep(2000)
    const cacheScroll = await deps.readMetricsAfterScroll(
      `cacheWindow ahead=1000dp behind=1000dp scroll x4, settle ${POST_SCROLL_SETTLE_MS}ms`,
      4,
    )
    const settledLead = effectiveSettledIndexLead(cacheScroll)
    const logGap = logTailGap(cacheScroll.metrics)
    assertLeadMetricsConsistent(
      "9.10",
      settledLead,
      logGap,
      cacheScroll.metrics.lines.length > 0,
    )
    const traceLines = deps.readTraceLines()
    const traceCounts = parseTraceCounts(cacheScroll.traceSummary)
    const hasSchedulePremeasure = traceHasSchedulePremeasure(cacheScroll.traceSummary, traceLines)
    const summary = formatCaseSummary(
      "9.10",
      "CacheWindow 1000dp：停稳尾部领先 ≥ 4",
      [
        {
          title: "测试配置",
          lines: [
            "场景：LazyLayoutCacheWindow(ahead=1000.dp, behind=1000.dp)",
            "Modifier.enableLazyListPrefetch() 开启",
          ],
        },
        {
          title: "操作步骤",
          lines: [
            "1. 点击「CacheWindow 1000dp」场景",
            "2. 点击「重置计数」",
            "3. 打开 UI 预取开关",
            "4. 静置约 2 秒",
            "5. 清空日志窗口",
            `6. 向下滑动列表 4 次，停稳后等待 ${POST_SCROLL_SETTLE_MS}ms`,
          ],
        },
        {
          title: "实测结果",
          lines: [
            `停稳 indexLead=${settledLead}（主判据：UI prefetch_index_lead 或 log 最后一行 indexLead）`,
            `log 尾部 gap=${logGap}（debug，须与 indexLead 一致）`,
            `9.4 单 item baseline indexLead=${baselineSingleItemGap}`,
          ],
        },
        {
          title: "跟踪日志",
          lines: [
            `schedulePremeasure=${traceCounts.schedulePremeasure}`,
            `resetCancel=${traceCounts.resetCancel}`,
            `onScrollSkipped=${traceCounts.onScrollSkipped}`,
          ],
        },
      ],
      [
        caseCriterion(
          "下滑触发 schedulePremeasure（Android 强校验）",
          "trace 含 schedulePremeasure 且计数 > 0",
          hasSchedulePremeasure ? "有 schedulePremeasure" : "无有效 schedulePremeasure",
          deps.platform === "ios" ? true : hasSchedulePremeasure,
        ),
        caseCriterion(
          "停稳领先大于单 item baseline",
          `indexLead > ${baselineSingleItemGap}`,
          `indexLead=${settledLead}`,
          settledLead > baselineSingleItemGap,
        ),
        caseCriterion(
          "停稳 indexLead 达到 CacheWindow 预期",
          `indexLead ≥ ${MIN_CACHE_WINDOW_SETTLED_GAP}`,
          `indexLead=${settledLead}`,
          settledLead >= MIN_CACHE_WINDOW_SETTLED_GAP,
        ),
        caseCriterion(
          "log 尾部 gap 与 indexLead 一致",
          `|logTailGap − indexLead| ≤ 1`,
          `indexLead=${settledLead}，logTailGap=${logGap}`,
          Math.abs(logGap - settledLead) <= 1 || cacheScroll.metrics.lines.length === 0,
        ),
      ],
    )
    const evidence = formatCaseEvidence(summary, cacheScroll.evidence)
    if (deps.platform === "android" && !hasSchedulePremeasure) {
      throw new EvidenceError("CacheWindow scroll produced no schedulePremeasure trace events", evidence)
    }
    if (settledLead <= baselineSingleItemGap) {
      throw new EvidenceError(
        `CacheWindow indexLead (${settledLead}) must exceed 1-item baseline (${baselineSingleItemGap})`,
        evidence,
      )
    }
    if (settledLead < MIN_CACHE_WINDOW_SETTLED_GAP) {
      throw new EvidenceError(
        `CacheWindow expected settled indexLead >= ${MIN_CACHE_WINDOW_SETTLED_GAP}, indexLead=${settledLead} logTailGap=${logGap}`,
        evidence,
      )
    }
    return evidence
  })

  await runIf(
    "9.11",
    "预 compose 后划入 viewport：composition 入树仅 1 次",
    async () => {
      await deps.tapScenarioModifierOptIn()
      await deps.tapResetCounts()
      await deps.ensurePrefetchOn()
      await deps.tapClearMetricsOnly()
      deps.clearLog()

      for (let i = 0; i < 12; i++) {
        await deps.scrollList("down", 1)
        await sleep(450)
        const metrics = deps.readDemoMetrics()
        const traceLines = deps.readTraceLines()
        const ui = deps.readUiPrefetchSpotMetrics
          ? await deps.readUiPrefetchSpotMetrics()
          : undefined
        const candidate = pickPrefetchEvidenceIndex(traceLines, metrics, ui)
        if (candidate !== null && (logHasPlacedIndex(metrics, candidate) || ui?.prefetchTargetPlaced === 1)) {
          break
        }
      }
      await sleep(POST_SCROLL_SETTLE_MS)

      const metrics = deps.readDemoMetrics()
      const traceLines = deps.readTraceLines()
      const ui = deps.readUiPrefetchSpotMetrics
        ? await deps.readUiPrefetchSpotMetrics()
        : undefined
      const iron = evaluatePrefetchIronEvidence(metrics, traceLines, ui)
      const target = iron.index

      const summary = formatCaseSummary(
        "9.11",
        "预 compose 后划入 viewport：composition 入树仅 1 次",
        [
          {
            title: "测试配置",
            lines: [
              "场景：Modifier 预取开；beyondBoundsItemCount=0",
              "从 log/trace 动态识别 prefetch index（非固定 index=8）",
              "铁证：executeRequest composed / prefetchPipeline composed + 屏外 compositionEnter",
            ],
          },
          {
            title: "操作步骤",
            lines: [
              "1. Modifier 开启预取 + 重置 + 仅清空指标",
              "2. 向下滑动直至某个 prefetch index 屏外 compose 并划入 viewport",
              `3. 停稳 ${POST_SCROLL_SETTLE_MS}ms`,
            ],
          },
          {
            title: "实测结果",
            lines: [
              `识别 prefetch target index=${target ?? "none"}`,
              `框架 pipeline compose：${iron.pipelineComposed ? "是" : "否"}`,
              `屏外入树：${iron.offScreenEnter ? "是" : "否"}`,
              `已 placed：${iron.placed ? "是" : "否"}`,
              `composition_enter_count=${iron.enterCount}，reentry=${iron.reentry}`,
              `铁证详情：${iron.detail}`,
            ],
          },
        ],
        [
          caseCriterion(
            "从日志识别 prefetch target index",
            "pickPrefetchEvidenceIndex 返回有效 index",
            target !== null ? `index=${target}` : "未识别",
            target !== null,
          ),
          caseCriterion(
            "框架 prefetch pipeline 已 compose 该 index",
            "trace executeRequest composed 或 prefetch_target_pipeline=1",
            iron.pipelineComposed ? `pipeline composed index=${target}` : "未观察到",
            iron.pipelineComposed,
          ),
          caseCriterion(
            "预 compose 发生在屏外",
            "compositionEnter inViewport=false",
            iron.offScreenEnter ? "已 off-screen 入树" : "未观察到",
            iron.offScreenEnter,
          ),
          caseCriterion(
            "target index 已进入 viewport",
            "placed(visible) 或 prefetch_target_placed=1",
            iron.placed ? "已 placed" : "未 placed",
            iron.placed,
          ),
          caseCriterion(
            "划入后 composition slot 未丢弃重建",
            "composition_enter_count=1 且无 compositionReentry",
            `enterCount=${iron.enterCount}，reentry=${iron.reentry}`,
            iron.enterCount === 1 && !iron.reentry,
          ),
          caseCriterion(
            "log 时序：prefetch compose 早于 placed（有 log 时）",
            "executeRequest composed 在 placed 之前",
            iron.detail,
            iron.orderingOk,
          ),
        ],
      )
      const evidence = formatCaseEvidence(
        summary,
        [
          formatMetricsEvidence("final window", metrics),
          summarizePrefetchTrace(traceLines),
          ui
            ? [
                "Demo UI prefetch target:",
                `  prefetch_target_index=${ui.prefetchTargetIndex}`,
                `  prefetch_target_enter_count=${ui.prefetchTargetEnterCount}`,
                `  prefetch_target_source=${ui.prefetchTargetSource}`,
                `  prefetch_target_placed=${ui.prefetchTargetPlaced}`,
                `  prefetch_target_pipeline=${ui.prefetchTargetPipeline}`,
              ].join("\n")
            : "",
        ]
          .filter(Boolean)
          .join("\n\n"),
      )
      if (target === null) {
        throw new EvidenceError("no prefetch target index found in logs/UI", evidence)
      }
      if (!iron.pipelineComposed) {
        throw new EvidenceError(
          `prefetch pipeline never composed index=${target}`,
          evidence,
        )
      }
      if (!iron.offScreenEnter) {
        throw new EvidenceError(`index=${target} not composed off-screen first`, evidence)
      }
      if (!iron.placed) {
        throw new EvidenceError(`index=${target} never entered viewport`, evidence)
      }
      if (iron.enterCount !== 1 || iron.reentry) {
        throw new EvidenceError(
          `index=${target} composition enterCount=${iron.enterCount} (expected 1, no reentry)`,
          evidence,
        )
      }
      if (!iron.orderingOk) {
        throw new EvidenceError(
          `prefetch compose / compositionEnter / placed ordering invalid for index=${target}`,
          evidence,
        )
      }
      return evidence
    },
  )

  await runIf(
    "9.12",
    "prefetch scheduler 行为对齐官方：不抢主帧 + 续帧 + 滑停后队列清空",
    async () => {
      await deps.tapScenarioModifierOptIn()
      await deps.tapResetCounts()
      await deps.ensurePrefetchOn()
      await deps.tapClearMetricsOnly()
      deps.clearLog()

      for (let i = 0; i < 8; i++) {
        await deps.scrollList("down", 1)
        await sleep(FAST_SCROLL_GAP_MS)
      }
      await sleep(PREFETCH_IDLE_SETTLE_MS)

      const metrics = deps.readDemoMetrics()
      const traceLines = deps.readTraceLines()
      const budget = evaluateSchedulerBudget(traceLines)
      const continuation = evaluateContinuation(traceLines)
      const classify = classifyCompositionEvents(metrics.lines, traceLines)

      const summary = formatCaseSummary(
        "9.12",
        "prefetch scheduler 行为对齐官方：不抢主帧 + 续帧 + 滑停后队列清空",
        [
          {
            title: "测试配置",
            lines: [
              "场景：Modifier 预取开；beyondBoundsItemCount=0",
              "对齐参考：AndroidPrefetchScheduler.android.kt（official 1.9）",
              "核心语义：非 idle 帧也允许 prefetch，但单次 elapsedNs 不得 > 该 task 开始时 startBudgetNs；预算耗尽要续帧；滑停后队列清空",
            ],
          },
          {
            title: "操作步骤",
            lines: [
              "1. Modifier 开启预取 + 重置 + 清空 log/trace",
              `2. 快速连续下滑 8 次（间隔 ${FAST_SCROLL_GAP_MS}ms，模拟惯性滑动）`,
              `3. 停稳 ${PREFETCH_IDLE_SETTLE_MS}ms`,
            ],
          },
          {
            title: "实测结果",
            lines: [
              `composed total=${budget.composedTotal}（其中 pausable=${budget.pausableCount}，full=${budget.fullCount}）`,
              `单次 elapsedNs ≤ startBudgetNs 检查：overBudget=${budget.overBudgetCount}`,
              `maxElapsedNs=${budget.maxElapsedNs}，minStartBudgetNs=${budget.minStartBudgetNs}，minAvailableNs(remaining)=${budget.minAvailableNs}`,
              `续帧：${continuation.detail}`,
              `事件分类：${classify.detail}`,
            ],
          },
        ],
        [
          caseCriterion(
            "至少发生 1 次 prefetch execute（行为存在）",
            "executeRequest composed ≥ 1",
            `composed=${budget.composedTotal}`,
            budget.composedTotal >= 1,
          ),
          caseCriterion(
            "每次 prefetch 不超本帧预算（不抢主帧）",
            "所有 executeRequest composed 满足 elapsedNs ≤ startBudgetNs",
            budget.overBudgetCount === 0
              ? "全部 within budget"
              : `overBudget=${budget.overBudgetCount} 个：${budget.overBudgetEvents
                  .map((e) => `index=${e.index} elapsed=${e.elapsedNs} > startBudget=${e.startBudgetNs}`)
                  .join("; ")}`,
            budget.overBudgetCount === 0,
          ),
          caseCriterion(
            "队列在滑停后清空（无残留任务）",
            "trace 末 frameEnd queuePending=false 或 trace 中无 pending",
            `finalQueuePending=${continuation.finalQueuePending}，pendingFrames=${continuation.pendingFrameCount}`,
            continuation.finalQueuePending !== true,
          ),
          caseCriterion(
            "事件分类：所有 execute 均为 prefetch pipeline、无 prefetchSlot reentry",
            "prefetchExecuteComposed=composedTotal 且 prefetchSlotReentry=0",
            classify.detail,
            classify.prefetchExecuteComposed === budget.composedTotal &&
              classify.prefetchSlotReentry === 0,
          ),
        ],
      )
      const evidence = formatCaseEvidence(
        summary,
        [
          summarizePrefetchTrace(traceLines),
          `scheduler budget evidence: ${budget.detail}`,
          `continuation evidence: ${continuation.detail}`,
          `composition classification: ${classify.detail}`,
          summarizeIdleFrameTrace(traceLines),
          `executeRequest composed events (last 12):`,
          traceLines
            .filter((l) => l.includes("executeRequest composed"))
            .slice(-12)
            .map((l) => `  ${l.trim()}`)
            .join("\n") || "  (no executeRequest composed lines)",
          formatMetricsEvidence("after settle", metrics),
        ].join("\n\n"),
      )
      if (traceLines.length === 0) {
        throw new EvidenceError(
          "no LazyListPrefetchTrace lines — enable trace and iOS console-pty capture",
          evidence,
        )
      }
      if (budget.composedTotal < 1) {
        throw new EvidenceError("no prefetch execute compose observed", evidence)
      }
      if (budget.overBudgetCount > 0) {
        throw new EvidenceError(
          `prefetch overran frame budget on ${budget.overBudgetCount} task(s)`,
          evidence,
        )
      }
      if (continuation.finalQueuePending === true) {
        throw new EvidenceError(
          "prefetch queue not drained after settle — pending tasks remain",
          evidence,
        )
      }
      if (
        classify.prefetchExecuteComposed !== budget.composedTotal ||
        classify.prefetchSlotReentry > 0
      ) {
        throw new EvidenceError(
          `composition events mismatch / prefetchSlot reentry: ${classify.detail}`,
          evidence,
        )
      }
      return evidence
    },
  )

  await runIf(
    "9.13",
    "长列表 + 一次长惯性滑动：统计 prefetch 数量与预算占用",
    async () => {
      await deps.tapScenarioModifierOptIn()
      await deps.tapResetCounts()
      await deps.ensurePrefetchOn()
      await deps.tapClearMetricsOnly()
      deps.clearLog()

      // 9.13：用 scrollList + fling 替代独立 longSwipe。120ms / fling=true 让 VelocityTracker
      // 拿到「短时大位移」高速样本，触发原生惯性。坐标由 driver 端按 lazy_list 的 rect 计算。
      const SWIPE_DURATION_MS = 120
      const INERTIA_WAIT_MS = 2500
      const SETTLE_AFTER_MS = 1000

      await deps.scrollList("down", 1, { fling: true, durationMs: SWIPE_DURATION_MS })
      await sleep(INERTIA_WAIT_MS)
      const traceDuringMotion = deps.readTraceLines()
      const composedDuringMotion = countExecuteComposedInTrace(traceDuringMotion)

      await sleep(SETTLE_AFTER_MS)

      const metrics = deps.readDemoMetrics()
      const traceLines = deps.readTraceLines()
      const budget = evaluateSchedulerBudget(traceLines)
      const continuation = evaluateContinuation(traceLines)
      const classify = classifyCompositionEvents(metrics.lines, traceLines)

      const composedAfterSettle = budget.composedTotal - composedDuringMotion
      const composedIndices = traceLines
        .map((l) => l.match(/executeRequest composed index=(\d+)/))
        .filter((m): m is RegExpMatchArray => Boolean(m))
        .map((m) => Number.parseInt(m[1], 10))
      const indexMin = composedIndices.length > 0 ? Math.min(...composedIndices) : -1
      const indexMax = composedIndices.length > 0 ? Math.max(...composedIndices) : -1
      const indexSpan = indexMax - indexMin

      const onScrollCount = traceLines.filter((l) => l.includes("onScroll delta=")).length
      const frameEndCount = traceLines.filter((l) => l.includes("frameEnd")).length

      const summary = formatCaseSummary(
        "9.13",
        "长列表 + 一次长惯性滑动：统计 prefetch 数量与预算占用",
        [
          {
            title: "测试配置",
            lines: [
              "场景：Modifier 预取开；ITEM_COUNT=100；beyondBoundsItemCount=0",
              `手势：单次长滑（视图顶部 10% ↔ 底部 90%），duration=${SWIPE_DURATION_MS}ms（高速度产生惯性 fling）`,
              "观察：完整滑动 + 惯性 + 停稳期间，统计 executeRequest composed 总数、index 跨度、单次预算占用",
            ],
          },
          {
            title: "操作步骤",
            lines: [
              "1. Modifier 开启预取 + 重置计数 + 清空 log/trace",
              `2. 单次长滑（${SWIPE_DURATION_MS}ms 内从 90% 拉到 10%）`,
              `3. 等待惯性滚动 ${INERTIA_WAIT_MS}ms（期间继续 prefetch）`,
              `4. 停稳额外等待 ${SETTLE_AFTER_MS}ms`,
            ],
          },
          {
            title: "实测结果",
            lines: [
              `prefetch executeRequest composed：总计 ${budget.composedTotal}（运动 + 惯性期 ${composedDuringMotion}，停稳后新增 ${composedAfterSettle}）`,
              `compose index 范围：[${indexMin}, ${indexMax}]，跨度 ${indexSpan} 个 item`,
              `单次预算占用：maxElapsedNs=${budget.maxElapsedNs}（约 ${(budget.maxElapsedNs / 1_000_000).toFixed(2)}ms），minStartBudgetNs=${budget.minStartBudgetNs}（约 ${(budget.minStartBudgetNs / 1_000_000).toFixed(2)}ms），minAvailableNs(remaining)=${budget.minAvailableNs}，overBudget=${budget.overBudgetCount}`,
              `pausable=${budget.pausableCount}，full=${budget.fullCount}`,
              `续帧：${continuation.detail}`,
              `滑动期间 frameEnd 数=${frameEndCount}，onScroll 数=${onScrollCount}`,
              `事件分类：${classify.detail}`,
            ],
          },
        ],
        [
          caseCriterion(
            "长滑期间发生 prefetch（数量 ≥ 8）",
            "executeRequest composed ≥ 8",
            `composed=${budget.composedTotal}`,
            budget.composedTotal >= 8,
          ),
          caseCriterion(
            "每次 prefetch 不超本帧预算",
            "所有 executeRequest composed 满足 elapsedNs ≤ startBudgetNs",
            budget.overBudgetCount === 0
              ? "全部 within budget"
              : `overBudget=${budget.overBudgetCount}：${budget.overBudgetEvents
                  .map((e) => `index=${e.index} elapsed=${e.elapsedNs} > startBudget=${e.startBudgetNs}`)
                  .join("; ")}`,
            budget.overBudgetCount === 0,
          ),
          caseCriterion(
            "停稳后队列清空",
            "trace 末 frameEnd queuePending=false 或 trace 中无 pending",
            `finalQueuePending=${continuation.finalQueuePending}，pendingFrames=${continuation.pendingFrameCount}`,
            continuation.finalQueuePending !== true,
          ),
          caseCriterion(
            "compose 全部来自 prefetch pipeline、无 prefetchSlot reentry",
            "prefetchExecuteComposed=composedTotal 且 prefetchSlotReentry=0",
            classify.detail,
            classify.prefetchExecuteComposed === budget.composedTotal &&
              classify.prefetchSlotReentry === 0,
          ),
        ],
      )
      const evidence = formatCaseEvidence(
        summary,
        [
          summarizePrefetchTrace(traceLines),
          `scheduler budget evidence: ${budget.detail}`,
          `continuation evidence: ${continuation.detail}`,
          `composition classification: ${classify.detail}`,
          summarizeIdleFrameTrace(traceLines),
          `executeRequest composed events (all ${budget.composedTotal}):`,
          traceLines
            .filter((l) => l.includes("executeRequest composed"))
            .map((l) => `  ${l.trim()}`)
            .join("\n") || "  (no executeRequest composed lines)",
          formatMetricsEvidence("after settle", metrics),
        ].join("\n\n"),
      )
      if (traceLines.length === 0) {
        throw new EvidenceError("no LazyListPrefetchTrace lines", evidence)
      }
      if (budget.composedTotal < 8) {
        throw new EvidenceError(
          `prefetch executeRequest composed too few: ${budget.composedTotal} (<8)`,
          evidence,
        )
      }
      if (budget.overBudgetCount > 0) {
        throw new EvidenceError(
          `prefetch overran frame budget on ${budget.overBudgetCount} task(s)`,
          evidence,
        )
      }
      if (continuation.finalQueuePending === true) {
        throw new EvidenceError("prefetch queue not drained after settle", evidence)
      }
      if (
        classify.prefetchExecuteComposed !== budget.composedTotal ||
        classify.prefetchSlotReentry > 0
      ) {
        throw new EvidenceError(
          `composition events mismatch / prefetchSlot reentry: ${classify.detail}`,
          evidence,
        )
      }
      return evidence
    },
  )

  return results
}

export function buildReport(
  platformLabel: string,
  deviceLabel: string,
  pageName: string,
  sessionLogPath: string,
  results: TestResult[],
  runLabel: string,
  scopeLabel = "全套 9.3–9.13",
): string {
  const passed = results.filter((r) => r.status === "passed").length
  const failed = results.filter((r) => r.status === "failed").length
  const skipped = results.filter((r) => r.status === "skipped").length
  return [
    `# LazyList Prefetch E2E (${platformLabel})`,
    "",
    `- RunAt: ${runLabel}`,
    `- Scope: ${scopeLabel}`,
    `- Device: ${deviceLabel}`,
    `- Page: ${pageName}`,
    `- Session log: ${sessionLogPath}`,
    `- Passed: ${passed} Failed: ${failed} Skipped: ${skipped}`,
    "",
    "## Results",
    "",
    ...results.map((r) => {
      const icon = r.status === "passed" ? "✅" : r.status === "skipped" ? "⏭️" : "❌"
      return `- ${icon} **${r.id}** ${r.name}${r.detail ? `: ${r.detail}` : ""}${r.durationMs ? ` (${r.durationMs}ms)` : ""}`
    }),
    "",
    "## 用例证据（摘要含操作步骤与判定结论）",
    "",
    ...results.flatMap((r) => {
      if (!r.evidence) return []
      return [`### ${r.id} ${r.name}`, "", "```", r.evidence, "```", ""]
    }),
  ].join("\n")
}
