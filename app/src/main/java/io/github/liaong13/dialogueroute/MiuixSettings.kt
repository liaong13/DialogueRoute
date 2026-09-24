package io.github.liaong13.dialogueroute

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.Msg
import io.github.liaong13.dialogueroute.core.PowerSetup
import io.github.liaong13.dialogueroute.core.Prefs
import io.github.liaong13.dialogueroute.core.kb.KbSelfCheck
import io.github.liaong13.dialogueroute.core.kb.KbStore
import io.github.liaong13.dialogueroute.jev.JudgeClient
import io.github.liaong13.dialogueroute.jev.ReplyClient
import io.github.liaong13.dialogueroute.jev.VisionClient
import io.github.liaong13.dialogueroute.xposed.XposedCaptureRuntime
import io.github.liaong13.dialogueroute.xposed.XposedProbeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.io.ByteArrayOutputStream

@Composable
internal fun SettingsScreen(onDirtyChange: (Boolean) -> Unit) {
    val base = MiuixTheme.textStyles
    val styles = remember(base) {
        base.copy(
            main = base.main.copy(fontSize = 16.sp, lineHeight = 22.sp),
            paragraph = base.paragraph.copy(fontSize = 14.sp, lineHeight = 20.sp),
            body1 = base.body1.copy(fontSize = 15.sp, lineHeight = 21.sp),
            body2 = base.body2.copy(fontSize = 13.sp, lineHeight = 19.sp),
            title4 = base.title4.copy(fontSize = 18.sp, lineHeight = 24.sp),
        )
    }
    MiuixTheme(textStyles = styles) { SettingsContent(onDirtyChange) }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(padding = PaddingValues(16.dp), content = content)
}

@Composable
private fun ModelSettingsCard(title: String, summary: String, provider: String, model: String,
                              configured: Boolean, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    SettingsCard {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .semantics { stateDescription = if (expanded) "已展开" else "已折叠" }
            .clickable(role = Role.Button, onClickLabel = if (expanded) "收起配置" else "展开配置") {
                expanded = !expanded
            }.padding(4.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, modifier = Modifier.weight(1f), style = MiuixTheme.textStyles.title4,
                    fontWeight = FontWeight.SemiBold)
                Text(if (expanded) "收起" else "配置", fontSize = 14.sp, color = MiuixTheme.colorScheme.primary)
            }
            Text("$provider · ${model.ifBlank { "未填写模型" }}", maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            StatusBadge(if (configured) "已填写密钥" else "待填写密钥", highlighted = configured)
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically(tween(200)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(100))) {
            Column {
                Spacer(Modifier.height(16.dp))
                SupportingText(summary)
                Spacer(Modifier.height(12.dp))
                content()
            }
        }
    }
}

