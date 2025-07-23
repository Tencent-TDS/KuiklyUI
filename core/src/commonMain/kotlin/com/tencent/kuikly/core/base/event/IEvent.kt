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

package com.tencent.kuikly.core.base.event

import com.tencent.kuikly.core.base.AbstractBaseView
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.RenderView

/**
 *  事件中心接口类。在我们这个跨平台UI框架的背景下，这里说的事件是指专门针对View的事件。整个event模块都只会用于View对象的事件管理。
 *  提供了三个维度的接口：
 *  1、提供事件的注册、取消注册和通知接口
 *  2、提供该事件中心所属的具体view的一些关键行为的回调，比如View的创建和移除，便于做事件中心的初始化和清理动作。
 *  3、提供获取该事件中心所属的view和对应终端侧render view的接口
 *
 *  整个event模块的核心类是[Event]类，该类包含了所有view都可以监听和处理的事件集合，并且给上层业务侧提供了便捷的事件监听方法。
 *  具体使用方式可以查看[Event]类。
 *
 */
interface IEvent {

    /**
     * 初始化该事件中心，传入必须的参数。
     * 因为存在创建对象后再设置的场景，所以无法把这些参数作为构造函数的参数
     * @param pagerId 该事件中心所属的view所在page的pageId
     * @param viewId 该事件中心所属的view的Id
     */
    fun init(pagerId:String, viewId:Int)

    /**
     * 注册事件
     * @param eventName 事件名称
     * @param eventHandlerFn 事件处理函数
     */
    fun register(eventName: String, eventHandlerFn: EventHandlerFn)

    /**
     * 取消注册
     * @param eventName 需要取消的事件
     */
    fun unRegister(eventName: String)

    /**
     * 当事件发生时调用处理函数
     * @param eventName 事件名称
     * @param data 该事件附带的数据，比如点击事件的具体点击位置信息
     * @return 如果有处理函数被找到并调用则返回true，否则返回false
     */
    fun onFireEvent(eventName: String, data: Any?): Boolean

    /**
     * 事件中心是否有注册了任意的事件
     * @return 如果事件中心有注册事件就返回true，否则返回false
     */
    fun isEmpty(): Boolean

    /**
     * 当该事件中心所属的view被移除时。这个view指的是当前UI框架里定义的view，区别于终端侧native的view，我们称之为render view。
     * 一般的实现方法是清理掉所有注册的事件，简单来说就是map.clear（）
     */
    fun onViewDidRemove()

    /**
     * 当终端侧native的view创建的时候。这里说的view比如Android的TextView或者iOS的UILabel。
     */
    fun onRenderViewDidCreated()

    /**
     * 当终端侧native的view移除的时候
     */
    fun onRenderViewDidRemoved()

    /**
     * 获取该事件中心所属的view
     * @return 所属的view
     */
    fun getView() : AbstractBaseView<*, *>?

    /**
     * 获取该事件中心所属的view对应的终端侧的render view
     * @return 所属的view对应的终端侧的render view
     */
    fun getRenderView() : RenderView?

    /**
     * 当renderView的Frame有变化时回调该方法
     * @param view 所属的view，目前只支持声明式view
     */
    fun onRelativeCoordinatesDidChanged(view: DeclarativeBaseView<*, *>)

    /**
     * 当view的LayoutFrame有变化时回调该方法
     * @param view 所属的view，目前只支持声明式view
     */
    fun onViewLayoutFrameDidChanged(view: DeclarativeBaseView<*, *>)

}

//定义事件处理函数
typealias EventHandlerFn = (parma: Any?) -> Unit