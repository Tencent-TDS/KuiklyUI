/**
 * One-off: test iOS left-edge swipe pop vs nav_back back().
 * Run: npx tsx scenarios/debug/debug-ios-swipe-back.ts  （在 skill 目录下）
 */
import { mkdir, writeFile } from "fs/promises"
import { AppiumMobileDriver } from "../../src/appium-mobile-driver.js"
import { REPORTS_DIR } from "../../src/paths.js"

const APPIUM_URL = process.env.APPIUM_URL ?? "http://127.0.0.1:4723"
const BUNDLE_ID = process.env.IOS_BUNDLE_ID ?? "com.tencent.kuiklycore.demo.luoyibu"
const DEVICE_NAME = process.env.IOS_DEVICE_NAME ?? "iPhone 17 Pro"
const PLATFORM_VERSION = process.env.IOS_PLATFORM_VERSION ?? "26.3"
const UDID = process.env.IOS_UDID ?? "2A4904F7-DDBA-4581-BCC9-3D8ABADDFA30"

const sleep = (ms: number) => new Promise<void>(r => setTimeout(r, ms))

async function navigateToRootDemoPage(driver: AppiumMobileDriver) {
  await driver.restartApp()
  await sleep(2000)
  const snapshot = await driver.getSnapshot()
  if (snapshot.elements.some(e => e.testTag?.startsWith("root_btn_"))) return
  await driver.input({ xpath: "//XCUIElementTypeTextField" }, "root_demo")
  await sleep(500)
  await driver.tap({ text: "跳转2" })
  await sleep(2000)
}

async function isOnRootDemoPage(driver: AppiumMobileDriver): Promise<boolean> {
  const snap = await driver.getSnapshot()
  return snap.elements.some(e => e.testTag?.startsWith("root_btn_"))
}

async function navigateToComposeAllSample(driver: AppiumMobileDriver) {
  await driver.restartApp()
  await sleep(2000)
  const snap = await driver.getSnapshot()
  if (snap.elements.some(e => e.testTag?.startsWith("demo_card_"))) return
  await driver.input({ xpath: "//XCUIElementTypeTextField" }, "ComposeAllSample")
  await sleep(500)
  await driver.tap({ text: "跳转2" })
  await sleep(2500)
}

async function isOnComposeAllSample(driver: AppiumMobileDriver): Promise<boolean> {
  const snap = await driver.getSnapshot()
  return snap.elements.some(e => e.testTag?.startsWith("demo_card_"))
}

async function openSelfDslSubPage(driver: AppiumMobileDriver) {
  await driver.tap({ accessibilityId: "root_btn_TextViewDemo" })
  await sleep(1500)
}

async function openComposeSubPage(driver: AppiumMobileDriver) {
  await driver.tap({ accessibilityId: "demo_card_TextDemo" })
  await sleep(1500)
}

/** Simulate iOS interactive pop: drag from left edge toward center */
async function swipePopFromLeftEdge(driver: AppiumMobileDriver) {
  // iPhone 17 Pro viewport ~402x874 in prior logs; start at left edge
  await driver.scroll({ startX: 5, startY: 400, endX: 320, endY: 400, durationMs: 400 })
}

async function tryMobileBack(driver: AppiumMobileDriver): Promise<{ ok: boolean; error?: string }> {
  try {
    const wd = (driver as unknown as { driver: { back: () => Promise<void> } | null }).driver
    if (!wd) return { ok: false, error: "no driver" }
    await wd.back()
    return { ok: true }
  } catch (e) {
    return { ok: false, error: e instanceof Error ? e.message : String(e) }
  }
}

interface StepResult {
  name: string
  passed: boolean
  detail: string
}

