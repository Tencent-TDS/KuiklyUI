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

import { IKuiklyRenderViewLifecycleCallback, IKuiklyViewDelegate, KRRenderModuleExportCreator,KRRenderViewExportCreator } from '@kuikly-open/render';
import { KRMyView } from './components/KRMyView';
import { KRMyDemoCustomView } from './components/KRMyDemoCustomView';
import { KRMyModule } from './modules/KRMyModule';
import { KRBridgeModule } from './modules/KRBridgeModule';
import { KROhosStreamRequestModule } from './modules/KROhosStreamRequestModule';
import { KuiklyPageView } from './components/KuiklyPageView';
import { KRMyLogModule } from './modules/KRMyLogModule';


export class KuiklyViewDelegate extends IKuiklyViewDelegate {
  getCustomRenderViewCreatorRegisterMap(): Map<string, KRRenderViewExportCreator>{
    const map: Map<string, KRRenderViewExportCreator> = new Map();
    map.set(KRMyView.VIEW_NAME, () => new KRMyView());
    map.set(KuiklyPageView.VIEW_NAME, () => new KuiklyPageView());
    return map;
  }

  getCustomRenderViewCreatorRegisterMapV2(): Map<string, KRRenderViewExportCreator> {
    const map: Map<string, KRRenderViewExportCreator> = new Map();
    map.set(KRMyDemoCustomView.VIEW_NAME, () => new KRMyDemoCustomView());
    return map;
  }

  getCustomRenderModuleCreatorRegisterMap(): Map<string, KRRenderModuleExportCreator> {
    const map: Map<string, KRRenderModuleExportCreator> = new Map();
    map.set(KRBridgeModule.MODULE_NAME, () => new KRBridgeModule());
    //map.set(HRBridgeModule.MODULE_NAME, () => new HRBridgeModule());

    map.set(KRMyModule.MODULE_NAME, () => new KRMyModule());
    map.set(KROhosStreamRequestModule.MODULE_NAME, () => new KROhosStreamRequestModule());
    map.set(KRMyLogModule.MODULE_NAME, () => new KRMyLogModule());
    return map;
  }

  getKuiklyRenderViewLifecycleCallback(): IKuiklyRenderViewLifecycleCallback | null {
    return new MyLifecycleCallBack()
  }

  // 控制 Native -> Kotlin 事件是否以同步方式发送，避免事件异步派发后晚于当前原生布局/上屏时机生效。
  // 典型场景：页面旋转、分屏或宿主容器尺寸突变时，可对 `rootViewSizeDidChanged` 返回 true，避免尺寸变化晚一帧生效导致白屏。
  syncSendEvent(event: string): boolean {
    // 若页面尺寸发生变化时，出现白屏，可恢复以下被注释的代码
    // if (event === "rootViewSizeDidChanged") {
    //   return true;
    // }
    return false;
  }
}

class MyLifecycleCallBack implements IKuiklyRenderViewLifecycleCallback {
  onInitCoreStart(): void {
    console.log("--KuiklyRenderView onInitCoreStart--")
  }

  onInitCoreFinish(): void {
    console.log("--KuiklyRenderView onInitCoreFinish--")
  }
  onInit(): void {
    console.log("--KuiklyRenderView onInit--")
  }

  onInitContextStart(): void {
    console.log("--KuiklyRenderView onInitContextStart--")
  }

  onInitContextFinish(): void {
    console.log("--KuiklyRenderView onInitContextFinish--")
  }

  onCreateInstanceStart(): void {
    console.log("--KuiklyRenderView onCreateInstanceStart--")
  }

  onCreateInstanceFinish(): void {
    console.log("--KuiklyRenderView onCreateInstanceFinish--")
  }

  onFirstFramePaint(): void {
    console.log("--KuiklyRenderView onFirstFramePaint--")
  }
}