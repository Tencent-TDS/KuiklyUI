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

/**
 * Created by kam on 2023/5/17.
 */
interface IKRPAGViewAdapter {

    /**
     * 创建PAGView
     * @param context
     * @return
     */
    fun createPAGView(context: Context): IPAGView

}

interface IPAGView {

    fun asView(): View

    /**
     * 设置PAG文件对应的资源路径
     * @param filePath filePath 本地资源路径（也可能来自kotlin侧设置src的字符串值）
     */
    fun setFilePath(filePath: String)

    /*
     * 设置播放次数，默认一次
     */
    fun setPAGViewRepeatCount(count: Int)

    fun playPAGView()

    fun stopPAGView()

    fun addPAGViewListener(listener: IPAGViewListener)

    fun removePAGViewListener(listener: IPAGViewListener)

    /**
     * 替换当前 PAG 文件中的文字图层信息
     * @param layerName 对应的文字图层名称
     * @param text 替换文案内容
     */
    fun replaceTextLayerContent(layerName: String, text: String)

    /**
     * 替换当前 PAG 文件中的图片图层信息
     * @param layerName 对应图片图层名称
     * @param filePath 替换图片的文件路径
     */
    fun replaceImageLayerContent(layerName: String, filePath: String)

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

interface IPAGViewListener {

    fun onAnimationStart(pagView: View)

    fun onAnimationEnd(pagView: View)

    fun onAnimationCancel(pagView: View)

    fun onAnimationRepeat(pagView: View)

}