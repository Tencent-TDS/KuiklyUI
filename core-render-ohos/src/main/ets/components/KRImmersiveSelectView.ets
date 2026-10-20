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
import { ComponentContent, uiMaterial } from '@kit.ArkUI';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { KRAny, KRValue, KuiklyRenderCallback } from '../utils/KRDataTypes';

/** 诊断日志。过滤关键字：ImmLightTest */
const kLogDomain = 0xA012;
const kLogTag = 'ImmLightTest';

function logI(msg: string): void {
  hilog.info(kLogDomain, kLogTag, '%{public}s', msg);
}

/** Kotlin 侧下发的属性键（与 ImmersiveLightSenseSelectOhosAttr / Attr.StyleConst 对齐） */
const kPropOptions = 'options';
const kPropSelectedIndex = 'selectedIndex';
const kPropNameSystemMaterialOhos = 'system-material-ohos';
const kEventSelectChanged = 'selectChanged';

/** 从材质属性串取值，格式与 KRImmersiveLightSenseView.ets 完全一致 */
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

/** 设备是否支持沉浸光感（低版本系统访问会抛异常，兜底 false） */
function isImmersiveMaterialSupported(): boolean {
  try {
    return uiMaterial.isImmersiveMaterialSupported();
  } catch (e) {
    return false;
  }
}

/**
 * 鸿蒙「沉浸光感」Select 的 ArkTS 实现。
 *
 * 两个前面踩过的坑都在这里规避了：
 * 1. **不用 width('100%')**：V2 转发根节点解析到的是全屏 wrapper，必须用
 *    sizeDidChanged 给到的显式尺寸（同 TabBar / Slider 的教训）；
 * 2. **事件回传必须是 JSON 字符串**：框架按 JSON 解析，裸值会抛
 *    JSONException 并 abort 进程（同 Dialog 的教训）。
 */
@Component
struct ImmersiveLightSenseSelectInner {
  view?: KRImmersiveSelectView;
  @Prop options: string[] = [];
  @Prop selected: number = 0;
  /** Kuikly 节点的真实尺寸（sizeDidChanged）；解析不到时给合理默认值 */
  @Prop viewWidth: number = 0;
  @Prop viewHeight: number = 0;

  build() {
    Select(this.view!.buildOptions())
      .selected(this.selected)
      .value(this.options.length > this.selected ? this.options[this.selected] : '')
      .width(this.viewWidth > 0 ? this.viewWidth : 150)
      .height(this.viewHeight > 0 ? this.viewHeight : 40)
      .onSelect((index: number, text?: string) => {
        this.view!.fireSelectChanged(index, text !== undefined ? text : '');
      })
      // 系统规定 systemMaterial 必须写在其他样式属性之后
      .systemMaterial(this.view!.buildMaterial())
  }
}

@Builder
function createImmersiveSelectView(view: KuiklyRenderBaseView) {
  ImmersiveLightSenseSelectInner({
    view: view as KRImmersiveSelectView,
    options: (view as KRImmersiveSelectView).options,
    selected: (view as KRImmersiveSelectView).selectedIndex,
    viewWidth: (view as KRImmersiveSelectView).frameWidth,
    viewHeight: (view as KRImmersiveSelectView).frameHeight
  });
}

export class KRImmersiveSelectView extends KuiklyRenderBaseView {
  options: string[] = [];
  selectedIndex: number = 0;
  /** Kuikly 节点的实际尺寸（sizeDidChanged），ArkTS 内容钉在节点尺寸内 */
  frameWidth: number = 0;
  frameHeight: number = 0;

  // 材质参数，由 Kotlin 侧 immersiveMaterial() 下发
  private materialEnable: boolean = false;
  private materialStyle: uiMaterial.ImmersiveStyle = uiMaterial.ImmersiveStyle.REGULAR;
  private materialInteractive: boolean = false;
  private materialApplyShadow: boolean = true;
  private materialColorValue: number | null = null;

  private selectChangedCallback: KuiklyRenderCallback | null = null;

  init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
  }

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    if (propKey === kEventSelectChanged && typeof propValue === 'function') {
      this.selectChangedCallback = propValue as KuiklyRenderCallback;
      return true;
    }
    if (propKey === kPropNameSystemMaterialOhos) {
      this.applyMaterialProp(propValue as string);
      this.updateArkUI();
      return true;
    }
    const text = propValue as string;
    if (propKey === kPropOptions) {
      this.options = text.length > 0 ? text.split(',') : [];
      logI(`select options -> ${this.options.length} (${text})`);
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
    const materialColor = takeMaterialValue(value, 'materialColor');
    this.materialStyle = style !== null ?
    Number.parseInt(style) as uiMaterial.ImmersiveStyle : uiMaterial.ImmersiveStyle.REGULAR;
    if (interactive !== null) {
      this.materialInteractive = interactive === '1';
    }
    if (applyShadow !== null) {
      this.materialApplyShadow = applyShadow === '1';
    }
    this.materialColorValue = materialColor !== null ? Number.parseInt(materialColor) : null;
    this.materialEnable = true;
  }

  /**
   * 构造材质对象；业务未设置或设备不支持时返回 undefined（自动降级为普通 Select）。
   */
  buildMaterial(): uiMaterial.ImmersiveMaterial | undefined {
    if (!isImmersiveMaterialSupported() || !this.materialEnable) {
      return undefined;
    }
    let options: uiMaterial.ImmersiveOptions = {
      style: this.materialStyle,
      applyShadow: this.materialApplyShadow,
      interactive: this.materialInteractive
    };
    if (this.materialColorValue !== null) {
      options.materialColor = this.materialColorValue;
    }
    return new uiMaterial.ImmersiveMaterial(options);
  }

  /** Select 组件的选项数组（ArkTS 对象字面量必须落在显式类型上） */
  buildOptions(): SelectOption[] {
    const list: SelectOption[] = [];
    for (let i = 0; i < this.options.length; i++) {
      const item: SelectOption = { value: this.options[i] };
      list.push(item);
    }
    return list;
  }

  fireSelectChanged(index: number, text: string): void {
    this.selectedIndex = index;
    const cb = this.selectChangedCallback;
    if (cb !== null) {
      // 必须是 JSON 字符串：框架按 JSON 解析，裸值会抛 JSONException 并 abort 进程
      const payload: string = JSON.stringify({ index: index, value: text });
      try {
        cb(payload as KRAny);
      } catch (e) {
        logI(`select callback error: ${e}`);
      }
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    super.call(method, params, callback);
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createImmersiveSelectView), this);
  }

  sizeDidChanged(width: number, height: number): void {
    const changed = this.frameWidth !== width || this.frameHeight !== height;
    this.frameWidth = width;
    this.frameHeight = height;
    logI(`select sizeDidChanged: ${width}x${height}`);
    if (changed) {
      this.updateArkUI();
    }
  }

  didMoveToParentView(): void {
  }

  onDestroy(): void {
  }
}
