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

import { KRAny, KRRecord, KuiklyRenderCallback } from '../utils/KRDataTypes';
import { KRRenderLog } from '../adapter/KRRenderLog';
import { KRNativeManager } from '../manager/KRNativeManager';
import { KuiklyRenderBaseModule } from './base/KRBaseModule';

export class KRExceptionModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  static readonly MODULE_NAME = 'KRExceptionModule';
  private static readonly ON_EXCEPTION = 'onException';

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRExceptionModule.ON_EXCEPTION:
        this.fireExceptionCallback(params);
        return null;
      default:
        return null;
    }
  }

  onDestroy(): void {
  }

  private fireExceptionCallback(params: KRAny) {
    try {
      const paramsJSON = JSON.parse(params as string) as KRRecord;
      const stack = paramsJSON['stack'] as string;
      KRNativeManager.getInstance().getNativeInstance(this.instanceId)?.fireExceptionCallback(stack);
    } catch (e) {
      KRRenderLog.e(KRExceptionModule.MODULE_NAME, `fireExceptionCallback, stack: ${e}`);
    }
  }
}