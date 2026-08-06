import type { ElementRect } from "./mobile-driver.js"

export interface BoundsStableOptions {
  timeoutMs?: number
  pollIntervalMs?: number
  /** 连续多少次采样 bounds 一致才算稳定，默认 3 */
  stableSamples?: number
  tolerancePx?: number
}

const DEFAULT_TIMEOUT_MS = 10_000
const DEFAULT_POLL_MS = 150
const DEFAULT_STABLE_SAMPLES = 3
const DEFAULT_TOLERANCE_PX = 2

export function rectsNearEqual(a: ElementRect, b: ElementRect, tolerancePx: number): boolean {
  return (
    Math.abs(a.x - b.x) <= tolerancePx &&
    Math.abs(a.y - b.y) <= tolerancePx &&
    Math.abs(a.width - b.width) <= tolerancePx &&
    Math.abs(a.height - b.height) <= tolerancePx
  )
}

/**
 * 在 poll 循环外维护的状态机：连续 stableSamples 次 bounds 一致则返回 true。
 */
export function boundsStableTick(
  prev: ElementRect | null,
  next: ElementRect,
  streak: number,
  stableSamples: number,
  tolerancePx: number,
): { prev: ElementRect; streak: number; stable: boolean } {
  if (prev !== null && rectsNearEqual(prev, next, tolerancePx)) {
    const newStreak = streak + 1
    return { prev: next, streak: newStreak, stable: newStreak >= stableSamples }
  }
  return { prev: next, streak: 1, stable: stableSamples <= 1 }
}

export function resolveBoundsStableOptions(opts?: BoundsStableOptions): Required<BoundsStableOptions> {
  return {
    timeoutMs: opts?.timeoutMs ?? DEFAULT_TIMEOUT_MS,
    pollIntervalMs: opts?.pollIntervalMs ?? DEFAULT_POLL_MS,
    stableSamples: opts?.stableSamples ?? DEFAULT_STABLE_SAMPLES,
    tolerancePx: opts?.tolerancePx ?? DEFAULT_TOLERANCE_PX,
  }
}

/** tap / 重交互后收敛：临时 waitForIdleTimeout，默认 1000ms */
export function defaultQuiescenceIdleMs(): number {
  const raw = process.env.APPIUM_WAIT_FOR_IDLE_MS
  if (!raw) return 1000
  const n = Number.parseInt(raw, 10)
  return Number.isFinite(n) && n >= 0 ? n : 1000
}

/** scrollWithin / fling 后收敛：默认 2000ms */
export function defaultScrollQuiescenceIdleMs(): number {
  const raw = process.env.APPIUM_SCROLL_WAIT_FOR_IDLE_MS
  if (!raw) return 2000
  const n = Number.parseInt(raw, 10)
  return Number.isFinite(n) && n >= 0 ? n : 2000
}

/**
 * 单次 scroll / fling 手势后的盲等（不 find）。默认 0；真机 u2 不稳时可设 APPIUM_SCROLL_POST_SETTLE_MS。
 */
export function defaultScrollPostGestureSettleMs(): number {
  const raw = process.env.APPIUM_SCROLL_POST_SETTLE_MS
  if (!raw) return 0
  const n = Number.parseInt(raw, 10)
  return Number.isFinite(n) && n >= 0 ? n : 0
}

/** tap 后盲等。默认 0；可设 APPIUM_TAP_POST_SETTLE_MS。 */
export function defaultTapPostGestureSettleMs(): number {
  const raw = process.env.APPIUM_TAP_POST_SETTLE_MS
  if (!raw) return 0
  const n = Number.parseInt(raw, 10)
  return Number.isFinite(n) && n >= 0 ? n : 0
}

export const DEFAULT_QUIESCENCE_TIMEOUT_MS = 10_000
export const DEFAULT_ANCHOR_PROBE_TIMEOUT_MS = 3000

export function defaultAndroidIdleTimeoutMs(): number {
  const raw = process.env.APPIUM_ANDROID_WAIT_FOR_IDLE_TIMEOUT_MS
  if (!raw) return 100
  const n = Number.parseInt(raw, 10)
  return Number.isFinite(n) && n >= 0 ? n : 100
}
