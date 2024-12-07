package com.travelsketch.ui.composable

import android.annotation.SuppressLint
import android.icu.text.Transliterator
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextLayoutInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.travelsketch.viewmodel.CanvasViewModel
import kotlinx.coroutines.coroutineScope


@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun CanvasScreen(canvasViewModel: CanvasViewModel) {
    var size by canvasViewModel.size
    val scale by canvasViewModel.scale
    val offsetX by canvasViewModel.offsetX
    val offsetY by canvasViewModel.offsetY
    var isChanging by canvasViewModel.isChanging

    val boxes = canvasViewModel.boxes
    val selected = canvasViewModel.selected
    val defaultBrush = canvasViewModel.defaultBrush
    val selectBrush = canvasViewModel.selectBrush
    val textField = canvasViewModel.textField

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(0f)
            .onGloballyPositioned { coordinates ->
                val tmp = coordinates.size.toSize()
                if (size == null) size = tmp
                val canvasWidth = size!!.width
                val canvasHeight = size!!.height

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
                detectTapGestures { offset ->
                    val tmp = boxes.findLast { box ->
                        val xRange = box.boxX.toFloat()..
                                (box.boxX + box.width!!).toFloat()
                        val yRange = (box.boxY - box.height!!).toFloat()..
                                box.boxY.toFloat()
                        offset.x in xRange && offset.y in yRange
                    }

                    if (tmp == null) {
                        if (isChanging) {
                            val txt = textField.value.text
                            selected.value!!.data = textField.value.text
                            isChanging = false
                        }

                        canvasViewModel.unselect()
                    } else {
                        if (tmp != selected.value) {
                            canvasViewModel.select(tmp)
                        } else {
                            canvasViewModel.defaultAction()
                        }
                    }
                }
            }
            .pointerInput(Unit) {
                coroutineScope {
                    while (true) {
                        awaitPointerEventScope {
                            var initDist: Float? = null

                            val pointerId = awaitFirstDown().id
                            val event = awaitPointerEvent()

                            if (event.changes.size >= 2) {
                                val midPnt = (event.changes[0].position +
                                        event.changes[1].position).div(2f)

                                drag(pointerId) { change ->
                                    if (initDist == null) {
                                        initDist = (midPnt - event.changes[0].position).getDistance()
                                    } else {
                                        val scale = (change.position - midPnt).getDistance() / initDist!!
                                        canvasViewModel.updateScale(scale)
                                    }
                                }

                            } else {
                                drag(pointerId) { change ->

                                    val tmp = boxes.findLast { box ->
                                        val xRange = box.boxX.toFloat()..
                                                (box.boxX + box.width!!).toFloat()
                                        val yRange = (box.boxY - box.height!!).toFloat()..
                                                box.boxY.toFloat()
                                        change.position.x in xRange && change.position.y in yRange
                                    }

                                    if (tmp != null) {
                                        canvasViewModel.select(tmp)
                                        canvasViewModel.updateBoxPosition(
                                            change.positionChange().x.toInt(),
                                            change.positionChange().y.toInt()
                                        )
                                    } else {
                                        canvasViewModel.updateOffset(
                                            change.positionChange().x,
                                            change.positionChange().y
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
        onDraw = {
            drawRect(color = Color.Transparent)
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
                    if (!(isChanging && box == selected.value)) {
                        val x = box.boxX.toFloat()
                        val y = box.boxY.toFloat()

                        when (box.type) {
                            "TEXT" -> {
                                canvas.nativeCanvas.drawText(
                                    box.data, x, y, defaultBrush.value
                                )
                            }
                        }
                    }
                }
            }
        }
    )
    if (isChanging) {
        val context = LocalContext.current
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        val density = context.resources.displayMetrics.density

        val fontMetrics = defaultBrush.value.fontMetrics
        val height = (fontMetrics.top - fontMetrics.bottom)/density

        val offsetXInDp = (offsetX * scale / density).dp
        val offsetYInDp = (offsetY * scale / density).dp
        val boxXInDp = selected.value?.boxX?.div(density)!!.dp
        val boxYInDp = selected.value?.boxY?.div(density)!!.dp

        BasicTextField(
            value = textField.value,
            onValueChange = { newText ->
                textField.value = newText
            },
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = (70f / scaledDensity).sp
            ),
            modifier = Modifier
                .offset(
                    x = offsetXInDp + boxXInDp,
                    y = offsetYInDp + boxYInDp + height.dp
                )
                .zIndex(1f)
        )
    }
}
