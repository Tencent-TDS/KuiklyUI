# 常见问题 FAQ

本文档收集 Kuikly Compose 开发中的常见问题和解决方案。

---

## 1. 点击事件失效 / 触摸手势获取不到最新状态

**关键词：** 点击失效、触摸事件失效、手势不生效、状态不更新、列表项点击错乱、LazyColumn 复用、pointerInput、detectTapGestures、detectDragGestures

### 问题描述

在 `pointerInput` 修饰符的闭包中读取外部状态时，即使状态已经变化，闭包内读取到的仍然是旧值，导致点击/触摸事件看起来"失效"或行为异常。

**典型症状：**
- ✋ 点击事件无反应或响应错误
- 🖱️ 拖拽/滑动手势获取到的状态是旧值
- 🔄 修改了开关状态，但手势回调中仍是旧状态
- ❓ 看起来点击"失效"了，但其实是状态未更新

**典型场景一：** 状态变化后点击行为异常
```kotlin
@Composable
fun BrokenExample() {
    var count by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color.Blue)
            .pointerInput(Unit) {  // ❌ key 为 Unit
                detectTapGestures {
                    println("点击时 count = $count")  // 永远打印 0
                }
            }
    ) {
        Text("点击我，count = $count")
    }
    
    Button(onClick = { count++ }) {
        Text("增加 count (当前 = $count)")
    }
}
```

**现象**：按钮让 `count` 变成 5，但点击蓝色方块仍打印 `count = 0`，看起来像是点击事件"失效"了。

**典型场景二：** LazyColumn 列表项复用后点击响应错乱
```kotlin
@Composable
fun BrokenListExample(items: List<ItemData>) {
    LazyColumn {
        items(items, key = { it.id }) { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(Unit) {  // ❌ key 为 Unit，复用时闭包不更新
                        detectTapGestures {
                            println("点击了: ${item.name}")  // 可能打印旧 item 的 name
                            navigateTo(item.id)              // 可能跳转到错误的页面
                        }
                    }
            ) {
                Text(item.name)
            }
        }
    }
}
```

**现象**：列表滚动后，点击某一项却跳转到了其他项的详情页，或者打印出错误的 item 信息。这是因为 LazyColumn 复用了列表项的 Composable，而 `pointerInput(Unit)` 的闭包仍捕获着旧 item 的值。

### 原因分析

`pointerInput` 的**闭包会捕获创建时的变量值**（类似 JavaScript 的闭包陷阱）：

1. `pointerInput(key)` 的 `key` 参数是**重启键（restart key）**
2. 当 `key` 值不变时，闭包不会重建，始终使用初始捕获的变量
3. 只有 `key` 变化时，才会取消旧协程，用新值重建闭包

```kotlin
// 等价的伪代码理解
pointerInput(key) { block }
// ↓ 相当于
LaunchedEffect(key) {  // key 变化时重启
    awaitPointerEventScope {
        block()  // 闭包捕获此时的外部变量
    }
}
```

### 解决方案

**将闭包内读取的状态作为 key 参数：**

```kotlin
@Composable
fun CorrectExample() {
    var count by remember { mutableStateOf(0) }
    
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(Color.Blue)
            .pointerInput(count) {  // ✅ key 为 count
                detectTapGestures {
                    println("点击时 count = $count")  // 正确打印最新值
                }
            }
    ) {
        Text("点击我，count = $count")
    }
    
    Button(onClick = { count++ }) {
        Text("增加 count")
    }
}
```

**规则：闭包内读取的所有可变状态都必须作为 key**

```kotlin
var isEnabled by remember { mutableStateOf(true) }
var threshold by remember { mutableStateOf(100f) }

Box(
    modifier = Modifier.pointerInput(isEnabled, threshold) {  // ✅ 多个依赖
        detectDragGestures { change, dragAmount ->
            if (isEnabled && dragAmount.x > threshold) {  // 读取外部状态
                // 处理逻辑
            }
        }
    }
)
```

### 常见陷阱对照表

| 场景 | 错误写法 | 问题 | 正确写法 |
|------|---------|------|----------|
| 读取布尔开关 | `pointerInput(Unit) { if (enabled) {...} }` | `enabled` 永远是初始值 | `pointerInput(enabled) { if (enabled) {...} }` |
| 读取配置参数 | `pointerInput(Unit) { handle(config) }` | `config` 变化不生效 | `pointerInput(config) { handle(config) }` |
| 调用外部函数 | `pointerInput(Unit) { onClick() }` | `onClick` 是旧引用 | `pointerInput(onClick) { onClick() }` |
| Lambda 回调中读取 | `pointerInput(Unit) { tap { use(state) } }` | Lambda 捕获旧 state | `pointerInput(state) { tap { use(state) } }` |
| LazyColumn 列表项 | `pointerInput(Unit) { tap { use(item) } }` | 复用后 item 是旧值 | `pointerInput(item) { tap { use(item) } }` |
| 只修改偏移量 | `pointerInput(offset) { offset += x }` | 过度重启 | `pointerInput(Unit) { offset += x }` ✅ |

---

## 2. 【Android】关闭 BottomSheet 后页面 TextField 自动获取焦点并弹出键盘

**关键词：** 输入框、键盘自动弹出、TextField、BottomSheet、焦点、软键盘、Android

**适用平台：** Android

### 问题描述

当页面包含 TextField 且弹出包含 TextField 的 BottomSheet 时，在 BottomSheet 完成交互（隐藏键盘）并关闭后，**页面上的 TextField 会自动获取焦点并弹出键盘**，不符合预期。

> **注意：** 此问题为 Android 平台特有，与 Android 系统的焦点管理机制有关。

**典型场景：**
```kotlin
// 页面包含 TextField
Column {
    TextField(value = text, onValueChange = { text = it })
    Button(onClick = { showBottomSheet = true }) {
        Text("打开 BottomSheet")
    }
}

// BottomSheet 中也包含 TextField
if (showBottomSheet) {
    ModalBottomSheet(
        onDismissRequest = { showBottomSheet = false }
    ) {
        TextField(value = sheetText, onValueChange = { sheetText = it })
    }
}
```

**现象**：关闭 BottomSheet 后，页面底部的 TextField 自动获取焦点，软键盘弹出。

### 原因分析

当 BottomSheet 关闭时，焦点会从 BottomSheet 中的 TextField 移除。由于页面上的 TextField 是可聚焦的，系统会自动将焦点传递给页面上的 TextField，从而触发软键盘弹出。

### 解决方案

在 Activity 的根布局 XML 文件中，添加 `focusable` 和 `focusableInTouchMode` 属性：

```xml
<FrameLayout
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:focusable="true"
    android:focusableInTouchMode="true"
    android:background="@android:color/white">
    
    <!-- Kuikly 渲染内容 -->

</FrameLayout>
```

**关键属性说明：**
- `android:focusable="true"`：使根布局可以获取焦点
- `android:focusableInTouchMode="true"`：使根布局在触摸模式下可以获取焦点

通过让根布局具备获取焦点的能力，当 BottomSheet 关闭时，焦点会传递给根布局而不是页面上的 TextField，从而避免键盘自动弹出。

**参考 Issue：** [#957](https://github.com/Tencent-TDS/KuiklyUI/issues/957)
