package com.example.kuiklyworkwithminiapp.adapter

import android.util.Log
import com.tencent.kuikly.core.render.android.adapter.IKRLogAdapter

object KRLogAdapter : IKRLogAdapter {

    override val asyncLogEnable: Boolean
        get() = true

    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun e(tag: String, msg: String) {
        Log.e(tag, msg)
    }
}