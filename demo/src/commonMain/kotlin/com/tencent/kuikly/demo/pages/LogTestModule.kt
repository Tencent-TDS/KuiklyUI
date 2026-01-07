package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.module.Module

class LogTestModule : Module() {
    override fun moduleName(): String {
        return "KRLogTestModule"
    }

    fun test() : String {
        return syncToNativeMethod (
            methodName = "test",
            arrayOf(1,2,3),
            null
        ).toString()
    }
}
