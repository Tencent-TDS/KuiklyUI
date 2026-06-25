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

package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.interaction.MutableInteractionSource
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.itemsIndexed
import com.tencent.kuikly.compose.foundation.shape.CircleShape
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.DialogProperties
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ==================== Page Entry ====================

@Page("SettingScreenDemo")
internal class SettingScreenDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent { SettingPageBody() }
    }
}

// ==================== ScreenModel (模拟 SettingScreenModel) ====================

private class DemoSettingScreenModel {
    // 模拟源码：初始值为空/默认，loadData 异步返回后才填充，
    // 触发 collectAsState 重组，使 LazyColumn 中 if(isLoggedIn && isMigratedToOp) 分支
    // 从无到有插入整块会员/积分/工作空间区域。

    private val _planName = MutableStateFlow("")
    val planName: StateFlow<String> = _planName

    private val _endTimeStr = MutableStateFlow("")
    val endTimeStr: StateFlow<String> = _endTimeStr

    private val _userName = MutableStateFlow("")
    val userName: StateFlow<String> = _userName

    private val _avatarUrl = MutableStateFlow("")
    val avatarUrl: StateFlow<String> = _avatarUrl

    private val _workspaces = MutableStateFlow(emptyList<DemoWorkspaceMeta>())
    val workspaces: StateFlow<List<DemoWorkspaceMeta>> = _workspaces

    private val _hasTeam = MutableStateFlow(false)
    val hasTeam: StateFlow<Boolean> = _hasTeam

    private val _workspaceMeta = MutableStateFlow<DemoWorkspaceMeta?>(null)
    val workspaceMeta: StateFlow<DemoWorkspaceMeta?> = _workspaceMeta

    // 模拟源码：isMigratedToOp 初始为 false，接口返回后变 true，
    // 使 if(isLoggedIn && isMigratedToOp) 整块从不渲染到渲染
    private val _isMigratedToOp = MutableStateFlow(false)
    val isMigratedToOp: StateFlow<Boolean> = _isMigratedToOp

    private val _appearanceMode = MutableStateFlow(DemoAppearanceMode.FOLLOW_SYSTEM)
    val appearanceMode: StateFlow<DemoAppearanceMode> = _appearanceMode

    private val _isMavis = MutableStateFlow(false)
    val isMavis: StateFlow<Boolean> = _isMavis

    // 模拟源码：isLoggedIn 初始为 true（AccountManager.isLoginFlow 的初始值）
    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    private val _creditBalance = MutableStateFlow(0)
    val creditBalance: StateFlow<Int> = _creditBalance

    private var isDataLoaded = false

    /**
     * 模拟源码 loadData()：
     * 1. walletApi.refreshUserExtraInfo() — 网络请求，返回后更新 membershipInfo/workspaces
     * 2. walletApi.getSubscribeInfo() — 网络请求，返回后更新 subscribeInfo
     *
     * 真实场景中这些接口返回有延迟，返回后 StateFlow 更新触发重组，
     * 导致 LazyColumn 中 if(isLoggedIn && isMigratedToOp) 条件分支从不存在变为存在，
     * 整块 item（工作空间/升级/积分/使用量）被插入。
     */
    suspend fun loadData() {
        if (isDataLoaded) return

        // 所有数据一次性返回，无延迟（模拟本地缓存命中）
        _userName.value = "MiniMax Tester"
        _avatarUrl.value = ""
        _planName.value = "Pro"
        _endTimeStr.value = "2026-12-31 到期"
        _creditBalance.value = 1280
        _workspaces.value = listOf(
            DemoWorkspaceMeta(0L, "个人空间", 1),
            DemoWorkspaceMeta(1001L, "Kuikly 测试团队", 2),
            DemoWorkspaceMeta(1002L, "研发三组", 3),
            DemoWorkspaceMeta(1003L, "设计协作空间", 3),
        )
        _workspaceMeta.value = DemoWorkspaceMeta(0L, "个人空间", 1)
        _hasTeam.value = true
        // 关键：isMigratedToOp 从 false 变 true，
        // 触发 LazyColumn 中 if(isLoggedIn && isMigratedToOp) 分支从无到有
        _isMigratedToOp.value = true

        isDataLoaded = true
    }

    /**
     * 模拟源码 refreshUserExtraInfo()
     */
    suspend fun refreshUserExtraInfo() {
        delay(600)
        _creditBalance.value = _creditBalance.value + 100
        _planName.value = if (_planName.value == "Pro") "Pro+" else "Pro"
    }

    fun updateWorkspace(id: Long) {
        val found = _workspaces.value.find { it.workspaceId == id }
        if (found != null) {
            _workspaceMeta.value = found
        }
    }

