package com.travelsketch.api
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject

object PushNotificationHelper {
    private const val SERVER_KEY = "AIzaSyASGe509Yx7iQJNjQbdhFwhkEr_G6BgOdk" // Replace with your Firebase Server Key
    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

    fun sendPushNotification(to: String, title: String, body: String) {
        val client = OkHttpClient()

        try {
            val json = JSONObject().apply {
                put("to", to)
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                })
            }

            val requestBody = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                json.toString()
            )

            val request = Request.Builder()
                .url(FCM_URL)
                .post(requestBody)
                .addHeader("Authorization", "key=$SERVER_KEY")
                .addHeader("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d("PushNotification", "Push notification sent successfully: ${response.body?.string()}")
            } else {
                Log.e("PushNotification", "Push notification failed: ${response.message}")
            }
        } catch (e: Exception) {
            Log.e("PushNotification", "Error sending push notification", e)
        }
    }
}
