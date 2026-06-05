/**
 * 9.12 半自动 E2E：HTTP Server 驱动手势 + 与全自动相同的 logcat 断言与报告。
 *
 * 前置：Appium @4723、MOBILE_TEST HTTP Server（默认 7902）、真机已连接。
 *
 *   MOBILE_TEST_PORT=7902 npx tsx .claude/skills/kuikly-mobile-test/src/server.ts
 *
 * Run（checkout 根）:
 *   KUIKLY_REPO_ROOT=$PWD ANDROID_UDID=<udid> MOBILE_TEST_PORT=7902 \
 *     npm run semi-auto:912 --prefix .claude/skills/kuikly-mobile-test
 */

import { execSync, spawnSync } from "node:child_process"
import { appendFile, mkdir, writeFile } from "node:fs/promises"
import { formatSessionLogLine } from "../../.claude/skills/kuikly-mobile-test/src/evidence.js"
import { LOGS_DIR, REPORTS_DIR } from "../../.claude/skills/kuikly-mobile-test/src/paths.js"
import {
  EvidenceError,
  parsePrefetchDemoLines,
  type LogMetrics,
} from "./lazy-prefetch-metrics.js"
import {
  buildReport,
  CASE_912_ID,
  CASE_912_NAME,
  runCase912WithDeps,
  type LazyPrefetchDeps,
  type TestResult,
} from "./lazy-prefetch-run-cases.js"
import {
  createLazyPrefetchRunMeta,
  formatLazyPrefetchRunDialogueSummary,
} from "./lazy-prefetch-run-utils.js"

const PORT = Number(process.env.MOBILE_TEST_PORT ?? 7902)
const BASE = `http://localhost:${PORT}`
const UDID = process.env.ANDROID_UDID ?? "15490798770018J"
const PKG = process.env.ANDROID_APP_PACKAGE ?? "com.tencent.kuikly.android.demo"
const ACT =
  process.env.ANDROID_APP_ACTIVITY ?? "com.tencent.kuikly.android.demo.KuiklyRenderActivity"
const PAGE = "LazyListPrefetchDemo"

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms))

function adb(args: string) {
  execSync(`adb -s ${UDID} ${args}`, { stdio: "pipe" })
}

function readPrefetchLogcat(): LogMetrics {
  const result = spawnSync(
    "adb",
    ["-s", UDID, "logcat", "-d", "-s", "System.out"],
    { encoding: "utf8" },
  )
  const lines = (result.stdout ?? "")
    .split("\n")
    .filter((l) => l.includes("LazyListPrefetchDemo"))
  return parsePrefetchDemoLines(lines)
}

function readPrefetchTraceLogcat(): string[] {
  const result = spawnSync(
    "adb",
    ["-s", UDID, "logcat", "-d", "-s", "System.out"],
    { encoding: "utf8" },
  )
  return (result.stdout ?? "")
    .split("\n")
    .filter((l) => l.includes("LazyListPrefetchTrace"))
}

async function httpJson(
  method: string,
  path: string,
  body?: Record<string, unknown>,
): Promise<Record<string, unknown>> {
  const init: RequestInit = {
    method,
    headers: { "Content-Type": "application/json" },
    signal: AbortSignal.timeout(path === "/start-session" ? 180_000 : 300_000),
  }
  if (body !== undefined) init.body = JSON.stringify(body)
  const res = await fetch(`${BASE}${path}`, init)
  const json = (await res.json()) as Record<string, unknown>
  if (!res.ok || json.ok === false) {
    throw new Error(
      `HTTP ${method} ${path} failed: ${res.status} ${JSON.stringify(json)}`,
    )
  }
  return json
}

class SessionLogWriter {
  constructor(readonly filePath: string) {}

  async write(message: string): Promise<void> {
    await appendFile(this.filePath, `${formatSessionLogLine(message)}\n`)
  }
}

function createHttpDeps(log: SessionLogWriter): LazyPrefetchDeps {
  const prefetchUi = { value: false }

  const tapTag = async (tag: string) => {
    await log.write(`HTTP POST /tap testTag=${tag}`)
    await httpJson("POST", "/tap", { selector: { testTag: tag } })
  }

  const notImpl = (name: string): never => {
    throw new Error(`${name} not used in 9.12 semi-auto`)
  }

  return {
    platform: "android",
    openDemo: async () => notImpl("openDemo"),
    clearLog() {
      adb("logcat -c")
    },
    readDemoMetrics: readPrefetchLogcat,
    readTraceLines: readPrefetchTraceLogcat,
    captureIdleWindow: async () => notImpl("captureIdleWindow"),
    readMetricsAfterScroll: async () => notImpl("readMetricsAfterScroll"),
    readReverseScrollPhase: async () => notImpl("readReverseScrollPhase"),
    tapScenarioModifierOptIn: async () => {
      await tapTag("scenario_modifier_opt_in")
    },
    tapScenarioGlobalOnly: async () => notImpl("tapScenarioGlobalOnly"),
    tapScenarioModifierOverrideOff: async () => notImpl("tapScenarioModifierOverrideOff"),
    tapScenarioCacheWindow: async () => notImpl("tapScenarioCacheWindow"),
    tapHeavyItemsToggle: async () => notImpl("tapHeavyItemsToggle"),
    tapResetCounts: async () => {
      await tapTag("reset_counts")
      prefetchUi.value = false
      await sleep(2000)
    },
    tapClearMetricsOnly: async () => tapTag("clear_metrics_only"),
    ensurePrefetchOff: async () => {
      if (prefetchUi.value) {
        await tapTag("prefetch_toggle")
        prefetchUi.value = false
      }
    },
    ensurePrefetchOn: async () => {
      if (!prefetchUi.value) {
        await tapTag("prefetch_toggle")
        prefetchUi.value = true
      }
    },
    scrollList: async (direction, times = 3, opts = {}) => {
      await log.write(
        `HTTP POST /scroll-within direction=${direction} times=${times} fling=${opts.fling ?? true}`,
      )
      await httpJson("POST", "/scroll-within", {
        selector: { testTag: "lazy_list" },
        direction,
        times,
        fling: opts.fling ?? true,
        durationMs: opts.durationMs,
        settleMs: opts.settleMs,
      })
    },
    setPrefetchUiState(value: boolean) {
      prefetchUi.value = value
    },
  }
}

