# 发布管理

> 以下场景读取本文件：需要发布新版本、了解版本号规则、本地验证发布产物、排查发布失败。

---

## 1. 正常发布流程

**打 tag 即触发 CI 自动发布**，命名规范：

```
v{KUIKLY_VERSION}-{KOTLIN_VERSION}
例：2.16.0-2.1.21
```

CI 会自动完成：兼容性文件替换 → 多版本编译 → 发布到 Maven 仓库 → 恢复原始文件。

---

## 2. 版本号规则

| 环境变量 | 说明 |
|---------|------|
| `KUIKLY_VERSION` | Kuikly 框架版本（如 `2.16.0`） |
| `KUIKLY_KOTLIN_VERSION` | 目标 Kotlin 版本（如 `2.1.21`） |

**发布产物版本号格式**：`{KUIKLY_VERSION}-{KUIKLY_KOTLIN_VERSION}`
**Maven Group**：`com.tencent.kuikly-open`

项目同时维护多个 Kotlin 版本（1.3.10 ~ 2.1.21），每个版本对应：
- `settings.{VERSION}.gradle.kts`
- `publish/{VERSION}_publish.sh`
- `publish/compatible/{VERSION}.yaml`（版本兼容性替换配置）

---

## 3. 本地发布验证

需要在本地验证发布产物时：

```bash
# 发布到本地 Maven（~/.m2）
java publish/FileReplacer.java replace publish/compatible/2.1.21.yaml
./publish/2.1.21_publish.sh all publishToMavenLocal
java publish/FileReplacer.java restore publish/compatible/2.1.21.yaml

# 验证产物
ls ~/.m2/repository/com/tencent/kuikly-open/core/
```

---

## 4. 常见发布问题

| 现象 | 解决方案 |
|------|---------|
| CI 发布失败，版本号不对 | 确认 tag 命名格式是否正确 |
| 本地发布后有 `.bak` 文件残留 | `java publish/FileReplacer.java restore publish/compatible/2.1.21.yaml` |
| OHOS 构建失败 | 使用 `2.0_ohos_publish.sh`，Kotlin 版本需为 `2.0.21-KBA-010` |
