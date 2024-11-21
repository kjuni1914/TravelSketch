package com.travelsketch.ui.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.travelsketch.viewmodel.MapViewModel

@Composable
fun MapSetupScreen(onLocationConfirmed: (LatLng) -> Unit = {},
                   mapViewModel: MapViewModel
) {
    val initialPosition = LatLng(37.7749, 126.9780) // 기본 위치 (서울)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 15f)
    }

    var searchText by remember { mutableStateOf("") }
    var selectedPosition by remember { mutableStateOf(initialPosition) }
    var markerLocationName by remember { mutableStateOf("Unknown Location") }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "지도 캔버스의 초기 위치를 선택해주세요.",
            fontSize = 24.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            textAlign = TextAlign.Center
        )

        TextField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = { Text(text = "검색: 대한민국") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 마커의 위치 표시
        Text(
            text = "위치: $markerLocationName",
            fontSize = 16.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            textAlign = TextAlign.Start,
            color = Color.Gray
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            Map(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            )
            CenterMarker()
        }

        Button(
            onClick = { // 현재 위치 데이터를 Firebase로 전송
                val canvas_id = "canvas_5" // 임의 값
                val avg_gps_latitude = selectedPosition.latitude
                val avg_gps_longitude = selectedPosition.longitude

                mapViewModel.createMapData(
                    canvasId = canvas_id,
                    avgGpsLatitude = avg_gps_latitude,
                    avgGpsLongitude = avg_gps_longitude
                )
                onLocationConfirmed(selectedPosition) },// 선택 위치 콜백 호출 },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(48.dp)
        ) {
            Text(text = "확인")
        }
    }

    // 카메라 이동 완료 시 선택 위치 업데이트
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            selectedPosition = cameraPositionState.position.target
            markerLocationName = getLocationName(selectedPosition) // 선택 위치 이름 가져오기
        }
    }
}

fun getLocationName(position: LatLng): String {
    return "위도: ${position.latitude}, 경도: ${position.longitude}"
}