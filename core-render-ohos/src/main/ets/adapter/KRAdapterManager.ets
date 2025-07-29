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

import { IKRLogAdapter } from './IKRLogAdapter';
import { IKRRouterAdapter } from './IKRRouterAdapter';
import { IKRColorParserAdapter } from './IKRColorParserAdapter';
import { IKRPAGViewAdapter } from './IKRPAGViewAdapter';
import { IKRVideoViewAdapter } from './IKRVideoViewAdapter';


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
}