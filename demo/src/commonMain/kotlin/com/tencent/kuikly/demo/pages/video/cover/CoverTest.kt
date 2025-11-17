//package com.tencent.kuiklydemo.mtt.cover
//
//import com.tencent.kuiklydemo.mtt.QBBasePager
//import com.tencent.kuikly.core.annotations.Page
//import com.tencent.kuikly.core.base.ViewBuilder
//import com.tencent.kuikly.core.log.KLog
//
//@Page("CoverTest")
//internal class CoverTest : QBBasePager() {
//
//    override fun body(): ViewBuilder {
//        val ctx = this
//        return {
//            // 全屏
////            QBCover {
////                attr {
////                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
////                    coverHeight(1280)
////                    topBarHeight(88)
////                    ignoreGauss(true)
////                    src("http://qqpublic.qpic.cn/qq_public/0/31-2109552929-F8BC4B36BDAF02FCB06CE8ACD30456A0/0?fmt=jpg&size=148&h=960&w=540&ppv=1")
////                    coverWidth(720)
////                    progressBarBottom(56)
////                }
////            }
//            // 半屏
//            QBCover {
//                attr {
//                    size(pagerData.pageViewWidth, pagerData.pageViewHeight)
//                    src("http://qqpublic.qpic.cn/qq_public/0/31-3047307586-1EE7FB4E43A0F435B0FB4AB0D12EF1DF/0?fmt=jpg&size=435&h=1080&w=1920&ppv=1")
//                    coverHeight(1080)
//                    coverWidth(1920)
//                    // iOS 和 安卓不一样
//                    topBarHeight(88)
//                    progressBarBottom(56)
//                    ignoreGauss(true)
//                }
//                event {
//                    onLoadEnd {
//                        KLog.d("QBCoverView", "loadEndEnd")
//                    }
//                }
//            }
//        }
//    }
//}