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

class ListViewModel : ViewModel() {
    private val repository = FirebaseRepository()

    private val _canvasData = MutableStateFlow<MapData?>(null)
    val canvasData: StateFlow<MapData?> = _canvasData

    // 모두 불러오기
    private val _canvasList = MutableStateFlow<List<MapData>>(emptyList())
    val canvasList: StateFlow<List<MapData>> = _canvasList
    // 친구 리스트
    private val _friendCanvasDataList = MutableStateFlow<List<MapData>>(emptyList())
    val friendCanvasDataList: StateFlow<List<MapData>> = _friendCanvasDataList

    // Firebase에서 Canvas 데이터 가져오기
    fun readMapCanvasData(canvasId: String) {
        viewModelScope.launch {
            _canvasData.value = repository.readMapCanvasData(canvasId)
        }
    }

    // 사용자 canvas_ids에 해당하는 Canvas 데이터만 가져오기
    fun readUserMapCanvasData(userId: String) {
        viewModelScope.launch {
            val userCanvasIds = repository.readUserCanvasIds(userId) // 사용자 canvas_ids 가져오기
            val allCanvasData = repository.readAllMapCanvasData() // 전체 Canvas 데이터 가져오기

            // 사용자 canvas_ids에 해당하는 데이터만 필터링
            val filteredCanvasData = allCanvasData.filter { canvas ->
                canvas.canvasId in userCanvasIds
            }

            _canvasList.value = filteredCanvasData // 상태 업데이트
        }
    }

    fun addFriendByEmail(currentUserId: String, friendEmail: String) {
        viewModelScope.launch {
            repository.addFriendByEmail(currentUserId, friendEmail)
        }
    }

    // 친구의 canvas 데이터를 가져오기
    fun readFriendMapCanvasData(userId: String) {
        viewModelScope.launch {
            val friendIds = repository.readUserFriendsIds(userId)

            val friendCanvasIds = friendIds.flatMap { friendId ->
                val canvasIds = repository.readUserCanvasIds(friendId)
                canvasIds
            }

            val allCanvasData = repository.readAllMapCanvasData()

            // 친구 canvas_ids 중 is_visible이 true인 데이터만 필터링
            val filteredFriendCanvasData = allCanvasData.filter { canvas ->
                val isFriendCanvas = canvas.canvasId in friendCanvasIds
                val isVisible = canvas.is_visible
                isFriendCanvas && isVisible
            }
            _friendCanvasDataList.value = filteredFriendCanvasData
        }
    }
    // is_visible 값을 변경하는 로직
    fun toggleCanvasVisibility(canvasId: String, newVisibility: Boolean) {
        viewModelScope.launch {
            try {
                // Firebase에서 is_visible 값 업데이트
                repository.updateCanvasVisibility(canvasId, newVisibility)

                // 로컬 상태 업데이트
                val updatedList = _canvasList.value.map { canvas ->
                    if (canvas.canvasId == canvasId) {
                        canvas.copy(is_visible = newVisibility)
                    } else {
                        canvas
                    }
                }
                _canvasList.value = updatedList
                Log.d("ListViewModel", "Canvas $canvasId visibility updated to $newVisibility")
            } catch (e: Exception) {
                Log.e("ListViewModel", "Failed to update canvas visibility for $canvasId", e)
            }
        }
    }
}
