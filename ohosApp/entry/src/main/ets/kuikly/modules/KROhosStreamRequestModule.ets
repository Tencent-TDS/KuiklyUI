import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';
import { http } from '@kit.NetworkKit';
import { JSON, util } from '@kit.ArkTS';

interface Header {
  'Authorization': string;
  'Content-Type': string;
  'Accept': string;
}

interface CallbackMessage {
  event: string;
  data: string;
}

export class KROhosStreamRequestModule extends KuiklyRenderBaseModule {
  // 定义模块名（注册时用到，全局唯一）
  static readonly MODULE_NAME = "KROhosStreamRequestModule";

  // 是否同步模式（同步模式的module运行在kuikly线程，支持同步调用和异步调用； 异步模式的module运行在ui线程，只支持异步调用）
  syncMode(): boolean {
    return true;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case 'request':
        this.request(params as string, callback);
        return null;
    }
    return null;
  }

  private request(paramsStr: string, callback: KuiklyRenderCallback | null) {
    let params = JSON.parse(paramsStr);
    const url = (params as object)?.["url"] as string;
    const model = (params as object)?.["model"] as string;
    const apiKey = (params as object)?.["apiKey"] as string;
    const prompt = (params as object)?.["prompt"] as string;

    const body = JSON.stringify({
      model: model,
      messages: [{ role: "user", content: prompt }],
      stream: true
    });

    const header: Header = {
      'Authorization': `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
      'Accept': 'text/event-stream'
    };

    const httpRequest = http.createHttp();
    const decoder = util.TextDecoder.create('utf-8', { ignoreBOM: true });

    httpRequest.on('dataReceive', (data: ArrayBuffer) => {
      const text = decoder.decodeToString(new Uint8Array(data));
      let info: CallbackMessage = {
        event: 'data',
        data: text
      }
      callback?.(info);
    });

    httpRequest.on('dataEnd', () => {
      let info: CallbackMessage = {
        event: 'end',
        data: ''
      }
      callback?.(info);
      httpRequest.off('dataReceive');
      httpRequest.off('dataEnd');
      httpRequest.destroy();
    });

    let streamInfo: http.HttpRequestOptions = {
      method: http.RequestMethod.POST,
      header: header,
      extraData: body,
      expectDataType: http.HttpDataType.STRING,
      usingCache: false,
      connectTimeout: 60000,
      readTimeout: 60000,
    };

    httpRequest.requestInStream(url, streamInfo).catch((err: Error) => {
      let info: CallbackMessage = {
        event: 'error',
        data: err.message
      }
      callback?.(info);
      httpRequest.off('dataReceive');
      httpRequest.off('dataEnd');
      httpRequest.destroy();
    });
  }

}
