package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.foundation.text.BasicTextField
import com.tencent.kuikly.compose.foundation.text.KeyboardActions
import com.tencent.kuikly.compose.foundation.text.KeyboardOptions
import com.tencent.kuikly.compose.foundation.text.autoHideKeyboardOnImeAction
import com.tencent.kuikly.compose.foundation.text.keepFocusOnKeyboardDismiss
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.SolidColor
import com.tencent.kuikly.compose.ui.platform.LocalSoftwareKeyboardController
import com.tencent.kuikly.compose.ui.text.TextStyle
import com.tencent.kuikly.compose.ui.text.input.ImeAction
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page

@Page("HideKeyboardTestDemo")
class HideKeyboardTestDemo : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            HideKeyboardTestContent()
        }
    }
}

@Composable
fun HideKeyboardTestContent() {
    val keyboardController = LocalSoftwareKeyboardController.current
    var text1 by remember { mutableStateOf("") }
    var text2 by remember { mutableStateOf("") }
    var statusText1 by remember { mutableStateOf("状态：等待操作") }
    var statusText2 by remember { mutableStateOf("状态：等待操作") }
    var keepFocus1 by remember { mutableStateOf(true) }
    var keepFocus2 by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = "Hide Keyboard 测试",
            style = TextStyle(fontSize = 20.sp, color = Color.Black)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ========== 输入框1 ==========
        Text(
            text = "输入框1：autoHideKeyboardOnImeAction + keepFocusOnKeyboardDismiss",
            style = TextStyle(fontSize = 12.sp, color = Color.DarkGray)
        )

        // 输入框1 的 keepFocus 开关
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    if (keepFocus1) Color(0xFF4CAF50) else Color(0xFFF44336),
                    RoundedCornerShape(8.dp)
                )
                .clickable {
                    keepFocus1 = !keepFocus1
                    statusText1 = "状态：keepFocus1 = $keepFocus1"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (keepFocus1) "keepFocus1 = true（收键盘保光标）" else "keepFocus1 = false（收键盘失焦）",
                style = TextStyle(fontSize = 12.sp, color = Color.White)
            )
        }

        Text(
            text = statusText1,
            style = TextStyle(fontSize = 12.sp, color = Color.Gray)
        )

        BasicTextField(
            value = text1,
            onValueChange = { text1 = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .padding(12.dp)
                .autoHideKeyboardOnImeAction(true)
                .keepFocusOnKeyboardDismiss(keepFocus1),
            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
            cursorBrush = SolidColor(Color.Blue),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    statusText1 = "状态：点击Done1，keepFocus1=$keepFocus1"
                }
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ========== 输入框2 ==========
        Text(
            text = "输入框2：autoHideKeyboardOnImeAction + keepFocusOnKeyboardDismiss",
            style = TextStyle(fontSize = 12.sp, color = Color.DarkGray)
        )

        // 输入框2 的 keepFocus 开关
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    if (keepFocus2) Color(0xFF4CAF50) else Color(0xFFF44336),
                    RoundedCornerShape(8.dp)
                )
                .clickable {
                    keepFocus2 = !keepFocus2
                    statusText2 = "状态：keepFocus2 = $keepFocus2"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (keepFocus2) "keepFocus2 = true（收键盘保光标）" else "keepFocus2 = false（收键盘失焦）",
                style = TextStyle(fontSize = 12.sp, color = Color.White)
            )
        }

        Text(
            text = statusText2,
            style = TextStyle(fontSize = 12.sp, color = Color.Gray)
        )

        BasicTextField(
            value = text2,
            onValueChange = { text2 = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                .padding(12.dp)
                .autoHideKeyboardOnImeAction(true)
                .keepFocusOnKeyboardDismiss(keepFocus2),
            textStyle = TextStyle(fontSize = 16.sp, color = Color.Black),
            cursorBrush = SolidColor(Color.Blue),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    statusText2 = "状态：点击Done2，keepFocus2=$keepFocus2"
                }
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 手动 hide 按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                .clickable {
                    keyboardController?.hide()
                    statusText1 = "状态：手动hide()"
                    statusText2 = "状态：手动hide()"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "手动调用 hide()",
                style = TextStyle(fontSize = 14.sp, color = Color.White)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 手动 show 按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(Color(0xFF2196F3), RoundedCornerShape(8.dp))
                .clickable {
                    keyboardController?.show()
                    statusText1 = "状态：手动show()，键盘应重新弹出"
                    statusText2 = "状态：手动show()，键盘应重新弹出"
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "手动调用 show()",
                style = TextStyle(fontSize = 14.sp, color = Color.White)
            )
        }
    }
}
