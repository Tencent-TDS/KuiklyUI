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

import { KuiklyRenderBaseModule } from './base/KRBaseModule';
import { KRAny, KuiklyRenderCallback } from '../utils/KRDataTypes';
import pasteboard from '@ohos.pasteboard';

interface ClipboardData {
  text: string;
}

export class KRClipboardModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return true;
  }
  static readonly MODULE_NAME = "KRClipboardModule";
  private static readonly SET_TEXT = "setText";
  private static readonly GET_TEXT = "getText";

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRClipboardModule.SET_TEXT:
        this.setText(params as string);
        return null;
      case KRClipboardModule.GET_TEXT:
        return this.getText();
      default:
        return null;
    }
  }

  // 同步设置剪贴板文本
  private setText(params: string) {
    try {
      const json = JSON.parse(params || '{}') as ClipboardData;
      const text: string = json.text || '';
      let pasteData: pasteboard.PasteData = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, text);
      const systemPasteboard = pasteboard.getSystemPasteboard();
      systemPasteboard.setDataSync(pasteData);
    } catch (e) {
      // ignore
    }
  }

  // 同步获取剪贴板文本
  private getText(): string {
    try {
      const systemPasteboard: pasteboard.SystemPasteboard = pasteboard.getSystemPasteboard();
      const pasteData: pasteboard.PasteData = systemPasteboard.getDataSync();
      const text: string = pasteData.getPrimaryText();
      return text || '';
    } catch (e) {
      return '';
    }
  }
} 