package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Modal
import com.tencent.kuikly.core.views.Scroller
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.willDismiss
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseDialogOhos
import com.tencent.kuikly.core.views.ohos.immersive.ImmersiveLightSenseDialogOhosAttr
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

/**
* 鸿蒙「沉浸光感」弹窗 —— 全面能力测试页。
*
* 用独立组件 [ImmersiveLightSenseDialogOhos]（不是给通用 Modal 加 Ohos 属性）：
* 组件内部固定使用系统背板样式并默认启用材质，业务无需也不应该关掉它。
*
* 页面按能力分组列出测试项，点击即打开对应配置的弹窗。测试清单见
* `devDocs/ohos-immersive-light-sense.md` 的「弹窗测试清单」章节。
*/
@Page("12011")
internal class ImmersiveLightSenseDialogDemoPage : BasePager() {

   /** -1 表示不显示；>=0 表示当前展示的测试项序号 */
   private var caseIndex by observable(-1)

   /**
    * 对照组的伪序号：走框架自绘弹窗（[Modal]），不经过原生弹窗背板与沉浸光感。
    * 单列出来是为了让 native 弹窗的 case 区间始终连续（0 until SELF_DRAWN_CASE）。
    */
   private val SELF_DRAWN_CASE = 12

   /**
    * 内容层自绘边框的伪序号。
    * 背景：启用沉浸光感后，背板描边归材质管，`border` 设多宽都不会画出来（官方未开放材质描边配置）。
    * 所以边框这条能力只能落到**内容层**——内容节点是 Kuikly 自绘的，边框照常生效。
    */
   private val OUTLINE_CASE = 13

   override fun body(): ViewBuilder {
       val ctx = this
       return {
           Image {
               attr {
                   absolutePosition(0f, 0f, 0f, 0f)
                   src("https://sqimg.qq.com/qq_product_operations/kan/images/viola/viola_bg.jpg")
               }
           }

           NavBar {
               attr {
                   title = "沉浸光感 Dialog"
               }
           }

           Scroller {
               attr {
                   flex(1f)
                   margin(15f)
               }

               Text {
                   attr {
                       margin(10f)
                       text("设备支持沉浸光感: ${PlatformUtils.isImmersiveMaterialSupported()}")
                       fontSize(13f)
                       color(Color(0xFFEEEEEE))
                   }
               }

               ctx.section("一、基础形态") {
                   ctx.caseButton(0, "默认弹窗（默认 REGULAR 档）")()
                   ctx.caseButton(1, "厚档材质 ULTRA_THICK")()
                   ctx.caseButton(2, "极薄档材质 ULTRA_THIN")()
               }()

               ctx.section("二、材质参数") {
                   ctx.caseButton(3, "材质赋色（半透明红）")()
                   ctx.caseButton(4, "关闭材质自带阴影 applyShadow=false")()
                   ctx.caseButton(14, "THIN(1) 档（档位全套 0~4 之一）")()
                   ctx.caseButton(15, "THICK(3) 档（档位全套 0~4 之一）")()
                   ctx.caseButton(16, "点光源 lightEffectColor（触点光效）")()
                   ctx.caseButton(17, "深色赋色 materialColor 0x80（不透明度对照 3 号 case）")()
                   ctx.caseButton(18, "关闭材质 enable=false（降级形态对照）")()
               }()

               ctx.section("三、通用属性继承") {
                   ctx.caseButton(5, "大圆角 borderRadius=40")()
                   ctx.caseButton(6, "小尺寸 220x160")()
                   ctx.caseButton(7, "背板背景色（半透明红）")()
                   ctx.caseButton(8, "边框 border 8vp（背板 / 预期无效果）")()
                   ctx.caseButton(ctx.OUTLINE_CASE, "内容层自绘边框 8vp（替代方案）")()
                   ctx.caseButton(9, "不透明度 opacity=0.6")()
               }()

               ctx.section("四、事件与交互") {
                   ctx.caseButton(10, "内容按钮点击关闭")()
                   ctx.caseButton(11, "系统返回键关闭（willDismiss）")()
               }()

               ctx.section("五、对照：框架自绘弹窗（无系统背板）") {
                   ctx.caseButton(ctx.SELF_DRAWN_CASE, "自绘弹窗 + 2vp 白边")()
               }()
           }

           // 对照组：框架自绘弹窗 —— 背板由 Kuikly 节点自己画（背景/圆角/边框都走通用属性），
           // 不涉及系统背板与沉浸光感。用于和上面原生弹窗的边框做视觉对照：
           // 自绘的这一版，边框一定会严格贴着圆角矩形。
           vif({ ctx.caseIndex == ctx.SELF_DRAWN_CASE }) {
               Modal {
                   View {
                       attr {
                           absolutePosition(0f, 0f, 0f, 0f)
                           backgroundColor(Color(0x66000000))
                           allCenter()
                       }
                       View {
                           attr {
                               width(300f)
                               height(220f)
                               borderRadius(24f)
                               backgroundColor(Color(0xB3FFFFFF))
                               border(
                                   border = Border(
                                       color = Color(0x99FFFFFF),
                                       lineWidth = 2f,
                                       lineStyle = BorderStyle.SOLID
                                   )
                               )
                           }
                           ctx.dialogContent(
                               ctx.caseTitle(ctx.SELF_DRAWN_CASE),
                               ctx.caseNote(ctx.SELF_DRAWN_CASE)
                           ) {
                               KLog.i("ImmLightTest", "self-drawn dialog close")
                               ctx.caseIndex = -1
                           }()
                       }
                   }
               }
           }

           vif({ (ctx.caseIndex in 0 until ctx.SELF_DRAWN_CASE) || ctx.caseIndex == ctx.OUTLINE_CASE }) {
               ImmersiveLightSenseDialogOhos {
                   attr {
                       ctx.applyCase(this, ctx.caseIndex)
                   }
                   // willDismiss 是弹窗组件级事件（ModalEvent），必须挂在组件上而不是内容按钮上
                   event {
                       willDismiss { reason ->
                           KLog.i("ImmLightTest", "dialog willDismiss, reason=$reason, caseIndex=${ctx.caseIndex} -> -1")
                           ctx.caseIndex = -1
                       }
                   }
                   ctx.dialogContent(
                       ctx.caseTitle(ctx.caseIndex),
                       ctx.caseNote(ctx.caseIndex),
                       outline = ctx.caseIndex == ctx.OUTLINE_CASE
                   ) {
                       KLog.i("ImmLightTest", "dialog close by inner button, caseIndex=${ctx.caseIndex} -> -1")
                       ctx.caseIndex = -1
                   }()
               }
           }
       }
   }

