package com.travelsketch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelsketch.data.repository.FirebaseMapRepository
import com.travelsketch.data.model.MapData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MapViewModel : ViewModel() {
    private val repository = FirebaseMapRepository()

    private val _canvasData = MutableStateFlow<MapData?>(null)
    val canvasData: StateFlow<MapData?> = _canvasData

    // Firebase에서 Canvas 데이터 가져오기
    fun fetchCanvasData(canvasId: String) {
        viewModelScope.launch {
            _canvasData.value = repository.fetchCanvasData(canvasId)
        }
    }

    // Firebase에 Map 데이터 생성
    fun createMapData(
        canvasId: String,
        avgGpsLatitude: Double,
        avgGpsLongitude: Double
    ) {
        viewModelScope.launch {
            repository.createCanvasData(canvasId, avgGpsLatitude, avgGpsLongitude)
        }
    }
}
