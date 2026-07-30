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

import { KRRecord } from './utils/KRDataTypes';
import { KRNativeRender } from './KRNativeRender';
import { KRNativeRenderControllerReadyCallback } from './manager/KRNativeInstance';
import { KuiklyNativeManager } from './native/KuiklyNativeManager';
import { KRRenderNativeMode } from './context/KRRenderNativeMode';
import { KRRenderExecuteModeBase } from './context/KRRenderExecuteModeBase';

/**
 * @brief View粒度入口类（业务可以使用该Ark组件作为接入层入口类）
 */
@Component
export struct KRNative {
  /**
   * 页面名字
   */
  pageName: string = 'router';
  /**
   * 透传给KR侧的页面数据
   */
  pageParams: KRRecord = {};
  /**
   * 执行模式
   */
  executeMode: KRRenderExecuteModeBase = KRRenderNativeMode.Native;
  /**
   * 执行上下文code，驱动渲染对应的代码。KRRenderExecuteMode.Native模式时: 传递 ''
   */
  contextCode: string = '';
  /**
   * KRNative Controller回调, 可获取此Controller来调用 KRNative 暴露的方法
   */
  onControllerReadyCallback: KRNativeRenderControllerReadyCallback | null = null;
  nativeManager: KuiklyNativeManager | null = null;

  aboutToAppear(): void {

  }

  build() {
    KRNativeRender({
      pageName: this.pageName,
      pageData: this.pageParams,
      executeMode: this.executeMode,
      contextCode: this.contextCode,
      onControllerReadyCallback: this.onControllerReadyCallback,
      nativeManager: this.nativeManager
    })
      .width('100%')
      .height('100%');
  }
}