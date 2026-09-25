package io.github.liaong13.dialogueroute

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import io.github.liaong13.dialogueroute.core.PowerSetup
import io.github.liaong13.dialogueroute.core.Prefs
import io.github.liaong13.dialogueroute.xposed.XposedProbeBridge
import kotlinx.coroutines.launch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import top.yukonga.miuix.kmp.basic.NavigationRail as MiuixNavigationRail
import top.yukonga.miuix.kmp.basic.NavigationRailItem as MiuixNavigationRailItem
import top.yukonga.miuix.kmp.basic.NavigationRailValue
import top.yukonga.miuix.kmp.basic.rememberNavigationRailState

class MiuixActivity : ComponentActivity() {
    private var requestedTab by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestedTab = intent.getIntExtra(EXTRA_TAB, 0).coerceIn(0, 2)
        setContent { DialogueApp(requestedTab) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        requestedTab = intent.getIntExtra(EXTRA_TAB, 0).coerceIn(0, 2)
    }

    companion object { const val EXTRA_TAB = "dialogue_route_tab" }
}

@Composable
private fun DialogueApp(requestedTab: Int) {
    val context = LocalContext.current
    val prefs = remember(context) { Prefs(context) }
    var themeMode by remember(context) { mutableStateOf(prefs.themeMode) }
    var dynamicColor by remember(context) { mutableStateOf(prefs.dynamicColor) }
    var uiStyle by remember(context) { mutableStateOf(prefs.uiStyle) }
    DisposableEffect(context) {
        val storage = context.getSharedPreferences(Prefs.PREFS_MAIN, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Prefs.KEY_THEME_MODE || key == null) themeMode = prefs.themeMode
            if (key == Prefs.KEY_DYNAMIC_COLOR || key == null) dynamicColor = prefs.dynamicColor
            if (key == Prefs.KEY_UI_STYLE || key == null) uiStyle = prefs.uiStyle
        }
        storage.registerOnSharedPreferenceChangeListener(listener)
        themeMode = prefs.themeMode
        dynamicColor = prefs.dynamicColor
        uiStyle = prefs.uiStyle
        onDispose { storage.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val systemDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        Prefs.THEME_DARK -> true
        Prefs.THEME_SYSTEM -> systemDark
        else -> false
    }
    SideEffect {
        (context as? android.app.Activity)?.window?.let { window ->
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }
    CompositionLocalProvider(LocalAppDarkTheme provides dark) {
        DialogueTheme(dark = dark, dynamicColor = dynamicColor, uiStyle = uiStyle) {
            val pager = rememberPagerState(initialPage = requestedTab, pageCount = { 3 })
            val scope = rememberCoroutineScope()
            var settingsDirty by remember { mutableStateOf(false) }
            var settingsRevision by remember { mutableIntStateOf(0) }
            var pendingDestination by remember { mutableStateOf<Int?>(null) }
            fun requestPage(index: Int) {
                if (index == pager.currentPage) return
                if (pager.currentPage == 2 && settingsDirty) pendingDestination = index
                else scope.launch {
                    if (ValueAnimator.areAnimatorsEnabled()) pager.animateScrollToPage(index)
                    else pager.scrollToPage(index)
                }
            }
            LaunchedEffect(requestedTab) {
                requestPage(requestedTab)
            }
            BackHandler(enabled = settingsDirty && pager.currentPage == 2) { pendingDestination = -1 }
            GlassBackdrop {
                val navigationSource = rememberHazeState()
                val density = LocalDensity.current
                var bottomTabBarHeight by remember { mutableStateOf(0.dp) }
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    val wide = maxWidth >= 840.dp
                    Row(Modifier.fillMaxSize()) {
                        if (wide) {
                            SideTabBar(pager.currentPage, ::requestPage)
                        }
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            HorizontalPager(state = pager, beyondViewportPageCount = 2,
                                userScrollEnabled = !settingsDirty,
                                modifier = Modifier.fillMaxSize().hazeSource(navigationSource)) { index ->
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                                    Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
                                        when (index) {
                                            0 -> HomeScreen(active = pager.currentPage == 0,
                                                bottomPadding = if (wide) 24.dp else 112.dp,
                                                onOpenSettings = { requestPage(2) })
                                            1 -> KnowledgeScreen(bottomPadding = if (wide) 24.dp else 112.dp)
                                            else -> key(settingsRevision) {
                                                SettingsScreen(bottomTabBarHeight = if (wide) 0.dp else bottomTabBarHeight,
                                                    onDirtyChange = { settingsDirty = it })
                                            }
                                        }
                                    }
                                }
                            }
                            if (!wide) GlassSource(navigationSource) {
                                Box(Modifier.align(Alignment.BottomCenter).onSizeChanged {
                                    bottomTabBarHeight = with(density) { it.height.toDp() }
                                }) {
                                    BottomTabBar(pager.currentPage, ::requestPage)
                                }
                            }
                        }
                    }
                }
            }
            AppDialog(show = pendingDestination != null, title = "设置尚未保存",
                summary = "继续编辑并保存，或放弃本次修改后离开。",
                onDismissRequest = { pendingDestination = null }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrimaryAction("继续编辑") { pendingDestination = null }
                    SecondaryAction("放弃修改并离开") {
                        val destination = pendingDestination
                        pendingDestination = null
                        settingsDirty = false
                        settingsRevision++
                        if (destination == -1) (context as? android.app.Activity)?.finish()
                        else if (destination != null) scope.launch {
                            if (ValueAnimator.areAnimatorsEnabled()) pager.animateScrollToPage(destination)
                            else pager.scrollToPage(destination)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SideTabBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    val tabs = listOf("首页", "知识库", "设置")
    val icons = listOf(AppIcons.Home, AppIcons.Knowledge, AppIcons.Settings)
    val selectedIcons = listOf(AppIcons.HomeSelected, AppIcons.KnowledgeSelected, AppIcons.SettingsSelected)
    val miuix = LocalMiuixStyle.current
    if (miuix) {
        val railState = rememberNavigationRailState(NavigationRailValue.Expanded)
        MiuixNavigationRail(modifier = Modifier.width(176.dp).fillMaxHeight()
            .padding(12.dp).glassSurface(radius = 26.dp),
            state = railState, color = Color.Transparent, showDivider = false,
            defaultWindowInsetsPadding = false, minWidth = 152.dp, expandedWidth = 152.dp) {
            tabs.forEachIndexed { index, title ->
                MiuixNavigationRailItem(selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    icon = if (index == selectedIndex) selectedIcons[index] else icons[index],
                    label = title)
            }
        }
        return
    }
    val indicatorY by animateDpAsState(
        targetValue = 64.dp * selectedIndex,
        animationSpec = if (ValueAnimator.areAnimatorsEnabled()) spring(
            dampingRatio = if (miuix) 0.82f else 0.72f,
            stiffness = if (miuix) 480f else 430f)
            else tween(0), label = "sideCapsule")
    Column(Modifier.width(176.dp).padding(12.dp).glassSurface(radius = 26.dp, floating = true)
        .padding(8.dp).selectableGroup()) {
        Box {
            Box(Modifier.fillMaxWidth().offset(y = indicatorY).height(64.dp)
                .background(colors.primaryContainer.copy(alpha = 0.82f), RoundedCornerShape(20.dp)))
            Column {
                tabs.forEachIndexed { index, title ->
                    val selected = index == selectedIndex
                    Row(Modifier.fillMaxWidth().height(64.dp)
                        .selectable(selected = selected, role = Role.Tab) { onSelect(index) }
                        .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(if (selected) selectedIcons[index] else icons[index],
                            contentDescription = null, modifier = Modifier.size(24.dp),
                            tint = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
                        Text(title, color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomTabBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp),
        contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
            GlassTabs(tabs = listOf("首页", "知识库", "设置"), selectedTabIndex = selectedIndex,
                onTabSelected = onSelect,
                icons = listOf(AppIcons.Home, AppIcons.Knowledge, AppIcons.Settings),
                selectedIcons = listOf(AppIcons.HomeSelected, AppIcons.KnowledgeSelected,
                    AppIcons.SettingsSelected),
                navigationBar = true)
        }
    }
}

@Composable
private fun HomeScreen(active: Boolean, bottomPadding: androidx.compose.ui.unit.Dp, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember(context) { Prefs(context) }
    var enabled by remember { mutableStateOf(prefs.enabled) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(active) {
        if (active) {
            enabled = prefs.enabled
            refresh++
        }
    }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = prefs.enabled
                refresh++
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val a11y = remember(refresh) {
        Settings.Secure.getString(context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)?.contains(
                "io.github.liaong13.dialogueroute/com.google.android.accessibility.selecttospeak.SelectToSpeakService") == true
    }
    val overlay = remember(refresh) { Settings.canDrawOverlays(context) }
    val battery = remember(refresh) {
        (context.getSystemService(Context.POWER_SERVICE) as PowerManager)
            .isIgnoringBatteryOptimizations(context.packageName)
    }
    val xposed = remember(refresh) {
        prefs.xposedEnabled && (XposedProbeBridge.lastContentAgeMs(context) ?: Long.MAX_VALUE) < 600_000L
    }
    val verdict = PowerSetup.verdict(a11y, overlay, prefs.hasKey(), battery, xposed)
    val captureTint = MaterialTheme.colorScheme.primary
    val overlayTint = MaterialTheme.colorScheme.tertiary
    val modelTint = MaterialTheme.colorScheme.secondary
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = bottomPadding),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth().glassSurface(radius = 24.dp).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(52.dp).glassSurface(radius = 16.dp)
                    .background(Brush.linearGradient(listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)))),
                    contentAlignment = Alignment.Center) {
                    Icon(AppIcons.Brand, contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
                PageHeading("对话攻略", "读懂对话，多一种回应的可能。")
            }
        }
        item {
            SectionCard {
                Box(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(end = 84.dp)) {
                        StatusBadge(if (!enabled) "助手已关闭" else if (verdict.ready) "基础配置已就绪" else "待完成配置",
                            highlighted = enabled && verdict.ready)
                        Spacer(Modifier.height(16.dp))
                        Text(if (!enabled) "需要时，随时开启" else if (verdict.ready) "下一句，由你决定" else "开始前，再准备一下",
                            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                    HomeStatusArt(Modifier.align(Alignment.TopEnd))
                }
                Spacer(Modifier.height(10.dp))
                SupportingText(if (!enabled) "开启后，在聊天中查看判断与回复建议。"
                    else if (verdict.ready) "打开聊天窗口，在悬浮窗中查看分析和回复选项。"
                    else "还需配置：${verdict.missing.joinToString("、")}")
                if (!verdict.ready) {
                    Spacer(Modifier.height(16.dp))
                    PrimaryAction("去完成配置", icon = AppIcons.Setup, onClick = onOpenSettings)
                }
            }
        }
        item {
            GlassCard(padding = PaddingValues(6.dp)) {
                RoundedSwitchPreference(title = "启用助手", summary = "采集、分析与悬浮窗的总开关",
                    checked = enabled, icon = AppIcons.Assistant,
                    onCheckedChange = { enabled = it; prefs.enabled = it })
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { SectionHeading("准备情况") }
                Row(Modifier.clip(RoundedCornerShape(12.dp))
                    .clickable(role = Role.Button, onClick = onOpenSettings)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("查看详情", color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium)
                    Icon(AppIcons.ChevronForward, contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            SectionCard {
                SetupStatusRow("聊天采集", when {
                    xposed -> "最近 10 分钟读取到微信正文"
                    a11y -> "无障碍权限已开启，正文可读性以聊天页为准"
                    else -> "开启无障碍，或配置 Xposed 增强模式"
                }, if (a11y || xposed) "已准备" else "待配置", a11y || xposed,
                    icon = AppIcons.Messages, tint = captureTint, divider = true)
                SetupStatusRow("悬浮窗", "在聊天上方展示判断与回复选项",
                    if (overlay) "已授权" else "待授权", overlay,
                    icon = AppIcons.FloatingWindow, tint = overlayTint, divider = true)
                SetupStatusRow("判断模型", "密钥配置后，可在设置中测试连通性",
                    if (prefs.hasKey()) "已配置" else "待配置", prefs.hasKey(),
                    icon = AppIcons.Model, tint = modelTint)
                if (verdict.recommendations.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SupportingText("后台运行建议：${verdict.recommendations.joinToString("、")}")
                }
            }
        }
    }
}

@Composable
private fun HomeStatusArt(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(15.dp)
    Box(modifier.size(76.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 54.dp, height = 66.dp)
            .graphicsLayer { rotationZ = 12f }.clip(shape)
            .background(Brush.linearGradient(listOf(colors.primaryContainer,
                colors.secondaryContainer)))
            .border(0.75.dp, Color.White.copy(alpha = 0.55f), shape),
            contentAlignment = Alignment.Center) {
            Icon(AppIcons.Document, contentDescription = null,
                modifier = Modifier.size(35.dp),
                tint = colors.onPrimaryContainer)
        }
        Box(Modifier.align(Alignment.BottomEnd).size(27.dp).clip(CircleShape)
            .background(colors.secondaryContainer)
            .border(0.75.dp, Color.White.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center) {
            Icon(AppIcons.SettingsSelected, contentDescription = null,
                modifier = Modifier.size(18.dp), tint = colors.onSecondaryContainer)
        }
    }
}
