package com.travelsketch.data.dao

import android.util.Log
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.getValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
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
            Log.d("asdfasdfasdf", "Writing box data - Canvas: $canvasId, Box: ${boxData.id}")
            Log.d("asdfasdfasdf", "Box data to write: $boxData")

            val reference = databaseRef.child("canvas").child(canvasId).child(boxData.id)
            reference.setValue(boxData).await()
            Log.d("asdfasdfasdf", "Successfully wrote box data")
            true
        } catch (e: Exception) {
            Log.e("asdfasdfasdf", "Error writing box data", e)
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
            Log.d("asdfasdfasdf", "Starting readAllBoxData for canvas: $canvasId")
            val snapshot = databaseRef.child("canvas").child(canvasId).get().await()
            Log.d("asdfasdfasdf", "Received snapshot exists: ${snapshot.exists()}")

            val boxDataList = mutableListOf<BoxData>()

            if (snapshot.exists()) {
                snapshot.children.forEach { child ->
                    if (child.key != "title") {
                        try {
                            val boxData = child.getValue(BoxData::class.java)
                            boxData?.let {
                                it.id = child.key ?: it.id
                                // data가 "uploading"이 아닌 경우에만 추가
                                if (it.data != "uploading") {
                                    boxDataList.add(it)
                                    Log.d("asdfasdfasdf", "Added box: $it")
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("asdfasdfasdf", "Error processing box data for key: ${child.key}", e)
                        }
                    }
                }
            }

            Log.d("asdfasdfasdf", "Returning ${boxDataList.size} boxes")
            boxDataList

        } catch (e: Exception) {
            Log.e("asdfasdfasdf", "Error in readAllBoxData", e)
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