package io.github.liaong13.dialogueroute.xposed.wechat

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.github.libxposed.api.XposedInterface
import io.github.liaong13.dialogueroute.xposed.XposedProbeBridge

/** 微信 3180 的聊天生命周期 hook；正文成员来自目标 APK 的反编译取证。 */
class WeChatPlugin(private val xposed: XposedInterface) {
    private var unsupportedLogged = false
    private var supportedVersion: Boolean? = null
    private val hookHandles = ArrayList<XposedInterface.HookHandle>()
    private var hostContext: Context? = null
    private val main = Handler(Looper.getMainLooper())
    private var chatProbeActive = false
    private val reader = WeChatReader()
    private val heartbeat = object : Runnable {
        override fun run() {
            val context = hostContext ?: return
            if (!chatProbeActive || !XposedProbeBridge.isEnabledInHost(context)) {
                chatProbeActive = false
                reader.stop()
                XposedProbeBridge.reportChatProbe(context, false)
                return
            }
            XposedProbeBridge.reportChatProbe(context, true)
            main.postDelayed(this, XposedProbeBridge.HEARTBEAT_INTERVAL_MS)
        }
    }

    fun install(loader: ClassLoader): Boolean = installChatHooks(loader)

    private fun installChatHooks(loader: ClassLoader): Boolean {
        val fragment = try {
            Class.forName(CHATTING_FRAGMENT, false, loader)
        } catch (_: ClassNotFoundException) {
            xposed.log(Log.WARN, TAG, "ChattingUIFragment absent; probe disabled")
            return false
        }
        val base = fragment.superclass ?: return false
        val resume = try {
            base.getDeclaredMethod("onResume")
        } catch (_: NoSuchMethodException) {
            xposed.log(Log.WARN, TAG, "chat resume member absent; probe disabled")
            return false
        }
        val pause = try {
            base.getDeclaredMethod("onPause")
        } catch (_: NoSuchMethodException) {
            xposed.log(Log.WARN, TAG, "chat pause member absent; probe disabled")
            return false
        }

        // 当前 APK 中父类 onResume/onPause 是稳定的聊天页生命周期入口。
        var resumeHook: XposedInterface.HookHandle? = null
        try {
            resumeHook = xposed.hook(resume).intercept { chain ->
                val result = chain.proceed()
                val activity = try {
                    chain.thisObject.javaClass.getMethod("thisActivity")
                        .invoke(chain.thisObject) as? Activity
                } catch (_: ReflectiveOperationException) { null }
                val context = activity?.applicationContext
                if (activity != null && context != null && isSupportedVersion(activity) &&
                    XposedProbeBridge.isEnabledInHost(context)) {
                    hostContext = context
                    chatProbeActive = true
                    main.removeCallbacks(heartbeat)
                    heartbeat.run()
                    reader.start(chain.thisObject, context)
                    xposed.log(Log.INFO, TAG, "chat fragment resumed; probe active")
                } else if (activity == null) {
                    xposed.log(Log.WARN, TAG, "chat fragment activity unavailable")
                } else if (supportedVersion == true) {
                    xposed.log(Log.INFO, TAG, "chat fragment resumed; app switch unavailable")
                }
                result
            }
            val pauseHook = xposed.hook(pause).intercept { chain ->
                val result = chain.proceed()
                chatProbeActive = false
                main.removeCallbacks(heartbeat)
                reader.stop()
                hostContext?.let { XposedProbeBridge.reportChatProbe(it, false) }
                result
            }
            hookHandles.add(resumeHook)
            hookHandles.add(pauseHook)
        } catch (error: RuntimeException) {
            resumeHook?.unhook()
            xposed.log(Log.WARN, TAG, "chat lifecycle probe install failed", error)
            return false
        }
        xposed.log(Log.INFO, TAG, "WeChat version 3180 chat probe installed")
        return true
    }

    private fun isSupportedVersion(activity: Activity): Boolean {
        supportedVersion?.let { return it }
        val versionCode = try {
            activity.packageManager.getPackageInfo(PACKAGE_NAME, 0).longVersionCode
        } catch (_: PackageManager.NameNotFoundException) {
            -1L
        } catch (error: RuntimeException) {
            xposed.log(Log.WARN, TAG, "WeChat version query failed", error)
            return false
        }
        val supported = versionCode == SUPPORTED_VERSION_CODE
        supportedVersion = supported
        if (!supported && !unsupportedLogged) {
            xposed.log(Log.INFO, TAG, "unsupported WeChat versionCode=$versionCode")
            unsupportedLogged = true
        }
        return supported
    }

    companion object {
        const val PACKAGE_NAME = "com.tencent.mm"
        const val SUPPORTED_VERSION_CODE = 3180L
        private const val CHATTING_FRAGMENT = "com.tencent.mm.ui.chatting.ChattingUIFragment"
        private const val TAG = "DialogueRouteXposed"
    }
}
