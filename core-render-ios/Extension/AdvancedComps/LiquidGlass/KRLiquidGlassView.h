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

#import "KRUIKit.h" // [macOS]
#import "KuiklyRenderViewExportProtocol.h"

#if TARGET_OS_IOS // [macOS]

NS_ASSUME_NONNULL_BEGIN

/**
 * 液态玻璃视图组件
 * 提供iOS 26.0+的UIGlassEffect支持
 */
@interface KRLiquidGlassView : UIVisualEffectView <KuiklyRenderViewExportProtocol>

@end

NS_ASSUME_NONNULL_END

#endif

// [macOS
#if TARGET_OS_OSX
#if __MAC_OS_X_VERSION_MAX_ALLOWED >= 260000

NS_ASSUME_NONNULL_BEGIN

/**
 * 液态玻璃视图组件（macOS版本）
 * 提供macOS 26.0+的NSGlassEffectView支持
 */
API_AVAILABLE(macos(26.0))
@interface KRLiquidGlassView : NSGlassEffectView <KuiklyRenderViewExportProtocol>

@end

NS_ASSUME_NONNULL_END

#endif
#endif
// macOS]
