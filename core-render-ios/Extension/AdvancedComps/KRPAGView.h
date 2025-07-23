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
#import "KuiklyRenderViewExportProtocol.h"
#import "KRHttpRequestTool.h"
NS_ASSUME_NONNULL_BEGIN

// should pod 'libpag', ">= 3.3.0.245-noffmpeg"
@protocol KRPagViewProtocol;
typedef void (^KRPagFileResponse)(NSString * _Nullable path , NSError * _Nullable error);
typedef id<KRPagViewProtocol> _Nonnull (^PAGViewCreator)(CGRect frame);

@interface KRPAGView : UIView<KuiklyRenderViewExportProtocol>
/*
 * @brief 自定义注册PAGView实现(默认只要pod 'libpag', ">= 3.3.0.245-noffmpeg"，就无需注册)
 * @param creator 创建pageView实例
 */
+ (void)registerPAGViewCreator:(PAGViewCreator)creator;
/*
 * @brief 获取(下载)cdnUrl对应的pag文件接口
 * 注：如果该文件已存在（下载过&未被清理），则直接返回使用本地缓存文件
 */
- (void)fetchPagFileIfNeedWithCdnUrl:(NSString *)url completion:(KRPagFileResponse)completion;
/*
 * @brief 根据cdnUrl返回唯一的Pag文件本地磁盘存储地址，用于判断文件是否已经存在
 */
+ (NSString *)generateStorePathWithCdnUrl:(NSString *)cdnUrl;

@end
@class PAGView;
@protocol IPAGViewListener,
            IPAGTextProtocol,
            IPAGLayerProtocol,
            IPAGCompositionProtocol,
            PAGImageProtocol,
            PAGImageLayerProtocol,
            IPAGTextLayerProtocol;

// The same with PagView

@protocol KRPagViewProtocol <NSObject>

@required

@property (nonatomic, weak) KRPAGView *ownerPagView;
/**
 * Load a pag file from the specified path, returns false if the file does not exist or the data is
 * not a pag file. Note: All PAGFiles loaded by the same path share the same internal cache. The
 * internal cache is alive until all PAGFiles are released. Use '[PAGFile Load:size:]' instead if
 * you don't want to load a PAGFile from the internal caches.
 */
- (BOOL)setPath:(NSString*)filePath;

/**
 * Adds a listener to the set of listeners that are sent events through the life of an animation,
 * such as start, repeat, and end.
 */
- (void)addListener:(id<IPAGViewListener>)listener;

/**
 * Removes a listener from the set listening to this animation.
 */
- (void)removeListener:(id<IPAGViewListener>)listener;

/**
 * Start the animation.
 */
- (void)play;

/**
 * Stop the animation.
 */
- (void)stop;

/**
 * Set the number of times the animation will repeat. The default is 1, which means the animation
 * will play only once. 0 means the animation will play infinity times.
 */
- (void)setRepeatCount:(int)repeatCount;

/**
 * Returns the current PAGComposition for PAGView to render as content.
 */
- (id<IPAGCompositionProtocol>)getComposition;

@optional
// kuikly侧设置的属性，一般用于业务扩展使用
- (void)kr_setKuiklyPropWithKey:(NSString *)propKey propValue:(id)propValue;

@end

// The same with IPAGViewListener
@protocol IPAGViewListener <NSObject>

@required
/**
 * Notifies the start of the animation.
 */
- (void)onAnimationStart:(PAGView*)pagView;

/**
 * Notifies the end of the animation.
 */
- (void)onAnimationEnd:(PAGView*)pagView;

/**
 * Notifies the cancellation of the animation.
 */
- (void)onAnimationCancel:(PAGView*)pagView;

/**
 * Notifies the repetition of the animation.
 */
- (void)onAnimationRepeat:(PAGView*)pagView;


@end

/**
 * The PAGText object stores a value for a TextLayer's Source Text property.
 */
@protocol IPAGTextProtocol <NSObject>

/**
 * The text layer’s Source Text value.
 */
@property(nonatomic, copy) NSString *text;

@end

@protocol IPAGCompositionProtocol <NSObject>

/**
 * Returns an array of layers that match the specified layer name.
 */
- (NSArray<id<IPAGLayerProtocol>>*)getLayersByName:(NSString *)layerName;

@end

@protocol IPAGLayerProtocol <NSObject>
@end

@protocol PAGImageLayerProtocol <IPAGLayerProtocol>

/**
 * Replace the original image content with the specified PAGImage object.
 * Passing in null for the image parameter resets the layer to its default image content.
 * The setImage() method only modifies the content of the calling PAGImageLayer.
 *
 * @param image The PAGImage object to replace with.
 */
- (void)setImage:(id<PAGImageProtocol>)image;

@end

/**
 * The PAGText object stores a value for a TextLayer's Source Text property.
 */
@protocol IPAGTextLayerProtocol <IPAGLayerProtocol>

/**
 * The text layer’s Source Text value.
 */
- (void)setText:(NSString*)text;

@end

@protocol PAGImageProtocol <NSObject>

/**
 * Creates a PAGImage object from a path of a image file, return null if the file does not exist or
 * it's not a valid image file.
 */
+ (id<PAGImageProtocol>)FromPath:(NSString*)path;

@end

NS_ASSUME_NONNULL_END
