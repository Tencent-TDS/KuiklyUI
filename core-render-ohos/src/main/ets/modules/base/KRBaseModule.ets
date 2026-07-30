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
import { KRNativeRenderController } from '../../KRNativeRenderController';
import { KRNativeManager } from '../../manager/KRNativeManager';
import { IKuiklyRenderModuleExport } from './IKRModuleExport';
import { KuiklyRenderBaseView } from '../../components/base/KRBaseViewExport';

export abstract class KuiklyRenderBaseModule implements IKuiklyRenderModuleExport {
  protected controller: KRNativeRenderController | null = null;
  protected instanceId = '';

  abstract syncMode(): boolean;

  init(instanceId: string): void {
    this.instanceId = instanceId;
    this.controller = KRNativeManager.getInstance().getNativeInstance(instanceId)?.getNativeRenderController() ?? null;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    return null;
  }

  onDestroy(): void {
  }

  // 根据tag获取自定义view
  viewWithTag(tag: number): KuiklyRenderBaseView | null {
    let nativeInstance = KRNativeManager.getInstance().getNativeInstance(this.instanceId);
    if (nativeInstance) {
      return nativeInstance.viewWithTag(tag);
    }
    return null;
  }
}