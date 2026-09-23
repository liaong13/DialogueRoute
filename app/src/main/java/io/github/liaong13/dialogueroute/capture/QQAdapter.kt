package io.github.liaong13.dialogueroute.capture

import android.content.res.Resources
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.Msg

/**
 * Mobile QQ (com.tencent.mobileqq). Nodes are NOT obfuscated (verified on QQ
 * 9.3.50 / Xiaomi 14, 1200x2670): message bodies are plain TextViews carrying
 * `id/mjn`, so collecting only that id already excludes timestamps, sender
 * nicknames (`id/mjq`) and the full-width system notice strips.
 *
 * The whole app lives under one SplashActivity (fragment architecture), so
 * "are we in a chat window" can only be answered by the tree itself — here, by
 * the chat input box `id/input`. No input box → not a chat → null; input box
 * but no `id/mjn` bodies → empty snapshot (OCR fallback's cue).
 *
 * Sender side: QQ pins the avatar to the outer edge of its own side (others on
 * the left at x≈156/1200 ≈ 13% of width, me on the right at width−156). A long
 * incoming message can push its center past mid-screen, so we compare which
 * edge of the bubble hugs its avatar column instead of using the center point.
 */
class QQAdapter : ChatAppAdapter {
    override val pkg = "com.tencent.mobileqq"

    override fun extract(root: AccessibilityNodeInfo, res: Resources): ChatSnapshot? {
        val width = res.displayMetrics.widthPixels
        // top, left, right, text
        val bubbles = ArrayList<Bubble>()
        var firstBubbleTop = Int.MAX_VALUE
        var title: String? = null
        var hasInput = false

        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var guard = 0
        while (stack.isNotEmpty() && guard < 5000) {
            guard++
            val node = stack.removeLast()
            val id = node.viewIdResourceName
            val text = node.text?.toString()
            if (id == BUBBLE_ID && !text.isNullOrBlank()) {
                val b = Rect(); node.getBoundsInScreen(b)
                bubbles.add(Bubble(b.top, b.left, b.right, text))
                if (b.top < firstBubbleTop) firstBubbleTop = b.top
            }
            if (!hasInput && id == INPUT_ID) hasInput = true
            if (id == TITLE_ID && title == null) text?.let { if (it.isNotBlank()) title = it }
            for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
        }
        if (bubbles.isEmpty() && !hasInput) return null

        if (title == null) title = findTitleInActionBar(root, firstBubbleTop, width, res)
        if (bubbles.isEmpty()) return ChatSnapshot(title, emptyList())

        val avatarEdge = (width * 0.13).toInt()
        bubbles.sortBy { it.top }
        val msgs = bubbles.map { b ->
            val dl = kotlin.math.abs(b.left - avatarEdge)
            val dr = kotlin.math.abs((width - avatarEdge) - b.right)
            Msg(if (dr < dl) "me" else "other", b.text)
        }
        return ChatSnapshot(title, msgs)
    }

    private data class Bubble(val top: Int, val left: Int, val right: Int, val text: String)

    companion object {
        private const val BUBBLE_ID = "com.tencent.mobileqq:id/mjn"
        private const val TITLE_ID = "com.tencent.mobileqq:id/371"
        private const val INPUT_ID = "com.tencent.mobileqq:id/input"
    }
}
