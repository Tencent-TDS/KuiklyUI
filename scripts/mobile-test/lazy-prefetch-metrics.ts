/** Shared log parsing for LazyList prefetch E2E (Android logcat + iOS console). */

export const MIN_CACHE_WINDOW_SETTLED_GAP = 4

export interface LogMetrics {
  maxIndexLead: number
  maxComposedIndex: number
  maxPlacedIndex: number
  lines: string[]
}

export interface ReverseComposeEvidence {
  composedIndex: number
  minVisible: number
  headGap: number
}

export function parsePrefetchDemoLines(lines: string[]): LogMetrics {
  let maxIndexLead = 0
  let maxComposedIndex = -1
  let maxPlacedIndex = -1
  for (const line of lines) {
    const lead = line.match(/indexLead=(\d+)/)
    if (lead) maxIndexLead = Math.max(maxIndexLead, Number.parseInt(lead[1], 10))
    const composed = line.match(/composed index=(\d+)/)
    if (composed) maxComposedIndex = Math.max(maxComposedIndex, Number.parseInt(composed[1], 10))
    const placed = line.match(/placed(?:\(visible\))? index=(\d+)/)
    if (placed) maxPlacedIndex = Math.max(maxPlacedIndex, Number.parseInt(placed[1], 10))
  }
  return { maxIndexLead, maxComposedIndex, maxPlacedIndex, lines }
}

export function readLastPrefetchMetrics(log: LogMetrics): number {
  for (let i = log.lines.length - 1; i >= 0; i--) {
    const lead = log.lines[i].match(/indexLead=(\d+)/)
    if (lead) return Number.parseInt(lead[1], 10)
  }
  return 0
}

export function maxLeadOnComposedLines(log: LogMetrics): number {
  let max = 0
  for (const line of log.lines) {
    if (!line.includes("composed index=")) continue
    const lead = line.match(/indexLead=(\d+)/)
    if (lead) max = Math.max(max, Number.parseInt(lead[1], 10))
  }
  return max
}

export function indexGap(metrics: LogMetrics): number {
  if (metrics.maxComposedIndex < 0 || metrics.maxPlacedIndex < 0) return 0
  return metrics.maxComposedIndex - metrics.maxPlacedIndex
}

export function readSettledIndexGap(metrics: LogMetrics, tailLines = 24): number {
  const tail = metrics.lines.slice(-tailLines)
  let lastComposed = -1
  let lastPlaced = -1
  for (let i = tail.length - 1; i >= 0; i--) {
    const line = tail[i]
    if (lastComposed < 0) {
      const composed = line.match(/composed index=(\d+)/)
      if (composed) lastComposed = Number.parseInt(composed[1], 10)
    }
    if (lastPlaced < 0) {
      const placed = line.match(/placed(?:\(visible\))? index=(\d+)/)
      if (placed) lastPlaced = Number.parseInt(placed[1], 10)
    }
    if (lastComposed >= 0 && lastPlaced >= 0) break
  }
  if (lastComposed >= 0 && lastPlaced >= 0) {
    return Math.max(0, lastComposed - lastPlaced)
  }
  return readLastPrefetchMetrics({ ...metrics, lines: tail })
}

export function readSettledHeadGap(metrics: LogMetrics, tailLines = 8): number {
  const tail = metrics.lines.slice(-tailLines)
  for (let i = tail.length - 1; i >= 0; i--) {
    const headGap = tail[i].match(/headGap=(\d+)/)
    if (headGap) return Number.parseInt(headGap[1], 10)
  }
  let minLayoutVisible = Number.POSITIVE_INFINITY
  for (const line of tail) {
    const m = line.match(/layoutVisible indices=\[([^\]]*)\]/)
    if (!m || m[1].length === 0) continue
    const indices = m[1].split(",").map((v) => Number.parseInt(v.trim(), 10))
    minLayoutVisible = Math.min(minLayoutVisible, ...indices)
  }
  if (!Number.isFinite(minLayoutVisible)) return 0
  let headGap = 0
  for (const line of tail) {
    const composed = line.match(/composed index=(\d+)/)
    if (!composed) continue
    const idx = Number.parseInt(composed[1], 10)
    if (idx < minLayoutVisible) {
      headGap = Math.max(headGap, minLayoutVisible - idx)
    }
  }
  return headGap
}

