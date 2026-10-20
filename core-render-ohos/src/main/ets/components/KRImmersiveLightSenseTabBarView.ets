/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of the KuiklyUI license;
 * you may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { KuiklyRenderBaseView } from './base/KRBaseViewExport';
import { ComponentContent, SymbolGlyphModifier } from '@kit.ArkUI';
import { HdsTabs, HdsTabsController, hdsMaterial } from '@kit.UIDesignKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { KRAny, KRValue, KuiklyRenderCallback } from '../utils/KRDataTypes';

/** 诊断日志。过滤关键字：ImmLightTest */
const kLogDomain = 0xA012;
const kLogTag = 'ImmLightTest';

function logI(msg: string): void {
  hilog.info(kLogDomain, kLogTag, '%{public}s', msg);
}

const kPropNameSystemMaterialOhos = 'system-material-ohos';
const kPropItems = 'items';
const kPropSelectedIndex = 'selectedIndex';
const kPropBarInsetX = 'barInsetX';
const kPropBarBottomOffset = 'barBottomOffset';
const kEventTabChanged = 'tabChanged';

const kDefaultInsetX = 16;
const kDefaultBottomOffset = 16;

/** 每页底色：滑动时页签栏"背后"的内容随之变化，便于观察材质取色 / 折射 */
const kPageColors: string[] = [
  '#FFE8F0FE', '#FFF3E5F5', '#FFE8F5E9', '#FFFFF3E0', '#FFEDE7F6'
];

/**
 * 底部页签的图标（SymbolGlyph）。官方 HdsTabs 底部页签必须用
 * `tabBar(new BottomTabBarStyle({normal, selected}, label))` 才会渲染成
 * "图标+文字"的底部页签样式；传裸字符串会被画成顶部下划线页签。
 * 图标名全部取自官方文档示例，保证存在。
 */
const kTabSymbols: Resource[] = [
  $r('sys.symbol.alarm_fill_1'),
  $r('sys.symbol.worldclock_fill_2'),
  $r('sys.symbol.stopwatch_2'),
  $r('sys.symbol.star'),
  $r('sys.symbol.square_and_pencil')
];

/**
 * 鸿蒙「沉浸光感」TabBar —— 基于官方 HDS 组件 HdsTabs 实现。
 *
 * 本文件已回滚至 11:33 可用基线（材质可见、滑动切换正常），后续迭代在此基础上逐项推进。
 * 回滚移除的实验性改动（均有日志证据、详见 devDocs）：
 * - itemIcons 图标映射（功能回退，后续按需重建）
 * - 透明点击层（V2 转发宿主内 onClick 不触发，死路）
 * - 受控 index / setProp updateArkUI（点击切换的尝试，未收口）
 * - materialType ADAPTIVE（设备只支持 IMMERSIVE[101]，ADAPTIVE[100] 静默失效）
 */
@Component
struct ImmersiveLightSenseTabBarInner {
  view?: KRImmersiveLightSenseTabBarView;
  @Prop items: string[] = [];
  @Prop selectedIndex: number = 0;
  @Prop barSideMargin: number = kDefaultInsetX;
  @Prop barBottomMargin: number = kDefaultBottomOffset;
  @Prop materialEnable: boolean = false;
  /**
   * Kuikly 节点的真实尺寸（来自 sizeDidChanged）。
   * **不要用 width('100%')**：V2 转发组件的 ArkTS 根节点解析到的是全屏 wrapper，
   * HdsTabs 会把 bar 画到全屏区域的底部——节点可见区域之外，TabBar 不可见。
   */
  @Prop viewWidth: number = 0;
  @Prop viewHeight: number = 0;

  aboutToAppear(): void {
    let sysTypes = 'unknown';
    try {
      sysTypes = JSON.stringify(hdsMaterial.getSystemMaterialTypes());
    } catch (e) {
      sysTypes = `error: ${e}`;
    }
    logI(`hdstabs aboutToAppear: items=${this.items.length}, selected=${this.selectedIndex}, ` +
      `material=${this.materialEnable}, sysMaterialTypes=${sysTypes}`);
  }

