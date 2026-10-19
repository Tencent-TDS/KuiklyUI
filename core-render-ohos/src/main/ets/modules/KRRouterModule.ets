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

import { KRAny, KRJsonValue, KRRecord, KuiklyRenderCallback } from '../utils/KRDataTypes';
import { KuiklyRenderAdapterManager } from '../adapter/KRAdapterManager';
import { KuiklyRenderBaseModule } from './base/KRBaseModule';
import { KRRenderLog } from '../adapter/KRRenderLog';
import { KRJSON } from '../utils/KRJSON';
import { common } from '@kit.AbilityKit';
import render from 'libkuikly.so';

export class KRRouterModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  acceptsKRJSON(): boolean {
    return true;
  }

  static readonly MODULE_NAME = 'KRRouterModule';
  private static readonly OPEN_PAGE = 'openPage';
  private static readonly CLOSE_PAGE = 'closePage';

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRRouterModule.OPEN_PAGE:
        this.openPage(params);
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

  // C++ containers now arrive as KRJsonValue. Historical JSON text remains
  // supported and is materialized only for that legacy input path.
  private openPage(params: KRAny) {
    try {
      let nativeParams: KRJsonValue | undefined =
        KRJSON.isNative(params) ? params as KRJsonValue : undefined;
      // A JSON-text envelope is parsed once in C++ so pageData stays a KRJsonValue.
      if (nativeParams === undefined && typeof params === 'string') {
        nativeParams = render.parseKRJson(params) as KRJsonValue;
      }
      const legacyParams: KRRecord | undefined =
        nativeParams === undefined ? KRJSON.asRecord(params) : undefined;
      const pageName = (nativeParams?.get('pageName') ??
        legacyParams?.['pageName']) as string;
      const pageData = (nativeParams?.get('pageData') ??
        legacyParams?.['pageData']) as KRAny | undefined;
      const ctx = getContext() as common.UIAbilityContext;
      const adapter = KuiklyRenderAdapterManager.krRouterAdapter;
      if (KRJSON.isNative(pageData) &&
          adapter?.openPageWithKRJson !== undefined) {
        adapter.openPageWithKRJson(ctx, pageName, pageData as KRJsonValue);
      } else {
        adapter?.openPage(ctx, pageName, KRJSON.asRecord(pageData));
      }
    } catch (e) {
      KRRenderLog.e(KRRouterModule.MODULE_NAME, `openPage error ${e}`);
    }
  }

  private closePage() {
    KuiklyRenderAdapterManager.krRouterAdapter?.closePage(getContext() as common.UIAbilityContext);
  }
}
