import { describe, expect, it } from "vitest"
import {
  expandCaseSelection,
  formatLazyPrefetchRunLabel,
  formatLazyPrefetchRunStamp,
  shouldRunCase,
} from "./lazy-prefetch-run-utils.js"

describe("lazy-prefetch-run-utils", () => {
  it("formats readable run stamp and label", () => {
    const stamp = formatLazyPrefetchRunStamp(new Date(2026, 4, 27, 16, 50, 33))
    expect(stamp).toBe("0527_165033")
    expect(formatLazyPrefetchRunLabel(stamp)).toBe("5月27日 16:50:33")
  })

  it("expands case dependencies for partial runs", () => {
    expect([...expandCaseSelection(["9.11"])]).toEqual(["9.11"])
    expect([...expandCaseSelection(["9.10"])]).toEqual(["9.10", "9.3", "9.4"])
    expect(shouldRunCase("setup", expandCaseSelection(["9.11"]))).toBe(true)
    expect(shouldRunCase("9.3", expandCaseSelection(["9.11"]))).toBe(false)
  })
})
