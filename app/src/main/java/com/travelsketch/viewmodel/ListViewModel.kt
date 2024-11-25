package com.travelsketch.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.travelsketch.data.repository.FirebaseMapRepository
import com.travelsketch.data.model.MapData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ListViewModel : ViewModel() {
    private val repository = FirebaseMapRepository()

    private val _canvasData = MutableStateFlow<MapData?>(null)
    val canvasData: StateFlow<MapData?> = _canvasData



    // 모두 불러오기
    private val _canvasList = MutableStateFlow<List<MapData>>(emptyList())
    val canvasList: StateFlow<List<MapData>> = _canvasList

    // Firebase에서 Canvas 데이터 가져오기
    fun readMapCanvasData(canvasId: String) {
        viewModelScope.launch {
            _canvasData.value = repository.readMapCanvasData(canvasId)
        }
    }

    fun readAllMapCanvasData() {
        viewModelScope.launch {
            _canvasList.value = repository.readAllMapCanvasData() // Firebase에서 전체 데이터 가져오기
        }
    }



    fun updateInitialPosition(latLng: LatLng) {
        _canvasData.value = MapData(
            avg_gps_latitude = latLng.latitude,
            avg_gps_longitude = latLng.longitude
        )
    }

}
