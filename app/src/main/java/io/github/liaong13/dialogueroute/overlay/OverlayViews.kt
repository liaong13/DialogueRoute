package io.github.liaong13.dialogueroute.overlay

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

/** 悬浮窗仍使用 WindowManager View，保留其独立的日夜配色和尺寸。 */
internal object OverlayViews {
    var isDark = true
        private set

    var COLOR_CARD_BG = Color.parseColor("#1C1E22")
        private set
    var COLOR_CARD_STROKE = Color.parseColor("#30333A")
        private set
    var COLOR_PRIMARY = Color.parseColor("#B6BEC9")
        private set
    var COLOR_PRIMARY_CONTAINER = Color.parseColor("#34383F")
        private set
    var COLOR_INK = Color.parseColor("#F3F4F6")
        private set
    var COLOR_SUB = Color.parseColor("#A5AAB3")
        private set
    var COLOR_INPUT_BG = Color.parseColor("#15171A")
        private set
    var COLOR_GREEN = Color.parseColor("#8CB99F")
        private set
    var COLOR_RED = Color.parseColor("#D58E8E")
        private set

    fun configurePalette(context: Context) {
        isDark = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
            Configuration.UI_MODE_NIGHT_YES
        if (isDark) {
            COLOR_CARD_BG = Color.parseColor("#1C1E22")
            COLOR_CARD_STROKE = Color.parseColor("#30333A")
            COLOR_PRIMARY = Color.parseColor("#B6BEC9")
            COLOR_PRIMARY_CONTAINER = Color.parseColor("#34383F")
            COLOR_INK = Color.parseColor("#F3F4F6")
            COLOR_SUB = Color.parseColor("#A5AAB3")
            COLOR_INPUT_BG = Color.parseColor("#15171A")
            COLOR_GREEN = Color.parseColor("#8CB99F")
            COLOR_RED = Color.parseColor("#D58E8E")
        } else {
            COLOR_CARD_BG = Color.WHITE
            COLOR_CARD_STROKE = Color.parseColor("#DCE7F5")
            COLOR_PRIMARY = Color.parseColor("#1769C2")
            COLOR_PRIMARY_CONTAINER = Color.parseColor("#E0EEFF")
            COLOR_INK = Color.parseColor("#172A43")
            COLOR_SUB = Color.parseColor("#596D86")
            COLOR_INPUT_BG = Color.parseColor("#F2F6FC")
            COLOR_GREEN = Color.parseColor("#218356")
            COLOR_RED = Color.parseColor("#BC4A4A")
        }
    }

    fun dp(context: Context, value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), context.resources.displayMetrics
    ).roundToInt()

    fun createButton(context: Context, label: String, onClick: () -> Unit): TextView =
        MaterialButton(context, null, com.google.android.material.R.attr.materialButtonStyle).apply {
            text = label
            textSize = 15f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
            isAllCaps = false
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(context, 16) }
            setOnClickListener { onClick() }
        }
}
