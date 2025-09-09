package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Animation
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.InterfaceStyle
import com.tencent.kuikly.core.base.Size
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewRef
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.GlassEffectContainer
import com.tencent.kuikly.core.views.GlassEffectStyle
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.InputView
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.ios.SegmentedControlIOS
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Slider
import com.tencent.kuikly.core.views.Switch
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.core.views.ios.iOSSlider
import com.tencent.kuikly.core.views.ios.iOSSwitch
import com.tencent.kuikly.demo.pages.app.theme.ThemeManager
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.base.TabbarIOS
import com.tencent.kuikly.demo.pages.demo.base.TabbarIOSItem
import kotlin.random.Random

@Page("LiquidGlassDemoPage")
internal class LiquidGlassDemoPage : BasePager() {

    private var shouldUseGlassEffect by observable(true)
    private var shouldAnimate by observable(false)
    private var randomTintColor by observable(0xFF90EE90.toString())
    private lateinit var inputRef: ViewRef<InputView>

    private var switch1State by observable("On")
    private var switch2State by observable("On")
    private var slider1Value by observable(0.5f)
    private var slider2Value by observable(0.5f)

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            ctx.testBackground()()
            NavBar {
                attr {
                    title = "iOS 26 Liquid Glass Demo"
                }
            }

