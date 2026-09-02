package uz.yuancalc.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import uz.yuancalc.core.parseAmount
import uz.yuancalc.ui.theme.Palette

/** One field treatment for the whole app: raised surface, hairline, gold focus. */
@Composable
private fun amountFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = Palette.SurfaceHigh,
    unfocusedContainerColor = Palette.SurfaceHigh,
    focusedBorderColor = Palette.Gold.copy(alpha = 0.55f),
    unfocusedBorderColor = Palette.Hairline,
    focusedLabelColor = Palette.Gold,
    unfocusedLabelColor = Palette.TextMid,
    cursorColor = Palette.Gold,
    focusedTextColor = Palette.TextHi,
    unfocusedTextColor = Palette.TextHi,
    focusedSuffixColor = Palette.TextMid,
    unfocusedSuffixColor = Palette.TextMid,
)

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
    suffix: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
        colors = amountFieldColors(),
        modifier = modifier,
    )
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
    suffix: String? = null,
) {
    fun render(v: Double?): String = v?.let(::trimNumber) ?: ""

    var draft by remember { mutableStateOf(render(value)) }
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(value, focused) {
        if (!focused) draft = render(value)
    }

    OutlinedTextField(
        value = draft,
        onValueChange = { text ->
            draft = text
            val parsed = parseAmount(text)
            when {
                parsed != null && accept(parsed) -> onCommit(parsed)
                text.isBlank() && allowEmpty -> onCommit(null)
            }
        },
        label = { Text(label) },
        singleLine = true,
        suffix = suffix?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = MaterialTheme.shapes.medium,
        colors = amountFieldColors(),
        modifier = modifier.onFocusChanged { focused = it.isFocused },
    )
}

/** Segmented toggle, used for g/kg, $/so'm, rounding step and language. */
@Composable
fun <T> OptionToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option.first == selected,
                onClick = { onSelect(option.first) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = Palette.SurfaceHigh,
                    activeContentColor = Palette.TextHi,
                    activeBorderColor = Palette.Hairline,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = Palette.TextMid,
                    inactiveBorderColor = Palette.Hairline,
                ),
                icon = {},
            ) { Text(option.second) }
        }
    }
}

/** Bordered card on the ink background; hairline instead of shadow. */
@Composable
fun SectionCard(title: String, content: @Composable () -> Unit) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Palette.Surface,
        contentColor = Palette.TextHi,
        border = BorderStroke(1.dp, Palette.Hairline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = Palette.TextLo,
            )
            content()
        }
    }
}

/** 9.0 -> "9", 1.8 -> "1.8" — avoids showing "9.0" in an editable field. */
fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
