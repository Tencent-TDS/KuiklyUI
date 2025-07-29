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

#ifndef KuiklyRenderViewExportProtocol_h
#define KuiklyRenderViewExportProtocol_h
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "KuiklyRenderModuleExportProtocol.h"
#import "KRComponentDefine.h"

@protocol KuiklyRenderShadowProtocol;
@protocol KuiklyRenderViewLifyCycleProtocol;
@class KuiklyRenderView; // need import KuiklyRenderView.h
/*
 * @brief 渲染视图组件协议
 * 组件通过实现 KuiklyRenderViewExportProtocol 协议 完成一个 native ui组件暴露
 */
@protocol KuiklyRenderViewExportProtocol <KuiklyRenderViewLifyCycleProtocol>

@required
/*
 * @brief 更新属性时调用（注：主线程调用）
 * @param propKey 视图实例属性名
 * @param propValue 视图实例属性值，类型一般为字符串等基础数据结构以及KuiklyRenderCallback（用于事件绑定）
 */
- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue;

@optional
/*
 * @brief Kuikly根视图(如果需要使用该属性, 需要实现 @@synthesize hr_rootView; and need import KuiklyRenderView.h)
 * 注: view init 之后 该属性才被赋值
 */
@property (nonatomic, weak, nullable) KuiklyRenderView *hr_rootView;

/*
 * @brief 重置view，准备被复用 (可选实现)
 * 注：主线程调用，若实现该方法则意味着能被复用
 *    复用时被设置过的属性会被重新设置为nil(可能需要做该属性被设置nil时的对应额外重置处理)
 */
- (void)hrv_prepareForeReuse;
/*
 * @brief 创建shdow对象(可选实现)
 * 注：1.子线程调用, 若实现该方法则意味着需要自定义计算尺寸
 *    2.该shadow对象不能和renderView是同一个对象
 * @return 返回shadow实例
 */
+ (id<KuiklyRenderShadowProtocol> _Nonnull)hrv_createShadow;
/*
 * @brief 设置当前renderView实例对应的shadow对象 (可选实现, 注：主线程调用)
 * @param shadow shadow实例
 */
- (void)hrv_setShadow:(id<KuiklyRenderShadowProtocol> _Nonnull)shadow;
/*
 * @brief kotlin侧调用当前View的实例方法(注：主线程调用&可选实现)
 * @param method 方法名
 * @param params 方法参数
 * @param callback 方法中的异步回调闭包参包
 */
- (void)hrv_callWithMethod:(NSString * _Nonnull)method
                    params:(NSString * _Nullable)params
                  callback:(KuiklyRenderCallback _Nullable)callback;

@end

/** shadow 协议，一般用于自定义布局时实现 **/
@protocol KuiklyRenderShadowProtocol <NSObject>

@required
/*
 * @brief 更新shadow对象属性时调用（注：子线程调用）
 * @param propKey 属性名
 * @param propValue 属性值
 */
- (void)hrv_setPropWithKey:(NSString * _Nonnull)propKey propValue:(id _Nonnull)propValue;

 /*
  *@brief 根据布局约束尺寸计算返回RenderView的实际尺寸（注：子线程调用）
  * @param constraintSize 约束的最大尺寸
  */
- (CGSize)hrv_calculateRenderViewSizeWithConstraintSize:(CGSize)constraintSize;

/*
 * @brief 调用当前shadow的实例方法
 * @param method 方法名
 * @param params 方法参数
 * @return 同步返回给kotlin侧的返回值
 */
- (NSString * _Nullable)hrv_callWithMethod:(NSString * _Nonnull)method
                    params:(NSString * _Nullable)params;

@optional
/*
 * @brief 生成要在主线程执行的任务当将要设置shadow给RenderView时
 * 注：该方法在context线程被调用
 */
- (dispatch_block_t _Nullable)hrv_taskToMainQueueWhenWillSetShadowToView;

@end



/* KuiklyRenderViewLifyCycleProtocol */

@protocol KuiklyRenderViewLifyCycleProtocol <NSObject>

@optional
- (void)hrv_insertSubview:(UIView *_Nonnull)subView atIndex:(NSInteger)index;
- (void)hrv_removeFromSuperview;

@end



#endif /* KuiklyRenderViewExportProtocol_h */


