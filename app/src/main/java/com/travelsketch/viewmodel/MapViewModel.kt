package com.travelsketch.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.travelsketch.data.dao.FirebaseRepository
import com.travelsketch.data.model.MapData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MapViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _canvasData = MutableStateFlow<MapData?>(null)
    val canvasData: StateFlow<MapData?> = _canvasData

    // map에 canvas 표시
    private val _canvasDataList = MutableStateFlow<List<MapData>>(emptyList())
    val canvasDataList: StateFlow<List<MapData>> = _canvasDataList

    private val _initialPosition = MutableStateFlow(LatLng(37.5665, 126.9780)) // 서울의 위도와 경도
    val initialPosition: StateFlow<LatLng> = _initialPosition

    private val _userCanvasDataList = MutableStateFlow<List<MapData>>(emptyList())
    val userCanvasDataList: StateFlow<List<MapData>> = _userCanvasDataList

    private val _friendCanvasDataList = MutableStateFlow<List<MapData>>(emptyList())
    val friendCanvasDataList: StateFlow<List<MapData>> = _friendCanvasDataList

    // 특정 사용자의 canvas_ids에 해당하는 Map 데이터를 가져오는 함수
    fun readUserMapCanvasData(userId: String) {
        viewModelScope.launch {
            val canvasIds = repository.readUserCanvasIds(userId) // 사용자 canvas_ids 가져오기
            val allCanvasData = repository.readAllMapCanvasData() // 모든 Canvas 데이터 읽기

            // canvasIds와 일치하는 데이터만 필터링
            val filteredCanvasData = allCanvasData.filter { canvas ->
                canvas.canvasId in canvasIds
            }

            _userCanvasDataList.value = filteredCanvasData // 상태 업데이트
        }
    }


    suspend fun getNextCanvasId(): String {
        val canvasList = repository.readAllMapCanvasData() // 모든 Canvas 데이터를 읽음

        // canvas_id에서 숫자만 추출하여 가장 큰 값을 찾음
        val maxCanvasNumber = canvasList.mapNotNull { canvas ->
            // "canvas_숫자" 형식에서 숫자를 추출
            canvas.canvasId.substringAfter("canvas_").toIntOrNull()
        }.maxOrNull() ?: 0 // 데이터가 없으면 0으로 초기화

        val nextCanvasNumber = maxCanvasNumber + 1 // 가장 큰 canvas_id의 숫자에 +1
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
        userId: String,
        canvasId: String,
        avgGpsLatitude: Double,
        avgGpsLongitude: Double,
        title : String
    ) {
        viewModelScope.launch {
            try {
                repository.createMapCanvasData(canvasId, avgGpsLatitude, avgGpsLongitude, title)
                repository.addUserCanvasId(userId, canvasId)
            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to create canvas or update user data", e)
            }
        }
    }

    // 친구의 canvas 데이터를 가져오기
    fun readFriendMapCanvasData(userId: String) {
        viewModelScope.launch {
            val friendIds = repository.readUserFriendsIds(userId)
            val friendCanvasIds = friendIds.flatMap { friendId ->
                repository.readUserCanvasIds(friendId)
            }

            val allCanvasData = repository.readAllMapCanvasData()

            // 친구 canvas_ids 중 is_visible이 true인 데이터만 필터링
            val filteredFriendCanvasData = allCanvasData.filter { canvas ->
                canvas.canvasId in friendCanvasIds && canvas.is_visible
            }

            _friendCanvasDataList.value = filteredFriendCanvasData
        }
    }

    fun updateInitialPosition(latLng: LatLng) {
        _initialPosition.value = latLng
    }
}
