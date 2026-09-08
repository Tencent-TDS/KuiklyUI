/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewConst
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.module.Module
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.TextConst
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.shadow.TextShadow
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

private const val CROSS_RUNTIME_TAG = "CrossRuntimeInvocationPerf"
private const val JSON_TAG = "JsonParsingPerf"

// JSON route metadata keys.
private const val KEY_CLICK_TS = "clickTimestampMs"
private const val KEY_FIELD_COUNT = "fieldCount"
private const val KEY_TIME = "time"
private const val KEY_BACKING = "jsonBacking"
private const val KEY_DYNAMIC_PREFIX = "dyn_"
private const val BACKING_KOTLIN = "kotlin"
private const val BACKING_CPP = "cpp"

private const val BYTES_PER_KB = 1024
private const val BYTES_PER_MB = 1024 * 1024
private const val TARGET_INITIAL_BYTES = 30 * BYTES_PER_KB
private const val INC_SMALL_BYTES = 30 * BYTES_PER_KB
private const val INC_MEDIUM_BYTES = 300 * BYTES_PER_KB
private const val INC_LARGE_BYTES = 3 * BYTES_PER_MB
private const val CALIBRATION_FIELDS = 200
private const val AVG_BYTES_PER_FIELD_FALLBACK = 17.0

private enum class PayloadShape(val id: String, val label: String) {
    PLAIN_STRING("plain_string", "普通 string"),
    JSON_STRING("json_string", "JSON string"),
    KR_RECORD("kr_record", "KRRecord 对象"),
}

private data class PayloadScale(val suffix: String, val chars: Int, val count: Int)

private val PAYLOAD_SCALES = listOf(
    PayloadScale("128k", 128 * 1024, 50),
    PayloadScale("384k", 384 * 1024, 20),
    PayloadScale("3m", 3 * 1024 * 1024, 3),
)

private val CASE_DEFS: Map<String, Pair<PayloadShape, PayloadScale>> =
    PAYLOAD_SCALES.flatMap { scale ->
        PayloadShape.values().map { shape ->
            caseName(shape, scale) to (shape to scale)
        }
    }.toMap()

private fun caseName(shape: PayloadShape, scale: PayloadScale): String =
    "${shape.id}_${scale.suffix}"

private fun initialResultLine(shape: PayloadShape, scale: PayloadScale): String =
    "ArkTS → Kotlin｜${shape.label}｜${formatPayloadBytes(scale.chars.toLong())} × ${scale.count} 次：未执行"

@Page("InteropPerfTestPage")
internal class InteropPerfTestPage : BasePager() {

    // ---- Cross-runtime invocation state ----
    private var crossStatus by observable("准备中：正在注册 ArkTS → Kotlin 回调")
    private var isTesting by observable(false)
    private var measureLine by observable(
        "Kotlin → ArkTS 同步文本测量｜$MEASURE_COUNT 次：未执行"
    )
    private var plainString128Line by observable(
        initialResultLine(PayloadShape.PLAIN_STRING, PAYLOAD_SCALES[0])
    )
    private var plainString384Line by observable(
        initialResultLine(PayloadShape.PLAIN_STRING, PAYLOAD_SCALES[1])
    )
    private var plainString3mLine by observable(
        initialResultLine(PayloadShape.PLAIN_STRING, PAYLOAD_SCALES[2])
    )
    private var jsonString128Line by observable(
        initialResultLine(PayloadShape.JSON_STRING, PAYLOAD_SCALES[0])
    )
    private var jsonString384Line by observable(
        initialResultLine(PayloadShape.JSON_STRING, PAYLOAD_SCALES[1])
    )
    private var jsonString3mLine by observable(
        initialResultLine(PayloadShape.JSON_STRING, PAYLOAD_SCALES[2])
    )
    private var krRecord128Line by observable(
        initialResultLine(PayloadShape.KR_RECORD, PAYLOAD_SCALES[0])
    )
    private var krRecord384Line by observable(
        initialResultLine(PayloadShape.KR_RECORD, PAYLOAD_SCALES[1])
    )
    private var krRecord3mLine by observable(
        initialResultLine(PayloadShape.KR_RECORD, PAYLOAD_SCALES[2])
    )
    private var registered = false
    private var payloadCallbackCount = 0
    private var completedCaseCount = 0

