/*
 * Tencent is pleased to support the open source community by making KuiklyUI
 * available.
 * Copyright (C) 2026 Tencent. All rights reserved.
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

package com.tencent.kuikly.demo.pages.fold

import com.tencent.kuikly.core.annotations.Page
import com.tencent.kuikly.core.base.BoxShadow
import com.tencent.kuikly.core.base.Color
import com.tencent.kuikly.core.base.ViewBuilder
import com.tencent.kuikly.core.base.ViewContainer
import com.tencent.kuikly.core.base.attr.ImageUri
import com.tencent.kuikly.core.directives.vif
import com.tencent.kuikly.core.reactive.handler.observable
import com.tencent.kuikly.core.utils.PlatformUtils
import com.tencent.kuikly.core.views.GlassEffectStyle
import com.tencent.kuikly.core.views.Image
import com.tencent.kuikly.core.views.List
import com.tencent.kuikly.core.views.LiquidGlass
import com.tencent.kuikly.core.views.Text
import com.tencent.kuikly.core.views.View
import com.tencent.kuikly.demo.pages.base.BasePager

/**
 * 用 [FoldSplitStackController] 搭的「文件 / 素材库」自适应页面，观感对齐 iOS 文件 App。
 *
 * 小屏：浏览位置列表 → 当前位置的目录 → 子文件夹缩略图 → 素材详情，逐级左右滑返回。
 * 大屏：浏览位置变成左侧液态玻璃浮窗，目录钉在舞台上，子文件夹从最右滑过来盖住它，
 * 目录同时用 [FoldSplitStackController.primaryShift] 后退、
 * [FoldSplitStackController.coverShadow] 压暗，做出画廊层次。
 *
 * 组件只负责窗口尺寸与页面栈；这里的玻璃、圆角、阴影、网格列数和跳转策略全部由本页面决定。
 */
@Page("FoldSplitFilesDemo")
internal class FoldSplitFilesDemo : BasePager() {

    /** 侧栏选中的浏览位置，决定一级目录里能看到哪些文件夹和文件。 */
    var selectedLocation by observable(LOCATION_ALL)

    private val navigator = FoldSplitStackController(
        pager = this,
        layout = FoldSplitStackLayout(
            masterWidth = MASTER_WIDTH,
            masterMinWidth = MASTER_MIN_WIDTH,
            masterMaxWidth = MASTER_MAX_WIDTH,
            masterInset = MASTER_INSET,
            detailMinWidth = CONTENT_MIN_WIDTH,
            contentPad = CONTENT_PAD,
            masterTextLeft = MASTER_TEXT_LEFT,
        ),
        masterContent = { controller ->
            vif({ controller.isCompact() }) {
                filesCompactRoot(this@FoldSplitFilesDemo, controller)
            }
            vif({ !controller.isCompact() }) {
                filesSidebar(this@FoldSplitFilesDemo, controller)
            }
        },
        primaryContent = { controller ->
            filesPrimaryPage(this@FoldSplitFilesDemo, controller)
        },
    )

    /** 换浏览位置：只换一级目录，已经打开的子文件夹和详情由组件收掉。 */
    fun selectLocation(id: String) {
        val changed = selectedLocation != id
        selectedLocation = id
        navigator.selectPrimary(closeDeeperPages = changed)
    }

    /** 二级：子文件夹缩略图页。 */
    fun openFolder(folderId: String, title: String) {
        navigator.open(
            id = "folder-$selectedLocation-$folderId",
            title = title,
            data = folderId,
        ) { controller, entry ->
            filesFolderPage(this@FoldSplitFilesDemo, controller, entry.data as? String ?: "")
        }
    }

    /** 三级：素材详情页；一级目录里的散文件也直接开这一页。 */
    fun openAsset(assetId: String, title: String) {
        navigator.open(
            id = "asset-$assetId",
            title = title,
            data = assetId,
        ) { controller, entry ->
            filesAssetPage(this@FoldSplitFilesDemo, controller, entry.data as? String ?: "")
        }
    }

    override fun body(): ViewBuilder {
        val ctx = this
        return {
            attr {
                flexDirectionColumn()
                paddingTop(ctx.pageData.statusBarHeight)
                backgroundColor(FILES_BG)
            }
            FoldSplitStack(ctx.navigator)
        }
    }

    companion object {
        const val LOCATION_ALL = "all"
        const val LOCATION_RECENT = "recent"
        const val LOCATION_IMAGES = "images"
        const val LOCATION_SHARED = "shared"
        const val LOCATION_BRAND = "brand"

        const val MASTER_WIDTH = 258f
        const val MASTER_MIN_WIDTH = 200f
        const val MASTER_MAX_WIDTH = 348f
        const val MASTER_INSET = 10f
        const val MASTER_RADIUS = 26f
        const val PAGE_RADIUS = 26f
        const val CONTENT_MIN_WIDTH = 430f
        const val CONTENT_PAD = 26f
        const val COMPACT_PAD = 16f

        /** 侧栏首列文字左边缘：浮层边距 + 侧栏内边距 + 行内边距 + 图标 + 图标右边距。 */
        const val MASTER_TEXT_LEFT = MASTER_INSET + 12f + 10f + 28f + 10f

        /** 磁贴理想宽度；文件夹和缩略图共用同一套格子。 */
        const val TILE_IDEAL_WIDTH = 118f
        const val TILE_MIN_COLUMNS = 3
        const val TILE_MAX_COLUMNS = 8
        const val TILE_GAP = 10f
    }
}

