package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ViewBuilder

@Page("p1")
internal class p1 : BasePager() {
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            
        }
    }
}