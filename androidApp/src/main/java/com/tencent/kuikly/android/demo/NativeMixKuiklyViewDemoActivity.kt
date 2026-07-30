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

package com.tencent.kuikly.android.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.tencent.kuikly.core.render.android.css.ktx.toPxI
import com.tencent.kuikly.core.render.android.expand.KuiklyBaseView

/**
 * Created by kam on 2023/9/5.
 */
class NativeMixKuiklyViewDemoActivity : AppCompatActivity() {

    private var kuiklyView: KuiklyBaseView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_mix_kuikly_view)
        val rootView = findViewById<ViewGroup>(R.id.root_view)
        val maskView = findViewById<View>(R.id.bg_mask)
        findViewById<TextView>(R.id.show_kuikly_view).setOnClickListener {
            kuiklyView = KuiklyBaseView(this).apply {
                layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 500f.toPxI())
            }
            kuiklyView?.onAttach("", "ViewExamplePage", mapOf())
            maskView?.visibility = View.VISIBLE
            rootView.addView(kuiklyView)
        }
        maskView?.setOnClickListener {
            kuiklyView?.onDetach()
            rootView.removeView(kuiklyView)
            maskView.visibility = View.GONE
        }
    }

    override fun onPause() {
        super.onPause()
        kuiklyView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        kuiklyView?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        kuiklyView?.onDetach()
    }

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, NativeMixKuiklyViewDemoActivity::class.java)
            context.startActivity(starter)
        }
    }
}