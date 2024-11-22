package com.travelsketch.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class CanvasViewModel : ViewModel() {
    var scale = mutableStateOf(1f)
    var offsetX = mutableStateOf(0f)
    var offsetY = mutableStateOf(0f)

    fun updateScale(newScale: Float) {
        scale.value *= newScale
    }

    fun updateOffset(panX: Float, panY: Float) {
        offsetX.value += panX
        offsetY.value += panY
    }
}