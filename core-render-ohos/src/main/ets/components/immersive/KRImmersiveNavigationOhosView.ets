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
import { ComponentContent } from '@kit.ArkUI';
import {
  HdsNavigation,
  HdsNavigationTitleMode,
  HdsNavigationMenuContentOptions,
  HdsNavigationBadgeIconOptions,
  HdsNavigationIconOptions,
  ScrollEffectType,
  hdsMaterial
} from '@kit.UIDesignKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { KRAny, KRValue, KuiklyRenderCallback } from '../../utils/KRDataTypes';

/** 诊断日志。过滤关键字：ImmLightTest */
const kLogDomain = 0xA012;
const kLogTag = 'ImmLightTest';

function logI(msg: string): void {
  hilog.info(kLogDomain, kLogTag, '%{public}s', msg);
}

/** Kotlin 侧下发的属性键 */
const kPropTitle = 'title';
const kPropSubTitle = 'subTitle';
const kPropMenuIcons = 'menuIcons';
const kPropMenuLabels = 'menuLabels';
const kPropTitleMode = 'titleMode';
const kPropHideBackButton = 'hideBackButton';
const kPropEnableScrollEffect = 'enableScrollEffect';
const kPropScrollEffectType = 'scrollEffectType';
const kPropNameSystemMaterialOhos = 'system-material-ohos';
const kEventMenuClick = 'menuClick';

/**
 * 菜单图标符号表（业务传 sys.symbol 符号名，不带前缀）。
 * $r() 只接受编译期字面量，查不到的名称回退默认图标（与 TabBar 的 itemIcons 同模式）。
 */
const kMenuSymbolTable: Record<string, Resource> = {
  'square_and_pencil': $r('sys.symbol.square_and_pencil'),
  'star': $r('sys.symbol.star'),
  'more': $r('sys.symbol.more'),
  'heart': $r('sys.symbol.heart'),
  'share': $r('sys.symbol.share'),
  'search': $r('sys.symbol.magnifyingglass'),
  'settings': $r('sys.symbol.gearshape'),
  'alarm_fill_1': $r('sys.symbol.alarm_fill_1')
};
const kDefaultMenuSymbol: Resource = $r('sys.symbol.more');

function resolveMenuSymbol(name: string): Resource {
  if (name.length > 0 && kMenuSymbolTable[name] !== undefined) {
    return kMenuSymbolTable[name];
  }
  return kDefaultMenuSymbol;
}

/** 滚动内容：纯色块（贴合官方示例配图——标题栏后浅色带 + 深色内容区 + 白色序号） */
const kTopBandColor: string = '#F1ECE4';
const kContentBlockColor: string = '#1C1C1C';
const kContentBlockCount: number = 10;

/** 内容块序号数组（ArkTS 严格模式不允许 Array.from 的推断写法，用显式循环） */
function buildBlockNumbers(): number[] {
  const nums: number[] = [];
  for (let i = 1; i <= kContentBlockCount; i++) {
    nums.push(i);
  }
  return nums;
}

function toTitleMode(mode: number): HdsNavigationTitleMode {
  if (mode === 0) {
    return HdsNavigationTitleMode.FREE;
  }
  if (mode === 1) {
    return HdsNavigationTitleMode.FULL;
  }
  return HdsNavigationTitleMode.MINI;
}

/**
 * 官方 HdsNavigation 验证组件（Inner）—— 标题栏沉浸光感能力摸底。
 *
 * 官方能力面（SDK 声明 + 官方示例）：
 * - titleMode：FREE(0) / FULL(1) / MINI(2) / MODAL(3)；
 * - titleBar：主标题 + 菜单（图标+label），systemMaterialEffect 标题栏按钮材质；
 * - 滚动联动：bindToScrollable + scrollEffectOpts（COMMON_BLUR / GRADIENT_BLUR），
 *   内容上滚时标题栏产生渐变模糊（沉浸光感核心视效之一）；
 * - hideBackButton / ignoreLayoutSafeArea（沉浸式延伸到状态栏）。
 *
 * V2 转发已知坑全部规避：显式尺寸（viewWidth/viewHeight）、Stack 固定根、
 * 材质关闭用 Material.empty、JSON 事件回传。
 */
