# 自研 DSL 常见错误

> 以下场景读取本文件：自研 DSL 开发中遇到编译报错、import 找不到、`Color.parseColor` 错误、Text 组件属性报错、调用了不存在的生命周期方法（如 `onDestroyView`）、Module 在 `pageWillDestroy` 中调用失败。

---

### 1. Import 路径错误

```kotlin
// ❌ 使用标准库路径
import kotlinx.coroutines.GlobalScope
import org.json.JSONObject

// ✅ 使用 Kuikly 框架路径
import com.tencent.kuikly.core.coroutines.GlobalScope
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
```

**规则**：优先使用 `com.tencent.kuikly.core.*` 下的类；遇到 import 报错时，在项目其他文件中搜索正确的 import 示例。

---

### 2. Text 组件属性用错

```kotlin
// ❌ Text 组件不支持 View 的布局属性
Text {
    attr {
        alignItemsCenter()     // Text 不支持
        paddingHorizontal(4f)  // Text 不支持 padding
    }
}

// ✅ Text 使用文本专属属性
Text {
    attr {
        textAlignCenter()
        marginLeft(4f)         // 用 margin 代替 padding
    }
}
```

**规则**：容器组件（`View`、`Row`、`Column`）支持 `alignItemsCenter()`、`padding()` 等布局属性；`Text` 使用 `textAlignCenter()`、`margin*()` 等。

---

### 3. 颜色 API 错误

```kotlin
// ❌ Android 特有 API，KMP 不可用
color(Color.parseColor("#333333"))

// ✅ 正确
color(Color(0xFF333333))
color(Color.parseString16ToLong("#333333"))
```

---

### 4. 生命周期方法错误

```kotlin
// ❌ 这些方法不存在
override fun onDestroyView() { }
override fun onResume() { }

// ✅ 正确的生命周期方法（按调用顺序）
// created → viewWillLoad → viewDidLoad → viewDidLayout
// → pageDidAppear → pageDidDisappear → pageWillDestroy → viewDestroyed
override fun pageWillDestroy() { super.pageWillDestroy() }
```

---

### 5. Module 调用时机错误

```kotlin
// ❌ pageWillDestroy 时 Native 侧已销毁
override fun pageWillDestroy() {
    super.pageWillDestroy()
    timerModule.clearAllInterval()  // 可能失败
}

// ✅ 在 pageDidDisappear 或更早时机调用
override fun pageDidDisappear() {
    super.pageDidDisappear()
    timerModule.clearAllInterval()  // 安全
}
```
