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

import { KRNetworkCacheUtil } from '../utils/KRNetworkCacheUtil';
import { KuiklyRenderBaseModule } from './base/KRBaseModule';
import { KRAny, KRRecord, KuiklyRenderCallback } from './../utils/KRDataTypes';
import { http } from '@kit.NetworkKit';
import { BusinessError } from '@kit.BasicServicesKit';
import Url from '@ohos.url';
import buffer from '@ohos.buffer';

interface ParamsObject {
  url: string;
  method: string;
  param: KRAny | null;
  headers: KRAny | null;
  cookie: string | null;
  timeout: number;
}

export class KRNetworkModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  static readonly MODULE_NAME = 'KRNetworkModule';
  private static readonly METHOD_HTTP_REQUEST = 'httpRequest';
  private static readonly METHOD_HTTP_REQUEST_BINARY = "httpRequestBinary";

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case 'FetchFileByDownloadOrCache':
        let context = this.controller?.getUIAbilityContext();
        if (context) {
          KRNetworkCacheUtil.fetchFileByDownloadOrCache(context, params as string, callback as KuiklyRenderCallback);
        }
        return null;
      case KRNetworkModule.METHOD_HTTP_REQUEST:
        return this.httpRequest(params, null, callback);
      case KRNetworkModule.METHOD_HTTP_REQUEST_BINARY:
        const args = params as Array<KRAny>;
        return this.httpRequest(args[0], args[1] as Int8Array, callback);
      default:
        return null;
    }
  }

  onDestroy(): void {
  }

  private httpRequest(params: KRAny, bytes: Int8Array | null, callback: KuiklyRenderCallback | null): KRAny {
    const binaryMode = bytes != null;
    let paramsObj: ParamsObject = JSON.parse(params as string);

    let urlOptions = this.createURLAndOptions(paramsObj, bytes);
    if (urlOptions == null) {
      return null;
    }

    let url = urlOptions[0];
    let options = urlOptions[1];

    let httpRequest = http.createHttp();
    httpRequest.request(url.toString(), options, (err: BusinessError, response: http.HttpResponse) => {
      if (callback) {
        let resultObject: Record<string, string> = {};
        let bytes: Int8Array | null = null;

        if (response && response.header) {
          resultObject['headers'] = JSON.stringify(response.header);
        } else {
          resultObject['headers'] = '';
        }

        if (err) {
          resultObject['data'] = '';
          resultObject['errorMsg'] = JSON.stringify(err);
          resultObject['success'] = '0';
        } else {
          if (binaryMode) {
            if (response.result instanceof ArrayBuffer) {
              bytes = new Int8Array(response.result);
            } else if (typeof response.result == 'string') {
              bytes = new Int8Array(buffer.from(response.result).buffer);
            }
          } else {
            resultObject['data'] = response.result.toString();
          }
          resultObject['errorMsg'] = '';
          resultObject['success'] = '1';
        }

        if (binaryMode) {
          callback([JSON.stringify(resultObject), bytes ?? new Int8Array(0)]);
        } else {
          callback(resultObject);
        }
      }

      httpRequest.destroy();
    });

    return null;
  }

  createURLAndOptions(params: ParamsObject, bytes: Int8Array | null): [Url.URL, http.HttpRequestOptions] | null {
    if (params == null) {
      return null;
    }

    let url = Url.URL.parseURL(params.url);
    console.log(`url parsed:${url}, url passed in :${params.url}`);

    let options: http.HttpRequestOptions = {};

    // set request method, url query parameters, body
    if (params.method == 'GET') {
      options.method = http.RequestMethod.GET;

      // append param to the url if there's any
      if (params.param) {
        let queryParam = new Url.URLParams(params.param as Record<string, string>);
        url.params.forEach((value, name, searchParams) => {
          queryParam.append(name, value);
        });
        url.search = queryParam.toString();
      }
    } else if (params.method == 'POST') {
      options.method = http.RequestMethod.POST;

      // set post body with params.param
      if (bytes != null && bytes.length > 0) {
        options.extraData = bytes.buffer;
      } else if (params.param != null) {
        if (this.isContentTypeJson(params.headers)) {
          // json body
          options.extraData = params.param;
        } else {
          let kvParams = new Url.URLParams();
          let param = params.param as KRRecord;
          Object.keys(param).forEach((k) => {
            let v: string = param[k] as string;
            kvParams.append(k, v);
          });
          options.extraData = kvParams.toString();
        }
      }
    }

    // set reqeuset headers
    if (params.headers) {
      let headers: Record<string, string> = {};
      let originalHeaders = params.headers as KRRecord;
      Object.keys(originalHeaders).forEach((key) => {
        let value: string = originalHeaders[key] as string;
        headers[key] = value;
      });

      // update cookie
      if (params.cookie) {
        headers['Cookie'] = params.cookie;
      }

      options.header = headers;
    }

    // set timeout
    options.readTimeout = params.timeout * 1000;

    //options.usingProxy = true;

    return [url, options];
  }

  private isContentTypeJson(headers: KRAny | null): boolean {
    if (!headers) {
      return false;
    }
    const headersRecord = headers as Record<string, string>;
    const contentType = headersRecord['content-type'] ?? headersRecord['Content-Type'];
    return contentType?.indexOf('/json') > 0;
  }
}