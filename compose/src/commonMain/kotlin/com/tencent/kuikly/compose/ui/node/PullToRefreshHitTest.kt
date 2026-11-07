/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * The file is licensed under the Apache License, Version 2.0.
 */

package com.tencent.kuikly.compose.ui.node

import com.tencent.kuikly.compose.ui.Modifier

/**
 * Lets hit testing take the native content inset applied by pull-to-refresh into account.
 *
 * The refresh-state displacement is applied by the render layer through `setContentInset`
 * (a content-view margin on HarmonyOS, `translationY` on Android). It only moves rendered
 * pixels and does not take part in Compose layout, while hit testing is computed from the
 * Compose layout tree and the pointer position. As a result, while the list is refreshing the
 * drawn position and the hit-tested position differ by the inset.
 *
 * The pull-to-refresh item registers its list node together with the current inset (px). When a
 * DOWN event arrives, if the hit path belongs to a registered list, the pointer is hit tested
 * again at `y - inset` and the second result is used. Nodes outside the list subtree (top bar,
 * bottom navigation, overlays, ...) are not affected.
 */
internal object PullToRefreshHitTest {
    private val entries = HashMap<LayoutNode, Int>()

    fun set(listNode: LayoutNode, insetPx: Int) {
        if (insetPx > 0) {
            entries[listNode] = insetPx
        } else {
            entries.remove(listNode)
        }
    }

    fun remove(listNode: LayoutNode) {
        entries.remove(listNode)
    }

    /**
     * @return the inset (px) that has to be compensated for the given hit path, or 0 when the hit
     * path does not belong to a registered list.
     */
    fun insetForHit(nodes: List<Modifier.Node>): Int {
        if (entries.isEmpty() || nodes.isEmpty()) {
            return 0
        }
        for ((listNode, inset) in entries) {
            if (inset <= 0) continue
            for (hit in nodes) {
                var node: LayoutNode? = hit.coordinator?.layoutNode
                while (node != null) {
                    if (node === listNode) {
                        return inset
                    }
                    node = node.parent
                }
            }
        }
        return 0
    }
}
