import { execSync } from "node:child_process"
import { ELEMENT_KEY } from "webdriver"
import type { Browser } from "webdriverio"
import { remote } from "webdriverio"
import type {
  ElementRect,
  MobileDriver,
  Platform,
  QuiescenceAnchor,
  ScrollOptions,
  ScrollWithinOptions,
  Selector,
  TapOptions,
  TapPostAction,
  UiElementSnapshot,
  UiQuiescenceOptions,
  UiSnapshot,
  UiViewTree,
} from "./mobile-driver.js"
import { NAV_BACK_TEST_TAG } from "./mobile-driver.js"
import {
  describeQuiescenceAnchor,
  isScrollableAnchor,
  locatorsForQuiescenceAnchor,
} from "./quiescence-anchor.js"
import {
  boundsStableTick,
  defaultAndroidIdleTimeoutMs,
  defaultQuiescenceIdleMs,
  defaultScrollPostGestureSettleMs,
  defaultScrollQuiescenceIdleMs,
  DEFAULT_ANCHOR_PROBE_TIMEOUT_MS,
  DEFAULT_QUIESCENCE_TIMEOUT_MS,
  resolveBoundsStableOptions,
} from "./ui-stable.js"
import { buildViewTree, filterVisibleTree, renderTreeText } from "./view-tree.js"
import {
  classifyUiautomator2Error,
  formatUiautomator2RecoverLog,
  isUiAutomator2RecoverableError,
  shouldHardRecoverUiautomator2,
  webdriverRequestTimeoutMs,
} from "./uiautomator2-error.js"

/**
 * WebDriverIO / UiAutomator2 在「元素不存在」时若 implicit 很大，单次 find 会卡 ~120s。
 * 业务侧用 waitFor 轮询时会把 3s 探测变成数分钟。
 */
const DEFAULT_IMPLICIT_WAIT_MS = 400
/** 真机 dump / find 偶发 >8s；可通过 ANDROID_UIA2_READ_TIMEOUT_MS 覆盖。 */
const DEFAULT_UIA2_SERVER_READ_TIMEOUT_MS = 20_000

export interface AppiumMobileDriverConfig {
  platform: Platform
  appiumUrl: string
  appPackage?: string
  appActivity?: string
  /** Android：写入 optionalIntentArguments，例如 LazyListPrefetchDemo 的 pageName。 */
  androidPageName?: string
  bundleId?: string
  deviceName?: string
  udid?: string
  platformVersion?: string
  noReset?: boolean
  fullReset?: boolean
  /** Demo 控制区在竖屏更易完整进 accessibility 树（横屏时底部按钮常被裁切）。 */
  lockPortrait?: boolean
}

/**
 * 把 `ScrollOptions` 的 start/end 反推成 `mobile: swipeGesture` 期望的 direction。
 * 主轴（dx vs dy 中较大的那个）的符号决定方向：
 * - dy < 0（end 在 start 上方）→ 内容上移 → 用户「up swipe」
 * - dy > 0 → 「down swipe」
 * - dx < 0 → 「left」  / dx > 0 → 「right」
 */
function swipeDirectionFromOpts(opts: ScrollOptions): "up" | "down" | "left" | "right" {
  const dx = opts.endX - opts.startX
  const dy = opts.endY - opts.startY
  if (Math.abs(dy) >= Math.abs(dx)) {
    return dy < 0 ? "up" : "down"
  }
  return dx < 0 ? "left" : "right"
}

/**
 * 没传 `area` 时，按 start/end 反推一个包围矩形（再向外扩 5% 余量），用于 `mobile: swipeGesture`。
 * 这是保守兜底 —— 调用方应优先传真实的 LazyColumn rect。
 */
function swipeAreaFromOpts(opts: ScrollOptions): ElementRect {
  const minX = Math.min(opts.startX, opts.endX)
  const minY = Math.min(opts.startY, opts.endY)
  const maxX = Math.max(opts.startX, opts.endX)
  const maxY = Math.max(opts.startY, opts.endY)
  const w = Math.max(1, maxX - minX)
  const h = Math.max(1, maxY - minY)
  const padX = Math.round(w * 0.05)
  const padY = Math.round(h * 0.05)
  return {
    x: Math.max(0, Math.round(minX - padX)),
    y: Math.max(0, Math.round(minY - padY)),
    width: Math.round(w + padX * 2),
    height: Math.round(h + padY * 2),
  }
}

/** Kuikly testTag value from selector (`accessibilityId` and `testTag` keys are equivalent). */
function kuiklyTagFromSelector(selector: Selector): string | null {
  if ("accessibilityId" in selector) return selector.accessibilityId
  if ("testTag" in selector) return selector.testTag
  return null
}

function describeSelectorBrief(selector: Selector): string {
  const tag = kuiklyTagFromSelector(selector)
  if (tag !== null) return `testTag=${tag}`
  if ("text" in selector) return `text=${selector.text}`
  if ("id" in selector) return `id=${selector.id}`
  if ("xpath" in selector) return "xpath=(…)"
  return "selector"
}

/** 盲等打点；设 KUIKLY_MOBILE_TEST_TIMING=0 可关闭。 */
function logUiGestureSettle(
  phase: "tap.after" | "scroll.after" | "tap.postTap.settle",
  ms: number,
  detail?: string,
): void {
  if (process.env.KUIKLY_MOBILE_TEST_TIMING === "0") return
  const suffix = detail ? ` ${detail}` : ""
  // eslint-disable-next-line no-console
  console.warn(`[kuikly-mobile-test] [ui-settle] ${phase} blind_wait=${ms}ms${suffix}`)
}

