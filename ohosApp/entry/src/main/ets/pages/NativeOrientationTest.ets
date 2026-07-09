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

/**
 * 纯 Native 转屏对比页（启动直达）。
 * 与 ComposeVideoOrientationDemo 一致：同一页面内黑色蒙层 + 顶部色块放大，不跳转新页面。
 */

import { common } from '@kit.AbilityKit';
import { curves, window } from '@kit.ArkUI';

const HEADER_HEIGHT = 220;
const VIDEO_EXPAND_ANIM_MS = 360;
const PORTRAIT_HOLD_MS = 100;

interface BoxSize {
  width: number;
  height: number;
}

@Entry
@Component
struct NativeOrientationTest {
  @State windowWidth: number = 360;
  @State windowHeight: number = 780;
  @State showOverlay: boolean = false;
  @State overlayFrozen: boolean = false;
  @State frozenWindowWidth: number = 0;
  @State frozenWindowHeight: number = 0;
  @State boxWidth: number = 360;
  @State boxHeight: number = HEADER_HEIGHT;
  @State statusText: string = '竖屏 · 同页转屏';
  private portraitHoldTimer: number = -1;

  aboutToDisappear(): void {
    if (this.portraitHoldTimer >= 0) {
      clearTimeout(this.portraitHoldTimer);
      this.portraitHoldTimer = -1;
    }
  }

  private overlayWindowWidth(): number {
    return this.overlayFrozen ? this.frozenWindowWidth : this.windowWidth;
  }

  private overlayWindowHeight(): number {
    return this.overlayFrozen ? this.frozenWindowHeight : this.windowHeight;
  }

  private targetBoxSize(windowW: number, windowH: number): BoxSize {
    if (windowW >= windowH) {
      const size: BoxSize = { width: windowW, height: windowW * 9 / 16 };
      return size;
    }
    const size: BoxSize = { width: windowH * 16 / 9, height: windowH };
    return size;
  }

  private requestOrientation(orientation: 'landscape' | 'portrait'): void {
    const target = orientation === 'landscape'
      ? window.Orientation.LANDSCAPE
      : window.Orientation.PORTRAIT;
    try {
      const context = getContext() as common.UIAbilityContext;
      context.windowStage.getMainWindowSync().setPreferredOrientation(target);
    } catch (e) {
      console.error(`requestOrientation error: ${JSON.stringify(e)}`);
    }
  }

  private animateBoxToTarget(windowW: number, windowH: number): void {
    const target = this.targetBoxSize(windowW, windowH);
    animateTo({
      duration: VIDEO_EXPAND_ANIM_MS,
      curve: curves.cubicBezierCurve(0.42, 0, 0.58, 1),
    }, () => {
      this.boxWidth = target.width;
      this.boxHeight = target.height;
    });
  }

  private enterLandscape(): void {
    this.overlayFrozen = false;
    this.boxWidth = this.windowWidth;
    this.boxHeight = HEADER_HEIGHT;
    // 与 Compose 一致：先转屏，再显示同页蒙层
    this.requestOrientation('landscape');
    this.showOverlay = true;
    this.statusText = '横屏蒙层（同页）';
    this.animateBoxToTarget(this.windowWidth, this.windowHeight);
  }

  private exitLandscape(): void {
    this.frozenWindowWidth = this.windowWidth;
    this.frozenWindowHeight = this.windowHeight;
    const frozenTarget = this.targetBoxSize(this.frozenWindowWidth, this.frozenWindowHeight);
    this.boxWidth = frozenTarget.width;
    this.boxHeight = frozenTarget.height;
    this.overlayFrozen = true;
    this.requestOrientation('portrait');
    this.statusText = '返回竖屏中';
    if (this.portraitHoldTimer >= 0) {
      clearTimeout(this.portraitHoldTimer);
    }
    this.portraitHoldTimer = setTimeout((): void => {
      this.showOverlay = false;
      this.overlayFrozen = false;
      this.statusText = '竖屏 · 同页转屏';
      this.portraitHoldTimer = -1;
    }, PORTRAIT_HOLD_MS);
  }

