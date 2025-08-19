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

#import "KuiklyContextParam.h"
#import "KuiklyRenderFrameworkContextHandler.h"

const KuiklyContextMode KuiklyContextMode_Framework = 1;

@implementation KuiklyBaseContextMode

- (instancetype)initFrameworkMode {
    self = [super init];
    if (self) {
        _modeId = KuiklyContextMode_Framework;
    }
    return self;
}

- (id<KuiklyRenderContextProtocol>)createContextHandlerWithContextCode:(NSString *)contextCode
                                                          contextParam:(KuiklyContextParam *)contextParam {
    return [[KuiklyRenderFrameworkContextHandler alloc] initWithContext:contextCode contextParam:contextParam];
}



@end

@interface KuiklyContextParam ()

// pageName 页面名 （对应的值为kotlin侧页面注解 @Page("xxxx")中的xxx名）
@property (nonatomic, copy, readwrite) NSString *pageName;

/// 资源文件目录URL, 用于资源文件放置于非MainBundle根目录下时指定自定义路径
@property (nonatomic, strong, nullable) NSURL *resourceFolderUrl;

@end

@implementation KuiklyContextParam

+ (instancetype)newWithPageName:(NSString *)pageName
              resourceFolderUrl:(nullable NSURL *)resourceFolderUrl {
    KuiklyContextParam *param = [KuiklyContextParam new];
    param.pageName = pageName;
    param.resourceFolderUrl = resourceFolderUrl;
    return param;
}

- (NSURL *)urlForFileName:(NSString *)fileName extension:(NSString *)fileExtension {
    if (self.resourceFolderUrl) {
        NSURL *rscUrl = [[self.resourceFolderUrl URLByAppendingPathComponent:fileName] URLByAppendingPathExtension:fileExtension];
        return rscUrl;
    }
    
    // use main bundle as default
    NSURL *fileURL = [[NSBundle mainBundle] URLForResource:fileName withExtension:fileExtension];
    return fileURL;
}

@end
