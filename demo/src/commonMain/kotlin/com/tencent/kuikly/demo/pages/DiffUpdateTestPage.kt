package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.datetime.DateTime
import com.tencent.kuikly.core.directives.vfor
import com.tencent.kuikly.core.layout.FlexAlign
import com.tencent.kuikly.core.layout.FlexJustifyContent
import com.tencent.kuikly.core.reactive.handler.observableList
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.Input
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.reactive.collection.ObservableList
import kotlin.random.Random

@Page("DiffUpdateTestPage")
internal class DiffUpdateTestPage : BasePager() {
    private var list by observableList<String>()
    private var inputIndex by observable("0")


    private val imageUrls = listOf(
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d0813ca.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45ad086d.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3ecf791d.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d77dc0ad.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8a01b17c.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/844aa82b.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/891fc305.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/7d986b3a.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/007634a8.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/38d02bd9.png",
        "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/a595874c.png"
    )

    // 随机获取一个图片URL
    private fun getRandomImageUrl(): String {
        return imageUrls[Random.nextInt(imageUrls.size)]
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    backgroundColor(Color.WHITE)
                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
                    justifyContent(FlexJustifyContent.CENTER)
                    alignItems(FlexAlign.CENTER)
                }

                List {
                    attr {
                        size(pagerData.pageViewWidth, pagerData.pageViewHeight-200)
                        border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                        alignItems(FlexAlign.CENTER)
                        justifyContent(FlexJustifyContent.CENTER)
                    }

                    vfor({ ctx.list }) { item -> // 使用item参数获取列表项数据
                        View {
                            attr {
                                width(pagerData.pageViewWidth)
                                alignItems(FlexAlign.CENTER)
                                justifyContent(FlexJustifyContent.CENTER)
                            }

                            Image {
                                attr {
                                    size(pagerData.pageViewWidth,100f)
                                    src(item) // 直接使用列表项的URL
                                }
                            }

                        }
                    }
                }

