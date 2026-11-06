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
const kPropTitle = 'title';
const kPropTitleMode = 'titleMode';
const kPropHideBackButton = 'hideBackButton';
const kPropEnableScrollEffect = 'enableScrollEffect';
const kPropScrollEffectType = 'scrollEffectType';
const kPropNameSystemMaterialOhos = 'system-material-ohos';
const kPropContentBackgroundColor = 'contentBackgroundColor';

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

/**
 * 「自制标题栏」验证组件（Inner）—— 用**原生 Navigation**（非 HdsNavigation）
 * 的官方能力对标 12018 的 HdsNavigation：
 *
 * 核心验证点（官方文档「示例20：设置 systemMaterial 开启标题栏材质效果」同款路径）：
 * - `NavigationTitleOptions.systemMaterial`：标题栏系统材质（返回键背板 / menu 背板
 *   生效范围）。传 `new uiMaterial.ImmersiveMaterial(...)`，关闭传 `Material.empty`；
 * - `NavigationTitleOptions.scrollEffectOptions`：标题栏滚动模糊
 *   （COMMON_BLUR / GRADUAL_BLUR），内容区滚动时标题栏产生渐变模糊 ——
 *   与 HdsNavigation 的 ScrollEffectType 同名同义（API 26）。
 *
 * 原生 Navigation 的滚动联动由系统自动监听内容区滚动组件（无需 bindToScrollable）。
 *
 * V2 转发已知坑全部规避：显式尺寸、Stack 固定根、JSON 事件回传。
 */
@Component
struct ImmersiveNavigationSelfDrawnInner {
  view?: KRImmersiveNavigationSelfDrawnView;
  @Prop title: string = '标题';
  /** 0=Free 1=Full 2=Mini（原生 NavigationTitleMode） */
  @Prop titleMode: number = 2;
  @Prop hideBackButton: boolean = false;
  @Prop enableScrollEffect: boolean = true;
  /** 0=COMMON_BLUR 1=GRADUAL_BLUR */
  @Prop scrollEffectType: number = 1;
  @Prop materialEnable: boolean = false;
  @Prop materialStyle: number = 2;
  /** 内容区背景色（原生 Navigation 默认白底；透明可对齐 HdsNavigation 观感） */
  @Prop contentBackgroundColor: string = '#00000000';
  /** Kuikly 节点的真实尺寸（sizeDidChanged），ArkTS 内容钉在节点尺寸内 */
  @Prop viewWidth: number = 0;
  @Prop viewHeight: number = 0;

