/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2025 Tencent. All rights reserved.
 * Licensed under the License of KuiklyUI;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * https://github.com/Tencent-TDS/KuiklyUI/blob/main/LICENSE
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tencent.kuikly.demo.pages.demo.wx

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.wx.views.WXButton
import com.tencent.kuikly.core.wx.views.WXButtonOpenType
import com.tencent.kuikly.core.wx.views.WXCamera
import com.tencent.kuikly.core.wx.views.WXCameraDevicePosition
import com.tencent.kuikly.core.wx.views.WXCameraFlash
import com.tencent.kuikly.core.wx.views.WXCameraResolution
import com.tencent.kuikly.core.wx.views.WXInput
import com.tencent.kuikly.core.wx.views.WXInputConfirmType
import com.tencent.kuikly.core.wx.views.WXInputType
import com.tencent.kuikly.core.wx.views.WXMap
import com.tencent.kuikly.core.wx.views.WXPicker
import com.tencent.kuikly.core.wx.views.WXPickerMode
import com.tencent.kuikly.core.wx.views.WXTextArea
import com.tencent.kuikly.core.wx.views.WXTextAreaConfirmType
import com.tencent.kuikly.core.module.RouterModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.wx.views.WXVideo
import com.tencent.kuikly.core.wx.views.WXVideoObjectFit
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar
import com.tencent.kuikly.demo.pages.demo.kit_demo.DeclarativeDemo.Common.ViewExampleSectionHeader

@Page("WXExamplePage")
internal class WXExamplePage : BasePager() {

    private var phoneNumberTip by observable("尚未获取")
    private var userInfoTip by observable("尚未获取")
    private var inputTextTip by observable("")
    private var inputNumberTip by observable("")
    private var inputConfirmTip by observable("尚未提交")
    private var textareaContent by observable("")
    private var textareaLineCount by observable(1)
    private val pickerOptions = listOf("北京", "上海", "广州", "深圳", "成都")
    private var pickerSelectedIndex by observable(0)
    private var pickerDate by observable("2026-04-21")
    private var pickerTime by observable("09:30")
    private var videoPlayStatus by observable("尚未播放")
    private var videoCurrentTime by observable("0.00")
    private var cameraStatus by observable("未初始化")
    private var cameraErrorTip by observable("")
    private var scanCodeTip by observable("尚未扫码")
    private var cameraDevicePosition by observable(WXCameraDevicePosition.BACK)
    private var cameraFlashMode by observable(WXCameraFlash.AUTO)
    private var mapCenterTip by observable("北京天安门")
    private var mapMarkerTip by observable("尚未点击 marker")
    private var mapRegionChangeTip by observable("视图无变化")

