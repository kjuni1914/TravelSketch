package com.travelsketch.viewmodel

import android.graphics.Color
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.travelsketch.data.dao.FirebaseClient
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CanvasViewModel : ViewModel() {
    val canvasWidth = 10000f
    val canvasHeight = 8000f

    var isLoading = mutableStateOf(false)
    var canvasId = mutableStateOf("")
    var isEditable = mutableStateOf(true)
    var boxes = mutableStateListOf<BoxData>()
    var selected = mutableStateOf<BoxData?>(null)

    var isTextPlacementMode = mutableStateOf(false)
    var textToPlace = mutableStateOf("")

    var isImagePlacementMode = mutableStateOf(false)
    var imageToPlace = mutableStateOf<String?>(null)

    private var context: Context? = null

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

    fun setContext(context: Context) {
        this.context = context
    }

    fun initializeCanvas(id: String) {
        canvasId.value = id
        viewAllBoxes(id)
    }

    private fun viewAllBoxes(canvasId: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                FirebaseClient.readAllBoxData(canvasId)?.forEach { box ->
                    boxes.add(box)
                }
            } finally {
                isLoading.value = false
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

    fun startTextPlacement(text: String) {
        textToPlace.value = text
        isTextPlacementMode.value = true
    }

    fun startImagePlacement(imageUri: String) {
        imageToPlace.value = imageUri
        isImagePlacementMode.value = true
    }

    fun createBox(canvasX: Float, canvasY: Float) {
        if (isTextPlacementMode.value) {
            createTextBox(canvasX, canvasY)
        } else if (isImagePlacementMode.value) {
            createImageBox(canvasX, canvasY)
        }
    }

    private fun createTextBox(canvasX: Float, canvasY: Float) {
        val text = textToPlace.value
        val paint = defaultBrush.value
        val textWidth = paint.measureText(text).toInt()
        val textHeight = (paint.fontMetrics.descent - paint.fontMetrics.ascent).toInt()
        val centeredX = canvasX - textWidth / 2f
        val centeredY = canvasY - textHeight / 2f

        val box = BoxData(
            boxX = centeredX.toInt(),
            boxY = centeredY.toInt(),
            width = textWidth,
            height = textHeight,
            type = BoxType.TEXT.toString(),
            data = text
        )
        boxes.add(box)
        isTextPlacementMode.value = false
        textToPlace.value = ""

        viewModelScope.launch {
            FirebaseClient.writeBoxData(
                canvasId.value,
                "box_${box.boxX}_${box.boxY}",
                box
            )
        }
    }

    private fun createImageBox(canvasX: Float, canvasY: Float) {
        viewModelScope.launch {
            val imageUri = imageToPlace.value ?: return@launch
            val uri = Uri.parse(imageUri)

            try {
                isLoading.value = true

                // 1. Calculate image dimensions
                val (width, height) = calculateImageDimensions(context!!, uri)
                val centeredX = canvasX - width / 2f
                val centeredY = canvasY - height / 2f

                // 2. Upload image to Firebase Storage
                val timestamp = System.currentTimeMillis()
                val imageFileName = "image_${timestamp}.jpg"
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("media/images/$imageFileName")

                // Upload file
                context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                    val uploadTask = storageRef.putStream(inputStream)
                    uploadTask.await()

                    // Get download URL
                    val downloadUrl = storageRef.downloadUrl.await().toString()

                    // 3. Create BoxData with the Firebase Storage URL
                    val box = BoxData(
                        boxX = centeredX.toInt(),
                        boxY = centeredY.toInt(),
                        width = width,
                        height = height,
                        type = BoxType.IMAGE.toString(),
                        data = downloadUrl
                    )

                    // 4. Save to Firebase Database
                    FirebaseClient.writeBoxData(
                        canvasId.value,
                        "box_${box.boxX}_${box.boxY}",
                        box
                    )

                    // 5. Add to local boxes list
                    boxes.add(box)
                }
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Error creating image box", e)
            } finally {
                isLoading.value = false
                isImagePlacementMode.value = false
                imageToPlace.value = null
            }
        }
    }

    private suspend fun calculateImageDimensions(context: Context, uri: Uri): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            val maxSize = 500
            val width = options.outWidth
            val height = options.outHeight
            val ratio = width.toFloat() / height.toFloat()

            if (width > height) {
                Pair(maxSize, (maxSize / ratio).toInt())
            } else {
                Pair((maxSize * ratio).toInt(), maxSize)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            selected.value?.let { box ->
                boxes.remove(box)
                selected.value = null
                FirebaseClient.deleteBoxData(canvasId.value, "box_${box.boxX}_${box.boxY}")
            }
        }
    }

    fun saveAll() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                boxes.forEachIndexed { index, box ->
                    FirebaseClient.writeBoxData(
                        canvasId.value,
                        "box_${box.boxX}_${box.boxY}",
                        box
                    )
                }
            } finally {
                isLoading.value = false
            }
        }
    }
}