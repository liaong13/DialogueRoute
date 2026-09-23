package io.github.liaong13.dialogueroute

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.github.liaong13.dialogueroute.core.PowerSetup
import io.github.liaong13.dialogueroute.core.Prefs
import kotlin.math.roundToInt

/**
 * Home / setup screen. Card-based layout with a live readiness summary, a
 * guided permission checklist (each row reflects its real granted state), a
 * prominent on/off switch, and a link to settings.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var container: LinearLayout
    private val a11yComponent =
        "io.github.liaong13.dialogueroute/com.google.android.accessibility.selecttospeak.SelectToSpeakService"

    private val accent = Color.parseColor("#8B5CF6")
    private val green = Color.parseColor("#16A34A")
    private val red = Color.parseColor("#DC2626")
    private val ink = Color.parseColor("#111827")
    private val sub = Color.parseColor("#6B7280")

    private fun dp(v: Int) = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).roundToInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        window.decorView.setBackgroundColor(Color.parseColor("#F8F5FF"))

        val scroll = ScrollView(this)
        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(28))
        }
        container.padForSystemBars()   // edge-to-edge: keep the title off the status bar
        scroll.addView(container)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        build()
    }

    private fun build() {
        container.removeAllViews()

        container.addView(text("对话攻略", 24f, ink, bold = true))
        container.addView(text("Dialogue Route · 读懂当前对话，判断关系走向并生成回复选项。选择权和发送权始终由你掌握。",
            13f, sub).apply { setPadding(0, dp(6), 0, dp(16)) })

        val a11y = isA11yEnabled()
        val overlay = Settings.canDrawOverlays(this)
        val key = prefs.hasKey()   // judge route key: the one analysis cannot run without
        val batteryExempt = isBatteryOptimizationExempt()
        val verdict = PowerSetup.verdict(a11y, overlay, key, batteryExempt)

        // Readiness card
        container.addView(statusCard(verdict))

        // Permission checklist
        container.addView(sectionLabel("权限设置"))
        container.addView(permCard("无障碍权限", "读取当前聊天窗口的消息文字", a11y) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        container.addView(permCard("悬浮窗权限", "在聊天窗口上方显示分析卡片", overlay) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        })
        container.addView(sectionLabel("后台运行建议"))
        container.addView(permCard("忽略系统电池优化", "可减少待机限制，不代表厂商后台限制已解除", batteryExempt) {
            openBatterySettings()
        })
        container.addView(permCard("厂商省电策略", "小米/HyperOS 等系统请另行检查「省电无限制」（本项无法自动检测）", null) {
            openAppDetails()
        })
        container.addView(permCard("自启动", "部分 ROM 重启后不会主动拉起服务（本项无法自动检测）", null) {
            openAutostartSettings()
        })

        // Actions
        container.addView(sectionLabel("其他"))
        container.addView(actionRow("设置", "密钥 · 模型 · 关系 · 透明度 · 会话白名单") {
            startActivity(Intent(this, SettingsActivity::class.java))
        })

        // Master toggle
        val toggle = bigToggle(prefs.enabled)
        toggle.setOnClickListener {
            prefs.enabled = !prefs.enabled
            build()
        }
        container.addView(toggle)
    }

    // ---------------------------------------------------------------- cards

    private fun statusCard(v: PowerSetup.Readiness): View {
        val c = cardBox()
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        head.addView(dot(if (v.ready) green else red).apply {
            (layoutParams as LinearLayout.LayoutParams).rightMargin = dp(10)
        })
        head.addView(text(if (v.ready) "攻略准备完成" else "尚未就绪", 16f, if (v.ready) green else ink, bold = true))
        c.addView(head)
        c.addView(checkLine("无障碍", v.accessibility))
        c.addView(checkLine("悬浮窗", v.overlay))
        c.addView(checkLine("密钥", v.key, okWord = "已设", noWord = "未设"))
        if (v.missing.isNotEmpty()) {
            c.addView(text("还差：" + v.missing.joinToString("、"), 12f, red).apply {
                setPadding(0, dp(8), 0, 0)
            })
        }
        if (v.recommendations.isNotEmpty()) {
            c.addView(text("后台运行建议：" + v.recommendations.joinToString("、"), 12f, sub).apply {
                setPadding(0, dp(8), 0, 0)
            })
        }
        // History recording is opt-in (off by default). Mention it here, never block on it.
        if (!prefs.contextEnabled) {
            c.addView(text("关联上下文未开启，可在设置里开启", 12f, sub).apply {
                setPadding(0, dp(8), 0, 0)
            })
        }
        return c
    }

    private fun checkLine(label: String, ok: Boolean, okWord: String = "已开", noWord: String = "未开"): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(5), 0, 0)
        }
        row.addView(text(if (ok) "✓" else "✗", 14f, if (ok) green else red, bold = true).apply {
            (this as TextView).width = dp(22)
        })
        row.addView(text(label + (if (ok) okWord else noWord), 13f, sub))
        return row
    }

    private fun permCard(title: String, desc: String, granted: Boolean?, onClick: () -> Unit): View {
        val c = cardBox()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(text(title, 15f, ink, bold = true))
        left.addView(text(desc, 12f, sub).apply { setPadding(0, dp(3), 0, 0) })
        if (granted == true) left.addView(text("✓ 已开启", 12f, green, bold = true).apply { setPadding(0, dp(4), 0, 0) })
        row.addView(left)
        val buttonLabel = when (granted) {
            true -> "已开启"
            false -> "去开启"
            null -> "去检查"
        }
        row.addView(btn(buttonLabel, granted != true, onClick))
        c.addView(row)
        return c
    }

    private fun actionRow(title: String, desc: String, onClick: () -> Unit): View {
        val c = cardBox()
        c.setOnClickListener { onClick() }
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val left = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        left.addView(text(title, 15f, ink, bold = true))
        left.addView(text(desc, 12f, sub).apply { setPadding(0, dp(3), 0, 0) })
        row.addView(left)
        row.addView(text("›", 22f, sub))
        c.addView(row)
        return c
    }

    private fun bigToggle(on: Boolean): View {
        return TextView(this).apply {
            text = if (on) "攻略助手已开启 · 点击关闭" else "攻略助手已关闭 · 点击开启"
            textSize = 15f; gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
            setTextColor(if (on) Color.WHITE else accent)
            background = roundBg(dp(14), if (on) accent else Color.WHITE, stroke = !on)
            setPadding(dp(16), dp(15), dp(16), dp(15))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(18) }
        }
    }

    // ---------------------------------------------------------------- atoms

    private fun cardBox(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = roundBg(dp(14), Color.WHITE)
        setPadding(dp(14), dp(13), dp(14), dp(13))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(10) }
    }

    private fun sectionLabel(t: String) = text(t, 12f, sub, bold = true).apply {
        setPadding(dp(2), dp(18), 0, dp(2))
    }

    private fun text(t: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = t; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun dot(color: Int) = View(this).apply {
        background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(color) }
        layoutParams = LinearLayout.LayoutParams(dp(10), dp(10))
    }

    private fun btn(label: String, enabled: Boolean, onClick: () -> Unit) = TextView(this).apply {
        text = label; textSize = 13f; gravity = Gravity.CENTER; setTypeface(typeface, Typeface.BOLD)
        setTextColor(if (enabled) Color.WHITE else sub)
        background = roundBg(dp(10), if (enabled) accent else Color.parseColor("#E5E7EB"))
        setPadding(dp(16), dp(8), dp(16), dp(8))
        if (enabled) setOnClickListener { onClick() }
    }

    private fun roundBg(radius: Int, color: Int, stroke: Boolean = false) = GradientDrawable().apply {
        cornerRadius = radius.toFloat(); setColor(color)
        if (stroke) setStroke(dp(1), accent)
    }

    // ------------------------------------------------------------ OEM setup

    // 此 API 只查询系统电池优化白名单，不能判断厂商省电策略或服务是否存活。
    private fun isBatteryOptimizationExempt(): Boolean {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    // 仅处理设置页面无法打开的情况；用户拒绝授权时保留其选择。
    private fun openBatterySettings() {
        val asked = runCatching {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${packageName}")))
        }.isSuccess
        if (asked) return
        runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .onFailure { openAppDetails() }
    }

    // 厂商入口可能缺失或禁止外部启动，失败时逐个尝试，最终引导到应用详情。
    private fun openAutostartSettings() {
        for (route in PowerSetup.AUTOSTART_ROUTES) {
            val started = runCatching {
                startActivity(Intent().apply {
                    setClassName(route.pkg, route.cls)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }.isSuccess
            if (started) return
        }
        Toast.makeText(this, "这个 ROM 没找到自启动页，去应用详情里手动开「自启动」", Toast.LENGTH_LONG).show()
        openAppDetails()
    }

    private fun openAppDetails() {
        runCatching {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${packageName}")))
        }.onFailure {
            Toast.makeText(this, "无法打开应用详情，请在系统设置中找到对话攻略", Toast.LENGTH_LONG).show()
        }
    }

    private fun isA11yEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        return enabled.contains(a11yComponent)
    }
}
