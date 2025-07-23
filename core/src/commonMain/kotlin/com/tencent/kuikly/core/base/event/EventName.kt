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

package com.tencent.kuikly.core.base.event

enum class EventName(val value:String) {
    // 单击事件
    CLICK("click"),
    // 双击事件
    DOUBLE_CLICK("doubleClick"),
    // 长按事件
    LONG_PRESS("longPress"),
    // 滑动事件
    PAN("pan"),
    // 捏合事件
    PINCH("pinch"),
    // 手势按下事件
    TOUCH_DOWN("touchDown"),
    // 手势抬起事件
    TOUCH_UP("touchUp"),
    // 手势移动事件
    TOUCH_MOVE("touchMove"),
    // 手势取消事件
    TOUCH_CANCEL("touchCancel"),
    // 动画结束事件
    ANIMATION_COMPLETE("animationCompletion"),
    // 屏幕帧VSYNC信号事件
    SCREEN_FRAME("screenFrame"),
}