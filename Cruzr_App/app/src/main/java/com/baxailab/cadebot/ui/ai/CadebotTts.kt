package com.baxailab.cadebot.ui.ai

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import java.util.Locale

/**
 * Vietnamese speech for Cadebot answers on Cruzr.
 *
 * Cruzr ships its own speech service and may also have Google TTS sideloaded,
 * so the engine that answers first is not necessarily the one that can speak
 * Vietnamese. This class walks a short cascade instead of committing to one
 * engine:
 *
 *  1. `com.google.android.tts`, when it is installed (requirement: prefer it).
 *  2. Whatever the robot has configured as the system default engine.
 *
 * An engine only counts as usable when `setLanguage(vi-VN)` comes back as
 * something other than `LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED`. That check is
 * the whole point: an engine that silently falls back to its own locale is what
 * made the robot read Vietnamese answers with an English voice. If no engine
 * passes, [isReady] stays false and nothing is spoken — a loud log beats a
 * confidently wrong pronunciation.
 */
class CadebotTts(private val context: Context) {

    companion object {
        private const val TAG = "CadebotTts"
        private val VIETNAMESE = Locale("vi", "VN")
        private const val GOOGLE_ENGINE = "com.google.android.tts"

        /** Old engines truncate very long utterances, so answers are chunked. */
        const val MAX_UTTERANCE_CHARS = 320
    }

    private val readyState = mutableStateOf(false)

    /** Observed by Compose: true once an engine proved it can speak Vietnamese. */
    val isReady: State<Boolean> get() = readyState

    private var engine: TextToSpeech? = null
    private var shutdown = false

    /** `null` means "let the platform pick the default engine". */
    private val candidates: List<String?> = buildList {
        val googleInstalled = runCatching {
            context.packageManager.getPackageInfo(GOOGLE_ENGINE, 0)
            true
        }.getOrDefault(false)
        if (googleInstalled) add(GOOGLE_ENGINE)
        add(null)
    }

    init {
        Log.i(TAG, "engine cascade=${candidates.map { it ?: "<system default>" }}")
        start(0)
    }

    private fun start(index: Int) {
        if (shutdown) return
        if (index >= candidates.size) {
            Log.e(
                TAG,
                "no TTS engine on this device can speak vi-VN — staying silent instead of " +
                    "reading Vietnamese with a foreign voice. Install Vietnamese voice data " +
                    "(Settings > Language & input > Text-to-speech)."
            )
            return
        }

        val requested = candidates[index]
        val label = requested ?: "<system default>"
        val listener = TextToSpeech.OnInitListener { status -> onEngineInit(index, label, status) }

        engine = if (requested != null) {
            TextToSpeech(context, listener, requested)
        } else {
            TextToSpeech(context, listener)
        }
    }

    private fun onEngineInit(index: Int, label: String, status: Int) {
        if (shutdown) return
        if (status != TextToSpeech.SUCCESS) {
            Log.w(TAG, "engine '$label' failed to init (status=$status) — trying next")
            releaseCurrent()
            start(index + 1)
            return
        }
        val current = engine
        if (current == null) {
            Log.w(TAG, "engine '$label' init succeeded but instance was released — trying next")
            start(index + 1)
            return
        }
        if (!adopt(current, label)) {
            releaseCurrent()
            start(index + 1)
            return
        }
        attachProgressListener(current)
        readyState.value = true
    }

    /** Returns true when [tts] can actually speak Vietnamese. */
    private fun adopt(tts: TextToSpeech, label: String): Boolean {
        val languageStatus = runCatching { tts.setLanguage(VIETNAMESE) }
            .getOrElse {
                Log.w(TAG, "engine '$label' threw on setLanguage: ${it.javaClass.simpleName}: ${it.message}")
                return false
            }
        val usable = languageStatus != TextToSpeech.LANG_MISSING_DATA &&
            languageStatus != TextToSpeech.LANG_NOT_SUPPORTED
        Log.i(TAG, "engine '$label' setLanguage vi-VN result=$languageStatus usable=$usable")
        if (!usable) return false

        pinVietnameseVoice(tts, label)
        return true
    }

    /**
     * Google TTS exposes several Vietnamese voices and picks a different one per
     * process start, which makes the robot sound inconsistent between answers.
     * Pin one, preferring a voice that does not need the network.
     */
    private fun pinVietnameseVoice(tts: TextToSpeech, label: String) {
        val chosen = runCatching {
            val vietnamese = tts.voices.orEmpty().filter { it.locale.language == "vi" }
            Log.i(TAG, "engine '$label' Vietnamese voices=${vietnamese.joinToString { it.name }}")
            vietnamese
                .filterNot { it.features.orEmpty().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
                .sortedWith(compareBy<Voice> { it.isNetworkConnectionRequired }.thenByDescending { it.quality })
                .firstOrNull()
        }.getOrNull()

        if (chosen == null) {
            Log.w(TAG, "engine '$label' exposes no explicit Vietnamese voice; using the engine language setting")
            return
        }
        val result = runCatching { tts.setVoice(chosen) }.getOrNull()
        Log.i(TAG, "selected voice=${chosen.name} locale=${chosen.locale} network=${chosen.isNetworkConnectionRequired} setVoice=$result")
    }

    private fun attachProgressListener(tts: TextToSpeech) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                Log.i(TAG, "onStart utterance=$utteranceId")
            }

