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

import { componentSnapshot, PixelMapDrawableDescriptor } from '@kit.ArkUI';
import { BusinessError } from '@kit.BasicServicesKit';
import { fileUri } from '@kit.CoreFileKit';
import { image } from '@kit.ImageKit';
import { KRRenderLog } from '../../adapter/KRRenderLog';
import { KRAny, KuiklyRenderCallback } from '../../utils/KRDataTypes';
import { KRPixelMapUtil } from '../../utils/KRPixelMapUtil';
import { KuiklyRenderBaseModule } from '../export';

class SnapshotData {
  public drawableDescriptor: PixelMapDrawableDescriptor | null = null;
  public path: string = '';
  public pathURI: string = '';
  public pixelMap: image.PixelMap | null = null;
  public message: string = '';
}

export class KRSnapshotModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRSnapshotModule';

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    if (method == 'toImage') {
      const json: Record<string, string> = JSON.parse(params as string);
      const id: string = json['node_id'];
      const internalParams = json['params'];
      const internalParamsJson: Record<string, Object> = JSON.parse(internalParams as string);
      const type: string | undefined = internalParamsJson['type']?.toString();
      let sampleSize = internalParamsJson['sampleSize'] as number;
      if (sampleSize <= 0) {
        sampleSize = 1;
      }

      componentSnapshot.get(id, (err: BusinessError<void>, data: image.PixelMap) => {

        const resultParam = new SnapshotData();
        if (err) {
          resultParam.message = (err instanceof Error ? err.message : JSON.stringify(err)) ?? '';
          KRRenderLog.i('KRSnapshotModule', `complete error message: ${err.message}`);
        } else {
          if (callback == null) {
            KRRenderLog.i('KRSnapshotModule', `callback null`);
            return;
          }

          if (type == 'dataUri') {
            let drawableDesciptor = new PixelMapDrawableDescriptor(data);
            resultParam.drawableDescriptor = drawableDesciptor;
            resultParam.pixelMap = data;
            callback([resultParam, new ArrayBuffer(1)]);
            return;
          }

          const filePath = `${getContext().tempDir}/${Date.now()}_${Math.floor(Math.random() * 0xffffff).toString(16)}.png`;
          resultParam.path = filePath.toString();
          resultParam.pathURI = fileUri.getUriFromPath(resultParam.path);
          if (type == 'file') {
            KRPixelMapUtil.toFile(resultParam.path, data).then((file) => {
              callback([resultParam, new ArrayBuffer(1)]);
            });
            return;
          }

          if (type == 'cacheKey') {
            let drawableDesciptor = new PixelMapDrawableDescriptor(data);
            resultParam.drawableDescriptor = drawableDesciptor;
            resultParam.pixelMap = data;
            callback([resultParam, new ArrayBuffer(1)]);
            KRPixelMapUtil.toFile(resultParam.path, data);
            return;
          }
        }
      }, { scale: 1 / sampleSize, waitUntilRenderFinished: true });
    }
    return null;
  }
}
