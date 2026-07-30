package com.tencent.kuikly.core

/**
 * Created by kamlin on 2022/4/24.
 */
external fun nativeLog(msg: String)

external fun callNative(
    methodId: Int,
    arg0: Any?,
    arg1: Any?,
    arg2: Any?,
    arg3: Any?,
    arg4: Any?,
    arg5: Any?
): Any?