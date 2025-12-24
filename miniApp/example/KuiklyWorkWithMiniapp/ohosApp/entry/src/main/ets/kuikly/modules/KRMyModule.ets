import { KRAny, KRRecord, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';

export class KRMyModule extends KuiklyRenderBaseModule {
  // 定义模块名（注册时用到，全局唯一）
  static readonly MODULE_NAME = "KRMyModule";

  // 是否同步模式（同步模式的module运行在kuikly线程，支持同步调用和异步调用； 异步模式的module运行在ui线程，只支持异步调用）
  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    // 分发响应
    switch (method) {
      case 'myFun1':
      // 接收JSON字符串，返回JSON对象
        const json: Record<string, string> = JSON.parse(params as string);
        return json["data"];
      case 'myFun2':
      // 接收基本类型数组，返回基本类型
        const array = params as Object[];
        const bytes = array[0] as Int8Array;
        const tmp1 = bytes[0];
        const tmp2 = bytes[1];
        bytes[0] = bytes[3];
        bytes[1] = bytes[2];
        bytes[2] = tmp2;
        bytes[3] = tmp1;
        return bytes;
      case 'myFun3':
      // 接收JSON字符串，回调JSON对象
        const json2: Record<string, string> = JSON.parse(params as string);
        const content = json2["data"];
        callback?.(`{"content": "${content}"}`);
        return null;
      case 'myFun4':
      // 接收基本类型数组，回调基本类型
        const array2 = params as Object[];
        callback?.(array2[0] as string);
        return null;
      case 'aTestMethod':
        console.log("aTestMethod called")
        const x = new Int8Array([33, 34, 35]);
        const ret : KRRecord = {'a': x, 's': "hello"}
        callback?.(ret);
        return null

      default:
        return null;
    }
  }
}