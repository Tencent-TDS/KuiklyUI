# 列表与滚动

本页说明 Kuikly Compose 中列表与滚动组件的支持情况与使用注意事项。  
基础用法和官方保持一致，请**优先查阅 Jetpack Compose 官方文档**。

> 官方文档（推荐阅读）：[Lists and grids](https://developer.android.com/develop/ui/compose/lists)

## 常用组件

### 列表

- `LazyColumn` / `LazyRow`：懒加载的纵向 / 横向列表

### 网格与瀑布流

- `LazyVerticalGrid` / `LazyHorizontalGrid`：网格
- `LazyVerticalStaggeredGrid` / `LazyHorizontalStaggeredGrid`：瀑布流

### 分页

- `HorizontalPager` / `VerticalPager`：分页容器

**特别说明**：以上组件本身自带滚动能力

## 扩展能力

### 回弹控制：`Modifier.bouncesEnable`

```kotlin
@Composable
fun NoBounceList(data: List<String>) {
    LazyColumn(
        modifier = Modifier.bouncesEnable(false) // 关闭回弹
    ) {
        items(data) { item ->
            Text(text = item)
        }
    }
}
```

### 嵌套滚动策略：`Modifier.nestedScroll`

- 扩展函数：`Modifier.nestedScroll(scrollUp: NestedScrollMode, scrollDown: NestedScrollMode)`  
- 枚举 `NestedScrollMode`：`SELF_ONLY` / `SELF_FIRST` / `PARENT_FIRST`  
- 作用：精细控制子列表与父容器（如外层 Scroller / Pager）在上下滚动时的优先级与联动策略。  
- 示例：
  - 上滑父优先、下拉子优先：

```kotlin
@Composable
fun NestedScrollList(data: List<String>) {
    LazyColumn(
        modifier = Modifier.nestedScroll(
            scrollUp = NestedScrollMode.SELF_FIRST,
            scrollDown = NestedScrollMode.PARENT_FIRST
        )
    ) {
        items(data) { item ->
            Text(text = item)
        }
    }
}
```

### 预加载配置：`beyondViewportPageCount` / `beyondBoundsItemCount`

- **HorizontalPager / VerticalPager**
  - 参数：`beyondViewportPageCount: Int`
  - 含义：在当前可见页面之前 / 之后额外预加载的页面数量，用于减少快速滑动时的白屏和抖动。
  - 建议：一般设置为 1～3，数值过大会增加一次性组合和布局的页数，影响首帧和内存。
- **LazyColumn / LazyRow**
  - 当前不提供显式的 `beyondXXX` 参数，但内部同样会根据滚动方向做预取和「视窗之外」的 item 组合；
  - 你可以通过控制 `item` 渲染开销、`contentPadding`、`item` 高度/宽度等方式，间接影响预加载体验。
- **LazyVerticalStaggeredGrid / LazyHorizontalStaggeredGrid**
  - 参数：`beyondBoundsItemCount: Int`
  - 含义：在当前可见区域之外额外预加载的 item 数量，用于瀑布流滚动时提前测量和布局后续单元格。
  - 建议：根据单个 item 渲染开销和屏幕尺寸来调节，通常 4～10 之间即可。

```kotlin
@Composable
fun PreloadPagerSample() {
    val pagerState = rememberPagerState(pageCount = { 10 })
    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = 2,          // 左右各预加载 2 页
        modifier = Modifier
            .height(200.dp)
            .fillMaxWidth()
    ) { page ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (page % 2 == 0) Color(0xFFE3F2FD) else Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
        ) {
            Text("Page $page")
        }
    }
}

@Composable
fun PreloadStaggeredGridSample(items: List<String>) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        beyondBoundsItemCount = 8,            // 预加载可见区域外的 8 个 item
        modifier = Modifier
            .fillMaxSize()
            .background(Color.LightGray),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(items.size, key = { index -> items[index] }) { index ->
            Text(
                text = items[index],
                modifier = Modifier
                    .padding(4.dp)
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(8.dp)
            )
        }
    }
}
```

### 下拉刷新：`pullToRefreshItem`

```kotlin
@Composable
fun PullToRefreshList(data: List<String>) {
    val lazyListState = rememberLazyListState()
    var isRefreshing by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf(data) }
    val pullToRefreshState = rememberPullToRefreshState(isRefreshing)

    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        // 1. 下拉刷新头部，必须放在列表最前面
        pullToRefreshItem(
            state = pullToRefreshState,
            scrollState = lazyListState,
            onRefresh = {
                // 这里可以触发实际的网络请求
                isRefreshing = true
                items = listOf("刷新后的 Item 1", "刷新后的 Item 2") + data
                isRefreshing = false
            }
        )

        // 2. 正常业务列表内容
        items(items) { item ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF5F5F5))
                    .padding(12.dp)
            ) {
                Text(text = item)
            }
        }
    }
}
```

## 示例：基础纵向列表（LazyColumn）

```kotlin
@Composable
fun SimpleColumnList(
    data: List<String>,
    onItemClick: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(
            items = data,
            key = { _, item -> item }  
        ) { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .background(Color.White)
                    .clickable { onItemClick(item) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF9E9E9E),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "这是一条描述文案，用于展示同一个 item 内多行内容的情况。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF9E9E9E),
                        maxLines = 2
                    )
                }
            }
        }
    }
}
```

## 示例：使用 key 保持滚动位置

在长列表中，推荐为每个 item 提供稳定的 `key`，这样在插入 / 删除 / 重新排序数据时，可以更好地复用已组合的 item，并保持当前滚动位置：

```kotlin
data class Message(val id: Long, val author: String, val content: String)

@Composable
fun MessageList(messages: List<Message>) {
    LazyColumn {
        items(
            items = messages,
            key = { message -> message.id }    // 使用稳定 id 作为 key
        ) { message ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(text = message.author, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(text = message.content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

## 示例：网格与错位网格

```kotlin
@Composable
fun SimpleGrid(items: List<String>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = items.size,
            key = { index -> items[index] }
        ) { index ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(if (index % 2 == 0) Color(0xFFE3F2FD) else Color(0xFFFFF3E0))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = items[index],
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun SimpleStaggeredGrid(items: List<String>) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5)),
        contentPadding = PaddingValues(8.dp),
        verticalItemSpacing = 8.dp,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            count = items.size,
            key = { index -> items[index] }
        ) { index ->
            val height = if (index % 3 == 0) 120.dp else if (index % 3 == 1) 80.dp else 160.dp
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height)
                        .background(Color(0xFFE1F5FE))
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = items[index],
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
```



## 更多代码示例

以下 Demo 展示了列表与滚动能力的典型用法，可在开源仓库中查看完整代码：

- [`LazyColumnDemo3.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyColumnDemo3.kt)：`LazyColumn` 各种状态信息示例  
- [`LazyColumnDemo4.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyColumnDemo4.kt)：`LazyColumn` 滑动 API 示例  
- [`LazyRowDemo1.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyRowDemo1.kt)：`LazyRow` 基本用法示例  
- [`LazyRowDemo2.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyRowDemo2.kt)：`LazyRow` 的 key / type 参数示例  
- [`LazyRowDemo3.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyRowDemo3.kt)：`LazyRow` 各种状态信息示例  
- [`LazyHorizontalGridDemo1.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyHorizontalGridDemo1.kt)：`LazyHorizontalGrid` 基本用法示例  
- [`LazyHorizontalGridDemo2.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyHorizontalGridDemo2.kt)：`LazyHorizontalGrid` 的 key、span 跨行参数示例  
- [`LazyHorizontalGridDemo3.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyHorizontalGridDemo3.kt)：`LazyHorizontalGrid` 各种状态信息示例  
- [`StaggeredHorizontalGridDemo1.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/StaggeredHorizontalGridDemo1.kt)：错位网格布局基础用法示例  
- [`StaggeredHorizontalGridDemo3.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/StaggeredHorizontalGridDemo3.kt)：错位网格布局各种状态信息示例  
- [`HorizontalPagerDemo1.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/HorizontalPagerDemo1.kt)：`HorizontalPager` 基本用法示例  
- [`HorizontalPagerDemo3.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/HorizontalPagerDemo3.kt)：`HorizontalPager` 各种状态信息示例  
- [`LazyColumnNestDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyColumnNestDemo.kt)：`LazyColumn` 嵌套滚动场景示例  
- [`LazyRowInHorizontalPager.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyRowInHorizontalPager.kt)：`LazyRow` 嵌套在 `HorizontalPager` 中的滚动示例  
- [`LazyColumnStickyHeader.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/LazyColumnStickyHeader.kt)：带粘性头部的 `LazyColumn` 示例  
- [`PullToRefreshDemo.kt`](https://github.com/Tencent-TDS/KuiklyUI/blob/main/demo/src/commonMain/kotlin/com/tencent/kuikly/demo/pages/compose/PullToRefreshDemo.kt)：`LazyColumn` 下拉刷新示例  

## 注意事项

- 性能：尽量使用 `items` / `itemsIndexed`，避免在 item 中持久创建大对象；长列表建议使用稳定 key。
- 预加载：`beyondViewportPageCount` / `beyondBoundsItemCount` 不宜设置过大，一般控制在小范围内（例如 1～3 页、4～10 个 item），否则会明显增加首帧时间和内存占用。
- 嵌套滚动：`LazyColumn` / `LazyRow` 与 `Pager`、外层滚动容器嵌套时，优先使用 `Modifier.nestedScroll`、`Modifier.bouncesEnable` 等官方/Kuikly 提供的能力，不建议自行拦截手势事件。
- 状态管理：业务状态尽量 hoist 到列表外（ViewModel / 上层 Composable），避免在 `items` 内部直接 `remember { mutableStateOf(...) }` 保存关键状态，以免 item 复用、插入/删除时出现错乱。


