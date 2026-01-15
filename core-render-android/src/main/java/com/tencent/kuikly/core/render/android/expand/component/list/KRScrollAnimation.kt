/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 */

package com.tencent.kuikly.core.render.android.expand.component.list

/**
 * 滚动动画抽象基类
 * 统一管理动画的生命周期和回调
 */
internal abstract class KRScrollAnimation {
    // 共享的回调属性
    open var onUpdate: (Float) -> Unit = {}
    open var onEnd: () -> Unit = {}

    // 抽象方法：子类实现具体的启动和取消逻辑
    abstract fun start()
    abstract fun cancel()
}