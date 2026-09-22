# 2.20.1 变更说明

## Part 1 总体说明

2.20.1 补上了图片滤镜与文字选中高亮两处样式能力，把 JS 分包从单模块扩展到多模块，并收敛了键盘、字体等端侧问题。具体如下：

- **图片与文本的样式表达**（Android、鸿蒙）：图片可以用 4×5 颜色矩阵实现灰度、饱和度、亮度与对比度调整，也可以显式清除滤镜（iOS 暂不支持图片颜色矩阵）；选中文字的高亮颜色同样可以自行指定。
- **JS 分包的多模块与页面入口**（H5、小程序、Android）：分包支持多模块，KSP 侧也能按页面名生成对应的页面入口，多模块、多页面场景可以各自独立打包与按需加载。
- **键盘、字体与构建路径问题收敛**（Android、小程序、H5）：Android 键盘模块的并发调用不再引发异常，小程序自定义字体显示与 JS 端构建路径问题一并修复。

历史提交可见 [2.20.0...2.20.1](https://github.com/Tencent-TDS/KuiklyUI/compare/2.20.0...2.20.1)，完整条目见下方 Part 2。

---

## Part 2 变更汇总

### Android

- fix: Android keyboardModule multiThread problem · [#1398](https://github.com/Tencent-TDS/KuiklyUI/pull/1398)

### Compose

- 1.增加输入框文字选中高亮颜色属性 · [#1390](https://github.com/Tencent-TDS/KuiklyUI/pull/1390)
- fix(compose): 修正 JS 端 IdentityHashCode 源文件目录路径 · [#1405](https://github.com/Tencent-TDS/KuiklyUI/pull/1405)
- feat: add image colorMatrix effect implementation · [#1234](https://github.com/Tencent-TDS/KuiklyUI/pull/1234)

### 多端

- Bugfix/miniapp custom font · [#1408](https://github.com/Tencent-TDS/KuiklyUI/pull/1408)

### 其他

- feat(web): js split support multi modules · [#1392](https://github.com/Tencent-TDS/KuiklyUI/pull/1392)
- feat: android ksp support entry get pageName · [#1399](https://github.com/Tencent-TDS/KuiklyUI/pull/1399)

---

## Part 3 破坏性变更

## 条件性行为变化

### JS 分包多模块与 KSP 页面入口生成

- 结果：JS 分包从单模块扩展到多模块，KSP 侧新增按页面名生成入口的能力，`{PageName}Entry.kt` 由 KSP 依据传入的页面名列表生成。
- 影响：仅影响使用 JS 分包 / 多入口打包的业务——`core-gradle-plugin` 与 `core-ksp` 需要切换到支持分包的版本，且 KSP 必须拿到页面名列表才能为每个页面生成独立入口；不升级或不传页面名时，多页面分包入口无法正确生成。不使用分包的业务不受影响。
- 迁移：将 `core-gradle-plugin`、`core-ksp` 升级到支持分包的版本；在业务模块的 `build.gradle.kts` 中增加 `fun getPageNameList(): String = project.properties["pageNameList"] as? String ?: ""`，并在 `ksp { }` 中补充 `arg("pageNameList", getPageNameList())`；构建时通过 `-PpageNameList=xxx` 指定页面名。