  build() {
    Stack() {
      Navigation() {
        // 滚动内容：原生 Navigation 自动监听内容区滚动 → 标题栏滚动渐变
        Scroll() {
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
      }
      // 原生 Navigation 内容区默认白底（HdsNavigation 默认透明）——
      // 开放给业务设置，Demo 里传透明对齐 HdsNavigation 观感
      .backgroundColor(this.contentBackgroundColor)
      .title(this.title, {
        // SAFE_AREA_PADDING：标题栏自动避让安全区（下移到状态栏之下，不再顶进状态栏）
        barStyle: BarStyle.SAFE_AREA_PADDING,
        // 标题栏默认底色是接近白色的一层背景 —— 显式透明（对齐 HdsNavigation 观感）
        backgroundColor: Color.Transparent,
        // ── 核心验证点 1：标题栏系统材质（示例20 同款）──
        systemMaterial: this.view!.buildTitleBarMaterial(),
        // ── 核心验证点 2：滚动渐变模糊（逐值映射，对齐 HdsScrollEffectType 常量）──
        scrollEffectOptions: this.enableScrollEffect ? {
          scrollEffectType: this.scrollEffectType === 0 ? ScrollEffectType.COMMON_BLUR :
            (this.scrollEffectType === 1 ? ScrollEffectType.GRADUAL_BLUR :
            (this.scrollEffectType === 2 ? ScrollEffectType.GRADIENT_BLUR :
            ScrollEffectType.GRADUAL_BLUR))
        } : undefined
      })
      .titleMode(this.titleMode === 0 ? NavigationTitleMode.Free :
        (this.titleMode === 1 ? NavigationTitleMode.Full : NavigationTitleMode.Mini))
      .hideBackButton(this.hideBackButton)
      .mode(NavigationMode.Stack)
    }
    .width(this.viewWidth > 0 ? this.viewWidth : '100%')
    .height(this.viewHeight > 0 ? this.viewHeight : '100%')
    .onAreaChange((_: Area, newArea: Area) => {
      logI(`selfnav root area: ${JSON.stringify(newArea.width)}x${JSON.stringify(newArea.height)}`);
    })
  }
}

@Builder
function createImmersiveNavigationSelfDrawnView(view: KuiklyRenderBaseView) {
  ImmersiveNavigationSelfDrawnInner({
    view: view as KRImmersiveNavigationSelfDrawnView,
    title: (view as KRImmersiveNavigationSelfDrawnView).title,
    titleMode: (view as KRImmersiveNavigationSelfDrawnView).titleMode,
    hideBackButton: (view as KRImmersiveNavigationSelfDrawnView).hideBackButton,
    enableScrollEffect: (view as KRImmersiveNavigationSelfDrawnView).enableScrollEffect,
    scrollEffectType: (view as KRImmersiveNavigationSelfDrawnView).scrollEffectType,
    materialEnable: (view as KRImmersiveNavigationSelfDrawnView).materialEnable,
    contentBackgroundColor: (view as KRImmersiveNavigationSelfDrawnView).contentBackgroundColor,
    viewWidth: (view as KRImmersiveNavigationSelfDrawnView).frameWidth,
    viewHeight: (view as KRImmersiveNavigationSelfDrawnView).frameHeight
  });
}

export class KRImmersiveNavigationSelfDrawnView extends KuiklyRenderBaseView {
  title: string = '标题';
  titleMode: number = 2;
  hideBackButton: boolean = false;
  enableScrollEffect: boolean = true;
  scrollEffectType: number = 1;
  materialEnable: boolean = false;
  materialStyle: number = 2;
  /** 内容区背景色（见 Inner 同名 @Prop 注释） */
  contentBackgroundColor: string = '#00000000';
  /** Kuikly 节点的实际尺寸（sizeDidChanged），用于把 ArkTS 内容钉在节点尺寸内 */
  frameWidth: number = 0;
  frameHeight: number = 0;

  init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
  }

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    if (propKey === kPropNameSystemMaterialOhos) {
      this.applyMaterialProp(propValue as string);
      this.updateArkUI();
      return true;
    }
    const text = propValue as string;
    if (propKey === kPropTitle) {
      this.title = text;
      logI(`selfnav title -> ${text}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropTitleMode) {
      this.titleMode = Number.parseInt(text);
      logI(`selfnav titleMode -> ${this.titleMode}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropHideBackButton) {
      this.hideBackButton = text === 'true' || text === '1';
      logI(`selfnav hideBackButton -> ${this.hideBackButton}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropEnableScrollEffect) {
      this.enableScrollEffect = text === 'true' || text === '1';
      logI(`selfnav enableScrollEffect -> ${this.enableScrollEffect}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropContentBackgroundColor) {
      this.contentBackgroundColor = text;
      logI(`selfnav contentBackgroundColor -> ${text}`);
      this.updateArkUI();
      return true;
    }
    if (propKey === kPropScrollEffectType) {
      this.scrollEffectType = Number.parseInt(text);
      logI(`selfnav scrollEffectType -> ${this.scrollEffectType}`);
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
    logI(`selfnav material -> enable=true, style=${this.materialStyle}`);
  }

  /**
   * 标题栏系统材质（NavigationTitleOptions.systemMaterial）。
   * 官方语义：undefined=按应用级开关走；Material.empty=无材质；ImmersiveMaterial=指定材质。
   * 关闭时用 Material.empty（显式关闭，避免受应用级开关 ENABLE 影响）。
   */
  buildTitleBarMaterial(): uiMaterial.Material {
    if (!this.materialEnable) {
      return uiMaterial.Material.empty;
    }
    return new uiMaterial.ImmersiveMaterial({
      style: this.materialStyle as uiMaterial.ImmersiveStyle
    });
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    super.call(method, params, callback);
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createImmersiveNavigationSelfDrawnView), this);
  }

  sizeDidChanged(width: number, height: number): void {
    const changed = this.frameWidth !== width || this.frameHeight !== height;
    this.frameWidth = width;
    this.frameHeight = height;
    logI(`selfnav sizeDidChanged: ${width}x${height}`);
    if (changed) {
      this.updateArkUI();
    }
  }

  didMoveToParentView(): void {
  }

  onDestroy(): void {
  }
}
