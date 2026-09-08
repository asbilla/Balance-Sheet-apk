package com.example.data.remote

import android.util.Log
import com.example.data.local.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class RemoteTransaction(
    val id: String,
    val date: String,
    val type: String,
    val notes: String,
    val amount: Double,
    val createdAt: String = ""
)

class SheetsApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
) {

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun postTransaction(webAppUrl: String, transaction: TransactionEntity): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Google Apps Script URL is empty"))
                }

                val payload = JSONObject().apply {
                    put("action", "add")
                    put("id", transaction.uuid)
                    put("date", transaction.date)
                    put("type", transaction.type)
                    put("notes", transaction.category)
                    put("amount", transaction.amount)
                    put("timestamp", transaction.timestamp)
                }

                val requestBody = payload.toString().toRequestBody(jsonMediaType)
                val request = Request.Builder()
                    .url(webAppUrl)
                    .post(requestBody)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        Log.d(TAG, "Successfully posted transaction: $responseBody")
                        Result.success(responseBody)
                    } else {
                        Log.e(TAG, "Failed posting transaction. Code: ${response.code}, body: $responseBody")
                        Result.failure(Exception("HTTP Error ${response.code}: $responseBody"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during postTransaction", e)
                Result.failure(e)
            }
        }
    }

    suspend fun fetchTransactions(webAppUrl: String): Result<List<RemoteTransaction>> {
        return withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Google Apps Script URL is empty"))
                }

                val request = Request.Builder()
                    .url(webAppUrl)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(Exception("HTTP Error ${response.code}: $responseBody"))
                    }

                    val transactions = parseTransactionsJson(responseBody)
                    Result.success(transactions)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching transactions", e)
                Result.failure(e)
            }
        }
    }

    suspend fun testConnection(webAppUrl: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                if (webAppUrl.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Please enter a valid URL"))
                }

                val request = Request.Builder()
                    .url(webAppUrl)
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        Result.success("Connection successful (HTTP ${response.code})")
                    } else {
                        Result.failure(Exception("Server returned HTTP ${response.code}: $responseBody"))
                    }
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun parseTransactionsJson(jsonString: String): List<RemoteTransaction> {
        val list = mutableListOf<RemoteTransaction>()
        val trimmed = jsonString.trim()

        val jsonArray: JSONArray = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                when {
                    obj.has("data") && obj.get("data") is JSONArray -> obj.getJSONArray("data")
                    obj.has("transactions") && obj.get("transactions") is JSONArray -> obj.getJSONArray("transactions")
                    else -> JSONArray()
                }
            }
            else -> JSONArray()
        }

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            val date = item.optString("date", "")
            val type = item.optString("type", "Daily Income")
            val notes = when {
                item.has("notes") -> item.optString("notes", "")
                item.has("category") -> item.optString("category", "")
                else -> ""
            }
            val amount = item.optDouble("amount", 0.0)
            val createdAt = item.optString("createdAt", "")

            if (date.isNotBlank() || amount > 0.0) {
                list.add(
                    RemoteTransaction(
                        id = id,
                        date = date,
                        type = type,
                        notes = notes,
                        amount = amount,
                        createdAt = createdAt
                    )
                )
            }
        }

        return list
    }

    companion object {
        private const val TAG = "SheetsApiService"
    }
}