    // ---- JSON parsing state ----
    private val payloadJson: JSONObject = JSONObject()
    private var cppPayloadJson: JSONObject? = null
    private var nextFieldIndex: Int = 0
    private var bytesPerField: Double = AVG_BYTES_PER_FIELD_FALLBACK
    private var fieldCount: Int by observable(0)
    private var payloadBytes: Int by observable(0)
    private var jsonStatusText: String by observable("等待操作\n点击「打开页面」以启动一次测量。")

    override fun createExternalModules(): Map<String, Module>? {
        val modules = super.createExternalModules()?.toMutableMap() ?: hashMapOf()
        modules[InteropPerfTestModule.MODULE_NAME] = InteropPerfTestModule()
        return modules
    }

    override fun created() {
        super.created()
        payloadJson.put(KEY_FIELD_COUNT, 0)

        val headerBytes = payloadJson.toString().length
        appendFields(CALIBRATION_FIELDS)
        val calibrationBytes = payloadJson.toString().length
        val perField = (calibrationBytes - headerBytes).toDouble() / CALIBRATION_FIELDS
        if (perField > 0) {
            bytesPerField = perField
        }

        val remainingBytes = (TARGET_INITIAL_BYTES - calibrationBytes).coerceAtLeast(0)
        if (remainingBytes > 0) {
            appendFieldsForBytes(remainingBytes)
        }

        refreshMetrics()
        prepareCppPayload(reason = "init")
        KLog.i(
            JSON_TAG,
            "init: fieldCount=$fieldCount, payloadBytes=$payloadBytes, " +
                "bytesPerField=$bytesPerField, headerBytes=$headerBytes"
        )
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        if (registered) {
            return
        }
        registered = true
        registerNativeCallbacks()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }

