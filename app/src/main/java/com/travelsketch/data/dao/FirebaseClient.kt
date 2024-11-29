package com.travelsketch.data.dao

import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.snap
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
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

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
            databaseRef.child("canvas").child(canvasId).child(boxId).setValue(boxData).await()
            true
        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
            false
        }
    }

    override suspend fun readBoxData(canvasId: String, boxId: String): BoxData? {
        return try {
            val snapshot = databaseRef.child("canvas").child(canvasId).child(boxId).get().await()

            if (snapshot.exists())
                snapshot.getValue(BoxData::class.java)
            else
                null

        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
            null
        }
    }

    override suspend fun readAllBoxData(canvasId: String): List<BoxData>? {
        return try {
            val snapshot = databaseRef.child("canvas").child(canvasId).get().await()
            val canvasDataList = mutableListOf<BoxData>()

            if (snapshot.exists()) {
                snapshot.children.forEach { child ->
                    if (child.key?.startsWith("box_") == true) {
                        val boxData = child.getValue(BoxData::class.java)
                        if (boxData != null)
                            canvasDataList.add(boxData)
                    }
                }
            }

            canvasDataList

        } catch (e: Exception) {
            Log.d("ITM", "DAO Error: $e")
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