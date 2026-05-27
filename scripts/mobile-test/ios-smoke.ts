import { AppiumMobileDriver } from "../../.claude/skills/kuikly-mobile-test/src/appium-mobile-driver.js"
import { EvidenceCollector, formatReportMarkdown } from "../../.claude/skills/kuikly-mobile-test/src/evidence.js"
import { REPORTS_DIR } from "../../.claude/skills/kuikly-mobile-test/src/paths.js"
import { snapshotToJson } from "../../.claude/skills/kuikly-mobile-test/src/snapshot-normalizer.js"
import { mkdir, writeFile } from "fs/promises"

const APPIUM_URL = process.env.APPIUM_URL ?? "http://127.0.0.1:4723"
const BUNDLE_ID = process.env.IOS_BUNDLE_ID ?? "com.tencent.kuiklycore.demo.luoyibu"
const DEVICE_NAME = process.env.IOS_DEVICE_NAME ?? "iPhone 17 Pro"
const PLATFORM_VERSION = process.env.IOS_PLATFORM_VERSION ?? "26.3"

async function main() {
  await mkdir(REPORTS_DIR, { recursive: true })

  const evidence = new EvidenceCollector("ios")

  const driver = new AppiumMobileDriver({
    platform: "ios",
    appiumUrl: APPIUM_URL,
    bundleId: BUNDLE_ID,
    deviceName: DEVICE_NAME,
    platformVersion: PLATFORM_VERSION,
  })

  try {
    evidence.recordStep("start_session", "running")
    await driver.startSession()
    evidence.recordStep("start_session", "passed")

    evidence.recordStep("get_snapshot", "running")
    const snapshot = await driver.getSnapshot()
    evidence.recordStep("get_snapshot", "passed")

    const snapshotPath = `${REPORTS_DIR}/ios_snapshot_${Date.now()}.json`
    await writeFile(snapshotPath, snapshotToJson(snapshot))
    evidence.setSnapshotPath(snapshotPath)

    console.log(`Snapshot: ${snapshot.elements.length} elements found`)
    console.log("First 5 elements:")
    for (const el of snapshot.elements.slice(0, 5)) {
      console.log(`  - testTag=${el.testTag ?? "-"} text=${el.text ?? "-"} type=${el.type ?? "-"}`)
    }

    if (snapshot.elements.length > 0) {
      const targetEl = snapshot.elements.find((e) => e.clickable && e.text)
      if (targetEl) {
        evidence.recordStep(`tap_${targetEl.text}`, "running")
        await driver.tap({ text: targetEl.text! })
        evidence.recordStep(`tap_${targetEl.text}`, "passed")

        await new Promise((r) => setTimeout(r, 2000))

        const afterSnapshot = await driver.getSnapshot()
        console.log(`After tap: ${afterSnapshot.elements.length} elements`)
      } else {
        evidence.recordStep("tap_any_clickable", "skipped", "no clickable element with text found")
      }
    }

    evidence.recordStep("assert_app_loaded", "running")
    await driver.assertText("Kuikly")
    evidence.recordStep("assert_app_loaded", "passed")

    await driver.takeScreenshot(`${REPORTS_DIR}/ios_screenshot_${Date.now()}.png`)

    const bundle = evidence.toBundle("passed")
    const report = formatReportMarkdown(bundle)
    const reportPath = `${REPORTS_DIR}/ios_smoke_${Date.now()}.md`
    await writeFile(reportPath, report)
    console.log(`\nReport: ${reportPath}`)

  } catch (err) {
    const errorMsg = err instanceof Error ? err.message : String(err)
    console.error(`Error: ${errorMsg}`)

    evidence.recordStep("error", "failed", errorMsg)

    try {
      await driver.takeScreenshot(`${REPORTS_DIR}/ios_failure_${Date.now()}.png`)
    } catch {}

    const bundle = evidence.toBundle("failed")
    const report = formatReportMarkdown(bundle)
    const reportPath = `${REPORTS_DIR}/ios_smoke_${Date.now()}.md`
    await writeFile(reportPath, report)
    console.log(`\nFailed report: ${reportPath}`)

  } finally {
    try {
      await driver.stopSession()
    } catch {}
  }
}

main().catch(console.error)
