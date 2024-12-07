package com.travelsketch.viewmodel

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.travelsketch.data.dao.FirebaseClient
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import kotlinx.coroutines.launch

class CanvasViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _canvasId = mutableStateOf<String?>(null)

    val canvasWidth = 10000f
    val canvasHeight = 8000f

    var isEditable = mutableStateOf(true)
    var boxes = mutableStateListOf<BoxData>()
    var selected = mutableStateOf<BoxData?>(null)
    var editingText = mutableStateOf(TextFieldValue(""))

    var log = mutableStateOf("")

    var isTextPlacementMode = mutableStateOf(false)
    var textToPlace = mutableStateOf("")

    var defaultBrush = mutableStateOf(Paint().apply {
        color = Color.BLACK
        textSize = 70f
        textAlign = Paint.Align.LEFT
    })

    var selectBrush = mutableStateOf(Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    })

    private var screenWidth = 0f
    private var screenHeight = 0f

    fun initializeCanvas(id: String) {
        _canvasId.value = id
        viewAllBoxes()
    }

    fun viewAllBoxes() {
        _canvasId.value?.let { canvasId ->
            viewModelScope.launch {
                FirebaseClient.readAllBoxData(canvasId)?.forEach { box ->
                    if (box.type == "TEXT") {
                        if (box.width == 0)
                            box.width = defaultBrush.value.measureText(box.data).toInt()
                        if (box.height == 0) {
                            // 텍스트 높이 계산
                            val fontMetrics = defaultBrush.value.fontMetrics
                            val textHeight = (fontMetrics.descent - fontMetrics.ascent).toInt()
                            box.height = textHeight
                        }
                    }
                    boxes.add(box)
                }
            }
        }
    }

    fun setScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    fun toggleIsEditable() {
        isEditable.value = !isEditable.value
    }

    fun select(box: BoxData) {
        selected.value = box
    }

    fun clearSelection() {
        selected.value = null
    }

    fun getEditable(): Boolean {
        return isEditable.value
    }

    fun saveAll() {
        _canvasId.value?.let { canvasId ->
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
                while (FirebaseClient.deleteBoxData(canvasId, "box_$idx")) {
                    idx++
                }
            }
        }
    }

    fun startTextPlacement(text: String) {
        textToPlace.value = text
        isTextPlacementMode.value = true
    }

    fun createBox(canvasX: Float, canvasY: Float) {
        if (isTextPlacementMode.value) {
            createTextBox(canvasX, canvasY)
        } else {
            val boxSize = 50
            val box = BoxData(
                boxX = canvasX.toInt(),
                boxY = canvasY.toInt(),
                width = boxSize,
                height = boxSize,
                type = BoxType.TEXT.toString()
            )
            boxes.add(box)
        }
    }

    fun createTextBox(canvasX: Float, canvasY: Float) {
        val text = textToPlace.value
        val paint = defaultBrush.value
        val textWidth = paint.measureText(text).toInt()
        // 텍스트 높이 계산
        val fontMetrics = paint.fontMetrics
        val textHeight = (fontMetrics.descent - fontMetrics.ascent).toInt()

        val box = BoxData(
            boxX = (canvasX - textWidth/2).toInt(),
            boxY = (canvasY - textHeight/2).toInt(),
            width = textWidth,
            height = textHeight,
            type = BoxType.TEXT.toString(),
            data = text
        )
        boxes.add(box)
        isTextPlacementMode.value = false
        textToPlace.value = ""
    }

    fun delete() {
        boxes.remove(selected.value)
        selected.value = null
    }
}
