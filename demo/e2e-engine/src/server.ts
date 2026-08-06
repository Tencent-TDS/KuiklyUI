import http from "node:http"
import type { Selector, Platform } from "./mobile-driver.js"
import { AppiumMobileDriver, type AppiumMobileDriverConfig } from "./appium-mobile-driver.js"
import { filterVisibleTree, renderTreeText, collapseSingleChildren } from "./view-tree.js"

const DEFAULT_PORT = 7900

interface StartSessionBody {
  platform: Platform
  appiumUrl?: string
  bundleId?: string
  appPackage?: string
  appActivity?: string
  deviceName?: string
  udid?: string
  platformVersion?: string
  noRestartApp?: boolean
}

type RequestBody = Record<string, unknown>

function parseSelector(raw: Record<string, unknown>): Selector {
  if (raw.accessibilityId) return { accessibilityId: raw.accessibilityId as string }
  if (raw.id) return { id: raw.id as string }
  if (raw.testTag) return { testTag: raw.testTag as string }
  if (raw.text) return { text: raw.text as string }
  if (raw.xpath) return { xpath: raw.xpath as string }
  throw new Error("Invalid selector: must have accessibilityId, id, testTag, text, or xpath")
}

export class MobileTestServer {
  private server: http.Server | null = null
  private driver: AppiumMobileDriver | null = null

  async start(port: number = DEFAULT_PORT): Promise<void> {
    this.server = http.createServer(async (req, res) => {
      await this.handleRequest(req, res)
    })

    return new Promise((resolve, reject) => {
      this.server!.listen(port, () => {
        console.log(`MobileTest server listening on http://localhost:${port}`)
        resolve()
      })
      this.server!.on("error", reject)
    })
  }

  async stop(): Promise<void> {
    if (this.driver) {
      try { await this.driver.stopSession() } catch {}
      this.driver = null
    }
    if (this.server) {
      await new Promise<void>(resolve => this.server!.close(() => resolve()))
      this.server = null
    }
  }

  private async handleRequest(req: http.IncomingMessage, res: http.ServerResponse) {
    const url = new URL(req.url ?? "/", `http://localhost`)
    const path = url.pathname

    res.setHeader("Content-Type", "application/json")

    try {
      const body = await this.readBody(req)
      const result = await this.route(path, url, body)
      res.writeHead(200)
      res.end(JSON.stringify({ ok: true, ...result }))
    } catch (err) {
      const message = err instanceof Error ? err.message : String(err)
      const status = message.includes("not started") ? 409 : 500
      res.writeHead(status)
      res.end(JSON.stringify({ ok: false, error: message }))
    }
  }

  private async readBody(req: http.IncomingMessage): Promise<RequestBody> {
    if (req.method === "GET") return {}
    const chunks: Buffer[] = []
    for await (const chunk of req) chunks.push(typeof chunk === "string" ? Buffer.from(chunk) : chunk)
    const raw = Buffer.concat(chunks).toString()
    if (!raw) return {}
    try { return JSON.parse(raw) } catch { return {} }
  }

  private async route(path: string, url: URL, body: RequestBody): Promise<Record<string, unknown>> {
    switch (path) {
      case "/status":
        return this.handleStatus()

      case "/start-session":
        return this.handleStartSession(body as unknown as StartSessionBody)

      case "/stop-session":
        return this.handleStopSession()

      case "/view-tree":
        return this.handleViewTree(url)

      case "/page-source":
        return this.handlePageSource()

      case "/tap":
        return this.handleTap(body)

      case "/tap-coordinate":
        return this.handleTapCoordinate(body)

      case "/input":
        return this.handleInput(body)

      case "/scroll":
        return this.handleScroll(body)
      case "/scroll-within":
        return this.handleScrollWithin(body)

      case "/back":
        return this.handleBack()

      case "/wait-for":
        return this.handleWaitFor(body)

      case "/assert-visible":
        return this.handleAssertVisible(body)

      case "/assert-text":
        return this.handleAssertText(body)

      case "/assert-in-viewport":
        return this.handleAssertInViewport(body)

      case "/get-element-rect":
        return this.handleGetElementRect(body)

      case "/screenshot":
        return this.handleScreenshot(body)

      case "/dismiss-alert":
        return this.handleDismissAlert()

      case "/restart-app":
        return this.handleRestartApp()

      default:
        throw new Error(`Unknown endpoint: ${path}`)
    }
  }

