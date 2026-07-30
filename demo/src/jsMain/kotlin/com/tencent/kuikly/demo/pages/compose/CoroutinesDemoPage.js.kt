package com.tencent.kuikly.demo.pages.compose

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val Dispatchers.IO: CoroutineDispatcher
    get() = Dispatchers.Default