    fun updateAppearanceMode(mode: DemoAppearanceMode) {
        _appearanceMode.value = mode
    }

    fun toggleChatMode() {
        _isMavis.value = !_isMavis.value
    }

    fun logout() {
        _isLoggedIn.value = false
        _userName.value = ""
        _avatarUrl.value = ""
    }

    fun login() {
        _isLoggedIn.value = true
        _userName.value = "MiniMax Tester"
    }

    fun onCleared() { /* no-op */ }
}

private data class DemoWorkspaceMeta(
    val workspaceId: Long,
    val workspaceName: String,
    val role: Int, // 1=owner, 2=admin, 3=member
)

private enum class DemoAppearanceMode(val label: String) {
    FOLLOW_SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

// ==================== Colors (模拟 colorResource) ====================

private val BgGroupedPrimary = Color(0xFFF6F7F9)
private val BgGroupedSecondary = Color.White
private val TextPrimary = Color(0xFF202124)
private val TextSecondary = Color(0xFF777D87)
private val TextTertiary = Color(0xFFB4BAC3)
private val TextAccent = Color(0xFF356BFF)
private val IconPrimary = Color(0xFF4D5968)
private val IconTertiary = Color(0xFFB4BAC3)
private val BorderDefault = Color(0xFFE6E8EC)
private val BgSecondary = Color(0xFFECEFF1)

// ==================== Main Body (完美复刻 SettingPageBody) ====================

@Composable
private fun SettingPageBody() {
    val screenModel = remember { DemoSettingScreenModel() }
    DisposableEffect(Unit) {
        onDispose { screenModel.onCleared() }
    }
    val planName by screenModel.planName.collectAsState()
    val endTimeStr by screenModel.endTimeStr.collectAsState()
    val userName by screenModel.userName.collectAsState()
    val avatarUrl by screenModel.avatarUrl.collectAsState()
    val workspaces by screenModel.workspaces.collectAsState()
    val hasTeam by screenModel.hasTeam.collectAsState()
    val workspaceMeta by screenModel.workspaceMeta.collectAsState()
    val isMigratedToOp by screenModel.isMigratedToOp.collectAsState()
    val appearanceMode by screenModel.appearanceMode.collectAsState()
    val isMavis by screenModel.isMavis.collectAsState()
    val isLoggedIn by screenModel.isLoggedIn.collectAsState()
    val creditBalance by screenModel.creditBalance.collectAsState()
    val loginScope = rememberCoroutineScope()

    var showLogoutDialog by remember { mutableStateOf(false) }
    var showAppearanceModeMenu by remember { mutableStateOf(false) }
    var showWorkspaceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        screenModel.loadData()
    }

