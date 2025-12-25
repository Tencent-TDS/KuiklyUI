package com.example.kuiklyworkwithminiapp

import com.example.kuiklyworkwithminiapp.base.BasePager
import com.example.kuiklyworkwithminiapp.components.CustomButton
import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.Border
import com.tencent.kuikly.core.base.BorderStyle
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.core.views.View

@Page("customButton", supportInLocal = true)
internal class CustomButtonPage : BasePager() {
    override fun body(): ViewBuilder {
        val ctx = this
        return {
            View {
                attr {
                    width(200f)
                    height(200f)
                    margin(60f)
                }
                CustomButton {
                    attr {
                        width(150f)
                        height(40f)
                        borderRadius(10f)
                        backgroundColor(Color.YELLOW)
                        marginBottom(10f)
                        padding(10f)
                        border(Border(1f, BorderStyle.SOLID, Color.BLACK))
                        titleAttr {
                            text("getPhoneNumber")
                        }
                        openType("getPhoneNumber")
                    }
                    event {
                        onGetPhoneNumber {
                            val callbackData = it as JSONObject
                            val data = callbackData.optString("data")
                            val dataObj = JSONObject(data)
                            KLog.d("onGetPhoneNumber", dataObj.toString())
                        }
                    }
                }

                CustomButton {
                    attr {
                        titleAttr {
                            text("openSetting")
                        }
                        openType("openSetting")
                    }
                }
            }
        }
    }
}