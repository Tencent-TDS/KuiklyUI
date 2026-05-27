import { execSync } from "node:child_process"
import { writeFileSync } from "node:fs"
import { AppiumMobileDriver } from "../../.claude/skills/kuikly-mobile-test/src/appium-mobile-driver.js"
import { LOGS_DIR } from "../../.claude/skills/kuikly-mobile-test/src/paths.js"

async function main() {
  const driver = new AppiumMobileDriver({
    platform: "android",
    appiumUrl: "http://127.0.0.1:4723",
    appPackage: "com.tencent.kuikly.android.demo",
    appActivity: "com.tencent.kuikly.android.demo.KuiklyRenderActivity",
    udid: "emulator-5554",
    deviceName: "emulator-5554",
  })
  await driver.startSession()
  execSync(
    "adb -s emulator-5554 shell am start -n com.tencent.kuikly.android.demo/.KuiklyRenderActivity --es pageName LazyListPrefetchDemo",
  )
  await new Promise((r) => setTimeout(r, 8000))
  const snap = await driver.getSnapshot()
  writeFileSync(
    `${LOGS_DIR}/debug_snapshot.json`,
    JSON.stringify(snap.elements, null, 2),
  )
  const tree = await driver.getViewTree()
  writeFileSync(`${LOGS_DIR}/debug_viewtree.txt`, tree.text)
  console.log("element count", snap.elements.length)
  for (const el of snap.elements.slice(0, 30)) {
    console.log({ tag: el.testTag, text: el.text, type: el.type })
  }
  await driver.stopSession()
}

main().catch(console.error)