            override fun onDone(utteranceId: String?) {
                Log.i(TAG, "onDone utterance=$utteranceId")
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                Log.w(TAG, "onStop utterance=$utteranceId interrupted=$interrupted")
            }

            @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
            override fun onError(utteranceId: String?) {
                Log.e(TAG, "onError utterance=$utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                Log.e(TAG, "onError utterance=$utteranceId code=$errorCode")
            }
        })
    }

    /**
     * Speaks [text] in full. [utteranceId] identifies the answer; each chunk gets
     * a `-index` suffix so the progress log shows which part finished.
     */
    fun speak(text: String, utteranceId: String) {
        val tts = engine
        if (!readyState.value || tts == null) {
            Log.w(TAG, "speak skipped: engine not ready (utterance=$utteranceId)")
            return
        }
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return

        // The chat bubble keeps the text exactly as the server wrote it; only the
        // string handed to the engine is rewritten.
        val spoken = normalizeForSpeech(trimmed)
        if (spoken != trimmed) {
            Log.i(TAG, "normalized for speech: \"$trimmed\" -> \"$spoken\"")
        }

        val chunks = splitForTts(spoken)
        Log.i(TAG, "speaking utterance=$utteranceId totalLength=${spoken.length} chunks=${chunks.size}")
        chunks.forEachIndexed { index, chunk ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            @Suppress("DEPRECATION")
            val result = tts.speak(chunk, queueMode, null, "$utteranceId-$index")
            Log.i(TAG, "speak result=$result chunk=${index + 1}/${chunks.size} length=${chunk.length} mode=${if (index == 0) "FLUSH" else "ADD"}")
        }
    }

    private fun releaseCurrent() {
        runCatching { engine?.shutdown() }
        engine = null
    }

    fun shutdown() {
        shutdown = true
        readyState.value = false
        runCatching { engine?.stop() }
        releaseCurrent()
    }
}

/** Keeps each utterance below the input limit of older Android TTS engines. */
internal fun splitForTts(text: String, maxLength: Int = CadebotTts.MAX_UTTERANCE_CHARS): List<String> {
    if (text.length <= maxLength) return listOf(text)

    val chunks = mutableListOf<String>()
    var remaining = text
    while (remaining.length > maxLength) {
        // Break on a sentence end when one sits in the second half of the window,
        // otherwise on a space, so a chunk never ends mid-word.
        val boundary = remaining.lastIndexOfAny(charArrayOf('.', '!', '?', '…', ';'), maxLength)
            .takeIf { it >= maxLength / 2 }
            ?: remaining.lastIndexOf(' ', maxLength).takeIf { it > 0 }
            ?: maxLength
        val piece = remaining.substring(0, boundary + 1).trim()
        if (piece.isNotEmpty()) chunks += piece
        remaining = remaining.substring(boundary + 1).trimStart()
    }
    if (remaining.isNotBlank()) chunks += remaining
    return chunks
}

/**
 * Rewrites a Cadebot answer so a Vietnamese engine pronounces it the way a
 * person would read it aloud. The visible text is never touched — only the copy
 * passed to [CadebotTts.speak].
 *
 * Two things the engine gets wrong on café answers:
 *
 *  - `18.000đ` is spoken letter-by-letter as "đờ" instead of "đồng", because the
 *    engine sees a bare consonant rather than a currency symbol.
 *  - `1x Matcha` is spoken as "một ích Matcha"; the `x` is a written shorthand
 *    for a quantity and should simply disappear.
 *
 * Both rewrites are deliberately narrow. `đ` is only treated as currency when it
 * follows a digit and no letter follows it, so ordinary words survive — "5 đứa"
 * and "20 độ" are left alone. `x` is only dropped when it sits between a number
 * and a word, so "2 x 3" keeps its multiplication sense.
 */
internal fun normalizeForSpeech(text: String): String {
    var spoken = text
    spoken = CURRENCY_SUFFIX.replace(spoken) { "${it.groupValues[1]} đồng" }
    spoken = CURRENCY_CODE.replace(spoken) { "${it.groupValues[1]} đồng" }
    spoken = QUANTITY_AFTER_NUMBER.replace(spoken) { "${it.groupValues[1]} " }
    spoken = QUANTITY_BEFORE_NUMBER.replace(spoken) { it.groupValues[1] }
    return spoken
}

/** `55.000đ`, `55000 Đ` — but not the `đ` that starts a word. */
private val CURRENCY_SUFFIX = Regex("""(\d)\s*[đĐ](?![\p{L}])""")

/** `55.000 VNĐ`, `55000 vnd`. */
private val CURRENCY_CODE = Regex("""(\d)\s*[vV][nN][đĐdD](?![\p{L}\d])""")

/** `1x Matcha`, `1x - Matcha`, `2 × Latte` -> the number alone. */
private val QUANTITY_AFTER_NUMBER = Regex("""(\d)\s*[xX×]\s*[-–—]?\s*(?=\p{L})""")

/**
 * `x1 Matcha` -> `1 Matcha`, the shorthand the cart uses.
 *
 * The second lookbehind is what keeps `2 x 3` intact: without it the space
 * before the `x` satisfies the first lookbehind and the multiplication sign gets
 * swallowed, turning the phrase into "2 3".
 */
private val QUANTITY_BEFORE_NUMBER = Regex("""(?<![\p{L}\d])(?<!\d\s{1,4})[xX×]\s*(\d)""")
