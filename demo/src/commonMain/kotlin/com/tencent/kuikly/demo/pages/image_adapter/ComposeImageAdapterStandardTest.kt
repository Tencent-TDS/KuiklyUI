package com.tencent.kuikly.demo.pages.image_adapter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.coil3.rememberAsyncImagePainter
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.material3.CenterAlignedTopAppBar
import com.tencent.kuikly.compose.material3.ExperimentalMaterial3Api
import com.tencent.kuikly.compose.material3.MaterialTheme
import com.tencent.kuikly.compose.material3.Scaffold
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TopAppBarDefaults
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.imageResource
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Size
import com.tencent.kuikly.compose.ui.geometry.isSpecified
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.unit.DpSize
import com.tencent.kuikly.compose.ui.unit.IntSize
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.module.RouterModule

@Page("cmp_image_adapter")
internal class ComposeImageAdapterStandardTest : ComposeContainer() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun willInit() {
        super.willInit()
        val ctx = this
        setContent {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("ImageAdapter基准测试") },
                        actions = {
                            Text(
                                text = "关闭",
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.clickable(onClick = ctx::finish).padding(10.dp)
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors().copy(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            ) { padding ->
                LazyColumn(Modifier.fillMaxSize().padding(padding).padding(10.dp)) {
                    item {
                        Content()
                    }
                }
            }
        }
    }

    private fun finish() {
        acquireModule<RouterModule>(RouterModule.MODULE_NAME).closePage()
    }
}

private const val BASE64_SRC =
    "iVBORw0KGgoAAAANSUhEUgAAAIQAAACEAQMAAABrihHkAAAABlBMVEUAAAD///+l2Z/dAAAAL0lEQVRIx2MAgf9Q8AHEGRUZFSFOBM6DyfCDREdFRkVGRUZFhpjIYCtXR0WGlAgAIh9YHRjOdfwAAAAASUVORK5CYII="
private val STANDARD_SIZE = Size(132f, 132f)

