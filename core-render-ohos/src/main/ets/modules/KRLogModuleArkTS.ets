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

import { KRRenderLog } from '../adapter/KRRenderLog';
import { KRAny, KuiklyRenderCallback } from '../utils/KRDataTypes';
import { KuiklyRenderBaseModule } from './export';

export class KRLogModuleArkTS extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRLogModuleArkTS';
  private static readonly METHOD_LOG_INFO: string = 'logInfo';
  private static readonly METHOD_LOG_DEBUG: string = 'logDebug';
  private static readonly METHOD_LOG_ERROR: string = 'logError';

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRLogModuleArkTS.METHOD_LOG_INFO: {
        this.logInfo(params);
        break;
      }
      case KRLogModuleArkTS.METHOD_LOG_DEBUG: {
        this.logDebug(params);
        break;
      }
      case KRLogModuleArkTS.METHOD_LOG_ERROR: {
        this.logError(params);
        break;
      }
      default:
        break;
    }
    return null;
  }

  private logInfo(params: KRAny) {
    const message: string = params as string;
    const tag: string = this.getTag(params as string);
    KRRenderLog.i(tag, message);
  }

  private logDebug(params: KRAny) {
    const message: string = params as string;
    const tag: string = this.getTag(params as string);
    KRRenderLog.d(tag, message);
  }

  private logError(params: KRAny) {
    const message: string = params as string;
    const tag: string = this.getTag(params as string);
    KRRenderLog.e(tag, message);
  }

  private getTag(msg: string): string {
    const prefix: string = '[KLog][';
    const suffix: string = ']:';
    const beginIndex = msg.indexOf(prefix);
    if (beginIndex != -1) {
      const endIndex = msg.indexOf(suffix, beginIndex + prefix.length);
      if (endIndex != -1) {
        return msg.substring(beginIndex + prefix.length, endIndex);
      }
    }
    return '';
  }
}