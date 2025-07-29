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

export interface IKRPAGViewController {
  setProp(propKey: string, propValue: KRValue | KuiklyRenderCallback): boolean;

  addListener(listener: IKRPAGViewListener): void;

  removeListener(listener: IKRPAGViewListener): void;

  play(): void;

  stop(): void;
}

export interface IKRPAGViewListener {
  onAnimationStart?: (viewController: IKRPAGViewController) => void;
  onAnimationEnd?: (viewController: IKRPAGViewController) => void;
  onAnimationRepeat?: (viewController: IKRPAGViewController) => void;
  onAnimationCancel?: (viewController: IKRPAGViewController) => void;
  onAnimationUpdate?: (viewController: IKRPAGViewController) => void;
}

export interface IKRPAGViewAdapter {
  createController(): IKRPAGViewController;

  createPAGView(ctx: UIContext, view: KuiklyRenderBaseView): ComponentContent<KuiklyRenderBaseView>;
}
