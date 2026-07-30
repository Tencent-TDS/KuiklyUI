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

import { KRViewCreator } from '../manager/KRNativeInstance';
import { KRNativeManager } from '../manager/KRNativeManager';
import { KuiklyRenderBaseView } from './base/KRBaseViewExport';
import { KRPAGView } from './KRPAGView';
import { KRVideoView } from './KRVideoView';
import { KRBlurView } from './KRBlurView';

/**
 * ArkTS侧内置Module均在此注册生成实例闭包
 */
export class ViewsRegisterEntry {
  /*
   *宿主注册扩展View时机回调钩子
   * 调用ViewsRegisterEntry.registerViewCreator 方法进行注册
   */
  static registerExternalViewCallback: () => void = () => {
  };

  static registerSDKViews() {
    // 注册ArkTS View
    KRNativeManager.getInstance().registerViewCreator('KRBlurView', (): KuiklyRenderBaseView => {
      return new KRBlurView();
    });

    KRNativeManager.getInstance().registerViewCreator('KRPAGView', (): KuiklyRenderBaseView => {
      return new KRPAGView();
    });

    KRNativeManager.getInstance().registerViewCreator('KRVideoView', (): KuiklyRenderBaseView => {
      return new KRVideoView();
    });
  }

  /**
   * 注册ArkTS侧View创建器
   * @param viewType 对应Kotlin侧的viewName返回字符串作为viewType注册，如：'KRImageView'
   */
  static registerViewCreator(viewType: string, viewCreator: KRViewCreator) {
    KRNativeManager.getInstance().registerViewCreator(viewType, viewCreator);
  }
}



