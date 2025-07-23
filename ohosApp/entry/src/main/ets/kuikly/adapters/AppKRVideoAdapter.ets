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

import { KRValue, KuiklyRenderBaseView, KuiklyRenderCallback } from '@kuikly-open/render';
import { UIContext } from '@ohos.arkui.UIContext';
import { ComponentContent } from '@kit.ArkUI';
import {
  IKRVideoViewAdapter,
  IKRVideoViewController,
  IKRVideoViewListener
} from '@kuikly-open/render/src/main/ets/adapter/IKRVideoViewAdapter';
import {
  KRVideoPlayState,
  KRVideoView,
  KRVideoViewContentMode
} from '@kuikly-open/render/src/main/ets/components/KRVideoView';

@Component
struct MyVideoView {
  @State videoViewController: AppKRVideoViewController | null = null;
  @State muted: boolean = false;
  @State rate: number = 1;
  @State imageFit: ImageFit = ImageFit.Cover;
  totalTime: number = 0;

  build() {
    Video({
      controller: this.videoViewController?.viewController,
      src: this.videoViewController?.videoSrc,
      currentProgressRate: this.rate
    })
      .muted(this.muted)
      .objectFit(this.imageFit)
      .onStart(() => {
        this.videoViewController?.onPlayStateDidChanged(KRVideoPlayState.Playing);
      })
      .onPause(() => {
        this.videoViewController?.onPlayStateDidChanged(KRVideoPlayState.Paused);
      })
      .onFinish(() => {
        this.videoViewController?.onPlayStateDidChanged(KRVideoPlayState.PlayEnd);
      })
      .onStop(() => {
        this.videoViewController?.onPlayStateDidChanged(KRVideoPlayState.PlayEnd);
      })
      .onError(() => {
        this.videoViewController?.onPlayStateDidChanged(KRVideoPlayState.Failed);
      })
      .onPrepared((params) => {
        this.videoViewController?.onFirstFrameDidDisplay();
        this.totalTime = params.duration;
      })
      .onUpdate((params) => {
        this.videoViewController?.onPlayTimeDidChanged(params.time, this.totalTime);
      });
  }

  aboutToAppear(): void {
    this.videoViewController?.myVideoViewController.setMutedCallback((muted: boolean) => {
      this.muted = muted;
    });
    this.videoViewController?.myVideoViewController.setRateCallback((rate: number) => {
      this.rate = rate;
    });
    this.videoViewController?.myVideoViewController.setResizeModeCallback((imageFit: ImageFit) => {
      this.imageFit = imageFit;
    });
  }

  aboutToDisappear(): void {
    this.videoViewController?.myVideoViewController.delete();
  }
}

@Builder
function createMyVideoView(view: KuiklyRenderBaseView) {
  MyVideoView({
    videoViewController: (view as KRVideoView).controller! as AppKRVideoViewController
  });
}

class MyVideoViewController {
  private mutedCallback: Function | undefined = () => {
  };
  private rateCallback: Function | undefined = () => {
  };
  private imageFitCallback: Function | undefined = () => {
  };

  setMutedCallback(callback: (muted: boolean) => void): void {
    this.mutedCallback = callback;
  }

  setRateCallback(callback: (rate: number) => void): void {
    this.rateCallback = callback;
  }

  setResizeModeCallback(callback: (imageFit: ImageFit) => void): void {
    this.imageFitCallback = callback;
  }

  setMuted(muted: boolean): void {
    if (this.mutedCallback) {
      this.mutedCallback(muted);
    }
  }

  setRate(rate: number): void {
    if (this.rateCallback) {
      this.rateCallback(rate);
    }
  }

  setResizeMode(imageFit: ImageFit): void {
    if (this.imageFitCallback) {
      this.imageFitCallback(imageFit);
    }
  }

  delete(): void {
    this.mutedCallback = undefined;
    this.rateCallback = undefined;
    this.imageFitCallback = undefined;
  }
}

class AppKRVideoViewController implements IKRVideoViewController {
  viewController: VideoController = new VideoController();
  myVideoViewController: MyVideoViewController = new MyVideoViewController();
  videoSrc: string = '';
  listeners = new Set<IKRVideoViewListener>();

  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean {
    return false;
  }

  addListener(listener: IKRVideoViewListener): void {
    this.listeners.add(listener);
  }

  removeListener(listener: IKRVideoViewListener): void {
    this.listeners.delete(listener);
  }

  setSrc(src: string): void {
    const resdir = getContext().resourceDir;
    if (src.startsWith('assets://')) {
      const trailing = src.substring(8);
      this.videoSrc = resdir + trailing;
    } else {
      this.videoSrc = src;
    }
  }

  preplay(): void {
    // this.viewController.setCurrentTime(0.04)
  }

  play(): void {
    this.viewController.start();
  }

  pause(): void {
    this.viewController.pause();
  }

  stop(): void {
    this.viewController.stop();
  }

  setVideoContentMode(videoViewContentMode: KRVideoViewContentMode): void {
    if (videoViewContentMode == KRVideoViewContentMode.Contain) {
      this.myVideoViewController.setResizeMode(ImageFit.Contain);
    } else if (videoViewContentMode == KRVideoViewContentMode.Cover) {
      this.myVideoViewController.setResizeMode(ImageFit.Cover);
    } else if (videoViewContentMode == KRVideoViewContentMode.Stretch) {
      this.myVideoViewController.setResizeMode(ImageFit.Fill);
    }
  }

  setMuted(muted: boolean): void {
    this.myVideoViewController.setMuted(muted);
  }

  setRate(rate: number): void {
    this.myVideoViewController.setRate(rate);
  }

  seekToTime(seekToTimeMs: number): void {
    this.viewController.setCurrentTime(seekToTimeMs / 1000);
  }

  onPlayStateDidChanged = (state: KRVideoPlayState) => {
    this.listeners.forEach((listener) => {
      if (listener.onPlayStateDidChanged) {
        listener.onPlayStateDidChanged(state, this);
      }
    });
  };
  onPlayTimeDidChanged = (currentTime: number, totalTime: number) => {
    this.listeners.forEach((listener) => {
      if (listener.onPlayTimeDidChanged) {
        listener.onPlayTimeDidChanged(currentTime, totalTime, this);
      }
    });
  };
  onFirstFrameDidDisplay = () => {
    this.listeners.forEach((listener) => {
      if (listener.onFirstFrameDidDisplay) {
        listener.onFirstFrameDidDisplay(this);
      }
    });
  };
  onCustomEventWithInfo = (info: Map<string, string>) => {
    this.listeners.forEach((listener) => {
      if (listener.onCustomEventWithInfo) {
        listener.onCustomEventWithInfo(this, info);
      }
    });
  };
}

export class AppKRVideoViewAdapter implements IKRVideoViewAdapter {
  createController(): IKRVideoViewController {
    return new AppKRVideoViewController();
  }

  createVideoView(ctx: UIContext, view: KuiklyRenderBaseView): ComponentContent<KuiklyRenderBaseView> {
    return new ComponentContent<KuiklyRenderBaseView>(ctx, wrapBuilder<[KuiklyRenderBaseView]>(createMyVideoView),
      view);
  }
}
