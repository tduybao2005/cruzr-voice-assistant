package com.baxailab.cadebot.data.remote

import android.content.Context
import com.baxailab.cadebot.data.model.AiMessage
import com.baxailab.cadebot.network.TlsCompat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CadebotApiService(
    context: Context,
    private val baseUrl: String
) {
    companion object {
        private const val TAG = "CadebotChat"
    }

    private val apiRoot = baseUrl.trimEnd('/')

    private val tls = TlsCompat.create(context)
    private val client = OkHttpClient.Builder()
        .sslSocketFactory(tls.sslContext.socketFactory, tls.trustManager)
        .connectTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .callTimeout(330, TimeUnit.SECONDS)
        .build()

    suspend fun processQuery(message: String, history: List<AiMessage>): AiMessage =
        withContext(Dispatchers.IO) {
            runCatching {
                val historyArr = JSONArray()
                history.takeLast(10).forEach { msg ->
                    historyArr.put(
                        JSONObject()
                            .put("role", if (msg.isUser) "user" else "assistant")
                            .put("content", msg.content)
                    )
                }

                val bodyJson = JSONObject()
                    .put("message", message)
                    .put("history", historyArr)
                    .toString()
                    .toRequestBody("application/json".toMediaType())

                Log.i(TAG, "POST $apiRoot/chat messageLength=${message.length} history=${history.takeLast(10).size}")
                val request = Request.Builder()
                    .url("$apiRoot/chat")
                    .post(bodyJson)
                    .build()

                client.newCall(request).execute().use { response ->
                    val raw = response.body?.string().orEmpty()
                    Log.i(TAG, "CHAT HTTP ${response.code} bodyLength=${raw.length}")
                    if (response.isSuccessful) {
                        if (raw.isBlank()) throw Exception("Empty response")
                        val wrapper = JSONObject(raw)
                        parseModelOutput(wrapper.getString("response"))
                    } else {
                        AiMessage(
                            content = "Cadebot đang bận, thử lại sau bạn nhé! (${response.code})",
                            isUser = false
                        )
                    }
                }
            }.onFailure { error ->
                Log.e(TAG, "CHAT request failed: ${error.javaClass.simpleName}: ${error.message}", error)
            }.getOrElse {
                AiMessage(
                    content = "Không kết nối được server. Kiểm tra WiFi và đảm bảo server đang chạy nhé!",
                    isUser = false
                )
            }
        }

    private fun parseModelOutput(raw: String): AiMessage {
        return runCatching {
            val start = raw.indexOf("{")
            val end = raw.lastIndexOf("}")
            if (start < 0 || end < 0) return AiMessage(content = raw.trim(), isUser = false)

            val json = JSONObject(raw.substring(start, end + 1))
            val answerText = json.optString("answerText").takeIf { it.isNotBlank() } ?: raw.trim()
            val spokenText = json.optString("spokenText").takeIf { it.isNotBlank() } ?: answerText
            val recommendedArr = json.optJSONArray("recommendedItems")
            val recommendedIds = mutableListOf<String>()

            recommendedArr?.let { arr ->
                for (i in 0 until arr.length()) {
                    when (val item = arr[i]) {
                        is JSONObject -> item.optString("menuItemId").takeIf { it.isNotBlank() }
                            ?.let { recommendedIds.add(it) }
                        is String -> if (item.isNotBlank()) recommendedIds.add(item)
                    }
                }
            }

            AiMessage(content = answerText, isUser = false, spokenText = spokenText, recommendedItems = recommendedIds)
        }.getOrElse {
            AiMessage(content = raw.trim(), isUser = false)
        }
    }
}
