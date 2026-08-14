/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { KRAny, KRValue, KuiklyRenderCallback } from '../../utils/KRDataTypes';
import { ComponentContent, NodeContent, UIContext } from '@kit.ArkUI';
import { KRNativeManager } from '../../manager/KRNativeManager';
import { KRNativeInstance } from '../../manager/KRNativeInstance';
import { KRColorParser } from '../../utils/KRColorParser';
import { KRRenderLog } from '../../adapter/KRRenderLog';
import accessibility from '@ohos.accessibility';
import { common } from '@kit.AbilityKit';

export const kCssBackgroundColor = 'backgroundColor'
export const kCssBackgroundImage = 'backgroundImage'
export const kCssAccessibilityText = 'accessibility'
export const kCssAccessibilityRole = 'accessibilityRole'
export const kCssAccessibilityInfo = 'accessibilityInfo'
const kMethodAccessibilityAnnounce = 'accessibilityAnnounce'
const kMethodAccessibilityFocus = 'accessibilityFocus'

export interface IKuiklyRenderViewExport {
  init(instanceId: string, viewTag: number, viewType: string): void;

  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean;

  /**
   * 调用当前View的实例方法
   * @param params 方法参数 (透传kotlin侧数据, 类型可为string, Array, Int8Array, number)
   * @param callback 回调
   */
  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void;
}

export abstract class KuiklyRenderBaseView implements IKuiklyRenderViewExport {
  private instanceId: string = '';
  private viewTag: number = 0;
  private viewType: string = '';
  private arkUINode: ComponentContent<KuiklyRenderBaseView> | null = null;
  private viewWidth: number = 0;
  private viewHeight: number = 0;
  cssBackgroundColor: ResourceColor | null = null;
  cssLinearGradient: LinearGradient | null = null;
  // 无障碍字段：由 setProp 写入，由业务方 @Component 在 build() 里读取后应用到 ArkTS 修饰器。
  // 参考：docs/DevGuide/ohos-custom-accessibility.md。
  cssAccessibilityText: string | null = null;
  cssAccessibilityRole: string | null = null;
  cssAccessibilityInfo: string | null = null;
  nodeId: string = '';

  getViewWidth(): number {
    return this.viewWidth;
  }

  getViewHeight(): number {
    return this.viewHeight;
  }

  getInstanceId(): string {
    return this.instanceId;
  }

  getViewTag(): number {
    return this.viewTag;
  }

  getViewType(): string {
    return this.viewType;
  }

  getUIContext(): UIContext | null {
    return this.getInstance()?.getUIContext() ?? null;
  }

  getInstance(): KRNativeInstance | null {
    return KRNativeManager.getInstance().getNativeInstance(this.instanceId);
  }

  getNodeId(): string {
    return this.nodeId;
  }

  // IKRViewExport interface
  /**
   * 初始化方法
   * @param instanceId 实例id
   * @param viewTag 视图Tag
   * @param viewType 视图类型
   */
  init(instanceId: string, viewTag: number, viewType: string): void {
    this.instanceId = instanceId;
    this.viewTag = viewTag;
    this.viewType = viewType;
  }

  // IKRViewExport interface
  /**
   * 设置（更新）属性时调用
   * @param propKey 视图实例属性名
   * @param propValue 视图实例属性值，类型一般为字符串等基础数据结构以及RenderCallback（用于事件绑定）
   * @return 是否处理了该key
   */
  setProp(propKey: string, propValue: KRAny | KuiklyRenderCallback): boolean {
    if (propKey == kCssBackgroundColor) {
      this.cssBackgroundColor = KRColorParser.toColor(propValue as string);
      return true;
    } else if (propKey == kCssBackgroundImage) {
      this.setBackgroundImage(propValue as string);
      return true;
    } else if (propKey == kCssAccessibilityText) {
      this.cssAccessibilityText = (propValue as string) ?? null;
      return true;
    } else if (propKey == kCssAccessibilityRole) {
      this.cssAccessibilityRole = (propValue as string) ?? null;
      return true;
    } else if (propKey == kCssAccessibilityInfo) {
      this.cssAccessibilityInfo = (propValue as string) ?? null;
      return true;
    }

    return false;
  }

  private setBackgroundImage(value: string): void {
    const linearGradientPrefix = 'linear-gradient(';
    if (value?.startsWith(linearGradientPrefix) && value.endsWith(')')) {
      const lg = value.substring(linearGradientPrefix.length, value.length - 1);
      this.cssLinearGradient = this.parseLinearGradient(lg);
    } else {
      this.cssLinearGradient = null;
    }
  }

