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
package com.tencent.kuikly.calendar.demo

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.core.render.android.adapter.IKRLogAdapter
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate

class MainActivity : AppCompatActivity() {

    private lateinit var delegator: KuiklyRenderViewBaseDelegator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupImmersiveMode()
        setupAdapters()

        val container = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.WHITE)
        }
        setContentView(container)

        val pageData = mutableMapOf<String, Any>(
            "appId" to 1,
            "sysLang" to (resources.configuration.locale?.language ?: "zh"),
            "debug" to if (BuildConfig.DEBUG) 1 else 0
        )

        delegator = KuiklyRenderViewBaseDelegator(object : KuiklyRenderViewBaseDelegatorDelegate {
            override fun debugLogEnable(): Boolean = true
            override fun coreExecuteModeX(): KuiklyRenderCoreExecuteModeBase = KuiklyRenderCoreExecuteModeBase.JVM
        })

        delegator.onAttach(
            container = container,
            contextCode = "",
            pageName = "CalendarDemoPage",
            pageData = pageData
        )
    }

    override fun onResume() {
        super.onResume()
        delegator.onResume()
    }

    override fun onPause() {
        super.onPause()
        delegator.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        delegator.onDetach()
    }

    private fun setupAdapters() {
        if (KuiklyRenderAdapterManager.krLogAdapter == null) {
            KuiklyRenderAdapterManager.krLogAdapter = object : IKRLogAdapter {
                override val asyncLogEnable: Boolean = false
                override fun d(tag: String, msg: String) { Log.d(tag, msg) }
                override fun e(tag: String, msg: String) { Log.e(tag, msg) }
                override fun i(tag: String, msg: String) { Log.i(tag, msg) }
            }
        }
    }

    private fun setupImmersiveMode() {
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = if (Build.VERSION.SDK_INT >= 26) Color.TRANSPARENT else 0x66000000
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                )
        }
    }
}
