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

package com.tencent.kuikly.demo.pages

import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View

@Page("1111")
internal class AbsLayoutFixPage : BasePager() {
    
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                allCenter()
            }

            View {
                attr {
                    size(200f, 200f)
                    backgroundColor(Color.YELLOW)
                    allCenter()
                }

                // bugfix1: 绝对布局不受父布局的对齐影响
                View {
                    attr {
                        positionAbsolute()
                        size(100f, 100f)
                        backgroundColor(Color.RED)
                    }
                }
            }

            // bugfix2: 绝对布局撑满父亲，父亲的宽度由孩子决定，父亲设置了对齐属性，此时绝对布局对孩子的对齐属性失效
            // bugfix3: 绝对布局嵌套绝对布局失效
            View {

                View {
                    attr {
                        size(pagerData.pageViewWidth, 500f)
                        backgroundColor(Color.RED)
                    }
                }

                View {
                    attr {
                        absolutePositionAllZero()
                        backgroundColor(Color.BLUE)
                    }
                }

                View {
                    attr {
                        positionAbsolute()
                        absolutePositionAllZero()
                        allCenter()
                    }

                    View {
                        attr {
                            positionAbsolute()
                            absolutePosition(top = 70f, left = 70f, right = 70f, bottom = 70f)
                            backgroundColor(Color.GRAY)
                            allCenter()
                        }

                        View {
                            attr {
                                size(70f, 70f)
                                backgroundColor(Color.GREEN)
                            }
                        }

                        View {
                            attr {
                                size(20f, 20f)
                                backgroundColor(Color.RED)
                            }
                        }

                        Text {
                            attr {
                                text("这是一个文本")
                                fontSize(20f)
                                color(Color.GREEN)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun debugUIInspector(): Boolean {
        return true
    }
}

/**
 * Created by kam on 2022/7/28.
 */

@Page("6", supportInLocal = true)
internal class AbsLayoutWithFlexBugFixPager : BasePager() {

    companion object {
        val TAG = "DaShiDouPopDialog"
    }

    override fun body(): ViewBuilder {
        val ctx = this
        // LogModuleAPI.logI(TAG, "ViewBuilder called")
        return {
            attr {
                flexDirectionRow()
                backgroundColor(Color.RED)
//                allCenter()
            }

            //bg
            View {
                attr {
                    positionRelative()
                    backgroundColor(Color.GREEN)
                    //由于是相对布局，主轴撑满了父容器，纵轴flex(1.0f) 设置也撑满，就做到撑满背景了
                    flex(1.0f)
                    //孩子项目居中
                    allCenter()
                }

                //相对位置，并且由父容器的allCenter决定在中央
                View {
                    attr{

                    }

                    Image {
                        attr {
                            size(pagerData.pageViewWidth * 0.8f, pagerData.pageViewHeight * 0.8f)
                            //  src(ctx.bgUrl)
                            backgroundColor(Color.YELLOW)
                        }
                    }
                    View {
                        attr {
                            flexDirectionColumn()
//                            absolutePositionAllZero()
                            absolutePosition(left = 0f, right = 0f, bottom = 30f)
                            backgroundColor(Color.RED)
//                            allCenter()
//                            justifyContentCenter()

                        }

                        Image {
                            attr {
                                alignSelfCenter()
                                size(100f, 30f)
                                backgroundColor(Color.BLUE)
                            }

                        }

                    }

                }
            }

        }

    }

    override fun debugUIInspector(): Boolean {
//        return super.debugUIInspector()
        return true
    }

}