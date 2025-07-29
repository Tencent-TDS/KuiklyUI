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

#import "KRMemoryCacheModule.h"
#import "NSObject+KR.h"
#import "KRImageView.h"

extern NSString *const KRImageBase64Prefix;
extern NSString *const KRImageAssetsPrefix;
extern NSString *const KRImageLocalPathPrefix;

static NSString *const kCacheStateComplete = @"Complete";
static NSString *const kCacheStateInProgress = @"InProgress";


@interface KRMemoryCacheModule(){
    NSMutableDictionary* _imageCache;
    NSLock* _imageCacheLock;
}

@property (nonatomic, strong) NSMutableDictionary <NSString *, id> *memoryKeyValueMap;

@end

@implementation KRMemoryCacheModule

- (instancetype)init{
    if(self = [super init]){
        _imageCacheLock = [[NSLock alloc] init];
        return self;
    }
    return nil;
}

- (void)setObject:(NSDictionary *)args {
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString *key = param[@"key"];
    id value = param[@"value"];
    [self setMemoryObjectWithKey:key value:value];
    
}

#pragma mark - public

- (id)memoryObjectForKey:(NSString *)key {
    if (key) {
        NSAssert([NSThread isMainThread], @"should be run on main thread");
        return self.memoryKeyValueMap[key];
    }
    return nil;
}

- (void)setMemoryObjectWithKey:(NSString *)key value:(id)value {
    NSAssert([NSThread isMainThread], @"should be run on main thread");
    NSAssert(key && value, @"setMemoryObjectWithKeyValue can't be nil");
    if (key && value) {
       self.memoryKeyValueMap[key] = value;
    }
    
}

#pragma mark - getter

- (NSMutableDictionary<NSString *, id> *)memoryKeyValueMap {
    if (!_memoryKeyValueMap) {
        _memoryKeyValueMap = [[NSMutableDictionary alloc] init];
    }
    return _memoryKeyValueMap;
}

- (NSString*)cacheImage:(NSDictionary*)args{
    NSDictionary *param = [args[KR_PARAM_KEY] hr_stringToDictionary];
    NSString* src = param[@"src"];
    NSNumber* sync = param[@"sync"];
    KuiklyRenderCallback callback = args[KR_CALLBACK_KEY];
    
    return [self cacheImage:src sync:sync.intValue callback:callback];
}

- (NSString*)cacheImage:(UIImage*)image withCachekey:(NSString*)cacheKey callback:(KuiklyRenderCallback)callback{
    NSString* state = @"Complete";
    NSNumber* errorCode = @(0);
    NSString* errorMsg = @"";
    
    [_imageCacheLock lock];
    _imageCache[cacheKey] = image;
    [_imageCacheLock unlock];
    
    errorMsg = @"";
    NSDictionary *result = @{
        @"state": kCacheStateComplete,
        @"errorCode": @(0),
        @"errorMsg": @"",
        @"cacheKey": cacheKey
    };
    return [result hr_dictionaryToString];
}

- (NSString*)cacheBase64Image:(NSString*)src withCacheKey:cacheKey sync:(bool)sync callback:(KuiklyRenderCallback)callback{
    @autoreleasepool {
        NSRange range = [src rangeOfString:@";base64,"];
        NSString * base64 = [src substringFromIndex:NSMaxRange(range)];
        NSData * imageData = [[NSData alloc] initWithBase64EncodedString:base64 options:NSDataBase64DecodingIgnoreUnknownCharacters];
        UIImage *image = [UIImage imageWithData:imageData];
        
        return [self cacheImage:image withCachekey:cacheKey callback:callback];
    }
}

- (NSString*)cacheLocalImageWithURL:(NSURL*)url withCacheKey:cacheKey sync:(bool)sync callback:(KuiklyRenderCallback)callback{
    @autoreleasepool {
        NSData* data = [NSData dataWithContentsOfURL:url];
        UIImage *image = [UIImage imageWithData:data];
        
        return [self cacheImage:image withCachekey:cacheKey callback:callback];
    }
}


- (NSString*)cacheImage:(NSString*)src sync:(bool)sync callback:(KuiklyRenderCallback)callback{
    [_imageCacheLock lock];
    if(_imageCache == nil){
        _imageCache = [[NSMutableDictionary alloc] init];
    }
    [_imageCacheLock unlock];
    
    NSString *cacheKey = [src kr_md5String];
    NSDictionary* result;
    if(sync){
        // base64 and local files are supported in sync mode
        if([src hasPrefix:KRImageBase64Prefix]){
            return [self cacheBase64Image:src withCacheKey:cacheKey sync:sync callback:callback];
        }
        
        NSURL *url = nil;
        if([src hasPrefix:KRImageAssetsPrefix]){
            NSString *fileExtension = [src pathExtension];
            NSRange subRange = NSMakeRange(KRImageAssetsPrefix.length, src.length - KRImageAssetsPrefix.length - fileExtension.length - 1);
            NSString *pathWithoutExtension = [src substringWithRange:subRange];
            KuiklyContextParam *contextParam = ((KuiklyRenderView *)self.hr_rootView).contextParam;
            url = [contextParam.contextMode urlForFileName:pathWithoutExtension extension:fileExtension];
        }
        if([src hasPrefix:KRImageLocalPathPrefix]){
            url = [NSURL URLWithString:src];
        }
        
        if(url){
            return [self cacheLocalImageWithURL:url withCacheKey:cacheKey sync:sync callback:callback];
        }
    }
    
    // fallback to async if sync mode is not applicable
    NSString* state = kCacheStateInProgress;
    NSNumber* errorCode = @(1);
    NSString* errorMsg = kCacheStateInProgress;

    KR_WEAK_SELF
    dispatch_async(dispatch_get_main_queue(), ^{
        KRImageView* imageView = [[KRImageView alloc] init];
        KR_STRONG_SELF_RETURN_IF_NIL
        
        [strongSelf->_imageCacheLock lock];
        strongSelf->_imageCache[cacheKey] = imageView;
        [strongSelf->_imageCacheLock unlock];
        
        KuiklyRenderCallback imageLoadSuccessCB = nil;
        if(callback){
            imageLoadSuccessCB = ^(id _Nullable result){
                UIImage* img = imageView.image;
                // update cache
                [strongSelf->_imageCacheLock lock];
                strongSelf->_imageCache[cacheKey] = img;
                [strongSelf->_imageCacheLock unlock];
                
                result = @{
                    @"state": kCacheStateComplete,
                    @"errorCode": @(0),
                    @"errorMsg": @"",
                    @"cacheKey": cacheKey
                };
                callback(result);
            };
        }
        
        [imageView hrv_setPropWithKey:@"loadSuccess" propValue:imageLoadSuccessCB];
        [imageView hrv_setPropWithKey:@"src" propValue:src];
    });
    
    result = @{
        @"state": state,
        @"errorCode": errorCode,
        @"errorMsg": errorMsg,
        @"cacheKey": cacheKey
    };
    return [result hr_dictionaryToString];
}

- (UIImage*)imageWithKey:(NSString*)key{
    [_imageCacheLock lock];
    NSObject* cacheObj = [_imageCache objectForKey:key];
    [_imageCacheLock unlock];
    return [cacheObj isKindOfClass:[UIImage class]] ? (UIImage*)cacheObj : nil;
}

- (void)dealloc {
    NSDictionary* cache = _imageCache;
    _imageCache = nil;
    dispatch_async(dispatch_get_main_queue(), ^{
        // ImageView is used for loading the images,
        // post it back to main thread for deallocation
        (void)cache;
    });
}

@end
