
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.travelsketch.R

data class ListElementData(
    val title: String,
    val canvasId: String, // canvasId 추가
    val isVisible : Boolean
)

@Composable
fun ListViewScreen(
    items: List<ListElementData>,
    friendItems: List<ListElementData>, // 친구의 캔버스 데이터 추가
    onNavigateToListView: () -> Unit,
    onNavigateToMapSetup: () -> Unit,
    onAddFriend: (String) -> Unit, // 친구 추가 콜백\
    onToggleVisibility: (String, Boolean) -> Unit, // visibility 변경 콜백 추가
    onNavigateToCanvas: (String, Boolean) -> Unit, // editable 상태 전달 추가
    onUpdateTitle: (String, String) -> Unit,
    onUpdateCoverImage: (String) -> Unit, // Update Cover Image 콜백 추가
    onDeleteCanvas: (String) -> Unit // Delete Canvas 콜백 추가
) {
    val CustomFontFamily = FontFamily(
        Font(R.font.waving_at_christmas) // 파일 이름은 확장자 없이 사용
    )
    val TitleFontFamily = FontFamily(
        Font(R.font.typo_crayonm) // 파일 이름은 확장자 없이 사용
    )
    // 팝업 상태 관리
    var showPopup by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(TextFieldValue("")) }

    Box(
        modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF335577))
    ) {
        // 리스트는 상단부터 시작
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // 친구의 캔버스 데이터 섹션
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(), // 가로 전체 크기
                    contentAlignment = Alignment.Center // 박스 내부 요소를 가운데 정렬
                ) {
                    Text(
                        text = "My Canvas List",
                        fontSize = 40.sp,
                        fontFamily = CustomFontFamily,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
            }
            items(items) { item ->
                ListElement(
                    title = item.title,
                    canvasId = item.canvasId, // canvasId 전달
                    isCurrentUserCanvas = true, // 현재 사용자의 캔버스
                    isVisible = item.isVisible, // 초기 is_visible 값
                    onToggleVisibility = onToggleVisibility,
                    onNavigateToCanvas = { onNavigateToCanvas(item.canvasId, true) }, // editable = true 전달
                    onUpdateTitle = onUpdateTitle,
                    onUpdateCoverImage = { onUpdateCoverImage(item.canvasId) }, // Update Cover Image 전달
                    onDeleteCanvas = { onDeleteCanvas(item.canvasId) } // Delete Canvas 전달
                )
            }
            // 친구의 캔버스 데이터 섹션
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier.fillMaxWidth(), // 가로 전체 크기
                    contentAlignment = Alignment.Center // 박스 내부 요소를 가운데 정렬
                ) {
                    Text(
                        text = "Friends' Canvases",
                        fontSize = 40.sp,
                        fontFamily = CustomFontFamily,
                        color = Color.White
                    )
                }
            }
            items(friendItems) { item ->
                ListElement(
                    title = item.title,
                    canvasId = item.canvasId,
                    isCurrentUserCanvas = false, // 친구의 캔버스
                    isVisible = true, // 초기 is_visible 값
                    onToggleVisibility = { _, _ -> }, // 비어 있는 콜백 전달
                    onNavigateToCanvas = { onNavigateToCanvas(item.canvasId, false) }, // editable = false 전달
                    onUpdateTitle = onUpdateTitle, // onUpdateTitle 전달
                    onUpdateCoverImage = {}, // 친구 캔버스에서는 사용하지 않음
                    onDeleteCanvas = {} // 친구 캔버스에서는 사용하지 않음
                )
            }
        }

        // 버튼을 화면 하단에 고정
        Button(
            onClick = { onNavigateToListView() },
            modifier = Modifier
                .absoluteOffset(y = (-32).dp) // 화면 하단에서 32dp 위로
                .align(Alignment.BottomCenter) // 하단 중앙 정렬
                .width(115.dp) // 너비 설정
                .height(48.dp) // 높이 설정
                .border(width = 0.5.dp, color = Color.Black, shape = RoundedCornerShape(25.dp)), // 테두리 추가

        colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFFFDE7)
            , // 밝은 파란색
                contentColor = Color.Black // 텍스트 색상
            )
        ) {
            Text(
                text = "Map",
                fontSize = 25.sp,
                fontFamily = TitleFontFamily,
                color = Color(0xFF2196F3)
            )
        }
        FloatingActionButton(
            onClick = { onNavigateToMapSetup() }, // 새로운 콜백 호출
            modifier = Modifier
                .align(Alignment.BottomStart) // 오른쪽 하단 정렬
                .padding(start = 16.dp, bottom = 40.dp),
            containerColor = Color(0xFFB3E5FC), // 버튼 배경색
            contentColor = Color.White, // 아이콘 또는 텍스트 색상
            shape = CircleShape // 동그란 모양
        ) {
            Text(
                text = "+", // "+" 텍스트 표시
                color = Color(0xFF001F3F),
                fontSize = 24.dp.value.sp // 텍스트 크기 설정
            )
        }
        // 친구 추가 버튼
        FloatingActionButton(
            onClick = { showPopup = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 40.dp),
            containerColor = Color(0xFFB3E5FC),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text(
                text = "\uD83D\uDC64",
                color = Color.White,
                fontSize = 20.dp.value.sp
            )
        }
        // 팝업 UI
        if (showPopup) {
            AlertDialog(
                onDismissRequest = { showPopup = false },
                title = { Text("Add Friend") },
                text = {
                    Column {
                        Text("Enter your friend's email:")
                        TextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            placeholder = { Text("Email") }
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        onAddFriend(emailInput.text)
                        showPopup = false
                    }) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    Button(onClick = { showPopup = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