export function findReverseComposeEvidence(
  metrics: LogMetrics,
  tailLines?: number,
): ReverseComposeEvidence | null {
  const lines = tailLines ? metrics.lines.slice(-tailLines) : metrics.lines
  let best: ReverseComposeEvidence | null = null
  for (const line of lines) {
    const composed = line.match(/composed index=(\d+)/)
    if (!composed) continue
    const composedIndex = Number.parseInt(composed[1], 10)
    for (const visLine of lines) {
      const vis = visLine.match(/layoutVisible indices=\[[^\]]*\] min=(\d+)/)
      if (!vis) continue
      const minVisible = Number.parseInt(vis[1], 10)
      if (composedIndex >= minVisible) continue
      const headGap = minVisible - composedIndex
      if (!best || headGap > best.headGap) {
        best = { composedIndex, minVisible, headGap }
      }
    }
  }
  return best
}

export function countTraceLines(lines: string[], needle: string): number {
  return lines.filter((l) => l.includes(needle)).length
}

export function parseTraceCounts(traceSummary: string): {
  schedulePremeasure: number
  resetCancel: number
  onScrollSkipped: number
  skipBudget: number
} {
  const pick = (key: string) => {
    const m = traceSummary.match(new RegExp(`${key}=(\\d+)`))
    return m ? Number.parseInt(m[1], 10) : 0
  }
  return {
    schedulePremeasure: pick("schedulePremeasure"),
    resetCancel: pick("strategyResetCancel"),
    onScrollSkipped: pick("onScrollSkipped"),
    skipBudget: pick("skipBudget"),
  }
}

export function summarizePrefetchTrace(lines: string[]): string {
  const count = (needle: string) => lines.filter((l) => l.includes(needle)).length
  const tail = lines.slice(-12).join("\n")
  return [
    `traceLines=${lines.length}`,
    `schedulePremeasure=${count("schedulePremeasure")}`,
    `executeComposed=${count("executeRequest composed")}`,
    `skipBudget=${count("skip budget")}`,
    `noOpExecutor=${count("NoOpPrefetchScheduler")}`,
    `onScrollSkipped=${count("onScroll skipped")}`,
    `strategyResetCancel=${count("strategy reset cancel")}`,
    `strategyScheduleForwardFalse=${count("forward=false")}`,
    tail
      ? `traceTail:\n${tail.split("\n").map((l) => `  ${l}`).join("\n")}`
      : "traceTail: (empty)",
  ].join("\n")
}

export function formatMetricsEvidence(label: string, metrics: LogMetrics): string {
  const lastLead = readLastPrefetchMetrics(metrics)
  const tailGap = readSettledIndexGap(metrics)
  const headGap = readSettledHeadGap(metrics)
  const tail = metrics.lines.slice(-8).join("\n")
  return [
    `${label}:`,
    `  maxIndexLead=${metrics.maxIndexLead}（全程最大 indexLead，仅 debug）`,
    `  lastIndexLead=${lastLead}（log 最后一行的 indexLead 字段；Demo 打印，可能与停稳领先不同）`,
    `  settledTailGap=${tailGap}（停稳尾部领先：log 尾部 lastComposedIndex − lastPlacedIndex；正向判据主指标）`,
    `  settledHeadGap=${headGap}（停稳头部领先：向上预取 / layoutVisible headGap；反向判据主指标）`,
    `  maxComposedIndex=${metrics.maxComposedIndex}`,
    `  maxPlacedIndex=${metrics.maxPlacedIndex}`,
    `  logLines=${metrics.lines.length}`,
    tail ? `  logTail:\n${tail.split("\n").map((l) => `    ${l}`).join("\n")}` : "  logTail: (empty)",
  ].join("\n")
}

