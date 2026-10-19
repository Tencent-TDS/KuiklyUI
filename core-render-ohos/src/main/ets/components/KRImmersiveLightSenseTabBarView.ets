/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of the KuiklyUI;
 * you may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { KuiklyRenderBaseView } from './base/KRBaseViewExport';
import { ComponentContent, uiMaterial } from '@kit.ArkUI';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { KRAny, KRValue, KuiklyRenderCallback } from '../utils/KRDataTypes';

const kLogDomain = 0xA012;
const kLogTag = 'ImmLightTest';

function logI(msg: string): void {
  hilog.info(kLogDomain, kLogTag, '%{public}s', msg);
}

const kPropNameSystemMaterialOhos = 'system-material-ohos';
const kPropItems = 'items';
const kPropSelectedIndex = 'selectedIndex';
const kEventTabChanged = 'tabChanged';

function takeMaterialValue(text: string, key: string): string | null {
  const start = text.indexOf(key + '=');
  if (start < 0) {
    return null;
  }
  let end = text.indexOf(',', start);
  if (end < 0) {
    end = text.length;
  }
  return text.substring(start + key.length + 1, end);
}

/** 各页签的底色。滑动切换时页签栏背后就是这些颜色，用于直观观察材质的取色/折射 */
const kPageColors: string[] = [
  '#FFE8F0FE', '#FFF3E5F5', '#FFE8F5E9', '#FFFFF3E0', '#FFEDE7F6'
];

function isMaterialSupported(): boolean {
  try {
    return uiMaterial.isImmersiveMaterialSupported();
  } catch (e) {
    return false;
  }
}

/**
 * 鸿蒙「沉浸光感」TabBar —— **原生 Tabs 组件**。
 *
 * 关键点：这里渲染的是真正的 ArkUI `Tabs`（barPosition = End），页签项是原生的，
 * 沉浸光感由系统画在原生页签栏上。**不承载 Kuikly 自绘子节点** ——
 * 之前那版把自绘视图塞进 tabBar builder，出来的只是几个自绘按钮拼在一起，
 * 既不是原生 TabBar 的观感，也拿不到原生控件的材质。
 *
 * 为什么不用 CAPI 直连（像 Slider / Toggle 那样）：`native_node.h` 的
 * ArkUI_NodeType 里**没有 ARKUI_NODE_TABS**，原生 Tabs 无法用 NDK 节点 API 创建，
 * 只能从 ArkTS 侧渲染。通道虽是 ArkTS，但组件本身仍是原生组件。
 */
@Component
struct ImmersiveLightSenseTabBarInner {
  view?: KRImmersiveLightSenseTabBarView;
  @Prop items: string[] = [];
  @Prop selectedIndex: number = 0;

  /**
   * 单个页签格的样式。鸿蒙的 TabBar 本身就是「Tabs + 每页一个自定义 tabBar builder」，
   * 也就是自绘 —— 所以这里把格的观感做扎实：等宽、图标位 + 文案、选中态变色。
   */
  @Builder
  TabCell(label: string, idx: number) {
    Column() {
      Text(label)
        .fontSize(13)
        .fontColor(this.selectedIndex === idx ? '#FF2A6AF0' : '#991A1A1A')
        .fontWeight(this.selectedIndex === idx ? FontWeight.Medium : FontWeight.Normal)
        .maxLines(1)
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    // 材质要设在**页签格内部**：官方判定 material 只在 "navigation title bar 或 Tabbar"
    // 区域生效，而这里的"Tabbar"指的是页签栏本身，落在 tabBar builder 内的这一层才在 scope 内。
    // 设在 Tabs 根节点上属于栏外，不会显现（这正是之前"一点光感都没有"的原因）。
    .systemMaterial(this.view!.buildMaterial())
  }

  build() {
    // 注意：容器必须占据"内容区 + 页签栏"整块区域，Tabs 才有内容区可供左右滑动切换。
    // 之前只给 72px 贴在底部，Tabs 没有内容区，滑动自然完全没反应。
    // 不要绑定 index：Tabs 的 index 参数只决定**初始**页签，后续程序化切换必须用
    // TabsController.changeIndex()。之前绑定了 index，导致点击/滑动后 prop 回来
    // 也无法改变已创建的 Tabs —— 表现为"始终停在第一个页签、点了没反应"。
    // 这里改为让 Tabs 自己维护选中态（点击与滑动都会自然切换），只把变化通知 Kotlin。
    Tabs({ barPosition: BarPosition.End }) {
      ForEach(this.items, (item: string, idx: number) => {
        TabContent() {
          // 每个页签一个不同底色：滑动切换时页签栏"背后"的内容随之变化，
          // 这是观察沉浸光感（折射 / 模糊取色）最直观的场景
          Column() {
          }
          .width('100%')
          .height('100%')
          .backgroundColor(kPageColors[idx % kPageColors.length])
        }.tabBar(this.TabCell(item, idx))
      }, (item: string, idx: number) => idx.toString())
    }
    .width('100%')
    .height('100%')
    .barMode(BarMode.Fixed)
    .barWidth('100%')
    .onChange((index: number) => {
      this.view!.fireTabChanged(index);
    })
  }
}

@Builder
function createImmersiveLightSenseTabBarView(view: KuiklyRenderBaseView) {
  ImmersiveLightSenseTabBarInner({
    view: view as KRImmersiveLightSenseTabBarView,
    items: (view as KRImmersiveLightSenseTabBarView).items,
    selectedIndex: (view as KRImmersiveLightSenseTabBarView).selectedIndex
  });
}

export class KRImmersiveLightSenseTabBarView extends KuiklyRenderBaseView {
  items: string[] = [];
  selectedIndex: number = 0;

