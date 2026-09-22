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
import { IKRAudioViewController, IKRAudioViewListener } from '../adapter/IKRAudioViewAdapter';

export enum KRAudioPlayState {
  Unknown = 0,
  Playing = 1, // 正在播放中
  Caching = 2, // 缓冲中
  Paused = 3,  // 播放暂停
  PlayEnd = 4, // 播放结束
  Failed = 5   // 播放失败
}

/// 播控操作状态化维护
enum KRAudioViewPlayControl {
  None = 0,
  Play = 1,  // 操作播放音频
  Pause = 2, // 操作暂停音频
  Stop = 3   // 操作停止音频
}

@Builder
function createEmptyView(view: KuiklyRenderBaseView) {
}

export class KRAudioView extends KuiklyRenderBaseView implements IKRAudioViewListener {
  override init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
    this.controller = KuiklyRenderAdapterManager.krAudioViewAdapter?.createController();
    this.controller?.addListener(this);
  }

  onPlayStateDidChanged = (state: KRAudioPlayState, viewController: IKRAudioViewController) => {
    this.isPlaying = (state === KRAudioPlayState.Playing);
    if (this.playStateDidChangedEvent) {
      this.playStateDidChangedEvent({ 'state': state });
    }
  };
  onPlayTimeDidChanged = (currentTime: number, totalTime: number, viewController: IKRAudioViewController) => {
    if (this.playTimeDidChangedEvent) {
      this.playTimeDidChangedEvent({ 'currentTime': currentTime, 'totalTime': totalTime });
    }
  };
  onCustomEventWithInfo = (viewController: IKRAudioViewController, info: Map<string, string>) => {
    if (this.customEventWithInfoEvent) {
      this.customEventWithInfoEvent(info);
    }
  };
  playStateDidChangedEvent: KuiklyRenderCallback | null = null;
  playTimeDidChangedEvent: KuiklyRenderCallback | null = null;
  customEventWithInfoEvent: KuiklyRenderCallback | null = null;
  src: string = '';
  controller: IKRAudioViewController | undefined;
  isPlaying: boolean = false;
  static readonly VIEW_NAME = 'KRAudioView';
  private static readonly PROP_SRC = 'src';
  private static readonly PROP_PLAY_CONTROL = 'playControl';
  private static readonly PROP_RATE = 'rate';
  private static readonly PROP_SEEK = 'seek';
  private static readonly EVENT_PLAY_STATE_CHANGE = 'stateChange';
  private static readonly EVENT_PLAY_TIME_CHANGE = 'playTimeChange';
  private static readonly EVENT_CUSTOM_EVENT = 'customEvent';

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case KRAudioView.PROP_SRC:
        this.controller?.setSrc(propValue as string);
        break;
      case KRAudioView.PROP_PLAY_CONTROL:
        this.setPlayControl(propValue as KRAudioViewPlayControl);
        break;
      case KRAudioView.PROP_RATE:
        this.controller?.setRate(propValue as number);
        break;
      case KRAudioView.PROP_SEEK:
        this.controller?.seekToTime(propValue as number);
        break;
      case KRAudioView.EVENT_PLAY_STATE_CHANGE:
        this.playStateDidChangedEvent = propValue as KuiklyRenderCallback;
        break;
      case KRAudioView.EVENT_PLAY_TIME_CHANGE:
        this.playTimeDidChangedEvent = propValue as KuiklyRenderCallback;
        break;
      case KRAudioView.EVENT_CUSTOM_EVENT:
        this.customEventWithInfoEvent = propValue as KuiklyRenderCallback;
        break;

      default:
        if (this.controller) {
          return this.controller.setProp(propKey, propValue);
        } else {
          return false;
        }
    }
    return true;
  }

  setPlayControl(playControl: KRAudioViewPlayControl) {
    switch (playControl) {
      case KRAudioViewPlayControl.Play:
        this.controller?.play();
        break;
      case KRAudioViewPlayControl.Pause:
        this.controller?.pause();
        break;
      case KRAudioViewPlayControl.Stop:
        this.controller?.stop();
        break;
      default:
        break;
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {

  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    if (KuiklyRenderAdapterManager.krAudioViewAdapter != null) {
      return KuiklyRenderAdapterManager.krAudioViewAdapter?.createAudioView(uiContext, this);
    }
    return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createEmptyView),
      this);
  }
}
