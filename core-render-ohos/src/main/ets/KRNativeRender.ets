/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import { KRNativeRenderControllerReadyCallback } from './manager/KRNativeInstance';
import { IKuiklyViewDelegate, KRNativeRenderController } from './KRNativeRenderController';
import common from '@ohos.app.ability.common';
import { KuiklyNativeManager } from './native/KuiklyNativeManager';
import { ErrorReason } from './exception/KuiklyRenderExportExceptions';
import { KRNativeManager } from './manager/KRNativeManager';
import { NodeContent } from '@kit.ArkUI';
import render from 'libkuikly.so';
import { KRRenderExecuteModeBase } from './context/KRRenderExecuteModeBase';
import { KRRenderNativeMode } from './context/KRRenderNativeMode';

/**
 * @brief ArkUI C-Api Native渲染根节点
 */
@Component
export struct KRNativeRender {
  /**
   * 页面名字
   */
  pageName: string = '';
  /**
   * 透传给KR侧的页面数据
   */
  pageData: KRRecord = {};
  /**
   * 执行模式
   */
  executeMode: KRRenderExecuteModeBase = KRRenderNativeMode.Native;
  /**
   * 执行上下文code，驱动渲染对应的代码。KRRenderExecuteMode.Native模式时: 传递 ''
   */
  contextCode: string = '';
  /**
   * assets资源的实际路径，如未设置，默认在resfile中找assets资源
   */
  assetsDir: string = '';
  /**
   * 控制器ready 回调
   */
  onControllerReadyCallback: KRNativeRenderControllerReadyCallback | null = null;
  nativeManager: KuiklyNativeManager | null = null;
  delegate?: IKuiklyViewDelegate;
  /**
   * @deprecated use pageName instead
   */
  pagerName?: string;
  /**
   * @deprecated use pageData instead
   */
  pagerData?: KRRecord;
  /**
   * @deprecated use pageData instead
   */
  pageParams?: KRRecord;
  /**
   * @deprecated use controller.registerExceptionCallback from onControllerReadyCallback instead
   */
  onRenderException?: ((exception: Error, reason: ErrorReason) => void);
  /**
   * 容器是否在输入法中
   * 普通app默认为false，输入法使用到的场景请置为true
   */
  imeMode = false;
  private controller?: KRNativeRenderController;
  private nodeContent: Content = new NodeContent();

  aboutToAppear(): void {
    this.delegate?.getCustomRenderViewCreatorRegisterMap().forEach((value, key) => {
      KRNativeManager.getInstance().registerViewCreator(key, value);
    });
    this.delegate?.getCustomRenderViewCreatorRegisterMapV2().forEach((value, key) => {
      KRNativeManager.getInstance().registerViewCreatorV2(key, value);
    });
    this.delegate?.getCustomRenderModuleCreatorRegisterMap().forEach((value, key) => {
      KRNativeManager.getInstance().registerModuleCreator(key, value);
    });

    this.controller = this.delegate ?? new KRNativeRenderController();
    this.controller!.init(
      this.getUIContext(),
      getContext(this) as common.UIAbilityContext,
      this.executeMode,
      this.pagerName ?? this.pageName,
      this.pagerData ?? this.pageParams ?? this.pageData,
      this.contextCode,
      this.imeMode,
      this.assetsDir
    );

    if (this.onRenderException) {
      this.controller!.registerExceptionCallback((executeMode, stack) => {
        this.onRenderException?.(new Error(stack), ErrorReason.UNKNOWN);
      });
    }
    this.onControllerReadyCallback?.(this.controller!);
    render.createNativeRoot(this.nodeContent, this.controller!.instanceId);
  }

  aboutToDisappear(): void {
    this.controller?.onAboutToDisappear();
  }

  build() {
    Stack({ alignContent: Alignment.TopStart }) {
      // Use content slot as a bridge
      ContentSlot(this.nodeContent);

      // Use xcomponent as a bridge(To be removed soon)
      // XComponent({
      //   id: this.controller!.instanceId,
      //   type: XComponentType.NODE,
      //   libraryname: 'kuikly'
      // })
      //   .backgroundColor(Color.Transparent)
      //   .id(this.controller!.instanceId)

    }
    .hitTestBehavior(HitTestMode.None)
    .width('100%')
    .height('100%')
    .onAttach(() => {
      this.nativeManager?.internalDoLoad();
      this.controller!.onAttach();
    })
    .onAreaChange((_: Area, newValue: Area) => {
      this.controller!.onAreaChange(newValue);
    })
    .onSizeChange((_: SizeOptions, newValue: SizeOptions) => {
      this.controller!.onSizeChange(newValue);
    });
  }
}