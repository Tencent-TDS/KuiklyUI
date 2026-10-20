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

import { KuiklyRenderBaseView } from '../base/KRBaseViewExport';
import { ComponentContent, NodeContent, uiMaterial } from '@kit.ArkUI';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { KRAny, KRValue, KuiklyRenderCallback } from '../../utils/KRDataTypes';

/**
 * 诊断日志。用 hilog 而不是 KRRenderLog —— 后者依赖宿主注入 adapter，未注入时完全静默，
 * 无法用于验证。过滤关键字：ImmLightTest
 */
const kLogDomain = 0xA012;
const kLogTag = 'ImmLightTest';

function logI(msg: string): void {
  hilog.info(kLogDomain, kLogTag, '%{public}s', msg);
}

/** Kotlin 侧下发的属性键，与 Attr.StyleConst.SYSTEM_MATERIAL_OHOS 保持一致。 */
const kPropNameSystemMaterialOhos = 'system-material-ohos';

/**
 * 从属性串里取出指定键的值。
 * 格式与 C++ 侧 ParseSystemMaterialOptions 完全一致：
 *   "off"                                        表示不设材质
 *   "style=N,interactive=1,applyShadow=0,materialColor=<十进制无符号>"
 */
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

/**
 * 设备是否支持沉浸光感（API 26+ 的系统材质能力）。
 * 该接口在低版本系统上不存在，访问会抛异常，故兜底返回 false。
 */
function isImmersiveMaterialSupported(): boolean {
  try {
    return uiMaterial.isImmersiveMaterialSupported();
  } catch (e) {
    return false;
  }
}

/**
 * 鸿蒙「沉浸光感」容器：Navigation 占满容器尺寸，只呈现标题栏区域，内容区留空。
 * 子视图挂进标题栏的 ContentSlot，因此落在系统规定的沉浸光感生效区（Navigation 标题栏 scope）内。
 *
 * 这里必须包一层 @Component struct —— 官方 FAQ 里「Navigation 标题栏承载自定义内容」
 * 的标准写法是：@Builder 作为 struct 的成员方法，以引用的方式传给 .title()。
 * 两种更直接的写法在本工程都不可用：
 *   1) 在 .title() 的 lambda 里直接写 Stack(){} —— ArkTS 解析不过，尾随 lambda 在
 *      对象字面量内被当成普通代码块（报 ';' expected）；
 *   2) 在 lambda 里调用全局 @Builder —— 能编译，但调用时丢失渲染上下文，运行时抛
 *      observeComponentCreation2 of undefined。
 */
@Component
struct ImmersiveLightSenseContainer {
  view?: KRImmersiveLightSenseView;
  /**
   * 标题栏高度（vp），由外层 builder 从 KRImmersiveLightSenseView.titleBarHeight 传入。
   *
   * 必须是 @Prop 而不是普通属性：首次 build 发生在布局之前，此时高度还是 0；
   * 布局完成后 C++ 侧会回调 sizeDidChanged → updateArkUI()，外层 builder 会带着
   * 新高度重跑。普通属性只做一次初始化、不接收父层更新，标题栏会一直停在 0 高度，
   * 子节点被裁掉（现象：节点树里有、屏幕上不渲染）。
   */
  @Prop titleHeight: number = 0;

  /**
   * 标题栏内容：承载 Kuikly 子节点的 ContentSlot。
   * 容器级材质就设在这一层上 —— 容器自身的节点不在系统规定的生效区域内，
   * 只有标题栏内这一层才在，因此表现为"整个标题栏区域有一层玻璃底"。
   */
  @Builder
  TitleBarContent() {
    Stack({ alignContent: Alignment.TopStart }) {
      ContentSlot(this.view!.getTitleBarContent());
    }
    .width('100%')
    .height('100%')
    // 系统规定 systemMaterial 必须写在其他样式属性之后
    .systemMaterial(this.view!.buildMaterial())
  }

  aboutToAppear(): void {
    logI(`aboutToAppear: supported=${isImmersiveMaterialSupported()}, titleHeight=${this.titleHeight}`);
  }

