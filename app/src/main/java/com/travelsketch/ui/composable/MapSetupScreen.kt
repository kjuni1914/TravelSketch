import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.rememberCameraPositionState
import com.travelsketch.ui.composable.CenterMarker
import com.travelsketch.ui.composable.Map
import com.travelsketch.viewmodel.MapViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSetupScreen(
    onLocationConfirmed: (LatLng) -> Unit = {},
    mapViewModel: MapViewModel
) {
    val initialPosition = LatLng(37.7749, 126.9780) // 기본 위치 (서울)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 15f)
    }

//    var searchText by remember { mutableStateOf("") }
    var selectedPosition by remember { mutableStateOf(initialPosition) }
    var markerLocationName by remember { mutableStateOf("Unknown Location") }
    var mapCanvasTitle by remember { mutableStateOf("") } // 제목 입력 필드 상태
    var canvasId by remember { mutableStateOf("") } // Firebase에서 가져올 canvas_id 상태

    LaunchedEffect(Unit) {
        canvasId = mapViewModel.getNextCanvasId() // ViewModel에서 canvas_id 가져오기
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "캔버스의 초기 위치를 선택해주세요.",
                fontSize = 24.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                textAlign = TextAlign.Center
            )

//            TextField(
//                value = searchText,
//                onValueChange = { searchText = it },
//                placeholder = { Text(text = "검색: 대한민국") },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(horizontal = 16.dp, vertical = 8.dp),
//                colors = TextFieldDefaults.textFieldColors(
//                    containerColor = Color.LightGray, // 배경색 설정
//                    cursorColor = Color.Black, // 커서 색상
//                    focusedIndicatorColor = Color.Transparent,
//                    unfocusedIndicatorColor = Color.Transparent
//                ),
//                shape = RoundedCornerShape(8.dp) // 모서리를 둥글게 설정
//            )
            TextField(
                value = mapCanvasTitle,
                onValueChange = { mapCanvasTitle = it },
                placeholder = { Text(text = "캔버스 제목을 입력하세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.LightGray, // 배경색 설정
                    cursorColor = Color.Black, // 커서 색상
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp) // 모서리를 둥글게 설정
            )

            Text(
                text = "위치: $markerLocationName",
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                textAlign = TextAlign.Start,
                color = Color.Gray
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Map(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState
                )
                CenterMarker()
            }
        }

        // 버튼을 절대 위치로 하단 고정
        Button(
            onClick = {
                val canvas_id = canvasId // 임의 값
                val avg_gps_latitude = selectedPosition.latitude
                val avg_gps_longitude = selectedPosition.longitude
                val map_canvas_title = mapCanvasTitle

                mapViewModel.createMapCanvasData(
                    canvasId = canvas_id,
                    avgGpsLatitude = avg_gps_latitude,
                    avgGpsLongitude = avg_gps_longitude,
                    title = map_canvas_title
                )
                onLocationConfirmed(selectedPosition)
            },
            modifier = Modifier
                .absoluteOffset(x = 0.dp, y = (-32).dp) // 화면 아래 고정
                .width(110.dp) // 너비 고정
                .height(48.dp) // 높이 고정
                .align(Alignment.BottomCenter),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD6D6D6),
                contentColor = Color.Black // 텍스트 색상
            )
        ) {
            Text(text = "확인")
        }
    }

    // 카메라 이동 완료 시 선택 위치 업데이트
    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            selectedPosition = cameraPositionState.position.target
            markerLocationName = getLocationName(selectedPosition) // 선택 위치 이름 가져오기
        }
    }
}

fun getLocationName(position: LatLng): String {
    val latitude = String.format("%.5f", position.latitude)
    val longitude = String.format("%.5f", position.longitude)
    return "위도: $latitude, 경도: $longitude"
}

