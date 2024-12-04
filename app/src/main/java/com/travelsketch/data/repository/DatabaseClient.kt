package com.travelsketch.data.repository

import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import com.travelsketch.data.model.CanvasData
import com.travelsketch.data.model.UserData
import com.travelsketch.data.model.ViewType

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
    suspend fun readCanvas(canvasId: String): CanvasData?
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
        time: Long?,
        type: String,
        width: Int
    )
    suspend fun readBox(canvasId:String, boxId: String): BoxData?
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
        time: Long?,
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
    suspend fun readUser(userId: String): UserData?
    suspend fun updateUser(
        userId: String,
        canvasIds: String,
        friendIds: String,
        phoneNumber: String,
        email: String
    )
    suspend fun deleteUser(userId: String)


    suspend fun getViewType(userId: String): ViewType
    suspend fun setViewType(userId: String, viewType: ViewType)
}