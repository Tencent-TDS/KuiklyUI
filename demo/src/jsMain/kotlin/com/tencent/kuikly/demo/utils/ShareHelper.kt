package com.tencent.kuikly.demo.utils

import com.tencent.kuikly.core.render.web.runtime.miniapp.page.MiniPageManage
import kotlin.js.json

actual fun onShareAppMessage(block: (Any?) -> ShareConfig?) {
    MiniPageManage.currentPage?.lifeCycle?.onShareAppMessage { args ->
        val config = block(args)
        if (config != null) {
            json("title" to config.title, "path" to config.path, "imageUrl" to config.imageUrl)
        } else {
            null
        }
    }
}
