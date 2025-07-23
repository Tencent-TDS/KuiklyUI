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

import render, { KRNativeCallback } from 'libkuikly.so';
import { KRAny, KRValue, KuiklyRenderCallback } from '../utils/KRDataTypes';
import { KRRenderModuleExportCreator } from '../modules/base/IKRModuleExport';
import { ModulesRegisterEntry } from '../modules/ModulesRegisterEntry';
import { KRNativeInstance, KRViewCreator } from './KRNativeInstance';
import { window } from '@kit.ArkUI';
import { KRWindowInfo } from '../foundation/KRWindowInfo';
import { KRConvertUtil } from '../utils/KRConvertUtil';
import { ViewsRegisterEntry } from '../components/ViewsRegisterEntry';
import { KRNativeRenderController } from '../KRNativeRenderController';

export enum KRCallNativeMethod {
  General = 0,
  Register = 1, // 注册native回调arkTS方法
  FireCallback = 2, // 响应native callback
  KeyboardHeightChange = 3, // 键盘高度变化
  FireViewEvent = 4, // 响应View事件
}

enum KRGenralMessageKind{
  RegisterCreateor = 0
}

export enum KRNativeCallArkTSMethod {
  Unknown = 0,
  CallModuleMethod = 1, // 调用Module方法
  CreateView = 2, // 创建逻辑View
  CreateArkUINode = 3, // 创建ArkTS的ArkUI节点
  SetViewProp = 4, // 设置View属性
  SetViewEvent = 5, // 设置View事件
  CallViewMethod = 6, // 调用View方法
  RemoveView = 7, // 删除视图
  SetViewSize = 8, // 设置View尺寸
  DidMoveToParentView = 9, // 添加到父节点中
};


export class KRNativeManager {
  // 静态实例，用于存储唯一的实例
  private static instance: KRNativeManager;
  // Native实例Map管理
  private instancesMap = new Map<string, KRNativeInstance>();
  // 是否初始化过
  private didInit: boolean = false;
  // 窗口信息
  private windowInfo: KRWindowInfo | null = null;

  // 私有构造函数，确保不能通过 new 关键字创建新实例
  private constructor() {
  }

  // 静态方法，用于获取唯一的实例
  public static getInstance(): KRNativeManager {
    if (!KRNativeManager.instance) {
      KRNativeManager.instance = new KRNativeManager();
    }
    return KRNativeManager.instance;
  }

  // 窗口创建时调用该方法进行初始化
  public onWindowStageCreate(windowStage: window.WindowStage): void {
    if (this.windowInfo == null) {
      this.windowInfo = new KRWindowInfo(windowStage.getMainWindowSync());
    }
  }

  public onLastWindow(win: window.Window): void {
    if (this.windowInfo == null) {
      this.windowInfo = new KRWindowInfo(win);
    }
  }

  public getWindowInfo(): KRWindowInfo | null {
    return this.windowInfo;
  }

  /**
   * 创建Native实例
   * @param instanceId 实例ID
   */
  public createNativeInstance(instanceId: string, uiContext: UIContext, controller: KRNativeRenderController) {
    this.initIfNeed();
    this.instancesMap[instanceId] = new KRNativeInstance(instanceId, uiContext, controller);
  }

  /**
   * 删除Native实例（延时1s）
   * @param instanceId
   */
  public removeNativeInstance(instanceId: string) {
    setTimeout(() => { // 延时创建，保证页面销毁时短时间可以做退出前调用
      let instance = this.getNativeInstance(instanceId);
      instance?.onDestroy();
      this.instancesMap.delete(instanceId);
    }, 1000);
  }

  /**
   * 获取实例
   * @param instanceId
   * @returns
   */
  public getNativeInstance(instanceId: string): KRNativeInstance | null {
    return this.instancesMap[instanceId];
  }


