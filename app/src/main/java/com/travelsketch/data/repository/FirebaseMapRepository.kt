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


            if (snapshot.exists()) {
                parseMapCanvasData(snapshot) // 데이터 파싱
            } else {
                Log.w("FirebaseRepository", "No data found for canvasId: $canvasId")
                null
            }
        } catch (e: Exception) {
            // 에러 발생 로그
            Log.e("FirebaseRepository", "Failed to fetch data for $canvasId", e)
            null
        }
    }

    suspend fun readAllMapCanvasData(): List<MapData> {
        return try {
            val snapshot = database.child("map").get().await() // "map" 하위 모든 데이터 가져오기
            val canvasList = mutableListOf<MapData>()

            for (canvasSnapshot in snapshot.children) {
                val mapData = parseMapCanvasData(canvasSnapshot) // 개별 Canvas 데이터 파싱
                canvasList.add(mapData)
            }

            canvasList
        } catch (e: Exception) {
            Log.e("FirebaseRepository", "Failed to fetch all map data", e)
            emptyList() // 에러 시 빈 리스트 반환
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
            "range" to 0,
            "title" to "여행"
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
        val mapCanvasTitle = snapshot.child("title").getValue(String::class.java) ?: ""

        Log.d(
            "FirebaseRepository",
            "Parsed data - previewBoxId: $previewBoxId, avgLatitude: $avgGpsLatitude, avgLongitude: $avgGpsLongitude, mapCanvasTitle: $mapCanvasTitle"
        )
        return MapData(previewBoxId, avgGpsLatitude, avgGpsLongitude,mapCanvasTitle)
    }
}
