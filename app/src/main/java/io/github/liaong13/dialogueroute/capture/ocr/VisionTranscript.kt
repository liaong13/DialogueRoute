package io.github.liaong13.dialogueroute.capture.ocr

import io.github.liaong13.dialogueroute.core.Msg

/** 只接受明确标注发送方的气泡，未标注的文本不猜测归属。 */
internal data class VisionTranscript(val title: String?, val messages: List<Msg>) {
    companion object {
        private val bubble = Regex("^(我|对方)[：:]\\s*(.+)$")
        private val heading = Regex("^标题[：:]\\s*(.+)$")
        private val listMarker = Regex("^(?:[-*•]\\s+|\\d+[.、]\\s*)")

        fun parse(raw: String): VisionTranscript {
            var title: String? = null
            val messages = ArrayList<Msg>()
            raw.lineSequence().forEach { line ->
                val text = line.trim().replaceFirst(listMarker, "")
                    .removeSurrounding("**").trim()
                if (title == null) heading.matchEntire(text)?.let {
                    title = it.groupValues[1].trim().takeIf { value -> value.isNotEmpty() }
                }
                bubble.matchEntire(text)?.let {
                    val body = it.groupValues[2].trim()
                    if (body.isNotEmpty()) messages.add(Msg(
                        if (it.groupValues[1] == "我") "me" else "other", body))
                }
            }
            return VisionTranscript(title, messages.takeLast(30))
        }
    }
}
