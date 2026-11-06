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

import { KuiklyRenderBaseView } from '../base/KRBaseViewExport';
import { ComponentContent, uiMaterial } from '@kit.ArkUI';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { KRAny, KRValue, KuiklyRenderCallback } from '../../utils/KRDataTypes';

/** 诊断日志。过滤关键字：ImmLightTest */
const kLogDomain = 0xA012;
const kLogTag = 'ImmLightTest';

function logI(msg: string): void {
  hilog.info(kLogDomain, kLogTag, '%{public}s', msg);
}

/** Kotlin 侧下发的属性键 */
const kPropItems = 'items';
const kPropSelectedIndex = 'selectedIndex';
const kPropPillRadius = 'pillRadius';
const kPropPillHeight = 'pillHeight';
const kPropPillBackgroundColor = 'pillBackgroundColor';
const kPropNameSystemMaterialOhos = 'system-material-ohos';
const kEventTabChanged = 'tabChanged';

/** 页签图标（SymbolGlyph），与 HdsTabs 版同源 —— 两版并排对照时观感一致 */
const kTabSymbols: Resource[] = [
  $r('sys.symbol.alarm_fill_1'),
  $r('sys.symbol.worldclock_fill_2'),
  $r('sys.symbol.stopwatch_2'),
  $r('sys.symbol.star'),
  $r('sys.symbol.square_and_pencil')
];

/**
 * 单个自绘格子（独立 @Component）：图标 + 文字 + 胶囊圆角 + 材质。
 * 独立成 struct 而不是 @Builder：① builder 体内不允许普通语句（如日志），
 * ② struct 的 aboutToAppear 可作为「格子真正创建」的诊断锚点。
 */
@Component
struct CustomTabCell {
  view?: KRImmersiveTabBarSelfDrawnView;
  @Prop label: string = '';
  @Prop idx: number = 0;
  @Prop selectedIndex: number = 0;
  @Prop materialEnable: boolean = false;
  @Prop materialStyle: number = 2;

  aboutToAppear(): void {
    // builder/格子创建的诊断锚点：一条没有 = tabBar builder 根本没执行
    logI(`customTabbar cell builder idx=${this.idx}, label=${this.label}`);
  }

  build() {
    // 格子自身不带圆角/底色：整体 bar 的外形（圆角/底板）由 Tabs 之外的底板层负责
    Column({ space: 2 }) {
      SymbolGlyph(kTabSymbols[this.idx % kTabSymbols.length])
        .fontSize(22)
        .fontColor([this.selectedIndex === this.idx ? '#FF0A59F7' : '#99000000'])
      Text(this.label)
        .fontSize(this.selectedIndex === this.idx ? 13 : 12)
        .fontWeight(this.selectedIndex === this.idx ? FontWeight.Medium : FontWeight.Normal)
        .fontColor(this.selectedIndex === this.idx ? '#FF0A59F7' : '#99000000')
        .maxLines(1)
    }
    .justifyContent(FlexAlign.Center)
    .width('100%')
    .height('100%')
    // 选中态不做背景色块（原生 HdsTabs 同款：仅图标/文字变色反馈）——
    // 之前的 '#1A0A59F7' 方形色块是与原生观感差异的最大来源
    .backgroundColor(Color.Transparent)
    // 切换动画近似：选中态图标字号变化平滑过渡（HdsTabs 的跨格滑动指示器
    // 是 HDS 闭源实现，Tabs 的 tabBar 槽位布局不归我们管，跨格滑动做不了）
    .animation({ duration: 250, curve: Curve.EaseOut })
    .padding({ left: 8, right: 8 })
    // 点击切换（双保险之一：Tabs 原生点击切换如果被转发层拦截，这里兜底）
    .onClick(() => {
      logI(`customTabbar cell onClick idx=${this.idx}`);
      this.view?.selectTab(this.idx);
    })
    .onAppear(() => {
      logI(`customTabbar cell appeared idx=${this.idx}, selected=${this.selectedIndex === this.idx}`);
    })
    // ── bar 上 pan 手势切换页签（与玻璃版同款语义）──
    // HdsTabs 版能"在页签栏上滑动切换"是系统组件内部的闭源行为；自绘格子的
    // bar 区域手势归自己的节点管，官方不会代劳，因此自绘 PanGesture：
    // 松手时按 位移 ÷ 槽宽（格子宽 = 组件宽 ÷ item 数）四舍五入决定切几页。
    // tap（onClick）与 pan 由系统手势仲裁自动区分：拖动判 pan，轻点判 tap。
    .gesture(
      PanGesture({ direction: PanDirection.Horizontal })
        .onActionEnd((event: GestureEvent) => {
          const itemsCount: number = this.view?.items.length ?? 1;
          const width: number = this.view?.frameWidth ?? 360;
          const slotWidth: number = itemsCount > 0 ? width / itemsCount : 90;
          const movedSlots: number = Math.round(event.offsetX / slotWidth);
          if (movedSlots !== 0) {
            logI(`customTabbar pan offset=${event.offsetX.toFixed(1)} ` +
              `slotWidth=${slotWidth.toFixed(1)} -> movedSlots=${movedSlots}`);
            this.view?.selectTab(this.idx + movedSlots);
          }
        })
    )
  }
}

