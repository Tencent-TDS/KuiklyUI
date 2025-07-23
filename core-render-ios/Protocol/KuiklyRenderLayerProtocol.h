/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

#ifndef KuiklyRenderLayerProtocol_h
#define KuiklyRenderLayerProtocol_h
#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
#import "KuiklyRenderViewExportProtocol.h"
#import "TDFModuleProtocol.h"
#import "KuiklyContextParam.h"

NS_ASSUME_NONNULL_BEGIN

#define KRV_ROOT_VIEW_TAG  @(-1)

/*
 * @brief 跨平台渲染层通用接口协议，native侧实现接口来实现原生组件渲染
 */
@protocol KuiklyRenderLayerProtocol <NSObject>

@required
/*
 * @brief 初始化
 * @param rootView 渲染层视图所在的宿主根视图
 * @param contextParam 上下文环境参数
 */
- (instancetype)initWithRootView:(UIView *)rootView contextParam:(KuiklyContextParam *)contextParam;
/*
 * @brief 完全初始化后调用
 */
- (void)didInit;

/*
 * @brief 创建渲染视图
 * @param tag 对应的视图索引id，用于Naitve侧与kotlin侧交互时对应的view id来索引
 * @param viewName 对应的视图类型，如Image，View等
 */
- (void)createRenderViewWithTag:(NSNumber *)tag
                       viewName:(NSString *)viewName;
/*
 * @brief 删除渲染视图
 * @param tag 对应的视图索引id
 */
- (void)removeRenderViewWithTag:(NSNumber *)tag;

/*
 * @brief 父渲染视图插入子渲染视图
 * @param parentTag 父亲视图id
 * @param childTag 插入的子视图id
 * @param index 插入位置
 */
- (void)insertSubRenderViewWithParentTag:(NSNumber *)parentTag
                                childTag:(NSNumber *)childTag
                                 atIndex:(NSInteger)index;
/*
 * @brief 设置渲染视图属性
 * @param tag 对应的视图索引id
 * @param propKey 属性名，如text
 * @param propValue 属性值
 */
- (void)setPropWithTag:(NSNumber *)tag propKey:(NSString *)propKey propValue:(id)propValue;
/*
 * @brief 设置view对应的shadow对象
 * @param tag 对应的视图索引id
 * @param shadow shadow实例
 */
- (void)setShadowWithTag:(NSNumber *)tag shadow:(id<KuiklyRenderShadowProtocol>)shadow;
/*
 * @brief 渲染视图更新坐标
 * @param tag 对应的视图索引id
 * @param frame 相对父亲的绝对坐标
 */
- (void)setRenderViewFrameWithTag:(NSNumber *)tag frame:(CGRect)frame;
/*
 * @brief 渲染视图返回自定义布局尺寸
 * @param tag 对应的视图索引id
 * @param constraintSize 约束的最大尺寸
 * @return 返回fit尺寸
 */
- (CGSize)calculateRenderViewSizeWithTag:(NSNumber *)tag constraintSize:(CGSize)constraintSize;
/*
 * @brief 调用渲染视图方法
 * @param tag 对应的视图索引id
 * @param method 方法名
 * @param params 方法参数
 * @param callback 方法中的异步回调闭包参数
 */
- (void)callViewMethodWithTag:(NSNumber *)tag
                       method:(NSString *)method
                       params:(NSString * _Nullable)params
                     callback:(KuiklyRenderCallback _Nullable)callback;

/*
 * @brief 调用module方法
 * @param moduleName 模块名
 * @param method 方法名
 * @param params 方法参数
 * @param callback 方法中的异步回调闭包参数
 */
- (NSString * _Nullable)callModuleMethodWithModuleName:(NSString *)moduleName
                       method:(NSString *)method
                       params:(NSString * _Nullable)params
                     callback:(KuiklyRenderCallback _Nullable)callback;


/*
 * @brief 调用tdf module方法
 * @param moduleName 模块名
 * @param method 方法名
 * @param params 方法参数
 * @param succCallbackId 成功回调, callbackId
 * @param errorCallback 错误回调, callbackId
 */
- (NSString * _Nullable)callTDFModuleMethodWithModuleName:(NSString *)moduleName
                                                   method:(NSString *)method
                                                   params:(NSString * _Nullable)params
                                           succCallbackId:(NSString *)succCallbackId
                                          errorCallbackId:(NSString *)errorCallbackId;

/****  shadow 相关 ***/
/*
 * @brief 创建shadow
 * @param tag 对应的view索引id
 * @param viewName 对应的view类型
 */
- (void)createShadowWithTag:(NSNumber *)tag
                       viewName:(NSString *)viewName;

/*
 * @brief 删除shadow
 * @param tag 对应的view索引id
 */
- (void)removeShadowWithTag:(NSNumber *)tag;
/*
 * @brief 更新shadow对象属性
 * @param tag 对应的shadow索引id
 * @param propKey 属性名
 * @param propValue 属性值
 */
- (void)setShadowPropWithTag:(NSNumber *)tag propKey:(NSString *)propKey propValue:(id)propValue;
/*
 * @brief 调用当前shadow的实例方法
 * @param tag 对应的shadow索引id
 * @param method 方法名
 * @param params 方法参数
 * @return 同步返回给kotlin侧的返回值
 */
- (NSString * _Nullable)callShadowMethodWithTag:(NSNumber *)tag method:(NSString * _Nonnull)method
                    params:(NSString * _Nullable)params;
/*
 * @brief 获取shadow实例
 * @param tag 对应的shadow索引id
 * @return shadow实例
 */
- (id<KuiklyRenderShadowProtocol>)shadowWithTag:(NSNumber *)tag;
/*
 * @brief 获取module实例
 * @param moduleName 模块名
 * @return module实例
 */
- (id<TDFModuleProtocol>)moduleWithName:(NSString *)moduleName;
/*
 * @brief 获取view实例
 * @param tag 对应的view索引id
 * @return view实例
 */
- (id<KuiklyRenderViewExportProtocol>)viewWithTag:(NSNumber *)tag;
/*
 * @brief 更新渲染视图的tag
 * @param curTag 当前要被修真的tag
 * @param newTag 更新到该tag值
 */
- (void)updateViewTagWithCurTag:(NSNumber *)curTag newTag:(NSNumber *)newTag;

@optional
/*
 * @brief 即将销毁时回调用
 */
- (void)willDealloc;
/**
 * @brief 收到手势响应时调用
 */
- (void)didHitTest;

@end


NS_ASSUME_NONNULL_END

#endif /* KuiklyRenderLayerProtocol_h */