async function main() {
  await mkdir(REPORTS_DIR, { recursive: true })
  const results: StepResult[] = []

  const driver = new AppiumMobileDriver({
    platform: "ios",
    appiumUrl: APPIUM_URL,
    bundleId: BUNDLE_ID,
    deviceName: DEVICE_NAME,
    platformVersion: PLATFORM_VERSION,
    udid: UDID,
  })

  try {
    console.log("Starting session...")
    await driver.startSession()
    await navigateToRootDemoPage(driver)
    results.push({
      name: "baseline RootDemoPage",
      passed: await isOnRootDemoPage(driver),
      detail: "root_btn_* visible",
    })

    // --- Self DSL: TextViewDemo swipe ---
    console.log("\n=== Test 1: Self DSL swipe pop (TextViewDemo) ===")
    await navigateToRootDemoPage(driver)
    await openSelfDslSubPage(driver)
    const onSubBeforeSwipe = !(await isOnRootDemoPage(driver))
    await swipePopFromLeftEdge(driver)
    await sleep(1500)
    let appAlive = true
    try {
      await driver.getSnapshot()
    } catch {
      appAlive = false
    }
    const onRootAfterSwipe = await isOnRootDemoPage(driver)
    results.push({
      name: "Self DSL swipe pop (TextViewDemo)",
      passed: onSubBeforeSwipe && onRootAfterSwipe && appAlive,
      detail: `subpage=${onSubBeforeSwipe}, backToRoot=${onRootAfterSwipe}, appAlive=${appAlive}`,
    })
    console.log(results.at(-1))

    // --- Compose: TextDemo swipe ---
    console.log("\n=== Test 2: Compose swipe pop (TextDemo) ===")
    await navigateToComposeAllSample(driver)
    if (!(await isOnComposeAllSample(driver))) {
      results.push({ name: "Compose swipe pop (TextDemo)", passed: false, detail: "failed to reach ComposeAllSample" })
    } else {
      await openComposeSubPage(driver)
      const onComposeSub = !(await isOnComposeAllSample(driver))
      await swipePopFromLeftEdge(driver)
      await sleep(1500)
      let composeAlive = true
      try { await driver.getSnapshot() } catch { composeAlive = false }
      const backToComposeList = await isOnComposeAllSample(driver)
      results.push({
        name: "Compose swipe pop (TextDemo)",
        passed: onComposeSub && backToComposeList && composeAlive,
        detail: `subpage=${onComposeSub}, backToList=${backToComposeList}, appAlive=${composeAlive}`,
      })
    }
    console.log(results.at(-1))

    // --- ViewDemo crash repro (for stack capture reference) ---
    console.log("\n=== Test 3: ViewDemo open (known crash repro) ===")
    await navigateToRootDemoPage(driver)
    await driver.tap({ accessibilityId: "root_btn_ViewDemo" })
    await sleep(2000)
    let viewDemoAlive = true
    try { await driver.getSnapshot() } catch { viewDemoAlive = false }
    results.push({
      name: "ViewDemo open",
      passed: viewDemoAlive,
      detail: viewDemoAlive ? "unexpectedly alive" : "crashed (ViewDemoPage.created -> BridgeModule.currentTimeStamp)",
    })
    console.log(results.at(-1))

    // --- nav_back (needs rebuilt app with nav_back testTag) ---
    console.log("\n=== Test 4: nav_back via back() ===")
    if (viewDemoAlive) {
      await navigateToRootDemoPage(driver)
      await openSelfDslSubPage(driver)
      try {
        await driver.back()
        await sleep(1500)
        results.push({
          name: "nav_back back()",
          passed: await isOnRootDemoPage(driver),
          detail: `backToRoot=${await isOnRootDemoPage(driver)}`,
        })
      } catch (e) {
        results.push({
          name: "nav_back back()",
          passed: false,
          detail: e instanceof Error ? e.message : String(e),
        })
      }
      console.log(results.at(-1))
    } else {
      await driver.restartApp()
      await sleep(2000)
      results.push({ name: "nav_back back()", passed: false, detail: "skipped after ViewDemo crash" })
    }

    // --- driver.back() on iOS (Appium system back) ---
    console.log("\n=== Test 5: driver.back() on iOS ===")
    await navigateToRootDemoPage(driver)
    await openSelfDslSubPage(driver)
    const mobileBack = await tryMobileBack(driver)
    await sleep(1500)
    let aliveAfterMobileBack = true
    try { await driver.getSnapshot() } catch { aliveAfterMobileBack = false }
    const onRootAfterMobileBack = aliveAfterMobileBack && (await isOnRootDemoPage(driver))
    results.push({
      name: "driver.back() iOS",
      passed: mobileBack.ok && aliveAfterMobileBack,
      detail: `execute=${mobileBack.ok}${mobileBack.error ? ` err=${mobileBack.error}` : ""}, appAlive=${aliveAfterMobileBack}, onRoot=${onRootAfterMobileBack}`,
    })
    console.log(results.at(-1))

  } finally {
    try { await driver.stopSession() } catch {}
  }

  const report = results.map(r => `- [${r.passed ? "PASS" : "FAIL"}] ${r.name}: ${r.detail}`).join("\n")
  const out = `# iOS swipe-back test\n\n${report}\n`
  const path = `${REPORTS_DIR}/ios_swipe_back_test_${Date.now()}.md`
  await writeFile(path, out)
  console.log(`\nReport: ${path}\n${out}`)
}

main().catch(err => {
  console.error(err)
  process.exit(1)
})
