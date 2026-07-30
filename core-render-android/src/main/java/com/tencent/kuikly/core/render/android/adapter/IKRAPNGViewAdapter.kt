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

package com.tencent.kuikly.core.render.android.adapter

import android.content.Context
import android.view.View

interface IKRAPNGViewAdapter {
    /**
     * 创建APNGView
     * @param context
     * @return 实现了IAPNGView的示例
     */
    fun createAPNGView(context: Context): IAPNGView
}

interface IAPNGView {
    /**
     * 设置apng文件对应的资源路径
     * @param filePath filePath 本地资源路径（也可能来自kotlin侧设置src的字符串值）
     */
    fun setFilePath(filePath: String)
    /*
     * 返回self View
     */
    fun asView(): View
    /*
     * 设置重播次数（默认是0，为无限次播放）
     */
    fun setRepeatCount(count: Int)
    /*
     * 播放动画
     */
    fun playAnimation()
    /*
     * 停止动画
     */
    fun stopAnimation()
    /*
     * 添加动画事件监听者（如，播放结束）
     */
    fun addAnimationListener(listener: IAPNGViewAnimationListener)
    /*
     * 移除动画事件监听者
     */
    fun removeAnimationListener(listener: IAPNGViewAnimationListener)
    /**
     * kuikly侧设置的属性，一般用于业务扩展使用
     * @param propKey
     * @param propValue
     * @return 如果处理了该属性就返回true，否则false
     */
    fun setKRProp(propKey: String, propValue: Any): Boolean {
        return false
    }

}

interface IAPNGViewAnimationListener {
    /*
     * 动画结束回调（注：循环播放中不会回调该事件，直到播放动画完全停止才回调）
     */
    fun onAnimationEnd(apngView: View)
}