package com.travelsketch.data.dao

import com.google.firebase.database.DatabaseReference
import com.google.firebase.storage.StorageReference
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.CanvasData
import com.travelsketch.data.model.UserData

interface DatabaseClient {
    val databaseRef: DatabaseReference?
        get() = null
    val storageRef: StorageReference?
        get() = null

    suspend fun writeCanvasData(canvasId:String, canvasData: CanvasData): Boolean
    suspend fun readCanvasData(canvasId: String): CanvasData?
    suspend fun readAllCanvasData(): List<CanvasData>?
    suspend fun deleteCanvasData(canvasId: String): Boolean

    suspend fun writeBoxData(canvasId: String, boxId: String, boxData: BoxData): Boolean
    suspend fun readBoxData(canvasId:String, boxId: String): BoxData?
    suspend fun readAllBoxData(canvasId:String): List<BoxData>?
    suspend fun deleteBoxData(canvasId: String, boxId: String): Boolean

    suspend fun writeUserData(userId: String, userData: UserData): Boolean
    suspend fun readUserData(userId: String): UserData?
    suspend fun deleteUserData(userId: String): Boolean
}