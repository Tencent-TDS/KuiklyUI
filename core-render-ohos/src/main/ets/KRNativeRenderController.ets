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

import { KRExceptionCallback } from './manager/KRNativeInstance';
import render from 'libkuikly.so';
import { AvoidAreaChangeParams, Insets, KRAny, KRRecord, KRValue } from './utils/KRDataTypes';
import { KRNativeManager } from './manager/KRNativeManager';
import { KRConvertUtil } from './utils/KRConvertUtil';
import { common, EnvironmentCallback } from '@kit.AbilityKit';
import { KRSize } from './foundation/KRSize';
import { display, window } from '@kit.ArkUI';
import { KRRect } from './foundation/KRRect';
import { KRRenderViewExportCreator } from './components/base/KRBaseViewExport';
import { KRRenderModuleExportCreator } from './modules/base/IKRModuleExport';
import { KRRenderLog } from './adapter/KRRenderLog';
import { BusinessError } from '@kit.BasicServicesKit';
import { Configuration } from '@ohos.app.ability.Configuration';
import { KRRenderExecuteModeBase } from './context/KRRenderExecuteModeBase';
import { KRRenderNativeMode } from './context/KRRenderNativeMode';

/** window尺寸变化事件名 */
export const KRWindowSizeDidChangedEventKey = 'windowSizeDidChanged';

/** RootView尺寸变化事件名. */
export const KRRootViewSizeDidChangedEventKey = 'rootViewSizeDidChanged';

/** 页面可见事件 */
export const KRPageDidAppearEventKey = 'viewDidAppear';

/** 页面不可见事件 */
export const KRPageDidDisappearEventKey = 'viewDidDisappear';

export const KRConfigurationUpdateEventKey = 'configurationDidChanged';

/** 字典key常量 */
export const KRRootViewWidthKey = 'rootViewWidth';

export const KRRootViewHeightKey = 'rootViewHeight';

export const KRUrlKey = 'url';

export const KRStatusBarHeightKey = 'statusBarHeight';

export const KRPlatformKey = 'platform';

export const KRDeviceWidthKey = 'deviceWidth';

export const KRDeviceHeightKey = 'deviceHeight';

export const KRActivityWidthKey = 'activityWidth';

export const KRActivityHeightKey = 'activityHeight';

export const KROsVersionKey = 'osVersion';

export const KRAppVersionKey = 'appVersion';

export const KRParamKey = 'param';

export const KRWidthKey = 'width';

export const KRHeightKey = 'height';

export const KRNativeBuild = 'nativeBuild';

export const KRSafeAreaInsets = 'safeAreaInsets';

export const KRDensityInfo = 'densityInfo';

export const KRExecuteMode = 'executeMode';

export const KRContextCode = 'contextCode';

export const KRDensity = 'density';

const KROnBackPressedKey = 'onBackPressed';

const KROnBackTimeoutMs = 200;

enum KRBackPressConsumedState {
  Undefined = 0,
  Consumed,
  NotConsumed
}

/** 全局递增实例ID */
let gGlobalInstanceId = 0;

export class KRNativeRenderController {
  /**
   * 页面实例 id
   */
  instanceId: string = '';
  /**
   * 执行上下文code，驱动渲染对应的代码。KRRenderExecuteMode.Native模式时: 传递 ''
   */
  contextCode: string = '';
  /**
   * assets资源的实际路径，如未设置，默认在resfile中找assets资源
   */
  assetsDir: string = '';
  /**
   * 执行模式
   */
  executeMode: KRRenderExecuteModeBase = KRRenderNativeMode.Native;
  /**
   * 页面名字
   */
  pageName: string = '';
  /**
   * 透传给KR侧的页面数据
   */
  pageParams: KRRecord = {};
  private fontSizeScale = 1.0;
  private fontWeightScale = 1.0;
  /**
   * 输入法模式
   * 在输入法中，获取不到window和safearea，所以只有有view rect后就可以初始化kuikly
   */
  imeMode = false;
  private environmentCallbackId = -1;
  private densityPixels: number = 0;
  private uiContext: UIContext | null = null;
  private uiAbilityContext: common.UIAbilityContext | null = null;
  private didInit: Boolean = false;
  private lazyTasks: (() => void)[] = [];
  /**
   * 外部自定义存放在Controller中的数据, 在Module中，可通过Controller来获取数据
   */
  private externalParams: KRRecord = {};
  /**
   * 外部自定义的Controller, 可设置此值，然后在Module中获取
   */
  private externalController: object | null = null;
  private safeAreaRect: KRRect | null = null;
  private viewRect: KRRect | null = null;
  private windowRect: KRRect | null = null;
  private updateSafeInsetsHandler: number = 0;
  private windowClass: window.Window | null = null;
  private destroyed: boolean = false;
  private onWindowSizeChanged: Callback<window.Size> = (params: window.Size) => {
    if (this.imeMode) {
      return;
    }
    const density = display.getDefaultDisplaySync().densityPixels;
    const w = params.width / density;
    const h = params.height / density;

    this.notifyWindowSizeChanged(w, h);
    this.doUpdateSafeArea();
  };
  private avoidAreaListener: Callback<AvoidAreaChangeParams> = (params: AvoidAreaChangeParams) => {
    if (this.imeMode) {
      return;
    }
    if (params.type == window.AvoidAreaType.TYPE_SYSTEM ||
      params.type == window.AvoidAreaType.TYPE_CUTOUT ||
      params.type == window.AvoidAreaType.TYPE_NAVIGATION_INDICATOR) {
      this.doUpdateSafeArea();
    }
  };

