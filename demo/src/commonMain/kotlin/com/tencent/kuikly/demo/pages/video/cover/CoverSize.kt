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


package com.tencent.kuikly.demo.pages.video.cover



const val ACTION_BAR_HEIGHT = 88

fun getTopBarHeight(deviceWidth: Float, deviceHeight: Float): Float {
//    if (getPlatform().name.startsWith("iOS")) {
//        return 60f// (88 / 2) * (deviceWidth* 2 / designScreenWidth) // px(ACTION_BAR_HEIGHT).toInt()
//    }
//    if (isFoldScreen()) {
//        return 1f;
//    }
    return 88f;
}


const val BASE_WIDTH = 750;

var zoomRatio = 0f
// 默认设计稿尺寸750
const val designScreenWidth = 750;
const val designZoomRatio = 0.5;

fun px(size: Int, resize: Boolean = true): Number {

    if (!resize) {
        return designScreenWidth * size
    }

    if (zoomRatio.toInt() == 0) {
        zoomRatio = adapterFoldScreenWidth().toFloat() / designScreenWidth.toFloat()
    }

    return zoomRatio * size;
}

val adapterScreenWidth = 0;
fun adapterFoldScreenWidth(): Number  {
    if (adapterScreenWidth != 0) return adapterScreenWidth
    // todo adapterScreenWidth计算
//    const { width, height } = Dimensions.get('window')
    if (isFoldScreen()) {
        // 该数值是按 oppo 折叠屏为基础计算一个固定百分比
        // adapterScreenWidth = 384 * (Math.max(width, height)) / 682.7;
    } else {
        // adapterScreenWidth = min(width, height);
    }

    return adapterScreenWidth
}

fun isFoldScreen(): Boolean{
    return false
}

const val COMMENT_BOX_HEIGHT = 50
val LAYER_BOTTOM = getSafeBottom() + COMMENT_BOX_HEIGHT

/** 安全距底（iphoneX），安卓防止底部导航条遮挡默认为6 */
//const val SAFE_BOTTOM = ((getPlatform().name.startsWith("iOS")) && isiPhoneX() ? 20 : 0) || ((getPlatform().name == "Android") ? 6 : 0)
fun getSafeBottom(): Int {
//    return if ((getPlatform().name.startsWith("iOS"))) {
//        20
//    } else if (getPlatform().name == "Android") {
//        6
//    } else {
//        0
//    }
    return 0
}


///** 判断机型是否为刘海屏机型*/
//fun isiPhoneX(): Boolean {
//    val rst = false;
//    if (getPlatform().name == "Android") return rst
//    // todo height的值获取
////    const { height } = Dimensions.get('window');
////    if (height === 812 || height === 896 || height === 844 || height === 926 || height === 852 || height === 932) {
////        rst = true;
////    }
//    return rst;
//}