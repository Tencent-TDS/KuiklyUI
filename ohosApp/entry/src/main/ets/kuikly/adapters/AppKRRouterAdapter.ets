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

import { IKRRouterAdapter, KRRecord } from '@kuikly-open/render';
import router from '@ohos.router';
import { common } from '@kit.AbilityKit';
import kuiklyEntry from 'libkuikly_entry.so';

export class AppKRRouterAdapter implements IKRRouterAdapter {
  openPage(context: common.UIAbilityContext, pageName: string, pageData: KRRecord): void {
    // 输入控件新/老实现对比页：入口页通过 pageData.useTextEditorImpl 指定本次要跑哪一套实现，
    // 在这里（真正 pushUrl 之前）同步写入 OHOS 侧全局标志位，保证目标页里新建的
    // Input/TextArea 在 RegisterViewCreator 的 lambda 中能读到正确的值。
    // 注意：该 napi 由 libkuikly_entry.so 暴露，内部透传到 libkuikly.so 的 C API，
    // 避免跨 so 使用 inline/static 变量各自持有副本导致开关失效。
    // 其它页面不关心该参数，保持原有行为。
    if (pageName === 'InputCompareDemoPage') {
      const useTextEditorImpl = (pageData as Record<string, string>)['useTextEditorImpl'];
      try {
        kuiklyEntry.setUseNewTextInputComponent(useTextEditorImpl === '1' ? 1 : 0);
      } catch (e) {
        console.error(`[AppKRRouterAdapter] setUseNewTextInputComponent failed: ${(e as Error).message}`);
      }
    } else if (pageName === 'EmojiTextInputDemo') {
      // emoji 短码 -> 图片替换依赖新 ARKUI_NODE_TEXT_EDITOR 控件（SetStyledText
      // 通路），老 KRTextFieldView 不支持 ImageSpan，必须强制开启新实现。
      try {
        kuiklyEntry.setUseNewTextInputComponent(1);
      } catch (e) {
        console.error(`[AppKRRouterAdapter] setUseNewTextInputComponent(1) for EmojiTextInputDemo failed: ${(e as Error).message}`);
      }
    }

    let url: string;
    if (pageName.startsWith('OverNativeClickDemo')) {
      url = 'pages/GestureThrough';
    } else if (pageName === 'NativeAppWaterfall') {
      url = 'pages/NativeAppWaterfall';
    } else if (pageName === 'KuiklyInList') {
      url = 'pages/KuiklyInList';
    } else {
      url = 'pages/Index';
    }
    router.pushUrl({
      url,
      params: {
        pageName,
        pageData
      }
    });
  }

  closePage(context: common.UIAbilityContext): void {
    router.back();
  }
}