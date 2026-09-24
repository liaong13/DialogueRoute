package io.github.liaong13.dialogueroute

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import java.util.function.Consumer

@Composable
internal fun PageHeading(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            SupportingText(subtitle)
        }
    }
}

@Composable
internal fun SectionHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 4.dp))
}

@Composable
internal fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(content = content)
}

@Composable
internal fun SupportingText(text: String) {
    Text(text, fontSize = 14.sp, lineHeight = 21.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun StatusBadge(label: String, highlighted: Boolean = false) {
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        color = if (highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.glassSurface(radius = 8.dp)
            .background(if (highlighted) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
internal fun AccentIconTile(icon: ImageVector, tint: Color, size: Dp = 44.dp) {
    val shape = RoundedCornerShape(14.dp)
    Box(Modifier.size(size).clip(shape)
        .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.30f),
            tint.copy(alpha = 0.12f))))
        .border(0.75.dp, tint.copy(alpha = 0.24f), shape),
        contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(size * 0.52f),
            tint = tint)
    }
}

@Composable
internal fun SetupStatusRow(title: String, description: String, status: String, ready: Boolean,
                            icon: ImageVector, tint: Color, divider: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AccentIconTile(icon, tint)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    StatusBadge(status, highlighted = ready)
                }
                SupportingText(description)
            }
        }
        if (divider) {
            Spacer(Modifier.fillMaxWidth().padding(start = 56.dp).height(0.75.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f)))
        }
    }
}

@Composable
internal fun EmptyState(title: String, description: String, icon: ImageVector,
                        actionLabel: String, onAction: () -> Unit) {
    GlassCard(padding = PaddingValues(24.dp)) {
        val dark = LocalAppDarkTheme.current
        val tint = if (dark) Color(0xFFAFCBFF) else Color(0xFF3972BE)
        Box(Modifier.align(Alignment.CenterHorizontally)
            .size(width = 216.dp, height = 172.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(
                tint.copy(alpha = 0.18f), Color.Transparent))))
            val backShape = RoundedCornerShape(18.dp)
            Box(Modifier.offset(x = (-35).dp, y = 12.dp).size(width = 90.dp, height = 112.dp)
                .graphicsLayer { rotationZ = -16f }.clip(backShape)
                .background(tint.copy(alpha = 0.16f))
                .border(0.75.dp, tint.copy(alpha = 0.24f), backShape))
            Box(Modifier.offset(x = 35.dp, y = 12.dp).size(width = 90.dp, height = 112.dp)
                .graphicsLayer { rotationZ = 14f }.clip(backShape)
                .background(tint.copy(alpha = 0.19f))
                .border(0.75.dp, tint.copy(alpha = 0.30f), backShape))
            val frontShape = RoundedCornerShape(22.dp)
            Column(Modifier.offset(y = 4.dp).size(width = 112.dp, height = 128.dp)
                .clip(frontShape)
                .background(Brush.linearGradient(if (dark)
                    listOf(Color(0xFF9ABDF3), Color(0xFF567DBC))
                else listOf(Color(0xFFE1EDFF), Color(0xFFACC9F5))))
                .border(1.dp, Color.White.copy(alpha = if (dark) 0.52f else 0.84f), frontShape),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(58.dp),
                    tint = if (dark) Color(0xFF203C69) else Color(0xFF3467A8))
                Spacer(Modifier.height(12.dp))
                Spacer(Modifier.size(width = 56.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.44f), RoundedCornerShape(4.dp)))
                Spacer(Modifier.height(6.dp))
                Spacer(Modifier.size(width = 42.dp, height = 4.dp)
                    .background(Color.White.copy(alpha = 0.30f), RoundedCornerShape(4.dp)))
            }
            Box(Modifier.align(Alignment.TopEnd).offset(x = (-18).dp, y = 17.dp)
                .size(11.dp).graphicsLayer { rotationZ = 45f }
                .background(tint.copy(alpha = 0.72f), RoundedCornerShape(2.dp)))
        }
        Spacer(Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(10.dp))
        Text(description, fontSize = 14.sp, lineHeight = 22.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(22.dp))
        PrimaryAction(actionLabel, icon = icon, onClick = onAction)
    }
}

@Composable
internal fun UiField(label: String, value: String, onValueChange: (String) -> Unit,
                     singleLine: Boolean = true,
                     visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
                         androidx.compose.ui.text.input.VisualTransformation.None) {
    val secret = visualTransformation is PasswordVisualTransformation
    var revealed by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        maxLines = if (singleLine) 1 else 6,
        visualTransformation = if (secret && revealed) VisualTransformation.None else visualTransformation,
        keyboardOptions = if (secret) KeyboardOptions(
            keyboardType = KeyboardType.Password, autoCorrectEnabled = false
        ) else KeyboardOptions.Default,
        trailingIcon = if (secret) {
            { TextButton(onClick = { revealed = !revealed }) {
                Text(if (revealed) "隐藏" else "显示")
            } }
        } else null,
        shape = RoundedCornerShape(12.dp),
    )
    Spacer(Modifier.height(12.dp))
}

@Composable
internal fun PrimaryAction(label: String, icon: ImageVector? = null, onClick: () -> Unit) {
    GlassAction(label, icon, onClick, primary = true)
}

@Composable
internal fun SecondaryAction(label: String, icon: ImageVector? = null, onClick: () -> Unit) {
    GlassAction(label, icon, onClick, primary = false)
}

