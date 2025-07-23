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
import { KRNativeManager } from '../manager/KRNativeManager';
import { KRRect } from './KRRect';

export class KRWindowInfo {
  private statusBarHeight: number | null = null;
  private windowRect: KRRect = new KRRect(0, 0, 0, 0);

  constructor(mainWindow: window.Window) {
    let height = mainWindow.getWindowAvoidArea(window.AvoidAreaType.TYPE_SYSTEM).topRect.height;
    this.statusBarHeight = px2vp(height);
    mainWindow.on('keyboardHeightChange', (data) => {
      KRNativeManager.getInstance().keyboardHeightChange(px2vp(data), 300);
    });
    let rect = mainWindow.getWindowProperties().windowRect;
    this.windowRect.x = rect.left;
    this.windowRect.y = rect.top;
    this.windowRect.width = rect.width;
    this.windowRect.height = rect.height;
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
}