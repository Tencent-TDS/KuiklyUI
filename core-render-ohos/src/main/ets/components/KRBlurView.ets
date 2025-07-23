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

import { KuiklyRenderBaseView } from './base/KRBaseViewExport';
import { ComponentContent } from '@kit.ArkUI';
import { KRAny, KRValue, KuiklyRenderCallback } from '../utils/KRDataTypes';


@Builder
function createArkUIView(view: KuiklyRenderBaseView) {
  Column() {
    Column() {

    }
    .width('100%')
    .height('100%')
    .backdropBlur((view as KRBlurView).blurRadius);
  }.width('100%')
  .height('100%');
}

export class KRBlurView extends KuiklyRenderBaseView {
  blurRadius: number = 0;

  init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
    this.setBlurRadius(10); // 默认值
  }

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    if (propKey == 'blurRadius') {
      this.setBlurRadius(propValue as number);
      return true;
    }
    return false;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {

  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createArkUIView),
      this);
  }

  didMoveToParentView(): void {
  }

  sizeDidChanged(width: number, height: number): void {
  }

  onDestroy(): void {
  }

  private setBlurRadius(blur: number) {
    this.blurRadius = 150 * (blur as number) / 10;
  }
}
