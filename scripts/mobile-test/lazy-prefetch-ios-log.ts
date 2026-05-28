import { execSync, type ChildProcess, spawn } from "node:child_process"
import { appendFileSync, readFileSync, writeFileSync } from "node:fs"
import { parsePrefetchDemoLines, type LogMetrics } from "./lazy-prefetch-metrics.js"

/**
 * Captures Kotlin/Native println from iOS Simulator.
 * K/N writes to stdout, not os_log — `log stream` misses it; use `simctl launch --console-pty`.
 */
export class IosConsoleLog {
  private lineOffset = 0
  private consoleProc: ChildProcess | null = null

  constructor(
    private readonly simUdid: string,
    readonly logPath: string,
  ) {}

  start(): void {
    writeFileSync(this.logPath, "")
  }

  /** Terminate and relaunch app; stdout/stderr append to logPath until stop(). */
  relaunchApp(bundleId: string): void {
    this.stopConsoleProc()
    try {
      execSync(`xcrun simctl terminate ${this.simUdid} ${bundleId}`, { stdio: "ignore" })
    } catch {
      // app may not be running
    }
    this.consoleProc = spawn(
      "xcrun",
      ["simctl", "launch", "--console-pty", this.simUdid, bundleId],
      { stdio: ["ignore", "pipe", "pipe"] },
    )
    const append = (buf: Buffer) => appendFileSync(this.logPath, buf)
    this.consoleProc.stdout?.on("data", append)
    this.consoleProc.stderr?.on("data", append)
    this.consoleProc.on("exit", () => {
      this.consoleProc = null
    })
  }

  stop(): void {
    this.stopConsoleProc()
  }

  private stopConsoleProc(): void {
    this.consoleProc?.kill("SIGTERM")
    this.consoleProc = null
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
