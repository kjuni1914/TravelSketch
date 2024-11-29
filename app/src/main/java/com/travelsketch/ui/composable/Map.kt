package com.travelsketch.ui.composable

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_NORMAL
import com.google.android.gms.maps.GoogleMapOptions
import com.google.maps.android.compose.*

@Composable
fun Map(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState
) {
    val context = LocalContext.current
    var isPermissionGranted by remember { mutableStateOf(false) }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!isPermissionGranted) {
            Toast.makeText(context, "Location permission is required to show your location", Toast.LENGTH_LONG).show()
        }
    }

    // 권한 요청
    LaunchedEffect(Unit) {
        if (!isPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 지도 렌더링
    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = isPermissionGranted, // 현재 위치 레이어 활성화
        ),
        uiSettings = MapUiSettings(
            compassEnabled = true,
            zoomControlsEnabled = true
        ),
        googleMapOptionsFactory = {
            GoogleMapOptions().apply {
                mapType(MAP_TYPE_NORMAL)
                compassEnabled(true)
                zoomControlsEnabled(true)
            }
        }
    )
}