  private materialEnable: boolean = false;
  private materialStyle: uiMaterial.ImmersiveStyle = uiMaterial.ImmersiveStyle.REGULAR;
  private materialInteractive: boolean = false;
  private materialApplyShadow: boolean = true;
  private tabChangedCallback: KuiklyRenderCallback | null = null;

  init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
  }

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    if (propKey === kEventTabChanged && typeof propValue === 'function') {
      this.tabChangedCallback = propValue as KuiklyRenderCallback;
      return true;
    }
    if (propKey === kPropNameSystemMaterialOhos) {
      this.applyMaterialProp(propValue as string);
      this.updateArkUI();
      return true;
    }
    const text = propValue as string;
    if (propKey === kPropItems) {
      this.items = text.length > 0 ? text.split(',') : [];
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropSelectedIndex) {
      this.selectedIndex = Number.parseInt(text);
      this.updateArkUI();
      return true;
    }
    return false;
  }

  private applyMaterialProp(value: string): void {
    if (!value || value === 'off') {
      this.materialEnable = false;
      return;
    }
    const style = takeMaterialValue(value, 'style');
    const interactive = takeMaterialValue(value, 'interactive');
    const applyShadow = takeMaterialValue(value, 'applyShadow');
    this.materialStyle = style !== null ?
    Number.parseInt(style) as uiMaterial.ImmersiveStyle : uiMaterial.ImmersiveStyle.REGULAR;
    if (interactive !== null) {
      this.materialInteractive = interactive === '1';
    }
    if (applyShadow !== null) {
      this.materialApplyShadow = applyShadow === '1';
    }
    this.materialEnable = true;
  }

  /** 未设材质或设备不支持时返回 undefined（页签栏照常渲染，自动降级） */
  buildMaterial(): uiMaterial.ImmersiveMaterial | undefined {
    if (!isMaterialSupported() || !this.materialEnable) {
      return undefined;
    }
    const material = new uiMaterial.ImmersiveMaterial({
      style: this.materialStyle,
      applyShadow: this.materialApplyShadow,
      interactive: this.materialInteractive
    });
    logI(`tabbar material created, style=${this.materialStyle}, items=${this.items.length}`);
    return material;
  }

  fireTabChanged(index: number): void {
    this.selectedIndex = index;
    const cb = this.tabChangedCallback;
    if (cb !== null) {
      // 必须是**JSON 字符串**：框架侧走 PagerManager#fireViewEvent → JSONObject(params)，
      // 传裸值（如 "0"）会抛 JSONException 并未捕获 abort 进程。
      const payload: string = JSON.stringify({ index: index });
      try {
        cb(payload as KRAny);
      } catch (e) {
        logI(`tabbar tabChanged callback error: ${e}`);
      }
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    super.call(method, params, callback);
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createImmersiveLightSenseTabBarView), this);
  }

  sizeDidChanged(width: number, height: number): void {
    logI(`tabbar sizeDidChanged: w=${width}, h=${height}`);
  }

  didMoveToParentView(): void {
  }

  onDestroy(): void {
  }
}
