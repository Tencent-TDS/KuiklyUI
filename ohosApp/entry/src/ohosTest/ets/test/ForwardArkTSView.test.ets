import { describe, beforeAll, it, expect } from '@ohos/hypium';
import { abilityDelegatorRegistry } from '@kit.TestKit';
import { Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { Driver, ON, Component } from '@ohos.UiTest';

const TAG = 'ForwardArkTSViewTest';
const PAGE_NAME = 'CustomViewExamplePage';
const ROUTER_TITLE = 'Kuikly页面路由';
const CUSTOM_TITLE = 'CustomViewExamplePage';
const TAP_ME = 'Tap Me';
const MY_VIEW_TEXT = 'hello this is My view';
const KUIKLY_CHILD_TEXT = 'Static text element from kuikly';
const JUMP_TEXTS = ['跳转', '跳转2'];
const LOOP_COUNT = 5;

async function waitForText(driver: Driver, text: string, timeoutMs: number = 12000): Promise<Component> {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const component = await driver.findComponent(ON.text(text));
    if (component) {
      return component;
    }
    await driver.delayMs(400);
  }
  throw new Error(`timeout waiting for text: ${text}`);
}

async function clickFirstAvailable(driver: Driver, texts: string[]): Promise<void> {
  for (const text of texts) {
    const component = await driver.findComponent(ON.text(text));
    if (component) {
      await component.click();
      return;
    }
  }
  throw new Error(`none of texts found: ${texts.join(',')}`);
}

async function openCustomViewPage(driver: Driver): Promise<void> {
  await waitForText(driver, ROUTER_TITLE, 15000);
  const inputs = await driver.findComponents(ON.type('TextInput'));
  expect(inputs.length > 0).assertTrue();
  await inputs[0].inputText(PAGE_NAME);
  await driver.delayMs(400);
  await clickFirstAvailable(driver, JUMP_TEXTS);
  await waitForText(driver, CUSTOM_TITLE, 15000);
}

export default function forwardArkTSViewTest() {
  describe('ForwardArkTSViewHandleScopeTest', () => {
    beforeAll(async () => {
      const delegator = abilityDelegatorRegistry.getAbilityDelegator();
      const args = abilityDelegatorRegistry.getArguments();
      const want: Want = {
        bundleName: args.bundleName,
        abilityName: 'EntryAbility',
        moduleName: 'entry'
      };
      hilog.info(0x0000, TAG, 'start EntryAbility for %{public}s', args.bundleName);
      await delegator.startAbility(want);
      const driver = Driver.create();
      await driver.delayMs(2500);
    })

    it('create_destroy_forward_arkts_view_loop', 0, async (done: Function) => {
      const driver = Driver.create();
      try {
        for (let i = 0; i < LOOP_COUNT; i++) {
          hilog.info(0x0000, TAG, 'loop %{public}d open CustomViewExamplePage', i);
          await openCustomViewPage(driver);

          const myView = await waitForText(driver, MY_VIEW_TEXT);
          expect(myView !== undefined).assertTrue();
          const kuiklyChild = await waitForText(driver, KUIKLY_CHILD_TEXT);
          expect(kuiklyChild !== undefined).assertTrue();
          const tapMe = await waitForText(driver, TAP_ME);
          await tapMe.click();
          await driver.delayMs(500);
          const updated = await waitForText(driver, '1');
          expect(updated !== undefined).assertTrue();

          await driver.pressBack();
          await waitForText(driver, ROUTER_TITLE, 12000);
        }
        hilog.info(0x0000, TAG, 'PASS create/destroy %{public}d times', LOOP_COUNT);
        done();
      } catch (e) {
        const message = (e as Error).message ?? String(e);
        hilog.error(0x0000, TAG, 'FAIL %{public}s', message);
        expect(message).assertEqual('');
        done();
      }
    })
  })
}