  public initIfNeed() {
    if (this.didInit) {
      return;
    }
    this.didInit = true;

    ModulesRegisterEntry.registerSDKModules();
    ModulesRegisterEntry.registerExternalModulesCallback();
    ViewsRegisterEntry.registerSDKViews();
    ViewsRegisterEntry.registerExternalViewCallback();
    let defaultId = '0';
    this.arkTSCallNative(defaultId, KRCallNativeMethod.Register.valueOf(), null, null, null, null, null,
      (instanceId, methodId, arg0, arg1, arg2, arg3, arg4, callbackId) => {
        const nativeInstance: KRNativeInstance | null = this.getNativeInstance(instanceId);
        if (!nativeInstance) {
          return null;
        }
        if (methodId == KRNativeCallArkTSMethod.CallModuleMethod.valueOf()) { // 调用module方法
          let callback: KuiklyRenderCallback | null = null;
          if (callbackId != null && callbackId.length) { // 构造一个callback
            callback = (res: KRAny) => {
              this.fireCallback(instanceId, callbackId, res);
            };
          }
          return nativeInstance.callModuleMethodFromNative(arg0, arg1, arg2, arg3, arg4, callback);
        } else if (methodId == KRNativeCallArkTSMethod.CreateView.valueOf()) { // 创建View节点
          nativeInstance.createView(arg0 as number, arg1 as string);
        } else if (methodId == KRNativeCallArkTSMethod.CreateArkUINode.valueOf()) { // 创建ArkUI Node节点
          return nativeInstance.generateViewBuilder(arg0 as number, arg1 as string);
        } else if (methodId == KRNativeCallArkTSMethod.SetViewProp.valueOf()) { // 设置View属性
          nativeInstance.setViewProp(arg0 as number, arg1 as string, arg2 as KRValue);
        } else if (methodId == KRNativeCallArkTSMethod.SetViewEvent.valueOf()) { // 设置View事件
          let tag = arg0 as number;
          let propKey = arg1 as string;
          let callback: KuiklyRenderCallback = (data: KRAny) => {
            this.fireViewEvent(instanceId, tag, propKey, data);
          };
          nativeInstance.setViewEvent(arg0 as number, arg1 as string, callback);
        } else if (methodId == KRNativeCallArkTSMethod.CallViewMethod.valueOf()) { // 调用module方法
          let callback: KuiklyRenderCallback | null = null;
          if (callbackId != null && callbackId.length) { // 构造一个callback
            callback = (res: KRAny) => {
              this.fireCallback(instanceId, callbackId, res);
            };
          }
          nativeInstance.callViewMethod(arg0 as number, arg1 as string, arg2, callback);
        } else if (methodId == KRNativeCallArkTSMethod.RemoveView.valueOf()) { // 删除view时调用
          let tag = arg0 as number;
          nativeInstance.removeView(tag);
        } else if (methodId == KRNativeCallArkTSMethod.DidMoveToParentView.valueOf()) { // ArkUI View添加到父节点
          let tag = arg0 as number;
          nativeInstance.didMoveToParentView(tag);
        } else if (methodId == KRNativeCallArkTSMethod.SetViewSize.valueOf()) { // 设置View size
          let tag = arg0 as number;
          nativeInstance.setViewSize(tag, arg1 as number, arg2 as number);
        }

        return null;
      });
  }

  // ArkTS调用Native侧方法唯一通信通道
  private arkTSCallNative(instanceId: string, methodId: number, arg0: KRAny, arg1: KRAny, arg2: KRAny, arg3: KRAny,
    arg4: KRAny, callback: KRNativeCallback | null): number {
    return render.arkTSCallNative(instanceId, methodId,
      KRConvertUtil.anyToCObject(arg0), // 数据转换
      KRConvertUtil.anyToCObject(arg1),
      KRConvertUtil.anyToCObject(arg2),
      KRConvertUtil.anyToCObject(arg3),
      KRConvertUtil.anyToCObject(arg4),
      callback);
  }

  /**
   * 注册ArkTS侧View创建器
   * @param viewType 对应Kotlin侧的viewName返回字符串作为viewType注册，如：'KRImageView'
   */
  public registerViewCreator(viewType: string, viewCreator: KRViewCreator) {
    KRNativeInstance.registerViewCreator(viewType, viewCreator);
    this.arkTSCallNative('0', KRCallNativeMethod.General.valueOf(),  KRGenralMessageKind.RegisterCreateor.valueOf(), viewType, 1, null,
      null, null);
  }

  public registerViewCreatorV2(viewType: string, viewCreator: KRViewCreator) {
    KRNativeInstance.registerViewCreator(viewType, viewCreator);
    this.arkTSCallNative('0', KRCallNativeMethod.General.valueOf(), KRGenralMessageKind.RegisterCreateor.valueOf(), viewType, 2, null,
      null, null);
  }

  /**
   * 注册ArkTS侧View创建器
   * @param moduleName 和native侧该module同名
   * @param moduleCreator  module实例创建器
   */
  public registerModuleCreator(moduleName: string, moduleCreator: KRRenderModuleExportCreator) {
    KRNativeInstance.registerModuleCreator(moduleName, moduleCreator);
  }

  /**
   * 通知键盘高度变化
   * @param height 键盘高度（单位vp）
   * @param duration 键盘弹出时间（单位毫秒）
   */
  public keyboardHeightChange(height: number, durationMs: number) {
    let globalId = '0';
    this.arkTSCallNative(globalId, KRCallNativeMethod.KeyboardHeightChange.valueOf(), height, durationMs, null, null,
      null, null);
  }

  private fireCallback(instanceId: string, callbackId: string, data: KRAny) {
    this.arkTSCallNative(instanceId, KRCallNativeMethod.FireCallback.valueOf(), callbackId, data, null, null, null,
      null);
  }

  private fireViewEvent(instanceId: string, tag: number, propKey: string, data: KRAny) {
    this.arkTSCallNative(instanceId, KRCallNativeMethod.FireViewEvent.valueOf(), tag, propKey, data, null, null, null);

  }
}