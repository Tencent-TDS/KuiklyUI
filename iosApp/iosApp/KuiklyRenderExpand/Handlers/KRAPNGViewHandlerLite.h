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

#import <UIKit/UIKit.h>
#import "KRAPNGView.h"

NS_ASSUME_NONNULL_BEGIN

/**
 * Zero-dependency APNG host adapter (reference implementation).
 *
 * Unlike KRAPNGViewHandler (built on SDWebImage's SDAnimatedImageView), this
 * implementation pulls in no third-party dependency: it parses the APNG chunk
 * structure (IHDR/acTL/fcTL/IDAT/fdAT) by hand, rebuilds each frame as a
 * standard PNG for UIImage to decode, and schedules playback by per-frame delays.
 *
 * Scope: only "full-frame" APNG files are supported (every frame matches the
 * canvas size, offset = 0, blend/dispose = 0). Most export tools produce
 * full-frame output by default, which covers typical icon/loading animations.
 * On parse failure or missing animation frames, falls back to displaying a
 * static image instead of crashing.
 *
 * Note: this class intentionally does NOT self-register in +load. The demo's
 * KRAPNGViewHandler already registers in +load, and with two +load registrations
 * the winner is undefined. To use this implementation, call +registerToKuikly
 * explicitly at app startup to override the registered creator.
 */
@interface KRAPNGViewHandlerLite : UIView <APNGImageViewProtocol>

/// Registers this implementation with Kuikly (overrides any previously
/// registered creator). Call once at app startup.
+ (void)registerToKuikly;

@end

NS_ASSUME_NONNULL_END