                View {
                    attr {
                        width(pagerData.pageViewWidth)
                        height(200f)
                        flexDirection(com.tencent.kuikly.core.layout.FlexDirection.COLUMN)
                        justifyContent(FlexJustifyContent.SPACE_AROUND)
                        alignItems(FlexAlign.CENTER)
                    }

                    // 输入框区域
                    View {
                        attr {
                            width(pagerData.pageViewWidth)
                            height(50f)
                            flexDirection(com.tencent.kuikly.core.layout.FlexDirection.ROW)
                            justifyContent(FlexJustifyContent.CENTER)
                            alignItems(FlexAlign.CENTER)
                        }

                        Text {
                            attr {
                                text("索引:")
                                color(Color.BLACK)
                                fontSize(16f)
                                margin(right = 10f)
                            }
                        }

                        Input {
                            attr {
                                width(100f)
                                height(40f)
                                backgroundColor(Color.WHITE)
                                border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                                fontSize(16f)
                                placeholder("0")
                                text(ctx.inputIndex)
                                keyboardTypeNumber()
                            }
                            event {
                                textDidChange {
                                    ctx.inputIndex = it.text
                                }
                            }
                        }
                    }

                    // 按钮区域 - 第一行
                    View {
                        attr {
                            width(pagerData.pageViewWidth)
                            height(50f)
                            flexDirection(com.tencent.kuikly.core.layout.FlexDirection.ROW)
                            justifyContent(FlexJustifyContent.SPACE_AROUND)
                            alignItems(FlexAlign.CENTER)
                        }

                        Button {
                            attr {
                                highlightBackgroundColor(Color.YELLOW)
                                backgroundColor(Color.GREEN)
                                size(100f, 45f)
                                border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                            }
                            Text {
                                attr {
                                    text("添加")
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    val index = ctx.inputIndex.toIntOrNull() ?: 0
                                    val safeIndex = index.coerceIn(0, ctx.list.size)
                                    ctx.list.add(safeIndex, ctx.getRandomImageUrl())
                                }
                            }
                        }

                        Button {
                            attr {
                                highlightBackgroundColor(Color.YELLOW)
                                backgroundColor(Color.BLACK)
                                size(100f, 45f)
                                border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                            }
                            Text {
                                attr {
                                    text("删除")
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    val index = ctx.inputIndex.toIntOrNull() ?: 0
                                    if (index >= 0 && index < ctx.list.size) {
                                        ctx.list.removeAt(index)
                                    }
                                }
                            }
                        }

                        Button {
                            attr {
                                highlightBackgroundColor(Color.YELLOW)
                                backgroundColor(Color.BLUE)
                                size(100f, 45f)
                                border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                            }
                            Text {
                                attr {
                                    text("替换")
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    val index = ctx.inputIndex.toIntOrNull() ?: 0
                                    if (index >= 0 && index < ctx.list.size) {
                                        val newLists = ctx.list.toMutableList()

                                        newLists[index] = ctx.imageUrls[5]
                                        newLists[index+1] = ctx.imageUrls[6]
                                        newLists[index+2] = ctx.imageUrls[7]

                                        ctx.list.diffUpdate(newLists)
                                    }
                                }
                            }
                        }
                    }

                    // 按钮区域 - 第二行
                    View {
                        attr {
                            width(pagerData.pageViewWidth)
                            height(50f)
                            flexDirection(com.tencent.kuikly.core.layout.FlexDirection.ROW)
                            justifyContent(FlexJustifyContent.SPACE_AROUND)
                            alignItems(FlexAlign.CENTER)
                        }

                        Button {
                            attr {
                                highlightBackgroundColor(Color.YELLOW)
                                backgroundColor(Color.RED)
                                size(100f, 45f)
                                border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                            }
                            Text {
                                attr {
                                    text("Clear")
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    val tempList = mutableListOf<String>()
                                    tempList.clear()
                                    tempList.addAll(ctx.imageUrls)
                                    ctx.list.diffUpdate(tempList)
                                }
                            }
                        }
                        Button {
                            attr {
                                highlightBackgroundColor(Color.YELLOW)
                                backgroundColor(Color.RED)
                                size(100f, 45f)
                                border(Border(2f, BorderStyle.SOLID, Color.BLACK))
                            }
                            Text {
                                attr {
                                    text("test")
                                    color(Color.WHITE)
                                }
                            }
                            event {
                                click {
                                    // Test 1: insert at beginning
                                    println("======= test 1: insert at beginning ==========")
                                    val list1 = ObservableList(mutableListOf("B", "C", "D"))
                                    list1.diffUpdate(listOf("A", "B", "C", "D"))
                                    println("Expected: [A, B, C, D]")
                                    println("Actual:   $list1")
                                    println("Pass: ${list1.toList() == listOf("A", "B", "C", "D")}")

                                    // Test 2: delete from middle
                                    println("\n======= test 2: delete from middle ==========")
                                    val list2 = ObservableList(mutableListOf("A", "B", "C", "D"))
                                    list2.diffUpdate(listOf("A", "C", "D"))
                                    println("Expected: [A, C, D]")
                                    println("Actual:   $list2")
                                    println("Pass: ${list2.toList() == listOf("A", "C", "D")}")

                                    // Test 3: replace element
                                    println("\n======= test 3: replace element ==========")
                                    val list3 = ObservableList(mutableListOf("A", "B", "C"))
                                    list3.diffUpdate(listOf("A", "X", "C"))
                                    println("Expected: [A, X, C]")
                                    println("Actual:   $list3")
                                    println("Pass: ${list3.toList() == listOf("A", "X", "C")}")

                                    // Test 4: move element
                                    println("\n======= test 4: move element ==========")
                                    val list4 = ObservableList(mutableListOf("A", "B", "C", "D"))
                                    list4.diffUpdate(listOf("D", "B", "C", "A"))
                                    println("Expected: [D, B, C, A]")
                                    println("Actual:   $list4")
                                    println("Pass: ${list4.toList() == listOf("D", "B", "C", "A")}")

                                    // Test 5: complex scenario
                                    println("\n======= test 5: complex scenario ==========")
                                    val list5 = ObservableList(mutableListOf("A", "B", "C", "D", "E"))
                                    list5.diffUpdate(listOf("B", "C", "X", "D", "F"))
                                    println("Expected: [B, C, X, D, F]")
                                    println("Actual:   $list5")
                                    println("Pass: ${list5.toList() == listOf("B", "C", "X", "D", "F")}")

                                    // Test 6: with custom comparator
                                    println("\n======= test 6: with custom comparator ==========")
                                    data class User(val id: Int, val name: String)
                                    val list6 = ObservableList(mutableListOf(
                                        User(1, "Alice"),
                                        User(2, "Bob")
                                    ))
                                    list6.diffUpdate(
                                        listOf(
                                            User(1, "Alice Updated"),
                                            User(3, "Charlie")
                                        )
                                    ) { old, new -> old.id == new.id }
                                    println("Expected size: 2")
                                    println("Actual size:   ${list6.size}")
                                    println("Expected: User(1, Alice), User(3, Charlie)")
                                    println("Actual:   ${list6[0]}, ${list6[1]}")
                                    println("Pass: ${list6.size == 2 && list6[0].id == 1 && list6[0].name == "Alice" && list6[1].id == 3}")

                                    // Test 7: empty to non-empty
                                    println("\n======= test 7: empty to non-empty ==========")
                                    val list7 = ObservableList<String>()
                                    list7.diffUpdate(listOf("A", "B", "C"))
                                    println("Expected: [A, B, C]")
                                    println("Actual:   $list7")
                                    println("Pass: ${list7.toList() == listOf("A", "B", "C")}")

                                    // Test 8: non-empty to empty
                                    println("\n======= test 8: non-empty to empty ==========")
                                    val list8 = ObservableList(mutableListOf("A", "B", "C"))
                                    list8.diffUpdate(emptyList())
                                    println("Expected: []")
                                    println("Actual:   $list8")
                                    println("Pass: ${list8.toList() == emptyList<String>()}")

                                    // Test 9: duplicate elements
                                    println("\n======= test 9: duplicate elements ==========")
                                    val list9 = ObservableList(mutableListOf("A", "A", "B", "C"))
                                    list9.diffUpdate(listOf("A", "B", "C"))
                                    println("Expected: [A, B, C]")
                                    println("Actual:   $list9")
                                    println("Pass: ${list9.toList() == listOf("A", "B", "C")}")

                                    // Test 10: completely different lists
                                    println("\n======= test 10: completely different lists ==========")
                                    val list10 = ObservableList(mutableListOf("A", "B", "C"))
                                    list10.diffUpdate(listOf("X", "Y", "Z"))
                                    println("Expected: [X, Y, Z]")
                                    println("Actual:   $list10")
                                    println("Pass: ${list10.toList() == listOf("X", "Y", "Z")}")

                                    // Test 11: single to single (different)
                                    println("\n======= test 11: single to single (different) ==========")
                                    val list11 = ObservableList(mutableListOf("A"))
                                    list11.diffUpdate(listOf("B"))
                                    println("Expected: [B]")
                                    println("Actual:   $list11")
                                    println("Pass: ${list11.toList() == listOf("B")}")

                                    // Test 12: single to multiple
                                    println("\n======= test 12: single to multiple ==========")
                                    val list12 = ObservableList(mutableListOf("A"))
                                    list12.diffUpdate(listOf("A", "B", "C"))
                                    println("Expected: [A, B, C]")
                                    println("Actual:   $list12")
                                    println("Pass: ${list12.toList() == listOf("A", "B", "C")}")

                                    // Test 13: multiple to single
                                    println("\n======= test 13: multiple to single ==========")
                                    val list13 = ObservableList(mutableListOf("A", "B", "C"))
                                    list13.diffUpdate(listOf("B"))
                                    println("Expected: [B]")
                                    println("Actual:   $list13")
                                    println("Pass: ${list13.toList() == listOf("B")}")

                                    // Test 14: complete reverse
                                    println("\n======= test 14: complete reverse ==========")
                                    val list14 = ObservableList(mutableListOf("A", "B", "C", "D", "E"))
                                    list14.diffUpdate(listOf("E", "D", "C", "B", "A"))
                                    println("Expected: [E, D, C, B, A]")
                                    println("Actual:   $list14")
                                    println("Pass: ${list14.toList() == listOf("E", "D", "C", "B", "A")}")

                                    // Test 15: partial overlap
                                    println("\n======= test 15: partial overlap ==========")
                                    val list15 = ObservableList(mutableListOf("A", "B", "C"))
                                    list15.diffUpdate(listOf("B", "C", "D", "E"))
                                    println("Expected: [B, C, D, E]")
                                    println("Actual:   $list15")
                                    println("Pass: ${list15.toList() == listOf("B", "C", "D", "E")}")

                                    // Test 16: identical lists (no change)
                                    println("\n======= test 16: identical lists (no change) ==========")
                                    val list16 = ObservableList(mutableListOf("A", "B", "C"))
                                    list16.diffUpdate(listOf("A", "B", "C"))
                                    println("Expected: [A, B, C]")
                                    println("Actual:   $list16")
                                    println("Pass: ${list16.toList() == listOf("A", "B", "C")}")

                                    // Test 17: multiple duplicates complex scenario
                                    println("\n======= test 17: multiple duplicates complex ==========")
                                    val list17 = ObservableList(mutableListOf("A", "B", "A", "C", "A"))
                                    list17.diffUpdate(listOf("A", "A", "C", "D"))
                                    println("Expected: [A, A, C, D]")
                                    println("Actual:   $list17")
                                    println("Pass: ${list17.toList() == listOf("A", "A", "C", "D")}")

                                    // Test 17.1: 2000 items with only 2 differences - Performance Test
                                    println("\n======= test 17.1: 2000 items with only 2 differences =========")
                                    val list17_1 = ObservableList((0..1999).map { "Item$it" }.toMutableList())
                                    val newList17_1 = (0..1999).map { 
                                        when (it) {
                                            500 -> "Modified500"
                                            1500 -> "Modified1500"
                                            else -> "Item$it"
                                        }
                                    }
                                    val startTime17_1 = DateTime.currentTimestamp()
                                    list17_1.diffUpdate(newList17_1)
                                    val endTime17_1 = DateTime.currentTimestamp()
                                    println("Expected size: 2000")
                                    println("Actual size:   ${list17_1.size}")
                                    println("Expected changes at index 500 and 1500")
                                    println("Actual[500]:   ${list17_1[500]}")
                                    println("Actual[1500]:  ${list17_1[1500]}")
                                    println("Time taken: ${endTime17_1 - startTime17_1}ms")
                                    println("Pass: ${list17_1.size == 2000 && list17_1[500] == "Modified500" && list17_1[1500] == "Modified1500"}")

                                    // Test 17.2: 2000 items completely different - Performance Test
                                    println("\n======= test 17.2: 2000 items completely different =========")
                                    val list17_2 = ObservableList((0..1999).map { "OldItem$it" }.toMutableList())
                                    val newList17_2 = (0..1999).map { "NewItem$it" }
                                    val startTime17_2 = DateTime.currentTimestamp()
                                    list17_2.diffUpdate(newList17_2)
                                    val endTime17_2 = DateTime.currentTimestamp()
                                    println("Expected size: 2000")
                                    println("Actual size:   ${list17_2.size}")
                                    println("Expected first: NewItem0, last: NewItem1999")
                                    println("Actual first:   ${list17_2.first()}, last: ${list17_2.last()}")
                                    println("Time taken: ${endTime17_2 - startTime17_2}ms")
                                    println("Pass: ${list17_2.size == 2000 && list17_2.first() == "NewItem0" && list17_2.last() == "NewItem1999"}")

                                    // Performance comparison
                                    println("\n======= Performance Comparison =========")
                                    println("2000 items with 2 differences: ${endTime17_1 - startTime17_1}ms")
                                    println("2000 items completely different: ${endTime17_2 - startTime17_2}ms")
                                    println("Difference: ${(endTime17_2 - startTime17_2) - (endTime17_1 - startTime17_1)}ms")

                                    // Test 18: single element same
                                    println("\n======= test 18: single element same ==========")
                                    val list18 = ObservableList(mutableListOf("A"))
                                    list18.diffUpdate(listOf("A"))
                                    println("Expected: [A]")
                                    println("Actual:   $list18")
                                    println("Pass: ${list18.toList() == listOf("A")}")

                                    // Test 19: insert at end
                                    println("\n======= test 19: insert at end ==========")
                                    val list19 = ObservableList(mutableListOf("A", "B", "C"))
                                    list19.diffUpdate(listOf("A", "B", "C", "D", "E"))
                                    println("Expected: [A, B, C, D, E]")
                                    println("Actual:   $list19")
                                    println("Pass: ${list19.toList() == listOf("A", "B", "C", "D", "E")}")

                                    // Test 20: delete from end
                                    println("\n======= test 20: delete from end ==========")
                                    val list20 = ObservableList(mutableListOf("A", "B", "C", "D", "E"))
                                    list20.diffUpdate(listOf("A", "B", "C"))
                                    println("Expected: [A, B, C]")
                                    println("Actual:   $list20")
                                    println("Pass: ${list20.toList() == listOf("A", "B", "C")}")

                                    // Test 21: alternating pattern
                                    println("\n======= test 21: alternating pattern ==========")
                                    val list21 = ObservableList(mutableListOf("A", "B", "C", "D"))
                                    list21.diffUpdate(listOf("B", "A", "D", "C"))
                                    println("Expected: [B, A, D, C]")
                                    println("Actual:   $list21")
                                    println("Pass: ${list21.toList() == listOf("B", "A", "D", "C")}")

                                    // Test 22: custom comparator with multiple matches
                                    println("\n======= test 22: custom comparator multiple matches ==========")
                                    data class Item(val type: String, val value: Int)
                                    val list22 = ObservableList(mutableListOf(
                                        Item("A", 1),
                                        Item("A", 2),
                                        Item("B", 3)
                                    ))
                                    list22.diffUpdate(
                                        listOf(
                                            Item("A", 10),
                                            Item("B", 20)
                                        )
                                    ) { old, new -> old.type == new.type }
                                    println("Expected size: 2")
                                    println("Actual size:   ${list22.size}")
                                    println("Actual:   $list22")
                                    println("Pass: ${list22.size == 2}")

                                    // Test 23: all duplicates
                                    println("\n======= test 23: all duplicates ==========")
                                    val list23 = ObservableList(mutableListOf("A", "A", "A"))
                                    list23.diffUpdate(listOf("A", "A"))
                                    println("Expected: [A, A]")
                                    println("Actual:   $list23")
                                    println("Pass: ${list23.toList() == listOf("A", "A")}")

                                    // Test 24: interleaved insert and delete
                                    println("\n======= test 24: interleaved insert and delete ==========")
                                    val list24 = ObservableList(mutableListOf("A", "B", "C", "D", "E"))
                                    list24.diffUpdate(listOf("A", "X", "C", "Y", "E"))
                                    println("Expected: [A, X, C, Y, E]")
                                    println("Actual:   $list24")
                                    println("Pass: ${list24.toList() == listOf("A", "X", "C", "Y", "E")}")

                                    // Test 25: large list performance test
                                    println("\n======= test 25: large list performance ==========")
                                    val list25 = ObservableList((0..99).map { "Item$it" }.toMutableList())
                                    val newList25 = (50..149).map { "Item$it" }
                                    val startTime = DateTime.currentTimestamp()
                                    list25.diffUpdate(newList25)
                                    val endTime = DateTime.currentTimestamp()
                                    println("Expected size: 100")
                                    println("Actual size:   ${list25.size}")
                                    println("Time taken: ${endTime - startTime}ms")
                                    println("Pass: ${list25.size == 100 && list25.first() == "Item50" && list25.last() == "Item149"}")

                                    // Test 26: empty to empty
                                    println("\n======= test 26: empty to empty ==========")
                                    val list26 = ObservableList<String>()
                                    list26.diffUpdate(emptyList())
                                    println("Expected: []")
                                    println("Actual:   $list26")
                                    println("Pass: ${list26.toList() == emptyList<String>()}")

                                    // Test 27: with null values
                                    println("\n======= test 27: with null values ==========")
                                    val list27 = ObservableList(mutableListOf<String?>("A", null, "C"))
                                    list27.diffUpdate(listOf(null, "B", "C"))
                                    println("Expected: [null, B, C]")
                                    println("Actual:   $list27")
                                    println("Pass: ${list27.toList() == listOf(null, "B", "C")}")

                                    // Test 28: consecutive diffUpdate calls
                                    println("\n======= test 28: consecutive diffUpdate calls ==========")
                                    val list28 = ObservableList(mutableListOf("A", "B", "C"))
                                    list28.diffUpdate(listOf("A", "X", "C"))
                                    list28.diffUpdate(listOf("A", "X", "C", "D"))
                                    list28.diffUpdate(listOf("X", "C", "D"))
                                    println("Expected: [X, C, D]")
                                    println("Actual:   $list28")
                                    println("Pass: ${list28.toList() == listOf("X", "C", "D")}")

                                    // Test 29: massive middle insertion
                                    println("\n======= test 29: massive middle insertion ==========")
                                    val list29 = ObservableList(mutableListOf("A", "B"))
                                    val middleElements = (1..50).map { "X$it" }
                                    list29.diffUpdate(listOf("A") + middleElements + listOf("B"))
                                    println("Expected size: 52")
                                    println("Actual size:   ${list29.size}")
                                    println("Pass: ${list29.size == 52 && list29.first() == "A" && list29.last() == "B"}")

                                    // Test 30: large list single difference
                                    println("\n======= test 30: large list single difference ==========")
                                    val list30 = ObservableList((0..99).map { "Item$it" }.toMutableList())
                                    val newList30 = (0..99).map { if (it == 50) "Modified" else "Item$it" }
                                    list30.diffUpdate(newList30)
                                    println("Expected: Item49, Modified, Item51")
                                    println("Actual:   ${list30[49]}, ${list30[50]}, ${list30[51]}")
                                    println("Pass: ${list30[50] == "Modified" && list30.size == 100}")

                                    // Test 31: alternating repeat pattern
                                    println("\n======= test 31: alternating repeat pattern ==========")
                                    val list31 = ObservableList(mutableListOf("A", "B", "A", "B", "A", "B"))
                                    list31.diffUpdate(listOf("B", "A", "B", "A", "B", "A"))
                                    println("Expected: [B, A, B, A, B, A]")
                                    println("Actual:   $list31")
                                    println("Pass: ${list31.toList() == listOf("B", "A", "B", "A", "B", "A")}")

                                    // Test 32: delete only middle elements
                                    println("\n======= test 32: delete only middle elements ==========")
                                    val list32 = ObservableList(mutableListOf("A", "B", "C", "D", "E"))
                                    list32.diffUpdate(listOf("A", "E"))
                                    println("Expected: [A, E]")
                                    println("Actual:   $list32")
                                    println("Pass: ${list32.toList() == listOf("A", "E")}")

                                    // Test 33: replace both first and last
                                    println("\n======= test 33: replace both first and last ==========")
                                    val list33 = ObservableList(mutableListOf("A", "B", "C", "D", "E"))
                                    list33.diffUpdate(listOf("X", "B", "C", "D", "Y"))
                                    println("Expected: [X, B, C, D, Y]")
                                    println("Actual:   $list33")
                                    println("Pass: ${list33.toList() == listOf("X", "B", "C", "D", "Y")}")

                                    // Test 34: custom comparator no matches
                                    println("\n======= test 34: custom comparator no matches ==========")
                                    data class Product(val id: Int, val name: String)
                                    val list34 = ObservableList(mutableListOf(
                                        Product(1, "A"),
                                        Product(2, "B")
                                    ))
                                    list34.diffUpdate(
                                        listOf(
                                            Product(3, "C"),
                                            Product(4, "D")
                                        )
                                    ) { old, new -> old.id == new.id }
                                    println("Expected size: 2")
                                    println("Actual size:   ${list34.size}")
                                    println("Expected: Product(3, C), Product(4, D)")
                                    println("Actual:   ${list34[0]}, ${list34[1]}")
                                    println("Pass: ${list34.size == 2 && list34[0].id == 3 && list34[1].id == 4}")

                                    // Test 35: same by comparator different objects
                                    println("\n======= test 35: same by comparator different objects ==========")
                                    data class Entity(val id: Int, var value: String)
                                    val list35 = ObservableList(mutableListOf(
                                        Entity(1, "old")
                                    ))
                                    list35.diffUpdate(
                                        listOf(Entity(1, "old"))
                                    ) { old, new -> old.id == new.id }
                                    println("Expected size: 1")
                                    println("Actual size:   ${list35.size}")
                                    println("Expected value: old")
                                    println("Actual value:   ${list35[0].value}")
                                    println("Pass: ${list35.size == 1 && list35[0].value == "old"}")

                                    println("\n======= All tests completed ==========")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
