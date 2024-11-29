package com.travelsketch.data.repository

import com.travelsketch.data.model.Box
import com.travelsketch.data.model.BoxType
import com.travelsketch.data.model.Canvas
import com.travelsketch.data.model.User

interface DatabaseClient {
    val database: Any?
        get() = null

    suspend fun createCanvas(
        canvasId: String,
        avgLatitude: Double?,
        avgLongitude: Double?,
        isVisible: Boolean,
        previewBoxId: String?,
        range: Double
    )
    suspend fun readCanvas(canvasId: String): Canvas?
    suspend fun updateCanvas(
        canvasId: String,
        avgLatitude: Double?,
        avgLongitude: Double?,
        isVisible: Boolean,
        previewBoxId: String?,
        range: Double
    )
    suspend fun deleteCanvas(canvasId: String)
    suspend fun createBox(
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
    )
    suspend fun readBox(canvasId:String, boxId: String): Box?
    suspend fun updateBox(
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
    )
    suspend fun deleteBox(canvasId:String, boxId: String)
    suspend fun createUser(
        userId: String,
        canvasIds: String,
        friendIds: String,
        phoneNumber: String,
        email: String
    )
    suspend fun readUser(userId: String): User?
    suspend fun updateUser(
        userId: String,
        canvasIds: String,
        friendIds: String,
        phoneNumber: String,
        email: String
    )
    suspend fun deleteUser(userId: String)
}