package com.tencent.kuikly.demo.pages.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.animation.core.animateFloatAsState
import com.tencent.kuikly.compose.foundation.Canvas
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.selection.toggleable
import com.tencent.kuikly.compose.foundation.selection.triStateToggleable
import com.tencent.kuikly.compose.material3.ButtonDefaults
import com.tencent.kuikly.compose.material3.Checkbox
import com.tencent.kuikly.compose.material3.CircularProgressIndicator
import com.tencent.kuikly.compose.material3.LinearProgressIndicator
import com.tencent.kuikly.compose.material3.LocalContentColor
import com.tencent.kuikly.compose.material3.MaterialTheme
import com.tencent.kuikly.compose.material3.ProgressIndicatorDefaults
import com.tencent.kuikly.compose.material3.Slider
import com.tencent.kuikly.compose.material3.Snackbar
import com.tencent.kuikly.compose.material3.SnackbarDuration
import com.tencent.kuikly.compose.material3.SnackbarHost
import com.tencent.kuikly.compose.material3.SnackbarHostState
import com.tencent.kuikly.compose.material3.SnackbarResult
import com.tencent.kuikly.compose.material3.SnackbarVisuals
import com.tencent.kuikly.compose.material3.Switch
import com.tencent.kuikly.compose.material3.SwitchDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextButton
import com.tencent.kuikly.compose.material3.TriStateCheckbox
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.geometry.Offset
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.graphics.Path
import com.tencent.kuikly.compose.ui.graphics.StrokeCap
import com.tencent.kuikly.compose.ui.graphics.drawscope.scale
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.semantics.Role
import com.tencent.kuikly.compose.ui.semantics.contentDescription
import com.tencent.kuikly.compose.ui.semantics.semantics
import com.tencent.kuikly.compose.ui.state.ToggleableState
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.core.annotations.Page
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@Page("material_demo")
internal class MaterialDemo : ComposeContainer() {

    override fun willInit() {
        setContent {
            DemoScaffold(
                title = "Material Demo",
                back = true
            ) {
                Text("Snackbar")
                SimpleSnackbar()
                IndefiniteSnackbar()
                CustomSnackbar()
                CoroutinesSnackbar()
                MultilineSnackbar()

                Text("Checkbox")
                CheckboxSample()
                SecondaryText("Checkbox with Text")
                CheckboxWithTextSample()
                SecondaryText("Tri-State Checkbox")
                TriStateCheckboxSample()

                Text("Switch")
                SwitchSample()
                SecondaryText("Switch with Thumb Icon")
                SwitchWithThumbIconSample()

                Text("Linear Progress Indicator")
                LinearProgressIndicatorSample()
                SecondaryText("Legacy Linear Progress Indicator")
                LegacyLinearProgressIndicatorSample()
                SecondaryText("Indeterminate Linear Progress Indicator")
                IndeterminateLinearProgressIndicatorSample()
                SecondaryText("Legacy Indeterminate Linear Progress Indicator")
                LegacyIndeterminateLinearProgressIndicatorSample()

                Text("Circular Progress Indicator")
                CircularProgressIndicatorSample()
                SecondaryText("Legacy Circular Progress Indicator")
                LegacyCircularProgressIndicatorSample()
                SecondaryText("Indeterminate Circular Progress Indicator")
                IndeterminateCircularProgressIndicatorSample()
                SecondaryText("Legacy Indeterminate Circular Progress Indicator")
                LegacyIndeterminateCircularProgressIndicatorSample()
            }
        }
    }
}

@Composable
private fun SecondaryText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.secondary,
        fontSize = 12.sp
    )
}

@Composable
private fun SwitchSample() {
    var checked by remember { mutableStateOf(true) }
    Switch(
        modifier = Modifier.semantics { contentDescription = "Demo" },
        checked = checked,
        onCheckedChange = { checked = it }
    )
}

@Composable
private fun SwitchWithThumbIconSample() {
    var checked by remember { mutableStateOf(true) }

    Switch(
        modifier = Modifier.semantics { contentDescription = "Demo with icon" },
        checked = checked,
        onCheckedChange = { checked = it },
        thumbContent = {
            if (checked) {
                // Icon isn't focusable, no need for content description
                val color = LocalContentColor.current
                val canvasSize = with(LocalDensity.current) { SwitchDefaults.IconSize.toPx() }
                val iconDimension = IconChecked.getBounds().let { it.width + it.left * 2 }
                Canvas(modifier = Modifier.size(SwitchDefaults.IconSize)) {
                    scale(canvasSize / iconDimension, Offset.Zero) {
                        drawPath(IconChecked, color)
                    }
                }
            }
        }
    )
}

