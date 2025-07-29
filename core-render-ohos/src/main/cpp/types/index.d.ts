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

import { constant } from "@kit.ConnectivityKit"
import { common } from "@kit.AbilityKit"
import { Context, UIContext } from "@ohos.arkui.UIContext"
import { ComponentContent } from "@kit.ArkUI"
import { resourceManager } from "@kit.LocalizationKit"

type KRValue = number | string | ArrayBuffer | Array<KRAny> | boolean | object
type KRArray = Array<KRValue | Record<string, KRValue>>
type KRRecord = Record<string, KRValue | KRArray | Record<string, KRValue | KRArray | Record<string, KRValue>>>
type KRAny = KRValue | KRArray | KRRecord | null
export type KRNativeCallback = (instanceId: string, methodId: number, arg0: KRAny, arg1: KRAny, arg2: KRAny, arg3: KRAny, arg4: KRAny, callbackID: string | null) => KRAny | ComponentContent<any>;

export const onRenderViewSizeChanged: (instanceId: string, width: number, height: number) => number
export const onDestroyRenderView: (instanceId: string) => number
export const onInitRenderView: (
  instanceId: string,
  pageName: string,
  pageDataJsonStr: string,
  renderViewWidth: number,
  renderViewHeight: number,
  configJson: string,
  uiContext: object,
  resourcesManager: resourceManager.ResourceManager
) => number
export const updateConfig: (instanceId: string, configJson: string) => number
/**
 * ArkTS层启动事件通知到Native层，用于Performance数据采集。
 * @param instanceId 实例id
 * @param excuteMode 运行模式
 */
export const OnLaunchStart: (instanceId: string, excuteMode: number) => void
/*
 * ArkTS调用Native侧方法唯一通信通道
 * @param instanceId 实例id
 * @param methodId 方法id，参考XXXX枚举
 * @param arg0 第一个KRValue类型参数
 * @param arg1 第二个KRValue类型参数
 * @param arg2 第三个KRValue类型参数
 * @param arg3 第四个KRValue类型参数
 * @param arg4 第五个KRValue类型参数
 * @param callback 来自native侧的异步回调闭包参数
 */
export const arkTSCallNative: (instanceId: string, methodId: number, arg0:KRValue|null, arg1: KRValue|null, arg2: KRValue|null, arg3: KRValue|null, arg4: KRValue|null, callback:KRNativeCallback | null ) => number

/**
 * 发送事件给页面
 * @param instanceId 实例id
 * @param event 事件名字
 * @param data 事件参数
 */
export const sendEvent: (
  instanceId: string,
  event: string,
  data: KRValue
) => void

export const createNativeRoot: (content: Object, instanceId: string) => void;

export const isBackPressConsumed: (instanceId: string, sendTime: number) => number;