package com.travelsketch.api

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.database.FirebaseDatabase

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        saveTokenToDatabase(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        remoteMessage.notification?.let {
            val title = it.title ?: "New Message"
            val body = it.body ?: "You have a new notification."
            sendNotification(title, body)
        }
    }

    private fun saveTokenToDatabase(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("FCM", "User ID: $userId, Token: $token")

        if (userId == null) {
            Log.w("FCM", "User is not logged in. Token will not be saved.")
            return
        }

        val database = FirebaseDatabase.getInstance().reference
        database.child("users").child(userId).child("fcmToken").setValue(token)
            .addOnSuccessListener {
                Log.d("FCM", "Token successfully saved to database")
            }
            .addOnFailureListener {
                Log.e("FCM", "Failed to save token to database", it)
            }
    }

    private fun sendNotification(title: String, body: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notificationChannelId = "FCM_CHANNEL_ID"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationChannelId,
                "FCM Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(0, notification)
    }
}
