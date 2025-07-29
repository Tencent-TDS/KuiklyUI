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
import { KuiklyRenderAdapterManager } from '../adapter/KRAdapterManager';
import { IKRPAGViewController, IKRPAGViewListener } from '../adapter/IKRPAGViewAdapter';

@Builder
function createEmptyView(view: KuiklyRenderBaseView) {
}

export class KRPAGView extends KuiklyRenderBaseView implements IKRPAGViewListener {
  override init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
    this.controller = KuiklyRenderAdapterManager.krPagAdapter?.createController();
    this.controller?.addListener(this);
  }

  onAnimationStart = (viewController: IKRPAGViewController) => {
    if (this.animationStartEvent) {
      this.animationStartEvent('{}');
    }
  };
  onAnimationEnd = (viewController: IKRPAGViewController) => {
    if (this.animationEndEvent) {
      this.animationEndEvent('{}');
    }
  };
  onAnimationRepeat = (viewController: IKRPAGViewController) => {
    if (this.animationRepeatEvent) {
      this.animationRepeatEvent('{}');
    }
  };
  onAnimationCancel = (viewController: IKRPAGViewController) => {
    if (this.animationCancelEvent) {
      this.animationCancelEvent('{}');
    }
  };
  // onAnimationUpdate = (viewController: IKRPAGViewController) => {
  // }

  loadFailureEvent: KuiklyRenderCallback | null = null;
  animationStartEvent: KuiklyRenderCallback | null = null;
  animationEndEvent: KuiklyRenderCallback | null = null;
  animationCancelEvent: KuiklyRenderCallback | null = null;
  animationRepeatEvent: KuiklyRenderCallback | null = null;
  src: string = '';
  autoPlay: boolean = true;
  repeatCount = 0;
  controller: IKRPAGViewController | undefined;
  static readonly VIEW_NAME = 'KRPAGView';
  private static readonly PROP_SRC = 'src';
  private static readonly PROP_REPEAT_COUNT = 'repeatCount';
  private static readonly PROP_AUTO_PLAY = 'autoPlay';
  private static readonly EVENT_LOAD_FAIL = 'loadFailure';
  private static readonly EVENT_ANIMATION_START = 'animationStart';
  private static readonly EVENT_ANIMATION_END = 'animationEnd';
  private static readonly EVENT_ANIMATION_Cancel = 'animationCancel';
  private static readonly EVENT_ANIMATION_REPEAT = 'animationRepeat';
  private static readonly PROP_METHOD_PLAY = 'play';
  private static readonly PROP_METHOD_STOP = 'stop';

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    console.log(`KRPAGView setProp propKey:${propKey}, propValue:${propValue}`);
    if (propKey == KRPAGView.EVENT_LOAD_FAIL) {
      this.loadFailureEvent = propValue as KuiklyRenderCallback;
    } else if (propKey == KRPAGView.EVENT_ANIMATION_START) {
      this.animationStartEvent = propValue as KuiklyRenderCallback;
    } else if (propKey == KRPAGView.EVENT_ANIMATION_END) {
      this.animationEndEvent = propValue as KuiklyRenderCallback;
    } else if (propKey == KRPAGView.EVENT_ANIMATION_Cancel) {
      this.animationCancelEvent = propValue as KuiklyRenderCallback;
    } else if (propKey == KRPAGView.EVENT_ANIMATION_REPEAT) {
      this.animationRepeatEvent = propValue as KuiklyRenderCallback;
    } else {
      if (this.controller) {
        return this.controller?.setProp(propKey, propValue);
      } else {
        return false;
      }
    }

    return true;
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {
    console.log(`KRPAGView call method :${method}`);
    if (method == KRPAGView.PROP_METHOD_PLAY) {
      this.controller?.play();
    } else if (method == KRPAGView.PROP_METHOD_STOP) {
      this.controller?.stop();
    }
  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    if (KuiklyRenderAdapterManager.krPagAdapter != null) {
      return KuiklyRenderAdapterManager.krPagAdapter?.createPAGView(uiContext, this);
    }
    return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createEmptyView), this)
  }
}