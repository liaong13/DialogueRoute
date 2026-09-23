package io.github.liaong13.dialogueroute

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TextFieldDefaults
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PageHeading(title: String, subtitle: String? = null) {
    Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
        Text(title, style = MiuixTheme.textStyles.title1, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Spacer(Modifier.height(8.dp))
            SupportingText(subtitle)
        }
    }
}

@Composable
internal fun SectionHeading(title: String) {
    Text(title, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp, bottom = 4.dp))
}

@Composable
internal fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(content = content)
}

@Composable
internal fun SupportingText(text: String) {
    Text(text, fontSize = 14.sp, lineHeight = 21.sp,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
}

@Composable
internal fun StatusBadge(label: String, highlighted: Boolean = false) {
    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
        color = if (highlighted) MiuixTheme.colorScheme.primary
            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.background(
            if (highlighted) MiuixTheme.colorScheme.primaryContainer
            else MiuixTheme.colorScheme.surface,
            RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 5.dp))
}

@Composable
internal fun SetupStatusRow(title: String, description: String, status: String, ready: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            StatusBadge(status, highlighted = ready)
        }
        SupportingText(description)
    }
}

@Composable
internal fun EmptyState(title: String, description: String) {
    SectionCard {
        Spacer(Modifier.height(12.dp))
        Text(title, style = MiuixTheme.textStyles.title4, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        SupportingText(description)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun UiField(label: String, value: String, onValueChange: (String) -> Unit,
                     singleLine: Boolean = true,
                     visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
                         androidx.compose.ui.text.input.VisualTransformation.None) {
    val secret = visualTransformation is PasswordVisualTransformation
    var revealed by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val colors = MiuixTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, fontSize = 13.sp, color = colors.onSurfaceVariantSummary)
        BasicTextField(value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = label },
            textStyle = MiuixTheme.textStyles.main.copy(fontSize = 16.sp, lineHeight = 22.sp,
                color = colors.onSurface), singleLine = singleLine,
            minLines = if (singleLine) 1 else 3, maxLines = if (singleLine) 1 else 6,
            visualTransformation = if (secret && revealed) VisualTransformation.None else visualTransformation,
            keyboardOptions = if (secret) KeyboardOptions(keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false) else KeyboardOptions.Default,
            interactionSource = interaction, cursorBrush = SolidColor(colors.primary),
            decorationBox = { input ->
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(colors.surface.copy(alpha = 0.65f))
                    .border(if (focused) 1.5.dp else 0.75.dp,
                        if (focused) colors.primary else colors.onSurface.copy(alpha = 0.10f),
                        RoundedCornerShape(12.dp))
                    .heightIn(min = 48.dp).padding(start = 12.dp, end = if (secret) 0.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).padding(vertical = 12.dp)) { input() }
                    if (secret) Box(modifier = Modifier.clip(RoundedCornerShape(12.dp))
                        .clickable(role = Role.Button, onClickLabel = if (revealed) "隐藏密钥" else "显示密钥") {
                            revealed = !revealed
                        }.heightIn(min = 48.dp).padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center) {
                        Text(if (revealed) "隐藏" else "显示", fontSize = 13.sp, color = colors.primary)
                    }
                }
            })
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
internal fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, colors = ButtonDefaults.buttonColorsPrimary(),
        cornerRadius = 14.dp, insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, fontSize = 16.sp, color = MiuixTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun SecondaryAction(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, cornerRadius = 14.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(color = MiuixTheme.colorScheme.surface,
            contentColor = MiuixTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text(label, fontSize = 15.sp, color = MiuixTheme.colorScheme.primary)
    }
}

@Composable
internal fun RoundedPreference(title: String, summary: String? = null, value: String? = null,
                               onClick: () -> Unit) {
    ArrowPreference(title = title, summary = summary,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).heightIn(min = 52.dp),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        endActions = {
            if (value != null) Text(value, modifier = Modifier.widthIn(max = 180.dp), fontSize = 14.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }, onClick = onClick)
}

@Composable
internal fun RoundedSwitchPreference(title: String, summary: String? = null, checked: Boolean,
                                     onCheckedChange: (Boolean) -> Unit) {
    SwitchPreference(title = title, summary = summary, checked = checked,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).heightIn(min = 52.dp),
        insideMargin = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        onCheckedChange = onCheckedChange)
}

@Composable
internal fun AppDialog(show: Boolean, title: String, summary: String? = null,
                       onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (!show) return
    Dialog(onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.widthIn(max = 448.dp).fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 24.dp).imePadding()) {
            Column(modifier = Modifier.fillMaxWidth().glassSurface(radius = 28.dp, floating = true)
                .verticalScroll(rememberScrollState()).padding(20.dp)) {
                Text(title, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold)
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

@Composable
internal fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val colors = MiuixTheme.colorScheme
    val background by animateColorAsState(
        targetValue = when {
            pressed || focused -> colors.primary.copy(alpha = 0.20f)
            selected -> colors.primaryContainer
            else -> Color.Transparent
        }, animationSpec = tween(if (pressed) 90 else 180), label = "choiceBackground")
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
        .background(background)
        .selectable(selected = selected, role = Role.RadioButton,
            interactionSource = interaction, indication = null, onClick = onClick)
        .heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            color = if (selected) colors.primary else colors.onSurface)
        Box(modifier = Modifier.size(20.dp)
            .border(if (selected) 2.dp else 1.dp,
                if (selected) colors.primary else colors.onSurfaceVariantSummary, CircleShape),
            contentAlignment = Alignment.Center) {
            if (selected) Box(Modifier.size(10.dp).background(colors.primary, CircleShape))
        }
    }
}
