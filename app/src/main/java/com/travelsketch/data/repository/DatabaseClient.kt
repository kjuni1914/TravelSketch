package com.travelsketch.data.repository

import com.travelsketch.data.model.BoxType

interface DatabaseClient {
    val database: Any?
        get() = null

    suspend fun createCanvas(
        canvasId: String,
        avgGpsLatitude: Double?,
        avgGpsLongitude: Double?,
        isVisible: Boolean,
        previewBoxId: String?,
        range: Double
    )
    suspend fun readCanvas(canvasId: String)
    suspend fun updateCanvas(
        canvasId: String,
        avgGpsLatitude: Double?,
        avgGpsLongitude: Double?,
        isVisible: Boolean,
        previewBoxId: String?,
        range: Double
    )
    suspend fun deleteCanvas(canvasId: String)
    suspend fun createBox(
        boxId: String,
        boxX: Int,
        boxY: Int,
        boxZ: Int,
        data: String,
        degree: Int,
        height: Int,
        latitude: Double?,
        longitude: Double?,
        time: Double?,
        type: BoxType,
        width: Int
    )
    suspend fun readBox(boxId: String)
    suspend fun updateBox(
        boxId: String,
        boxX: Int,
        boxY: Int,
        boxZ: Int,
        data: String,
        degree: Int,
        height: Int,
        latitude: Double?,
        longitude: Double?,
        time: Double?,
        type: BoxType,
        width: Int
    )
    suspend fun deleteBox(boxId: String)
    suspend fun createUser(
        userId: String,
        canvasIds: String,
        friendIds: String,
        phoneNumber: String,
        viewType: Boolean
    )
    suspend fun readUser(userId: String)
    suspend fun updateUser(
        userId: String,
        canvasIds: String,
        friendIds: String,
        phoneNumber: String,
        viewType: Boolean
    )
    suspend fun deleteUser(userId: String)
}