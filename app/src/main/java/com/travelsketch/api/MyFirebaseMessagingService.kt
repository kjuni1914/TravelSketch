package com.travelsketch.api

import android.util.Log
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
        // Handle the incoming message if needed
        Log.d("FCM", "Message received: ${remoteMessage.notification?.body}")
    }

    private fun saveTokenToDatabase(token: String) {
        // 사용자 ID 가져오기
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("FCM", "User ID: $userId, Token: $token")

        if (userId == null) {
            Log.e("FCM", "User ID is null. Cannot save token.")
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
}
