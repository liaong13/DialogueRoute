package io.github.liaong13.dialogueroute.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentCallbacks
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import io.github.liaong13.dialogueroute.MiuixActivity
import io.github.liaong13.dialogueroute.core.Analysis
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.Prefs
import io.github.liaong13.dialogueroute.core.RankedReply
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** 悬浮窗只展示分析、复制或填入草稿；发送始终由用户完成。 */
class OverlayController(private val ctx: Context) {
    private val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val prefs = Prefs(ctx)
    private val storage = ctx.getSharedPreferences(Prefs.PREFS_MAIN, Context.MODE_PRIVATE)
    private val main = Handler(Looper.getMainLooper())
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var lp: WindowManager.LayoutParams? = null
    private var listenersRegistered = false
    private var hiddenByUser = false
    private var analysisEpoch = 0L

    private var themeMode by mutableStateOf(prefs.themeMode)
    private var opacity by mutableStateOf(prefs.overlayOpacity / 100f)
    private var configuration by mutableStateOf(Configuration(ctx.resources.configuration))
    private val appearanceListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        main.post { if (composeView != null) refreshAppearance() }
    }
    private val configurationListener = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            main.post {
                if (composeView != null) {
                    configuration = Configuration(newConfig)
                    clampPosition()
                    updateWindow()
                }
            }
        }
        override fun onLowMemory() = Unit
    }

    private var expanded by mutableStateOf(false)
    private var menuVisible by mutableStateOf(false)
    private var judging by mutableStateOf(false)
    private var generatingReplies by mutableStateOf(false)
    private var lastJudgment by mutableStateOf<Analysis?>(null)
    private var pendingReplies by mutableStateOf<List<RankedReply>?>(null)
    private var judgmentError by mutableStateOf<String?>(null)
    private var replyError by mutableStateOf<String?>(null)
    private var preview by mutableStateOf<ChatSnapshot?>(null)
    private var ctxNotes by mutableStateOf(0)
    private var ctxHistory by mutableStateOf(0)
    private var noteText by mutableStateOf<String?>(null)
    private var lastFill by mutableStateOf<((String) -> Unit)?>(null)

    var onManualAnalyze: (() -> Unit)? = null
    var onSaveContact: (() -> Unit)? = null
    var onOcrCapture: (() -> Unit)? = null
    var onInspectCapture: (() -> Unit)? = null

    private var collapsedX = dp(8)
    private var collapsedY = dp(150)
    private var panelX = prefs.panelX
    private var panelY = prefs.panelY
    private var panelWidthPx = 0
    private var panelHeightPx = 0
    private var startX = 0
    private var startY = 0
    private var touchX = 0f
    private var touchY = 0f
    private var moved = false
    private var longFired = false
    private val longPress = Runnable {
        if (!moved && composeView != null && !expanded) {
            longFired = true
            showMenu()
        }
    }

    fun isShowing(): Boolean = composeView != null
    fun isCurrentAnalysis(token: Long): Boolean = token == analysisEpoch && !hiddenByUser && prefs.enabled

    private fun dp(value: Int) = (ctx.resources.displayMetrics.density * value).roundToInt()
    private val screenW get() = ctx.resources.displayMetrics.widthPixels
    private val screenH get() = ctx.resources.displayMetrics.heightPixels
    private val panelWidthDp: Int get() {
        val width = configuration.screenWidthDp
        val fraction = if (menuVisible) 0.74f else 0.84f
        return minOf(if (menuVisible) 220 else 252,
            (width * fraction).roundToInt().coerceAtLeast(1), (width - 16).coerceAtLeast(1))
    }

    private fun refreshAppearance() {
        themeMode = prefs.themeMode
        opacity = prefs.overlayOpacity / 100f
        configuration = Configuration(ctx.resources.configuration)
    }

    private fun ensureRoot() {
        if (composeView != null || hiddenByUser) return
        if (!Settings.canDrawOverlays(ctx)) {
            Log.w("DIALOGUEROUTE", "overlay: canDrawOverlays=false")
            return
        }
        refreshAppearance()
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.bubbleX.takeIf { it >= 0 } ?: dp(8)
            y = prefs.bubbleY.takeIf { it >= 0 } ?: dp(150)
        }
        lp = params
        clampPosition()
        val owner = OverlayLifecycleOwner()
        owner.performRestore(null)
        owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        val cv = ComposeView(ctx)
        cv.setViewTreeLifecycleOwner(owner)
        cv.setViewTreeSavedStateRegistryOwner(owner)
        cv.setViewTreeViewModelStoreOwner(owner)
        composeView = cv
        lifecycleOwner = owner
        cv.setContent {
            val systemDark = configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val darkTheme = when (themeMode) {
                Prefs.THEME_DARK -> true
                Prefs.THEME_SYSTEM -> systemDark
                else -> false
            }
            val colors = if (darkTheme) darkColorScheme(
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
            MiuixTheme(colors = colors) {
                if (expanded) {
                    OverlayPanel(
                        analysis = lastJudgment, replies = pendingReplies,
                        judging = judging, generatingReplies = generatingReplies,
                        error = judgmentError, replyError = replyError,
                        notes = ctxNotes, history = ctxHistory, note = noteText,
                        preview = preview, menuVisible = menuVisible,
                        panelWidth = panelWidthDp.dp,
                        contentHeight = minOf(236f, configuration.screenHeightDp * 0.30f).dp,
                        opacity = opacity,
                        onClose = { updateExpanded(false) },
                        onPanelTouch = ::onPanelTouch,
                        onSizeChanged = { width, height ->
                            panelWidthPx = width
                            panelHeightPx = height
                            if (expanded) {
                                clampPosition()
                                updateWindow()
                            }
                        },
                        onMenu = { menuVisible = !menuVisible },
                        onSettings = ::openSettings,
                        onManualAnalyze = { preview = null; onManualAnalyze?.invoke() },
                        onCopy = ::copy,
                        onFill = { text ->
                            lastFill?.let { fill ->
                                fill(text)
                                updateExpanded(false)
                            }
                        },
                        canFill = lastFill != null,
                        onSaveContact = onSaveContact?.let { action -> { menuVisible = false; action() } },
                        onOcrCapture = onOcrCapture?.let { action -> { menuVisible = false; action() } },
                        onInspectCapture = onInspectCapture?.let { action -> { menuVisible = false; action() } },
                        onHide = { hide(); hiddenByUser = true },
                        onDismissPreview = { preview = null }
                    )
                } else {
                    OverlayBubble(
                        dangerLevel = lastJudgment?.dangerLevel?.score,
                        onTouch = ::onBubbleTouch,
                        onOpen = { updateExpanded(true) },
                        onMenu = ::showMenu
                    )
                }
            }
        }
        try {
            wm.addView(cv, params)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            storage.registerOnSharedPreferenceChangeListener(appearanceListener)
            ctx.applicationContext.registerComponentCallbacks(configurationListener)
            listenersRegistered = true
        } catch (e: Exception) {
            Log.e("DIALOGUEROUTE", "overlay addView failed: ${e.javaClass.simpleName}")
            hide()
        }
    }

    // 只让悬浮球处理原始触摸，面板按钮和滚动由 Compose 自己分发。
    private fun onBubbleTouch(event: MotionEvent): Boolean {
        val params = lp ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = params.x; startY = params.y
                touchX = event.rawX; touchY = event.rawY
                moved = false; longFired = false
                main.removeCallbacks(longPress)
                main.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE -> {
                if (longFired) return true
                val dx = (event.rawX - touchX).roundToInt()
                val dy = (event.rawY - touchY).roundToInt()
                val slop = ViewConfiguration.get(ctx).scaledTouchSlop
                if (abs(dx) > slop || abs(dy) > slop) {
                    moved = true
                    main.removeCallbacks(longPress)
                }
                if (moved) {
                    params.x = startX + dx; params.y = startY + dy
                    clampPosition()
                    updateWindow()
                }
            }
            MotionEvent.ACTION_UP -> {
                main.removeCallbacks(longPress)
                if (!longFired) {
                    if (moved) {
                        prefs.bubbleX = params.x; prefs.bubbleY = params.y
                    } else updateExpanded(true)
                }
            }
            MotionEvent.ACTION_CANCEL -> main.removeCallbacks(longPress)
        }
        return true
    }

    private fun clampPosition() {
        val params = lp ?: return
        if (expanded) {
            val width = panelWidthPx.takeIf { it > 0 } ?: dp(panelWidthDp)
            val height = panelHeightPx.takeIf { it > 0 } ?: dp(340)
            params.x = params.x.coerceIn(dp(8), (screenW - width - dp(8)).coerceAtLeast(dp(8)))
            params.y = params.y.coerceIn(dp(24), (screenH - height - dp(24)).coerceAtLeast(dp(24)))
        } else {
            params.x = params.x.coerceIn(dp(8), (screenW - dp(56)).coerceAtLeast(dp(8)))
            params.y = params.y.coerceIn(dp(24), (screenH - dp(120)).coerceAtLeast(dp(24)))
        }
    }

    private fun onPanelTouch(event: MotionEvent): Boolean {
        if (!expanded) return false
        val params = lp ?: return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = params.x; startY = params.y
                touchX = event.rawX; touchY = event.rawY
                moved = false
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = (event.rawX - touchX).roundToInt()
                val dy = (event.rawY - touchY).roundToInt()
                val slop = ViewConfiguration.get(ctx).scaledTouchSlop
                if (abs(dx) > slop || abs(dy) > slop) moved = true
                if (moved) {
                    params.x = startX + dx
                    params.y = startY + dy
                    clampPosition()
                    updateWindow()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (moved) savePanelPosition()
            }
        }
        return true
    }

    private fun savePanelPosition() {
        val params = lp ?: return
        if (!expanded) return
        panelX = params.x
        panelY = params.y
        prefs.panelX = panelX
        prefs.panelY = panelY
    }

    private fun updateWindow() {
        val params = lp ?: return
        composeView?.let { runCatching { wm.updateViewLayout(it, params) } }
    }

    private fun updateExpanded(value: Boolean) {
        if (composeView == null || value == expanded) return
        main.removeCallbacks(longPress)
        val params = lp ?: return
        if (value) {
            collapsedX = params.x; collapsedY = params.y
            params.x = if (panelX >= 0) panelX else collapsedX
            params.y = if (panelY >= 0) panelY else minOf(collapsedY, (screenH * 0.14f).roundToInt())
        } else {
            panelX = params.x; panelY = params.y
            params.x = collapsedX; params.y = collapsedY
            menuVisible = false
        }
        expanded = value
        clampPosition()
        updateWindow()
    }

    private fun showMenu() {
        updateExpanded(true)
        menuVisible = true
    }

    private fun openSettings() {
        menuVisible = false
        updateExpanded(false)
        runCatching {
            ctx.startActivity(Intent(ctx, MiuixActivity::class.java)
                .putExtra(MiuixActivity.EXTRA_TAB, 2)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }.onFailure { toast("无法打开设置") }
    }

    fun showIdle(title: String?) {
        ensureRoot()
    }

    fun showCapturePreview(snapshot: ChatSnapshot) {
        ensureRoot()
        preview = snapshot
        menuVisible = false
        updateExpanded(true)
    }

    fun resetForNewConversation() {
        analysisEpoch++
        judging = false
        generatingReplies = false
        lastJudgment = null
        lastFill = null
        pendingReplies = null
        noteText = null
        judgmentError = null
        replyError = null
        preview = null
        ctxNotes = 0
        ctxHistory = 0
    }

    /** 返回本轮标识，调用方在投递异步结果和填入前核对。 */
    fun showLoading(): Long {
        resetForNewConversation()
        if (!hiddenByUser) {
            judging = true
            generatingReplies = true
            menuVisible = false
            ensureRoot()
            updateExpanded(true)
        }
        return analysisEpoch
    }

    fun setContextInfo(notes: Int, history: Int) {
        ctxNotes = notes; ctxHistory = history
    }

    fun setNote(note: String?) {
        noteText = note
    }

    fun setHiddenForShot(hidden: Boolean) {
        composeView?.visibility = if (hidden) View.INVISIBLE else View.VISIBLE
    }

    fun showError(msg: String) {
        judging = false
        lastJudgment = null
        judgmentError = msg
        ensureRoot()
        updateExpanded(true)
    }

    fun showJudgment(analysis: Analysis) {
        judging = false
        judgmentError = null
        lastJudgment = analysis
    }

    fun showReplies(ranked: List<RankedReply>, error: String? = null, onFill: (String) -> Unit) {
        generatingReplies = false
        lastFill = onFill
        replyError = error
        pendingReplies = ranked
    }

    fun toast(msg: String) = Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()

    private fun copy(text: String) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("dialogue_route_reply", text))
        toast("已复制")
    }

    fun hide() {
        main.removeCallbacks(longPress)
        if (listenersRegistered) {
            storage.unregisterOnSharedPreferenceChangeListener(appearanceListener)
            ctx.applicationContext.unregisterComponentCallbacks(configurationListener)
            listenersRegistered = false
        }
        val cv = composeView
        val owner = lifecycleOwner
        composeView = null
        lifecycleOwner = null
        lp = null
        panelWidthPx = 0
        panelHeightPx = 0
        expanded = false
        menuVisible = false
        hiddenByUser = false
        resetForNewConversation()
        cv?.disposeComposition()
        if (cv != null) runCatching { wm.removeView(cv) }
        owner?.destroy()
    }
}

private class OverlayLifecycleOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun handleLifecycleEvent(event: Lifecycle.Event) = registry.handleLifecycleEvent(event)

    fun performRestore(savedStateBundle: Bundle?) {
        savedState.performAttach()
        savedState.performRestore(savedStateBundle)
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
        store.clear()
    }
}