// iOS 系统色。顶层属性按声明顺序初始化，颜色必须写在数据列表之前。
private val FILES_BG = Color(0xFFF2F2F7)
private val FILES_CARD = Color.WHITE
private val FILES_TEXT = Color(0xFF1C1C1E)
private val FILES_SECONDARY = Color(0xFF7C7C80)
private val FILES_FAINT = Color(0xFF8E8E93)
private val FILES_DIVIDER = Color(0xFFE4E4E8)
private val FILES_CHEVRON = Color(0xFFC7C7CC)
private val FILES_THUMB_BG = Color(0xFFE5E5EA)
private val FILES_SEARCH_BG = Color(0xFFE9E9ED)
private val FILES_BLUE = Color(0xFF0A84FF)
private val FILES_GREEN = Color(0xFF30D158)
private val FILES_PURPLE = Color(0xFFAF52DE)
private val FILES_ORANGE = Color(0xFFFF9F0A)

/** 不支持液态玻璃时侧栏浮层的底色。 */
private val FILES_GLASS_FALLBACK = Color(0xE6F8F8FA)

/** iOS 文件夹图标那种两截式蓝色。 */
private val FOLDER_BODY = Color(0xFF5AA9F8)
private val FOLDER_TAB = Color(0xFF3D97F5)

private class FilesLocation(
    val id: String,
    val icon: String,
    val title: String,
    val color: Color,
)

private class FilesFolder(
    val id: String,
    val title: String,
)

private class FilesAsset(
    val id: String,
    val title: String,
    val kind: String,
    val size: String,
    val imageName: String,
    /** 所在子文件夹，空串表示散在当前位置根下。 */
    val folder: String = "",
    val recent: Boolean = false,
    val shared: Boolean = false,
    val brand: Boolean = false,
)

private val LOCATIONS = listOf(
    FilesLocation(FoldSplitFilesDemo.LOCATION_ALL, "▦", "全部文件", FILES_BLUE),
    FilesLocation(FoldSplitFilesDemo.LOCATION_RECENT, "◷", "最近使用", FILES_FAINT),
    FilesLocation(FoldSplitFilesDemo.LOCATION_IMAGES, "▧", "图片", FILES_GREEN),
    FilesLocation(FoldSplitFilesDemo.LOCATION_SHARED, "⇄", "共享给我", FILES_PURPLE),
    FilesLocation(FoldSplitFilesDemo.LOCATION_BRAND, "◇", "品牌资料", FILES_ORANGE),
)

private val FOLDERS = listOf(
    FilesFolder("design", "设计稿"),
    FilesFolder("shoot", "拍摄素材"),
    FilesFolder("export", "导出成品"),
    FilesFolder("share", "共享给我"),
    FilesFolder("archive", "归档"),
)

private val ASSETS = listOf(
    FilesAsset("a1", "产品主视觉", "PNG", "2.4 MB", "views.png", "design", recent = true, brand = true),
    FilesAsset("a2", "组件示意图", "PNG", "920 KB", "sample.png", "design", brand = true),
    FilesAsset("a3", "猫咪封面 A", "PNG", "1.1 MB", "cat1.png", "shoot", recent = true),
    FilesAsset("a4", "猫咪封面 B", "PNG", "1.3 MB", "cat2.png", "shoot"),
    FilesAsset("a5", "熊猫吉祥物", "PNG", "680 KB", "panda.png", "shoot", brand = true),
    FilesAsset("a6", "发布会主 KV", "PNG", "3.1 MB", "views.png", "export", recent = true, brand = true),
    FilesAsset("a7", "落地页导出", "JPG", "1.6 MB", "sample.png", "export"),
    FilesAsset("a8", "联合活动稿", "PNG", "2.0 MB", "panda2.png", "share", shared = true),
    FilesAsset("a9", "合作方素材", "JPG", "760 KB", "penguin2.png", "share", shared = true, recent = true),
    FilesAsset("a10", "旧版视觉", "PNG", "4.2 MB", "weig.png", "archive"),
    FilesAsset("a11", "字体规范", "PDF", "1.8 MB", "sample.png", recent = true, brand = true),
    FilesAsset("a12", "配色说明", "PDF", "640 KB", "views.png", shared = true),
    FilesAsset("a13", "团队头像", "JPG", "340 KB", "cat1.png"),
)

private fun assetMatchesLocation(asset: FilesAsset, location: String): Boolean = when (location) {
    FoldSplitFilesDemo.LOCATION_RECENT -> asset.recent
    FoldSplitFilesDemo.LOCATION_IMAGES -> asset.kind != "PDF"
    FoldSplitFilesDemo.LOCATION_SHARED -> asset.shared
    FoldSplitFilesDemo.LOCATION_BRAND -> asset.brand
    else -> true
}

private fun locationCount(location: String): Int =
    ASSETS.count { assetMatchesLocation(it, location) }