@Composable
private fun SettingsContent(onDirtyChange: (Boolean) -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { Prefs(context) }
    val scope = rememberCoroutineScope()
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    var xposed by remember { mutableStateOf(prefs.xposedEnabled) }
    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var judgeProvider by remember { mutableStateOf(prefs.judgeProvider) }
    var judgeBase by remember { mutableStateOf(prefs.judgeBaseUrl) }
    var judgeKey by remember { mutableStateOf(prefs.judgeKey) }
    var judgeModel by remember { mutableStateOf(prefs.judgeModel) }
    var replyBase by remember { mutableStateOf(prefs.replyBaseUrl) }
    var replyKey by remember { mutableStateOf(prefs.replyKey) }
    var replyModel by remember { mutableStateOf(prefs.replyModel) }
    var visionBase by remember { mutableStateOf(prefs.visionBaseUrl) }
    var visionKey by remember { mutableStateOf(prefs.visionKey) }
    var visionModel by remember { mutableStateOf(prefs.visionModel) }
    var relationship by remember { mutableStateOf(prefs.relationship) }
    var whitelist by remember { mutableStateOf(prefs.whitelist.joinToString("\n")) }
    var autoAnalyze by remember { mutableStateOf(prefs.autoAnalyze) }
    var ocrFallback by remember { mutableStateOf(prefs.ocrFallback) }
    var ocrAuto by remember { mutableStateOf(prefs.ocrAutoAnalyze) }
    var historyEnabled by remember { mutableStateOf(prefs.contextEnabled) }
    var historyCount by remember { mutableStateOf(prefs.contextHistoryCount.toString()) }
    var opacity by remember { mutableStateOf(prefs.overlayOpacity.toFloat()) }
    var judgeResult by remember { mutableStateOf("") }
    var replyResult by remember { mutableStateOf("") }
    var visionResult by remember { mutableStateOf("") }
    var kbResult by remember { mutableStateOf("") }
    var clearDialog by remember { mutableStateOf(false) }
    var section by remember { mutableIntStateOf(0) }
    val currentValues = listOf(themeMode, xposed, judgeProvider, judgeBase, judgeKey, judgeModel,
        replyBase, replyKey, replyModel, visionBase, visionKey, visionModel, relationship, whitelist,
        autoAnalyze, ocrFallback, ocrAuto, historyEnabled, historyCount, opacity)
    var savedValues by remember { mutableStateOf(currentValues) }
    val dirty = currentValues != savedValues
    LaunchedEffect(dirty) { onDirtyChange(dirty) }

    val a11y = remember(refresh) { Settings.Secure.getString(context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(
            "io.github.liaong13.dialogueroute/com.google.android.accessibility.selecttospeak.SelectToSpeakService") == true }
    val overlay = remember(refresh) { Settings.canDrawOverlays(context) }
    val battery = remember(refresh) {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)
    }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        Column(modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("设置", modifier = Modifier.weight(1f), fontSize = 28.sp,
                    lineHeight = 36.sp, fontWeight = FontWeight.Bold)
                StatusBadge(if (dirty) "未保存" else "已保存", highlighted = dirty)
            }
            GlassTabs(tabs = listOf("模型", "权限", "偏好"),
                selectedTabIndex = section, onTabSelected = { section = it })
        }
        key(section) {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (section == 1) {
                    item { SectionHeading("权限设置") }
                    item {
                        SettingsCard {
                            RoundedPreference(title = "无障碍权限", summary = if (a11y) "已开启" else "去开启",
                                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) })
                            RoundedPreference(title = "悬浮窗权限", summary = if (overlay) "已开启" else "去开启",
                                onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}"))) })
                        }
                    }
                    item { SectionHeading("后台运行") }
                    item {
                        SettingsCard {
                            RoundedPreference(title = "忽略系统电池优化", summary = if (battery) "已开启" else "去检查",
                                onClick = { openBatterySettings(context) })
                            RoundedPreference(title = "厂商省电策略", summary = "请检查省电无限制，无法自动检测",
                                onClick = { openAppDetails(context) })
                            RoundedPreference(title = "自启动", summary = "请检查重启后自动运行，无法自动检测",
                                onClick = { openAutostartSettings(context) })
                        }
                    }
                    item { SectionHeading("采集模式") }
                    item {
                        SettingsCard {
                            RoundedSwitchPreference(title = "Xposed 增强模式", summary = "保存后还需在框架中启用模块并勾选微信",
                                checked = xposed, onCheckedChange = { xposed = it })
                            Text(when {
                                !xposed -> "状态：未开启"
                                XposedProbeBridge.isContentActive(context) -> "状态：微信正文采集运行中"
                                (XposedProbeBridge.lastContentAgeMs(context) ?: Long.MAX_VALUE) < 600_000L ->
                                    "状态：最近成功读取微信正文"
                                else -> "状态：等待微信聊天页"
                            }, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                            Text("当前适配微信 8.0.78（3180）")
                        }
                    }
                }
                if (section == 0) {
                    item(key = "judge") {
                        ModelSettingsCard("判断模型", "识别对方意图，为候选回复排序。",
                            provider = when (judgeProvider) {
                                Prefs.PROVIDER_OPENROUTER -> "OpenRouter"
                                Prefs.PROVIDER_TYPESAFE -> "TypeSafe"
                                else -> "自定义"
                            }, model = judgeModel, configured = judgeKey.isNotBlank()) {
                            ProviderChoices(listOf("OpenRouter", "TypeSafe 直连", "自定义"),
                                listOf(Prefs.PROVIDER_OPENROUTER, Prefs.PROVIDER_TYPESAFE, Prefs.PROVIDER_CUSTOM),
                                judgeProvider) { chosen ->
                                judgeProvider = chosen
                                when (chosen) {
                                    Prefs.PROVIDER_OPENROUTER -> {
                                        judgeBase = Prefs.DEFAULT_JUDGE_BASE_OPENROUTER
                                        judgeModel = Prefs.DEFAULT_JUDGE_MODEL_OPENROUTER
                                    }
                                    Prefs.PROVIDER_TYPESAFE -> {
                                        judgeBase = Prefs.DEFAULT_JUDGE_BASE_TYPESAFE
                                        judgeModel = Prefs.DEFAULT_JUDGE_MODEL_TYPESAFE
                                    }
                                    else -> {
                                        judgeBase = when (judgeBase.trimEnd('/')) {
                                            Prefs.DEFAULT_JUDGE_BASE_OPENROUTER -> "$judgeBase/alpha/decisions"
                                            Prefs.DEFAULT_JUDGE_BASE_TYPESAFE -> "$judgeBase/v1/systemone"
                                            else -> judgeBase
                                        }
                                    }
                                }
                            }
                            UiField("Base URL", judgeBase, { judgeBase = it })
                            UiField("密钥", judgeKey, { judgeKey = it },
                                visualTransformation = PasswordVisualTransformation())
                            UiField("模型", judgeModel, { judgeModel = it })
                            SecondaryAction("测试判断") {
                                judgeResult = "测试中…"
                                scope.launch { judgeResult = testJudge(context, judgeProvider, judgeBase,
                                    judgeKey, judgeModel, relationship) }
                            }
                            if (judgeResult.isNotEmpty()) Text(judgeResult)
                        }
                    }
                    item(key = "reply") {
                        ModelSettingsCard("回复模型", "生成回复选项，支持 OpenAI 兼容接口。",
                            provider = providerLabel(replyBase), model = replyModel,
                            configured = replyKey.ifBlank { judgeKey }.isNotBlank()) {
                            ProviderChoices(listOf("OpenRouter", "DeepSeek 官方", "通义兼容", "自定义"),
                                listOf(Prefs.DEFAULT_REPLY_BASE, Prefs.DEEPSEEK_BASE, Prefs.DASHSCOPE_BASE, "custom"),
                                replyBase.trimEnd('/')) { chosen ->
                                if (chosen != "custom") {
                                    replyBase = chosen
                                    replyModel = when (chosen) {
                                        Prefs.DEEPSEEK_BASE -> Prefs.DEEPSEEK_MODEL
                                        Prefs.DASHSCOPE_BASE -> Prefs.DASHSCOPE_MODEL
                                        else -> Prefs.DEFAULT_REPLY_MODEL
                                    }
                                }
                            }
                            UiField("Base URL", replyBase, { replyBase = it })
                            UiField("密钥（留空则使用判断密钥）", replyKey, { replyKey = it },
                                visualTransformation = PasswordVisualTransformation())
                            UiField("模型", replyModel, { replyModel = it })
                            SecondaryAction("测试回复") {
                                replyResult = "测试中…"
                                scope.launch { replyResult = testReply(context, judgeKey, replyBase, replyKey, replyModel) }
                            }
                            if (replyResult.isNotEmpty()) Text(replyResult)
                        }
                    }
                    item(key = "vision") {
                        ModelSettingsCard("视觉模型 · 可选", "仅用于视觉连通测试；自动 OCR 使用本地模型。",
                            provider = providerLabel(visionBase.ifBlank { Prefs.DEFAULT_VISION_BASE }),
                            model = visionModel,
                            configured = visionKey.ifBlank { replyKey.ifBlank { judgeKey } }.isNotBlank()) {
                            ProviderChoices(listOf("OpenRouter", "通义兼容", "自定义"),
                                listOf(Prefs.DEFAULT_VISION_BASE, Prefs.DASHSCOPE_BASE, "custom"),
                                visionBase.trimEnd('/')) { chosen ->
                                if (chosen != "custom") {
                                    visionBase = chosen
                                    visionModel = if (chosen == Prefs.DASHSCOPE_BASE)
                                        Prefs.DASHSCOPE_VISION_MODEL else Prefs.DEFAULT_VISION_MODEL
                                }
                            }
                            UiField("Base URL", visionBase, { visionBase = it })
                            UiField("密钥（留空则回退到回复或判断密钥）", visionKey, { visionKey = it },
                                visualTransformation = PasswordVisualTransformation())
                            UiField("模型", visionModel, { visionModel = it })
                            SecondaryAction("测试视觉") {
                                visionResult = "测试中…"
                                scope.launch { visionResult = testVision(context, judgeKey, replyBase,
                                    replyKey, visionBase, visionKey, visionModel) }
                            }
                            if (visionResult.isNotEmpty()) Text(visionResult)
                        }
                    }
                    item { SupportingText("聊天内容与选中的知识库上下文会发送至所配置的模型服务。") }
                }
                if (section == 2) {
                    item { SectionHeading("外观") }
                    item {
                        SettingsCard {
                            SupportingText("默认使用浅色。保存后应用于主界面和悬浮窗。")
                            Spacer(Modifier.height(12.dp))
                            Column(Modifier.selectableGroup()) {
                                ChoiceRow("浅色", themeMode == Prefs.THEME_LIGHT) { themeMode = Prefs.THEME_LIGHT }
                                ChoiceRow("深色", themeMode == Prefs.THEME_DARK) { themeMode = Prefs.THEME_DARK }
                                ChoiceRow("跟随系统", themeMode == Prefs.THEME_SYSTEM) { themeMode = Prefs.THEME_SYSTEM }
                            }
                        }
                    }
                    item { SectionHeading("分析设置") }
                    item {
                        SettingsCard {
                            UiField("关系描述", relationship, { relationship = it }, singleLine = false)
                            UiField("会话白名单（每行一个，留空为全部）", whitelist,
                                { whitelist = it }, singleLine = false)
                            RoundedSwitchPreference(title = "收到新消息时自动分析", checked = autoAnalyze,
                                onCheckedChange = { autoAnalyze = it })
                            RoundedSwitchPreference(title = "树读不到正文时使用 OCR", checked = ocrFallback,
                                onCheckedChange = { ocrFallback = it })
                            RoundedSwitchPreference(title = "OCR 模式自动分析", summary = "关闭时识别后由用户点悬浮球分析",
                                checked = ocrAuto, onCheckedChange = { ocrAuto = it })
                            RoundedSwitchPreference(title = "记录聊天历史", summary = "只保存在本机，用于关联上下文",
                                checked = historyEnabled, onCheckedChange = { historyEnabled = it })
                            UiField("注入最近历史条数（0–100）", historyCount, { historyCount = it })
                        }
                    }
                    item { SectionHeading("悬浮窗外观设置") }
                    item {
                        SettingsCard {
                            SliderPreference(value = opacity, onValueChange = { opacity = it },
                                title = "不透明度", valueText = "${opacity.toInt()}%",
                                summary = "越低越透，越能看清聊天内容", valueRange = 60f..100f)
                        }
                    }
                    item { SectionHeading("数据管理") }
                    item {
                        SettingsCard {
                            SecondaryAction("运行知识库自检") {
                                kbResult = "自检中…"
                                scope.launch { kbResult = withContext(Dispatchers.IO) { KbSelfCheck.run(context) } }
                            }
                            if (kbResult.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(kbResult)
                            }
                            Spacer(Modifier.height(12.dp))
                            RoundedPreference(title = "清空知识库与历史", summary = "删除全部本地笔记、联系人及聊天历史",
                                onClick = { clearDialog = true })
                        }
                    }
                }
            }
        }
        if (dirty) Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryAction("保存设置") {
                val resolvedProvider = when (judgeBase.trim().trimEnd('/')) {
                    Prefs.DEFAULT_JUDGE_BASE_OPENROUTER -> Prefs.PROVIDER_OPENROUTER
                    Prefs.DEFAULT_JUDGE_BASE_TYPESAFE -> Prefs.PROVIDER_TYPESAFE
                    else -> judgeProvider
                }
                prefs.judgeProvider = resolvedProvider
                prefs.judgeBaseUrl = judgeBase.trim().ifBlank { when (resolvedProvider) {
                    Prefs.PROVIDER_TYPESAFE -> Prefs.DEFAULT_JUDGE_BASE_TYPESAFE
                    Prefs.PROVIDER_CUSTOM -> ""
                    else -> Prefs.DEFAULT_JUDGE_BASE_OPENROUTER
                } }
                prefs.judgeKey = judgeKey
                prefs.judgeModel = judgeModel.trim().ifBlank { when (resolvedProvider) {
                    Prefs.PROVIDER_TYPESAFE -> Prefs.DEFAULT_JUDGE_MODEL_TYPESAFE
                    Prefs.PROVIDER_CUSTOM -> ""
                    else -> Prefs.DEFAULT_JUDGE_MODEL_OPENROUTER
                } }
                prefs.replyBaseUrl = replyBase.trim().ifBlank { Prefs.DEFAULT_REPLY_BASE }
                prefs.replyKey = replyKey
                prefs.replyModel = replyModel.trim().ifBlank { Prefs.DEFAULT_REPLY_MODEL }
                prefs.visionBaseUrl = visionBase.trim()
                prefs.visionKey = visionKey
                prefs.visionModel = visionModel.trim().ifBlank { Prefs.DEFAULT_VISION_MODEL }
                prefs.relationship = relationship
                prefs.whitelist = whitelist.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                prefs.autoAnalyze = autoAnalyze
                prefs.ocrFallback = ocrFallback
                prefs.ocrAutoAnalyze = ocrAuto
                prefs.contextEnabled = historyEnabled
                prefs.contextHistoryCount = historyCount.trim().toIntOrNull()?.coerceIn(0, 100) ?: 30
                prefs.overlayOpacity = opacity.toInt()
                prefs.themeMode = themeMode
                if (prefs.xposedEnabled != xposed) {
                    prefs.xposedEnabled = xposed
                    XposedCaptureRuntime.get(context).onChatClosed()
                }
                savedValues = currentValues
                Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AppDialog(show = clearDialog, title = "清空知识库与历史",
        summary = "这会删除笔记、联系人和聊天历史，无法恢复。",
        onDismissRequest = { clearDialog = false }) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryAction("取消") { clearDialog = false }
            PrimaryAction("确认清空") {
                KbStore.get(context).clearAll()
                kbResult = "已清空知识库与历史"
                clearDialog = false
            }
        }
    }
}

