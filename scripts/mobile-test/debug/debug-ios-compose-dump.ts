/**
 * One-off: dump Compose navigation view-tree.
 * Run: npx tsx scenarios/debug/debug-ios-compose-dump.ts  （在 skill 目录下）
 */
import { writeFileSync, mkdirSync } from "fs"
import { AppiumMobileDriver } from "../../src/appium-mobile-driver.js"
import { renderTreeText } from "../../src/view-tree.js"

const sleep = (ms: number) => new Promise<void>(r => setTimeout(r, ms))

async function main() {
  mkdirSync("logs", { recursive: true })
  const driver = new AppiumMobileDriver({
    platform: "ios",
    appiumUrl: "http://127.0.0.1:4723",
    bundleId: "com.tencent.kuiklycore.demo.luoyibu",
    deviceName: "iPhone 17 Pro",
    platformVersion: "26.3",
    udid: "2A4904F7-DDBA-4581-BCC9-3D8ABADDFA30",
  })
  await driver.startSession()
  await driver.restartApp()
  await sleep(2000)
  await driver.input({ xpath: "//XCUIElementTypeTextField" }, "ComposeAllSample")
  await sleep(500)
  await driver.tap({ text: "跳转2" })
  await sleep(2500)
  const tree = await driver.getViewTree()
  writeFileSync("logs/compose_nav_visible.txt", renderTreeText(tree.tree, tree.viewport))
  const snap = await driver.getSnapshot()
  console.log(snap.elements.filter(e => e.testTag || e.id).slice(0, 20))
  await driver.stopSession()
}

main().catch(e => { console.error(e); process.exit(1) })