  private doUpdateSafeArea() {
    if (this.imeMode) {
      return;
    }
    if (this.updateSafeInsetsHandler > 0) {
      clearTimeout(this.updateSafeInsetsHandler);
    }
    this.updateSafeInsetsHandler = setTimeout(() => {
      this.updateSafeInsetsHandler = 0;
      try {
        this.onSafeAreaInsetsChanged(this.getSafeAreaRect(this.windowClass!));
      } catch (e) {
        KRRenderLog.e('Window', `Error obtaining safearea, code:${e.code}`);
      }
    }, 0);
  }

  init(uiContext: UIContext,
    uiAbilityContext: common.UIAbilityContext,
    executeMode: KRRenderExecuteModeBase,
    pageName: string,
    pageParams: KRRecord,
    contextCode: string,
    imeMode: boolean,
    assetsDir: string
  ) {
    this.uiContext = uiContext;
    this.uiAbilityContext = uiAbilityContext;
    this.executeMode = executeMode;
    this.pageName = pageName;
    this.pageParams = pageParams;
    this.contextCode = contextCode;
    this.imeMode = imeMode;
    this.assetsDir = assetsDir;
    gGlobalInstanceId++;
    this.instanceId = gGlobalInstanceId + '';
    render.OnLaunchStart(this.instanceId, executeMode.mode); //  页面启动事件通知到native层，用于Performance性能采集
    this.densityPixels = display.getDefaultDisplaySync().densityPixels;
    KRNativeManager.getInstance().createNativeInstance(this.instanceId, uiContext, this);
    const currentConfig = uiAbilityContext.config;
    this.fontSizeScale = currentConfig.fontSizeScale ? currentConfig.fontSizeScale : 1;
    this.fontWeightScale = currentConfig.fontWeightScale ? currentConfig.fontWeightScale : 1;

    const theInstanceId = this.instanceId;
    const theController = this;
    let envCallback: EnvironmentCallback = {
      onConfigurationUpdated(config) {
        theController.onConfigurationUpdate(config);
      },
      onMemoryLevel(level) {
        KRRenderLog.i('Configuration', `memory level: ${level}`);
      }
    };
    try {
      let applicationContext = uiAbilityContext.getApplicationContext();
      this.environmentCallbackId = applicationContext.on('environment', envCallback);
    } catch (paramError) {
      KRRenderLog.e('Configuration',
        `error: ${(paramError as BusinessError).code}, ${(paramError as BusinessError).message}`);
    }

    if (!imeMode) {
      // window api not available in ime according to hw representative, which is not documented anywhere though :(
      window.getLastWindow(getContext(this)).then((data) => {
        if (this.destroyed) {
          return;
        }
        try {
          this.windowClass = data;
          const winRect = data.getWindowProperties().windowRect;
          this.windowRect = new KRRect(0, 0, px2vp(winRect.width), px2vp(winRect.height));
          KRNativeManager.getInstance().onLastWindow(data);
          this.onSafeAreaInsetsChanged(this.getSafeAreaRect(data));
          data.on('avoidAreaChange', this.avoidAreaListener);
          data.on('windowSizeChange', this.onWindowSizeChanged);
        } catch (e) {
          KRRenderLog.e('Window', `getLastWindow callback error code:${e.code}`);
        }
      });
    }
  }

  /**
   * 未捕获异常回调
   * @param exceptionCallback
   */
  registerExceptionCallback(exceptionCallback: KRExceptionCallback) {
    KRNativeManager.getInstance().getNativeInstance(this.instanceId)?.registerExceptionCallback(exceptionCallback);
  }

