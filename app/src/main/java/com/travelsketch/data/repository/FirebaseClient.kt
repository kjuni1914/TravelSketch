package com.travelsketch.data.repository

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.travelsketch.data.model.Box
import com.travelsketch.data.model.BoxType
import com.travelsketch.data.model.Canvas
import com.travelsketch.data.model.User
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object FirebaseClient: DatabaseClient {
    override val database: DatabaseReference
        get() = FirebaseDatabase.getInstance().reference

    override suspend fun createCanvas(
        canvasId: String,
        avgLatitude: Double?,
        avgLongitude: Double?,
        isVisible: Boolean,
        previewBoxId: String?,
        range: Double
    ) {
        val canvas = Canvas(
            avgLongitude = avgLongitude,
            avgLatitude = avgLatitude,
            isVisible = isVisible,
            previewBoxId = previewBoxId,
            range = range
        )
        database.child("map").child(canvasId).setValue(canvas).await()
    }

    override suspend fun readCanvas(canvasId: String): Canvas? {
        return suspendCoroutine { continuation ->
            database.child("map").child(canvasId).get().addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val canvas = dataSnapshot.getValue(Canvas::class.java)
                    continuation.resume(canvas)
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }

    override suspend fun updateCanvas(
        canvasId: String,
        avgLatitude: Double?,
        avgLongitude: Double?,
        isVisible: Boolean,
        previewBoxId: String?,
        range: Double
    ) {
        val updatedCanvas = Canvas(
            avgLatitude = avgLatitude,
            avgLongitude = avgLongitude,
            isVisible = isVisible,
            previewBoxId = previewBoxId,
            range = range
        )
        database.child("map").child(canvasId).setValue(updatedCanvas).await()
    }

    override suspend fun deleteCanvas(canvasId: String) {
        database.child("map").child(canvasId).removeValue().await()
    }

    override suspend fun createBox(
        canvasId: String,
        boxId: String,
        boxX: Int,
        boxY: Int,
        boxZ: Int,
        data: String,
        degree: Int,
        height: Int,
        latitude: Double?,
        longitude: Double?,
        time: Int?,
        type: String,
        width: Int
    ) {
        val box = Box(
            boxX = boxX,
            boxY = boxY,
            boxZ = boxZ,
            data = data,
            degree = degree,
            height = height,
            width = width,
            latitude = latitude,
            longitude = longitude,
            time = time)
        database.child("canvas").child(canvasId).child(boxId).setValue(box).await()
    }

    override suspend fun readBox(canvasId:String, boxId: String): Box? {
        return suspendCoroutine { continuation ->
            database.child("canvas").child(canvasId).child(boxId).get()
                .addOnSuccessListener { dataSnapshot ->
                if (dataSnapshot.exists()) {
                    val boxData = dataSnapshot.getValue(Box::class.java)
                    continuation.resume(boxData)
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }

    override suspend fun updateBox(
        canvasId: String,
        boxId: String,
        boxX: Int,
        boxY: Int,
        boxZ: Int,
        data: String,
        degree: Int,
        height: Int,
        latitude: Double?,
        longitude: Double?,
        time: Int?,
        type: BoxType,
        width: Int
    ) {
        val updatedBox = Box(
            boxX = boxX,
            boxY = boxY,
            boxZ = boxZ,
            data = data,
            degree = degree,
            height = height,
            width = width,
            latitude = latitude,
            longitude = longitude,
            time = time)
        database.child("canvas").child(canvasId).child(boxId).setValue(updatedBox).await()
    }

    override suspend fun deleteBox(canvasId:String, boxId: String) {
        database.child("canvas").child(canvasId).child(boxId).removeValue().await()
    }

    override suspend fun createUser(
        userId: String,
        canvasIds: String,
        friendIds: String,
        phoneNumber: String,
        viewType: Boolean
    ) {
        val user = User(
            phoneNumber = phoneNumber,
            canvasIds = canvasIds,
            friendIds = friendIds,
            viewType = viewType
        )
        database.child("users").child(userId).setValue(user).await()
    }

    override suspend fun readUser(userId: String): User? {
        return suspendCoroutine { continuation ->
            database.child("users").child(userId).get()
                .addOnSuccessListener { dataSnapshot ->
                    if (dataSnapshot.exists()) {
                        val user = dataSnapshot.getValue(User::class.java)
                        continuation.resume(user)
                    } else {
                        continuation.resume(null)
                    }
            }.addOnFailureListener { exception ->
                continuation.resumeWithException(exception)
            }
        }
    }

    override suspend fun updateUser(
        userId: String,
        canvasIds: String,
        friendIds: String,
        phoneNumber: String,
        viewType: Boolean
    ) {
        val updatedUser = User(
            phoneNumber = phoneNumber,
            canvasIds = canvasIds,
            friendIds = friendIds,
            viewType = viewType
        )
        database.child("users").child(userId).setValue(updatedUser).await()
    }

    override suspend fun deleteUser(userId: String) {
        database.child("users").child(userId).removeValue().await()
    }


}