  private requireDriver(): AppiumMobileDriver {
    if (!this.driver) throw new Error("Session not started. Call /start-session first.")
    return this.driver
  }

  private handleStatus(): Record<string, unknown> {
    return {
      sessionActive: this.driver !== null,
      platform: this.driver ? this.driver.config.platform : null,
    }
  }

  private async handleStartSession(body: StartSessionBody): Promise<Record<string, unknown>> {
    if (this.driver) {
      console.log("[start-session] Existing session found. Stopping it first...")
      try { await this.driver.stopSession() } catch {}
      this.driver = null
    }

    if (!body.platform) throw new Error("Missing required field: platform (ios|android)")

    const config: AppiumMobileDriverConfig = {
      platform: body.platform,
      appiumUrl: body.appiumUrl ?? "http://127.0.0.1:4723",
      bundleId: body.bundleId,
      appPackage: body.appPackage,
      appActivity: body.appActivity,
      deviceName: body.deviceName,
      udid: body.udid,
      platformVersion: body.platformVersion,
    }

    console.log(`[start-session] Creating Appium session (${body.platform})...`)
    this.driver = new AppiumMobileDriver(config)
    await this.driver.startSession()
    if (body.noRestartApp) {
      console.log("[start-session] Skipping restartApp (noRestartApp=true).")
    } else {
      console.log("[start-session] Appium session created. Restarting app...")
      await this.driver.restartApp()
      console.log("[start-session] App restarted. Waiting for stability...")
      await new Promise(r => setTimeout(r, 500))
    }
    try { await this.driver.dismissAlert() } catch {}
    console.log("[start-session] Session ready.")
    return { platform: body.platform }
  }

  private async handleStopSession(): Promise<Record<string, unknown>> {
    this.requireDriver()
    console.log("[stop-session] Stopping session...")
    await this.driver!.stopSession()
    this.driver = null
    console.log("[stop-session] Session stopped.")
    return {}
  }

  private async handleViewTree(url: URL): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    const viewTree = await driver.getViewTree()
    const visibleOnly = url.searchParams.get("visible") === "true"

    let treeToRender = visibleOnly ? filterVisibleTree(viewTree.tree) : viewTree.tree
    if (!treeToRender) throw new Error("No visible elements found")

    treeToRender = collapseSingleChildren(treeToRender)

    const text = renderTreeText(treeToRender)