  private listColor(index: number): string {
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7'];
    return colors[index % colors.length];
  }

  @Builder
  ColorBlock() {
    Column() {
      Text('Native 顶部色块')
        .fontSize(18)
        .fontColor(Color.White)
        .fontWeight(FontWeight.Bold)
      Text(`${this.windowWidth} x ${this.windowHeight}`)
        .fontSize(13)
        .fontColor('#EEEEEE')
        .margin({ top: 8 })
      if (this.showOverlay) {
        Text('（同页蒙层，非新页面）')
          .fontSize(11)
          .fontColor('#B3E5FC')
          .margin({ top: 6 })
      }
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
    .backgroundColor('#2196F3')
  }

  build() {
    Stack() {
      // 底层：始终是同一个页面（列表 + 头部占位）
      Column() {
        Text(this.statusText)
          .fontSize(12)
          .fontColor('#666666')
          .width('100%')
          .textAlign(TextAlign.Center)
          .padding({ top: 8, bottom: 4 })

        Scroll() {
          Column() {
            if (!this.showOverlay) {
              Stack({ alignContent: Alignment.BottomEnd }) {
                this.ColorBlock()
                Button('横屏')
                  .fontSize(14)
                  .fontColor(Color.White)
                  .backgroundColor('#CC000000')
                  .margin({ right: 12, bottom: 12 })
                  .onClick((): void => {
                    this.enterLandscape();
                  })
              }
              .width('100%')
              .height(HEADER_HEIGHT)
            } else {
              // 色块已移到蒙层，头部留空位（对齐 Compose videoInOverlay）
              Column()
                .width('100%')
                .height(HEADER_HEIGHT)
                .backgroundColor(Color.Black)
            }

            ForEach([
              1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
              11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
              21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
            ], (index: number) => {
              Row() {
                Column()
                  .width(42)
                  .height(42)
                  .borderRadius(21)
                  .backgroundColor(this.listColor(index))
                Column() {
                  Text(`列表项 ${index}`)
                    .fontSize(15)
                    .fontWeight(FontWeight.Bold)
                    .fontColor('#222222')
                  Text('纯 Native 列表，与 Compose Demo 同页转屏')
                    .fontSize(13)
                    .fontColor('#666666')
                    .margin({ top: 4 })
                }
                .layoutWeight(1)
                .margin({ left: 12 })
              }
              .width('100%')
              .padding({ left: 16, right: 16, top: 14, bottom: 14 })
              .backgroundColor(Color.White)
            }, (index: number) => `${index}`)
          }
          .width('100%')
        }
        .layoutWeight(1)
      }
      .width('100%')
      .height('100%')
      .backgroundColor('#F5F6FA')
      .onAreaChange((_old, area) => {
        this.windowWidth = area.width as number;
        this.windowHeight = area.height as number;
        if (this.showOverlay && !this.overlayFrozen) {
          this.animateBoxToTarget(this.windowWidth, this.windowHeight);
        }
      })

      // 同页蒙层：黑色盖住底层，色块在此放大（非 router 跳转）
      if (this.showOverlay) {
        Stack({ alignContent: Alignment.TopStart }) {
          Column() {
            Column() {
              this.ColorBlock()
            }
            .width(this.boxWidth)
            .height(this.boxHeight)
          }
          .width(this.overlayWindowWidth())
          .height(this.overlayWindowHeight())
          .backgroundColor(Color.Black)
          .justifyContent(FlexAlign.Center)

          Button('退出横屏')
            .fontSize(15)
            .fontColor(Color.White)
            .backgroundColor('#CC000000')
            .padding({ left: 14, right: 14, top: 9, bottom: 9 })
            .margin({ left: 16, top: 32 })
            .onClick((): void => {
              this.exitLandscape();
            })
        }
        .width('100%')
        .height('100%')
        .backgroundColor(Color.Black)
      }
    }
    .width('100%')
    .height('100%')
  }
}
