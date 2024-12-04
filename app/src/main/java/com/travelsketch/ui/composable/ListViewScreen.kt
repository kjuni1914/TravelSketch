
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    onToggleVisibility: (String, Boolean) -> Unit // visibility 변경 콜백 추가
//    onElementClick: (String) -> Unit // canvasId 전달 콜백

) {
    // 팝업 상태 관리
    var showPopup by remember { mutableStateOf(false) }
    var emailInput by remember { mutableStateOf(TextFieldValue("")) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 리스트는 상단부터 시작
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
        ) {
            items(items) { item ->
                ListElement(
                    title = item.title,
                    canvasId = item.canvasId, // canvasId 전달
                    isCurrentUserCanvas = true, // 현재 사용자의 캔버스
                    isVisible = item.isVisible, // 초기 is_visible 값
                    onToggleVisibility = onToggleVisibility
//                    onClick = onElementClick // 클릭 이벤트 처리
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
                        fontSize = 20.sp
                    )
                }
            }
            items(friendItems) { item ->
                ListElement(
                    title = item.title,
                    canvasId = item.canvasId,
                    isCurrentUserCanvas = false, // 친구의 캔버스
                    isVisible = true, // 초기 is_visible 값
                    onToggleVisibility = { _, _ -> } // 비어 있는 콜백 전달

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
                .height(48.dp), // 높이 설정
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFD6D6D6), // 밝은 회색 배경
                contentColor = Color.Black // 텍스트 색상
            )
        ) {
            Text(text = "Map View")
        }
        FloatingActionButton(
            onClick = { onNavigateToMapSetup() }, // 새로운 콜백 호출
            modifier = Modifier
                .align(Alignment.BottomStart) // 오른쪽 하단 정렬
                .padding(start = 16.dp, bottom = 40.dp),
            containerColor = Color(0xFF6200EE), // 버튼 배경색
            contentColor = Color.White, // 아이콘 또는 텍스트 색상
            shape = CircleShape // 동그란 모양
        ) {
            Text(
                text = "+", // "+" 텍스트 표시
                color = Color.White,
                fontSize = 24.dp.value.sp // 텍스트 크기 설정
            )
        }
        // 친구 추가 버튼
        FloatingActionButton(
            onClick = { showPopup = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 40.dp),
            containerColor = Color(0xFF03DAC5),
            contentColor = Color.White,
            shape = CircleShape
        ) {
            Text(
                text = "Add",
                color = Color.White,
                fontSize = 16.dp.value.sp
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
                        onAddFriend(emailInput.text) // 친구 추가 콜백 호출
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