export function countComposedAheadEvents(log: LogMetrics): number {
  return log.lines.filter(
    (l) => l.includes("composed index=") && /indexLead=([1-9]\d*)/.test(l),
  ).length
}

/** How many times index entered composition tree (official prefetch: 1 = slot kept until visible). */
export function readCompositionEnterCount(metrics: LogMetrics, index: number): number {
  let max = 0
  const pattern = new RegExp(`composition(?:Enter|Reentry) index=${index} enterCount=(\\d+)`)
  for (const line of metrics.lines) {
    const m = line.match(pattern)
    if (m) max = Math.max(max, Number.parseInt(m[1], 10))
  }
  return max
}

export function logHasOffScreenCompositionEnter(metrics: LogMetrics, index: number): boolean {
  return metrics.lines.some((l) =>
    new RegExp(`compositionEnter index=${index} enterCount=1 inViewport=false`).test(l),
  )
}

export function logHasCompositionReentry(metrics: LogMetrics, index: number): boolean {
  return metrics.lines.some((l) =>
    new RegExp(`compositionReentry index=${index} enterCount=`).test(l),
  )
}

export function countCompositionReentry(metrics: LogMetrics): number {
  return metrics.lines.filter((l) => /compositionReentry index=\d+/.test(l)).length
}

export function countPrefetchPipelineReentry(metrics: LogMetrics): number {
  return metrics.lines.filter((l) =>
    /compositionReentry index=\d+.*prefetchSlot=true/.test(l),
  ).length
}

export function logHasComposedIndex(metrics: LogMetrics, index: number): boolean {
  return metrics.lines.some(
    (l) =>
      new RegExp(`composed index=${index}\\b`).test(l) ||
      new RegExp(`compositionEnter index=${index} enterCount=1`).test(l),
  )
}

export function logHasPlacedIndex(metrics: LogMetrics, index: number): boolean {
  return metrics.lines.some((l) => new RegExp(`placed\\(visible\\) index=${index}\\b`).test(l))
}

/** Framework LazyListPrefetchTrace: prefetch executor actually composed this index. */
export function traceHasExecuteComposedIndex(lines: string[], index: number): boolean {
  return lines.some((l) =>
    new RegExp(`executeRequest composed index=${index}\\b`).test(l),
  )
}

export function traceHasSchedulePremeasureIndex(lines: string[], index: number): boolean {
  return lines.some((l) =>
    new RegExp(`schedulePremeasure index=${index}\\b`).test(l),
  )
}

/** Demo mirrors framework trace via ComposeFoundationFlags.lazyListPrefetchTraceListener. */
export function logHasPrefetchPipelineComposed(metrics: LogMetrics, index: number): boolean {
  return metrics.lines.some((l) =>
    new RegExp(`prefetchPipeline composed index=${index}\\b source=prefetch`).test(l),
  )
}

export function logCompositionEnterPipelineComposed(metrics: LogMetrics, index: number): boolean {
  return metrics.lines.some((l) =>
    new RegExp(
      `compositionEnter index=${index} enterCount=1 inViewport=false pipelineComposed=true`,
    ).test(l),
  )
}

const PIPELINE_COMPOSED_RE = /(?:executeRequest composed|prefetchPipeline composed) index=(\d+)\b/

export function findPipelineComposedIndices(
  traceLines: string[],
  demoMetrics: LogMetrics,
): number[] {
  const indices = new Set<number>()
  for (const line of [...traceLines, ...demoMetrics.lines]) {
    const match = line.match(PIPELINE_COMPOSED_RE)
    if (match) indices.add(Number.parseInt(match[1], 10))
  }
  return [...indices].sort((a, b) => a - b)
}

export interface PrefetchTargetUi {
  prefetchTargetIndex: number
  prefetchTargetEnterCount: number
  prefetchTargetSource: string
  prefetchTargetPlaced: number
  prefetchTargetPipeline: number
}

