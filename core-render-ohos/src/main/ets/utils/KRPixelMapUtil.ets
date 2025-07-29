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

import image from '@ohos.multimedia.image'
import buffer from '@ohos.buffer';
import fs from '@ohos.file.fs';

export class KRPixelMapUtil {
  public static toDataUri(pixelMap: PixelMap): Promise<string> {
    const mimeType = 'image/png';
    const imagePackerApi = image.createImagePacker();
    const packOpts: image.PackingOption = { format: mimeType, quality: 100 }
    return imagePackerApi.packing(pixelMap, packOpts)
      .then((data) => {
        return `data:${mimeType};base64,${buffer.from(data).toString('base64')}`;
      })
      .finally(() => {
        imagePackerApi.release();
      });
  }

  public static toFile(filePath: string, pixelMap: PixelMap): Promise<string> {
    const mimeType = 'image/png';
    const imagePackerApi = image.createImagePacker();
    const packOpts: image.PackingOption = { format: mimeType, quality: 100 }
    return fs.open(filePath, fs.OpenMode.CREATE | fs.OpenMode.READ_WRITE)
      .then((file) => {
        return imagePackerApi.packToFile(pixelMap, file.fd, packOpts)
      })
      .then(() => filePath)
      .finally(() => {
        imagePackerApi.release();
      });
  }
}