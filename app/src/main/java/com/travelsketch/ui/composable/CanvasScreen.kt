package com.travelsketch.ui.composable

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
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

    // Paints
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

    // Coordinate conversion functions
    fun screenToCanvas(screenPos: Offset): Offset {
        return (screenPos - canvasState.offset) / canvasState.scale
    }

    fun canvasToScreen(canvasPos: Offset): Offset {
        return canvasPos * canvasState.scale + canvasState.offset
    }

    // Drawing functions
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
                    screenPos.x + scaledWidth/2,
                    screenPos.y + (scaledHeight/2 - (scaledPaint.descent() + scaledPaint.ascent())/2),
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
            .pointerInput(Unit) {
                detectTapGestures { offset ->
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
                            } else {
                                viewModel.defaultAction()
                            }
                        }
                    }
                }
            }
    ) {
        // Draw canvas boundary
        drawIntoCanvas { canvas ->
            canvas.nativeCanvas.drawRect(
                canvasState.offset.x,
                canvasState.offset.y,
                canvasState.offset.x + viewModel.canvasWidth * canvasState.scale,
                canvasState.offset.y + viewModel.canvasHeight * canvasState.scale,
                boundaryPaint
            )
        }

        // Draw all boxes
        viewModel.boxes.forEach { box -> drawBox(box) }

        // Draw selection handles if there's a selected box
        viewModel.selected.value?.let { selected -> drawSelectionHandles(selected) }

        // Draw overlay when in text placement mode
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
}
