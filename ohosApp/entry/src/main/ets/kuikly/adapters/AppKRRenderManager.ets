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

import { KuiklyRenderAdapterManager } from '@kuikly-open/render';
import { AppKRLogAdapter } from './AppKRLogAdapter';
import { AppKRPagViewAdapter } from './AppKRPAGAdapter';
import { AppKRRouterAdapter } from './AppKRRouterAdapter';
import { AppKRVideoViewAdapter } from './AppKRVideoAdapter';

export class AppKRRenderManager {
  // 静态实例，用于存储唯一的实例
  private static instance: AppKRRenderManager;
  // 是否初始化过
  private didInit: boolean = false;

  // 私有构造函数，确保不能通过 new 关键字创建新实例
  private constructor() {
  }

  // 静态方法，用于获取唯一的实例
  public static getInstance(): AppKRRenderManager {
    if (!AppKRRenderManager.instance) {
      AppKRRenderManager.instance = new AppKRRenderManager();
    }
    return AppKRRenderManager.instance;
  }

  /**
   * 初始化KRRender
   */
  public initIfNeed() {
    if (this.didInit) {
      return;
    }
    this.didInit = true;
    this.registerAdapter();
  }

  // 注册Adapter
  private registerAdapter() {
    KuiklyRenderAdapterManager.krRouterAdapter = new AppKRRouterAdapter();
    KuiklyRenderAdapterManager.krLogAdapter = new AppKRLogAdapter();
    KuiklyRenderAdapterManager.krPagAdapter = new AppKRPagViewAdapter();
    KuiklyRenderAdapterManager.krVideoViewAdapter = new AppKRVideoViewAdapter();
  }
}