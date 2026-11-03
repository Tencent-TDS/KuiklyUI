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

import 'libkuikly_entry.so';
import { AbilityStage } from '@kit.AbilityKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { MMKV } from '@tencent/mmkv';
import { setup, init } from '@kuiklybase/knoi';
import BuildProfile from 'BuildProfile';
import { CrashReport } from '../kuikly/CrashReport/CrashReport';
import { LeakWatcherManager } from '../kuikly/LeakWatcherManager';

export default class EntryAbilityStage extends AbilityStage {
  onCreate(): void {
    hilog.info(0x0000, 'testTag', '%{public}s', 'AbilityStage onCreate');
    // 初始化 KNOI：参数1为 KMP 生成的动态库名称，参数2为是否 Debug 模式
    setup("libshared.so", BuildProfile.DEBUG);
    init();

    CrashReport.getInstance().initCrashReport(this.context);

    // MMKV 初始化（必须先于任何 defaultMMKV() 调用）。
    // 用 applicationContext，避免持有 AbilityStage 的 context。
    MMKV.initialize(this.context.getApplicationContext());
    LeakWatcherManager.init(this.context.getApplicationContext());
  }
}
