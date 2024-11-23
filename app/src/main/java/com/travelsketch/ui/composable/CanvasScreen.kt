package com.travelsketch.ui.composable

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import com.travelsketch.data.model.Box
import com.travelsketch.viewmodel.CanvasViewModel

@Composable
fun CanvasScreen(viewModel: CanvasViewModel) {

    // import androidx.compose.runtime.getValue
    // can make state variables to r/w

    // by keyword can make the variable's state
    // when it is changed then reflect the change
    // into the UI
    val scale by viewModel.scale
    val offsetX by viewModel.offsetX
    val offsetY by viewModel.offsetY
    var boxes = mutableListOf(Box(
        boxX = 120,
        boxY = 200,
        boxZ = 0,
        data = "text_content_here",
        degree = 0,
        height = 150,
        latitude = 37.71513,
        longitude = 126.734086,
        time = 163816092,
        type = "TEXT",
        width = 300
    ), Box(

    ))
    val isEditable by viewModel.isEditable


    Canvas(
        modifier = Modifier
            .fillMaxSize()
            /**
             * Modifier와 함께 사용되어서, 지정된 값에 따른 컴포넌트의 변환을 적용
             *
             * @Stable
             * fun Modifier.graphicsLayer(
             *     scaleX: Float = 1f,
             *     scaleY: Float = 1f,
             *     alpha: Float = 1f,
             *     translationX: Float = 0f,
             *     translationY: Float = 0f,
             *     shadowElevation: Float = 0f,
             *     rotationX: Float = 0f,
             *     rotationY: Float = 0f,
             *     rotationZ: Float = 0f,
             *     cameraDistance: Float = DefaultCameraDistance,
             *     transformOrigin: TransformOrigin = TransformOrigin.Center,
             *     shape: Shape = RectangleShape,
             *     clip: Boolean = false,
             *     renderEffect: RenderEffect? = null,
             *     ambientShadowColor: Color = DefaultShadowColor,
             *     spotShadowColor: Color = DefaultShadowColor,
             *     compositingStrategy: CompositingStrategy = CompositingStrategy.Auto
             * )
             *
             * 자주 사용할 것 같은 것은 transition, scale, rotation
             */
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = offsetX,
                translationY = offsetY
            )
            /**
             * 터치 스크린의 이벤트를 처리 하기 위한 함수를 정의
             * 그 중에 detectTransformGestures 가 있는데,
             * 줌, 회전 등의 다양한 제스처 처리 가능.
             *
             * suspend fun PointerInputScope.detectTransformGestures(
             *     panZoomLock: Boolean = false,
             *     onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
             * ) {
             *
             * 비동기 처리이기 때문에 이 함수가 동작되는 동안에도 다른 처리가 가능
             * detectTapGestures : UI 터치 감지
             * detectDragGestures : 드래그 했을 때 수행할 동작
             * ...
             */
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    viewModel.updateScale(zoom)
                    viewModel.updateOffset(pan.x, pan.y)
                }
            },
        onDraw = {
            drawRect(color = Color.White)

            boxes.forEach { box ->
                if (box.type == "TEXT") {
                    val x = box.boxX.toFloat()
                    val y = box.boxY.toFloat()

                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = 40f
                            textAlign = Paint.Align.LEFT
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        canvas.nativeCanvas.drawText(
                            box.data,
                            x, y, paint
                        )
                    }
                }
            }
        }
    )
    Button(
        onClick = {
            viewModel.toggleIsEditable()
        }
    ) { }
    if (isEditable)
        Editor()
}