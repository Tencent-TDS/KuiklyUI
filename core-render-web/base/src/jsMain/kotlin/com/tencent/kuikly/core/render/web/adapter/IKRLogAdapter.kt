package com.tencent.kuikly.core.render.web.adapter

/**
 * Kuikly log adapter
 */
interface IKRLogAdapter {
    /*
     * Whether to support asynchronous (sub-thread) log printing
     * Note: 1. If you don't care about the relative order of KLog and platform-side log printing, return true, i.e., prioritize performance
     *       2. Regardless of asynchronous or synchronous, KLog interface printing maintains relative timing
     */
    val asyncLogEnable: Boolean

    /**
     * Print info level log
     * @param tag Log tag
     * @param msg Log message
     */
    fun i(tag: String, msg: String)

    /**
     * Print debug level log
     * @param tag Log tag
     * @param msg Log message
     */
    fun d(tag: String, msg: String)

    /**
     * Print error level log
     * @param tag Log tag
     * @param msg Log message
     */
    fun e(tag: String, msg: String)
}
