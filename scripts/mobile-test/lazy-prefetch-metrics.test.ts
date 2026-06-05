import { describe, expect, it } from "vitest"
import {
  countCompositionReentry,
  countExecuteComposedInTrace,
  countPrefetchPipelineReentry,
  evaluateContinuation,
  evaluatePrefetchCancelEvidence,
  evaluatePrefetchComposeIdleOnly,
  evaluatePrefetchIronEvidence,
  evaluateSchedulerBudget,
  parsePrefetchDemoLines,
  pickPrefetchEvidenceIndex,
} from "./lazy-prefetch-metrics.js"

describe("pickPrefetchEvidenceIndex", () => {
  it("picks pipeline-composed off-screen index that was placed", () => {
    const trace = ["LazyListPrefetchTrace executeRequest composed index=6 elapsedNs=100"]
    const demo = parsePrefetchDemoLines([
      "LazyListPrefetchDemo prefetchPipeline composed index=6 source=prefetch",
      "LazyListPrefetchDemo compositionEnter index=6 enterCount=1 inViewport=false pipelineComposed=true",
      "LazyListPrefetchDemo placed(visible) index=6 total=4 indexLead=2",
    ])
    expect(pickPrefetchEvidenceIndex(trace, demo)).toBe(6)
    const iron = evaluatePrefetchIronEvidence(demo, trace)
    expect(iron.index).toBe(6)
    expect(iron.enterCount).toBe(1)
    expect(iron.pipelineComposed).toBe(true)
    expect(iron.placed).toBe(true)
    expect(iron.reentry).toBe(false)
  })

  it("counts prefetch pipeline reentry from log lines", () => {
    const demo = parsePrefetchDemoLines([
      "LazyListPrefetchDemo compositionEnter index=3 enterCount=1 inViewport=false",
      "LazyListPrefetchDemo compositionReentry index=3 enterCount=2 inViewport=true prefetchSlot=true",
      "LazyListPrefetchDemo compositionReentry index=5 enterCount=2 inViewport=true prefetchSlot=false",
    ])
    expect(countPrefetchPipelineReentry(demo)).toBe(1)
    expect(countCompositionReentry(demo)).toBe(2)
  })

  it("9.12: executeRequest composed must correlate with isFrameIdle", () => {
    const trace = [
      "LazyListPrefetchTrace processRequests start isFrameIdle=false queueSize=1",
      "LazyListPrefetchTrace processRequests skip budget: isFrameIdle=false availableNs=1000 queueSize=1",
      "LazyListPrefetchTrace processRequests start isFrameIdle=true queueSize=1",
      "LazyListPrefetchTrace executeRequest composed index=4 elapsedNs=100 mode=pausable",
    ]
    const idle = evaluatePrefetchComposeIdleOnly(trace)
    expect(idle.hadPrefetchCompose).toBe(true)
    expect(idle.nonIdleComposedCount).toBe(0)
    expect(idle.idleComposedCount).toBe(1)
    expect(idle.composedEvents[0]?.index).toBe(4)

    const bad = evaluatePrefetchComposeIdleOnly([
      "LazyListPrefetchTrace processRequests start isFrameIdle=false queueSize=1",
      "LazyListPrefetchTrace executeRequest composed index=2 elapsedNs=50",
    ])
    expect(bad.nonIdleComposedCount).toBe(1)
  })

  it("evaluateSchedulerBudget: legacy lines without startBudgetNs fallback to elapsed+available, never over budget", () => {
    // 旧 trace 行（无 startBudgetNs），fallback = elapsedNs + availableNs。
    // 这等价于"假设任务恰好把预算花完"，定义上 elapsed === startBudget，永远不会 > 它，
    // 即 9.12 之前误报的 over-budget 案例在新口径下不再被判超支。
    const trace = [
      "LazyListPrefetchTrace executeRequest composed index=6 elapsedNs=500000 mode=pausable availableNs=1000000",
      "LazyListPrefetchTrace executeRequest composed index=7 elapsedNs=2000000 availableNs=1500000",
    ]
    const b = evaluateSchedulerBudget(trace)
    expect(b.composedTotal).toBe(2)
    expect(b.overBudgetCount).toBe(0)
    expect(b.pausableCount).toBe(1)
    expect(b.fullCount).toBe(1)
  })

  it("evaluateSchedulerBudget: with startBudgetNs detects real over-budget (elapsed > startBudget)", () => {
    // 新 trace 行带 startBudgetNs：真超支当且仅当 elapsedNs > startBudgetNs。
    // index=9 startBudget=1.5ms，pausable composition 实际花了 2ms ⇒ 真超支。
    // index=8 startBudget=1ms，elapsed=600µs ⇒ 在预算内。
    const trace = [
      "LazyListPrefetchTrace executeRequest composed index=8 elapsedNs=600000 mode=pausable startBudgetNs=1000000 availableNs=400000",
      "LazyListPrefetchTrace executeRequest composed index=9 elapsedNs=2000000 mode=pausable startBudgetNs=1500000 availableNs=0",
    ]
    const b = evaluateSchedulerBudget(trace)
    expect(b.composedTotal).toBe(2)
    expect(b.overBudgetCount).toBe(1)
    expect(b.overBudgetEvents[0]?.index).toBe(9)
    expect(b.overBudgetEvents[0]?.startBudgetNs).toBe(1500000)
    expect(b.overBudgetEvents[0]?.elapsedNs).toBe(2000000)
    expect(b.minStartBudgetNs).toBe(1000000)
    expect(b.pausableCount).toBe(2)
    expect(b.fullCount).toBe(0)
  })

  it("evaluateContinuation: tracks queuePending across frames", () => {
    const trace = [
      "LazyListPrefetchTrace frameEnd isFrameIdle=false needsProactive=true scheduledRedraws=2 queuePending=true spentNs=800000",
      "LazyListPrefetchTrace frameEnd isFrameIdle=false needsProactive=true scheduledRedraws=2 queuePending=true spentNs=600000",
      "LazyListPrefetchTrace frameEnd isFrameIdle=true needsProactive=false scheduledRedraws=0 queuePending=false spentNs=0",
    ]
    const c = evaluateContinuation(trace)
    expect(c.pendingFrameCount).toBe(2)
    expect(c.continuationWorkFrameCount).toBe(2)
    expect(c.finalQueuePending).toBe(false)
  })

  it("evaluatePrefetchCancelEvidence: counts by stage", () => {
    const trace = [
      "LazyListPrefetchTrace strategy reset cancel index=3",
      "LazyListPrefetchTrace request cancel index=3",
      "LazyListPrefetchTrace executeRequest invalid index=5 canceled=true",
      "LazyListPrefetchTrace scheduler cancelAll queueSize=2",
      "LazyListPrefetchTrace cacheWindow cancel index=7",
      "LazyListPrefetchTrace runRequest paused hasMoreWork queueSize=1",
    ]
    const c = evaluatePrefetchCancelEvidence(trace)
    expect(c.hadAnyCancel).toBe(true)
    expect(c.strategyResetCancel).toBe(1)
    expect(c.requestCancel).toBe(1)
    expect(c.requestInvalidCanceled).toBe(1)
    expect(c.schedulerCancelAll).toBe(1)
    expect(c.cacheWindowCancel).toBe(1)
    expect(c.runRequestPaused).toBe(1)
    expect(c.totalCancel).toBe(5)
  })

  it("prefers UI prefetch_target_index when logs empty", () => {
    const iron = evaluatePrefetchIronEvidence(
      parsePrefetchDemoLines([]),
      [],
      {
        prefetchTargetIndex: 5,
        prefetchTargetEnterCount: 1,
        prefetchTargetSource: "prefetch",
        prefetchTargetPlaced: 1,
        prefetchTargetPipeline: 1,
      },
    )
    expect(iron.index).toBe(5)
    expect(iron.enterCount).toBe(1)
    expect(iron.pipelineComposed).toBe(true)
    expect(iron.placed).toBe(true)
  })
})
