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

package com.tencent.kuikly.android.demo

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.tencent.kuikly.core.render.android.expand.KuiklyBaseView

/**
 * 卡片式风格瀑布流 Demo —— 演示如何在原生 RecyclerView 中以 View 粒度嵌入 KuiklyBaseView。
 *
 * 架构关系：
 *   NativeAppWaterfallActivity (Native Activity)
 *     └─ RecyclerView (StaggeredGridLayoutManager 两列瀑布流)
 *          └─ AppCardViewHolder
 *               └─ KuiklyBaseView(pageName="AppCardPage", pageData={...})
 *                    └─ AppCardPage (Kuikly DSL 页面)
*/
class NativeAppWaterfallActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val cardList = mutableListOf<AppCardData>()
    private val activeKuiklyViews = mutableListOf<KuiklyBaseView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_app_waterfall)
        title = "卡片式 Native 瀑布流"

        setupData()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        activeKuiklyViews.forEach { it.onResume() }
    }

    override fun onPause() {
        super.onPause()
        activeKuiklyViews.forEach { it.onPause() }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeKuiklyViews.forEach { it.onDetach() }
        activeKuiklyViews.clear()
    }

    // region Data

    private fun setupData() {
        // 数据来源：follow_0.json & follow_1.json（三端保持一致）
        val titles = listOf(
            "清晨的阳光洒在窗台上",
            "我想你",
            "夏日避暑好去处",
            "苹果发布了最新款 iPhone 15",
            "发现一个绝美露营地",
            "埃塞俄比亚耶加雪菲",
            "我家主子今天又双叒叕拆家了",
            "iPhone15预测 全系C口+钛金属边框",
            "苏州街边偶遇的蟹黄面",
            "封神票房破20亿",
            "北方人第一次见到会飞的蟑螂",
            "年轻人要多学习的真实意思",
            "禁止电动车进电梯",
            "美食推荐 超棒的小吃店",
            "拍摄夜景时的摄影技巧",
            "明星动态 反派角色造型颠覆",
            "健身日常 HIIT 高强度间歇训练",
            "好书推荐 百年孤独",
            "今天带狗狗去公园玩飞盘",
            "音乐分享 迷上了爵士乐"
        )

        val descs = listOf(
            "一杯咖啡，一本书，一段静谧的时光。生活不需要太多的喧嚣，简单才是最真实的幸福。",
            "我们这代人最擅长的，就是把\"我想你\"翻译成\"你看月亮了吗\"。",
            "分享今日打卡的冷门咖啡馆，空调超足人还少！",
            "A17 芯片性能提升显著，摄像头系统也进行了全面升级。",
            "星空太震撼了！周末去哪玩看这里。",
            "柑橘风味明显！咖啡豆评测分享。",
            "云吸猫协会常任理事的日常记录。",
            "果粉冲吗？全系C口+钛金属边框。",
            "一碗浇了8只蟹的膏黄！碳水爱好者天堂。",
            "乌尔善的选角眼光太毒了！强烈推荐。",
            "当北方人第一次见到会飞的蟑螂时的反应...南北差异太大了。",
            "领导说\"年轻人要多学习\"的真实意思：下班后免费加班三小时。",
            "建议把标语换成\"电动车进电梯会爆炸\"，恐惧比道德更有效。",
            "他们家的牛肉面汤底浓郁，牛肉软烂入味，简直是人间美味！",
            "使用三脚架和慢快门可以有效减少噪点，拍出清晰明亮的照片。",
            "据悉，某一线明星将在新剧中挑战反派角色，造型颠覆以往形象。",
            "虽然过程很艰难，但完成后的成就感无与伦比！坚持运动！",
            "被马尔克斯的魔幻现实主义深深吸引。推荐给喜欢文学的朋友们！",
            "结果它太兴奋了，直接把飞盘叼到旁边的小池塘里...还好它自己会游泳！",
            "尤其是 Louis Armstrong 的 What a Wonderful World，每次听都觉得心情特别平静。"
        )

        val nicknames = listOf(
            "晨间漫步者", "文字失语症", "快乐小番茄", "数码先知", "旅行小青蛙",
            "咖啡研究所", "喵星人日记", "数码先知", "碳水教父", "院线雷达",
            "迷惑行为大赏", "反卷战士", "人间观察员", "美食探店达人", "光影捕手",
            "娱乐小喇叭", "健身狂魔", "书香满屋", "萌宠日记", "音乐爱好者"
        )

        val avatarUrls = listOf(
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d0813ca.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45ad086d.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3ecf791d.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d77dc0ad.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8a01b17c.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/844aa82b.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/891fc305.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9bd34fff.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/ce73f60a.jpg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/7d986b3a.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/007634a8.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/b2fc4f8d.jpg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/cbf96255.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/e8fc74d5.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/7ffe7f72.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/53af4d52.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d15833c3.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/4321675f.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/b6329f72.png"
        )

        val tags = listOf(
            "生活", "文字", "探店", "科技", "露营",
            "咖啡", "萌宠", "数码", "美食", "电影",
            "搞笑", "职场", "观点", "美食", "摄影",
            "娱乐", "健身", "读书", "萌宠", "音乐"
        )

        // 使用 follow_0/follow_1 中的腾讯 CDN 图片
        val imageUrls = listOf(
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/59591ba6.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8ae4eef2.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/bee80ae7.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/cadabbca.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/0b393eef.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/610f6fc3.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/126148bf.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/3b504031.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/f36214ee.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/5cdb0696.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/8d510f14.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/6f1a911f.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/d502c511.jpeg",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/9a547ff4.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/4cdea3e6.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/c5b97de3.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/45acc362.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/20d212d8.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/533c54a0.png",
            "https://vfiles.gtimg.cn/wuji_dashboard/xy/starter/451b99b9.png"
        )

        val likeNums = listOf(
            400, 5300, 256, 800, 320,
            180, 800, 1050, 420, 3800,
            15000, 2200, 6800, 550, 600,
            1200, 700, 450, 600, 380
        )

        // 参差不齐的图片高度（dp），确保瀑布流效果
        val imageHeights = listOf(
            180f, 220f, 160f, 240f, 200f,
            250f, 170f, 210f, 190f, 230f,
            260f, 150f, 200f, 220f, 180f,
            240f, 170f, 230f, 190f, 210f
        )

        // 循环扩充到 200 条，确保 RecyclerView 必须回收复用 ViewHolder
        val totalCount = 200
        for (i in 0 until totalCount) {
            val idx = i % 20
            cardList.add(
                AppCardData(
                    imageUrl = imageUrls[idx],
                    imageHeight = imageHeights[idx],
                    title = titles[idx],
                    nickname = nicknames[idx],
                    avatarUrl = avatarUrls[idx],
                    likeCount = likeNums[idx],
                    tag = tags[idx],
                    colorIndex = i,
                    desc = descs[idx]
                )
            )
        }
    }

    // endregion

    // region RecyclerView Setup

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.recycler_view)

        // 两列瀑布流
        val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
        recyclerView.layoutManager = layoutManager

        // 间距
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
                val spacing = dp2px(3f)
                outRect.set(spacing, spacing, spacing, spacing)
            }
        })

        recyclerView.adapter = AppWaterfallAdapter()
    }

    // endregion

    // region Adapter & ViewHolder

    private inner class AppWaterfallAdapter : RecyclerView.Adapter<AppCardViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppCardViewHolder {
            val container = FrameLayout(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            return AppCardViewHolder(container)
        }

        override fun onBindViewHolder(holder: AppCardViewHolder, position: Int) {
            holder.bind(cardList[position])
        }

        override fun getItemCount(): Int = cardList.size

        override fun onViewRecycled(holder: AppCardViewHolder) {
            super.onViewRecycled(holder)
            holder.onRecycled()
        }

        override fun onViewDetachedFromWindow(holder: AppCardViewHolder) {
            super.onViewDetachedFromWindow(holder)
            holder.onDetachedFromWindow()
        }
    }

    private inner class AppCardViewHolder(
        private val container: FrameLayout
    ) : RecyclerView.ViewHolder(container) {

        private var kuiklyView: KuiklyBaseView? = null

        fun onRecycled() {
            // ViewHolder 被回收时，从 activeKuiklyViews 中移除
            kuiklyView?.let {
                activeKuiklyViews.remove(it)
            }
        }

        fun onDetachedFromWindow() {
            // ViewHolder 脱离窗口时，触发 onPause 生命周期
            kuiklyView?.onPause()
        }

        fun bind(card: AppCardData) {
            // 卡片总高度 = 封面图高度 + 额外区域（标签行+标题+描述+底部栏+内间距 ≈ 132dp）
            val totalHeightDp = card.imageHeight + 132f
            val totalHeightPx = dp2px(totalHeightDp)

            if (kuiklyView == null) {
                // 首次创建：初始化 KuiklyBaseView
                val newKuiklyView = KuiklyBaseView(container.context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        totalHeightPx
                    )
                }

                // 构造 pageData
                val pageData = mapOf<String, Any>(
                    "imageUrl" to card.imageUrl,
                    "imageHeight" to card.imageHeight.toDouble(),
                    "title" to card.title,
                    "nickname" to card.nickname,
                    "avatarUrl" to card.avatarUrl,
                    "likeCount" to card.likeCount,
                    "tag" to card.tag,
                    "colorIndex" to card.colorIndex,
                    "desc" to card.desc
                )

                // 加载 Kuikly 页面
                newKuiklyView.onAttach("", "AppCardPage", pageData)

                container.addView(newKuiklyView)
                kuiklyView = newKuiklyView
                activeKuiklyViews.add(newKuiklyView)
            } else {
                // Cell 复用：只更新数据，不销毁重建
                kuiklyView?.let { view ->
                    // 更新高度（如果高度变化了）
                    view.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        totalHeightPx
                    )
                    // 发送事件通知 KuiklyDSL 层更新数据
                    view.sendEvent("CardDataWillChanged", mapOf("data" to mapOf<String, Any>(
                        "imageUrl" to card.imageUrl,
                        "imageHeight" to card.imageHeight.toDouble(),
                        "title" to card.title,
                        "nickname" to card.nickname,
                        "avatarUrl" to card.avatarUrl,
                        "likeCount" to card.likeCount,
                        "tag" to card.tag,
                        "colorIndex" to card.colorIndex,
                        "desc" to card.desc
                    )))
                }
            }
        }
    }

    // endregion

    // region Utils

    private fun dp2px(dp: Float): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    // endregion

    // region Data Class

    data class AppCardData(
        val imageUrl: String,
        val imageHeight: Float,
        val title: String,
        val nickname: String,
        val avatarUrl: String,
        val likeCount: Int,
        val tag: String,
        val colorIndex: Int,
        val desc: String
    )

    // endregion

    companion object {
        @JvmStatic
        fun start(context: Context) {
            val starter = Intent(context, NativeAppWaterfallActivity::class.java)
            context.startActivity(starter)
        }
    }
}
