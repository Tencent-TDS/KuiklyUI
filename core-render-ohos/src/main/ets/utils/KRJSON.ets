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
import { KRAny, KRJsonValue, KRRecord } from './KRDataTypes';

const KR_JSON_TYPE_ARRAY = 4;
const KR_JSON_TYPE_OBJECT = 5;

/**
 * Normalizes historical JSON-text parameters and internal KRJsonValue
 * parameters at ArkTS consumption boundaries.
 *
 * New internal code should use [get] / [at] whenever it only needs selected
 * fields. [asRecord] / [asArray] are compatibility escape hatches for legacy
 * consumers and system APIs that require a normal ArkTS container.
 */
export class KRJSON {
  static isNative(value: KRAny | undefined): boolean {
    return typeof value === 'object' && value !== null &&
      render.isKRJSON(value as object);
  }

  static get(value: KRAny | undefined, key: string): KRAny | undefined {
    if (KRJSON.isNative(value)) {
      return (value as KRJsonValue).get(key) as KRAny | undefined;
    }
    if (typeof value === 'string') {
      return (JSON.parse(value) as KRRecord)[key] as KRAny | undefined;
    }
    return (value as KRRecord | undefined)?.[key] as KRAny | undefined;
  }

  static at(value: KRAny | undefined, index: number): KRAny | undefined {
    if (KRJSON.isNative(value)) {
      return (value as KRJsonValue).at(index) as KRAny | undefined;
    }
    if (typeof value === 'string') {
      return (JSON.parse(value) as Array<KRAny>)[index];
    }
    return (value as Array<KRAny> | undefined)?.[index];
  }

  static asJSONString(value: KRAny | undefined): string {
    if (typeof value === 'string') {
      return value;
    }
    if (KRJSON.isNative(value)) {
      return (value as KRJsonValue).toJSONString();
    }
    return JSON.stringify(value ?? {});
  }

  static asRecord(value: KRAny | undefined): KRRecord {
    if (KRJSON.isNative(value) || typeof value === 'string') {
      return JSON.parse(KRJSON.asJSONString(value)) as KRRecord;
    }
    return (value ?? {}) as KRRecord;
  }

  static asArray(value: KRAny | undefined): Array<KRAny> {
    if (KRJSON.isNative(value) || typeof value === 'string') {
      const parsed = JSON.parse(KRJSON.asJSONString(value)) as Array<KRAny>;
      return Array.isArray(parsed) ? parsed as Array<KRAny> : [];
    }
    return Array.isArray(value) ? value as Array<KRAny> : [];
  }

  /**
   * Converts a KRJsonValue back to the shape modules/views received before
   * KRJsonValue existed: objects become JSON text, arrays become ArkTS arrays
   * (element-wise, bytes stay Int8Array). Used for receivers that do not
   * override `acceptsKRJSON()`.
   */
  static toLegacy(value: KRAny | undefined): KRAny {
    if (!KRJSON.isNative(value)) {
      return (value ?? null) as KRAny;
    }
    const json = value as KRJsonValue;
    if (json.type === KR_JSON_TYPE_ARRAY) {
      const result: Array<KRAny> = [];
      for (let i = 0; i < json.size; i++) {
        result.push(KRJSON.toLegacy(json.at(i) as KRAny | undefined));
      }
      return result;
    }
    if (json.type === KR_JSON_TYPE_OBJECT) {
      return json.toJSONString();
    }
    return null;
  }
}
