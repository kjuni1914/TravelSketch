package com.travelsketch.viewmodel

import android.Manifest
import android.app.Activity
import android.graphics.Color
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.travelsketch.api.GPTApiClient
import com.travelsketch.data.dao.FirebaseClient
import com.travelsketch.data.dao.FirebaseRepository
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import com.travelsketch.data.model.ViewMode
import com.travelsketch.data.util.ReceiptClassifier
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class CanvasViewModel : ViewModel() {
    val canvasWidth = 10000f
    val canvasHeight = 8000f

    private var receiptClassifier: ReceiptClassifier? = null

    var isLoading = mutableStateOf(false)
    var isUploading = mutableStateOf(false)
    var canvasId = mutableStateOf("")
    var isEditable = mutableStateOf(true)
    var boxes = mutableStateListOf<BoxData>()
    var selected = mutableStateOf<BoxData?>(null)
    var currentViewMode = mutableStateOf(ViewMode.ALL)

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
        receiptClassifier = ReceiptClassifier(context)
        Log.d("asdfasdfasdf", "Receipt classifier initialized")
    }

    fun setScreenSize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
    }

    fun initializeCanvas(id: String) {
        Log.d("asdfasdfasdf", "Initializing canvas with ID: $id")
        canvasId.value = id
        viewAllBoxes(id)
    }


    fun loadImage(imageUrl: String) {
        if (imageUrl.isEmpty() || imageUrl == "uploading" || bitmaps.containsKey(imageUrl)) return

        Log.d("asdfasdfasdf", "Starting image load for URL: $imageUrl")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    Log.d("asdfasdfasdf", "Attempting to connect to URL: $imageUrl")
                    val url = URL(imageUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.doInput = true
                    connection.connect()

                    Log.d("asdfasdfasdf", "Connection established, decoding bitmap")
                    BitmapFactory.decodeStream(connection.inputStream)
                }

                bitmap?.let {
                    Log.d("asdfasdfasdf", "Bitmap successfully decoded")
                    withContext(Dispatchers.Main) {
                        bitmaps[imageUrl] = it
                        invalidateCanvasState.value = !invalidateCanvasState.value
                        Log.d("asdfasdfasdf", "Bitmap stored and canvas invalidated")
                    }
                } ?: Log.e("asdfasdfasdf", "Failed to decode bitmap")

            } catch (e: Exception) {
                Log.e("asdfasdfasdf", "Failed to load image: $imageUrl", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        receiptClassifier?.close()
        receiptClassifier = null
    }

    fun loadVideo(videoUrl: String) {
        if (videoUrl.isEmpty() || videoUrl == "uploading" || bitmaps.containsKey(videoUrl)) return

        Log.d("asdfasdfasdf", "Starting image load for URL: $videoUrl")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    Log.d("asdfasdfasdf", "Attempting to connect to URL: $videoUrl")
                    val url = URL(videoUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.doInput = true
                    connection.connect()

                    Log.d("asdfasdfasdf", "Connection established, decoding bitmap")
                    BitmapFactory.decodeStream(connection.inputStream)
                }

                bitmap?.let {
                    Log.d("asdfasdfasdf", "Bitmap successfully decoded")
                    withContext(Dispatchers.Main) {
                        bitmaps[videoUrl] = it
                        invalidateCanvasState.value = !invalidateCanvasState.value
                        Log.d("asdfasdfasdf", "Bitmap stored and canvas invalidated")
                    }
                } ?: Log.e("asdfasdfasdf", "Failed to decode bitmap")

            } catch (e: Exception) {
                Log.e("asdfasdfasdf", "Failed to load image: $videoUrl", e)
            }
        }
    }

    private var boxIdMap = mutableMapOf<String, BoxData>()


    private fun viewAllBoxes(canvasId: String) {
        Log.d("asdfasdfasdf", "Starting viewAllBoxes for canvasId: $canvasId")
        viewModelScope.launch {
            isLoading.value = true
            try {
                Log.d("asdfasdfasdf", "Reading box data from Firebase")
                val snapshot = FirebaseClient.readAllBoxData(canvasId)
                Log.d("asdfasdfasdf", "Received data from Firebase: $snapshot")

                withContext(Dispatchers.Main) {
                    boxes.clear()
                    boxIdMap.clear()
                    Log.d("asdfasdfasdf", "Cleared existing boxes")

                    snapshot?.forEach { boxData ->
                        Log.d("asdfasdfasdf", "Processing box: ${boxData.id}")
                        Log.d("asdfasdfasdf", "Box data: $boxData")
                        boxes.add(boxData)
                        boxIdMap[boxData.id] = boxData

                        if (boxData.type == BoxType.IMAGE.toString()) {
                            Log.d("asdfasdfasdf", "Found image box with data: ${boxData.data}")
                            if (!boxData.data.isNullOrEmpty() && boxData.data != "uploading") {
                                Log.d("asdfasdfasdf", "Starting image load for: ${boxData.data}")
                                if (boxData.data.startsWith("http")) {
                                    loadImage(boxData.data)
                                } else {
                                    Log.d("asdfasdfasdf", "Invalid image URL format: ${boxData.data}")
                                }
                            } else {
                                Log.d("asdfasdfasdf", "Skipping image load - data empty or uploading")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("asdfasdfasdf", "Error in viewAllBoxes", e)
            } finally {
                isLoading.value = false
            }
        }
    }

    fun viewMediaBoxes(canvasId: String) {
        Log.d("CanvasViewModel", "Starting viewMediaBoxes for canvas: $canvasId")
        viewModelScope.launch {
            isLoading.value = true
            try {
                Log.d("CanvasViewModel", "Requesting media box data from Firebase")
                val snapshot = FirebaseClient.readAllBoxData(canvasId)
                Log.d("CanvasViewModel", "Received ${snapshot?.size ?: 0} boxes from Firebase")

                withContext(Dispatchers.Main) {
                    boxes.clear()
                    boxIdMap.clear()
                    Log.d("CanvasViewModel", "Cleared existing boxes")

                    snapshot?.forEach { boxData ->
                        if (boxData != null &&
                            (boxData.type == BoxType.IMAGE.toString() || boxData.type == BoxType.VIDEO.toString())) {
                            Log.d("CanvasViewModel", "Processing media box: ${boxData.id}, type: ${boxData.type}, data: ${boxData.data}")
                            boxes.add(boxData)
                            boxIdMap[boxData.id] = boxData

                            if (boxData.type == BoxType.IMAGE.toString() &&
                                !boxData.data.isNullOrEmpty() &&
                                boxData.data != "uploading") {
                                Log.d("CanvasViewModel", "Found image box, initiating loading for: ${boxData.data}")
                                loadImage(boxData.data)
                            } else if (boxData.type == BoxType.VIDEO.toString() &&
                                !boxData.data.isNullOrEmpty() &&
                                boxData.data.startsWith("http")) {
                                Log.d("CanvasViewModel", "Found video box, loading thumbnail for: ${boxData.data}")
                            }
                        }
                    }
                    Log.d("CanvasViewModel", "Finished processing media boxes. Total count: ${boxes.size}")
                }
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Error loading media boxes", e)
            } finally {
                isLoading.value = false
                Log.d("CanvasViewModel", "viewMediaBoxes completed")
            }
        }
    }

    fun arrangeMediaBoxes(): List<BoxData> {
        val arrangedBoxes = mutableListOf<BoxData>()

        // Step 1: 미디어 박스 필터링
        val mediaBoxes = boxes.filter {
            it.type == BoxType.IMAGE.toString() || it.type == BoxType.VIDEO.toString()
        }.sortedBy { it.id }

        // Step 2: 시작 위치와 간격 설정
        val startX = 50
        val startY = 50
        val spacing = 20
        val maxCanvasWidth = canvasWidth.toInt()

        var currentX = startX
        var currentY = startY
        var rowHeight = 0

        mediaBoxes.forEach { box ->
            // 캔버스 폭을 초과하면 다음 행으로 이동
            if (currentX + (box.width ?: 0) + spacing > maxCanvasWidth) {
                currentX = startX
                currentY += rowHeight + spacing
                rowHeight = 0
            }

            // 새 위치로 배치된 박스 데이터 생성
            arrangedBoxes.add(
                box.copy(
                    boxX = currentX,
                    boxY = currentY
                )
            )

            // X 좌표 업데이트
            currentX += (box.width ?: 0) + spacing

            // 행 높이 업데이트
            rowHeight = maxOf(rowHeight, box.height ?: 0)
        }

        return arrangedBoxes
    }

    fun arrangeReceiptBoxes(): List<BoxData> {
        val arrangedBoxes = mutableListOf<BoxData>()

        // Step 1: 미디어 박스 필터링
        val mediaBoxes = boxes.filter {
            it.type == BoxType.RECEIPT.toString()
        }.sortedBy { it.id }

        // Step 2: 시작 위치와 간격 설정
        val startX = 50
        val startY = 50
        val spacing = 20
        val maxCanvasWidth = canvasWidth.toInt()

        var currentX = startX
        var currentY = startY
        var rowHeight = 0

        mediaBoxes.forEach { box ->
            // 캔버스 폭을 초과하면 다음 행으로 이동
            if (currentX + (box.width ?: 0) + spacing > maxCanvasWidth) {
                currentX = startX
                currentY += rowHeight + spacing
                rowHeight = 0
            }

            // 새 위치로 배치된 박스 데이터 생성
            arrangedBoxes.add(
                box.copy(
                    boxX = currentX,
                    boxY = currentY
                )
            )

            // X 좌표 업데이트
            currentX += (box.width ?: 0) + spacing

            // 행 높이 업데이트
            rowHeight = maxOf(rowHeight, box.height ?: 0)
        }

        return arrangedBoxes
    }

    private fun createImageBox(canvasX: Float, canvasY: Float) {
        viewModelScope.launch {
            val imageUri = imageToPlace.value ?: return@launch
            val uri = Uri.parse(imageUri)

            var tempBox: BoxData? = null
            try {
                isUploading.value = true
                Log.d("asdfasdfasdf", "Starting image box creation process")

                // GPS 정보 추출
                val latLong = try {
                    context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                        val exif = androidx.exifinterface.media.ExifInterface(inputStream)
                        val latLongArray = FloatArray(2)
                        if (exif.getLatLong(latLongArray)) {
                            Pair(latLongArray[0].toDouble(), latLongArray[1].toDouble())
                        } else {
                            null
                        }
                    }
                } catch (e: Exception) {
                    Log.e("CanvasViewModel", "Error extracting GPS data", e)
                    null
                }

                Log.d("CanvasViewModel", "Extracted GPS coordinates: $latLong")

                // 이미지 분류 수행
                val bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, uri)
                val isReceipt = receiptClassifier?.classifyImage(bitmap) ?: false
                Log.d("asdfasdfasdf", "Image classification result - Is Receipt: $isReceipt")

                val (width, height) = calculateImageDimensions(context!!, uri)
                val boxId = UUID.randomUUID().toString()
                val boxType = if (isReceipt) BoxType.RECEIPT else BoxType.IMAGE

                tempBox = BoxData(
                    id = boxId,
                    boxX = (canvasX - width / 2f).toInt(),
                    boxY = (canvasY - height / 2f).toInt(),
                    width = width,
                    height = height,
                    type = boxType.toString(),
                    data = "uploading",
                    latitude = latLong?.first,
                    longitude = latLong?.second
                )

                // Firebase Storage에 업로드
                val downloadUrl = FirebaseRepository().uploadImageAndGetUrl(uri)
                Log.d("asdfasdfasdf", "Successfully uploaded to Firebase. URL: $downloadUrl")

                val finalBox = tempBox.copy(data = downloadUrl)
                val saveSuccess = FirebaseClient.writeBoxData(
                    canvasId.value,
                    boxId,
                    finalBox
                )

                if (!saveSuccess) {
                    throw Exception("Failed to save box data to database")
                }

                boxes.add(finalBox)
                boxIdMap[finalBox.id] = finalBox
                loadImage(downloadUrl)

                // 위치 업데이트
                updateCanvasLocation(canvasId.value, boxes)

                if (!saveSuccess) {
                    throw Exception("Failed to save box data to database")
                }

                boxes.add(finalBox)
                boxIdMap[finalBox.id] = finalBox
                loadImage(downloadUrl)

                // // GPT API 호출 (영수증인 경우)
                // if (isReceipt) {
                //     GPTApiClient.sendImage(
                //         imageUri = uri,
                //         context = context!!,
                //         apiKey = "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" // API 키를 설정하세요
                //     ) { gptResult ->
                //         Log.d("CanvasViewModel", "GPT API Result: $gptResult")

                //         // 텍스트 박스 생성
                //         val textBox = BoxData(
                //             id = UUID.randomUUID().toString(),
                //             boxX = finalBox.boxX,
                //             boxY = finalBox.boxY + (finalBox.height ?: 0) + 20, // 이미지 아래에 배치
                //             width = finalBox.width,
                //             height = 200, // 텍스트 박스의 높이를 설정
                //             type = BoxType.TEXT.toString(),
                //             data = gptResult
                //         )

                //         viewModelScope.launch {
                //             try {
                //                 FirebaseClient.writeBoxData(
                //                     canvasId.value,
                //                     textBox.id,
                //                     textBox
                //                 )
                //                 // 캔버스에 텍스트 박스 추가
                //                 boxes.add(textBox)
                //                 boxIdMap[textBox.id] = textBox
                //                 invalidateCanvasState.value = !invalidateCanvasState.value
                //             } catch (e: Exception) {
                //                 Log.e("CanvasViewModel", "Error saving text box to Firebase", e)
                //             }
                //         }

                //         // 캔버스에 텍스트 박스 추가
                //         boxes.add(textBox)
                //         boxIdMap[textBox.id] = textBox

                //         invalidateCanvasState.value = !invalidateCanvasState.value
                //     }
                // }

            } catch (e: Exception) {
                Log.e("asdfasdfasdf", "Error in createImageBox", e)
                tempBox?.let {
                    boxes.remove(it)
                    boxIdMap.remove(it.id)
                }
            } finally {
                isUploading.value = false
                isImagePlacementMode.value = false
                imageToPlace.value = null
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
        } else if (isVideoPlacementMode.value) {
            createVideoBox(canvasX, canvasY)
        }
    }

    private val loadingImages = mutableStateMapOf<String, Boolean>()

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

    private fun reloadAllImages() {
        boxes.forEach { box ->
            if (box.type == BoxType.IMAGE.toString() && !box.data.isNullOrEmpty() && box.data != "uploading") {
                loadImage(box.data)
            }
        }
    }


    private suspend fun uploadImageToFirebase(uri: Uri): String {
        return withContext(Dispatchers.IO) {
            try {
                val timestamp = System.currentTimeMillis()
                val imageFileName = "image_${timestamp}.jpg"
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("media/images/$imageFileName")

                context?.contentResolver?.openInputStream(uri)?.use { inputStream ->
                    storageRef.putStream(inputStream).await()
                    storageRef.downloadUrl.await().toString()
                } ?: throw Exception("Failed to open input stream")
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Failed to upload image to Firebase", e)
                throw e
            }
        }
    }

    private fun createVideoBox(canvasX: Float, canvasY: Float) {
        viewModelScope.launch {
            val videoUri = videoToPlace.value ?: return@launch
            val uri = Uri.parse(videoUri)
            var tempBox: BoxData? = null

            try {
                isUploading.value = true
                Log.d("asdfasdfasdf", "Starting video box creation")

                val width = 600
                val height = 800
                val boxId = UUID.randomUUID().toString()

                tempBox = BoxData(
                    id = boxId,  // ID 명시적 설정
                    boxX = (canvasX - width / 2f).toInt(),
                    boxY = (canvasY - height / 2f).toInt(),
                    width = width,
                    height = height,
                    type = BoxType.VIDEO.toString(),
                    data = videoUri
                )
                boxes.add(tempBox)


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
                        finalBox.id,
                        finalBox
                    )
                    invalidateCanvasState.value = !invalidateCanvasState.value

                }
            } catch (e: Exception) {
                Log.e("asdf", "Error creating video box", e)
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
                Log.e("calculateImageDimensions", "Error calculating image dimensions", e)
                Pair(500, 500)
            }
        }
    }


    fun delete() {
        viewModelScope.launch {
            selected.value?.let { box ->
                try {
                    if ((box.type == BoxType.IMAGE.toString() || box.type == BoxType.RECEIPT.toString()) &&
                        box.data.startsWith("https")) {
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

                    updateCanvasLocation(canvasId.value, boxes)
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
                // 위치 업데이트 추가
                updateCanvasLocation(canvasId.value, boxes)
            } finally {
                isLoading.value = false
            }
        }
    }



    fun checkStoragePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun requestStoragePermission(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            PackageManager.PERMISSION_GRANTED
        )
    }

    fun createPDF(): String? {
        val pdfDocument = PdfDocument()
        val paint = Paint().apply {
            textSize = 90f
            isFilterBitmap = true
        }

        val pageInfo = PdfDocument.PageInfo.Builder(
            dpToPx(screenWidth),
            dpToPx(screenHeight),
            1).create()

        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // 텍스트, 이미지, 동영상 그림
        for (box in boxes) {
            if (box.type == BoxType.TEXT.toString())
                canvas.drawText(box.data, box.boxX.toFloat(), box.boxY.toFloat(), paint)
            else if (box.type == BoxType.IMAGE.toString()) {
                if (box.data.startsWith("http") && bitmaps.containsKey(box.data)) {
                    Log.d("TEST", "감지 ${bitmaps[box.data]}")
                    val bitmap = bitmaps[box.data]
                    val scaledBitmap = Bitmap.createScaledBitmap(
                        bitmap!!,
                        dpToPx(box.width!!.toFloat()), dpToPx(box.height!!.toFloat()),
                        false
                    )

                    canvas.drawBitmap(
                        scaledBitmap,
                        dpToPx(box.boxX.toFloat()/(2.6f)).toFloat(),
                        dpToPx(box.boxY.toFloat()/7).toFloat(),
                        paint
                    )
                }
            }
        }

        pdfDocument.finishPage(page)

        val directory = context?.getExternalFilesDir(null)
        val file = File(directory, "tmp.pdf")

        try {
            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }

            Log.d("TEST", "PDF가 ${file.absolutePath}에 저장되었습니다")
            return file.absolutePath

        } catch (e: IOException) {
            e.printStackTrace()
            Log.d("TEST", "PDF 저장 중 오류 발생")
        } finally {
            pdfDocument.close()
        }
        return null
    }

    fun sharePdfFile(context: Context, filePath: String) {
        val file = File(filePath)

        if (file.exists()) {
            val fileUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Select App to share"))
        } else {
            Log.d("TEST", "File doesn't exist.")
        }
    }

    private fun dpToPx(dp: Float): Int {
        val density = context!!.resources.displayMetrics.density
        return (dp * density).toInt()
    }

    fun updateCanvasLocation(canvasId: String, boxes: List<BoxData>) {
        viewModelScope.launch {
            try {
                // 위치 데이터가 있는 박스들만 필터링
                val boxesWithLocation = boxes.filter { box ->
                    box.latitude != null && box.longitude != null &&
                            box.latitude != 0.0 && box.longitude != 0.0
                }

                // 데이터베이스 직접 업데이트
                val updates = mutableMapOf<String, Any>()

                if (boxesWithLocation.isNotEmpty()) {
                    // 위치가 있는 박스가 있으면 평균 계산
                    val avgLatitude = boxesWithLocation.map { it.latitude!! }.average()
                    val avgLongitude = boxesWithLocation.map { it.longitude!! }.average()
                    updates["avg_gps_latitude"] = avgLatitude
                    updates["avg_gps_longitude"] = avgLongitude
                } else {
                    // 위치 데이터가 없는 경우 초기값으로 설정
                    val originalData = FirebaseRepository().readMapCanvasData(canvasId)
                    if (originalData != null) {
                        updates["avg_gps_latitude"] = originalData.avg_gps_latitude
                        updates["avg_gps_longitude"] = originalData.avg_gps_longitude
                    }
                }

                // Firebase 업데이트 실행
                if (updates.isNotEmpty()) {
                    FirebaseClient.databaseRef
                        .child("map")
                        .child(canvasId)
                        .updateChildren(updates)
                        .await()

                    Log.d("CanvasViewModel", "Updated canvas location with updates: $updates")
                }
            } catch (e: Exception) {
                Log.e("CanvasViewModel", "Failed to update canvas location", e)
            }
        }
    }


    init {
        reloadAllImages()
    }
}