    val bgColor = BgGroupedPrimary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
    ) {
        // CustomTopBar
        CustomTopBarDemo(
            onBackClick = { /* routerModule?.closePage() */ },
            backgroundColor = bgColor,
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(bgColor)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            forceClearIgnoreOffset = true,
        ) {
            // 头像和用户名
            item {
                UserProfileSection(
                    username = userName,
                    isLoggedIn = isLoggedIn,
                    onProfileClick = {
                        if (!isLoggedIn) {
                            screenModel.login()
                        }
                    },
                )
            }

            if (isLoggedIn && isMigratedToOp) {
                // 用户信息行（已迁移才显示）
                item {
                    Spacer(modifier = Modifier.height(36.dp))
                    val currentWorkspaceName =
                        workspaceMeta?.workspaceName?.takeIf { it.isNotBlank() } ?: "个人空间"
                    SettingSectionCard {
                        SettingValueListItem(
                            iconText = "W",
                            title = currentWorkspaceName,
                            value = "",
                            showTrailingArrow = false,
                            trailingText = if (hasTeam && !isMavis) "切换" else null,
                            onClick = {
                                if (!isMavis) {
                                    showWorkspaceSheet = true
                                }
                            },
                        )
                        SettingSectionDivider()
                        // 升级/订阅
                        SettingValueListItem(
                            iconText = "S",
                            title = "升级/订阅",
                            value = planName,
                            titleColor = TextAccent,
                            onClick = { /* walletApi.openPurchaseScreen */ },
                        )
                        SettingSectionDivider()
                        // 积分
                        SettingValueListItem(
                            iconText = "C",
                            title = "积分",
                            value = "$creditBalance",
                            onClick = { /* walletApi.openPurchaseScreen */ },
                        )
                        SettingSectionDivider()
                        // 使用量
                        SettingValueListItem(
                            iconText = "U",
                            title = "使用量",
                            value = "",
                            onClick = { /* routerModule?.openPage("KuiklyUsage") */ },
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                SettingSectionTitle(text = "通用")
                Spacer(modifier = Modifier.height(8.dp))
                // 通用行
                SettingSectionCard {
                    SettingSectionItem(
                        iconText = "P",
                        title = "个人信息",
                        onClick = {
                            if (!isLoggedIn) {
                                screenModel.login()
                            }
                        },
                    )
                    SettingSectionDivider()
                    SettingSectionItem(
                        iconText = "A",
                        title = "外观",
                        value = appearanceMode.label,
                        onClick = { showAppearanceModeMenu = true },
                    )
                    SettingSectionDivider()
                    SettingSectionItem(
                        iconText = "G",
                        title = "偏好设置",
                        onClick = { /* routerModule?.openPage("KuiklySettingPreference") */ },
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                SettingSectionTitle(text = "关于 MiniMax")
                Spacer(modifier = Modifier.height(8.dp))
                // 关于 MiniMax
                SettingSectionCard {
                    SettingSectionItem(
                        iconText = "I",
                        title = "关于 MiniMax",
                        onClick = { /* routerModule?.openPage("KuiklyAboutApp") */ },
                    )
                    SettingSectionDivider()
                    SettingSectionItem(
                        iconText = "R",
                        title = "举报违规内容",
                        onClick = { /* routerModule?.openPage("KuiklyReport") */ },
                    )
                    SettingSectionDivider()
                    SettingSectionItem(
                        iconText = "E",
                        title = "反馈使用问题",
                        onClick = { /* mailto:contact@minimax.io */ },
                    )
                }
            }

            // 对话模式切换
            if (isLoggedIn) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    SettingSectionCard {
                        SettingSectionItem(
                            iconText = "M",
                            title = if (isMavis) "切换到经典模式" else "切换到 Mavis 模式",
                            showTrailingArrow = false,
                            onClick = {
                                screenModel.toggleChatMode()
                            },
                        )
                    }
                }
            }

            // 退出登录
            if (isLoggedIn) {
                item {
                    Spacer(modifier = Modifier.height(32.dp))
                    SettingSectionCard {
                        SettingSectionItem(
                            iconText = "L",
                            title = "退出登录",
                            showTrailingArrow = false,
                            onClick = { showLogoutDialog = true },
                        )
                    }
                }
            }
        }
    }

    // AppearanceModePopupMenu
    if (showAppearanceModeMenu) {
        AppearanceModePopupMenu(
            selectedMode = appearanceMode,
            onSelect = { mode ->
                screenModel.updateAppearanceMode(mode)
                showAppearanceModeMenu = false
            },
            onDismiss = { showAppearanceModeMenu = false },
        )
    }

    // WorkspaceSelectSheet（用 Dialog 模拟 UikitSheet）
    if (showWorkspaceSheet) {
        Dialog(
            onDismissRequest = { showWorkspaceSheet = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BgGroupedPrimary, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .padding(top = 16.dp),
                ) {
                    // Title bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "选择工作空间",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                        )
                    }
                    // Sheet content - 嵌套 LazyColumn
                    WorkspaceSelectSheetContent(
                        items = workspaces,
                        selectedWorkspaceId = workspaceMeta?.workspaceId ?: 0L,
                        onSelect = { selectedId ->
                            screenModel.updateWorkspace(selectedId)
                            showWorkspaceSheet = false
                        },
                    )
                }
            }
        }
    }

    // LogoutDialog
    if (showLogoutDialog) {
        Dialog(
            onDismissRequest = { showLogoutDialog = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
            ),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showLogoutDialog = false },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 40.dp)
                        .background(BgGroupedSecondary, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("确认退出登录？", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(BgSecondary, RoundedCornerShape(10.dp))
                                .clickable(onClick = { showLogoutDialog = false })
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("取消", fontSize = 15.sp, color = TextPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(Color(0xFFFF4444), RoundedCornerShape(10.dp))
                                .clickable(onClick = {
                                    screenModel.logout()
                                    showLogoutDialog = false
                                })
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("退出", fontSize = 15.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

// ==================== CustomTopBar ====================

@Composable
private fun CustomTopBarDemo(
    onBackClick: () -> Unit,
    backgroundColor: Color,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(backgroundColor)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBackClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "<", fontSize = 20.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

// ==================== UserProfileSection ====================

@Composable
private fun UserProfileSection(
    username: String,
    isLoggedIn: Boolean,
    onProfileClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onProfileClick,
            )
            .padding(top = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (isLoggedIn) Color(0xFF4D7CFE) else Color(0xFFCDD2D9)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (isLoggedIn) "M" else "?",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text = username.ifEmpty { "登录" },
            fontSize = 20.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ==================== Section Components ====================

@Composable
private fun SettingSectionTitle(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal,
        color = TextSecondary,
        modifier = Modifier.padding(horizontal = 12.dp),
    )
}

@Composable
private fun SettingSectionCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgGroupedSecondary, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingSectionDivider() {
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 32.dp)
            .height(0.5.dp)
            .background(BorderDefault),
    )
}

// SettingSectionItem（对应源码中的通用设置行）
@Composable
private fun SettingSectionItem(
    iconText: String,
    title: String,
    value: String = "",
    showTrailingArrow: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(Color(0xFFE9EDF5), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(iconText, fontSize = 12.sp, color = IconPrimary, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = title,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = TextSecondary,
            )
            Spacer(modifier = Modifier.size(4.dp))
        }
        if (showTrailingArrow) {
            Text(text = ">", fontSize = 18.sp, color = IconTertiary)
        }
    }
}

// SettingValueListItem（对应源码中的带 value 的设置行，如工作空间、积分等）
@Composable
private fun SettingValueListItem(
    iconText: String,
    title: String,
    value: String,
    showTrailingArrow: Boolean = true,
    trailingText: String? = null,
    titleColor: Color? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(0xFFE9EDF5), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(iconText, fontSize = 12.sp, color = IconPrimary, fontWeight = FontWeight.Bold)
            }
            Text(
                text = title,
                fontSize = 17.sp,
                color = titleColor ?: TextPrimary,
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (value.isNotBlank()) {
                Text(
                    text = value,
                    fontSize = 17.sp,
                    color = TextSecondary,
                )
            }
            if (trailingText != null) {
                Text(
                    text = trailingText,
                    fontSize = 14.sp,
                    color = TextAccent,
                )
            } else if (showTrailingArrow) {
                Text(text = ">", fontSize = 18.sp, color = IconTertiary)
            } else {
                Spacer(modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ==================== WorkspaceSelectSheetContent（嵌套 LazyColumn） ====================

@Composable
private fun WorkspaceSelectSheetContent(
    items: List<DemoWorkspaceMeta>,
    selectedWorkspaceId: Long,
    onSelect: (Long) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgGroupedPrimary)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(BgGroupedSecondary, RoundedCornerShape(16.dp)),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
            ) {
                itemsIndexed(items = items, key = { _, item -> item.workspaceId }) { index, item ->
                    val roleText = when (item.role) {
                        1 -> "所有者"
                        2 -> "管理员"
                        3 -> "成员"
                        else -> ""
                    }
                    val displayName = item.workspaceName.takeIf { it.isNotBlank() } ?: "个人空间"
                    WorkspaceSelectRow(
                        title = displayName,
                        selected = item.workspaceId == selectedWorkspaceId,
                        roleText = roleText,
                        onClick = { onSelect(item.workspaceId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkspaceSelectRow(
    title: String,
    selected: Boolean,
    roleText: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                fontSize = 17.sp,
                color = TextPrimary,
            )
            if (roleText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .background(BgSecondary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = roleText,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = TextTertiary,
                    )
                }
            }
        }
        if (selected) {
            Text(text = "✓", fontSize = 16.sp, color = IconPrimary)
        } else {
            Spacer(modifier = Modifier.size(20.dp))
        }
    }
}

// ==================== AppearanceModePopupMenu ====================

@Composable
private fun AppearanceModePopupMenu(
    selectedMode: DemoAppearanceMode,
    onSelect: (DemoAppearanceMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val items = listOf(
        DemoAppearanceMode.FOLLOW_SYSTEM,
        DemoAppearanceMode.LIGHT,
        DemoAppearanceMode.DARK,
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            scrimColor = Color.Transparent,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 80.dp)
                    .background(
                        color = BgGroupedSecondary,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    ),
            ) {
                items.forEachIndexed { index, mode ->
                    val isLast = index == items.lastIndex
                    AppearanceModeRow(
                        text = mode.label,
                        selected = selectedMode == mode,
                        showDivider = !isLast,
                        onClick = { onSelect(mode) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppearanceModeRow(
    text: String,
    selected: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(start = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = text,
                fontSize = 17.sp,
                color = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Text(text = "✓", fontSize = 16.sp, color = IconPrimary)
            } else {
                Spacer(modifier = Modifier.size(16.dp))
            }
        }
        if (showDivider) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(0.5.dp)
                    .background(BorderDefault),
            )
        }
    }
}
