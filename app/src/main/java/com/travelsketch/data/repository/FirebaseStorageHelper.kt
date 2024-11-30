package com.travelsketch.data.repository

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

object FirebaseStorageHelper {
    private val storage = Firebase.storage

    suspend fun getImageUrl(previewBoxId: String): String? {
        return try {
            val path = "media/images/$previewBoxId.png" // Storage 경로 생성
            val ref = storage.reference.child(path)
            ref.downloadUrl.await().toString() // URL 반환
        } catch (e: Exception) {
            println("Error fetching image URL for $previewBoxId: ${e.message}")
            null
        }
    }
}
