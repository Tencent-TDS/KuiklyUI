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
import { IKRVideoViewController, IKRVideoViewListener } from '../adapter/IKRVideoViewAdapter';

export enum KRVideoPlayState {
  Unknown = 0,
  Playing = 1, // 正在播放中 （注：回调该状态时，视频应该是有画面的）
  Caching = 2, // 缓冲中 （注：如果未调用过VAVideoPlayStatusPlaying状态，不能调用该状态）
  Paused = 3, // 播放暂停 （注：如果一个视频处于PrepareToPlay状态，此时调用了暂停操作， 应该回调该状态）
  PlayEnd = 4, // 播放结束
  Failed = 5 // 播放失败
}

export enum KRVideoViewContentMode {
  Contain, // 按原视频比例显示，是竖屏的就显示出竖屏的，两边留黑；
  Cover, // 按原比例拉伸视频，直到两边都占满
  Stretch, // 拉伸视频内容达到边框占满，但不按原比例拉伸
}

/// 播控操作状态化维护
enum KRVideoViewPlayControl {
  None = 0,
  Preplay = 1, //操作预播放视频
  Play = 2, // 操作播放视频
  Pause = 3, // 操作暂停视频
  Stop = 4 // 操作停止视频
};

@Builder
function createEmptyView(view: KuiklyRenderBaseView) {
}

export class KRVideoView extends KuiklyRenderBaseView implements IKRVideoViewListener {
  override init(instanceId: string, viewTag: number, viewType: string): void {
    super.init(instanceId, viewTag, viewType);
    this.controller = KuiklyRenderAdapterManager.krVideoViewAdapter?.createController();
    this.controller?.addListener(this);
  }

  onPlayStateDidChanged = (state: KRVideoPlayState, viewController: IKRVideoViewController) => {
    if (this.playStateDidChangedEvent) {
      this.playStateDidChangedEvent({ 'state': state });
    }
  };
  onPlayTimeDidChanged = (currentTime: number, totalTime: number, viewController: IKRVideoViewController) => {
    if (this.playTimeDidChangedEvent) {
      this.playTimeDidChangedEvent({ 'currentTime': currentTime, 'totalTime': totalTime });
    }
  };
  onFirstFrameDidDisplay = (viewController: IKRVideoViewController) => {
    if (this.firstFrameDidDisplayEvent) {
      this.firstFrameDidDisplayEvent({});
    }
  };
  onCustomEventWithInfo = (viewController: IKRVideoViewController, info: Map<string, string>) => {
    if (this.customEventWithInfoEvent) {
      this.customEventWithInfoEvent(info);
    }
  };
  playStateDidChangedEvent: KuiklyRenderCallback | null = null;
  playTimeDidChangedEvent: KuiklyRenderCallback | null = null;
  firstFrameDidDisplayEvent: KuiklyRenderCallback | null = null;
  customEventWithInfoEvent: KuiklyRenderCallback | null = null;
  src: string = '';
  controller: IKRVideoViewController | undefined;
  static readonly VIEW_NAME = 'KRVideoView';
  private static readonly PROP_SRC = 'src';
  private static readonly PROP_PLAY_CONTROL = 'playControl';
  private static readonly PROP_MUTED = 'muted';
  private static readonly PROP_RATE = 'rate';
  private static readonly PROP_RESIZE_MODE = 'resizeMode';
  private static readonly EVENT_PLAY_STATE_CHANGE = 'stateChange';
  private static readonly EVENT_PLAY_TIME_CHANGE = 'playTimeChange';
  private static readonly EVENT_FIRST_FRAME = 'firstFrame';
  private static readonly EVENT_CUSTOM_EVENT = 'customEvent';

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    switch (propKey) {
      case KRVideoView.PROP_SRC:
        this.controller?.setSrc(propValue as string);
        break;
      case KRVideoView.PROP_PLAY_CONTROL:
        this.setPlayControl(propValue as KRVideoViewPlayControl);
        break;
      case KRVideoView.PROP_MUTED:
        this.controller?.setMuted(propValue as boolean);
        break;
      case KRVideoView.PROP_RATE:
        this.controller?.setRate(propValue as number);
        break;
      case KRVideoView.PROP_RESIZE_MODE:
        this.setResizeMode(propValue as string);
        break;
      case KRVideoView.EVENT_PLAY_STATE_CHANGE:
        this.playStateDidChangedEvent = propValue as KuiklyRenderCallback;
        break;
      case KRVideoView.EVENT_PLAY_TIME_CHANGE:
        this.playTimeDidChangedEvent = propValue as KuiklyRenderCallback;
        break;
      case KRVideoView.EVENT_FIRST_FRAME:
        this.firstFrameDidDisplayEvent = propValue as KuiklyRenderCallback;
        break;
      case KRVideoView.EVENT_CUSTOM_EVENT:
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

  setPlayControl(playControl: KRVideoViewPlayControl) {
    switch (playControl) {
      case KRVideoViewPlayControl.Preplay:
        this.controller?.preplay();
        break;
      case KRVideoViewPlayControl.Play:
        this.controller?.play();
        break;
      case KRVideoViewPlayControl.Pause:
        this.controller?.pause();
        break;
      case KRVideoViewPlayControl.Stop:
        this.controller?.stop();
        break;
      default:
        break;
    }
  }

  setResizeMode(resizeMode: string) {
    if (resizeMode == 'contain') {
      this.controller?.setVideoContentMode(KRVideoViewContentMode.Contain);
    } else if (resizeMode == 'cover') {
      this.controller?.setVideoContentMode(KRVideoViewContentMode.Cover);
    } else if (resizeMode == 'stretch') {
      this.controller?.setVideoContentMode(KRVideoViewContentMode.Stretch);
    }
  }

  call(method: string, params: KRAny, callback: KuiklyRenderCallback | null): void {

  }

  createArkUIView(): ComponentContent<KuiklyRenderBaseView> {
    const uiContext = this.getUIContext() as UIContext;
    if (KuiklyRenderAdapterManager.krVideoViewAdapter != null) {
      return KuiklyRenderAdapterManager.krVideoViewAdapter?.createVideoView(uiContext, this);
    }
    return new ComponentContent<KuiklyRenderBaseView>(uiContext, wrapBuilder<[KuiklyRenderBaseView]>(createEmptyView),
      this);
  }
}