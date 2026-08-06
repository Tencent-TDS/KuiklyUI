import type { Platform, QuiescenceAnchor, Selector } from "./mobile-driver.js"

export function isScrollableAnchor(anchor: QuiescenceAnchor): anchor is { scrollable: true } {
  return "scrollable" in anchor && anchor.scrollable === true
}

export function describeQuiescenceAnchor(anchor: QuiescenceAnchor): string {
  if (isScrollableAnchor(anchor)) return "{ scrollable: true }"
  return JSON.stringify(anchor)
}

/** 将 QuiescenceAnchor 转成用于 find 的 locator 链（与 findElementWithFallback 一致）。 */
export function locatorsForQuiescenceAnchor(
  anchor: QuiescenceAnchor,
  platform: Platform,
): Array<{ using: string; value: string }> {
  if (isScrollableAnchor(anchor)) {
    if (platform === "android") {
      return [{ using: "-android uiautomator", value: "new UiSelector().scrollable(true)" }]
    }
    return [
      {
        using: "-ios predicate string",
        value:
          "type == 'XCUIElementTypeScrollView' OR type == 'XCUIElementTypeTable' OR type == 'XCUIElementTypeCollectionView'",
      },
    ]
  }
  return locatorsForSelector(anchor, platform)
}

type LocatorEntry = { using: string; value: string }

function locatorEntry(locator: string, platform: Platform): LocatorEntry {
  if (locator.startsWith("android=new ")) {
    return { using: "-android uiautomator", value: locator.slice("android=".length) }
  }
  if (locator.startsWith("//")) return { using: "xpath", value: locator }
  if (locator.startsWith("-ios predicate string:")) {
    return { using: "-ios predicate string", value: locator.slice("-ios predicate string:".length).trim() }
  }
  if (platform === "ios" && locator.startsWith("id==")) {
    return { using: "id", value: locator.slice(4) }
  }
  if (locator.startsWith("id=")) {
    return { using: "id", value: locator.slice(3) }
  }
  return { using: "xpath", value: locator }
}

function kuiklyTagFromSelector(selector: Selector): string | null {
  if ("accessibilityId" in selector) return selector.accessibilityId
  if ("testTag" in selector) return selector.testTag
  return null
}

function selectorToLocator(selector: Selector, platform: Platform): string {
  if ("id" in selector) {
    return platform === "ios" ? `id==${selector.id}` : `id=${selector.id}`
  }
  const tag = kuiklyTagFromSelector(selector)
  if (tag !== null) {
    if (platform === "ios") {
      const escaped = tag.replace(/'/g, "\\'")
      return `-ios predicate string:identifier CONTAINS '${escaped}' OR name CONTAINS '${escaped}'`
    }
    const escaped = tag.replace(/\\/g, "\\\\").replace(/"/g, '\\"')
    return `android=new UiSelector().resourceIdMatches(".*${escaped}$")`
  }
  if ("text" in selector) {
    if (platform === "android") {
      return `//*[@content-desc="${selector.text}"]`
    }
    return `-ios predicate string:label == '${selector.text}'`
  }
  if ("xpath" in selector) {
    return selector.xpath
  }
  throw new Error(`Unknown selector: ${JSON.stringify(selector)}`)
}

function locatorsForSelector(selector: Selector, platform: Platform): LocatorEntry[] {
  const out: LocatorEntry[] = [locatorEntry(selectorToLocator(selector, platform), platform)]
  if (platform === "android" && "text" in selector) {
    out.push({ using: "xpath", value: `//*[@text="${selector.text}"]` })
  }
  const tag = kuiklyTagFromSelector(selector)
  if (platform === "android" && tag) {
    out.push({ using: "xpath", value: `//*[@content-desc="${tag}"]` })
    out.push({ using: "xpath", value: `//*[@resource-id="${tag}"]` })
  }
  if (platform === "ios" && tag) {
    out.push({ using: "xpath", value: `//*[@name="${tag}"]` })
  }
  return out
}
