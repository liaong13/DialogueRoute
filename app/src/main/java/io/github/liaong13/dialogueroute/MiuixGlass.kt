package io.github.liaong13.dialogueroute

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text

internal val LocalAppDarkTheme = staticCompositionLocalOf { false }
private val LocalGlassSource = staticCompositionLocalOf<HazeState?> { null }

@Composable
internal fun GlassSource(state: HazeState, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalGlassSource provides state, content = content)
}

@Composable
internal fun GlassBackdrop(content: @Composable () -> Unit) {
    val dark = LocalAppDarkTheme.current
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
                drawRect(if (dark) Color(0xFF121C2A) else Color(0xFFF8FAFC))
                drawPath(upper, if (dark) Color(0xFF1A2B3B) else Color(0xFFEAF1F8))
                drawCircle(if (dark) Color(0xFF19313B) else Color(0xFFEAF5F3),
                    radius = size.width * 0.54f,
                    center = Offset(size.width * 1.14f, size.height * 0.18f))
                drawCircle(if (dark) Color(0xFF1C3040) else Color(0xFFE8F2F7),
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
    val hazeState = LocalGlassSource.current.takeIf { useBackdrop }
    val glassShape = RoundedCornerShape(radius)
    val fill = if (dark) Color(0xFF29384A) else Color.White
    val highlight = Color.White.copy(alpha = if (dark) 0.14f else 0.44f)
    val edge = if (dark) Color(0xFF9AAEC8).copy(alpha = 0.17f)
        else Color(0xFFB6C6D8).copy(alpha = 0.34f)
    val base = this.shadow(if (floating) 3.dp else 0.dp, glassShape, clip = false).clip(glassShape)
    return if (hazeState != null) {
        val style = remember(dark, radius, floating) {
            GlassStyle.clear.then {
                shape(glassShape)
                backgroundColor(if (dark) Color(0xFF121C2A) else Color(0xFFF8FAFC))
                tint((if (dark) Color(0xFF9DB9D5) else Color.White).copy(alpha = if (dark) 0.04f else 0.10f))
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
        animationSpec = spring(dampingRatio = 0.78f, stiffness = 500f), label = "glassCardPress")
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
    Box(Modifier.fillMaxWidth().glassSurface(radius = 26.dp, floating = true)) {
        if (navigationBar) {
            NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp,
                windowInsets = WindowInsets(0, 0, 0, 0)) {
                tabs.forEachIndexed { index, title ->
                    val itemIcon = if (index == selectedTabIndex)
                        selectedIcons?.getOrNull(index) ?: icons?.getOrNull(index)
                    else icons?.getOrNull(index)
                    NavigationBarItem(
                        selected = index == selectedTabIndex,
                        onClick = { onTabSelected(index) },
                        icon = {
                            itemIcon?.let { icon ->
                                Icon(icon, contentDescription = null)
                            }
                        },
                        label = { Text(title, maxLines = 1) },
                    )
                }
            }
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
