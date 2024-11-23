package com.travelsketch.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.travelsketch.data.model.MapData
import kotlinx.coroutines.tasks.await

class FirebaseMapRepository {
    private val database = FirebaseDatabase.getInstance().reference

    // Canvas 데이터 읽기
    suspend fun readMapCanvasData(canvasId: String): MapData? {
        return try {
            val snapshot = database.child("map").child(canvasId).get().await()
            parseMapCanvasData(snapshot)
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch data for $canvasId", e)
            null
        }
    }

    // Canvas 데이터 생성
    suspend fun createMapCanvasData(
        canvasId: String,
        avgGpsLatitude: Double,
        avgGpsLongitude: Double
    ) {
        val data = mapOf(
            "avg_gps_latitude" to avgGpsLatitude,
            "avg_gps_longitude" to avgGpsLongitude,
            "is_visible" to false,
            "preview_box_id" to "box_0",
            "range" to 0
        )

        try {
            database.child("map").child(canvasId).setValue(data).await()
            Log.d("FirebaseRepository", "Data successfully created for $canvasId")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to create data for $canvasId", e)
        }
    }

    // Firebase Snapshot을 CanvasData로 변환
    private fun parseMapCanvasData(snapshot: DataSnapshot): MapData {
        val previewBoxId = snapshot.child("preview_box_id").getValue(String::class.java) ?: ""
        val avgGpsLatitude = snapshot.child("avg_gps_latitude").getValue(Double::class.java) ?: 0.0
        val avgGpsLongitude = snapshot.child("avg_gps_longitude").getValue(Double::class.java) ?: 0.0

        Log.d(
            "FirebaseRepository",
            "Parsed data - previewBoxId: $previewBoxId, avgLatitude: $avgGpsLatitude, avgLongitude: $avgGpsLongitude"
        )
        return MapData(previewBoxId, avgGpsLatitude, avgGpsLongitude)
    }
}