            NavBar {
                attr {
                    title = "Interop Perf Test"
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    padding(16f)
                }

                Text {
                    attr {
                        text(
                            "跨 runtime 调用测试：覆盖普通 string、JSON string 和 KRRecord " +
                                "三种 payload，并分别测试 128 KB / 384 KB / 3 MB。"
                        )
                        fontSize(13f)
                        color(Color(0xFF555555))
                        marginBottom(12f)
                    }
                }

                Button {
                    attr {
                        height(60f)
                        marginBottom(16f)
                        borderRadius(6f)
                        backgroundColor(
                            if (ctx.isTesting) Color(0xFFB8B8B8L) else Color(0xFF1677FFL)
                        )
                        if (!ctx.isTesting) {
                            highlightBackgroundColor(Color(0x33111111))
                        }
                        alignSelfStretch()
                        testTag("run_cross_runtime_tests")
                        titleAttr {
                            text(if (ctx.isTesting) "Testing…" else "Run Cross Runtime Tests")
                            fontSize(16f)
                            color(Color.WHITE)
                        }
                    }
                    event {
                        click {
                            if (!ctx.isTesting) {
                                ctx.runAllCrossRuntimeBenches()
                            }
                        }
                    }
                }

                ResultLine("cross_status") { ctx.crossStatus }
                ResultLine("cross_measure") { ctx.measureLine }
                ResultLine("cross_plain_string_128k") { ctx.plainString128Line }
                ResultLine("cross_plain_string_384k") { ctx.plainString384Line }
                ResultLine("cross_plain_string_3m") { ctx.plainString3mLine }
                ResultLine("cross_json_string_128k") { ctx.jsonString128Line }
                ResultLine("cross_json_string_384k") { ctx.jsonString384Line }
                ResultLine("cross_json_string_3m") { ctx.jsonString3mLine }
                ResultLine("cross_kr_record_128k") { ctx.krRecord128Line }
                ResultLine("cross_kr_record_384k") { ctx.krRecord384Line }
                ResultLine("cross_kr_record_3m") { ctx.krRecord3mLine }

                View {
                    attr {
                        height(1f)
                        backgroundColor(Color(0xFFE5E5E5))
                        margin(top = 24f, bottom = 24f)
                    }
                }

                Text {
                    attr {
                        text(
                            "JSON 解析测试：预先准备 Kotlin backed 与 C++ backed 两份 JSONObject，" +
                                "点击打开页面时测量路由/解析耗时。"
                        )
                        fontSize(13f)
                        color(Color(0xFF555555))
                        marginBottom(12f)
                    }
                }

                View {
                    attr {
                        margin(bottom = 8f)
                        backgroundColor(Color(0xFFF0F4FF))
                        borderRadius(8f)
                        padding(all = 12f)
                    }
                    Text {
                        attr {
                            text(
                                "当前 JSONObject 字段数：${ctx.fieldCount}\n" +
                                    "当前 JSON 体积：${formatJsonBytes(ctx.payloadBytes)} " +
                                    "(${ctx.payloadBytes} 字节)"
                            )
                            fontSize(14f)
                            color(Color(0xFF333333))
                            testTag("json_perf_field_count_text")
                        }
                    }
                }

                ButtonRow(
                    title = "① 打开页面（kotlin backed json）",
                    bgColor = Color(0xFF007AFF),
                    testTagName = "json_perf_open_kotlin_btn",
                    onClick = { ctx.openJsonPage(cppBacked = false) }
                )
                ButtonRow(
                    title = "② 打开页面（c++ backed json）",
                    bgColor = Color(0xFF5856D6),
                    testTagName = "json_perf_open_cpp_btn",
                    onClick = { ctx.openJsonPage(cppBacked = true) }
                )
                ButtonRow(
                    title = "③ + 30 KB",
                    bgColor = Color(0xFF34C759),
                    testTagName = "json_perf_add_30kb_btn",
                    onClick = { ctx.addBytes(INC_SMALL_BYTES, "30 KB") }
                )
                ButtonRow(
                    title = "④ + 300 KB",
                    bgColor = Color(0xFFFF9500),
                    testTagName = "json_perf_add_300kb_btn",
                    onClick = { ctx.addBytes(INC_MEDIUM_BYTES, "300 KB") }
                )
                ButtonRow(
                    title = "⑤ + 3 MB",
                    bgColor = Color(0xFFFF3B30),
                    testTagName = "json_perf_add_3mb_btn",
                    onClick = { ctx.addBytes(INC_LARGE_BYTES, "3 MB") }
                )

                View {
                    attr {
                        margin(top = 24f, bottom = 16f)
                        backgroundColor(Color(0xFFF5F5F5))
                        borderRadius(8f)
                        padding(all = 16f)
                    }
                    Text {
                        attr {
                            text(ctx.jsonStatusText)
                            fontSize(14f)
                            color(Color(0xFF222222))
                            testTag("json_perf_status_text")
                        }
                    }
                }
            }
        }
    }

    // ---- Cross-runtime invocation helpers ----
    private fun runMeasureBench(): Boolean {
        val shadow = TextShadow(pagerId, MEASURE_SHADOW_REF, ViewConst.TYPE_RICH_TEXT)
        shadow.setProp(TextConst.FONT_SIZE, MEASURE_FONT_SIZE)
        shadow.setProp(TextConst.TEXT_USE_DP_FONT_SIZE_DIM, 1)
        val start = DateTime.currentTimestamp()
        var lastWidth = 0f
        var lastHeight = 0f
        try {
            for (i in 0 until MEASURE_COUNT) {
                shadow.setProp(TextConst.VALUE, "测$i 中文Abc🎉/${i * 17}")
                val size = shadow.calculateRenderViewSize(MEASURE_MAX_WIDTH, MEASURE_MAX_HEIGHT)
                lastWidth = size.width
                lastHeight = size.height
            }
        } catch (throwable: Throwable) {
            measureLine =
                "Kotlin → ArkTS 同步文本测量｜失败\n" +
                    "异常：${throwable.message ?: throwable.toString()}"
            crossStatus = "文本测量失败，请查看日志"
            KLog.e(CROSS_RUNTIME_TAG, "measure bench failed: $throwable")
            return false
        } finally {
            shadow.removeFromParentComponent()
        }
        val cost = DateTime.currentTimestamp() - start
        measureLine =
            "Kotlin → ArkTS 同步文本测量｜$MEASURE_COUNT 次\n" +
                "累计耗时：$cost ms｜平均：${formatAverageMs(cost, MEASURE_COUNT)} ms/次"
        crossStatus = "文本测量完成，继续执行 ArkTS → Kotlin 四类 payload 测试"
        KLog.i(CROSS_RUNTIME_TAG, measureLine)
        return true
    }

    private fun registerNativeCallbacks() {
        val module = acquireModule<InteropPerfTestModule>(
            InteropPerfTestModule.MODULE_NAME
        )
        module.setStatsCallback { data ->
            applyNativeStats(data)
        }
        val jsonSink: (JSONObject?) -> Unit = { data ->
            payloadCallbackCount++
            data?.length()
        }
        for (scale in PAYLOAD_SCALES) {
            val plainCase = caseName(PayloadShape.PLAIN_STRING, scale)
            module.setCallbackWithPlainString(plainCase) { data ->
                payloadCallbackCount++
                (data as? String)?.length
            }
            module.setCallbackWithJson(caseName(PayloadShape.JSON_STRING, scale), jsonSink)
            module.setCallbackWithJson(caseName(PayloadShape.KR_RECORD, scale), jsonSink)
        }
        crossStatus = "就绪：9 个 payload case 已注册；点击按钮开始跨 runtime 测试"
        KLog.i(CROSS_RUNTIME_TAG, crossStatus)
    }

    private fun runAllCrossRuntimeBenches() {
        isTesting = true
        payloadCallbackCount = 0
        completedCaseCount = 0
        crossStatus = "执行中：依次测试文本测量和 9 个 ArkTS → Kotlin payload case"
        if (!runMeasureBench()) {
            isTesting = false
            return
        }
        val module = acquireModule<InteropPerfTestModule>(
            InteropPerfTestModule.MODULE_NAME
        )
        for (scale in PAYLOAD_SCALES) {
            module.runPlainString(caseName(PayloadShape.PLAIN_STRING, scale), scale.count, scale.chars)
            module.runJsonString(caseName(PayloadShape.JSON_STRING, scale), scale.count, scale.chars)
            module.runKRRecord(caseName(PayloadShape.KR_RECORD, scale), scale.count, scale.chars)
        }
    }

    private fun applyNativeStats(data: JSONObject?) {
        if (data == null) {
            return
        }
        val caseName = data.optString("case", "")
        val count = data.optInt("count")
        val costMs = data.optLong("cost_ms")
        val bytes = data.optLong("bytes")
        val line =
            "${caseDescription(caseName)}\n" +
                "规模：${formatPayloadBytes(bytes / count.coerceAtLeast(1))} × $count｜" +
                "累计耗时：$costMs ms｜平均：${formatAverageMs(costMs, count)} ms/次"
        setResultLine(caseName, line)
        completedCaseCount++
        if (completedCaseCount == CASE_DEFS.size) {
            isTesting = false
            crossStatus = "全部完成：已收到 $payloadCallbackCount / $EXPECTED_CALLBACK_COUNT 次 payload 回调"
        }
        KLog.i(CROSS_RUNTIME_TAG, line)
    }

    private fun setResultLine(caseName: String, line: String) {
        when (caseName) {
            "plain_string_128k" -> plainString128Line = line
            "plain_string_384k" -> plainString384Line = line
            "plain_string_3m" -> plainString3mLine = line
            "json_string_128k" -> jsonString128Line = line
            "json_string_384k" -> jsonString384Line = line
            "json_string_3m" -> jsonString3mLine = line
            "kr_record_128k" -> krRecord128Line = line
            "kr_record_384k" -> krRecord384Line = line
            "kr_record_3m" -> krRecord3mLine = line
        }
    }

    private fun caseDescription(caseName: String): String {
        val case = CASE_DEFS[caseName] ?: return "ArkTS → Kotlin｜未知 case=$caseName"
        return "ArkTS → Kotlin｜${case.first.label}｜${formatPayloadBytes(case.second.chars.toLong())}"
    }

    private fun formatAverageMs(totalMs: Long, count: Int): String {
        val hundredths = (totalMs * 100 + count.coerceAtLeast(1) / 2) / count.coerceAtLeast(1)
        return "${hundredths / 100}.${(hundredths % 100).toString().padStart(2, '0')}"
    }

    // ---- JSON parsing helpers ----
    private fun appendFieldsForBytes(targetDeltaBytes: Int) {
        val perField = if (bytesPerField > 0) bytesPerField else AVG_BYTES_PER_FIELD_FALLBACK
        val deltaFields = (targetDeltaBytes / perField).toInt().coerceAtLeast(1)
        appendFields(deltaFields)
    }

    private fun appendFields(deltaFields: Int) {
        val start = nextFieldIndex
        val end = start + deltaFields
        for (i in start until end) {
            val key = "$KEY_DYNAMIC_PREFIX$i"
            when (i % 4) {
                0 -> payloadJson.put(key, i.toLong())
                1 -> payloadJson.put(key, "val_$i")
                2 -> payloadJson.put(key, i % 2 == 0)
                else -> payloadJson.put(key, i * 0.5)
            }
        }
        nextFieldIndex = end
        fieldCount = nextFieldIndex
        payloadJson.put(KEY_FIELD_COUNT, fieldCount)
    }

    private fun refreshMetrics() {
        payloadBytes = payloadJson.toString().length
    }

    private fun addBytes(deltaBytes: Int, label: String) {
        val operationStartTs = DateTime.currentTimestamp()
        val before = fieldCount
        val appendStartTs = DateTime.currentTimestamp()
        appendFieldsForBytes(deltaBytes)
        val appendEndTs = DateTime.currentTimestamp()
        val metricsStartTs = DateTime.currentTimestamp()
        refreshMetrics()
        val metricsEndTs = DateTime.currentTimestamp()
        val cppPrepareStartTs = DateTime.currentTimestamp()
        prepareCppPayload(reason = "add_$label")
        val cppPrepareEndTs = DateTime.currentTimestamp()
        val added = fieldCount - before
        jsonStatusText = "已追加 $label（+$added 字段）\n" +
            "当前字段数 = $fieldCount\n" +
            "当前体积   = ${formatJsonBytes(payloadBytes)} ($payloadBytes 字节)\n" +
            "c++ backed 数据已预先准备"
        KLog.i(
            JSON_TAG,
            "addBytes: label=$label, addedFields=$added, " +
                "fieldCount=$fieldCount, payloadBytes=$payloadBytes, " +
                "appendMs=${appendEndTs - appendStartTs}, " +
                "serializeForMetricsMs=${metricsEndTs - metricsStartTs}, " +
                "prepareCppPayloadMs=${cppPrepareEndTs - cppPrepareStartTs}, " +
                "totalMs=${cppPrepareEndTs - operationStartTs}"
        )
    }

    private fun prepareCppPayload(reason: String) {
        val prepareStartTs = DateTime.currentTimestamp()
        val stringifyStartTs = DateTime.currentTimestamp()
        val jsonText = payloadJson.toString()
        val stringifyEndTs = DateTime.currentTimestamp()
        val parseStartTs = DateTime.currentTimestamp()
        cppPayloadJson = JSONObject(jsonText)
        val parseEndTs = DateTime.currentTimestamp()
        KLog.i(
            JSON_TAG,
            "prepareCppPayload: reason=$reason, fieldCount=$fieldCount, " +
                "payloadBytes=${jsonText.length}, stringifyMs=${stringifyEndTs - stringifyStartTs}, " +
                "parseMs=${parseEndTs - parseStartTs}, totalMs=${parseEndTs - prepareStartTs}"
        )
    }

    private fun openJsonPage(cppBacked: Boolean) {
        val backing = if (cppBacked) BACKING_CPP else BACKING_KOTLIN
        val clickTs = DateTime.currentTimestamp()
        KLog.i(
            JSON_TAG,
            "trace: stage=source_click_enter, traceId=$clickTs, " +
                "fieldCount=$fieldCount, backing=$backing"
        )

        val pageData = JSONObject().apply {
            put(KEY_CLICK_TS, clickTs)
            put(KEY_TIME, clickTs)
            put(KEY_BACKING, backing)
            put("payload", if (cppBacked) cppPayloadJson ?: payloadJson else payloadJson)
        }
        KLog.i(
            JSON_TAG,
            "trace: stage=source_payload_ready, traceId=$clickTs, " +
                "fieldCount=$fieldCount, payloadBytes=$payloadBytes, backing=$backing, " +
                "elapsedFromClickMs=${DateTime.currentTimestamp() - clickTs}"
        )
        jsonStatusText = "已发起跳转（$backing backed）\n" +
            "fieldCount = $fieldCount\n" +
            "JSON 体积 = ${formatJsonBytes(payloadBytes)} ($payloadBytes 字节)\n" +
            "clickTs = $clickTs\n" +
            "请在子页面查看耗时"
        val routerOpenStartTs = DateTime.currentTimestamp()
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).openPage(
            "InteropPerfDestPage",
            pageData
        )
        val routerOpenEndTs = DateTime.currentTimestamp()
        KLog.i(
            JSON_TAG,
            "trace: stage=router_openPage_return, traceId=$clickTs, backing=$backing, " +
                "routerCallMs=${routerOpenEndTs - routerOpenStartTs}, " +
                "elapsedFromClickMs=${routerOpenEndTs - clickTs}"
        )
    }

    companion object {
        private const val MEASURE_COUNT = 10000
        private const val MEASURE_FONT_SIZE = 16f
        private const val MEASURE_MAX_WIDTH = 320f
        private const val MEASURE_MAX_HEIGHT = 100000f
        private const val MEASURE_SHADOW_REF = 1_000_000
        private val EXPECTED_CALLBACK_COUNT =
            PAYLOAD_SCALES.sumOf { it.count } * PayloadShape.values().size
    }
}

