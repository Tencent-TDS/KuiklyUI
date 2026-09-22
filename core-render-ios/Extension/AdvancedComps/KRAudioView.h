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

#import "KRUIKit.h" // [macOS]
#import "KuiklyRenderViewExportProtocol.h"
NS_ASSUME_NONNULL_BEGIN
@protocol KRAudioViewProtocol;
@protocol KRAudioViewDelegate;

/*
 * 创建音频播放器视图实例闭包
 * @param src 初始化的播放源数据（一般为音频 cdn url）
 * @param frame 初始化视图位置大小
 * @return 返回一个实现了 KRAudioViewProtocol 协议且为 UIView 子类的播放器实例
 */
typedef id<KRAudioViewProtocol> _Nonnull (^AudioViewCreator)(NSString *src, CGRect frame);


@interface KRAudioView : UIView<KuiklyRenderViewExportProtocol>

/*
 * @brief 注册 AudioView 实现
 * @param creator 创建 AudioView 实例
 */
+ (void)registerAudioViewCreator:(AudioViewCreator)creator;

@end

@protocol KRAudioViewProtocol <NSObject>

@required
/*
 * 播放器事件变化回调代理(如：播放状态变化回调 or 播放时间变化回调该代理)
 */
@property (nonatomic, weak) id<KRAudioViewDelegate> kra_delegate;
/*
 * 播放音频
 */
- (void)kra_play;
/*
 * 暂停音频
 */
- (void)kra_pause;
/*
 * 停止并销毁音频
 */
- (void)kra_stop;
/*
 * 设置倍速（1.0, 1.5, 2.0）
 */
- (void)kra_setRate:(CGFloat)rate;
/*
 * seek 音频
 * @param seekTotime 时间，单位毫秒
 */
- (void)kra_seekToTime:(NSUInteger)seekTotime;

@optional
/*
 * kuikly 侧设置的属性，一般用于业务扩展使用
 */
- (void)kra_setPropWithKey:(NSString *)propKey propValue:(id)propValue;
/*
 * kuikly 侧调用方法，一般用于业务扩展使用
 */
- (void)kra_callWithMethod:(NSString * _Nonnull)method
                    params:(NSString * _Nullable)params;
@end

//播放状态
typedef NS_ENUM(NSInteger, KRAudioPlayState) {
    KRAudioPlayStateUnknown = 0,
    KRAudioPlayStatePlaying = 1, // 正在播放中
    KRAudioPlayStateCaching = 2, // 缓冲中
    KRAudioPlayStatePaused = 3,  // 播放暂停
    KRAudioPlayStatePlayEnd = 4, // 播放结束
    KRAudioPlayStateFaild = 5,   // 播放失败
};


@protocol KRAudioViewDelegate <NSObject>

@required
/*
 * @brief 播放状态发生变化时回调
 */
- (void)audioPlayStateDidChangedWithState:(KRAudioPlayState)playState extInfo:(NSDictionary<NSString *, NSString *> *)extInfo;
/*
 * @brief 播放时间发生变化时回调
 * @param currentTime 当前播放时间，单位毫秒
 * @param totalTime 音频总时长，单位毫秒
 */
- (void)playTimeDidChangedWithCurrentTime:(NSUInteger)currentTime totalTime:(NSUInteger)totalTime;

@end



NS_ASSUME_NONNULL_END
