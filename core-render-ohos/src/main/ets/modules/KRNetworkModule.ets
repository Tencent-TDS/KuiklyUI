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
import { KRRenderLog } from '../adapter/KRRenderLog';
import { util } from '@kit.ArkTS';

interface ParamsObject {
  url: string;
  method: string;
  param: KRAny | null;
  headers: KRAny | null;
  cookie: string | null;
  timeout: number;
}

interface StreamParamsObject extends ParamsObject {
  requestId: string;
}

interface StreamEventData {
  event: string;
  data: string;
  headers?: string;
  statusCode?: number;
}

export class KRNetworkModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  static readonly MODULE_NAME = 'KRNetworkModule';
  private static readonly METHOD_HTTP_REQUEST = 'httpRequest';
  private static readonly METHOD_HTTP_REQUEST_BINARY = "httpRequestBinary";
  private static readonly METHOD_HTTP_STREAM_REQUEST = 'httpStreamRequest';
  private static readonly METHOD_CLOSE_STREAM_REQUEST = 'closeStreamRequest';
  private static readonly DEFAULT_TIMEOUT_S = 30;
  private static readonly STATE_CODE_UNKNOWN = -1000;
  private activeStreamRequests: Map<string, http.HttpRequest> = new Map();

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
      case KRNetworkModule.METHOD_HTTP_STREAM_REQUEST:
        this.httpStreamRequest(params as string, callback);
        return null;
      case KRNetworkModule.METHOD_CLOSE_STREAM_REQUEST:
        this.closeStreamRequest(params as string);
        return null;
      default:
        return null;
    }
  }

  onDestroy(): void {
    this.activeStreamRequests.forEach((httpReq) => {
      try {
        httpReq.off('dataReceive');
        httpReq.off('dataEnd');
        httpReq.destroy();
      } catch (_) {}
    });
    this.activeStreamRequests.clear();
  }

  private httpStreamRequest(paramsStr: string, callback: KuiklyRenderCallback | null): void {
    try {
      const params: StreamParamsObject = JSON.parse(paramsStr);
      const requestId = params.requestId;

      // requestId 空校验
      if (!requestId) {
        KRRenderLog.e("NetworkModule", "Stream request error: requestId is empty");
        if (callback) {
          const eventData: StreamEventData = {
            event: 'error',
            data: 'requestId is empty',
            statusCode: KRNetworkModule.STATE_CODE_UNKNOWN
          };
          callback(eventData);
        }
        return;
      }

      const urlOptions = this.createURLAndOptions(params, null);
      if (urlOptions == null) {
        return;
      }

      const url = urlOptions[0];
      const options = urlOptions[1];

      const httpRequest = http.createHttp();
      this.activeStreamRequests.set(requestId, httpRequest);

      const decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });
      let isFirstCallback = true;
      let responseHeaders = '';
      let responseCode = 0;

      httpRequest.on('dataReceive', (data: ArrayBuffer) => {
        if (!callback || !this.activeStreamRequests.has(requestId)) return;
        const text = decoder.decodeToString(new Uint8Array(data));
        const eventData: StreamEventData = {
          event: 'data',
          data: text
        };
        if (isFirstCallback) {
          isFirstCallback = false;
          eventData.headers = responseHeaders;
          eventData.statusCode = responseCode;
        }
        callback(eventData);
      });

      httpRequest.on('dataEnd', () => {
        if (!this.activeStreamRequests.has(requestId)) return;
        if (callback) {
          const eventData: StreamEventData = {
            event: 'complete',
            data: ''
          };
          callback(eventData);
        }
        httpRequest.off('dataReceive');
        httpRequest.off('dataEnd');
        httpRequest.destroy();
        this.activeStreamRequests.delete(requestId);
      });

      // 监听 headersReceive 以获取响应头
      httpRequest.on('headersReceive', (header: Object) => {
        try {
          responseHeaders = JSON.stringify(header);
        } catch (_) {
          responseHeaders = '';
        }
      });

      options.expectDataType = http.HttpDataType.STRING;

      httpRequest.requestInStream(url.toString(), options).then((code: number) => {
        responseCode = code;
        if (code >= 200 && code <= 299) {
          // 成功，数据通过 dataReceive/dataEnd 事件回调处理
        } else {
          // HTTP 非2xx 错误码处理
          if (callback && this.activeStreamRequests.has(requestId)) {
            const eventData: StreamEventData = {
              event: 'error',
              data: `HTTP error ${code}`,
              statusCode: code
            };
            callback(eventData);
          }
          httpRequest.off('dataReceive');
          httpRequest.off('dataEnd');
          httpRequest.off('headersReceive');
          httpRequest.destroy();
          this.activeStreamRequests.delete(requestId);
        }
      }).catch((err: Error) => {
        KRRenderLog.e("NetworkModule", `Stream request error: ${err.message}`);
        // 如果是主动关闭导致的异常（requestId 已被 closeStreamRequest 移除），不回调 error
        if (callback && this.activeStreamRequests.has(requestId)) {
          const eventData: StreamEventData = {
            event: 'error',
            data: err.message,
            statusCode: KRNetworkModule.STATE_CODE_UNKNOWN
          };
          callback(eventData);
        }
        httpRequest.off('dataReceive');
        httpRequest.off('dataEnd');
        httpRequest.off('headersReceive');
        httpRequest.destroy();
        this.activeStreamRequests.delete(requestId);
      });
    } catch (e) {
      const errmsg = `stream request error: ${(e as BusinessError).message}`;
      KRRenderLog.e("NetworkModule", errmsg);
      if (callback) {
        const eventData: StreamEventData = {
          event: 'error',
          data: errmsg,
          statusCode: KRNetworkModule.STATE_CODE_UNKNOWN
        };
        callback(eventData);
      }
    }
  }

  private closeStreamRequest(paramsStr: string): void {
    try {
      const params = JSON.parse(paramsStr) as Record<string, string>;
      const requestId = params['requestId'];
      if (requestId && this.activeStreamRequests.has(requestId)) {
        const httpReq = this.activeStreamRequests.get(requestId);
        if (httpReq) {
          httpReq.off('dataReceive');
          httpReq.off('dataEnd');
          httpReq.destroy();
        }
        this.activeStreamRequests.delete(requestId);
      }
    } catch (e) {
      KRRenderLog.e("NetworkModule", `close stream error: ${(e as BusinessError).message}`);
    }
  }

  private httpRequest(params: KRAny, bytes: Int8Array | null, callback: KuiklyRenderCallback | null): KRAny {
    const binaryMode = bytes != null;
    try {
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
    } catch (e) {
      if (callback) {
        const errmsg = `${(e as BusinessError).message}. params: ${params}`;
        KRRenderLog.e("NetworkModule", errmsg);
        setTimeout(() => {
          let resultObject: Record<string, string> = {};
          resultObject['errorMsg'] = errmsg;
          resultObject['success'] = "0";
          callback(resultObject);
        });
      }
    }
    return null;
  }

  createURLAndOptions(params: ParamsObject, bytes: Int8Array | null): [Url.URL, http.HttpRequestOptions] | null {
    if (params == null) {
      return null;
    }

    let url = Url.URL.parseURL(params.url);

    let options: http.HttpRequestOptions = {};

    // 对于二进制请求,需要指定 expectDataType 为 ARRAY_BUFFER
    if (bytes !== null) {
      options.expectDataType = http.HttpDataType.ARRAY_BUFFER;
    }

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
    const timeoutS = params.timeout > 0 ? params.timeout : KRNetworkModule.DEFAULT_TIMEOUT_S;
    options.readTimeout = timeoutS * 1000;
    options.connectTimeout = timeoutS * 1000;

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