/**
 * One-off: Compose Card swipe pop debug.
 * Run: npx tsx scenarios/debug/debug-ios-compose-swipe.ts  （在 skill 目录下）
 */
import { AppiumMobileDriver } from "../../src/appium-mobile-driver.js"

const sleep = (ms: number) => new Promise<void>(r => setTimeout(r, ms))

async function main() {
  const driver = new AppiumMobileDriver({
    platform: "ios",
    appiumUrl: "http://127.0.0.1:4723",
    bundleId: "com.tencent.kuiklycore.demo.luoyibu",
    deviceName: "iPhone 17 Pro",
    platformVersion: "26.3",
    udid: "2A4904F7-DDBA-4581-BCC9-3D8ABADDFA30",
  })
  try {
    await driver.startSession()
    await driver.restartApp()
    await sleep(2000)
    await driver.input({ xpath: "//XCUIElementTypeTextField" }, "ComposeAllSample")
    await sleep(500)
    await driver.tap({ text: "跳转2" })
    await sleep(2500)

    const before = await driver.getSnapshot()
    const cards = before.elements.filter(e => e.testTag?.startsWith("demo_card_"))
    console.log("ComposeAllSample cards:", cards.length, cards.slice(0, 2).map(e => e.testTag))

    await driver.tap({ testTag: "demo_card_TextDemo" })
    await sleep(1500)
    const onSub = !(await driver.getSnapshot()).elements.some(e => e.testTag?.startsWith("demo_card_"))
    console.log("entered subpage:", onSub)

    await driver.scroll({ startX: 5, startY: 400, endX: 320, endY: 400, durationMs: 400 })
    await sleep(1500)

    let alive = true
    let afterCards = 0
    try {
      afterCards = (await driver.getSnapshot()).elements.filter(e => e.testTag?.startsWith("demo_card_")).length
    } catch {
      alive = false
    }
    console.log(JSON.stringify({ composeSwipePop: { onSub, backToList: afterCards > 0, appAlive: alive, cardCount: afterCards } }))
  } finally {
    await driver.stopSession()
  }
}

main().catch(e => { console.error(e); process.exit(1) })
