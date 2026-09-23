package io.github.liaong13.dialogueroute.xposed

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import io.github.liaong13.dialogueroute.core.Prefs

/** Only a boolean setting and a short-lived probe heartbeat cross the process boundary. */
object XposedProbeBridge {
    const val AUTHORITY = "io.github.liaong13.dialogueroute.xposed.probe"
    const val ENABLED_KEY = "xposed_enabled"
    const val HEARTBEAT_KEY = "xposed_wechat_heartbeat"
    const val ACTIVE_KEY = "xposed_wechat_active"
    const val CONTENT_HEARTBEAT_KEY = "xposed_wechat_content_heartbeat"
    const val CONTENT_ACTIVE_KEY = "xposed_wechat_content_active"
    const val RESUME_KEY = "xposed_wechat_resume_elapsed"
    const val HEARTBEAT_INTERVAL_MS = 30_000L
    private const val HEARTBEAT_VALID_MS = 75_000L
    private val uri = Uri.parse("content://$AUTHORITY")

    fun isEnabledInHost(context: Context): Boolean = try {
        context.contentResolver.call(uri, "enabled", null, null)?.getBoolean("enabled") == true
    } catch (_: RuntimeException) {
        false
    }

    fun reportChatProbe(context: Context, active: Boolean) {
        try {
            context.contentResolver.call(uri, if (active) "active" else "inactive", null, null)
        } catch (_: RuntimeException) {
            // A missing or unavailable app provider must not break the host app.
        }
    }

    /** Sends only the active conversation's bounded text window to our app process. */
    fun sendSnapshot(context: Context, session: String, title: String?, sides: ArrayList<String>,
        texts: ArrayList<String>, capturedAt: Long): Boolean = try {
        val data = android.os.Bundle().apply {
            putString("session", session)
            putString("title", title)
            putStringArrayList("sides", sides)
            putStringArrayList("texts", texts)
            putLong("capturedAt", capturedAt)
        }
        context.contentResolver.call(uri, "snapshot", null, data)?.getBoolean("accepted") == true
    } catch (_: RuntimeException) {
        false
    }

    fun pollFill(context: Context, session: String): android.os.Bundle? = try {
        context.contentResolver.call(uri, "pollFill", null,
            android.os.Bundle().apply { putString("session", session) })
    } catch (_: RuntimeException) {
        null
    }

    fun reportFill(context: Context, id: Long, success: Boolean) {
        try {
            context.contentResolver.call(uri, "fillResult", null, android.os.Bundle().apply {
                putLong("id", id)
                putBoolean("success", success)
            })
        } catch (_: RuntimeException) {
            // The app may have been stopped while the draft was being filled.
        }
    }

    fun lastProbeAgeMs(context: Context): Long? {
        val sp = context.getSharedPreferences(Prefs.PREFS_MAIN, Context.MODE_PRIVATE)
        val last = sp.getLong(HEARTBEAT_KEY, -1L)
        val now = SystemClock.elapsedRealtime()
        return if (last >= 0 && now >= last) now - last else null
    }

    fun isChatProbeActive(context: Context): Boolean {
        val sp = context.getSharedPreferences(Prefs.PREFS_MAIN, Context.MODE_PRIVATE)
        return sp.getBoolean(ACTIVE_KEY, false) &&
            (lastProbeAgeMs(context) ?: Long.MAX_VALUE) < HEARTBEAT_VALID_MS
    }

    fun isContentActive(context: Context): Boolean {
        val sp = context.getSharedPreferences(Prefs.PREFS_MAIN, Context.MODE_PRIVATE)
        val last = sp.getLong(CONTENT_HEARTBEAT_KEY, -1L)
        val now = SystemClock.elapsedRealtime()
        return sp.getBoolean(ENABLED_KEY, false) && sp.getBoolean(CONTENT_ACTIVE_KEY, false) &&
            last >= 0 && now >= last &&
            now - last < HEARTBEAT_VALID_MS
    }

    fun lastContentAgeMs(context: Context): Long? {
        val sp = context.getSharedPreferences(Prefs.PREFS_MAIN, Context.MODE_PRIVATE)
        val last = sp.getLong(CONTENT_HEARTBEAT_KEY, -1L)
        val now = SystemClock.elapsedRealtime()
        return if (last >= 0 && now >= last) now - last else null
    }
}
