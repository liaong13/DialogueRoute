package io.github.liaong13.dialogueroute.capture

import android.content.res.Resources
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import io.github.liaong13.dialogueroute.core.ChatSnapshot
import io.github.liaong13.dialogueroute.core.Msg

/** Trailing "8:11 上午" / "10:29 下午" / "8:11 AM" stamp X glues onto a message. */
private val X_TAIL_TIME = Regex("""\d{1,2}[:：]\d{2}\s*(上午|下午|AM|PM|am|pm)?$""")

/** X uses "。" as a field separator, so a message can end with a run of them. */
private val X_TRAILING_DOTS = Regex("""。+$""")

/**
 * Split one X DM row's contentDescription into (sender, body).
 *
 * "你：你这个说的就是那个虚拟人物，是吗？。8:11 上午。Read。"
 *      → ("你", "你这个说的就是那个虚拟人物，是吗？")
 * "你：他这个东西开源应该问题不大。。。Read。"
 *      → ("你", "他这个东西开源应该问题不大")
 * "All-In：附加的帖子。。"          → ("All-In", "附加的帖子")
 *
 * The sender is everything before the FIRST separator (full-width "：" in the
 * Chinese UI, ": " as a rough fallback elsewhere); the rest is the body plus
 * chrome — the read receipt, the timestamp, and the "。" gluing them on — which
 * is stripped from the tail in that order. Punctuation the user actually typed
 * ("是吗？") survives. Null when there is no separator or nothing is left.
 */
private fun parseXDesc(desc: String): Pair<String, String>? {
    val full = desc.indexOf('：')
    val half = desc.indexOf(": ")
    val cut: Int
    val skip: Int
    when {
        full >= 0 && (half < 0 || full <= half) -> { cut = full; skip = 1 }
        half >= 0 -> { cut = half; skip = 2 }
        else -> return null
    }
    val sender = desc.substring(0, cut).trim()
    var body = desc.substring(cut + skip).trim()
    for (tail in arrayOf("Read。", "Read", "已读。", "已读")) {
        if (body.endsWith(tail)) { body = body.removeSuffix(tail).trim(); break }
    }
    body = X_TRAILING_DOTS.replace(body, "").trim()
    X_TAIL_TIME.find(body)?.let { body = body.substring(0, it.range.first).trim() }
    body = X_TRAILING_DOTS.replace(body, "").trim()
    if (sender.isEmpty() || body.isEmpty()) return null
    return sender to body
}

/**
 * X / Twitter (com.twitter.android) direct messages. Verified on X 12.25.2 /
 * Xiaomi 14 (1200x2670), Chinese system language.
 *
 * The DM thread is Compose UI: each message is a bare `android.view.View` with
 * NO resource-id, full screen width and empty text — the whole message lives in
 * contentDescription ("All-In：重新写了一个😂。10:29 下午。"). The date divider is a
 * TextView with no "：", so filtering on class + full width + a separator keeps
 * it out. An attachment row ("All-In：附加的帖子。。") nests the quoted post's own
 * TextViews; we only take the row View's own desc, never its children.
 *
 * Every screen runs under the same MainActivity, so "are we in a DM thread" can
 * only be answered by the tree: a thread has the message EditText, the DM list
 * does not. The list's rows look similar but read
 * "All-In, @all_in_2026, 你这个说的就是那…", so ", @" is an extra guard.
 *
 * Side comes from the sender label ("你" / "You"), not geometry — every row is
 * full width no matter who spoke.
 */
class XAdapter : ChatAppAdapter {
    override val pkg = "com.twitter.android"

    override fun extract(root: AccessibilityNodeInfo, res: Resources): ChatSnapshot? {
        val width = res.displayMetrics.widthPixels
        val rows = ArrayList<Row>()
        var firstRowTop = Int.MAX_VALUE
        var hasInput = false
        // A full-width View whose desc has a separator ("：" / ": ") — the shape
        // of a message row, whether or not parseXDesc could fully parse it.
        var hasMessageRowShape = false
        // A thread with zero messages still carries this placeholder text.
        var hasDmLabel = false
        // DM-list-only signals: the "compose new DM" affordance, or a "聊天/
        // Messages" heading with nothing under it (parsed as an actual row).
        var hasNewDmMarker = false
        var sawListHeading = false

        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)
        var guard = 0
        while (stack.isNotEmpty() && guard < 6000) {
            guard++
            val node = stack.removeLast()
            val cls = node.className?.toString()
            if (!hasInput && (node.isEditable || cls == "android.widget.EditText")) hasInput = true

            val desc = node.contentDescription?.toString()
            if (desc == "新私信" || desc == "New message") hasNewDmMarker = true
            if (cls == "android.view.View" && !desc.isNullOrBlank() && !desc.contains(", @")) {
                val b = Rect(); node.getBoundsInScreen(b)
                if (b.left == 0 && b.right == width) {
                    if (desc.contains('：') || desc.contains(": ")) hasMessageRowShape = true
                    val parsed = parseXDesc(desc)
                    if (parsed != null) {
                        rows.add(Row(b.top, parsed.first, parsed.second))
                        if (b.top < firstRowTop) firstRowTop = b.top
                    }
                }
            }

            if (cls == "android.widget.TextView") {
                val text = node.text?.toString()?.trim()
                if (text == "私信" || text == "Message" || text == "发送私信") hasDmLabel = true
                if (text == "聊天" || text == "Messages") sawListHeading = true
            }
            for (i in node.childCount - 1 downTo 0) node.getChild(i)?.let { stack.addLast(it) }
        }
        // Explicit "this is the DM list, not a thread" signals — checked before
        // the generic rule below so they win even if a search box's EditText
        // would otherwise have counted as "hasInput".
        if (hasNewDmMarker || (sawListHeading && rows.isEmpty())) return null

        // A chat window needs BOTH an input box AND something that only a
        // thread has: an actual message-row shape, or the "私信" placeholder an
        // empty thread shows. A list screen's search box has an EditText too,
        // so hasInput alone used to misfire OCR fallback on the DM list.
        if (!hasInput || !(hasMessageRowShape || hasDmLabel)) return null

        // X left-aligns the thread title (x≈300..443 of 1200), so widen the
        // shared helper's "roughly centered" band for this app.
        val title = findTitleInActionBar(root, firstRowTop, width, res, 0.15, 0.85)
        // In a DM thread but no rows parsed → empty snapshot (OCR fallback cue).
        if (rows.isEmpty()) return ChatSnapshot(title, emptyList())
        rows.sortBy { it.top }
        val msgs = rows.map { Msg(if (it.sender == "你" || it.sender == "You") "me" else "other", it.text) }
        return ChatSnapshot(title, msgs)
    }

    private data class Row(val top: Int, val sender: String, val text: String)
}
