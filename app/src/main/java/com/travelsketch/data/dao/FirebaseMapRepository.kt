package com.travelsketch.data.dao

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.travelsketch.data.model.MapData
import kotlinx.coroutines.tasks.await

class FirebaseMapRepository {
    private val database = FirebaseDatabase.getInstance().reference

    // 특정 Canvas 데이터 읽기
    suspend fun readMapCanvasData(canvasId: String): MapData? {
        return try {
            val snapshot = database.child("map").child(canvasId).get().await()
            if (snapshot.exists()) {
                parseMapCanvasData(snapshot, canvasId) // canvasId 포함
            } else {
                Log.w("FirebaseRepository", "No data found for canvasId: $canvasId")
                null
            }
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch data for $canvasId", e)
            null
        }
    }

    // 모든 Canvas 데이터 읽기
    suspend fun readAllMapCanvasData(): List<MapData> {
        return try {
            val snapshot = database.child("map").get().await()
            val canvasList = mutableListOf<MapData>()

            for (canvasSnapshot in snapshot.children) {
                val canvasId = canvasSnapshot.key ?: continue // 문서 이름(canvasId) 가져오기
                val mapData = parseMapCanvasData(canvasSnapshot, canvasId) // canvasId 포함
                canvasList.add(mapData)
            }

            canvasList
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch all map data", e)
            emptyList()
        }
    }

    // Canvas 데이터 생성
    suspend fun createMapCanvasData(
        canvasId: String,
        avgGpsLatitude: Double,
        avgGpsLongitude: Double,
        title: String
    ) {
        val data = mapOf(
            "avg_gps_latitude" to avgGpsLatitude,
            "avg_gps_longitude" to avgGpsLongitude,
            "is_visible" to false,
            "preview_box_id" to "box_0",
            "range" to 0.0,
            "title" to title
        )

        try {
            database.child("map").child(canvasId).setValue(data).await()
            Log.d("FirebaseRepository", "Data successfully created for $canvasId")
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to create data for $canvasId", e)
        }
    }

    // Firebase Snapshot을 CanvasData로 변환
    private fun parseMapCanvasData(snapshot: DataSnapshot, canvasId: String): MapData {
        val previewBoxId = snapshot.child("preview_box_id").getValue(String::class.java) ?: ""
        val avgGpsLatitude = snapshot.child("avg_gps_latitude").getValue(Double::class.java) ?: 0.0
        val avgGpsLongitude = snapshot.child("avg_gps_longitude").getValue(Double::class.java) ?: 0.0
        val mapCanvasTitle = snapshot.child("title").getValue(String::class.java) ?: ""
        val isVisible = snapshot.child("is_visible").getValue(Boolean::class.java) ?: false
        val range = snapshot.child("range").getValue(Double::class.java) ?: 0.0
        Log.d(
            "FirebaseRepository",
            "Parsed data - canvasId: $canvasId, previewBoxId: $previewBoxId, avgLatitude: $avgGpsLatitude, avgLongitude: $avgGpsLongitude, title: $mapCanvasTitle"
        )
        return MapData(
            canvasId = canvasId, // canvasId 추가
            avg_gps_latitude = avgGpsLatitude,
            avg_gps_longitude = avgGpsLongitude,
            is_visible = isVisible,
            preview_box_id = previewBoxId,
            range = range,
            title = mapCanvasTitle
        )
    }
}
