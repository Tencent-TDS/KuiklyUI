import { MobileTestServer } from "../../.claude/skills/kuikly-mobile-test/src/server.js"

const sleep = (ms: number) => new Promise<void>(r => setTimeout(r, ms))

const IOS_UDID = process.env.IOS_UDID ?? "2A4904F7-DDBA-4581-BCC9-3D8ABADDFA30"
const PORT = Number(process.env.MOBILE_TEST_PORT ?? "7901")

async function main() {
  const server = new MobileTestServer()
  await server.start(PORT)
  console.log(`Server started on port ${PORT}`)

  const baseUrl = `http://localhost:${PORT}`

  const post = async (path: string, body?: Record<string, unknown>) => {
    const res = await fetch(`${baseUrl}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: body ? JSON.stringify(body) : undefined,
    })
    return res.json()
  }

  const get = async (path: string) => {
    const res = await fetch(`${baseUrl}${path}`)
    return res.json()
  }

  interface TestResult {
    name: string
    passed: boolean
    detail?: string
  }

  const results: TestResult[] = []
  const run = async (name: string, fn: () => Promise<void>) => {
    try {
      await fn()
      results.push({ name, passed: true })
      console.log(`  ✅ ${name}`)
    } catch (e) {
      results.push({ name, passed: false, detail: e instanceof Error ? e.message : String(e) })
      console.log(`  ❌ ${name} - ${e instanceof Error ? e.message : String(e)}`)
    }
  }

  await run("GET /status (no session)", async () => {
    const r = await get("/status")
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
    if (r.sessionActive !== false) throw new Error(`Expected sessionActive=false`)
  })

  await run("POST /start-session (ios)", async () => {
    const r = await post("/start-session", {
      platform: "ios",
      udid: IOS_UDID,
      bundleId: "com.tencent.kuiklycore.demo.luoyibu",
      deviceName: "iPhone 17 Pro",
      platformVersion: "26.3",
    })
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
    if (r.platform !== "ios") throw new Error(`Expected platform=ios`)
  })

  await run("POST /restart-app (clean state)", async () => {
    const r = await post("/restart-app")
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
  })

  await post("/dismiss-alert")

  await run("GET /status (active session)", async () => {
    const r = await get("/status")
    if (r.sessionActive !== true) throw new Error(`Expected sessionActive=true`)
    if (r.platform !== "ios") throw new Error(`Expected platform=ios`)
  })

  await run("GET /view-tree?visible=true", async () => {
    const r = await get("/view-tree?visible=true")
    if (r.ok !== true) throw new Error(`Expected ok=true`)
    if (typeof r.text !== "string" || r.text.length === 0) throw new Error(`Missing view-tree text`)
    console.log(`     ${r.text.split("\n").length} lines`)
  })

  await run("POST /assert-text", async () => {
    const r = await post("/assert-text", { text: "跳转2" })
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
  })

  await run("POST /input + /tap (navigate)", async () => {
    await post("/restart-app")
    await sleep(2500)
    await post("/dismiss-alert")
    await sleep(500)
    await post("/input", {
      selector: { xpath: "//XCUIElementTypeTextField" },
      text: "root_demo",
    })
    await sleep(800)
    await post("/tap", { selector: { text: "跳转2" } })
    await sleep(2500)
    const r = await get("/view-tree?visible=true")
    if (!r.text?.includes('testTag="root_btn_')) throw new Error("RootDemoPage not reached")
  })

  await run("POST /assert-visible", async () => {
    const r = await post("/assert-visible", { selector: { accessibilityId: "root_btn_KTV页面路由" } })
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
  })

  await run("POST /get-element-rect", async () => {
    const r = await post("/get-element-rect", { selector: { accessibilityId: "root_btn_KTV页面路由" } })
    if (r.ok !== true) throw new Error(`Expected ok=true`)
    if (!r.rect || r.rect.width <= 0) throw new Error(`Invalid rect: ${JSON.stringify(r.rect)}`)
    console.log(`     rect: x=${r.rect.x} y=${r.rect.y} w=${r.rect.width} h=${r.rect.height}`)
  })

  await run("POST /assert-in-viewport", async () => {
    const r = await post("/assert-in-viewport", { selector: { accessibilityId: "root_btn_KTV页面路由" } })
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
  })

  await run("POST /screenshot", async () => {
    const r = await post("/screenshot", { outputPath: "/tmp/mobile-test-server-e2e.png" })
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
  })

  await run("POST /restart-app", async () => {
    const r = await post("/restart-app")
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
  })

  await run("POST /dismiss-alert", async () => {
    const r = await post("/dismiss-alert")
    console.log(`     ok=${r.ok} (expected, may not have alert)`)
  })

  await run("POST /stop-session", async () => {
    const r = await post("/stop-session")
    if (r.ok !== true) throw new Error(`Expected ok=true, got ${JSON.stringify(r)}`)
  })

  await run("GET /status (after stop)", async () => {
    const r = await get("/status")
    if (r.sessionActive !== false) throw new Error(`Expected sessionActive=false`)
  })

  await run("POST /tap (no session → 409)", async () => {
    const res = await fetch(`${baseUrl}/tap`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ selector: { text: "x" } }),
    })
    if (res.status !== 409) throw new Error(`Expected 409, got ${res.status}`)
    const r = await res.json()
    if (r.ok !== false) throw new Error(`Expected ok=false`)
  })

  await server.stop()

  console.log("\n" + "=".repeat(50))
  console.log("Server E2E Test Results")
  console.log("=".repeat(50))
  const passed = results.filter(r => r.passed).length
  const failed = results.filter(r => !r.passed).length
  console.log(`  Total: ${results.length} | Passed: ${passed} | Failed: ${failed}`)
  process.exit(failed > 0 ? 1 : 0)
}

main().catch(e => { console.error(e); process.exit(1) })
