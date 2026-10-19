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
import { KRJSON } from '../utils/KRJSON';

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

interface StreamRequestState {
  httpRequest: http.HttpRequest;
  idleTimer: number;
  callback: KuiklyRenderCallback | null;
}

export class KRNetworkModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  acceptsKRJSON(): boolean {
    return true;
  }

  static readonly MODULE_NAME = 'KRNetworkModule';
  private static readonly METHOD_HTTP_REQUEST = 'httpRequest';
  private static readonly METHOD_HTTP_REQUEST_BINARY = "httpRequestBinary";
  private static readonly METHOD_HTTP_STREAM_REQUEST = 'httpStreamRequest';
  private static readonly METHOD_CLOSE_STREAM_REQUEST = 'closeStreamRequest';
  private static readonly DEFAULT_TIMEOUT_S = 30;
  private static readonly STATE_CODE_UNKNOWN = -1000;
  private static readonly STATE_CODE_TIMEOUT = -1001;
  // @ohos.net.http "Operation timeout"
  private static readonly HTTP_ERROR_CODE_TIMEOUT = 2300028;
  // readTimeout maps to CURLOPT_TIMEOUT_MS, a total-duration limit. Streams use a module-managed
  // idle timer instead, so the native limit is pushed out of reach (must stay within uint32).
  private static readonly STREAM_NATIVE_READ_TIMEOUT_MS = 0x7FFFFFFF;
  private static readonly CONTENT_TYPE_FORM = 'application/x-www-form-urlencoded; charset=UTF-8';
  private activeStreamRequests: Map<string, StreamRequestState> = new Map();

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case 'FetchFileByDownloadOrCache':
        let context = this.controller?.getUIAbilityContext();
        if (context) {
          KRNetworkCacheUtil.fetchFileByDownloadOrCache(context, params as string, callback as KuiklyRenderCallback);
        } else if (callback) {
          callback(null);
        }
        return null;
      case KRNetworkModule.METHOD_HTTP_REQUEST:
        return this.httpRequest(params, null, callback);
      case KRNetworkModule.METHOD_HTTP_REQUEST_BINARY:
        return this.httpRequest(KRJSON.at(params, 0) as KRAny,
          (KRJSON.at(params, 1) as Int8Array | undefined) ?? null, callback);
      case KRNetworkModule.METHOD_HTTP_STREAM_REQUEST:
        this.httpStreamRequest(params, callback);
        return null;
      case KRNetworkModule.METHOD_CLOSE_STREAM_REQUEST:
        this.closeStreamRequest(params);
        return null;
      default:
        return null;
    }
  }

  onDestroy(): void {
    this.activeStreamRequests.forEach((state) => {
      clearTimeout(state.idleTimer);
      this.cleanupHttpRequest(state.httpRequest);
    });
    this.activeStreamRequests.clear();
  }

  /**
   * 统一清理 HttpRequest 资源：注销事件监听并销毁请求。
   * off/destroy 均可能抛异常，这里全部 try-catch 兜底，异常仅记录日志不再抛出，
   */
  private cleanupHttpRequest(httpReq: http.HttpRequest): void {
    try {
      httpReq.off('dataReceive');
    } catch (e) {
      KRRenderLog.e("NetworkModule", `off dataReceive error: ${(e as BusinessError).message}`);
    }
    try {
      httpReq.off('dataEnd');
    } catch (e) {
      KRRenderLog.e("NetworkModule", `off dataEnd error: ${(e as BusinessError).message}`);
    }
    try {
      httpReq.off('headersReceive');
    } catch (e) {
      KRRenderLog.e("NetworkModule", `off headersReceive error: ${(e as BusinessError).message}`);
    }
    try {
      httpReq.destroy();
    } catch (e) {
      KRRenderLog.e("NetworkModule", `destroy httpRequest error: ${(e as BusinessError).message}`);
    }
  }

  /**
   * 清理指定 requestId 对应的流式请求，包含停止空闲计时、事件反注册、销毁与 Map 移除。
   * 返回 false 表示该请求已被关闭或已发出终态。
   */
  private finalizeStreamRequest(requestId: string): boolean {
    const state = this.activeStreamRequests.get(requestId);
    if (!state) {
      return false;
    }
    this.activeStreamRequests.delete(requestId);
    clearTimeout(state.idleTimer);
    this.cleanupHttpRequest(state.httpRequest);
    return true;
  }

  /**
   * 流式请求唯一的终态入口：先完成状态转换与资源清理，再通知业务，
   * 保证每个请求最多发出一次 complete/error，且业务回调重入时不会看到中间状态。
   */
  private finishStreamRequest(requestId: string, callback: KuiklyRenderCallback | null,
    events: StreamEventData[]): void {
    if (!this.finalizeStreamRequest(requestId) || !callback) {
      return;
    }
    for (let i = 0; i < events.length; i++) {
      callback(events[i]);
    }
  }

  private static streamEvent(event: string, data: string, statusCode?: number, headers?: string): StreamEventData {
    const eventData: StreamEventData = { event: event, data: data };
    if (statusCode !== undefined) {
      eventData.statusCode = statusCode;
    }
    if (headers !== undefined) {
      eventData.headers = headers;
    }
    return eventData;
  }

  private httpStreamRequest(rawParams: KRAny, callback: KuiklyRenderCallback | null): void {
    let requestId = '';
    let httpRequest: http.HttpRequest | null = null;
    try {
      const params = this.parseStreamParams(rawParams);
      requestId = params.requestId;

      // requestId 空校验
      if (!requestId) {
        KRRenderLog.e("NetworkModule", "Stream request error: requestId is empty");
        if (callback) {
          callback(KRNetworkModule.streamEvent('error', 'requestId is empty', KRNetworkModule.STATE_CODE_UNKNOWN));
        }
        return;
      }

      // 防御性判断：requestId 必须唯一，避免覆盖已有的流式请求导致句柄丢失
      if (this.activeStreamRequests.has(requestId)) {
        KRRenderLog.e("NetworkModule", `Stream request error: duplicate requestId=${requestId}`);
        if (callback) {
          callback(KRNetworkModule.streamEvent('error', 'duplicate requestId', KRNetworkModule.STATE_CODE_UNKNOWN));
        }
        return;
      }

      // GET 查询参数由 applyStreamRequestOptions 按流式请求的取值规则拼接
      const baseParams: ParamsObject = {
        url: params.url,
        method: params.method,
        param: params.method == 'GET' ? null : params.param,
        headers: params.headers,
        cookie: params.cookie,
        timeout: params.timeout
      };
      const urlOptions = this.createURLAndOptions(baseParams, null);
      if (urlOptions == null) {
        if (callback) {
          callback(KRNetworkModule.streamEvent('error', 'invalid request params', KRNetworkModule.STATE_CODE_UNKNOWN));
        }
        return;
      }

      const url = urlOptions[0];
      const options = urlOptions[1];
      this.applyStreamRequestOptions(params, url, options);
      const idleTimeoutMs = (params.timeout > 0 ? params.timeout : KRNetworkModule.DEFAULT_TIMEOUT_S) * 1000;

      const currentRequest = http.createHttp();
      httpRequest = currentRequest;
      const state: StreamRequestState = { httpRequest: currentRequest, idleTimer: -1, callback: callback };
      this.activeStreamRequests.set(requestId, state);

      // 与 Android/iOS 一致：timeout 表示“连续无数据”的空闲超时，收到数据即重新计时，
      // 不限制持续推送的总时长。
      const armIdleTimer = () => {
        clearTimeout(state.idleTimer);
        state.idleTimer = setTimeout(() => {
          this.finishStreamRequest(requestId, callback, [
            KRNetworkModule.streamEvent('error', 'request timeout', KRNetworkModule.STATE_CODE_TIMEOUT)
          ]);
        }, idleTimeoutMs);
      };
      armIdleTimer();

      const decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });
      let isFirstCallback = true;
      let responseHeaders = '';

      // 监听 headersReceive 以获取响应头
      currentRequest.on('headersReceive', (header: Object) => {
        if (!this.activeStreamRequests.has(requestId)) return;
        armIdleTimer();
        try {
          responseHeaders = JSON.stringify(header);
        } catch (_) {
          responseHeaders = '';
        }
      });

      currentRequest.on('dataReceive', (data: ArrayBuffer) => {
        // 若 requestId 已从 activeStreamRequests 中移除，说明请求已被主动关闭、已超时或已发出终态，
        // 此时静默丢弃后续 chunk 即可：native 侧已入队的事件可能在 off 之前就 post 到了 ArkTS 事件循环。
        if (!this.activeStreamRequests.has(requestId)) return;
        armIdleTimer();
        if (!callback) return;
        // Keep an incomplete UTF-8 suffix inside TextDecoder until the next chunk.
        const text = decoder.decodeToString(new Uint8Array(data), { stream: true });
        if (!text) return;
        const eventData = KRNetworkModule.streamEvent('data', text);
        if (isFirstCallback) {
          isFirstCallback = false;
          // requestInStream 只有在请求结束时才给出状态码，headersReceive 也不承诺包含状态码，
          // 因此首帧只携带响应头，不把未知状态码当作 0 暴露给业务。
          eventData.headers = responseHeaders;
        }
        callback(eventData);
      });

      // 不监听 dataEnd 来决定终态：OpenHarmony 先派发 dataEnd 再 resolve requestInStream，
      // 若在 dataEnd 里发 complete，401/500 等错误响应会被误报为成功。Promise 结果是终态的唯一来源。
      options.expectDataType = http.HttpDataType.STRING;

      currentRequest.requestInStream(url.toString(), options).then((code: number) => {
        if (code >= 200 && code <= 299) {
          const events: StreamEventData[] = [];
          const tail = decoder.decodeToString(new Uint8Array(0), { stream: false });
          if (tail) {
            events.push(KRNetworkModule.streamEvent('data', tail, undefined, isFirstCallback ? responseHeaders : undefined));
          }
          events.push(KRNetworkModule.streamEvent('complete', ''));
          this.finishStreamRequest(requestId, callback, events);
        } else {
          // 非 2xx 响应（含 3xx / 4xx / 5xx）统一按错误处理。
          // 关于 3xx：HarmonyOS http 底层（libcurl）默认会自动跟随重定向，业务侧通常只会看到最终响应码；
          // 但 requestInStream 未开放 FOLLOWLOCATION 选项，若底层未跟随（如跨协议 http→https 被拒、
          // 或 Location 头缺失），3xx 会直接透传到这里——此时已无法继续流式读取，
          // 作为 error 回调给业务侧是合适的。
          this.finishStreamRequest(requestId, callback, [
            KRNetworkModule.streamEvent('error', `HTTP error ${code}`, code, responseHeaders)
          ]);
        }
      }).catch((err: BusinessError) => {
        KRRenderLog.e("NetworkModule", `Stream request error: ${err.code} ${err.message}`);
        // 主动关闭导致的异常（requestId 已被移除）由 finishStreamRequest 静默忽略
        const isTimeout = err.code == KRNetworkModule.HTTP_ERROR_CODE_TIMEOUT;
        this.finishStreamRequest(requestId, callback, [
          isTimeout ?
            KRNetworkModule.streamEvent('error', 'request timeout', KRNetworkModule.STATE_CODE_TIMEOUT) :
            KRNetworkModule.streamEvent('error', err.message, KRNetworkModule.STATE_CODE_UNKNOWN)
        ]);
      });
    } catch (e) {
      const errmsg = `stream request error: ${(e as BusinessError).message}`;
      KRRenderLog.e("NetworkModule", errmsg);
      // 兜底清理，两个分支互斥、对应 try 块中不同阶段抛出异常的场景：
      //   1. 已 set 进 activeStreamRequests（如 on() 注册事件监听时抛异常）
      //      → 走 finalizeStreamRequest 统一路径：反注册事件 + destroy + delete。
      //   2. httpRequest 已创建但尚未 set 进 map 的中间状态
      //      （如 http.createHttp() 之后、activeStreamRequests.set() 之前抛异常，窗口极窄）
      //      → 此时 map 里没有条目，只需 cleanupHttpRequest 释放句柄，避免泄漏。
      // 分支互斥：命中分支 1 时 httpRequest 指向的对象已被 finalizeStreamRequest 清理，
      // 因此 else if 不会（也不应该）再重复清理。
      const finalized = requestId ? this.finalizeStreamRequest(requestId) : false;
      if (!finalized && httpRequest) {
        this.cleanupHttpRequest(httpRequest);
      }
      if (callback) {
        callback(KRNetworkModule.streamEvent('error', errmsg, KRNetworkModule.STATE_CODE_UNKNOWN));
      }
    }
  }

  /**
   * 流式请求的 body 与超时契约（Android / iOS / 鸿蒙一致）：
   * - Content-Type 含 json 时发送 JSON；否则按 UTF-8 application/x-www-form-urlencoded 编码，
   *   未指定 Content-Type 时显式补齐（鸿蒙默认 content-type 为 application/json，必须覆盖）。
   * - GET 参数追加到 URL 已有查询参数之后；参数值的转换规则与表单 body 相同（见 toFormValue）。
   * - 空闲超时由模块自行计时，native readTimeout 只作兜底。
   * - 不使用 http 缓存：缓存命中时 requestInStream 只返回状态码，正文不经过 dataReceive。
   */
  private applyStreamRequestOptions(params: StreamParamsObject, url: Url.URL, options: http.HttpRequestOptions): void {
    options.readTimeout = KRNetworkModule.STREAM_NATIVE_READ_TIMEOUT_MS;
    options.usingCache = false;
    if (params.param == null) {
      return;
    }
    if (params.method == 'GET') {
      const queryParams = new Url.URLParams(url.search);
      const query = params.param as KRRecord;
      Object.keys(query).forEach((key) => {
        queryParams.append(key, KRNetworkModule.toFormValue(query[key]));
      });
      url.search = queryParams.toString();
      return;
    }
    if (params.method != 'POST') {
      return;
    }
    const headers: Record<string, string> = {};
    if (options.header) {
      const original = options.header as Record<string, string>;
      Object.keys(original).forEach((key) => {
        headers[key] = original[key];
      });
    }
    const contentTypeKey = Object.keys(headers).find((key) => key.toLowerCase() == 'content-type');
    const contentType = contentTypeKey ? headers[contentTypeKey] : undefined;
    if (contentType && contentType.toLowerCase().indexOf('json') >= 0) {
      options.extraData = JSON.stringify(params.param);
    } else {
      const formParams = new Url.URLParams();
      const record = params.param as KRRecord;
      Object.keys(record).forEach((key) => {
        formParams.append(key, KRNetworkModule.toFormValue(record[key]));
      });
      options.extraData = formParams.toString();
      if (!contentType) {
        headers['Content-Type'] = KRNetworkModule.CONTENT_TYPE_FORM;
      }
    }
    options.header = headers;
  }

  /**
   * 表单 / GET 参数值转换（三端一致）：null 编码为空值（key=），boolean 为 true/false，
   * 对象与数组为 JSON 字符串。
   */
  private static toFormValue(value: KRAny | undefined): string {
    if (value === null || value === undefined) {
      return '';
    }
    if (typeof value === 'string') {
      return value as string;
    }
    if (typeof value === 'object') {
      return JSON.stringify(value);
    }
    return value.toString();
  }

  private closeStreamRequest(params: KRAny): void {
    try {
      const requestId = KRJSON.get(params, 'requestId');
      if (typeof requestId === 'string' && requestId.length > 0) {
        this.finalizeStreamRequest(requestId);
      }
    } catch (e) {
      KRRenderLog.e("NetworkModule", `close stream error: ${(e as BusinessError).message}`);
    }
  }

  private httpRequest(params: KRAny, bytes: Int8Array | null, callback: KuiklyRenderCallback | null): KRAny {
    const binaryMode = bytes != null;
    try {
      let paramsObj = this.parseRequestParams(params);

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
            resultObject['statusCode'] = response ? response.responseCode.toString() : '-1000';
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
            resultObject['statusCode'] = response.responseCode.toString();
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
          resultObject['statusCode'] = '-1000';
          callback(resultObject);
        });
      }
    }
    return null;
  }

  private parseRequestParams(params: KRAny): ParamsObject {
    const source = KRJSON.asRecord(params);
    return {
      url: source['url'] as string,
      method: source['method'] as string,
      param: (source['param'] as KRAny | undefined) ?? null,
      headers: (source['headers'] as KRAny | undefined) ?? null,
      cookie: (source['cookie'] as string | undefined) ?? null,
      timeout: source['timeout'] as number,
    };
  }

  private parseStreamParams(params: KRAny): StreamParamsObject {
    const base = this.parseRequestParams(params);
    const requestId = KRJSON.get(params, 'requestId');
    return {
      url: base.url,
      method: base.method,
      param: base.param,
      headers: base.headers,
      cookie: base.cookie,
      timeout: base.timeout,
      requestId: typeof requestId === 'string' ? requestId : '',
    };
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

    // set reqeuset headers; cookie must be applied even when no custom headers are given
    if (params.headers || params.cookie) {
      let headers: Record<string, string> = {};
      if (params.headers) {
        let originalHeaders = params.headers as KRRecord;
        Object.keys(originalHeaders).forEach((key) => {
          let value: string = originalHeaders[key] as string;
          headers[key] = value;
        });
      }

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
