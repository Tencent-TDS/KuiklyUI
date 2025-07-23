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

import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { KRAny, KuiklyRenderBaseModule, KuiklyRenderCallback } from '@kuikly-open/render';
import util from '@ohos.util';
import fs from '@ohos.file.fs';
import { fileUri } from '@kit.CoreFileKit';
import { KRNetworkCacheUtil } from '@kuikly-open/render/src/main/ets/utils/KRNetworkCacheUtil';

export class KRBridgeModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'HRBridgeModule';
  static readonly SHOW_ALERT = 'showAlert';
  static readonly CLOSE_PAGE = 'closePage';
  static readonly OPEN_PAGE = 'openPage';
  static readonly COPY_TO_PASTE_BOARD = 'copyToPasteboard';
  static readonly TOAST = 'toast';
  static readonly LOG = 'log';
  static readonly REPORT_DT = 'reportDT';
  static readonly REPORT_REAL_TIME = 'reportRealtime';
  static readonly LOCAL_SERVE_TIME = 'localServeTime';
  static readonly CURRENT_TIME_STAMP = 'currentTimestamp';
  static readonly DATE_FORMATTER = 'dateFormatter';
  static readonly READ_ASSET_FILE = 'readAssetFile';
  static readonly GET_LOCAL_IMAGE_PATH = 'getLocalImagePath';

  syncMode(): boolean {
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback): KRAny {
    switch (method) {
      case KRBridgeModule.SHOW_ALERT: {
        this.showAlert(params, callback);
        break;
      }
      case KRBridgeModule.CLOSE_PAGE: {
        this.closePage();
        break;
      }
      case KRBridgeModule.OPEN_PAGE: {
        this.openPage();
        break;
      }
      case KRBridgeModule.COPY_TO_PASTE_BOARD: {
        this.copyToPasteboard();
        break;
      }
      case KRBridgeModule.TOAST: {
        this.toast();
        break;
      }
      case KRBridgeModule.LOG: {
        this.log(params);
        break;
      }
      case KRBridgeModule.REPORT_DT: {
        this.reportDT(params);
        break;
      }
      case KRBridgeModule.REPORT_REAL_TIME: {
        this.reportRealtime(params);
        break;
      }
      case KRBridgeModule.LOCAL_SERVE_TIME: {
        this.localServeTime(params, callback);
        break;
      }
      case KRBridgeModule.CURRENT_TIME_STAMP: {
        return this.currentTimestamp();
      }
      case KRBridgeModule.DATE_FORMATTER: {
        return this.dateFormatter(params);
      }
      case KRBridgeModule.READ_ASSET_FILE: {
        this.readAssetFile(params, callback);
        break;
      }
      case KRBridgeModule.GET_LOCAL_IMAGE_PATH: {
        this.getLocalImagePath(params, callback);
        break;
      }
      default:
        break;
    }
    return null;
  }

  private showAlert(params: KRAny, callback: KuiklyRenderCallback) {
  }

  // TODO close page
  private closePage() {
  }

  // TODO open new page
  private openPage() {
  }

  // TODO copy to paste board
  private copyToPasteboard() {
  }

  // TODO open toast
  private toast() {
  }

  private log(params: KRAny) {
    if (params == null) {
      return;
    }
    console.log(`KuiklyRender ${JSON.stringify(params)}`,);
  }

  private reportDT(params: KRAny) {
  }

  private reportRealtime(params: KRAny) {
  }

  private localServeTime(params: KRAny, callback: KuiklyRenderCallback) {
    callback({ 'time': Date.now() / 1000.0 });
  }

  private currentTimestamp(): string {
    return Date.now().toString();
  }

  // TODO format date
  private dateFormatter(params: KRAny): string {
    console.log(`========== ${params}`);
    return '';
  }

  private readAssetFile(params: KRAny, callback: KuiklyRenderCallback) {
    // let startTimeMills = Date.now();
    let records = JSON.parse(params as string) as Record<string, string>;
    let assetPath = records['assetPath'];

    let context = getContext(this) as common.UIAbilityContext;

    let resDir = context.resourceDir;
    let path = resDir + '/' + assetPath;
    let assetURI = fileUri.getUriFromPath(path);
    const file = fs.openSync(assetURI, fs.OpenMode.READ_ONLY);
    const stat = fs.statSync(file.fd);
    const arrayBuffer = new ArrayBuffer(stat.size);
    fs.read(file.fd, arrayBuffer, (err: BusinessError, readLen: number) => {
      if (err) {
        console.error("read failed with error message: " + err.message + ", error code: " + err.code);
      } else {
        let buf = new Uint8Array(arrayBuffer, 0, readLen);
        const textDecoder = new util.TextDecoder('utf-8');
        const res = textDecoder.decodeWithStream(new Uint8Array(buf));
        callback({
          'result': JSON.parse(res)
        });
      }
      fs.closeSync(file);
    });
  }

  private getLocalImagePath(params: KRAny, callback: KuiklyRenderCallback) {
    let records = JSON.parse(params as string) as Record<string, string>;
    let imageUrl = records['imageUrl'];
    let context = getContext(this) as common.UIAbilityContext;
    KRNetworkCacheUtil.fetchFileByDownloadOrCache(context, imageUrl, (filePath: KRAny) => {
      callback({
        'localPath': filePath
      });
    });
  }
}
