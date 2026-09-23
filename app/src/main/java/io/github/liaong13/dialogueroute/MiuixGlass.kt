package io.github.liaong13.dialogueroute

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val LocalAppDarkTheme = staticCompositionLocalOf { false }

@Composable
internal fun GlassBackdrop(content: @Composable () -> Unit) {
    val dark = LocalAppDarkTheme.current
    Box(Modifier.fillMaxSize().drawWithCache {
        val base = Brush.verticalGradient(if (dark)
            listOf(Color(0xFF17243C), Color(0xFF101A28), Color(0xFF111724))
        else listOf(Color(0xFFF0F3FB), Color(0xFFF4F6FA), Color(0xFFF0F5F8)))
        val glow = Brush.radialGradient(
            colors = listOf(Color(0xFF8E9FEF).copy(alpha = if (dark) 0.12f else 0.07f), Color.Transparent),
            center = Offset(size.width, size.height * 0.18f), radius = size.width.coerceAtLeast(1f))
        onDrawBehind { drawRect(base); drawRect(glow) }
    }) { content() }
}

@Composable
internal fun Modifier.glassSurface(radius: Dp = 24.dp, floating: Boolean = false): Modifier {
    val dark = LocalAppDarkTheme.current
    val shape = RoundedCornerShape(radius)
    val fill = if (dark) Color(0xFF253247) else Color.White
    val highlight = Color.White.copy(alpha = if (dark) 0.22f else 0.92f)
    val edge = if (dark) Color(0xFF91A7C8).copy(alpha = 0.10f) else Color(0xFFB8C6DC).copy(alpha = 0.42f)
    return this
        .shadow(if (floating) 8.dp else 2.dp, shape, clip = false)
        .clip(shape)
        .background(Brush.verticalGradient(listOf(
            fill.copy(alpha = if (!dark) 0.97f else if (floating) 0.92f else 0.88f),
            fill.copy(alpha = if (!dark) 0.92f else if (floating) 0.84f else 0.80f))))
        .border(0.75.dp, Brush.linearGradient(listOf(highlight, edge, highlight.copy(alpha = 0.08f))), shape)
}

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(20.dp),
                       onClick: (() -> Unit)? = null,
                       content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = modifier.fillMaxWidth().glassSurface()
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
        .padding(padding), content = content)
}

@Composable
internal fun GlassTabs(tabs: List<String>, selectedTabIndex: Int, onTabSelected: (Int) -> Unit,
                      icons: List<ImageVector>? = null) {
    val dark = LocalAppDarkTheme.current
    val shape = RoundedCornerShape(22.dp)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().glassSurface(radius = 26.dp, floating = true)
        .padding(4.dp)) {
        val tabWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(targetValue = tabWidth * selectedTabIndex,
            animationSpec = spring(dampingRatio = 0.9f, stiffness = 400f), label = "glassSelection")
        Box(Modifier.matchParentSize()) {
            Box(Modifier.offset(x = indicatorOffset).width(tabWidth).fillMaxHeight().padding(2.dp)
                .shadow(2.dp, shape).clip(shape)
                .background(Brush.verticalGradient(if (dark)
                    listOf(Color(0xFF4B6281), Color(0xFF344B69))
                else listOf(Color.White, Color(0xFFEAF1FF))))
                .border(0.75.dp, Color.White.copy(alpha = if (dark) 0.24f else 0.95f), shape))
        }
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            tabs.forEachIndexed { index, title ->
                val selected = index == selectedTabIndex
                val tint = if (selected) {
                    if (dark) Color(0xFFEAF3FF) else MiuixTheme.colorScheme.primary
                }
                    else MiuixTheme.colorScheme.onSurfaceVariantSummary
                Row(modifier = Modifier.weight(1f).clip(shape)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onTabSelected(index) })
                    .heightIn(min = if (icons == null) 48.dp else 54.dp)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    icons?.getOrNull(index)?.let { icon ->
                        Image(icon, contentDescription = null, modifier = Modifier.size(20.dp),
                            colorFilter = ColorFilter.tint(tint))
                    }
                    Text(title, fontSize = if (icons == null) 15.sp else 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = tint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
