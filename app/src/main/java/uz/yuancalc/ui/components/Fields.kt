package uz.yuancalc.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import uz.yuancalc.core.parseAmount
import uz.yuancalc.ui.theme.Palette

/**
 * The one field treatment from the reference: a 56dp raised surface with a
 * 14dp radius, the small label stacked INSIDE above the value, hairline border
 * that turns accent (with the label) while focused.
 */
@Composable
private fun FieldShell(
    label: String,
    focused: Boolean,
    modifier: Modifier = Modifier,
    field: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    val border by animateColorAsState(
        targetValue = if (focused) Palette.AccentFocus else Palette.Hairline,
        animationSpec = tween(180),
        label = "fieldBorder",
    )
    val labelColor by animateColorAsState(
        targetValue = if (focused) Palette.Accent else Palette.TextMid,
        animationSpec = tween(180),
        label = "fieldLabel",
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(1.dp, Alignment.CenterVertically),
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .background(Palette.SurfaceHigh)
            .border(1.dp, border, shape)
            .padding(horizontal = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = labelColor)
        field()
    }
}

/** The text input inside a [FieldShell]; the shell owns all decoration. */
@Composable
private fun ShellInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    placeholder: String? = null,
    onFocus: ((Boolean) -> Unit)? = null,
) {
    var focused by remember { mutableStateOf(false) }
    FieldShell(label, focused, modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Palette.TextHi),
            cursorBrush = SolidColor(Palette.Accent),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = Palette.TextLo,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged {
                    focused = it.isFocused
                    onFocus?.invoke(it.isFocused)
                },
        )
    }
}

/**
 * A numeric text field whose text is owned by the caller. Used for the
 * calculator inputs, where the raw string is what gets persisted.
 */
@Composable
fun AmountField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    ShellInput(label, value, onValueChange, modifier)
}

/**
 * A numeric field bound to a persisted Double, for Settings.
 *
 * While the field has focus the user edits a local draft and nothing fights
 * the cursor: a valid parse is committed as they type, an invalid or partial
 * one ("", "2.", "abc") is simply held. When focus leaves, the draft snaps to
 * whatever is actually persisted, so a half-typed value can never stick.
 *
 * Binding the text directly to the persisted value instead (the obvious
 * approach) rewrites the field on every keystroke, which is how a cargo rate
 * of $50 690 900/kg once ended up on a real phone.
 */
@Composable
fun DraftNumberField(
    label: String,
    value: Double?,
    onCommit: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    allowEmpty: Boolean = false,
    accept: (Double) -> Boolean = { it >= 0.0 },
    placeholder: String? = null,
) {
    fun render(v: Double?): String = v?.let(::trimNumber) ?: ""

    var draft by remember { mutableStateOf(render(value)) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) draft = render(value)
    }

    ShellInput(
        label = label,
        value = draft,
        onValueChange = { text ->
            draft = text
            val parsed = parseAmount(text)
            when {
                parsed != null && accept(parsed) -> onCommit(parsed)
                text.isBlank() && allowEmpty -> onCommit(null)
            }
        },
        placeholder = placeholder,
        onFocus = { focused = it },
        modifier = modifier,
    )
}

/**
 * Free-text sibling of [DraftNumberField], for names. Same draft discipline:
 * the focused field owns its text, non-blank edits commit as typed, and on
 * focus loss the draft snaps back to what is actually persisted.
 */
@Composable
fun DraftTextField(
    label: String,
    value: String,
    onCommit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draft by remember { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) draft = value
    }

    ShellInput(
        label = label,
        value = draft,
        onValueChange = { text ->
            draft = text
            if (text.isNotBlank()) onCommit(text)
        },
        keyboardType = KeyboardType.Text,
        onFocus = { focused = it },
        modifier = modifier,
    )
}

/**
 * Segmented toggle as a recessed track with a raised thumb — used for g/kg,
 * $/so'm, the calculator mode, rounding and language. One shared thumb SLIDES
 * to the selected segment (growing or shrinking to its width on the way)
 * instead of disappearing on one and reappearing on the other.
 */
