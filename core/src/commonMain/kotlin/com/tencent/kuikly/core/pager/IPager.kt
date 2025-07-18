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

package com.tencent.kuikly.core.pager

import com.tencent.kuikly.core.base.AbstractBaseView
import com.tencent.kuikly.core.base.AnimationManager
import com.tencent.kuikly.core.coroutines.LifecycleScope
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.manager.Task
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject

interface IPager {
    var pageData: PageData
    var pageName : String
    val lifecycleScope : LifecycleScope
    var animationManager: AnimationManager?
    val isDebugUIInspector : Boolean
    var didCreateBody: Boolean
    fun onCreatePager(pagerId: String, pageData: JSONObject)

    fun onDestroyPager()

    fun onLayoutView()

    /*
     * native 首帧上屏后回调
     */
    fun onFirstFramePaint() {}

    fun onViewEvent(viewRef: Int, event: String, res: JSONObject?)

    fun <T : Module> getModule(name: String): T? {
        return null
    }

    fun <T : Module> acquireModule(name: String): T

    fun createExternalModules(): Map<String, Module>? {
        return null
    }

    /*
     * 页面可见时回调
     */
    fun pageDidAppear() { }
    /*
     * 页面不可见时回调
     */
    fun pageDidDisappear() { }
    /*
     * 页面将要销毁时回调
     */
    fun pageWillDestroy() { }
    /*
     * 同步UI上屏操作(UI队列中任务立即执行上屏，用于当前帧提前同步上屏优化)
     */
    fun syncFlushUI() { }

    fun themeDidChanged(data: JSONObject) {
    }

    /*
     * 绑定监听一个表达式变化
     * @param valueBlock 所要监听观测的表达式结果（如：{this.props.key} 即监听key变量变化）
     * @param byOwner 用于删除监听的对象标识（注：该值不是所要监听的表达式或者属性传值）
     * @param valueChange 表达式结果变化时回调 （注：首次调用该方法会马上回调，同时变化时也会回调）
     */
    fun bindValueChange(valueBlock: () -> Any, byOwner: Any, valueChange: (value: Any) -> Unit) {
    }
    /*
     * 删除所有该owner对象所对应的绑定监听（对应配对bindValueChange方法）
     * @param byOwner 监听的对象标识 （对应bindValueChange方法中的第二个参数byOwner）
     */
    fun unbindAllValueChange(byOwner: Any) {}

    fun addPagerEventObserver(observer: IPagerEventObserver)

    fun removePagerEventObserver(observer: IPagerEventObserver)

    fun addPagerLayoutEventObserver(observer: IPagerLayoutEventObserver)

    fun removePagerLayoutEventObserver(observer: IPagerLayoutEventObserver)

    fun onReceivePagerEvent(pagerEvent: String, eventData: JSONObject)

    fun putNativeViewRef(nativeRef: Int, view: AbstractBaseView<*, *>)

    fun removeNativeViewRef(nativeRef: Int)

    fun getViewWithNativeRef(nativeRef: Int): AbstractBaseView<*, *>? {
        return null
    }

    fun addNextTickTask(task: Task)

    fun addTaskWhenPagerUpdateLayoutFinish(task: () -> Unit)

    fun addTaskWhenPagerDidCalculateLayout(task: () -> Unit)

    fun setMemoryCache(key: String, value: Any) {
    }

    fun getValueForKey(key: String): Any? {
        return null
    }

    fun isWillDestroy(): Boolean {
        return false
    }

    fun didInit() {
    }

    fun isNightMode(): Boolean {
        return false
    }
    // UI视图调试开关
    fun debugUIInspector(): Boolean {
        return false
    }

    /**
     *  是否允许页面中字体自定义缩放
     * @return 是否运行，默认为NO，如为YES，端侧需实现android#KuiklyRenderAdapterManager.krFontAdapter&&iOS#KuiklkyRenderBirdge.registerFontHandler
     */
    fun scaleFontSizeEnable(): Boolean {
        return false
    }

    /**
     * 自定义返回页面对应的缩放系数
     */
    fun pagerDensity(): Float {
        return pageData.density
    }

    fun setPageTrace(pageTrace: PageCreateTrace) {}
}