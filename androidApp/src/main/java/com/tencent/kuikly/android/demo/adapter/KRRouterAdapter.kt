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

package com.tencent.kuikly.android.demo.adapter

import android.app.Activity
import android.content.Context
import com.tencent.kuikly.android.demo.KuiklyRenderActivity
import com.tencent.kuikly.android.demo.NativeMixKuiklyViewDemoActivity
import com.tencent.kuikly.core.render.android.adapter.IKRRouterAdapter
import org.json.JSONObject

/**
 * Created by kam on 2023/4/19.
 */
class KRRouterAdapter : IKRRouterAdapter {

    override fun openPage(
        context: Context,
        pageName: String,
        pageData: JSONObject,
    ) {
        if (pageName == "NativeMixKuikly") {
            NativeMixKuiklyViewDemoActivity.start(context)
        } else {
            KuiklyRenderActivity.start(context, pageName, pageData)
        }
    }

    override fun closePage(context: Context) {
        (context as? Activity)?.finish()
    }
}