async function ensureServerAndAppium(): Promise<void> {
  try {
    const st = await fetch(`${BASE}/status`, { signal: AbortSignal.timeout(3000) })
    const body = (await st.json()) as { ok?: boolean }
    if (!body.ok) throw new Error("status not ok")
  } catch {
    throw new Error(
      `MobileTest HTTP Server 未就绪 (${BASE})。请先运行：MOBILE_TEST_PORT=${PORT} npx tsx .claude/skills/kuikly-mobile-test/src/server.ts`,
    )
  }
  const appiumUrl = process.env.APPIUM_URL ?? "http://127.0.0.1:4723"
  try {
    const st = await fetch(`${appiumUrl}/status`, { signal: AbortSignal.timeout(3000) })
    if (!st.ok) throw new Error(`Appium HTTP ${st.status}`)
  } catch {
    throw new Error(`Appium 未就绪 (${appiumUrl})，请先 appium --port 4723`)
  }
}

async function openPageAndSession(log: SessionLogWriter): Promise<void> {
  await log.write(`adb launch ${PAGE}`)
  adb(`shell am force-stop ${PKG}`)
  await sleep(1000)
  adb(`shell am start -n ${PKG}/${ACT} --es pageName ${PAGE}`)
  await sleep(4000)

  await log.write("HTTP POST /start-session noRestartApp=true")
  await httpJson("POST", "/start-session", {
    platform: "android",
    udid: UDID,
    appPackage: PKG,
    appActivity: ACT,
    deviceName: UDID,
    noRestartApp: true,
  })

  await log.write("HTTP POST /wait-for prefetch_toggle")
  await httpJson("POST", "/wait-for", {
    selector: { testTag: "prefetch_toggle" },
    timeoutMs: 20_000,
  })
}

async function main() {
  await mkdir(LOGS_DIR, { recursive: true })
  await mkdir(REPORTS_DIR, { recursive: true })

  const runMeta = createLazyPrefetchRunMeta()
  const sessionPath = `${LOGS_DIR}/mobile_test_session_android_prefetch_semi_${runMeta.stamp}.log`
  const reportPath = `${REPORTS_DIR}/lazy_prefetch_semi_${runMeta.stamp}.md`
  const log = new SessionLogWriter(sessionPath)

  await writeFile(
    sessionPath,
    `${formatSessionLogLine(`SESSION START semi-auto ${CASE_912_ID} udid=${UDID} port=${PORT}`)}\n`,
  )

  await ensureServerAndAppium()
  await openPageAndSession(log)

  const deps = createHttpDeps(log)
  const t0 = Date.now()
  let result: TestResult

  try {
    const evidence = await runCase912WithDeps(deps)
    const durationMs = Date.now() - t0
    result = {
      id: CASE_912_ID,
      name: CASE_912_NAME,
      status: "passed",
      evidence,
      durationMs,
    }
    await log.write(`${CASE_912_ID} ${CASE_912_NAME} → 通过 (${durationMs}ms)`)
    console.log(`✅ ${CASE_912_ID} ${CASE_912_NAME}`)
  } catch (err) {
    const detail = err instanceof Error ? err.message : String(err)
    const evidence = err instanceof EvidenceError ? err.evidence : undefined
    const durationMs = Date.now() - t0
    result = {
      id: CASE_912_ID,
      name: CASE_912_NAME,
      status: "failed",
      detail,
      evidence,
      durationMs,
    }
    await log.write(`${CASE_912_ID} ${CASE_912_NAME} → 不通过: ${detail} (${durationMs}ms)`)
    console.error(`❌ ${CASE_912_ID} ${CASE_912_NAME}: ${detail}`)
  }

  try {
    await httpJson("POST", "/stop-session", {})
    await log.write("HTTP POST /stop-session ok")
  } catch (e) {
    await log.write(`stop-session: ${e instanceof Error ? e.message : String(e)}`)
  }

  const report = buildReport(
    "Android",
    UDID,
    PAGE,
    sessionPath,
    [result],
    runMeta.label,
    `半自动 ${CASE_912_ID}（HTTP :${PORT} + 与全自动相同断言）`,
  )
  await writeFile(reportPath, report)

  await log.write(`SESSION END 结果=${result.status === "passed" ? "通过" : "不通过"} 报告=${reportPath}`)
  console.log(
    formatLazyPrefetchRunDialogueSummary({
      platform: "Android",
      runLabel: runMeta.label,
      scopeLabel: `半自动 ${CASE_912_ID}`,
      results: [result],
      reportPath,
      sessionLogPath: sessionPath,
    }),
  )

  if (result.status === "failed") process.exit(1)
}

main().catch((err) => {
  console.error(err)
  process.exit(1)
})
