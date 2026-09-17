package com.baxailab.cadebot.ui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The robot used to speak less than the chat bubble showed. Chunking is where
 * that can silently come back, so the invariant under test is simple: whatever
 * goes in must come back out, word for word.
 */
class TtsChunkingTest {

    private fun rejoin(chunks: List<String>) = chunks.joinToString(" ")

    @Test
    fun `short answer stays a single utterance`() {
        val text = "Mộc Lam Latte giá 55.000đ bạn nhé."
        assertEquals(listOf(text), splitForTts(text))
    }

    @Test
    fun `long answer keeps every word and respects the limit`() {
        val sentence = "Cadebot xin giới thiệu món Mộc Lam Latte thơm béo pha từ hạt Arabica rang vừa. "
        val text = sentence.repeat(12).trim()

        val chunks = splitForTts(text)

        assertTrue("expected more than one chunk", chunks.size > 1)
        chunks.forEach {
            assertTrue("chunk too long: ${it.length}", it.length <= CadebotTts.MAX_UTTERANCE_CHARS + 1)
            assertTrue("empty chunk", it.isNotBlank())
        }
        assertEquals(
            text.split(Regex("\\s+")),
            rejoin(chunks).split(Regex("\\s+"))
        )
    }

    @Test
    fun `text without spaces or punctuation is still fully spoken`() {
        val text = "a".repeat(1000)
        val chunks = splitForTts(text)
        assertEquals(text, chunks.joinToString(""))
    }
}
