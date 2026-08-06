import type { BoundsStableOptions } from "./ui-stable.js"

export type Platform = "android" | "ios"

/** Demo navigation bar back button testTag (ComposeNavigationBar + Self DSL NavigationBar). */
export const NAV_BACK_TEST_TAG = "nav_back"

export type Selector =
  | { id: string }
  | { text: string }
  | { accessibilityId: string }
  | { testTag: string }
  | { xpath: string }

/** quiescence 用的参照节点：常规 selector 或整屏第一个可滚动容器。 */
export type QuiescenceAnchor = Selector | { scrollable: true }

export interface ScrollOptions {
  startX: number
  startY: number
  endX: number
  endY: number
  /**
   * 滑动手势（pointerMove）持续时长。
   * - 普通拖拽 / 慢速滚动：默认 500ms
   * - 快甩模式：建议 ≤ 200ms（与 `fling: true` 配合，制造更高速度）
   */
  durationMs?: number
  /**
   * 是否使用「快甩」手势。
   *
   * - `false`（默认）：兼容老行为，pointerDown 后插入 100ms pause 再 pointerMove，
   *   适合精确拖拽 / 慢速滚动。注意 iOS UIScrollView 仍可能因系统判定速度足够而进入 deceleration。
   * - `true`：去掉 down → move 之间的 leading pause，并对 `durationMs` 自动 clamp 到 ≤200ms，
   *   让 VelocityTracker / UIScrollView 在 pointerUp 时拿到「短时大位移」的高速度样本。
   *
   * 实战经验（Android）：留 100ms pause 时几乎 100% 被判作 drag-release（无惯性）；
   * 去掉后 swipe 距离 ≥ 半屏、duration ≈ 150ms 即可稳定 fling。iOS 对 pause 不那么敏感，
   * `fling: true` 的价值是 velocity / targetContentOffset 更大，而不是唯一能触发惯性的开关。
   *
   * 平台分发（仅当 fling=true）：
   * - **Android**：优先走 Appium 原生 `mobile: swipeGesture`（UiAutomator2 driver）。它直接驱动
   *   `GestureUtils#performSwipe`，emulator 也能稳定触发惯性，避免 W3C performActions 上
   *   VelocityTracker 偶尔丢失高速样本的问题。需要 `area`（或可由 startX/Y..endX/Y 推算的包围矩形）。
   * - **iOS**：继续走 W3C `performActions` + 短 duration；XCUITest 对 W3C 路径处理稳定。
   *   需要确认是否真的进入惯性时，优先看 render 层 UIScrollView delegate 日志
   *   (`willEndDragging velocity` / `willDecelerate` / `didEndDecelerating`)，不要用 page source hash。
   */
  fling?: boolean
  /**
   * 滚动手势作用的矩形区域（屏幕坐标系）。仅当 `fling=true && platform="android"` 时被使用，
   * 作为 `mobile: swipeGesture` 的 area 参数（必须把 LazyColumn 的可滚动 viewport 整个圈住，
   * 否则 release 出列会被 Android 判作 cancel）。
   *
   * 缺省时按 startX/Y..endX/Y 反推一个包围盒（保守，可能会比真实可滚动区域窄）。
   */
  area?: ElementRect
}

export interface ElementRect {
  x: number
  y: number
  width: number
  height: number
}

/**
 * `scrollWithin(target, opts)` 的参数。语义比 `scroll()` 高一层：
 *   "在某个容器（通常是 LazyColumn）内部滚动"，driver 负责拿 rect、算 margin、夹 viewport、
 *   选 fling 路径，调用方只描述"想看到什么方向的内容、滑几次、要不要惯性"。
 *
 * 五层防御保证手势真的落在 target 范围内（见 AppiumMobileDriver.scrollWithin 注释）。
 */
