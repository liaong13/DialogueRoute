package io.github.liaong13.dialogueroute.xposed

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.PackageReadyParam
import io.github.liaong13.dialogueroute.xposed.wechat.WeChatPlugin

/** 同一 APK 的现代 Xposed 入口；普通应用组件不会实例化它。 */
class DialogueRouteModule : XposedModule() {
    private var processName: String? = null
    private var installed = false
    private var attachHook: XposedInterface.HookHandle? = null
    private var weChatPlugin: WeChatPlugin? = null
    private var lifecycleCallbacks: Application.ActivityLifecycleCallbacks? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        processName = param.processName
        log(Log.INFO, TAG, "module loaded; api=$apiVersion; process=${param.processName}")
    }

    override fun onPackageReady(param: PackageReadyParam) {
        if (installed || param.packageName != WeChatPlugin.PACKAGE_NAME ||
            processName != WeChatPlugin.PACKAGE_NAME || apiVersion < 102
        ) return

        try {
            val attach = Application::class.java.getDeclaredMethod("attach", Context::class.java)
            attachHook = hook(attach).intercept { chain ->
                val result = chain.proceed()
                val application = chain.thisObject as? Application
                if (application?.packageName == WeChatPlugin.PACKAGE_NAME) {
                    registerLifecycle(application)
                }
                result
            }
            installed = true
            log(Log.INFO, TAG, "application lifecycle hook installed")
        } catch (error: Throwable) {
            log(Log.WARN, TAG, "application lifecycle hook failed", error)
        }
    }

    private fun registerLifecycle(application: Application) {
        if (lifecycleCallbacks != null) return
        val callbacks = object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                if (activity.javaClass.name != "com.tencent.mm.ui.LauncherUI" ||
                    weChatPlugin != null) return
                val liveLoader = activity.javaClass.classLoader ?: return
                val plugin = WeChatPlugin(this@DialogueRouteModule)
                val ready = try { plugin.install(liveLoader) }
                catch (error: Throwable) {
                    log(Log.WARN, TAG, "chat hook installation failed", error)
                    false
                }
                if (ready) {
                    weChatPlugin = plugin
                    log(Log.INFO, TAG, "chat hooks installed from live Activity classloader")
                }
            }
            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        }
        lifecycleCallbacks = callbacks
        application.registerActivityLifecycleCallbacks(callbacks)
        log(Log.INFO, TAG, "application lifecycle callbacks registered")
    }

    companion object {
        private const val TAG = "DialogueRouteXposed"
    }
}
