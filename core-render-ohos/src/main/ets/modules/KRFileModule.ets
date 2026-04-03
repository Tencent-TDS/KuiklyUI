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

import { KRAny, KuiklyRenderCallback } from '../utils/KRDataTypes';
import { KRRenderLog } from '../adapter/KRRenderLog';
import { KuiklyRenderBaseModule } from './base/KRBaseModule';
import fs from '@ohos.file.fs';

/**
 * 文件读写 Module，提供 App Caches 目录的文件写入能力。
 * 主要用于 RecompositionProfiler 导出 JSON 报告供 AI 分析。
 *
 * 写入目录：context.cacheDir/KuiklyProfiler/
 * HarmonyOS 文件路径：/data/storage/el2/base/cache/KuiklyProfiler/
 * 读取命令：hdc file recv /data/storage/el2/base/cache/KuiklyProfiler/<filename> /tmp/
 */
export class KRFileModule extends KuiklyRenderBaseModule {
  syncMode(): boolean {
    return false;
  }

  static readonly MODULE_NAME = 'KRFileModule';
  private static readonly METHOD_WRITE_FILE = 'writeFile';
  private static readonly METHOD_APPEND_FILE = 'appendFile';
  private static readonly METHOD_GET_FILES_DIR = 'getFilesDir';
  private static readonly PARAM_FILENAME = 'filename';
  private static readonly PARAM_CONTENT = 'content';

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRFileModule.METHOD_WRITE_FILE:
        this.writeFile(params, callback);
        return null;
      case KRFileModule.METHOD_APPEND_FILE:
        this.appendFile(params, callback);
        return null;
      case KRFileModule.METHOD_GET_FILES_DIR:
        this.getFilesDir(callback);
        return null;
      default:
        return null;
    }
  }

  onDestroy(): void {
  }

  private profilerDir(): string | null {
    try {
      const cacheDir = getContext().cacheDir;
      const dir = `${cacheDir}/KuiklyProfiler`;
      if (!fs.accessSync(dir)) {
        fs.mkdirSync(dir);
      }
      return dir;
    } catch (e) {
      KRRenderLog.e(KRFileModule.MODULE_NAME, `profilerDir error: ${e}`);
      return null;
    }
  }

  private getFilesDir(callback: KuiklyRenderCallback | null): void {
    const dir = this.profilerDir();
    if (dir !== null) {
      callback?.({ path: dir });
    } else {
      callback?.({ error: 'context unavailable' });
    }
  }

  private writeFile(params: KRAny, callback: KuiklyRenderCallback | null): void {
    try {
      const paramsObj = JSON.parse(params as string) as Record<string, string>;
      const filename = paramsObj[KRFileModule.PARAM_FILENAME];
      const content = paramsObj[KRFileModule.PARAM_CONTENT];

      if (!filename || !content) {
        callback?.({ error: 'missing filename or content' });
        return;
      }

      const dir = this.profilerDir();
      if (dir === null) {
        callback?.({ error: 'context unavailable' });
        return;
      }

      const filePath = `${dir}/${filename}`;
      const file = fs.openSync(filePath, fs.OpenMode.WRITE_ONLY | fs.OpenMode.CREATE | fs.OpenMode.TRUNC);
      fs.writeSync(file.fd, content);
      fs.closeSync(file);
      callback?.({ path: filePath });
    } catch (e) {
      KRRenderLog.e(KRFileModule.MODULE_NAME, `writeFile error: ${e}`);
      callback?.({ error: `${e}` });
    }
  }

  private appendFile(params: KRAny, callback: KuiklyRenderCallback | null): void {
    try {
      const paramsObj = JSON.parse(params as string) as Record<string, string>;
      const filename = paramsObj[KRFileModule.PARAM_FILENAME];
      const content = paramsObj[KRFileModule.PARAM_CONTENT];

      if (!filename || !content) {
        callback?.({ error: 'missing filename or content' });
        return;
      }

      const dir = this.profilerDir();
      if (dir === null) {
        callback?.({ error: 'context unavailable' });
        return;
      }

      const filePath = `${dir}/${filename}`;
      // 追加写，末尾加换行，适合 JSONL 格式
      const file = fs.openSync(filePath, fs.OpenMode.WRITE_ONLY | fs.OpenMode.CREATE | fs.OpenMode.APPEND);
      fs.writeSync(file.fd, content + '\n');
      fs.closeSync(file);
      callback?.({ path: filePath });
    } catch (e) {
      KRRenderLog.e(KRFileModule.MODULE_NAME, `appendFile error: ${e}`);
      callback?.({ error: `${e}` });
    }
  }
}
