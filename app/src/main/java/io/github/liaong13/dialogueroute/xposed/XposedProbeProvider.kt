package io.github.liaong13.dialogueroute.xposed

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Binder
import android.os.Bundle
import android.os.SystemClock
import io.github.liaong13.dialogueroute.core.Prefs
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.Msg
import io.github.liaong13.dialogueroute.capture.ChatCaptureService

/** Narrow bridge for the injected WeChat process; never exposes app secrets. */
class XposedProbeProvider : ContentProvider() {
    override fun onCreate() = true

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val ctx = context ?: return null
        val callerPackages = ctx.packageManager.getPackagesForUid(Binder.getCallingUid()) ?: return null
        if (WECHAT_PACKAGE !in callerPackages) return null
        val sp = ctx.getSharedPreferences(Prefs.PREFS_MAIN, Context.MODE_PRIVATE)
        val enabled = sp.getBoolean(XposedProbeBridge.ENABLED_KEY, false)
        return when (method) {
            "enabled" -> Bundle().apply { putBoolean("enabled", enabled) }
            "active" -> {
                if (enabled) {
                    val edit = sp.edit()
                        .putLong(XposedProbeBridge.HEARTBEAT_KEY, SystemClock.elapsedRealtime())
                        .putBoolean(XposedProbeBridge.ACTIVE_KEY, true)
                    if (!sp.getBoolean(XposedProbeBridge.ACTIVE_KEY, false)) {
                        edit.putLong(XposedProbeBridge.RESUME_KEY,
                            SystemClock.elapsedRealtime())
                    }
                    edit.apply()
                }
                Bundle.EMPTY
            }
            "inactive" -> {
                sp.edit().putBoolean(XposedProbeBridge.ACTIVE_KEY, false)
                    .putBoolean(XposedProbeBridge.CONTENT_ACTIVE_KEY, false).apply()
                XposedCaptureRuntime.get(ctx).onChatClosed()
                Bundle.EMPTY
            }
            "snapshot" -> {
                if (!enabled) return null
                val data = extras ?: return null
                val session = data.getString("session")?.takeIf { it.length in 1..128 }
                    ?: return null
                val capturedAt = data.getLong("capturedAt", -1L)
                val now = SystemClock.elapsedRealtime()
                if (!sp.getBoolean(XposedProbeBridge.ACTIVE_KEY, false) ||
                    capturedAt < sp.getLong(XposedProbeBridge.RESUME_KEY, now) ||
                    capturedAt > now || now - capturedAt > 10_000L
                ) return null
                val title = data.getString("title")?.take(100)
                val sides = data.getStringArrayList("sides") ?: return null
                val texts = data.getStringArrayList("texts") ?: return null
                if (sides.size != texts.size || texts.isEmpty() || texts.size > 30 ||
                    texts.any { it.isBlank() || it.length > 2000 } ||
                    sides.any { it != "me" && it != "other" }
                ) return null
                val snapshot = ChatSnapshot(title, sides.indices.map { Msg(sides[it], texts[it]) })
                sp.edit().putLong(XposedProbeBridge.CONTENT_HEARTBEAT_KEY,
                    SystemClock.elapsedRealtime())
                    .putBoolean(XposedProbeBridge.CONTENT_ACTIVE_KEY, true).apply()
                ChatCaptureService.hideOverlayForXposed()
                XposedCaptureRuntime.get(ctx).onSnapshot(session, snapshot)
                Bundle().apply { putBoolean("accepted", true) }
            }
            "pollFill" -> {
                if (!enabled) return null
                val session = extras?.getString("session") ?: return null
                XposedCaptureRuntime.get(ctx).pollFill(session)
            }
            "fillResult" -> {
                val data = extras ?: return null
                val id = data.getLong("id")
                XposedCaptureRuntime.get(ctx).onFillResult(id, data.getBoolean("success"))
                Bundle.EMPTY
            }
            else -> null
        }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?,
        selectionArgs: Array<out String>?): Int = 0

    private companion object {
        const val WECHAT_PACKAGE = "com.tencent.mm"
    }
}
