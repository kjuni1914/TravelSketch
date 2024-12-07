package com.travelsketch.ui.composable

import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.toSize
import com.travelsketch.viewmodel.CanvasViewModel

@Composable
fun CanvasScreen(canvasViewModel: CanvasViewModel) {
    val scale by canvasViewModel.scale
    val offsetX by canvasViewModel.offsetX
    val offsetY by canvasViewModel.offsetY

    val boundaryPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                val size = coordinates.size.toSize()
                canvasViewModel.setScreenSize(size.width, size.height)
            }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // 스케일 업데이트를 먼저 수행
                    canvasViewModel.updateScale(zoom, centroid.x, centroid.y)
                    // 현재 스케일에 상관없이 일관된 이동감을 위해 pan 값을 조정
                    canvasViewModel.updateOffset(pan.x, pan.y)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val tmp = canvasViewModel.boxes.findLast { box ->
                        val xRange = box.boxX.toFloat()..
                                (box.boxX + box.width!!).toFloat()
                        val yRange = (box.boxY - box.height!!).toFloat()..
                                box.boxY.toFloat()
                        offset.x in xRange && offset.y in yRange
                    }

                    if(tmp == null) {
                        canvasViewModel.unselect()
                    } else {
                        if (!tmp.equals(canvasViewModel.selected.value)) canvasViewModel.select(tmp)
                        else canvasViewModel.defaultAction()
                    }
                }
            },
        onDraw = {
            translate(offsetX, offsetY) {
                scale(scale) {
                    drawIntoCanvas { canvas ->
                        // 경계 그리기
                        canvas.nativeCanvas.drawRect(
                            0f,
                            0f,
                            canvasViewModel.canvasWidth,
                            canvasViewModel.canvasHeight,
                            boundaryPaint
                        )

                        // 선택된 박스 그리기
                        if (canvasViewModel.selected.value != null) {
                            canvasViewModel.selected.value?.let {
                                val fontMetrics = canvasViewModel.defaultBrush.value.fontMetrics
                                val x = it.boxX.toFloat()
                                val y = it.boxY.toFloat()
                                val width = canvasViewModel.defaultBrush.value.measureText(it.data)
                                val height = fontMetrics.top - fontMetrics.bottom

                                canvas.nativeCanvas.drawCircle(x, y, 5f, canvasViewModel.selectBrush.value)
                                canvas.nativeCanvas.drawCircle(x, y+height, 5f, canvasViewModel.selectBrush.value)
                                canvas.nativeCanvas.drawCircle(x+width, y, 5f, canvasViewModel.selectBrush.value)
                                canvas.nativeCanvas.drawCircle(x+width, y+height, 5f, canvasViewModel.selectBrush.value)
                            }
                        }

                        // 모든 박스 그리기
                        canvasViewModel.boxes.forEach { box ->
                            val x = box.boxX.toFloat()
                            val y = box.boxY.toFloat()

                            when (box.type) {
                                "TEXT" -> {
                                    canvas.nativeCanvas.drawText(
                                        box.data,
                                        x, y, canvasViewModel.defaultBrush.value
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}