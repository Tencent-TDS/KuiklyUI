/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

import render from 'libkuikly.so';
import { KRJsonValue, KRPageData, KRRecord } from './KRDataTypes';
import { KRRenderLog } from '../adapter/KRRenderLog';

const KR_JSON_ROUTE_PAYLOAD_TOKEN_KEY = '__kuiklyKRJsonPayloadId';

/**
 * Resolves router pageData for a Kuikly route target.
 *
 * KRJsonValue serializes itself to a small router-safe token. This helper is
 * the only ArkTS-side place that understands that transport representation;
 * route hosts receive ordinary [KRPageData] without handling N-API registry
 * details themselves.
 */
export class KRRouterParams {
  static resolvePageData(params: Record<string, Object> | null | undefined): KRPageData {
    if (params === null || params === undefined) {
      return {} as KRRecord;
    }
    const pageData = params['pageData'] as KRRecord | undefined;
    const tokenCarrier = pageData as Record<string, string> | undefined;
    const token = tokenCarrier?.[KR_JSON_ROUTE_PAYLOAD_TOKEN_KEY];
    if (token !== undefined) {
      const nativePayload = render.takeKRJsonRoutePayload(token) as KRJsonValue | undefined;
      if (nativePayload !== undefined) {
        return nativePayload;
      }
      // Expired or already consumed: the carrier holds only the token, never page data.
      KRRenderLog.e('KRRouterParams', `route payload ${token} is unavailable, using empty pageData`);
      return {} as KRRecord;
    }
    return pageData ?? ({} as KRRecord);
  }
}
