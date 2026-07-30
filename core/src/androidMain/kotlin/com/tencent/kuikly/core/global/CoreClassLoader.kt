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

package com.tencent.kuikly.core.global

import com.tencent.kuikly.core.base.Attr
import com.tencent.kuikly.core.base.BaseObject
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ContainerAttr
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.event.Event
import com.tencent.kuikly.core.coroutines.GlobalScope
import com.tencent.kuikly.core.directives.DirectivesView
import com.tencent.kuikly.core.layout.FlexLayout
import com.tencent.kuikly.core.layout.FlexLayoutCache
import com.tencent.kuikly.core.layout.FlexLayoutContext
import com.tencent.kuikly.core.layout.FlexNode
import com.tencent.kuikly.core.layout.FlexStyle
import com.tencent.kuikly.core.layout.LayoutImpl
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.ModuleConst
import com.tencent.kuikly.core.nvi.serialization.json.JSONArray
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.PageData
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.ReactiveObserver
import com.tencent.kuikly.core.utils.ConvertUtil
import com.tencent.kuikly.core.views.DivView
import com.tencent.kuikly.core.views.ImageAttr
import com.tencent.kuikly.core.views.ImageView
import com.tencent.kuikly.core.views.ListAttr
import com.tencent.kuikly.core.views.ListContentView
import com.tencent.kuikly.core.views.ListView
import com.tencent.kuikly.core.views.PageListView
import com.tencent.kuikly.core.views.RichTextView
import com.tencent.kuikly.core.views.TextAttr
import com.tencent.kuikly.core.views.TextView
import com.tencent.kuikly.core.views.compose.ButtonView
import com.tencent.kuikly.core.views.shadow.TextShadow

internal class InnerPagerClassLoader : Pager() {
    override fun body(): ViewBuilder {
        return {

        }
    }

}

internal class InnerDirectivesViewClassLoader : DirectivesView()

class CoreClassLoader {
    init {
        BridgeManager
        JSONObject("{}")
        JSONArray("[]")
        PagerManager
        ViewConst
        GlobalScope
        PageData
        ModuleConst
        InnerPagerClassLoader::class.simpleName
        DivView::class.simpleName
        ListView::class.simpleName
        ListContentView::class.simpleName
        ListAttr()
        ContainerAttr()
        Event()
        Attr()
        InnerDirectivesViewClassLoader::class.simpleName
        TextView::class.simpleName
        ComposeEvent()
        RichTextView::class.simpleName
        ImageView::class.simpleName
        ImageAttr()
        TextAttr()
        ButtonView::class.simpleName
        PageListView::class.simpleName
        TextShadow::class.simpleName
        ReactiveObserver
        BaseObject()
        FlexLayout()
        FlexNode()
        FlexStyle()
        FlexLayoutContext()
        LayoutImpl
        FlexLayoutCache()
        GlobalFunctions::class.simpleName
        KLog
        Color
        ConvertUtil
    }
}