  private parseLinearGradient(lg: string): LinearGradient | null {
    try {
      const splits = lg.split(',');
      return {
        direction: KuiklyRenderBaseView.gradientDirectionOrder[parseInt(splits[0], 10)],
        colors: splits.slice(1).map((value) => {
          const pair = value.trim().split(' ');
          return [KRColorParser.toColor(pair[0]), parseFloat(pair[1])];
        }),
      };
    } catch (err) {
      KRRenderLog.e('kuikly.render', `parse linearGradient input="${lg}" error=${JSON.stringify(err)}`);
      return null;
    }
  }

  private static readonly gradientDirectionOrder = [
    GradientDirection.Top,
    GradientDirection.Bottom,
    GradientDirection.Left,
    GradientDirection.Right,
    GradientDirection.LeftTop,
    GradientDirection.RightTop,
    GradientDirection.LeftBottom,
    GradientDirection.RightBottom
  ];

  // IKRViewExport interface
  /**
   * 设置事件时调用
   * @param eventKey 和KR约定好的事件名
   * @param eventCallback 事件响应回调用KR层
   */
  //abstract setEvent(eventKey: string, eventCallback: KTCallback): void
  /**
   * 调用当前View(Node)的实例方法
   * @param method 方法名字
   * @param params 方法参数 (透传kotlin侧数据，类型可为string, Array, Int8Array, number)
   * @param callback 闭包回调
   *
   * 基类默认实现拦截 `accessibilityAnnounce` / `accessibilityFocus` 两个方法（通过
   * `@ohos.accessibility.sendAccessibilityEvent` 触发无障碍事件）。子类 override 时，
   * 对未处理的 method 应调用 `super.call(method, params, callback)` 以避免丢失基础能力。
   */
  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    if (method === kMethodAccessibilityAnnounce) {
      this.sendAccessibilityAnnounce((params as string) ?? '');
      return;
    }
    if (method === kMethodAccessibilityFocus) {
      this.sendAccessibilityFocus();
      return;
    }
  }

  // 取 bundleName：优先从 UIContext.getHostContext() 拿，失败回退到 KRNativeInstance 的 ability context。
  // 空串会导致 accessibility.sendAccessibilityEvent 抛 401，由 spike 验证。
  private resolveBundleName(): string {
    const ctx = (this.getUIContext()?.getHostContext() as common.Context | undefined);
    const bn = ctx?.applicationInfo?.name;
    if (bn && bn.length > 0) {
      return bn;
    }
    return '';
  }

  private sendAccessibilityAnnounce(message: string): void {
    KuiklyRenderBaseView.postAccessibilityAnnounce(this.resolveBundleName(), message);
  }

  private sendAccessibilityFocus(): void {
    KuiklyRenderBaseView.postAccessibilityFocus(this.resolveBundleName(), this.nodeId);
  }

  /**
   * 触发一次无障碍主动播报（announceForAccessibility）。
   *
   * 与具体 view 实例无关：CAPI 内置组件在 ArkTS 侧没有 view 注册项，可通过 KRNativeInstance
   * 的全局兜底直接调用本静态方法完成播报。
   *
   * @param bundleName 应用包名（accessibility.EventInfo 必填字段，空串会触发 401）
   * @param message    需要朗读的文本
   */
  public static postAccessibilityAnnounce(bundleName: string, message: string): void {
    if (!bundleName) {
      KRRenderLog.e('KuiklyRenderBaseView',
        `accessibilityAnnounce skipped: bundleName is empty, message=${message}`);
      return;
    }
    try {
      const ev = new accessibility.EventInfo('announceForAccessibility', bundleName, 'common');
      ev.textAnnouncedForAccessibility = message;
      accessibility.sendAccessibilityEvent(ev).catch((err: Error) => {
        KRRenderLog.e('KuiklyRenderBaseView',
          `accessibilityAnnounce failed: ${JSON.stringify(err)}`);
      });
    } catch (e) {
      KRRenderLog.e('KuiklyRenderBaseView',
        `accessibilityAnnounce throw: ${JSON.stringify(e)}`);
    }
  }

  /**
   * 触发一次无障碍焦点跳转（requestFocusForAccessibility）。
   *
   * @param bundleName 应用包名
   * @param customId   目标节点的 customId（对应 ArkTS 组件 .id() 或 CAPI 侧 NODE_ID）
   */
  public static postAccessibilityFocus(bundleName: string, customId: string): void {
    if (!bundleName) {
      KRRenderLog.e('KuiklyRenderBaseView',
        `accessibilityFocus skipped: bundleName is empty, customId=${customId}`);
      return;
    }
    try {
      const ev = new accessibility.EventInfo('requestFocusForAccessibility', bundleName, 'common');
      // customId 需业务方 @Component 最外层绑定 `.id(this.renderView.getNodeId())` 才能命中；
      // CAPI 组件由 cpp 侧写入 NODE_ID，同样按此值匹配。
      ev.customId = customId;
      accessibility.sendAccessibilityEvent(ev).catch((err: Error) => {
        KRRenderLog.e('KuiklyRenderBaseView',
          `accessibilityFocus failed: ${JSON.stringify(err)}`);
      });
    } catch (e) {
      KRRenderLog.e('KuiklyRenderBaseView',
        `accessibilityFocus throw: ${JSON.stringify(e)}`);
    }
  }

  /**
   * 从任意 UIContext 解析 bundleName（供 KRNativeInstance 兜底调用时使用）。
   */
  public static resolveBundleNameFrom(uiContext: UIContext | null): string {
    const ctx = (uiContext?.getHostContext() as common.Context | undefined);
    const bn = ctx?.applicationInfo?.name;
    return (bn && bn.length > 0) ? bn : '';
  }

  /**
   * 创建该节点对应的ArkUI
   * @returns 返回ComponentContent实例
   * 参考：
   * new ComponentContent<KRBaseViewExport>(this.getUIContext() as UIContext, wrapBuilder<[KRBaseViewExport]>(createArkUIView), this);
   * 其中createArkUIView定义为：
   * @Builder
    function createArkUIView(view: KRBaseViewExport) {
         // 构造你的UI,如Column
          Column(){

          }.width('100%').height('100%') // 务必设置width('100%').height('100%')，保证在ArkTS节点在CAPI侧的Wrapper Node容器中撑满
    }
   }
   */
  abstract createArkUIView(): ComponentContent<KuiklyRenderBaseView>

  getContentSlot(): NodeContent | null{
    return null
  }

  /**
   * View销毁时，此方法会被调用，用于销毁View中的资源
   */
  onDestroy(): void {
  }

  /**
   * 已经添加到父节点中
   */
  didMoveToParentView() {
  }

  /**
   * 设置ViewSize
   */
  sizeDidChanged(width: number, height: number) {
  }

  /**
   * 在View数据变化的情况下，可以调用该方法更新ArkUI View节点
   */
  updateArkUI() {
    if (this.arkUINode) {
      this.arkUINode.update(this);
    }
  }

  /**
   * @final （禁止重写）
   */
  toSetProp(propKey: string, propValue: KRValue): boolean {
    let res = this.setProp(propKey, propValue);
    if (this.arkUINode != null) {
      this.arkUINode.update(this);
    }
    return res;
  }

  /**
   * @final （禁止重写）
   */
  toSetEvent(eventKey: string, eventCallback: KuiklyRenderCallback): void {
    this.setProp(eventKey, eventCallback);
  }

  /**
   * @final  （禁止重写）
   */
  toCreateArkUIView(nodeId: string): ComponentContent<KuiklyRenderBaseView> | null {
    if(this.getUIContext() == null){
      // in this case, the instance must has been destroyed
      return null
    }
    this.nodeId = nodeId;
    let node: ComponentContent<KuiklyRenderBaseView> = this.createArkUIView();
    this.arkUINode = node;
    return node;
  }

  /**
   * @final  （禁止重写）
   */
  toDestroy(): void {
    this.onDestroy();

    if(this.arkUINode){
      (this.arkUINode as ComponentContent<KuiklyRenderBaseView>).dispose()
    }
    this.arkUINode = null;
  }

  /**
   * @final  （禁止重写）
   */
  toCall(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    this.call(method, params, callback);
  }

  /**
   * @final  （禁止重写）
   */
  toDidMoveToParentView() {
    this.didMoveToParentView();
  }

  /**
   * @final  （禁止重写）
   */
  toSizeDidChanged(width: number, height: number) {
    this.viewWidth = width;
    this.viewHeight = height;
    this.sizeDidChanged(width, height);
    this.updateArkUI();
  }

  onConfigurationUpdate(){
    if( this.arkUINode){
      this.arkUINode.updateConfiguration()
    }
  }
}

export type KRRenderViewExportCreator = () => KuiklyRenderBaseView;