package com.travelsketch.ui.activity

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.lifecycle.ViewModelProvider
import com.travelsketch.ui.composable.CanvasScreen
import com.travelsketch.ui.composable.Editor
import com.travelsketch.ui.composable.StatusBar
import com.travelsketch.ui.layout.CanvasEditLayout
import com.travelsketch.viewmodel.CanvasViewModel

class CanvasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Intent에서 canvasId 가져오기
        val canvasId = intent.getStringExtra("CANVAS_ID")
        if (canvasId == null) {
            Log.e("CanvasActivity", "No canvas ID provided")
            finish()
            return
        }

        val canvasViewModel = ViewModelProvider(this)[CanvasViewModel::class.java]
        // canvasId로 ViewModel 초기화
        canvasViewModel.initializeCanvas(canvasId)

        setContent {
            CanvasEditLayout(
                canvas = {
                    CanvasScreen(canvasViewModel)
                },
                button = {
                    Button(
                        onClick = {
                            canvasViewModel.toggleIsEditable()
                        }
                    ) {
                        Text("Edit")
                    }
                },
                editor = {
                    if (canvasViewModel.getEditable()) {
                        Editor(canvasViewModel)
                    }
                },
                statusBar = {
                    StatusBar(canvasViewModel)
                }
            )
        }
    }
}