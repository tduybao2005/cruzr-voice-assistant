package com.baxailab.cadebot.data.remote

import android.content.Context
import com.baxailab.cadebot.data.model.CartItem
import com.baxailab.cadebot.data.model.OrderCreateResult
import com.baxailab.cadebot.data.model.OrderStatusResult
import com.baxailab.cadebot.network.TlsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class PaymentApiException(message: String) : Exception(message)

class PaymentApiService internal constructor(
    private val baseUrl: String,
    private val client: OkHttpClient
) {
    /** Production entry point; [AppModule] passes the application context. */
    constructor(context: Context, baseUrl: String) : this(baseUrl, tlsClient(context))

    companion object {
        // Android 5.1 trên Cruzr thiếu root ISRG của Let's Encrypt trong trust store
        // hệ thống -> handshake tới cadebot.example.com ném
        // CertPathValidatorException "Trust anchor for certification path not found".
        // CadebotApiService và SttService đã dùng TlsCompat; service này trước đây bị
        // bỏ sót, nên riêng thanh toán chết còn chat/STT thì chạy.
        private fun tlsClient(context: Context): OkHttpClient {
            val tls = TlsCompat.create(context)
            return baseClient()
                .sslSocketFactory(tls.sslContext.socketFactory, tls.trustManager)
                .build()
        }

        /**
         * JVM unit tests have no Context (and MockWebServer speaks plain HTTP),
         * so they build the same client without the Android-only TLS patch.
         */
        internal fun testClient(): OkHttpClient = baseClient().build()

        private fun baseClient() = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
    }

    suspend fun createOrder(tableId: String, items: List<CartItem>): Result<OrderCreateResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val itemsArr = JSONArray()
                items.forEach { cartItem ->
                    val optionsJson = JSONObject()
                        .put("size", cartItem.selectedSize)
                        .put("sweetness", cartItem.selectedSweetness)
                        .put("ice", cartItem.selectedIce)
                        .put("temperature", cartItem.selectedTemperature)
                        .put("toppings", JSONArray(cartItem.selectedToppings))
                        .put("note", cartItem.note)
                    itemsArr.put(
                        JSONObject()
                            .put("itemCode", cartItem.menuItem.menuItemId)
                            .put("quantity", cartItem.quantity)
                            .put("options", optionsJson)
                    )
                }
                val bodyJson = JSONObject()
                    .put("tableId", tableId)
                    .put("items", itemsArr)
                    .toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$baseUrl/api/orders/create")
                    .post(bodyJson)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw PaymentApiException("Tạo đơn thất bại (${response.code})")
                }
                val raw = response.body?.string() ?: throw PaymentApiException("Phản hồi trống")
                val json = JSONObject(raw)
                OrderCreateResult(
                    orderCode = json.getString("orderCode"),
                    tableId = json.getString("tableId"),
                    totalAmount = json.getInt("totalAmount"),
                    status = json.getString("status"),
                    qrUrl = json.getString("qrUrl"),
                    transferContent = json.getString("transferContent"),
                    expiresAt = json.getString("expiresAt"),
                    secondsRemaining = json.getInt("secondsRemaining")
                )
            }
        }

    suspend fun getOrderStatus(orderCode: String): Result<OrderStatusResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("$baseUrl/api/orders/$orderCode/status")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw PaymentApiException("Không lấy được trạng thái đơn (${response.code})")
                }
                val raw = response.body?.string() ?: throw PaymentApiException("Phản hồi trống")
                val json = JSONObject(raw)
                OrderStatusResult(
                    orderCode = json.getString("orderCode"),
                    status = json.getString("status"),
                    totalAmount = json.getInt("totalAmount"),
                    paidAmount = if (json.isNull("paidAmount")) null else json.getInt("paidAmount"),
                    paidAt = if (json.isNull("paidAt")) null else json.getString("paidAt"),
                    secondsRemaining = json.getInt("secondsRemaining"),
                    qrUrl = json.getString("qrUrl")
                )
            }
        }
}
