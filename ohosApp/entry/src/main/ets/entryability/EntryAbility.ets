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

import { AbilityConstant, UIAbility, Want } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { window } from '@kit.ArkUI';
import { KuiklyRenderAdapterManager } from '@kuikly-open/render';
import { AppKRRouterAdapter } from '../kuikly/adapters/AppKRRouterAdapter';
import fs from '@ohos.file.fs';
import { BusinessError } from '@kit.BasicServicesKit';
import Napi from 'libkuikly_entry.so';

export default class EntryAbility extends UIAbility {
  onCreate(want: Want, launchParam: AbilityConstant.LaunchParam): void {
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onCreate');
  }

  onDestroy(): void {
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onDestroy');
  }

  onWindowStageCreate(windowStage: window.WindowStage): void {
    // Main window is created, set main page for this ability
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onWindowStageCreate');
    KuiklyRenderAdapterManager.krRouterAdapter = new AppKRRouterAdapter();

    // KRNativeManager.getInstance().onWindowStageCreate(windowStage);
    windowStage.loadContent('pages/Index', (err) => {
      if (err.code) {
        hilog.error(0x0000, 'testTag', 'Failed to load the content. Cause: %{public}s', JSON.stringify(err) ?? '');
        return;
      }
      hilog.info(0x0000, 'testTag', 'Succeeded in loading the content.');

      // copy font data to tmp folder
      const content = getContext().resourceManager.getRawFileContentSync('Satisfy-Regular.ttf')
      const destPath = `${getContext().tempDir}/Satisfy-Regular.ttf`;

      fs.open(destPath, fs.OpenMode.CREATE | fs.OpenMode.READ_WRITE, (err: BusinessError, data) => {
        if (err) {
          console.error(`copy file failed with error message: ${err.message}, error code: ${err.code}`);
        } else {
          fs.write(data.fd, content.buffer, {
            offset: 0, length: content.length
          }).then((result) => {
            console.info(`copy file succeed:${result}`);
            Napi.setFontPath(destPath)
          })
        }
      })
    });
    let mainWindow = windowStage.getMainWindowSync();
    mainWindow.setWindowLayoutFullScreen(true);
  }

  onWindowStageDestroy(): void {
    // Main window is destroyed, release UI related resources
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onWindowStageDestroy');
  }

  onForeground(): void {
    // Ability has brought to foreground
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onForeground');
  }

  onBackground(): void {
    // Ability has back to background
    hilog.info(0x0000, 'testTag', '%{public}s', 'Ability onBackground');
  }
}
