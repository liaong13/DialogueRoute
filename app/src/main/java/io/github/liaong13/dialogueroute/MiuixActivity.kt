package io.github.liaong13.dialogueroute

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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.core.view.WindowCompat
import io.github.liaong13.dialogueroute.core.PowerSetup
import io.github.liaong13.dialogueroute.core.Prefs
import io.github.liaong13.dialogueroute.xposed.XposedProbeBridge
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.icon.extended.VerticalSplit
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

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
    DisposableEffect(context) {
        val storage = context.getSharedPreferences(Prefs.PREFS_MAIN, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == Prefs.KEY_THEME_MODE || key == null) themeMode = prefs.themeMode
        }
        storage.registerOnSharedPreferenceChangeListener(listener)
        themeMode = prefs.themeMode
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
    val colors = if (dark) darkColorScheme(
        primary = Color(0xFFA8C9FA), primaryVariant = Color(0xFFA8C9FA),
        onPrimary = Color(0xFF102D50), primaryContainer = Color(0xFF233B57),
        background = Color(0xFF11151C), surface = Color(0xFF11151C),
        surfaceVariant = Color(0xFF1D242E), surfaceContainer = Color(0xFF1D242E),
        secondary = Color(0xFF233B57)
    ) else lightColorScheme(
        primary = Color(0xFF1769C2), primaryVariant = Color(0xFF1769C2),
        onPrimary = Color.White,
        primaryContainer = Color(0xFFE8F0FF), background = Color(0xFFF4F6FA),
        onBackground = Color(0xFF202B3D), onSurface = Color(0xFF202B3D),
        onSurfaceVariantSummary = Color(0xFF58667C),
        surface = Color(0xFFEDF1F7), surfaceVariant = Color.White,
        surfaceContainer = Color.White, secondary = Color(0xFFE8F0FF)
    )
    CompositionLocalProvider(LocalAppDarkTheme provides dark) {
        MiuixTheme(colors = colors) {
            val pager = rememberPagerState(initialPage = requestedTab, pageCount = { 3 })
            val scope = rememberCoroutineScope()
            var settingsDirty by remember { mutableStateOf(false) }
            var settingsRevision by remember { mutableIntStateOf(0) }
            var pendingDestination by remember { mutableStateOf<Int?>(null) }
            fun requestPage(index: Int) {
                if (index == pager.currentPage) return
                if (pager.currentPage == 2 && settingsDirty) pendingDestination = index
                else scope.launch { pager.animateScrollToPage(index) }
            }
            androidx.compose.runtime.LaunchedEffect(requestedTab) {
                requestPage(requestedTab)
            }
            BackHandler(enabled = settingsDirty && pager.currentPage == 2) { pendingDestination = -1 }
            GlassBackdrop {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        BottomTabBar(pager.currentPage, ::requestPage)
                    }
                ) { padding ->
                    HorizontalPager(state = pager, beyondViewportPageCount = 2, userScrollEnabled = !settingsDirty,
                        modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { index ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                            Box(modifier = Modifier.widthIn(max = 680.dp).fillMaxSize()) {
                                when (index) {
                                    0 -> HomeScreen(active = pager.currentPage == 0,
                                        onOpenSettings = { requestPage(2) })
                                    1 -> KnowledgeScreen()
                                    else -> key(settingsRevision) { SettingsScreen(onDirtyChange = { settingsDirty = it }) }
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
                        else if (destination != null) scope.launch { pager.animateScrollToPage(destination) }
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomTabBar(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = bottomInset + 8.dp),
        contentAlignment = Alignment.Center) {
        Box(Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = 20.dp)) {
            GlassTabs(tabs = listOf("主页", "知识库", "设置"), selectedTabIndex = selectedIndex,
                onTabSelected = onSelect,
                icons = listOf(MiuixIcons.VerticalSplit, MiuixIcons.Contacts, MiuixIcons.Settings))
        }
    }
}

@Composable
private fun HomeScreen(active: Boolean, onOpenSettings: () -> Unit) {
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            PageHeading("对话攻略", "读懂对话，多一种回应的可能。")
        }
        item {
            SectionCard {
                StatusBadge(if (!enabled) "助手已关闭" else if (verdict.ready) "基础配置已就绪" else "待完成配置",
                    highlighted = enabled && verdict.ready)
                Spacer(Modifier.height(16.dp))
                Text(if (!enabled) "需要时，随时开启" else if (verdict.ready) "下一句，由你决定" else "开始前，再准备一下",
                    style = MiuixTheme.textStyles.title2, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                SupportingText(if (!enabled) "开启后，在聊天中查看判断与回复建议。"
                    else if (verdict.ready) "打开聊天窗口，在悬浮窗中查看分析和回复选项。"
                    else "还需配置：${verdict.missing.joinToString("、")}")
                Spacer(Modifier.height(12.dp))
                RoundedSwitchPreference(title = "启用助手", summary = "采集、分析与悬浮窗的总开关",
                    checked = enabled, onCheckedChange = { enabled = it; prefs.enabled = it })
                if (!verdict.ready) {
                    Spacer(Modifier.height(8.dp))
                    PrimaryAction("去完成配置", onOpenSettings)
                }
            }
        }
        item { SectionHeading("准备情况") }
        item {
            SectionCard {
                SetupStatusRow("聊天采集", when {
                    xposed -> "最近 10 分钟读取到微信正文"
                    a11y -> "无障碍权限已开启，正文可读性以聊天页为准"
                    else -> "开启无障碍，或配置 Xposed 增强模式"
                }, if (a11y || xposed) "已准备" else "待配置", a11y || xposed)
                SetupStatusRow("悬浮窗", "在聊天上方展示判断与回复选项",
                    if (overlay) "已授权" else "待授权", overlay)
                SetupStatusRow("判断模型", "密钥配置后，可在设置中测试连通性",
                    if (prefs.hasKey()) "已配置" else "待配置", prefs.hasKey())
                if (verdict.recommendations.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    SupportingText("后台运行建议：${verdict.recommendations.joinToString("、")}")
                }
            }
        }
    }
}