/** Pick index prefetched off-screen by framework pipeline; prefer one already placed. */
export function pickPrefetchEvidenceIndex(
  traceLines: string[],
  demoMetrics: LogMetrics,
  ui?: Partial<PrefetchTargetUi>,
): number | null {
  if (ui?.prefetchTargetIndex !== undefined && ui.prefetchTargetIndex >= 0) {
    return ui.prefetchTargetIndex
  }

  const merged = [...traceLines, ...demoMetrics.lines]
  let bestIndex: number | null = null
  let bestScore = Number.POSITIVE_INFINITY

  const pipelineIndices = findPipelineComposedIndices(traceLines, demoMetrics)
  const targetFromLog = merged
    .map((line) => line.match(/prefetchTarget index=(\d+)/))
    .find(Boolean)
  if (targetFromLog) {
    return Number.parseInt(targetFromLog[1], 10)
  }

  for (const index of pipelineIndices) {
    if (!logHasOffScreenCompositionEnter(demoMetrics, index)) continue

    const pipelineAt = merged.findIndex((l) =>
      new RegExp(
        `(?:executeRequest composed|prefetchPipeline composed) index=${index}\\b`,
      ).test(l),
    )
    if (pipelineAt < 0) continue

    const placedAt = merged.findIndex((l) =>
      new RegExp(`placed\\(visible\\) index=${index}\\b`).test(l),
    )
    if (placedAt >= 0 && pipelineAt >= placedAt) continue

    const score = placedAt >= 0 ? pipelineAt : pipelineAt + 10_000
    if (score < bestScore) {
      bestScore = score
      bestIndex = index
    }
  }
  return bestIndex
}

export interface PrefetchIronEvidence {
  index: number | null
  pipelineComposed: boolean
  schedulePremeasure: boolean
  compositionSourcePrefetch: boolean
  offScreenEnter: boolean
  placed: boolean
  enterCount: number
  reentry: boolean
  orderingOk: boolean
  detail: string
}

/** Iron evidence for a dynamically chosen prefetch index. */
export function evaluatePrefetchIronEvidence(
  demoMetrics: LogMetrics,
  traceLines: string[],
  ui?: Partial<PrefetchTargetUi>,
): PrefetchIronEvidence {
  const index = pickPrefetchEvidenceIndex(traceLines, demoMetrics, ui)
  if (index === null) {
    return {
      index: null,
      pipelineComposed: false,
      schedulePremeasure: false,
      compositionSourcePrefetch: false,
      offScreenEnter: false,
      placed: false,
      enterCount: 0,
      reentry: false,
      orderingOk: false,
      detail: "no prefetch target index",
    }
  }

  const pipelineComposed =
    traceHasExecuteComposedIndex(traceLines, index) ||
    logHasPrefetchPipelineComposed(demoMetrics, index) ||
    ui?.prefetchTargetPipeline === 1 ||
    (ui?.prefetchTargetIndex === index &&
      (ui.prefetchTargetSource === "prefetch" ||
        ui.prefetchTargetSource === "prefetch_offscreen"))

  const schedulePremeasure = traceHasSchedulePremeasureIndex(traceLines, index)

  const offScreenEnter =
    logHasOffScreenCompositionEnter(demoMetrics, index) ||
    ui?.prefetchTargetSource === "prefetch" ||
    ui?.prefetchTargetSource === "prefetch_offscreen"

  const placed =
    logHasPlacedIndex(demoMetrics, index) || ui?.prefetchTargetPlaced === 1

  const enterLog = readCompositionEnterCount(demoMetrics, index)
  const enterCount =
    ui?.prefetchTargetIndex === index && ui.prefetchTargetEnterCount !== undefined
      ? ui.prefetchTargetEnterCount
      : enterLog

  const reentry = logHasCompositionReentry(demoMetrics, index)

  const compositionSourcePrefetch =
    ui?.prefetchTargetSource === "prefetch" ||
    ui?.prefetchTargetSource === "prefetch_offscreen" ||
    logCompositionEnterPipelineComposed(demoMetrics, index) ||
    (pipelineComposed && offScreenEnter)

  const merged = [...traceLines, ...demoMetrics.lines]
  let pipelineAt = -1
  let enterAt = -1
  let placedAt = -1
  for (let i = 0; i < merged.length; i++) {
    const line = merged[i]
    if (
      pipelineAt < 0 &&
      new RegExp(
        `(?:executeRequest composed|prefetchPipeline composed) index=${index}\\b`,
      ).test(line)
    ) {
      pipelineAt = i
    }
    if (
      enterAt < 0 &&
      new RegExp(`compositionEnter index=${index} enterCount=1`).test(line)
    ) {
      enterAt = i
    }
    if (placedAt < 0 && new RegExp(`placed\\(visible\\) index=${index}\\b`).test(line)) {
      placedAt = i
    }
  }
  const orderingOk =
    merged.length === 0 ||
    (pipelineAt >= 0 &&
      (enterAt < 0 || pipelineAt <= enterAt) &&
      (placedAt < 0 || pipelineAt < placedAt))

  const detail = [
    `index=${index}`,
    `pipelineComposed=${pipelineComposed}`,
    `compositionSourcePrefetch=${compositionSourcePrefetch}`,
    `offScreenEnter=${offScreenEnter}`,
    `placed=${placed}`,
    `enterCount=${enterCount}`,
    `reentry=${reentry}`,
    `orderingOk=${orderingOk}`,
    merged.length > 0
      ? `order pipeline@${pipelineAt} enter@${enterAt} placed@${placedAt}`
      : "order n/a (no log)",
  ].join(", ")

  return {
    index,
    pipelineComposed,
    schedulePremeasure,
    compositionSourcePrefetch,
    offScreenEnter,
    placed,
    enterCount,
    reentry,
    orderingOk,
    detail,
  }
}

