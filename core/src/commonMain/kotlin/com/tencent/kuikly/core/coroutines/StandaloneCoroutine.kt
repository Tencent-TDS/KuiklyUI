package com.tencent.kuikly.core.coroutines

import kotlin.coroutines.CoroutineContext

internal class StandaloneCoroutine(
    parentContext: CoroutineContext,
) : AbstractCoroutine<Unit>(parentContext) {

    override fun resumeWith(result: Result<Unit>) {}
}