private fun locationTitle(location: String): String =
    LOCATIONS.firstOrNull { it.id == location }?.title ?: "全部文件"

private fun folderTitle(folder: String): String =
    FOLDERS.firstOrNull { it.id == folder }?.title ?: ""

private fun assetInFolder(asset: FilesAsset, location: String, folder: String): Boolean =
    asset.folder == folder && assetMatchesLocation(asset, location)

private fun folderCount(location: String, folder: String): Int =
    ASSETS.count { assetInFolder(it, location, folder) }

/** 散在当前位置根下的文件，iOS 文件里排在文件夹分组下面。 */
private fun assetIsLoose(asset: FilesAsset, location: String): Boolean =
    asset.folder.isEmpty() && assetMatchesLocation(asset, location)

private fun hasLooseAssets(location: String): Boolean =
    ASSETS.any { assetIsLoose(it, location) }

/** 分组列表最后一行不画分隔线；读的是 observable，位置变了会自己更新。 */
private fun isLastLooseAsset(location: String, asset: FilesAsset): Boolean =
    ASSETS.lastOrNull { assetIsLoose(it, location) }?.id == asset.id

private fun findAsset(assetId: String): FilesAsset =
    ASSETS.firstOrNull { it.id == assetId } ?: ASSETS.first()

// region 布局尺寸

/** 正文两侧留白。小屏没有主窗可让，收窄一些换阅读宽度。 */
private fun filesReadingPad(controller: FoldSplitStackController): Float =
    if (controller.isCompact()) FoldSplitFilesDemo.COMPACT_PAD else FoldSplitFilesDemo.CONTENT_PAD

/** 小屏 `contentInsetLeft()` 为 0，所以这里不用再分断点。 */
private fun filesContentLeft(controller: FoldSplitStackController): Float =
    controller.contentInsetLeft() + filesReadingPad(controller)

/**
 * 网格可用宽度：组件给出没被主窗遮住的宽度，再扣掉两侧留白就是可读区。
 *
 * 只能在 `attr {}` 里调用——它读的是 observable 宽度，写在函数体里只会算一次。
 */
private fun filesGridWidth(controller: FoldSplitStackController): Float =
    (controller.detailContentWidth() - filesReadingPad(controller) * 2f).coerceAtLeast(1f)

/** 至少三列：单栏手机按理想宽度去除只会得到一列，整屏摆一个文件夹全是留白。 */
private fun filesColumns(controller: FoldSplitStackController): Int =
    (filesGridWidth(controller) / FoldSplitFilesDemo.TILE_IDEAL_WIDTH).toInt()
        .coerceIn(FoldSplitFilesDemo.TILE_MIN_COLUMNS, FoldSplitFilesDemo.TILE_MAX_COLUMNS)

/** 每个磁贴自带一条 marginRight，所以每列都要扣一份间距，否则最后一列会被挤到下一行。 */
private fun filesCardWidth(controller: FoldSplitStackController): Float {
    val columns = filesColumns(controller)
    val gaps = FoldSplitFilesDemo.TILE_GAP * columns
    return ((filesGridWidth(controller) - gaps) / columns).coerceAtLeast(1f)
}

/**
 * 一级目录被覆盖的进度。
 *
 * 组件把后退位移写在 [FoldSplitStackController.primaryShift] 上并自己做了平移，
 * 这里再用同一个值反推进度，给画廊补上圆角和投影。
 */
private fun filesGalleryProgress(controller: FoldSplitStackController): Float {
    if (controller.isCompact()) {
        return 0f
    }
    val recede = controller.galleryRecedeOffset()
    return if (recede <= 0f) 0f else (-controller.primaryShift / recede).coerceIn(0f, 1f)
}

// endregion

// region master 窗口

/** 小屏 master：铺满整屏的浏览位置列表，被 PageList 的占位窗透出来。 */
private fun ViewContainer<*, *>.filesCompactRoot(
    page: FoldSplitFilesDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(FILES_BG)
        }
        List {
            attr {
                flex(1f)
                bouncesEnable(true)
                showScrollerIndicator(false)
                backgroundColor(FILES_BG)
            }
            View {
                attr {
                    width(controller.pageWidth())
                    paddingLeft(FoldSplitFilesDemo.COMPACT_PAD)
                    paddingRight(FoldSplitFilesDemo.COMPACT_PAD)
                    paddingTop(8f)
                    paddingBottom(28f)
                }
                Text {
                    attr {
                        text("浏览")
                        marginBottom(16f)
                        fontSize(36f)
                        fontWeightBold()
                        color(FILES_TEXT)
                    }
                }
                filesSearchBar(height = 42f, fontSize = 17f)
                filesSectionTitle("我的资料库", marginTop = 22f)
                View {
                    attr {
                        borderRadius(12f)
                        overflow(true)
                        backgroundColor(FILES_CARD)
                    }
                    LOCATIONS.forEachIndexed { index, location ->
                        filesLocationRow(
                            page = page,
                            location = location,
                            compact = true,
                            showSeparator = index != LOCATIONS.lastIndex,
                        )
                    }
                }
                filesStorageBar(marginTop = 26f)
            }
        }
    }
}

