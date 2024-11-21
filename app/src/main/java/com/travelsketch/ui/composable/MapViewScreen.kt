package com.travelsketch.ui.composable

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.travelsketch.viewmodel.MapViewModel

@Composable
fun MapViewScreen(viewModel: MapViewModel) {
    val canvasDataState = viewModel.canvasData.collectAsState()

    canvasDataState.value?.let { canvasData ->
        val initialPosition = LatLng(canvasData.avg_gps_latitude, canvasData.avg_gps_longitude)
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(initialPosition, 15f)
        }

        Map(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        )
    }
}