    private fun openWebViewDemo(url: String) {
        val params = JSONObject().apply { put("url", url) }
        acquireModule<RouterModule>(RouterModule.MODULE_NAME)
            .openPage("WXWebViewDemoPage", params)
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr { backgroundColor(Color.WHITE) }
            NavBar { attr { title = "WX Component Demo" } }
            List {
                attr { flex(1f) }

                ViewExampleSectionHeader {
                    attr { title = "WXButton 基础样式" }
                }
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentSpaceAround()
                        padding(top = 12f, bottom = 12f)
                    }
                    WXButton {
                        attr {
                            type("default")
                            size("mini")
                            titleAttr {
                                text("默认")
                                fontSize(14f)
                            }
                        }
                    }
                    WXButton {
                        attr {
                            type("primary")
                            size("mini")
                            titleAttr {
                                text("主要")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                    }
                    WXButton {
                        attr {
                            type("warn")
                            size("mini")
                            titleAttr {
                                text("警告")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXButton 状态：plain / disabled / loading" }
                }
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentSpaceAround()
                        padding(top = 12f, bottom = 12f)
                    }
                    WXButton {
                        attr {
                            type("primary")
                            plain(true)
                            size("mini")
                            titleAttr {
                                text("Plain")
                                fontSize(14f)
                            }
                        }
                    }
                    WXButton {
                        attr {
                            type("primary")
                            disabled(true)
                            size("mini")
                            titleAttr {
                                text("Disabled")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                    }
                    WXButton {
                        attr {
                            type("primary")
                            loading(true)
                            size("mini")
                            titleAttr {
                                text("Loading")
                                fontSize(14f)
                                color(Color.WHITE)
                            }
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXButton open-type：获取手机号" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        alignItemsCenter()
                        padding(top = 12f, bottom = 12f)
                    }
                    WXButton {
                        attr {
                            type("primary")
                            openType(WXButtonOpenType.GET_PHONE_NUMBER)
                            width(150f)
                            height(40f)
                            titleAttr {
                                text("获取手机号")
                                fontSize(15f)
                                color(Color.WHITE)
                            }
                        }
                        event {
                            onGetPhoneNumber { detail ->
                                KLog.i("WXExamplePage", "onGetPhoneNumber: $detail")
                                ctx.phoneNumberTip = detail.toString()
                            }
                            onError { err ->
                                KLog.e("WXExamplePage", "getPhoneNumber error: $err")
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("授权结果：${ctx.phoneNumberTip}")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXButton open-type：获取用户信息" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        alignItemsCenter()
                        padding(top = 12f, bottom = 12f)
                    }
                    WXButton {
                        attr {
                            type("default")
                            openType(WXButtonOpenType.GET_USER_INFO)
                            titleAttr {
                                text("获取用户信息")
                                fontSize(15f)
                            }
                        }
                        event {
                            onGetUserInfo { detail ->
                                KLog.i("WXExamplePage", "onGetUserInfo: $detail")
                                ctx.userInfoTip = detail.toString()
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("授权结果：${ctx.userInfoTip}")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXButton form-type：联系客服 / 打开设置" }
                }
                View {
                    attr {
                        flexDirectionRow()
                        alignItemsCenter()
                        justifyContentSpaceAround()
                        padding(top = 12f, bottom = 12f)
                    }
                    WXButton {
                        attr {
                            openType(WXButtonOpenType.CONTACT)
                            size("mini")
                            titleAttr {
                                text("联系客服")
                                fontSize(14f)
                            }
                        }
                        event {
                            onContact { detail ->
                                KLog.i("WXExamplePage", "onContact: $detail")
                            }
                        }
                    }
                    WXButton {
                        attr {
                            openType(WXButtonOpenType.OPEN_SETTING)
                            size("mini")
                            titleAttr {
                                text("打开设置")
                                fontSize(14f)
                            }
                        }
                        event {
                            onOpenSetting { detail ->
                                KLog.i("WXExamplePage", "onOpenSetting: $detail")
                            }
                        }
                    }
                    WXButton {
                        attr {
                            openType(WXButtonOpenType.FEEDBACK)
                            size("mini")
                            titleAttr {
                                text("意见反馈")
                                fontSize(14f)
                            }
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXInput 基础用法：实时输入 / 回车提交" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXInput {
                        attr {
                            height(40f)
                            backgroundColor(0xFFF5F5F5)
                            borderRadius(6f)
                            padding(left = 12f, right = 12f)
                            type(WXInputType.TEXT)
                            placeholder("请输入文本，回车提交")
                            confirmType(WXInputConfirmType.DONE)
                            maxLength(50)
                        }
                        event {
                            onInput { detail ->
                                KLog.i("WXExamplePage", "onInput: $detail")
                                ctx.inputTextTip = detail.optString("data")
                            }
                            onConfirm { detail ->
                                KLog.i("WXExamplePage", "onConfirm: $detail")
                                ctx.inputConfirmTip = detail.optString("data")
                            }
                            onFocus { KLog.i("WXExamplePage", "input onFocus") }
                            onBlur { KLog.i("WXExamplePage", "input onBlur") }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("当前输入：${ctx.inputTextTip}")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("最近一次提交：${ctx.inputConfirmTip}")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXInput 数字键盘 / 密码 / 禁用" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXInput {
                        attr {
                            height(40f)
                            backgroundColor(0xFFF5F5F5)
                            borderRadius(6f)
                            padding(left = 12f, right = 12f)
                            type(WXInputType.NUMBER)
                            placeholder("仅可输入数字")
                        }
                        event {
                            onInput { detail ->
                                ctx.inputNumberTip = detail.optString("data")
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("数字输入：${ctx.inputNumberTip}")
                        }
                    }
                    WXInput {
                        attr {
                            marginTop(12f)
                            height(40f)
                            backgroundColor(0xFFF5F5F5)
                            borderRadius(6f)
                            padding(left = 12f, right = 12f)
                            type(WXInputType.TEXT)
                            password(true)
                            placeholder("密码输入")
                        }
                    }
                    WXInput {
                        attr {
                            marginTop(12f)
                            height(40f)
                            backgroundColor(0xFFEEEEEE)
                            borderRadius(6f)
                            padding(left = 12f, right = 12f)
                            disabled(true)
                            value("禁用状态，不可编辑")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXTextArea 多行输入：自动换行 / 换行回调" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXTextArea {
                        attr {
                            minHeight(100f)
                            backgroundColor(0xFFF5F5F5)
                            borderRadius(6f)
                            padding(left = 12f, right = 12f, top = 8f, bottom = 8f)
                            placeholder("请输入多行文本。支持自动换行")
                            autoHeight(true)
                            maxLength(200)
                            confirmType(WXTextAreaConfirmType.DONE)
                            showConfirmBar(true)
                        }
                        event {
                            onInput { detail ->
                                KLog.i("WXExamplePage", "textarea onInput: $detail")
                                ctx.textareaContent = detail.optString("data")
                            }
                            onLineChange { detail ->
                                KLog.i("WXExamplePage", "textarea onLineChange: $detail")
                                // 待解析 JSON 可获取 lineCount，此处简单计算显示
                                ctx.textareaLineCount = ctx.textareaContent.count { it == '\n' } + 1
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("当前内容长度：${ctx.textareaContent.length}，行数估算：${ctx.textareaLineCount}")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXPicker 选择器：selector" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXPicker {
                        attr {
                            mode(WXPickerMode.SELECTOR)
                            range(ctx.pickerOptions)
                            valueIndex(ctx.pickerSelectedIndex)
                        }
                        event {
                            onChange { detail ->
                                KLog.i("WXExamplePage", "picker onChange: $detail")
                                val data = detail.optString("data")
                                val index = Regex("\"value\":(\\d+)")
                                    .find(data)?.groupValues?.getOrNull(1)?.toIntOrNull()
                                if (index != null) {
                                    ctx.pickerSelectedIndex = index
                                }
                            }
                            onCancel { KLog.i("WXExamplePage", "picker onCancel") }
                        }
                        View {
                            attr {
                                height(40f)
                                backgroundColor(0xFFF5F5F5)
                                borderRadius(6f)
                                padding(left = 12f, right = 12f)
                                alignItemsCenter()
                                flexDirectionRow()
                            }
                            Text {
                                attr {
                                    fontSize(14f)
                                    color(0xFF333333)
                                    text("当前城市：${ctx.pickerOptions[ctx.pickerSelectedIndex]}（点我选择）")
                                }
                            }
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXPicker 日期 / 时间 选择器" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXPicker {
                        attr {
                            mode(WXPickerMode.DATE)
                            start("2020-01-01")
                            end("2030-12-31")
                            value(ctx.pickerDate)
                        }
                        event {
                            onChange { detail ->
                                val data = detail.optString("data")
                                val match = Regex("\"value\":\"(.*?)\"")
                                    .find(data)?.groupValues?.getOrNull(1)
                                if (!match.isNullOrEmpty()) ctx.pickerDate = match
                            }
                        }
                        View {
                            attr {
                                height(40f)
                                backgroundColor(0xFFF5F5F5)
                                borderRadius(6f)
                                padding(left = 12f, right = 12f)
                                alignItemsCenter()
                                flexDirectionRow()
                            }
                            Text {
                                attr {
                                    fontSize(14f)
                                    color(0xFF333333)
                                    text("日期：${ctx.pickerDate}（点我选择）")
                                }
                            }
                        }
                    }
                    WXPicker {
                        attr {
                            marginTop(12f)
                            mode(WXPickerMode.TIME)
                            value(ctx.pickerTime)
                        }
                        event {
                            onChange { detail ->
                                val data = detail.optString("data")
                                val match = Regex("\"value\":\"(.*?)\"")
                                    .find(data)?.groupValues?.getOrNull(1)
                                if (!match.isNullOrEmpty()) ctx.pickerTime = match
                            }
                        }
                        View {
                            attr {
                                height(40f)
                                backgroundColor(0xFFF5F5F5)
                                borderRadius(6f)
                                padding(left = 12f, right = 12f)
                                alignItemsCenter()
                                flexDirectionRow()
                            }
                            Text {
                                attr {
                                    fontSize(14f)
                                    color(0xFF333333)
                                    text("时间：${ctx.pickerTime}（点我选择）")
                                }
                            }
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXVideo 视频播放器：基础属性与回调" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXVideo {
                        attr {
                            width(343f)
                            height(200f)
                            backgroundColor(0xFF000000)
                            src("http://vjs.zencdn.net/v/oceans.mp4")
                            poster("https://pic2.zhimg.com/v2-2a0434dd4e4bb7a638b8df699a505ca1_b.jpg")
                            controls(true)
                            objectFit(WXVideoObjectFit.CONTAIN)
                            showFullscreenBtn(true)
                            showPlayBtn(true)
                            showCenterPlayBtn(true)
                            initialTime(0)
                            title("WX Video Demo")
                        }
                        event {
                            onPlay { detail ->
                                KLog.i("WXExamplePage", "video onPlay: $detail")
                                ctx.videoPlayStatus = "正在播放"
                            }
                            onPause { detail ->
                                KLog.i("WXExamplePage", "video onPause: $detail")
                                ctx.videoPlayStatus = "已暂停"
                            }
                            onEnded { detail ->
                                KLog.i("WXExamplePage", "video onEnded: $detail")
                                ctx.videoPlayStatus = "播放结束"
                            }
                            onTimeUpdate { detail ->
                                val data = detail.optString("data")
                                val match = Regex("\"currentTime\":([0-9.]+)")
                                    .find(data)?.groupValues?.getOrNull(1)
                                if (!match.isNullOrEmpty()) ctx.videoCurrentTime = match
                            }
                            onWaiting { KLog.i("WXExamplePage", "video onWaiting") }
                            onError { detail ->
                                KLog.e("WXExamplePage", "video onError: $detail")
                                ctx.videoPlayStatus = "播放异常"
                            }
                            onFullscreenChange { detail ->
                                KLog.i("WXExamplePage", "video onFullscreenChange: $detail")
                            }
                            onLoadedMetadata { detail ->
                                KLog.i("WXExamplePage", "video onLoadedMetadata: $detail")
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("状态：${ctx.videoPlayStatus}")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("当前进度：${ctx.videoCurrentTime} s")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXVideo 高级属性：自动播放 / 循环 / 静音" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 20f)
                    }
                    WXVideo {
                        attr {
                            width(343f)
                            height(180f)
                            backgroundColor(0xFF000000)
                            src("http://vjs.zencdn.net/v/oceans.mp4")
                            autoplay(true)
                            loop(true)
                            muted(true)
                            controls(false)
                            showCenterPlayBtn(false)
                            objectFit(WXVideoObjectFit.COVER)
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF999999)
                            text("自动播放 + 静音 + 循环 + cover 适配")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXCamera 相机：基础用法" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXCamera {
                        attr {
                            width(343f)
                            height(260f)
                            backgroundColor(0xFF000000)
                            mode("normal")
                            resolution(WXCameraResolution.MEDIUM)
                            devicePosition(ctx.cameraDevicePosition)
                            flash(ctx.cameraFlashMode)
                        }
                        event {
                            onInitDone { detail ->
                                KLog.i("WXExamplePage", "camera onInitDone: $detail")
                                ctx.cameraStatus = "已就绪"
                            }
                            onStop { detail ->
                                KLog.i("WXExamplePage", "camera onStop: $detail")
                                ctx.cameraStatus = "已停止"
                            }
                            onError { detail ->
                                KLog.e("WXExamplePage", "camera onError: $detail")
                                ctx.cameraStatus = "出现错误"
                                ctx.cameraErrorTip = detail.toString()
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("相机状态：${ctx.cameraStatus}")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(0xFFCC3333)
                            text(if (ctx.cameraErrorTip.isEmpty()) "" else "错误信息：${ctx.cameraErrorTip}")
                        }
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            justifyContentSpaceAround()
                            marginTop(12f)
                        }
                        WXButton {
                            attr {
                                type("default")
                                size("mini")
                                titleAttr {
                                    text(if (ctx.cameraDevicePosition == WXCameraDevicePosition.BACK) "切前置" else "切后置")
                                    fontSize(14f)
                                }
                            }
                            event {
                                click {
                                    ctx.cameraDevicePosition =
                                        if (ctx.cameraDevicePosition == WXCameraDevicePosition.BACK) {
                                            WXCameraDevicePosition.FRONT
                                        } else {
                                            WXCameraDevicePosition.BACK
                                        }
                                }
                            }
                        }
                        WXButton {
                            attr {
                                type("default")
                                size("mini")
                                titleAttr {
                                    text("闪光灯：${ctx.cameraFlashMode}")
                                    fontSize(14f)
                                }
                            }
                            event {
                                click {
                                    ctx.cameraFlashMode = when (ctx.cameraFlashMode) {
                                        WXCameraFlash.AUTO -> WXCameraFlash.ON
                                        WXCameraFlash.ON -> WXCameraFlash.OFF
                                        WXCameraFlash.OFF -> WXCameraFlash.TORCH
                                        else -> WXCameraFlash.AUTO
                                    }
                                }
                            }
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXCamera 扫码模式：scanCode" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXCamera {
                        attr {
                            width(343f)
                            height(200f)
                            backgroundColor(0xFF000000)
                            mode("scanCode")
                        }
                        event {
                            onScanCode { detail ->
                                KLog.i("WXExamplePage", "camera onScanCode: $detail")
                                ctx.scanCodeTip = detail.toString()
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("扫码结果：${ctx.scanCodeTip}")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXMap 地图：基础显示 / 控件" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 12f)
                    }
                    WXMap {
                        attr {
                            width(343f)
                            height(260f)
                            latitude(39.9042)
                            longitude(116.4074)
                            scale(14.0)
                            showCompass(true)
                            showScale(true)
                            showLocation(true)
                            enableZoom(true)
                            enableScroll(true)
                            enableRotate(true)
                        }
                        event {
                            onRegionChange { detail ->
                                KLog.i("WXExamplePage", "map onRegionChange: $detail")
                                val data = detail.optString("data")
                                val type = Regex("\"type\":\"(.*?)\"")
                                    .find(data)?.groupValues?.getOrNull(1) ?: "-"
                                ctx.mapRegionChangeTip = "type=$type"
                            }
                            onError { detail ->
                                KLog.e("WXExamplePage", "map onError: $detail")
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("中心：${ctx.mapCenterTip}")
                        }
                    }
                    Text {
                        attr {
                            marginTop(4f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("视图事件：${ctx.mapRegionChangeTip}")
                        }
                    }
                }

                ViewExampleSectionHeader {
                    attr { title = "WXMap 标注 / 折线 / 圆：markers & polyline" }
                }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 20f)
                    }
                    WXMap {
                        attr {
                            width(343f)
                            height(300f)
                            latitude(39.9042)
                            longitude(116.4074)
                            scale(13.0)
                            markersJson(
                                """[
                                {"id":1,"latitude":39.9042,"longitude":116.4074,"title":"天安门","width":30,"height":30,"callout":{"content":"天安门广场","display":"ALWAYS","color":"#333333","fontSize":12,"borderRadius":4,"padding":6,"bgColor":"#FFFFFF"}},
                                {"id":2,"latitude":39.9164,"longitude":116.3972,"title":"故宫","width":30,"height":30}
                            ]""".trimIndent()
                            )
                            polylineJson(
                                """[{
                                "points":[
                                    {"latitude":39.9042,"longitude":116.4074},
                                    {"latitude":39.9164,"longitude":116.3972}
                                ],
                                "color":"#FF0000DD",
                                "width":4,
                                "dottedLine":false
                            }]""".trimIndent()
                            )
                            circlesJson(
                                """[{
                                "latitude":39.9042,
                                "longitude":116.4074,
                                "color":"#FF000088",
                                "fillColor":"#FF000033",
                                "radius":300,
                                "strokeWidth":2
                            }]""".trimIndent()
                            )
                            includePointsJson(
                                """[
                                {"latitude":39.9042,"longitude":116.4074},
                                {"latitude":39.9164,"longitude":116.3972}
                            ]""".trimIndent()
                            )
                            showLocation(true)
                            showScale(true)
                        }
                        event {
                            onMarkerTap { detail ->
                                KLog.i("WXExamplePage", "map onMarkerTap: $detail")
                                val data = detail.optString("data")
                                val markerId = Regex("\"markerId\":(\\d+)")
                                    .find(data)?.groupValues?.getOrNull(1) ?: "-"
                                ctx.mapMarkerTip = "点击 marker id=$markerId"
                            }
                            onTap { detail ->
                                KLog.i("WXExamplePage", "map onTap: $detail")
                            }
                            onCalloutTap { detail ->
                                KLog.i("WXExamplePage", "map onCalloutTap: $detail")
                            }
                        }
                    }
                    Text {
                        attr {
                            marginTop(8f)
                            fontSize(12f)
                            color(0xFF666666)
                            text("交互：${ctx.mapMarkerTip}")
                        }
                    }
                }

                // ---------------- WXWebView 跳转入口 ----------------
                ViewExampleSectionHeader { attr { title = "WXWebView 跳转入口" } }
                View {
                    attr {
                        flexDirectionColumn()
                        padding(left = 16f, right = 16f, top = 12f, bottom = 20f)
                    }
                    Text {
                        attr {
                            fontSize(12f)
                            color(0xFF999999)
                            marginBottom(8f)
                            text("说明：微信小程序 web-view 会自动占满整页，因此在独立子页面展示。")
                        }
                    }
                    View {
                        attr {
                            flexDirectionRow()
                            justifyContentSpaceAround()
                        }
                        WXButton {
                            attr {
                                type("default")
                                size("mini")
                                titleAttr {
                                    text("官方文档")
                                    fontSize(14f)
                                }
                            }
                            event {
                                click {
                                    ctx.openWebViewDemo(
                                        "https://developers.weixin.qq.com/miniprogram/dev/component/web-view.html"
                                    )
                                }
                            }
                        }
                        WXButton {
                            attr {
                                type("primary")
                                size("mini")
                                titleAttr {
                                    text("Kuikly 介绍")
                                    fontSize(14f)
                                }
                            }
                            event {
                                click {
                                    ctx.openWebViewDemo("https://github.com/Tencent-TDS/KuiklyUI")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
