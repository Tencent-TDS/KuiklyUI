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

import { KRAny, KRValue, KuiklyRenderCallback } from '../utils/KRDataTypes';
import { KuiklyRenderBaseView } from '../components/base/KRBaseViewExport';
import { IKuiklyRenderModuleExport, KRRenderModuleExportCreator } from '../modules/base/IKRModuleExport';
import { ComponentContent, NodeContent } from '@kit.ArkUI';
import { KRNativeRenderController } from '../KRNativeRenderController';
import { KuiklyRenderBaseModule } from '../modules/base/KRBaseModule';
import { KRRenderExecuteModeBase } from '../context/KRRenderExecuteModeBase';
import { KRRenderNativeMode } from '../context/KRRenderNativeMode';


export type KRViewCreator = () => KuiklyRenderBaseView;

export type KRExceptionCallback = (executeMode: KRRenderExecuteModeBase, stack: string) => void;

export type KRNativeRenderControllerReadyCallback = (controller: KRNativeRenderController) => void;

export class KRNativeInstance {
  private uiContext: UIContext | null = null;
  private controller: KRNativeRenderController | null = null;
  private modules = new Map<string, IKuiklyRenderModuleExport>();
  private viewRegistry = new Map<number, KuiklyRenderBaseView>();
  private static moduleCreatorRegistry = new Map<string, KRRenderModuleExportCreator>();
  private static viewCreatorRegistry = new Map<string, KRViewCreator>();
  private exceptionCallback: KRExceptionCallback | null = null;
  private mInstanceId: string = '';

  constructor(instanceId: string, uiContext: UIContext, controller: KRNativeRenderController) {
    this.mInstanceId = instanceId;
    this.uiContext = uiContext;
    this.controller = controller;
  }

  public registerExceptionCallback(callback: KRExceptionCallback | null) {
    this.exceptionCallback = callback;
  }

  public fireExceptionCallback(stack: string) {
    if (this.exceptionCallback != null) {
      this.exceptionCallback(this.controller?.executeMode || KRRenderNativeMode.Native, stack);
    }
  }

  public getUIContext(): UIContext {
    return this.uiContext as UIContext;
  }

  public getNativeRenderController(): KRNativeRenderController | null {
    return this.controller;
  }

  public createView(tag: number, viewType: string) {
    let creator: KRViewCreator | undefined = KRNativeInstance.viewCreatorRegistry.get(viewType);
    if (creator) {
      let view: KuiklyRenderBaseView = creator();
      if (view) {
        view.init(this.mInstanceId, tag, viewType);
        this.viewRegistry.set(tag, view);
      }
    }
  }

  // 根据tag获取自定义view
  public viewWithTag(tag: number): KuiklyRenderBaseView | null {
    let view = this.viewRegistry.get(tag);
    if (view) {
      return view;
    }
    return null;
  }

  public generateViewBuilder(tag: number, nodeId: string): KRAny {
    if (this.viewRegistry.has(tag)) {
      let view = this.viewRegistry.get(tag);
      if (view) {
        let arr = [view.toCreateArkUIView(nodeId), view.getContentSlot()]
        return arr
      }
    }
    return null;
  }

  // 设置View属性
  public setViewProp(tag: number, propKey: string, propValue: KRValue) {
    if (this.viewRegistry.has(tag)) {
      let view = this.viewRegistry.get(tag);
      if (view) {
        view.toSetProp(propKey, propValue);
      }
    }
  }

  // 设置View事件
  public setViewEvent(tag: number, propKey: string, callback: KuiklyRenderCallback) {
    if (this.viewRegistry.has(tag)) {
      let view = this.viewRegistry.get(tag);
      if (view) {
        view.toSetEvent(propKey, callback);
      }
    }
  }

  // 调用View方法
  public callViewMethod(tag: number, method: string, params: KRAny, callback: KuiklyRenderCallback | null) {
    if (this.viewRegistry.has(tag)) {
      let view = this.viewRegistry.get(tag);
      if (view) {
        view.toCall(method, params, callback);
      }
    }
  }

  // 删除View
  public removeView(tag: number) {
    if (this.viewRegistry.has(tag)) {
      let view = this.viewRegistry.get(tag);
      if (view) {
        view.toDestroy();
        this.viewRegistry.delete(tag);
      }
    }
  }

  public didMoveToParentView(tag: number) {
    if (this.viewRegistry.has(tag)) {
      let view = this.viewRegistry.get(tag);
      if (view) {
        view.toDidMoveToParentView();
      }
    }
  }

  public setViewSize(tag: number, width: number, height: number) {
    if (this.viewRegistry.has(tag)) {
      let view = this.viewRegistry.get(tag);
      if (view) {
        view.toSizeDidChanged(width, height);
      }
    }
  }


  /**
   * 注册ArkTS侧Module创建器(一个页面最多有一个同module实例)
   * @param moduleName 和native侧该module同名
   * @param moduleCreator  module实例创建器
   */
  public static registerModuleCreator(moduleName: string, moduleCreator: KRRenderModuleExportCreator) {
    KRNativeInstance.moduleCreatorRegistry.set(moduleName, moduleCreator);
  }

  /**
   * 注册ArkTS侧View创建器
   * @param viewType 对应Kotlin侧的viewName返回字符串作为viewType注册，如：'KRImageView'
   */
  public static registerViewCreator(viewType: string, viewCreator: KRViewCreator) {
    KRNativeInstance.viewCreatorRegistry.set(viewType, viewCreator);
  }


  /**
   * 获取Module实例（不存在就创建）
   * @param moduleName
   * @returns
   */
  public getOrCreateModule(moduleName: string): IKuiklyRenderModuleExport | null {
    if (this.modules.has(moduleName)) {
      let module = this.modules.get(moduleName);
      if (module != undefined) {
        return module;
      }
    }
    if (KRNativeInstance.moduleCreatorRegistry.has(moduleName)) {
      let creator: KRRenderModuleExportCreator | undefined = KRNativeInstance.moduleCreatorRegistry.get(moduleName);
      if (creator != undefined) {
        let module: IKuiklyRenderModuleExport | null = creator();
        if (module != null) {
          if (module instanceof KuiklyRenderBaseModule) {
            module.init(this.mInstanceId);
          }
          this.modules.set(moduleName, module);
        }
        return module;
      }
    }
    return null;

  }

  public callModuleMethodFromNative(arg0: KRAny, arg1: KRAny, arg2: KRAny, arg3: KRAny, arg4: KRAny,
    callback: KuiklyRenderCallback | null): KRAny {
    let moduleName = arg0 as string;
    let moduleInstance = this.getOrCreateModule(moduleName);
    if (moduleInstance != null) {
      let methodName = arg1 as string;
      return moduleInstance.call(methodName, arg2, callback);
    }
    return null;
  }

  /**
   * 销毁时调用
   */
  public onDestroy() {
    this.uiContext = null;
    this.viewRegistry.forEach((value, key, _) => {
      value.onDestroy();
    });
    this.viewRegistry.clear();
    this.modules.forEach((value, key, _) => {
      value.onDestroy();
    });
    this.modules.clear();
  }
}