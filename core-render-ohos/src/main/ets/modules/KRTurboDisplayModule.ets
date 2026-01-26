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
import { KuiklyRenderBaseModule } from './base/KRBaseModule';

/**
 * TurboDisplay 模块
 * 对齐 Android/iOS 的 KRTurboDisplayModule 实现
 */
export class KRTurboDisplayModule extends KuiklyRenderBaseModule {
  static readonly MODULE_NAME = 'KRTurboDisplayModule';
  
  // 方法名常量
  private static readonly IS_TURBO_DISPLAY = 'isTurboDisplay';
  private static readonly SET_CURRENT_UI_AS_FIRST_SCREEN = 'setCurrentUIAsFirstScreenForNextLaunch';
  private static readonly CLOSE_TURBO_DISPLAY = 'closeTurboDisplay';
  public firstScreenTurboDisplay: boolean = false;

  syncMode(): boolean {
    return true;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): KRAny {
    switch (method) {
      case KRTurboDisplayModule.IS_TURBO_DISPLAY:
        return this.isTurboDisplay();
      case KRTurboDisplayModule.SET_CURRENT_UI_AS_FIRST_SCREEN:
        this.setCurrentUIAsFirstScreenForNextLaunch();
        return null;
      case KRTurboDisplayModule.CLOSE_TURBO_DISPLAY:
        this.closeTurboDisplay();
        return null;
      default:
        return null;
    }
  }

  onDestroy(): void {
    // 清理资源
    this.firstScreenTurboDisplay = false;
  }

  /**
   * 判断当前是否为 TurboDisplay 模式
   * 对齐 Android: KRTurboDisplayModule.isTurboDisplay()
   * 对齐 iOS: KRTurboDisplayModule.isTurboDisplay
   */
  private isTurboDisplay(): boolean {
    return this.firstScreenTurboDisplay;
  }

  /**
   * 将当前 UI 设置为下次启动的首屏
   * 对齐 Android: KRTurboDisplayModule.setCurrentUIAsFirstScreenForNextLaunch()
   * 对齐 iOS: KRTurboDisplayModule.setCurrentUIAsFirstScreenForNextLaunch
   */
  private setCurrentUIAsFirstScreenForNextLaunch(): void {
    // TODO: 实现缓存当前UI的逻辑（需要调用 C++ 层的缓存方法）
  }

  /**
   * 关闭 TurboDisplay 模式
   * 对齐 Android: KRTurboDisplayModule.closeTurboDisplay()
   * 对齐 iOS: KRTurboDisplayModule.closeTurboDisplay
   */
  private closeTurboDisplay(): void {
    this.firstScreenTurboDisplay = false;
  }
}
