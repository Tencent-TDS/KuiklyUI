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

#import <Foundation/Foundation.h>
#import "KuiklyRenderContextProtocol.h"

NS_ASSUME_NONNULL_BEGIN

extern const KuiklyContextMode KuiklyContextMode_Framework;

@class KuiklyContextParam;

// Kuikly接入模式
@interface KuiklyBaseContextMode : NSObject

@property (nonatomic, assign) KuiklyContextMode modeId;

// 创建Framework接入模式实例，modeId为KuiklyContextMode_Framework
- (instancetype)initFrameworkMode;

// 创建Kuikly执行环境的实现者
- (id<KuiklyRenderContextProtocol>)createContextHandlerWithContextCode:(NSString *)contextCode
                                                          contextParam:(KuiklyContextParam *)contextParam;

@end

@interface KuiklyContextParam : NSObject

/// pageName 页面名 （对应的值为kotlin侧页面注解 @Page("xxxx")中的xxx名）
@property (nonatomic, copy, readonly) NSString *pageName;

/// contextMode context产物模式
@property (nonatomic, strong) KuiklyBaseContextMode *contextMode;

/// 资源文件目录URL, 用于资源文件放置于非MainBundle根目录下时指定自定义路径
@property (nonatomic, strong, readonly, nullable) NSURL *resourceFolderUrl;

/// Initialize context-related parameters
/// - Parameters:
///   - pageName: Page name (corresponds to the value in the Kotlin-side page annotation @Page("xxxx"), case-sensitive)
///   - resourceFolderUrl: URL of the folder containing resource files. If empty, the mainBundle URL will be used by default.
///
///   When using SPM for integration, resource files are typically located in an independent bundle.
///   The resource directory can be passed in the following format:
///   [[[NSBundle mainBundle] bundleURL] URLByAppendingPathComponent:@"shared_SharedResource.bundle/KuiklyResources"];
///   Replace shared_SharedResource and KuiklyResources with the actual bundle name and subdirectory name, respectively.
+ (instancetype)newWithPageName:(NSString *)pageName
              resourceFolderUrl:(nullable NSURL *)resourceFolderUrl;

/// Obtain the URL of the resource file, which is used to load resources such as images
/// - Parameters:
///   - fileName: File name
///   - fileExtension: File extension
- (NSURL *)urlForFileName:(NSString *)fileName extension:(NSString *)fileExtension;

@end

NS_ASSUME_NONNULL_END
