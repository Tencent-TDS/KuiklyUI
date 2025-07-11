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

import { IKRLogAdapter } from './IKRLogAdapter';
import { IKRRouterAdapter } from './IKRRouterAdapter';
import { IKRColorParserAdapter } from './IKRColorParserAdapter';
import { IKRPAGViewAdapter } from './IKRPAGViewAdapter';
import { IKRVideoViewAdapter } from './IKRVideoViewAdapter';
import render from 'libkuikly.so';
import { image } from '@kit.ImageKit';

/**
 * 图片回调函数
 * @param src 新图片源
 * @param pixelMap 适配器实际获取到的图片资源
 * 注：Kuikly内部会优先使用pixelMap作为图片资源，如果pixelMap为空，则会根据src内部加载图片资源
 */
export type ImageCallback = (src: string, pixelMap: image.PixelMap | null) => void;
/**
 * 业务注册的图片适配器函数
 * @param src 图片源
 * @param callback 图片回调函数，在适配器函数内部获取到图片资源后需主动调用
 */
export type ImageAdapterFunc = (src: string, callback: ImageCallback) => void;

export class KuiklyRenderAdapterManager {
  /**
   * 页面跳转适配器
   */
  static krRouterAdapter?: IKRRouterAdapter | null = null;
  /**
   * Log适配器
   */
  static krLogAdapter: IKRLogAdapter | null = null;
  /**
   * 颜色转换器
   */
  static krColorParserAdapter: IKRColorParserAdapter | null = null;
  /**
   * 视频控件适配器
   */
  static krVideoViewAdapter: IKRVideoViewAdapter | null = null;
  /**
   * Pag适配器
   */
  static krPagAdapter: IKRPAGViewAdapter | null = null;
  /**
   * 注册图片适配器
   */
  static registerKRImageAdapter(imageAdapterFunc: ImageAdapterFunc) {
    render.registerImageAdapter((src: string, callbackId: string) => {
      imageAdapterFunc(src, (src: string, pixelMap: image.PixelMap | null) => {
        render.fireImageCallback(src, pixelMap, callbackId);
      })
    });
  }
}