@Composable
private fun ProviderChoices(labels: List<String>, ids: List<String>, selected: String,
                            onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = labels.getOrNull(ids.indexOf(selected)) ?: "自定义"
    RoundedPreference(title = "服务商", value = selectedLabel,
        onClick = { expanded = true })
    Spacer(Modifier.height(8.dp))
    AppDialog(show = expanded, title = "选择服务商",
        onDismissRequest = { expanded = false }) {
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                ChoiceRow(label, selected = ids[index] == selected ||
                    (ids[index] == "custom" && selected !in ids)) {
                    onSelect(ids[index])
                    expanded = false
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SecondaryAction("取消") { expanded = false }
    }
}

private fun providerLabel(base: String): String = when (base.trim().trimEnd('/')) {
    Prefs.DEFAULT_REPLY_BASE -> "OpenRouter"
    Prefs.DEEPSEEK_BASE -> "DeepSeek"
    Prefs.DASHSCOPE_BASE -> "通义"
    else -> "自定义"
}

private fun openAppDetails(context: Context) {
    runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}"))) }
}

private fun openBatterySettings(context: Context) {
    if (runCatching { context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}"))) }.isSuccess) return
    if (runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }.isSuccess) return
    openAppDetails(context)
}

private fun openAutostartSettings(context: Context) {
    for (route in PowerSetup.AUTOSTART_ROUTES) {
        if (runCatching { context.startActivity(Intent().setClassName(route.pkg, route.cls)) }.isSuccess) return
    }
    Toast.makeText(context, "未找到自启动页，请在应用详情中检查", Toast.LENGTH_LONG).show()
    openAppDetails(context)
}

