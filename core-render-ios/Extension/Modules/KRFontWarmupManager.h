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

NS_ASSUME_NONNULL_BEGIN

/**
 * CoreText 全局字体缓存（级联表 / CharacterSet）首次构建不是线程安全的。
 * 本类把业务会用到的自定义字体，在主线程提前做一次 TextStorage 排版触发首次构建，
 * 避免 Context Queue 测量端与主线程渲染端并发触发首次构建导致野指针崩溃。
 */
@interface KRFontWarmupManager : NSObject

/// 预热 info.plist（UIAppFonts）中声明的自定义字体（需在主线程调用）。
/// 会按常用字号构建字体；内部去重，已预热过的字号不会重复构建。
+ (void)warmupInfoPlistFonts;

/// 预热 info.plist 字体的幂等入口：整个进程内仅执行一次。
/// 可在任意线程调用（内部保证在主线程执行）。
/// 框架会在渲染 Core 初始化时（首帧渲染前）自动调用，确保字体缓存已在主线程单线程构建完成。
/// 业务也可在启动期（如 didFinishLaunching 之后）提前调用一次，以消除首帧前的同步预热开销；
/// 若运行期动态修改了 UIAppFonts，可再次调用 +warmupInfoPlistFonts 重新预热。
+ (void)warmupInfoPlistFontsOnce;

+ (instancetype)sharedManager;

/// 主线程同步预热字体。内部会去重，已预热过的字体不会重复构建。
- (void)warmupFonts:(NSArray<UIFont *> *)fonts;

/// 异步派发主线程预热。允许在子线程发现新字体后调用，不会阻塞当前线程。
- (void)warmupFontsAsync:(NSArray<UIFont *> *)fonts;

/// 字体热更前调用：解除强引用、失效预热标记，配合系统 CTFontManager 注销/重新注册。
- (void)invalidateFont:(UIFont *)font;

/// 清空所有预热缓存（字体全量热更 / 内存告警时使用）。
- (void)invalidateAllFonts;

@end

NS_ASSUME_NONNULL_END