@Composable
private fun GlassAction(label: String, icon: ImageVector?, onClick: () -> Unit, primary: Boolean) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 550f), label = "glassActionPress")
    val dark = LocalAppDarkTheme.current
    val modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .glassSurface(radius = 16.dp)
        .then(if (primary) Modifier.background(
            if (dark) Color(0xFFAFCBEE).copy(alpha = 0.70f)
            else Color(0xFFBED7F8).copy(alpha = 0.68f)) else Modifier)
    val foreground = if (primary) Color(0xFF15253A) else MaterialTheme.colorScheme.primary
    if (primary) {
        Button(onClick = onClick, modifier = modifier, interactionSource = interaction,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
                contentColor = foreground)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            if (icon != null) Spacer(Modifier.size(8.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier, interactionSource = interaction,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.Transparent,
                contentColor = foreground)) {
            if (icon != null) Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp))
            if (icon != null) Spacer(Modifier.size(8.dp))
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun RoundedPreference(title: String, summary: String? = null, value: String? = null,
                               onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = summary?.let { { Text(it) } },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) Text(value, modifier = Modifier.widthIn(max = 180.dp),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(AppIcons.ChevronForward, contentDescription = null)
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .clickable(role = Role.Button, onClick = onClick),
    )
}

@Composable
internal fun RoundedSwitchPreference(title: String, summary: String? = null, checked: Boolean,
                                     icon: ImageVector? = null,
                                     onCheckedChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
        .heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (icon != null) AccentIconTile(icon, colors.primary, size = 42.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, fontSize = 16.sp, color = colors.onSurface)
            if (summary != null) SupportingText(summary)
        }
        Switch(checked = checked, onCheckedChange = null, thumbContent = {
            Icon(if (checked) AppIcons.Check else AppIcons.Close,
                contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize))
        })
    }
}

@Composable
internal fun AppDialog(show: Boolean, title: String, summary: String? = null,
                       onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val visibility = remember { MutableTransitionState(false) }
    LaunchedEffect(show) { visibility.targetState = show }
    if (!show && !visibility.currentState && visibility.isIdle) return
    val dark = LocalAppDarkTheme.current
    val shape = RoundedCornerShape(28.dp)
    Dialog(onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val context = LocalContext.current
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val density = LocalDensity.current
        val manager = remember(context) {
            context.applicationContext.getSystemService(WindowManager::class.java)
        }
        var blurEnabled by remember(manager) {
            mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                manager?.isCrossWindowBlurEnabled == true)
        }
        DisposableEffect(manager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && manager != null) {
                val listener = Consumer<Boolean> { blurEnabled = it }
                manager.addCrossWindowBlurEnabledListener(context.mainExecutor, listener)
                onDispose { manager.removeCrossWindowBlurEnabledListener(listener) }
            } else onDispose { }
        }
        SideEffect {
            window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window?.setDimAmount(if (blurEnabled) {
                if (dark) 0.36f else 0.28f
            } else if (dark) 0.52f else 0.38f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && window != null) {
                if (blurEnabled) window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                window.attributes = window.attributes.apply {
                    setBlurBehindRadius(if (blurEnabled) with(density) { 16.dp.roundToPx() } else 0)
                }
            }
        }
        AnimatedVisibility(visibleState = visibility,
            enter = fadeIn(tween(170)) + scaleIn(initialScale = 0.94f, animationSpec = tween(240)),
            exit = fadeOut(tween(140)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180))) {
            Box(modifier = Modifier.widthIn(max = 448.dp).fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp).imePadding()) {
                Column(modifier = Modifier.fillMaxWidth().shadow(18.dp, shape).clip(shape)
                    .background(Brush.verticalGradient(if (dark)
                        listOf(Color(0xFF26394D).copy(alpha = if (blurEnabled) 0.90f else 0.97f),
                            Color(0xFF1A2A3D).copy(alpha = if (blurEnabled) 0.92f else 0.98f))
                    else listOf(Color.White.copy(alpha = if (blurEnabled) 0.94f else 0.98f),
                        Color(0xFFF0F5FC).copy(alpha = if (blurEnabled) 0.92f else 0.97f))))
                    .border(0.75.dp, if (dark) Color(0xFFB5CCE7).copy(alpha = 0.25f)
                        else Color.White.copy(alpha = 0.72f), shape)
                    .verticalScroll(rememberScrollState()).padding(20.dp)) {
                    Text(title, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    if (summary != null) {
                        Spacer(Modifier.height(6.dp))
                        SupportingText(summary)
                    }
                    Spacer(Modifier.height(20.dp))
                    content()
                }
            }
        }
    }
}

@Composable
internal fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val background by animateColorAsState(
        targetValue = when {
            pressed || focused -> colors.primary.copy(alpha = 0.15f)
            selected -> colors.primary.copy(alpha = 0.18f)
            else -> colors.onSurface.copy(alpha = 0.025f)
        }, animationSpec = tween(if (pressed) 90 else 180), label = "choiceBackground")
    val shape = RoundedCornerShape(14.dp)
    Row(modifier = Modifier.fillMaxWidth().clip(shape)
        .background(background)
        .border(0.75.dp, if (selected) colors.primary.copy(alpha = 0.28f)
            else colors.onSurface.copy(alpha = 0.06f), shape)
        .selectable(selected = selected, role = Role.RadioButton,
            interactionSource = interaction, indication = null, onClick = onClick)
        .heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) colors.primary else colors.onSurface)
        RadioButton(selected = selected, onClick = null)
    }
}
