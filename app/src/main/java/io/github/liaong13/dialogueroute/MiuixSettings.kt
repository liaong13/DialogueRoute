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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.Msg
import io.github.liaong13.dialogueroute.core.ModelProvider
import io.github.liaong13.dialogueroute.core.PowerSetup
import io.github.liaong13.dialogueroute.core.Prefs
import io.github.liaong13.dialogueroute.core.ProviderSelection
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import java.io.ByteArrayOutputStream

@Composable
internal fun SettingsScreen(bottomTabBarHeight: Dp, onDirtyChange: (Boolean) -> Unit) {
    SettingsContent(bottomTabBarHeight, onDirtyChange)
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    GlassCard(padding = PaddingValues(16.dp), content = content)
}

@Composable
private fun ProviderSettingsCard(provider: ModelProvider, initiallyExpanded: Boolean,
                                 content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable(provider.id) { mutableStateOf(initiallyExpanded) }
    SettingsCard {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .semantics { stateDescription = if (expanded) "已展开" else "已折叠" }
            .clickable(role = Role.Button, onClickLabel = if (expanded) "收起供应商" else "配置供应商") {
                expanded = !expanded
            }.padding(4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(provider.name.ifBlank { ModelProvider.label(provider.kind) },
                    modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(if (expanded) "收起" else "配置", fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary)
            }
            Text(ModelProvider.label(provider.kind), fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            StatusBadge(if (provider.key.isNotBlank()) "已填写密钥" else "待填写密钥",
                highlighted = provider.key.isNotBlank())
        }
        AnimatedVisibility(visible = expanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(150)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(100))) {
            Column {
                Spacer(Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
private fun ModelSettingsCard(title: String, summary: String, provider: String, model: String,
                              configured: Boolean, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.985f else 1f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 500f), label = "modelCardPress")
    SettingsCard {
        Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .semantics { stateDescription = if (expanded) "已展开" else "已折叠" }
            .clickable(interactionSource = interaction, indication = null,
                role = Role.Button, onClickLabel = if (expanded) "收起配置" else "展开配置") {
                expanded = !expanded
            }.padding(4.dp).animateContentSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text(if (expanded) "收起" else "配置", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text("$provider · ${model.ifBlank { "未填写模型" }}", maxLines = 1,
                overflow = TextOverflow.Ellipsis, fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun SettingsContent(bottomTabBarHeight: Dp, onDirtyChange: (Boolean) -> Unit) {
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
    val initialProviders = remember(prefs) {
        val saved = ModelProvider.decode(prefs.modelProviders)
        if (saved.isNotEmpty() &&
            saved.any { it.id == prefs.judgeProviderId && it.canJudge } &&
            saved.any { it.id == prefs.replyProviderId && it.canChat } &&
            (prefs.visionProviderId.isBlank() ||
                saved.any { it.id == prefs.visionProviderId && it.canVision })) {
            ProviderSelection(saved, prefs.judgeProviderId, prefs.replyProviderId,
                prefs.visionProviderId)
        } else ModelProvider.fromLegacy(prefs)
    }
    var providers by remember { mutableStateOf(initialProviders.providers) }
    var newProviderId by remember { mutableStateOf("") }
    var judgeProviderId by remember { mutableStateOf(initialProviders.judgeId) }
    var replyProviderId by remember { mutableStateOf(initialProviders.replyId) }
    var visionProviderId by remember { mutableStateOf(initialProviders.visionId) }
    var judgeModel by remember { mutableStateOf(prefs.judgeModel) }
    var replyModel by remember { mutableStateOf(prefs.replyModel) }
    var visionModel by remember { mutableStateOf(prefs.visionModel) }
    var relationship by remember { mutableStateOf(prefs.relationship) }
    var whitelist by remember { mutableStateOf(prefs.whitelist.joinToString("\n")) }
    var autoAnalyze by remember { mutableStateOf(prefs.autoAnalyze) }
    var ocrEngine by remember { mutableStateOf(prefs.ocrEngine) }
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
    val currentValues = listOf(themeMode, xposed, providers, judgeProviderId, replyProviderId,
        visionProviderId, judgeModel, replyModel, visionModel, relationship, whitelist,
        autoAnalyze, ocrEngine, ocrFallback, ocrAuto, historyEnabled, historyCount, opacity)
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
        Column(modifier = Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    PageHeading("设置", "管理模型、权限与使用偏好。")
                }
                Box(Modifier.padding(top = 12.dp)) {
                    StatusBadge(if (dirty) "未保存" else "已保存", highlighted = dirty)
                }
            }
            GlassTabs(tabs = listOf("模型", "权限", "偏好"),
                selectedTabIndex = section, onTabSelected = { section = it })
        }
        Crossfade(targetState = section, modifier = Modifier.weight(1f),
            animationSpec = tween(180), label = "settingsSection") { visibleSection ->
            LazyColumn(modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, top = 4.dp, end = 20.dp,
                    bottom = if (dirty) 20.dp else bottomTabBarHeight + 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (visibleSection == 1) {
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
                            }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("当前适配微信 8.0.78（3180）",
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                if (visibleSection == 0) {
                    item { SectionHeading("1 · 供应商与密钥") }
                    item { SupportingText("先添加供应商和密钥；同一个供应商可用于多个模型，密钥不会跨供应商借用。") }
                    items(providers.size, key = { providers[it].id }) { index ->
                        val provider = providers[index]
                        ProviderSettingsCard(provider, initiallyExpanded = provider.id == newProviderId) {
                            UiField("名称", provider.name, { value ->
                                providers = providers.toMutableList().also {
                                    it[index] = provider.copy(name = value)
                                }
                            })
                            if (provider.kind == ModelProvider.CUSTOM_CHAT ||
                                provider.kind == ModelProvider.CUSTOM_JUDGE) {
                                UiField(if (provider.kind == ModelProvider.CUSTOM_JUDGE)
                                    "完整判断 URL" else "兼容接口 Base URL", provider.baseUrl,
                                    { value -> providers = providers.toMutableList().also {
                                        it[index] = provider.copy(baseUrl = value)
                                    } })
                            }
                            UiField("API Key", provider.key, { value ->
                                providers = providers.toMutableList().also {
                                    it[index] = provider.copy(key = value)
                                }
                            }, visualTransformation = PasswordVisualTransformation())
                            SecondaryAction("移除供应商") {
                                providers = providers.filterNot { it.id == provider.id }
                                if (judgeProviderId == provider.id) judgeProviderId = ""
                                if (replyProviderId == provider.id) replyProviderId = ""
                                if (visionProviderId == provider.id) visionProviderId = ""
                            }
                        }
                    }
                    item {
                        ProviderKindPicker { kind ->
                            val provider = ModelProvider.create(kind)
                            val count = providers.count { it.kind == kind }
                            val added = provider.copy(name = if (count == 0)
                                provider.name else "${provider.name} ${count + 1}")
                            providers = providers + added
                            newProviderId = added.id
                        }
                    }
                    item { SectionHeading("2 · 模型用途") }
                    item(key = "judge") {
                        ModelSettingsCard("判断模型", "识别对方意图，为候选回复排序。",
                            provider = providers.find { it.id == judgeProviderId }?.name ?: "未选择",
                            model = judgeModel, configured = providers.find {
                                it.id == judgeProviderId }?.key?.isNotBlank() == true) {
                            RouteProviderPicker(providers.filter { it.canJudge }, judgeProviderId) {
                                judgeProviderId = it
                                val kind = providers.find { provider -> provider.id == it }?.kind
                                judgeModel = when (kind) {
                                    ModelProvider.TYPESAFE -> Prefs.DEFAULT_JUDGE_MODEL_TYPESAFE
                                    ModelProvider.OPENROUTER -> Prefs.DEFAULT_JUDGE_MODEL_OPENROUTER
                                    else -> ""
                                }
                            }
                            ModelPicker("判断模型", judgeModel,
                                modelSuggestions(providers.find { it.id == judgeProviderId }?.kind, 0)) {
                                judgeModel = it
                            }
                            UiField("模型", judgeModel, { judgeModel = it })
                            SecondaryAction("测试判断") {
                                val provider = providers.find { it.id == judgeProviderId }
                                judgeResult = if (provider == null) "请先选择供应商" else "测试中…"
                                if (provider != null) scope.launch { judgeResult = testJudge(context,
                                    provider.judgeMode(), provider.judgeBase(), provider.key,
                                    judgeModel, relationship) }
                            }
                            if (judgeResult.isNotEmpty()) Text(judgeResult,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item(key = "reply") {
                        ModelSettingsCard("回复模型", "生成回复选项，支持 OpenAI 兼容接口。",
                            provider = providers.find { it.id == replyProviderId }?.name ?: "未选择",
                            model = replyModel, configured = providers.find {
                                it.id == replyProviderId }?.key?.isNotBlank() == true) {
                            RouteProviderPicker(providers.filter { it.canChat }, replyProviderId) {
                                replyProviderId = it
                                replyModel = modelSuggestions(
                                    providers.find { provider -> provider.id == it }?.kind, 1)
                                    .firstOrNull() ?: ""
                            }
                            ModelPicker("回复模型", replyModel,
                                modelSuggestions(providers.find { it.id == replyProviderId }?.kind, 1)) {
                                replyModel = it
                            }
                            UiField("模型", replyModel, { replyModel = it })
                            SecondaryAction("测试回复") {
                                val provider = providers.find { it.id == replyProviderId }
                                replyResult = if (provider == null) "请先选择供应商" else "测试中…"
                                if (provider != null) scope.launch { replyResult = testReply(context,
                                    provider.chatBase(), provider.key, replyModel) }
                            }
                            if (replyResult.isNotEmpty()) Text(replyResult,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item(key = "vision") {
                        ModelSettingsCard(if (ocrEngine == Prefs.OCR_VISION) "视觉模型 · OCR 已启用"
                            else "视觉模型 · 可选",
                            "选择视觉 OCR 后用于识别聊天截图，截图会发送给该供应商。",
                            provider = providers.find { it.id == visionProviderId }?.name ?: "未选择",
                            model = visionModel, configured = providers.find {
                                it.id == visionProviderId }?.key?.isNotBlank() == true) {
                            RouteProviderPicker(providers.filter { it.canVision }, visionProviderId,
                                optional = true) {
                                visionProviderId = it
                                visionModel = modelSuggestions(
                                    providers.find { provider -> provider.id == it }?.kind, 2)
                                    .firstOrNull() ?: ""
                            }
                            ModelPicker("视觉模型", visionModel,
                                modelSuggestions(providers.find { it.id == visionProviderId }?.kind, 2)) {
                                visionModel = it
                            }
                            UiField("模型", visionModel, { visionModel = it })
                            SecondaryAction("测试视觉") {
                                val provider = providers.find { it.id == visionProviderId }
                                visionResult = if (provider == null) "请先选择供应商" else "测试中…"
                                if (provider != null) scope.launch { visionResult = testVision(context,
                                    provider.chatBase(), provider.key, visionModel) }
                            }
                            if (visionResult.isNotEmpty()) Text(visionResult,
                                color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    item { SupportingText("聊天内容与选中的知识库上下文会发送至所配置的模型服务。") }
                }
                if (visibleSection == 2) {
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
                            Spacer(Modifier.height(12.dp))
                            Text("OCR 识别方式", fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface)
                            Column(Modifier.selectableGroup()) {
                                ChoiceRow("本地 OCR（离线）", ocrEngine == Prefs.OCR_MLKIT) {
                                    ocrEngine = Prefs.OCR_MLKIT
                                }
                                ChoiceRow("视觉模型 OCR", ocrEngine == Prefs.OCR_VISION) {
                                    ocrEngine = Prefs.OCR_VISION
                                }
                            }
                            SupportingText(if (ocrEngine == Prefs.OCR_VISION)
                                "保存后，自动 OCR 补采和手动截屏识别都会把裁剪后的聊天截图发送给选定的视觉供应商，可能产生费用。关闭下方自动分析仍会进行 OCR；请先配置并测试视觉模型。"
                            else "在设备上识别，不发送截图给视觉模型；两种方式都需要截图权限。")
                            RoundedSwitchPreference(title = "无法读取正文时使用 OCR", checked = ocrFallback,
                                onCheckedChange = { ocrFallback = it })
                            RoundedSwitchPreference(title = "OCR 模式自动分析",
                                summary = "关闭后自动补采仍会识别，需点悬浮球分析；手动识别仍会直接分析",
                                checked = ocrAuto, onCheckedChange = { ocrAuto = it })
                            RoundedSwitchPreference(title = "记录聊天历史", summary = "只保存在本机，用于关联上下文",
                                checked = historyEnabled, onCheckedChange = { historyEnabled = it })
                            UiField("注入最近历史条数（0–100）", historyCount, { historyCount = it })
                        }
                    }
                    item { SectionHeading("悬浮窗外观设置") }
                    item {
                        SettingsCard {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text("不透明度", modifier = Modifier.weight(1f))
                                Text("${opacity.toInt()}%")
                            }
                            SupportingText("越低越透，越能看清聊天内容")
                            Slider(value = opacity, onValueChange = { opacity = it },
                                valueRange = 60f..100f)
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
                                Text(kbResult, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Spacer(Modifier.height(12.dp))
                            RoundedPreference(title = "清空知识库与历史", summary = "删除全部本地笔记、联系人及聊天历史",
                                onClick = { clearDialog = true })
                        }
                    }
                }
            }
        }
        if (dirty) Column(modifier = Modifier.fillMaxWidth().padding(
            start = 20.dp, top = 8.dp, end = 20.dp, bottom = bottomTabBarHeight + 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryAction("保存设置") {
                val judge = providers.find { it.id == judgeProviderId && it.canJudge }
                val reply = providers.find { it.id == replyProviderId && it.canChat }
                val vision = providers.find { it.id == visionProviderId && it.canVision }
                val invalid = when {
                    judge == null || reply == null -> "请为判断和回复模型选择可用供应商"
                    ocrEngine == Prefs.OCR_VISION && vision == null -> "视觉 OCR 需要先选择视觉供应商"
                    visionProviderId.isNotBlank() && vision == null -> "请选择可用的视觉供应商"
                    ocrEngine == Prefs.OCR_VISION && vision?.key.isNullOrBlank() -> "请填写视觉供应商密钥"
                    providers.any { it.name.isBlank() } -> "请填写供应商名称"
                    providers.any { (it.kind == ModelProvider.CUSTOM_CHAT ||
                        it.kind == ModelProvider.CUSTOM_JUDGE) &&
                        !(it.baseUrl.startsWith("https://") || it.baseUrl.startsWith("http://")) } ->
                        "自定义接口需要完整的 http(s) 地址"
                    judgeModel.isBlank() || replyModel.isBlank() ||
                        (vision != null && visionModel.isBlank()) -> "请填写模型名称"
                    else -> null
                }
                if (invalid != null) {
                    Toast.makeText(context, invalid, Toast.LENGTH_SHORT).show()
                    return@PrimaryAction
                }
                prefs.saveModelConfiguration(ProviderSelection(providers.map {
                    it.copy(name = it.name.trim(), baseUrl = it.baseUrl.trim(), key = it.key.trim())
                }, judgeProviderId, replyProviderId, visionProviderId),
                    judgeModel, replyModel, visionModel)
                prefs.relationship = relationship
                prefs.whitelist = whitelist.lines().map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                prefs.autoAnalyze = autoAnalyze
                prefs.ocrEngine = ocrEngine
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
private fun ChoicePicker(title: String, labels: List<String>, ids: List<String>, selected: String,
                         emptyLabel: String = "请选择", onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = labels.getOrNull(ids.indexOf(selected)) ?: emptyLabel
    RoundedPreference(title = title, value = selectedLabel,
        onClick = { expanded = true })
    Spacer(Modifier.height(8.dp))
    AppDialog(show = expanded, title = title,
        onDismissRequest = { expanded = false }) {
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            labels.forEachIndexed { index, label ->
                ChoiceRow(label, selected = ids[index] == selected) {
                    onSelect(ids[index])
                    expanded = false
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        SecondaryAction("取消") { expanded = false }
    }
}

@Composable
private fun ProviderKindPicker(onAdd: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    SecondaryAction("添加供应商") { expanded = true }
    AppDialog(show = expanded, title = "添加供应商", onDismissRequest = { expanded = false }) {
        val kinds = listOf(ModelProvider.OPENROUTER, ModelProvider.TYPESAFE,
            ModelProvider.DEEPSEEK, ModelProvider.DASHSCOPE,
            ModelProvider.CUSTOM_CHAT, ModelProvider.CUSTOM_JUDGE)
        Column(modifier = Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            kinds.forEach { kind -> ChoiceRow(ModelProvider.label(kind), selected = false) {
                onAdd(kind)
                expanded = false
            } }
        }
        Spacer(Modifier.height(16.dp))
        SecondaryAction("取消") { expanded = false }
    }
}

@Composable
private fun RouteProviderPicker(providers: List<ModelProvider>, selected: String,
                                optional: Boolean = false, onSelect: (String) -> Unit) {
    if (providers.isEmpty() && !optional) SupportingText("请先添加支持此用途的供应商")
    else ChoicePicker("供应商", (if (optional) listOf("不配置视觉") else emptyList()) +
        providers.map { it.name }, (if (optional) listOf("") else emptyList()) +
        providers.map { it.id }, selected, onSelect = onSelect)
}

@Composable
private fun ModelPicker(title: String, selected: String, suggestions: List<String>,
                        onSelect: (String) -> Unit) {
    if (suggestions.isNotEmpty()) ChoicePicker("$title · 常用选项", suggestions,
        suggestions, selected, emptyLabel = "使用下方自填模型", onSelect = onSelect)
}

private fun modelSuggestions(kind: String?, route: Int): List<String> = when (route) {
    0 -> when (kind) {
        ModelProvider.OPENROUTER -> listOf(Prefs.DEFAULT_JUDGE_MODEL_OPENROUTER)
        ModelProvider.TYPESAFE -> listOf(Prefs.DEFAULT_JUDGE_MODEL_TYPESAFE)
        else -> emptyList()
    }
    1 -> when (kind) {
        ModelProvider.OPENROUTER -> listOf(Prefs.DEFAULT_REPLY_MODEL)
        ModelProvider.DEEPSEEK -> listOf(Prefs.DEEPSEEK_MODEL)
        ModelProvider.DASHSCOPE -> listOf(Prefs.DASHSCOPE_MODEL)
        else -> emptyList()
    }
    else -> when (kind) {
        ModelProvider.OPENROUTER -> listOf(Prefs.DEFAULT_VISION_MODEL)
        ModelProvider.DASHSCOPE -> listOf(Prefs.DASHSCOPE_VISION_MODEL)
        else -> emptyList()
    }
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

private suspend fun testReply(context: Context, base: String,
                              key: String, model: String): String = withContext(Dispatchers.IO) {
    if (key.isBlank()) return@withContext "请先填写供应商密钥"
    val route = scratch(context, "dialogue_route_scratch_reply") {
        replyBaseUrl = base.ifBlank { Prefs.DEFAULT_REPLY_BASE }
        replyKey = key
        replyModel = model.ifBlank { Prefs.DEFAULT_REPLY_MODEL }
    }
    if (runCatching { ReplyClient(route).ping() }.isSuccess) "回复接口连通成功"
    else "回复接口测试失败，请检查配置"
}

private suspend fun testVision(context: Context, base: String, key: String,
                               model: String): String =
    withContext(Dispatchers.IO) {
        val effectiveBase = base.ifBlank { Prefs.DEFAULT_VISION_BASE }
        if (!VisionClient.supportsVision(effectiveBase)) return@withContext "当前接口不支持视觉"
        if (key.isBlank()) return@withContext "请先填写供应商密钥"
        val route = scratch(context, "dialogue_route_scratch_vision") {
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