private fun scratch(context: Context, name: String, fill: Prefs.() -> Unit): Prefs {
    context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
    return Prefs(context, name).apply(fill)
}

private suspend fun testJudge(context: Context, provider: String, base: String,
                              key: String, model: String, relationship: String): String =
    withContext(Dispatchers.IO) {
        if (key.isBlank()) return@withContext "请先填写判断密钥"
        if (provider == Prefs.PROVIDER_CUSTOM && (base.isBlank() || model.isBlank()))
            return@withContext "自定义接口需要完整 URL 和模型"
        val route = scratch(context, "dialogue_route_scratch_judge") {
            judgeProvider = provider
            judgeBaseUrl = base
            judgeKey = key
            judgeModel = model
        }
        val result = runCatching { JudgeClient(route).judge(
            ChatSnapshot("连通测试", listOf(Msg("other", "在吗？"), Msg("me", "在"))),
            relationship) }
        if (result.isSuccess && result.getOrNull()?.error == null) "判断接口连通成功"
        else "判断接口测试失败，请检查配置"
    }

private suspend fun testReply(context: Context, judgeKey: String, base: String,
                              key: String, model: String): String = withContext(Dispatchers.IO) {
    if (key.ifBlank { judgeKey }.isBlank()) return@withContext "请先填写密钥"
    val route = scratch(context, "dialogue_route_scratch_reply") {
        this.judgeKey = judgeKey
        replyBaseUrl = base.ifBlank { Prefs.DEFAULT_REPLY_BASE }
        replyKey = key
        replyModel = model.ifBlank { Prefs.DEFAULT_REPLY_MODEL }
    }
    if (runCatching { ReplyClient(route).ping() }.isSuccess) "回复接口连通成功"
    else "回复接口测试失败，请检查配置"
}

private suspend fun testVision(context: Context, judgeKey: String, replyBase: String,
                               replyKey: String, base: String, key: String, model: String): String =
    withContext(Dispatchers.IO) {
        val effectiveBase = base.ifBlank { Prefs.DEFAULT_VISION_BASE }
        if (!VisionClient.supportsVision(effectiveBase)) return@withContext "当前接口不支持视觉"
        if (key.ifBlank { replyKey.ifBlank { judgeKey } }.isBlank())
            return@withContext "请先填写密钥"
        val route = scratch(context, "dialogue_route_scratch_vision") {
            this.judgeKey = judgeKey
            this.replyBaseUrl = replyBase
            this.replyKey = replyKey
            visionBaseUrl = effectiveBase
            visionKey = key
            visionModel = model.ifBlank { Prefs.DEFAULT_VISION_MODEL }
        }
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val encoded = try {
            bitmap.eraseColor(android.graphics.Color.WHITE)
            val bytes = ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.toByteArray()
            }
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } finally { bitmap.recycle() }
        if (runCatching { VisionClient(route).ask(encoded, "这张图是什么颜色？只回答颜色。") }.isSuccess)
            "视觉接口连通成功" else "视觉接口测试失败，请检查配置"
    }
