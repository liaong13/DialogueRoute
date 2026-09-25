package io.github.liaong13.dialogueroute

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.liaong13.dialogueroute.core.Prefs
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

internal val LocalMiuixStyle = staticCompositionLocalOf { false }

@Composable
internal fun DialogueTheme(
    dark: Boolean,
    dynamicColor: Boolean = false,
    uiStyle: String = Prefs.STYLE_MATERIAL3,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val staticColors = if (dark) darkColorScheme(
        primary = Color(0xFFA8C9FA),
        onPrimary = Color(0xFF102D50),
        primaryContainer = Color(0xFF233B57),
        onPrimaryContainer = Color(0xFFD9E8FF),
        secondary = Color(0xFFB5C8E2),
        onSecondary = Color(0xFF25374D),
        secondaryContainer = Color(0xFF34465E),
        onSecondaryContainer = Color(0xFFD1E3FF),
        background = Color(0xFF11151C),
        surface = Color(0xFF11151C),
        onBackground = Color(0xFFF2F6FC),
        onSurface = Color(0xFFF2F6FC),
        onSurfaceVariant = Color(0xFFABB9CB),
        surfaceVariant = Color(0xFF1D242E),
        surfaceContainer = Color(0xFF1D242E),
        outline = Color(0xFF7B8CA2),
        surfaceTint = Color(0xFFA8C9FA),
    ) else lightColorScheme(
        primary = Color(0xFF1769C2),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8F0FF),
        onPrimaryContainer = Color(0xFF113A67),
        secondary = Color(0xFF516780),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDCE8F8),
        onSecondaryContainer = Color(0xFF344A62),
        background = Color(0xFFF4F6FA),
        surface = Color(0xFFEDF1F7),
        onBackground = Color(0xFF202B3D),
        onSurface = Color(0xFF202B3D),
        onSurfaceVariant = Color(0xFF58667C),
        surfaceVariant = Color.White,
        surfaceContainer = Color.White,
        outline = Color(0xFF75869B),
        surfaceTint = Color(0xFF1769C2),
    )
    val dynamicColors = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else null
    val miuix = uiStyle == Prefs.STYLE_MIUIX
    val colors = (dynamicColors ?: staticColors).let { scheme ->
        if (!miuix) scheme else scheme.copy(
            background = if (dark) Color(0xFF0F1115) else Color(0xFFF5F5F7),
            surface = if (dark) Color(0xFF1C1D22) else Color(0xFFFFFFFF),
            surfaceContainer = if (dark) Color(0xFF26272D) else Color(0xFFF8F8FA),
            surfaceVariant = if (dark) Color(0xFF292B31) else Color(0xFFF0F0F3),
        )
    }
    val typography = Typography().let { base ->
        if (!miuix) base else Typography(
            headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
            titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
    val shapes = if (miuix) Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
    ) else Shapes()
    val miuixColors = (if (dark) miuixDarkColorScheme() else miuixLightColorScheme()).copy(
        primary = colors.primary,
        onPrimary = colors.onPrimary,
        primaryContainer = colors.primaryContainer,
        onPrimaryContainer = colors.onPrimaryContainer,
        secondary = colors.secondary,
        onSecondary = colors.onSecondary,
        secondaryContainer = colors.secondaryContainer,
        onSecondaryContainer = colors.onSecondaryContainer,
        background = colors.background,
        onBackground = colors.onBackground,
        onBackgroundVariant = colors.onSurfaceVariant,
        surface = colors.surface,
        onSurface = colors.onSurface,
        surfaceContainer = colors.surfaceContainer,
        onSurfaceContainer = colors.onSurface,
        onSurfaceContainerVariant = colors.onSurfaceVariant,
        outline = colors.outline,
    )
    CompositionLocalProvider(LocalMiuixStyle provides miuix) {
        MiuixTheme(colors = miuixColors) {
            MaterialTheme(colorScheme = colors, typography = typography, shapes = shapes, content = content)
        }
    }
}