export interface ScrollWithinOptions {
  /**
   * **内容**滚动方向，不是手指方向：
   * - `"down"` = 内容下滚（看见后续 item）= 手指 bottom → top
   * - `"up"`   = 内容上滚（看见前面 item）= 手指 top → bottom
   * - `"left"` / `"right"` 同理
   */
  direction: "up" | "down" | "left" | "right"
  /** 重复滑动次数，默认 1。两次之间 sleep(settleMs)。 */
  times?: number
  /**
   * 是否触发原生惯性 fling。Android 走 `mobile: swipeGesture`，iOS 走短 duration W3C。
   * 详见 `ScrollOptions.fling`。
   */
  fling?: boolean
  /** 手势持续时间。缺省：fling ? 150ms : 450ms。 */
  durationMs?: number
  /**
   * 防 release 出列被判 cancel 的内边距比例。默认 0.05（5%），从 clamped 后的可见区两端各扣。
   * Android 还会叠加最小 80px 系统手势安全边距（但保留至少 100px 可滑动距离），
   * 避免底部贴边列表扫到系统手势区。想让 iOS 贴着边沿（极端 fling）可以传 0；
   * Android 上传 0 也不会取消 80px 系统安全边距。
   */
  marginPercent?: number
  /**
   * 每次滑动手势结束后盲等（不 find）。默认 **0**（可用 `APPIUM_SCROLL_POST_SETTLE_MS` 覆盖）；
   * iOS 未指定时仍为 fling 1200 / 非 fling 700。
   */
  settleMs?: number
  /**
   * 全部手势结束后再做一次 ui quiescence（对 anchor find + idle，见 APPIUM_SCROLL_WAIT_FOR_IDLE_MS）。
   * 默认 false；动画期 find 易触发真机 u2 crash，优先用 `settleMs` 盲等。
   */
  postScrollQuiescence?: boolean
  /** postScrollQuiescence 时用作 anchor，默认即 scrollWithin 的 target */
  postScrollQuiescenceAnchor?: QuiescenceAnchor
}

export interface UiElementSnapshot {
  testTag?: string
  text?: string
  type?: string
  enabled?: boolean
  clickable?: boolean
  visible?: boolean
  bounds?: [number, number, number, number]
}

export interface UiTreeNode {
  type: string
  rawType?: string
  testTag?: string
  text?: string
  value?: string
  placeholder?: string
  accessible?: boolean
  enabled?: boolean
  visible?: boolean
  clickable?: boolean
  checked?: boolean
  scrollable?: boolean
  traits?: string
  /** Absolute screen coordinates: [x, y, width, height] */
  bounds?: [number, number, number, number]
  /** Parent-relative coordinates: [x, y, width, height] (x/y relative to parent's top-left) */
  boundsParent?: [number, number, number, number]
  children: UiTreeNode[]
}

export interface UiViewTree {
  platform: Platform
  source: "appium"
  viewport: [number, number]
  tree: UiTreeNode
}

export interface UiSnapshot {
  platform: Platform
  source: "appium"
  elements: UiElementSnapshot[]
}

/**
 * tap / 重交互之后的通用收敛策略（与业务 log 无关）。
 *
 * - quiescence：临时抬高 Appium `waitForIdleTimeout`，再对 anchor 做一次轻量 find，
 *   让驱动在「无障碍树相对静止」后再继续（适合 reset、场景切换、scrollToItem 后）。
 * - stableBounds：轮询 anchor 的屏幕 bounds，连续 N 次一致才算稳定（适合弹窗、按钮位置；
 *   **不能**用来判断 LazyColumn 内部 item 滚动结束——容器 rect 通常不变）。
 * - settle：固定 sleep（盲等），兼容旧脚本。
 */
