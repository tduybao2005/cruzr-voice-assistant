package com.baxailab.cadebot.ui.ai

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.baxailab.cadebot.network.TlsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

class SttService(private val context: Context) {
    companion object {
        private const val TAG = "CadebotStt"
    }

    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    private val tls = TlsCompat.create(context)
    private val client = OkHttpClient.Builder()
        .sslSocketFactory(tls.sslContext.socketFactory, tls.trustManager)
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .callTimeout(195, TimeUnit.SECONDS)
        .build()

    @Suppress("DEPRECATION")
    fun startRecording() {
        audioFile = File(context.cacheDir, "cadebot_stt.m4a").also { it.delete() }
        Log.i(TAG, "startRecording file=${audioFile!!.absolutePath}")
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(16000)
            setAudioChannels(1)
            setAudioEncodingBitRate(64000)
            setOutputFile(audioFile!!.absolutePath)
            prepare()
            start()
        }
        Log.i(TAG, "recording started")
    }

    fun stopRecording() {
        runCatching {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        }
        mediaRecorder = null
        Log.i(TAG, "recording stopped file=${audioFile?.length() ?: 0} bytes")
    }

    // STT chạy server-side trên gateway (PhoWhisper-large) — APK không giữ API key nào.
    suspend fun transcribe(serverUrl: String): String? = withContext(Dispatchers.IO) {
        val file = audioFile?.takeIf { it.exists() && it.length() > 0 }
        if (file == null) {
            Log.e(TAG, "audio file missing or empty")
            return@withContext null
        }

        runCatching {
            Log.i(TAG, "POST $serverUrl/stt (${file.length()} bytes)")
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "audio.m4a",
                    file.asRequestBody("audio/mp4".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("$serverUrl/stt")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                Log.i(TAG, "STT HTTP ${response.code} bodyLength=${responseBody.length}")
                if (response.isSuccessful) {
                    JSONObject(responseBody).optString("text")
                    .takeIf { it.isNotBlank() }
                } else null
            }
        }.onFailure { error ->
            Log.e(TAG, "STT request failed: ${error.javaClass.simpleName}: ${error.message}", error)
        }.getOrNull()
    }

    fun release() {
        runCatching { mediaRecorder?.release() }
        mediaRecorder = null
    }
}
