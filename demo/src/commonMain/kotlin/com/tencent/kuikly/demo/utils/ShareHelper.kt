package com.tencent.kuikly.demo.utils

data class ShareConfig(val title: String, val path: String = "", val imageUrl: String = "")

expect fun onShareAppMessage(block: (Any?) -> ShareConfig?)
