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

import { KRValue, KuiklyRenderCallback } from '../utils/KRDataTypes';
import { KuiklyRenderBaseView } from '../components/base/KRBaseViewExport';
import { ComponentContent } from '@kit.ArkUI';
import { KRVideoPlayState, KRVideoViewContentMode } from '../components/KRVideoView';

export interface IKRVideoViewController {
  /**
   * kuikly侧设置的属性，一般用于业务扩展使用
   */
  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean;

  /**
   * 添加事件监听
   */
  addListener(listener: IKRVideoViewListener): void;

  /**
   * 移除事件监听
   */
  removeListener(listener: IKRVideoViewListener): void;

  /**
   * 设置视频源
   */
  setSrc(src: string): void;

  /**
   * 预播放到第一帧（停在第一帧，用于预加载优化）
   */
  preplay(): void;

  /**
   * 播放视频
   */
  play(): void;

  /**
   * 暂停视频
   */
  pause(): void;

  /**
   * 停止并销毁视频
   */
  stop(): void;

  /**
   * 设置画面拉伸模式
   */
  setVideoContentMode(videoViewContentMode: KRVideoViewContentMode): void;

  /**
   * 设置静音属性
   */
  setMuted(muted: boolean): void;

  /**
   * 设置倍速（1.0, 1.5, 2.0）
   */
  setRate(rate: number): void;

  /**
   * seek视频
   * @param seekToTimeMs 时间，单位毫秒
   */
  seekToTime(seekToTimeMs: number): void;
}

export interface IKRVideoViewListener {
  /*
   * 播放状态发生变化时回调
   */
  onPlayStateDidChanged?: (state: KRVideoPlayState, viewController: IKRVideoViewController) => void;

  /*
   * 播放时间发生变化时回调
   * @param currentTime 当前播放时间，单位毫秒
   * @param totalTime 视频总时长，单位毫秒
   */
  onPlayTimeDidChanged?: (currentTime: number, totalTime: number, viewController: IKRVideoViewController) => void;

  /*
   * 视频首帧画面上屏显示时回调该方法（kotlin侧通过该时机来隐藏视频封面）
   */
  onFirstFrameDidDisplay?: (viewController: IKRVideoViewController) => void;

  /*
   * 业务自定义扩展事件通用事件通道
   */
  onCustomEventWithInfo?: (viewController: IKRVideoViewController, info: Map<string, string>) => void;
}

export interface IKRVideoViewAdapter {
  createController(): IKRVideoViewController
  createVideoView(ctx: UIContext, view: KuiklyRenderBaseView): ComponentContent<KuiklyRenderBaseView>
}
