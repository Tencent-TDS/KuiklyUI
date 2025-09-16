package com.tencent.kuikly.core.render.web.adapter

object KuiklyRenderAdapterManager {
    /**
     * Color conversion adapter
     */
    var krColorParseAdapter: IKRColorParserAdapter? = null

    /**
     * Kuikly Log adapter
     */
    var krLogAdapter: IKRLogAdapter? = null

    /**
     * Text post-processor adapter
     */
    var krTextPostProcessorAdapter: IKRTextPostProcessorAdapter? = null
}

object KuiklyRenderLog {
    fun i(tag: String, msg: String) {
        KuiklyRenderAdapterManager.krLogAdapter?.i(tag, msg)
    }

    fun d(tag: String, msg: String) {
        KuiklyRenderAdapterManager.krLogAdapter?.d(tag, msg)
    }

    fun e(tag: String, msg: String) {
        KuiklyRenderAdapterManager.krLogAdapter?.e(tag, msg)
    }
}
