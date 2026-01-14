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

import { KRAny, KRArray, KRRecord, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';

export class KRMyLogModule extends KuiklyRenderBaseModule {
  // 定义模块名（注册时用到，全局唯一）
  static readonly MODULE_NAME = 'KRMyLogModule';

  // 是否同步模式（同步模式的module运行在kuikly线程，支持同步调用和异步调用； 异步模式的module运行在ui线程，只支持异步调用）
  syncMode(): boolean {
    return true;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    // 分发响应
    switch (method) {
      case 'test':
        console.log(params?.toString());
        // 测试返回复杂 Object 类型
        const res: Record<string, ESObject> = {
          // 嵌套 Object
          "nested": {
            "key1": "value1",
            "key2": "value2"
          },
          // 基础类型
          "string": "中文测试🎉",
          "int": 100,
          "float": 3.14159,
          "negative": -50,
          "boolTrue": true,
          "boolFalse": false,
          // Array
          "intArray": [1, 2, 3],
          "strArray": ["a", "b", "c"],
          // 混合 Array
          "mixedArray": [1, "str", true, {"innerKey": "innerValue"}],
          // 边界情况
          "emptyObj": {},
          "emptyArr": [],
          "emptyStr": "",
          "zero": 0,
          "largeNum": 9999999999
        }
        return res;

      default:
        return null;
    }
  }
}
