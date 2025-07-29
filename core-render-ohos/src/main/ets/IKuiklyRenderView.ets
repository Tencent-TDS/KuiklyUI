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

import type { KRRecord } from './utils/KRDataTypes';
import type { KRRenderModuleExportCreator, KuiklyRenderBaseModule } from './modules/export';
import { IKuiklyRenderViewExport, KRRenderViewExportCreator, KuiklyRenderBaseView } from './components/export';

/**
 * 代表Kuikly页面的根View接口，实现此接口的View，视为kuikly页面的根View
 * 外界通过实现此接口的View与Kuikly页面交互和通信
 */
export interface IKuiklyRenderView {

  /**
   * Native事件发送给Kuikly页面
   * @param event 发送给 Kuikly 侧的事件名字
   * @param data 发送给 Kuikly 侧的事件数据
   */
  sendEvent(event: string, data: KRRecord): void;

  /**
   * 获取 [KuiklyRenderBaseModule]
   * @param name module 的名字
   * @return [KuiklyRenderBaseModule] 的子类
   */
  getModule<T extends KuiklyRenderBaseModule>(moduleName: string): T | null;

  /**
   * 根据tag获取[View]
   * @param tag [View]对应的tag
   * @return 获取的[View]
   */
  getView(tag: number): KuiklyRenderBaseView | null;

  /**
   * View 可见
   */
  pageDidAppear(): void;

  /**
   * View 不可见
   */
  pageDidDisappear(): void;

  /**
   * 销毁 [IKuiklyRenderView]
   */
  destroy(): void;

  /**
   * 同步布局和渲染（在当前线程渲染执行队列中所有任务以实现同步渲染）
   */
  syncFlushAllRenderTasks(): void;

  /**
   * 添加子孩子
   */
  addSubRenderView(subRenderView: IKuiklyRenderViewExport): void;

  /**
   * 获取渲染视图创建注册Map
   */
  getRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator>;

  /**
   * 获取module创建注册Map
   */
  getRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator>;

  /**
   * 执行任务当首屏完成后(优化首屏性能)（仅支持在主线程调用）
   */
  // performWhenViewDidLoad(task: KuiklyRenderCoreTask): void;

  // /**
  //  * 添加生命周期回调
  //  * @param callback
  //  */
  // addKuiklyRenderLifecycleCallback(callback: IKuiklyRenderLifecycleCallback): void;
  //
  // /**
  //  * 移除生命周周期回调
  //  */
  // removeKuiklyRenderLifeCycleCallback(callback: IKuiklyRenderLifecycleCallback): void;

  /**
   * 分发onActivityResult事件
   * @param requestCode
   * @param resultCode
   * @param data
   */
  // dispatchOnActivityResult(requestCode: number, resultCode: number, data: Intent | null): void;

  /**
   * 获取当前kuikly实例的instanceId
   * @returns
   */
  getInstanceId(): string;

}