    return {
      platform: viewTree.platform,
      viewport: viewTree.viewport,
      visibleOnly,
      text,
    }
  }

  private async handlePageSource(): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    const source = await driver.getPageSource()
    return { source }
  }

  private async handleTap(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (!body.selector || typeof body.selector !== "object") {
      throw new Error("Missing required field: selector")
    }
    const selector = parseSelector(body.selector as Record<string, unknown>)
    const postTap = body.postTap as Record<string, unknown> | undefined
    if (postTap && typeof postTap === "object") {
      await driver.tap(selector, { postTap: postTap as never })
    } else {
      await driver.tap(selector)
    }
    return {}
  }

  private async handleTapCoordinate(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (typeof body.x !== "number" || typeof body.y !== "number") {
      throw new Error("Missing required fields: x, y (numbers)")
    }
    await driver.tapCoordinate(body.x, body.y)
    return {}
  }

  private async handleInput(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (!body.selector || typeof body.selector !== "object") {
      throw new Error("Missing required field: selector")
    }
    if (typeof body.text !== "string") {
      throw new Error("Missing required field: text")
    }
    await driver.input(parseSelector(body.selector as Record<string, unknown>), body.text)
    return {}
  }

  private async handleScroll(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (typeof body.startX !== "number" || typeof body.startY !== "number" ||
        typeof body.endX !== "number" || typeof body.endY !== "number") {
      throw new Error("Missing required fields: startX, startY, endX, endY (numbers)")
    }
    // area: 可选 { x, y, width, height }，仅在 fling=true && android 时被 mobile:swipeGesture 使用。
    let area: { x: number; y: number; width: number; height: number } | undefined
    if (body.area && typeof body.area === "object") {
      const a = body.area as Record<string, unknown>
      if (
        typeof a.x === "number" &&
        typeof a.y === "number" &&
        typeof a.width === "number" &&
        typeof a.height === "number"
      ) {
        area = { x: a.x, y: a.y, width: a.width, height: a.height }
      }
    }
    await driver.scroll({
      startX: body.startX,
      startY: body.startY,
      endX: body.endX,
      endY: body.endY,
      durationMs: typeof body.durationMs === "number" ? body.durationMs : undefined,
      fling: typeof body.fling === "boolean" ? body.fling : undefined,
      area,
    })
    return {}
  }

  private async handleScrollWithin(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (!body.selector || typeof body.selector !== "object") {
      throw new Error("Missing required field: selector (target container, e.g. {testTag: 'lazy_list'})")
    }
    if (typeof body.direction !== "string" ||
        !["up", "down", "left", "right"].includes(body.direction)) {
      throw new Error("Missing/invalid required field: direction ∈ {up,down,left,right}")
    }
    await driver.scrollWithin(
      parseSelector(body.selector as Record<string, unknown>),
      {
        direction: body.direction as "up" | "down" | "left" | "right",
        times: typeof body.times === "number" ? body.times : undefined,
        fling: typeof body.fling === "boolean" ? body.fling : undefined,
        durationMs: typeof body.durationMs === "number" ? body.durationMs : undefined,
        marginPercent: typeof body.marginPercent === "number" ? body.marginPercent : undefined,
        settleMs: typeof body.settleMs === "number" ? body.settleMs : undefined,
      },
    )
    return {}
  }

  private async handleBack(): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    await driver.back()
    return {}
  }

  private async handleWaitFor(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (!body.selector || typeof body.selector !== "object") {
      throw new Error("Missing required field: selector")
    }
    if (typeof body.timeoutMs !== "number") {
      throw new Error("Missing required field: timeoutMs")
    }
    await driver.waitFor(parseSelector(body.selector as Record<string, unknown>), body.timeoutMs)
    return {}
  }

  private async handleAssertVisible(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (!body.selector || typeof body.selector !== "object") {
      throw new Error("Missing required field: selector")
    }
    await driver.assertVisible(parseSelector(body.selector as Record<string, unknown>))
    return {}
  }

  private async handleAssertText(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (typeof body.text !== "string") {
      throw new Error("Missing required field: text")
    }
    await driver.assertText(body.text)
    return {}
  }

  private async handleAssertInViewport(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (!body.selector || typeof body.selector !== "object") {
      throw new Error("Missing required field: selector")
    }
    await driver.assertInViewport(parseSelector(body.selector as Record<string, unknown>))
    return {}
  }

  private async handleGetElementRect(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (!body.selector || typeof body.selector !== "object") {
      throw new Error("Missing required field: selector")
    }
    const rect = await driver.getElementRect(parseSelector(body.selector as Record<string, unknown>))
    return { rect }
  }

  private async handleScreenshot(body: RequestBody): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    if (typeof body.outputPath !== "string") {
      throw new Error("Missing required field: outputPath")
    }
    await driver.takeScreenshot(body.outputPath)
    return { outputPath: body.outputPath }
  }

  private async handleDismissAlert(): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    await driver.dismissAlert()
    return {}
  }

  private async handleRestartApp(): Promise<Record<string, unknown>> {
    const driver = this.requireDriver()
    console.log("[restart-app] Restarting app...")
    await driver.restartApp()
    console.log("[restart-app] App restarted.")
    return {}
  }
}

if (process.argv[1]?.endsWith("server.ts") || process.argv[1]?.endsWith("server.js")) {
  const port = parseInt(process.env.MOBILE_TEST_PORT ?? "7900", 10)
  const server = new MobileTestServer()
  server.start(port).catch(e => {
    console.error("Failed to start server:", e)
    process.exit(1)
  })

  process.on("SIGINT", async () => {
    await server.stop()
    process.exit(0)
  })
}
