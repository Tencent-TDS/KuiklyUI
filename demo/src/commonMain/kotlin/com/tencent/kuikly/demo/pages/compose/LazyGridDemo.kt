package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.extension.bouncesEnable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.lazy.grid.GridCells
import com.tencent.kuikly.compose.foundation.lazy.grid.LazyVerticalGrid
import com.tencent.kuikly.compose.foundation.lazy.grid.rememberLazyGridState
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.log.KLog
import kotlinx.coroutines.launch
import kotlin.random.Random

@Page("LazyGridDemo")
class LazyGridDemo : ComposeContainer() {
  override fun willInit() {
    super.willInit()
    setContent {
      ComposeNavigationBar {
        lazyGridExample()
      }
    }
  }

  @Composable
  private fun lazyGridExample() {
    Column(
      modifier = Modifier.fillMaxSize()
    ) {
      fun getData(key: String): List<String> {
        return List(20) {
          key + Random.nextInt(10000).toString().padStart(4, '0')
        }
      }

      val lazyGridState = rememberLazyGridState()
      val filterItemDataList = remember {
        mutableStateListOf<String>().apply {
          addAll(getData("init_"))
        }
      }
      val isAtBottom = remember {
        derivedStateOf {
          (lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
            ?: 0) >= lazyGridState.layoutInfo.totalItemsCount - 1
        }
      }

      LaunchedEffect(isAtBottom.value) {
        KLog.i(
          "[LazyGrid]",
          "isAtBottom= ${isAtBottom.value}, " +
            "lastVisibleIndex = ${lazyGridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index}, " +
            "totalItemsCount = ${lazyGridState.layoutInfo.totalItemsCount}"
        )
        if (isAtBottom.value && filterItemDataList.isNotEmpty()) {
          // 到达底部，加载更多数据
          launch {
            KLog.i("[LazyGrid]", "isAtBottom, requestData")
            filterItemDataList.addAll(getData("AtBottom_"))
          }
        }
      }

      LazyVerticalGrid(
        columns = GridCells.Fixed(5),
        state = lazyGridState,
        contentPadding = PaddingValues(
          start = 12.dp,
          end = 12.dp,
          top = 12.dp,
          bottom = 0.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .bouncesEnable(false)
      ) {
        items(filterItemDataList.size) { index ->
          var backgroundColor by remember { mutableStateOf(Color.Green) }
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(150.dp)
              .padding(horizontal = 20.dp, vertical = 8.dp)
              .background(backgroundColor, RoundedCornerShape(4.dp))
              .clickable {
                backgroundColor = if (backgroundColor == Color.Green) Color.Red else Color.Green
                KLog.i("[LazyGrid]", "click ${filterItemDataList[index]}")
              },
            contentAlignment = Alignment.Center
          ) {
            Text(
              filterItemDataList[index],
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = Color.Black,
            )
          }
        }
      }
    }
  }
}