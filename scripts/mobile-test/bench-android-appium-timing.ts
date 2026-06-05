/**
 * 真机 Appium 耗时探测（不跑 E2E 用例）。
 *
 *   KUIKLY_REPO_ROOT=... ANDROID_UDID=15490798770018J \
 *   npx tsx scripts/mobile-test/bench-android-appium-timing.ts
 */

import { execSync } from "node:child_process"
import { AppiumMobileDriver } from "../../.claude/skills/kuikly-mobile-test/src/appium-mobile-driver.js"

const UDID = process.env.ANDROID_UDID ?? "15490798770018J"
const APP_PACKAGE = process.env.ANDROID_APP_PACKAGE ?? "com.tencent.kuikly.android.demo"
const APP_ACTIVITY =
  process.env.ANDROID_APP_ACTIVITY ?? "com.tencent.kuikly.android.demo.KuiklyRenderActivity"
const PAGE_NAME = "LazyListPrefetchDemo"

function adb(args: string) {
  execSync(`adb -s ${UDID} ${args}`, { stdio: "pipe" })
}

function ms(start: number) {
  return `${Date.now() - start}ms`
}

async function timed<T>(label: string, fn: () => Promise<T>): Promise<T> {
  const t0 = Date.now()
  try {
    const result = await fn()
    console.log(`✅ ${label}: ${ms(t0)}`)
    return result
  } catch (e) {
    console.log(`❌ ${label}: ${ms(t0)} — ${e instanceof Error ? e.message : String(e)}`)
    throw e
  }
}

async function main() {
  console.log(`device=${UDID} page=${PAGE_NAME}\n`)

  await timed("adb am start (pageName intent)", async () => {
    adb(
      `shell am start -n ${APP_PACKAGE}/${APP_ACTIVITY} --es pageName ${PAGE_NAME}`,
    )
  })

  const driver = new AppiumMobileDriver({
    platform: "android",
    appiumUrl: process.env.APPIUM_URL ?? "http://127.0.0.1:4723",
    appPackage: APP_PACKAGE,
    appActivity: APP_ACTIVITY,
    androidPageName: PAGE_NAME,
    deviceName: UDID,
    udid: UDID,
  })

  try {
    await timed("startSession", () => driver.startSession())

    await timed("elementExists prefetch_toggle (miss on wrong page)", async () =>
      driver.elementExists({ testTag: "prefetch_toggle" }),
    )

    await timed("waitFor prefetch_toggle 5s", async () =>
      driver.waitFor({ testTag: "prefetch_toggle" }, 5000),
    )

    await timed("getSelectorLabel prefetch_toggle", async () =>
      driver.getSelectorLabel({ testTag: "prefetch_toggle" }),
    )

    if (process.env.BENCH_INCLUDE_PAGE_SOURCE === "1") {
      await timed("getPageSource (full dump)", async () => {
        const src = await driver.getPageSource()
        return src.length
      }).then((len) => console.log(`   pageSource chars=${len}`))

      await timed("getViewTree (parse full dump)", () => driver.getViewTree())
    } else {
      console.log("⏭️  skip getPageSource/getViewTree (set BENCH_INCLUDE_PAGE_SOURCE=1 to measure)")
    }
  } finally {
    await driver.stopSession().catch(() => {})
  }
}

main().catch(() => process.exit(1))
