package com.travelsketch.ui.composable

import android.content.Context
import android.graphics.Paint
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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

    val boxPaint = remember {
        Paint().apply {
            color = android.graphics.Color.BLACK
            style = Paint.Style.FILL
        }
    }

    fun screenToCanvas(screenPos: Offset): Offset {
        return (screenPos - canvasState.offset) / canvasState.scale
    }

    fun canvasToScreen(canvasPos: Offset): Offset {
        return canvasPos * canvasState.scale + canvasState.offset
    }

    fun DrawScope.drawBox(box: BoxData) {
        drawIntoCanvas { canvas ->
            val screenPos = canvasToScreen(Offset(box.boxX.toFloat(), box.boxY.toFloat()))
            val scaledWidth = box.width!! * canvasState.scale
            val scaledHeight = box.height!! * canvasState.scale

            if (box.type == BoxType.TEXT.toString()) {
                val scaledPaint = Paint(viewModel.defaultBrush.value).apply {
                    textSize = viewModel.defaultBrush.value.textSize * canvasState.scale
                    textAlign = Paint.Align.CENTER
                }

                canvas.nativeCanvas.drawText(
                    box.data,
                    screenPos.x + scaledWidth / 2,
                    screenPos.y + (scaledHeight / 2 - (scaledPaint.descent() + scaledPaint.ascent()) / 2),
                    scaledPaint
                )
            } else {
                canvas.nativeCanvas.drawRect(
                    screenPos.x,
                    screenPos.y - scaledHeight,
                    screenPos.x + scaledWidth,
                    screenPos.y,
                    boxPaint
                )
            }
        }
    }

    fun DrawScope.drawSelectionHandles(box: BoxData) {
        drawIntoCanvas { canvas ->
            val screenPos = canvasToScreen(Offset(box.boxX.toFloat(), box.boxY.toFloat()))
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
                                val boxSize = Offset(box.width!!.toFloat(), box.height!!.toFloat())

                                canvasPos.x >= boxPos.x &&
                                        canvasPos.x <= boxPos.x + boxSize.x &&
                                        if (box.type == BoxType.TEXT.toString()) {
                                            canvasPos.y >= boxPos.y &&
                                                    canvasPos.y <= boxPos.y + boxSize.y
                                        } else {
                                            canvasPos.y >= boxPos.y - boxSize.y &&
                                                    canvasPos.y <= boxPos.y
                                        }
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
                                val canvasDragAmount = dragAmount / canvasState.scale
                                selectedBox.boxX = (screenToCanvas(change.position).x - dragStartRelativeOffset.x).toInt()
                                selectedBox.boxY = (screenToCanvas(change.position).y - dragStartRelativeOffset.y).toInt()

                                selectedBoxPosition.value = Offset(selectedBox.boxX.toFloat(), selectedBox.boxY.toFloat())

                                val screenPos = canvasToScreen(Offset(selectedBox.boxX.toFloat(), selectedBox.boxY.toFloat()))
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
                            isDragging = false
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

                            if (viewModel.isTextPlacementMode.value) {
                                viewModel.createTextBox(canvasPos.x, canvasPos.y)
                            } else {
                                val hitBox = viewModel.boxes.findLast { box ->
                                    val boxPos = Offset(box.boxX.toFloat(), box.boxY.toFloat())
                                    val boxSize = Offset(box.width!!.toFloat(), box.height!!.toFloat())

                                    if (box.type == BoxType.TEXT.toString()) {
                                        canvasPos.x >= boxPos.x &&
                                                canvasPos.x <= boxPos.x + boxSize.x &&
                                                canvasPos.y >= boxPos.y &&
                                                canvasPos.y <= boxPos.y + boxSize.y
                                    } else {
                                        canvasPos.x >= boxPos.x &&
                                                canvasPos.x <= boxPos.x + boxSize.x &&
                                                canvasPos.y >= boxPos.y - boxSize.y &&
                                                canvasPos.y <= boxPos.y
                                    }
                                }

                                if (hitBox == null) {
                                    if (viewModel.selected.value != null) {
                                        viewModel.clearSelection()
                                    } else {
                                        onTapForBox(canvasPos)
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

            viewModel.boxes.forEach { box -> drawBox(box) }

            viewModel.selected.value?.let { selected -> drawSelectionHandles(selected) }

            if (viewModel.isTextPlacementMode.value) {
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
            val pos = selectedBoxPosition.value!!
            Text(
                text = "X: ${pos.x.toInt()}, Y: ${pos.y.toInt()}",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                color = Color.Black
            )
        }
    }
}
