/**
 * 最小复现：9.12 setup 阶段 UiAutomator2 instrumentation crash + logcat 抓取。
 *
 *   cd .claude/skills/kuikly-mobile-test
 *   KUIKLY_REPO_ROOT=$PWD ANDROID_UDID=<udid> npm run repro:uia2-crash
 */
import { execSync, spawn } from "node:child_process"
import { mkdir, writeFile } from "node:fs/promises"
import { join } from "node:path"
import { AppiumMobileDriver } from "../../.claude/skills/kuikly-mobile-test/src/appium-mobile-driver.js"
import { LOGS_DIR } from "../../.claude/skills/kuikly-mobile-test/src/paths.js"

const UDID = process.env.ANDROID_UDID ?? "emulator-5554"
const APP_PACKAGE = process.env.ANDROID_APP_PACKAGE ?? "com.tencent.kuikly.android.demo"
const APP_ACTIVITY =
  process.env.ANDROID_APP_ACTIVITY ?? "com.tencent.kuikly.android.demo.KuiklyRenderActivity"
const PAGE = "LazyListPrefetchDemo"

const sleep = (ms: number) => new Promise<void>((r) => setTimeout(r, ms))

function adb(args: string) {
  return execSync(`adb -s ${UDID} ${args}`, { encoding: "utf8" })
}

function stamp() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, "0")
  return `${p(d.getMonth() + 1)}${p(d.getDate())}_${p(d.getHours())}${p(d.getMinutes())}${p(d.getSeconds())}`
}

async function tapTag(driver: AppiumMobileDriver, tag: string, label: string) {
  console.log(`[repro] tap ${label} (${tag}) …`)
  const t0 = Date.now()
  try {
    await driver.tap({ testTag: tag })
    console.log(`[repro] tap ${label} OK ${Date.now() - t0}ms`)
  } catch (e) {
    console.error(`[repro] tap ${label} FAIL ${Date.now() - t0}ms:`, e instanceof Error ? e.message : e)
    throw e
  }
  if (tag === "reset_counts") {
    const ms = Number(process.env.ANDROID_RESET_COUNTS_SETTLE_MS ?? 3500)
    if (ms > 0) await sleep(ms)
  } else if (tag === "scenario_modifier_opt_in") {
    await sleep(1200)
  } else {
    await sleep(700)
  }
}

async function findOnly(driver: AppiumMobileDriver, tag: string, label: string) {
  console.log(`[repro] findElements ${label} (${tag}) …`)
  const t0 = Date.now()
  try {
    const ok = await driver.elementExists({ testTag: tag })
    console.log(`[repro] find ${label} exists=${ok} ${Date.now() - t0}ms`)
    return ok
  } catch (e) {
    console.error(`[repro] find ${label} FAIL ${Date.now() - t0}ms:`, e instanceof Error ? e.message : e)
    throw e
  }
}

function extractCrashLines(logcat: string): string {
  const lines = logcat.split("\n")
  const hits: string[] = []
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]
    if (
      /FATAL EXCEPTION|Process: io\.appium\.uiautomator2|AndroidRuntime|INSTRUMENTATION|uiautomator2\.server/i.test(
        line,
      )
    ) {
      const start = Math.max(0, i - 2)
      const end = Math.min(lines.length, i + 25)
      hits.push(lines.slice(start, end).join("\n"))
      hits.push("---")
      i = end
    }
  }
  return hits.length ? hits.join("\n") : "(no FATAL / uia2 markers in logcat dump)"
}

async function main() {
  await mkdir(LOGS_DIR, { recursive: true })
  const runId = stamp()
  const logPath = join(LOGS_DIR, `uia2_crash_logcat_${runId}.txt`)
  const summaryPath = join(LOGS_DIR, `uia2_crash_summary_${runId}.md`)

  adb("logcat -c")
  const logcatProc = spawn(
    "adb",
    ["-s", UDID, "logcat", "-v", "threadtime", "AndroidRuntime:E", "*:S"],
    { stdio: ["ignore", "pipe", "pipe"] },
  )
  let logcatBuf = ""
  logcatProc.stdout?.on("data", (c: Buffer) => {
    logcatBuf += c.toString()
  })
  logcatProc.stderr?.on("data", (c: Buffer) => {
    logcatBuf += c.toString()
  })

  const driver = new AppiumMobileDriver({
    platform: "android",
    appiumUrl: process.env.APPIUM_URL ?? "http://127.0.0.1:4723",
    appPackage: APP_PACKAGE,
    appActivity: APP_ACTIVITY,
    androidPageName: PAGE,
    deviceName: UDID,
    udid: UDID,
  })

  async function openDemo() {
    adb(`shell am start -n ${APP_PACKAGE}/${APP_ACTIVITY} --es pageName ${PAGE}`)
    await sleep(1500)
    await driver.startSession()
    await driver.waitFor({ testTag: "prefetch_toggle" }, 12000)
  }

  const steps: { step: string; ok: boolean; error?: string }[] = []

  async function step(name: string, fn: () => Promise<void>) {
    try {
      await fn()
      steps.push({ step: name, ok: true })
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e)
      steps.push({ step: name, ok: false, error: msg })
      throw e
    }
  }

  try {
    await step("openDemo", () => openDemo())
    await step("tap scenario_modifier_opt_in", () => tapTag(driver, "scenario_modifier_opt_in", "scenario"))
    await step("tap reset_counts", () => tapTag(driver, "reset_counts", "reset"))
    await step("tap prefetch_toggle (ensure on)", async () => {
      const on = await findOnly(driver, "prefetch_toggle", "prefetch_toggle")
      if (!on) await tapTag(driver, "prefetch_toggle", "prefetch_toggle")
      else console.log("[repro] prefetch already visible, skip extra tap")
    })
    await step("find clear_metrics_only", () => findOnly(driver, "clear_metrics_only", "clear_metrics"))
    await step("tap clear_metrics_only", () => tapTag(driver, "clear_metrics_only", "clear_metrics"))
  } catch {
    // keep logcat for diagnosis
  } finally {
    try {
      await driver.stopSession()
    } catch {
      /* ignore */
    }
    logcatProc.kill("SIGTERM")
    await sleep(500)
    const dump = adb("logcat -d -v threadtime")
    const full = `${logcatBuf}\n\n=== logcat -d ===\n${dump}`
    await writeFile(logPath, full, "utf8")
    const excerpt = extractCrashLines(full)
    const md = [
      `# UiAutomator2 crash repro (${runId})`,
      "",
      `- Device: ${UDID}`,
      `- Page: ${PAGE}`,
      "",
      "## Steps",
      "",
      ...steps.map((s) => `- ${s.ok ? "✅" : "❌"} ${s.step}${s.error ? `: ${s.error}` : ""}`),
      "",
      "## Logcat excerpt (FATAL / uia2)",
      "",
      "```",
      excerpt,
      "```",
      "",
      `Full log: ${logPath}`,
    ].join("\n")
    await writeFile(summaryPath, md, "utf8")
    console.log(`[repro] wrote ${summaryPath}`)
    console.log(`[repro] wrote ${logPath}`)
    console.log("\n--- excerpt ---\n", excerpt)
  }

  const failed = steps.some((s) => !s.ok)
  process.exit(failed ? 1 : 0)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