@Composable
private fun Check(text: String, pass: Boolean? = null) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("- $text", style = MaterialTheme.typography.bodyMedium)
        if (pass == true) {
            Text("✓", color = Color.Green, style = MaterialTheme.typography.titleSmall)
        } else if (pass == false) {
            Text("✗", color = Color.Red, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun Content() {
    val dpSize = with(LocalDensity.current) {
        DpSize(
            STANDARD_SIZE.width.toDp(),
            STANDARD_SIZE.height.toDp()
        )
    }
    val boxSize = DpSize(100.dp, 80.dp)

    // 1. base64
    Text("1. base64", style = MaterialTheme.typography.titleMedium)
    var result1 by remember { mutableStateOf<Boolean?>(null) }
    var base64IntrinsicSize by remember { mutableStateOf(Size.Unspecified) }
    val base64Painter = rememberAsyncImagePainter(
        "data:image/png;base64,$BASE64_SRC",
        onSuccess = {
            base64IntrinsicSize = it.painter.intrinsicSize
            result1 = base64IntrinsicSize == STANDARD_SIZE
        },
        onError = { result1 = false }
    )
    base64Painter.prefetch()
    Row {
        Box(Modifier.size(boxSize)) {
            Image(
                painter = base64Painter,
                contentDescription = null,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.None
            )
            Box(Modifier.size(dpSize).background(Color.Blue.copy(alpha = 0.5f)))
        }
        Canvas(Modifier.size(100.dp, 80.dp)) {
            if (base64IntrinsicSize.isSpecified) {
                with(base64Painter) { draw(base64IntrinsicSize) }
            }
            drawRect(Color.Red.copy(alpha = 0.5f), size = STANDARD_SIZE)
        }
    }
    Check("intrinsicSize测试", result1)
    Check("Image测试, 查看蓝色方块是否与图片重合")
    Check("drawImage测试, 查看红色方块是否与图片重合")

    // 2. assets
    Spacer(Modifier.height(15.dp))
    Text("2. assets", style = MaterialTheme.typography.titleMedium)
    var result2 by remember { mutableStateOf<Boolean?>(null) }
    var result22 by remember { mutableStateOf<Boolean?>(null) }
    var assetsIntrinsicSize by remember { mutableStateOf(Size.Unspecified) }
    var assetsIntrinsicSize2 by remember { mutableStateOf(IntSize.Zero) }
    val assetsPainter = painterResource(sample)
    LaunchedEffect(assetsPainter.intrinsicSize) {
        if (assetsPainter.intrinsicSize != assetsIntrinsicSize) {
            assetsIntrinsicSize = assetsPainter.intrinsicSize
            result2 = assetsIntrinsicSize == STANDARD_SIZE
        }
    }
    val assetsImage = imageResource(sample)
    LaunchedEffect(assetsImage.width, assetsImage.height) {
        if (assetsImage.width != assetsIntrinsicSize2.width || assetsImage.height != assetsIntrinsicSize2.height) {
            assetsIntrinsicSize2 = IntSize(assetsImage.width, assetsImage.height)
            result22 = assetsImage.width == STANDARD_SIZE.width.toInt() && assetsImage.height == STANDARD_SIZE.height.toInt()
        }
    }
    Row {
        Box(Modifier.size(boxSize)) {
            Image(
                painter = assetsPainter,
                contentDescription = null,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.None
            )
            Box(Modifier.size(dpSize).background(Color.Blue.copy(alpha = 0.5f)))
        }
        Canvas(Modifier.size(100.dp, 80.dp)) {
            drawImage(assetsImage)
            drawRect(Color.Red.copy(alpha = 0.5f), size = STANDARD_SIZE)
        }
    }
    Check("intrinsicSize测试", result2)
    Check("imageResource size测试", result22)
    Check("Image测试, 查看蓝色方块是否与图片重合")
    Check("drawImage测试, 查看红色方块是否与图片重合")

    // 3 http/https
    Spacer(Modifier.height(15.dp))
    Text("3. http/https", style = MaterialTheme.typography.titleMedium)
    var result3 by remember { mutableStateOf<Boolean?>(null) }
    var httpIntrinsicSize by remember { mutableStateOf(Size.Unspecified) }
    val httpPainter = rememberAsyncImagePainter(
        "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/21e7b9c2.png",
        onSuccess = {
            httpIntrinsicSize = it.painter.intrinsicSize
            result3 = httpIntrinsicSize == STANDARD_SIZE
        },
        onError = { result3 = false }
    )
    httpPainter.prefetch()
    Row {
        Box(Modifier.size(boxSize)) {
            Image(
                painter = httpPainter,
                contentDescription = null,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.None
            )
            Box(Modifier.size(dpSize).background(Color.Blue.copy(alpha = 0.5f)))
        }
        Canvas(Modifier.size(100.dp, 80.dp)) {
            if (httpIntrinsicSize.isSpecified) {
                with(httpPainter) { draw(httpIntrinsicSize) }
            }
            drawRect(Color.Red.copy(alpha = 0.5f), size = STANDARD_SIZE)
        }
    }
    Check("intrinsicSize测试", result3)
    Check("Image测试, 查看蓝色方块是否与图片重合")
    Check("drawImage测试, 查看红色方块是否与图片重合")

    // 4 gif
    Spacer(Modifier.height(15.dp))
    Text("4. gif", style = MaterialTheme.typography.titleMedium)
    var result4 by remember { mutableStateOf<Boolean?>(null) }
    var gifIntrinsicSize by remember { mutableStateOf(Size.Unspecified) }
    val gifPainter = rememberAsyncImagePainter(
        "https://vfiles.gtimg.cn/wuji_dashboard/wupload/xy/starter/2963d536.gif",
        onSuccess = {
            gifIntrinsicSize = it.painter.intrinsicSize
            result4 = gifIntrinsicSize == STANDARD_SIZE
        },
        onError = { result4 = false }
    )
    gifPainter.prefetch()
    Row {
        Box(Modifier.size(boxSize)) {
            Image(
                painter = gifPainter,
                contentDescription = null,
                alignment = Alignment.TopStart,
                contentScale = ContentScale.None
            )
            Box(Modifier.size(dpSize).background(Color.Blue.copy(alpha = 0.5f)))
        }
        Canvas(Modifier.size(100.dp, 80.dp)) {
            if (gifIntrinsicSize.isSpecified) {
                with(gifPainter) { draw(gifIntrinsicSize) }
            }
            drawRect(Color.Red.copy(alpha = 0.5f), size = STANDARD_SIZE)
        }
    }
    Check("intrinsicSize测试", result4)
    Check("Image测试, 查看蓝色方块是否与图片重合")
    Check("drawImage测试, 查看红色方块是否与图片重合")
}

@OptIn(InternalResourceApi::class)
private val sample: DrawableResource by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("sample.png").toUrl(""))
}