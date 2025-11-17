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


package com.tencent.kuikly.demo.pages.video.cover

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.event.Event

class QBCoverView : DeclarativeBaseView<CoverAttr, CoverEvent>() {

    override fun createAttr(): CoverAttr {
        return CoverAttr()
    }

    override fun createEvent(): CoverEvent {
        return CoverEvent()
    }

    override fun viewName(): String {
        return "QBCoverView"
    }

}

class CoverAttr: Attr() {

    fun src(src: String): CoverAttr {
        "src" with src
        return this
    }

    fun topBarHeight(height: Int): CoverAttr {
        "topBarHeight" with height
        return this
    }

    fun progressBarBottom(progressBarBottom: Int): CoverAttr {
        "progressBarBottom" with progressBarBottom
        return this
    }

    fun coverWidth(coverWidth: Int): CoverAttr {
        "coverWidth" with coverWidth
        return this
    }

    fun coverHeight(coverHeight: Int): CoverAttr {
        "coverHeight" with coverHeight
        return this
    }

    fun ignoreGauss(ignoreGauss: Boolean): CoverAttr {
        "ignoreGauss" with ignoreGauss
        return this
    }

    fun props(src: String, topBarHeight: Float, progressBarBottom: Int, coverWidth: Int, coverHeight: Int, ignoreGauss: Boolean): CoverAttr{
        val ignoreGaussInt = if(ignoreGauss) 1 else 0
        "props" with mapOf<String, Any>("src" to src,
            "topBarHeight" to topBarHeight,
            "progressBarBottom" to progressBarBottom,
            "coverWidth" to coverWidth,
            "coverHeight" to coverHeight,
            "ignoreGauss" to ignoreGaussInt)
        return this
    }
}


class CoverEvent: Event() {
    fun onLoadEnd(handler:() -> Unit) {
        register("onLoadEnd") {
            handler()
        }
    }

}

fun ViewContainer<*, *>.QBCover(init: QBCoverView.() -> Unit) {
    addChild(QBCoverView(), init)
}