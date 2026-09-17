package com.baxailab.cadebot.ui.ai

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The rewrite only affects what the engine is handed, so the risk is not that it
 * fails to fire — it is that it fires on text it should have left alone. Half of
 * these cases exist to pin that down.
 */
class SpeechNormalizerTest {

    @Test
    fun `currency suffix is read as dong`() {
        assertEquals("Mộc Lam Latte giá 55.000 đồng", normalizeForSpeech("Mộc Lam Latte giá 55.000đ"))
        assertEquals("18000 đồng", normalizeForSpeech("18000đ"))
        assertEquals("chỉ 85.000 đồng thôi", normalizeForSpeech("chỉ 85.000đ thôi"))
    }

    @Test
    fun `currency suffix works with a space and in upper case`() {
        assertEquals("110.000 đồng", normalizeForSpeech("110.000 đ"))
        assertEquals("110.000 đồng", normalizeForSpeech("110.000Đ"))
    }

    @Test
    fun `currency code is read as dong`() {
        assertEquals("Giá 55.000 đồng", normalizeForSpeech("Giá 55.000 VNĐ"))
        assertEquals("55000 đồng", normalizeForSpeech("55000 vnd"))
    }

    @Test
    fun `a d that starts a real word is left alone`() {
        // "đ" here begins đứa / độ / đồng — rewriting it would mangle the sentence.
        assertEquals("có 5 đứa trẻ", normalizeForSpeech("có 5 đứa trẻ"))
        assertEquals("pha ở 90 độ C", normalizeForSpeech("pha ở 90 độ C"))
        assertEquals("giảm 10.000 đồng", normalizeForSpeech("giảm 10.000 đồng"))
    }

    @Test
    fun `quantity x after a number disappears`() {
        assertEquals("1 matcha", normalizeForSpeech("1x - matcha"))
        assertEquals("1 matcha", normalizeForSpeech("1x matcha"))
        assertEquals("2 Latte và 3 Americano", normalizeForSpeech("2x Latte và 3x Americano"))
        assertEquals("2 Latte", normalizeForSpeech("2 × Latte"))
    }

    @Test
    fun `quantity x before a number disappears`() {
        assertEquals("1 Mộc Lam Latte", normalizeForSpeech("x1 Mộc Lam Latte"))
    }

    @Test
    fun `x between two numbers keeps its multiplication sense`() {
        assertEquals("2 x 3", normalizeForSpeech("2 x 3"))
    }

    @Test
    fun `an x inside a word is left alone`() {
        assertEquals("hộp box 2 cái", normalizeForSpeech("hộp box 2 cái"))
        assertEquals("size XL", normalizeForSpeech("size XL"))
    }

    @Test
    fun `a whole answer is rewritten in one pass`() {
        assertEquals(
            "Bạn gọi 2 Mộc Lam Latte, tổng 110.000 đồng nhé.",
            normalizeForSpeech("Bạn gọi 2x Mộc Lam Latte, tổng 110.000đ nhé.")
        )
    }

    @Test
    fun `text with nothing to rewrite is returned unchanged`() {
        val text = "Cadebot sẽ giao món đến bàn bạn sau khi pha chế xong!"
        assertEquals(text, normalizeForSpeech(text))
    }
}