  onViewSizeChanged(rect: KRRect, oldDensity: number, newDensity: number) {
    this.viewRect = rect;
    if (this.didInit) {
      let densityInfo: KRRecord | null = null;
      if (oldDensity != newDensity) {
        densityInfo = {
          'oldDensity': oldDensity,
          'newDensity': newDensity
        };
      }
      this.notifySizeChange(densityInfo);
    } else {
      this.doOnAttach();
    }
  }

  notifyWindowSizeChanged(width: number, height: number) {
    const data: KRRecord = {};
    data[KRWidthKey] = width;
    data[KRHeightKey] = height;
    render.sendEvent(this.instanceId, KRWindowSizeDidChangedEventKey, JSON.stringify(data));
  }

  paramsReady(): boolean {
    if (this.imeMode) {
      return this.viewRect != null;
    } else {
      return this.safeAreaRect != null && // safearea updated
        KRNativeManager.getInstance().getWindowInfo() != null && // window info updated
        this.viewRect != null; // view rect updated
    }
  }

  onConfigurationUpdate(config: Configuration) {
    if(!this.didInit){
      return
    }
    const data: KRRecord = {
      'platform': 'ohos',
    };
    if (config.fontWeightScale) {
      data['fontWeightScale'] = config.fontWeightScale;
    }
    if (config.fontSizeScale) {
      data['fontSizeScale'] = config.fontSizeScale;
    }
    // update config to render core
    render.updateConfig(this.instanceId, JSON.stringify(data));

    // notify core to mark text elements dirty
    render.sendEvent(this.instanceId, KRConfigurationUpdateEventKey, JSON.stringify(data));
  }

  onSafeAreaInsetsChanged(rect: KRRect) {
    this.safeAreaRect = rect;
    if (this.didInit) {
      this.notifySizeChange();
    } else {
      this.doOnAttach();
    }
  }

  private encodeSafeAreaInsetsString() {
    if (this.safeAreaRect == null || this.viewRect == null) {
      return '0 0 0 0';
    }
    const left = Math.max(0, this.safeAreaRect.x - this.viewRect.x);
    const right = Math.max(0, this.viewRect.width - this.safeAreaRect.width - left);
    const top = Math.max(0, this.safeAreaRect.y - this.viewRect.y);
    const bottom = Math.max(0, this.viewRect.height - this.safeAreaRect.height - top);
    return `${top} ${left} ${bottom} ${right}`;
  }

  private notifySizeChange(densityInfo: KRRecord | null = null) {
    if (this.viewRect == null) {
      return;
    }
    const data: KRRecord = {};
    data[KRWidthKey] = this.viewRect.width;
    data[KRHeightKey] = this.viewRect.height;
    data[KRSafeAreaInsets] = this.encodeSafeAreaInsetsString();
    if (densityInfo) {
      data[KRDensityInfo] = JSON.stringify(densityInfo);
    }
    this.sendEvent(KRRootViewSizeDidChangedEventKey, data);

    this.runKRRenderTask(() => {
      render.onRenderViewSizeChanged(this.instanceId, this.viewRect?.width as number, this.viewRect?.height as number);
    });
  }

  onAttach() {
    this.doOnAttach();
  }

  onAreaChange(newValue: Area) {
    this.viewRect = new KRRect(newValue.globalPosition.x as number,
      newValue.globalPosition.y as number,
      newValue.width as number,
      newValue.height as number);
    if (this.didInit) {
      this.doOnAreaChange(newValue);
      this.notifySizeChange();
    } else {
      this.doOnAttach();
    }
  }

  onSizeChange(newValue: SizeOptions) {
    if (this.viewRect == null) {
      return;
    }
    this.viewRect.width = newValue.width as number;
    this.viewRect.height = newValue.height as number;
  }

  onAboutToDisappear() {
    this.destroyed = true;
    try {
      if (this.windowClass) {
        this.windowClass.off('avoidAreaChange', this.avoidAreaListener);
        this.windowClass.off('windowSizeChange', this.onWindowSizeChanged);
      }
      if (this.environmentCallbackId != -1) {
        let applicationContext = this.uiAbilityContext?.getApplicationContext();
        applicationContext?.off('environment', this.environmentCallbackId);
        this.environmentCallbackId = -1;
      }
    } catch (e) {
      KRRenderLog.e('Window', `unregister event failed:${e.code}`);
    }

    this.doAboutToDisappear();
    KRNativeManager.getInstance().removeNativeInstance(this.instanceId);
  }

