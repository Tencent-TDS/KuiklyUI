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

export { KRNative } from './src/main/ets/KRNative';

export { KRNative as KTNative } from './src/main/ets/KRNative';

export * from './src/main/ets/utils/KRDataTypes';

// 兼容旧版本
export {
  KRValue as KTValue,
  KRArray as KTArray,
  KRRecord as KTRecord,
  KRAny as KTAny,
} from './src/main/ets/utils/KRDataTypes';

export * from './src/main/ets/adapter/export';

export * from './src/main/ets/exception/KuiklyRenderExportExceptions';

export { KuiklyNativeManager } from './src/main/ets/native/KuiklyNativeManager';

export * from './src/main/ets/modules/export';

export { KRNativeRender as Kuikly } from './src/main/ets/KRNativeRender';

export { IKuiklyViewDelegate, KRNativeRenderController } from './src/main/ets/KRNativeRenderController';

// 兼容旧版本
export { KRNativeRenderController as KTNativeRenderController } from './src/main/ets/KRNativeRenderController';

export { KuiklyRenderBaseView, KRRenderViewExportCreator, KRPAGView } from './src/main/ets/components/export';

export { KuiklyRenderAdapterManager } from './src/main/ets/adapter/KRAdapterManager';

export { ViewsRegisterEntry } from './src/main/ets/components/ViewsRegisterEntry';

export { ModulesRegisterEntry } from './src/main/ets/modules/ModulesRegisterEntry';

export { KRConvertUtil } from './src/main/ets/utils/KRConvertUtil';

// 兼容旧版本
export { KRConvertUtil as KTConvertUtil } from './src/main/ets/utils/KRConvertUtil';

export { IKuiklyRenderContextHandler,
  IKuiklyContextNativeCommunicator } from './src/main/ets/context/IKuiklyRenderContextHandler';

export { IKRPAGViewAdapter, IKRPAGViewController, IKRPAGViewListener } from './src/main/ets/adapter/IKRPAGViewAdapter';

export { KRRenderExecuteModeBase } from './src/main/ets/context/KRRenderExecuteModeBase';

export { KRRenderNativeMode } from './src/main/ets/context/KRRenderNativeMode';