package com.travelsketch.data.dao

import android.net.Uri
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.travelsketch.data.model.MapData
import kotlinx.coroutines.tasks.await
import java.io.File

class FirebaseRepository {
    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance()

    // 특정 Canvas 데이터 읽기
    suspend fun readMapCanvasData(canvasId: String): MapData? {
        return try {
            val snapshot = database.child("map").child(canvasId).get().await()
            if (snapshot.exists()) {
                parseMapCanvasData(snapshot, canvasId) // canvasId 포함
            } else {
                Log.w("FirebaseRepository", "No data found for canvasId: $canvasId")
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch data for $canvasId", e)
            null
        }
    }

    // 모든 Canvas 데이터 읽기
    suspend fun readAllMapCanvasData(): List<MapData> {
        return try {
            val snapshot = database.child("map").get().await()
            val canvasList = mutableListOf<MapData>()

            for (canvasSnapshot in snapshot.children) {
                val canvasId = canvasSnapshot.key ?: continue // 문서 이름(canvasId) 가져오기
                val mapData = parseMapCanvasData(canvasSnapshot, canvasId) // canvasId 포함
                canvasList.add(mapData)
            }

            canvasList
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch all map data", e)
            emptyList()
        }
    }

    // 사용자의 canvas_ids 읽기
    suspend fun readUserCanvasIds(userId: String): List<String> {
        return try {
            val snapshot = database.child("users").child(userId).child("canvas_ids").get().await()
            if (snapshot.exists()) {
                // 데이터를 반복문으로 수집하여 List<String>으로 변환
                snapshot.children.mapNotNull { it.getValue(String::class.java) }
            } else {
                Log.w("FirebaseRepository", "No canvas_ids found for userId: $userId")
                emptyList()
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch canvas_ids for userId: $userId", e)
            emptyList()
        }
    }


    // Canvas 데이터 생성
    suspend fun createMapCanvasData(
        canvasId: String,
        avgGpsLatitude: Double,
        avgGpsLongitude: Double,
        title: String
    ) {
        val data = mapOf(
            "avg_gps_latitude" to avgGpsLatitude,
            "avg_gps_longitude" to avgGpsLongitude,
            "is_visible" to false,
            "preview_box_id" to "image_1",
            "range" to 0.0,
            "title" to title
        )
        val canvasData = mapOf(
            "title" to title
        )

        try {
            database.child("map").child(canvasId).setValue(data).await()
            Log.d("FirebaseRepository", "Data successfully created for $canvasId")
            database.child("canvas").child(canvasId).setValue(canvasData).await()
            Log.d("FirebaseRepository", "Data successfully created in 'canvas' for $canvasId")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to create data for $canvasId", e)
        }
    }

    suspend fun addUserCanvasId(userId: String, canvasId: String) {
        try {
            val userCanvasRef = database.child("users").child(userId).child("canvas_ids")
            val currentCanvasIds = userCanvasRef.get().await().children.mapNotNull { it.getValue(String::class.java) }

            // 중복 방지: 이미 존재하지 않는 경우에만 추가
            if (!currentCanvasIds.contains(canvasId)) {
                val updatedCanvasIds = currentCanvasIds + canvasId
                userCanvasRef.setValue(updatedCanvasIds).await()
                Log.d("FirebaseRepository", "Canvas ID $canvasId added for user $userId")
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to add Canvas ID $canvasId for user $userId", e)
        }
    }

    suspend fun addFriendByEmail(currentUserId: String, friendEmail: String) {
        try {
            // 이메일로 친구의 userId 검색
            val usersSnapshot = database.child("users").get().await()
            var friendUserId: String? = null

            for (userSnapshot in usersSnapshot.children) {
                val email = userSnapshot.child("email").getValue(String::class.java)
                if (email == friendEmail) {
                    friendUserId = userSnapshot.key
                    break
                }
            }

            if (friendUserId == null) {
                Log.w("FirebaseRepository", "User with email $friendEmail not found")
                return
            }

            // 현재 사용자의 friends_ids 읽기
            val currentUserFriendsRef = database.child("users").child(currentUserId).child("friends_ids")
            val currentUserFriends = currentUserFriendsRef.get().await().children.mapNotNull { it.getValue(String::class.java) }

            // 이미 친구인지 확인
            if (currentUserFriends.contains(friendUserId)) {
                Log.w("FirebaseRepository", "User $friendUserId is already a friend of $currentUserId")
                return
            }

            // 친구 추가 로직
            val updatedCurrentUserFriends = currentUserFriends + friendUserId
            currentUserFriendsRef.setValue(updatedCurrentUserFriends).await()
            Log.d("FirebaseRepository", "Added $friendUserId to $currentUserId's friends list")

            // 친구의 friends_ids 업데이트
            val friendUserFriendsRef = database.child("users").child(friendUserId).child("friends_ids")
            val friendUserFriends = friendUserFriendsRef.get().await().children.mapNotNull { it.getValue(String::class.java) }

            val updatedFriendUserFriends = friendUserFriends + currentUserId
            friendUserFriendsRef.setValue(updatedFriendUserFriends).await()
            Log.d("FirebaseRepository", "Added $currentUserId to $friendUserId's friends list")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to add friend by email $friendEmail", e)
        }
    }
    // 친구의 canvas ids 가져오기
    suspend fun readUserFriendsIds(userId: String): List<String> {
        return try {
            val snapshot = database.child("users").child(userId).child("friends_ids").get().await()
            snapshot.children.mapNotNull { it.getValue(String::class.java) }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch friends_ids for userId: $userId", e)
            emptyList()
        }
    }

    suspend fun updateCanvasVisibility(canvasId: String, isVisible: Boolean) {
        try {
            database.child("map").child(canvasId).child("is_visible").setValue(isVisible).await()
            Log.d("FirebaseRepository", "Canvas $canvasId visibility updated to $isVisible")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to update visibility for canvas $canvasId", e)
        }
    }

    suspend fun updateCanvasTitle(canvasId: String, title: String) {
        try {
            database.child("map").child(canvasId).child("title").setValue(title).await()
            database.child("canvas").child(canvasId).child("title").setValue(title).await()
            Log.d("FirebaseRepository", "Canvas $canvasId visibility updated to $title")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to update visibility for canvas $canvasId", e)
        }
    }

    suspend fun updatePreviewImage(canvasId: String, imageName: String) {
        val databaseRef = FirebaseDatabase.getInstance().reference.child("map/$canvasId/preview_box_id")
        databaseRef.setValue(imageName).await()
    }

    suspend fun updateFcmToken(userId: String, token: String) {
        database.child("users").child(userId).child("fcmToken").setValue(token).await()
    }

    suspend fun getFriendsFcmTokens(userId: String): List<String> {
        val tokens = mutableListOf<String>()
        try {
            // Get the user's friends_ids
            val friendsSnapshot = database.child("users").child(userId).child("friends_ids").get().await()
            val friendsIds = friendsSnapshot.children.mapNotNull { it.getValue(String::class.java) }

            // Fetch each friend's FCM token
            for (friendId in friendsIds) {
                val tokenSnapshot = database.child("users").child(friendId).child("fcmToken").get().await()
                val token = tokenSnapshot.getValue(String::class.java)
                if (token != null) {
                    tokens.add(token)
                }
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch friends' FCM tokens", e)
        }
        return tokens
    }


    suspend fun uploadFile(fileUri: Uri, path: String): String? {
        return try {
            val storageRef = storage.reference.child(path)
            storageRef.putFile(fileUri).await()
            storageRef.downloadUrl.await().toString() // 업로드 후 다운로드 URL 반환
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "File upload failed", e)
            null
        }
    }

    suspend fun deleteFile(path: String): Boolean {
        return try {
            val storageRef = storage.reference.child(path)
            storageRef.delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteCanvas(canvasId: String) {
        try {
            // Realtime Database에서 캔버스 삭제
            val databaseRef = FirebaseDatabase.getInstance().reference
            databaseRef.child("map").child(canvasId).removeValue().await()
            databaseRef.child("canvas").child(canvasId).removeValue().await()
            // 관련 Storage 데이터 삭제 (예: preview_box_id 이미지)
            val storageRef = FirebaseStorage.getInstance().reference.child("media/images/$canvasId")
            storageRef.delete().await()

            Log.d("FirebaseRepository", "Successfully deleted canvas and related data for $canvasId")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to delete canvas $canvasId", e)
        }
    }

    // Firebase Snapshot을 CanvasData로 변환
    private fun parseMapCanvasData(snapshot: DataSnapshot, canvasId: String): MapData {
        val previewBoxId = snapshot.child("preview_box_id").getValue(String::class.java) ?: ""
        val avgGpsLatitude = snapshot.child("avg_gps_latitude").getValue(Double::class.java) ?: 0.0
        val avgGpsLongitude = snapshot.child("avg_gps_longitude").getValue(Double::class.java) ?: 0.0
        val mapCanvasTitle = snapshot.child("title").getValue(String::class.java) ?: ""
        val isVisible = snapshot.child("is_visible").getValue(Boolean::class.java) ?: false
        val range = snapshot.child("range").getValue(Double::class.java) ?: 0.0

        return MapData(
            canvasId = canvasId, // canvasId 추가
            avg_gps_latitude = avgGpsLatitude,
            avg_gps_longitude = avgGpsLongitude,
            is_visible = isVisible,
            preview_box_id = previewBoxId,
            range = range,
            title = mapCanvasTitle
        )
    }
    fun uploadPhotoToFirebase(filePath: String, onSuccess: (String) -> Unit) {
        val file = Uri.fromFile(File(filePath))
        val storageRef = FirebaseStorage.getInstance().reference.child("images/${file.lastPathSegment}")

        storageRef.putFile(file)
            .addOnSuccessListener { onSuccess(storageRef.path) }
            .addOnFailureListener { Log.e("Firebase Storage", "Upload failed: ${it.message}") }
    }
    fun saveMetadataToFirestore(
        latitude: Double?,
        longitude: Double?,
        timestamp: String?,
        storagePath: String,
        onComplete: () -> Unit
    ) {
        val metadata = hashMapOf(
            "latitude" to latitude,
            "longitude" to longitude,
            "timestamp" to timestamp,
            "imagePath" to storagePath
        )

        FirebaseFirestore.getInstance()
            .collection("imageMetadata")
            .add(metadata)
            .addOnSuccessListener { onComplete() }
            .addOnFailureListener { Log.e("Firestore", "Failed to save metadata: ${it.message}") }
    }

}
