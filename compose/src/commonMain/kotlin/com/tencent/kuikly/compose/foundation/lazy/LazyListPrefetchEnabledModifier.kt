/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.compose.foundation.lazy

import com.tencent.kuikly.compose.foundation.ExperimentalFoundationApi
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.modifier.ModifierLocal
import com.tencent.kuikly.compose.ui.modifier.ModifierLocalModifierNode
import com.tencent.kuikly.compose.ui.modifier.modifierLocalMapOf
import com.tencent.kuikly.compose.ui.modifier.modifierLocalOf
import com.tencent.kuikly.compose.ui.node.ModifierNodeElement

internal val ModifierLocalLazyListPrefetchEnabled = modifierLocalOf<Boolean?> { null }

/**
 * Opt-in prefetch for this LazyList. When omitted, [ComposeFoundationFlags.isLazyListPrefetchEnabled]
 * applies. Explicit false overrides a global true.
 */
@ExperimentalFoundationApi
fun Modifier.enableLazyListPrefetch(enabled: Boolean = true): Modifier =
    this then LazyListPrefetchEnabledElement(enabled)

@OptIn(ExperimentalFoundationApi::class)
private data class LazyListPrefetchEnabledElement(val enabled: Boolean) :
    ModifierNodeElement<LazyListPrefetchEnabledNode>() {
    override fun create() = LazyListPrefetchEnabledNode(enabled)

    override fun update(node: LazyListPrefetchEnabledNode) {
        node.enabled = enabled
        node.updateProvidedValue()
    }

    override fun equals(other: Any?): Boolean =
        other is LazyListPrefetchEnabledElement && other.enabled == enabled

    override fun hashCode(): Int = enabled.hashCode()
}

@OptIn(ExperimentalFoundationApi::class)
private class LazyListPrefetchEnabledNode(var enabled: Boolean) :
    Modifier.Node(), ModifierLocalModifierNode {

    override val providedValues =
        modifierLocalMapOf(ModifierLocalLazyListPrefetchEnabled to enabled)

    override fun onAttach() {
        updateProvidedValue()
    }

    fun updateProvidedValue() {
        provide(ModifierLocalLazyListPrefetchEnabled, enabled)
    }
}
