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


import { window } from '@kit.ArkUI';
import { KRRenderLog } from '../adapter/KRRenderLog';
import { KRNativeManager } from '../manager/KRNativeManager';
import { KRRect } from './KRRect';

export class KRWindowInfo {
  private statusBarHeight: number | null = null;
  private windowRect: KRRect = new KRRect(0, 0, 0, 0);
  private window: window.Window
  private windowId: string = ""
  public instanceCount: number = 0
  private keyboardHeightChangeCallback = (data: number) => {
    KRNativeManager.getInstance().keyboardHeightChange(px2vp(data), 300, this.windowId);
  };

  private windowSizeChangeCallback = () => {
    let rect = this.window.getWindowProperties().windowRect;
    this.windowRect.x = rect.left;
    this.windowRect.y = rect.top;
    this.windowRect.width = rect.width;
    this.windowRect.height = rect.height;
  };

  constructor(mainWindow: window.Window) {
    this.window = mainWindow
    this.windowId = mainWindow.getWindowProperties().id + ""
    this.instanceCount = 1
    let height = mainWindow.getWindowAvoidArea(window.AvoidAreaType.TYPE_SYSTEM).topRect.height;
    this.statusBarHeight = px2vp(height);
    let rect = mainWindow.getWindowProperties().windowRect;
    this.windowRect.x = rect.left;
    this.windowRect.y = rect.top;
    this.windowRect.width = rect.width;
    this.windowRect.height = rect.height;
    try {
      mainWindow.on('keyboardHeightChange', this.keyboardHeightChangeCallback);
      mainWindow.on('windowSizeChange', this.windowSizeChangeCallback)
    } catch (e) {
      KRRenderLog.e("WindowInfo", `setWindow on error code:${e.code}`)
    }
  }

  getStatusBarHeightS(): number {
    if (this.statusBarHeight == null) {
      return 38;
    }
    return this.statusBarHeight;
  }

  getWindowRect(): KRRect {
    return this.windowRect;
  }

  addInstanceCount() {
    this.instanceCount++;
  }

  subInstanceCount() {
    this.instanceCount--;
  }

  getInstanceCount(): number {
    return this.instanceCount;
  }

  offWindowCallback() {
    try {
      this.window.off('keyboardHeightChange', this.keyboardHeightChangeCallback);
      this.window.off('windowSizeChange', this.windowSizeChangeCallback)
    } catch (e) {
      KRRenderLog.e("WindowInfo", `setWindow off error code:${e.code}`)
    }
  }
}