/**
 * 「自绘 TabBar」验证组件（Inner）—— 与 HdsTabs 版并存的实验路线。
 *
 * 验证命题：**原生 Tabs（barPosition End）的 tabBar scope 内，自绘格子 +
 * systemMaterial 能否出材质**。若成立，胶囊外形（圆角/高度/背景）完全由业务控制，
 * 消除 HdsTabs 版"圆角不可控"的官方限制。
 *
 * 与 HdsTabs 版的差异：
 * - 格子是 ArkTS 自绘节点（普通节点，参与命中测试 —— HdsTabs 胶囊由系统材质层
 *   渲染、点击命中不可达，本版无需 Kuikly 透明点击条）；
 * - 圆角/高度业务可传（pillRadius/pillHeight）；
 * - 无 HDS 内置选中动画（指示效果用选中态背景表达）。
 *
 * 已知教训全部规避（详见各处注释）：
 * - V2 转发：显式尺寸（不用 '100%'）、Stack 固定根、0 items 防御、
 *   selectedIndex 受控（prop 变化必须 updateArkUI）；
 * - 材质：关闭用 Material.empty（不能 undefined，否则组件不上屏）。
 */
@Component
struct ImmersiveTabBarSelfDrawnInner {
  view?: KRImmersiveTabBarSelfDrawnView;
  @Prop items: string[] = [];
  @Prop selectedIndex: number = 0;
  /** 整个 bar 的圆角（vp）—— 作用在 bar 底板层，不是单个格子 */
  @Prop pillRadius: number = 0;
  /** 胶囊格高度（vp） */
  @Prop pillHeight: number = 52;
  /**
   * bar 底板背景色，默认透明：底板若不透明（如纯白），格子的沉浸光感材质
   * 折射采样的就是这层纯色 —— 玻璃模糊纯色看不出任何效果（12017 实测踩过）。
   * 默认透明让页面背景透进 bar 区域，材质才有内容可折射。
   */
  @Prop pillBackgroundColor: string = '#00000000';
  @Prop materialEnable: boolean = false;
  /** 材质厚薄：1=THIN 更通透（贴近原生胶囊观感）；REGULAR(2) 偏白雾 */
  @Prop materialStyle: number = 1;
  /** 悬浮位置：左右内缩 / 底部抬离（FloatingTabBarStyle，与 HdsTabs 版同义） */
  @Prop barSideMargin: number = 16;
  @Prop barBottomMargin: number = 48;
  /** Kuikly 节点的真实尺寸（sizeDidChanged），ArkTS 内容钉在节点尺寸内 */
  @Prop viewWidth: number = 0;
  @Prop viewHeight: number = 0;

  /**
   * 自绘格的 builder（官方 tabBuilder 同款调用形式：.tabBar(this.TabCellBuilder(label, idx))）。
   * 内部创建 CustomTabCell（独立 @Component，其 aboutToAppear/onAppear 为诊断锚点）。
   */
  @Builder
  TabCellBuilder(label: string, idx: number) {
    CustomTabCell({
      view: this.view,
      label: label,
      idx: idx,
      selectedIndex: this.selectedIndex,
      materialEnable: this.materialEnable,
      materialStyle: this.materialStyle
    })
  }

