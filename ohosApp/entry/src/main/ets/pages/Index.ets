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

import { KRNativeRenderController, KRRecord, Kuikly } from '@kuikly-open/render';
import globalNativeManager from '../kuikly/MyNativeManager';
import { KuiklyViewDelegate } from '../kuikly/KuiklyViewDelegate';
import pasteboard from '@ohos.pasteboard';
import { BusinessError } from '@ohos.base';
import router from '@ohos.router';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { ContextCodeHandler } from '../kuikly/ContextCodeHandler';
import { AppKRRenderManager } from '../kuikly/adapters/AppKRRenderManager';

@Entry
@Component
struct Index {
  private kuiklyViewDelegate = new KuiklyViewDelegate();
  private kuiklyController: KRNativeRenderController | null = null;
  private pageName: string | null = null;
  private pageData?: KRRecord;
  private contextCode: string = '';
  private contextCodeHandler: ContextCodeHandler = new ContextCodeHandler();
  private useDefaultBackPress = false;
  @State showKuikly: boolean = false;

  onBackPress(): boolean | void {
    if (this.useDefaultBackPress) {
      // Note: set useDefaultBackPress to false when testing back press event dispatching loging.
      return;
    }

    if (this.kuiklyViewDelegate) {
      // send back press event and wait for consumed result synchronously
      return this.kuiklyController?.onBackPress();
    }
  }

  aboutToAppear(): void {
    AppKRRenderManager.getInstance().initIfNeed();
    const params = router.getParams() as Record<string, Object>;
    this.pageName = params?.pageName as string;
    this.pageData = (params?.pageData as KRRecord | null) ?? {};
    if (this.contextCodeHandler.isNeedGetContextCode(params)) {
      this.contextCodeHandler.handleGetContextCode(getContext(), params, (contextCode) => {
        this.contextCode = contextCode;
        this.showKuikly = true;
      }, (stack) => {
        this.showExceptionDialog(stack);
      });
    } else {
      this.showKuikly = true;
    }
  }

  onPageShow(): void {
    const res = getContext(this).resourceDir;
    this.kuiklyViewDelegate.pageDidAppear();
  }

  onPageHide(): void {
    this.kuiklyViewDelegate.pageDidDisappear();
  }

  build() {
    Stack() {
      if (this.showKuikly) {
        Kuikly({
          pageName: this.pageName ?? 'router',
          pageData: this.pageData ?? {},
          delegate: this.kuiklyViewDelegate,
          contextCode: this.contextCode,
          executeMode: this.contextCodeHandler.getExecuteMode(this.contextCode),
          onControllerReadyCallback: (controller) => {
            this.kuiklyController = controller;
            controller.registerExceptionCallback((executeMode, stack) => {
              this.showExceptionDialog(stack);
              const stackInfo: KRRecord = JSON.parse(stack);
              // note: print it line by line to avoid being truncated
              stackInfo['stack'].toString().split('\n').forEach((it) => {
                hilog.error(0x0000, 'demo', '%{public}s', it);
              });
            });
          },
          nativeManager: globalNativeManager,
        });
      }
    }.expandSafeArea([SafeAreaType.KEYBOARD]);

    // .backgroundColor(Color.Green)
  }

  private showExceptionDialog(stack: string) {
    AlertDialog.show(
      {
        title: '未捕获异常',
        message: stack,
        autoCancel: true,
        alignment: DialogAlignment.Bottom,
        offset: { dx: 0, dy: -20 },
        gridCount: 3,
        confirm: {
          value: '复制',
          style: DialogButtonStyle.HIGHLIGHT,
          action: () => {
            let pasteData: pasteboard.PasteData = pasteboard.createData(pasteboard.MIMETYPE_TEXT_PLAIN, stack);
            let systemPasteboard: pasteboard.SystemPasteboard = pasteboard.getSystemPasteboard();
            systemPasteboard.setData(pasteData).then((data: void) => {
              console.info('Succeeded in setting PasteData.');
            }).catch((err: BusinessError) => {
              console.error('Failed to set PasteData. Cause: ' + err.message);
            });
          }
        },
        onWillDismiss: (dismissDialogAction: DismissDialogAction) => {
          if (dismissDialogAction.reason == DismissReason.PRESS_BACK) {
            dismissDialogAction.dismiss();
          }
          if (dismissDialogAction.reason == DismissReason.TOUCH_OUTSIDE) {
            dismissDialogAction.dismiss();
          }
        }
      }
    );
  }
}