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

    suspend fun createCanvasData(canvasId:String, canvasData: CanvasData)
    suspend fun readCanvasData(canvasId: String): CanvasData?
    suspend fun updateCanvasData(canvasId:String, canvasData: CanvasData)
    suspend fun deleteCanvasData(canvasId: String)
    suspend fun createBoxData(canvasId: String, boxId: String, boxData: BoxData)
    suspend fun readBoxData(canvasId:String, boxId: String): BoxData?
    suspend fun updateBoxData(canvasId: String, boxId: String, boxData: BoxData)
    suspend fun deleteBoxData(canvasId: String, boxId: String)
    suspend fun createUserData(userId: String, userData: UserData)
    suspend fun readUserData(userId: String): UserData?
    suspend fun updateUserData(userId: String, userData: UserData)
    suspend fun deleteUserData(userId: String)
}