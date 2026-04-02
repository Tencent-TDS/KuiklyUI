# Compose DSL 常见错误

> 以下场景读取本文件：Compose DSL 开发中遇到 `Unresolved reference`、import 报错、使用了 `androidx.compose.*` 包名编译失败、组件找不到。

---

### 1. 包名错误（最高频）

KuiklyUI 对 Compose 做了深度改造，除 `runtime` 外所有包名均已替换：

```kotlin
// ❌ 使用官方 androidx 包（编译报错）
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.animation.AnimatedVisibility

// ✅ 使用 Kuikly fork 包
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.animation.AnimatedVisibility
import androidx.compose.runtime.*  // runtime 唯一使用官方包 ✅
```

**完整包名对照**：见 [.ai/compose-dsl/AGENTS.md 第 2 节](../compose-dsl/AGENTS.md)
