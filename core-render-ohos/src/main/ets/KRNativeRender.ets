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
import { KRNativeRenderControllerReadyCallback } from './manager/KRNativeInstance';
import { IKuiklyViewDelegate, KRNativeRenderController } from './KRNativeRenderController';
import common from '@ohos.app.ability.common';
import { KuiklyNativeManager } from './native/KuiklyNativeManager';
import { ErrorReason } from './exception/KuiklyRenderExportExceptions';
import { KRNativeManager } from './manager/KRNativeManager';
import { NodeContent, Size } from '@kit.ArkUI';
import render from 'libkuikly.so';
import { KRRenderExecuteModeBase } from './context/KRRenderExecuteModeBase';
import { KRRenderNativeMode } from './context/KRRenderNativeMode';
import { KRRect } from './foundation/KRRect';

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
   * 鸿蒙侧对KRSharedPreferencesModule实现方式的选择
   * 默认使用基于XML持久化的PreferencesModule
   * 如果为true，则调用KROhSharedPreferencesModule
   */
  useOhSharedPreferences: boolean = false

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
  initialSize?: Size;
  /**
   * 二阶段验证开关：true 时把整棵 Kuikly 页面挂到 Navigation 标题栏内。
   * 二阶段结论已得出（自绘节点进 scope 即可生效），现改为由业务侧的
   * 标题栏容器组件（KRTitleBarView）自行承载 Navigation，故恢复为 false。
   */
  navScopeTest: boolean = false;
  navTitleWidth: number = 400;
  navTitleHeight: number = 500;
  private controller?: KRNativeRenderController;
  private nodeContent: Content = new NodeContent();
  private loaded = false;

  loadNativeLibIfNeeded(): void{
    if (!this.loaded && this.nativeManager != null){
      this.nativeManager?.internalDoLoad()
      this.loaded = true
    }
  }
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
    if (this.delegate != null) {
      const callback = this.delegate?.getKuiklyRenderViewLifecycleCallback()
      if (callback != null) {
        this.controller.addKuiklyRenderViewLifecycleCallback(callback)
      }
    }
    this.controller!.init(
      this.getUIContext(),
      getContext(this) as common.UIAbilityContext,
      this.executeMode,
      this.pagerName ?? this.pageName,
      this.pagerData ?? this.pageParams ?? this.pageData,
      this.contextCode,
      this.imeMode,
      this.assetsDir,
      this.useOhSharedPreferences
    );

    if (this.onRenderException) {
      this.controller!.registerExceptionCallback((executeMode, stack) => {
        this.onRenderException?.(new Error(stack), ErrorReason.UNKNOWN);
      });
    }
    this.onControllerReadyCallback?.(this.controller!);
    render.createNativeRoot(this.nodeContent, this.controller!.instanceId);
    this.loadNativeLibIfNeeded()
    if (this.initialSize) {
      this.controller!.onViewSizeChanged(new KRRect(0, 0, this.initialSize.width, this.initialSize.height));
    }
  }

  aboutToDisappear(): void {
    this.controller?.onAboutToDisappear();
  }

  /**
   * 把 Kuikly 原生节点树（ContentSlot）放进 Navigation 标题栏，
   * 用于验证"自绘节点只要落在官方规定的生效区域内，沉浸光感是否就能渲染"。
   * 验证完毕后应恢复为 false。
   */
  @Builder
  KuiklyInNavigationTitleBar() {
    Stack({ alignContent: Alignment.TopStart }) {
      // Use content slot as a bridge
      ContentSlot(this.nodeContent);
    }
    .hitTestBehavior(HitTestMode.None)
    .width(this.navTitleWidth)
    .height(this.navTitleHeight)
    .onAttach(() => {
      this.loadNativeLibIfNeeded()
      this.controller!.onAttach();
    })
    .onAreaChange((_: Area, newValue: Area) => {
      this.controller!.onAreaChange(newValue);
    })
    .onSizeChange((_: SizeOptions, newValue: SizeOptions) => {
      this.controller!.onSizeChange(newValue);
    });
  }

  build() {
    if (this.navScopeTest) {
      // 测试态：Kuikly 根挂在 Navigation 标题栏内（官方规定的生效区域）
      Navigation() {
        Column() {
          Text('Kuikly 内容区（空）— 节点已挂到标题栏').fontSize(14)
        }
        .width('100%')
        .height('100%')
        .backgroundColor(Color.Orange)
      }
      .title({ builder: this.KuiklyInNavigationTitleBar, height: this.navTitleHeight },
        { barStyle: BarStyle.STACK })
      .titleMode(NavigationTitleMode.Mini)
      .hideTitleBar(false)
      .width('100%')
      .height('100%')
    } else {
      Stack({ alignContent: Alignment.TopStart }) {
        // Use content slot as a bridge
        ContentSlot(this.nodeContent);
      }
      .hitTestBehavior(HitTestMode.None)
      .width('100%')
      .height('100%')
      .onAttach(() => {
        this.loadNativeLibIfNeeded()
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
}