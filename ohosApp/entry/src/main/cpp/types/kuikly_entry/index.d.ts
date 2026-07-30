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

export const injectToKTRender: () => void
export const initKuikly: () => number;
export const saveImageOfInternet: (internetUrl: string, tarPath: string, tarName: string, callback: Function) => string;
export const setFontPath: (path: string) => number;
export const setResourceManager: (resmgr: resourceManager.ResourceManager) => number;

/**
 * 把应用沙盒 cacheDir 下发给 native，供业务侧 adapter 需要写入沙盒时使用。
 * 应在 EntryAbility.onCreate / onWindowStageCreate 时尽早调用。
 */
export const setCacheDir: (path: string) => number;

/**
 * 把 hap 的 resfile 根路径（context.resourceDir）下发给 native，供
 * TextPostProcessor 等业务侧 adapter 拼接 "file://${resourceDir}/${rel}" URI
 * 后交给 SDK（resfile 中的资源由 hvigorfile.ts 的 kuiklyCopyAssets 从
 * demo/commonMain/assets 同步过来）。应在 EntryAbility.onWindowStageCreate 时尽早调用。
 */
export const setResfileDir: (path: string) => number;

/**
 * 设置输入控件新实现开关。0=走老的 KRTextFieldView/KRTextAreaView；
 * 1=在 API>=24 时走新的 KRTextEditorFieldView/KRTextEditorAreaView。
 * 只影响设置后新创建的 Input/TextArea。开关值实际存储在 libkuikly.so 内，
 * 本方法通过动态链接符号透传到 core-render-ohos 的 C API。
 *
 * 说明：EmojiTextInputDemo 依赖的新输入组件已在 native `initKuikly()` 阶段统一开启，
 * 这里保留该接口仅供 `InputCompareDemoPage` 一类的新/老实现对比场景显式切换。
 */
export const setUseNewTextInputComponent: (value: number) => number;