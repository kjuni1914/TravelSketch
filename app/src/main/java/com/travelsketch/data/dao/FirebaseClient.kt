package com.travelsketch.data.dao

import android.net.Uri
import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.CanvasData
import com.travelsketch.data.model.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FirebaseClient: DatabaseClient {
    override val databaseRef: DatabaseReference
        get() = FirebaseDatabase.getInstance().reference
    override val storageRef: StorageReference
        get() = FirebaseStorage.getInstance().reference

    override suspend fun createCanvasData(canvasId:String, canvasData: CanvasData) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("map").child(canvasId).setValue(canvasData)
                .addOnSuccessListener {
                    Log.d("CanvasActivity", "Create $canvasId")
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "Fail to add $canvasId")
                }
        }
    }

    override suspend fun readCanvasData(canvasId: String): CanvasData? {
        return suspendCoroutine { continuation ->
            databaseRef.child("map").child(canvasId).get()
                .addOnSuccessListener { dataSnapshot ->
                    try {
                        val canvasData = dataSnapshot.getValue<CanvasData>()
                            ?: throw Exception("$canvasId not found")
                        continuation.resume(canvasData)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "$canvasId is not existed")
                    continuation.resume(null)
                }
        }
    }

    override suspend fun updateCanvasData(canvasId: String, canvasData: CanvasData) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("map").child(canvasId).setValue(canvasData)
                .addOnSuccessListener {
                    Log.d("CanvasActivity", "Update $canvasId")
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "Fail to update $canvasId")
                }
        }
    }

    override suspend fun deleteCanvasData(canvasId: String) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("map").child(canvasId).removeValue()
            databaseRef.child("canvas").child(canvasId).removeValue()
        }
    }

    override suspend fun createBoxData(canvasId:String, boxId:String, boxData: BoxData) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("canvas").child(canvasId).child(boxId).setValue(boxData)
                .addOnSuccessListener {
                    Log.d("CanvasActivity", "Create $canvasId")
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "Fail to add $canvasId")
                }
        }
    }

    override suspend fun readBoxData(canvasId: String, boxId: String): BoxData? {
        return suspendCoroutine { continuation ->
            databaseRef.child(canvasId).child(boxId).get()
                .addOnSuccessListener { dataSnapshot ->
                    try {
                        val box = dataSnapshot.getValue<BoxData>()
                            ?: throw Exception("$boxId cannot found")
                        continuation.resume(box)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "$boxId is not existed")
                    continuation.resume(null)
                }
        }
    }

    override suspend fun updateBoxData(canvasId: String, boxId:String, boxData: BoxData) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("canvas").child(canvasId).child(boxId).setValue(boxData)
                .addOnSuccessListener {
                    Log.d("CanvasActivity", "Update $boxId")
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "Fail to update $boxId")
                }
        }
    }

    override suspend fun deleteBoxData(canvasId: String, boxId: String) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("canvas").child(canvasId).child(boxId).removeValue()
        }
    }

    override suspend fun createUserData(userId: String, userData: UserData) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("users").child(userId).setValue(userData)
                .addOnSuccessListener {
                    Log.d("CanvasActivity", "Create $userId")
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "Fail to add $userId")
                }
        }
    }

    override suspend fun readUserData(userId: String): UserData? {
        return suspendCoroutine { continuation ->
            databaseRef.child("users").child(userId).get()
                .addOnSuccessListener { dataSnapshot ->
                    try {
                        val user = dataSnapshot.getValue<UserData>()
                            ?: throw Exception("$userId cannot found")
                        continuation.resume(user)
                    } catch (e: Exception) {
                        continuation.resumeWithException(e)
                    }
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "$userId is not existed")
                    continuation.resume(null)
                }
        }
    }

    override suspend fun updateUserData(userId: String, userData: UserData) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("users").child(userId).setValue(userData)
                .addOnSuccessListener {
                    Log.d("CanvasActivity", "Update $userId")
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "Fail to update $userId")
                }
        }
    }

    override suspend fun deleteUserData(userId: String) {
        enterCoroutineScopeWithIODispatcher {
            databaseRef.child("users").child(userId).removeValue()
        }
    }

    override suspend fun createImageData(boxId: String, imageUri: Uri) {
        enterCoroutineScopeWithIODispatcher {
            storageRef.child("media").child("image").child("$boxId.jpg")
                .putFile(imageUri).addOnSuccessListener {
                    Log.d("CanvasActivity", "Create $boxId")
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "Fail to update $boxId image")
                }
        }
    }

    override suspend fun readImageData(boxId: String): Uri? {
        enterCoroutineScopeWithIODispatcher {
            storageRef.child("media").child("image").child("$boxId.jpg")
                .putFile(imageUri).addOnSuccessListener {
                    Log.d("CanvasActivity", "Create $boxId")
                }.addOnFailureListener {
                    Log.d("CanvasActivity", "Fail to update $boxId image")
                }
        }
    }

    override suspend fun updateImageData(imageData: ImageData) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteImageData(imageId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun createRecordData(recordData: RecordData) {
        TODO("Not yet implemented")
    }

    override suspend fun readRecordData(recordId: String): RecordData {
        TODO("Not yet implemented")
    }

    override suspend fun updateRecordData(recordData: RecordData) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteRecordData(recordId: String) {
        TODO("Not yet implemented")
    }

    override suspend fun createVideoData(videoData: VideoData) {
        TODO("Not yet implemented")
    }

    override suspend fun readVideoData(videoId: String): VideoData {
        TODO("Not yet implemented")
    }

    override suspend fun updateVideoData(videoData: VideoData) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteVideoData(videoId: String) {
        TODO("Not yet implemented")
    }

    private fun enterCoroutineScopeWithIODispatcher(
        execute: () -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            execute()
        }
    }
}