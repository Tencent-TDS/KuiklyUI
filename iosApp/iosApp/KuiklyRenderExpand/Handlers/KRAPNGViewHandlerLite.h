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
 * 零第三方依赖的 APNG 宿主适配器（参考实现）。
 *
 * 与 KRAPNGViewHandler（基于 SDWebImage 的 SDAnimatedImageView）不同，
 * 本实现不引入任何第三方库：手工解析 APNG 块结构（IHDR/acTL/fcTL/IDAT/fdAT），
 * 逐帧重建标准 PNG 交给 UIImage 解码，按帧延时调度播放。
 *
 * 适用边界：仅支持「全帧」APNG（每帧尺寸=画布尺寸、offset=0、blend/dispose=0），
 * 这类文件覆盖了大多数图标/加载动效场景（常见导出工具默认产出全帧）。
 * 解析失败或不含动画帧时降级为静态图显示，不崩溃。
 *
 * 注意：本类不做 +load 自动注册（demo 中的 KRAPNGViewHandler 已在 +load 注册，
 * 两个 +load 同时存在时生效顺序不确定）。业务如需使用本实现，请在启动时显式调用
 * +registerToKuikly 覆盖注册。
 */
@interface KRAPNGViewHandlerLite : UIView <APNGImageViewProtocol>

/// 注册到 Kuikly（覆盖此前注册的 creator），业务启动时调用
+ (void)registerToKuikly;

@end

NS_ASSUME_NONNULL_END
