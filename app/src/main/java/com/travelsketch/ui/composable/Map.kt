package com.travelsketch.ui.composable

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.GoogleMapOptions
import com.google.maps.android.compose.*

@Composable
fun Map(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState
) {
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        googleMapOptionsFactory = {
            GoogleMapOptions().apply {
                mapType(MAP_TYPE_NORMAL)
                compassEnabled(true)
                zoomControlsEnabled(true)
            }
        }
    )
}

