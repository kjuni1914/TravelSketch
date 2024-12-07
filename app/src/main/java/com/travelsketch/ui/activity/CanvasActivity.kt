package com.travelsketch.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModelProvider
import com.travelsketch.ui.composable.CanvasScreen
import com.travelsketch.ui.composable.Editor
import com.travelsketch.ui.composable.StatusBar
import com.travelsketch.ui.layout.CanvasEditLayout
import com.travelsketch.viewmodel.CanvasViewModel

class CanvasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val canvasId = intent.getStringExtra("CANVAS_ID")
        if (canvasId == null) {
            Log.e("CanvasActivity", "No canvas ID provided")
            finish()
            return
        }

        val canvasViewModel = ViewModelProvider(this)[CanvasViewModel::class.java]
        canvasViewModel.initializeCanvas(canvasId)

        setContent {
            val showDialog = remember { mutableStateOf(false) }
            val isEditing = remember { mutableStateOf(false) }
            val lastTapPosition = remember { mutableStateOf<Offset?>(null) }

            CanvasEditLayout(
                canvas = {
                    CanvasScreen(
                        viewModel = canvasViewModel,
                        onTapForBox = { canvasPos ->
                            if (isEditing.value) {
                                canvasViewModel.createBox(canvasPos.x, canvasPos.y)
                                lastTapPosition.value = canvasPos
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
                            showDialog = showDialog
                        )
                    }
                },
                statusBar = {
                    StatusBar(canvasViewModel)
                }
            )
        }
    }
}