// 根工程：声明插件版本。
// Kuikly 版本与 iOS 渲染端 OpenKuiklyIOSRender 对应（KMP 坐标：com.tencent.kuikly-open:2.15.0-2.0.21）。
// 注：@Page 路由表由 core-ksp（KSP）在编译期生成，无需额外的 Kuikly Gradle 插件，
//     因此本工程不引入 com.tencent.kuikly-open.kuikly 插件（与官方 multiModuleDemo 一致）。

plugins {
    id("com.android.application").version("8.1.0").apply(false)
    id("com.android.library").version("8.1.0").apply(false)
    kotlin("android").version("2.0.21").apply(false)
    kotlin("multiplatform").version("2.0.21").apply(false)
    id("com.google.devtools.ksp").version("2.0.21-1.0.28").apply(false)
}
