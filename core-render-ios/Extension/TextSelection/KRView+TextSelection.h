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
#import "KRTextSelectionHelper.h"

NS_ASSUME_NONNULL_BEGIN

/// Text selection method names
extern NSString *const KRTextSelectionMethodCreateSelection;
extern NSString *const KRTextSelectionMethodGetSelection;
extern NSString *const KRTextSelectionMethodClearSelection;

/// KRView category for text selection support
@interface KRView (TextSelection)

/**
 * Handle text selection method call from DSL.
 * @param method The method name.
 * @param params The JSON string parameters.
 * @param callback The callback for returning results.
 * @return YES if the method was handled, NO otherwise.
 */
- (BOOL)kr_handleTextSelectionMethod:(NSString *)method
                              params:(NSString * _Nullable)params
                            callback:(KuiklyRenderCallback _Nullable)callback;

/**
 * Clean up text selection resources.
 * Should be called in dealloc.
 */
- (void)kr_cleanupTextSelection;

@end

NS_ASSUME_NONNULL_END

