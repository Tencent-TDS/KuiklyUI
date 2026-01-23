package com.tencent.kuikly.demo.pages.jank

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.nativeRef
import com.tencent.kuikly.compose.foundation.Image
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.heightIn
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.layout.widthIn
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.LazyRow
import com.tencent.kuikly.compose.foundation.lazy.itemsIndexed
import com.tencent.kuikly.compose.foundation.lazy.rememberLazyListState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.resources.DrawableResource
import com.tencent.kuikly.compose.resources.InternalResourceApi
import com.tencent.kuikly.compose.resources.painterResource
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.layout.ContentScale
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.TextUnit
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.attr.ImageUri

private const val TEXT_PERF_DEBUG_TAG = "[TextPerfDebug]"

/**
 * 用于调试的 Text 组件，会在重组和布局时打印日志
 * @param uniqueId 用于标识组件实例的唯一ID
 * @param text 显示的文本内容
 */
@Composable
fun DebugText(
    uniqueId: String,
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    lineHeight: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    // 用于存储 viewRef
    var viewRefValue by remember { mutableStateOf(-1) }
    
    // 每次重组时打印日志
    SideEffect {
        println("$TEXT_PERF_DEBUG_TAG[Recompose] id=$uniqueId viewRef=$viewRefValue text=$text")
    }
    
    Text(
        text = text,
        modifier = modifier.nativeRef { viewRef ->
            if (viewRefValue != viewRef.nativeRef) {
                viewRefValue = viewRef.nativeRef
                println("$TEXT_PERF_DEBUG_TAG[bindViewRef] id=$uniqueId viewRef=${viewRef.nativeRef}")
            }
        },
        color = color,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = fontWeight,
        maxLines = maxLines,
        overflow = overflow,
        onTextLayout = { layoutResult ->
            println("$TEXT_PERF_DEBUG_TAG[Layout] id=$uniqueId viewRef=$viewRefValue size=(w:${layoutResult.size.width}, h:${layoutResult.size.height})")
        }
    )
}

/**
 * CloudView limited 2025 copyright.
 *
 * @Description: TODO
 * @User: tonysheng
 * @Date: 2025/12/2
 * @Time: 21:13
 * @version: V1.0
 */
@Page("LazyColumnJankDemo")
class LazyColumnJankDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()

        setContent {
            LagPageView()
        }
    }
}
@Immutable // 声明数据类是不可变的，帮助 Compose 编译器优化
data class ListItemData(val type: Int, val id: Int)

val listData = mutableListOf<ListItemData>().apply {
    repeat(200) {
        // add(Pair(if (it < 40) 1 else 2, it))
        add(ListItemData(1, it))
    }
}

@Composable
fun LagPageView() {
    val listState = rememberLazyListState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
    ) {
        itemsIndexed(listData, key = { index, item -> "${item.id}" }, contentType = { index, item -> "RankingRow" }) { cIndex, item ->
            RankingCardView(cIndex)
        }
    }
}

val cardListData = mutableListOf<ListItemData>().apply {
    repeat(16) {
        add(ListItemData(5, it + 1000))
    }
}

@Stable
@Composable
fun RankingCardView(carIndex: Int) {
    // 使用 LazyRow 验证嵌套 Lazy 组件的复用是否生效
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(cardListData, key = { index, item -> "${carIndex}_${item.id}" }, contentType = { _, _ -> "RankingItem" }) { index, itemData ->
            RankingItemView2(carIndex, index)
        }
    }
}

@OptIn(InternalResourceApi::class)
private val coverIcon0 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon0.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon1 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon1.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon2 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon2.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon3 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon3.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon4 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon4.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon5 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon5.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon6 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon6.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon7 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon7.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon8 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon8.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val coverIcon9 by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("cover_icon9.webp").toUrl(""))
}
@OptIn(InternalResourceApi::class)
private val halfStar by lazy(LazyThreadSafetyMode.NONE) {
    DrawableResource(ImageUri.commonAssets("novel_half_star.png").toUrl(""))
}


@Stable
@Composable
fun RankingItemView2(carIndex: Int, itemIndex: Int) {

    // println("${TimeUtils.formatDate(getTimeMillis(), "yyyy-MM-dd HH:mm:ss:SSS")}   RankingItemView  index: $index")
    Row(
        modifier = Modifier.width(300.dp).height(94.dp)
    ) {
        Box(
            modifier = Modifier.padding(start = 6.dp, top = 5.dp).size(60.dp, 84.dp)
        ) {
            val resId = when (carIndex % 9) {
                1 -> coverIcon1
                2 -> coverIcon2
                3 -> coverIcon3
                4 -> coverIcon4
                5 -> coverIcon5
                6 -> coverIcon6
                7 -> coverIcon7
                8 -> coverIcon8
                9 -> coverIcon9
                else -> coverIcon0
            }
            Image(
                painter = painterResource(resId),
                colorFilter = null,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier.padding(start = 2.dp).width(20.dp).heightIn(19.dp), contentAlignment = Alignment.Center

            ) {
                Image(
                    modifier = Modifier.widthIn(16.dp).height(19.dp),
                    painter = painterResource(halfStar),
                    colorFilter = null,
                    contentDescription = "Image"
                )

                DebugText(
                    uniqueId = "${carIndex}_${itemIndex}_rank",
                    modifier = Modifier.fillMaxSize(),
                    text = "${carIndex + itemIndex}",
                    color = Color.Black.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

        }

        // Right Container
        Column(
            modifier = Modifier.align(Alignment.CenterVertically).fillMaxSize().padding(start = 12.dp)
        ) {
            // Title
            DebugText(
                uniqueId = "${carIndex}_${itemIndex}_title",
                modifier = Modifier.padding(end = 6.dp),
                text = "Title_${carIndex}_${itemIndex}",
                color = Color.Black.copy(0.3f),
                fontSize = 13.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 4.dp)
            ) {
                DebugText(
                    uniqueId = "${carIndex}_${itemIndex}_tag",
                    text = "Tag_${carIndex}_${itemIndex}",
                    color = Color.Black.copy(0.3f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Image(
                    painter = painterResource(halfStar),
                    colorFilter = null,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 14.dp),
                    contentScale = ContentScale.Crop
                )

                DebugText(
                    uniqueId = "${carIndex}_${itemIndex}_views",
                    text = "${100 + carIndex + itemIndex} Views",
                    color = Color.Black.copy(0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp, end = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            DebugText(
                uniqueId = "${carIndex}_${itemIndex}_rule",
                text = "Rule_${carIndex}_${itemIndex}",
                color = Color.Black.copy(0.3f),
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(0.3f))
                    .padding(start = 4.dp, top = 2.dp, end = 4.dp, bottom = 2.dp)
            )
        }
    }
}