export interface CompositionEventClassification {
  prefetchOffScreenEnter: number
  visibleItemEnter: number
  compositionReentryTotal: number
  prefetchSlotReentry: number
  prefetchExecuteComposed: number
  detail: string
}

/** Split Demo log lines: prefetch vs visible scroll-in vs reentry (9.12 diagnosis). */
export function classifyCompositionEvents(
  demoLines: string[],
  traceLines: string[] = [],
): CompositionEventClassification {
  let prefetchOffScreenEnter = 0
  let visibleItemEnter = 0
  let compositionReentryTotal = 0
  let prefetchSlotReentry = 0
  for (const line of demoLines) {
    if (/compositionReentry index=\d+/.test(line)) {
      compositionReentryTotal++
      if (/prefetchSlot=true/.test(line)) prefetchSlotReentry++
      continue
    }
    if (!/compositionEnter index=\d+ enterCount=1/.test(line)) continue
    if (/inViewport=false.*pipelineComposed=true/.test(line)) {
      prefetchOffScreenEnter++
    } else if (/inViewport=true.*pipelineComposed=false/.test(line)) {
      visibleItemEnter++
    }
  }
  const prefetchExecuteComposed = countExecuteComposedInTrace(traceLines)
  return {
    prefetchOffScreenEnter,
    visibleItemEnter,
    compositionReentryTotal,
    prefetchSlotReentry,
    prefetchExecuteComposed,
    detail: [
      `prefetchExecuteComposed=${prefetchExecuteComposed}`,
      `prefetchOffScreenEnter=${prefetchOffScreenEnter}`,
      `visibleItemEnter=${visibleItemEnter}`,
      `compositionReentry=${compositionReentryTotal}`,
      `prefetchSlotReentry=${prefetchSlotReentry}`,
    ].join(", "),
  }
}

export function countIdleFrameEnd(traceLines: string[]): number {
  return traceLines.filter((l) => /frameEnd isFrameIdle=true\b/.test(l)).length
}