private fun formatPayloadBytes(bytes: Long): String = when {
    bytes >= BYTES_PER_MB -> "${bytes / BYTES_PER_MB} MB"
    bytes >= BYTES_PER_KB -> "${bytes / BYTES_PER_KB} KB"
    else -> "$bytes B"
}

private fun formatJsonBytes(bytes: Int): String {
    val absBytes = bytes.toLong()
    return when {
        absBytes >= BYTES_PER_MB -> {
            val mb = absBytes.toDouble() / BYTES_PER_MB
            "${((mb * 100).toInt() / 100.0)} MB"
        }
        absBytes >= BYTES_PER_KB -> {
            val kb = absBytes.toDouble() / BYTES_PER_KB
            "${((kb * 100).toInt() / 100.0)} KB"
        }
        else -> "$bytes B"
    }
}

private fun ViewContainer<*, *>.ResultLine(tag: String, textProvider: () -> String) {
    Text {
        attr {
            text(textProvider())
            fontSize(14f)
            marginBottom(8f)
            testTag(tag)
        }
    }
}

private fun ViewContainer<*, *>.ButtonRow(
    title: String,
    bgColor: Color,
    testTagName: String,
    onClick: () -> Unit
) {
    View {
        attr {
            alignItemsCenter()
            marginTop(12f)
        }
        Button {
            attr {
                titleAttr {
                    text(title)
                    color(Color.WHITE)
                    fontSize(15f)
                }
                backgroundColor(bgColor)
                size(width = 300f, height = 48f)
                borderRadius(8f)
                testTag(testTagName)
            }
            event {
                click {
                    onClick()
                }
            }
        }
    }
}