  onPageShow() {
    this.doOnPageShow();

    if (this.didInit && !this.destroyed) {
      const newDensity = display.getDefaultDisplaySync().densityPixels;
      if (newDensity != this.densityPixels) {
        // density变化，原因是修改了系统的无障碍选项
        this.doOnDensityChanged(newDensity);
      }
    }
  }

  private doOnDensityChanged(newDensity: number) {
    const oldDensity = this.densityPixels;
    this.densityPixels = newDensity;

    let data: Record<string, string | number> = {
      'vp2px': vp2px(1.0),
    };

    render.updateConfig(this.instanceId, JSON.stringify(data));

    if (this.windowRect) {
      this.windowRect = new KRRect(0, 0,
        px2vp(this.windowRect.width * oldDensity),
        px2vp(this.windowRect.height * oldDensity)
      );

      this.notifyWindowSizeChanged(this.windowRect.width, this.windowRect.height);
    }
    if (this.viewRect) {
      this.viewRect = new KRRect(
        px2vp(this.viewRect.x * oldDensity),
        px2vp(this.viewRect.y * oldDensity),
        px2vp(this.viewRect.width * oldDensity),
        px2vp(this.viewRect.height * oldDensity)
      );

      this.onViewSizeChanged(this.viewRect, oldDensity, newDensity);
    }
    if (this.windowClass) {
      try {
        this.onSafeAreaInsetsChanged(this.getSafeAreaRect(this.windowClass));
      } catch (e) {
        KRRenderLog.e('Window', `doOnDensityChanged update safearea error code:${e.code}`);
      }
    }
  }

  onPageHide() {
    this.doOnPageHide();
  }

  sendEvent(event: string, data: KRRecord) {
    this.doSendEvent(event, data);
  }

  onBackPress() {
    const sendTime = Date.now();
    this.doSendEvent(KROnBackPressedKey, {});

    while (Date.now() - sendTime < KROnBackTimeoutMs) {
      const result = render.isBackPressConsumed(this.instanceId, sendTime);
      if (result === KRBackPressConsumedState.Consumed) {
        return true;
      } else if (result === KRBackPressConsumedState.NotConsumed) {
        return false;
      }
    }
    return false;
  }

  putParam(key: string, value: KRValue) {
    this.externalParams[key] = value;
  }

  getParam(key: string): KRAny | null {
    return this.externalParams[key];
  }

  setExternalController(controller: object) {
    this.externalController = controller;
  }

  getExternalController<T>(): T {
    return this.externalController as T;
  }

  getUIAbilityContext(): common.UIAbilityContext | null {
    return this.uiAbilityContext;
  }

  private doOnPageShow() {
    const data: KRRecord = {};
    data[KRPageDidAppearEventKey] = '1';
    this.doSendEvent(KRPageDidAppearEventKey, data);
  }

  private doOnPageHide() {
    const data: KRRecord = {};
    data[KRPageDidDisappearEventKey] = '1';
    this.doSendEvent(KRPageDidDisappearEventKey, data);
  }

  private doSendEvent(event: string, data: KRRecord) {
    this.runKRRenderTask(() => {
      render.sendEvent(this.instanceId, event, KRConvertUtil.anyToCObject(data));
    });
  }

  private doOnAttach() {
    if (this.didInit) {
      return;
    }
    if (!this.paramsReady()) {
      return;
    }
    this.doDidInit();
  }

  private doDidInit() {
    if (this.viewRect == null) {
      return;
    }
    this.didInit = true;
    let pageData = this.createPageParams(this.pageParams, this.viewRect.width, this.viewRect.height);
    let configJson = this.createConfigJson(new KRSize(this.viewRect.width, this.viewRect.height));

    render.onInitRenderView(this.instanceId, this.pageName, pageData, this.viewRect.width, this.viewRect.height,
      configJson
      , this.uiContext, this.uiContext?.getHostContext()?.resourceManager);
    this.runLazyTasksOnce();
  }

  private doAboutToDisappear() {
    this.runKRRenderTask(() => {
      render.onDestroyRenderView(this.instanceId);
    });
  }

  private doOnAreaChange(newValue: Area) {
    this.runKRRenderTask(() => {
      render.onRenderViewSizeChanged(this.instanceId, newValue.width as number, newValue.height as number);
    });
  }

