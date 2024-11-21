package com.travelsketch.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import com.travelsketch.ui.composable.MapSetupScreen
import com.travelsketch.viewmodel.MapViewModel

class MapSetupActivity : ComponentActivity() {
    private val mapViewModel: MapViewModel by viewModels()
    // 위치 권한 허용 관련
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            setMapContent()
        } else {
            println("Location permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (hasLocationPermission()) {
            setMapContent()
        } else {
            requestLocationPermission()
        }
    }

    // 위치 권한 허용 여부
    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun setMapContent() {
        setContent {
            MapSetupScreen(
                mapViewModel = mapViewModel, // ViewModel 전달
                onLocationConfirmed = { latLng ->
                    // 확인 버튼 눌렀을 때 처리
                    println("위도: ${latLng.latitude}, 경도: ${latLng.longitude}")
                }
            )
        }
    }
}