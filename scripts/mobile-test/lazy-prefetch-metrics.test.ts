import { describe, expect, it } from "vitest"
import {
  countCompositionReentry,
  countExecuteComposedInTrace,
  countPrefetchPipelineReentry,
  evaluateContinuation,
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

  it("evaluateSchedulerBudget: detects over-budget execute", () => {
    const trace = [
      "LazyListPrefetchTrace executeRequest composed index=6 elapsedNs=500000 mode=pausable availableNs=1000000",
      "LazyListPrefetchTrace executeRequest composed index=7 elapsedNs=2000000 availableNs=1500000",
    ]
    const b = evaluateSchedulerBudget(trace)
    expect(b.composedTotal).toBe(2)
    expect(b.overBudgetCount).toBe(1)
    expect(b.overBudgetEvents[0]?.index).toBe(7)
    expect(b.pausableCount).toBe(1)
    expect(b.fullCount).toBe(1)
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
