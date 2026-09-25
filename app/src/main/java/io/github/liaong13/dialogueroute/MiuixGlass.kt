package io.github.liaong13.dialogueroute

import android.animation.ValueAnimator
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeSourceRetention
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.RefractionProfile
import dev.chrisbanes.haze.glass.hazeGlass
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem
import top.yukonga.miuix.kmp.basic.TabRowDefaults as MiuixTabRowDefaults
import top.yukonga.miuix.kmp.basic.TabRowWithContour as MiuixTabRowWithContour

internal val LocalAppDarkTheme = staticCompositionLocalOf { false }
private val LocalGlassSource = staticCompositionLocalOf<HazeState?> { null }

@Composable
internal fun GlassSource(state: HazeState, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassSource provides state, content = content)
}

@Composable
internal fun GlassBackdrop(content: @Composable () -> Unit) {
    val dark = LocalAppDarkTheme.current
    val colors = MaterialTheme.colorScheme
    val hazeState = rememberHazeState()
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().hazeSource(hazeState).drawWithCache {
            val upper = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width * 0.34f, 0f)
                lineTo(size.width * 0.58f, size.height * 0.26f)
                lineTo(0f, size.height * 0.42f)
                close()
            }
            onDrawBehind {
                drawRect(colors.background)
                drawPath(upper, colors.primary.copy(alpha = if (dark) 0.09f else 0.07f))
                drawCircle(colors.secondary.copy(alpha = if (dark) 0.10f else 0.07f),
                    radius = size.width * 0.54f,
                    center = Offset(size.width * 1.14f, size.height * 0.18f))
                drawCircle(colors.tertiary.copy(alpha = if (dark) 0.10f else 0.07f),
                    radius = size.width * 0.64f,
                    center = Offset(-size.width * 0.28f, size.height * 1.06f))
            }
        })
        CompositionLocalProvider(LocalGlassSource provides hazeState) {
            Box(Modifier.fillMaxSize().padding(androidx.compose.foundation.layout.WindowInsets.Companion.systemBars.asPaddingValues())) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalHazeApi::class)
@Composable
internal fun Modifier.glassSurface(radius: Dp = 24.dp, floating: Boolean = false,
                                   useBackdrop: Boolean = true): Modifier {
    val dark = LocalAppDarkTheme.current
    val miuix = LocalMiuixStyle.current
    val colors = MaterialTheme.colorScheme
    val hazeState = LocalGlassSource.current.takeIf { useBackdrop }
    val glassShape = RoundedCornerShape(radius + if (miuix) 4.dp else 0.dp)
    val fill = colors.surfaceContainer
    val highlight = Color.White.copy(alpha = if (dark) 0.14f else 0.44f)
    val edge = colors.outline.copy(alpha = if (dark) 0.22f else 0.30f)
    val base = this.shadow(if (floating) 3.dp else 0.dp, glassShape, clip = false).clip(glassShape)
    return if (hazeState != null) {
        val style = remember(dark, miuix, radius, floating, colors.surface, colors.primary) {
            GlassStyle.clear.then {
                shape(glassShape)
                backgroundColor(colors.surface)
                tint(colors.primary.copy(alpha = if (dark) 0.04f else 0.08f))
                optics(refractionStrength = 0.16f, refractionDisplacement = 6.dp,
                    refractionDetailIntensity = 0f, refractionProfile = RefractionProfile.Edge(4.dp),
                    depth = 0.18f, blurRadius = 20.dp)
                specularIntensity(0f)
                edgeShadow(Color.Transparent)
                ambientResponse(0f)
                edgeSoftness(0.dp)
                chromaticAberrationStrength(0.02f)
                whitePoint(if (dark) 0.06f else 0.13f)
                chromaMultiplier(1.02f)
                contentNormalBlend(0.04f)
            }
        }
        base.hazeGlass(input = HazeInput.Sources(state = hazeState,
            retention = HazeSourceRetention.ClearWhenUnavailable), style = style)
            .background(fill.copy(alpha = if (dark) 0.05f else 0.10f))
            .border(0.75.dp, Brush.linearGradient(listOf(highlight, edge, edge)), glassShape)
    } else {
        base.background(Brush.verticalGradient(listOf(
            fill.copy(alpha = if (dark) 0.68f else 0.78f),
            fill.copy(alpha = if (dark) 0.55f else 0.68f))))
            .border(0.75.dp, Brush.linearGradient(listOf(highlight, edge, edge)), glassShape)
    }
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(20.dp),
                       onClick: (() -> Unit)? = null,
                       content: @Composable ColumnScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (onClick != null && pressed) 0.985f else 1f,
        animationSpec = if (ValueAnimator.areAnimatorsEnabled()) spring(dampingRatio = 0.78f,
            stiffness = 500f) else tween(0), label = "glassCardPress")
    Column(modifier = modifier.fillMaxWidth()
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .glassSurface()
        .then(if (onClick != null) Modifier.clickable(interactionSource = interaction,
            indication = null, onClick = onClick) else Modifier)
        .padding(padding), content = content)
}