  private createPageParams(params: KRRecord, viewWidth: number, viewHeight: number): string {
    let screenSize = KRConvertUtil.screenSize();
    let pageData: Record<string, string | number | KRRecord> = {};
    pageData[KRParamKey] = params;
    pageData[KRRootViewWidthKey] = viewWidth;
    pageData[KRRootViewHeightKey] = viewHeight;
    pageData[KRUrlKey] = this.pageName;
    if (!this.imeMode) {
      pageData[KRStatusBarHeightKey] = this.safeAreaRect!.y;
      let windowInfo = KRNativeManager.getInstance().getWindowInfo();
      if (windowInfo) {
        pageData[KRActivityWidthKey] = px2vp(windowInfo.getWindowRect().width);
        pageData[KRActivityHeightKey] = px2vp(windowInfo.getWindowRect().height);
      }
      pageData[KRSafeAreaInsets] = this.encodeSafeAreaInsetsString();
    }
    pageData[KRDeviceWidthKey] = screenSize.width;
    pageData[KRDeviceHeightKey] = screenSize.height;
    pageData[KROsVersionKey] = ''; // 系统版本
    pageData[KRNativeBuild] = 2;
    pageData[KRAppVersionKey] = '1.0.0';
    pageData[KRPlatformKey] = 'ohos';
    pageData[KRExecuteMode] = this.executeMode.mode;
    pageData[KRContextCode] = this.contextCode;
    pageData[KRDensity] = display.getDefaultDisplaySync().densityPixels;

    return JSON.stringify(pageData);
  }

  private createConfigJson(screenSize: KRSize): string {
    let data: Record<string, string | number> = {
      'vp2px': vp2px(1.0),
      'screen_width': screenSize.width,
      'screen_height': screenSize.height,
      'resfile_dir': getContext().resourceDir,
      'files_dir': getContext().filesDir,
      'assets_dir': this.assetsDir,
      'fontWeightScale': this.fontWeightScale,
      'fontSizeScale': this.fontSizeScale
    };

    return JSON.stringify(data);
  }

  private runKRRenderTask(task: () => void) {
    if (this.didInit) {
      task();
    } else {
      this.lazyTasks.push(task);
    }
  }

  private runLazyTasksOnce() {
    this.lazyTasks.forEach((task, _) => {
      task();
    });
    this.lazyTasks = [];
  }

  private getSafeAreaRect(data: window.Window): KRRect {
    const winRect = data.getWindowProperties().windowRect;
    const insets: Insets = {
      left: 0,
      top: 0,
      right: winRect.width,
      bottom: winRect.height,
    };
    const handle = (avoidArea: window.AvoidArea) => {
      if (avoidArea.leftRect.width) {
        insets.left = Math.max(insets.left, avoidArea.leftRect.left + avoidArea.leftRect.width);
      }
      if (avoidArea.topRect.height) {
        insets.top = Math.max(insets.top, avoidArea.topRect.top + avoidArea.topRect.height);
      }
      if (avoidArea.rightRect.width) {
        insets.right = Math.min(insets.right, avoidArea.rightRect.left);
      }
      if (avoidArea.bottomRect.height) {
        insets.bottom = Math.min(insets.bottom, avoidArea.bottomRect.top);
      }
    };
    handle(data.getWindowAvoidArea(window.AvoidAreaType.TYPE_SYSTEM));
    handle(data.getWindowAvoidArea(window.AvoidAreaType.TYPE_CUTOUT));
    handle(data.getWindowAvoidArea(window.AvoidAreaType.TYPE_NAVIGATION_INDICATOR));
    const result = new KRRect(px2vp(insets.left), px2vp(insets.top), px2vp(insets.right - insets.left),
      px2vp(insets.bottom - insets.top));
    console.log(`getSafeAreaRect safeAreaInsets: ${result.x}, ${result.y}, ${result.width}, ${result.height}`);
    return result;
  }
}

export abstract class IKuiklyViewDelegate extends KRNativeRenderController {
  /**
   * 获取自定义扩展渲染视图创建注册Map
   */
  abstract getCustomRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator>;

  /**
   * 获取自定义扩展渲染视图创建注册Map。
   * 通过这个方式注册的creator，创建的自定义view将不会有影子节点处理基础事件，需要用户在arkts侧响应所有属性的设置。
   * 当这个方式的好处是，由于不存在影子节点，其view曾经和DSL中定义的是保持一致的。
   * 建议仅在有强一致层级需求的时候才采用。
   */
  getCustomRenderViewCreatorRegisterMapV2(): Map<string, KRRenderViewExportCreator>{
    // by default
    return new Map();
  }

  /**
   * 获取自定义扩展module创建注册Map
   */
  abstract getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator>;

  pageDidAppear() {
    this.onPageShow();
  }

  pageDidDisappear() {
    this.onPageHide();
  }
}
