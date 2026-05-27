import { type ChildProcess, spawn } from "node:child_process"
import { appendFileSync, readFileSync, writeFileSync } from "node:fs"
import { parsePrefetchDemoLines, type LogMetrics } from "./lazy-prefetch-metrics.js"

/** Captures iOS Simulator unified log for LazyList prefetch println tags. */
export class IosConsoleLog {
  private lineOffset = 0
  private proc: ChildProcess | null = null

  constructor(
    private readonly simUdid: string,
    readonly logPath: string,
  ) {}

  start(): void {
    writeFileSync(this.logPath, "")
    this.proc = spawn(
      "xcrun",
      [
        "simctl",
        "spawn",
        this.simUdid,
        "log",
        "stream",
        "--style",
        "compact",
        "--level",
        "debug",
        "--predicate",
        'eventMessage CONTAINS "LazyListPrefetch"',
      ],
      { stdio: ["ignore", "pipe", "pipe"] },
    )
    const append = (buf: Buffer) => appendFileSync(this.logPath, buf)
    this.proc.stdout?.on("data", append)
    this.proc.stderr?.on("data", append)
  }

  stop(): void {
    this.proc?.kill("SIGTERM")
    this.proc = null
  }

  clear(): void {
    this.lineOffset = this.allLines().length
  }

  private allLines(): string[] {
    try {
      return readFileSync(this.logPath, "utf8").split("\n")
    } catch {
      return []
    }
  }

  private sinceOffset(): string[] {
    return this.allLines().slice(this.lineOffset)
  }

  readDemoLines(): string[] {
    return this.sinceOffset().filter((l) => l.includes("LazyListPrefetchDemo"))
  }

  readTraceLines(): string[] {
    return this.sinceOffset().filter((l) => l.includes("LazyListPrefetchTrace"))
  }

  readDemoMetrics(): LogMetrics {
    return parsePrefetchDemoLines(this.readDemoLines())
  }
}