export function summarizeIdleFrameTrace(traceLines: string[], tailLines = 16): string {
  const idleFrames = traceLines.filter((l) => l.includes("frameEnd isFrameIdle="))
  const tail = idleFrames.slice(-tailLines)
  const trueCount = idleFrames.filter((l) => /frameEnd isFrameIdle=true\b/.test(l)).length
  const falseCount = idleFrames.filter((l) => /frameEnd isFrameIdle=false\b/.test(l)).length
  return [
    `frameEndLines=${idleFrames.length} idleTrue=${trueCount} idleFalse=${falseCount}`,
    tail.length > 0
      ? `frameEndTail:\n${tail.map((l) => `  ${l.trim()}`).join("\n")}`
      : "frameEndTail: (empty)",
  ].join("\n")
}

/**
 * Per executeRequest composed event with availableNs / elapsedNs / mode (9.12 official alignment).
 * Official AndroidPrefetchScheduler runs prefetch in both idle (overtime) and non-idle frames as
 * long as availableTimeNanos > 0; what must hold is: elapsedNs <= availableNs at start of task.
 */
export interface ExecuteComposedEvent {
  index: number
  elapsedNs: number
  availableNs: number
  mode: "pausable" | "full"
}

const EXECUTE_COMPOSED_DETAIL_RE =
  /executeRequest composed index=(\d+) elapsedNs=(\d+)(?: mode=(\w+))? availableNs=(\d+|\d+\.\d+E\d+)/

export function parseExecuteComposedEvents(traceLines: string[]): ExecuteComposedEvent[] {
  const out: ExecuteComposedEvent[] = []
  for (const line of traceLines) {
    const m = line.match(EXECUTE_COMPOSED_DETAIL_RE)
    if (!m) continue
    out.push({
      index: Number.parseInt(m[1], 10),
      elapsedNs: Number.parseInt(m[2], 10),
      availableNs: Number.parseFloat(m[4]),
      mode: (m[3] as "pausable" | "full" | undefined) ?? "full",
    })
  }
  return out
}

export interface SchedulerBudgetEvidence {
  composedTotal: number
  overBudgetCount: number
  overBudgetEvents: ExecuteComposedEvent[]
  pausableCount: number
  fullCount: number
  /** Max elapsedNs ever observed. */
  maxElapsedNs: number
  /** Min availableNs at task start (smallest budget actually accepted). */
  minAvailableNs: number
  detail: string
}

export function evaluateSchedulerBudget(traceLines: string[]): SchedulerBudgetEvidence {
  const events = parseExecuteComposedEvents(traceLines)
  const overBudget = events.filter((e) => e.elapsedNs > e.availableNs)
  const pausableCount = events.filter((e) => e.mode === "pausable").length
  const maxElapsedNs = events.reduce((m, e) => Math.max(m, e.elapsedNs), 0)
  const minAvailableNs = events.reduce(
    (m, e) => Math.min(m, e.availableNs),
    Number.POSITIVE_INFINITY,
  )
  return {
    composedTotal: events.length,
    overBudgetCount: overBudget.length,
    overBudgetEvents: overBudget,
    pausableCount,
    fullCount: events.length - pausableCount,
    maxElapsedNs,
    minAvailableNs: Number.isFinite(minAvailableNs) ? minAvailableNs : 0,
    detail: [
      `composed=${events.length}`,
      `overBudget=${overBudget.length}`,
      `pausable=${pausableCount}`,
      `maxElapsedNs=${maxElapsedNs}`,
      `minAvailableNs=${Number.isFinite(minAvailableNs) ? minAvailableNs : "n/a"}`,
    ].join(", "),
  }
}

export interface ContinuationEvidence {
  /** Number of frames where queuePending=true at frameEnd (i.e. needed continuation). */
  pendingFrameCount: number
  /** Number of frames with both queuePending=true and spentNs>0 (real continuation work). */
  continuationWorkFrameCount: number
  /** Final state of queuePending at the last frameEnd line. */
  finalQueuePending: boolean | null
  /** Number of executeRequest composed events. */
  composedTotal: number
  detail: string
}

const FRAME_END_RE =
  /frameEnd isFrameIdle=(true|false) needsProactive=(true|false) scheduledRedraws=(\d+) queuePending=(true|false) spentNs=(\d+)/

