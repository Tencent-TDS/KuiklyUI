package com.tencent.kuikly.demo.pages.video.manager

import com.tencent.kuikly.core.log.KLog
import com.tencent.kuikly.core.manager.BridgeManager
import com.tencent.kuikly.core.manager.PagerManager
import com.tencent.kuikly.core.module.CodecModule
import com.tencent.kuikly.core.nvi.serialization.json.JSONObject
import com.tencent.kuikly.demo.pages.base.BridgeModule
import com.tencent.kuikly.demo.pages.video.type.VideoItem
import com.tencent.kuikly.demo.pages.app.model.AppFeedModel
import com.tencent.kuikly.demo.pages.app.model.AppFeedsType

object LoadVideosManager {
    private var page = 1
    private var isLoad: Boolean = false

    val videoUrls = arrayOf(
        "http://shortv.cdp.qq.com/szg_2958_50001_0bc37uaacaaaaaajo4amcvtvd7odah6qaaka.f104101.mp4?bid=qb&bitnum=6&dis_k=8861b84d501a700e4ec8a267639b4db9&dis_k2=51f61d6450388a12ee20be2c5ab87c15&dis_t=1728892416&hwcode=5.200000&network=1&qbvid=k356793ev6n&rand=i6WzS2Qixw&seqid=b92cbb91dd2e404534df1a0e0a82da1b&sf=qbnews&subsrc=mini_video_float",
        "http://wsv.cdp.qq.com/gzc_9548_1047_0bc3muacgaaav4aeyyik35trozieensqai2a.f70.mp4?bid=qb&bitnum=3&dis_k=cf2f4713e4469526b1683e5952f83a54&dis_k2=68ef425c515cf838437d45ba8b829ec1&dis_t=1728892416&hwcode=5.200000&network=1&qbvid=i3567ovurv1&rand=g4I5PcgrCS&seqid=b92cbb91dd2e404534df1a0e0a82da1b&sf=qbnews&subsrc=mini_video_float",
        "http://shortv.cdp.qq.com/szg_5393_50001_0bc32uaaaaaahuabrfak6ftvdvodadkqaaca.f104101.mp4?bid=qb&bitnum=6&dis_k=b1ef989d96965ef7b0201c315a77e5d3&dis_k2=c21ce5b11090933f6c78e94e86e1c502&dis_t=1728892416&hwcode=5.200000&network=1&qbvid=q3567vffekz&rand=iH0l9kug13&seqid=b92cbb91dd2e404534df1a0e0a82da1b&sf=qbnews&subsrc=mini_video_float",
        "http://wsv.cdp.qq.com/gzc_9696_1047_0bc3mmabwaaakqagtxyla5troyyednrqag2a.f70.mp4?bid=qb&bitnum=3&dis_k=21ac2365ba355de7c379ab17dfbf9256&dis_k2=dacce4be27266df70e0d65c9e215c6df&dis_t=1728892416&hwcode=5.200000&network=1&qbvid=l3567de54aj&rand=n8j6wa8F0w&seqid=b92cbb91dd2e404534df1a0e0a82da1b&sf=qbnews&subsrc=mini_video_float"
    )
    val imgUrls = arrayOf(
        "http://qqpublic.qpic.cn/qq_public/0/31-2734983327-462AC307AB964D1B469E0CCA1887C5E9/0?fmt=jpg&size=136&h=960&w=540&ppv=1",
        "http://qqpublic.qpic.cn/qq_public/0/31-1722925855-74D1893984D22A82799EFCE94944B2C7/0?fmt=jpg&size=126&h=960&w=540&ppv=1",
        "http://qqpublic.qpic.cn/qq_public/0/31-1040339783-3743A49612E574B2FA243D69A89F225A/0?fmt=jpg&size=115&h=960&w=540&ppv=1",
        "http://qqpublic.qpic.cn/qq_public/0/31-1620008444-FA07C8F5EAB38AB066A69EC8A2260D99/0?fmt=jpg&size=118&h=960&w=540&ppv=1"
    )

