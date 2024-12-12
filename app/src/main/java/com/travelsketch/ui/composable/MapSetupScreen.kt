import android.net.http.HttpException
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresExtension
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.rememberCameraPositionState
import com.travelsketch.R
import com.travelsketch.api.RetrofitInstance
import com.travelsketch.ui.composable.CenterMarker
import com.travelsketch.ui.composable.Map
import com.travelsketch.ui.composable.getAddressFromLatLng
import com.travelsketch.viewmodel.MapViewModel
import kotlinx.coroutines.launch

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapSetupScreen(
    onLocationConfirmed: (LatLng, String, Boolean) -> Unit = { _, _, _ -> },
    mapViewModel: MapViewModel
) {
    val context = LocalContext.current
    val initialPosition = LatLng(37.7749, 126.9780) // 기본 위치 (서울)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialPosition, 15f)
    }

    val userId = FirebaseAuth.getInstance().currentUser?.uid
    if (userId == null) {
        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
        return
    }

    var searchText by remember { mutableStateOf("") }
    var selectedPosition by remember { mutableStateOf(initialPosition) }
    var markerLocationName by remember { mutableStateOf("Unknown Location") }
    var mapCanvasTitle by remember { mutableStateOf("") } // 제목 입력 필드 상태
    var canvasId by remember { mutableStateOf("") } // Firebase에서 가져올 canvas_id 상태

    val coroutineScope = rememberCoroutineScope()

    val CustomFontFamily = FontFamily(
        Font(R.font.waving_at_christmas)
    )

    LaunchedEffect(Unit) {
        canvasId = mapViewModel.getNextCanvasId() // ViewModel에서 canvas_id 가져오기
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF335577)) // 배경 설정
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Text(
                text = "Select Initial location of Canvas",
                fontSize = 45.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                fontFamily = CustomFontFamily,
                textAlign = TextAlign.Center,
                color = Color.White
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text(text = "Location: Korea") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color(0xFFFFF9C4),
                        cursorColor = Color.Black,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                // 검색 버튼
                Button(
                    onClick = {
                        if (searchText.isNotEmpty()) {
                            coroutineScope.launch {
                                val geocodedPosition = searchLocation(
                                    searchText,
                                    "AIzaSyD2nKPV8VIa2XbivuB6gtIqBRI4thUjbc0"
                                )
                                if (geocodedPosition != null) {
                                    cameraPositionState.position =
                                        CameraPosition.fromLatLngZoom(geocodedPosition, 15f)
                                } else {
                                    markerLocationName = "해당 위치를 찾을 수 없습니다."
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFF59D),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "검색",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            TextField(
                value = mapCanvasTitle,
                onValueChange = { mapCanvasTitle = it },
                placeholder = { Text(text = "캔버스 제목을 입력하세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color(0xFFFFF9C4),
                    cursorColor = Color.Black,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Text(
                text = "위치: $markerLocationName",
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                textAlign = TextAlign.Start,
                color = Color.White
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

        Button(
            onClick = {
                if (mapCanvasTitle.isEmpty()) {
                    Toast.makeText(context, "제목을 입력해주세요", Toast.LENGTH_SHORT).show()
                } else {
                    val avgGpsLatitude = selectedPosition.latitude
                    val avgGpsLongitude = selectedPosition.longitude

                    mapViewModel.createMapCanvasData(
                        userId = userId,
                        canvasId = canvasId,
                        avgGpsLatitude = avgGpsLatitude,
                        avgGpsLongitude = avgGpsLongitude,
                        title = mapCanvasTitle
                    )
                    onLocationConfirmed(selectedPosition, canvasId, true)
                }
            },
            modifier = Modifier
                .absoluteOffset(x = 0.dp, y = (-32).dp)
                .width(110.dp)
                .height(48.dp)
                .align(Alignment.BottomCenter),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD6D6D6),
                contentColor = Color.Black
            )
        ) {
            Text(text = "확인")
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            selectedPosition = cameraPositionState.position.target
            markerLocationName = getAddressFromLatLng(selectedPosition, "AIzaSyD2nKPV8VIa2XbivuB6gtIqBRI4thUjbc0")
        }
    }
}

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
suspend fun searchLocation(query: String, apiKey: String): LatLng? {
    return try {
        val response = RetrofitInstance.geocodingService.getGeocodingByAddress(query, apiKey)
        if (response.results.isNotEmpty()) {
            val location = response.results[0].geometry.location
            LatLng(location.lat, location.lng)
        } else {
            println("No results found for query: $query")
            null
        }
    } catch (e: HttpException) {
        println("HTTP Exception: ${e.message}")
        null
    } catch (e: Exception) {
        println("Error fetching location: ${e.message}")
        null
    }
}
