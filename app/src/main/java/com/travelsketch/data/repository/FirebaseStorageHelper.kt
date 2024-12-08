package com.travelsketch.data.repository

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await

object FirebaseStorageHelper {
    private val storage = Firebase.storage

    suspend fun getImageUrl(previewBoxId: String): String? {
        val extensions = listOf("png", "jpg")
        for (extension in extensions) {
            val path = "media/images/$previewBoxId.$extension"
            val ref = storage.reference.child(path)
            try {
                return ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                // 파일이 존재하지 않으면 예외가 발생하므로 무시하고 다음 확장자를 확인
            }
        }
        // 모든 확장자를 확인했으나 파일이 존재하지 않는 경우
        println("Error: No image found for $previewBoxId with .png or .jpg extensions.")
        return null
    }
}
