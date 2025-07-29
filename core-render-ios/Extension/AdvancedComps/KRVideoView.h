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
NS_ASSUME_NONNULL_BEGIN
@protocol KRVideoViewProtocol;
@protocol KRVideoViewDelegate;

typedef NS_ENUM(NSInteger, KRVideoViewContentMode) {
    KRVideoViewContentModeScaleAspectFit, // 按原视频比例显示，是竖屏的就显示出竖屏的，两边留黑；
    KRVideoViewContentModeScaleAspectFill, // 按原比例拉伸视频，直到两边都占满
    KRVideoViewContentModeScaleToFill, // 拉伸视频内容达到边框占满，但不按原比例拉伸
};

/*
 * 创建播放器视图实例闭包
 * @param src 初始化的播放源数据，该值可能为cdn 视频url，或者 json string数据（业务可与kotlin侧约定该src传值内容）
 * @param frame 初始化视图位置大小
 * @return 返回一个实现了KRVideoViewProtocol协议且为UIView子类的播放器实例
 */
typedef id<KRVideoViewProtocol> _Nonnull (^VideoViewCreator)(NSString *src, CGRect frame);


@interface KRVideoView : UIView<KuiklyRenderViewExportProtocol>

/*
 * @brief 注册VideoView实现
 * @param creator 创建VideoView实例
 */
+ (void)registerVideoViewCreator:(VideoViewCreator)creator;

@end

@protocol KRVideoViewProtocol <NSObject>

@required
/*
 * 播放器事件变化回调代理(如：播放状态变化回调 or 播放时间变化回调该代理)
 */
@property (nonatomic, weak) id<KRVideoViewDelegate> krv_delegate;
/*
 * 预播放到第一帧（停在第一帧，用于预加载优化）
 */
- (void)krv_preplay;
/*
 * 播放视频
 */
- (void)krv_play;
/*
 * 暂停视频
 */
- (void)krv_pause;
/*
 * 停止并销毁视频
 */
- (void)krv_stop;
/*
 * 设置画面拉伸模式
 */
- (void)krv_setVideoContentMode:(KRVideoViewContentMode)videoViewContentMode;
/*
 * 设置静音属性
 */
- (void)krv_setMuted:(BOOL)muted;
/*
 * 设置倍速（1.0, 1.5, 2.0）
 */
- (void)krv_setRate:(CGFloat)rate;
/*
 * seek视频
 * @param time 时间，单位毫秒
 */
- (void)krv_seekToTime:(NSUInteger)seekTotime;

@optional
/*
 * kuikly侧设置的属性，一般用于业务扩展使用
 */
- (void)krv_setPropWithKey:(NSString *)propKey propValue:(id)propValue;
/*
 * kuikly侧调用方法，一般用于业务扩展使用
 */
- (void)krv_callWithMethod:(NSString * _Nonnull)method
                    params:(NSString * _Nullable)params;
@end

//播放状态
typedef NS_ENUM(NSInteger, KRVideoPlayState) {
    KRVideoPlayStateUnknown = 0,
    KRVideoPlayStatePlaying = 1, // 正在播放中 （注：回调该状态时，视频应该是有画面的）
    KRVideoPlayStateCaching = 2, // 缓冲中  （注：如果未调用过VAVideoPlayStatusPlaying状态，不能调用该状态）
    KRVideoPlayStatePaused = 3,  // 播放暂停 （注：如果一个视频处于PrepareToPlay状态，此时调用了暂停操作， 应该回调该状态）
    KRVideoPlayStatePlayEnd = 4, // 播放结束
    KRVideoPlayStateFaild = 5,   // 播放失败
};


@protocol KRVideoViewDelegate <NSObject>

@required
/*
 * @brief 播放状态发生变化时回调
 */
- (void)videoPlayStateDidChangedWithState:(KRVideoPlayState)playState extInfo:(NSDictionary<NSString *, NSString *> *)extInfo;
/*
 * @brief 播放时间发生变化时回调
 * @param currentTime 当前播放时间，单位毫秒
 * @param totalTime 视频总时长，单位毫秒
 */
- (void)playTimeDidChangedWithCurrentTime:(NSUInteger)currentTime totalTime:(NSUInteger)totalTime;
/*
 * @brief 视频首帧画面上屏显示时回调该方法（kotlin侧通过该时机来隐藏视频封面）
 */
- (void)videoFirstFrameDidDisplay;
/*
 * @brief 业务自定义扩展事件通用事件通道
 */
- (void)customEventWithInfo:(NSDictionary<NSString *, NSString *> *)eventInfo;


@end



NS_ASSUME_NONNULL_END
