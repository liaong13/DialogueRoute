package io.github.liaong13.dialogueroute.capture

import android.content.res.Resources
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.github.liaong13.dialogueroute.core.BubbleRect
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.Msg

/** Only my own Feishu bubbles carry the sent/read strip. */
private const val FEISHU_READ_STATE_ID = "time_read_state_container_align_bubble"

/** Does this bubble carry the "sent / read" strip that only mine have? */
private fun feishuHasReadState(bubble: AccessibilityNodeInfo): Boolean {
    val stack = ArrayDeque<AccessibilityNodeInfo>()
    stack.addLast(bubble)
    var guard = 0
    while (stack.isNotEmpty() && guard < 400) {
        guard++
        val node = stack.removeLast()
        val id = node.viewIdResourceName ?: ""
        if (id.endsWith(FEISHU_READ_STATE_ID)) return true
        for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
    }
    return false
}

/**
 * Feishu bubble rectangles in SCREEN coordinates, top to bottom, with the side
 * the read-receipt strip implies.
 *
 * Split out of [FeishuAdapter.extract] so the capture service can call it again
 * from inside the screenshot callback: several hundred ms pass between reading
 * the tree and the picture arriving (debounce + overlay hide + the shot itself),
 * and a list that scrolled in between would make us crop the wrong rows.
 */
internal fun collectFeishuBubbleRects(
    root: AccessibilityNodeInfo,
    res: Resources
): List<BubbleRect> {
    val height = res.displayMetrics.heightPixels
    val topBand = (height * 0.14).toInt()      // action bar + tab row
    val bottomBand = (height * 0.84).toInt()   // input box + keyboard
    val rects = ArrayList<BubbleRect>()
    val stack = ArrayDeque<AccessibilityNodeInfo>()
    stack.addLast(root)
    var guard = 0
    while (stack.isNotEmpty() && guard < 6000) {
        guard++
        val node = stack.removeLast()
        val id = node.viewIdResourceName ?: ""
        if (id.endsWith(":id/bubble_content_container")) {
            val b = Rect(); node.getBoundsInScreen(b)
            if (b.width() > 0 && b.height() > 0 && b.bottom > topBand && b.top < bottomBand) {
                rects.add(BubbleRect(Rect(b), if (feishuHasReadState(node)) "me" else "other"))
            }
        }
        for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
    }
    rects.sortBy { it.rect.top }
    return rects
}

/**
 * Feishu / Lark (com.ss.android.lark). Nodes are not obfuscated, but the message
 * text is DRAWN, not laid out as views (verified 2026-09-21): the tree gives us
 * bubble rectangles and chrome, and almost never a body. So this adapter is a
 * hybrid — it reports what it can read as messages (usually nothing) and always
 * reports the bubble geometry in [ChatSnapshot.bubbleRects] for the service to
 * OCR rect by rect.
 *
 * "In a chat window" = the tree has `id/message`, `id/bubble_content_container`
 * or the `id/kb_rich_text_content` input box.
 *
 * Side: Feishu left-aligns everyone, so geometry says nothing. What does say
 * something is the read-receipt strip (`…time_read_state_container_align_bubble`)
 * that only hangs off MY bubbles — present → "me", absent → "other". Unverified
 * on a real device (see the B-stage report's gaps).
 */
class FeishuAdapter : ChatAppAdapter {
    override val pkg = "com.ss.android.lark"

    override fun extract(root: AccessibilityNodeInfo, res: Resources): ChatSnapshot? {
        val width = res.displayMetrics.widthPixels
        val height = res.displayMetrics.heightPixels
        val topBand = (height * 0.14).toInt()      // action bar + tab row
        val bottomBand = (height * 0.84).toInt()   // input box + keyboard

        var isChat = false
        var title: String? = null
        val items = ArrayList<Triple<Int, Int, String>>() // top, centerX, text
        // Same collection the service re-runs inside the screenshot callback.
        val rects = collectFeishuBubbleRects(root, res)

        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var guard = 0
        while (stack.isNotEmpty() && guard < 6000) {
            guard++
            val node = stack.removeLast()
            val id = node.viewIdResourceName ?: ""
            if (id.endsWith(":id/message") || id.endsWith(":id/bubble_content_container") ||
                id.endsWith(":id/kb_rich_text_content")) isChat = true
            if (id.endsWith(":id/group_name")) node.text?.toString()?.let { if (title == null) title = it }

            val text = node.text?.toString()
            val cls = node.className?.toString()
            if (!text.isNullOrBlank() && cls == "android.widget.TextView" && !isChrome(id) && !looksLikeTimestamp(text)) {
                val b = Rect(); node.getBoundsInScreen(b)
                if (b.top in (topBand + 1) until bottomBand) {
                    items.add(Triple(b.top, b.centerX(), text.trim()))
                }
            }
            for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
        }
        if (!isChat) return null

        if (items.isEmpty()) return ChatSnapshot(title, emptyList(), rects)

        items.sortBy { it.first }
        val msgs = items.map { (_, cx, text) ->
            Msg(if (cx > width / 2) "me" else "other", text)
        }
        return ChatSnapshot(title, msgs, rects)
    }

    /** Non-message UI text to skip: title, sender name, time, system notices,
     *  the input EditText. Bodies have no id (bare TextView) so they pass. */
    private fun isChrome(id: String): Boolean =
        id.endsWith(":id/group_name") ||
            id.endsWith(":id/name_tv") ||
            id.endsWith(":id/date_tv") ||
            id.endsWith(":id/system_label") ||
            id.endsWith(":id/kb_rich_text_content") ||
            id.endsWith(":id/thread_title_tv") ||
            id.endsWith(":id/thread_subtitle_tv")
}
