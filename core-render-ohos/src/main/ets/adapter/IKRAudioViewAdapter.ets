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
import { KRAudioPlayState } from '../components/KRAudioView';

export interface IKRAudioViewController {
  /**
   * kuikly侧设置的属性，一般用于业务扩展使用
   */
  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean;

  /**
   * 添加事件监听
   */
  addListener(listener: IKRAudioViewListener): void;

  /**
   * 移除事件监听
   */
  removeListener(listener: IKRAudioViewListener): void;

  /**
   * 设置音频源
   */
  setSrc(src: string): void;

  /**
   * 播放音频
   */
  play(): void;

  /**
   * 暂停音频
   */
  pause(): void;

  /**
   * 停止并销毁音频
   */
  stop(): void;

  /**
   * 设置倍速（1.0, 1.25, 1.5, 2.0）
   */
  setRate(rate: number): void;

  /**
   * seek音频
   * @param seekToTimeMs 时间，单位毫秒
   */
  seekToTime(seekToTimeMs: number): void;
}

export interface IKRAudioViewListener {
  /*
   * 播放状态发生变化时回调
   */
  onPlayStateDidChanged?: (state: KRAudioPlayState, viewController: IKRAudioViewController) => void;

  /*
   * 播放时间发生变化时回调
   * @param currentTime 当前播放时间，单位毫秒
   * @param totalTime 音频总时长，单位毫秒
   */
  onPlayTimeDidChanged?: (currentTime: number, totalTime: number, viewController: IKRAudioViewController) => void;

  /*
   * 业务自定义扩展事件通用事件通道
   */
  onCustomEventWithInfo?: (viewController: IKRAudioViewController, info: Map<string, string>) => void;
}

export interface IKRAudioViewAdapter {
  createController(): IKRAudioViewController
  createAudioView(ctx: UIContext, view: KuiklyRenderBaseView): ComponentContent<KuiklyRenderBaseView>
}
