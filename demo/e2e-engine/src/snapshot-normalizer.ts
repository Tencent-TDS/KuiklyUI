import type { UiElementSnapshot, UiSnapshot } from "./mobile-driver.js"

export function normalizeSnapshot(snapshot: UiSnapshot): UiSnapshot {
  const filtered = snapshot.elements.filter((el) => {
    return el.testTag || el.text || el.clickable || el.enabled === false
  })

  const compact = filtered.map((el) => {
    const out: UiElementSnapshot = {}
    if (el.testTag) out.testTag = el.testTag
    if (el.text) out.text = el.text
    if (el.type) {
      out.type = el.type
        .replace(/^android\.widget\./, "")
        .replace(/^XCUIElementType/, "")
    }
    if (el.enabled !== undefined) out.enabled = el.enabled
    if (el.clickable) out.clickable = el.clickable
    if (el.visible !== undefined) out.visible = el.visible
    if (el.bounds) out.bounds = el.bounds
    return out
  })

  return { ...snapshot, elements: compact }
}

export function snapshotToJson(snapshot: UiSnapshot): string {
  const normalized = normalizeSnapshot(snapshot)
  const json = JSON.stringify(normalized, null, 2)
  const sizeKB = Buffer.byteLength(json, "utf-8") / 1024
  if (sizeKB > 10) {
    console.warn(`Snapshot size ${sizeKB.toFixed(1)}KB exceeds 10KB threshold`)
  }
  return json
}

export function isInViewport(
  elBounds: [number, number, number, number],
  viewportBounds: [number, number, number, number]
): boolean {
  const [ex1, ey1, ex2, ey2] = elBounds
  const [vx1, vy1, vx2, vy2] = viewportBounds
  return ex1 < vx2 && ex2 > vx1 && ey1 < vy2 && ey2 > vy1
}
