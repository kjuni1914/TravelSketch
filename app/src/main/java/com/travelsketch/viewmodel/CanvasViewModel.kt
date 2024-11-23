package com.travelsketch.viewmodel

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class CanvasViewModel : ViewModel() {

    var scale = mutableFloatStateOf(1f)
    var offsetX = mutableFloatStateOf(1f)
    var offsetY = mutableFloatStateOf(0f)
    var isEditable = mutableStateOf(true)

    fun updateScale(newScale: Float) {
        scale.floatValue *= newScale
    }

    fun updateOffset(panX: Float, panY: Float) {
        offsetX.floatValue += panX
        offsetY.floatValue += panY
    }

    fun toggleIsEditable() {
        isEditable.value = !isEditable.value
    }

//    fun selectBox(box: Box) = selectedBoxes.add(box)
//    fun selectBoxes(boxes: Iterable<Box>) = selectedBoxes.addAll(boxes)
//    fun deselectBox(box: Box) = selectedBoxes.remove(box)
//    fun deselectBoxes(boxes: Iterable<Box>) = selectedBoxes.removeAll(boxes)
//    fun initSelectedBox() = selectedBoxes.clear()
}