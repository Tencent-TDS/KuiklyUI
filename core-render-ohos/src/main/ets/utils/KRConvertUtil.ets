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


import { KRSize } from '../foundation/KRSize';
import display from '@ohos.display';
import { KRAny, KRArray, KRRecord, KRValue } from './KRDataTypes';


export class KRConvertUtil {
  /**
   * 将任意 KRAny 类型的值转换为 native 侧能接收类型的值
   * @param value 要转换的 KRAny 类型的值。
   * @returns 转换后的 native侧能接收 类型的值。
   */
  public static anyToCObject(value: KRAny): KRValue | null {
    if (value === null) {
      return null
    }
    if (Array.isArray(value)) {
      if (KRConvertUtil.hasByteArrayElement(value as KRArray)) {
        return value as KRArray
      } else {
        return JSON.stringify(value);
      }
    } else if (value instanceof ArrayBuffer) {
      return value;
    } else if (typeof value === 'object') {
      return JSON.stringify(value);
    } else {
      return value as KRValue;
    }
  }


  /**
   * 获取屏幕size
   */
  public static screenSize(): KRSize {
    let width = 0
    let height = 0
    try {
      const displayRes = display.getDefaultDisplaySync()
      width = displayRes.width
      height = displayRes.height
    } catch (e) {
    }
    return new KRSize(px2vp(width) , px2vp(height))
  }

  public static toKRRecord(value: KRAny): KRRecord {
    return JSON.parse(value as string) as KRRecord
  }

  private static hasByteArrayElement(array: KRArray): boolean {
    let has = false;
    array.forEach((item) => {
      if (item instanceof ArrayBuffer) {
        has = true
        return
      }
      if (item instanceof Int8Array){
        has = true
        return
      }
    })
    return has
  }


}
