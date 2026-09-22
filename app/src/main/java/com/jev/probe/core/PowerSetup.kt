package com.jev.probe.core

/** 基础配置与后台运行建议分开判断；配置齐全不代表服务存活或聊天内容可读。 */
object PowerSetup {

    // 只列自启动入口；电池设置页即使能打开，也不能算自启动引导成功。
    val AUTOSTART_ROUTES: List<AutostartRoute> = listOf(
        AutostartRoute(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity",
            "MIUI / HyperOS 自启动管理"),
        AutostartRoute(
            "com.coloros.safecenter",
            "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "ColorOS 自启动管理"),
        AutostartRoute(
            "com.vivo.permissionmanager",
            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "OriginOS 自启动管理"),
        AutostartRoute(
            "com.huawei.systemmanager",
            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity",
            "EMUI 自启动管理")
    )

    data class AutostartRoute(val pkg: String, val cls: String, val label: String)

    data class Readiness(
        val accessibility: Boolean,
        val overlay: Boolean,
        val key: Boolean,
        val batteryOptimizationExempt: Boolean
    ) {
        val ready: Boolean get() = accessibility && overlay && key

        val missing: List<String>
            get() = buildList {
                if (!accessibility) add("无障碍权限")
                if (!overlay) add("悬浮窗权限")
                if (!key) add("判断接口密钥")
            }

        val recommendations: List<String>
            get() = if (batteryOptimizationExempt) emptyList() else listOf("允许忽略系统电池优化")
    }

    fun verdict(
        accessibility: Boolean,
        overlay: Boolean,
        key: Boolean,
        batteryOptimizationExempt: Boolean
    ): Readiness = Readiness(accessibility, overlay, key, batteryOptimizationExempt)
}
