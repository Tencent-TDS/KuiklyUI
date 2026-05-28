/** Shared run control, log naming, cleanup, and agent dialogue template for LazyList prefetch E2E. */

import { readdir, unlink } from "node:fs/promises"
import { join } from "node:path"
import type { TestResult } from "./lazy-prefetch-run-cases.js"

/** Case id → prerequisite cases (also executed when using LAZY_PREFETCH_ONLY). */
export const LAZY_PREFETCH_CASE_DEPENDENCIES: Record<string, string[]> = {
  "9.4": ["9.3"],
  "9.7": ["9.3"],
  "9.10": ["9.3", "9.4"],
}

export interface LazyPrefetchRunMeta {
  stamp: string
  label: string
}

export function formatLazyPrefetchRunStamp(date = new Date()): string {
  const pad = (n: number) => String(n).padStart(2, "0")
  return `${pad(date.getMonth() + 1)}${pad(date.getDate())}_${pad(date.getHours())}${pad(date.getMinutes())}${pad(date.getSeconds())}`
}

/** e.g. 0527_165033 → 5月27日 16:50:33 */
export function formatLazyPrefetchRunLabel(stamp: string): string {
  const m = stamp.match(/^(\d{2})(\d{2})_(\d{2})(\d{2})(\d{2})$/)
  if (!m) return stamp
  return `${Number(m[1])}月${Number(m[2])}日 ${m[3]}:${m[4]}:${m[5]}`
}

export function createLazyPrefetchRunMeta(date = new Date()): LazyPrefetchRunMeta {
  const stamp = formatLazyPrefetchRunStamp(date)
  return { stamp, label: formatLazyPrefetchRunLabel(stamp) }
}

/** LAZY_PREFETCH_ONLY=9.11 or 9.10,9.11 — empty = full suite. */
export function parseOnlyCases(envValue = process.env.LAZY_PREFETCH_ONLY): string[] | null {
  const raw = envValue?.trim()
  if (!raw) return null
  return raw.split(/[,;\s]+/).map((s) => s.trim()).filter(Boolean)
}

export function expandCaseSelection(requested: string[]): Set<string> {
  const set = new Set<string>()
  const visit = (id: string) => {
    if (set.has(id)) return
    set.add(id)
    for (const dep of LAZY_PREFETCH_CASE_DEPENDENCIES[id] ?? []) visit(dep)
  }
  for (const id of requested) visit(id)
  return set
}

export function resolveCasesToRun(only: string[] | null | undefined): Set<string> | null {
  if (only === undefined) {
    const fromEnv = parseOnlyCases()
    return fromEnv ? expandCaseSelection(fromEnv) : null
  }
  if (only === null) return null
  return expandCaseSelection(only)
}

export function shouldRunCase(id: string, only: Set<string> | null): boolean {
  if (!only) return true
  if (id === "setup") return true
  return only.has(id)
}

export function formatScopeLabel(only: Set<string> | null): string {
  if (!only) return "全套 9.3–9.13"
  const ids = [...only].filter((id) => id !== "setup").sort()
  return `仅 ${ids.join("、")}（含依赖）`
}

const IOS_LOG_PREFIXES = [
  "lazy_prefetch_ios_native_",
  "mobile_test_session_ios_prefetch_",
  "kuikly_ios_prefetch_console",
]

const ANDROID_LOG_PREFIXES = [
  "lazy_prefetch_e2e_",
  "mobile_test_session_android_prefetch_",
]

/** Remove previous LazyList prefetch artifacts for the same platform before a new run. */
export async function cleanupLazyPrefetchLogs(
  platform: "ios" | "android",
  logsDir: string,
): Promise<string[]> {
  const prefixes = platform === "ios" ? IOS_LOG_PREFIXES : ANDROID_LOG_PREFIXES
  const removed: string[] = []
  let files: string[] = []
  try {
    files = await readdir(logsDir)
  } catch {
    return removed
  }
  for (const name of files) {
    if (!prefixes.some((p) => name.startsWith(p))) continue
    await unlink(join(logsDir, name)).catch(() => {})
    removed.push(join(logsDir, name))
  }
  return removed
}

/** Fixed dialogue block for the agent to paste after an E2E run (see kuikly-mobile-test SKILL §E2E 执行规范). */
export function formatMobileTestRunDialogueSummary(opts: {
  platform: "iOS" | "Android"
  scenario: string
  runLabel: string
  scopeLabel: string
  results: Array<{ id: string; name: string; status: string; detail?: string }>
  reportPath: string
  sessionLogPath: string
  extraLogPaths?: Array<{ label: string; path: string }>
}): string {
  const passed = opts.results.filter((r) => r.status === "passed")
  const failed = opts.results.filter((r) => r.status === "failed")
  const skipped = opts.results.filter((r) => r.status === "skipped")

  const lines = opts.results.map((r) => {
    const icon = r.status === "passed" ? "✅" : r.status === "skipped" ? "⏭️" : "❌"
    const suffix = r.detail ? `：${r.detail}` : ""
    return `- ${icon} **${r.id}** ${r.name}${suffix}`
  })

  return [
    "## 测试完成",
    "",
    `- **平台**：${opts.platform}`,
    `- **场景**：${opts.scenario}`,
    `- **运行时间**：${opts.runLabel}`,
    `- **范围**：${opts.scopeLabel}`,
    "",
    "### 结果",
    `- 通过 **${passed.length}**，失败 **${failed.length}**，跳过 **${skipped.length}**`,
    "",
    ...lines,
    "",
    "### 日志路径",
    `- 报告：\`${opts.reportPath}\``,
    `- Session：\`${opts.sessionLogPath}\``,
    ...(opts.extraLogPaths?.map((e) => `- ${e.label}：\`${e.path}\``) ?? []),
  ].join("\n")
}

/** @deprecated Use formatMobileTestRunDialogueSummary; kept for prefetch scripts. */
export function formatLazyPrefetchRunDialogueSummary(opts: {
  platform: "iOS" | "Android"
  runLabel: string
  scopeLabel: string
  results: TestResult[]
  reportPath: string
  sessionLogPath: string
  consoleLogPath?: string
}): string {
  return formatMobileTestRunDialogueSummary({
    platform: opts.platform,
    scenario: "LazyListPrefetchDemo",
    runLabel: opts.runLabel,
    scopeLabel: opts.scopeLabel,
    results: opts.results,
    reportPath: opts.reportPath,
    sessionLogPath: opts.sessionLogPath,
    extraLogPaths: opts.consoleLogPath
      ? [{ label: "Console", path: opts.consoleLogPath }]
      : undefined,
  })
}
