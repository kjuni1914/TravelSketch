package com.travelsketch.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.travelsketch.ui.composable.CanvasScreen
import com.travelsketch.viewmodel.CanvasViewModel

class CanvasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val canvasViewModel = ViewModelProvider(this).get(CanvasViewModel::class.java)

        setContent {
            CanvasScreen(canvasViewModel)
        }
    }
}