@Composable
internal fun GlassTabs(tabs: List<String>, selectedTabIndex: Int, onTabSelected: (Int) -> Unit,
                      icons: List<ImageVector>? = null, selectedIcons: List<ImageVector>? = null,
                      navigationBar: Boolean = false) {
    val miuix = LocalMiuixStyle.current
    Box(Modifier.fillMaxWidth().glassSurface(radius = 26.dp, floating = true)
        .then(if (navigationBar) Modifier.background(MaterialTheme.colorScheme.surface.copy(
            alpha = if (LocalAppDarkTheme.current) 0.56f else 0.68f)) else Modifier)) {
        if (navigationBar && miuix) {
            MiuixNavigationBar(color = Color.Transparent, showDivider = false,
                defaultWindowInsetsPadding = false) {
                tabs.forEachIndexed { index, title ->
                    val icon = if (index == selectedTabIndex)
                        selectedIcons?.getOrNull(index) ?: icons?.getOrNull(index)
                    else icons?.getOrNull(index)
                    icon?.let {
                        MiuixNavigationBarItem(selected = index == selectedTabIndex,
                            onClick = { onTabSelected(index) }, icon = it, label = title)
                    }
                }
            }
        } else if (navigationBar) {
            GlassNavigationItems(tabs, selectedTabIndex, onTabSelected, icons, selectedIcons)
        } else if (miuix) {
            val colors = MaterialTheme.colorScheme
            MiuixTabRowWithContour(tabs = tabs, selectedTabIndex = selectedTabIndex,
                onTabSelected = onTabSelected, modifier = Modifier.padding(5.dp),
                colors = MiuixTabRowDefaults.tabRowColors(
                    backgroundColor = Color.Transparent,
                    contentColor = colors.onSurfaceVariant,
                    selectedBackgroundColor = colors.primaryContainer,
                    selectedContentColor = colors.onPrimaryContainer),
                cornerRadius = 18.dp)
        } else {
            PrimaryTabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent) {
                tabs.forEachIndexed { index, title ->
                    val icon = icons?.getOrNull(index)
                    if (icon == null) {
                        Tab(selected = index == selectedTabIndex,
                            onClick = { onTabSelected(index) },
                            text = { Text(title, maxLines = 1) })
                    } else {
                        Tab(selected = index == selectedTabIndex,
                            onClick = { onTabSelected(index) },
                            text = { Text(title, maxLines = 1) },
                            icon = { Icon(icon, contentDescription = null) })
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassNavigationItems(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    icons: List<ImageVector>?,
    selectedIcons: List<ImageVector>?,
) {
    if (tabs.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val motionEnabled = ValueAnimator.areAnimatorsEnabled()
    val miuix = LocalMiuixStyle.current
    var dragDelta by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(Modifier.fillMaxWidth().padding(5.dp)) {
        val itemWidth = maxWidth / tabs.size
        val itemWidthPx = constraints.maxWidth.toFloat() / tabs.size
        val dragDp = with(androidx.compose.ui.platform.LocalDensity.current) { dragDelta.toDp() }
        val target = itemWidth * selectedTabIndex + dragDp
        val indicatorX by animateDpAsState(
            targetValue = target.coerceIn(0.dp, maxWidth - itemWidth),
            animationSpec = if (motionEnabled) spring(dampingRatio = if (miuix) 0.82f else 0.72f,
                stiffness = if (miuix) 480f else 430f)
                else tween(0), label = "navigationCapsule")
        Box(Modifier.offset(x = indicatorX).width(itemWidth).heightIn(min = 64.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.primaryContainer.copy(alpha = if (LocalAppDarkTheme.current) 0.75f else 0.86f))
            .border(0.7.dp, colors.primary.copy(alpha = 0.18f), RoundedCornerShape(22.dp)))
        Row(Modifier.fillMaxWidth().pointerInput(tabs.size, selectedTabIndex, motionEnabled, itemWidthPx) {
            if (motionEnabled && itemWidthPx > 0f) detectHorizontalDragGestures(
                onHorizontalDrag = { change, delta ->
                    change.consume()
                    dragDelta = (dragDelta + delta * 0.78f)
                        .coerceIn(-itemWidthPx * selectedTabIndex,
                            itemWidthPx * (tabs.lastIndex - selectedTabIndex))
                },
                onDragEnd = {
                    onTabSelected((selectedTabIndex + dragDelta / itemWidthPx).roundToInt()
                        .coerceIn(0, tabs.lastIndex))
                    dragDelta = 0f
                },
                onDragCancel = { dragDelta = 0f },
            )
        }) {
            tabs.forEachIndexed { index, title ->
                val selected = index == selectedTabIndex
                val icon = if (selected) selectedIcons?.getOrNull(index) ?: icons?.getOrNull(index)
                    else icons?.getOrNull(index)
                val interaction = remember(index) { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val scale by animateFloatAsState(
                    if (pressed && motionEnabled) 0.92f else 1f,
                    animationSpec = if (motionEnabled) spring(dampingRatio = 0.72f, stiffness = 420f)
                        else tween(0), label = "navigationPress")
                Column(Modifier.weight(1f).heightIn(min = 64.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .selectable(selected = selected, role = Role.Tab,
                        interactionSource = interaction, indication = null) { onTabSelected(index) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(24.dp),
                        tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant) }
                    Text(title, maxLines = 1, fontSize = 12.sp,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
                }
            }
        }
    }
}
