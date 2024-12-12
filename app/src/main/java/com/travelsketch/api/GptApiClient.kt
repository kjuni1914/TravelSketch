package com.travelsketch.api

import android.content.Context
import android.net.Uri
import android.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object GPTApiClient {
    private val client = OkHttpClient()
    private const val URL = "https://api.openai.com/v1/chat/completions"

    fun sendImage(
        imageUri: Uri?,
        context: Context,
        apiKey: String,
        onResult: (String) -> Unit
    ) {

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val inputStream = imageUri?.let { context.contentResolver.openInputStream(it) }
        val bytes = inputStream?.readBytes()
        val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)

        val requestBody = JSONObject().apply {
            put("model", "gpt-4o-2024-08-06")
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")

                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", "Extract the receipt photo as ‘product name, price’, and translate in Korean")
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().apply {
                                put("url", "data:image/jpeg;base64,${base64Image}")
                            })
                        })
                    })
                })
            })
        }.toString().toRequestBody(mediaType)

        val request = Request.Builder()
            .url(URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                onResult("Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        try {
                            val responseBody = it.body?.string()
                            if (responseBody == null) {
                                onResult("Empty response body")
                                return
                            }

                            val responseJson = JSONObject(responseBody)
                            val firstChoice = responseJson.getJSONArray("choices").getJSONObject(0)

                            // Check if it's a tool call response
                            if (firstChoice.getJSONObject("message").has("tool_calls")) {
                                val toolCalls = firstChoice.getJSONObject("message").getJSONArray("tool_calls")
                                val functionCall = toolCalls.getJSONObject(0).getJSONObject("function")
                                val arguments = functionCall.getString("arguments")

                                onResult(arguments)
                            } else {
                                // Regular message response
                                val messageContent = firstChoice
                                    .getJSONObject("message")
                                    .getString("content")
                                onResult(messageContent)
                            }
                        } catch (e: Exception) {
                            onResult("Parsing Error: ${e.message}")
                        }
                    } else {
                        // Print out the error body for more details
                        val errorBody = it.body?.string()
                        onResult("Error: ${response.code} - ${response.message}\nDetails: $errorBody")
                    }
                }
            }
        })
    }
}