  build() {
    Stack() {
      // 防御：items 是创建后才下发的（aboutToAppear 时为 0）。
      // 带 0 个 TabContent 创建的 HdsTabs 可能进入不可恢复状态，
      // 所以等 items 到达、@Prop 触发重渲染后再创建组件。
      if (this.items.length > 0) {
        HdsTabs({ controller: this.view!.hdsTabsController }) {
        ForEach(this.items, (label: string, idx: number) => {
          // 容器本体只负责承载页签栏与切换，页面内容由业务在 Kuikly 侧自行承载
          TabContent() {
            Column() {
            }
            .width('100%')
            .height('100%')
            .backgroundColor(kPageColors[idx % kPageColors.length])
          }
          .tabBar(new BottomTabBarStyle(
            {
              normal: new SymbolGlyphModifier(kTabSymbols[idx % kTabSymbols.length]),
              selected: new SymbolGlyphModifier(kTabSymbols[idx % kTabSymbols.length])
            },
            label
          ))
          .onAppear(() => {
            logI(`hdstabs TabContent appeared idx=${idx}, label=${label}`);
          })
        }, (label: string, idx: number) => idx.toString())
      }
      .width(this.viewWidth > 0 ? this.viewWidth : '100%')
      .height(this.viewHeight > 0 ? this.viewHeight : '100%')
      // 底部页签三件套，官方示例缺一不可：
      // - barPosition(End)：bar 放到组件底部（默认 Start 是顶部）
      // - barOverlap(true)：悬浮胶囊样式需要
      // - vertical(false)：水平方向页签
      .barPosition(BarPosition.End)
      .barOverlap(true)
      .vertical(false)
      // 官方悬浮样式：左右内缩 + 底部抬离 + 沉浸光感材质，全部一次配齐
      .barFloatingStyle({
        barSideMargin: this.barSideMargin,
        barBottomMargin: this.barBottomMargin,
        systemMaterialEffect: {
          // **必须 IMMERSIVE**：实测设备 getSystemMaterialTypes() 返回 [101]（IMMERSIVE），
          // ADAPTIVE(100) 不在支持列表，设了会静默失效（光感消失且无任何日志）。
          materialType: this.materialEnable ?
          hdsMaterial.MaterialType.IMMERSIVE : hdsMaterial.MaterialType.NONE,
          materialLevel: hdsMaterial.MaterialLevel.ADAPTIVE
        }
      })
        .onChange((index: number) => {
          logI(`hdstabs onChange index=${index}`);
          this.view!.selectTab(index);
        })
        .onAreaChange((_: Area, newArea: Area) => {
          logI(`hdstabs area: ${JSON.stringify(newArea.width)}x${JSON.stringify(newArea.height)}`);
        })
      } else {
        Column() {
        }.width('100%').height('100%')
      }
    }
    .width(this.viewWidth > 0 ? this.viewWidth : '100%')
    .height(this.viewHeight > 0 ? this.viewHeight : '100%')
    .onAreaChange((_: Area, newArea: Area) => {
      logI(`hdstabs root area: ${JSON.stringify(newArea.width)}x${JSON.stringify(newArea.height)}`);
    })
  }
}

@Builder
function createImmersiveLightSenseTabBarView(view: KuiklyRenderBaseView) {
  ImmersiveLightSenseTabBarInner({
    view: view as KRImmersiveLightSenseTabBarView,
    items: (view as KRImmersiveLightSenseTabBarView).items,
    selectedIndex: (view as KRImmersiveLightSenseTabBarView).selectedIndex,
    barSideMargin: (view as KRImmersiveLightSenseTabBarView).barInsetX,
    barBottomMargin: (view as KRImmersiveLightSenseTabBarView).barBottomOffset,
    materialEnable: (view as KRImmersiveLightSenseTabBarView).materialEnable,
    viewWidth: (view as KRImmersiveLightSenseTabBarView).frameWidth,
    viewHeight: (view as KRImmersiveLightSenseTabBarView).frameHeight
  });
}

export class KRImmersiveLightSenseTabBarView extends KuiklyRenderBaseView {
  items: string[] = [];
  selectedIndex: number = 0;
  barInsetX: number = kDefaultInsetX;
  barBottomOffset: number = kDefaultBottomOffset;
  materialEnable: boolean = false;
  /** Kuikly 节点的实际尺寸（sizeDidChanged），用于把 ArkTS 内容钉在节点尺寸内 */
  frameWidth: number = 0;
  frameHeight: number = 0;

  /** HdsTabs 的控制器（继承 TabsController，提供 changeIndex 程序化切换） */
  hdsTabsController: HdsTabsController = new HdsTabsController();

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
      const text = propValue as string;
      this.materialEnable = text !== 'off';
      logI(`hdstabs materialEnable -> ${this.materialEnable}`);
      this.updateArkUI();
      return true;
    }
    const text = propValue as string;
    if (propKey === kPropItems) {
      this.items = text.length > 0 ? text.split(',') : [];
      logI(`hdstabs items -> ${this.items.length} (${text})`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropSelectedIndex) {
      this.selectTab(Number.parseInt(text));
      return true;
    }
    if (propKey === kPropBarInsetX) {
      this.barInsetX = Number.parseFloat(text);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropBarBottomOffset) {
      this.barBottomOffset = Number.parseFloat(text);
      this.updateArkUI();
      return true;
    }
    return false;
  }

  /**
   * 切换页签的唯一入口（点击 / 滑动 / 业务回传都走这里）。
   * 相同索引直接忽略 —— 否则 changeIndex -> onChange -> 事件 -> 回传
   * 会形成回声循环。
   */
  selectTab(index: number): void {
    if (index === this.selectedIndex) {
      return;
    }
    this.selectedIndex = index;
    logI(`hdstabs selectTab -> ${index}`);
    try {
      this.hdsTabsController.changeIndex(index);
    } catch (e) {
      logI(`hdstabs changeIndex error: ${e}`);
    }
    const cb = this.tabChangedCallback;
    if (cb !== null) {
      // 必须是 JSON 字符串：框架走 PagerManager#fireViewEvent -> JSONObject 解析
      const payload: string = JSON.stringify({ index: index });
      try {
        cb(payload as KRAny);
      } catch (e) {
        logI(`hdstabs tabChanged callback error: ${e}`);
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
    const changed = this.frameWidth !== width || this.frameHeight !== height;
    this.frameWidth = width;
    this.frameHeight = height;
    logI(`hdstabs sizeDidChanged: w=${width}, h=${height}`);
    if (changed) {
      // 11:33 基线的关键一环：不 updateArkUI，@Prop viewWidth/viewHeight 永远为 0，
      // HdsTabs 回退到 '100%'（全屏 wrapper），bar 画到节点可见区域之外 = 不可见
      this.updateArkUI();
    }
  }

  didMoveToParentView(): void {
  }

  onDestroy(): void {
  }
}
