package com.travelsketch.ui.composable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
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
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import com.travelsketch.viewmodel.CanvasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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


    val context = LocalContext.current
    val vibrator = remember {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    val boundaryPaint = remember {
        Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.STROKE
            strokeWidth = 5f
        }
    }

    val bitmaps = remember { mutableStateMapOf<String, Bitmap>() }
    val loadingStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(viewModel.boxes) {
        viewModel.boxes.filter { it.type == BoxType.IMAGE.toString() }.forEach { box ->
            val imageUrl = box.data
            if (!imageUrl.isNullOrEmpty() && imageUrl != "uploading" && !bitmaps.containsKey(imageUrl)) {
                loadingStates[imageUrl] = true
                try {
                    val bitmap = if (imageUrl.startsWith("http")) {
                        loadBitmapFromNetwork(imageUrl)
                    } else {
                        withContext(Dispatchers.IO) {
                            try {
                                val uri = Uri.parse(imageUrl)
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    BitmapFactory.decodeStream(input)
                                }
                            } catch (e: Exception) {
                                Log.e("CanvasScreen", "Error loading local image", e)
                                null
                            }
                        }
                    }
                    bitmap?.let {
                        bitmaps[imageUrl] = it
                    }
                } catch (e: Exception) {
                    Log.e("CanvasScreen", "Error loading image", e)
                } finally {
                    loadingStates[imageUrl] = false
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
        val isSelectedAndDragging = (box == viewModel.selected.value && isDragging && selectedBoxPosition.value != null)

        val (boxX, boxY) = if (isSelectedAndDragging) {
            selectedBoxPosition.value!!
        } else {
            Offset(box.boxX.toFloat(), box.boxY.toFloat())
        }

        when (box.type) {
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

            BoxType.IMAGE.toString() -> {
                val imageUrl = box.data
                val bitmap = bitmaps[imageUrl]
                val isLoading = loadingStates[imageUrl] == true || imageUrl == "uploading"

                val screenPos = canvasToScreen(Offset(boxX, boxY))
                val scaledWidth = box.width!! * canvasState.scale
                val scaledHeight = box.height!! * canvasState.scale

                if (isLoading) {
                    drawRect(
                        color = Color.Gray.copy(alpha = 0.3f),
                        topLeft = Offset(screenPos.x, screenPos.y),
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
                } else if (bitmap != null) {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawBitmap(
                            bitmap,
                            null,
                            android.graphics.RectF(
                                screenPos.x,
                                screenPos.y,
                                screenPos.x + scaledWidth,
                                screenPos.y + scaledHeight
                            ),
                            null
                        )
                    }
                } else {
                    drawRect(
                        color = Color.Red.copy(alpha = 0.3f),
                        topLeft = Offset(screenPos.x, screenPos.y),
                        size = Size(scaledWidth, scaledHeight)
                    )
                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = android.graphics.Color.RED
                            textSize = 40f * canvasState.scale
                            textAlign = Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.drawText(
                            "Load Failed",
                            screenPos.x + scaledWidth / 2,
                            screenPos.y + scaledHeight / 2,
                            paint
                        )
                    }
                }
            }        }
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

                            if (viewModel.isTextPlacementMode.value || viewModel.isImagePlacementMode.value) {
                                onTapForBox(canvasPos)
                                // 박스 배치 후 배치 모드 종료
                                viewModel.isTextPlacementMode.value = false
                                viewModel.isImagePlacementMode.value = false
                                viewModel.textToPlace.value = ""
                                viewModel.imageToPlace.value = null
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

            viewModel.boxes.forEach { box ->
                drawBox(box)
            }

            viewModel.selected.value?.let { selected ->
                drawSelectionHandles(selected)
            }

            if (viewModel.isTextPlacementMode.value || viewModel.isImagePlacementMode.value) {
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