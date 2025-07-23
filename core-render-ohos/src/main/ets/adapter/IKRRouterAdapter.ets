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

import { KRRecord } from '../utils/KRDataTypes';
import { common } from '@kit.AbilityKit';

export interface IKRRouterAdapter {
  /**
   * 打开一个 Kuikly 页面
   * @param context 上下文
   * @param pageName 页面名字
   * @param pageData 页面参数
   */
  openPage(context: common.UIAbilityContext, pageName: string, pageData: KRRecord): void;

  /**
   * 关闭当前页面
   * @param 上下文
   */
  closePage(context: common.UIAbilityContext): void;
}