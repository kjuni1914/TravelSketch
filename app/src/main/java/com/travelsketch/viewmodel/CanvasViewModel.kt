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
        Log.d("CanvasViewModel", "Initializing canvas with ID: $id")
        canvasId.value = id
        viewAllBoxes(id)
    }

    private var boxIdMap = mutableMapOf<String, BoxData>()


    private fun viewAllBoxes(canvasId: String) {
        Log.d("CanvasViewModel", "Starting viewAllBoxes for canvas: $canvasId")
        viewModelScope.launch {
            isLoading.value = true
            try {
                Log.d("CanvasViewModel", "Requesting box data from Firebase")
                val snapshot = FirebaseClient.readAllBoxData(canvasId)
                Log.d("CanvasViewModel", "Received ${snapshot?.size ?: 0} boxes from Firebase")

                withContext(Dispatchers.Main) {
                    boxes.clear()
                    boxIdMap.clear()
                    Log.d("CanvasViewModel", "Cleared existing boxes")

                    snapshot?.forEach { boxData ->
                        if (boxData != null) {
                            Log.d("CanvasViewModel", "Processing box: ${boxData.id}, type: ${boxData.type}, data: ${boxData.data}")
                            boxes.add(boxData)
                            boxIdMap[boxData.id] = boxData

                            if (boxData.type == BoxType.IMAGE.toString() &&
                                !boxData.data.isNullOrEmpty() &&
                                boxData.data != "uploading") {
                                Log.d("CanvasViewModel", "Found image box, initiating loading for: ${boxData.data}")
                                loadImage(boxData.data)
                            }
                        }
                    }
                    Log.d("CanvasViewModel", "Finished processing all boxes. Total count: ${boxes.size}")
                }
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Error loading boxes", e)
            } finally {
                isLoading.value = false
                Log.d("CanvasViewModel", "viewAllBoxes completed")
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

    private val loadingImages = mutableStateMapOf<String, Boolean>()

    private fun loadImage(imageUrl: String) {
        Log.d("CanvasViewModel", "Starting loadImage for URL: $imageUrl")
        if (imageUrl.isEmpty() || imageUrl == "uploading" || bitmaps.containsKey(imageUrl)) {
            Log.d("CanvasViewModel", "Skipping loadImage: isEmpty=${imageUrl.isEmpty()}, isUploading=${imageUrl == "uploading"}, alreadyLoaded=${bitmaps.containsKey(imageUrl)}")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            Log.d("CanvasViewModel", "Starting image loading coroutine for: $imageUrl")
            loadingImages[imageUrl] = true
            try {
                val bitmap = if (imageUrl.startsWith("http")) {
                    Log.d("CanvasViewModel", "Loading remote image from: $imageUrl")
                    val url = java.net.URL(imageUrl)
                    val connection = url.openConnection()
                    connection.connect()
                    val inputStream = connection.getInputStream()
                    BitmapFactory.decodeStream(inputStream)
                } else {
                    Log.d("CanvasViewModel", "Loading local image from: $imageUrl")
                    context?.let { ctx ->
                        val uri = Uri.parse(imageUrl)
                        ctx.contentResolver.openInputStream(uri)?.use { input ->
                            BitmapFactory.decodeStream(input)
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    bitmap?.let {
                        Log.d("CanvasViewModel", "Successfully loaded bitmap for: $imageUrl")
                        bitmaps[imageUrl] = it
                        invalidateCanvasState.value = !invalidateCanvasState.value
                    } ?: Log.e("CanvasViewModel", "Failed to decode bitmap for: $imageUrl")
                }
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Failed to load image: $imageUrl", e)
            } finally {
                loadingImages[imageUrl] = false
            }
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
                Log.d("CanvasViewModel", "Starting image upload for URI: $imageUri")

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

                // Firebase Storage에 업로드
                val timestamp = System.currentTimeMillis()
                val imageFileName = "image_${timestamp}.jpg"
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("media/images/$imageFileName")

                try {
                    Log.d("CanvasViewModel", "Uploading image to Firebase Storage...")
                    val downloadUrl = context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                        val uploadTask = storageRef.putStream(inputStream)
                        uploadTask.await() // 업로드 완료 대기

                        Log.d("CanvasViewModel", "Image upload successful, getting download URL")
                        val url = storageRef.downloadUrl.await().toString()
                        Log.d("CanvasViewModel", "Got download URL: $url")
                        url
                    } ?: throw Exception("Failed to get input stream")

                    // 최종 박스 데이터 업데이트
                    val finalBox = tempBox.copy(data = downloadUrl)
                    Log.d("CanvasViewModel", "Updating box data with URL in boxes list")

                    val index = boxes.indexOf(tempBox)
                    if (index != -1) {
                        boxes[index] = finalBox
                        boxIdMap[finalBox.id] = finalBox
                    }

                    // Firebase Database에 저장
                    Log.d("CanvasViewModel", "Saving box data to Firebase Database...")
                    val saveResult = FirebaseClient.writeBoxData(
                        canvasId.value,
                        finalBox.id,
                        finalBox
                    )

                    if (saveResult) {
                        Log.d("CanvasViewModel", "Successfully saved box data to Firebase")
                        // 이미지 즉시 로드 시도
                        loadImage(downloadUrl)
                    } else {
                        Log.e("CanvasViewModel", "Failed to save box data to Firebase")
                    }

                } catch (e: Exception) {
                    Log.e("CanvasViewModel", "Upload failed", e)
                    // 실패 시 임시 박스 제거
                    boxes.remove(tempBox)
                    boxIdMap.remove(tempBox.id)
                    throw e
                }

            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Error creating image box", e)
                boxes.removeIf { it.data == "uploading" }
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