@Component
struct ImmersiveNavigationInner {
  view?: KRImmersiveNavigationOhosView;
  @Prop title: string = '标题';
  @Prop subTitle: string = '';
  /** 菜单图标（sys.symbol 符号名）与文字，两数组同下标组成一个菜单项 */
  @Prop menuIcons: string[] = [];
  @Prop menuLabels: string[] = [];
  /** 0=FREE 1=FULL 2=MINI 3=MODAL */
  @Prop titleMode: number = 2;
  @Prop hideBackButton: boolean = false;
  @Prop enableScrollEffect: boolean = false;
  /** 0=COMMON_BLUR 1=GRADIENT_BLUR */
  @Prop scrollEffectType: number = 1;
  @Prop materialEnable: boolean = false;
  @Prop materialStyle: number = 2;
  /** Kuikly 节点的真实尺寸（sizeDidChanged），ArkTS 内容钉在节点尺寸内 */
  @Prop viewWidth: number = 0;
  @Prop viewHeight: number = 0;

  scroller: Scroller = new Scroller();

  build() {
    Stack() {
      HdsNavigation() {
        // 滚动内容：纯色块（官方示例同款配色），滚动时观察标题栏渐变模糊
        Scroll(this.scroller) {
          Column() {
            // 顶部浅色带：标题栏背后的浅色（对应官方配图上沿）
            Column()
              .width('100%')
              .height(80)
              .backgroundColor(kTopBandColor)
            ForEach(buildBlockNumbers(), (num: number) => {
              Stack() {
                Text(`${num}`)
                  .fontSize(40)
                  .fontColor(Color.White)
              }
              .width('100%')
              .height(420)
              .backgroundColor(kContentBlockColor)
              .margin({ bottom: 2 })
            }, (num: number) => num.toString())
          }
        }
        .height('100%')
        .clipContent(ContentClipMode.SAFE_AREA)
      }
      .mode(NavigationMode.Stack)
      .titleBar({
        content: {
          title: { mainTitle: this.title, subTitle: this.subTitle },
          menu: this.view!.buildMenus()
        },
        style: {
          scrollEffectOpts: {
            enableScrollEffect: this.enableScrollEffect,
            scrollEffectType: this.scrollEffectType === 0 ?
            ScrollEffectType.COMMON_BLUR : ScrollEffectType.GRADIENT_BLUR
          },
          systemMaterialEffect: {
            // 实测设备只支持 IMMERSIVE[101]，ADAPTIVE 不在支持列表会静默失效
            materialType: this.materialEnable ?
            hdsMaterial.MaterialType.IMMERSIVE : hdsMaterial.MaterialType.NONE,
            materialLevel: hdsMaterial.MaterialLevel.ADAPTIVE
          }
        },
        // true = 标题栏避让安全区（下移到状态栏之下）；false 会顶进状态栏（实测过高）
        avoidLayoutSafeArea: true,
        enableComponentSafeArea: false
      })
      .bindToScrollable([this.scroller])
      .hideBackButton(this.hideBackButton)
      .titleMode(toTitleMode(this.titleMode))
      // 不 ignoreLayoutSafeArea(TOP)：实测延伸到状态栏下标题栏位置过高（12018 反馈），
      // 保持标题栏位于状态栏之下的常规位置。
      .ignoreLayoutSafeArea([LayoutSafeAreaType.SYSTEM], [LayoutSafeAreaEdge.BOTTOM])
    }
    .width(this.viewWidth > 0 ? this.viewWidth : '100%')
    .height(this.viewHeight > 0 ? this.viewHeight : '100%')
    .onAreaChange((_: Area, newArea: Area) => {
      logI(`hdsnav root area: ${JSON.stringify(newArea.width)}x${JSON.stringify(newArea.height)}`);
    })
  }
}

@Builder
function createImmersiveNavigationOhosView(view: KuiklyRenderBaseView) {
  ImmersiveNavigationInner({
    view: view as KRImmersiveNavigationOhosView,
    title: (view as KRImmersiveNavigationOhosView).title,
    titleMode: (view as KRImmersiveNavigationOhosView).titleMode,
    hideBackButton: (view as KRImmersiveNavigationOhosView).hideBackButton,
    enableScrollEffect: (view as KRImmersiveNavigationOhosView).enableScrollEffect,
    scrollEffectType: (view as KRImmersiveNavigationOhosView).scrollEffectType,
    materialEnable: (view as KRImmersiveNavigationOhosView).materialEnable,
    materialStyle: (view as KRImmersiveNavigationOhosView).materialStyle,
    viewWidth: (view as KRImmersiveNavigationOhosView).frameWidth,
    viewHeight: (view as KRImmersiveNavigationOhosView).frameHeight
  });
}

