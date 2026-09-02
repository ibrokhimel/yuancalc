package uz.yuancalc.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import uz.yuancalc.R
import uz.yuancalc.ui.theme.Ds
import uz.yuancalc.ui.theme.Palette

private enum class Tab(val labelRes: Int, val icon: ImageVector) {
    CALCULATOR(R.string.tab_calculator, Icons.Outlined.Calculate),
    CONVERT(R.string.tab_convert, Icons.Outlined.SwapHoriz),
    SETTINGS(R.string.tab_settings, Icons.Outlined.Settings),
}

/**
 * The three screens live in a pager, so left/right swipes anywhere on the
 * content move between them and every screen stays composed — a tab switch is
 * a scroll, not a rebuild. The header and nav highlight follow the pager's
 * target page, so they answer mid-swipe.
 */
@Composable
fun AppScaffold(vm: CalculatorViewModel) {
    val pager = rememberPagerState { Tab.entries.size }
    val scope = rememberCoroutineScope()
    val settings by vm.settings.collectAsStateWithLifecycle()

    ApplyLanguage(settings.language)

    val tab = Tab.entries[pager.targetPage.coerceIn(0, Tab.entries.size - 1)]
    val goTo: (Tab) -> Unit = { t ->
        scope.launch { pager.animateScrollToPage(t.ordinal) }
    }

    Scaffold(
        containerColor = Palette.Ink,
        topBar = { Header(tab) },
        bottomBar = { NavPill(current = tab, onSelect = goTo) },
    ) { padding ->
        val openSettings = { goTo(Tab.SETTINGS) }
        HorizontalPager(
            state = pager,
            beyondViewportPageCount = Tab.entries.size - 1,
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) { page ->
            when (Tab.entries[page]) {
                Tab.CALCULATOR -> CalculatorScreen(vm, openSettings)
                Tab.CONVERT -> ConvertScreen(vm, openSettings)
                Tab.SETTINGS -> SettingsScreen(vm)
            }
        }
    }
}

/** Brand line in accent, then the screen title large — the reference layout. */
@Composable
private fun Header(tab: Tab) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
    ) {
        Text(
            stringResource(R.string.app_name).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = Palette.Accent,
        )
        Text(
            stringResource(tab.labelRes),
            style = MaterialTheme.typography.headlineMedium,
            color = Palette.TextHi,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

/**
 * The reference nav: a floating pill centered over the background, one item
 * per tab. The active item carries a soft accent wash and its label; inactive
 * items collapse to bare icons, so a switch reads as the pill re-balancing.
 *
 * Besides taps, the pill takes a held drag: sliding a finger across it
 * selects whatever item is under the finger, like scrubbing a segmented
 * control.
 */
@Composable
private fun NavPill(current: Tab, onSelect: (Tab) -> Unit) {
    val pillShape = RoundedCornerShape(28.dp)
    val itemShape = RoundedCornerShape(20.dp)
    val bounds = remember { mutableStateMapOf<Tab, Rect>() }
    val select by rememberUpdatedState(onSelect)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { change, _ ->
                        val x = change.position.x
                        bounds.entries
                            .firstOrNull { x >= it.value.left && x <= it.value.right }
                            ?.let { select(it.key) }
                    }
                }
                .shadow(
                    elevation = 16.dp,
                    shape = pillShape,
                    ambientColor = Color(0x59000000),
                    spotColor = Color(0x59000000),
                )
                .clip(pillShape)
                .background(Palette.Surface)
                .border(1.dp, Palette.Hairline, pillShape)
                .padding(8.dp),
        ) {
            Tab.entries.forEach { t ->
                val active = t == current
                val wash by animateColorAsState(
                    targetValue = if (active) Palette.AccentSoft else Color.Transparent,
                    animationSpec = tween(200),
                    label = "navWash",
                )
                val tint by animateColorAsState(
                    targetValue = if (active) Palette.Accent else Palette.TextMid,
                    animationSpec = tween(200),
                    label = "navTint",
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .height(40.dp)
                        .onGloballyPositioned { bounds[t] = it.boundsInParent() }
                        .clip(itemShape)
                        .background(wash)
                        .clickable { onSelect(t) }
                        .padding(horizontal = 13.dp),
                ) {
                    Icon(
                        t.icon,
                        contentDescription = stringResource(t.labelRes),
                        tint = tint,
                        modifier = Modifier.size(22.dp),
                    )
                    AnimatedVisibility(
                        visible = active,
                        enter = expandHorizontally() + fadeIn(),
                        exit = shrinkHorizontally() + fadeOut(),
                    ) {
                        Text(
                            stringResource(t.labelRes),
                            style = Ds.NavLabel,
                            color = Palette.Accent,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
