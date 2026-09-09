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
import { i18n } from '@kit.LocalizationKit';
import { bundleManager } from '@kit.AbilityKit';
import { deviceInfo } from '@kit.BasicServicesKit';

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

export const KRSystemLanguageKey = 'sysLang';

export const KRFeature = 'feature';

const KROnBackPressedKey = 'onBackPressed';

enum KRBackPressConsumedState {
  Undefined = 0,
  Consumed,
  NotConsumed
}

/** safeArea 纠偏：浮点尺寸比较容差（dp） */
const KRSafeAreaMatchToleranceDp = 1.0;
/** safeArea 纠偏：变化量等式判定容差（dp） */
const KRSafeAreaDeltaEps = 0.1;

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
  /**
   * 鸿蒙侧，向用户提供新旧两种KRSharedPreferencesMoudle使用的控制开关（变量）
   */
  useOhSharedPreferences: boolean = false;
  /**
   * 所在的窗口Id
   */
  windowId: string = ""
  /**
   * SizeChanged 执行时机是否需提前至 onSizeChanged 而非 onAreaChanged，以避免页面 Size 变化时出现白屏
   */
  private sizeChangeHandledByOnSizeChange: boolean = false;
  /** 上一帧窗口尺寸：突变帧注入检测（判据B）需要窗口变化量 */
  private prevWindowRect: KRRect | null = null;
  /** 上一帧 safeAreaRect：判据B的还原基准（稳态帧不会被注入，天然可靠） */
  private lastSafeAreaRect: KRRect | null = null;
  /** 最近一次「viewRect 与 windowRect 同朝向」时换算出的 insets（判据A兜底值） */
  private lastTrustedInsets: string | null = null;

  private lifecycleCallbacks: Array<IKuiklyRenderViewLifecycleCallback> = [];

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
    const density = this.getWindowActiveDensity();
    const w = params.width / density;
    const h = params.height / density;

    // windowRect 是 deviceWidth/Height 唯一数据源，必须跟随窗口更新，否则旋转后 Compose 屏幕尺寸冻结；
    // prevWindowRect 保留旧值，供判据B计算窗口变化量
    this.prevWindowRect = this.windowRect;
    this.windowRect = new KRRect(0, 0, w, h);

    this.notifyWindowSizeChanged(w, h);
    // 同步刷新 safeArea（不走 setTimeout），避免与布局回调竞争产生「旧 viewRect − 新 safeAreaRect」错配
    this.refreshSafeAreaSync();
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

  /**
   * 同步刷新 safeAreaRect 但不下发：让 safeAreaRect 与 windowRect 同帧就绪，
   * 消除 setTimeout(0) 与布局回调的时序竞争，等 viewRect 同朝向后由布局链路统一下发。
   */
  private refreshSafeAreaSync(): void {
    if (this.imeMode || this.windowClass == null) {
      return;
    }
    // 同步已取到最新值，取消排队中的去抖任务，避免稍后重复赋值
    if (this.updateSafeInsetsHandler > 0) {
      clearTimeout(this.updateSafeInsetsHandler);
      this.updateSafeInsetsHandler = 0;
    }
    try {
      const synced = this.getSafeAreaRect(this.windowClass);
      const corrected = this.correctInjectedSafeArea(synced);
      this.onSafeAreaInsetsChanged(corrected, false);
    } catch (e) {
      KRRenderLog.e('Window', `Error obtaining safearea, code:${e.code}`);
    }
  }

  /** 将 safeAreaRect（x/y/width/height）按给定窗口尺寸换算为 [top,left,bottom,right] 侧值 */
  private sideInsetsOf(rect: KRRect, winW: number, winH: number): number[] {
    const sides: number[] = [0, 0, 0, 0];
    sides[0] = rect.y;
    sides[1] = rect.x;
    sides[2] = winH - rect.y - rect.height;
    sides[3] = winW - rect.x - rect.width;
    return sides;
  }

  /**
   * 判据B（兜底）：注入的机理是系统把「窗口尺寸变化量」错塞进 safeArea，故注入侧必满足
   * |本帧侧值−上帧侧值| == |本帧窗口−上帧窗口|（同轴，0.1dp）——基于 bug 机理而非数值分布判定，无比例假设；
   * 真实安全区变化（键盘/状态栏显隐）不会与窗口变化量精确等值，且仅突变帧进入本函数，稳态帧天然跳过，故不误伤。
   */
  private correctInjectedSafeArea(newRect: KRRect): KRRect {
    const prevWin = this.prevWindowRect;
    const curWin = this.windowRect;
    const prevSafe = this.lastSafeAreaRect;
    this.lastSafeAreaRect = newRect;
    if (prevWin == null || curWin == null || prevSafe == null) {
      return newRect;
    }
    const deltaW = Math.abs(curWin.width - prevWin.width);
    const deltaH = Math.abs(curWin.height - prevWin.height);
    const oldSides = this.sideInsetsOf(prevSafe, prevWin.width, prevWin.height);
    const newSides = this.sideInsetsOf(newRect, curWin.width, curWin.height);
    // top/left/bottom/right 分别锚定窗口高/宽轴
    const windowDeltas = [deltaH, deltaW, deltaH, deltaW];
    let corrected = false;
    for (let i = 0; i < 4; i++) {
      if (windowDeltas[i] < KRSafeAreaMatchToleranceDp) {
        continue; // 该轴窗口没变化，不存在注入，也不该动真实值
      }
      const sideDelta = Math.abs(newSides[i] - oldSides[i]);
      if (Math.abs(sideDelta - windowDeltas[i]) < KRSafeAreaDeltaEps) {
        newSides[i] = oldSides[i];
        corrected = true;
      }
    }
    if (!corrected) {
      return newRect;
    }
    return new KRRect(newSides[1], newSides[0],
      curWin.width - newSides[1] - newSides[3],
      curWin.height - newSides[0] - newSides[2]);
  }

  /** 判据A：viewRect 与 windowRect 朝向一致性（短边对短边、长边对长边），异朝向相减必然产生轴差量级假值 */
  private sameOrientation(a: KRRect, b: KRRect): boolean {
    const aShort = Math.min(a.width, a.height);
    const aLong = Math.max(a.width, a.height);
    const bShort = Math.min(b.width, b.height);
    const bLong = Math.max(b.width, b.height);
    return Math.abs(aShort - bShort) < KRSafeAreaMatchToleranceDp &&
      Math.abs(aLong - bLong) < KRSafeAreaMatchToleranceDp;
  }

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
    assetsDir: string,
    useOhSharedPreferences: boolean,
    viewRect?: KRRect,
  ) {
    this.uiContext = uiContext;
    this.uiAbilityContext = uiAbilityContext;
    this.executeMode = executeMode;
    this.pageName = pageName;
    this.pageParams = pageParams;
    this.contextCode = contextCode;
    this.imeMode = imeMode;
    this.assetsDir = assetsDir;
    if (viewRect) {
      this.viewRect = viewRect;
    }
    this.useOhSharedPreferences = useOhSharedPreferences;
    gGlobalInstanceId++;
    this.instanceId = gGlobalInstanceId + '';
    render.OnLaunchStart(this.instanceId, executeMode.mode); //  页面启动事件通知到native层，用于Performance性能采集
    try {
      this.windowClass = window.findWindow(uiContext.getWindowName());
    } catch (e) {
      KRRenderLog.e(`window`, `findWindow, stack: ${e}`);
    }
    this.densityPixels = this.getWindowActiveDensity();
    KRNativeManager.getInstance().createNativeInstance(this.instanceId, uiContext, this);
    const currentConfig = uiAbilityContext.config;
    this.fontSizeScale = currentConfig.fontSizeScale ? currentConfig.fontSizeScale : 1;
    if (!this.fontSizeScaleFollowSystem()) {
      this.fontSizeScale = 1;
    }
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
      const winRect = this.windowClass!.getWindowProperties().windowRect;
      this.windowRect = new KRRect(0,0, px2vp(winRect.width), px2vp(winRect.height));
      this.windowId = this.windowClass!.getWindowProperties().id + "";
      KRNativeManager.getInstance().onLastWindow(this.windowClass!);
      this.onSafeAreaInsetsChanged(this.getSafeAreaRect(this.windowClass!));
      this.windowClass!.on('avoidAreaChange', this.avoidAreaListener);
      this.windowClass!.on('windowSizeChange', this.onWindowSizeChanged);
    }
  }

  /**
   * 未捕获异常回调
   * @param exceptionCallback
   */
  registerExceptionCallback(exceptionCallback: KRExceptionCallback) {
    KRNativeManager.getInstance().getNativeInstance(this.instanceId)?.registerExceptionCallback(exceptionCallback);
  }

  onViewSizeChanged(rect: KRRect, oldDensity: number = this.densityPixels, newDensity: number = this.densityPixels) {
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
        KRNativeManager.getInstance().getWindowInfo(this.windowId) != undefined && // window info updated
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
    let fontSizeScaleFollowSystem = this.fontSizeScaleFollowSystem();
    if (!fontSizeScaleFollowSystem) {
      data['fontSizeScale'] = 1
    }
    data['fontSizeScaleFollowSystem'] = this.fontSizeScaleFollowSystem();

    const instance = KRNativeManager.getInstance().getNativeInstance(this.instanceId)
    if (instance){
      instance.onConfigurationUpdate()
    }

    // update config to render core
    render.updateConfig(this.instanceId, JSON.stringify(data));

    // notify core to mark text elements dirty
    render.sendEvent(this.instanceId, KRConfigurationUpdateEventKey, JSON.stringify(data));
  }

  /**
   * 更新 safeAreaRect；notify=false 仅存不下发（windowSizeChange 专用）：
   * 此刻 ArkUI 尚未布局、viewRect 仍是旧朝向，立即下发必产生错配 insets。
   */
  onSafeAreaInsetsChanged(rect: KRRect, notify: boolean = true) {
    this.safeAreaRect = rect;
    if (this.didInit) {
      if (notify) {
        this.notifySizeChange();
      }
    } else {
      this.doOnAttach();
    }
  }

  private encodeSafeAreaInsetsString() {
    if (this.safeAreaRect == null || this.viewRect == null) {
      return '0 0 0 0';
    }
    // 判据A（核心）：viewRect 与 windowRect 异朝向时换算必携带轴差假值（结构决定），
    // 此时返回最近一次同朝向的可信 insets，等 viewRect 同朝向后自然恢复换算
    const win = this.windowRect;
    const oriented = win != null && this.sameOrientation(this.viewRect, win);
    if (win != null && !oriented) {
      return this.lastTrustedInsets ?? '0 0 0 0';
    }
    const left = Math.max(0, this.safeAreaRect.x - this.viewRect.x);
    const right = Math.max(0, this.viewRect.width - this.safeAreaRect.width - left);
    const top = Math.max(0, this.safeAreaRect.y - this.viewRect.y);
    const bottom = Math.max(0, this.viewRect.height - this.safeAreaRect.height - top);
    const result = `${top} ${left} ${bottom} ${right}`;
    this.lastTrustedInsets = result;
    return result;
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
    // 随事件下发最新窗口尺寸，Kotlin 侧 PagerData 收到后同步更新，
    // Compose screenWidthDp/screenHeightDp 随之跟随旋转互换，否则冻结在初始朝向
    if (this.windowRect != null) {
      data[KRDeviceWidthKey] = this.windowRect.width;
      data[KRDeviceHeightKey] = this.windowRect.height;
    }
    this.doSendEvent(KRRootViewSizeDidChangedEventKey, data, this.syncSendEvent(KRRootViewSizeDidChangedEventKey));

    this.runKRRenderTask(() => {
      render.onRenderViewSizeChanged(this.instanceId, this.viewRect?.width as number, this.viewRect?.height as number);
    });
  }

  onAttach() {
    this.doOnAttach();
  }
  // 判断 View size 是否发生了改变
  private isViewSizeChanged(width: number, height: number): boolean {
    return this.viewRect == null ||
      Math.abs(this.viewRect.width - width) >= 0.1 ||
      Math.abs(this.viewRect.height - height) >= 0.1;
  }

  onAreaChange(newValue: Area) {
    const newWidth = newValue.width as number;
    const newHeight = newValue.height as number;
    const sizeChanged = this.isViewSizeChanged(newWidth, newHeight);
    // 始终更新 viewRect（包括 globalPosition），确保位置信息准确
    this.viewRect = new KRRect(newValue.globalPosition.x as number,
      newValue.globalPosition.y as number,
      newWidth,
      newHeight);
    if (this.didInit) {
      if (this.sizeChangeHandledByOnSizeChange) {
        // 已在 onSizeChange 中提前处理过，消费标志位，跳过重复执行
        this.sizeChangeHandledByOnSizeChange = false;
      } else if (sizeChanged) {
        // 兜底：onSizeChange 未处理（syncSendEvent=false 或未触发），在此处触发
        this.doOnAreaChange(newValue);
        this.notifySizeChange();
      }
    } else {
      this.doOnAttach();
    }
  }

  onSizeChange(newValue: SizeOptions) {
    if (this.viewRect == null) {
      return;
    }
    const newWidth = newValue.width as number;
    const newHeight = newValue.height as number;
    if (!this.isViewSizeChanged(newWidth, newHeight)) {
      return;
    }
    // 仅当业务方开启了 rootViewSizeDidChanged 同步策略时，才在布局前提前触发同步渲染
    if (this.didInit && this.syncSendEvent(KRRootViewSizeDidChangedEventKey)) {
      // onSizeChange 是布局前回调：提前更新 viewRect 使后续下发必然使用「新 viewRect − 新 safeAreaRect」，
      // 关闭与 safeArea 刷新之间的错配窗口
      this.viewRect.width = newWidth;
      this.viewRect.height = newHeight;
      this.runKRRenderTask(() => {
        render.onRenderViewSizeChanged(this.instanceId, newWidth, newHeight);
      });
      this.notifySizeChange();
      // 标记已处理，避免 onAreaChange 中重复执行
      this.sizeChangeHandledByOnSizeChange = true;
    }
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
    KRNativeManager.getInstance().removeWindowInfoIfNeed(this.windowId);
    KRNativeManager.getInstance().removeNativeInstance(this.instanceId);
  }

  onPageShow() {
    this.doOnPageShow();

    if (this.didInit && !this.destroyed) {
      const newDensity = this.getWindowActiveDensity();
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

  sendEventSync(event: string, data: KRRecord, sync: boolean) {
    this.doSendEvent(event, data, sync);
  }

  onBackPress() {
    const sendTime = Date.now();
    this.doSendEvent(KROnBackPressedKey, {}, this.syncSendEvent(KROnBackPressedKey));
    const result = render.isBackPressConsumed(this.instanceId, sendTime);
    if (result === KRBackPressConsumedState.Consumed) {
      return true;
    } else if (result === KRBackPressConsumedState.NotConsumed) {
      return false;
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
    this.doSendEvent(KRPageDidAppearEventKey, data, this.syncSendEvent(KRPageDidAppearEventKey));
  }

  private doOnPageHide() {
    const data: KRRecord = {};
    data[KRPageDidDisappearEventKey] = '1';
    this.doSendEvent(KRPageDidDisappearEventKey, data, this.syncSendEvent(KRPageDidDisappearEventKey));
  }
  // 外部接口：提供给业务指定需要同步执行的事件
  syncSendEvent(event: string): boolean {
    return event === KROnBackPressedKey;
  }

  private doSendEvent(event: string, data: KRRecord, sync?: boolean) {
    this.runKRRenderTask(() => {
      const shouldSync = sync ?? this.syncSendEvent(event);
      if (shouldSync) {
        render.sendEventSync(this.instanceId, event, KRConvertUtil.anyToCObject(data), true);
      } else {
        render.sendEvent(this.instanceId, event, KRConvertUtil.anyToCObject(data));
      }
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
    // 初始下发基线快照（deviceWidth/deviceHeight 初值来自物理屏幕 screenSize）
    let configJson = this.createConfigJson(new KRSize(this.viewRect.width, this.viewRect.height));

    render.onInitRenderView(this.instanceId, this.pageName, pageData, this.viewRect.width, this.viewRect.height,
      configJson
      , this.uiContext, this.uiContext?.getHostContext()?.resourceManager);
    this.runLazyTasksOnce();
  }


  addKuiklyRenderViewLifecycleCallback(callback: IKuiklyRenderViewLifecycleCallback) {
    this.lifecycleCallbacks.push(callback)
  }

  dispatchLifecycleStateChanged(state: number, params?: Object): void {
    switch (state) {
      case KRLifecycleState.STATE_INIT: {
        this.lifecycleCallbacks.forEach(callback => callback.onInit());
        break;
      }
      case KRLifecycleState.STATE_INIT_CORE_START: {
        this.lifecycleCallbacks.forEach(callback => callback.onInitCoreStart());
        break;
      }
      case KRLifecycleState.STATE_INIT_CORE_FINISH: {
        this.lifecycleCallbacks.forEach(callback => callback.onInitCoreFinish());
        break;
      }
      case KRLifecycleState.STATE_INIT_CONTEXT_START: {
        this.lifecycleCallbacks.forEach(callback => callback.onInitContextStart());
        break;
      }
      case KRLifecycleState.STATE_INIT_CONTEXT_FINISH: {
        this.lifecycleCallbacks.forEach(callback => callback.onInitContextFinish());
        break;
      }
      case KRLifecycleState.STATE_CREATE_INSTANCE_START: {
        this.lifecycleCallbacks.forEach(callback => callback.onCreateInstanceStart());
        break;
      }
      case KRLifecycleState.STATE_CREATE_INSTANCE_FINISH: {
        this.lifecycleCallbacks.forEach(callback => callback.onCreateInstanceFinish());
        break;
      }
      case KRLifecycleState.STATE_FIRST_FRAME_PAINT: {
        this.lifecycleCallbacks.forEach(callback => callback.onFirstFramePaint());
        break;
      }
    }
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
    params[KRSystemLanguageKey] = i18n.System.getSystemLanguages()[0];
    pageData[KRParamKey] = params;
    pageData[KRRootViewWidthKey] = viewWidth;
    pageData[KRRootViewHeightKey] = viewHeight;
    pageData[KRUrlKey] = this.pageName;
    if (!this.imeMode) {
      pageData[KRStatusBarHeightKey] = this.safeAreaRect!.y;
      let windowInfo = KRNativeManager.getInstance().getWindowInfo(this.windowId);
      if (windowInfo != undefined) {
        pageData[KRActivityWidthKey] = px2vp(windowInfo.getWindowRect().width);
        pageData[KRActivityHeightKey] = px2vp(windowInfo.getWindowRect().height);
      }
      pageData[KRSafeAreaInsets] = this.encodeSafeAreaInsetsString();
    }

    let versionName = "1.0.0"
    let osFullName = ""
    try {
      const bundleInfo = bundleManager.getBundleInfoForSelfSync(bundleManager.BundleFlag.GET_BUNDLE_INFO_WITH_APPLICATION)
      versionName = bundleInfo.versionName;
      osFullName = deviceInfo.osFullName; // system version
    }catch (e) {
      // noop
    }

    pageData[KRDeviceWidthKey] = screenSize.width;
    pageData[KRDeviceHeightKey] = screenSize.height;
    pageData[KROsVersionKey] = osFullName; // 系统版本
    pageData[KRNativeBuild] = 2;
    pageData[KRAppVersionKey] = versionName;
    pageData[KRPlatformKey] = 'ohos';
    pageData[KRExecuteMode] = this.executeMode.mode;
    pageData[KRContextCode] = this.contextCode;
    pageData[KRDensity] = this.getWindowActiveDensity();
    pageData[KRFeature] = {
      "box_shadow_fill" : 1
    };
    return JSON.stringify(pageData);
  }

  private createConfigJson(screenSize: KRSize): string {
    let data: Record<string, string | number | boolean> = {
      'vp2px': vp2px(1.0),
      'screen_width': screenSize.width,
      'screen_height': screenSize.height,
      'resfile_dir': getContext().resourceDir,
      'files_dir': getContext().filesDir,
      'assets_dir': this.assetsDir,
      'fontWeightScale': this.fontWeightScale,
      'fontSizeScale': this.fontSizeScale,
      'imeMode': this.imeMode ? 1 : 0,
      'windowId': this.windowId,
      'fontSizeScaleFollowSystem': this.fontSizeScaleFollowSystem() ? 1 : 0,
      'performanceMonitorTypesMask': this.getMonitorTypeMask(),
      "useOhSharedPreferences": this.useOhSharedPreferences
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
    return result;
  }

  /**
   * 获取窗口活跃的density
   * customDensity的值默认为systemDensity
   * 如果windowStage主动设置了DefaultDensityEnabled/CustomDensity,则分别是defaultDensity/customDensity
   * 因此默认取customDensity作为活跃的density
   */
  private getWindowActiveDensity() {
    if (this.windowClass != null) {
      return this.windowClass.getWindowDensityInfo?.().customDensity ?? display.getDefaultDisplaySync().densityPixels;
    } else {
      return display.getDefaultDisplaySync().densityPixels
    }
  }

  /**
   * 字体缩放是否跟随系统
   * @return boolean true:跟随系统(默认) false:不跟随系统(缩放比例为1)
   */
  fontSizeScaleFollowSystem(): boolean {
    return true
  }

  /**
   * 回调启动数据
   */
  onGetLaunchData(data: KRRecord): void {
  }

  /**
   * 回调性能数据
   */
  onGetPerformanceData(data: KRRecord): void {
  }

  /**
   * Kuikly框架设置性能监控选项，默认只开启动监控
   * @return Array<KRMonitorType>: 需要设置的性能监控选项列表
   */
  performanceMonitorTypes(): Array<KRMonitorType> {
    return [KRMonitorType.LAUNCH];
  }

  private getMonitorTypeMask(): number {
    const mask = this.performanceMonitorTypes().reduce((acc: KRMonitorType, cur: KRMonitorType) => acc | cur, 0);
    return mask;
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

  /**
   * 获取自定义[KuiklyRenderView]生命周期回调
   */
  getKuiklyRenderViewLifecycleCallback(): IKuiklyRenderViewLifecycleCallback | null {
    return null
  }
}

enum KRLifecycleState {
  STATE_INIT = 1,
  STATE_INIT_CORE_START,
  STATE_INIT_CORE_FINISH,
  STATE_INIT_CONTEXT_START,
  STATE_INIT_CONTEXT_FINISH,
  STATE_CREATE_INSTANCE_START,
  STATE_CREATE_INSTANCE_FINISH,
  STATE_FIRST_FRAME_PAINT
}

/**
 * RenderView 生命周期回调
 */
export interface IKuiklyRenderViewLifecycleCallback {

  /**
   * 启动
   */
  onInit(): void

  /**
   * renderCore 初始化开始
   */
  onInitCoreStart(): void

  /**
   * renderCore 初始化完成
   */
  onInitCoreFinish(): void

  /**
   * 初始化渲染环境开始
   */
  onInitContextStart(): void

  /**
   * 初始化渲染环境完成
   */
  onInitContextFinish(): void

  /**
   * 创建页面开始
   */
  onCreateInstanceStart(): void

  /**
   * 创建页面结束
   */
  onCreateInstanceFinish(): void

  /**
   * 页面首帧渲染完成
   */
  onFirstFramePaint(): void

}

/**
 * 性能监控类型
 */
export enum KRMonitorType {
  LAUNCH = 1 << 0,  // 1 启动监控
  FRAME = 1 << 1,  // 2 FPS监控
  MEMORY = 1 << 2,  // 4 内存监控
}