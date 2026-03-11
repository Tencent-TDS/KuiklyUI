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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.AnimatedContentTransitionScope
import com.tencent.kuikly.compose.animation.fadeIn
import com.tencent.kuikly.compose.animation.fadeOut
import com.tencent.kuikly.compose.animation.slideInHorizontally
import com.tencent.kuikly.compose.animation.slideOutHorizontally
import com.tencent.kuikly.compose.animation.togetherWith
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Button
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Scaffold
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.navigation.LocalNavHostController
import com.tencent.kuikly.compose.material3.navigation.NavHost
import com.tencent.kuikly.compose.material3.navigation.rememberNavHostController
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextAlign
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

/**
 * NavHost Demo - Demonstrates cross-platform NavHost usage.
 *
 * Shows:
 * - Basic navigation with NavHost + NavHostController
 * - Route parameters (e.g. "detail/{id}")
 * - System back button handling
 * - Animated transitions between screens
 * - Navigation via LocalNavHostController CompositionLocal
 */
@Page("NavHostDemo")
internal class NavHostDemo : ComposeContainer() {
    override fun willInit() {
        super.willInit()
        setContent {
            ComposeNavigationBar("NavHost Demo") {
                NavHostDemoContent()
            }
        }
    }
}

@Composable
private fun NavHostDemoContent() {
    val navController = rememberNavHostController()

    NavHost(
        navController = navController,
        startDestination = "home",
        transitionSpec = {
            // Slide in from right + fade in, slide out to left + fade out
            (slideInHorizontally { it } + fadeIn()) togetherWith
                    (slideOutHorizontally { -it } + fadeOut())
        }
    ) {
        composable("home") {
            HomeScreen()
        }
        composable("list") {
            ListScreen()
        }
        composable("detail/{id}") { entry ->
            val id = entry.getStringArgument("id") ?: "unknown"
            DetailScreen(id)
        }
        composable("settings") {
            SettingsScreen()
        }
    }
}

// ===== Screens =====

@Composable
private fun HomeScreen() {
    val navController = LocalNavHostController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF0F4FF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Header icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF2196F3)),
            contentAlignment = Alignment.Center
        ) {
            Text("🏠", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Home",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Cross-platform NavHost Demo",
            fontSize = 14.sp,
            color = Color(0xFF666666)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation buttons
        NavButton(
            text = "Go to List",
            color = Color(0xFF4CAF50),
            onClick = { navController.navigate("list") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Go to Detail (id=42)",
            color = Color(0xFFFF9800),
            onClick = { navController.navigate("detail/42") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Go to Settings",
            color = Color(0xFF9C27B0),
            onClick = { navController.navigate("settings") }
        )
    }
}

@Composable
private fun ListScreen() {
    val navController = LocalNavHostController.current
    val items = remember { (1..10).map { "Item #$it" } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5FFF5))
            .padding(16.dp)
    ) {
        Text(
            text = "List Screen",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(16.dp))

        items.forEachIndexed { index, item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { navController.navigate("detail/${index + 1}") },
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF4CAF50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = item,
                        fontSize = 16.sp,
                        color = Color(0xFF333333)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(id: String) {
    val navController = LocalNavHostController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF8F0))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFFF9800)),
            contentAlignment = Alignment.Center
        ) {
            Text("📄", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Detail Screen",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Route parameter id = $id",
            fontSize = 16.sp,
            color = Color(0xFF666666)
        )

        Spacer(modifier = Modifier.height(32.dp))

        NavButton(
            text = "Go to Settings",
            color = Color(0xFF9C27B0),
            onClick = { navController.navigate("settings") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Back to Home (clear stack)",
            color = Color(0xFFF44336),
            onClick = {
                navController.navigate("home") {
                    popUpTo("home") { inclusive = true }
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        NavButton(
            text = "Go Back",
            color = Color(0xFF607D8B),
            onClick = { navController.popBackStack() }
        )
    }
}

@Composable
private fun SettingsScreen() {
    val navController = LocalNavHostController.current
    var counter by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F0FF))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF9C27B0)),
            contentAlignment = Alignment.Center
        ) {
            Text("⚙", fontSize = 36.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Settings",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1A1A2E)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Counter to demonstrate state preservation
        Text(
            text = "Counter: $counter",
            fontSize = 20.sp,
            color = Color(0xFF9C27B0),
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { counter-- },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                )
            ) {
                Text("-", fontSize = 18.sp)
            }

            Button(
                onClick = { counter++ },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF9C27B0)
                )
            ) {
                Text("+", fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        NavButton(
            text = "Go Back",
            color = Color(0xFF607D8B),
            onClick = { navController.popBackStack() }
        )
    }
}

// ===== Shared Components =====

@Composable
private fun NavButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
