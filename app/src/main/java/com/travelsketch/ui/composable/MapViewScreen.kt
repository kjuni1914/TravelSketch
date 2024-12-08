import android.Manifest
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresExtension
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.travelsketch.R
import com.travelsketch.data.repository.FirebaseStorageHelper
import com.travelsketch.ui.composable.CanvasMarker
import com.travelsketch.ui.composable.getAddressFromLatLng
import com.travelsketch.viewmodel.MapViewModel
import kotlinx.coroutines.launch

@RequiresExtension(extension = Build.VERSION_CODES.S, version = 7)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapViewScreen(
    viewModel: MapViewModel,
    onNavigateToListView: () -> Unit,
    onNavigateToMapSetup: () -> Unit,
    onNavigateToCanvas: (String) -> Unit // canvasId를 전달받는 콜백 추가
) {
    val canvasDataList by viewModel.userCanvasDataList.collectAsState() // 사용자 캔버스 데이터 상태 관찰
    val friendCanvasDataList by viewModel.friendCanvasDataList.collectAsState() // 친구 캔버스 데이터
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(viewModel.initialPosition.value, 15f)
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    var locationName by remember { mutableStateOf("Unknown Location") }
    val isShaking = remember { mutableStateOf(false) }

    // 현재 사용자 ID 가져오기
    val currentUser = FirebaseAuth.getInstance().currentUser
    val userId = currentUser?.uid ?: run {
        Log.e("MapViewScreen", "User is not authenticated")
        return
    }
    var isPermissionGranted by remember { mutableStateOf(false) }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        isPermissionGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (!isPermissionGranted) {
            Toast.makeText(context, "Location permission is required to show your location", Toast.LENGTH_LONG).show()
        }
    }
    val TitleFontFamily = FontFamily(
        Font(R.font.typo_crayonm) // 파일 이름은 확장자 없이 사용
    )
    // 권한 요청
    LaunchedEffect(Unit) {
        if (!isPermissionGranted) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // 사용자 데이터를 읽어오기
    LaunchedEffect(userId) {
        viewModel.readUserMapCanvasData(userId)
    }

    // 사용자와 친구 데이터 읽기
    LaunchedEffect(userId) {
        viewModel.readUserMapCanvasData(userId)
        viewModel.readFriendMapCanvasData(userId) // 친구 캔버스 데이터 읽기
    }

    // 흔드는 제스처 감지
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val shakeListener = object : SensorEventListener {
            private val shakeThreshold = 20.0f
            private var lastTime: Long = 0

            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return

                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]

                val acceleration = kotlin.math.sqrt(x * x + y * y + z * z)
                val currentTime = System.currentTimeMillis()

                if (acceleration > shakeThreshold && currentTime - lastTime > 500) {
                    lastTime = currentTime
                    isShaking.value = true
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            shakeListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        onDispose {
            sensorManager.unregisterListener(shakeListener)
        }
    }

    // 흔들렸을 때 줌 작아지는 애니메이션
    LaunchedEffect(isShaking.value) {
        if (isShaking.value) {
            isShaking.value = false
            coroutineScope.launch {
                val targetZoom = 1f
                val currentZoom = cameraPositionState.position.zoom
                val durationMillis = 1000L
                val steps = 50
                val delayMillis = durationMillis / steps

                for (i in 0..steps) {
                    val progress = i.toFloat() / steps
                    val interpolatedZoom = currentZoom + (targetZoom - currentZoom) * progress * progress * progress
                    cameraPositionState.move(CameraUpdateFactory.zoomTo(interpolatedZoom))
                    kotlinx.coroutines.delay(delayMillis)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // 사용자 캔버스 데이터 지도에 표시
            canvasDataList.forEach { canvas ->
                val previewBoxId = canvas.preview_box_id // preview_box_id 가져오기
                val imageUrl by produceState<String?>(initialValue = null) {
                    value = try {
                        FirebaseStorageHelper.getImageUrl(previewBoxId)
                    } catch (e: Exception) {
                        null
                    }
                }

                if (imageUrl != null) {
                    CanvasMarker(
                        position = LatLng(canvas.avg_gps_latitude, canvas.avg_gps_longitude),
                        imageUrl = imageUrl,
                        imageResId = R.drawable.paris, // paris.png를 사용자 마커 아이콘으로 설정
                        canvasId = canvas.toString(), // canvasId 전달
                        cameraPositionState = cameraPositionState,
                        onClick = { canvasId ->
                            // 사용자 마커 클릭 시 동작
                            onNavigateToCanvas(canvasId) // CanvasActivity로 이동
                        }
                    )
                }
            }

            // 친구 캔버스 데이터 지도에 표시
            friendCanvasDataList.forEach { canvas ->
                val previewBoxId = canvas.preview_box_id // preview_box_id 가져오기
                val imageUrl by produceState<String?>(initialValue = null) {
                    value = try {
                        FirebaseStorageHelper.getImageUrl(previewBoxId)
                    } catch (e: Exception) {
                        println("Error fetching image URL for $previewBoxId: ${e.message}")
                        null
                    }
                }

                if (imageUrl != null) {
                    CanvasMarker(
                        position = LatLng(canvas.avg_gps_latitude, canvas.avg_gps_longitude),
                        imageUrl = imageUrl,
                        canvasId = canvas.toString(),
                        imageResId = R.drawable.paris, // 친구 마커 아이콘 설정
                        cameraPositionState = cameraPositionState,
                        onClick = { canvasId ->
                            // 친구 마커 클릭 시 동작
                            onNavigateToCanvas(canvasId) // CanvasActivity로 이동
                        },
                        borderColor = Color.Green
                    )
                }
            }
        }
    }
    // 검색 필드 및 버튼 상단 고정
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    ,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Search : Seoul") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = TextFieldDefaults.textFieldColors(
                        containerColor = Color.LightGray,
                        cursorColor = Color.Black,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
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
                                    locationName = "위치를 찾을 수 없습니다."
                                }
                            }
                        }
                    },
                    modifier = Modifier.height(56.dp),

                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD6D6D6),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "검색",
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 하단 버튼
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = { onNavigateToListView() },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp)
                        .width(110.dp)
                        .height(48.dp)
                        .border(width = 0.5.dp, color = Color.Gray, shape = RoundedCornerShape(25.dp)), // 테두리 추가
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFF9C4),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = "List",
                        fontSize = 25.sp,
                        fontFamily = TitleFontFamily,
                        color = Color(0xFF2196F3)
                    )
                }

                FloatingActionButton(
                    onClick = { onNavigateToMapSetup() },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 16.dp),
                    containerColor = Color(0xFFB3E5FC),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Text(
                        text = "+",
                        color = Color(0xFF001F3F),
                        fontSize = 24.sp
                    )
                }
            }
        }

        // 카메라 이동 완료 후 위치 업데이트
        LaunchedEffect(cameraPositionState.isMoving) {
            if (!cameraPositionState.isMoving) {
                val currentPosition = cameraPositionState.position.target
                locationName = getAddressFromLatLng(currentPosition, "AIzaSyD2nKPV8VIa2XbivuB6gtIqBRI4thUjbc0")
            }
        }
    }

