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

import { KRAny, KuiklyRenderCallback } from '../../utils/KRDataTypes';


/**
 * module组件协议，module通过实现[IKuiklyRenderModuleExport]协议, 将module提供给Native侧调用
 * 注：module需要去KRNativeManager进行注册，方式为：KRNativeManager.getInstance().registerModuleCreator
 */
export interface IKuiklyRenderModuleExport {
  /**
   * 调用当前module的实例方法
   * @param method 方法名字
   * @param params 方法参数 (透传kotlin侧数据, 类型可为string, Array, Int8Array, number)
   * @param callback 闭包回调
   * @return 方法返回值 (类型可为string, Array, Int8Array, number)
   */
  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny;

  /**
   * Module销毁时，此方法会被调用，用于销毁module中的资源
   */
  onDestroy(): void;

  /**
   * 是否同步模式。
   * 同步模式的module运行在kuikly线程，支持同步调用和异步调用；
   * 异步模式的module运行在ui线程，只支持异步调用。
   * @returns true表示同步模式，false表示异步模式
   */
  syncMode(): boolean;
}

export type KRRenderModuleExportCreator = () => IKuiklyRenderModuleExport;
