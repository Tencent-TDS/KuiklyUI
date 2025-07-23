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

import { KuiklyRenderAdapterManager } from '../adapter/KRAdapterManager';

export class KRColorParser {
  /**
   * Convert color from string
   * @param colorStr
   * @returns color object of type string and number should conform to ResourceColor [https://developer.huawei.com/consumer/cn/doc/harmonyos-references/ts-types-0000001774121374#ZH-CN_TOPIC_0000001774121374__resourcecolor]
   */
  public static toColor(colorString: string, castToString: boolean = false): ResourceColor {
    const result: ResourceColor = KuiklyRenderAdapterManager.krColorParserAdapter?.toColor(colorString) ?? KRColorParser.toColorInternal(colorString);
    if (castToString) {
      if (!isNaN(result as number)) {
        return '#' + result.toString(16).padStart(6, '0');
      }
      return result.toString();
    }
    return result;
  }

  private static toColorInternal(colorString: string): ResourceColor {
    const rgba = parseInt(colorString);
    const a = ((rgba >> 24) & 0xFF);
    if (a == 0xFF) {
      return rgba & 0xFFFFFF;
    }
    const r = ((rgba >> 16) & 0xFF);
    const g = ((rgba >> 8) & 0xFF);
    const b = ((rgba) & 0xFF);
    return `rgba(${r},${g},${b},${a / 255.0})`;
  }

}
