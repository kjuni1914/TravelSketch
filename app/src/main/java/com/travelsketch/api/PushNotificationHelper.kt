import android.util.Log
import com.google.auth.oauth2.GoogleCredentials
import com.travelsketch.GlobalApplication
import com.travelsketch.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object PushNotificationHelper {
    private const val PROJECT_ID = "travelsketch-a4f20"
    private const val BASE_URL = "https://fcm.googleapis.com/v1/projects/$PROJECT_ID/messages:send"

    suspend fun sendPushNotification(to: String, title: String, body: String) {
        withContext(Dispatchers.IO) {
            try {
                val accessToken = getAccessToken()
                val client = OkHttpClient()

                val message = JSONObject().apply {
                    put("message", JSONObject().apply {
                        put("token", to)
                        put("notification", JSONObject().apply {
                            put("title", title)
                            put("body", body)
                        })
                        put("android", JSONObject().apply {
                            put("priority", "HIGH")
                        })
                    })
                }

                val request = Request.Builder()
                    .url(BASE_URL)
                    .addHeader("Authorization", "Bearer $accessToken")
                    .addHeader("Content-Type", "application/json")
                    .post(message.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("PushNotification", "Failed to send notification", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            if (!response.isSuccessful) {
                                Log.e("PushNotification", "Error response: ${response.code}")
                                Log.e("PushNotification", "Error body: ${response.body?.string()}")
                            } else {
                                Log.d("PushNotification", "Successfully sent notification")
                                Log.d("PushNotification", "Response: ${response.body?.string()}")
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("PushNotification", "Error sending push notification", e)
            }
        }
    }

    private fun getAccessToken(): String {
        val context = GlobalApplication.getContext()
        val stream = context.resources.openRawResource(R.raw.service_account)

        val credentials = GoogleCredentials.fromStream(stream)
            .createScoped(listOf("https://www.googleapis.com/auth/firebase.messaging"))

        credentials.refresh()
        return credentials.accessToken.tokenValue
    }

}