   /** 每个测试项的弹窗配置 */
   private fun applyCase(attr: ImmersiveLightSenseDialogOhosAttr, index: Int) {
       with(attr) {
           width(300f)
           height(220f)
           borderRadius(24f)
           when (index) {
               1 -> immersiveMaterial(style = 4, interactive = true)
               2 -> immersiveMaterial(style = 0, interactive = true)
               3 -> immersiveMaterial(style = 2, materialColor = Color(0x4DFF3B30))
               4 -> immersiveMaterial(style = 2, applyShadow = false)
               5 -> borderRadius(40f)
               6 -> {
                   width(220f)
                   height(160f)
               }
               7 -> backgroundColor(Color(0x66FF3B30))
               8 -> border(
                   border = Border(
                       // 保留 8vp 是为了让"完全无效果"这件事一眼可辨：
                       // 材质接管背板描边，这里设多宽都不会画出来（官方未开放材质描边配置）。
                       color = Color(0x99FFFFFF),
                       lineWidth = 8f,
                       lineStyle = BorderStyle.SOLID
                   )
               )
               9 -> opacity(0.6f)
               // 14~18：材质参数矩阵（档位全套 + 点光源 + 赋色透明度 + 降级）
               14 -> immersiveMaterial(style = 1)
               15 -> immersiveMaterial(style = 3)
               16 -> immersiveMaterial(style = 2, lightEffectColor = Color(0x80FFFFFF))
               17 -> immersiveMaterial(style = 2, materialColor = Color(0x80FF3B30))
               18 -> immersiveMaterial(enable = false)
               else -> { /* 10 / 11 用默认配置，验证事件 */ }
           }
       }
   }

   private fun caseTitle(index: Int): String = when (index) {
       0 -> "默认弹窗"
       1 -> "厚档材质"
       2 -> "极薄档材质"
       3 -> "材质赋色"
       4 -> "关闭材质阴影"
       5 -> "大圆角"
       6 -> "小尺寸"
       7 -> "背板背景色"
       8 -> "背板边框（无效）"
       9 -> "不透明度"
       10 -> "内容按钮关闭"
       11 -> "系统返回键关闭"
       14 -> "THIN 档"
       15 -> "THICK 档"
       16 -> "点光源"
       17 -> "深色赋色"
       18 -> "关闭材质"
       SELF_DRAWN_CASE -> "自绘弹窗边框"
       OUTLINE_CASE -> "内容层边框"
       else -> "未知"
   }

