package com.travelsketch.viewmodel

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelsketch.data.dao.FirebaseClient
import com.travelsketch.data.model.BoxData
import kotlinx.coroutines.launch

class CanvasViewModel : ViewModel() {
    var canvasId = "canvas_1"
    var size = mutableStateOf<Size?>(null)
    var scale = mutableFloatStateOf(1f)
    var offsetX = mutableFloatStateOf(0f)
    var offsetY = mutableFloatStateOf(0f)
    var centerX = mutableFloatStateOf(0f)
    var centerY = mutableFloatStateOf(0f)

    var isEditable = mutableStateOf(true)
    var isChanging = mutableStateOf(false)
    var boxes = mutableStateListOf<BoxData>()
    var selected = mutableStateOf<BoxData?>(null)
    var textField = mutableStateOf(TextFieldValue(""))
    var log = mutableStateOf("")


    var defaultBrush = mutableStateOf( Paint().apply {
        color = Color.BLACK
        textSize = 70f
        textAlign = Paint.Align.LEFT
    } )

    var selectBrush = mutableStateOf( Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    } )

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

    fun viewAllBoxes() {
        viewModelScope.launch {
            FirebaseClient.readAllBoxData(
                canvasId = canvasId
            )?.forEach { box ->
                if (box.type == "TEXT") {
                    if (box.width == 0)
                        box.width = defaultBrush.value.measureText(box.data).toInt()
                    if (box.height == 0)
                        box.height = defaultBrush.value.measureText(box.data).toInt()
                }
                boxes.add(box)
            }
        }
    }

    fun select(box: BoxData) {
        selected.value = box
    }

    fun unselect() {
        selected.value = null
    }

    fun getEditable(): Boolean {
        return isEditable.value
    }

    fun setCenter(x: Float, y: Float) {
        centerX.value = x
        centerY.value = y
        log.value = "${centerX.value}, ${centerY.value}"
    }

    private fun createBox(
        type: String,
        data: String,
        width: Int,
        height: Int,
        latitude: Double,
        longitude: Double,
        time: Long
    ) {
        val boxData = BoxData(
            type = type,
            data = data,
            boxX = centerX.floatValue.toInt(),
            boxY = centerY.floatValue.toInt(),
            boxZ = 0,
            width = width,
            height = height,
            degree = 0,
            longitude = longitude,
            latitude = latitude,
            time = time
        )
        boxes.add(boxData)
    }

    fun createText() {
        val fontMetrics = defaultBrush.value.fontMetrics
        val textHeight = fontMetrics.descent - fontMetrics.ascent
        createBox(
            type = "TEXT",
            data = "text",
            width = defaultBrush.value.measureText("text").toInt(),
            height = textHeight.toInt(),
            latitude = .0,
            longitude = .0,
            time = 0
        )
    }

    fun delete() {
        boxes.remove(selected.value)
        selected.value = null
    }

    fun saveAll() {
        var idx = 0

        viewModelScope.launch {
            boxes.forEach { box ->
                FirebaseClient.writeBoxData(
                    canvasId,
                    "box_$idx",
                    boxData = box
                )
                idx++
            }
            while (FirebaseClient.deleteBoxData(
                    canvasId,
                    "box_$idx"
                )) {
                idx++
            }
        }
    }

    fun defaultAction() {
        isChanging.value = true
        textField.value = TextFieldValue(selected.value!!.data)
    }

    fun updateBoxPosition(dx: Int, dy: Int) {
        val index = boxes.indexOf(selected.value)
        if (selected != null) {
            val x = selected.value?.boxX!!
            val y = selected.value?.boxY!!
            val width = selected.value?.width!!
            val height = selected.value?.height!!

            val updatedBox = selected.value?.copy(
                boxX = minOf(maxOf(x + dx, 0), size.value?.width?.minus(width)!!.toInt()),
                boxY = minOf(maxOf(y + dy, 0), size.value?.height?.minus(height)!!.toInt())
            )
            if (updatedBox != null) {
                boxes[index] = updatedBox
                selected.value = updatedBox
            }
        }
    }
}