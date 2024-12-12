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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import com.travelsketch.data.model.ViewMode
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
    onTapForBox: (Offset) -> Unit,
    editable: Boolean // Add editable flag
) {
    var canvasState by remember { mutableStateOf(CanvasState()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragStartRelativeOffset by remember { mutableStateOf(Offset.Zero) }
    val selectedBoxPosition = remember { mutableStateOf<Offset?>(null) }
    val invalidateCanvasState = viewModel.invalidateCanvasState

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

    LaunchedEffect(viewModel.boxes, invalidateCanvasState.value) {
        viewModel.boxes.forEach { box ->
            if ((box.type == BoxType.IMAGE.toString() || box.type == BoxType.RECEIPT.toString()) &&
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
            if (box.type == BoxType.VIDEO.toString() && !localBitmaps.containsKey(box.data) && box.data.startsWith("http")) {
                val thumbnail = loadVideoThumbnail(context, box.data)
                if (thumbnail != null) {
                    localBitmaps[box.data] = thumbnail
                    invalidateCanvasState.value = !invalidateCanvasState.value
                } else {
                    Log.e("CanvasScreen", "Failed to load thumbnail for video: ${box.data}")
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




        when (box.type) {
            BoxType.IMAGE.toString(), BoxType.RECEIPT.toString() -> {
                val mediaUrl = box.data
                Log.d("CanvasScreen", "Processing ${box.type} box: ${box.id}, URL: $mediaUrl")

                val screenPos = canvasToScreen(Offset(boxX, boxY))
                val scaledWidth = (box.width ?: 0) * canvasState.scale
                val scaledHeight = (box.height ?: 0) * canvasState.scale

                Log.d("CanvasScreen", "Drawing ${box.type} box at $screenPos with size ${scaledWidth}x${scaledHeight}")
                Log.d("CanvasScreen", "Local bitmap available: ${localBitmaps[mediaUrl] != null}")

                if (mediaUrl == "uploading") {
                    // Draw uploading state
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
                    // Draw loaded bitmap or loading state
                    localBitmaps[mediaUrl]?.let { bitmap ->
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

                                // Draw border
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
                        } catch (e: Exception) {
                            Log.e("CanvasScreen", "Failed to draw bitmap", e)
                            drawRect(
                                color = Color.Red.copy(alpha = 0.3f),
                                topLeft = screenPos,
                                size = Size(scaledWidth, scaledHeight)
                            )
                        }
                    } ?: run {
                        // Show loading state when bitmap is not yet loaded
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
            }

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
                            "IMAGE\nUploading...",
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

                // 이미지 테두리 그리기
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


                if (viewModel.selected.value == box) {
                    // 비디오 플레이어 표시 시 썸네일 생략
                    return
                }

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
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .zIndex(1f), // Ensure the buttons are above other components
            horizontalArrangement = Arrangement.spacedBy(16.dp) // 버튼 간 간격 설정
        )
        {
            androidx.compose.material3.Button(onClick = {
                viewModel.currentViewMode.value =  ViewMode.ALL
                if (viewModel.currentViewMode.value == ViewMode.ALL) {
                    viewModel.arrangeMediaBoxes() // 미디어 박스 정렬 및 위치 재배치
                }
            }) {
                Text(text = "All")
            }
            androidx.compose.material3.Button(onClick = {
                viewModel.currentViewMode.value =  ViewMode.MEDIA_ONLY
                if (viewModel.currentViewMode.value == ViewMode.MEDIA_ONLY) {
                    viewModel.arrangeMediaBoxes() // 미디어 박스 정렬 및 위치 재배치
                }
            }) {
                Text(text = "Media")
            }
            androidx.compose.material3.Button(onClick = {
                viewModel.currentViewMode.value =  ViewMode.RECEIPTS_ONLY
                if (viewModel.currentViewMode.value == ViewMode.RECEIPTS_ONLY) {
                    viewModel.arrangeReceiptBoxes() // 미디어 박스 정렬 및 위치 재배치
                }
            }) {
                Text(text = "Receipt")
            }
        }
        val boxesToRender = when (viewModel.currentViewMode.value) {
            ViewMode.MEDIA_ONLY -> viewModel.arrangeMediaBoxes()
            ViewMode.RECEIPTS_ONLY -> viewModel.arrangeReceiptBoxes()
            ViewMode.ALL -> viewModel.boxes
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
                            if (!editable) return@detectDragGesturesAfterLongPress // Skip drag if not editable

                            val canvasPos = screenToCanvas(offset)
                            val boxesToShow = if (viewModel.currentViewMode.value == ViewMode.MEDIA_ONLY) {
                                viewModel.boxes.filter { it.type in listOf(BoxType.IMAGE.toString(), BoxType.VIDEO.toString()) }
                            } else if (viewModel.currentViewMode.value == ViewMode.RECEIPTS_ONLY){
                                viewModel.arrangeReceiptBoxes()
                            }else{
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
                            if (!editable) return@detectDragGesturesAfterLongPress // Skip drag if not editable

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
                            if (!editable) return@detectDragGesturesAfterLongPress // Skip drag if not editable

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

        boxesToRender.find { it.id == viewModel.selected.value?.id }?.let { selectedBox ->
            if (selectedBox.type == BoxType.VIDEO.toString()) {
                val screenPos = canvasToScreen(Offset(selectedBox.boxX.toFloat(), selectedBox.boxY.toFloat()))
                val scaledWidth = (selectedBox.width ?: 0) * canvasState.scale
                val scaledHeight = (selectedBox.height ?: 0) * canvasState.scale

                val density = LocalDensity.current
                val xDp = with(density) { screenPos.x.toDp() }
                val yDp = with(density) { screenPos.y.toDp() }
                val wDp = with(density) { scaledWidth.toDp() }
                val hDp = with(density) { scaledHeight.toDp() }

                VideoPlayer(
                    videoUrl = selectedBox.data,
                    modifier = Modifier
                        .absoluteOffset(x = xDp, y = yDp)
                        .size(wDp, hDp)
                )
            }
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
suspend fun loadVideoThumbnail(context: Context, videoUrl: String): Bitmap? = withContext(Dispatchers.IO) {
    Log.d("asdf", "Attempting to load video thumbnail for: $videoUrl")
    try {
        val tempFile = File(context.cacheDir, "temp_video.mp4")

        if (tempFile.exists()) {
            tempFile.delete()
        }

        val connection = URL(videoUrl).openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.doInput = true
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            Log.e("asdf", "Failed to download video. Response code: ${connection.responseCode}")
            return@withContext null
        }

        connection.inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (tempFile.length() == 0L) {
            Log.e("asdf", "Downloaded video file is empty.")
            return@withContext null
        }

        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(tempFile.absolutePath)
        val thumbnail = retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST)
        retriever.release()

        if (thumbnail == null) {
            Log.e("asdf", "Failed to retrieve thumbnail from video.")
        } else {
            Log.d("asdf", "Thumbnail successfully retrieved.")
        }

        thumbnail
    } catch (e: Exception) {
        Log.e("asdf", "Exception while loading video thumbnail", e)
        null
    }
}
