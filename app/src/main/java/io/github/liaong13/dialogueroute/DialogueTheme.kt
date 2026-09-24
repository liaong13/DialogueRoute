package io.github.liaong13.dialogueroute

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
internal fun DialogueTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) darkColorScheme(
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
    MaterialTheme(colorScheme = colors, content = content)
}