/**
 * 大屏 master：iOS 风格液态玻璃浮窗。
 *
 * 组件只给了 frame，圆角靠外层 overflow 裁切，阴影用 useShadowPath 让玻璃层也能投影。
 * 右边缘的拖宽热区由组件自己铺在这层内容之上。
 */
private fun ViewContainer<*, *>.filesSidebar(
    page: FoldSplitFilesDemo,
    controller: FoldSplitStackController,
) {
    val onGlass = PlatformUtils.isLiquidGlassSupported()
    val radius = FoldSplitFilesDemo.MASTER_RADIUS
    View {
        attr {
            absolutePositionAllZero()
            borderRadius(radius)
            boxShadow(BoxShadow(0f, 6f, 20f, Color(0x24000000)), useShadowPath = true)
        }
        View {
            attr {
                absolutePositionAllZero()
                borderRadius(radius)
                overflow(true)
                backgroundColor(Color.TRANSPARENT)
            }
            if (onGlass) {
                LiquidGlass {
                    attr {
                        absolutePositionAllZero()
                        borderRadius(radius)
                        overflow(true)
                        glassEffectTintColor(Color(0x66FFFFFF))
                        glassEffectStyle(GlassEffectStyle.REGULAR)
                        touchEnable(false)
                    }
                }
            } else {
                View {
                    attr {
                        absolutePositionAllZero()
                        borderRadius(radius)
                        overflow(true)
                        backgroundColor(FILES_GLASS_FALLBACK)
                        touchEnable(false)
                    }
                }
            }
            View {
                attr {
                    flex(1f)
                    padding(left = 12f, top = 14f, right = 12f, bottom = 14f)
                }
                Text {
                    attr {
                        text("浏览")
                        marginLeft(10f)
                        marginBottom(12f)
                        fontSize(28f)
                        fontWeightBold()
                        color(FILES_TEXT)
                    }
                }
                filesSearchBar(height = 36f, fontSize = 15f, onGlass = onGlass)
                List {
                    attr {
                        flex(1f)
                        marginTop(10f)
                        bouncesEnable(false)
                        showScrollerIndicator(false)
                        backgroundColor(Color.TRANSPARENT)
                    }
                    filesSectionTitle("位置", marginTop = 8f)
                    LOCATIONS.forEach { location ->
                        filesLocationRow(
                            page = page,
                            location = location,
                            compact = false,
                            showSeparator = false,
                            onGlass = onGlass,
                        )
                    }
                    filesStorageBar(marginTop = 22f, onGlass = onGlass)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.filesLocationRow(
    page: FoldSplitFilesDemo,
    location: FilesLocation,
    compact: Boolean,
    showSeparator: Boolean,
    onGlass: Boolean = false,
) {
    View {
        attr {
            height(if (compact) 58f else 48f)
            paddingLeft(if (compact) 16f else 10f)
            paddingRight(if (compact) 14f else 12f)
            flexDirectionRow()
            alignItemsCenter()
            borderRadius(if (compact) 0f else 10f)
            backgroundColor(
                when {
                    compact -> FILES_CARD
                    page.selectedLocation != location.id -> Color.TRANSPARENT
                    // 玻璃上的选中态：压暗一层就够，别再叠不透明色把玻璃盖死。
                    onGlass -> Color(0x1A000000)
                    else -> Color(0xFFDCDCE2)
                },
            )
        }
        event {
            click {
                page.selectLocation(location.id)
            }
        }
        // iOS 列表惯例：彩色圆角小方块装图标，而不是裸字形。
        View {
            attr {
                size(28f, 28f)
                borderRadius(7f)
                allCenter()
                backgroundColor(location.color)
            }
            Text {
                attr {
                    text(location.icon)
                    fontSize(14f)
                    color(Color.WHITE)
                }
            }
        }
        Text {
            attr {
                text(location.title)
                flex(1f)
                marginLeft(10f)
                fontSize(if (compact) 17f else 15f)
                color(FILES_TEXT)
            }
        }
        View {
            attr {
                height(20f)
                paddingLeft(8f)
                paddingRight(8f)
                borderRadius(10f)
                allCenter()
                backgroundColor(
                    if (compact) Color(0xFFF2F2F7) else Color(0x18000000),
                )
            }
            Text {
                attr {
                    text(locationCount(location.id).toString())
                    fontSize(11f)
                    fontWeightMedium()
                    color(FILES_FAINT)
                }
            }
        }
        if (compact) {
            Text {
                attr {
                    text("›")
                    marginLeft(8f)
                    fontSize(22f)
                    color(FILES_CHEVRON)
                }
            }
        }
        if (showSeparator) {
            filesSeparator(52f)
        }
    }
}

private fun ViewContainer<*, *>.filesStorageBar(marginTop: Float, onGlass: Boolean = false) {
    View {
        attr {
            marginTop(marginTop)
            marginLeft(8f)
            marginRight(8f)
        }
        Text {
            attr {
                text("存储空间")
                marginBottom(8f)
                fontSize(11f)
                color(FILES_FAINT)
            }
        }
        View {
            attr {
                height(6f)
                borderRadius(3f)
                backgroundColor(if (onGlass) Color(0x33FFFFFF) else FILES_DIVIDER)
            }
            View {
                attr {
                    width(96f)
                    height(6f)
                    borderRadius(3f)
                    backgroundColor(FILES_BLUE)
                }
            }
        }
        View {
            attr {
                marginTop(8f)
                flexDirectionRow()
                alignItemsCenter()
            }
            Text {
                attr {
                    text("已使用 3.8 GB，共 10 GB")
                    flex(1f)
                    fontSize(11f)
                    color(FILES_SECONDARY)
                }
            }
            Text {
                attr {
                    text("管理")
                    fontSize(11f)
                    fontWeightMedium()
                    color(FILES_BLUE)
                }
            }
        }
    }
}

// endregion

// region 一级目录

/**
 * 一级详情：当前位置的目录。
 *
 * 大屏时被组件钉在 PageList 底下并跟着 [FoldSplitStackController.primaryShift] 后退，
 * 这里再补圆角、投影和 [FoldSplitStackController.coverShadow] 蒙版，凑成画廊层次。
 */
private fun ViewContainer<*, *>.filesPrimaryPage(
    page: FoldSplitFilesDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            absolutePositionAllZero()
            backgroundColor(FILES_BG)
            borderRadius(FoldSplitFilesDemo.PAGE_RADIUS * filesGalleryProgress(controller))
            overflow(true)
            boxShadow(
                BoxShadow(0f, 6f, 22f, Color(0x000000, filesGalleryProgress(controller) * 0.20f)),
            )
        }
        vif({ controller.isCompact() }) {
            filesCompactBackBar(controller)
        }
        List {
            attr {
                flex(1f)
                bouncesEnable(true)
                showScrollerIndicator(false)
                // Android 上 List 默认透明；嵌在 PageList 里会透出底页，必须自己铺底色。
                backgroundColor(FILES_BG)
            }
            View {
                attr {
                    // 子项必须写成整屏宽，List 默认按内容收缩，大屏会只占右侧一截。
                    width(controller.pageWidth())
                    paddingLeft(filesContentLeft(controller))
                    paddingRight(filesReadingPad(controller))
                    paddingTop(if (controller.isCompact()) 4f else 26f)
                    paddingBottom(44f)
                }
                Text {
                    attr {
                        text(locationTitle(page.selectedLocation))
                        marginBottom(6f)
                        fontSize(if (controller.isCompact()) 34f else 28f)
                        fontWeightBold()
                        color(FILES_TEXT)
                    }
                }
                filesPrimaryToolbar(page, controller)
                filesSearchBar(height = 38f, fontSize = 15f)
                filesSectionTitle("文件夹", marginTop = 22f)
                View {
                    attr {
                        flexDirectionRow()
                        flexWrapWrap()
                    }
                    FOLDERS.forEach { folder ->
                        vif({ folderCount(page.selectedLocation, folder.id) > 0 }) {
                            filesFolderTile(page, controller, folder)
                        }
                    }
                }
                vif({ hasLooseAssets(page.selectedLocation) }) {
                    View {
                        filesSectionTitle("文件", marginTop = 10f)
                        // iOS 的 inset-grouped 列表：一张白卡装所有行，行间是内缩的细分隔线。
                        View {
                            attr {
                                borderRadius(10f)
                                overflow(true)
                                backgroundColor(FILES_CARD)
                            }
                            ASSETS.forEach { asset ->
                                vif({ assetIsLoose(asset, page.selectedLocation) }) {
                                    filesRow(page, asset)
                                }
                            }
                        }
                    }
                }
            }
        }
        // 蒙版只在覆盖的滑动过程里出现，两端 coverShadow 都是 0。
        vif({ !controller.isCompact() }) {
            View {
                attr {
                    absolutePositionAllZero()
                    backgroundColor(Color(0x000000, controller.coverShadow * 0.12f))
                    touchEnable(false)
                }
            }
        }
    }
}

/** 一级目录工具条：排序与视图只是视觉，不改导航。 */
private fun ViewContainer<*, *>.filesPrimaryToolbar(
    page: FoldSplitFilesDemo,
    controller: FoldSplitStackController,
) {
    View {
        attr {
            marginBottom(12f)
            flexDirectionRow()
            alignItemsCenter()
        }
        Text {
            attr {
                text("${locationCount(page.selectedLocation)} 个项目")
                flex(1f)
                fontSize(13f)
                color(FILES_SECONDARY)
            }
        }
        filesToolbarChip("名称")
        filesToolbarChip("图标", selected = true)
        vif({ !controller.isCompact() }) {
            filesToolbarChip("＋ 新建")
        }
    }
}

private fun ViewContainer<*, *>.filesToolbarChip(
    title: String,
    selected: Boolean = false,
) {
    View {
        attr {
            height(28f)
            marginLeft(6f)
            paddingLeft(10f)
            paddingRight(10f)
            borderRadius(14f)
            allCenter()
            backgroundColor(if (selected) FILES_BLUE else FILES_CARD)
        }
        Text {
            attr {
                text(title)
                fontSize(12f)
                fontWeightMedium()
                color(if (selected) Color.WHITE else FILES_TEXT)
            }
        }
    }
}

/** 小屏一级目录顶部的返回：回到 master 根列表。 */
private fun ViewContainer<*, *>.filesCompactBackBar(controller: FoldSplitStackController) {
    View {
        attr {
            height(44f)
            paddingLeft(FoldSplitFilesDemo.COMPACT_PAD)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FILES_BG)
        }
        event {
            click {
                controller.backToMaster()
            }
        }
        Text {
            attr {
                text("‹ 浏览")
                fontSize(17f)
                color(FILES_BLUE)
            }
        }
    }
}

/** 文件夹磁贴：用两个 View 拼出文件夹形状，不依赖 emoji 在各端的渲染差异。 */
private fun ViewContainer<*, *>.filesFolderTile(
    page: FoldSplitFilesDemo,
    controller: FoldSplitStackController,
    folder: FilesFolder,
) {
    View {
        attr {
            width(filesCardWidth(controller))
            marginRight(FoldSplitFilesDemo.TILE_GAP)
            marginBottom(FoldSplitFilesDemo.TILE_GAP)
            paddingTop(12f)
            paddingBottom(12f)
            flexDirectionColumn()
            alignItemsCenter()
            borderRadius(16f)
            backgroundColor(FILES_CARD)
            boxShadow(BoxShadow(0f, 2f, 10f, Color(0x0D000000)))
        }
        event {
            click {
                page.openFolder(folder.id, folder.title)
            }
        }
        // 图形跟着格子宽度缩放，否则窄屏三列时固定宽度的文件夹会顶满格子。
        View {
            attr {
                width(filesGlyphWidth(controller))
                height(filesGlyphWidth(controller) * 0.78f)
            }
            View {
                attr {
                    positionAbsolute()
                    left(filesGlyphWidth(controller) * 0.09f)
                    top(0f)
                    width(filesGlyphWidth(controller) * 0.42f)
                    height(filesGlyphWidth(controller) * 0.16f)
                    borderRadius(3f)
                    backgroundColor(FOLDER_TAB)
                }
            }
            View {
                attr {
                    positionAbsolute()
                    left(0f)
                    top(filesGlyphWidth(controller) * 0.11f)
                    right(0f)
                    bottom(0f)
                    borderRadius(8f)
                    backgroundColor(FOLDER_BODY)
                }
            }
        }
        Text {
            attr {
                text(folder.title)
                lines(1)
                marginTop(8f)
                fontSize(14f)
                color(FILES_TEXT)
            }
        }
        View {
            attr {
                height(20f)
                marginTop(6f)
                paddingLeft(8f)
                paddingRight(8f)
                borderRadius(10f)
                allCenter()
                backgroundColor(Color(0xFFEAF3FF))
            }
            Text {
                attr {
                    text("${folderCount(page.selectedLocation, folder.id)} 项")
                    fontSize(11f)
                    fontWeightMedium()
                    color(FILES_BLUE)
                }
            }
        }
    }
}

/** 文件夹图形宽度：取格子的 62%，再夹在一个可读区间里。 */
private fun filesGlyphWidth(controller: FoldSplitStackController): Float =
    (filesCardWidth(controller) * 0.62f).coerceIn(44f, 82f)

/** iOS 列表行：缩略图 + 名称 + 副标题 + 右箭头，分隔线从缩略图右边起。 */
private fun ViewContainer<*, *>.filesRow(page: FoldSplitFilesDemo, asset: FilesAsset) {
    View {
        attr {
            height(64f)
            paddingLeft(14f)
            paddingRight(14f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FILES_CARD)
        }
        event {
            click {
                page.openAsset(asset.id, asset.title)
            }
        }
        View {
            attr {
                size(44f, 44f)
                borderRadius(8f)
                overflow(true)
                backgroundColor(FILES_THUMB_BG)
            }
            Image {
                attr {
                    absolutePositionAllZero()
                    src(ImageUri.commonAssets(asset.imageName))
                    resizeCover()
                }
            }
        }
        View {
            attr {
                flex(1f)
                marginLeft(12f)
                flexDirectionColumn()
            }
            Text {
                attr {
                    text(asset.title)
                    lines(1)
                    fontSize(17f)
                    color(FILES_TEXT)
                }
            }
            Text {
                attr {
                    text("${asset.kind} · ${asset.size}")
                    marginTop(2f)
                    fontSize(13f)
                    color(FILES_FAINT)
                }
            }
        }
        Text {
            attr {
                text("›")
                fontSize(20f)
                color(FILES_CHEVRON)
            }
        }
        vif({ !isLastLooseAsset(page.selectedLocation, asset) }) {
            filesSeparator(68f)
        }
    }
}

// endregion

// region 二级：子文件夹缩略图

private fun ViewContainer<*, *>.filesFolderPage(
    page: FoldSplitFilesDemo,
    controller: FoldSplitStackController,
    folderId: String,
) {
    filesPushedPage(controller, folderTitle(folderId)) {
        Text {
            attr {
                text(folderTitle(folderId))
                marginBottom(4f)
                fontSize(if (controller.isCompact()) 32f else 26f)
                fontWeightBold()
                color(FILES_TEXT)
            }
        }
        Text {
            attr {
                text("${locationTitle(page.selectedLocation)} · " +
                    "${folderCount(page.selectedLocation, folderId)} 项")
                marginBottom(10f)
                fontSize(13f)
                color(FILES_SECONDARY)
            }
        }
        View {
            attr {
                marginBottom(16f)
                flexDirectionRow()
            }
            filesToolbarChip("名称", selected = true)
            filesToolbarChip("日期")
            filesToolbarChip("大小")
        }
        View {
            attr {
                flexDirectionRow()
                flexWrapWrap()
            }
            ASSETS.forEach { asset ->
                vif({ assetInFolder(asset, page.selectedLocation, folderId) }) {
                    filesThumbCard(page, controller, asset)
                }
            }
        }
    }
}

/** 子文件夹里的缩略图卡片，点开进三级素材详情。 */
private fun ViewContainer<*, *>.filesThumbCard(
    page: FoldSplitFilesDemo,
    controller: FoldSplitStackController,
    asset: FilesAsset,
) {
    View {
        attr {
            width(filesCardWidth(controller))
            marginRight(FoldSplitFilesDemo.TILE_GAP)
            marginBottom(FoldSplitFilesDemo.TILE_GAP)
            flexDirectionColumn()
        }
        event {
            click {
                page.openAsset(asset.id, asset.title)
            }
        }
        View {
            attr {
                // 和文件夹共用同一套格子，缩略图跟着格子做成方形。
                height(filesCardWidth(controller))
                borderRadius(10f)
                overflow(true)
                backgroundColor(FILES_THUMB_BG)
            }
            Image {
                attr {
                    absolutePositionAllZero()
                    src(ImageUri.commonAssets(asset.imageName))
                    resizeCover()
                }
            }
            View {
                attr {
                    positionAbsolute()
                    right(6f)
                    bottom(6f)
                    height(18f)
                    paddingLeft(6f)
                    paddingRight(6f)
                    borderRadius(9f)
                    allCenter()
                    backgroundColor(Color(0x99000000))
                }
                Text {
                    attr {
                        text(asset.kind)
                        fontSize(9f)
                        fontWeightMedium()
                        color(Color.WHITE)
                    }
                }
            }
        }
        Text {
            attr {
                text(asset.title)
                lines(1)
                marginTop(8f)
                fontSize(14f)
                color(FILES_TEXT)
            }
        }
        Text {
            attr {
                text("${asset.kind} · ${asset.size}")
                lines(1)
                marginTop(2f)
                fontSize(12f)
                color(FILES_FAINT)
            }
        }
    }
}

// endregion

// region 三级：素材详情

private fun ViewContainer<*, *>.filesAssetPage(
    page: FoldSplitFilesDemo,
    controller: FoldSplitStackController,
    assetId: String,
) {
    val asset = findAsset(assetId)
    filesPushedPage(controller, asset.title) {
        // 深色预览卡：不做原生全屏 overlay，素材直接在这一页里居中铺开。
        View {
            attr {
                height(if (controller.isCompact()) 280f else 340f)
                borderRadius(18f)
                overflow(true)
                backgroundColor(Color.BLACK)
            }
            Image {
                attr {
                    absolutePositionAllZero()
                    src(ImageUri.commonAssets(asset.imageName))
                    resizeContain()
                }
            }
            View {
                attr {
                    positionAbsolute()
                    left(14f)
                    bottom(12f)
                    height(24f)
                    paddingLeft(10f)
                    paddingRight(10f)
                    borderRadius(12f)
                    allCenter()
                    backgroundColor(Color(0x66000000))
                }
                Text {
                    attr {
                        text(asset.kind)
                        fontSize(12f)
                        color(Color.WHITE)
                    }
                }
            }
        }
        Text {
            attr {
                text(asset.title)
                marginTop(18f)
                fontSize(24f)
                fontWeightBold()
                color(FILES_TEXT)
            }
        }
        Text {
            attr {
                text("${locationTitle(page.selectedLocation)} / " +
                    folderTitle(asset.folder).ifEmpty { "根目录" })
                marginTop(6f)
                marginBottom(18f)
                fontSize(13f)
                color(FILES_SECONDARY)
            }
        }
        View {
            attr {
                borderRadius(12f)
                overflow(true)
                backgroundColor(FILES_CARD)
            }
            filesMetaRow("种类", asset.kind)
            filesMetaRow("大小", asset.size)
            filesMetaRow("修改时间", "今天 11:24")
            filesMetaRow("共享给", if (asset.shared) "3 人" else "未共享", showSeparator = false)
        }
        View {
            attr {
                marginTop(16f)
                flexDirectionRow()
            }
            filesActionChip("分享", FILES_BLUE) {
                controller.close()
            }
            filesActionChip("下载", FILES_GREEN) {
                controller.close()
            }
            filesActionChip("回到目录", FILES_PURPLE) {
                controller.closeToPrimary()
            }
        }
    }
}

private fun ViewContainer<*, *>.filesMetaRow(
    title: String,
    value: String,
    showSeparator: Boolean = true,
) {
    View {
        attr {
            height(46f)
            paddingLeft(14f)
            paddingRight(14f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(FILES_CARD)
        }
        Text {
            attr {
                text(title)
                flex(1f)
                fontSize(15f)
                color(FILES_TEXT)
            }
        }
        Text {
            attr {
                text(value)
                fontSize(15f)
                color(FILES_SECONDARY)
            }
        }
        if (showSeparator) {
            filesSeparator(14f)
        }
    }
}

private fun ViewContainer<*, *>.filesActionChip(
    title: String,
    accent: Color,
    onClick: () -> Unit,
) {
    View {
        attr {
            flex(1f)
            height(44f)
            marginRight(8f)
            borderRadius(12f)
            allCenter()
            backgroundColor(FILES_CARD)
        }
        event {
            click {
                onClick()
            }
        }
        Text {
            attr {
                text(title)
                fontSize(14f)
                fontWeightMedium()
                color(accent)
            }
        }
    }
}

private fun ViewContainer<*, *>.filesActionRow(
    title: String,
    accent: Color = FILES_BLUE,
    onClick: () -> Unit,
) {
    View {
        attr {
            height(46f)
            marginTop(12f)
            borderRadius(12f)
            allCenter()
            backgroundColor(FILES_CARD)
        }
        event {
            click {
                onClick()
            }
        }
        Text {
            attr {
                text(title)
                fontSize(16f)
                color(accent)
            }
        }
    }
}

// endregion

// region 通用外壳与小控件

/**
 * 二级及以上页面统一的外壳。
 *
 * 页面自身铺满整屏，靠 [filesContentLeft] 让开主窗，所以左右滑的位移是完整一屏。
 */
private fun ViewContainer<*, *>.filesPushedPage(
    controller: FoldSplitStackController,
    title: String,
    content: ViewContainer<*, *>.() -> Unit,
) {
    View {
        attr {
            flex(1f)
            backgroundColor(FILES_BG)
            borderRadius(if (controller.isCompact()) 0f else FoldSplitFilesDemo.PAGE_RADIUS)
            overflow(true)
        }
        filesNavigationBar(controller, title)
        List {
            attr {
                flex(1f)
                bouncesEnable(true)
                showScrollerIndicator(false)
                // Android 上 List 默认透明；嵌在 PageList 里会透出底页，必须自己铺底色。
                backgroundColor(FILES_BG)
            }
            View {
                attr {
                    width(controller.pageWidth())
                    paddingLeft(filesContentLeft(controller))
                    paddingRight(filesReadingPad(controller))
                    paddingTop(6f)
                    paddingBottom(44f)
                }
                content()
            }
        }
    }
}

private fun ViewContainer<*, *>.filesNavigationBar(
    controller: FoldSplitStackController,
    title: String,
) {
    View {
        attr {
            height(52f)
            // 顶栏也铺满，标题和返回按钮落在主窗右侧的可读区。
            paddingLeft(controller.contentInsetLeft())
            flexDirectionRow()
            alignItemsCenter()
            justifyContentCenter()
            backgroundColor(FILES_BG)
        }
        Text {
            attr {
                text(title)
                fontSize(17f)
                fontWeightSemiBold()
                color(FILES_TEXT)
            }
        }
        View {
            attr {
                positionAbsolute()
                left(controller.contentInsetLeft() + 14f)
                top(9f)
                height(34f)
                paddingLeft(8f)
                paddingRight(11f)
                borderRadius(17f)
                flexDirectionRow()
                alignItemsCenter()
                backgroundColor(FILES_CARD)
                boxShadow(BoxShadow(0f, 1f, 4f, Color(0x14000000)))
            }
            event {
                click {
                    controller.close()
                }
            }
            Text {
                attr {
                    text("‹")
                    marginTop(-2f)
                    fontSize(26f)
                    color(FILES_BLUE)
                }
            }
            Text {
                attr {
                    text("返回")
                    fontSize(14f)
                    color(FILES_BLUE)
                }
            }
        }
    }
}

private fun ViewContainer<*, *>.filesSearchBar(
    height: Float,
    fontSize: Float,
    onGlass: Boolean = false,
) {
    View {
        attr {
            height(height)
            paddingLeft(11f)
            paddingRight(10f)
            borderRadius(11f)
            flexDirectionRow()
            alignItemsCenter()
            backgroundColor(if (onGlass) Color(0x33FFFFFF) else FILES_SEARCH_BG)
        }
        Text {
            attr {
                text("⌕")
                fontSize(fontSize)
                color(FILES_SECONDARY)
            }
        }
        Text {
            attr {
                text("搜索文件与文件夹")
                flex(1f)
                marginLeft(7f)
                fontSize(fontSize)
                color(FILES_SECONDARY)
            }
        }
        Text {
            attr {
                text("🎙")
                fontSize(13f)
                color(FILES_FAINT)
            }
        }
    }
}

private fun ViewContainer<*, *>.filesSectionTitle(title: String, marginTop: Float) {
    Text {
        attr {
            text(title)
            marginLeft(4f)
            marginTop(marginTop)
            marginBottom(8f)
            fontSize(13f)
            color(FILES_FAINT)
        }
    }
}

private fun ViewContainer<*, *>.filesSeparator(left: Float) {
    View {
        attr {
            positionAbsolute()
            left(left)
            right(0f)
            bottom(0f)
            height(0.33f)
            backgroundColor(FILES_DIVIDER)
        }
    }
}

// endregion