export function evaluateContinuation(traceLines: string[]): ContinuationEvidence {
  let pendingFrameCount = 0
  let continuationWorkFrameCount = 0
  let finalQueuePending: boolean | null = null
  for (const line of traceLines) {
    const m = line.match(FRAME_END_RE)
    if (!m) continue
    const queuePending = m[4] === "true"
    const spentNs = Number.parseInt(m[5], 10)
    finalQueuePending = queuePending
    if (queuePending) pendingFrameCount++
    if (queuePending && spentNs > 0) continuationWorkFrameCount++
  }
  const composedTotal = parseExecuteComposedEvents(traceLines).length
  return {
    pendingFrameCount,
    continuationWorkFrameCount,
    finalQueuePending,
    composedTotal,
    detail: [
      `pendingFrames=${pendingFrameCount}`,
      `continuationWorkFrames=${continuationWorkFrameCount}`,
      `finalQueuePending=${finalQueuePending}`,
      `composedTotal=${composedTotal}`,
    ].join(", "),
  }
}

export interface PrefetchComposeIdleEvent {
  index: number
  isFrameIdle: boolean | null
  line: string
}

export interface PrefetchComposeIdleEvidence {
  composedEvents: PrefetchComposeIdleEvent[]
  nonIdleComposedCount: number
  idleComposedCount: number
  unknownIdleComposedCount: number
  hadPrefetchCompose: boolean
  detail: string
}

const EXECUTE_COMPOSED_RE = /executeRequest composed index=(\d+)\b/

/** Parse isFrameIdle from LazyListPrefetchTrace processRequests / frame prefetch lines. */
export function parseTraceFrameIdle(line: string): boolean | null {
  if (!line.includes("isFrameIdle=")) return null
  if (
    !line.includes("processRequests") &&
    !line.includes("frame prefetch") &&
    !line.includes("frameEnd")
  ) {
    return null
  }
  const m = line.match(/isFrameIdle=(true|false)/)
  return m ? m[1] === "true" : null
}

/**
 * Correlate executeRequest composed with the latest processRequests/frame isFrameIdle flag.
 * Used by 9.12: prefetch pipeline compose should run on idle frames only.
 */
export function evaluatePrefetchComposeIdleOnly(traceLines: string[]): PrefetchComposeIdleEvidence {
  let currentIdle: boolean | null = null
  const composedEvents: PrefetchComposeIdleEvent[] = []

  for (const line of traceLines) {
    const idle = parseTraceFrameIdle(line)
    if (idle !== null) currentIdle = idle

    const composed = line.match(EXECUTE_COMPOSED_RE)
    if (!composed) continue
    composedEvents.push({
      index: Number.parseInt(composed[1], 10),
      isFrameIdle: currentIdle,
      line: line.trim(),
    })
  }

  const idleComposedCount = composedEvents.filter((e) => e.isFrameIdle === true).length
  const nonIdleComposedCount = composedEvents.filter((e) => e.isFrameIdle === false).length
  const unknownIdleComposedCount = composedEvents.filter((e) => e.isFrameIdle === null).length

  const detail = [
    `composedTotal=${composedEvents.length}`,
    `idleComposed=${idleComposedCount}`,
    `nonIdleComposed=${nonIdleComposedCount}`,
    `unknownIdleComposed=${unknownIdleComposedCount}`,
    composedEvents.length > 0
      ? `events=${composedEvents.map((e) => `index=${e.index}@idle=${e.isFrameIdle}`).join(", ")}`
      : "events=none",
  ].join(", ")

  return {
    composedEvents,
    nonIdleComposedCount,
    idleComposedCount,
    unknownIdleComposedCount,
    hadPrefetchCompose: composedEvents.length > 0,
    detail,
  }
}

export function countExecuteComposedInTrace(traceLines: string[]): number {
  return traceLines.filter((l) => EXECUTE_COMPOSED_RE.test(l)).length
}

export class EvidenceError extends Error {
  constructor(
    message: string,
    readonly evidence: string,
  ) {
    super(message)
    this.name = "EvidenceError"
  }
}
