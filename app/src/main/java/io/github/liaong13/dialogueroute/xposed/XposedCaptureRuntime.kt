package io.github.liaong13.dialogueroute.xposed

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.Prefs
import io.github.liaong13.dialogueroute.core.kb.ContextBuilder
import io.github.liaong13.dialogueroute.core.kb.KbStore
import io.github.liaong13.dialogueroute.jev.JevClient
import io.github.liaong13.dialogueroute.overlay.OverlayController
import java.util.concurrent.Executors

/** App-process owner for the WeChat Xposed source. No AccessibilityService is required. */
class XposedCaptureRuntime private constructor(private val context: Context) {
    private val prefs = Prefs(context)
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newFixedThreadPool(3)
    private val fillLock = Any()
    private var overlay: OverlayController? = null
    private var session: String? = null
    private var snapshot: ChatSnapshot? = null
    private var signature = ""
    private var generation = 0
    private var analyzingGeneration = -1
    private var pendingFill: FillCommand? = null
    private var lastFillId = 0L
    private var lastFillText: String? = null
    private var lastFillSession: String? = null
    private var lastFillGeneration = -1
    private var debounce: Runnable? = null

    private data class FillCommand(val id: Long, val session: String, val text: String,
        val createdAt: Long)

    fun onSnapshot(sessionId: String, incoming: ChatSnapshot) {
        main.post {
            if (!prefs.enabled || !prefs.xposedEnabled || !prefs.isAllowed(incoming.title)) {
                closeOnMain()
                return@post
            }
            val ui = ensureOverlay()
            val nextSignature = incoming.signature()
            if (session == sessionId && signature == nextSignature && ui.isShowing()) return@post
            val changed = session != sessionId || signature != nextSignature
            session = sessionId
            snapshot = incoming
            signature = nextSignature
            if (!changed) {
                ui.showIdle(incoming.title)
                return@post
            }
            generation++
            debounce?.let(main::removeCallbacks)
            synchronized(fillLock) { pendingFill = null }
            lastFillText = null
            lastFillSession = null
            ui.resetForNewConversation()
            if (incoming.latestFrom == "other" && prefs.autoAnalyze) {
                val token = generation
                debounce = Runnable { analyze(token, incoming) }.also {
                    main.postDelayed(it, 800)
                }
            } else {
                ui.showIdle(incoming.title)
            }
        }
    }

    fun onChatClosed() { main.post { closeOnMain() } }

    private fun closeOnMain() {
        generation++
        debounce?.let(main::removeCallbacks)
        debounce = null
        session = null
        snapshot = null
        signature = ""
        synchronized(fillLock) { pendingFill = null }
        lastFillText = null
        lastFillSession = null
        overlay?.hide()
    }

    private fun ensureOverlay(): OverlayController {
        overlay?.let { return it }
        return OverlayController(context).also { ui ->
            ui.onManualAnalyze = { snapshot?.let { analyze(generation, it) } }
            ui.onInspectCapture = { snapshot?.let(ui::showCapturePreview) }
            ui.onSaveContact = {
                val title = snapshot?.title
                val currentSession = session
                if (title.isNullOrBlank() || currentSession == null) {
                    ui.toast("当前会话没有可用标题")
                } else {
                    worker.execute {
                        val result = try {
                            KbStore.get(context).saveOrMergeContact(title, WECHAT_PACKAGE)
                        } catch (e: Exception) { "保存失败：${e.javaClass.simpleName}" }
                        main.post { if (session == currentSession) ui.toast(result) }
                    }
                }
            }
            overlay = ui
        }
    }

    private fun analyze(token: Int, current: ChatSnapshot) {
        if (token != generation || session == null || analyzingGeneration == token) return
        val ui = ensureOverlay()
        if (!prefs.hasKey()) {
            ui.showError("未设置判断接口密钥，去设置里填")
            return
        }
        analyzingGeneration = token
        ui.showLoading()
        ui.setNote("来源：微信 Xposed 当前会话文本")
        val client = JevClient(prefs)
        val relationship = prefs.relationship
        worker.execute {
            val extra = try {
                ContextBuilder.build(context, current, WECHAT_PACKAGE, prefs)
            } catch (_: Exception) { null }
            main.post {
                if (token == generation) ui.setContextInfo(extra?.notes?.size ?: 0,
                    extra?.history?.size ?: 0)
            }
            worker.execute {
                val judgment = try { client.judge(current, relationship, extra) }
                catch (e: Exception) {
                    main.post {
                        if (token == generation) {
                            analyzingGeneration = -1
                            ui.showError("判断失败：${e.javaClass.simpleName}")
                        }
                    }
                    return@execute
                }
                main.post {
                    if (token != generation) return@post
                    if (judgment.error != null) {
                        analyzingGeneration = -1
                        ui.showError(judgment.error)
                    }
                    else ui.showJudgment(judgment)
                }
            }
            worker.execute {
                var error: String? = null
                val replies = try { client.draftAndRank(current, relationship, extra) }
                catch (e: Exception) {
                    error = e.javaClass.simpleName
                    emptyList()
                }
                main.post {
                    if (token != generation) return@post
                    analyzingGeneration = -1
                    ui.showReplies(replies, error) { text -> requestFill(token, text) }
                }
            }
        }
    }

    private fun requestFill(token: Int, text: String) {
        val currentSession = session
        if (token != generation || currentSession == null || text.isBlank()) return
        synchronized(fillLock) {
            lastFillId++
            lastFillText = text
            lastFillSession = currentSession
            lastFillGeneration = token
            pendingFill = FillCommand(lastFillId, currentSession, text,
                SystemClock.elapsedRealtime())
        }
        overlay?.toast("正在填入微信草稿")
    }

    fun pollFill(sessionId: String): Bundle? = synchronized(fillLock) {
        val command = pendingFill ?: return@synchronized null
        if (command.session != sessionId ||
            SystemClock.elapsedRealtime() - command.createdAt > 15_000L
        ) return@synchronized null
        pendingFill = null
        Bundle().apply {
            putLong("id", command.id)
            putString("text", command.text)
        }
    }

    fun onFillResult(id: Long, success: Boolean) {
        main.post {
            if (id != lastFillId || session != lastFillSession ||
                generation != lastFillGeneration) return@post
            if (success) overlay?.toast("已填入，确认后自己发送")
            else {
                lastFillText?.let { text ->
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("dialogue_route_reply", text))
                }
                overlay?.toast("填入失败，已复制回复")
            }
            lastFillText = null
            lastFillSession = null
        }
    }

    companion object {
        private const val WECHAT_PACKAGE = "com.tencent.mm"
        @Volatile private var instance: XposedCaptureRuntime? = null

        fun get(context: Context): XposedCaptureRuntime = instance ?:
            synchronized(this) {
                instance ?: XposedCaptureRuntime(context.applicationContext).also { instance = it }
            }
    }
}