    fun loadVideos(): MutableList<VideoItem> {
        var videoList =  mutableListOf<VideoItem>()
        videoUrls.forEachIndexed { index, url ->
            val videoItem = VideoItem(videoUrl = url, imgUrl = imgUrls[index])
            videoList.add(videoItem)
        }
        return videoList
    }

    internal fun requestFeedsWithMockedData(callback: (List<VideoItem>) -> Unit) {
//        val temp = JSONObject(videoJson)
//        val abc = temp.optString("common_rsp")
//        val efg = JSONObject(abc)
//        val arr = efg.optJSONArray("items")
//        val codecModule = PagerManager.getCurrentPager().acquireModule<CodecModule>(CodecModule.MODULE_NAME)
//        var videoList =  mutableListOf<VideoItem>()
//        if (arr == null) {
//            KLog.d("123","abc is null")
//        } else {
//            KLog.d("12345", arr.toString())
//            for (i in 0..arr.length()) {
//                val item = arr.optJSONObject(i)
//                if (item != null) {
//                    val title = item.optString("title")
//                    val url = item.optString("url")
//                    val shared_sub_info = item.optJSONObject("shared_sub_info")
//                    val comment_num = shared_sub_info?.optString("comment_num") ?: ""
//                    val share_num = shared_sub_info?.optString("share_num") ?: ""
//                    val collect_num = shared_sub_info?.optString("collect_num")
//                    val tempMap = parseQBUrlToMap(url)
//                    val videoUrl = tempMap["videoUrl"]?.let { codecModule.urlDecode(it) } ?: ""
//                    val currentId = tempMap["currentId"]
//                    val userurl = tempMap["userurl"]?.let { codecModule.urlDecode(it) } ?:""
//                    val imgUrl = tempMap["imgUrl"]?.let { codecModule.urlDecode(it) }?: ""
//                    val iPraiseNum = tempMap["iPraiseNum"] ?: ""
//                    val extinfo = tempMap["extinfo"]?.let { codecModule.urlDecode(it) }
//                    val sFrom = tempMap["sFrom"]?.let{ codecModule.urlDecode(it) }
//                    val sUserIcon = tempMap["sUserIcon"]?.let{ codecModule.urlDecode(it) } ?: ""
//                    val duration = tempMap["duration"]?.toInt() ?: 0
//                    // todo 解析数据填充
//                    videoList.add(VideoItem(videoUrl = videoUrl, imgUrl = imgUrl, duration = duration))
//                }
//            }
//        }
//        isLoad = false
//        callback(videoList)
    }
    private fun requestFromModule(callback: (List<VideoItem>) -> Unit) {
        val fetcherModule = PagerManager.getCurrentPager().getModule<BridgeModule>(BridgeModule.MODULE_NAME)
        val pathName = getFileName()
        fetcherModule?.readAssetFile(pathName) { json ->
            if (json == null || json.optString("error").isNotEmpty()) {
                callback(listOf())
            } else {
                callback(parseJson(json))
            }
            isLoad = false
        }
//        val fetcherModule = PagerManager.getCurrentPager().getModule<KRMttVideoFeedFetcherModule>(KRMttVideoFeedFetcherModule.MODULE_NAME)
//        if(fetcherModule != null){
//            fetcherModule.fetchVideoFeeds(true){ temp ->
//                val arr = temp?.optJSONObject("common_rsp")?.optJSONArray("items")
//                val codecModule = PagerManager.getCurrentPager().acquireModule<CodecModule>(CodecModule.MODULE_NAME)
//                var videoList =  mutableListOf<VideoItem>()
//                if (arr == null) {
//                    KLog.d("LoadVideosManager","items is null")
//                } else {
//                    KLog.d("LoadVideosManager", arr.toString())
//                    for (i in 0..arr.length()) {
//                        val item = arr.optJSONObject(i)
//                        if (item != null) {
//                            val title = item.optString("title")
//                            val url = item.optString("url")
//                            val shared_sub_info = item.optJSONObject("shared_sub_info")
////                            val comment_num = shared_sub_info?.optString("comment_num") ?: ""
////                            val share_num = shared_sub_info?.optString("share_num") ?: ""
////                            val collect_num = shared_sub_info?.optString("collect_num")
//                            val tempMap = parseQBUrlToMap(url)
//                            val videoUrl = tempMap["videoUrl"]?.let { codecModule.urlDecode(it) } ?: ""
//                            val currentId = tempMap["currentId"]
//                            val userurl = tempMap["userurl"]?.let { codecModule.urlDecode(it) } ?: ""
//                            val imgUrl = tempMap["imgUrl"]?.let { codecModule.urlDecode(it) }?: ""
//                            val iPraiseNum = tempMap["iPraiseNum"]?.toInt() ?: 0
//                            val extinfo = tempMap["extinfo"]?.let { codecModule.urlDecode(it) }
//                            val sFrom = tempMap["sFrom"]?.let{ codecModule.urlDecode(it) } ?: ""
//                            val sUserIcon = tempMap["sUserIcon"]?.let{ codecModule.urlDecode(it) } ?: ""
//                            val duration = tempMap["duration"]?.toInt() ?: 0
//                            val transferInfo = tempMap["transferInfo"]?.let { codecModule.urlDecode(it) } ?: ""
//                            val (videoHeight, videoWidth) = parseVideoTransferInfo(transferInfo)
//                            // todo 解析数据填充
//                            val iCollectNum = tempMap["iCollectNum"]?.toInt() ?: 0
//                            val iCommentNum = tempMap["iCommentNum"]?.toInt() ?: 0
//                            val iShareNum = tempMap["iShareNum"]?.toInt() ?: 0
//                            videoList.add(VideoItem(videoUrl = videoUrl, imgUrl = imgUrl,
//                                duration = duration, nick = sFrom,
//                                description = title, avatar = sUserIcon, likeNum = iPraiseNum,
//                                retweetNum = iShareNum, commentNum = iCommentNum, collectNum = iCollectNum,
//                                videoWidth = videoWidth, videoHeight = videoHeight
//                            ))
//
//                        }
//                    }
//
//                    isLoad = false
//                    callback(videoList)
//                }
//            }
//        }else{
//            KLog.e("LoadVideosManager", "unable to get fetcher module")
//        }
    }

