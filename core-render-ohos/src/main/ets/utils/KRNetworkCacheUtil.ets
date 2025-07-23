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

import { KuiklyRenderCallback } from '../utils/KRDataTypes';
import { common } from '@kit.AbilityKit';
import { BusinessError, request } from '@kit.BasicServicesKit';

import fs from '@ohos.file.fs';
import { KRRenderLog } from '../adapter/KRRenderLog';

export class  KRNetworkCacheUtil {
  // 保存正在进行的下载任务
  private static downloadTasks: Map<string, KuiklyRenderCallback[]> = new Map();
  /**
   * 通用下载或者本地缓存来返回url对应的本地磁盘文件路径
   * @param url
   * @param callback
   */
  static fetchFileByDownloadOrCache(context: common.UIAbilityContext, url: string, callback: KuiklyRenderCallback) {
    let cacheFilePath = KRNetworkCacheUtil.generateCacheFilePathByUrl(context, url)
    if (cacheFilePath.length) {
      if (!(KRNetworkCacheUtil.downloadTasks.has(url))) {
        fs.access(cacheFilePath, (err, res) => {
          if (err) {//mhttps://developer.huawei.com/consumer/cn/doc/harmonyos-references-V2/js-apis-file-fs-0000001451843016-V2
            KRRenderLog.i('KRNetworkCacheUtil', `access failed with error message: ${err.message}, error code: ${err.code}`)
          } else {
            if (res) {
              callback(cacheFilePath);
              return ;
            }
            KRNetworkCacheUtil.downloadFile(context, cacheFilePath, url, callback)
          }
        });
      } else {
        KRNetworkCacheUtil.downloadFile(context, cacheFilePath, url, callback)
      }
    } else {
      KRRenderLog.i('KRNetworkCacheUtil', `cache file path is empty by url: ${url}`)
      callback('')
    }
  }

  static downloadFile(context: common.UIAbilityContext, cacheFilePath: string, url: string, callback: KuiklyRenderCallback) {
    // 如果已经有一个下载任务在进行中，就将回调函数添加到任务的回调函数数组中
    if (KRNetworkCacheUtil.downloadTasks.has(url)) {
      KRNetworkCacheUtil.downloadTasks.get(url)!.push(callback);
    } else {
      // 创建一个新的下载任务，并将回调函数添加到任务的回调函数数组中
      KRNetworkCacheUtil.downloadTasks.set(url, [callback]);
      request.downloadFile(context, {
        url: url,
        filePath: cacheFilePath
      }).then((downloadTask) => {
        downloadTask.on('complete', () => {
          KRRenderLog.i('KRNetworkCacheUtil', `donwload success with url:${url} cacheFilePath:${cacheFilePath}`)
          // 调用所有与该URL关联的回调函数
          KRNetworkCacheUtil.downloadTasks.get(url)!.forEach((cb) => cb(cacheFilePath));
          // 从下载任务映射中移除已完成的任务
          KRNetworkCacheUtil.downloadTasks.delete(url);
        })

        downloadTask.on('fail', (err: number) => {
          KRRenderLog.i('KRNetworkCacheUtil', `donwload fail with url:${url} cacheFilePath:${cacheFilePath} err:${err}`)
          // 调用所有与该URL关联的回调函数
          KRNetworkCacheUtil.downloadTasks.get(url)!.forEach((cb) => cb(cacheFilePath));
          // 从下载任务映射中移除已完成的任务
          KRNetworkCacheUtil.downloadTasks.delete(url);
        });

      }).catch((err: BusinessError) => {
        KRRenderLog.i('KRNetworkCacheUtil', `Invoke downloadTask failed, code is ${err.code}, message is ${err.message} url is ${url}`)
        // 调用所有与该URL关联的回调函数，传入空字符串表示下载失败
        KRNetworkCacheUtil.downloadTasks.get(url)!.forEach((cb) => cb(''));
        // 从下载任务映射中移除已完成的任务
        KRNetworkCacheUtil.downloadTasks.delete(url);
      });
    }
  }

  static urlToFilename(url: string): string {
    return url.replace(/[^a-zA-Z0-9_-]/g, '_');
  }

  /**
   * 根据网络url生成本地的文件磁盘文件路径
   * @param url
   */
  static generateCacheFilePathByUrl(context: common.UIAbilityContext, url: string): string {
      let type = KRNetworkCacheUtil.getPathExtension(url);
      if (type && type.length) {
         return context.cacheDir + '/' + KRNetworkCacheUtil.urlToFilename(url) + '.' + type
      }
      return ''
  }


  static  getPathExtension(url: string): string | null {
    const pathWithoutQuery = url.split('?')[0]; // 先去掉查询参数部分
    const pathParts = pathWithoutQuery.split('/');
    const fileName = pathParts[pathParts.length - 1];
    const fileParts = fileName.split('.');

    if (fileParts.length > 1) {
      return fileParts[fileParts.length - 1];
    } else {
      return null;
    }
  }
}