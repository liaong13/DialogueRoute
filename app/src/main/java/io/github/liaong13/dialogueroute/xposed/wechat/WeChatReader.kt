package io.github.liaong13.dialogueroute.xposed.wechat

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import io.github.liaong13.dialogueroute.xposed.XposedProbeBridge
import java.lang.reflect.Field
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Member names are taken from the installed WeChat 8.0.78 / 3180 APK. */
class WeChatReader {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val busy = AtomicBoolean(false)
    private var fragment: Any? = null
    private var context: Context? = null
    @Volatile private var epoch = 0
    private var lastSignature = ""
    private var lastSentAt = 0L
    private val tick = object : Runnable {
        override fun run() {
            val target = fragment ?: return
            val appContext = context ?: return
            val version = epoch
            val snapshot = readSnapshot(target)
            if (snapshot != null && busy.compareAndSet(false, true)) {
                io.execute {
                    try {
                        if (version != epoch || !XposedProbeBridge.isEnabledInHost(appContext))
                            return@execute
                        val now = SystemClock.elapsedRealtime()
                        if (snapshot.signature != lastSignature || now - lastSentAt >= 20_000L) {
                            if (XposedProbeBridge.sendSnapshot(appContext, snapshot.session,
                                    snapshot.title, snapshot.sides, snapshot.texts,
                                    snapshot.capturedAt)) {
                                lastSignature = snapshot.signature
                                lastSentAt = now
                            }
                        }
                        val command = XposedProbeBridge.pollFill(appContext, snapshot.session)
                        if (command != null) main.post {
                            val id = command.getLong("id")
                            val text = command.getString("text")
                            val ok = version == epoch && fragment === target &&
                                text != null && text.length <= 4000 &&
                                readSession(target) == snapshot.session &&
                                runCatching { fillDraft(target, text) }.getOrDefault(false)
                            io.execute { XposedProbeBridge.reportFill(appContext, id, ok) }
                        }
                    } finally {
                        busy.set(false)
                    }
                }
            }
            main.postDelayed(this, 1_500L)
        }
    }

    fun start(chatFragment: Any, appContext: Context) {
        stop()
        fragment = chatFragment
        context = appContext.applicationContext
        main.post(tick)
    }

    fun stop() {
        epoch++
        main.removeCallbacks(tick)
        fragment = null
        context = null
        lastSignature = ""
        lastSentAt = 0L
    }

    private data class TextSnapshot(val session: String, val title: String?,
        val sides: ArrayList<String>, val texts: ArrayList<String>, val signature: String,
        val capturedAt: Long)

    private fun readSnapshot(target: Any): TextSnapshot? {
        return try {
        val view = target.javaClass.getMethod("getView").invoke(target) as? View
        if (view?.isShown != true) return null
        val chatContext = field(target, "f") ?: return null
        val talker = chatContext.javaClass.getMethod("x").invoke(chatContext) as? String
        val account = chatContext.javaClass.getMethod("t").invoke(chatContext) as? String
        if (talker.isNullOrBlank() || account.isNullOrBlank()) return null
        val session = digest("$account\u0000$talker")
        val title = (chatContext.javaClass.getMethod("w").invoke(chatContext) as? String)
            ?.trim()?.takeIf { it.isNotEmpty() }?.take(100)
        val adapter = field(target, "s") ?: return null
        val count = adapter.javaClass.getMethod("getCount").invoke(adapter) as Int
        if (count !in 1..100_000) return null
        val itemAt = adapter.javaClass.getMethod("X0", Int::class.javaPrimitiveType)
        val sides = ArrayList<String>()
        val texts = ArrayList<String>()
        val identity = StringBuilder(session)
        for (index in maxOf(0, count - 30) until count) {
            val item = itemAt.invoke(adapter, index) ?: continue
            val cls = item.javaClass
            if ((cls.getMethod("getType").invoke(item) as Int) != 1) continue
            val side = when (cls.getMethod("z0").invoke(item) as Int) {
                1 -> "me"
                0 -> "other"
                else -> continue
            }
            var body = cls.getMethod("j").invoke(item) as? String ?: continue
            if (talker.endsWith("@chatroom") && side == "other") {
                val sender = cls.getMethod("Q1").invoke(item) as? String
                val separator = body.indexOf(":\n")
                if (sender.isNullOrBlank() || separator !in 1..128 ||
                    body.substring(0, separator).trim() != sender) continue
                body = body.substring(separator + 2)
            }
            body = body.trim()
            if (body.isEmpty()) continue
            body = body.take(2000)
            sides.add(side)
            texts.add(body)
            identity.append('|').append(cls.getMethod("getMsgId").invoke(item))
                .append(':').append(side).append(':').append(body)
        }
        if (texts.isEmpty()) return null
        TextSnapshot(session, title, sides, texts, digest(identity.toString()),
            SystemClock.elapsedRealtime())
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun readSession(target: Any): String? {
        return try {
            val chatContext = field(target, "f") ?: return null
            val talker = chatContext.javaClass.getMethod("x").invoke(chatContext) as? String
            val account = chatContext.javaClass.getMethod("t").invoke(chatContext) as? String
            if (talker.isNullOrBlank() || account.isNullOrBlank()) null
            else digest("$account\u0000$talker")
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun fillDraft(target: Any, text: String): Boolean {
        val root = target.javaClass.getMethod("getView").invoke(target) as? View ?: return false
        val footer = findFooter(root) ?: return false
        val input = footer.javaClass.getMethod("getToSendEt").invoke(footer) ?: return false
        val inputApi = Class.forName("st5.i", false, input.javaClass.classLoader)
        val inputView = inputApi.getMethod("g").invoke(input) as? View ?: return false
        if (!inputView.isShown || !inputView.isEnabled) return false
        val existing = inputApi.getMethod("getText").invoke(input)?.toString().orEmpty()
        if (existing.isNotBlank() && existing != text) return false
        inputApi.getMethod("setText", CharSequence::class.java).invoke(input, text)
        val actual = inputApi.getMethod("getText").invoke(input)?.toString()
        if (actual != text) return false
        inputApi.getMethod("setSelection", Int::class.javaPrimitiveType).invoke(input,
            text.length)
        return true
    }

    private fun findFooter(root: View): View? {
        val queue = ArrayDeque<View>()
        queue.add(root)
        var visited = 0
        while (queue.isNotEmpty() && visited++ < 2000) {
            val view = queue.removeFirst()
            if (view.javaClass.name == "com.tencent.mm.pluginsdk.ui.chat.ChatFooter") return view
            if (view is ViewGroup) {
                for (index in 0 until view.childCount) queue.add(view.getChildAt(index))
            }
        }
        return null
    }

    private fun field(target: Any, name: String): Any? {
        var cls: Class<*>? = target.javaClass
        while (cls != null) {
            val member: Field = try { cls.getDeclaredField(name) }
            catch (_: NoSuchFieldException) { cls = cls.superclass; continue }
            member.isAccessible = true
            return member.get(target)
        }
        return null
    }

    private fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).take(16)
        .joinToString("") { "%02x".format(it) }
}
