package com.travelsketch.ui.composable

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.travelsketch.R

@Composable
fun CanvasMarker(
    position: LatLng,
    imageResId: Int,
    cameraPositionState: CameraPositionState,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // 줌 레벨 가져오기
    val zoom = cameraPositionState.position.zoom

    // 줌 레벨에 따라 크기 계산
    val (width, height) = calculateMarkerSize(zoom)

    // 배경 포함 커스텀 BitmapDescriptor 생성
    val customIcon = getScaledBitmapDescriptorWithBackground(context, imageResId, width, height)

    // Google Map의 Marker 사용
    Marker(
        state = MarkerState(position = position),
        icon = customIcon,
        onClick = {
            onClick()
            true
        }
    )
}

fun calculateMarkerSize(zoom: Float): Pair<Int, Int> {
    val minSize = 50 // 최소 크기
    val maxSize = 200 // 최대 크기
    val normalizedZoom = zoom.coerceIn(2f, 21f) // 줌 레벨 제한
    val size = ((maxSize - minSize) * ((normalizedZoom - 2f) / (21f - 2f))).toInt() + minSize
    return Pair(size, size) // 가로, 세로 크기 반환
}


fun getScaledBitmapDescriptorWithBackground(context: Context, drawableRes: Int, width: Int, height: Int): BitmapDescriptor {
    val customBitmap = createCustomMarkerBitmap(context, drawableRes, width, height)
    return BitmapDescriptorFactory.fromBitmap(customBitmap)
}


fun createCustomMarkerBitmap(context: Context, drawableRes: Int, width: Int, height: Int): Bitmap {
    // 원본 이미지를 로드
    val originalBitmap = BitmapFactory.decodeResource(context.resources, drawableRes)

    // 배경 크기 설정
    val backgroundWidth = width + 20 // 이미지 크기보다 약간 크게
    val backgroundHeight = height + 20

    // 배경 Bitmap 생성
    val backgroundBitmap = Bitmap.createBitmap(backgroundWidth, backgroundHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(backgroundBitmap)

    // 네모 배경 그리기
    val paint = Paint()
    paint.color = Color.DKGRAY // 배경 색상 (칠판처럼 보이게)
    paint.style = Paint.Style.FILL
    canvas.drawRect(0f, 0f, backgroundWidth.toFloat(), backgroundHeight.toFloat(), paint)

    // 이미지를 축소하여 배경 중앙에 배치
    val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false)
    val left = (backgroundWidth - width) / 2
    val top = (backgroundHeight - height) / 2
    canvas.drawBitmap(scaledBitmap, left.toFloat(), top.toFloat(), null)

    return backgroundBitmap
}