   private fun caseNote(index: Int): String = when (index) {
       0 -> "未设置材质参数，默认 REGULAR 档"
       1 -> "ULTRA_THICK(4)，应明显更糊"
       2 -> "ULTRA_THIN(0)，应最透"
       3 -> "materialColor 半透明红，材质层带红调"
       4 -> "applyShadow=false，材质阴影应消失"
       5 -> "borderRadius=40，背板四角圆角应变大"
       6 -> "220x160，背板应随之变小"
       7 -> "backgroundColor 半透明红，背板应透红"
       8 -> "背板 border 由材质接管，8vp 也画不出来"
       9 -> "opacity=0.6，整体应变淡"
       10 -> "点「关闭」按钮应关闭弹窗"
       11 -> "按系统返回键应触发 willDismiss 并关闭"
       14 -> "THIN(1)，与 0/2/4 号组成档位全套"
       15 -> "THICK(3)，与 0/2/4 号组成档位全套"
       16 -> "lightEffectColor 触点光效，按压背板应有光反馈"
       17 -> "materialColor 0x80 红调更浓，与 3 号(0x4D)对比透明度影响"
       18 -> "enable=false 降级：无材质的普通系统弹窗"
       SELF_DRAWN_CASE -> "Kuikly 自绘，边框应严格贴圆角"
       OUTLINE_CASE -> "内容层 8vp 白边，应紧跟背板一圈"
       else -> ""
   }

   private fun section(title: String, content: ViewBuilder): ViewBuilder {
       return {
           Text {
               attr {
                   margin(top = 16f, left = 10f, right = 10f, bottom = 6f)
                   text(title)
                   fontSize(14f)
                   fontWeightSemiBold()
                   color(Color(0xCCFFFFFF))
               }
           }
           content.invoke(this)
       }
   }

   private fun caseButton(index: Int, label: String): ViewBuilder {
       val ctx = this
       return {
           View {
               attr {
                   margin(left = 10f, right = 10f, top = 6f, bottom = 6f)
                   padding(12f)
                   borderRadius(12f)
                   backgroundColor(Color(0x66000000))
                   allCenter()
               }
               event {
                   click {
                       KLog.i("ImmLightTest", "open dialog case=$index")
                       ctx.caseIndex = index
                   }
               }
               Text {
                   attr {
                       text("$index. $label")
                       fontSize(15f)
                       color(Color.WHITE)
                   }
               }
           }
       }
   }

   /**
    * 弹窗内容。
    *
    * 尺寸用 `flex(1f) + alignSelfStretch()` 撑满父容器，**不要写死绝对值**，也不要指望百分比：
    *   - 弹窗内容节点被 NativeDialog 接管后，在 ArkUI 侧布局尺寸恒为 0；
    *   - Kuikly 侧则正常持有弹窗节点的尺寸（来自组件 attr 的 width/height），
    *     所以「按 Kuikly 的 flex 规则撑满」是可靠的，而「依赖 ArkUI 父尺寸」的做法会失效；
    *   - Kuikly 的 width/height 只接受绝对值，没有百分比（百分比仅支持 top/left/right/bottom）。
    *
    * 文字用深色 —— 系统材质背板偏浅，白字对比度不足（CAPI 无 colorInvert，无法自动反色）。
    */
   private fun dialogContent(
       title: String,
       note: String,
       outline: Boolean = false,
       onClose: () -> Unit
   ): ViewBuilder {
       return {
           View {
               attr {
                   flex(1f)
                   alignSelfStretch()
                   padding(24f)
                   flexDirectionColumn()
                   alignItemsCenter()
                   justifyContentCenter()
                   backgroundColor(Color.TRANSPARENT)
                   if (outline) {
                       // 内容层自绘边框：内容根节点用 flex 撑满弹窗，它的矩形和背板重合，
                       // 所以边框能严格贴住背板；半径取 24 与背板圆角对齐。
                       // 这是沉浸光感弹窗下"要边框"的推荐做法（背板 border 由材质接管，设了无效）。
                       borderRadius(24f)
                       border(
                           border = Border(
                               color = Color(0xCCFFFFFF),
                               lineWidth = 8f,
                               lineStyle = BorderStyle.SOLID
                           )
                       )
                   }
               }

               Text {
                   attr {
                       text(title)
                       fontSize(17f)
                       fontWeightSemiBold()
                       color(Color(0xFF1A1A1A))
                   }
               }

               Text {
                   attr {
                       margin(top = 10f)
                       text(note)
                       fontSize(13f)
                       color(Color(0x99202020))
                   }
               }

               View {
                   attr {
                       margin(top = 24f)
                       width(140f)
                       height(42f)
                       borderRadius(21f)
                       allCenter()
                       backgroundColor(Color(0x1A000000))
                   }
                   event {
                       click {
                           onClose()
                       }
                   }
                   Text {
                       attr {
                           text("关闭")
                           fontSize(15f)
                           color(Color(0xFF1A1A1A))
                       }
                   }
               }
           }
       }
   }
}
