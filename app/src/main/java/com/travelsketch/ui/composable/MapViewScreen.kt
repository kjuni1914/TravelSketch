import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.travelsketch.R
import com.travelsketch.ui.composable.CanvasMarker
import com.travelsketch.viewmodel.MapViewModel

@Composable
fun MapViewScreen(
    viewModel: MapViewModel,
    onNavigateToListView: () -> Unit // 화면 전환 콜백
) {
    val canvasDataState = viewModel.canvasData.collectAsState()

    canvasDataState.value?.let { canvasData ->
        val cameraPositionState = rememberCameraPositionState {
            position = CameraPosition.fromLatLngZoom(
                LatLng(canvasData.avg_gps_latitude, canvasData.avg_gps_longitude),
                15f
            )
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Google Map 표시
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState
            ) {
                // CanvasMarker 추가
                CanvasMarker(
                    position = LatLng(canvasData.avg_gps_latitude, canvasData.avg_gps_longitude),
                    imageResId = R.drawable.paris, // paris.png를 마커 아이콘으로 설정
                    cameraPositionState = cameraPositionState,
                    onClick = {
                        // 마커 클릭 시 동작
                        println("Marker clicked at: ${canvasData.avg_gps_latitude}, ${canvasData.avg_gps_longitude}")
                    }
                )
            }

            // 하단 버튼 추가
            Button(
                onClick = { onNavigateToListView() },
                modifier = Modifier
                    .absoluteOffset(y = (-32).dp) // 하단 경계에서 32dp 위로
                    .align(Alignment.BottomCenter) // 하단 중앙 정렬
                    .width(110.dp) // 너비 설정 (높이의 2배)
                    .height(48.dp), // 고정된 높이
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD6D6D6),
                    contentColor = Color.Black // 텍스트 색상
                )
            ) {
                Text(text = "List View")
            }
        }
    }
}