private val IconChecked by lazy(LazyThreadSafetyMode.NONE) {
    Path().apply {
        moveTo(9.0f, 16.17f)
        lineTo(4.83f, 12.0f)
        relativeLineTo(-1.42f, 1.41f)
        lineTo(9.0f, 19.0f)
        lineTo(21.0f, 7.0f)
        relativeLineTo(-1.41f, -1.41f)
        close()
    }
}

@Composable
private fun LinearProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.2f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress },
        )
        Row(modifier = Modifier.padding(10.dp)) {
            SecondaryText("Set progress:")
            Slider(
                modifier = Modifier.width(150.dp).height(12.dp),
                value = progress,
                valueRange = 0f..1f,
                onValueChange = { progress = it },
            )
        }
    }
}

@Composable
private fun LegacyLinearProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.2f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            progress = { animatedProgress },
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp,
            drawStopIndicator = {}
        )
        Row(modifier = Modifier.padding(10.dp)) {
            SecondaryText("Set progress:")
            Slider(
                modifier = Modifier.width(150.dp).height(12.dp),
                value = progress,
                valueRange = 0f..1f,
                onValueChange = { progress = it },
            )
        }
    }
}

@Composable
private fun IndeterminateLinearProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator()
    }
}

@Composable
private fun LegacyIndeterminateLinearProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        LinearProgressIndicator(
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp
        )
    }
}

@Composable
private fun CircularProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.2f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(progress = { animatedProgress })
        Row(modifier = Modifier.padding(10.dp)) {
            SecondaryText("Set progress:")
            Slider(
                modifier = Modifier.width(150.dp).height(12.dp),
                value = progress,
                valueRange = 0f..1f,
                onValueChange = { progress = it },
            )
        }
    }
}

@Composable
private fun LegacyCircularProgressIndicatorSample() {
    var progress by remember { mutableStateOf(0.2f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            trackColor = Color.Transparent,
            strokeCap = StrokeCap.Butt,
            gapSize = 0.dp
        )
        Row(modifier = Modifier.padding(10.dp)) {
            SecondaryText("Set progress:")
            Slider(
                modifier = Modifier.width(150.dp).height(12.dp),
                value = progress,
                valueRange = 0f..1f,
                onValueChange = { progress = it },
            )
        }
    }
}

@Composable
private fun IndeterminateCircularProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator()
    }
}

@Composable
private fun LegacyIndeterminateCircularProgressIndicatorSample() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        CircularProgressIndicator(strokeCap = StrokeCap.Butt)
    }
}

@Composable
private fun CheckboxSample() {
    val checkedState = remember { mutableStateOf(true) }
    Checkbox(checked = checkedState.value, onCheckedChange = { checkedState.value = it })
}

@Composable
private fun CheckboxWithTextSample() {
    val (checkedState, onStateChange) = remember { mutableStateOf(true) }
    Row(
        Modifier.fillMaxWidth()
            .height(56.dp)
            .toggleable(
                value = checkedState,
                onValueChange = { onStateChange(!checkedState) },
                role = Role.Checkbox
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checkedState,
            onCheckedChange = null // null recommended for accessibility with screenreaders
        )
        Text(
            text = "Option selection",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun TriStateCheckboxSample() {
    Column {
        // define dependent checkboxes states
        val (state, onStateChange) = remember { mutableStateOf(true) }
        val (state2, onStateChange2) = remember { mutableStateOf(true) }

        // TriStateCheckbox state reflects state of dependent checkboxes
        val parentState =
            remember(state, state2) {
                if (state && state2) ToggleableState.On
                else if (!state && !state2) ToggleableState.Off else ToggleableState.Indeterminate
            }
        // click on TriStateCheckbox can set state for dependent checkboxes
        val onParentClick = {
            val s = parentState != ToggleableState.On
            onStateChange(s)
            onStateChange2(s)
        }

        // The sample below composes just basic checkboxes which are not fully accessible on their
        // own. See the CheckboxWithTextSample as a way to ensure your checkboxes are fully
        // accessible.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier =
                Modifier.triStateToggleable(
                    state = parentState,
                    onClick = onParentClick,
                    role = Role.Checkbox
                )
        ) {
            TriStateCheckbox(
                state = parentState,
                onClick = null,
            )
            Text("Receive Emails")
        }
        Spacer(Modifier.size(25.dp))
        Column(Modifier.padding(24.dp, 0.dp, 0.dp, 0.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.toggleable(
                        value = state,
                        onValueChange = onStateChange,
                        role = Role.Checkbox
                    )
            ) {
                Checkbox(state, null)
                Text("Daily")
            }
            Spacer(Modifier.size(25.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier =
                    Modifier.toggleable(
                        value = state2,
                        onValueChange = onStateChange2,
                        role = Role.Checkbox
                    )
            ) {
                Checkbox(state2, null)
                Text("Weekly")
            }
        }
    }
}

@Composable
private fun SimpleSnackbar() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box {
        var clickCount by remember { mutableStateOf(0) }
        Button(onClick = {
            scope.launch { snackbarHostState.showSnackbar("Snackbar # ${++clickCount}") }
        }) {
            Text("Simple snackbar")
        }
        SnackbarHost(snackbarHostState)
    }
}

@Composable
private fun IndefiniteSnackbar() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box {
        var clickCount by remember { mutableStateOf(0) }
        Button(onClick = {
            // show snackbar as a suspend function
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Snackbar # ${++clickCount}",
                    actionLabel = "Action",
                    withDismissAction = true,
                    duration = SnackbarDuration.Indefinite
                )
            }
        }) {
            Text("Indefinite snackbar")
        }
        SnackbarHost(snackbarHostState)
    }
}

