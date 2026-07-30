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
import { KuiklyRenderAdapterManager } from '../adapter/KRAdapterManager';
import { KuiklyRenderBaseModule } from './base/KRBaseModule';
import { KRRenderLog } from '../adapter/KRRenderLog';
import { common } from '@kit.AbilityKit';

export class KRRouterModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  static readonly MODULE_NAME = 'KRRouterModule';
  private static readonly OPEN_PAGE = 'openPage';
  private static readonly CLOSE_PAGE = 'closePage';

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRRouterModule.OPEN_PAGE:
        this.openPage(params as string);
        return null;
      case KRRouterModule.CLOSE_PAGE:
        this.closePage();
        return null;
      default:
        return null;
    }
  }

  onDestroy(): void {
  }

  // 打开页面
  private openPage(params: string) {
    try {
      const paramsJSON = JSON.parse(params) as KRRecord;
      const pageName = paramsJSON['pageName'] as string;
      const pageData = (paramsJSON['pageData'] as KRRecord | undefined) ?? {};
      const ctx = getContext() as common.UIAbilityContext;
      KuiklyRenderAdapterManager.krRouterAdapter?.openPage(ctx, pageName, pageData);
    } catch (e) {
      KRRenderLog.e(KRRouterModule.MODULE_NAME, `openPage error ${e}`);
    }
  }

  private closePage() {
    KuiklyRenderAdapterManager.krRouterAdapter?.closePage(getContext() as common.UIAbilityContext);
  }
}
