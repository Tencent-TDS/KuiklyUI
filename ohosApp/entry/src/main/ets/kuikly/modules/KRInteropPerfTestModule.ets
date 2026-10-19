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

import {
  KRAny,
  KuiklyRenderBaseModule,
  KuiklyRenderCallback
} from '@kuikly-open/render';

const DEFAULT_PAYLOAD_CHARS = 128 * 1024;
const DEFAULT_COUNT = 50;

interface PerfConfig {
  caseName: string;
  count: number;
  payloadChars: number;
}

interface PerfRecord {
  kind: string;
  i: number;
  v: string;
}

export class KRInteropPerfTestModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRInteropPerfTestModule';

  private statsCallback: KuiklyRenderCallback | null = null;
  private plainStringCallbacks = new Map<string, KuiklyRenderCallback | null>();
  private jsonCallbacks = new Map<string, KuiklyRenderCallback | null>();
  private plainStringPayloadCaches = new Map<string, string[]>();
  private jsonStringPayloadCaches = new Map<string, string[]>();
  private krRecordPayloadCaches = new Map<string, PerfRecord[]>();

  syncMode(): boolean {
    return true;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case 'SetStatsCallback':
        this.statsCallback = callback;
        return null;
      case 'SetCallbackWithPlainString':
        this.plainStringCallbacks.set(params as string, callback);
        return null;
      case 'SetCallbackWithJson':
        this.jsonCallbacks.set(params as string, callback);
        return null;
      case 'RunPlainString': {
        const config = this.parseConfig(params);
        const cached = this.plainStringPayloadCaches.get(config.caseName);
        const payloads = this.preparePlainStringPayloads(
          cached ?? [], config.count, config.payloadChars
        );
        this.plainStringPayloadCaches.set(config.caseName, payloads);
        this.runPreparedStrings(
          config.caseName, payloads, this.plainStringCallbacks.get(config.caseName) ?? null
        );
        return null;
      }
      case 'RunJsonString': {
        const config = this.parseConfig(params);
        const cached = this.jsonStringPayloadCaches.get(config.caseName);
        const payloads = this.prepareJsonStringPayloads(
          cached ?? [], config.count, config.payloadChars
        );
        this.jsonStringPayloadCaches.set(config.caseName, payloads);
        this.runPreparedStrings(
          config.caseName, payloads, this.jsonCallbacks.get(config.caseName) ?? null
        );
        return null;
      }
      case 'RunKRRecord': {
        const config = this.parseConfig(params);
        const cached = this.krRecordPayloadCaches.get(config.caseName);
        const payloads = this.prepareKRRecordPayloads(
          cached ?? [], config.count, config.payloadChars
        );
        this.krRecordPayloadCaches.set(config.caseName, payloads);
        this.runPreparedRecords(
          config.caseName, payloads, this.jsonCallbacks.get(config.caseName) ?? null
        );
        return null;
      }
      default:
        return null;
    }
  }

  private parseConfig(params: KRAny): PerfConfig {
    let caseName = '';
    let count = DEFAULT_COUNT;
    let payloadChars = DEFAULT_PAYLOAD_CHARS;
    if (typeof params === 'string' && params.length > 0) {
      const caseMatch = /"case":\s*"([^"]+)"/.exec(params);
      if (caseMatch !== null) {
        caseName = caseMatch[1];
      }
      const countMatch = /"count":\s*(\d+)/.exec(params);
      if (countMatch !== null) {
        count = parseInt(countMatch[1], 10);
      }
      const charsMatch = /"payloadChars":\s*(\d+)/.exec(params);
      if (charsMatch !== null) {
        payloadChars = parseInt(charsMatch[1], 10);
      }
    }
    return { caseName, count, payloadChars };
  }

  private makePad(targetLen: number): string {
    const token = '0123456789abcdef测Abc';
    const repeat = Math.ceil(targetLen / token.length);
    return token.repeat(repeat).substring(0, targetLen);
  }

  private makeJsonString(i: number, targetLen: number): string {
    const record: PerfRecord = { kind: 'json_string', i: i, v: '' };
    const padLen = Math.max(0, targetLen - JSON.stringify(record).length);
    record.v = this.makePad(padLen);
    return JSON.stringify(record);
  }

  private makeKRRecord(i: number, targetLen: number): PerfRecord {
    const record: PerfRecord = { kind: 'kr_record', i: i, v: '' };
    const padLen = Math.max(0, targetLen - JSON.stringify(record).length);
    record.v = this.makePad(padLen);
    return record;
  }

  private preparePlainStringPayloads(
    cached: string[],
    count: number,
    payloadChars: number
  ): string[] {
    if (cached.length === count) {
      return cached;
    }
    const out: string[] = [];
    for (let i = 0; i < count; i++) {
      out.push(this.makePad(payloadChars));
    }
    return out;
  }

  private prepareJsonStringPayloads(
    cached: string[],
    count: number,
    payloadChars: number
  ): string[] {
    if (cached.length === count) {
      return cached;
    }
    const out: string[] = [];
    for (let i = 0; i < count; i++) {
      out.push(this.makeJsonString(i, payloadChars));
    }
    return out;
  }

  private prepareKRRecordPayloads(
    cached: PerfRecord[],
    count: number,
    payloadChars: number
  ): PerfRecord[] {
    if (cached.length === count) {
      return cached;
    }
    const out: PerfRecord[] = [];
    for (let i = 0; i < count; i++) {
      out.push(this.makeKRRecord(i, payloadChars));
    }
    return out;
  }

  private runPreparedStrings(
    caseName: string,
    payloads: string[],
    callback: KuiklyRenderCallback | null
  ): void {
    const count = payloads.length;
    let bytes = 0;
    for (const payload of payloads) {
      bytes += payload.length;
    }
    const start = Date.now();
    for (let i = 0; i < count; i++) {
      callback?.(payloads[i]);
    }
    this.report(caseName, count, Date.now() - start, bytes);
  }

  private runPreparedRecords(
    caseName: string,
    payloads: PerfRecord[],
    callback: KuiklyRenderCallback | null
  ): void {
    const count = payloads.length;
    let bytes = 0;
    for (const payload of payloads) {
      bytes += JSON.stringify(payload).length;
    }
    const start = Date.now();
    for (let i = 0; i < count; i++) {
      callback?.(payloads[i]);
    }
    this.report(caseName, count, Date.now() - start, bytes);
  }

  private report(caseName: string, count: number, costMs: number, bytes: number): void {
    const line = `{"case":"${caseName}","count":${count},"cost_ms":${costMs},"bytes":${bytes}}`;
    console.info(
      `[CrossRuntimeInvocationPerf] ${caseName} count=${count} cost_ms=${costMs} bytes=${bytes}`
    );
    this.statsCallback?.(line);
  }
}
