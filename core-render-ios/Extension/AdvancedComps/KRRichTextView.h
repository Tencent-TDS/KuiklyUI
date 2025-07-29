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

#import "KRLabel.h"
#import "KuiklyRenderViewExportProtocol.h"

NS_ASSUME_NONNULL_BEGIN
extern NSString *const KuiklyIndexAttributeName;
@interface KRRichTextView : KRLabel<KuiklyRenderViewExportProtocol>

@end

@interface KRRichTextShadow : NSObject<KuiklyRenderShadowProtocol>

@property (nonatomic, strong) NSMutableAttributedString *attributedString;
@property (nonatomic, assign) bool strokeAndFill;

- (NSAttributedString *)buildAttributedString;


@end

@interface KRRichTextAttachment : NSTextAttachment

@property (nonatomic , assign) CGFloat offsetY ;
@property (nonatomic , assign) NSUInteger charIndex ;

@end

NS_ASSUME_NONNULL_END