export type TapPostAction =
  | { strategy: "settle"; ms: number }
  | {
      strategy: "quiescence"
      /** 必填：等哪片 UI 静下来（勿绑会滑出树的 list item） */
      anchor: QuiescenceAnchor
      /** 临时 waitForIdleTimeout，默认 1000（APPIUM_WAIT_FOR_IDLE_MS） */
      idleTimeoutMs?: number
      /** 整体超时（ms），默认 10000 */
      timeoutMs?: number
      /** 子超时：尝试 find anchor（ms），默认 3000 */
      anchorProbeTimeoutMs?: number
      /** anchor 找不到时：idleOnly 继续测（默认）| fail */
      ifAnchorMissing?: "idleOnly" | "fail"
    }
  | {
      strategy: "stableBounds"
      anchor: QuiescenceAnchor
      timeoutMs?: number
      pollIntervalMs?: number
      stableSamples?: number
      tolerancePx?: number
    }

export interface TapOptions {
  /** tap 后收敛（`settle` / `quiescence` / `stableBounds`）。默认不盲等。 */
  postTap?: TapPostAction
}

export interface UiQuiescenceOptions {
  /** 省略则仅 idle-only（适合 tapCoordinate / 无稳定节点） */
  anchor?: QuiescenceAnchor
  idleTimeoutMs?: number
  timeoutMs?: number
  anchorProbeTimeoutMs?: number
  ifAnchorMissing?: "idleOnly" | "fail"
}

export interface MobileDriver {
  startSession(): Promise<void>
  stopSession(): Promise<void>
  getSnapshot(): Promise<UiSnapshot>
  getViewTree(): Promise<UiViewTree>
  tap(selector: Selector, options?: TapOptions): Promise<void>
  /**
   * 临时抬高 waitForIdleTimeout 后对 anchor 做一次 find，用于动画/重组后的通用收敛。
   * Android / iOS 均通过 Appium settings 实现，不解析业务 log。
   */
  waitForUiQuiescence(options: UiQuiescenceOptions): Promise<void>
  /**
   * 轮询直到 anchor 的 bounds 连续稳定。见 TapPostAction.stableBounds 说明。
   */
  waitForStableBounds(anchor: QuiescenceAnchor, options?: BoundsStableOptions): Promise<void>
  tapCoordinate(x: number, y: number): Promise<void>
  input(selector: Selector, text: string): Promise<void>
  /**
   * Return to the previous page.
   * - Android: system Back key via Appium.
   * - iOS: taps nav_back testTag (RouterModule.closePage); does NOT use Appium mobile:back.
   */
  back(): Promise<void>
  scroll(opts: ScrollOptions): Promise<void>
  /**
   * 在 `target` 容器内部滚动（LazyColumn / ScrollView / etc.）。比 `scroll()` 多干这些活：
   *  1. `getElementRect(target)` 拿到容器屏幕矩形 —— 找不到节点直接抛
   *  2. clamp 到 viewport 内（防 rect 部分出视口）
   *  3. clamped 区两端按 `marginPercent` 留边（防 release 出列被判 cancel）
   *  4. 按 `direction` 算手指起终点（注意是**内容**方向，不是手指方向）
   *  5. Android+fling 路径自动把 clamped rect 作 area 传给 `mobile: swipeGesture`，
   *     UiAutomator2 内部强制不出 area
   *
   * 找不到 target、或 clamp 后可用边过窄（< 100px），抛 Error；不要悄悄退化成全屏 swipe。
   */
  scrollWithin(target: Selector, opts: ScrollWithinOptions): Promise<void>
  waitFor(selector: Selector, timeoutMs: number): Promise<void>
  elementExists(selector: Selector): Promise<boolean>
  /** 读单个 selector 的 label/content-desc，避免整页 getPageSource。 */
  getSelectorLabel(selector: Selector): Promise<string>
  assertVisible(selector: Selector): Promise<void>
  assertText(text: string): Promise<void>
  assertInViewport(selector: Selector): Promise<void>
  getElementRect(selector: Selector): Promise<ElementRect>
  takeScreenshot(outputPath: string): Promise<void>
  dismissAlert(): Promise<void>
  restartApp(): Promise<void>
  getPageSource(): Promise<string>
}