    internal fun requestFeeds(callback: (List<VideoItem>) -> Unit) {
        // 已经在请求中了
        if (isLoad) {
            callback(listOf())
            return
        }

        isLoad = true

        requestFromModule(callback)
    }

    private fun getFileName() : String {
        val fileName = "PageListVideo/video_$page.json"
//        val fileName = "WBTabPage/json/follow_0.json"
        page += 1
        if (page >= 5) {
            page = 1
        }
        return fileName
    }

    private fun parseJson(json: JSONObject):List<VideoItem> {
        val videoList =  mutableListOf<VideoItem>()

        val jsonArray = json.optJSONArray("result");
        jsonArray?.let {
            for (i in 0 until it.length()) {
                it.optJSONObject(i)?.let { item ->
//                    KLog.d("xxx", item.toString())
                    val title = item.optString("description")

                    val videoUrl = item.optString("videoUrl")
                    val imgUrl =  item.optString("imgUrl")
                    val likeNum = item.optInt("likeNum")
                    val nick = item.optString("nick")

                    val codecModule = PagerManager.getCurrentPager().acquireModule<CodecModule>(CodecModule.MODULE_NAME)

                    val sUserIcon = item.optString("avatar").let{ codecModule.urlDecode(it) }
//                    val duration = tempMap["duration"]?.toInt() ?: 0
//                    val transferInfo = tempMap["transferInfo"]?.let { codecModule.urlDecode(it) } ?: ""
//                    val (videoHeight, videoWidth) = parseVideoTransferInfo(transferInfo)

                    val iCollectNum = item.optInt("iCollectNum")
                    val iCommentNum = item.optInt("iCommentNum")
                    val iShareNum = item.optInt("iShareNum")

                    videoList.add(VideoItem(videoUrl = videoUrl, imgUrl = imgUrl,
                        duration = 0, nick = nick,
                        description = title, avatar = sUserIcon, likeNum = likeNum,
                        retweetNum = iShareNum, commentNum = iCommentNum, collectNum = iCollectNum,
                        videoWidth = 0, videoHeight = 0
                    ))

                }


            }
        }
        return videoList
    }
}