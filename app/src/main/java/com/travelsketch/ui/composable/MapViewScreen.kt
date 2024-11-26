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
    // 초기 위치 상태 관찰
    val initialPosition = viewModel.initialPosition.collectAsState()
    // Firebase에서 가져온 모든 캔버스 데이터 상태 관찰
    val canvasDataList = viewModel.canvasDataList.collectAsState()

    // Google Map의 카메라 위치 상태
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition.value, 15f)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Google Map 표시
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // 모든 캔버스를 CanvasMarker로 지도에 표시
            canvasDataList.value.forEach { canvas ->
                CanvasMarker(
                    position = LatLng(canvas.avg_gps_latitude, canvas.avg_gps_longitude),
                    imageResId = R.drawable.paris, // paris.png를 마커 아이콘으로 설정
                    cameraPositionState = cameraPositionState,
                    onClick = {
                        // 마커 클릭 시 동작
                        println("Marker clicked at: ${canvas.avg_gps_latitude}, ${canvas.avg_gps_longitude}")
                    }
                )
            }
        }

        // 하단 버튼 추가
        Button(
            onClick = { onNavigateToListView() },
            modifier = Modifier
                .absoluteOffset(y = (-32).dp) // 하단 경계에서 32dp 위로
                .align(Alignment.BottomCenter) // 하단 중앙 정렬
                .width(110.dp) // 버튼 너비
                .height(48.dp), // 버튼 높이
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD6D6D6), // 버튼 배경색
                contentColor = Color.Black // 텍스트 색상
            )
        ) {
            Text(text = "List View")
        }
    }
}
