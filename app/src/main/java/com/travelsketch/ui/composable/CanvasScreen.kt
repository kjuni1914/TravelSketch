package com.travelsketch.ui.composable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.travelsketch.R
import com.travelsketch.data.model.BoxData
import com.travelsketch.data.model.BoxType
import com.travelsketch.data.model.ViewMode
import com.travelsketch.viewmodel.CanvasViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
    editable: Boolean
) {
    var canvasState by remember { mutableStateOf(CanvasState()) }
    var isDragging by remember { mutableStateOf(false) }

    // **추가한 로직 시작**
    var isRotating by remember { mutableStateOf(false) }
    var rotateStartAngle by remember { mutableStateOf(0f) }
    var initialDegree by remember { mutableStateOf(0) }
    // **추가한 로직 끝**

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

    fun rotatePoint(x: Float, y: Float, cx: Float, cy: Float, angleDeg: Float): Pair<Float,Float> {
        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
        val dx = x - cx
        val dy = y - cy
        val rx = dx * cos(angleRad) - dy * sin(angleRad)
        val ry = dx * sin(angleRad) + dy * cos(angleRad)
        return Pair(cx + rx, cy + ry)
    }

    fun DrawScope.drawBox(box: BoxData) {
        val isSelectedAndDragging = (box == viewModel.selected.value && isDragging && selectedBoxPosition.value != null)

        val (boxX, boxY) = if (isSelectedAndDragging) {
            selectedBoxPosition.value!!
        } else {
            Offset(box.boxX.toFloat(), box.boxY.toFloat())
        }

        val screenPos = canvasToScreen(Offset(boxX, boxY))
        val scaledWidth = (box.width ?: 0) * canvasState.scale
        val scaledHeight = (box.height ?: 0) * canvasState.scale

        // **추가한 로직 시작**: 회전 적용
        drawIntoCanvas { canvas ->
            val cx = screenPos.x + scaledWidth / 2
            val cy = screenPos.y + scaledHeight / 2
            canvas.save()
            canvas.rotate(box.degree.toFloat(), cx, cy)
            // **추가한 로직 끝**

            when (box.type) {
                BoxType.IMAGE.toString(), BoxType.RECEIPT.toString() -> {
                    val mediaUrl = box.data
                    if (mediaUrl == "uploading") {
                        drawRect(
                            color = Color.Gray.copy(alpha = 0.3f),
                            topLeft = screenPos,
                            size = Size(scaledWidth, scaledHeight)
                        )
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
                    } else {
                        localBitmaps[mediaUrl]?.let { bitmap ->
                            try {
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
                            } catch (e: Exception) {
                                Log.e("CanvasScreen", "Failed to draw bitmap", e)
                                drawRect(
                                    color = Color.Red.copy(alpha = 0.3f),
                                    topLeft = screenPos,
                                    size = Size(scaledWidth, scaledHeight)
                                )
                            }
                        } ?: run {
                            drawRect(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                topLeft = screenPos,
                                size = Size(scaledWidth, scaledHeight)
                            )
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

                BoxType.VIDEO.toString() -> {
                    val videoUrl = box.data
                    val bitmap = localBitmaps[videoUrl]
                    if (viewModel.selected.value == box) {
                        // 비디오 플레이어 표시 시 썸네일 생략
                        canvas.restore()
                        return
                    }

                    if (bitmap != null) {
                        try {
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
                        } catch (e: Exception) {
                            Log.e("CanvasScreen", "Failed to draw video thumbnail", e)
                            drawRect(
                                color = Color.Red.copy(alpha = 0.3f),
                                topLeft = screenPos,
                                size = Size(scaledWidth, scaledHeight)
                            )
                        }
                    } else {
                        drawRect(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            topLeft = screenPos,
                            size = Size(scaledWidth, scaledHeight)
                        )
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
                BoxType.TEXT.toString() -> {
                    val paint = Paint(viewModel.defaultBrush.value).apply {
                        textSize = viewModel.defaultBrush.value.textSize * canvasState.scale
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.nativeCanvas.drawText(
                        box.data ?: "",
                        screenPos.x + scaledWidth / 2,
                        screenPos.y + (scaledHeight / 2 - (paint.descent() + paint.ascent()) / 2),
                        paint
                    )
                }
            }

            canvas.restore()
        }
    }

    fun DrawScope.drawSelectionHandles(box: BoxData) {
        val (boxX, boxY) = if (box == viewModel.selected.value && (isDragging || isRotating) && selectedBoxPosition.value != null) {
            selectedBoxPosition.value!!
        } else {
            Offset(box.boxX.toFloat(), box.boxY.toFloat())
        }

        drawIntoCanvas { canvas ->
            val screenPos = canvasToScreen(Offset(boxX, boxY))
            val scaledWidth = box.width!! * canvasState.scale
            val scaledHeight = box.height!! * canvasState.scale

            val cx = screenPos.x + scaledWidth / 2
            val cy = screenPos.y + scaledHeight / 2

            // 회전 변환
            canvas.save()
            canvas.rotate(box.degree.toFloat(), cx, cy)

            val handleRadius = 20f * canvasState.scale
            // 회전 손잡이: 박스 상단 중앙 위로 조금 떨어진 곳
            val handleX = cx
            val handleY = cy - scaledHeight/2 - 50f * canvasState.scale

            // 핸들 그리기
            val handlePaint = Paint().apply {
                style = Paint.Style.FILL
                color = android.graphics.Color.GREEN
            }
            canvas.nativeCanvas.drawCircle(handleX, handleY, handleRadius, handlePaint)

            // 모서리 핸들
            val borderHandleRadius = 5f * canvasState.scale

            canvas.nativeCanvas.run {
                drawCircle(screenPos.x, screenPos.y, borderHandleRadius, viewModel.selectBrush.value)
                drawCircle(screenPos.x, screenPos.y + scaledHeight, borderHandleRadius, viewModel.selectBrush.value)
                drawCircle(screenPos.x + scaledWidth, screenPos.y, borderHandleRadius, viewModel.selectBrush.value)
                drawCircle(screenPos.x + scaledWidth, screenPos.y + scaledHeight, borderHandleRadius, viewModel.selectBrush.value)
            }

            canvas.restore()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .zIndex(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.all_button_icon),
                contentDescription = "All",
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        viewModel.currentViewMode.value =  ViewMode.ALL
                        if (viewModel.currentViewMode.value == ViewMode.ALL) {
                            viewModel.arrangeMediaBoxes()
                        }
                    }
            )
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.media_button_icon),
                contentDescription = "Media",
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        viewModel.currentViewMode.value =  ViewMode.MEDIA_ONLY
                        if (viewModel.currentViewMode.value == ViewMode.MEDIA_ONLY) {
                            viewModel.arrangeMediaBoxes()
                        }
                    }
            )
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.receipt_button_icon),
                contentDescription = "Receipt",
                modifier = Modifier
                    .size(48.dp)
                    .clickable {
                        viewModel.currentViewMode.value =  ViewMode.RECEIPTS_ONLY
                        if (viewModel.currentViewMode.value == ViewMode.RECEIPTS_ONLY) {
                            viewModel.arrangeReceiptBoxes()
                        }
                    }
            )
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
                        if (!isDragging && !isRotating) {
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
                            if (!editable) return@detectDragGesturesAfterLongPress

                            val canvasPos = screenToCanvas(offset)
                            val hitBox = viewModel.boxes.findLast { box ->
                                val boxPos = Offset(box.boxX.toFloat(), box.boxY.toFloat())
                                val boxSize = Size(box.width!!.toFloat(), box.height!!.toFloat())
                                val cx = boxPos.x + boxSize.width/2
                                val cy = boxPos.y + boxSize.height/2

                                // 회전 핸들 위치 계산
                                val handleDist = 50f
                                val handleX = cx
                                val handleY = cy - boxSize.height/2 - (handleDist / canvasState.scale)

                                // 박스가 회전했으니 역회전해서 포인터를 비교
                                val angleRad = Math.toRadians(box.degree.toDouble())
                                val sinA = sin(angleRad).toFloat()
                                val cosA = cos(angleRad).toFloat()

                                // 역회전한 좌표로 변경
                                val dx = (canvasPos.x - cx)
                                val dy = (canvasPos.y - cy)
                                val rx = dx * cosA + dy * sinA
                                val ry = -dx * sinA + dy * cosA

                                val handleDx = (handleX - cx)
                                val handleDy = (handleY - cy)
                                val handleRadius = 20f

                                // 역회전한 핸들 좌표
                                val rxHandle = handleDx
                                val ryHandle = handleDy

                                val distToHandle = ((rx - rxHandle)*(rx - rxHandle) + (ry - ryHandle)*(ry - ryHandle))
                                val inHandle = distToHandle <= (handleRadius*handleRadius)/(canvasState.scale*canvasState.scale)

                                val isInXRange = canvasPos.x >= boxPos.x && canvasPos.x <= boxPos.x + boxSize.width
                                val isInYRange = canvasPos.y >= boxPos.y && canvasPos.y <= boxPos.y + boxSize.height

                                if (inHandle) {
                                    // 회전 모드 진입
                                    isRotating = true
                                    viewModel.select(box)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(100)
                                    }
                                    val angle = atan2(dy, dx) * (180f / Math.PI.toFloat())
                                    rotateStartAngle = angle
                                    initialDegree = box.degree
                                    selectedBoxPosition.value = Offset(box.boxX.toFloat(), box.boxY.toFloat())
                                    return@findLast true
                                } else if (isInXRange && isInYRange) {
                                    // 드래그 모드 진입
                                    isDragging = true
                                    viewModel.select(box)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(100)
                                    }

                                    dragStartRelativeOffset = Offset(
                                        x = canvasPos.x - box.boxX.toFloat(),
                                        y = canvasPos.y - box.boxY.toFloat()
                                    )
                                    selectedBoxPosition.value = Offset(box.boxX.toFloat(), box.boxY.toFloat())
                                    return@findLast true
                                }

                                false
                            }
                        },
                        onDrag = { change, dragAmount ->
                            if (!editable) return@detectDragGesturesAfterLongPress

                            change.consume()
                            val selectedBox = viewModel.selected.value
                            if (isDragging && selectedBox != null) {
                                val canvasPos = screenToCanvas(change.position)
                                val newX = (canvasPos.x - dragStartRelativeOffset.x).toInt()
                                val newY = (canvasPos.y - dragStartRelativeOffset.y).toInt()

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
                            } else if (isRotating && selectedBox != null) {
                                val canvasPos = screenToCanvas(change.position)
                                val boxPos = Offset(selectedBox.boxX.toFloat(), selectedBox.boxY.toFloat())
                                val boxSize = Size(selectedBox.width!!.toFloat(), selectedBox.height!!.toFloat())
                                val cx = boxPos.x + boxSize.width/2
                                val cy = boxPos.y + boxSize.height/2

                                val dx = canvasPos.x - cx
                                val dy = canvasPos.y - cy
                                val angle = atan2(dy, dx) * (180f / Math.PI.toFloat())
                                val deltaAngle = angle - rotateStartAngle
                                selectedBox.degree = (initialDegree + deltaAngle.toInt()) % 360
                                invalidateCanvasState.value = !invalidateCanvasState.value
                            }
                        },
                        onDragEnd = {
                            if (!editable) return@detectDragGesturesAfterLongPress

                            if (isDragging) {
                                isDragging = false
                                val selectedBox = viewModel.selected.value
                                selectedBoxPosition.value?.let { finalPos ->
                                    viewModel.updateBoxPosition(finalPos.x.toInt(), finalPos.y.toInt())
                                }
                                viewModel.saveAll()
                            } else if (isRotating) {
                                isRotating = false
                                viewModel.saveAll()
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            isRotating = false
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (!isDragging && !isRotating) {
                            val canvasPos = screenToCanvas(offset)

                            if (viewModel.isTextPlacementMode.value || viewModel.isImagePlacementMode.value || viewModel.isVideoPlacementMode.value) {
                                onTapForBox(canvasPos)
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

