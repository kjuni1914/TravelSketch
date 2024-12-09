package com.travelsketch.ui.composable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import com.travelsketch.viewmodel.CanvasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class CanvasState(
    var scale: Float = 1f,
    var offset: Offset = Offset.Zero,
    var screenWidth: Float = 0f,
    var screenHeight: Float = 0f
)

@Composable
fun CanvasScreen(
    viewModel: CanvasViewModel,
    onTapForBox: (Offset) -> Unit
) {
    var canvasState by remember { mutableStateOf(CanvasState()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartRelativeOffset by remember { mutableStateOf(Offset.Zero) }
    val selectedBoxPosition = remember { mutableStateOf<Offset?>(null) }
    val invalidateCanvasState = viewModel.invalidateCanvasState
    var showMediaOnly by remember { mutableStateOf(false) } // State to toggle media-only mode

    val context = LocalContext.current
    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    val localBitmaps = remember { mutableStateMapOf<String, Bitmap>() }
    val loadingStates = remember { mutableStateMapOf<String, Boolean>() }



    val boundaryPaint = remember {
        Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
    }

    // 이미지 로드 효과
    LaunchedEffect(viewModel.boxes, invalidateCanvasState.value) {
        viewModel.boxes.forEach { box ->
            if (box.type == BoxType.IMAGE.toString() &&
                !box.data.isNullOrEmpty() &&
                box.data != "uploading" &&
                box.data.startsWith("http")) {

                val imageUrl = box.data
                if (!localBitmaps.containsKey(imageUrl)) {
                    loadingStates[imageUrl] = true
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            val url = URL(imageUrl)
                            val connection = url.openConnection() as HttpURLConnection
                            connection.connectTimeout = 10000
                            connection.readTimeout = 10000
                            connection.connect()

                            BitmapFactory.decodeStream(connection.inputStream)
                        }

                        bitmap?.let {
                            withContext(Dispatchers.Main) {
                                localBitmaps[imageUrl] = it
                                invalidateCanvasState.value = !invalidateCanvasState.value
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("CanvasScreen", "Failed to load image: $imageUrl", e)
                    } finally {
                        loadingStates[imageUrl] = false
                    }
                }
            }
        }
    }

    LaunchedEffect(viewModel.boxes, invalidateCanvasState.value) {
        viewModel.boxes.forEach { box ->
            if (box.type == BoxType.VIDEO.toString() &&
                !box.data.isNullOrEmpty() &&
                box.data.startsWith("http") &&
                !localBitmaps.containsKey(box.data)) {

                val thumbnail = loadVideoThumbnail(context, box.data)
                thumbnail?.let {
                    localBitmaps[box.data] = it
                    invalidateCanvasState.value = !invalidateCanvasState.value
                }
            }
        }
    }


    fun screenToCanvas(screenPos: Offset): Offset {
        return (screenPos - canvasState.offset) / canvasState.scale
    }

    fun canvasToScreen(canvasPos: Offset): Offset {
        return canvasPos * canvasState.scale + canvasState.offset
    }

    fun DrawScope.drawBox(box: BoxData) {
        if (box.type == BoxType.IMAGE.toString()) {
            val imageUrl = box.data
            val screenPos = canvasToScreen(Offset(box.boxX.toFloat(), box.boxY.toFloat()))

            Log.d("CanvasScreen", "Drawing box: ${box.id}")
            Log.d("CanvasScreen", "URL: $imageUrl")
            Log.d("CanvasScreen", "Bitmap available: ${localBitmaps.containsKey(imageUrl)}")
            Log.d("CanvasScreen", "Loading state: ${loadingStates[imageUrl]}")
        }
        val isSelectedAndDragging = (box == viewModel.selected.value && isDragging && selectedBoxPosition.value != null)

        val (boxX, boxY) = if (isSelectedAndDragging) {
            selectedBoxPosition.value!!
        } else {
            Offset(box.boxX.toFloat(), box.boxY.toFloat())
        }

        if (box.type == BoxType.VIDEO.toString() && !localBitmaps.containsKey(box.data)) {
            val thumbnail = loadVideoThumbnail(context, box.data)
            if (thumbnail != null) {
                localBitmaps[box.data] = thumbnail
                invalidateCanvasState.value = !invalidateCanvasState.value
            } else {
                Log.e("CanvasScreen", "Failed to load thumbnail for video: ${box.data}")
            }
        }


        when (box.type) {
            BoxType.IMAGE.toString() -> {
                val imageUrl = box.data
                Log.d("CanvasScreen", "Processing image box: ${box.id}, URL: $imageUrl")

                val screenPos = canvasToScreen(Offset(boxX, boxY))
                val scaledWidth = (box.width ?: 0) * canvasState.scale
                val scaledHeight = (box.height ?: 0) * canvasState.scale

                Log.d("CanvasScreen", "Drawing IMAGE box at $screenPos with size ${scaledWidth}x${scaledHeight}")
                Log.d("CanvasScreen", "Local bitmap available: ${localBitmaps[imageUrl] != null}")

                if (imageUrl == "uploading") {
                    drawRect(
                        color = Color.Gray.copy(alpha = 0.3f),
                        topLeft = screenPos,
                        size = Size(scaledWidth, scaledHeight)
                    )
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 40f * canvasState.scale
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.drawText(
                            "Uploading...",
                            screenPos.x + scaledWidth / 2,
                            screenPos.y + scaledHeight / 2,
                            paint
                        )
                    }
                } else {
                    localBitmaps[imageUrl]?.let { bitmap ->
                        try {
                            Log.d("CanvasScreen", "Drawing bitmap at $screenPos")
                            drawIntoCanvas { canvas ->
                                val destinationRect = android.graphics.RectF(
                                    screenPos.x,
                                    screenPos.y,
                                    screenPos.x + scaledWidth,
                                    screenPos.y + scaledHeight
                                )
                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    isFilterBitmap = true
                                }
                                canvas.nativeCanvas.drawBitmap(
                                    bitmap,
                                    null,
                                    destinationRect,
                                    paint
                                )
                                Log.d("CanvasScreen", "Successfully drew bitmap to canvas")
                            }
                        } catch (e: Exception) {
                            Log.e("CanvasScreen", "Failed to draw bitmap", e)
                            drawRect(
                                color = Color.Red.copy(alpha = 0.3f),
                                topLeft = screenPos,
                                size = Size(scaledWidth, scaledHeight)
                            )
                        }
                    } ?: run {
                        Log.d("CanvasScreen", "Bitmap not found in cache, showing loading state")
                        drawRect(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            topLeft = screenPos,
                            size = Size(scaledWidth, scaledHeight)
                        )
                        drawIntoCanvas { canvas ->
                            val paint = Paint().apply {
                                color = android.graphics.Color.BLACK
                                textSize = 40f * canvasState.scale
                                textAlign = Paint.Align.CENTER
                            }
                            canvas.nativeCanvas.drawText(
                                "Loading...",
                                screenPos.x + scaledWidth / 2,
                                screenPos.y + scaledHeight / 2,
                                paint
                            )
                        }
                    }
                }

                // 이미지 경계선 그리기
                drawIntoCanvas { canvas ->
                    val borderPaint = Paint().apply {
                        style = Paint.Style.STROKE
                        strokeWidth = 2f * canvasState.scale
                        color = android.graphics.Color.BLACK
                    }
                    canvas.nativeCanvas.drawRect(
                        screenPos.x,
                        screenPos.y,
                        screenPos.x + scaledWidth,
                        screenPos.y + scaledHeight,
                        borderPaint
                    )
                }
            }

            BoxType.VIDEO.toString() -> {
                val videoUrl = box.data
                Log.d("CanvasScreen", "Processing video box: ${box.id}, URL: $videoUrl")

                val screenPos = canvasToScreen(Offset(boxX, boxY))
                val scaledWidth = (box.width ?: 0) * canvasState.scale
                val scaledHeight = (box.height ?: 0) * canvasState.scale

                Log.d("CanvasScreen", "Drawing VIDEO box at $screenPos with size ${scaledWidth}x${scaledHeight}")

                val bitmap = localBitmaps[videoUrl]

                if (bitmap != null) {
                    try {
                        Log.d("CanvasScreen", "Drawing video thumbnail at $screenPos")
                        drawIntoCanvas { canvas ->
                            val destinationRect = android.graphics.RectF(
                                screenPos.x,
                                screenPos.y,
                                screenPos.x + scaledWidth,
                                screenPos.y + scaledHeight
                            )
                            val paint = Paint().apply {
                                isAntiAlias = true
                                isFilterBitmap = true
                            }
                            canvas.nativeCanvas.drawBitmap(
                                bitmap,
                                null,
                                destinationRect,
                                paint
                            )
                            Log.d("CanvasScreen", "Successfully drew video thumbnail")
                        }
                    } catch (e: Exception) {
                        Log.e("CanvasScreen", "Failed to draw video thumbnail", e)
                        drawRect(
                            color = Color.Red.copy(alpha = 0.3f),
                            topLeft = screenPos,
                            size = Size(scaledWidth, scaledHeight)
                        )
                    }
                } else {
                    Log.d("CanvasScreen", "Video thumbnail not found, showing loading state")
                    drawRect(
                        color = Color.LightGray.copy(alpha = 0.3f),
                        topLeft = screenPos,
                        size = Size(scaledWidth, scaledHeight)
                    )
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 40f * canvasState.scale
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.drawText(
                            "Loading...",
                            screenPos.x + scaledWidth / 2,
                            screenPos.y + scaledHeight / 2,
                            paint
                        )
                    }
                }
            }
            BoxType.TEXT.toString() -> {
                drawIntoCanvas { canvas ->
                    val screenPos = canvasToScreen(Offset(boxX, boxY))
                    val scaledWidth = box.width!! * canvasState.scale
                    val scaledHeight = box.height!! * canvasState.scale

                    val scaledPaint = Paint(viewModel.defaultBrush.value).apply {
                        textSize = viewModel.defaultBrush.value.textSize * canvasState.scale
                        textAlign = Paint.Align.CENTER
                    }

                    canvas.nativeCanvas.drawText(
                        box.data ?: "",
                        screenPos.x + scaledWidth / 2,
                        screenPos.y + (scaledHeight / 2 - (scaledPaint.descent() + scaledPaint.ascent()) / 2),
                        scaledPaint
                    )
                }
            }
        }
    }



    fun DrawScope.drawSelectionHandles(box: BoxData) {
        val (boxX, boxY) = if (box == viewModel.selected.value && isDragging && selectedBoxPosition.value != null) {
            selectedBoxPosition.value!!
        } else {
            Offset(box.boxX.toFloat(), box.boxY.toFloat())
        }

        drawIntoCanvas { canvas ->
            val screenPos = canvasToScreen(Offset(boxX, boxY))
            val scaledWidth = box.width!! * canvasState.scale
            val scaledHeight = box.height!! * canvasState.scale
            val handleRadius = 5f * canvasState.scale

            canvas.nativeCanvas.run {
                drawCircle(screenPos.x, screenPos.y, handleRadius, viewModel.selectBrush.value)
                drawCircle(screenPos.x, screenPos.y + scaledHeight, handleRadius, viewModel.selectBrush.value)
                drawCircle(screenPos.x + scaledWidth, screenPos.y, handleRadius, viewModel.selectBrush.value)
                drawCircle(screenPos.x + scaledWidth, screenPos.y + scaledHeight, handleRadius, viewModel.selectBrush.value)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier
            .align(Alignment.TopStart)
            .padding(16.dp)
            .zIndex(1f) // Ensure the button is above other components
        ) {
            androidx.compose.material3.Button(onClick = {
                showMediaOnly = !showMediaOnly // Toggle the mode
                if (showMediaOnly) {
                    viewModel.arrangeMediaBoxes() // 미디어 박스 정렬 및 위치 재배치
                }
            }) {
                Text(text = if (showMediaOnly) "Show All" else "Show Media Only")
            }
        }
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coordinates ->
                    val size = coordinates.size.toSize()
                    canvasState = canvasState.copy(
                        screenWidth = size.width,
                        screenHeight = size.height
                    )
                    viewModel.setScreenSize(size.width, size.height)
                }
                .then(if (invalidateCanvasState.value) Modifier else Modifier)
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        if (!isDragging) {
                            val oldScale = canvasState.scale
                            val newScale = (oldScale * zoom).coerceIn(0.1f, 5f)

                            val o = canvasState.offset
                            val c = centroid
                            val scaleFactor = newScale / oldScale
                            val newOffset = Offset(
                                x = o.x + (c.x - o.x) * (1 - scaleFactor),
                                y = o.y + (c.y - o.y) * (1 - scaleFactor)
                            ) + pan

                            canvasState = canvasState.copy(
                                scale = newScale,
                                offset = newOffset
                            )
                        }
                    }
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { offset ->
                            val canvasPos = screenToCanvas(offset)
                            val boxesToShow = if (showMediaOnly) {
                                viewModel.boxes.filter { it.type in listOf(BoxType.IMAGE.toString(), BoxType.VIDEO.toString()) }
                            } else {
                                viewModel.boxes
                            }
                            val hitBox = viewModel.boxes.findLast { box ->
                                val boxPos = Offset(box.boxX.toFloat(), box.boxY.toFloat())
                                val boxSize = Size(box.width!!.toFloat(), box.height!!.toFloat())

                                val isInXRange = canvasPos.x >= boxPos.x &&
                                        canvasPos.x <= boxPos.x + boxSize.width
                                val isInYRange = canvasPos.y >= boxPos.y &&
                                        canvasPos.y <= boxPos.y + boxSize.height

                                isInXRange && isInYRange
                            }

                            if (hitBox != null) {
                                isDragging = true
                                viewModel.select(hitBox)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(100)
                                }

                                dragStartRelativeOffset = Offset(
                                    x = canvasPos.x - hitBox.boxX.toFloat(),
                                    y = canvasPos.y - hitBox.boxY.toFloat()
                                )
                                selectedBoxPosition.value = Offset(hitBox.boxX.toFloat(), hitBox.boxY.toFloat())
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val selectedBox = viewModel.selected.value
                            if (isDragging && selectedBox != null) {
                                val canvasPos = screenToCanvas(change.position)
                                val newX = (canvasPos.x - dragStartRelativeOffset.x).toInt()
                                val newY = (canvasPos.y - dragStartRelativeOffset.y).toInt()

                                // 드래그 중에는 selectedBoxPosition을 업데이트하여 화면에 실시간으로 반영
                                selectedBoxPosition.value = Offset(newX.toFloat(), newY.toFloat())

                                val screenPos = canvasToScreen(Offset(newX.toFloat(), newY.toFloat()))
                                val margin = 50f

                                var newOffset = canvasState.offset

                                if (screenPos.x + selectedBox.width!! * canvasState.scale + margin > canvasState.screenWidth) {
                                    newOffset = newOffset.copy(x = newOffset.x - 25f)
                                }
                                if (screenPos.x - margin < 0) {
                                    newOffset = newOffset.copy(x = newOffset.x + 25f)
                                }
                                if (screenPos.y + selectedBox.height!! * canvasState.scale + margin > canvasState.screenHeight) {
                                    newOffset = newOffset.copy(y = newOffset.y - 25f)
                                }
                                if (screenPos.y - margin < 0) {
                                    newOffset = newOffset.copy(y = newOffset.y + 25f)
                                }

                                canvasState = canvasState.copy(offset = newOffset)
                            }
                        },
                        onDragEnd = {
                            // 드래그 종료 시 ViewModel에 최종 위치 반영
                            isDragging = false
                            val selectedBox = viewModel.selected.value
                            selectedBoxPosition.value?.let { finalPos ->
                                viewModel.updateBoxPosition(finalPos.x.toInt(), finalPos.y.toInt())
                            }
                            viewModel.saveAll()
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (!isDragging) {
                            val canvasPos = screenToCanvas(offset)

                            if (viewModel.isTextPlacementMode.value || viewModel.isImagePlacementMode.value || viewModel.isVideoPlacementMode.value) {
                                onTapForBox(canvasPos)
                                // 박스 배치 후 배치 모드 종료
                                viewModel.isTextPlacementMode.value = false
                                viewModel.isImagePlacementMode.value = false
                                viewModel.isVideoPlacementMode.value = false
                                viewModel.textToPlace.value = ""
                                viewModel.imageToPlace.value = null
                                viewModel.videoToPlace.value = null
                            } else {
                                val hitBox = viewModel.boxes.findLast { box ->
                                    val boxPos = Offset(box.boxX.toFloat(), box.boxY.toFloat())
                                    val boxSize = Size(box.width!!.toFloat(), box.height!!.toFloat())

                                    val isInXRange = canvasPos.x >= boxPos.x &&
                                            canvasPos.x <= boxPos.x + boxSize.width
                                    val isInYRange = canvasPos.y >= boxPos.y &&
                                            canvasPos.y <= boxPos.y + boxSize.height

                                    isInXRange && isInYRange
                                }

                                if (hitBox == null) {
                                    if (viewModel.selected.value != null) {
                                        viewModel.clearSelection()
                                    }
                                } else {
                                    if (hitBox != viewModel.selected.value) {
                                        viewModel.select(hitBox)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            drawIntoCanvas { canvas ->
                canvas.nativeCanvas.drawRect(
                    canvasState.offset.x,
                    canvasState.offset.y,
                    canvasState.offset.x + viewModel.canvasWidth * canvasState.scale,
                    canvasState.offset.y + viewModel.canvasHeight * canvasState.scale,
                    boundaryPaint
                )
            }
            val boxesToRender = if (showMediaOnly) {
                viewModel.arrangeMediaBoxes()
            } else {
                viewModel.boxes
            }

            boxesToRender.forEach { box ->
                drawBox(box)
            }

            viewModel.selected.value?.let { selected ->
                drawSelectionHandles(selected)
            }

            if (viewModel.isTextPlacementMode.value || viewModel.isImagePlacementMode.value || viewModel.isVideoPlacementMode.value) {
                drawRect(
                    color = Color.Gray.copy(alpha = 0.2f),
                    topLeft = Offset(canvasState.offset.x, canvasState.offset.y),
                    size = Size(
                        viewModel.canvasWidth * canvasState.scale,
                        viewModel.canvasHeight * canvasState.scale
                    )
                )
            }
        }

        if (isDragging && selectedBoxPosition.value != null) {
            Text(
                text = "X: ${selectedBoxPosition.value!!.x.toInt()}, Y: ${selectedBoxPosition.value!!.y.toInt()}",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                color = Color.Black
            )
        }
    }
}

suspend fun loadBitmapFromNetwork(urlString: String): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val url = java.net.URL(urlString)
            val connection = url.openConnection()
            connection.connect()
            val inputStream = connection.getInputStream()
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

fun loadVideoThumbnail(context: Context, videoUrl: String): Bitmap? {
    return try {
        // 1. Firebase Storage URL에서 비디오 다운로드
        val uri = Uri.parse(videoUrl)
        val tempFile = File(context.cacheDir, uri.toString())

        val connection = URL(videoUrl).openConnection() as HttpURLConnection
        connection.connect()
        connection.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 2. MediaMetadataRetriever로 로컬 파일 경로에서 썸네일 생성
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(tempFile.absolutePath)
        val thumbnail = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
        retriever.release()

        thumbnail
    } catch (e: Exception) {
        Log.e("CanvasScreen", "Failed to load video thumbnail", e)
        null
    }
}


