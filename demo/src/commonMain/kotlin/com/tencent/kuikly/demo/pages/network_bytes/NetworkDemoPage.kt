package com.tencent.kuikly.demo.pages.network_bytes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.module.NetworkModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.demo.pages.compose.DemoScaffold
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Page("network")
internal class NetworkDemoPage : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            DemoScaffold("网络请求示例", back = true) {
                NetworkDemo()
            }
        }
    }

    @Composable
    private fun NetworkDemo() {
        var result by remember { mutableStateOf("") }
        var src by remember { mutableStateOf("") }
        val painter = rememberAsyncImagePainter(src)
        val scope = rememberCoroutineScope()
        Column {
            Row {
                Button(
                    onClick = {
                        result = "requestGet..."
                        src = ""
                        scope.launch {
                            try {
                                val bytes = requestGet()
                                result = bytes.toString()
                            } catch (e: Exception) {
                                result = "${e.message}"
                            }
                        }
                    }
                ) {
                    Text("requestGet")
                }
                Button(
                    onClick = {
                        result = "requestGetBinary..."
                        src = ""
                        scope.launch {
                            try {
                                val bytes = requestGetBinary()
                                result = ""
                                src = "data:image/png;base64,${bytes.encodeBase64()}"
                            } catch (e: Exception) {
                                result = "${e.message}"
                            }
                        }
                    }
                ) {
                    Text("requestGetBinary")
                }
            }
            Row {
                Button(
                    onClick = {
                        result = "requestPost..."
                        src = ""
                        scope.launch {
                            try {
                                val bytes = requestPost()
                                result = bytes.toString()
                            } catch (e: Exception) {
                                result = "${e.message}"
                            }
                        }
                    }
                ) {
                    Text("requestPost")
                }
                Button(
                    onClick = {
                        result = "requestPostBinary..."
                        src = ""
                        scope.launch {
                            try {
                                val bytes = requestPostBinary()
                                result = bytes.decodeToString()
                            } catch (e: Exception) {
                                result = "${e.message}"
                            }
                        }
                    }
                ) {
                    Text("requestPostBinary")
                }
            }
            Text(result)
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier.size(100.dp, 100.dp)
            )
        }
    }

    private suspend fun requestGet(): JSONObject = suspendCoroutine { continuation ->
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestGet(
            "https://httpbin.org/get",
            JSONObject().apply { put("key", "value") }
        ) { data, success, errorMsg, _ ->
            if (success) {
                continuation.resume(data)
            } else {
                continuation.resumeWithException(IllegalStateException("请求失败: $errorMsg"))
            }
        }
    }

    private suspend fun requestGetBinary(): ByteArray = suspendCoroutine { continuation ->
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestGetBinary(
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/componenthub/Dfnp7Q9F.png",
            JSONObject()
        ) { data, success, errorMsg, _ ->
            if (success) {
                continuation.resume(data)
            } else {
                continuation.resumeWithException(IllegalStateException("请求失败: $errorMsg"))
            }
        }
    }

    private suspend fun requestPost(): JSONObject = suspendCoroutine { continuation ->
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestPost(
            "https://httpbin.org/post",
            JSONObject().apply { put("key", "value") }
        ) { data, success, errorMsg, _ ->
            if (success) {
                continuation.resume(data)
            } else {
                continuation.resumeWithException(IllegalStateException("请求失败: $errorMsg"))
            }
        }
    }

    private suspend fun requestPostBinary(): ByteArray = suspendCoroutine { continuation ->
        acquireModule<NetworkModule>(NetworkModule.MODULE_NAME).requestPostBinary(
            "https://httpbin.org/post",
            "hello world".encodeToByteArray(),
        ) { data, success, errorMsg, _ ->
            if (success) {
                continuation.resume(data)
            } else {
                continuation.resumeWithException(IllegalStateException("请求失败: $errorMsg"))
            }
        }
    }

    private fun ByteArray.encodeBase64(): String {
        val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val result = StringBuilder((size + 2) / 3 * 4)
        var i = 0
        while (i < size) {
            val b0 = this[i].toInt() and 0xFF
            val b1 = if (i + 1 < size) this[i + 1].toInt() and 0xFF else 0
            val b2 = if (i + 2 < size) this[i + 2].toInt() and 0xFF else 0
            result.append(table[b0 shr 2])
            result.append(table[((b0 and 0x03) shl 4) or (b1 shr 4)])
            if (i + 1 < size) {
                result.append(table[((b1 and 0x0F) shl 2) or (b2 shr 6)])
            } else {
                result.append('=')
            }
            if (i + 2 < size) {
                result.append(table[b2 and 0x3F])
            } else {
                result.append('=')
            }
            i += 3
        }
        return result.toString()
    }
}
