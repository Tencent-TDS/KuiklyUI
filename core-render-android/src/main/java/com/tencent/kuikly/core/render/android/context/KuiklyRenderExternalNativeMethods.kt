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

package com.tencent.kuikly.core.render.android.context

import java.util.concurrent.ConcurrentHashMap

/**
 * Process-global registry that lets business modules extend the set of
 * native methodIds handled by the Kuikly Render pipeline.
 *
 * Motivation
 * -----------
 * The built-in [KuiklyRenderNativeMethod] enum covers the render protocol
 * methods used by Kuikly Core (CreateRenderView, SetViewProp, …). But some
 * embedding scenarios need to dispatch **additional** methodIds through
 * the *same* NativeBridge transport (e.g. `SHOW_DATE_TIME_PICKER = 103`,
 * `EXECUTE_FUNCTION_CALL = 106`, `INVOKE_CUSTOM_FUNCTION = 107`).
 *
 * Rather than teach the render core about every possible business methodId,
 * this registry provides a small extension point:
 *
 * 1. Business modules call [register] at init time, passing the numeric
 *    methodId and a [Handler] that consumes the 6 wire arguments.
 * 2. When [KuiklyRenderJvmContextHandler.callNative] receives a methodId
 *    that does **not** map to a known [KuiklyRenderNativeMethod] enum value,
 *    it consults this registry via [dispatch]. If a handler is registered,
 *    it is invoked synchronously on the caller thread (the Kuikly Context
 *    Thread) and its return value is propagated back to the caller.
 *
 * Threading
 * ---------
 * Handlers are invoked on the *Kuikly Context Thread* — the same thread the
 * built-in native methods run on. Handlers that need to touch UI (e.g. pop
 * a dialog) must post to the Android main thread themselves. Handlers used
 * with a synchronous caller contract must return quickly.
 *
 * Lifetime
 * --------
 * Registration is process-global and lives for the lifetime of the process.
 * Callers responsible for their handler's transitive state (e.g. Activity
 * references, view lifetimes) must [unregister] when appropriate.
 */
object KuiklyRenderExternalNativeMethods {

    /**
     * Handler for an externally-registered native method.
     *
     * @param methodId The raw methodId as received on the wire.
     * @param args     Exactly 6 positional arguments (may contain nulls).
     * @return         The value to hand back to the Kuikly caller, or `null`
     *                 for void-like methods.
     */
    interface Handler {
        fun invoke(methodId: Int, args: List<Any?>): Any?
    }

    private val handlers: MutableMap<Int, Handler> = ConcurrentHashMap()

    /**
     * Register a handler for [methodId]. If a handler was previously
     * registered for the same id, it is replaced.
     */
    @JvmStatic
    fun register(methodId: Int, handler: Handler) {
        handlers[methodId] = handler
    }

    /**
     * Remove a previously registered handler. No-op if none was registered.
     */
    @JvmStatic
    fun unregister(methodId: Int) {
        handlers.remove(methodId)
    }

    /**
     * Whether a handler is registered for [methodId].
     */
    @JvmStatic
    fun contains(methodId: Int): Boolean = handlers.containsKey(methodId)

    /**
     * Look up and invoke the handler registered for [methodId] (if any).
     *
     * Called by [KuiklyRenderJvmContextHandler.callNative] as a fallback for
     * methodIds that do not match a [KuiklyRenderNativeMethod] enum value.
     *
     * @return the handler's return value, or `null` if none is registered.
     */
    @JvmStatic
    fun dispatch(methodId: Int, args: List<Any?>): Any? =
        handlers[methodId]?.invoke(methodId, args)
}
