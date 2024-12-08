package com.travelsketch.viewmodel

import android.graphics.Color
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.travelsketch.data.dao.FirebaseClient
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import com.travelsketch.ui.composable.loadVideoThumbnail
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CanvasViewModel : ViewModel() {
    val canvasWidth = 10000f
    val canvasHeight = 8000f

    var isLoading = mutableStateOf(false)
    var isUploading = mutableStateOf(false)
    var canvasId = mutableStateOf("")
    var isEditable = mutableStateOf(true)
    var boxes = mutableStateListOf<BoxData>()
    var selected = mutableStateOf<BoxData?>(null)

    var isTextPlacementMode = mutableStateOf(false)
    var textToPlace = mutableStateOf("")

    var isImagePlacementMode = mutableStateOf(false)
    var imageToPlace = mutableStateOf<String?>(null)

    var isVideoPlacementMode = mutableStateOf(false)
    var videoToPlace = mutableStateOf<String?>(null)

    val bitmaps = mutableStateMapOf<String, Bitmap>()
    val invalidateCanvasState = mutableStateOf(false)

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

    fun setScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    fun initializeCanvas(id: String) {
        canvasId.value = id
        viewAllBoxes(id)
    }

    private var boxIdMap = mutableMapOf<String, BoxData>()

    private fun viewAllBoxes(canvasId: String) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                val snapshot = FirebaseClient.readAllBoxData(canvasId)
                boxes.clear()
                boxIdMap.clear()

                snapshot?.forEach { boxData ->
                    boxes.add(boxData)
                    boxIdMap[boxData.id] = boxData
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun toggleIsEditable() {
        isEditable.value = !isEditable.value
        if (!isEditable.value) {
            clearSelection()
        }
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

    fun startVideoPlacement(videoUri: String) {
        videoToPlace.value = videoUri
        isVideoPlacementMode.value = true
    }

    fun createBox(canvasX: Float, canvasY: Float) {
        if (isTextPlacementMode.value) {
            createTextBox(canvasX, canvasY)
        } else if (isImagePlacementMode.value) {
            createImageBox(canvasX, canvasY)
        } else if (isVideoPlacementMode.value){
            createVideoBox(canvasX,canvasY)
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
        boxIdMap[box.id] = box

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
                isUploading.value = true

                // 1. 먼저 임시 BoxData를 생성하고 화면에 표시
                val (width, height) = calculateImageDimensions(context!!, uri)
                val centeredX = canvasX - width / 2f
                val centeredY = canvasY - height / 2f

                // 임시 박스 생성
                val tempBox = BoxData(
                    boxX = centeredX.toInt(),
                    boxY = centeredY.toInt(),
                    width = width,
                    height = height,
                    type = BoxType.IMAGE.toString(),
                    data = "uploading"
                )

                // 임시 박스 추가
                boxes.add(tempBox)
                boxIdMap[tempBox.id] = tempBox

                try {
                    val timestamp = System.currentTimeMillis()
                    val imageFileName = "image_${timestamp}.jpg"
                    val storageRef = FirebaseStorage.getInstance().reference
                        .child("media/images/$imageFileName")

                    val downloadUrl = context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                        storageRef.putStream(inputStream).await()
                        storageRef.downloadUrl.await().toString()
                    } ?: throw Exception("Failed to get input stream")

                    // 상태 업데이트를 한 번만 하도록 수정
                    tempBox.data = downloadUrl
                    // DB에 저장
                    FirebaseClient.writeBoxData(
                        canvasId.value,
                        tempBox.id,
                        tempBox
                    )

                } catch (e: Exception) {
                    boxes.remove(tempBox)
                    boxIdMap.remove(tempBox.id)
                    Log.e("CanvasViewModel", "Error uploading image", e)
                    throw e

                    }
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Error creating image box", e)
                // 에러 발생 시 임시 박스 제거
                boxes.removeIf { it.data == imageUri }
            } finally {
                isUploading.value = false
                isImagePlacementMode.value = false
                imageToPlace.value = null
            }
        }
    }

    private fun createVideoBox(canvasX: Float, canvasY: Float) {
        viewModelScope.launch {
            val videoUri = videoToPlace.value ?: return@launch
            val uri = Uri.parse(videoUri)


            try {
                isUploading.value = true

                val width = 600
                val height = 400
                val centeredX = canvasX - width / 2f
                val centeredY = canvasY + height / 2f // Adjust Y position
                val thumbnail = loadVideoThumbnail(context!!, videoUri)

                val tempBox = BoxData(
                    boxX = centeredX.toInt(),
                    boxY = centeredY.toInt(),
                    width = width,
                    height = height,
                    type = BoxType.VIDEO.toString(),
                    data = videoUri // Temporary local URI
                )
                boxes.add(tempBox)

                thumbnail?.let {
                    bitmaps[videoUri] = it
                    invalidateCanvasState.value = !invalidateCanvasState.value // Trigger recomposition
                }

                // Upload video to Firebase Storage
                val timestamp = System.currentTimeMillis()
                val videoFileName = "video_${timestamp}.mp4"
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("media/videos/$videoFileName")

                context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                    val uploadTask = storageRef.putStream(inputStream)
                    uploadTask.await()

                    val downloadUrl = storageRef.downloadUrl.await().toString()

                    // Update BoxData with Firebase URL
                    val finalBox = tempBox.copy(data = downloadUrl)
                    val index = boxes.indexOf(tempBox)
                    if (index != -1) {
                        boxes[index] = finalBox
                    }

                    // Save to Firebase Database
                    FirebaseClient.writeBoxData(
                        canvasId.value,
                        "box_${finalBox.boxX}_${finalBox.boxY}",
                        finalBox
                    )
                }
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Error creating video box", e)
                boxes.removeIf { it.data == videoUri }
            } finally {
                isUploading.value = false
                isVideoPlacementMode.value = false
                videoToPlace.value = null
            }
        }
    }

    private suspend fun calculateImageDimensions(context: Context, uri: Uri): Pair<Int, Int> {
        return withContext(Dispatchers.IO) {
            try {
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
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Error calculating image dimensions", e)
                Pair(500, 500)
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            selected.value?.let { box ->
                try {
                    if (box.type == BoxType.IMAGE.toString() && box.data.startsWith("https")) {
                        try {
                            val storage = FirebaseStorage.getInstance()
                            val imageRef = storage.getReferenceFromUrl(box.data)
                            imageRef.delete().await()
                        } catch (e: Exception) {
                            Log.e("CanvasViewModel", "Failed to delete image from storage", e)
                        }
                    }

                    FirebaseClient.deleteBoxData(canvasId.value, box.id)

                    boxes.remove(box)
                    boxIdMap.remove(box.id)
                    selected.value = null
                } catch (e: Exception) {
                    Log.e("CanvasViewModel", "Failed to delete box", e)
                }
            }
        }
    }

    fun updateBoxPosition(newX: Int, newY: Int) {
        val currentBox = selected.value ?: return

        currentBox.boxX = newX
        currentBox.boxY = newY

        val index = boxes.indexOfFirst { it.id == currentBox.id }
        if (index != -1) {
            boxes[index] = currentBox
            boxIdMap[currentBox.id] = currentBox
        }

        viewModelScope.launch {
            FirebaseClient.writeBoxData(canvasId.value, currentBox.id, currentBox)
        }
    }


    fun saveAll() {
        viewModelScope.launch {
            isLoading.value = true
            try {
                boxes.forEach { box ->
                    FirebaseClient.writeBoxData(
                        canvasId.value,
                        box.id,
                        box
                    )
                }
            } finally {
                isLoading.value = false
            }
        }
    }

    fun endPlacementMode() {
        isTextPlacementMode.value = false
        isImagePlacementMode.value = false
        textToPlace.value = ""
        imageToPlace.value = null
    }
}