export class KRImmersiveNavigationOhosView extends KuiklyRenderBaseView {
  title: string = '标题';
  subTitle: string = '';
  menuIcons: string[] = [];
  menuLabels: string[] = [];
  titleMode: number = 2;
  hideBackButton: boolean = false;
  enableScrollEffect: boolean = false;
  scrollEffectType: number = 1;
  materialEnable: boolean = false;
  materialStyle: number = 2;
  /** Kuikly 节点的实际尺寸（sizeDidChanged），用于把 ArkTS 内容钉在节点尺寸内 */
  frameWidth: number = 0;
  frameHeight: number = 0;

  private menuClickCallback: KuiklyRenderCallback | null = null;

  /** 构建标题栏菜单（图标+文字+点击回调），布局排版全交 HdsNavigation 内置 */
  buildMenus(): HdsNavigationMenuContentOptions {
    const items: HdsNavigationBadgeIconOptions[] = [];
    for (let i = 0; i < this.menuLabels.length; i++) {
      const idx = i;
      // ArkTS 严格模式：对象字面量必须逐层显式标注类型
      const content: HdsNavigationIconOptions = {
        icon: resolveMenuSymbol(this.menuIcons.length > idx ? this.menuIcons[idx] : ''),
        label: this.menuLabels[idx],
        action: (): void => {
          logI(`hdsnav menu onClick idx=${idx}`);
          const cb = this.menuClickCallback;
          if (cb !== null) {
            try {
              // 必须是 JSON 字符串：框架按 JSON 解析（TabBar 事件同款约定）
              cb(JSON.stringify({ index: idx }) as KRAny);
            } catch (e) {
              logI(`hdsnav menuClick callback error: ${e}`);
            }
          }
        }
      };
      const item: HdsNavigationBadgeIconOptions = { content: content };
      items.push(item);
    }
    const menus: HdsNavigationMenuContentOptions = { value: items };
    return menus;
  }

  init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
  }

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    if (propKey === kEventMenuClick && typeof propValue === 'function') {
      this.menuClickCallback = propValue as KuiklyRenderCallback;
      return true;
    }
    if (propKey === kPropNameSystemMaterialOhos) {
      this.applyMaterialProp(propValue as string);
      this.updateArkUI();
      return true;
    }
    const text = propValue as string;
    if (propKey === kPropTitle) {
      this.title = text;
      logI(`hdsnav title -> ${text}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropSubTitle) {
      this.subTitle = text;
      logI(`hdsnav subTitle -> ${text}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropMenuIcons) {
      this.menuIcons = text.length > 0 ? text.split(',') : [];
      logI(`hdsnav menuIcons -> ${this.menuIcons.length}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropMenuLabels) {
      this.menuLabels = text.length > 0 ? text.split(',') : [];
      logI(`hdsnav menuLabels -> ${this.menuLabels.length}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropTitleMode) {
      this.titleMode = Number.parseInt(text);
      logI(`hdsnav titleMode -> ${this.titleMode}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropHideBackButton) {
      this.hideBackButton = text === 'true' || text === '1';
      logI(`hdsnav hideBackButton -> ${this.hideBackButton}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropEnableScrollEffect) {
      this.enableScrollEffect = text === 'true' || text === '1';
      logI(`hdsnav enableScrollEffect -> ${this.enableScrollEffect}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropScrollEffectType) {
      this.scrollEffectType = Number.parseInt(text);
      logI(`hdsnav scrollEffectType -> ${this.scrollEffectType}`);
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
    logI(`hdsnav material -> enable=true, style=${this.materialStyle}`);
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    super.call(method, params, callback);
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createImmersiveNavigationOhosView), this);
  }

  sizeDidChanged(width: number, height: number): void {
    const changed = this.frameWidth !== width || this.frameHeight !== height;
    this.frameWidth = width;
    this.frameHeight = height;
    logI(`hdsnav sizeDidChanged: ${width}x${height}`);
    if (changed) {
      this.updateArkUI();
    }
  }

  didMoveToParentView(): void {
  }

  onDestroy(): void {
  }
}
