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

#import "KRView.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * Demo 自定义 View，对齐鸿蒙侧 `KRMyDemoCustomView.ets`：
 * - `message` 属性：居中显示文字（黄底黑边）
 * - `onMyViewTapped` 事件：Tap Me 按钮点击回调
 * - kuikly 侧嵌入子节点：直接走 UIView 层级 addSubview，作为叠加层显示在装饰内容之上
 *
 * 无障碍属性 `accessibility` / `accessibilityRole` / `accessibilityInfo` 走
 * `UIView+CSS` 通用属性通道，本类无需重写。
 * `accessibilityAnnounce` / `accessibilityFocus` method 通过继承 `KRView` 复用
 * 其 `hrv_callWithMethod:` 实现。
 */
@interface KRMyDemoCustomView : KRView

@end

NS_ASSUME_NONNULL_END
