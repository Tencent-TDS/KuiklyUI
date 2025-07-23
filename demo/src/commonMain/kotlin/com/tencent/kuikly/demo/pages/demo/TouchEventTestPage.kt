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
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.core.views.compose.Button
import com.tencent.kuikly.demo.pages.base.BasePager
import com.tencent.kuikly.demo.pages.demo.base.NavBar

@Page("TouchEventTestPage")
internal class TouchEventTestPage : BasePager() {

    override fun body(): ViewBuilder {

        return {
            NavBar {
                attr {
                    title = "TouchEventTestPage"
                }
            }
            List {
                attr {
                    flex(1f)
                }

                View {
                    attr {
                        paddingBottom(100f)
                        paddingTop(100f)
                        backgroundColor(Color.YELLOW)
                    }
                    event {
                        touchUp {
                            KLog.i("YELLOW", "YELLOW:touchUp")
                        }

                        touchDown {
                            KLog.i("YELLOW", "YELLOW:touchDown")
                        }

                        touchMove {
                            KLog.i("YELLOW", "YELLOW:touchMove")
                        }
                        click {
                            KLog.i("YELLOW", "YELLOW:click")
                        }
                    }
                    Button {
                        attr {
                            height(100f)
                            backgroundColor(Color.RED)
                            titleAttr {
                                text("我是按钮啊")
                                fontSize(30f)
                            }
                        }

                        event {
                            touchUp {
                                KLog.i("RED", "RED:touchUp")
                            }

                            touchDown {
                                KLog.i("RED", "RED:touchDown")
                            }

                            touchMove {
                                KLog.i("RED", "RED:touchMove")
                            }
                            click {
                                KLog.i("RED", "RED:click")
                            }
                        }
                    }
                }
                Button {
                    attr {
                        height(100f)
                        marginTop(-50f)
                        backgroundColor(Color.GREEN)
                        titleAttr {
                            text("我是Green按钮")
                            fontSize(30f)
                        }
                    }

                    event {
                        touchUp {
                            KLog.i("GREEN", "GREEN:touchUp")
                        }

                        touchDown {
                            KLog.i("GREEN", "GREEN:touchDown")
                        }

                        touchMove {
                            KLog.i("GREEN", "GREEN:touchMove")
                        }
//                        click {
//                            KLog.i("GREEN", "GREEN:click")
//                        }
                    }
                }

            }
        }
    }
}
