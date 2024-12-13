package com.travelsketch.ui.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.auth.FirebaseAuth
import com.travelsketch.ui.composable.HostMapAndMapViewFragments

class MapViewActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions()
        val initialFragment = intent.getStringExtra("FRAGMENT") ?: "MAP_VIEW"
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            // 로그인되지 않은 경우 처리
            Log.d("MapViewActivity", "사용자가 로그인되어 있지 않습니다.")
            finish() // 액티비티 종료
        }

        setContent {
            HostMapAndMapViewFragments(
                fragmentManager = supportFragmentManager,
                initialFragment = initialFragment,
                userId = userId!! // 사용자 ID 전달
            )
        }
    }

    fun navigateToListViewActivity() {
        val intent = Intent(this, ListViewActivity::class.java)
        startActivity(intent)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach { (permission, isGranted) ->
                when (permission) {
                    Manifest.permission.CAMERA -> {
                        if (!isGranted) {
                            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Manifest.permission.POST_NOTIFICATIONS -> {
                        if (!isGranted) {
                            Toast.makeText(this, "Notification permission is required", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }

    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionsToRequest = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (permissionsToRequest.isNotEmpty()) {
                requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.CAMERA))
            }
        }
    }


    private fun navigateToCanvas(canvasId: String, isEditable: Boolean) {
        val intent = Intent(this, CanvasActivity::class.java).apply {
            putExtra("CANVAS_ID", canvasId)
            putExtra("EDITABLE", isEditable) // Pass editable flag
        }
        startActivity(intent)
    }

}