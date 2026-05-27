import { describe, expect, it } from "vitest"
import {
  countCompositionReentry,
  countPrefetchPipelineReentry,
  evaluatePrefetchIronEvidence,
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