@Composable
private fun CustomSnackbar() {
    class SnackbarVisualsWithError(override val message: String, val isError: Boolean) :
        SnackbarVisuals {
        override val actionLabel: String
            get() = if (isError) "Error" else "OK"

        override val withDismissAction: Boolean
            get() = false

        override val duration: SnackbarDuration
            get() = SnackbarDuration.Indefinite
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box {
        var clickCount by remember { mutableStateOf(0) }
        Button(onClick = {
            scope.launch {
                snackbarHostState.showSnackbar(
                    SnackbarVisualsWithError(
                        "Snackbar # ${++clickCount}",
                        isError = clickCount % 2 != 0
                    )
                )
            }
        }) {
            Text("Custom snackbar")
        }
        // reuse default SnackbarHost to have default animation and timing handling
        SnackbarHost(snackbarHostState) { data ->
            // custom snackbar with the custom action button color and border
            val isError = (data.visuals as? SnackbarVisualsWithError)?.isError ?: false
            val buttonColor =
                if (isError) {
                    ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                } else {
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.inversePrimary
                    )
                }

            Snackbar(
                modifier =
                    Modifier.border(2.dp, MaterialTheme.colorScheme.secondary).padding(12.dp),
                action = {
                    TextButton(
                        onClick = { if (isError) data.dismiss() else data.performAction() },
                        colors = buttonColor
                    ) {
                        Text(data.visuals.actionLabel ?: "")
                    }
                }
            ) {
                Text(data.visuals.message)
            }
        }
    }
}

@Composable
private fun CoroutinesSnackbar() {
    // decouple snackbar host state from scaffold state for demo purposes
    // this state, channel and flow is for demo purposes to demonstrate business logic layer
    val snackbarHostState = remember { SnackbarHostState() }
    // we allow only one snackbar to be in the queue here, hence conflated
    val channel = remember { Channel<Int>(Channel.CONFLATED) }
    var clickCount by remember { mutableStateOf(0) }
    LaunchedEffect(channel) {
        channel.receiveAsFlow().collect { index ->
            val result =
                snackbarHostState.showSnackbar(
                    message = "Snackbar # $index",
                    actionLabel = "Action on $index"
                )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    /* action has been performed */
                }
                SnackbarResult.Dismissed -> {
                    /* dismissed, no action needed */
                }
            }
        }
    }
    Box {
        Button(onClick = {
            // offset snackbar data to the business logic
            channel.trySend(++clickCount)
        }) {
            Text("Coroutines snackbar")
        }
        SnackbarHost(snackbarHostState)
    }
}

@Composable
private fun MultilineSnackbar() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    Box {
        Button(onClick = {
            scope.launch {
                val longMessage =
                    "Very very very very very very very very very very very very very " +
                            "very very very very very very very very very very very very " +
                            "very very very very very very very very very very long message"
                snackbarHostState.showSnackbar(longMessage)
            }
        }) {
            Text("Multiline snackbar")
        }
        SnackbarHost(snackbarHostState) { data ->
            Snackbar {
                // The Material spec recommends a maximum of 2 lines of text.
                Text(data.visuals.message, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}