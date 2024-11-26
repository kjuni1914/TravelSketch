package com.travelsketch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.travelsketch.data.repository.FirebaseMapRepository
import com.travelsketch.data.model.MapData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val repository = FirebaseMapRepository()

    private val _canvasData = MutableStateFlow<MapData?>(null)
    val canvasData: StateFlow<MapData?> = _canvasData

    // map에 canvas 표시
    private val _canvasDataList = MutableStateFlow<List<MapData>>(emptyList())
    val canvasDataList: StateFlow<List<MapData>> = _canvasDataList

    private val _initialPosition = MutableStateFlow(LatLng(37.7749, -122.4194)) // Default 위치
    val initialPosition: StateFlow<LatLng> = _initialPosition

    suspend fun getNextCanvasId(): String {
        val canvasList = repository.readAllMapCanvasData() // 모든 Canvas 데이터를 읽음
        val nextCanvasNumber = canvasList.size + 1 // 기존 Canvas 개수 + 1
        return "canvas_$nextCanvasNumber" // 다음 canvas_id 반환
    }

    // Firebase에서 Canvas 데이터 가져오기
    fun readMapCanvasData(canvasId: String) {
        viewModelScope.launch {
            _canvasData.value = repository.readMapCanvasData(canvasId)
        }
    }

    // Firebase에 Map 데이터 생성
    fun createMapCanvasData(
        canvasId: String,
        avgGpsLatitude: Double,
        avgGpsLongitude: Double,
        title : String
    ) {
        viewModelScope.launch {
            repository.createMapCanvasData(canvasId, avgGpsLatitude, avgGpsLongitude,title)
        }
    }

    fun updateInitialPosition(latLng: LatLng) {
        _initialPosition.value = latLng
    }

    // 모든 캔버스 데이터를 Firebase에서 가져오기
    fun fetchAllCanvasData() {
        viewModelScope.launch {
            val canvasList = repository.readAllMapCanvasData() // Firebase에서 읽기
            _canvasDataList.value = canvasList // 상태 업데이트
        }
    }
}
