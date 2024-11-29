package com.travelsketch.ui.composable

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.travelsketch.data.model.BoxType
import com.travelsketch.viewmodel.CanvasViewModel


@Composable
fun CanvasScreen(canvasViewModel: CanvasViewModel) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = FocusRequester()

    val focus by canvasViewModel.focus
    val scale by canvasViewModel.scale
    val offsetX by canvasViewModel.offsetX
    val offsetY by canvasViewModel.offsetY

    val boxes = canvasViewModel.boxes
    val selected = canvasViewModel.selected
    val defaultBrush = canvasViewModel.defaultBrush
    val selectBrush = canvasViewModel.selectBrush
    var editingText = canvasViewModel.editingText

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size.toSize()
                val canvasWidth = size.width
                val canvasHeight = size.height

                canvasViewModel.setCenter(
                    minOf(maxOf(-offsetX + canvasWidth / 2, 0f), canvasWidth),
                    minOf(maxOf(-offsetY + canvasHeight / 2, 0f), canvasHeight)
                )
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    canvasViewModel.updateScale(zoom)
                    canvasViewModel.updateOffset(pan.x, pan.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val tmp = boxes.findLast { box ->
                        val xRange = box.boxX.toFloat()..
                                (box.boxX + box.width!!).toFloat()
                        val yRange = (box.boxY - box.height!!).toFloat()..
                                box.boxY.toFloat()
                        offset.x in xRange && offset.y in yRange
                    }

                    if(tmp == null) {
                        canvasViewModel.unselect()
                    } else {
                        if (!tmp.equals(selected)) canvasViewModel.select(tmp)
                        else canvasViewModel.defaultAction()
                    }
                }
            },
        onDraw = {
            drawRect(color = Color.Yellow)
            drawIntoCanvas { canvas ->
                if (selected != null) {
                    selected.value?.let {
                        val fontMetrics = defaultBrush.value.fontMetrics
                        val x = it.boxX.toFloat()
                        val y = it.boxY.toFloat()
                        val width = defaultBrush.value.measureText(selected.value?.data)
                        val height = fontMetrics.top - fontMetrics.bottom

                        canvas.nativeCanvas.drawCircle(x, y, 5f, selectBrush.value)
                        canvas.nativeCanvas.drawCircle(x, y+height, 5f, selectBrush.value)
                        canvas.nativeCanvas.drawCircle(x+width, y, 5f, selectBrush.value)
                        canvas.nativeCanvas.drawCircle(x+width, y+height, 5f, selectBrush.value)
                    }
                }
                boxes.forEach { box ->
                    val x = box.boxX.toFloat()
                    val y = box.boxY.toFloat()

                    when (box.type) {
                        "TEXT" -> {
                            canvas.nativeCanvas.drawText(
                                box.data,
                                x, y, defaultBrush.value
                            )
                        }
                    }
                }
            }
        }
    )

}
