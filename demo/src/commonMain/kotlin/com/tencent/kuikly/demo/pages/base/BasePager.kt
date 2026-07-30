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

package com.tencent.kuikly.demo.pages.base

import com.tencent.kuikly.core.base.DeclarativeBaseView
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.pager.IViewCreator
import com.tencent.kuikly.core.pager.Pager
import com.tencent.kuikly.core.reactive.handler.*
import com.tencent.kuikly.core.views.RichText
import com.tencent.kuikly.demo.pages.base.extension.ExtRichTextView
import com.tencent.kuikly.demo.pages.base.extension.ExtTextView

/**
 * Created by kam on 2022/6/22.
 */

internal abstract class BasePager : Pager() {
    private var nightModel : Boolean? by observable(null)
    var pagerAppear: Boolean = true

    override fun createExternalModules(): Map<String, Module>? {
        val externalModules = hashMapOf<String, Module>()
        externalModules[BridgeModule.MODULE_NAME] = BridgeModule()
        externalModules[TDFTestModule.MODULE_NAME] = TDFTestModule()
        return externalModules
    }

    override fun body(): ViewBuilder {
        return {
            attr {

            }
            RichText {
                attr {

                }
            }

        }
    }

    override fun created() {
        super.created()

        registerViewCreator(ViewConst.TYPE_TEXT_CLASS_NAME, object : IViewCreator {
            override fun createView(): DeclarativeBaseView<*, *> {
                return ExtTextView()
            }
        })
        registerViewCreator(ViewConst.TYPE_RICH_TEXT_CLASS_NAME, object : IViewCreator {
            override fun createView(): DeclarativeBaseView<*, *> {
                return ExtRichTextView()
            }
        })
        isNightMode()
    }

    override fun themeDidChanged(data: JSONObject) {
        super.themeDidChanged(data)
        nightModel = data.optBoolean(IS_NIGHT_MODE_KEY)
    }

    // 是否为夜间模式
    override fun isNightMode(): Boolean {
        if (nightModel == null) {
            nightModel = pageData.params.optBoolean(IS_NIGHT_MODE_KEY)
        }
        return nightModel!!
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        pagerAppear = true
    }

    override fun pageDidDisappear() {
        super.pageDidDisappear()
        pagerAppear = false
    }

    companion object {
        const val IS_NIGHT_MODE_KEY = "isNightMode"
    }

}