            Scroller {
                attr {
                    flex(1f)
                    margin(15f)
                }

                // 基础效果演示分组
                ctx.createDemoSection("基础效果演示") {
                    View {
                        attr {
                            size(300f, 60f)
                            borderRadius(30f)
                            margin(10f)
                            allCenter()
                            alignSelfCenter()

                            if (PlatformUtils.isLiquidGlassSupported() && ctx.shouldUseGlassEffect) {
                                glassEffectIOS() // iOS平台将自动添加液态玻璃效果
                                backgroundColor(Color.TRANSPARENT)
                            } else {
                                backgroundColor(Color.GRAY)
                                glassEffectIOS(enable = false)
                            }
                        }
                        event {
                            click {
                                ctx.shouldUseGlassEffect = !ctx.shouldUseGlassEffect
                            }
                        }
                        Text {
                            attr {
                                if (ctx.shouldUseGlassEffect) {
                                    text("Regular Style")
                                } else {
                                    text("普通背景效果")
                                }
                                fontSize(21f)
                                alignSelfCenter()
                                fontWeight600()
                            }
                        }
                    }
                    // Clear效果演示分组
                    View {
                        attr {
                            size(300f, 60f)
                            borderRadius(30f)
                            margin(10f)
                            allCenter()
                            alignSelfCenter()
                            glassEffectIOS(style = GlassEffectStyle.CLEAR)
                        }
                        Text {
                            attr {
                                text("Clear Style")
                                fontSize(21f)
                                alignSelfCenter()
                                fontWeight600()
                            }
                        }
                    }
                    Button {
                        attr {
                            flexDirectionRow()
                            size(300f, 60f)
                            margin(10f)
                            borderRadius(15f)
                            allCenter()
                            alignSelfCenter()
                            titleAttr {
                                text("Kuikly Liquid Glass Button")
                                fontSize(21f)
                                fontWeight600()
                            }
                            // 使用Liquid Glass时，不能同时设置背景色
                            if (PlatformUtils.isLiquidGlassSupported()) {
                                glassEffectIOS(interactive = true, tintColor = Color(ctx.randomTintColor))
                            } else {
                                backgroundColor(Color(ctx.randomTintColor))
                            }
                        }
                        event {
                            click {
                                // 点击后切换随机颜色
                                ctx.randomTintColor = ctx.getRandomColor().toString()
                            }
                        }
                    }
                }()

                // 组合效果演示分组
                ctx.createDemoSection("组合效果演示") {
                    View {
                        attr {
                            flexDirectionRow()
                            margin(10f)
                            glassEffectContainerIOS(10f)
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                flex(2f)
                                justifyContentCenter()
                                glassEffectIOS(interactive = false)
                            }
                            Text {
                                attr {
                                    margin(10f)
                                    text("融合效果：")
                                    fontSize(19f)
                                    alignSelfCenter()
                                    fontWeight500()
                                }
                            }
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                flex(1f)
                                glassEffectIOS(tintColor = Color.GREEN)
                            }
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                flex(1f)
                                glassEffectIOS()
                            }
                        }
                    }
                }()

                // 交互组件演示分组
                ctx.createDemoSection("交互组件演示") {
                    View {
                        attr {
                            flexDirectionRow()
                            margin(10f)
                            glassEffectContainerIOS(10f)
                        }
                        View {
                            attr {
                                height(60f)
                                borderRadius(30f)
                                marginRight(10f)
                                flex(1f)
                                glassEffectIOS()
                            }
                            Input {
                                ref {
                                    ctx.inputRef = it
                                }
                                attr {
                                    marginLeft(30f)
                                    flex(1f)
                                    placeholder("Type to Search")
                                    fontSize(16f)
                                }
                            }
                        }
                        View {
                            attr {
                                size(60f, 60f)
                                borderRadius(30f)
                                justifyContentCenter()
                                glassEffectIOS()
                            }
                            event {
                                click {
                                    ctx.blurInput()
                                }
                            }
                            Text {
                                attr {
                                    text("×")
                                    fontSize(28f)
                                    alignSelfCenter()
                                    fontWeight400()
                                }
                            }
                        }
                    }
                    
                    // Switch 演示
                    View {
                        attr {
                            margin(10f)
                            padding(15f)
                            borderRadius(15f)
                            backgroundColor(Color(0xFFF8F9FA))
                            border(border = Border(
                                color = Color(0xFFE9ECEF),
                                lineWidth = 1f,
                                lineStyle = BorderStyle.SOLID)
                            )
                        }
                        Text {
                            attr {
                                text("Switch 液态玻璃效果:")
                                fontSize(18f)
                                fontWeight500()
                                color(Color(0xFF495057))
                                marginBottom(15f)
                            }
                        }
                        
                        // 使用统一的Switch组件，自动切换iOS原生实现
                        View {
                            attr {
                                flexDirectionRow()
                                alignItemsCenter()
                                justifyContentSpaceBetween()
                                marginBottom(10f)
                            }
                            Text {
                                attr {
                                    text("启用玻璃效果")
                                    fontSize(16f)
                                }
                            }
                            Switch {
                                attr {
                                    isOn(true)
                                    enableGlassEffect(true) // iOS 26.0+将自动使用原生Switch
                                    thumbColor(Color.WHITE)
                                    onColor(Color(0xFF34C759))
                                    unOnColor(Color(0xFF8E8E93))
                                }
                                event {
                                    switchOnChanged { isOn ->
                                        ctx.switch1State = if (isOn) "On" else "Off"
                                    }
                                }
                            }
                            Text {
                                attr {
                                    text(ctx.switch1State)
                                    fontSize(16f)
                                    marginLeft(10f)
                                }
                            }
                        }
                        
                        View {
                            attr {
                                flexDirectionRow()
                                alignItemsCenter()
                                justifyContentSpaceBetween()
                            }
                            Text {
                                attr {
                                    text("禁用玻璃效果")
                                    fontSize(16f)
                                }
                            }
                            Switch {
                                attr {
                                    isOn(true)
                                    enableGlassEffect(false) // 使用自定义实现
                                    thumbColor(Color.WHITE)
                                    onColor(Color(0xFF34C759))
                                    unOnColor(Color(0xFF8E8E93))
                                }
                                event {
                                    switchOnChanged { isOn ->
                                        ctx.switch2State = if (isOn) "On" else "Off"
                                    }
                                }
                            }
                            Text {
                                attr {
                                    text(ctx.switch2State)
                                    fontSize(16f)
                                    marginLeft(10f)
                                }
                            }
                        }
                    }
                    
                    // Slider 演示
                    View {
                        attr {
                            margin(10f)
                            padding(15f)
                            borderRadius(15f)
                            backgroundColor(Color(0xFFF8F9FA))
                            border(border = Border(
                                color = Color(0xFFE9ECEF),
                                lineWidth = 1f,
                                lineStyle = BorderStyle.SOLID)
                            )
                        }
                        Text {
                            attr {
                                text("Slider 液态玻璃效果:")
                                fontSize(18f)
                                fontWeight500()
                                color(Color(0xFF495057))
                                marginBottom(15f)
                            }
                        }
                        
                        // 启用玻璃效果的 Slider
                        View {
                            attr {
                                flexDirectionRow()
                                alignItemsCenter()
                                justifyContentSpaceBetween()
                                marginBottom(10f)
                            }
                            Text {
                                attr {
                                    text("启用：")
                                    fontSize(16f)
                                    width(50f)
                                }
                            }
                            Slider {
                                attr {
                                    currentProgress(0.5f)
                                    enableGlassEffect(true)
                                    progressColor(Color(0xFF34C759))
                                    trackColor(Color(0xFF8E8E93))
                                    thumbColor(Color.WHITE)
                                    thumbSize(Size(40f, 30f))
                                    size(150f, 30f)
                                }
                                event {
                                    progressDidChanged {
                                        ctx.slider1Value = it
                                    }
                                }
                            }
                            Text {
                                attr {
                                    text("${ctx.slider1Value}")
                                    fontSize(16f)
                                    marginLeft(10f)
                                    lines(1)
                                    width(50f)
                                }
                            }
                        }
                        
                        // 禁用玻璃效果的 Slider
                        View {
                            attr {
                                flexDirectionRow()
                                alignItemsCenter()
                                justifyContentSpaceBetween()
                            }
                            Text {
                                attr {
                                    text("禁用：")
                                    fontSize(16f)
                                    width(50f)
                                }
                            }
                            Slider {
                                attr {
                                    currentProgress(0.5f)
                                    enableGlassEffect(false)
                                    progressColor(Color(0xFF34C759))
                                    trackColor(Color(0xFF8E8E93))
                                    thumbColor(Color.WHITE)
                                    thumbSize(Size(40f, 30f))
                                    size(150f, 30f)
                                }
                                event {
                                    progressDidChanged {
                                        ctx.slider2Value = it
                                    }
                                }
                            }
                            Text {
                                attr {
                                    text("${ctx.slider2Value}")
                                    fontSize(16f)
                                    marginLeft(10f)
                                    lines(1)
                                    width(50f)
                                }
                            }
                        }
                    }
                }()

                // 动画效果演示分组
                ctx.createDemoSection("动画效果演示") {
                    View {
                        attr {
                            justifyContentCenter()
                            margin(10f)
                            borderRadius(30f)
                            alignSelfCenter()
                            if (ctx.shouldAnimate) {
                                size(200f, 200f)
                                animate(Animation.easeInOut(3f), ctx.shouldAnimate)
                            } else {
                                size(80f, 80f)
                                animate(Animation.easeInOut(3f), ctx.shouldAnimate)
                            }
                            glassEffectIOS()
                        }
                        event {
                            click {
                                ctx.shouldAnimate = !ctx.shouldAnimate
                            }
                        }
                        Text {
                            attr {
                                if (ctx.shouldAnimate) {
                                    text("点击收起")
                                } else {
                                    text("点击展开")
                                }
                                fontSize(18f)
                                alignSelfCenter()
                                fontWeight500()
                            }
                        }
                    }
                }()

                vif({ PlatformUtils.isIOS() }) {
                    // 组件方式写法演示分组 - 内置
                    ctx.createDemoSection("独立组件方式使用示例（内置组件）") {
                        ctx.liquidGlassComponentDemo()()
                        ctx.iOSSwitchAndSliderDemo()()
                        ctx.iOSSystemSegmentedControlDemo()()
                    }()
                    
                    // 组件方式写法演示分组 - 自定义
                    ctx.createDemoSection("独立组件方式使用示例（自定义组件）") {
                        ctx.iOSSystemTabbarDemo()()
                    }()
                }
            }
        }
    }

    private fun blurInput() {
        inputRef.view?.blur()
    }

    private fun createDemoSection(title: String, content: ViewBuilder): ViewBuilder {
        return {
            View {
                attr {
                    margin(15f, 10f, 15f, 10f)
                    padding(20f)
                    borderRadius(20f)
                    border(border = Border(
                        color = Color(0xFFE5E5E5),
                        lineWidth = 1f,
                        lineStyle = BorderStyle.SOLID)
                    )
                }
                
                // 分组标题
                Text {
                    attr {
                        text(title)
                        fontSize(24f)
                        fontWeight600()
                        color(Color(0xFF333333))
                        backgroundColor(Color(0xFFF8F9FA))
                        marginBottom(15f)
                        alignSelfFlexStart()

                    }
                }
                
                // 分隔线
                View {
                    attr {
                        height(2f)
                        backgroundColor(Color(0xFFE8E8E8))
                        marginBottom(20f)
                        borderRadius(1f)
                    }
                }
                
                // 内容区域
                content()
            }
        }
    }

    private fun iOSSystemTabbarDemo(): ViewBuilder {
        val pageName = getPager().pageName
        return {
            View {
                attr {
                    margin(10f)
                    padding(15f)
                    borderRadius(15f)
                    backgroundColor(Color(0xFFF8F9FA))
                }

                Text {
                    attr {
                        text("系统风格 Tabbar 组件")
                        fontSize(18f)
                        fontWeight500()
                        color(Color(0xFF495057))
                        marginBottom(15f)
                        alignSelfCenter()
                    }
                }
                
                TabbarIOS {
                    attr {
                        height(80f)
                        items(
                            listOf(
                                TabbarIOSItem(
                                    "首页",
                                    ThemeManager.getAssetUri("default", "tabbar_home.png").toUrl(pageName),
                                    ThemeManager.getAssetUri("default", "tabbar_home_highlighted.png").toUrl(pageName),
                                ),
                                TabbarIOSItem(
                                    "我的",
                                    ThemeManager.getAssetUri("default", "tabbar_profile.png").toUrl(pageName),
                                    ThemeManager.getAssetUri("default", "tabbar_profile_highlighted.png").toUrl(pageName),
                                )
                            )
                        )
                        selectedIndex(0)
                    }
                    event {
                        onTabSelected {
                            // 处理 tab 切换
                        }
                    }
                }
            }
        }
    }

    private fun iOSSystemSegmentedControlDemo(): ViewBuilder {
        return {
            View {
                attr {
                    margin(10f)
                    padding(15f)
                    borderRadius(15f)
                    backgroundColor(Color(0xFFF8F9FA))
                }

                Text {
                    attr {
                        text("系统风格 SegmentedControl 组件")
                        fontSize(18f)
                        fontWeight500()
                        color(Color(0xFF495057))
                        marginBottom(15f)
                        alignSelfCenter()
                    }
                }
                
                SegmentedControlIOS {
                    attr {
                        height(40f)
                        width(200f)
                        alignSelfCenter()
                        titles(listOf("选项1", "选项2", "选项3"))
                        selectedIndex(0)
                    }
                    event {
                        onValueChanged {
                            // 处理选中变化
                        }
                    }
                }
            }
        }
    }

    private fun iOSSwitchAndSliderDemo(): ViewBuilder {
        return {
            View {
                attr {
                    margin(10f)
                }
                
                // Switch 演示
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        marginBottom(20f)
                        padding(15f)
                        borderRadius(15f)
                        backgroundColor(Color(0xFFF8F9FA))
                        border(border = Border(
                            color = Color(0xFFE9ECEF),
                            lineWidth = 1f,
                            lineStyle = BorderStyle.SOLID)
                        )
                    }
                    Text {
                        attr {
                            text("iOS Switch:")
                            fontSize(18f)
                            fontWeight500()
                            color(Color(0xFF495057))
                            flex(1f)
                        }
                    }
                    iOSSwitch {
                        attr {
                            size(100f, 30f)
                            isOn(true)
                        }
                    }
                }
                
                // Slider 演示
                View {
                    attr {
                        padding(15f)
                        borderRadius(15f)
                        backgroundColor(Color(0xFFF8F9FA))
                        border(border = Border(
                            color = Color(0xFFE9ECEF),
                            lineWidth = 1f,
                            lineStyle = BorderStyle.SOLID)
                        )
                    }
                    Text {
                        attr {
                            text("iOS Slider:")
                            fontSize(18f)
                            fontWeight500()
                            color(Color(0xFF495057))
                            marginBottom(15f)
                        }
                    }
                    iOSSlider {
                        attr {
                            size(200f, 30f)
                            currentProgress(0.5f)
                            alignSelfCenter()
                        }
                    }
                    iOSSlider {
                        attr {
                            size(100f, 100f)
                            marginTop(10f)
                            currentProgress(0.5f)
                            alignSelfCenter()
                            sliderDirection(false)
                            trackThickness(6f)
                        }
                    }
                }
            }
        }
    }

    private fun liquidGlassComponentDemo(): ViewBuilder {
        return {
            View {
                attr {
                    margin(10f)
                }

                // Regular style
                LiquidGlass {
                    attr {
                        height(60f)
                        justifyContentCenter()
                        marginBottom(15f)
                        borderRadius(30f)
                        glassEffectInteractive(false)
                    }
                    Text {
                        attr {
                            text("Regular Style")
                            fontSize(20f)
                            alignSelfCenter()
                            fontWeight600()
                        }
                    }
                }
                
                // Clear style
                LiquidGlass {
                    attr {
                        height(60f)
                        justifyContentCenter()
                        marginBottom(15f)
                        borderRadius(30f)
                        glassEffectInteractive(false)
                        glassEffectStyle(GlassEffectStyle.CLEAR)
                    }
                    Text {
                        attr {
                            text("Clear Style")
                            fontSize(20f)
                            alignSelfCenter()
                            fontWeight600()
                        }
                    }
                }
                
                // 组合效果容器
                GlassEffectContainer {
                    attr {
                        spacing(15f)
                        flexDirectionRow()
                        interfaceStyle(InterfaceStyle.LIGHT)
                    }
                    LiquidGlass {
                        attr {
                            height(60f)
                            borderRadius(30f)
                            flex(1f)
                            justifyContentCenter()
                            glassEffectInteractive(true)
                        }
                        Text {
                            attr {
                                text("默认")
                                fontSize(16f)
                                alignSelfCenter()
                                fontWeight500()
                            }
                        }
                    }
                    LiquidGlass {
                        attr {
                            height(60f)
                            borderRadius(30f)
                            flex(1f)
                            justifyContentCenter()
                            glassEffectInteractive(true)
                            glassEffectTintColor(Color.YELLOW)
                        }
                        Text {
                            attr {
                                text("黄色")
                                fontSize(16f)
                                alignSelfCenter()
                                fontWeight500()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun testBackground(): ViewBuilder {
        return {
            Scroller {
                attr {
                    absolutePosition(0f, 0f, 0f, 0f)
                }
                View {
                    View {
                        attr {
                            height(100f)
                        }
                    }
                    Image {
                        attr {
                            resizeStretch()
                            size(pagerData.pageViewWidth, 360f)
                            src(ImageUri.commonAssets("penguin2.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeCover()
                            size(pagerData.pageViewWidth, 800f)
                            src(ImageUri.commonAssets("views.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeCover()
                            size(pagerData.pageViewWidth, 800f)
                            src(ImageUri.commonAssets("cat1.png"))
                        }
                    }
                    Image {
                        attr {
                            resizeCover()
                            size(pagerData.pageViewWidth, 800f)
                            src(ImageUri.commonAssets("cat2.png"))
                        }
                    }
                }
            }
        }
    }

    private fun getRandomColor(): Color {
        val red = Random.nextInt(0, 256)
        val green = Random.nextInt(0, 256)
        val blue = Random.nextInt(0, 256)
        return Color(red, green, blue, 1f)
    }

}