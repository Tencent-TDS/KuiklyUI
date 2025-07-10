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

import { KRRecord, KRRenderNativeMode, Kuikly } from '@kuikly-open/render';
import globalNativeManager from '../kuikly/MyNativeManager';
import { KuiklyViewDelegate } from '../kuikly/KuiklyViewDelegate';
import pasteboard from '@ohos.pasteboard';
import { BusinessError } from '@ohos.base';
import router from '@ohos.router';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { AppKRRenderManager } from '../kuikly/adapters/AppKRRenderManager';

@Entry
@Component
struct GestureThrough {
  private kuiklyViewDelegate = new KuiklyViewDelegate();
  private pageName: string | null = null;
  private pageData?: KRRecord;

  aboutToAppear(): void {
    AppKRRenderManager.getInstance().initIfNeed();
    const params = router.getParams() as Record<string, Object>;
    this.pageName = params?.pageName as string;
    this.pageData = (params?.pageData as KRRecord | null) ?? {};
  }

  onPageShow(): void {
    this.kuiklyViewDelegate.pageDidAppear();
  }

  onPageHide(): void {
    this.kuiklyViewDelegate.pageDidDisappear();
  }

  build() {
    Stack() {
      Button('按钮遮挡')
        .onClick(() => {
          this.getUIContext().getPromptAction().showToast({
            message: 'Button onClick'
          })
        })
      // Row() {
      //   Stack().width(50).height(50).backgroundColor(Color.Green).hitTestBehavior(HitTestMode.Transparent)
      //   Stack().width(50).height(50).backgroundColor(Color.Red)
      // }.hitTestBehavior(HitTestMode.None).opacity(0.5)
      Kuikly({
        pageName: this.pageName ?? 'router',
        pageData: this.pageData,
        delegate: this.kuiklyViewDelegate,
        contextCode: '',
        executeMode: KRRenderNativeMode.Native,
        onControllerReadyCallback: (controller) => {
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
      }).hitTestBehavior(HitTestMode.None);
    }.expandSafeArea([SafeAreaType.KEYBOARD]).width('100%').height('100%');
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