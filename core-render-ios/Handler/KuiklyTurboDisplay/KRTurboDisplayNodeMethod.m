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

#import "KRTurboDisplayNodeMethod.h"

@implementation KRTurboDisplayNodeMethod

#define TYPE @"type"
#define NAME @"name"
#define METHOD @"method"
#define PARAMS @"params"

#pragma mark - NSCoding

- (instancetype)initWithCoder:(NSCoder *)coder {
    self = [super init];
    if (self) {
        _type = [coder decodeIntegerForKey:TYPE];
        _name = [coder decodeObjectForKey:NAME];
        _method = [coder decodeObjectForKey:METHOD];
        _params = [coder decodeObjectForKey:PARAMS];
    }
    return self;
}

- (void)encodeWithCoder:(NSCoder *)coder {
    [coder encodeInteger:_type forKey:TYPE];
    [coder encodeObject:_name forKey:NAME];
    [coder encodeObject:_method forKey:METHOD];
    if ([_params conformsToProtocol:@protocol(NSCoding)]) {
        [coder encodeObject:_params forKey:PARAMS];
    }
}


- (KRTurboDisplayNodeMethod *)deepCopy {
    KRTurboDisplayNodeMethod *newMethod = [KRTurboDisplayNodeMethod new];
    newMethod.type = self.type;
    newMethod.name = self.name;
    newMethod.method = self.method;
    newMethod.params = self.params;
    return newMethod;
}

@end