  build() {
    // Stack 固定根 + 0 items 防御（V2 转发宿主内根级分支切换挂载失败，Select 踩过）
    Stack({ alignContent: Alignment.Bottom }) {
      if (this.items.length > 0) {
        // ── 整体 bar 底板：圆角/外形业务可控的核心层 ──
        // 圆角作用在"整个 tabbar"上（贴底、与 Tabs 的 bar 区域对齐），
        // 格子层透明叠在其上。底板普通背景色；沉浸光感材质仍在格子上
        //（保持 tabBar scope 内的 systemMaterial 命题不变）。
        Column()
          .width('100%')
          .height(this.pillHeight + 24)
          .borderRadius(this.pillRadius)
          .backgroundColor(this.pillBackgroundColor)
        Tabs({ index: this.selectedIndex, controller: this.view!.tabsController }) {
          ForEach(this.items, (label: string, idx: number) => {
            TabContent() {
              Column() {
              }
              .width('100%')
              .height('100%')
              .backgroundColor(Color.Transparent)
            }
            // TabContent 自身默认白底必须透明（白底独立于内部 Column，Select/Tabs 踩过的同款）
            .backgroundColor(Color.Transparent)
            // **@Builder 必须直接调用传入**（官方 tabBuilder 同款）：
            // 包一层箭头函数 ((): void => {...}) 的话，内部的 @Component 创建会静默失败
            // ——实测 CustomTabCell 的 aboutToAppear 零次、格子不上屏。
            .tabBar(this.TabCellBuilder(label, idx))
          }, (label: string, idx: number) => idx.toString())
        }
        .width('100%')
        .height('100%')
        // **Tabs 组件整体背景必须透明**：Tabs（含 TabContent）默认白底，会整块遮挡
        // 背景图（实测页面中部一大块白色）——白底独立于 barBackgroundColor，
        // 两者都要设透明。
        .backgroundColor(Color.Transparent)
        // bar 高度给足（格高 + 图标文字所需 + 留白），内容 '100%' 居中不被裁
        .barHeight(this.pillHeight + 24)
        // ── 官方悬浮材质背板（API 26 的 FloatingTabBarStyle，原生 Tabs 上
        // 与 HdsTabs 的 barFloatingStyle 同源能力）──：背板承载沉浸光感材质
        //（interactive 形变 + lightEffect 光感反馈），自绘格子只负责内容。
        .barFloatingStyle({
          barSideMargin: this.barSideMargin,
          barBottomMargin: this.barBottomMargin,
          systemMaterial: this.materialEnable ?
          new uiMaterial.ImmersiveMaterial({
            style: this.materialStyle as uiMaterial.ImmersiveStyle,
            applyShadow: true,
            interactive: true,
            lightEffect: { color: Color.White }
          }) : uiMaterial.Material.empty
        })
        // bar 去底部：必须链式设置（构造参数的 barPosition 在自绘 tabBar 场景实测不生效，
        // bar 会出现在顶部）。与 HdsTabs 版的链式三件套保持一致。
        .barPosition(BarPosition.End)
        .vertical(false)
        // **必须透明**：Tabs 的 bar 默认白底会整条遮挡背景图（实测：页面中部出现白色
        // 横条），也让材质无从观察。自绘格的背景/材质由格子自己负责。
        .barBackgroundColor(Color.Transparent)
        // 滑动切换保留（自绘格是普通节点，不存在 HdsTabs 材质层的手势仲裁问题）
        .scrollable(true)
        // 材质生效区域条件之一：内容延伸到 bar 底下（官方 HdsTabs 三条件同款）
        .barOverlap(true)
        // 底部安全区延伸：bar 贴到屏幕物理底边，导航条区域不再多出一条白底
        .expandSafeArea([SafeAreaType.SYSTEM], [SafeAreaEdge.BOTTOM])
        .onChange((index: number) => {
          logI(`customTabbar onChange index=${index}`);
          this.view!.selectTab(index);
        })
      } else {
        Column() {
        }.width('100%').height('100%')
      }
    }
    .width(this.viewWidth > 0 ? this.viewWidth : '100%')
    .height(this.viewHeight > 0 ? this.viewHeight : '100%')
    .onAreaChange((_: Area, newArea: Area) => {
      logI(`customTabbar root area: ${JSON.stringify(newArea.width)}x${JSON.stringify(newArea.height)}`);
    })
  }
}

@Builder
function createImmersiveTabBarSelfDrawnView(view: KuiklyRenderBaseView) {
  ImmersiveTabBarSelfDrawnInner({
    view: view as KRImmersiveTabBarSelfDrawnView,
    items: (view as KRImmersiveTabBarSelfDrawnView).items,
    selectedIndex: (view as KRImmersiveTabBarSelfDrawnView).selectedIndex,
    pillRadius: (view as KRImmersiveTabBarSelfDrawnView).pillRadius,
    pillHeight: (view as KRImmersiveTabBarSelfDrawnView).pillHeight,
    pillBackgroundColor: (view as KRImmersiveTabBarSelfDrawnView).pillBackgroundColor,
    materialEnable: (view as KRImmersiveTabBarSelfDrawnView).materialEnable,
    barSideMargin: (view as KRImmersiveTabBarSelfDrawnView).barInsetX,
    barBottomMargin: (view as KRImmersiveTabBarSelfDrawnView).barBottomOffset,
    materialStyle: (view as KRImmersiveTabBarSelfDrawnView).materialStyle,
    viewWidth: (view as KRImmersiveTabBarSelfDrawnView).frameWidth,
    viewHeight: (view as KRImmersiveTabBarSelfDrawnView).frameHeight
  });
}

