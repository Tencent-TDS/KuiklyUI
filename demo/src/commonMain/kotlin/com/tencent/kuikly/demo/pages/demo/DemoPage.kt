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

package com.tencent.kuikly.demo.pages.demo

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.ComposeEvent
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.directives.vforIndex
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.CalendarModule
import com.tencent.kuikly.core.module.ICalendar
import com.tencent.kuikly.core.module.SnapshotModule
import com.tencent.kuikly.core.reactive.collection.ObservableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * Created by kam on 2022/6/22.
 */
@Page("example")
internal class DemoPage : BasePager() {
    private var showActionSheet by observable(false)
    private var dataList: ObservableList<CardView.Item> by observableList()

    override fun createEvent(): ComposeEvent {
        return ComposeEvent()
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {

////            // 导航栏（顶部显示协商历史）
            Topbar {
                event {

                }
            }
//            // 列表
            List {
                attr {
                    flex(1f)
                    // backgroundLinearGradient(Direction.TO_RIGHT, ColorStop())
                }

                vforIndex({ ctx.dataList }) { itemData, index, count ->
                    // 协商历史卡片
                    KLog.i("DemoPage", "vfor index:${index} count${count}")
                    Card {
                        attr {
                            item = itemData
                        }
                    }
                }
            }

        }
    }

    override fun created() {
        super.created()

        val calendar =
            PagerManager.getCurrentPager().acquireModule<CalendarModule>(CalendarModule.MODULE_NAME)
                .newCalendarInstance()
        val d = calendar.get(ICalendar.Field.DAY_OF_WEEK)

        KLog.i("CalendarModule", "今天是周:${d}")

        do {

            var item = CardView.Item()
            item.apply {
                title = "取消售后申请"
                detailInfo = "撤销售后申请\n进入开始流程"
                avatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"
                index = dataList.count()
            }

            dataList.add(item)
        } while (false)

        do {
            var item = CardView.Item()
            item.apply {
                title = "同意售后申请"
                detailInfo = "商家已同意退货申请。\n" +
                        "退货地址：江苏省扬州市仪征新集镇迎宾路3号花藤印染院内亿合帽业二楼"

                index = dataList.count()
                avatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"
            }
            dataList.add(item)
        } while (false)

        do {
            var item = CardView.Item()
            item.apply {
                title = "发起售后申请"
                detailInfo =
                    "发起了退货退款售后申请\n售后类型：退货退款\n货物状态：已收到货\n退货原因：7天无理由退款\n退款金额：¥59\n退货方式：线下寄件"
                avatarUrl =
                    "https://p3.toutiaoimg.com/large/pgc-image/54b93ce31b2e47c3aa1224b8fbfe4ffa"
                index = dataList.count()
            }
            item.pictures.add("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg")
            item.pictures.add("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg")
            item.pictures.add("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg")
            item.pictures.add("https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg")

            dataList.add(item)
        } while (false)

        do {

            var item = CardView.Item()
            item.apply {
                title = "取消售后申请"
                detailInfo = "撤销售后申请\n进入开始流程"
                avatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"
                index = dataList.count()
            }

            dataList.add(item)
        } while (false)

        do {
            var item = CardView.Item()
            item.apply {
                title = "同意售后申请"
                detailInfo = "商家已同意退货申请。\n" +
                        "退货地址：江苏省扬州市仪征新集镇迎宾路3号花藤印染院内亿合帽业二楼"

                index = dataList.count()
                avatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"
            }
            dataList.add(item)
        } while (false)

        do {

            var item = CardView.Item()
            item.apply {
                title = "取消售后申请"
                detailInfo = "撤销售后申请\n进入开始流程"
                avatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"
                index = dataList.count()
            }

            dataList.add(item)
        } while (false)

        do {
            var item = CardView.Item()
            item.apply {
                title = "同意售后申请"
                detailInfo = "商家已同意退货申请。\n" +
                        "退货地址：江苏省扬州市仪征新集镇迎宾路3号花藤印染院内亿合帽业二楼"

                index = dataList.count()
                avatarUrl = "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c498f4b4.jpg"
            }
            dataList.add(item)
        } while (false)

    }

    override fun viewDidLoad() {
        super.viewDidLoad()
        getPager().acquireModule<SnapshotModule>(SnapshotModule.MODULE_NAME)
            .snapshotPager("example")
    }
}
