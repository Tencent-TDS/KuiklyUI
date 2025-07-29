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

export const kCssBackgroundColor = 'backgroundColor'
export const kCssBackgroundImage = 'backgroundImage'

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
   * @param params 方法参数 (透传kotlin侧数据, 类型可为string, Array, Int8Array, number)
   * @param callback 闭包回调
   */
  abstract call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void

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
  toCreateArkUIView(nodeId: string): ComponentContent<KuiklyRenderBaseView> {
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
}

export type KRRenderViewExportCreator = () => KuiklyRenderBaseView;