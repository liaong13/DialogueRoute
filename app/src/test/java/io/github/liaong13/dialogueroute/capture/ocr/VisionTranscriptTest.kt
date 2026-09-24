package io.github.liaong13.dialogueroute.capture.ocr

import io.github.liaong13.dialogueroute.core.Msg
import org.junit.Assert.assertEquals
import org.junit.Test

class VisionTranscriptTest {
    @Test
    fun `accepts labeled bubbles and ignores unlabeled model text`() {
        val result = VisionTranscript.parse("""
            标题：小明
            1. 对方：今晚有空吗？
            - **我：八点以后可以**
            总结：两人正在约时间
            没有发送方的正文
        """.trimIndent())

        assertEquals("小明", result.title)
        assertEquals(listOf(Msg("other", "今晚有空吗？"), Msg("me", "八点以后可以")),
            result.messages)
    }

    @Test
    fun `unlabeled reply does not invent a sender`() {
        assertEquals(emptyList<Msg>(), VisionTranscript.parse("识别到一条消息").messages)
    }
}
