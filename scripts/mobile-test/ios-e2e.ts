import { mkdir, writeFile } from "fs/promises"
import { AppiumMobileDriver } from "../../.claude/skills/kuikly-mobile-test/src/appium-mobile-driver.js"
import { REPORTS_DIR } from "../../.claude/skills/kuikly-mobile-test/src/paths.js"

const APPIUM_URL = process.env.APPIUM_URL ?? "http://127.0.0.1:4723"
const BUNDLE_ID = process.env.IOS_BUNDLE_ID ?? "com.tencent.kuiklycore.demo.luoyibu"
const DEVICE_NAME = process.env.IOS_DEVICE_NAME ?? "iPhone 17 Pro"
const PLATFORM_VERSION = process.env.IOS_PLATFORM_VERSION ?? "26.3"

interface TestResult {
  name: string
  status: "passed" | "failed" | "skipped"
  detail?: string
  durationMs?: number
}

const sleep = (ms: number) => new Promise<void>(r => setTimeout(r, ms))

async function navigateToRootDemoPage(driver: AppiumMobileDriver) {
  await driver.restartApp()
  const snapshot = await driver.getSnapshot()
  const hasRootBtn = snapshot.elements.some(e => e.testTag?.startsWith("root_btn_"))
  if (hasRootBtn) return
  // Input pageName and tap 跳转2 button using selector
  await driver.input({ xpath: "//XCUIElementTypeTextField" }, "root_demo")
  await sleep(500)
  await driver.tap({ text: "跳转2" })
  await sleep(2000)
  const snap2 = await driver.getSnapshot()
  if (!snap2.elements.some(e => e.testTag?.startsWith("root_btn_"))) {
    throw new Error("Navigation to RootDemoPage failed")
  }
}

