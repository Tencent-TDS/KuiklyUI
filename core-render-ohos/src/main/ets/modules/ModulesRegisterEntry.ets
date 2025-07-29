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

import { KRNativeManager } from '../manager/KRNativeManager';
import { IKuiklyRenderModuleExport } from './base/IKRModuleExport';
import { KRSnapshotModule } from './internal/KRSnapshotModule';
import { KRLogModuleArkTS } from './KRLogModuleArkTS';
import { KRNotifyModule } from './KRNotifyModule';
import { KRExceptionModule } from './KRExceptionModule';
import { KRNetworkModule } from './KRNetworkModule';
import { KRRouterModule } from './KRRouterModule';

/**
 * ArkTS侧内置Module均在此注册生成实例闭包
 */
export class ModulesRegisterEntry {
  static registerExternalModulesCallback: () => void = () => {
  };

  static registerSDKModules() {
    // 注册Router Module
    KRNativeManager.getInstance().registerModuleCreator('KRRouterModule', (): IKuiklyRenderModuleExport => {
      return new KRRouterModule();
    });
    KRNativeManager.getInstance().registerModuleCreator(KRExceptionModule.MODULE_NAME, (): IKuiklyRenderModuleExport => {
        return new KRExceptionModule();
      });
    KRNativeManager.getInstance().registerModuleCreator(KRNetworkModule.MODULE_NAME, (): IKuiklyRenderModuleExport => {
      return new KRNetworkModule();
    });
    KRNativeManager.getInstance().registerModuleCreator(KRSnapshotModule.MODULE_NAME, (): IKuiklyRenderModuleExport => {
      return new KRSnapshotModule();
    });
    KRNativeManager.getInstance().registerModuleCreator(KRNotifyModule.MODULE_NAME, (): IKuiklyRenderModuleExport => {
      return new KRNotifyModule();
    });
    KRNativeManager.getInstance().registerModuleCreator(KRLogModuleArkTS.MODULE_NAME, (): IKuiklyRenderModuleExport => {
      return new KRLogModuleArkTS();
    });
  }

  static registerModuleCreator(moduleName: string, moduleCreator: () => IKuiklyRenderModuleExport) {
    KRNativeManager.getInstance().registerModuleCreator(moduleName, moduleCreator);
  }
}