/**
 * Classify WebDriver / Appium errors on Android for recover strategy and logging.
 * See pitfalls.md §13 and session evidence (instrumentation Process crashed vs client abort).
 */

export type Uiautomator2ErrorClass =
  | "instrumentation_dead"
  | "appium_proxy_timeout"
  | "webdriver_client_abort"
  | "other"

export function errorMessage(err: unknown): string {
  return err instanceof Error ? err.message : String(err)
}

export function classifyUiautomator2Error(err: unknown): Uiautomator2ErrorClass {
  const msg = errorMessage(err)
  if (
    /instrumentation process is not running|cannot be proxied to UiAutomator2/i.test(msg) ||
    /Process crashed/i.test(msg) ||
    /socket hang up/i.test(msg)
  ) {
    return "instrumentation_dead"
  }
  if (/operation was aborted due to timeout when running/i.test(msg)) {
    return "webdriver_client_abort"
  }
  if (
    /Could not proxy command to the remote server/i.test(msg) ||
    /timeout of \d+ms exceeded when running "/i.test(msg)
  ) {
    return "appium_proxy_timeout"
  }
  return "other"
}

export function isUiAutomator2RecoverableError(err: unknown): boolean {
  const kind = classifyUiautomator2Error(err)
  return kind !== "other"
}

/** Crash / Appium 等 uia2 代理超时 → 直接 hard recover；避免 soft recover 在死进程上再耗 5～20s。 */
export function shouldHardRecoverUiautomator2(err: unknown): boolean {
  const kind = classifyUiautomator2Error(err)
  return kind === "instrumentation_dead" || kind === "appium_proxy_timeout"
}

export function formatUiautomator2RecoverLog(
  kind: Uiautomator2ErrorClass,
  action: "hard_recover" | "soft_recover",
  err: unknown,
): string {
  const hint =
    kind === "webdriver_client_abort"
      ? " (check WEBDRIVER_REQUEST_TIMEOUT_MS; do not set connectionRetryTimeout too low)"
      : kind === "appium_proxy_timeout"
        ? " (Appium uiautomator2ServerReadTimeout / device uia2 slow)"
        : kind === "instrumentation_dead"
          ? " (io.appium.uiautomator2.server crashed; see Appium log INSTRUMENTATION_RESULT)"
          : ""
  return `[kuikly-mobile-test] UiAutomator2 class=${kind} action=${action}${hint}: ${errorMessage(err)}`
}

/** WebDriverIO 用此值作为单次 HTTP 请求的 AbortSignal.timeout（默认 120s，勿误设为 5s）。 */
export function webdriverRequestTimeoutMs(): number {
  const raw = process.env.WEBDRIVER_REQUEST_TIMEOUT_MS
  if (!raw) return 120_000
  const n = Number.parseInt(raw, 10)
  return Number.isFinite(n) && n >= 10_000 ? n : 120_000
}