@Page("InteropPerfDestPage")
internal class InteropPerfDestPage : BasePager() {

    private var summaryText: String by observable("正在解析 pageData ...")
    private var pageCreatedTs: Long = 0L
    private var clickTs: Long = 0L

    private fun trace(stage: String, nowTs: Long = DateTime.currentTimestamp(), extra: String = "") {
        val elapsedFromClick = if (clickTs > 0L) nowTs - clickTs else -1L
        val elapsedFromCreated = if (pageCreatedTs > 0L) nowTs - pageCreatedTs else -1L
        KLog.i(
            JSON_TAG,
            "trace: stage=$stage, traceId=$clickTs, " +
                "elapsedFromClickMs=$elapsedFromClick, " +
                "elapsedFromResultCreatedMs=$elapsedFromCreated" +
                if (extra.isEmpty()) "" else ", $extra"
        )
    }

    override fun created() {
        super.created()
        pageCreatedTs = DateTime.currentTimestamp()
        trace("result_created_enter")

        val paramsReadStartTs = DateTime.currentTimestamp()
        val routeParams: JSONObject = pageData.params
        clickTs = routeParams.optLong(KEY_CLICK_TS, 0L)
        val params = routeParams.optJSONObject("payload") ?: JSONObject()
        val timeField = routeParams.optLong(KEY_TIME, 0L)
        val fieldCount = params.optInt(KEY_FIELD_COUNT, 0)
        val backing = routeParams.optString(KEY_BACKING, "")
        val paramsReadEndTs = DateTime.currentTimestamp()
        val cost = if (clickTs > 0L) pageCreatedTs - clickTs else -1L
        trace(
            "result_params_read",
            paramsReadEndTs,
            "paramsReadMs=${paramsReadEndTs - paramsReadStartTs}, " +
                "fieldCount=$fieldCount, backing=$backing, pageCreatedCostMs=$cost"
        )

        val keyCountStartTs = DateTime.currentTimestamp()
        val totalKeys = params.length()
        val keyCountEndTs = DateTime.currentTimestamp()
        trace(
            "result_key_count_done",
            keyCountEndTs,
            "keyCountMs=${keyCountEndTs - keyCountStartTs}, actualKeys=$totalKeys"
        )

        val serializeStartTs = DateTime.currentTimestamp()
        val realBytes = params.toString().length
        val serializeEndTs = DateTime.currentTimestamp()
        trace(
            "result_serialize_done",
            serializeEndTs,
            "serializeMs=${serializeEndTs - serializeStartTs}, realJsonBytes=$realBytes"
        )

        val sampleStartTs = DateTime.currentTimestamp()
        val samples = mutableListOf<String>()
        val keyList = params.keys().asSequence().filter { it.startsWith(KEY_DYNAMIC_PREFIX) }.toList()
        val sampleKeys = if (keyList.size <= 5) keyList else keyList.take(5)
        for (k in sampleKeys) {
            samples.add("$k = ${params.opt(k)}")
        }
        val sampleEndTs = DateTime.currentTimestamp()
        trace(
            "result_samples_done",
            sampleEndTs,
            "sampleMs=${sampleEndTs - sampleStartTs}, dynamicKeyCount=${keyList.size}"
        )

        val summaryBuildStartTs = DateTime.currentTimestamp()
        val sb = StringBuilder()
        sb.appendLine("===== JSON 解析结果 =====")
        sb.appendLine("time (动态字段) = $timeField")
        sb.appendLine("clickTimestampMs = $clickTs")
        sb.appendLine("pageCreatedMs    = $pageCreatedTs")
        sb.appendLine("耗时(click → created) = $cost ms")
        sb.appendLine("jsonBacking = $backing")
        sb.appendLine("声明 fieldCount = $fieldCount")
        sb.appendLine("params 实际 key 总数 = $totalKeys")
        sb.appendLine("真实 JSON 体积 = ${formatJsonBytes(realBytes)} ($realBytes 字节)")
        sb.appendLine("动态字段抽样：")
        if (samples.isEmpty()) {
            sb.appendLine("  <无>")
        } else {
            samples.forEach { sb.appendLine("  $it") }
        }
        val summary = sb.toString()
        val summaryBuildEndTs = DateTime.currentTimestamp()
        trace(
            "result_summary_built",
            summaryBuildEndTs,
            "summaryBuildMs=${summaryBuildEndTs - summaryBuildStartTs}, summaryChars=${summary.length}"
        )

        summaryText = summary
        KLog.i(
            JSON_TAG,
            "parsed: traceId=$clickTs, backing=$backing, fieldCount=$fieldCount, actualKeys=$totalKeys, " +
                "realJsonBytes=$realBytes, time=$timeField, clickTs=$clickTs, createdTs=$pageCreatedTs, costMs=$cost"
        )
    }

    override fun viewWillLoad() {
        super.viewWillLoad()
        trace("result_view_will_load")
    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        trace("result_view_did_load")
    }

    override fun viewDidLayout() {
        super.viewDidLayout()
        trace("result_view_did_layout")
    }

    override fun pageDidAppear() {
        super.pageDidAppear()
        trace("result_page_did_appear_ui_complete")
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                backgroundColor(Color.WHITE)
            }
            NavBar {
                attr {
                    title = "Open Page Destination"
                }
            }
            View {
                attr {
                    backgroundColor(Color(0xFFF5F5F5))
                    borderRadius(12f)
                    padding(16f)
                    margin(16f)
                }
                Text {
                    attr {
                        text(ctx.summaryText)
                        fontSize(13f)
                        color(Color(0xFF222222))
                        testTag("json_perf_result_text")
                    }
                }
            }
        }
    }
}