  /**
   * 根节点始终是 Navigation —— 它是普通组件，任何版本都能渲染标题栏与内容；
   * 缺的只是 uiMaterial（设备不支持时 buildMaterial 返回 undefined，即不设材质）。
   * 因此不需要"降级为另一个根"的分支：ArkUI 的 build() 只允许一个根节点，
   * 而且 if/else 分支出两个根会直接编译失败。
   */
  build() {
    Navigation() {
      Column() {
      }.width('100%').height('100%')
    }
    // barStyle 使用 STACK —— 官方沉浸光感文档推荐的标题栏样式
    .title({ builder: this.TitleBarContent, height: this.titleHeight },
      { barStyle: BarStyle.STACK })
    .titleMode(NavigationTitleMode.Mini)
    .hideBackButton(true)
    .hideToolBar(true)
    .width('100%')
    .height('100%')
  }
}

/**
 * 对外的 ArkTS 根 builder：只负责把容器 struct 挂上去。
 * ComponentContent 只接受全局 @Builder，故保留这一层转发。
 * 入参类型必须与基类一致（KuiklyRenderBaseView），内部再 cast。
 */
@Builder
function createImmersiveLightSenseView(view: KuiklyRenderBaseView) {
  ImmersiveLightSenseContainer({
    view: view as KRImmersiveLightSenseView,
    titleHeight: (view as KRImmersiveLightSenseView).titleBarHeight
  });
}

export class KRImmersiveLightSenseView extends KuiklyRenderBaseView {
  private nodeContent: NodeContent = new NodeContent();
  /**
   * 容器高度（vp）。基类已有私有的 viewHeight，这里必须用不同的名字。
   */
  titleBarHeight: number = 0;

  // 容器级材质参数，由 Kotlin 侧 immersiveMaterial() / systemMaterialOhos() 下发
  private materialEnable: boolean = false;
  private materialStyle: uiMaterial.ImmersiveStyle = uiMaterial.ImmersiveStyle.REGULAR;
  private materialInteractive: boolean = false;
  private materialApplyShadow: boolean = true;
  private materialColorValue: number | null = null;

  init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
  }

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    if (propKey === kPropNameSystemMaterialOhos) {
      this.applyMaterialProp(propValue as string);
      this.updateArkUI();
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
   * 构造容器级材质对象（标题栏区域的"玻璃底"）。
   * 业务未设置或设备不支持时返回 undefined，即不设材质。
   */
  buildMaterial(): uiMaterial.ImmersiveMaterial | undefined {
    const supported = isImmersiveMaterialSupported();
    if (!supported) {
      // 只在这里打日志：业务没设材质是正常情况，设备不支持才是需要被看见的降级事件
      logI(`device not supported, material disabled. titleHeight=${this.titleBarHeight}`);
      return undefined;
    }
    if (!this.materialEnable) {
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
    const material = new uiMaterial.ImmersiveMaterial(options);
    logI(`container material created, style=${this.materialStyle}, titleHeight=${this.titleBarHeight}`);
    return material;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    super.call(method, params, callback);
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext,
      wrapBuilder<[KuiklyRenderBaseView]>(createImmersiveLightSenseView), this);
  }

  /**
   * 提供内部 ContentSlot 的挂载点：Kuikly 的子节点会通过
   * OH_ArkUI_NodeContent_AddNode 挂到这个 NodeContent 上。
   */
  getContentSlot(): NodeContent | null {
    return this.nodeContent;
  }

  /**
   * 非空版本，供 builder 内直接交给 ContentSlot 使用，避免可空类型问题。
   */
  getTitleBarContent(): NodeContent {
    return this.nodeContent;
  }

  sizeDidChanged(width: number, height: number): void {
    this.titleBarHeight = height;
    // 高度是标题栏能否渲染的关键：为 0 说明 V2 的 frame 链路又断了，内容会被裁掉
    logI(`sizeDidChanged: w=${width}, h=${height}`);
  }

  didMoveToParentView(): void {
  }

  onDestroy(): void {
  }
}
