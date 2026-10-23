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

package com.tencent.kuikly.core.views.ios

/** 大标题模式，对应 `UINavigationItem.largeTitleDisplayMode`。 */
enum class UILargeTitleDisplayMode(val value: String) {
    Never("never"),
    Automatic("automatic"),
    Always("always")
}

/** iOS 27+ Duo 上按钮轴向。 */
enum class UIBarAxis(val value: String) {
    Automatic("automatic"),
    HorizontalOnly("horizontalOnly"),
    VerticalPreferred("verticalPreferred")
}

/** 对应 `UIBarMinimizationBehavior`（iOS 27+）。 */
enum class UINavigationBarMinimizationBehavior(val value: String) {
    Automatic("automatic"),
    Never("never"),
    OnScrollDown("onScrollDown"),
    OnScrollUp("onScrollUp")
}

/** 对应 `UIBarMinimizationSafeAreaAdjustment`（iOS 27+）。 */
enum class UINavigationBarMinimizationSafeAreaAdjustment(val value: String) {
    Automatic("automatic"),
    Enabled("enabled"),
    Disabled("disabled")
}

/** 对应 `UIBarMinimizationRestorationBehavior`（iOS 27+）。 */
enum class UINavigationBarMinimizationRestoration(val value: String) {
    Automatic("automatic"),
    AtScrollEdge("atScrollEdge")
}

/** 对应 `UINavigationBarAppearance` configure 预设（iOS 13+）。 */
enum class UINavigationBarBackgroundStyle(val value: String) {
    Default("default"),
    Opaque("opaque"),
    Transparent("transparent")
}

/** 对应 `UIBarStyle`。 */
enum class UINavigationBarStyle(val value: String) {
    Default("default"),
    Black("black")
}

/** 对应 `UINavigationBar.isTranslucent`；[Auto] 在 Opaque 时为 NO。 */
enum class UINavigationBarTranslucent(val value: String) {
    Auto("auto"),
    Yes("yes"),
    No("no")
}

/** 导航栏按钮左右：`leading` / `trailing`。 */
enum class UIBarButtonItemPlacement(val value: String) {
    Leading("leading"),
    Trailing("trailing")
}

/**
 * 导航栏标题字重，下发 `titleFontWeight`。
 * 与 iOS `KRNormalizedFontWeight` 别名一致；也可用 [com.tencent.kuikly.core.views.FontWeight] 重载。
 */
enum class UINavigationBarTitleFontWeight(val value: String) {
    Ultralight("ultralight"),
    Thin("thin"),
    Light("light"),
    Regular("regular"),
    Normal("normal"),
    Medium("medium"),
    Semibold("semibold"),
    Bold("bold"),
    Heavy("heavy"),
    Black("black")
}

/**
 * SF Symbol / 位图是否按模板图渲染（随 [UIBarButtonItemAttr.tintColor] 着色）。
 * 对应 attr `template`（`true` / `false`）。
 */
enum class UIBarButtonItemSymbolRendering(val value: String) {
    Template("true"),
    Original("false")
}
