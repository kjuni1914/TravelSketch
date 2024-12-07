package com.travelsketch.ui.activity

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import com.travelsketch.ui.composable.CanvasScreen
import com.travelsketch.ui.composable.Editor
import com.travelsketch.ui.composable.ImageSourceDialog
import com.travelsketch.ui.composable.StatusBar
import com.travelsketch.ui.composable.TextInputDialog
import com.travelsketch.ui.layout.CanvasEditLayout
import com.travelsketch.viewmodel.CanvasViewModel
import java.io.File

class CanvasActivity : ComponentActivity() {
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private var tempImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        val canvasViewModel = ViewModelProvider(this)[CanvasViewModel::class.java]
        canvasViewModel.setContext(this)
        val canvasId = intent.getStringExtra("CANVAS_ID")
        if (canvasId == null) {
            finish()
            return
        }

        canvasViewModel.initializeCanvas(canvasId)

        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                canvasViewModel.startImagePlacement(it.toString())
            }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                tempImageUri?.let { uri ->
                    canvasViewModel.startImagePlacement(uri.toString())
                }
            }
        }

        setContent {
            val showDialog = remember { mutableStateOf(false) }
            val showImageSourceDialog = remember { mutableStateOf(false) }
            val isEditing = remember { mutableStateOf(false) }

            if (showDialog.value) {
                TextInputDialog(
                    onDismiss = { showDialog.value = false },
                    onConfirm = { text ->
                        canvasViewModel.startTextPlacement(text)
                        showDialog.value = false
                    }
                )
            }

            if (showImageSourceDialog.value) {
                ImageSourceDialog(
                    onDismiss = { showImageSourceDialog.value = false },
                    onSelectCamera = {
                        val imageFile = File.createTempFile("temp_image", ".jpg", cacheDir)
                        tempImageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", imageFile)
                        tempImageUri?.let { cameraLauncher.launch(it) }
                        showImageSourceDialog.value = false
                    },
                    onSelectGallery = {
                        galleryLauncher.launch("image/*")
                        showImageSourceDialog.value = false
                    }
                )
            }

            CanvasEditLayout(
                canvas = {
                    CanvasScreen(
                        viewModel = canvasViewModel,
                        onTapForBox = { canvasPos ->
                            if (isEditing.value) {
                                canvasViewModel.createBox(canvasPos.x, canvasPos.y)
                            }
                        }
                    )
                },
                button = {
                    Button(
                        onClick = {
                            isEditing.value = !isEditing.value
                            canvasViewModel.toggleIsEditable()
                        }
                    ) {
                        Text(if (isEditing.value) "Done" else "Edit")
                    }
                },
                editor = {
                    if (isEditing.value) {
                        Editor(
                            canvasViewModel = canvasViewModel,
                            showDialog = showDialog,
                            showImageSourceDialog = showImageSourceDialog
                        )
                    }
                },
                statusBar = {
                    StatusBar(canvasViewModel)
                }
            )
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
            }
        }

    private fun checkAndRequestPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
}