@Composable
fun <T> OptionToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fillEqually: Boolean = false,
) {
    val bounds = remember { mutableStateMapOf<T, Rect>() }
    val currentSelected = rememberUpdatedState(selected)
    val select = rememberUpdatedState(onSelect)
    val thumbX = remember { Animatable(Float.NaN) }
    val thumbW = remember { Animatable(Float.NaN) }
    val target = bounds[selected]

    LaunchedEffect(target) {
        val t = target ?: return@LaunchedEffect
        if (thumbX.value.isNaN()) {
            thumbX.snapTo(t.left)
            thumbW.snapTo(t.width)
        } else {
            val motion = spring<Float>(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
            )
            launch { thumbX.animateTo(t.left, motion) }
            launch { thumbW.animateTo(t.width, motion) }
        }
    }

    Box(
        modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.SurfaceHigh)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val x = change.position.x
                    bounds.entries
                        .firstOrNull { x >= it.value.left && x <= it.value.right }
                        ?.takeIf { it.key != currentSelected.value }
                        ?.let { select.value(it.key) }
                }
            }
            .padding(3.dp),
    ) {
        if (!thumbX.value.isNaN()) {
            Box(
                Modifier
                    .offset { IntOffset(thumbX.value.roundToInt(), 0) }
                    .width(with(LocalDensity.current) { thumbW.value.toDp() })
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(17.dp))
                    .background(Palette.SurfaceRaised),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxHeight(),
        ) {
            options.forEach { option ->
                val active = option.first == selected
                val label by animateColorAsState(
                    targetValue = if (active) Palette.TextHi else Palette.TextMid,
                    animationSpec = tween(180),
                    label = "segLabel",
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .then(if (fillEqually) Modifier.weight(1f) else Modifier)
                        .fillMaxHeight()
                        .onGloballyPositioned { bounds[option.first] = it.boundsInParent() }
                        .clip(RoundedCornerShape(17.dp))
                        .clickable { onSelect(option.first) }
                        .padding(horizontal = 14.dp),
                ) {
                    Text(option.second, style = MaterialTheme.typography.labelLarge, color = label)
                }
            }
        }
    }
}

/**
 * Bordered card on the ink background. The reference has two variants and both
 * are ported exactly: the default is square-cornered with a faint drop shadow,
 * [rounded] is 18dp-cornered with no shadow (cargo profiles, app, max cost).
 */
@Composable
fun SectionCard(
    title: String,
    rounded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = if (rounded) MaterialTheme.shapes.large else RectangleShape,
        color = Palette.Surface,
        contentColor = Palette.TextHi,
        border = BorderStroke(1.dp, Palette.Hairline),
        shadowElevation = if (rounded) 0.dp else 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .animateContentSize(
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                )
                .padding(16.dp),
        ) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Palette.TextLo,
            )
            content()
        }
    }
}

/**
 * A slider for price multiples: thin raised track with hairline notches at the
 * verdict boundaries, accent range, and a ring thumb on the ink background.
 * Values snap to 0.05 so a scrub lands on tidy numbers like 1.85.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultipleSlider(
    value: Double,
    onChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 1f..4f,
    notches: List<Double> = emptyList(),
) {
    Slider(
        value = value.toFloat().coerceIn(range.start, range.endInclusive),
        onValueChange = { v -> onChange((v * 20).roundToInt() / 20.0) },
        valueRange = range,
        thumb = {
            Box(
                Modifier
                    .size(16.dp)
                    .background(Palette.Ink, CircleShape)
                    .border(2.dp, Palette.Accent, CircleShape),
            )
        },
        track = { state ->
            val span = state.valueRange.endInclusive - state.valueRange.start
            val fraction = (state.value - state.valueRange.start) / span
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp),
            ) {
                val trackWidth = maxWidth
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Palette.SurfaceHigh),
                )
                notches.forEach { notch ->
                    val f = (notch.toFloat() - state.valueRange.start) / span
                    if (f > 0f && f < 1f) {
                        Box(
                            Modifier
                                .offset(x = trackWidth * f - 1.dp)
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(Palette.Hairline),
                        )
                    }
                }
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Palette.Accent),
                )
            }
        },
        modifier = modifier,
    )
}

/** 9.0 -> "9", 1.8 -> "1.8" — avoids showing "9.0" in an editable field. */
fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
