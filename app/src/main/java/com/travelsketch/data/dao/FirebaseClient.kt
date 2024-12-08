package com.travelsketch.data.dao

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.CanvasData
import com.travelsketch.data.model.UserData
import kotlinx.coroutines.tasks.await

object FirebaseClient: DatabaseClient {
    override val databaseRef: DatabaseReference
        get() = FirebaseDatabase.getInstance().reference
    override val storageRef: StorageReference
        get() = FirebaseStorage.getInstance().reference

    override suspend fun writeCanvasData(
        canvasId: String,
        canvasData: CanvasData
    ): Boolean {
        return try {
            databaseRef.child("map").child(canvasId).setValue(canvasData).await()
            true
        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
            false
        }
    }

    override suspend fun readCanvasData(canvasId: String): CanvasData? {
        return try {
            val snapshot = databaseRef.child("map").child(canvasId).get().await()
            if (snapshot.exists())
                snapshot.getValue(CanvasData::class.java)
            else
                null
        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
            null
        }
    }

    override suspend fun readAllCanvasData(): List<CanvasData>? {
        return try {
            val snapshot = databaseRef.child("map").get().await()
            val canvasDataList = mutableListOf<CanvasData>()

            if (snapshot.exists()) {
                snapshot.children.forEach { child ->
                    val data = child.getValue(CanvasData::class.java)
                    if (data != null)
                        canvasDataList.add(data)
                }
            }

            canvasDataList
        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
            null
        }
    }

    suspend fun readUserCanvasData(userId: String): List<CanvasData>? {
        return try {
            val snapshot = databaseRef.child("users").child(userId)
                .child("canvas_ids").get().await()
            val canvasDataList = mutableListOf<CanvasData>()

            if (snapshot.exists()) {
                snapshot.children.forEach { child ->
                    val canvasId = child.value as? String
                    if (canvasId != null) {
                        readCanvasData(canvasId)?.let {
                            canvasDataList.add(it)
                        }
                    }
                }
            }

            canvasDataList
        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
            null
        }
    }

    override suspend fun deleteCanvasData(canvasId: String): Boolean {
        return try {
            databaseRef.child("map").child(canvasId).removeValue().await()
            true
        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
            false
        }
    }

    override suspend fun writeBoxData(
        canvasId: String,
        boxId: String,
        boxData: BoxData
    ): Boolean {
        return try {
            Log.d("FirebaseClient", "Writing box data - Canvas: $canvasId, Box: $boxId")
            Log.d("FirebaseClient", "Box data: $boxData")

            val reference = databaseRef.child("canvas").child(canvasId).child(boxData.id)
            Log.d("FirebaseClient", "Writing to path: ${reference}")  // toString() 없이도 작동합니다

            reference.setValue(boxData).await()
            Log.d("FirebaseClient", "Successfully wrote box data")
            true
        } catch (e: Exception) {
            Log.e("FirebaseClient", "Error writing box data", e)
            e.printStackTrace()
            false
        }
    }

    override suspend fun readBoxData(canvasId: String, boxId: String): BoxData? {
        return try {
            val snapshot = databaseRef.child("canvas").child(canvasId).child(boxId).get().await()

            if (snapshot.exists()) {
                val boxData = snapshot.getValue(BoxData::class.java)
                // ID 필드 설정
                boxData?.id = boxId
                boxData
            } else {
                null
            }
        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
            null
        }
    }

    override suspend fun readAllBoxData(canvasId: String): List<BoxData>? {
        return try {
            Log.d("FirebaseClient", "Attempting to read boxes for canvas: $canvasId")
            val reference = databaseRef.child("canvas").child(canvasId)
            Log.d("FirebaseClient", "Database reference: $reference")

            val snapshot = reference.get().await()
            Log.d("FirebaseClient", "Snapshot exists: ${snapshot.exists()}")
            Log.d("FirebaseClient", "Snapshot children count: ${snapshot.childrenCount}")

            val boxDataList = mutableListOf<BoxData>()

            if (snapshot.exists()) {
                snapshot.children.forEach { child ->
                    Log.d("FirebaseClient", "Processing child key: ${child.key}")
                    // title 필드는 무시하고 BoxData만 처리
                    if (child.key != "title") {
                        try {
                            val boxData = child.getValue(BoxData::class.java)
                            if (boxData != null) {
                                boxData.id = child.key ?: boxData.id
                                boxDataList.add(boxData)
                                Log.d("FirebaseClient", "Added box: $boxData")
                            }
                        } catch (e: Exception) {
                            Log.e("FirebaseClient", "Error parsing box data for key: ${child.key}", e)
                        }
                    }
                }
            }

            Log.d("FirebaseClient", "Finished reading. Total boxes: ${boxDataList.size}")
            boxDataList
        } catch (e: Exception) {
            Log.e("FirebaseClient", "Error reading box data", e)
            e.printStackTrace()
            null
        }
    }

    override suspend fun deleteBoxData(canvasId: String, boxId: String): Boolean {
        return try {
            databaseRef.child("canvas").child(canvasId).child(boxId).removeValue().await()
            true
        } catch (e: Exception) {
            Log.d("ITM", "$e")
            false
        }
    }

    override suspend fun writeUserData(userId: String, userData: UserData): Boolean {
        return try {
            databaseRef.child("users").child(userId).setValue(userData).await()
            true
        } catch (e: Exception) {
            Log.d("ITM", "$e")
            false
        }
    }

    override suspend fun readUserData(userId: String): UserData? {
        return try {
            val snapshot = databaseRef.child("users").child(userId).get().await()

            if (snapshot.exists())
                snapshot.getValue<UserData>()
            else
                null
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun deleteUserData(userId: String): Boolean {
        return try {
            databaseRef.child("users").child(userId).removeValue().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}