function selectorToLocator(selector: Selector, platform: Platform): string {
  if ("id" in selector) {
    return platform === "ios"
      ? `id==${selector.id}`
      : `id=${selector.id}`
  }
  const tag = kuiklyTagFromSelector(selector)
  if (tag !== null) {
    if (platform === "ios") {
      const escaped = tag.replace(/'/g, "\\'")
      return `-ios predicate string:identifier CONTAINS '${escaped}' OR name CONTAINS '${escaped}'`
    }
    // Kuikly Android（Compose + Self DSL）把 testTag 直接写到原生 `resource-id`：
    //   - Compose：Modifier.semantics { testTag = ... } → setViewIdResourceName
    //   - Self DSL：view-tree.ts 也只从 `resource-id` 提取 testTag（见 view-tree.ts L86-L89）
    // 而 `content-desc` 在我们这里被 Compose semantics 用来塞 Text 内容（如 "Prefetch: OFF"），
    // 用 `description()` 当主 selector 永远命不中，会导致每次 selector 调用：
    //   1. Appium webdriver log 里多打一条 `no such element` 噪声
    //   2. 多一次 Appium roundtrip ≈ 50ms 开销
    // 真机上 xpath 全树扫描易触发 uiautomator2 read timeout；优先 UiSelector.resourceIdMatches。
    const escaped = tag.replace(/\\/g, "\\\\").replace(/"/g, '\\"')
    return `android=new UiSelector().resourceIdMatches(".*${escaped}$")`
  }
  if ("text" in selector) {
    if (platform === "android") {
      // Android: view-tree text comes from content-desc, not xml text attribute.
      // Use XPath to match regardless of clickable state.
      return `//*[@content-desc="${selector.text}"]`
    }
    return `-ios predicate string:label == '${selector.text}'`
  }
  if ("xpath" in selector) {
    return selector.xpath
  }
  throw new Error(`Unknown selector: ${JSON.stringify(selector)}`)
}

type LocatorEntry = { using: string; value: string }

/** W3C findElements 返回的节点引用（避免再调 findElement 卡满 uia2ServerReadTimeout）。 */
type ElementRef = Record<string, string>

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

/** elementExists 用：主 selector + 与 findElementWithFallback 相同的兜底链，避免 isExisting() 二次长等待。 */
function locatorsForSelector(selector: Selector, platform: Platform): LocatorEntry[] {
  const out: LocatorEntry[] = [locatorEntry(selectorToLocator(selector, platform), platform)]
  if (platform === "android" && "text" in selector) {
    out.push({ using: "xpath", value: `//*[@text="${selector.text}"]` })
  }
  // Android testTag：仅用 UiSelector.resourceIdMatches（见 selectorToLocator 注释）。
  // 勿在 find 失败后再扫 xpath 全树，重组高峰易 20s 超时并触发 hard_recover 把场景打回默认态。
  const tag = kuiklyTagFromSelector(selector)
  if (platform === "ios" && tag) {
    out.push({ using: "xpath", value: `//*[@name="${tag}"]` })
  }
  return out
}

function elementIdFromRef(ref: ElementRef): string {
  const id = ref[ELEMENT_KEY] ?? ref.ELEMENT
  if (!id) {
    throw new Error("findElements returned a node without element id")
  }
  return id
}

function parseAndroidBoundsAttribute(bounds: string | null): ElementRect | null {
  if (!bounds) return null
  const m = /\[(\d+),(\d+)\]\[(\d+),(\d+)\]/.exec(bounds)
  if (!m) return null
  const x1 = Number(m[1])
  const y1 = Number(m[2])
  const x2 = Number(m[3])
  const y2 = Number(m[4])
  return { x: x1, y: y1, width: x2 - x1, height: y2 - y1 }
}

async function findElementRefWithFallback(
  driver: Browser,
  selector: Selector,
  platform: Platform,
): Promise<ElementRef> {
  const locators = locatorsForSelector(selector, platform)
  for (const entry of locators) {
    const els = await driver.findElements(entry.using, entry.value)
    if (els.length > 0) {
      return els[0] as ElementRef
    }
  }
  const primary = selectorToLocator(selector, platform)
  throw new Error(`Element not found for selector ${JSON.stringify(selector)} (tried ${primary})`)
}

async function clickElementRef(driver: Browser, ref: ElementRef): Promise<void> {
  await driver.elementClick(elementIdFromRef(ref))
}

async function elementRectFromRef(
  driver: Browser,
  ref: ElementRef,
  platform: Platform,
): Promise<ElementRect> {
  if (platform === "android") {
    const bounds = await driver.getElementAttribute(elementIdFromRef(ref), "bounds")
    const parsed = parseAndroidBoundsAttribute(bounds)
    if (parsed) return parsed
  }
  const rect = await driver.getElementRect(elementIdFromRef(ref))
  return { x: rect.x, y: rect.y, width: rect.width, height: rect.height }
}

function isUiAutomator2DeadError(err: unknown): boolean {
  return classifyUiautomator2Error(err) === "instrumentation_dead"
}

function androidUia2ReadTimeoutMs(): number {
  const raw = process.env.ANDROID_UIA2_READ_TIMEOUT_MS
  if (!raw) return DEFAULT_UIA2_SERVER_READ_TIMEOUT_MS
  const n = Number.parseInt(raw, 10)
  return Number.isFinite(n) && n >= 8000 ? n : DEFAULT_UIA2_SERVER_READ_TIMEOUT_MS
}

function buildCapabilities(config: AppiumMobileDriverConfig): Record<string, unknown> {
  const base: Record<string, unknown> = {
    platformName: config.platform === "android" ? "Android" : "iOS",
    "appium:automationName": config.platform === "android" ? "UiAutomator2" : "XCUITest",
    "appium:noReset": config.noReset ?? true,
    "appium:fullReset": config.fullReset ?? false,
    "appium:newCommandTimeout": 300,
  }

  if (config.platform === "android") {
    base["appium:appPackage"] = config.appPackage ?? "com.tencent.kuikly.android.demo"
    base["appium:appActivity"] =
      config.appActivity ?? "com.tencent.kuikly.android.demo.KuiklyRenderActivity"
    base["appium:deviceName"] = config.deviceName ?? "emulator-5554"
    // 调用方已 adb am start 时，禁止 session 创建再次冷启动把页面打回路由。
    base["appium:autoLaunch"] = false
    if (config.udid) base["appium:udid"] = config.udid
    if (config.androidPageName) {
      base["appium:optionalIntentArguments"] = `-es pageName ${config.androidPageName}`
    }
    base["appium:settings[waitForSelectorTimeout]"] = 2000
    base["appium:settings[waitForIdleTimeout]"] = 100
    base["appium:uiautomator2ServerReadTimeout"] = androidUia2ReadTimeoutMs()
    base["appium:uiautomator2ServerLaunchTimeout"] = 60_000
  } else {
    base["appium:bundleId"] = config.bundleId ?? "com.tencent.kuiklycore.demo.luoyibu"
    base["appium:deviceName"] = config.deviceName ?? "iPhone 17 Pro"
    base["appium:platformVersion"] = config.platformVersion ?? "26.3"
    if (config.udid) base["appium:udid"] = config.udid
  }

  return base
}

const SCROLL_WITHIN_RECT_CACHE_TTL_MS = 60_000

export class AppiumMobileDriver implements MobileDriver {
  readonly config: AppiumMobileDriverConfig
  private driver: Browser | null = null
  /** 同一 target 连续 scrollWithin 时复用 rect，避免真机连滑后反复 getElementRect 触发 uia2 超时。 */
  private scrollWithinRectCache = new Map<string, { rect: ElementRect; at: number }>()
  private lastHardRecoverAt = 0

  constructor(config: AppiumMobileDriverConfig) {
    this.config = config
  }

  async startSession(): Promise<void> {
    const caps = buildCapabilities(this.config)
    this.driver = await remote({
      protocol: "http",
      hostname: new URL(this.config.appiumUrl).hostname,
      port: parseInt(new URL(this.config.appiumUrl).port || "4723"),
      path: new URL(this.config.appiumUrl).pathname || "/wd/hub",
      // 单次 find 在真机卡死时，避免 WebDriverIO 默认 ~120s 才抛错。
      waitforTimeout: 2000,
      waitforInterval: 100,
      // 勿把 connectionRetryTimeout 设成 5s：WebDriverIO 用它做「单次 HTTP 请求」AbortSignal，
      // 会在 Appium/uia2 仍处理 click 时误报 aborted due to timeout（见 uiautomator2-error.ts）。
      connectionRetryTimeout: webdriverRequestTimeoutMs(),
      // 默认 3 会在 uia2 proxy 20s 超时后再叠 3 次，单次 tap 可拖到 ~80s。
      connectionRetryCount: 0,
      capabilities: {
        alwaysMatch: caps,
        firstMatch: [{}],
      },
    })
    await this.driver.setTimeout({ implicit: DEFAULT_IMPLICIT_WAIT_MS })
    if (this.config.platform === "android") {
      try {
        await this.driver.updateSettings({
          waitForIdleTimeout: 100,
          waitForSelectorTimeout: 3000,
        })
      } catch {
        // older uiautomator2 builds may ignore unknown keys
      }
      if (this.config.lockPortrait) {
        try {
          await this.driver.setOrientation("PORTRAIT")
        } catch {
          // 部分设备/Activity 不支持旋转时忽略
        }
      }
    }
  }

  async stopSession(): Promise<void> {
    if (this.driver) {
      try {
        await this.driver.deleteSession()
      } catch {
        // 真机 UiAutomator2 僵死时 deleteSession 也会超时；仍清空本地句柄。
      }
      this.driver = null
    }
  }

  private requireDriver(): Browser {
    if (!this.driver) {
      throw new Error("Session not started. Call startSession() first.")
    }
    return this.driver
  }

  /** 清掉设备上卡死的 UiAutomator2 instrumentation，避免 deleteSession/newSession 连锁超时。 */
  private killUiautomator2ServerOnDevice(): void {
    const udidArg = this.config.udid ? `-s ${this.config.udid} ` : ""
    for (const pkg of ["io.appium.uiautomator2.server", "io.appium.uiautomator2.server.test"]) {
      try {
        execSync(`adb ${udidArg}shell am force-stop ${pkg}`, { stdio: "ignore" })
      } catch {
        // ignore
      }
    }
  }

  /** adb 直跳 demo（与场景脚本 launchActivity 一致），供 hard recover 重建页面。 */
  private relaunchAndroidAppViaAdb(): void {
    const pkg = this.config.appPackage
    if (!pkg) return
    const act =
      this.config.appActivity ?? "com.tencent.kuikly.android.demo.KuiklyRenderActivity"
    const udidArg = this.config.udid ? `-s ${this.config.udid} ` : ""
    let cmd = `adb ${udidArg}shell am start -n ${pkg}/${act}`
    if (this.config.androidPageName) {
      cmd += ` --es pageName ${this.config.androidPageName}`
    }
    execSync(cmd, { stdio: "ignore" })
  }

  /** 软恢复失败时：删 session → adb 进页 → 新 session（避免 instrumentation 僵死拖满 20s×N）。 */
  private async hardRecoverAndroidUiAutomator2(): Promise<void> {
    const now = Date.now()
    if (now - this.lastHardRecoverAt < 8000) {
      throw new Error(
        "UiAutomator2 hard recover skipped (another recover within 8s); wait and retry E2E",
      )
    }
    this.lastHardRecoverAt = now
    const recoverT0 = Date.now()
    // eslint-disable-next-line no-console
    console.warn(
      "[kuikly-mobile-test] [hard_recover] start: force-stop uia2 → deleteSession → adb relaunch → newSession",
    )
    this.scrollWithinRectCache.clear()
    this.killUiautomator2ServerOnDevice()
    await this.stopSession()
    this.relaunchAndroidAppViaAdb()
    await new Promise((r) => setTimeout(r, 2500))
    await this.startSession()
    await new Promise((r) => setTimeout(r, 500))
    // eslint-disable-next-line no-console
    console.warn(
      `[kuikly-mobile-test] [hard_recover] done elapsed=${Date.now() - recoverT0}ms (retries same WebDriver call once)`,
    )
  }

  /** 真机 swipeGesture / 连点 find 后 instrumentation 挂掉时，用 activateApp 拉起进程再重试一次。 */
  private async recoverAndroidUiAutomator2(): Promise<void> {
    const driver = this.requireDriver()
    const appId = this.config.appPackage
    if (!appId) return
    try {
      await driver.activateApp(appId)
    } catch {
      await driver.execute("mobile: activateApp", { appId })
    }
    await new Promise((r) => setTimeout(r, 1200))
    try {
      await driver.updateSettings({
        waitForIdleTimeout: 100,
        waitForSelectorTimeout: 3000,
      })
    } catch {
      // ignore
    }
  }

  private async withUiAutomator2Retry<T>(fn: () => Promise<T>): Promise<T> {
    try {
      return await fn()
    } catch (e) {
      if (this.config.platform !== "android" || !isUiAutomator2RecoverableError(e)) throw e
      const kind = classifyUiautomator2Error(e)
      if (shouldHardRecoverUiautomator2(e)) {
        // eslint-disable-next-line no-console
        console.warn(formatUiautomator2RecoverLog(kind, "hard_recover", e))
        await this.hardRecoverAndroidUiAutomator2()
        return await fn()
      }
      // eslint-disable-next-line no-console
      console.warn(formatUiautomator2RecoverLog(kind, "soft_recover", e))
      await this.recoverAndroidUiAutomator2()
      try {
        return await fn()
      } catch (e2) {
        if (!isUiAutomator2RecoverableError(e2)) throw e2
        const kind2 = classifyUiautomator2Error(e2)
        // eslint-disable-next-line no-console
        console.warn(formatUiautomator2RecoverLog(kind2, "hard_recover", e2))
        await this.hardRecoverAndroidUiAutomator2()
        return await fn()
      }
    }
  }

  async getSnapshot(): Promise<UiSnapshot> {
    const driver = this.requireDriver()
    const pageSource = await driver.getPageSource()
    const elements = this.parsePageSource(pageSource)
    return {
      platform: this.config.platform,
      source: "appium",
      elements,
    }
  }

  async getViewTree(): Promise<UiViewTree> {
    const driver = this.requireDriver()
    const pageSource = await driver.getPageSource()
    const windowSize = await driver.getWindowSize()
    const viewport: [number, number] = [windowSize.width, windowSize.height]
    const tree = buildViewTree(pageSource, this.config.platform, viewport)
    if (!tree) throw new Error("Failed to parse view tree from page source")
    return tree
  }

  async tap(selector: Selector, options?: TapOptions): Promise<void> {
    await this.withUiAutomator2Retry(async () => {
      const driver = this.requireDriver()
      const ref = await findElementRefWithFallback(driver, selector, this.config.platform)
      await clickElementRef(driver, ref)
    })
    if (options?.postTap) {
      await this.runPostTap(options.postTap, describeSelectorBrief(selector))
    }
  }

  private async runPostTap(action: TapPostAction, selectorHint?: string): Promise<void> {
    if (action.strategy === "settle") {
      if (action.ms > 0) {
        logUiGestureSettle("tap.postTap.settle", action.ms, selectorHint)
        await new Promise((r) => setTimeout(r, action.ms))
      }
      return
    }
    if (action.strategy === "quiescence") {
      await this.waitForUiQuiescence({
        anchor: action.anchor,
        idleTimeoutMs: action.idleTimeoutMs,
        timeoutMs: action.timeoutMs,
        anchorProbeTimeoutMs: action.anchorProbeTimeoutMs,
        ifAnchorMissing: action.ifAnchorMissing ?? "idleOnly",
      })
      return
    }
    await this.waitForStableBounds(action.anchor, {
      timeoutMs: action.timeoutMs,
      pollIntervalMs: action.pollIntervalMs,
      stableSamples: action.stableSamples,
      tolerancePx: action.tolerancePx,
    })
  }

  private async restoreDefaultIdleTimeout(): Promise<void> {
    const driver = this.requireDriver()
    try {
      await driver.updateSettings({
        waitForIdleTimeout: defaultAndroidIdleTimeoutMs(),
        waitForSelectorTimeout: 3000,
      })
    } catch {
      // ignore
    }
  }

  private async idleOnlyProbe(): Promise<void> {
    const driver = this.requireDriver()
    await driver.getWindowSize()
  }

  private async probeQuiescenceAnchor(
    anchor: QuiescenceAnchor,
    probeTimeoutMs: number,
  ): Promise<boolean> {
    const driver = this.requireDriver()
    const locators = locatorsForQuiescenceAnchor(anchor, this.config.platform)
    const deadline = Date.now() + probeTimeoutMs
    for (const entry of locators) {
      if (Date.now() >= deadline) return false
      try {
        const els = await driver.findElements(entry.using, entry.value)
        if (els.length > 0) return true
      } catch {
        // 继续尝试下一条 locator
      }
    }
    return false
  }

  async waitForUiQuiescence(options: UiQuiescenceOptions = {}): Promise<void> {
    if (this.config.platform === "android") {
      await this.withUiAutomator2Retry(() => this.waitForUiQuiescenceOnce(options))
      return
    }
    await this.waitForUiQuiescenceOnce(options)
  }

  private async waitForUiQuiescenceOnce(options: UiQuiescenceOptions = {}): Promise<void> {
    const idleMs = options.idleTimeoutMs ?? defaultQuiescenceIdleMs()
    const timeoutMs = options.timeoutMs ?? DEFAULT_QUIESCENCE_TIMEOUT_MS
    const anchorProbeMs = options.anchorProbeTimeoutMs ?? DEFAULT_ANCHOR_PROBE_TIMEOUT_MS
    const ifMissing = options.ifAnchorMissing ?? "idleOnly"

    const driver = this.requireDriver()
    try {
      await driver.updateSettings({ waitForIdleTimeout: idleMs })
      if (options.anchor) {
        const found = await this.probeQuiescenceAnchor(
          options.anchor,
          Math.min(anchorProbeMs, timeoutMs),
        )
        if (found) {
          await this.idleOnlyProbe()
          return
        }
        const msg = `quiescence: anchor not found (${describeQuiescenceAnchor(options.anchor)}), fallback idle-only`
        if (ifMissing === "fail") {
          throw new Error(msg)
        }
        // eslint-disable-next-line no-console
        console.warn(`[kuikly-mobile-test] ${msg}`)
      }
      await this.idleOnlyProbe()
    } finally {
      await this.restoreDefaultIdleTimeout()
    }
  }

  async waitForStableBounds(
    anchor: QuiescenceAnchor,
    options?: import("./ui-stable.js").BoundsStableOptions,
  ): Promise<void> {
    const resolved = resolveBoundsStableOptions(options)
    const deadline = Date.now() + resolved.timeoutMs
    let prev: ElementRect | null = null
    let streak = 0

    while (Date.now() < deadline) {
      try {
        const rect = await this.withUiAutomator2Retry(() =>
          this.getElementRectForAnchor(anchor),
        )
        const tick = boundsStableTick(
          prev,
          rect,
          streak,
          resolved.stableSamples,
          resolved.tolerancePx,
        )
        prev = tick.prev
        streak = tick.streak
        if (tick.stable) return
      } catch {
        // 节点暂时不存在（如列表滑动中），继续轮询
      }
      await new Promise((r) => setTimeout(r, resolved.pollIntervalMs))
    }
    throw new Error(
      `waitForStableBounds timed out after ${resolved.timeoutMs}ms for anchor ${describeQuiescenceAnchor(anchor)}`,
    )
  }

  private async getElementRectForAnchor(anchor: QuiescenceAnchor): Promise<ElementRect> {
    if (isScrollableAnchor(anchor)) {
      const driver = this.requireDriver()
      const locators = locatorsForQuiescenceAnchor(anchor, this.config.platform)
      for (const entry of locators) {
        const els = await driver.findElements(entry.using, entry.value)
        if (els.length > 0) {
          return elementRectFromRef(driver, els[0] as ElementRef, this.config.platform)
        }
      }
      throw new Error("scrollable anchor not found")
    }
    return this.getElementRect(anchor)
  }

  async input(selector: Selector, text: string): Promise<void> {
    const driver = this.requireDriver()
    const ref = await findElementRefWithFallback(driver, selector, this.config.platform)
    const id = elementIdFromRef(ref)
    await driver.elementClear(id)
    await driver.elementSendKeys(id, text)
  }

  async back(): Promise<void> {
    const driver = this.requireDriver()
    if (this.config.platform === "ios") {
      const ref = await findElementRefWithFallback(
        driver,
        { testTag: NAV_BACK_TEST_TAG },
        "ios",
      )
      await clickElementRef(driver, ref)
      return
    }
    await driver.back()
  }

  async scroll(opts: ScrollOptions): Promise<void> {
    await this.withUiAutomator2Retry(async () => this.scrollOnce(opts))
  }

  private async scrollOnce(opts: ScrollOptions): Promise<void> {
    const driver = this.requireDriver()
    const fling = opts.fling === true

    // Android fling 走 Appium 原生 `mobile: swipeGesture`：UiAutomator2 直驱 GestureUtils#performSwipe，
    // emulator 也稳，避开 W3C performActions 偶发 VelocityTracker 丢样本问题。
    if (fling && this.config.platform === "android") {
      const direction = swipeDirectionFromOpts(opts)
      const area = opts.area ?? swipeAreaFromOpts(opts)
      try {
        // speed 单位 px/s；5000 在 1080p emulator 上能稳定触发惯性（≈1080px 滑半屏耗时 ~200ms）。
        // percent: 0.9 留 5% margin，避免 release 出列被判 cancel。
        await driver.execute("mobile: swipeGesture", {
          left: area.x,
          top: area.y,
          width: area.width,
          height: area.height,
          direction,
          percent: 0.9,
          speed: 5000,
        })
        return
      } catch (e) {
        if (isUiAutomator2DeadError(e)) throw e
        // 真机偶发 instrumentation 退出；降级 W3C 而非让业务脚本改 adb 绕过 scrollWithin。
        // eslint-disable-next-line no-console
        console.warn(
          `[kuikly-mobile-test] mobile:swipeGesture failed (${e instanceof Error ? e.message : String(e)}); fallback W3C performActions`,
        )
      }
    }

    // iOS fling 与所有非 Android-fling 路径：走 W3C performActions。
    // fling 模式：clamp 到 ≤200ms 以制造快甩。Android 借此保证 VelocityTracker 拿到高速样本；
    // iOS UIScrollView 即使普通拖拽也可能 decelerate，但短 duration 会带来更大的 velocity/targetContentOffset。
    const duration = fling
      ? Math.min(opts.durationMs ?? 150, 200)
      : (opts.durationMs ?? 500)
    const actions: Record<string, unknown>[] = [
      { type: "pointerMove", duration: 0, origin: "viewport", x: opts.startX, y: opts.startY },
      { type: "pointerDown", button: 0 },
    ]
    // 普通拖拽保留 100ms pause（兼容旧脚本/精确拖拽场景）；
    // 快甩省略 pause，否则 Android VelocityTracker 在长 pause 后判作 drag-release。
    if (!fling) {
      actions.push({ type: "pause", duration: 100 })
    }
    actions.push(
      { type: "pointerMove", duration, origin: "viewport", x: opts.endX, y: opts.endY },
      { type: "pointerUp", button: 0 },
    )
    await driver.performActions([{
      type: "pointer",
      id: "finger1",
      parameters: { pointerType: "touch" },
      actions,
    }])
    await driver.releaseActions()
  }

  /**
   * 在 target 容器内部滚动 —— 见 MobileDriver.scrollWithin 注释。五层防御：
   *  ① getElementRect 找不到节点抛错（不退化成全屏 swipe）
   *  ② clamp rect 到 viewport
   *  ③ margin 在 clamped 区上算
   *  ④ 起终点物理上在 clamped rect 内
   *  ⑤ Android+fling 路径把 clampedArea 给 mobile:swipeGesture，UiAutomator2 强制不出 area
   */
  async scrollWithin(target: Selector, opts: ScrollWithinOptions): Promise<void> {
    // ① 找容器（同 target 在 TTL 内复用 rect，减少连滑后的 uia2 查询）
    const cacheKey = JSON.stringify(target)
    const cached = this.scrollWithinRectCache.get(cacheKey)
    const rect =
      cached && Date.now() - cached.at < SCROLL_WITHIN_RECT_CACHE_TTL_MS
        ? cached.rect
        : await this.getElementRect(target)
    if (!cached || cached.rect !== rect) {
      this.scrollWithinRectCache.set(cacheKey, { rect, at: Date.now() })
    }
    const driver = this.requireDriver()
    const { width: vpW, height: vpH } = await driver.getWindowSize()

    // ② clamp 到 viewport
    const left = Math.max(0, rect.x)
    const top = Math.max(0, rect.y)
    const right = Math.min(vpW, rect.x + rect.width)
    const bottom = Math.min(vpH, rect.y + rect.height)
    const visW = right - left
    const visH = bottom - top

    const isVertical = opts.direction === "up" || opts.direction === "down"
    const usable = isVertical ? visH : visW
    const MIN_USABLE_PX = 100
    if (usable < MIN_USABLE_PX) {
      throw new Error(
        `scrollWithin: target ${JSON.stringify(target)} clamped 后可见${isVertical ? "高度" : "宽度"}=${usable}px ` +
          `< ${MIN_USABLE_PX}px（原 rect=${JSON.stringify(rect)}, viewport=${vpW}x${vpH}）。` +
          `目标被遮挡/出视口/列表为空？dismissAlert 后重试，或调用方先 scroll-into-view。`,
      )
    }

    // ③ margin 算在 clamped 可见区上
    const marginPct = opts.marginPercent ?? 0.05
    const requestedMargin = usable * marginPct
    // Android 的底部系统手势 / nav 区域可能仍在 viewport 内，5% margin 对贴底列表不够。
    // 但小容器不能被安全边距吃空，因此最多收缩到仍保留 MIN_USABLE_PX 的可滑动距离。
    const androidSystemGestureMargin = this.config.platform === "android" ? 80 : 0
    const maxMargin = Math.max(0, (usable - MIN_USABLE_PX) / 2)
    const margin = Math.min(Math.max(requestedMargin, androidSystemGestureMargin), maxMargin)
    const lo = (isVertical ? top : left) + margin
    const hi = (isVertical ? bottom : right) - margin

    // ④ direction = 内容方向；手指方向反向
    const cross = isVertical ? left + visW / 2 : top + visH / 2
    let startMain: number
    let endMain: number
    switch (opts.direction) {
      case "down":
      case "right":
        startMain = hi
        endMain = lo
        break
      case "up":
      case "left":
        startMain = lo
        endMain = hi
        break
    }
    const startX = isVertical ? cross : startMain
    const startY = isVertical ? startMain : cross
    const endX = isVertical ? cross : endMain
    const endY = isVertical ? endMain : cross

    // ⑤ Android+fling 走 mobile:swipeGesture 需要真实可滚动 area。
    // area 也必须使用同一份安全边距；否则 UiAutomator2 会在 full rect 内生成手势，
    // 贴底列表可能扫到系统手势区，把 App 送回 Launcher。
    const gestureArea: ElementRect = isVertical
      ? { x: left, y: lo, width: visW, height: hi - lo }
      : { x: lo, y: top, width: hi - lo, height: visH }

    const fling = opts.fling === true
    if (fling && usable < 300) {
      // eslint-disable-next-line no-console
      console.warn(
        `scrollWithin: fling on small region (${usable}px usable) — VelocityTracker 可能拿不到高速样本`,
      )
    }

    const durationMs = opts.durationMs ?? (fling ? 150 : 450)
    const settleMs =
      opts.settleMs ??
      (this.config.platform === "android" ? defaultScrollPostGestureSettleMs() : fling ? 1200 : 700)
    const times = opts.times ?? 1
    for (let i = 0; i < times; i++) {
      await this.scroll({
        startX,
        startY,
        endX,
        endY,
        durationMs,
        fling,
        area: gestureArea,
      })
      if (settleMs > 0) {
        logUiGestureSettle(
          "scroll.after",
          settleMs,
          `${describeSelectorBrief(target)} gesture=${i + 1}/${times}`,
        )
        await new Promise((r) => setTimeout(r, settleMs))
      }
    }
    if (opts.postScrollQuiescence === true) {
      await this.waitForUiQuiescence({
        anchor: opts.postScrollQuiescenceAnchor ?? target,
        idleTimeoutMs: defaultScrollQuiescenceIdleMs(),
        ifAnchorMissing: "idleOnly",
      })
    }
  }

  async tapCoordinate(x: number, y: number): Promise<void> {
    const driver = this.requireDriver()
    await driver.performActions([{
      type: "pointer",
      id: "finger1",
      parameters: { pointerType: "touch" },
      actions: [
        { type: "pointerMove", duration: 0, origin: "viewport", x, y },
        { type: "pointerDown", button: 0 },
        { type: "pause", duration: 100 },
        { type: "pointerUp", button: 0 },
      ],
    }])
    await driver.releaseActions()
  }

  async waitFor(selector: Selector, timeoutMs: number): Promise<void> {
    const driver = this.requireDriver()
    const deadline = Date.now() + timeoutMs
    while (Date.now() < deadline) {
      if (await this.elementExists(selector)) return
      await new Promise(r => setTimeout(r, 200))
    }
    throw new Error(`waitFor timed out after ${timeoutMs}ms for selector ${JSON.stringify(selector)}`)
  }

  /**
   * 单次探测节点是否存在（受 session implicit 限制，默认 ~400ms 内返回）。
   * 不做 UiAutomator2 hard recover：waitFor 轮询里 recover 会 deleteSession，
   * 与导航循环并发导致「session terminated」。
   */
  async elementExists(selector: Selector): Promise<boolean> {
    if (this.config.platform === "android") {
      try {
        return await this.withUiAutomator2Retry(() => this.elementExistsOnce(selector))
      } catch {
        return false
      }
    }
    return this.elementExistsOnce(selector)
  }

  private async elementExistsOnce(selector: Selector): Promise<boolean> {
    const driver = this.requireDriver()
    const locators = locatorsForSelector(selector, this.config.platform)
    for (const entry of locators) {
      const els = await driver.findElements(entry.using, entry.value)
      if (els.length > 0) return true
    }
    return false
  }

  /**
   * 读单个节点的展示文案，避免为读一个 testTag 去 dump 整页 getPageSource。
   * Android Compose：testTag 在 resource-id，文案常在 content-desc（如 Prefetch: ON）。
   */
  async getSelectorLabel(selector: Selector): Promise<string> {
    return this.withUiAutomator2Retry(async () => {
      const driver = this.requireDriver()
      const ref = await findElementRefWithFallback(driver, selector, this.config.platform)
      const id = elementIdFromRef(ref)
      if (this.config.platform === "android") {
        const desc = await driver.getElementAttribute(id, "content-desc")
        if (desc) return desc
        const text = await driver.getElementAttribute(id, "text")
        return text ?? ""
      }
      const label = await driver.getElementAttribute(id, "label")
      if (label) return label
      const name = await driver.getElementAttribute(id, "name")
      if (name) return name
      return (await driver.getElementText(id)) ?? ""
    })
  }

  async assertVisible(selector: Selector): Promise<void> {
    const driver = this.requireDriver()
    const ref = await findElementRefWithFallback(driver, selector, this.config.platform)
    const displayed = await driver.isElementDisplayed(elementIdFromRef(ref))
    if (!displayed) {
      throw new Error(`Assertion failed: element exists but not displayed for selector ${JSON.stringify(selector)}`)
    }
  }

  async assertText(text: string): Promise<void> {
    const driver = this.requireDriver()
    const snapshot = await this.getSnapshot()
    const found = snapshot.elements.some(
      (el) => el.text === text || el.text?.includes(text)
    )
    if (!found) {
      throw new Error(`Assertion failed: text "${text}" not found in snapshot`)
    }
  }

  async assertInViewport(selector: Selector): Promise<void> {
    const driver = this.requireDriver()
    const ref = await findElementRefWithFallback(driver, selector, this.config.platform)
    const { x, y, width, height } = await elementRectFromRef(
      driver,
      ref,
      this.config.platform,
    )
    if (width === 0 || height === 0) {
      throw new Error(
        `assertInViewport failed: element has zero size (width=${width}, height=${height}), likely off-screen`,
      )
    }
    if (this.config.platform === "ios") {
      const windowSize = await driver.getWindowSize()
      if (
        x + width <= 0 ||
        y + height <= 0 ||
        x >= windowSize.width ||
        y >= windowSize.height
      ) {
        throw new Error(
          `assertInViewport failed: element at (${x},${y}) size (${width},${height}) is outside window (${windowSize.width}x${windowSize.height})`,
        )
      }
    }
  }

  async getElementRect(selector: Selector): Promise<ElementRect> {
    return this.withUiAutomator2Retry(async () => {
      const driver = this.requireDriver()
      const ref = await findElementRefWithFallback(driver, selector, this.config.platform)
      return elementRectFromRef(driver, ref, this.config.platform)
    })
  }

  async takeScreenshot(outputPath: string): Promise<void> {
    const driver = this.requireDriver()
    const screenshot = await driver.takeScreenshot()
    const { writeFile } = await import("fs/promises")
    await writeFile(outputPath, screenshot, "base64")
  }

  async dismissAlert(): Promise<void> {
    const driver = this.requireDriver()
    try {
      await driver.execute("mobile: dismissAlert", { action: "accept" })
    } catch {
      try {
        const btn = await driver.$('-ios predicate string:type == "XCUIElementTypeButton" AND name == "确定"')
        if (await btn.isExisting()) {
          await btn.click()
        }
      } catch {}
    }
  }

  async getPageSource(): Promise<string> {
    const driver = this.requireDriver()
    return driver.getPageSource()
  }

  async restartApp(): Promise<void> {
    const driver = this.requireDriver()
    const appId = this.config.bundleId || this.config.appPackage
    if (!appId) throw new Error("Cannot restart app: no bundleId or appPackage configured")
    if (this.config.platform === "android") {
      await driver.execute("mobile: terminateApp", { appId })
      await new Promise(r => setTimeout(r, 1000))
      await driver.execute("mobile: activateApp", { appId })
    } else {
      await driver.execute("mobile: terminateApp", { bundleId: appId })
      await new Promise(r => setTimeout(r, 1000))
      await driver.execute("mobile: launchApp", { bundleId: appId })
    }
    await new Promise(r => setTimeout(r, 2000))
  }

  private parsePageSource(xml: string): UiElementSnapshot[] {
    const elements: UiElementSnapshot[] = []

    const attrPatterns: Record<string, RegExp> = {
      id: /\bresource-id="([^"]*)"/,
      text: /\btext="([^"]*)"/,
      type: /\b(?:class|type)="([^"]*)"/,
      enabled: /\benabled="([^"]*)"/,
      clickable: /\bclickable="([^"]*)"/,
      visible: /\b(?:displayed|visible)="([^"]*)"/,
      bounds: /\bbounds="\[([^\]]*)\]\[([^\]]*)\]"/,
      contentDesc: /\bcontent-desc="([^"]*)"/,
      label: /\blabel="([^"]*)"/,
      name: /\bname="([^"]*)"/,
      value: /\bvalue="([^"]*)"/,
      x: /\bx="([^"]*)"/,
      y: /\by="([^"]*)"/,
      width: /\bwidth="([^"]*)"/,
      height: /\bheight="([^"]*)"/,
    }

    const lines = xml.split(/>\s*</)
    for (const line of lines) {
      const isElement = /^[A-Z]/.test(line) || /^node\b/.test(line) || /^XCUIElementType/.test(line)
      if (!isElement) continue

      const el: UiElementSnapshot = {}

      const id = attrPatterns.id.exec(line)?.[1]
      const contentDesc = attrPatterns.contentDesc.exec(line)?.[1]
      const label = attrPatterns.label.exec(line)?.[1]
      const name = attrPatterns.name.exec(line)?.[1]
      const rawText = attrPatterns.text.exec(line)?.[1]
      const rawValue = attrPatterns.value.exec(line)?.[1]
      const textVal = rawText || rawValue || label || name

      const typeVal = attrPatterns.type.exec(line)?.[1]
      const enabledVal = attrPatterns.enabled.exec(line)?.[1]
      const clickableVal = attrPatterns.clickable.exec(line)?.[1]
      const visibleVal = attrPatterns.visible.exec(line)?.[1]
      const boundsVal = attrPatterns.bounds.exec(line)

      if (!id && !contentDesc && !textVal) continue

      if (id) {
        const colonIdx = id.indexOf(":id/")
        el.testTag = colonIdx !== -1 ? id.substring(colonIdx + 4) : (id.includes(":") ? undefined : id)
      }
      if (contentDesc && !el.testTag) el.testTag = contentDesc
      else if (name && typeVal?.startsWith("XCUIElementType") && name !== label && !el.testTag) {
        const spaceIdx = name.indexOf(" ")
        el.testTag = spaceIdx !== -1 ? name.substring(spaceIdx + 1) : name
      }
      if (textVal) el.text = textVal
      if (typeVal) el.type = typeVal
      if (enabledVal) el.enabled = enabledVal === "true"
      if (clickableVal) el.clickable = clickableVal === "true"
      if (visibleVal) el.visible = visibleVal === "true"

      if (boundsVal) {
        const leftTop = boundsVal[1].split(",").map(Number)
        const rightBottom = boundsVal[2].split(",").map(Number)
        el.bounds = [leftTop[0], leftTop[1], rightBottom[0], rightBottom[1]]
      } else {
        const xVal = attrPatterns.x.exec(line)?.[1]
        const yVal = attrPatterns.y.exec(line)?.[1]
        const wVal = attrPatterns.width.exec(line)?.[1]
        const hVal = attrPatterns.height.exec(line)?.[1]
        if (xVal && yVal && wVal && hVal) {
          const x = Number(xVal), y = Number(yVal), w = Number(wVal), h = Number(hVal)
          el.bounds = [x, y, x + w, y + h]
        }
      }

      elements.push(el)
    }

    return elements
  }
}
