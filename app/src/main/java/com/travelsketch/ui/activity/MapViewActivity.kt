package com.travelsketch.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.travelsketch.ui.composable.MapViewScreen
import com.travelsketch.viewmodel.MapViewModel

class MapViewActivity : ComponentActivity() {
    private val viewModel: MapViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val canvas_id : String = "1"
        viewModel.fetchCanvasData(canvasId = "${canvas_id}")

        setContent {
            MapViewScreen(viewModel = viewModel)
        }
    }
}