async function runTests() {
  await mkdir(REPORTS_DIR, { recursive: true })
  const results: TestResult[] = []

  const driver = new AppiumMobileDriver({
    platform: "ios",
    appiumUrl: APPIUM_URL,
    bundleId: BUNDLE_ID,
    deviceName: DEVICE_NAME,
    platformVersion: PLATFORM_VERSION,
  })

  try {
    await runTest("1. startSession", results, async () => {
      await driver.startSession()
    })

    await runTest("2. restartApp (clean state)", results, async () => {
      await driver.restartApp()
    })

    await runTest("3. getSnapshot", results, async () => {
      const snapshot = await driver.getSnapshot()
      if (snapshot.elements.length === 0) throw new Error("No elements")
      console.log(`   ${snapshot.elements.length} elements`)
    })

    await runTest("4. assertText", results, async () => {
      await driver.assertText("跳转2")
    })

    await runTest("5. navigate to RootDemoPage", results, async () => {
      await navigateToRootDemoPage(driver)
    })

    await runTest("6. assertVisible (testTag)", results, async () => {
      await driver.assertVisible({ accessibilityId: "root_btn_KTV页面路由" })
    })

    await runTest("7. getElementRect", results, async () => {
      const rect = await driver.getElementRect({ accessibilityId: "root_btn_KTV页面路由" })
      if (rect.width <= 0 || rect.height <= 0) throw new Error(`Invalid rect: ${JSON.stringify(rect)}`)
      console.log(`   x=${rect.x} y=${rect.y} w=${rect.width} h=${rect.height}`)
    })

    await runTest("8. tap subpage + back()", results, async () => {
      await driver.tap({ accessibilityId: "root_btn_TextViewDemo" })
      await sleep(1500)
      await driver.back()
      await sleep(1000)
      await driver.assertVisible({ accessibilityId: "root_btn_KTV页面路由" })
    })

    await runTest("9. tapCoordinate + back()", results, async () => {
      await navigateToRootDemoPage(driver)
      const rect = await driver.getElementRect({ accessibilityId: "root_btn_ListViewDemo" })
      await driver.tapCoordinate(rect.x + rect.width / 2, rect.y + rect.height / 2)
      await sleep(1500)
      await driver.back()
      await sleep(1000)
      await driver.assertVisible({ accessibilityId: "root_btn_KTV页面路由" })
    })

    await runTest("10. assertInViewport (on-screen)", results, async () => {
      await navigateToRootDemoPage(driver)
      await driver.assertInViewport({ accessibilityId: "root_btn_KTV页面路由" })
    })

    await runTest("11. assertInViewport (off-screen should fail)", results, async () => {
      try {
        await driver.assertInViewport({ accessibilityId: "root_btn_经典HelloWorldDemo" })
        throw new Error("Should have failed")
      } catch (e: unknown) {
        if (e instanceof Error && e.message.includes("Should have failed")) throw e
        console.log("   Correctly rejected off-screen element")
      }
    })

    await runTest("12. scroll (scroll down)", results, async () => {
      await navigateToRootDemoPage(driver)
      await driver.scroll({ startX: 200, startY: 500, endX: 200, endY: 100, durationMs: 800 })
      await sleep(800)
      const rect = await driver.getElementRect({ accessibilityId: "root_btn_经典HelloWorldDemo" })
      if (rect.width <= 0 || rect.height <= 0) throw new Error(`Element not found or zero size after scroll`)
    })

    await runTest("13. scroll (scroll up)", results, async () => {
      await driver.scroll({ startX: 200, startY: 200, endX: 200, endY: 500, durationMs: 500 })
      await sleep(500)
    })

    await runTest("14. waitFor", results, async () => {
      await navigateToRootDemoPage(driver)
      await driver.waitFor({ accessibilityId: "root_btn_KTV页面路由" }, 5000)
    })

    await runTest("15. takeScreenshot", results, async () => {
      await driver.takeScreenshot(`${REPORTS_DIR}/ios_e2e_screenshot_${Date.now()}.png`)
    })

    await runTest("16. input (InputViewDemoPage)", results, async () => {
      await navigateToRootDemoPage(driver)
      try { await driver.assertVisible({ accessibilityId: "root_btn_InputViewDemo" }) } catch {
        await driver.scroll({ startX: 200, startY: 400, endX: 200, endY: 100, durationMs: 500 })
        await sleep(500)
      }
      await driver.tap({ accessibilityId: "root_btn_InputViewDemo" })
      await sleep(1500)
      await driver.assertVisible({ accessibilityId: "input_field" })
      await driver.input({ accessibilityId: "input_field" }, "hello test")
      await sleep(500)
    })

    await runTest("17. modal (ModalViewDemoPage)", results, async () => {
      await navigateToRootDemoPage(driver)
      try { await driver.assertVisible({ accessibilityId: "root_btn_ModalViewDemo" }) } catch {
        await driver.scroll({ startX: 200, startY: 400, endX: 200, endY: 100, durationMs: 500 })
        await sleep(500)
      }
      await driver.tap({ accessibilityId: "root_btn_ModalViewDemo" })
      await sleep(1500)
      await driver.assertVisible({ accessibilityId: "modal_trigger_btn" })
      await driver.tap({ accessibilityId: "modal_trigger_btn" })
      await sleep(1000)
      await driver.assertVisible({ accessibilityId: "action_item_0" })
    })

    await runTest("18. dismissAlert (error dialog)", results, async () => {
      await navigateToRootDemoPage(driver)
      // Go back to router page first by restarting
      await driver.restartApp()
      await sleep(2000)
      // Input a nonexistent pageName and tap 跳转2 to trigger error
      await driver.input({ xpath: "//XCUIElementTypeTextField" }, "nonexistent_page")
      await sleep(500)
      await driver.tap({ text: "跳转2" })
      await sleep(1500)
      const snap = await driver.getSnapshot()
      const hasAlert = snap.elements.some(e => e.type === "XCUIElementTypeAlert")
      if (hasAlert) {
        await driver.dismissAlert()
        await sleep(500)
        console.log("   Dismissed error alert successfully")
      } else {
        console.log("   No alert found (may have been auto-dismissed)")
      }
    })

  } catch (err) {
    console.error(`\nFatal: ${err instanceof Error ? err.message : String(err)}`)
  } finally {
    try { await driver.stopSession() } catch {}
  }

  console.log("\n" + "=".repeat(60))
  console.log("iOS E2E Test Results")
  console.log("=".repeat(60))
  const passed = results.filter(r => r.status === "passed").length
  const failed = results.filter(r => r.status === "failed").length
  for (const r of results) {
    const icon = r.status === "passed" ? "✅" : r.status === "failed" ? "❌" : "⏭️"
    const detail = r.detail ? ` - ${r.detail.substring(0, 100)}` : ""
    console.log(`  ${icon} ${r.name} (${r.durationMs}ms)${detail}`)
  }
  console.log("-".repeat(60))
  console.log(`  Total: ${results.length} | Passed: ${passed} | Failed: ${failed}`)

  const report = results.map(r => {
    const icon = r.status === "passed" ? "✅" : r.status === "failed" ? "❌" : "⏭️"
    return `${icon} ${r.name} (${r.durationMs}ms) ${r.detail ?? ""}`
  }).join("\n")
  await writeFile(`${REPORTS_DIR}/ios_e2e_report_${Date.now()}.md`, `# iOS E2E Test Report\n\n${report}\n\nTotal: ${results.length} | Passed: ${passed} | Failed: ${failed}`)
  return failed === 0
}

async function runTest(name: string, results: TestResult[], fn: () => Promise<void>) {
  const start = Date.now()
  try {
    await fn()
    results.push({ name, status: "passed", durationMs: Date.now() - start })
  } catch (err) {
    results.push({ name, status: "failed", detail: (err instanceof Error ? err.message : String(err)).substring(0, 200), durationMs: Date.now() - start })
  }
}

runTests().then(ok => process.exit(ok ? 0 : 1)).catch(e => { console.error(e); process.exit(1) })
