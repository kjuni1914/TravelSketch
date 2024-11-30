
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ListElementData(
    val title: String,
    val canvasId: String // canvasId 추가
)

@Composable
fun ListViewScreen(
    items: List<ListElementData>,
    onNavigateToListView: () -> Unit,
    onNavigateToMapSetup: () -> Unit,
//    onElementClick: (String) -> Unit // canvasId 전달 콜백

) {
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
//                    onClick = onElementClick // 클릭 이벤트 처리
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
                .align(Alignment.BottomEnd) // 오른쪽 하단 정렬
                .padding(16.dp), // 화면 경계로부터 여백
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
    }
}
