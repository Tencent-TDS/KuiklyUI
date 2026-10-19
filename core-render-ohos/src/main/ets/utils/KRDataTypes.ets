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

import window from '@ohos.window'

export type KRValue = number | string | ArrayBuffer | Array<KRAny> | boolean | object
export type KRArray = Array<KRValue | Record<string, KRValue>>
export type KRRecord = Record<string, KRValue | KRArray | Record<string, KRValue | KRArray | Record<string, KRValue>>>
export type KRAny = KRValue | KRArray | KRRecord | null
export type KuiklyRenderCallback = (result: KRAny) => void;
export interface Insets {
  left: number,
  top: number,
  right: number,
  bottom: number,
}
export interface AvoidAreaChangeParams {
  type: window.AvoidAreaType,
  area: window.AvoidArea,
}