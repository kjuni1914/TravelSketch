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

    var scale = mutableStateOf(1f)
    var offsetX = mutableStateOf(0f)
    var offsetY = mutableStateOf(0f)

    var focus = mutableStateOf(false)
    var isEditable = mutableStateOf(true)
    var boxes = mutableStateListOf<BoxData>()
    var selected = mutableStateOf<BoxData?>(null)
    var log = mutableStateOf("")

    var editingText = mutableStateOf(TextFieldValue(""))

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
                        if (box.height == 0)
                            box.height = defaultBrush.value.measureText(box.data).toInt()
                    }
                    boxes.add(box)
                }
            }
        }
    }

    fun initializeViewport(width: Float, height: Float) {
        this.screenWidth = width
        this.screenHeight = height

        scale.value = minOf(
            width / (canvasWidth / 2),
            height / (canvasHeight / 2)
        )

        offsetX.value = (width - canvasWidth * scale.value) / 2
        offsetY.value = (height - canvasHeight * scale.value) / 2
    }

    fun updateScale(zoom: Float, focusX: Float, focusY: Float) {
        val oldScale = scale.value
        val newScale = (oldScale * zoom).coerceIn(0.1f, 5f)

        val canvasX = (focusX - offsetX.value) / oldScale
        val canvasY = (focusY - offsetY.value) / oldScale

        scale.value = newScale

        offsetX.value = focusX - (canvasX * newScale)
        offsetY.value = focusY - (canvasY * newScale)
    }

    fun updateOffset(panX: Float, panY: Float) {
        offsetX.value += panX
        offsetY.value += panY
    }

    fun setScreenSize(width: Float, height: Float) {
        if (screenWidth == 0f && screenHeight == 0f) {
            initializeViewport(width, height)
        }
        screenWidth = width
        screenHeight = height
    }

    fun toggleIsEditable() {
        isEditable.value = !isEditable.value
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
                while (FirebaseClient.deleteBoxData(
                        canvasId,
                        "box_$idx"
                    )
                ) {
                    idx++
                }
            }
        }
    }

    fun createBox(canvasX: Float, canvasY: Float) {
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

//    private fun createBox(
//        type: String,
//        data: String,
//        width: Int,
//        height: Int,
//        latitude: Double,
//        longitude: Double,
//        time: Long
//    ) {
//        val screenCenterX = screenWidth / 2
//        val screenCenterY = screenHeight / 2
//
//        val canvasX = (screenCenterX - offsetX.value) / scale.value
//        val canvasY = (screenCenterY - offsetY.value) / scale.value
//
//        val boxData = BoxData(
//            type = type,
//            data = data,
//            boxX = canvasX.toInt(),
//            boxY = canvasY.toInt(),
//            boxZ = 0,
//            width = width,
//            height = height,
//            degree = 0,
//            longitude = longitude,
//            latitude = latitude,
//            time = time
//        )
//        boxes.add(boxData)
//    }

    fun createText(text: String, x: Int, y: Int) {
        val box = BoxData(
            boxX = x,
            boxY = y,
            data = text,
            type = BoxType.TEXT.toString()
        )
    }
    fun delete() {
        boxes.remove(selected.value)
        selected.value = null
    }

    fun defaultAction() {
        editingText.value = TextFieldValue(selected.value!!.data)
        delete()
    }
}
