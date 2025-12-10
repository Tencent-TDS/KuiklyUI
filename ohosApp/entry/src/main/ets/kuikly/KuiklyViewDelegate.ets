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
    return map;
  }

  getKuiklyRenderViewLifecycleCallback(): IKuiklyRenderViewLifecycleCallback | null {
    return new MyLifecycleCallBack()
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