export class KRImmersiveTabBarSelfDrawnView extends KuiklyRenderBaseView {
  items: string[] = [];
  selectedIndex: number = 0;
  /** 整个 bar 的圆角（vp）—— 作用在底板层（对齐业务自绘 tabbar 的平底/圆角外形） */
  pillRadius: number = 0;
  /** 胶囊格高度（vp） */
  pillHeight: number = 52;
  /** bar 底板背景色（见 Inner 同名 @Prop 注释），默认透明 */
  pillBackgroundColor: string = '#00000000';
  /** 悬浮位置：左右内缩（FloatingTabBarStyle.barSideMargin，与 HdsTabs 版同义） */
  barInsetX: number = 16;
  /** 悬浮位置：底部抬离（FloatingTabBarStyle.barBottomMargin，与 HdsTabs 版同义） */
  barBottomOffset: number = 48;
  materialEnable: boolean = false;
  materialStyle: number = 1;
  /** Kuikly 节点的实际尺寸（sizeDidChanged），ArkTS 内容钉在节点尺寸内 */
  frameWidth: number = 0;
  frameHeight: number = 0;

  /** 程序化切换页签（selectTab -> changeIndex） */
  tabsController: TabsController = new TabsController();

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
      logI(`customTabbar items -> ${this.items.length} (${text})`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropSelectedIndex) {
      // prop 变化必须 selectTab + updateArkUI（HdsTabs 版教训：
      // 漏 updateArkUI 则 @Prop 永不更新，视觉停在旧页签）
      this.selectTab(Number.parseInt(text));
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropPillRadius) {
      this.pillRadius = Number.parseFloat(text);
      this.updateArkUI();
      return true;
    }
    if (propKey === 'barInsetX') {
      this.barInsetX = Number.parseFloat(text);
      logI(`customTabbar barInsetX -> ${this.barInsetX}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === 'barBottomOffset') {
      this.barBottomOffset = Number.parseFloat(text);
      logI(`customTabbar barBottomOffset -> ${this.barBottomOffset}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropPillBackgroundColor) {
      this.pillBackgroundColor = text;
      logI(`customTabbar pillBackgroundColor -> ${text}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropPillHeight) {
      this.pillHeight = Number.parseFloat(text);
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
    const start = value.indexOf('style=');
    if (start >= 0) {
      let end = value.indexOf(',', start);
      if (end < 0) {
        end = value.length;
      }
      this.materialStyle = Number.parseInt(value.substring(start + 6, end));
    }
    this.materialEnable = true;
    logI(`customTabbar material -> enable=true, style=${this.materialStyle}`);
  }

  /** 切换页签的唯一入口（点击 / 滑动 / 业务回传都走这里）。越界与相同索引忽略。 */
  selectTab(index: number): void {
    if (index < 0 || (this.items.length > 0 && index >= this.items.length)) {
      return;
    }
    if (index === this.selectedIndex) {
      return;
    }
    this.selectedIndex = index;
    logI(`customTabbar selectTab -> ${index}`);
    // 切换能力的两根支柱（此前双双缺失，导致"点击无反应"）：
    // 1) changeIndex 驱动 Tabs 视觉切换；
    // 2) updateArkUI 刷新 @Prop selectedIndex，格子的选中态才会变。
    try {
      this.tabsController.changeIndex(index);
    } catch (e) {
      logI(`customTabbar changeIndex error: ${e}`);
    }
    this.updateArkUI();
    const cb = this.tabChangedCallback;
    if (cb !== null) {
      // 必须是 JSON 字符串：框架按 JSON 解析，裸值会抛 JSONException 并 abort
      const payload: string = JSON.stringify({ index: index });
      try {
        cb(payload as KRAny);
      } catch (e) {
        logI(`customTabbar tabChanged callback error: ${e}`);
      }
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    super.call(method, params, callback);
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createImmersiveTabBarSelfDrawnView), this);
  }

  sizeDidChanged(width: number, height: number): void {
    const changed = this.frameWidth !== width || this.frameHeight !== height;
    this.frameWidth = width;
    this.frameHeight = height;
    logI(`customTabbar sizeDidChanged: ${width}x${height}`);
    if (changed) {
      this.updateArkUI();
    }
  }

  didMoveToParentView(): void {
  }

  onDestroy(): void {
  }
}
