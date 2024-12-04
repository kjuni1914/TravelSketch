package com.travelsketch.ui.composable

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.travelsketch.viewmodel.CanvasViewModel

@Composable
fun StatusBar(canvasViewModel: CanvasViewModel) {
    val log by canvasViewModel.log
    Text(log)
}